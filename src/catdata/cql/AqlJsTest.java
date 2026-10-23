package catdata.cql;

import static catdata.cql.CqlTestHelpers.ops;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import catdata.Pair;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** The bridge to the typeside's javascript, over a type Str of java.lang.String. */
class AqlJsTest {

  private static final String STR = "Str";

  /** twice : Str -> Str, boom : Str -> Str and len : Str -> Str, the last without javascript. */
  private static final Map<String, Pair<List<String>, String>> SYMS = new LinkedHashMap<>();

  static {
    SYMS.put("twice", new Pair<>(List.of(STR), STR));
    SYMS.put("boom", new Pair<>(List.of(STR), STR));
    SYMS.put("len", new Pair<>(List.of(STR), STR));
  }

  private static final Map<String, String> FNS =
      Map.of("twice", "x => x + x", "boom", "x => { throw new Error('nope') }");

  private static AqlJs<String, String> js(
      Map<String, Pair<List<String>, String>> syms,
      String javaType,
      String parser,
      Map<String, String> fns) {
    return new AqlJs<>(ops(), syms, Map.of(STR, javaType), Map.of(STR, parser), fns);
  }

  private static AqlJs<String, String> js() {
    return js(SYMS, "java.lang.String", "x => x.trim()", FNS);
  }

  private static Term<String, String, String, String, String, String, String> str(String s) {
    return Term.Obj(s, STR);
  }

  private static Term<String, String, String, String, String, String, String> app(
      String f, Term<String, String, String, String, String, String, String> a) {
    return Term.Sym(f, List.of(a));
  }

  private static String message(Executable e) {
    return assertThrows(RuntimeException.class, e).getMessage();
  }

  @Nested
  class Parsing {

    @Test
    void parsesAConstantWithTheParserOfItsType() {
      assertEquals("ab", js().parse(STR, "  ab "));
    }

    @Test
    void rejectsATypeWithoutAParser() {
      String m = message(() -> js().parse("Num", "1"));
      assertTrue(m.endsWith("execution of 1 no javascript definition for Num"), m);
    }

    @Test
    void reportsAParserThatFails() {
      var js = js(SYMS, "java.lang.String", "x => x.nope()", FNS);
      String m = message(() -> js.parse(STR, "ab"));
      assertTrue(m.contains("execution of ab cannot convert to Str error: "), m);
      assertTrue(
          m.endsWith(
              "Possible fix: check the external_parsers of the typeside for type conversion"
                  + " errors."),
          m);
    }

    @Test
    void invokesAnySourceAtAType() {
      assertEquals("abc", js().invoke(STR, "(x, y) => x + y", "ab", "c"));
    }
  }

  @Nested
  class Reduction {

    @Test
    void evaluatesASymbolOfConstantsUntilNothingChanges() {
      assertSame(str("abab"), js().reduce(app("twice", str("ab"))));
      assertSame(str("abababab"), js().reduce(app("twice", app("twice", str("ab")))));
    }

    @Test
    void leavesASymbolWithoutJavascriptOrWithANonConstantArgument() {
      var js = js();
      Term<String, String, String, String, String, String, String> y = Term.Sk("y");
      assertSame(app("len", str("ab")), js.reduce(app("len", str("ab"))));
      assertSame(app("twice", y), js.reduce(app("twice", y)));
      assertSame(app("len", str("abab")), js.reduce(app("len", app("twice", str("ab")))));
    }

    @Test
    void reducesUnderForeignKeysAndAttributes() {
      var js = js();
      Term<String, String, String, String, String, String, String> e = Term.Gen("e");
      assertSame(Term.Att("a", Term.Fk("f", e)), js.reduce(Term.Att("a", Term.Fk("f", e))));
      assertSame(Term.Var("v"), js.reduce(Term.Var("v")));
      assertSame(str("ab"), js.reduce(str("ab")));
    }

    @Test
    void reportsAFunctionThatFails() {
      String m = message(() -> js().reduce(app("boom", str("ab"))));
      assertTrue(m.contains("execution of boom on arguments [ab], class java.lang.String"), m);
      assertTrue(m.contains("error: Error: nope"), m);
    }

    @Test
    void leavesEveryTermAloneWithoutJavaTypes() {
      var none = new AqlJs<String, String>(ops(), SYMS, Map.of(), Map.of(), Map.of());
      assertSame(app("twice", str("ab")), none.reduce(app("twice", str("ab"))));
    }

    /** Without java types there are no parsers, and parse reads a table that does not exist. */
    @Test
    void cannotParseWithoutJavaTypes() {
      var none = new AqlJs<String, String>(ops(), SYMS, Map.of(), Map.of(), Map.of());
      assertThrows(NullPointerException.class, () -> none.parse(STR, "ab"));
    }
  }

  @Nested
  class Equality {

    @Test
    void comparesTheJavaTablesOnly() {
      var a = js();
      assertEquals(a, js());
      assertEquals(a.hashCode(), js().hashCode());
      assertTrue(a.equals(a));
      assertFalse(a.equals(null));
      assertEquals(a, js(Map.of(), "java.lang.String", "x => x.trim()", FNS));
    }

    @Test
    void differsByFunctionsParsersOrTypes() {
      var a = js();
      assertNotEquals(a, js(SYMS, "java.lang.String", "x => x.trim()", Map.of()));
      assertNotEquals(a, js(SYMS, "java.lang.String", "x => x", FNS));
      assertNotEquals(a, js(SYMS, "java.lang.Object", "x => x.trim()", FNS));
    }

    /** Issue 14: equals casts its argument without checking its class. */
    @Test
    void castsTheArgument() {
      Object other = "js";
      assertThrows(ClassCastException.class, () -> js().equals(other));
    }

    @Test
    void printsItsTables() {
      String s = js().toString();
      assertTrue(
          s.startsWith("AqlJs [java_tys={Str=java.lang.String}, java_parsers={Str=x => x.trim()}"),
          s);
    }
  }
}
