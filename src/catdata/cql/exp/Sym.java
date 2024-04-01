package catdata.cql.exp;

import java.util.List;
import java.util.Map;

import catdata.Pair;
import gnu.trove.map.hash.THashMap;

public class Sym {
  public final String str;
  public final Pair<List<String>, String> ty;

  private static Map<String, Map<Pair<List<String>, String>, Sym>> cache = new THashMap<>(1024 * 54, 1);

  public static synchronized Sym Sym(String str, Pair<List<String>, String> ty) {
    Map<Pair<List<String>, String>, Sym> m = cache.get(str);
    if (m == null) {
      m = new THashMap<>(16, 1);
      cache.put(str, m);
    }
    Sym sym = m.get(ty);
    if (sym != null) {
      return sym;
    }
    sym = new Sym(str, ty);
    m.put(ty, sym);
    return sym;
  }

  private Sym(String str, Pair<List<String>, String> ty) {
    this.str = str;
    this.ty = ty;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this;
  }

  @Override
  public String toString() {
    return str;
  }

}