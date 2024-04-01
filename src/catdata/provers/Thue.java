package catdata.provers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.collections4.iterators.IteratorIterable;
import org.apache.commons.collections4.list.TreeList;

import catdata.Pair;
import catdata.Quad;
import catdata.Util;
import catdata.cql.ConsList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

/**
 * @author Ryan Wisnesky
 * 
 *         Implementation of a Knuth-Bendix variant for Thue systems (monoid
 *         presentations), following the Kapur and Narendran 1985 paper.
 * 
 *         Note: In this variant of Knuth-Bendix, two terms can be equal without
 *         having the same normal form. Therefore, clients MUST use equiv
 *         instead of normalize followed by equals. is provided for client
 *         convenience.
 * 
 *         Note: The constructor mutates the input words IN PLACE.
 * 
 * @param <Y> the alphabet
 */
public class Thue<X> {

  class Rules {

    private final Map<X, Set<Pair<ConsList<X>, ConsList<X>>>> rs;
    private final Map<X, Set<Pair<ConsList<X>, ConsList<X>>>> es1;
    private final Map<X, Set<Pair<ConsList<X>, ConsList<X>>>> es2;

    private final Map<X, Set<Pair<ConsList<X>, ConsList<X>>>> index;

//    private final Map<ConsList<X>, ConsList<X>> nf_cache = new Map<>();
//    private final Map<ConsList<X>, Set<ConsList<X>>> c_cache = new Map<>();

    public Iterable<Pair<ConsList<X>, ConsList<X>>> rules(X y) {
      IteratorChain<Pair<ConsList<X>, ConsList<X>>> chain = new IteratorChain<>();
      chain.addIterator(rs.get(y).iterator());
      chain.addIterator(es1.get(y).iterator());
      return new IteratorIterable<>(chain, false);
    }

    public Iterable<Pair<ConsList<X>, ConsList<X>>> allRules() {
      IteratorChain<Pair<ConsList<X>, ConsList<X>>> chain = new IteratorChain<>();
      for (Set<Pair<ConsList<X>, ConsList<X>>> x : rs.values()) {
        chain.addIterator(x.iterator());
      }
      for (Set<Pair<ConsList<X>, ConsList<X>>> x : es1.values()) {
        chain.addIterator(x.iterator());
      }
      return new IteratorIterable<>(chain, false);
    }

    public Rules(List<Pair<List<X>, List<X>>> rules, Collection<X> xs) {
      this.rs = new THashMap<>(rules.size());
      this.es1 = new THashMap<>();
      this.es2 = new THashMap<>();
      this.index = new THashMap<>(xs.size());

      for (X x : xs) {
        this.rs.put(x, new THashSet<>());
        this.es1.put(x, new THashSet<>());
        this.es2.put(x, new THashSet<>());
        this.index.put(x, new THashSet<>());
      }
      for (Pair<List<X>, List<X>> x : rules) {
        add(ConsList.new0(x.first, false), ConsList.new0(x.second, false));
      }

    }

    public void add(ConsList<X> x, ConsList<X> y) {
      Pair<ConsList<X>, ConsList<X>> p = orient(x, y);
      add0(p);
    }

    public void add(Pair<ConsList<X>, ConsList<X>> z) {
      Pair<ConsList<X>, ConsList<X>> p = orient(z);
      add0(p);
    }

    private void add0(Pair<ConsList<X>, ConsList<X>> p) {
      if (p.first.isEmpty() && p.second.isEmpty()) {
        return;
      } else if (p.first.size() == p.second.size()) {
        X x = p.first.get(0);
        Set<Pair<ConsList<X>, ConsList<X>>> set = es1.get(x);
        set.add(p);
        x = p.second.get(0);
        set = es2.get(x);
        set.add(p);
      } else {
        X x = p.first.get(0);
        Set<Pair<ConsList<X>, ConsList<X>>> set = rs.get(x);
        set.add(p);
      }
      for (X y : p.first.list) {
        index.get(y).add(p);
      }
      for (X y : p.second.list) {
        index.get(y).add(p);
      }
      // nf_cache.map.clear();
      // c_cache.map.clear();
    }

    public synchronized void simplify() {
      Set<Pair<ConsList<X>, ConsList<X>>> unmarked = new THashSet<>(rs.size() + es1.size() + es2.size());
      for (Pair<ConsList<X>, ConsList<X>> rule : allRules()) {
        unmarked.add(rule);
      }
      Pair<ConsList<X>, ConsList<X>> lr;
      while ((lr = getUnmarked(Collections.emptySet(), unmarked)) != null) {

        if (lr.first.size() == lr.second.size()) {
          if (!es1.get(lr.first.get(0)).remove(lr)) {
            Util.anomaly();
          }
          if (!es2.get(lr.second.get(0)).remove(lr)) {
            Util.anomaly();
          }
        } else {
          if (!rs.get(lr.first.get(0)).remove(lr)) {
            Util.anomaly();
          }
        }
        /* */

        if (!normal_form(lr.first).equals(lr.first)) {
          for (X y : lr.first.list) {
            index.get(y).remove(lr);
          }
          for (X y : lr.second.list) {
            index.get(y).remove(lr);
          }
        } else {
          if (lr.first.size() == lr.second.size()) {
            if (!es1.get(lr.first.get(0)).add(lr)) {
              Util.anomaly();
            }
            if (!es2.get(lr.second.get(0)).add(lr)) {
              Util.anomaly();
            }
          } else {
            if (!rs.get(lr.first.get(0)).add(lr)) {
              Util.anomaly();
            }
          }
        }
        unmarked.remove(lr);
      }
    }

    private synchronized void remove(Pair<ConsList<X>, ConsList<X>> rule) {
      if (rule.first.size() == rule.second.size()) {
        if (!es1.get(rule.first.get(0)).remove(rule)) {
          Util.anomaly();
        }
        if (!es2.get(rule.second.get(0)).remove(rule)) {
          Util.anomaly();
        }
      } else {
        if (!rs.get(rule.first.get(0)).remove(rule)) {
          Util.anomaly();
        }
      }
      for (X y : rule.first.list) {
        index.get(y).remove(rule);
      }
      for (X y : rule.second.list) {
        index.get(y).remove(rule);
      }
      // nf_cache.map.clear();
      // c_cache.map.clear();
    }

    public Pair<ConsList<X>, ConsList<X>> orient(ConsList<X> x, ConsList<X> y) {
      if (x.size() >= y.size()) {
        return new Pair<>(x, y);
      }
      return new Pair<>(y, x);
    }

    public Pair<ConsList<X>, ConsList<X>> orient(Pair<ConsList<X>, ConsList<X>> z) {
      if (z.first.size() >= z.second.size()) {
        return z;
      }
      return new Pair<>(z.second, z.first);
    }

    public synchronized void addAll(Collection<Pair<ConsList<X>, ConsList<X>>> ce) {
      for (Pair<ConsList<X>, ConsList<X>> x : ce) {
        add(x);
      }
    }

    @Override
    public String toString() {
      return ""; // rules.toString();
    }

    public Collection<X> xs() {
      return rs.keySet();
    }

    private boolean close1_h(Set<ConsList<X>> ret, ConsList<X> e, int i, ConsList<X> rule1, ConsList<X> rule2,
        ConsList<X> f0) {
      List<X> test = e.list.subList(i, i + rule1.size());

      if (!test.equals(rule1.list)) {
        return false;
      }

      List<X> ret2 = new TreeList<>(e.list);
      delete(ret2, i, rule1.size());
      add(ret2, i, rule2.list);
      ConsList<X> ll = ConsList.new0(ret2, false);
      ret.add(ll);

      return f0.equals(ll);

    }

    // TODO aql bottleneck
    private synchronized Optional<Set<ConsList<X>>> close1(Set<ConsList<X>> es, ConsList<X> f0) {
      Set<ConsList<X>> ret = new THashSet<>(es.size());

      for (ConsList<X> e : es) {
        int i = 0;
        for (X x : e.list) {
          for (Pair<ConsList<X>, ConsList<X>> rule0 : es1.get(x)) {
            if (rule0.first.size() > e.size() - i) {
              continue;
            }
            boolean b = close1_h(ret, e, i, rule0.first, rule0.second, f0);
            if (b) {
              return Optional.empty();
            }
          }
          for (Pair<ConsList<X>, ConsList<X>> rule0 : es2.get(x)) {
            if (rule0.second.size() > e.size() - i) {
              continue;
            }
            boolean b = close1_h(ret, e, i, rule0.second, rule0.first, f0);
            if (b) {
              return Optional.empty();
            }
          }
          i++;
        }
      }

      return Optional.of(ret);
    }

    private synchronized boolean close(ConsList<X> e, ConsList<X> f0) {

      Set<ConsList<X>> init = (new THashSet<>());
      init.add(e);

      while (true) {
        if (init.contains(f0)) {
          return true;
        }
        Optional<Set<ConsList<X>>> next = close1(init, f0);

        if (next.isEmpty()) {
          return true;
        }

        if (init.containsAll(next.get())) {
          return init.contains(f0);
        }
        init.addAll(next.get());

      }

    }

    private synchronized void addCP1(ConsList<X> li, ConsList<X> ri, ConsList<X> lj, ConsList<X> rj,
        Set<Pair<ConsList<X>, ConsList<X>>> ret) {
      Iterator<Pair<List<X>, List<X>>> uvIt = split(li);
      outer: while (uvIt.hasNext()) {
        Pair<List<X>, List<X>> uv = uvIt.next();
        List<X> u = uv.first;
        List<X> v = uv.second;
        if (v.size() > lj.size()) {
          continue;
        }
        Iterator<X> vIt = v.iterator();
        Iterator<X> ljIt = lj.iterator();
        while (vIt.hasNext()) {
          if (!vIt.next().equals(ljIt.next())) {
            continue outer;
          }
        }

        List<X> w = lj.list.subList(v.size(), lj.size());

        ConsList<X> urj = ConsList.new0(u, rj.list);
        ConsList<X> riw = ConsList.new0(ri.list, w);
        if (!almost_joinable(urj, riw)) {
          Pair<ConsList<X>, ConsList<X>> p = orient(urj, riw);
          ret.add(p);
        }
      }
    }

    private synchronized void addCP2(ConsList<X> li, ConsList<X> ri, ConsList<X> lj, ConsList<X> rj,
        Set<Pair<ConsList<X>, ConsList<X>>> ret) {
      outer: for (int i = 0; i < li.size(); i++) {
        if (lj.size() > li.size() - i) {
          return;
        }
        for (int j = 0; j < lj.size(); j++) {
          if (!li.get(i + j).equals(lj.get(j))) {
            continue outer;
          }
        }

        List<X> u = li.list.subList(0, i);
        List<X> w = li.list.subList(i + lj.size(), li.size());

        ConsList<X> urjw = ConsList.new0(u, rj.list, w);
        // Pair<ConsList<X>, ConsList<X>> xx = new Pair<>(ri, urjw);
        if (!almost_joinable(ri, urjw)) {
          ret.add(orient(ri, urjw));
        }
      }
    }

    private synchronized void normalize() {
      Set<Pair<ConsList<X>, ConsList<X>>> marked = new THashSet<>(rs.size() + es1.size() + es2.size()); // size()
                                                        // * 4
      Set<Pair<ConsList<X>, ConsList<X>>> unmarked = new THashSet<>(rs.size() + es1.size() + es2.size());
      for (Pair<ConsList<X>, ConsList<X>> rule : allRules()) {
        unmarked.add(rule);
      }
      Pair<ConsList<X>, ConsList<X>> lr;
      while ((lr = getUnmarked(marked, unmarked)) != null) {
        remove(lr);
        ConsList<X> l0 = normal_form(lr.first);
        ConsList<X> r0 = normal_form(lr.second);
        if (!almost_joinable(l0, r0)) {
          Pair<ConsList<X>, ConsList<X>> l0r0 = orient(l0, r0);
          add(l0r0);
          marked.add(l0r0);
          unmarked.remove(l0r0);
        }
        marked.add(lr); // TODO aql added, only in paper in some
        unmarked.remove(lr);
      }
    }

    private synchronized Pair<ConsList<X>, ConsList<X>> getUnmarked(
        @SuppressWarnings("unused") Set<Pair<ConsList<X>, ConsList<X>>> marked,
        Iterable<Pair<ConsList<X>, ConsList<X>>> unmarked) {
      for (Pair<ConsList<X>, ConsList<X>> rule : unmarked) {
        // if (!marked.contains(rule)) {
        return rule;
        // }
      }
      return null;
    }

    private synchronized ConsList<X> normal_form(ConsList<X> e) {
      if (e.isEmpty()) {
        return e;
      }
      // ConsList<X> tt = t.nf_cache.map.get(e);
      // if (tt != null) {
      // return tt;
      // }

      Iterator<X> it = e.iterator();
      int j = 0;

      while (it.hasNext()) {
        int i = j;
        j++;
        X x = it.next();

        for (Pair<ConsList<X>, ConsList<X>> rule : rs.get(x)) {
          if (rule.first.size() > e.size() - i) {
            continue;
          }
          ConsList<X> test = e.subList(i, i + rule.first.size(), false);
          if (!test.equals(rule.first)) {
            continue;
          }

          List<X> ret2 = new TreeList<>(e.list);
          delete(ret2, i, rule.first.size());
          add(ret2, i, rule.second.list);
          ConsList<X> ret = ConsList.new0(ret2, false);
          if (!e.equals(ret)) {
            ConsList<X> z = normal_form(ret);
            return z;
          }
          return e;
        }
      }
      // t.nf_cache.put(e, e);
      return e;
    }

    private void delete(List<X> l, int i, int size) {
      for (int j = 0; j < size; j++) {
        l.remove(i);
      }
    }

    private void add(List<X> l, int i, List<X> add) {
      l.addAll(i, add);
    }

    private synchronized boolean almost_joinable(ConsList<X> urj, ConsList<X> riw) {
      ConsList<X> e0 = normal_form(urj);
      ConsList<X> f0 = normal_form(riw);
      if (e0.size() != f0.size()) {
        return false;
      }
      if (e0.equals(f0)) {
        return true;
      }

      return close(e0, f0);

    }

    private synchronized void cp_h(Set<Pair<ConsList<X>, ConsList<X>>> ret, Pair<ConsList<X>, ConsList<X>> rule1,
        Pair<ConsList<X>, ConsList<X>> rule2) {

      List<Quad<ConsList<X>, ConsList<X>, ConsList<X>, ConsList<X>>> todo = new ArrayList<>(5);

      if (rule1.first.size() == rule1.second.size()) {
        todo.add(new Quad<>(rule1.first, rule1.second, rule2.first, rule2.second));
        todo.add(new Quad<>(rule1.second, rule1.first, rule2.first, rule2.second));
      } else if (rule2.first.size() == rule2.second.size()) {
        todo.add(new Quad<>(rule1.first, rule1.second, rule2.first, rule2.second));
        todo.add(new Quad<>(rule1.first, rule1.second, rule2.second, rule2.first));
      } else {
        todo.add(new Quad<>(rule1.first, rule1.second, rule2.first, rule2.second));
        //
      }
      for (Quad<ConsList<X>, ConsList<X>, ConsList<X>, ConsList<X>> rule : todo) {
        addCP1(rule.first, rule.second, rule.third, rule.fourth, ret);
        addCP2(rule.first, rule.second, rule.third, rule.fourth, ret);
      }

    }

    private synchronized Set<Pair<ConsList<X>, ConsList<X>>> cp() {

      Set<Pair<ConsList<X>, ConsList<X>>> ret = new THashSet<>();

      for (Pair<ConsList<X>, ConsList<X>> rule1 : allRules()) {
        for (X x : rule1.first.list) {
          for (Pair<ConsList<X>, ConsList<X>> rule2 : index.get(x)) {
            if (rule1.first.size() == rule1.second.size() && rule2.first.size() == rule2.second.size()) {
              continue;
            }
            cp_h(ret, rule1, rule2);
          }
        }
        for (X x : rule1.second.list) {
          for (Pair<ConsList<X>, ConsList<X>> rule2 : index.get(x)) {
            if (rule1.first.size() == rule1.second.size() && rule2.first.size() == rule2.second.size()) {
              continue;
            }
            cp_h(ret, rule1, rule2);
          }
        }
      }

      return ret;
    }

    public int size() {
      int i = 0;
      for (Set<Pair<ConsList<X>, ConsList<X>>> x : rs.values()) {
        i += x.size();
      }
      for (Set<Pair<ConsList<X>, ConsList<X>>> x : es1.values()) {
        i += x.size();
      }
      for (Set<Pair<ConsList<X>, ConsList<X>>> x : es2.values()) {
        i += x.size();
      }
      return i;

    }

    public synchronized void add(X x) {
      rs.put(x, new THashSet<>());
      es1.put(x, new THashSet<>());
      es2.put(x, new THashSet<>());
      index.put(x, new THashSet<>());
    }
  }

  Rules rules;

  // private boolean finished = false;
  // private final Map<Pair<ConsList<X>, ConsList<X>>, Boolean> equivs =
  // (new HashMap<>());
//  private final int max_iterations;
  // private int iteration = 0;

  /**
   * @param rules          to be completed. DOES NOT copy, and MUTATES IN PLACE
   *                       the pairs inside of rules
   * @param max_iterations to run (-1 for infinity)
   */
  public Thue(List<Pair<List<X>, List<X>>> rules, Collection<X> xs) {
    this.rules = new Rules(rules, xs);
    // slow = new ThueSlow<>(rules);
    // slow.complete();
    complete();

    // Set<Pair<List<X>, List<X>>> a = new HashSet<>();
    // this.rules.allRules().iterator().forEachRemaining(x -> a.add(new
    // Pair<>(x.first.list, x.second.list)));

    // Set<Pair<List<X>, List<X>>> b = new HashSet<>(slow.rules);
    /*
     * if (!a.equals(b)) { System.out.println(Util.sep(a, "\n"));
     * System.out.println("----"); System.out.println(Util.sep(b, "\n"));
     * 
     * Util.anomaly(); }
     */

  }

  // ThueSlow<X, ?> slow;

  public synchronized void complete() {

    while (!step())
      ;
//    finished = true;
  }

  public synchronized boolean equiv0(List<X> a, List<X> b) {
    ConsList<X> bb = ConsList.new0(b, false);
    ConsList<X> aa = ConsList.new0(a, false);
    boolean eq = rules.almost_joinable(aa, bb);
    return eq;
  }

  // Map<T,UnionFind<X>> equivs; for length 1, but no time is spent there
  public synchronized boolean equiv(List<X> a, List<X> b) {
    ConsList<X> bb = ConsList.new0(b, false);
    ConsList<X> aa = ConsList.new0(a, false);
    boolean eq = rules.almost_joinable(aa, bb);
    return eq;
  }

  // ///////////////////////////////////////////////////////////////////////////////////////////

  int i = 0;

  private synchronized boolean step() {
    // System.out.println("\n\n" + (i++) + "-----------------------------" +
    // rules.size());
    // System.out.println(slow.toString());
    // System.out.print("+++++");
    // System.out.println(toString());
//    slow.step(slow.rules);
    // if (i > 8) {
    // Util.anomaly();
    // }

//    check_oriented(rules.allRules());
    rules.normalize();
    Collection<Pair<ConsList<X>, ConsList<X>>> ce = rules.cp();
    // System.out.println("+++++");

    // System.out.println(Util.sep(ce, "\n"));

    if (ce.isEmpty()) {
      rules.simplify();
      return true;
    }
    rules.addAll(ce);

    // rules.simplify(); //broken for now
    return false;
  }

  private static <X> Iterator<Pair<List<X>, List<X>>> split(ConsList<X> l) {
    return new Iterator<>() {

      int i = 1;

      @Override
      public boolean hasNext() {
        return i != l.size();
      }

      @Override
      public Pair<List<X>, List<X>> next() {
        Pair<List<X>, List<X>> ret = new Pair<>(l.list.subList(0, i), l.list.subList(i, l.size()));
        i++;
        return ret;
      }

    };

  }

  @Override
  public String toString() {
    String z = "";

    for (X xx : rules.xs()) {
      if (rules.rs.get(xx).isEmpty()) {
        continue;
      }
      List<String> ret = rules.rs.get(xx).stream().map(x -> {
        String s1 = Util.sep(x.first.list, ".");
        String s2 = Util.sep(x.second.list, ".");
        if (x.first.size() == x.second.size()) {
          return Util.anomaly();
        }
        return s1 + " -> " + s2;
      }).collect(Collectors.toList());
      z += Util.sep(ret, "\n") + "\n";
    }

    for (X xx : rules.xs()) {
      if (rules.es1.get(xx).isEmpty()) {
        continue;
      }
      List<String> ret = rules.es1.get(xx).stream().map(x -> {
        String s1 = Util.sep(x.first.list, ".");
        String s2 = Util.sep(x.second.list, ".");
        if (x.first.size() == x.second.size()) {
          return s1 + " = " + s2;
        }
        return Util.anomaly();
      }).collect(Collectors.toList());
      z += Util.sep(ret, "\n") + "\n";
    }

    for (X xx : rules.xs()) {
      if (rules.es2.get(xx).isEmpty()) {
        continue;
      }
      List<String> ret = rules.es2.get(xx).stream().map(x -> {
        String s1 = Util.sep(x.first.list, ".");
        String s2 = Util.sep(x.second.list, ".");
        if (x.first.size() == x.second.size()) {
          return s1 + " = " + s2;
        }
        return Util.anomaly();
      }).collect(Collectors.toList());
      z += Util.sep(ret, "\n") + "\n";
    }

    return z;
  }

}