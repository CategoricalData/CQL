package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.LocStr;
import catdata.Pair;
import catdata.ParseException;
import catdata.Quad;
import catdata.Triple;
import catdata.cql.AqlJs;
import catdata.cql.Collage;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.Term;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RawTerm}, the untyped terms the parser builds, and the inference that turns
 * them into typed {@link Term}s.
 */
class RawTermTest {

  private static RawTerm t(String head, RawTerm... args) {
    return new RawTerm(head, new LinkedList<>(List.of(args)));
  }

  private static final RawTerm X = new RawTerm("x");

  @Nested
  class Printing {

    @Test
    void printsAConstantVariableOrAnnotatedConstant() {
      assertEquals("x", X.toString());
      assertEquals("\"1\"", new RawTerm("1").toString());
      assertEquals("\"a b\"", new RawTerm("a b").toString());
      assertEquals("\"1\"@Integer", new RawTerm("1", "Integer").toString());
    }

    @Test
    void printsAUnaryTermAsAPathAndOthersAsApplications() {
      assertEquals("x.f.g", t("g", t("f", X)).toString());
      assertEquals("plus(x, \"1\")", t("plus", X, new RawTerm("1")).toString());
    }

    @Test
    void printsForEgglog() {
      assertEquals("(x)", X.toStringEgglog());
      assertEquals("(plus (x) (f (x)))", t("plus", X, t("f", X)).toStringEgglog());
    }

    @Test
    void printsForSqlWithTheGivenQuote() {
      assertEquals("NULL", new RawTerm("null").toStringX("`"));
      assertEquals("`x`", X.toStringX("`"));
      assertEquals("`x`.`f`", t("f", X).toStringX("`"));
      assertEquals("`plus`(`x`, `y`)", t("plus", X, new RawTerm("y")).toStringX("`"));
    }

    /** The argument of a unary term is printed with toString, so a path of two quotes as one. */
    @Test
    void quotesANestedPathAsOneName() {
      assertEquals("`x.f`.`g`", t("g", t("f", X)).toStringX("`"));
    }
  }

  @Nested
  class Construction {

    @Test
    void rejectsAMissingHeadOrArguments() {
      RuntimeException head = assertThrows(RuntimeException.class, () -> new RawTerm(null));
      assertEquals("Attempt to create raw term with null head", head.getMessage());
      RuntimeException args =
          assertThrows(RuntimeException.class, () -> new RawTerm("f", (List<RawTerm>) null));
      assertEquals("Attempt to create raw term with null args", args.getMessage());
    }

    /** A head that contains 0@Float is rejected with the message ExA. */
    @Test
    void rejectsAHeadContainingADebuggingMarker() {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> new RawTerm("user0@Float.org"));
      assertEquals("ExA", e.getMessage());
    }

    /** The same marker in a literal stops the whole program from parsing. */
    @Test
    void cannotParseALiteralContainingTheMarker() {
      ParseException e =
          assertThrows(
              ParseException.class,
              () ->
                  ExpTestHelpers.parse(
                      """
                      typeside Ty = sql
                      schema S = literal : Ty { entities A attributes email : A -> Varchar }
                      instance I = literal : S {
                        generators a : A
                        equations a.email = "user0@Float.org"
                      }
                      """));
      assertTrue(e.getMessage().contains("ExA"), e.getMessage());
    }

    @Test
    void equalityComparesHeadArgumentsAndAnnotation() {
      RawTerm a = new RawTerm("1", "Integer");
      assertEquals(a, a);
      assertEquals(a, new RawTerm("1", "Integer"));
      assertEquals(a.hashCode(), new RawTerm("1", "Integer").hashCode());
      assertNotEquals(a, new RawTerm("1"));
      assertNotEquals(new RawTerm("1"), a);
      assertNotEquals(a, new RawTerm("1", "String"));
      assertNotEquals(a, new RawTerm("2", "Integer"));
      assertNotEquals(t("f", X), t("f", new RawTerm("y")));
      assertNotEquals(a, null);
      assertNotEquals(a, "1");
    }
  }

  @Nested
  class Helpers {

    @Test
    void packsAPathForwardAndBackward() {
      RawTerm p = t("g", t("f", X));
      assertEquals(List.of("x", "f", "g"), p.forwardPack());
      assertEquals(List.of("g", "f", "x"), p.backwardPack());
      assertEquals(List.of("plus", "x"), t("plus", X, new RawTerm("y")).backwardPack());
      assertEquals(List.of("x", "plus"), t("plus", X, new RawTerm("y")).forwardPack());
    }

    @Test
    void findsANodeByIndex() {
      RawTerm p = t("plus", X, t("f", new RawTerm("y")));
      assertEquals("plus", p.byIndex());
      assertEquals("f", p.byIndex(1));
      assertEquals("y", p.byIndex(1, 0));
      assertNull(p.byIndex(2));
    }

    @Test
    void appendsAVariableToTheInnermostTerm() {
      RawTerm p = t("g", t("f", new RawTerm("x", new ArrayList<>())));
      assertSame(p, p.append("v"));
      assertEquals("v.x.f.g", p.toString());
    }

    @Test
    void foldsAPathOntoAVariable() {
      assertEquals("v.f.g", RawTerm.fold(List.of("f", "g"), "v").toString());
      assertEquals("v.f.g", RawTerm.fold(Set.of("A"), List.of("A", "f", "g"), "v").toString());
      RuntimeException e =
          assertThrows(
              RuntimeException.class, () -> RawTerm.fold(Set.of("A"), List.of("B", "f"), "v"));
      assertEquals("Not an entity: B.  Paths must start with entities.", e.getMessage());
    }

    @Test
    void foldsABinaryOperatorToTheLeft() {
      RawTerm y = new RawTerm("y");
      RawTerm z = new RawTerm("z");
      assertEquals("plus(plus(x, y), z)", RawTerm.foldl1(List.of(X, y, z), "plus").toString());
      assertSame(X, RawTerm.foldl(X, List.of(), "plus"));
    }

    @Test
    void filtersOutUnaryHeads() {
      RawTerm p = t("plus", t("iso", X), new RawTerm("y"));
      assertEquals("plus(x, y)", p.filter(Set.of("iso")).toString());
      RuntimeException e = assertThrows(RuntimeException.class, () -> p.filter(Set.of("plus")));
      assertEquals("Warehousing pattern isomorphism name collision", e.getMessage());
    }

    @Test
    void doesNotInferFromAString() {
      assertNull(X.inferFrom("m"));
    }

    @Test
    void projectsEquationsOutOfTheirLocations() {
      Triple<List<Pair<String, String>>, RawTerm, RawTerm> eq = new Triple<>(List.of(), X, X);
      assertEquals(Set.of(eq), RawTerm.eqs1(List.of(new Pair<>(3, eq))));
      Quad<String, String, RawTerm, RawTerm> q = new Quad<>("x", "A", X, X);
      assertEquals(Set.of(q), RawTerm.eqs2(List.of(new Pair<>(new LocStr(0, "e"), q))));
    }
  }

  @Nested
  class Inference {

    private static final String SCHEMA =
        """
        typeside Ty = literal {
          types String Int
          constants a b : String one : Int
          functions f : String -> String
        }
        schema S = literal : Ty {
          entities A B
          foreign_keys g : A -> B
          attributes n : A -> String  m : A -> Int  n : B -> String
        }
        """;

    private Schema<String, String, Sym, Fk, Att> schema() throws Exception {
      return ExpTestHelpers.eval(SCHEMA, "S", Kind.SCHEMA);
    }

    private Collage<String, String, Sym, Fk, Att, String, String> col() throws Exception {
      return schema().collage().convert();
    }

    private AqlJs<String, Sym> js() throws Exception {
      return schema().typeSide.js;
    }

    private String inferError(Map<String, Chc<String, String>> vars, RawTerm l, RawTerm r)
        throws Exception {
      Collage<String, String, Sym, Fk, Att, String, String> col = col();
      AqlJs<String, Sym> js = js();
      return assertThrows(
              RuntimeException.class, () -> RawTerm.infer1x(vars, l, r, null, col, "pre: ", js))
          .getMessage();
    }

    @Test
    void infersTheSortOfAnEquation() throws Exception {
      var q =
          RawTerm.infer1x(
              Map.of("x", Chc.inRight("A")),
              t("n", X),
              t("f", new RawTerm("a")),
              null,
              col(),
              "",
              js());
      assertEquals(Map.of("x", Chc.inRight("A")), q.first);
      assertEquals(Chc.inLeft("String"), q.fourth);
      assertEquals("x.n", q.second.toString());
    }

    @Test
    void infersTheSortOfAnUntypedVariable() throws Exception {
      Map<String, Chc<String, String>> vars = new HashMap<>();
      vars.put("x", null);
      var q = RawTerm.infer1x(vars, t("m", X), new RawTerm("one"), null, col(), "", js());
      assertEquals(Map.of("x", Chc.inRight("A")), q.first);
    }

    @Test
    void reportsAnUndefinedSymbolWithTheClosestOnes() throws Exception {
      String msg = inferError(Map.of("x", Chc.inRight("A")), t("zz", X), X);
      assertTrue(msg.startsWith("pre: Cannot infer a well-sorted term for x.zz.\n"), msg);
      assertTrue(msg.contains("Undefined (or not java-parseable) symbol: zz."), msg);
      assertTrue(msg.contains("\nAvailable symbols:\n\t"), msg);
    }

    @Test
    void reportsAnUndefinedRightHandSide() throws Exception {
      String msg = inferError(Map.of("x", Chc.inRight("A")), t("n", X), new RawTerm("zz"));
      assertTrue(msg.startsWith("pre: Cannot infer a well-sorted term for zz.\n"), msg);
      assertTrue(msg.contains("Undefined (or not java-parseable) symbol: zz\n"), msg);
      assertTrue(msg.endsWith("Expected type: String"), msg);
    }

    @Test
    void infersTheRightHandSideAgainstTheSortOfTheLeft() throws Exception {
      String msg = inferError(Map.of("x", Chc.inRight("A")), t("n", X), t("m", X));
      assertEquals("pre: Cannot infer a well-sorted term for x.m.\nExpected type: String", msg);
    }

    @Test
    void reportsAnAmbiguousTerm() throws Exception {
      Map<String, Chc<String, String>> vars = new HashMap<>();
      vars.put("x", null);
      String msg = inferError(vars, t("n", X), t("n", X));
      assertTrue(msg.startsWith("pre: Cannot infer a unique well-sorted term for x.n = x.n"), msg);
      assertTrue(msg.contains("\nCandidates:\n\n"), msg);
    }

    @Test
    void reportsAnExpectedSortThatDoesNotMatch() throws Exception {
      Collage<String, String, Sym, Fk, Att, String, String> col = col();
      AqlJs<String, Sym> js = js();
      String msg =
          assertThrows(
                  RuntimeException.class,
                  () ->
                      RawTerm.infer1x(
                          Map.of("x", Chc.inRight("A")),
                          t("n", X),
                          null,
                          Chc.inLeft("Int"),
                          col,
                          "",
                          js))
              .getMessage();
      assertTrue(msg.endsWith("\nExpected type: Int"), msg);
    }

    @Test
    void reportsAnAnnotationThatIsNotAType() throws Exception {
      String msg = inferError(Map.of(), new RawTerm("1", "Nope"), new RawTerm("1", "Nope"));
      assertTrue(msg.startsWith("pre: Annotation Nope is not a type ("), msg);
    }

    @Test
    void reportsTooManyArguments() throws Exception {
      String msg =
          inferError(Map.of(), t("f", new RawTerm("a"), new RawTerm("b")), new RawTerm("a"));
      assertTrue(
          msg.startsWith(
              "Wrong number of arguments to top-level application in f(a, b).  Possible cause: name"
                  + " clash with typeside constants."),
          msg);
    }

    @Test
    void infersAnEquationInADeclaredContext() throws Exception {
      Triple<
              Map<String, Chc<String, String>>,
              Term<String, String, Sym, Fk, Att, String, String>,
              Term<String, String, Sym, Fk, Att, String, String>>
          r =
              RawTerm.infer2(
                  List.of(new Pair<>("x", "A"), new Pair<>("s", "String")),
                  t("n", X),
                  new RawTerm("s"),
                  col(),
                  js());
      assertEquals(Map.of("x", Chc.inRight("A"), "s", Chc.inLeft("String")), r.first);
    }

    @Test
    void infersTheSortOfAnUndeclaredContextVariable() throws Exception {
      List<Pair<String, String>> ctx = new ArrayList<>();
      ctx.add(new Pair<>("x", null));
      var r = RawTerm.infer2(ctx, t("m", X), new RawTerm("one"), col(), js());
      assertEquals(Map.of("x", Chc.inRight("A")), r.first);
    }

    @Test
    void rejectsABadContext() throws Exception {
      Collage<String, String, Sym, Fk, Att, String, String> col = col();
      AqlJs<String, Sym> js = js();
      assertEquals(
          "Duplicate variable x in context (x, A), (x, A)",
          assertThrows(
                  RuntimeException.class,
                  () ->
                      RawTerm.infer2(
                          List.of(new Pair<>("x", "A"), new Pair<>("x", "A")), X, X, col, js))
              .getMessage());
      assertEquals(
          "Nope is neither a type nor entity",
          assertThrows(
                  RuntimeException.class,
                  () -> RawTerm.infer2(List.of(new Pair<>("x", "Nope")), X, X, col, js))
              .getMessage());
      col.getEns().add("String");
      assertEquals(
          "Ambiguous: String is an entity and a type",
          assertThrows(
                  RuntimeException.class,
                  () -> RawTerm.infer2(List.of(new Pair<>("x", "String")), X, X, col, js))
              .getMessage());
    }

    @Test
    void findsTheHeadOfAPrecedenceSymbol() throws Exception {
      Collage<String, String, Sym, Fk, Att, String, String> col = col();
      assertEquals("f", RawTerm.toHeadNoPrim("f", col).toString());
      assertEquals("g", RawTerm.toHeadNoPrim("g", col).toString());
      assertEquals("m", RawTerm.toHeadNoPrim("m", col).toString());
      assertThrows(RuntimeException.class, () -> RawTerm.toHeadNoPrim("zz", col));
      RuntimeException none =
          assertThrows(RuntimeException.class, () -> RawTerm.assertUnambig("f", null));
      assertEquals("No collage within which to interpret a precedence.", none.getMessage());
    }

    /** Attributes of two entities with one name count once, so n is not ambiguous. */
    @Test
    void picksOneOfTwoAttributesWithTheSameName() throws Exception {
      Collage<String, String, Sym, Fk, Att, String, String> col = col();
      assertEquals("n", RawTerm.toHeadNoPrim("n", col).toString());
    }

    @Test
    void rejectsAPrecedenceSymbolOfTwoKinds() throws Exception {
      Collage<String, String, Sym, Fk, Att, String, String> col = col();
      col.atts().put(Att.Att("B", "f"), new Pair<>("B", "String"));
      RuntimeException amb =
          assertThrows(RuntimeException.class, () -> RawTerm.toHeadNoPrim("f", col));
      assertTrue(amb.getMessage().startsWith("f is ambiguous in "), amb.getMessage());
    }
  }
}
