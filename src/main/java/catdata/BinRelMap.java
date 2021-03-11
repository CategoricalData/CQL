package catdata;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import gnu.trove.map.hash.THashMap;

public class BinRelMap<X, Y> implements Iterable<Pair<X, Y>> {

  @Override
  public int hashCode() {
    return R.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof BinRelMap))
      return false;
    BinRelMap<?, ?> other = (BinRelMap<?, ?>) obj;
    if (R == null) {
      if (other.R != null)
        return false;
    } else if (!R.equals(other.R))
      return false;
    return true;
  }

  // does not allow empty sets
  public class It implements Iterator<Pair<X, Y>> {

    Iterator<X> xIt;
    X x;
    Iterator<Y> yIt;
    Set<Y> ys;

    public It() {
      xIt = R.keySet().iterator();
    }

    @Override
    public void remove() {
      yIt.remove();
      if (ys.isEmpty()) {
        xIt.remove();
      }
    }

    @Override
    public boolean hasNext() {
      if (yIt != null && yIt.hasNext()) {
        return true;
      }
      if (!xIt.hasNext()) {
        return false;
      }
      while (xIt.hasNext()) {
        x = xIt.next();
        yIt = R.get(x).iterator();
        ys = R.get(x);
        if (yIt.hasNext()) {
          return true;
        }
      }

      return false;
    }

    @Override
    public Pair<X, Y> next() {
      Y y = yIt.next();
      return new Pair<>(x, y);
    }

  }

  public Map<X, Set<Y>> toRel(Set<Pair<X, Y>> m) {
    Map<X, Set<Y>> ret = new THashMap<>();
    for (Pair<X, Y> p : m) {
      Set<Y> ys = ret.get(p.first);
      ys.add(p.second);
    }
    return ret;
  }

  private Map<X, Set<Y>> R;

  public BinRelMap(Set<Pair<X, Y>> r) {
    R = toRel(r);
  }

  public BinRelMap(BinRelMap<X, Y> r) {
    R = new THashMap<>();
    for (X x : r.R.keySet()) {
      R.put(x, new TreeSet<>(r.R.get(x)));
    }
  }

  public BinRelMap() {
    R = new THashMap<>();
  }

  public synchronized boolean add(X x, Y y) {
    Set<Y> ys = R.get(x);
    if (ys == null) {
      ys = new TreeSet<>();
      R.put(x, ys);
    }
    return ys.add(y);
  }

  public synchronized void add(X x) {
    Set<Y> set = R.putIfAbsent(x, new TreeSet<>());
    if (set != null) {
      Util.anomaly();
    }
  }

  public synchronized void remove(X x, Y y) {
    Set<Y> ys = R.get(x);
    if (ys == null) {
      return;
    }
    ys.remove(y);
    // if (ys.isEmpty()) {
    // R.remove(x);
    // }
  }

  public synchronized void remove(X x) {
    Set<Y> ys = R.get(x);
    ys.clear();
  }

  public synchronized <Z> BinRelMap<X, Z> compose(BinRelMap<Y, Z> rhs) {
    Map<X, Set<Z>> ret = new THashMap<>();
    for (X x : R.keySet()) {
      Set<Z> set = new TreeSet<>();
      for (Y y : R.get(x)) {
        Set<Z> w = rhs.R.get(y);
        if (w != null) {
          set.addAll(w);
        }
      }
      if (!set.isEmpty()) {
        ret.put(x, set);
      }
    }
    BinRelMap<X, Z> b = new BinRelMap<>();
    b.R = ret;
    return b;
  }

  public synchronized void addAll(BinRelMap<X, Y> a) {
    for (X x : a.R.keySet()) {
      Set<Y> ys = R.get(x);
      if (ys == null) {
        ys = new TreeSet<>();
        R.put(x, ys);
      }
      ys.addAll(a.R.get(x));
    }
  }

  public boolean isEmpty() {
    return R.isEmpty();
  }

  @Override
  public Iterator<Pair<X, Y>> iterator() {
    return new It();
  }

  @Override
  public String toString() {
    return "BinRelMap [R=" + R + "]";
  }

  public synchronized Collection<X> keySet() {
    return R.keySet();
  }

  public synchronized Collection<Y> get(X x) {
    return R.get(x);
  }

  public synchronized boolean containsKey(X x) {
    return R.containsKey(x);
  }

  public synchronized int size() {
    int i = 0;
    for (@SuppressWarnings("unused")
    Pair<X, Y> x : this) {
      i++;
    }
    return i;
  }

}
