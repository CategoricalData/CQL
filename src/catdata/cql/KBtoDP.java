package catdata.cql;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.provers.DPKB;

public class KBtoDP<Ty, En, Sym, Fk, Att, Gen, Sk> implements DP<Ty, En, Sym, Fk, Att, Gen, Sk> {

  // private final Map<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>, Boolean> cache = new
  // THashMap<>();

  private final Function<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> simp;

  private final DPKB<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String> dpkb;

  private final AqlJs<Ty, Sym> js;

  private final boolean allowNew;

  // private final Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col;

  public KBtoDP(AqlJs<Ty, Sym> js,
      Function<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> simp,
      DPKB<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String> dpkb, boolean allowNew,
      Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col) {
    this.simp = simp;
    this.dpkb = dpkb;
    this.js = js;
    this.allowNew = allowNew;
    // this.col = col;
    // dpkbs kb can be smaller than col, mediated by simp
  }

  @Override
  public synchronized boolean eq(Map<String, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs,
      Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
    if (ctx == null) {
      ctx = Collections.emptyMap();
    }
    if (lhs.equals(rhs)) { // need
      return true;
    }

    Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs2 = simp.apply(lhs);
    Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs2 = simp.apply(rhs);

    if (lhs.hasTypeType(ctx)) {
      lhs2 = js.reduce(lhs2);
      rhs2 = js.reduce(rhs2);

    }

    boolean b;
    if (lhs2.equals(rhs2)) {
      b = true;
    } else {
      dealWithNew(lhs2, allowNew);
      dealWithNew(rhs2, allowNew);

      b = dpkb.eq(ctx, lhs2.toKB(), rhs2.toKB());
    }

    return b;
  }

  private void dealWithNew(Term<Ty, En, Sym, Fk, Att, Gen, Sk> t, boolean allowNew) {
    if (allowNew) {
      if (dpkb.kb == null) {
        return;
      }
      for (Pair<Object, Ty> x : t.objs()) {
        Head<Ty, En, Sym, Fk, Att, Gen, Sk> h = Head.mkHead(x.first, x.second);
        if (!dpkb.kb.syms.containsKey(h)) {
          dpkb.add(h, Chc.inLeft(x.second));
        }
      }
    } else {
      throw new RuntimeException("New lhs java object: " + t.toStringUnambig());
    }
  }

  @Override
  public String toStringProver() {
    return "Definitional simplification and reflexivity wrapping plus java of " + (dpkb.getClass().getName()) +"\n\n" + dpkb;
  }

  @Override
  public boolean supportsTrivialityCheck() {
    return dpkb.supportsTrivialityCheck();
  }

};