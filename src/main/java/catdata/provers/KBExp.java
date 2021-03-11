package catdata.provers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import gnu.trove.set.hash.THashSet;

/**
 * 
 * @author Ryan Wisnesky
 *
 *         First-order terms with constants/functions, and variables.
 *
 * @param <C> type of constant/function symbols
 * @param <V> type of variables
 */
public interface KBExp<C, V> {

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  public abstract Set<V> getVars();

  abstract boolean isVar();

  abstract V getVar();

  abstract C f();

  abstract List<KBExp<C, V>> getArgs();

  public KBExp<C, V> substitute(Map<V, KBExp<C, V>> sigma);

  public KBExp<C, V> replace(List<Integer> l, KBExp<C, V> r);

  /////////////////////////////////////////////////////////////////////////////////

  public default int size() {
    if (isVar()) {
      return 0;
    }
    int i = 1;
    for (KBExp<C, V> x : getArgs()) {
      i += x.size();
    }
    return i + 1;
  }

  public default boolean occurs(V v) {
    if (isVar()) {
      return getVar().equals(v);
    }
    for (KBExp<C, V> arg : getArgs()) {
      if (arg.occurs(v)) {
        return true;
      }
    }
    return false;
  }

  public default Collection<C> symbols() {
    Map<C, Integer> mm = Util.mk();
    symbols(mm);
    return mm.keySet();
  }

  public default void symbols(Map<C, Integer> symbols) {
    if (isVar()) {
      return;
    }
    for (KBExp<C, V> e : getArgs()) {
      e.symbols(symbols);
    }
    Integer i = symbols.get(f());
    if (i == null) {
      symbols.put(f(), getArgs().size());
    } else {
      if (getArgs().size() != i) {
        throw new RuntimeException("Symbol " + f() + " used with arity " + i + " and also " + getArgs().size());
      }
    }
  }

  public default Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, KBExp<C, V>>>> cp(List<Integer> p, KBExp<C, V> a,
      KBExp<C, V> b, KBExp<C, V> g, KBExp<C, V> d) {
//    try {
    if (isVar()) {
      return Collections.EMPTY_SET;
    }

    Set<Triple<KBExp<C, V>, KBExp<C, V>, Map<V, KBExp<C, V>>>> ret = new THashSet<>();

    int q = 0;
    for (KBExp<C, V> arg : getArgs()) {
      List<Integer> p0 = new ArrayList<>(p.size() + 1);
      p0.addAll(p);
      p0.add(q++);
      ret.addAll(arg.cp(p0, a, b, g, d));
    }
    Map<V, KBExp<C, V>> s;
    s = KBUnifier.unify0(this, a);
    if (s != null) {
      Triple<KBExp<C, V>, KBExp<C, V>, Map<V, KBExp<C, V>>> toadd = new Triple<>(d.substitute(s),
          g.replace(p, b).substitute(s), s);
      ret.add(toadd);

    }
    return ret;
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // throw new RuntimeException("Interrupted " + e.getMessage());
    // }

  }

  public default <S> S type(Map<C, Pair<List<S>, S>> ctx, Map<V, S> cur) {
    if (isVar()) {
      return cur.get(getVar());
    }
    Pair<List<S>, S> p = ctx.get(f());
    if (p == null) {
      throw new RuntimeException("Missing symbol " + f() + " in ctx " + ctx);
    }
    List<S> sorts = p.first;
    int i = 0;
    for (KBExp<C, V> arg : getArgs()) {
      S s = sorts.get(i);
      S s0 = arg.type(ctx, cur);
      if (!s.equals(s0)) {
        throw new RuntimeException("Argument " + arg + " " + arg.getClass() + " expected at sort " + s
            + " but actually at sort " + s0 + ".\n\n" + cur);
      }
      i++;
    }

    return p.second;
  }

  public default boolean hasAsSubterm(KBExp<C, V> sub) {
    if (equals(sub)) {
      return true;
    }
    for (KBExp<C, V> arg : getArgs()) {
      if (arg.hasAsSubterm(sub)) {
        return true;
      }
    }
    return false;
  }

  public default void vars(Collection<V> vars) {
    if (isVar()) {
      vars.add(getVar());
      return;
    }
    List<KBExp<C, V>> s = getArgs();
    if (s == null) {
      return;
    }
    for (KBExp<C, V> e : s) {
      e.vars(vars);
    }
  }

  public default <T> boolean allSubExps0(Map<C, Pair<List<T>, T>> fn, T o,
      Map<T, Map<KBExp<C, V>, Set<KBExp<C, V>>>> pred) {
    synchronized (pred) {

      if (isVar()) {
        if (!pred.get(o).containsKey(this)) {
          pred.get(o).put(this, (new THashSet<>()));
          return true;
        }
        return false;
      }

      boolean ret = false;
      if (!pred.containsKey(o)) {
        throw new RuntimeException("Anomaly: " + o + " not in " + Util.sep(pred, " -> ", "\n"));
      }
      if (!pred.get(o).containsKey(this)) {
        pred.get(o).put(this, (new THashSet<>()));
        ret = true;
      }
      List<T> z = fn.get(this.f()).first;
      Iterator<T> it = z.iterator();
      for (KBExp<C, V> arg : getArgs()) {
        T x = it.next();
        ret = arg.allSubExps0(fn, x, pred) | ret;
        ret = pred.get(x).get(arg).add(this) | ret;
      }
      return ret;
    }
  }

}
