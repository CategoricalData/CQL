package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.LineException;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.TransExp.TransExpVar;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InstExpCoEq}, the coequalizer of two transforms {@code I -> J}. The
 * transforms h and k send the row a and the labelled null s of I to different rows and nulls of J,
 * so the coequalizer identifies b with c and t with u.
 */
class InstExpCoEqTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x : String }
      schema S = literal : Ty { entities E attributes v : E -> String }
      instance I = literal : S { generators a : E s : String equations a.v = s }
      instance J = literal : S { generators b c : E t u : String equations b.v = t c.v = u }
      instance K = literal : S { generators d : E }
      transform h = literal : I -> J { generators a -> b s -> t }
      transform k = literal : I -> J { generators a -> c s -> u }
      transform m = literal : K -> J { generators d -> b }
      transform n = literal : I -> K { generators a -> d s -> d.v }
      """;

  @Test
  void identifiesWhatTheTwoTransformsSendTheSourceTo() throws Exception {
    Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> q =
        ExpTestHelpers.eval(PROGRAM + "instance Q = coequalize h k\n", "Q", Kind.INSTANCE);
    assertEquals(1, q.algebra().size("E"));
    assertEquals(1, q.algebra().talg().sks.size(), q.algebra().talg().sks.toString());
  }

  @Test
  void refusesTransformsWithDifferentEnds() throws Exception {
    LineException e =
        assertThrows(
            LineException.class,
            () -> ExpTestHelpers.typing(PROGRAM + "instance Q = coequalize h m\n"));
    assertTrue(e.getMessage().startsWith("Domains do not match: I and"), e.getMessage());
    e =
        assertThrows(
            LineException.class,
            () -> ExpTestHelpers.typing(PROGRAM + "instance Q = coequalize h n\n"));
    assertTrue(e.getMessage().startsWith("CoDomains do not match: J and"), e.getMessage());
  }

  @Test
  void comparesItsTransformsAndOptions() {
    var a = new InstExpCoEq<>(new TransExpVar("h"), new TransExpVar("k"), List.of());
    var b = new InstExpCoEq<>(new TransExpVar("h"), new TransExpVar("k"), List.of());
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, new InstExpCoEq<>(new TransExpVar("k"), new TransExpVar("k"), List.of()));
    assertNotEquals(a, new InstExpCoEq<>(new TransExpVar("h"), new TransExpVar("h"), List.of()));
    assertNotEquals(
        a,
        new InstExpCoEq<>(
            new TransExpVar("h"),
            new TransExpVar("k"),
            List.of(new Pair<>("require_consistency", "false"))));
    assertNotEquals(a, null);
    assertNotEquals(a, "coequalize h k");
    assertTrue(a.direct(null).isEmpty());
  }

  @Test
  @SuppressWarnings("unchecked")
  void acceptsAVisitor() {
    var a = new InstExpCoEq<>(new TransExpVar("h"), new TransExpVar("k"), List.of());
    InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
    when(v.visit(Unit.unit, a)).thenReturn("visited");
    assertEquals("visited", a.accept(Unit.unit, v));
  }
}
