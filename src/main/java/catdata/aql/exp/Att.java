package catdata.aql.exp;

import java.util.Map;

import gnu.trove.map.hash.THashMap;

public class Att {
  public final String str;
  public final String en;

  public synchronized static Att Att(String en, String str) {
    Map<String, Att> m = SchExpRaw.attCache.get(en);
    if (m == null) {
      m = new THashMap<>(16, 1);
      SchExpRaw.attCache.put(en, m);
    }
    Att att = m.get(str);
    if (att != null) {
      return att;
    }
    att = new Att(en, str);
    m.put(str, att);
    return att;
  }

  private Att(String en, String str) {
    this.str = str;
    this.en = en;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  @Override
  public String toString() {
    return str;
  }

  public String convert() {
    return str;
  }
}