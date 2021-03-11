
package catdata;

public class LocException extends RuntimeException {

  private static final long serialVersionUID = -877116458767124048L;

  public final int loc;

  public LocException(int loc, String msg) {
    super(msg);
    this.loc = loc;
  }

}
