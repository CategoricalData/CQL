package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EdsExpAll}, {@code constraints C = all I T}, the constraints over the
 * schema of I that I satisfies, which reformulation uses. The search itself runs in the
 * reformulation tests; these cover the expression.
 */
class EdsExpAllTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String }
      schema S = literal : Ty { entities A B }
      schema T = literal : Ty { entities A }
      instance I = literal : S { generators a : A }
      constraints C = all I T
      """;

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static EdsExpAll<?, ?, ?, ?> exp(String i, String t) {
    return new EdsExpAll(new InstExpVar(i), new SchExpVar(t));
  }

  private final EdsExpAll<?, ?, ?, ?> exp = exp("I", "T");

  /** Each entity is universal, existential or absent, so n entities give 3 to the n choices. */
  @Test
  void choosesARoleForEachEntity() {
    Set<Set<Pair<String, Boolean>>> choices = EdsExpAll.gt(List.of("A", "B"));
    assertEquals(9, choices.size());
    Set<String> printed =
        choices.stream()
            .map(c -> c.stream().map(Pair::toString).sorted().collect(Collectors.joining(" ")))
            .collect(Collectors.toSet());
    assertTrue(printed.contains("(A, true) (B, null)"), printed.toString());
    assertTrue(printed.contains("(A, false) (B, false)"), printed.toString());
    assertEquals(Set.of(Set.of()), EdsExpAll.gt(List.of()));
  }

  @Test
  void isTypedByTheSchemaOfTheInstance() throws Exception {
    assertEquals(new SchExpVar("S"), exp.type(ExpTestHelpers.typing(PROGRAM)));
    AqlEnv env = ExpTestHelpers.run(PROGRAM).env;
    assertThrows(IgnoreException.class, () -> exp.eval0(env, true));
  }

  /** The target schema is neither a dependency nor visited. */
  @Test
  void dependsOnTheInstanceOnly() {
    assertEquals(List.of(new Pair<>("I", Kind.INSTANCE)), new ArrayList<>(exp.deps()));
    assertTrue(exp.options().isEmpty());
    assertFalse(exp.isVar());
    assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
    List<Exp<?>> seen = new ArrayList<>();
    exp.map(seen::add);
    assertEquals(List.of(exp), seen);
  }

  @Test
  void printsTheInstanceAndTheSchema() {
    assertEquals("all I T", exp.toString());
  }

  @Test
  void equalityComparesTheInstanceAndTheSchema() {
    assertEquals(exp, exp);
    assertEquals(exp, exp("I", "T"));
    assertEquals(exp.hashCode(), exp("I", "T").hashCode());
    assertNotEquals(exp, exp("J", "T"));
    assertNotEquals(exp, exp("I", "S"));
    assertNotEquals(exp, null);
    assertNotEquals(exp, "all");
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    EdsExpVisitor<String, Unit, RuntimeException> v = mock(EdsExpVisitor.class);
    when(v.visit(Unit.unit, exp)).thenReturn("visited");
    assertEquals("visited", exp.accept(Unit.unit, v));
  }
}
