package catdata.graph;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import catdata.Util;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

//WeightedQuickUnionPathCompressionUF a la Robert Sedgewick and Kevin Wayne
public class UnionFind<X> {

  @Override
  public String toString() {
    return "UnionFind [parent=" + parent + ", size=" + size + "]";
  }

  public Collection<X> values() {
    return iso2.keySet();
  }

  private int[] parent;
  private int[] size;

  public X[] iso1;
  public final TObjectIntHashMap<X> iso2;
  int top;

  @SuppressWarnings("unchecked")
  public UnionFind(int n, Iterable<X> xs) {
    if (n == 0) {
      n = 1;
    }
    parent = new int[n];
    size = new int[n];
    iso1 = (X[]) new Object[n];
    iso2 = new TObjectIntHashMap<>(n, .75f, -1);

    top = 0;
    for (X x : xs) {
      parent[top] = top;
      size[top] = 1;
      iso1[top] = x;
      iso2.put(x, top);
      top++;
    }
    if (top > n) {
      Util.anomaly();
    }
  }

  public int size() {
    int ret = 0;
    for (int i = 0; i < top; i++) {
      if (size[i] > 0) {
        ret++;
      }
    }
    return ret;
  }

  public int Find(int p) {
    int root = p;
    while (root != parent[root]) {
      root = parent[root];
    }

    while (p != root) {
      int newp = parent[p];
      parent[p] = root;
      p = newp;
    }
    return root;
  }

  public X find(X p) {
    int x = iso2.get(p);
    return iso1[Find(x)];
  }

  public boolean connected(X p, X q) {
    int pp = iso2.get(p);
    int qq = iso2.get(q);
    int a = Find(pp);
    int b = Find(qq);
    return a == b;
  }

  public synchronized void union(X p, X q) {
    union(iso2.get(p), iso2.get(q));
  }

  public synchronized void union(int p, int q) {
    int rootP = Find(p);
    int rootQ = Find(q);
    if (rootP == rootQ) {
      return;
    }

    // make smaller root point to larger one
    if (size[rootP] < size[rootQ]) {
      parent[rootP] = rootQ;
      size[rootQ] = size[rootQ] + size[rootP];
      size[rootP] = 0;
    } else {
      parent[rootQ] = rootP;
      size[rootP] = size[rootP] + size[rootQ];
      size[rootQ] = 0;
    }
  }

  public synchronized Map<X, Set<X>> toMap() {
    Map<X, Set<X>> ret = new THashMap<>(iso2.size());
    for (X x : iso2.keySet()) {
      ret.put(x, eqc(x));
    }
    return ret;
  }

  private synchronized Set<X> eqc(X x) {
    Set<X> ret = new THashSet<>();
    for (X x0 : iso2.keySet()) {
      if (connected(x, x0)) {
        ret.add(x0);
      }
    }
    return ret;
  }

  public int findNoAdd(X x) {
    int i = iso2.get(x);
    if (i == -1) {
      return -1;
    }
    return Find(i);
  }

}
