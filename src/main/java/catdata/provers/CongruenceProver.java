package catdata.provers;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.Triple;
import catdata.graph.UnionFind;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

//Congruence closure a la Nelson Oppen
//TODO aql better incremental computation for congruence closure
public class CongruenceProver<T, C, V> extends DPKB<T, C, V> {

  @Override
  public String toString() {
    return "CongruenceProver [uf=" + ufs + ", pred=" + pred + "]";
  }

  private Map<T, UnionFind<KBExp<C, V>>> ufs;

  // in paper this doesn't check label - appears to be typo (!)
  private boolean congruent(KBExp<C, V> u, KBExp<C, V> v) {
    if (!u.f().equals(v.f())) {
      return false;
    }
    Iterator<KBExp<C, V>> it = v.getArgs().iterator();
    for (KBExp<C, V> arg : u.getArgs()) {
      KBExp<C, V> arg2 = it.next();
      T t = arg.type(kb.syms, Collections.emptyMap());
      if (!ufs.get(t).connected(arg, arg2)) {
        return false;
      }
    }
    return true;
  }

  private void merge1(KBExp<C, V> u, KBExp<C, V> v) {
    T t = u.type(this.kb.syms, Collections.emptyMap());
    UnionFind<KBExp<C, V>> uf = ufs.get(t);
    if (uf.connected(u, v)) {
      return;
    }

    Set<KBExp<C, V>> pu = new THashSet<>();
    Set<KBExp<C, V>> pv = new THashSet<>();

    for (KBExp<C, V> exp : pred.get(t).keySet()) {
      if (uf.connected(u, exp)) {
        pu.addAll(pred.get(t).get(exp));
      }
    }

    for (KBExp<C, V> exp : pred.get(t).keySet()) {
      if (uf.connected(v, exp)) {
        pv.addAll(pred.get(t).get(exp));
      }
    }

    uf.union(u, v);

    for (KBExp<C, V> x : pu) {
      for (KBExp<C, V> y : pv) {
        if (!uf.connected(x, y) && congruent(x, y)) {
          merge1(x, y);
        }
      }
    }
  }

  private final Map<T, Map<KBExp<C, V>, Set<KBExp<C, V>>>> pred = new THashMap<>();

  public CongruenceProver(KBTheory<T, C, V> th) {
    super(th);
    for (T t : th.tys) {
      pred.put(t, new THashMap<>());
    }
    for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : kb.eqs) {
      if (!eq.first.isEmpty()) {
        throw new RuntimeException("Congruence closure does not work with universal quantification.");
      }
      T t = eq.second.type(th.syms, eq.first);
      eq.second.allSubExps0(th.syms, t, pred);
      eq.third.allSubExps0(th.syms, t, pred);
    }
    for (C c : th.syms.keySet()) {
      Pair<List<T>, T> t = th.syms.get(c);
      if (!t.first.isEmpty()) {
        continue;
      }
      (kb.factory.KBApp(c, Collections.emptyList())).allSubExps0(th.syms, t.second, pred);
    }
    for (C c : th.syms.keySet()) {
      Pair<List<T>, T> t = th.syms.get(c);
      if (t.first.size() != 1) {
        continue;
      }
      for (C d : th.syms.keySet()) {
        Pair<List<T>, T> t0 = th.syms.get(d);
        if (t0.first.size() != 0 || !t0.second.equals(t.first.get(0))) {
          continue;
        }
        KBExp<C, V> dd = kb.factory.KBApp(d, Collections.emptyList());
        KBExp<C, V> zz = kb.factory.KBApp(c, Collections.singletonList(dd));
        zz.allSubExps0(th.syms, t.second, pred);

      }
    }
    doCong();

    // p = new CongruenceProverUniform<>(th);
  }
  // CongruenceProverUniform<T, C, V> p;

  private synchronized void doCong2() {
    doCong();
  }

  private synchronized void doCong() {
    ufs = (new THashMap<>(kb.tys.size()));
    for (T t : kb.tys) {
      ufs.put(t, new UnionFind<>(pred.get(t).keySet().size(), pred.get(t).keySet()));
    }
    for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : kb.eqs) {
      merge1(eq.second, eq.third);
    }
  }

  // synchronized bc eq can trigger redo
  @Override
  public synchronized boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
    if (!ctx.isEmpty()) {
      throw new RuntimeException("Congruence prover can only be used with ground equations.");
    }
    T t = lhs.type(this.kb.syms, ctx);
    boolean changed = lhs.allSubExps0(kb.syms, t, pred) | rhs.allSubExps0(kb.syms, t, pred);
    if (changed) {
      // System.out.println("triggered on " + lhs + " = " + rhs);
      doCong2();
    }
    boolean b = ufs.get(t).connected(lhs, rhs);
    // boolean c = p.eq(ctx, lhs, rhs);
    // if (b != c) {
    // System.out.println(lhs + " = " + rhs);
    // System.out.println(b + " and " + c);
    // System.out.println(p.nf(lhs) + " eq " + p.nf(rhs));
    // System.out.println(this.kb.toString());

    // Util.anomaly();
    // }
    return b;
  }

  @Override
  public synchronized void add(C c, T t) {
    this.kb.syms.put(c, new Pair<>(Collections.emptyList(), t));
  }

  @Override
  public boolean supportsTrivialityCheck() {
    return false;
  }

}
