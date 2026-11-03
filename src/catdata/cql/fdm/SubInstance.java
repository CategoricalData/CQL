package catdata.cql.fdm;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.Algebra;
import catdata.cql.AqlOptions;
import catdata.cql.DP;
import catdata.cql.Instance;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.exp.QueryExpReformulate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class SubInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
    extends Instance<Ty, En, Sym, Fk, Att, Pair<En, X>, Sk, Pair<En, X>, Y> {

  private final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;
  private final Map<En, Set<X>> xs;

  @Override
  public IMap<Pair<En, X>, En> gens() {
    return new IMap<>() {

      @Override
      public En get(Pair<En, X> x) {
        return x.first;
      }

      @Override
      public boolean containsKey(Pair<En, X> x) {
        return xs.get(x.first).contains(x.second);
      }

      @Override
      public void entrySet(BiConsumer<? super Pair<En, X>, ? super En> f) {
        for (En en : I.schema().ens) {
          xs.get(en).forEach(z -> f.accept(new Pair<>(en, z), en));
        }
      }

      @Override
      public int size() {
        int i = 0;
        for (En en : I.schema().ens) {
          i += xs.get(en).size();
        }
        return i;
      }

      @Override
      public En remove(Pair<En, X> x) {
        xs.get(x).remove(x.second);
        return x.first;
      }

      @Override
      public void put(Pair<En, X> x, En y) {
        xs.get(y).add(x.second);
      }
    };
  }

  public Transform<Ty, En, Sym, Fk, Att, Pair<En, X>, Sk, Gen, Sk, Pair<En, X>, Y, X, Y>
      getInclusion() {
    return new Transform<>() {

      @Override
      public BiFunction<Pair<En, X>, En, Term<Void, En, Void, Fk, Void, Gen, Void>> gens() {
        return new BiFunction<>() {

          @Override
          public Term<Void, En, Void, Fk, Void, Gen, Void> apply(Pair<En, X> t, En u) {
            return I.algebra().repr(t.first, t.second);
          }
        };
      }

      @Override
      public BiFunction<Sk, Ty, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> sks() {
        return new BiFunction<>() {

          @Override
          public Term<Ty, En, Sym, Fk, Att, Gen, Sk> apply(Sk t, Ty u) {
            return Term.Sk(t);
          }
        };
      }

      @Override
      public Instance<Ty, En, Sym, Fk, Att, Pair<En, X>, Sk, Pair<En, X>, Y> src() {
        return SubInstance.this;
      }

      @Override
      public Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> dst() {
        return I;
      }
    };
  }

  public static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> boolean core0(
      Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I, AqlOptions ops) {
    Iterable<Pair<En, X>> j = I.algebra().allXs0();
    Pair<En, X>[] r = Iterators.toArray(j.iterator(), Pair.class);
    ArrayList<Pair<En, X>> p = Lists.newArrayList(r);

    for (Set<Pair<En, X>> s : Util.powerSet(p)) {
      if (s.size() == I.size()) {
        continue;
      }
      try {
        var i = new SubInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>(s, I, ops);
        Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Pair<En, X>, Sk, X, Y, Pair<En, X>, Y> t =
            QueryExpReformulate.homI(I, i);

        if (t != null) {
          return false;
        }

      } catch (Exception ex) {

      }
    }
    return true;
  }

  public static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
      Pair<
              Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Pair<En, X>, Sk, X, Y, Pair<En, X>, Y>,
              SubInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>>
          subs(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I, AqlOptions ops) {
    Iterable<Pair<En, X>> j = I.algebra().allXs0();
    Pair<En, X>[] r = Iterators.toArray(j.iterator(), Pair.class);
    ArrayList<Pair<En, X>> p = Lists.newArrayList(r);

    var z = new LinkedList<>(Util.powerSet(p));
    z.sort(
        new Comparator<>() {

          @Override
          public int compare(Set<Pair<En, X>> o1, Set<Pair<En, X>> o2) {
            return Integer.compare(o1.size(), o2.size());
          }
        });

    for (Set<Pair<En, X>> s : z) {
      //			System.out.println("trying " + Util.sep(s, " "));
      try {
        var i = new SubInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>(s, I, ops);
        Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Pair<En, X>, Sk, X, Y, Pair<En, X>, Y> t =
            QueryExpReformulate.homI(I, i);

        if (t != null && core0(i, ops)) {
          return new Pair<>(t, i);
        }

      } catch (Exception ex) {
        // ex.printStackTrace();
      }
    }
    return Util.anomaly();
  }

  public SubInstance(
      Set<Pair<En, X>> xs0, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I0, AqlOptions ops) {
    xs = Util.newSetsFor(I0.schema().ens);
    for (Pair<En, X> x : xs0) {
      xs.get(x.first).add(x.second);
    }

    I = I0;

    algebra = new InnerAlgebra();

    validate();
    validateMore();
    algebra().validateMore();
  }

  @Override
  public Schema<Ty, En, Sym, Fk, Att> schema() {
    return I.schema();
  }

  @Override
  public IMap<Sk, Ty> sks() {
    return I.sks();
  }

  private Term<Ty, En, Sym, Fk, Att, Gen, Sk> conv(Term<Ty, En, Sym, Fk, Att, Pair<En, X>, Sk> t) {
    if (t.obj() != null) {
      return t.convert();
    } else if (t.sym() != null) {
      return Term.Sym(t.sym(), t.args.stream().map(z -> conv(z)).collect(Collectors.toList()));
    } else if (t.sk() != null) {
      return t.convert();
    } else if (t.att() != null) {
      return Term.Att(t.att(), conv(t.arg));
    } else if (t.fk() != null) {
      return Term.Fk(t.fk(), conv(t.arg));
    } else if (t.gen() != null) {
      return I.algebra().repr(t.gen().first, t.gen().second).convert();
    }
    throw new RuntimeException(t.toString());
  }

  @Override
  public DP<Ty, En, Sym, Fk, Att, Pair<En, X>, Sk> dp() {
    return new DP<Ty, En, Sym, Fk, Att, Pair<En, X>, Sk>() {

      @Override
      public String toStringProver() {
        return "Sub-instance wrapper of " + I.algebra().toStringProver();
      }

      @Override
      public boolean eq(
          Map<String, Chc<Ty, En>> ctx,
          Term<Ty, En, Sym, Fk, Att, Pair<En, X>, Sk> lhs,
          Term<Ty, En, Sym, Fk, Att, Pair<En, X>, Sk> rhs) {
        if (ctx != null && !ctx.isEmpty()) {
          throw new RuntimeException("Cannot answer a non-ground equation");
        }

        Chc<Ty, En> x = type(rhs);
        if (x.left) {
          return I.dp().eq(null, conv(lhs), conv(rhs));
        }
        return algebra().intoX(lhs.convert()).equals(algebra().intoX(rhs.convert()));
      }
    };
  }

  @Override
  public Algebra<Ty, En, Sym, Fk, Att, Pair<En, X>, Sk, Pair<En, X>, Y> algebra() {
    return algebra;
  }

  private Algebra<Ty, En, Sym, Fk, Att, Pair<En, X>, Sk, Pair<En, X>, Y> algebra;

  private final class InnerAlgebra
      extends Algebra<Ty, En, Sym, Fk, Att, Pair<En, X>, Sk, Pair<En, X>, Y> {

    private InnerAlgebra() {}

    @Override
    public Schema<Ty, En, Sym, Fk, Att> schema() {
      return I.schema();
    }

    @Override
    public Iterable<Pair<En, X>> en(En en) {
      return new Iterable<>() {

        @Override
        public Iterator<Pair<En, X>> iterator() {
          return Iterators.transform(xs.get(en).iterator(), z -> new Pair<>(en, z));
        }
      };
    }

    @Override
    public Pair<En, X> fk(Fk fk, Pair<En, X> x) {
      return new Pair<>(I.schema().fks.get(fk).second, I.algebra().fk(fk, x.second));
    }

    @Override
    public Term<Ty, Void, Sym, Void, Void, Void, Y> att(Att att, Pair<En, X> x) {
      return I.algebra().att(att, x.second);
    }

    @Override
    public Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Sk sk) {
      return I.algebra().sk(sk);
    }

    @Override
    public Pair<En, X> gen(Pair<En, X> gen) {
      return gen;
    }

    @Override
    public synchronized Term<Void, En, Void, Fk, Void, Pair<En, X>, Void> repr(
        En en, Pair<En, X> x) {
      return Term.Gen(x);
    }

    @Override
    public TAlg<Ty, Sym, Y> talg0() {
      return I.algebra().talg();
    }

    @Override
    public String toStringProver() {
      return "Sub-instance algebra wrapper of " + I.algebra().toStringProver();
    }

    @Override
    public Object printX(En en, Pair<En, X> x) {
      return I.algebra().printX(en, x.second);
    }

    @Override
    public Object printY(Ty ty, Y y) {
      return I.algebra().printY(ty, y);
    }

    @Override
    public boolean hasFreeTypeAlgebra() {
      return I.algebra().hasFreeTypeAlgebra();
    }

    @Override
    public synchronized int size(En en) {
      return xs.get(en).size();
    }

    @Override
    public Chc<Sk, Pair<Pair<En, X>, Att>> reprT_prot(Y y) {
      var z = I.algebra().reprT_prot(y);
      if (z.left) {
        return Chc.inLeft(z.l);
      } else {
        return Chc.inRight(
            new Pair<>(new Pair<>(I.schema().atts.get(z.r.second).first, z.r.first), z.r.second));
      }
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
