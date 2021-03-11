package catdata.aql;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.provers.DPKB;
import catdata.provers.KBExp;
import catdata.provers.KBTheory;
import catdata.provers.MonoidalProver;

public class MonoidalFreeDP<Ty, En, Sym, Fk, Att, Gen, Sk>
    extends DPKB<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String> {

  private final DPKB<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String> dpkb;

  @Override
  public String toString() {
    return dpkb.toString();
  }

  public MonoidalFreeDP(KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String> th) {
    super(th);
    if (!ok(th)) {
      throw new RuntimeException("Not monoidal: " + th);
    }
    KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String> local = new KBTheory<>(th);
    Iterator<Head<Ty, En, Sym, Fk, Att, Gen, Sk>> it = local.syms.keySet().iterator();
    while (it.hasNext()) {
      Head<Ty, En, Sym, Fk, Att, Gen, Sk> h = it.next();
      if (local.syms.get(h).first.size() > 1) {
        it.remove();
      }
    }
    // System.out.println(local);
    dpkb = new MonoidalProver<>(local);
  }

  // no equations involving symbols with arity > 1

  public static <Ty, En, Sym, Fk, Att, Gen, Sk> boolean ok(
      KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String> th) {
    for (Triple<Map<String, Chc<Ty, En>>, KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String>, KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String>> eq : th.eqs) {
      if (!ok(eq.second) || !ok(eq.third) || eq.first.size() > 1) {
        return false;
      }
    }
    return true;
  }

  public static <Ty, En, Sym, Fk, Att, Gen, Sk> boolean ok(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col) {
    for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : col.eqs()) {
      if (!ok(eq.lhs) || !ok(eq.rhs) || eq.ctx.size() > 1) {
        return false;
      }
    }
    return true;
  }

  public static <Ty, En, Sym, Fk, Att, Gen, Sk> boolean ok(Term<Ty, En, Sym, Fk, Att, Gen, Sk> e) {
    if (e.var != null || e.gen() != null || e.sk() != null || e.obj() != null
        || (e.sym() != null && e.args().size() == 0) || (e.sym() != null && e.args().size() == 1)) {
      return true;
    } else if (e.args().size() > 1) {
      return false;
    }
    for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> x : e.args()) {
      if (ok(x)) {
        return true;
      }
    }
    return false;
  }

  public static <Ty, En, Sym, Fk, Att, Gen, Sk> boolean ok(KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String> e) {
    if (e.isVar()) {
      return true;
    } else if (e.getArgs().size() > 1) {
      return false;
    } else if (e.getArgs().size() == 1) {
      return ok(e.getArgs().get(0));
    }
    return true;
  }

  @Override
  public synchronized boolean eq(Map<String, Chc<Ty, En>> ctx, KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String> lhs,
      KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String> rhs) {
    if (ctx.size() > 1) {
      Util.anomaly();
    }
    if (lhs.getArgs().size() > 1 && rhs.getArgs().size() > 1) {
      if (!lhs.f().equals(rhs.f())) {
        return false;
      }
      Iterator<KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String>> it = lhs.getArgs().iterator();
      Iterator<KBExp<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, String>> jt = rhs.getArgs().iterator();
      while (it.hasNext()) {
        if (!eq(ctx, it.next(), jt.next())) {
          return false;
        }
      }
      //System.out.println("true on " + lhs + " and " + rhs);
      return true;
    } else if (lhs.getArgs().size() <= 1 && rhs.getArgs().size() <= 1) {
      return dpkb.eq(ctx, lhs, rhs);
    }
    return false;
  }

  @Override
  public synchronized void add(Head<Ty, En, Sym, Fk, Att, Gen, Sk> c, Chc<Ty, En> t) {
    this.kb.syms.put(c, new Pair<>(Collections.emptyList(), t));
    dpkb.add(c, t);
  }

  @Override
  public boolean supportsTrivialityCheck() {
    return false;
  }

};
