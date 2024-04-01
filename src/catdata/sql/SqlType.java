package catdata.sql;

import java.lang.reflect.Field;
import java.sql.Types;

public class SqlType {

  public String name;

  private SqlType() {
  }

  public static SqlType resolve(String t) {
    Class<Types> c = Types.class;
    try {
      Field[] fields = c.getFields();
      for (Field field : fields) {
        Object o = field.get(null);
        if (t.equals(o.toString())) {
          return new SqlType(field.getName());
        }
      }
    } catch (IllegalAccessException | IllegalArgumentException | SecurityException ex) {
      ex.printStackTrace();
    }
    System.out.println("Warning: couldn't find type " + t);
    return new SqlType("VARCHAR");
//    throw new RuntimeException("Couldn't find type " + t);
  }

  private SqlType(String name) {
    if (name == null) {
      throw new RuntimeException();
    }
    this.name = name;
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
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SqlType other = (SqlType) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return name;
  }

}
