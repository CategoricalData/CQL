package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.cql.DP;
import catdata.cql.Term;
import catdata.cql.TypeSide;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Sym;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link LPOUKB}, the completion behind {@code prover = completion}, on a typeside
 * where plus is commutative and associative over the constants a and b. Commutativity cannot be
 * oriented, so the system is only ground complete.
 */
class LPOUKBTest {

  private TypeSide<String, Sym> typeside;

  @SuppressWarnings("unchecked")
  private DP<String, Void, Sym, Void, Void, Void, Void> dp(String options) throws Exception {
    AqlMultiDriver d =
        new AqlMultiDriver(
            AqlParserFactory.getParser()
                .parseProgram(
                    """
                    typeside Ty = literal { types T constants a b : T
                      functions plus : T, T -> T
                      equations forall x y. plus(x, y) = plus(y, x)
                        forall x y z. plus(plus(x, y), z) = plus(x, plus(y, z))
                      options prover = completion completion_precedence = "a b plus" %s }
                    """
                        .formatted(options)),
            null);
    d.start();
    assertNull(d.env.exn);
    typeside = (TypeSide<String, Sym>) d.env.defs.tys.get("Ty");
    return typeside.semantics();
  }

  private Term<String, Void, Sym, Void, Void, Void, Void> c(String name) {
    return Term.Sym(sym(name), List.of());
  }

  private Term<String, Void, Sym, Void, Void, Void, Void> plus(
      Term<String, Void, Sym, Void, Void, Void, Void> x,
      Term<String, Void, Sym, Void, Void, Void, Void> y) {
    return Term.Sym(sym("plus"), List.of(x, y));
  }

  private Sym sym(String name) {
    return typeside.syms.keySet().stream()
        .filter(s -> s.toString().equals(name))
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "completion_syntactic_ac = true", "completion_sort = false"})
  void decidesGroundEquationsWithAnUnorientableAxiom(String options) throws Exception {
    var dp = dp(options);
    assertTrue(dp.eq(Map.of(), plus(c("a"), c("b")), plus(c("b"), c("a"))));
    assertTrue(
        dp.eq(Map.of(), plus(plus(c("a"), c("b")), c("a")), plus(c("a"), plus(c("a"), c("b")))));
    assertFalse(dp.eq(Map.of(), c("a"), c("b")));
  }

  @Test
  void cannotDecideAnEquationWithVariables() throws Exception {
    var dp = dp("");
    Term<String, Void, Sym, Void, Void, Void, Void> x = Term.Var("x");
    Term<String, Void, Sym, Void, Void, Void, Void> y = Term.Var("y");
    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () ->
                dp.eq(Map.of("x", Chc.inLeft("T"), "y", Chc.inLeft("T")), plus(x, y), plus(y, x)));
    assertEquals("System not complete, cannot decide non-ground equality", e.getMessage());
  }

  /**
   * Without unfailing completion, the system stops with commutativity unoriented, as when it is
   * unfailing, but reduction then ignores the unoriented equations, so their ground instances fail.
   */
  @Test
  void answersFalseForAnInstanceOfAnUnorientedAxiomWhenNotUnfailing() throws Exception {
    var dp = dp("completion_unfailing = false");
    assertFalse(dp.eq(Map.of(), plus(c("a"), c("b")), plus(c("b"), c("a"))));
  }

  /**
   * Typesides where plus is only commutative, so the completion prover adds nothing for
   * associativity unless it takes plus for an AC symbol.
   */
  @Nested
  class Subsumption {

    @SuppressWarnings("unchecked")
    private DP<String, Void, Sym, Void, Void, Void, Void> dp(String equations, String options)
        throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(
              AqlParserFactory.getParser()
                  .parseProgram(
                      """
                      typeside Ty = literal { types T constants a b c : T
                        functions plus : T, T -> T
                        equations forall x y. plus(x, y) = plus(y, x) %s
                        options prover = completion completion_precedence = "a b c plus" %s }
                      """
                          .formatted(equations, options)),
              null);
      d.start();
      assertNull(d.env.exn);
      typeside = (TypeSide<String, Sym>) d.env.defs.tys.get("Ty");
      return typeside.semantics();
    }

    /**
     * Commutativity alone does not give associativity, since {@code x * y = -x - y} on the integers
     * is commutative and not associative, but the prover takes plus for an AC symbol and proves it.
     */
    @Test
    void provesAssociativityFromCommutativityAlone() throws Exception {
      var dp = dp("", "");
      assertTrue(
          dp.eq(Map.of(), plus(plus(c("a"), c("b")), c("c")), plus(c("a"), plus(c("b"), c("c")))));
      assertTrue(
          typeside.semantics().toStringProver().contains(" -> "),
          "commutativity cannot be oriented, so the only rule is the associativity it added");
    }

    /** With {@code plus(x, a) = x}, plus(a, b) = plus(b, a) is an instance of an axiom. */
    @ParameterizedTest
    @ValueSource(strings = {"", "completion_syntactic_ac = true"})
    void cannotProveAnInstanceOfCommutativityWithARightUnit(String options) throws Exception {
      var dp = dp("forall x. plus(x, a) = x", options);
      assertFalse(dp.eq(Map.of(), plus(c("a"), c("b")), plus(c("b"), c("a"))));
      assertTrue(dp.eq(Map.of(), plus(c("b"), c("a")), c("b")));
    }
  }
}
