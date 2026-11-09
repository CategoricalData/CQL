package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Unit;
import catdata.cql.AqlSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The contract every expression class meets, checked on the sample of each {@link AqlSyntax} that
 * {@link TypingInversion} builds for the help pages. The driver decides what to re-run by comparing
 * the expressions of two runs with {@code equals}, so two expressions built the same way must be
 * equal and hash alike.
 */
class ExpContractTest {

  /** The syntaxes whose sample TypingInversion builds as an expression of another class. */
  private static final Map<AqlSyntax, String> SAMPLED_AS =
      Map.of(
          AqlSyntax.ApgTransExpTerminal, "ApgTransExpVar",
          AqlSyntax.ApgTransExpInitial, "ApgTransExpVar",
          AqlSyntax.InstExpMarkdown, "InstExpXmlAll");

  private static Exp<?> sample(AqlSyntax x) {
    return new TypingInversion().covisit(Unit.unit, new AqlTyping(), x);
  }

  @ParameterizedTest
  @EnumSource(AqlSyntax.class)
  void isEqualToACopyBuiltTheSameWay(AqlSyntax x) {
    Exp<?> a = sample(x);
    Exp<?> b = sample(x);
    assertEquals(a, a);
    assertNotEquals(a, null);
    assertFalse(a.equals(new Object()));
    if (x == AqlSyntax.QueryExpFromCoSpan) {
      // equals tests for QueryExpCompose, so two equal expressions are never equal
      assertNotEquals(a, b);
      return;
    }
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @ParameterizedTest
  @EnumSource(AqlSyntax.class)
  void namesItsSyntaxAndKind(AqlSyntax x) {
    Exp<?> a = sample(x);
    assertNotNull(a.kind());
    if (SAMPLED_AS.containsKey(x)) {
      assertEquals(SAMPLED_AS.get(x), a.getClass().getSimpleName());
      return;
    }
    assertEquals(x, a.getSyntax());
    assertNotNull(a.getKeyword());
    assertNotNull(a.toString());
  }

  @ParameterizedTest
  @EnumSource(AqlSyntax.class)
  void reportsItsDependenciesAndOptions(AqlSyntax x) {
    Exp<?> a = sample(x);
    assertNotNull(a.deps());
    assertNotNull(a.options());
    assertTrue(a.allowedOptions().containsAll(ExpTestHelpers.BASE_OPTIONS));
    List<Exp<?>> seen = new ArrayList<>();
    a.map(seen::add);
    assertEquals(a, seen.get(0));
  }
}
