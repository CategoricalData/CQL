package catdata.aql.fdm;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import org.apache.commons.collections.ListUtils;

import com.github.jsonldjava.shaded.com.google.common.collect.Iterables;

import catdata.Chc;
import catdata.Pair;
import catdata.RuntimeInterruptedException;
import catdata.Unit;
import catdata.aql.Algebra;
import catdata.aql.AqlOptions;
import catdata.aql.Collage;
import catdata.aql.DP;
import catdata.aql.Instance;
import catdata.aql.Schema;
import catdata.aql.Term;

import catdata.graph.UnionFind;
import gnu.trove.map.hash.THashMap;

public class DistinctInstanceParallel<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
    extends Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> {

  private final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;

  private final Map<En, boolean[][]> distinguished = Collections.synchronizedMap(new THashMap<>());

  private final Map<En, UnionFind<X>> ufs = new THashMap<>();

  private final int conv(En en, X x) {
    return ufs.get(en).iso2.get(x);
  }

  private final X deconv(En en, int x) {
    return ufs.get(en).iso1[x];
  }

  private boolean undistinguished(En en, int x, int y, List<Fk> path) {
    if (x == y) {
      return true;
    }
    var m = distinguished.get(en);
    synchronized (m) {
      if (m[x][y]) {
        return false;
      }
    }
    for (Fk fk : path) {
      En en2 = schema().fks.get(fk).second;
      x = conv(en2, I.algebra().fk(fk, deconv(en, x)));
      y = conv(en2, I.algebra().fk(fk, deconv(en, y)));
      en = en2;

      if (x == y) {
        return true;
      }
      m = distinguished.get(en);
      synchronized (m) {
        if (m[x][y]) {
          return false;
        }
      }
    }
    for (Att att : schema().attsFrom(en)) {
      var a = I.algebra().att(att, deconv(en, x));
      var b = I.algebra().att(att, deconv(en, y));

      if (!I.dp().eq(null, I.reprT(a), I.reprT(b))) {
        synchronized (m) {
          m[x][y] = true;
          m[y][x] = true;
        }
        return false;
      }
    }
    return true;
  }

  public DistinctInstanceParallel(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I0, AqlOptions ops) {
    I = I0;
    Map<En, List<List<Fk>>> paths = new THashMap<>();

    for (En en : I.schema().ens) {
      ufs.put(en, new UnionFind<>(I.algebra().size(en), I.algebra().en(en)));
      int num = I.algebra().size(en);
      distinguished.put(en, new boolean[num][num]);
      List<Fk> l = Collections.emptyList();
      paths.put(en, Collections.singletonList(l));
    }
    ExecutorService exec = Executors.newFixedThreadPool(1); // Executors.newCachedThreadPool();

    for (;;) {

      // System.out.println("paths: " + paths);
      List<Boolean> l = new LinkedList<>();
      l.add(false);
      List<Boolean> changed = Collections.synchronizedList(l);

      List<Future<Unit>> results = new LinkedList<>();

      for (En en : I.schema().ens) {
        for (List<Fk> path : paths.get(en)) {
          En en2 = path.isEmpty() ? en : schema().fks.get(path.get(0)).first;
    
          Callable<Unit> c = new Callable<>() {
            @Override
            public Unit call() {
              boolean[][] d = distinguished.get(en2);
              for (int x = 0; x < d.length; x++) {
                for (int y = 0; y < d.length; y++) {
                  if (undistinguished(en2, x, y, path)) {
                    // System.out.println("unmerge " + x + " " + y);
                    synchronized (d) {
                      if (d[x][y]) {
                        d[x][y] = false;
                        changed.set(0, true);
                      }
                    }
                  }
                }
              }
              return Unit.unit;
            }
          };
          results.add(exec.submit(c));
        }
      }

      try {
        for (Future<Unit> fr : results) {
          fr.get();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
        throw new RuntimeInterruptedException(e);
      } catch (ExecutionException e) {
        e.printStackTrace();
        throw new RuntimeException(e.getMessage());
      }

      if (!changed.get(0)) {
        break;
      }
      Map<En, List<List<Fk>>> paths0 = new THashMap<>();
      for (En en : I.schema().ens) {
        paths0.put(en, new LinkedList<>());
      }
      for (En en : I.schema().ens) {
        List<List<Fk>> it = paths.get(en);
        for (List<Fk> path : it) {
          for (Fk fk : I.schema().fksFrom(en)) {
            En en2 = schema().fks.get(fk).second;
            paths0.get(en2).add(ListUtils.union(path, Collections.singletonList(fk)));
            // TODO: not consider equivalent paths?
          }
        }
      }
      // System.out.println("paths0: " + paths0);
      paths = paths0;
    }

    // exec.shutdown();

    for (En en : schema().ens) {
      boolean[][] d = distinguished.get(en);
      UnionFind<X> uf = ufs.get(en);
      for (int x = 0; x < d.length; x++) {
        for (int y = 0; y < d.length; y++) {
          if (x == y) {
            continue;
          }
          if (!d[x][y]) {
            uf.union(x, y);
          }
        }
      }
    }

//    this.validateMore();
//    algebra().validateMore();
    // System.out.println(algebra());
    // System.out.println(this);

    algebra = new InnerAlgebra();

    validate();
    I.eqs((l, r) -> {
      if (!dp().eq(null, l, r)) {
        throw new RuntimeException("Equation " + l + " = " + r + " not preserved in quotient (anomaly).");
      }
    });
  }

  // private TalgSimplifier<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> talg;
  public Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col;
  //private DP<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> dp_ty;

  @Override
  public Schema<Ty, En, Sym, Fk, Att> schema() {
    return I.schema();
  }

  @Override
  public IMap<Gen, En> gens() {
    return I.gens();
  }

  private List<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqs = new LinkedList<>();

  public synchronized void eqs(
      BiConsumer<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> f) {
    I.eqs(f);
    for (Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq : eqs) {
      f.accept(eq.first, eq.second);
    }
  }

  @Override
  public IMap<Sk, Ty> sks() {
    return I.sks();
  }

  @Override
  public DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp() {
    return new DP<Ty, En, Sym, Fk, Att, Gen, Sk>() {

      @Override
      public String toStringProver() {
        return "Distinct-instance wrapper of " + I.algebra().toStringProver();
      }

      @Override
      public boolean eq(Map<String, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs,
          Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
        if (ctx != null && !ctx.isEmpty()) {
          throw new RuntimeException("Cannot answer a non-ground equation");
        }
        // System.out.println(System.identityHashCode(col) + "in eq col is " + col);

        Chc<Ty, En> x = I.type(rhs);
        if (x.left) {
          return I.dp().eq(null, lhs, rhs);
        }
        return algebra().intoX(lhs.convert()).equals(algebra().intoX(rhs.convert()));
      }
    };
  }

  @Override
  public Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> algebra() {
    return algebra;
  }

  private Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> algebra;

  private final class InnerAlgebra extends Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> {

    private InnerAlgebra() {

    }

    @Override
    public Schema<Ty, En, Sym, Fk, Att> schema() {
      return I.schema();
    }

    @Override
    public Iterable<X> en(En en) {
      return Iterables.filter(I.algebra().en(en), x -> ufs.get(en).find(x).equals(x));
    }

    @Override
    public X fk(Fk fk, X x) {
      return ufs.get(I.schema().fks.get(fk).second).find(I.algebra().fk(fk, x));
    }

    @Override
    public Term<Ty, Void, Sym, Void, Void, Void, Y> att(Att att, X x) {
      return I.algebra().att(att, x);
    }

    @Override
    public Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Sk sk) {
      return I.algebra().sk(sk);
    }

    @Override
    public X gen(Gen gen) {
      return ufs.get(I.gens().get(gen)).find(I.algebra().gen(gen));
    }

    @Override
    public synchronized Term<Void, En, Void, Fk, Void, Gen, Void> repr(En en, X x) {
      return I.algebra().repr(en, x);
    }

    @Override
    public TAlg<Ty, Sym, Y> talg0() {
      return I.algebra().talg();
    }

    @Override
    public String toStringProver() {
      return "Distinct-instance algebra wrapper of " + I.algebra().toStringProver();
    }

    @Override
    public Object printX(En en, X x) {
      return I.algebra().printX(en, x);
    }

    @Override
    public Object printY(Ty ty, Y y) {
      return I.algebra().printY(ty, y);
    }

    @Override
    public boolean hasFreeTypeAlgebra() {
      return I.algebra().hasFreeTypeAlgebra();
    }

    private Map<En, Integer> sizes = new THashMap<>();

    @Override
    public synchronized int size(En en) {
      if (sizes.containsKey(en)) {
        return sizes.get(en);
      }
      int i = ufs.get(en).size();
      sizes.put(en, i);
      return i;
    }

    @Override
    public Chc<Sk, Pair<X, Att>> reprT_prot(Y y) {
      Chc<Sk, Pair<X, Att>> x = I.algebra().reprT_prot(y);
      if (x.left) {
        return x;
      }
      Pair<X, Att> p = x.r;
      En en = schema().atts.get(p.second).first;
      return Chc.inRightNC(new Pair<>(ufs.get(en).find(p.first), p.second));
    }

    @Override
    public boolean hasNulls() {
      return I.algebra().hasNulls();
    }
  }

  @Override
  public boolean requireConsistency() {
    return I.requireConsistency();
  }

  @Override
  public boolean allowUnsafeJava() {
    return I.allowUnsafeJava();
  }

}
