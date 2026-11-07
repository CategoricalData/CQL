package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.ParseException;
import catdata.Unit;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.exp.QueryExp.QueryExpVar;
import catdata.cql.exp.QueryExp.QueryExpVisitor;
import catdata.cql.exp.QueryExpRaw.Block;
import catdata.cql.exp.QueryExpRaw.PreAgg;
import catdata.cql.exp.QueryExpRaw.Trans;
import catdata.cql.exp.SchExp.SchExpVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link QueryExpRaw}, the query literal {@code literal : S -> T { ... }}: one block
 * per target entity, each with a {@code from}, {@code where}, {@code attributes} and {@code
 * foreign_keys} clause, plus {@code params}, {@code bindings} and {@code imports}.
 */
class QueryExpRawTest {

  private static final String SCHEMAS =
      """
      typeside Ty = sql
      schema S = literal : Ty {
        entities Emp Dept
        foreign_keys worksIn : Emp -> Dept
        attributes name : Emp -> Varchar  salary : Emp -> Integer  dname : Dept -> Varchar
      }
      schema T = literal : Ty {
        entities E D
        foreign_keys d : E -> D
        attributes n : E -> Varchar  dn : D -> Varchar
      }
      instance I = literal : S {
        generators e1 e2 : Emp d1 : Dept
        equations
          e1.worksIn = d1 e2.worksIn = d1 d1.dname = x
          e1.name = a e1.salary = 1 e2.name = b e2.salary = 2
      }
      """;

  private static final String D_BLOCK = "entity D -> { from y:Dept attributes dn -> y.dname }\n";

  private static final String E_BLOCK =
      "entity E -> { from x:Emp where x.salary = 1 attributes n -> x.name"
          + " foreign_keys d -> { y -> x.worksIn } }\n";

  private static final String QUERY =
      SCHEMAS + "query Q = literal : S -> T {\n" + D_BLOCK + E_BLOCK + "}\n";

  /** The value of an attribute on every row of an entity, sorted. */
  private static Set<String> values(
      Instance<String, String, Sym, Fk, Att, String, String, Object, Object> j,
      String en,
      String att) {
    Set<String> ret = new TreeSet<>();
    for (Object x : j.algebra().en(en)) {
      Object v = j.algebra().att(Att.Att(en, att), x).obj();
      ret.add(String.valueOf(((Optional<?>) v).orElse(null)));
    }
    return ret;
  }

  private static Instance<String, String, Sym, Fk, Att, String, String, Object, Object> run(
      String program) throws ParseException {
    return ExpTestHelpers.eval(program + "instance J = eval Q I\n", "J", Kind.INSTANCE);
  }

  /** The block of a target entity, as the outline indexes it. */
  private static Block blockOf(QueryExpRaw e, String en) {
    return (Block) e.raw().get(en).get(0).s;
  }

  private static QueryExpRaw query(String program) throws ParseException {
    return ExpTestHelpers.exp(program, "Q");
  }

  private static String block(String en, String body) {
    return "query Q = literal : S -> T {\n"
        + D_BLOCK
        + "entity "
        + en
        + " -> { "
        + body
        + " }\n}\n";
  }

  @Nested
  class Evaluation {

    @Test
    void evaluatesEachBlock() throws Exception {
      var j = run(QUERY);
      assertEquals(Set.of("a"), values(j, "E", "n"));
      assertEquals(Set.of("x"), values(j, "D", "dn"));
    }

    /** Over the sql typeside, a query with params and bindings cannot be evaluated. */
    @Test
    void cannotEvaluateBoundParameters() throws Exception {
      String q =
          SCHEMAS
              + "query Q = literal : S -> T {\n params v : Integer bindings v = 2\n"
              + D_BLOCK
              + E_BLOCK.replace("x.salary = 1", "x.salary = v")
              + "}\ninstance J = eval Q I\n";
      String msg = ExpTestHelpers.error(q);
      assertTrue(msg.contains("Cannot enumerate: java.util.Optional."), msg);
    }

    @Test
    void takesTheBlocksOfItsImports() throws Exception {
      String q =
          QUERY.replace("query Q =", "query Q0 =") + "query Q = literal : S -> T { imports Q0 }\n";
      var j = run(q);
      assertEquals(Set.of("a"), values(j, "E", "n"));
      assertEquals(Set.of("x"), values(j, "D", "dn"));
    }

    /** Importing a query with params fails. */
    @Test
    void cannotImportAQueryWithParameters() throws Exception {
      String q =
          SCHEMAS
              + "query Q0 = literal : S -> T {\n params v : Integer bindings v = 2\n"
              + D_BLOCK
              + E_BLOCK.replace("x.salary = 1", "x.salary = v")
              + "}\n"
              + "query Q = literal : S -> T { imports Q0 }\n";
      String msg = ExpTestHelpers.error(q);
      assertTrue(msg.contains("Anomaly: please report"), msg);
    }

    @Test
    void allowsATypeInTheFromClause() throws Exception {
      String q =
          SCHEMAS
              + block(
                  "E",
                  "from x:Emp v:Integer where x.salary = v attributes n -> x.name"
                      + " foreign_keys d -> { y -> x.worksIn }");
      QueryExpRaw e = query(q);
      assertEquals(
          Set.of(new Pair<>("x", "Emp"), new Pair<>("v", "Integer")),
          Set.copyOf(blockOf(e, "E").gens));
      assertTrue(ExpTestHelpers.eval(q, "Q", Kind.QUERY) != null);
    }

    @Test
    void needsTheUnsafeOptionToAggregate() throws Exception {
      String msg =
          ExpTestHelpers.error(
              SCHEMAS
                  + block(
                      "E",
                      "from x:Emp attributes n -> from z:Emp return z.name aggregate x.name lambda"
                          + " a b. a foreign_keys d -> { y -> x.worksIn }"));
      assertTrue(
          msg.contains(
              "To enable aggregation, set allow_aggregation_unsafe=true globally.  Aggregation is"
                  + " not functorial."),
          msg);
    }

    @Test
    void rejectsATargetEntityTheTargetLacks() throws Exception {
      String msg = ExpTestHelpers.error(SCHEMAS + block("Z", "from x:Emp"));
      assertTrue(
          msg.contains(
              "In block for target entity Z, The proposed target entity Z does not actually appear"
                  + " in the target schema"),
          msg);
    }

    @Test
    void rejectsAFromVariableOfNoSourceEntity() throws Exception {
      String msg = ExpTestHelpers.error(SCHEMAS + block("E", "from x:Nope"));
      assertTrue(
          msg.contains("From clause contains x:Nope, but Nope is not a source entity.  Available:"),
          msg);
    }

    @Test
    void rejectsAnAttributeTheTargetLacks() throws Exception {
      String msg =
          ExpTestHelpers.error(
              SCHEMAS
                  + block(
                      "E",
                      "from x:Emp attributes zz -> x.name n -> x.name"
                          + " foreign_keys d -> { y -> x.worksIn }"));
      assertTrue(msg.contains("In return clause for zz, Not a target attribute: zz"), msg);
    }

    @Test
    void rejectsAForeignKeyTheTargetLacks() throws Exception {
      String msg =
          ExpTestHelpers.error(
              SCHEMAS
                  + block(
                      "E",
                      "from x:Emp attributes n -> x.name"
                          + " foreign_keys zz -> { y -> x.worksIn } d -> { y -> x.worksIn }"));
      assertTrue(msg.contains("Not a foreign key: zz"), msg);
    }

    @Test
    void rejectsAForeignKeyToAVariableTheTargetBlockLacks() throws Exception {
      String msg =
          ExpTestHelpers.error(
              SCHEMAS
                  + block(
                      "E",
                      "from x:Emp attributes n -> x.name foreign_keys d -> { q -> x.worksIn }"));
      assertTrue(msg.contains("Not an entity or type: q"), msg);
    }

    @Test
    void rejectsAStarOutsideSimpleQueries() {
      ParseException e =
          assertThrows(
              ParseException.class,
              () -> ExpTestHelpers.parse(SCHEMAS + block("E", "from x:Emp attributes *")));
      assertTrue(
          String.valueOf(e.getMessage()).contains("Cannot use * in non-simple queries"),
          String.valueOf(e.getMessage()));
    }
  }

  @Nested
  class Expression {

    @Test
    void dependsOnBothSchemasAndTheImports() throws Exception {
      QueryExpRaw e =
          query(
              SCHEMAS
                  + "query Q0 = literal : S -> T { }\nquery Q = literal : S -> T { imports Q0 }\n");
      assertEquals(
          Set.of(
              new Pair<>("S", Kind.SCHEMA),
              new Pair<>("T", Kind.SCHEMA),
              new Pair<>("Q0", Kind.QUERY)),
          Set.copyOf(e.deps()));
      assertEquals(List.of(new QueryExpVar("Q0")), List.copyOf(e.imports()));
    }

    @Test
    void isTypedByItsSchemas() throws Exception {
      AqlTyping g = ExpTestHelpers.typing(QUERY);
      assertEquals(new Pair<>(new SchExpVar("S"), new SchExpVar("T")), query(QUERY).type(g));
      assertEquals(new Pair<>(new SchExpVar("S"), new SchExpVar("T")), g.defs.qs.get("Q"));
    }

    @Test
    void keepsItsOptions() throws Exception {
      QueryExpRaw e = query(QUERY.replace("\n}\n", "\noptions timeout = 5 }\n"));
      assertEquals("5", e.options().get("timeout"));
      assertTrue(e.allowedOptions().containsAll(ExpTestHelpers.BASE_OPTIONS));
    }

    @Test
    void indexesItsBlocksForTheOutline() throws Exception {
      QueryExpRaw e = query(QUERY);
      assertEquals(Set.of("D", "E"), e.raw().keySet());
      Block b = blockOf(e, "E");
      assertEquals(Set.of("from", "where", "attributes", "foreign_keys"), b.raw().keySet());
      assertEquals("x : Emp", b.raw().get("from").get(0).toString());
      assertEquals("x.salary = \"1\"", b.raw().get("where").get(0).toString());
      assertEquals("n -> x.name", b.raw().get("attributes").get(0).toString());
    }

    @Test
    void printsItsBlocksAndOptions() throws Exception {
      QueryExpRaw e = query(QUERY.replace("\n}\n", "\noptions timeout = 5 }\n"));
      String s = e.toString();
      assertTrue(s.startsWith("literal : S -> T {"), s);
      assertTrue(
          s.contains("\nentity E -> { \n\tfrom \n\t\tx : Emp\n\twhere\n\t\tx.salary = \"1\""), s);
      assertTrue(s.contains("\n\tforeign_keys\n\t\td -> {y -> x.worksIn options }"), s);
      assertTrue(s.endsWith(" options timeout = 5}"), s);
    }

    @Test
    void printsItsImports() throws Exception {
      QueryExpRaw e =
          query(
              SCHEMAS
                  + "query Q0 = literal : S -> T { }\nquery Q = literal : S -> T { imports Q0 }\n");
      assertEquals("literal : S -> T {\nimports Q0}", e.toString());
    }

    /** The printed query has no params or bindings, so it parses to another query. */
    @Test
    void printsWithoutParametersOrBindings() throws Exception {
      String q =
          SCHEMAS
              + "query Q = literal : S -> T {\n params v : Integer bindings v = 2\n"
              + D_BLOCK
              + "}\n";
      QueryExpRaw e = query(q);
      assertFalse(e.toString().contains("params"), e.toString());
      QueryExpRaw again = query(SCHEMAS + "query Q = " + e + "\n");
      assertNotEquals(e, again);
      assertEquals(e.toString(), again.toString());
    }

    @Test
    void printsABlockInTheViewerFormat() throws Exception {
      Block b = blockOf(query(QUERY.replace("} }\n}", "} options timeout = 5 }\n}")), "E");
      String s = b.toString2();
      assertTrue(s.startsWith("\t\t\t\t\tfrom x : Emp\n\t\t\t\twhere\tx.salary = \"1\""), s);
      assertTrue(s.contains("\n\t\t\t\tattributes\tn -> x.name"), s);
      assertTrue(s.contains("\n\t\t\t\tforeign_keys\td -> {{y -> x.worksIn options }}"), s);
      assertTrue(s.endsWith("\n\t\t\t\toptions \n\t\t\t\ttimeout = 5"), s);
      assertSame(s, b.toString2());
    }

    @Test
    void equalityComparesEveryPart() throws Exception {
      QueryExpRaw a = query(QUERY);
      QueryExpRaw b = query(QUERY);
      assertEquals(a, a);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
      assertNotEquals(a, query(QUERY.replace("x.salary = 1", "x.salary = 2")));
      assertNotEquals(a, query(QUERY.replace("literal : S -> T", "literal : T -> T")));
      assertNotEquals(a, query(QUERY.replace("literal : S -> T", "literal : S -> S")));
      assertNotEquals(a, query(QUERY.replace("\n}\n", "\noptions timeout = 5 }\n")));
      assertNotEquals(a, null);
      assertNotEquals(a, "literal");
    }

    @Test
    void equalityComparesParametersBindingsAndImports() throws Exception {
      String base = SCHEMAS + "query Q0 = literal : S -> T { }\n";
      QueryExpRaw plain = query(base + "query Q = literal : S -> T { }\n");
      assertNotEquals(plain, query(base + "query Q = literal : S -> T { params v : Integer }\n"));
      assertNotEquals(
          plain,
          query(base + "query Q = literal : S -> T { params v : Integer bindings v = 1 }\n"));
      assertNotEquals(plain, query(base + "query Q = literal : S -> T { imports Q0 }\n"));
    }

    @Test
    void blocksCompareEveryClause() throws Exception {
      Block b = blockOf(query(QUERY), "E");
      assertEquals(b, b);
      assertEquals(b, blockOf(query(QUERY), "E"));
      assertEquals(b.hashCode(), blockOf(query(QUERY), "E").hashCode());
      assertNotEquals(b, blockOf(query(QUERY), "D"));
      assertNotEquals(b, blockOf(query(QUERY.replace("x.salary = 1", "x.salary = 2")), "E"));
      assertNotEquals(b, blockOf(query(QUERY.replace("n -> x.name", "n -> x.worksIn.dname")), "E"));
      assertNotEquals(b, blockOf(query(QUERY.replace("from x:Emp", "from x:Emp z:Dept")), "E"));
      assertNotEquals(
          b,
          blockOf(
              query(QUERY.replace("{ y -> x.worksIn }", "{ y -> x.worksIn options timeout = 1 }")),
              "E"));
      assertNotEquals(
          b, blockOf(query(QUERY.replace("} }\n}", "} options timeout = 5 }\n}")), "E"));
      assertNotEquals(b, null);
      assertNotEquals(b, "entity E");
    }

    @Test
    void blocksAndTranslationsAreInertExpressions() throws Exception {
      Block b = blockOf(query(QUERY), "E");
      assertEquals(Unit.unit, b.type(new AqlTyping()));
      assertNull(b.kind());
      assertNull(b.eval0(null, false));
      assertNull(b.deps());
      assertNull(b.Var("v"));
      assertTrue(b.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, b.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      b.map(seen::add);
      assertEquals(List.of(b), seen);

      Trans t = b.fks.iterator().next().second;
      assertEquals(Unit.unit, t.type(new AqlTyping()));
      assertNull(t.kind());
      assertNull(t.eval0(null, false));
      assertNull(t.deps());
      assertNull(t.Var("v"));
      assertNull(t.options());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, t.allowedOptions());
      seen.clear();
      t.map(seen::add);
      assertEquals(List.of(t), seen);
      assertEquals("y -> x.worksIn", t.raw().get("generators").get(0).toString());
    }

    @Test
    void translationsCompareTheirGeneratorsAndOptions() throws Exception {
      Trans t = blockOf(query(QUERY), "E").fks.iterator().next().second;
      assertEquals(t, t);
      assertEquals(t, blockOf(query(QUERY), "E").fks.iterator().next().second);
      assertEquals(
          t.hashCode(), blockOf(query(QUERY), "E").fks.iterator().next().second.hashCode());
      assertNotEquals(
          t,
          blockOf(
                  query(
                      QUERY.replace(
                          "{ y -> x.worksIn }", "{ y -> x.worksIn options timeout = 1 }")),
                  "E")
              .fks
              .iterator()
              .next()
              .second);
      assertNotEquals(
          t,
          blockOf(
                  query(
                      SCHEMAS
                          + block("E", "from x:Emp foreign_keys d -> { y -> x.worksIn.dname }")),
                  "E")
              .fks
              .iterator()
              .next()
              .second);
      assertNotEquals(t, null);
      assertNotEquals(t, "{}");
    }

    @Test
    void aggregatesCompareEveryPart() throws Exception {
      RawTerm x = new RawTerm("x");
      RawTerm y = new RawTerm("y");
      List<Pair<String, String>> gens = List.of(new Pair<>("z", "Emp"));
      List<Pair<RawTerm, RawTerm>> eqs = List.of(new Pair<>(x, y));
      Pair<String, String> ctx = new Pair<>("a", "b");
      PreAgg p = new PreAgg(gens, eqs, x, ctx, y, x);
      assertEquals(p, p);
      assertEquals(p, new PreAgg(gens, eqs, x, ctx, y, x));
      assertEquals(p.hashCode(), new PreAgg(gens, eqs, x, ctx, y, x).hashCode());
      assertNotEquals(p, new PreAgg(List.of(), eqs, x, ctx, y, x));
      assertNotEquals(p, new PreAgg(gens, List.of(), x, ctx, y, x));
      assertNotEquals(p, new PreAgg(gens, eqs, y, ctx, y, x));
      assertNotEquals(p, new PreAgg(gens, eqs, x, new Pair<>("b", "a"), y, x));
      assertNotEquals(p, new PreAgg(gens, eqs, x, ctx, x, x));
      assertNotEquals(p, new PreAgg(gens, eqs, x, ctx, y, y));
      assertNotEquals(p, null);
      assertNotEquals(p, "aggregate");
      PreAgg none = new PreAgg(null, null, null, null, null, null);
      assertEquals(none, new PreAgg(null, null, null, null, null, null));
      assertEquals(none.hashCode(), new PreAgg(null, null, null, null, null, null).hashCode());
      assertNotEquals(none, p);
      assertNotEquals(new PreAgg(gens, null, null, null, null, null), p);
      assertNotEquals(new PreAgg(gens, eqs, null, null, null, null), p);
      assertNotEquals(new PreAgg(gens, eqs, x, null, null, null), p);
      assertNotEquals(new PreAgg(gens, eqs, x, ctx, null, null), p);
      assertNotEquals(new PreAgg(gens, eqs, x, ctx, y, null), p);
      assertEquals(
          "from z:Emp\n\twhere x=y\n\treturn x\n\taggregate y\n\tlambda a b. x", p.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() throws Exception {
      QueryExpRaw e = query(QUERY);
      QueryExpVisitor<String, Unit, RuntimeException> v = mock(QueryExpVisitor.class);
      when(v.visit(Unit.unit, e)).thenReturn("visited");
      assertEquals("visited", e.accept(Unit.unit, v));
    }

    @Test
    void mapsOverItsSchemas() throws Exception {
      QueryExpRaw e = query(QUERY);
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e, new SchExpVar("S"), new SchExpVar("T")), seen);
    }
  }

  /**
   * A query into a schema whose observation equation {@code f.v = f.w} the query must respect,
   * checked by the completion prover over a typeside where plus is commutative.
   */
  @Nested
  class TargetEquationsUnderCompletion {

    private String program(String equations, String v, String w) {
      return """
          options prover = completion completion_precedence = "a b c x plus v w"
          typeside Ty = literal { types T constants a b c : T functions plus : T, T -> T
            equations forall x y. plus(x, y) = plus(y, x) %s }
          schema S = literal : Ty { entities E attributes v w : E -> T }
          schema S2 = literal : Ty { entities F attributes v w : F -> T
            observation_equations forall f. f.v = f.w }
          query Q = literal : S -> S2 { entity F -> { from x:E attributes v -> %s w -> %s } }
          """
          .formatted(equations, v, w);
    }

    /** Commutativity alone does not make plus associative, but the query passes. */
    @Test
    void acceptsAnEquationThatNeedsAssociativity() throws Exception {
      AqlMultiDriver d =
          ExpTestHelpers.run(program("", "plus(plus(a, b), c)", "plus(a, plus(b, c))"));
      assertNull(d.env.exn);
      String msg = ExpTestHelpers.error(program("", "plus(a, b)", "plus(b, c)"));
      assertTrue(
          msg.contains("Target equation f.v = f.w not respected: transforms to (a plus b) = (b"),
          msg);
    }

    /** {@code plus(a, b) = plus(b, a)} is an instance of an axiom, but the query fails. */
    @Test
    void rejectsAnInstanceOfCommutativityGivenARightUnit() throws Exception {
      String msg =
          ExpTestHelpers.error(program("forall x. plus(x, a) = x", "plus(a, b)", "plus(b, a)"));
      assertTrue(msg.contains("transforms to (a plus b) = (b plus a), which is not provable"), msg);
    }
  }
}
