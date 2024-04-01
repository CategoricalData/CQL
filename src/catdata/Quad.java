package catdata;

/**
 * @author Ryan Wisnesky
 */
public class Quad<A, B, C, D> {

  public final A first;
  public final B second;
  public final C third;
  public final D fourth;

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((fourth == null) ? 0 : fourth.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    result = prime * result + ((third == null) ? 0 : third.hashCode());
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
    Quad<?, ?, ?, ?> other = (Quad<?, ?, ?, ?>) obj;
    if (first == null) {
      if (other.first != null)
        return false;
    } else if (!first.equals(other.first))
      return false;
    if (fourth == null) {
      if (other.fourth != null)
        return false;
    } else if (!fourth.equals(other.fourth))
      return false;
    if (second == null) {
      if (other.second != null)
        return false;
    } else if (!second.equals(other.second))
      return false;
    if (third == null) {
      if (other.third != null)
        return false;
    } else if (!third.equals(other.third))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "(" + first + ", " + second + ", " + third + ", " + fourth + ")";
  }

  public Triple<A, B, C> first3() {
    return new Triple<>(first, second, third);
  }

  public Quad(A first, B second, C third, D fourth) {
    this.first = first;
    this.second = second;
    this.third = third;
    this.fourth = fourth;
  }

  public Triple<B, C, D> last3() {
    return new Triple<>(second, third, fourth);

  }

}
