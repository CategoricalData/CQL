package catdata.sql;

import java.lang.reflect.Field;
import java.sql.Types;

public class SqlType {

  public String name;

  /**
   * The {@link Types} code the driver reported, which is kept even when it is not a standard code
   * and {@link #name} falls back to VARCHAR.
   */
  public final int jdbcType;

  public static SqlType resolve(String t) {
    Class<Types> c = Types.class;
    try {
      Field[] fields = c.getFields();
      for (Field field : fields) {
        Object o = field.get(null);
        if (t.equals(o.toString())) {
          return new SqlType(field.getName(), Integer.parseInt(t));
        }
      }
    } catch (IllegalAccessException | IllegalArgumentException | SecurityException ex) {
      ex.printStackTrace();
    }
    System.out.println("Warning: couldn't find type " + t);
    return new SqlType("VARCHAR", Integer.parseInt(t));
    //    throw new RuntimeException("Couldn't find type " + t);
  }

  private SqlType(String name, int jdbcType) {
    if (name == null) {
      throw new RuntimeException();
    }
    this.name = name;
    this.jdbcType = jdbcType;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SqlType other = (SqlType) obj;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    return true;
  }

  @Override
  public String toString() {
    return name;
  }
}
