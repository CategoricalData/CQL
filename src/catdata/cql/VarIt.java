package catdata.cql;

import java.util.Iterator;

public class VarIt implements Iterator<String> {

  private static int index = 0;

  private VarIt() {
  }

  public static Iterator<String> it() {
    return new VarIt();
  }

  private static String fresh() {
    return ("v" + index++);
  }

  @Override
  public boolean hasNext() {
    return true;
  }

  @Override
  public String next() {
    return fresh();
  }

}