package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import catdata.Chc;
import org.junit.jupiter.api.Test;

/** The defaults of a decision procedure, and the one for the empty theory. */
class DPTest {

  private static final Term<String, String, String, String, String, String, String> A =
      Term.Sym("a", List.of());

  /** Syntactic equality, with nothing but the two abstract methods. */
  private static final DP<String, String, String, String, String, String, String> SYNTACTIC =
      new DP<>() {
        @Override
        public String toStringProver() {
          return "syntactic";
        }

        @Override
        public boolean eq(
            Map<String, Chc<String, String>> ctx,
            Term<String, String, String, String, String, String, String> lhs,
            Term<String, String, String, String, String, String, String> rhs) {
          return lhs.equals(rhs);
        }
      };

  @Test
  void checksNoTrivialityAndComputesNoNormalFormsByDefault() {
    assertTrue(SYNTACTIC.eq(Map.of(), A, A));
    assertFalse(SYNTACTIC.supportsTrivialityCheck());
    String m = assertThrows(RuntimeException.class, () -> SYNTACTIC.nf(Map.of(), A)).getMessage();
    assertEquals("Anomaly: please report", m);
  }

  @Test
  void theInitialProcedureDecidesNothing() {
    DP<String, String, String, String, String, String, String> dp = DP.initial();
    assertEquals("Theorem prover for empty theory", dp.toStringProver());
    assertFalse(dp.supportsTrivialityCheck());
    assertNull(assertThrows(RuntimeException.class, () -> dp.eq(Map.of(), A, A)).getMessage());
  }
}
