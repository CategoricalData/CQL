package catdata.aql;

import java.io.Serializable;
import java.util.Iterator;

import catdata.aql.It.ID;

@SuppressWarnings("serial")
public class It implements Iterator<ID>, Serializable {

  private int next = 0;

  @Override
  public boolean hasNext() {
    return true;
  }

  @Override
  public synchronized ID next() {
    return new ID("id" + next++);
  }

  public static class ID implements Serializable {

    public final String str;

    @Override
    public int hashCode() {
      return str.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass()) {
        return false;
      }
      ID other = (ID) obj;
    
      if (str == null) {
        if (other.str != null)
          return false;
      } else if (!str.equals(other.str))
        return false;
      return true;
    }

    private ID(String str) {
      this.str = str;
    }

    @Override
    public String toString() {
      return str;
    }

    // breaks query composition
    /*
     * private It getOuterType() { return It.this; }
     */

  }

}