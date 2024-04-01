package catdata.graph;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

import catdata.Util;
import gnu.trove.map.hash.THashMap;

/* Dijkstra's algorith a la Sedgewick and Wayne
 */
class ShortestPath<N, E> {

  private Collection<DirectedEdge<N, E>> edgesFrom(N n) {
    Collection<DirectedEdge<N, E>> ret = new LinkedList<>();
    for (DirectedEdge<N, E> e : edges) {
      if (e.from.equals(n)) {
        ret.add(e);
      }
    }
    return ret;
  }

  private static class DirectedEdge<N, E> {
    public final E e;
    public final N from;
    public final N to;
    public final double weight;

    private DirectedEdge(N v, N w, E e, double weight) {
      if (Double.isNaN(weight)) {
        throw new IllegalArgumentException("Weight is NaN");
      }
      if (weight <= 0) {
        throw new IllegalArgumentException("Weight is <= 0");
      }
      from = v;
      to = w;
      this.weight = weight;
      this.e = e;
    }

    @Override
    public String toString() {
      return e + ": " + from + "->" + to + " " + String.format("%5.2f", weight);
    }

    @Override
    public int hashCode() {
      int prime = 31;
      int result = 1;
      result = prime * result + ((e == null) ? 0 : e.hashCode());
      result = prime * result + ((from == null) ? 0 : from.hashCode());
      result = prime * result + ((to == null) ? 0 : to.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      DirectedEdge<?, ?> other = (DirectedEdge<?, ?>) obj;
      if (e == null) {
        if (other.e != null)
          return false;
      } else if (!e.equals(other.e))
        return false;
      if (from == null) {
        if (other.from != null)
          return false;
      } else if (!from.equals(other.from))
        return false;
      if (to == null) {
        if (other.to != null)
          return false;
      } else if (!to.equals(other.to))
        return false;
      return true;
    }

  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final Map<N, Double> distTo; // distTo[v] = distance of shortest s->v path
  private final Map<N, DirectedEdge<N, E>> edgeTo; // edgeTo[v] = last edge on
  // shortest s->v path
  private final Wrapper<N, Double> pq; // priority queue of vertices

  private final Collection<DirectedEdge<N, E>> edges;

  public ShortestPath(DMG<N, E> G, N s) {
    edges = new LinkedList<>();
    for (E e : G.edges.keySet()) {
      edges.add(new DirectedEdge<>(G.edges.get(e).first, G.edges.get(e).second, e, 1));
    }
    distTo = new THashMap<>();
    edgeTo = new THashMap<>();
    for (N n : G.nodes) {
      distTo.put(n, Double.POSITIVE_INFINITY);
      // edgeTo.put(n, null);
    }

    distTo.put(s, 0.0);

    // relax vertices in order of distance from s
    pq = new Wrapper<>(G.nodes);
    pq.insert(s, distTo.get(s));
    while (!pq.isEmpty()) {
      N v = pq.delMin();
      for (DirectedEdge<N, E> ed : edgesFrom(v)) {
        relax(ed);
      }
    }
    check(G, s);
  }

  // relax edge e and update pq if changed
  private void relax(DirectedEdge<N, E> e) {
    N v = e.from, w = e.to;
    if (distTo.get(w) > distTo.get(v) + e.weight) {
      distTo.put(w, distTo.get(v) + e.weight);
      edgeTo.put(w, e);
      if (pq.contains(w)) {
        pq.decreaseKey(w, distTo.get(w));
      } else {
        pq.insert(w, distTo.get(w));
      }
    }
  }

  public double distTo(N v) {
    return distTo.get(v);
  }

  public boolean hasPathTo(N v) {
    return distTo.get(v) < Double.POSITIVE_INFINITY;
  }

  public List<E> pathTo(N v) {
    if (!hasPathTo(v)) {
      return null;
    }
    Stack<E> path = new Stack<>();
    for (DirectedEdge<N, E> e = edgeTo.get(v); e != null; e = edgeTo.get(e.from)) {
      path.push(e.e);
    }
    Iterator<E> it = path.iterator();
    List<E> ret = new LinkedList<>();
    while (it.hasNext()) {
      ret.add(it.next());
    }
    return Util.reverse(ret); // TODO aql apparently this is backwards
  }

  private void check(DMG<N, E> G, N s) {
    if (distTo.get(s) != 0.0 || edgeTo.get(s) != null) {
      throw new RuntimeException("distTo[s] and edgeTo[s] inconsistent");
    }
    for (N v : G.nodes) {
      if (v.equals(s)) {

      } else if (edgeTo.get(v) == null && distTo.get(v) != Double.POSITIVE_INFINITY) {
        throw new RuntimeException("distTo[] and edgeTo[] inconsistent");
      }
    }

    for (N v : G.nodes) {
      for (DirectedEdge<N, E> e : edgesFrom(v)) {
        N w = e.to;
        if (distTo.get(v) + e.weight < distTo.get(w)) {
          throw new RuntimeException("edge " + e + " not relaxed");
        }
      }
    }

    for (N w : G.nodes) {
      if (edgeTo.get(w) == null) {
        continue;
      }
      DirectedEdge<N, E> e = edgeTo.get(w);
      N v = e.from;
      if (!w.equals(e.to)) {
        throw new RuntimeException("Anomaly in Shortest Path, please report");
      }
      if (distTo.get(v) + e.weight != distTo.get(w)) {
        throw new RuntimeException("edge " + e + " on shortest path not tight");
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////

  private static class Wrapper<N, Key extends Comparable<Key>> implements Iterable<N> {
    private final IndexMinPQ<Key> x;

    final Map<N, Integer> iso1 = new THashMap<>();
    final Map<Integer, N> iso2 = new THashMap<>();

    private Wrapper(Collection<N> nodes) {
      int i = 0;
      for (N n : nodes) {
        iso1.put(n, i);
        iso2.put(i, n);
        i++;
      }
      x = new IndexMinPQ<>(nodes.size());
    }

    public void decreaseKey(N w, Key d) {
      x.decreaseKey(iso1.get(w), d);
    }

    public boolean contains(N w) {
      return x.contains(iso1.get(w));
    }

    public N delMin() {
      return iso2.get(x.delMin());
    }

    public boolean isEmpty() {
      return x.isEmpty();
    }

    public void insert(N n, Key d) {
      x.insert(iso1.get(n), d);
    }

    @Override
    public Iterator<N> iterator() {
      Iterator<Integer> it = x.iterator();
      return new Iterator<>() {
        @Override
        public boolean hasNext() {
          return it.hasNext();
        }

        @Override
        public N next() {
          return iso2.get(it.next());
        }
      };
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////

  private static class IndexMinPQ<Key extends Comparable<Key>> implements Iterable<Integer> {
    private final int maxN; // maximum number of elements on PQ
    private int n; // number of elements on PQ
    private final int[] pq; // binary heap using 1-based indexing
    private final int[] qp; // inverse of pq - qp[pq[i]] = pq[qp[i]] = i
    private final Key[] keys; // keys[i] = priority of i

    @SuppressWarnings({ "unchecked" })
    private IndexMinPQ(int maxN) {
      if (maxN < 0)
        throw new IllegalArgumentException();
      this.maxN = maxN;
      n = 0;
      keys = (Key[]) new Comparable[maxN + 1]; // make this of length maxN??
      pq = new int[maxN + 1];
      qp = new int[maxN + 1]; // make this of length maxN??
      for (int i = 0; i <= maxN; i++)
        qp[i] = -1;
    }

    public boolean isEmpty() {
      return n == 0;
    }

    public boolean contains(int i) {
      if (i < 0 || i >= maxN)
        throw new IndexOutOfBoundsException();
      return qp[i] != -1;
    }

    // public int size() {
    // return n;
    // }

    public void insert(int i, Key key) {
      if (i < 0 || i >= maxN)
        throw new IndexOutOfBoundsException();
      if (contains(i))
        throw new IllegalArgumentException("index is already in the priority queue");
      n++;
      qp[i] = n;
      pq[n] = i;
      keys[i] = key;
      swim(n);
    }

    /*
     * public int minIndex() { if (n == 0) throw new
     * NoSuchElementException("Priority queue underflow"); return pq[1]; }
     * 
     * public Key minKey() { if (n == 0) throw new
     * NoSuchElementException("Priority queue underflow"); return keys[pq[1]]; }
     */

    public int delMin() {
      if (n == 0)
        throw new NoSuchElementException("Priority queue underflow");
      int min = pq[1];
      exch(1, n--);
      sink(1);
      assert min == pq[n + 1];
      qp[min] = -1; // delete
      keys[min] = null; // to help with garbage collection
      pq[n + 1] = -1; // not needed
      return min;
    }
    /*
     * public Key keyOf(int i) { if (i < 0 || i >= maxN) throw new
     * IndexOutOfBoundsException(); if (!contains(i)) throw new
     * NoSuchElementException("index is not in the priority queue"); else return
     * keys[i]; }
     */

    /*
     * public void changeKey(int i, Key key) { if (i < 0 || i >= maxN) throw new
     * IndexOutOfBoundsException(); if (!contains(i)) throw new
     * NoSuchElementException("index is not in the priority queue"); keys[i] = key;
     * swim(qp[i]); sink(qp[i]); }
     */

    /*
     * @Deprecated public void change(int i, Key key) { changeKey(i, key); }
     */

    public void decreaseKey(int i, Key key) {
      if (i < 0 || i >= maxN)
        throw new IndexOutOfBoundsException();
      if (!contains(i))
        throw new NoSuchElementException("index is not in the priority queue");
      if (keys[i].compareTo(key) <= 0)
        throw new IllegalArgumentException(
            "Calling decreaseKey() with given argument would not strictly decrease the key");
      keys[i] = key;
      swim(qp[i]);
    }

    /*
     * public void increaseKey(int i, Key key) { if (i < 0 || i >= maxN) throw new
     * IndexOutOfBoundsException(); if (!contains(i)) throw new
     * NoSuchElementException("index is not in the priority queue"); if
     * (keys[i].compareTo(key) >= 0) throw new
     * IllegalArgumentException("Calling increaseKey() with given argument would not strictly increase the key"
     * ); keys[i] = key; sink(qp[i]); }
     * 
     * public void delete(int i) { if (i < 0 || i >= maxN) throw new
     * IndexOutOfBoundsException(); if (!contains(i)) throw new
     * NoSuchElementException("index is not in the priority queue"); int index =
     * qp[i]; exch(index, n--); swim(index); sink(index); keys[i] = null; qp[i] =
     * -1; }
     */

    private boolean greater(int i, int j) {
      return keys[pq[i]].compareTo(keys[pq[j]]) > 0;
    }

    private void exch(int i, int j) {
      int swap = pq[i];
      pq[i] = pq[j];
      pq[j] = swap;
      qp[pq[i]] = i;
      qp[pq[j]] = j;
    }

    private void swim(int k) {
      while (k > 1 && greater(k / 2, k)) {
        exch(k, k / 2);
        k /= 2;
      }
    }

    private void sink(int k) {
      while (2 * k <= n) {
        int j = 2 * k;
        if (j < n && greater(j, j + 1))
          j++;
        if (!greater(k, j))
          break;
        exch(k, j);
        k = j;
      }
    }

    @Override
    public Iterator<Integer> iterator() {
      return new HeapIterator();
    }

    private class HeapIterator implements Iterator<Integer> {
      // create a new pq
      private final IndexMinPQ<Key> copy;

      // add all elements to copy of heap
      // takes linear time since already in heap order so no keys move
      private HeapIterator() {
        copy = new IndexMinPQ<>(pq.length - 1);
        for (int i = 1; i <= n; i++)
          copy.insert(pq[i], keys[pq[i]]);
      }

      @Override
      public boolean hasNext() {
        return !copy.isEmpty();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Integer next() {
        if (!hasNext())
          throw new NoSuchElementException();
        return copy.delMin();
      }
    }

  }
}
