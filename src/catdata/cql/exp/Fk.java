package catdata.cql.exp;

import java.util.Map;

import gnu.trove.map.hash.THashMap;

public class Fk {
  public final String str;
  public final String en;

  private Fk(String en, String str) {
    this.str = str;
    this.en = en;
  }

  public synchronized static Fk Fk(String en, String str) {
    Map<String, Fk> m = SchExpRaw.fkCache.get(en);
    if (m == null) {
      m = new THashMap<>(16, 1);
      SchExpRaw.fkCache.put(en, m);
    }
    Fk fk = m.get(str);
    if (fk != null) {
      return fk;
    }
    fk = new Fk(en, str);
    m.put(str, fk);
    return fk;
  }

  @Override
  public String toString() {
    return str;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  public String convert() {
    return str;
  }

}