package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Triple;
import catdata.Unit;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.exp.TyExp.TyExpVisitor;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TyExpAdt}, an unfinished typeside of algebraic data types that no grammar
 * rule builds: it evaluates to null, and its helper {@code Cat} writes a CQL program whose instance
 * enumerates the values of each sum and product type.
 */
class TyExpAdtTest {

  private static final TyExpAdt EXP = new TyExpAdt(Map.of(), List.of(), List.of(), List.of());

  /** The program Cat writes for the given base types, constants and type formers. */
  private static String cat(
      Set<String> base,
      Set<Pair<String, String>> consts,
      List<Triple<String, String, Boolean>> types)
      throws Exception {
    Class<?> c = Class.forName("catdata.cql.exp.TyExpAdt$Cat");
    Constructor<?> k = c.getDeclaredConstructor(Set.class, Set.class, List.class);
    k.setAccessible(true);
    return k.newInstance(base, consts, types).toString();
  }

  /** The values the program's instance G gives each type, as value : type. */
  private static Set<String> values(String program) throws Exception {
    Instance<String, String, Sym, Fk, Att, ?, ?, ?, ?> g =
        ExpTestHelpers.eval(program, "G", Kind.INSTANCE);
    Set<String> ret = new TreeSet<>();
    g.gens().forEach((gen, en) -> ret.add(gen + " : " + en));
    return ret;
  }

  @Nested
  class Cat {

    @Test
    void enumeratesAProduct() throws Exception {
      String program = cat(Set.of(), Set.of(), List.of(new Triple<>("1", "1", true)));
      assertTrue(program.startsWith("schema S = literal : empty {\n"), program);
      Set<String> v = values(program);
      assertTrue(v.containsAll(Set.of("tt : V", "(tt, tt) : V", "(1 * 1) : T")), v.toString());
    }

    @Test
    void enumeratesASumOfAConstantAndTheUnit() throws Exception {
      String program =
          cat(
              Set.of("Color"),
              Set.of(new Pair<>("red", "Color")),
              List.of(new Triple<>("Color", "1", false)));
      Set<String> v = values(program);
      assertTrue(
          v.containsAll(Set.of("red : V", "inl_1 red : V", "inr_Color tt : V", "(Color + 1) : T")),
          v.toString());
    }

    @Test
    void mainPrintsTheProgramOfTheProductOfTheUnit() throws Exception {
      PrintStream out = System.out;
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
      try {
        TyExpAdt.main(new String[0]);
      } finally {
        System.setOut(out);
      }
      assertEquals(
          cat(Set.of(), Set.of(), List.of(new Triple<>("1", "1", true))) + System.lineSeparator(),
          buf.toString(StandardCharsets.UTF_8));
    }
  }

  @Nested
  class Expression {

    @Test
    void evaluatesToNothing() throws Exception {
      assertNull(EXP.eval0(new AqlEnv(ExpTestHelpers.parse("")), false));
      assertNull(EXP.toString());
      assertEquals(Unit.unit, EXP.type(new AqlTyping()));
    }

    @Test
    void hasNoPartsOrDependencies() {
      assertTrue(EXP.deps().isEmpty());
      assertTrue(EXP.imports().isEmpty());
      assertTrue(EXP.raw().isEmpty());
      assertEquals(Map.of(), EXP.options());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, EXP.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      EXP.map(seen::add);
      assertEquals(List.of(EXP), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(EXP, new TyExpAdt(Map.of(), List.of(), List.of(), List.of()));
      assertEquals(
          EXP.hashCode(), new TyExpAdt(Map.of(), List.of(), List.of(), List.of()).hashCode());
      assertNotEquals(EXP, new TyExpAdt(Map.of("timeout", "1"), List.of(), List.of(), List.of()));
      assertNotEquals(EXP, new TyExpAdt(Map.of(), List.of("A"), List.of(), List.of()));
      assertNotEquals(
          EXP, new TyExpAdt(Map.of(), List.of(), List.of(new Pair<>("a", "A")), List.of()));
      assertNotEquals(
          EXP, new TyExpAdt(Map.of(), List.of(), List.of(), List.of(new Triple<>("A", "B", true))));
      TyExpAdt none = new TyExpAdt(null, null, null, null);
      assertEquals(none, new TyExpAdt(null, null, null, null));
      assertEquals(none.hashCode(), new TyExpAdt(null, null, null, null).hashCode());
      assertNotEquals(none, EXP);
      assertNotEquals(EXP, none);
      assertNotEquals(new TyExpAdt(null, List.of(), null, null), EXP);
      assertNotEquals(new TyExpAdt(Map.of(), List.of(), null, null), EXP);
      assertNotEquals(new TyExpAdt(Map.of(), List.of(), null, List.of()), EXP);
      assertNotEquals(EXP, null);
      assertNotEquals(EXP, "adt");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitorAndTheTypingInversion() {
      TyExpVisitor<String, Unit, RuntimeException> v = mock(TyExpVisitor.class);
      when(v.visit(Unit.unit, EXP)).thenReturn("visited");
      assertEquals("visited", EXP.accept(Unit.unit, v));
      assertEquals(EXP, EXP.coaccept(Unit.unit, new TypingInversion(), new AqlTyping()));
    }
  }
}
