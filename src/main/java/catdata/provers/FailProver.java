package catdata.provers;

import java.util.Map;

public class FailProver<T, C, V> extends DPKB<T, C, V> {

  public FailProver(KBTheory<T, C, V> kb) {
    super(kb);
  }

  @Override
  public boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
    throw new RuntimeException();
  }

  @Override
  public String toString() {
    return "Fail prover";
  }

  @Override
  public void add(C c, T t) {
    throw new RuntimeException("Fail");
  }

  @Override
  public boolean supportsTrivialityCheck() {
    return false;
  }

}
