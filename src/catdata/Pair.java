package catdata;

/**
 * @author Ryan Wisnesky
 */

public class Pair<T1, T2> /* Comparable<Pair<T1, T2>>, */ {

  public T1 first; // TODO aql make these final. Same for Triple
  public T2 second;

  public void setFirst(T1 x) {
    first = x;
    // hashCode2();
  }

  public void setSecond(T2 x) {
    second = x;
    // hashCode2();
  }

  public Pair(T1 value, T2 value2) {
    first = value;
    second = value2;
    // Util.assertNotNull(value, value2);
    // hashCode2();
  }
  // private int hashCode = -1;

  @Override
  public String toString() {
    return "(" + first + ", " + second + ")";

  }

  private int code = -1;

  @Override
  public synchronized int hashCode() {
    if (code != -1) {
      return code;
    }
    final int prime = 31;
    int result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    code = result;
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
    Pair<?, ?> other = (Pair<?, ?>) obj;
    
    return ((first  == null && other.first  == null) || first .equals(other.first )) &&
         ((second == null && other.second == null) || second.equals(other.second));
  }

  // public Pair<T2, T1> reverse() {
  // return new Pair<>(second, first);
  // }

}
