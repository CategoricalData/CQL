package catdata;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
//import gnu.trove.map.hash.THashMap;

public class LocStr {

  // TODO CQL really, no one should use locstr equality, but right now mapexpraw
  // does so this is here until mapexpraw (and whoever else) is fixed
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((loc == null) ? 0 : loc.hashCode());
    result = prime * result + ((str == null) ? 0 : str.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof LocStr))
      return false;
    LocStr other = (LocStr) obj;
    if (loc == null) {
      if (other.loc != null)
        return false;
    } else if (!loc.equals(other.loc))
      return false;
    if (str == null) {
      if (other.str != null)
        return false;
    } else if (!str.equals(other.str))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return str;
  }

  public final Integer loc;
  public final String str;

  public LocStr(Integer loc, String str) {
    this.loc = loc;
    this.str = str;
  }

  public static Set<String> set1(Collection<LocStr> l) {
    Set<String> ret = new LinkedHashSet<>(l.size());
    for (LocStr x : l) {
      ret.add(x.str);
    }
    return ret;
  }

  public static Map<Pair<String, Object>, Pair<Integer, Function<Pair<String, Object>, String>>> imports(
      String section, Collection<LocStr> l, Function<Pair<String, Object>, String> f) {
    Map<Pair<String, Object>, Pair<Integer, Function<Pair<String, Object>, String>>> ret = new LinkedHashMap<>(l.size());
    for (LocStr s : l) {
      ret.put(new Pair<>(section, s.str), new Pair<>(s.loc, f));
    }
    return ret;
  }

  public static Set<Pair<String, Pair<List<String>, String>>> functions1(
      Collection<Pair<LocStr, Pair<List<String>, String>>> f) {
    Set<Pair<String, Pair<List<String>, String>>> ret = new LinkedHashSet<>(f.size());
    for (Pair<LocStr, Pair<List<String>, String>> x : f) {
      ret.add(new Pair<>(x.first.str, x.second));
    }
    return ret;
  }

  public static <X> Set<Pair<String, X>> set2(Collection<Pair<LocStr, X>> l) {
    return set2y(l, Function.identity()); 
  }

  public static <X> Set<Pair<String, Pair<String, X>>> set2x(Collection<Pair<LocStr, Pair<String, String>>> atts,
      Function<String, X> f) {
    Set<Pair<String, Pair<String, X>>> ret = new LinkedHashSet<>(atts.size());
    
    for (Pair<LocStr, Pair<String, String>> x : atts) {
      ret.add(new Pair<>(x.first.str, new Pair<>(x.second.first, f.apply(x.second.second))));
    }
    
    return ret;
  }

  public static <En, Y> Set<Pair<String, En>> set2y(Collection<Pair<LocStr, Y>> gens, Function<Y, En> f) {
    Set<Pair<String, En>> ret = new LinkedHashSet<>(gens.size());
    for (Pair<LocStr, Y> x : gens) {
       ret.add(new Pair<>(x.first.str, f.apply(x.second)));
    }
    return ret;
  }

  public static Set<Pair<String, Triple<List<String>, String, String>>> functions2(
      Collection<Pair<LocStr, Triple<List<String>, String, String>>> f) {
    Set<Pair<String, Triple<List<String>, String, String>>> ret = new LinkedHashSet<>(f.size());
    for (Pair<LocStr, Triple<List<String>, String, String>> x : f) {
      ret.add(new Pair<>(x.first.str, x.second));      
    }
    return ret;
  }
  
  public static List<InteriorLabel<Object>> imports(String section, List<LocStr> imports) {
    List<InteriorLabel<Object>> ret = new LinkedList<>();
    for (LocStr str : imports) {
      ret.add(new InteriorLabel<>(section, str.str, str.loc, x -> x.toString()));
    }
    return ret;
  }

  
  public static <X> Set<X> proj2(Collection<Pair<Integer, X>> eqs) {
    return eqs.stream().map(x -> x.second).collect(Collectors.toSet());
  }

  public static <X> List<X> proj2l(Collection<Pair<Integer, X>> eqs) {
    return eqs.stream().map(x -> x.second).collect(Collectors.toList());
  }
  public static <X, Y> List<Pair<Pair<Y, String>, X>> list2x(List<Pair<Pair<Y, LocStr>, X>> as) {
    return as.stream().map(x -> new Pair<>(new Pair<>(x.first.first, x.first.second.str), x.second))
        .collect(Collectors.toUnmodifiableList());
  }

  public static <X> List<Pair<String, X>> list2(List<Pair<LocStr, X>> as) {
    return as.stream().map(x -> new Pair<>(x.first.str, x.second)).collect(Collectors.toUnmodifiableList());
  }

  public static <X, Y> List<Pair<Y, X>> list2(List<Pair<LocStr, X>> as, Function<String, Y> f) {
    return as.stream().map(x -> new Pair<>(f.apply(x.first.str), x.second))
        .collect(Collectors.toUnmodifiableList());
  }
  

}