package catdata.cql.fdm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

//import org.apache.commons.collections4.list.TreeList;

import catdata.Chc;
import catdata.Pair;
import catdata.RuntimeInterruptedException;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.cql.Algebra;
import catdata.cql.AqlOptions;
import catdata.cql.Collage;
import catdata.cql.DP;
import catdata.cql.Eq;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class SlowInitialAlgebra<Ty, En, Sym, Fk, Att, Gen, Sk, X> extends
    Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Chc<Sk, Pair<X, Att>>> implements DP<Ty, En, Sym, Fk, Att, Gen, Sk> { // is

  @Override
  public boolean hasNulls() {
    return talg().sks.isEmpty();
  }

  // DP
  @Override
  public Object printX(En en, X x) {
    return repr(en, x).toString(Util.voidFn(), printGen, false);
  }

  @Override
  public Object printY(Ty ty, Chc<Sk, Pair<X, Att>> y) {
    return y.left ? printSk.apply(y.l) : printX(schema.atts.get(y.r.second).first, y.r.first) + "." + y.r.second;
  }

  private DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp; // may just be on entity side, if java
  private final Function<Unit, DP<Ty, En, Sym, Fk, Att, Gen, Sk>> ddp;

  private final Map<En, Set<X>> ens;
  private final Map<X, Map<Fk, X>> fks = new THashMap<>();
  private final Map<X, Term<Void, En, Void, Fk, Void, Gen, Void>> reprs = new THashMap<>();
  private final Map<Term<Void, En, Void, Fk, Void, Gen, Void>, X> nfs = new THashMap<>();

  private final Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col;
  private final Schema<Ty, En, Sym, Fk, Att> schema;
  private final Iterator<X> fresh;

  private final Function<Gen, String> printGen;
  private final Function<Sk, String> printSk;

  private AqlOptions ops;

  private final Iterable<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqsIt;

  public SlowInitialAlgebra(Function<Unit, DP<Ty, En, Sym, Fk, Att, Gen, Sk>> dp, Schema<Ty, En, Sym, Fk, Att> schema,
      Map<Gen, En> gens, Map<Sk, Ty> sks,
      Iterable<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqs,
      Iterator<X> fresh, Function<Gen, String> printGen, Function<Sk, String> printSk, AqlOptions ops) {
    ens = Util.newSetsFor(schema.ens);
    this.schema = schema;
    this.fresh = fresh;
    this.printGen = printGen;
    this.printSk = printSk;
    this.ops = ops;
    this.col = new CCollage<>(schema.collage());
    this.eqsIt = eqs;
    this.ddp = dp;
    gens.forEach((kk, vv) -> {
      this.col.gens().put(kk, vv);
    });
    sks.forEach((kk, vv) -> {
      this.col.sks().put(kk, vv);
    });
    eqs.forEach(p -> {
      this.col.eqs().add(new Eq<>(null, p.first, p.second));
    });

  }

  private void initDp() {
    if (dp != null) {
      return;
    }
    this.dp = ddp.apply(Unit.unit);
    try {
      while (saturate1())
        ;
    } catch (InterruptedException exn) {
      throw new RuntimeInterruptedException(exn);
    }
  }

  private synchronized boolean add(Term<Void, En, Void, Fk, Void, Gen, Void> term) throws InterruptedException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException();
    }

    X x = nf0(term);
    if (x != null) {
      return false;
    }

    x = fresh.next();

    nfs.put(term, x);
    ens.get(col.type(Collections.emptyMap(), term.convert()).r).add(x);
    reprs.put(x, term);

    Map<Fk, X> map = (new THashMap<>());
    for (Fk fk : schema().fks.keySet()) {
      if (!col.type(Collections.emptyMap(), term.convert()).r.equals(schema().fks.get(fk).first)) {
        continue;
      }
      add(Term.Fk(fk, term));
      map.put(fk, nf0(Term.Fk(fk, term)));
    }
    fks.put(x, map);

    return true;
  }

  @SuppressWarnings("unchecked")
  private synchronized boolean saturate1() throws InterruptedException {
    boolean changed = false;
    for (Gen gen : col.gens().keySet()) {
      @SuppressWarnings("rawtypes")
      Term xx = Term.Gen(gen);
      if (col.type(Collections.emptyMap(), xx).left) {
        continue;
      }
      changed = changed | add(xx);
    }
    for (Fk fk : col.fks().keySet()) {
      En en = col.fks().get(fk).first;
      List<X> set = (new ArrayList<>(ens.get(schema().fks.get(fk).first)));
      for (X x : set) { // concurrent modification otherwise
        changed = changed | add(Term.Fk(fk, repr(en, x)));
      }
    }

    return changed;
  }

  @Override
  public Schema<Ty, En, Sym, Fk, Att> schema() {
    return schema;
  }

  @Override
  public Collection<X> en(En en) {
    initDp();
    return ens.get(en);
  }

  @Override
  public X fk(Fk fk, X x) {
    initDp();
    X r = fks.get(x).get(fk);
    if (r == null) {
      throw new RuntimeException("Anomaly, please report: foreign key " + fk + " on ID (" + x
          + ") has no mapping; available: " + fks.get(x));
    }
    return r;
  }

  @Override
  public Term<Void, En, Void, Fk, Void, Gen, Void> repr(En en, X x) {
    initDp();
    Term<Void, En, Void, Fk, Void, Gen, Void> ret = reprs.get(x);
    return ret;
  }

  private synchronized X nf0(Term<Void, En, Void, Fk, Void, Gen, Void> term) {
    X xx = nfs.get(term);
    if (xx != null) {
      return xx;
    }
    En en = col.type(Collections.emptyMap(), term.convert()).r;
    for (X x : ens.get(en)) {
      if (dp.eq(null, term.convert(), repr(en, x).convert())) {
        nfs.put(term, x);
        return x;
      }
    }
    return null;
  }

  @Override
  public X gen(Gen gen) {
    initDp();
    X x = nf0(Term.Gen(gen));
    if (x == null) {
      throw new RuntimeException("Anomaly: please report");
    }
    return x;
  }

  @Override
  public boolean eq(Map<String, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs,
      Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
    if (ctx != null && !ctx.isEmpty()) {
      Util.anomaly();
    }
    initDp();

    return dp.eq(null, lhs, rhs);
  }

  @Override
  public synchronized TAlg<Ty, Sym, Chc<Sk, Pair<X, Att>>> talg0() {
    if (talg != null) {
      return talg.talg.out;
    }
    initDp();
    talg = new TalgSimplifier<>(this, eqsIt.iterator(), col.sks(),
        (Integer) ops.getOrDefault(AqlOption.talg_reduction));
    return talg.talg.out;
  }

  @Override
  public Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> att(Att att, X x) {
    initDp();
    return reprT0(Chc.inRight(new Pair<>(x, att)));
  }

  private Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> reprT0(Chc<Sk, Pair<X, Att>> y) {
    talg();
    return schema().typeSide.js.java_tys.isEmpty() ? talg.simpl(Term.Sk(y))
        : schema.typeSide.js.reduce(talg.simpl(Term.Sk(y)));
  }

  private TalgSimplifier<Ty, En, Sym, Fk, Att, Gen, Sk, X, Chc<Sk, Pair<X, Att>>> talg;

  Boolean b;

  @Override
  public synchronized boolean hasFreeTypeAlgebra() {
    if (col.eqs().size() == 0) {
      return true;
    }
    if (b != null) {
      return b;
    }
    initDp();
    talg();
    Set<Eq<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>> l = (new THashSet<>(
        schema().typeSide.eqs.size()));
    for (Triple<Map<String, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>> eq : schema().typeSide.eqs) {
      l.add(new Eq<>(Util.inLeft(eq.first), talg.transX(eq.second.convert()), talg.transX(eq.third.convert())));
    }
    b = Util.diff(talg().eqsNoDefns(), l).isEmpty();
    return b;
  }

  public boolean hasFreeTypeAlgebraOnJava() {
    if (col.eqs().size() == 0) {
      return true;
    }
    return talg().eqsNoDefns().stream()
        .filter(x -> schema().typeSide.js.java_tys.containsKey(talg().type(schema.typeSide, x.first)))
        .collect(Collectors.toList()).isEmpty();
  }

  @Override
  public Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> sk(Sk sk) {
    return reprT0(Chc.inLeft(sk));
  }

  @Override
  public String toStringProver() {
    initDp();
    return dp.toStringProver();
  }

//  @Override
  public DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp() {
    return this;
  }

  public String talgToString() {
    return talg.talg.toString();
  }

  @Override
  public int size(En en) {
    return ens.get(en).size();
  }

  @Override
  public Chc<Sk, Pair<X, Att>> reprT_prot(Chc<Sk, Pair<X, Att>> y) {
    return y;
  }

}
