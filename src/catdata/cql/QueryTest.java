package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import catdata.cql.Query.Agg;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QueryTest {

  private static final String TY = "Varchar";
  private static final String BOOL = "Boolean";

  private static final String EMP = "Employee";
  private static final String DEPT = "Department";
  private static final String CO = "Company";
  private static final Fk WORKS_IN = Fk.Fk(EMP, "worksIn");
  private static final Fk PART_OF = Fk.Fk(DEPT, "partOf");
  private static final Att NAME = Att.Att(EMP, "name");
  private static final Att LABEL = Att.Att(DEPT, "label");
  private static final Att TITLE = Att.Att(CO, "title");

  private static final String ROW = "Row";
  private static final Att OUT = Att.Att(ROW, "out");

  private static final String PERSON = "Person";
  private static final String UNIT = "Unit";
  private static final Fk EMPLOYER = Fk.Fk(PERSON, "employer");
  private static final Att PNAME = Att.Att(PERSON, "pname");
  private static final Att UNAME = Att.Att(UNIT, "uname");

  private static final Fk BACKUP = Fk.Fk(PERSON, "backup");

  private static final String BLANK = "Blank";

  private static final String NODE = "Node";
  private static final Fk PARENT = Fk.Fk(NODE, "parent");
  private static final String STEP = "Step";
  private static final Fk NEXT = Fk.Fk(STEP, "next");

  private static final String EMPTY_FROM = "Empty from clause doesn't work with sql";

  private static AqlOptions ops() {
    return AqlOptions.initialOptions;
  }

  private static AqlOptions unvalidated() {
    return new AqlOptions(ops(), AqlOption.dont_validate_unsafe, true);
  }

  /**
   * The schemas, built once and shared: constructing {@link SqlTypeSide} spins up the JS engine
   * and a prover, which is slow. The source schema is acyclic on purpose; a cyclic one with
   * generators and no equations trips the divergence check in the initial algebra.
   */
  private static final class Fixtures {
    private static final TypeSide<String, Sym> TS = SqlTypeSide.SqlTypeSide(ops());

    /** Employee -worksIn-> Department -partOf-> Company, with a Varchar attribute on each. */
    private static final Schema<String, String, Sym, Fk, Att> SRC =
        schema(
            List.of(EMP, DEPT, CO),
            Map.of(NAME, EMP, LABEL, DEPT, TITLE, CO),
            Map.of(WORKS_IN, new Pair<>(EMP, DEPT), PART_OF, new Pair<>(DEPT, CO)));

    /** A single entity with a single attribute. */
    private static final Schema<String, String, Sym, Fk, Att> ROWS =
        schema(List.of(ROW), Map.of(OUT, ROW), Map.of());

    /** Person -employer-> Unit, with a Varchar attribute on each. */
    private static final Schema<String, String, Sym, Fk, Att> PEOPLE =
        schema(
            List.of(PERSON, UNIT),
            Map.of(PNAME, PERSON, UNAME, UNIT),
            Map.of(EMPLOYER, new Pair<>(PERSON, UNIT)));

    /** ROWS plus an entity with no attributes, which sorts before Row. */
    private static final Schema<String, String, Sym, Fk, Att> BLANKS =
        schema(List.of(BLANK, ROW), Map.of(OUT, ROW), Map.of());

    /** ROWS with the equation forall x:Row. x.out = "k". */
    private static final Schema<String, String, Sym, Fk, Att> CONSTANT_ROWS =
        schema(
            List.of(ROW),
            Map.of(OUT, ROW),
            Map.of(),
            List.of(forall("x", ROW, Term.Att(OUT, Term.Var("x")), Term.Obj(Optional.of("k"), TY))));

    /** ROWS with the equation forall x:Row. isNull(x.out) = false. */
    private static final Schema<String, String, Sym, Fk, Att> NON_NULL_ROWS =
        schema(
            List.of(ROW),
            Map.of(OUT, ROW),
            Map.of(),
            List.of(
                forall(
                    "x",
                    ROW,
                    Term.Sym(
                        Sym.Sym("isNull", new Pair<>(List.of(TY), BOOL)),
                        List.of(Term.Att(OUT, Term.Var("x")))),
                    Term.Obj(Optional.of(false), BOOL))));

    /** PEOPLE with a second foreign key and the equation forall p:Person. p.employer = p.backup. */
    private static final Schema<String, String, Sym, Fk, Att> PAIRED =
        schema(
            List.of(PERSON, UNIT),
            Map.of(PNAME, PERSON, UNAME, UNIT),
            Map.of(EMPLOYER, new Pair<>(PERSON, UNIT), BACKUP, new Pair<>(PERSON, UNIT)),
            List.of(
                forall(
                    "p",
                    PERSON,
                    Term.Fk(EMPLOYER, Term.Var("p")),
                    Term.Fk(BACKUP, Term.Var("p")))));

    /** Node -parent-> Node. */
    private static final Schema<String, String, Sym, Fk, Att> TREE =
        schema(List.of(NODE), Map.of(), Map.of(PARENT, new Pair<>(NODE, NODE)));

    /** Step -next-> Step. */
    private static final Schema<String, String, Sym, Fk, Att> LOOP =
        schema(List.of(STEP), Map.of(), Map.of(NEXT, new Pair<>(STEP, STEP)));

    private static Schema<String, String, Sym, Fk, Att> schema(
        List<String> ens, Map<Att, String> atts, Map<Fk, Pair<String, String>> fks) {
      return schema(ens, atts, fks, List.of());
    }

    private static Schema<String, String, Sym, Fk, Att> schema(
        List<String> ens,
        Map<Att, String> atts,
        Map<Fk, Pair<String, String>> fks,
        List<Eq<String, String, Sym, Fk, Att, Void, Void>> eqs) {
      Collage<String, String, Sym, Fk, Att, Void, Void> col = new CCollage<>();
      col.tys().addAll(TS.tys);
      col.syms().putAll(TS.syms);
      col.java_tys().putAll(TS.js.java_tys);
      col.getEns().addAll(ens);
      atts.forEach((a, en) -> col.atts().put(a, new Pair<>(en, TY)));
      col.fks().putAll(fks);
      col.eqs().addAll(eqs);
      return new Schema<>(TS, col, ops());
    }

    /** A schema equation in a single entity-sorted variable. */
    private static Eq<String, String, Sym, Fk, Att, Void, Void> forall(
        String v,
        String en,
        Term<String, String, Sym, Fk, Att, Void, Void> l,
        Term<String, String, Sym, Fk, Att, Void, Void> r) {
      return new Eq<>(Map.of(v, Chc.inRight(en)), l, r);
    }
  }

  private static Term<String, String, Sym, Fk, Att, String, String> gen(String v) {
    return Term.Gen(v);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> sk(String v) {
    return Term.Sk(v);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> fk(
      Fk f, Term<String, String, Sym, Fk, Att, String, String> t) {
    return Term.Fk(f, t);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> att(
      Att a, Term<String, String, Sym, Fk, Att, String, String> t) {
    return Term.Att(a, t);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> str(String s) {
    return Term.Obj(Optional.of(s), TY);
  }

  /** The path v.fks[0].fks[1]..., as it appears in a foreign key clause. */
  private static Term<Void, String, Void, Fk, Void, String, Void> path(String v, Fk... fks) {
    Term<Void, String, Void, Fk, Void, String, Void> t = Term.Gen(v);
    for (Fk f : fks) {
      t = Term.Fk(f, t);
    }
    return t;
  }

  private static Pair<
          Term<String, String, Sym, Fk, Att, String, String>,
          Term<String, String, Sym, Fk, Att, String, String>>
      eq(
          Term<String, String, Sym, Fk, Att, String, String> l,
          Term<String, String, Sym, Fk, Att, String, String> r) {
    return new Pair<>(l, r);
  }

  /** One "from ... where ..." block of a query. */
  private static final class Block {
    private final LinkedHashMap<String, Chc<String, String>> vars = new LinkedHashMap<>();
    private final List<Eq<String, String, Sym, Fk, Att, String, String>> eqs = new ArrayList<>();

    Block and(String v, String en) {
      vars.put(v, Chc.inLeft(en));
      return this;
    }

    Block andType(String v) {
      vars.put(v, Chc.inRight(TY));
      return this;
    }

    Block where(
        Term<String, String, Sym, Fk, Att, String, String> l,
        Term<String, String, Sym, Fk, Att, String, String> r) {
      eqs.add(new Eq<>(null, l, r));
      return this;
    }
  }

  private static Block from(String v, String en) {
    return new Block().and(v, en);
  }

  private static Block fromNothing() {
    return new Block();
  }

  /** Assembles the maps the {@link Query} constructor takes. */
  private static final class Builder {
    private final Map<String, String> params = new HashMap<>();
    private final Map<String, Term<String, Void, Sym, Void, Void, Void, Void>> consts =
        new HashMap<>();
    private final Map<String, Block> blocks = new HashMap<>();
    private final Map<Att, Chc<Term<String, String, Sym, Fk, Att, String, String>, Agg<String, String, Sym, Fk, Att>>> atts =
        new HashMap<>();
    private final Map<Fk, Map<String, Term<Void, String, Void, Fk, Void, String, Void>>> fks =
        new HashMap<>();
    private final Map<Fk, Map<String, Term<String, String, Sym, Fk, Att, String, String>>> fkSks =
        new HashMap<>();
    private AqlOptions options = ops();

    Builder entity(String en2, Block b) {
      blocks.put(en2, b);
      return this;
    }

    Builder attribute(Att a, Term<String, String, Sym, Fk, Att, String, String> t) {
      atts.put(a, Chc.inLeft(t));
      return this;
    }

    Builder aggregate(Att a, Agg<String, String, Sym, Fk, Att> g) {
      atts.put(a, Chc.inRight(g));
      return this;
    }

    Builder foreignKey(Fk f, String v, Term<Void, String, Void, Fk, Void, String, Void> t) {
      fks.computeIfAbsent(f, k -> new HashMap<>()).put(v, t);
      return this;
    }

    /** Maps a type-sorted variable of the foreign key's target block. */
    Builder foreignKeySkolem(
        Fk f, String v, Term<String, String, Sym, Fk, Att, String, String> t) {
      fkSks.computeIfAbsent(f, k -> new HashMap<>()).put(v, t);
      return this;
    }

    Builder param(String p) {
      params.put(p, TY);
      return this;
    }

    Builder bind(String p, String value) {
      consts.put(p, Term.Obj(Optional.of(value), TY));
      return this;
    }

    Builder options(AqlOptions o) {
      options = o;
      return this;
    }

    Query<String, String, Sym, Fk, Att, String, Fk, Att> build(
        Schema<String, String, Sym, Fk, Att> src, Schema<String, String, Sym, Fk, Att> dst) {
      return new Query<>(params, consts, ens(), atts, fks(), sks(), src, dst, options);
    }

    /** Builds through {@link Query#makeQuery}, which drops params and bindings. */
    Query<String, String, Sym, Fk, Att, String, Fk, Att> make(
        Schema<String, String, Sym, Fk, Att> src, Schema<String, String, Sym, Fk, Att> dst) {
      return Query.makeQuery(ens(), atts, fks(), sks(), src, dst, options);
    }

    private Map<String, Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens() {
      Map<String, Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens =
          new HashMap<>();
      blocks.forEach((en2, b) -> ens.put(en2, new Triple<>(b.vars, b.eqs, options)));
      return ens;
    }

    private Map<Fk, Pair<Map<String, Term<Void, String, Void, Fk, Void, String, Void>>, AqlOptions>> fks() {
      Map<Fk, Pair<Map<String, Term<Void, String, Void, Fk, Void, String, Void>>, AqlOptions>> ret =
          new HashMap<>();
      fks.forEach((f, m) -> ret.put(f, new Pair<>(m, options)));
      return ret;
    }

    private Map<Fk, Map<String, Term<String, String, Sym, Fk, Att, String, String>>> sks() {
      Map<Fk, Map<String, Term<String, String, Sym, Fk, Att, String, String>>> ret =
          new HashMap<>();
      fks.keySet().forEach(f -> ret.put(f, new HashMap<>(fkSks.getOrDefault(f, Map.of()))));
      return ret;
    }
  }

  /** Row: from e:Employee return out -> e.name. */
  private static Builder rowBuilder() {
    return new Builder().entity(ROW, from("e", EMP)).attribute(OUT, att(NAME, gen("e")));
  }

  private static Query<String, String, Sym, Fk, Att, String, Fk, Att> rowQuery() {
    return rowBuilder().build(Fixtures.SRC, Fixtures.ROWS);
  }

  /**
   * Person: from e:Employee return pname -> e.name; Unit: from d:Department return uname ->
   * d.label; employer: d -> e.worksIn.
   */
  private static Builder peopleBuilder() {
    return new Builder()
        .entity(PERSON, from("e", EMP))
        .entity(UNIT, from("d", DEPT))
        .attribute(PNAME, att(NAME, gen("e")))
        .attribute(UNAME, att(LABEL, gen("d")))
        .foreignKey(EMPLOYER, "d", path("e", WORKS_IN));
  }

  private static Query<String, String, Sym, Fk, Att, String, Fk, Att> peopleQuery() {
    return peopleBuilder().build(Fixtures.SRC, Fixtures.PEOPLE);
  }

  /** aggregate over d:Department where d = e.worksIn of d.label, starting from "" with a. */
  private static Agg<String, String, Sym, Fk, Att> agg() {
    return new Agg<>(
        str(""),
        Term.Var("a"),
        Map.of("d", DEPT),
        Set.of(eq(gen("d"), fk(WORKS_IN, gen("e")))),
        att(LABEL, gen("d")),
        new Pair<>("a", "b"));
  }

  /** Row: from e:Employee return out -> agg(). */
  private static Query<String, String, Sym, Fk, Att, String, Fk, Att> aggQuery() {
    return new Builder()
        .entity(ROW, from("e", EMP))
        .aggregate(OUT, agg())
        .build(Fixtures.SRC, Fixtures.ROWS);
  }

  @Nested
  class Construction {

    @Test
    void buildsOneFrozenBlockPerTargetEntity() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = peopleQuery();
      assertEquals(Set.of(PERSON, UNIT), new HashSet<>(q.ens.keySet()));
      assertEquals(Map.of("e", EMP), q.ens.get(PERSON).gens);
      assertEquals(Map.of("d", DEPT), q.ens.get(UNIT).gens);
    }

    @Test
    void buildsOneSingletonBlockPerSourceType() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      assertEquals(
          new HashSet<>(Fixtures.SRC.typeSide.tys), new HashSet<>(q.tys.keySet()));
      assertEquals(Map.of("_y_", TY), q.tys.get(TY).sks);
      assertTrue(q.tys.get(TY).gens.isEmpty());
    }

    @Test
    void typeSortedFromVariablesBecomeSkolems() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(ROW, from("e", EMP).andType("x"))
              .attribute(OUT, sk("x"))
              .build(Fixtures.SRC, Fixtures.ROWS);
      assertEquals(Map.of("e", EMP), q.ens.get(ROW).gens);
      assertEquals(Map.of("x", TY), q.ens.get(ROW).sks);
    }

    @Test
    void parametersBecomeSkolemsOfEveryBlock() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          rowBuilder().param("p").build(Fixtures.SRC, Fixtures.ROWS);
      assertEquals(Map.of("p", TY), q.ens.get(ROW).sks);
    }

    @Test
    void foreignKeyTransformsRunFromTheTargetBlockToTheSourceBlock() {
      // A foreign key Person -> Unit becomes a transform from the Unit block to the Person block.
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = peopleQuery();
      var t = q.fks.get(EMPLOYER);
      assertSame(q.ens.get(UNIT), t.src());
      assertSame(q.ens.get(PERSON), t.dst());
      assertEquals(path("e", WORKS_IN), t.gens().apply("d", DEPT));
    }

    @Test
    void foreignKeyTransformsFixTheParameters() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          peopleBuilder().param("p").build(Fixtures.SRC, Fixtures.PEOPLE);
      assertEquals(sk("p"), q.fks.get(EMPLOYER).sks().apply("p", TY));
    }

    @Test
    void foreignKeyTransformsMapTypeSortedVariables() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          peopleBuilder()
              .entity(UNIT, from("d", DEPT).andType("x"))
              .foreignKeySkolem(EMPLOYER, "x", att(NAME, gen("e")))
              .build(Fixtures.SRC, Fixtures.PEOPLE);
      assertEquals(att(NAME, gen("e")), q.fks.get(EMPLOYER).sks().apply("x", TY));
    }

    @Test
    void recordsThePerForeignKeyOptions() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = peopleQuery();
      assertSame(ops(), q.doNotValidate.get(EMPLOYER));
    }
  }

  @Nested
  class TotalityCheck {

    private RuntimeException failure(Builder b, Schema<String, String, Sym, Fk, Att> dst) {
      return assertThrows(RuntimeException.class, () -> b.build(Fixtures.SRC, dst));
    }

    @Test
    void rejectsAMissingBlock() {
      // Blocks are checked before return and foreign key clauses, so none are needed here.
      Builder b = new Builder().entity(PERSON, from("e", EMP));
      assertEquals("no query for Unit", failure(b, Fixtures.PEOPLE).getMessage());
    }

    @Test
    void rejectsABlockForAnUnknownEntity() {
      Builder b = rowBuilder().entity("Ghost", from("e", EMP));
      String msg = failure(b, Fixtures.ROWS).getMessage();
      assertTrue(
          msg.startsWith("there is a query for Ghost, which is not an entity in the target"), msg);
    }

    @Test
    void rejectsAMissingReturnClause() {
      Builder b = new Builder().entity(ROW, from("e", EMP));
      String msg = failure(b, Fixtures.ROWS).getMessage();
      assertTrue(msg.startsWith("no return clause for attribute"), msg);
      assertTrue(msg.endsWith("at entity Row"), msg);
    }

    @Test
    void rejectsAReturnClauseForAnUnknownAttribute() {
      Builder b = rowBuilder().attribute(Att.Att(ROW, "ghost"), att(NAME, gen("e")));
      String msg = failure(b, Fixtures.ROWS).getMessage();
      assertTrue(msg.startsWith("there is an attributes clause for"), msg);
    }

    @Test
    void rejectsAMissingForeignKeyClause() {
      Builder b =
          new Builder()
              .entity(PERSON, from("e", EMP))
              .entity(UNIT, from("d", DEPT))
              .attribute(PNAME, att(NAME, gen("e")))
              .attribute(UNAME, att(LABEL, gen("d")));
      String msg = failure(b, Fixtures.PEOPLE).getMessage();
      assertTrue(msg.startsWith("no transform for foreign key"), msg);
    }

    @Test
    void rejectsAForeignKeyClauseForAnUnknownForeignKey() {
      Builder b = peopleBuilder().foreignKey(Fk.Fk(PERSON, "ghost"), "d", path("e", WORKS_IN));
      String msg = failure(b, Fixtures.PEOPLE).getMessage();
      assertTrue(msg.startsWith("there is a transform for"), msg);
    }
  }

  @Nested
  class Validation {

    @Test
    void namesTheEntityWhoseBlockFailsToBuild() {
      Builder b = new Builder().entity(ROW, from("e", "Nowhere")).attribute(OUT, str("x"));
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> b.build(Fixtures.SRC, Fixtures.ROWS));
      assertTrue(e.getMessage().startsWith("In block for entity Row, "), e.getMessage());
    }

    @Test
    void namesTheForeignKeyWhoseTransformFailsToBuild() {
      Builder b = peopleBuilder().foreignKey(EMPLOYER, "d", path("nope"));
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> b.build(Fixtures.SRC, Fixtures.PEOPLE));
      assertTrue(
          e.getMessage().startsWith("In transform for foreign key employer, "), e.getMessage());
    }

    @Test
    void rejectsAForeignKeyClauseThatDoesNotPreserveTheTargetWhereClause() {
      // Unit requires d.label = "rd", but nothing in the Person block implies e.worksIn.label = "rd",
      // so d -> e.worksIn is not a morphism of the frozen instances.
      Builder b =
          peopleBuilder()
              .entity(UNIT, from("d", DEPT).where(att(LABEL, gen("d")), str("rd")));
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> b.build(Fixtures.SRC, Fixtures.PEOPLE));
      assertTrue(
          e.getMessage().startsWith("In transform for foreign key employer, "), e.getMessage());
    }

    @Test
    void acceptsTheForeignKeyClauseOnceTheSourceBlockImpliesTheEquation() {
      Builder b =
          peopleBuilder()
              .entity(UNIT, from("d", DEPT).where(att(LABEL, gen("d")), str("rd")))
              .entity(
                  PERSON, from("e", EMP).where(att(LABEL, fk(WORKS_IN, gen("e"))), str("rd")));
      assertDoesNotThrow(() -> b.build(Fixtures.SRC, Fixtures.PEOPLE));
    }

    @Test
    void dontValidateUnsafeSkipsTheEquationCheck() {
      Builder b =
          peopleBuilder()
              .entity(UNIT, from("d", DEPT).where(att(LABEL, gen("d")), str("rd")))
              .options(unvalidated());
      assertDoesNotThrow(() -> b.build(Fixtures.SRC, Fixtures.PEOPLE));
    }

    @Test
    void validatesTheLocalBlockOfAnAggregate() {
      assertDoesNotThrow(QueryTest::aggQuery);
    }
  }

  /** A query must send every equation of its target schema to one provable in the source. */
  @Nested
  class TargetEquations {

    @Test
    void acceptsAnAttributeEquationThatTheWhereClauseImplies() {
      Builder b =
          rowBuilder().entity(ROW, from("e", EMP).where(att(NAME, gen("e")), str("k")));
      assertDoesNotThrow(() -> b.build(Fixtures.SRC, Fixtures.CONSTANT_ROWS));
    }

    @Test
    void rejectsAnAttributeEquationThatTheWhereClauseDoesNotImply() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class, () -> rowBuilder().build(Fixtures.SRC, Fixtures.CONSTANT_ROWS));
      String msg = e.getMessage();
      assertTrue(msg.startsWith("Target equation "), msg);
      assertTrue(msg.contains(" not respected: transforms to e.name = "), msg);
      assertTrue(msg.contains("which is not provable in the sub-query for Row"), msg);
    }

    @Test
    void translatesFunctionSymbolsThroughTheReturnClauses() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class, () -> rowBuilder().build(Fixtures.SRC, Fixtures.NON_NULL_ROWS));
      String msg = e.getMessage();
      assertTrue(msg.contains("transforms to isNull(e.name) = "), msg);
    }

    @Test
    void anAttributeEquationOverAnAggregateIsAnAnomaly() {
      Builder b =
          new Builder()
              .entity(ROW, from("e", EMP).where(att(NAME, gen("e")), str("k")))
              .aggregate(OUT, agg());
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> b.build(Fixtures.SRC, Fixtures.CONSTANT_ROWS));
      assertEquals("Anomaly: please report", e.getMessage());
    }

    /** Person: from e:Employee, d2:Department; employer: d -> e.worksIn; backup: d -> d2. */
    private Builder paired() {
      return peopleBuilder()
          .entity(PERSON, from("e", EMP).and("d2", DEPT))
          .foreignKey(BACKUP, "d", path("d2"));
    }

    @Test
    void acceptsAPathEquationThatTheWhereClauseImplies() {
      Builder b =
          paired()
              .entity(
                  PERSON, from("e", EMP).and("d2", DEPT).where(fk(WORKS_IN, gen("e")), gen("d2")));
      assertDoesNotThrow(() -> b.build(Fixtures.SRC, Fixtures.PAIRED));
    }

    @Test
    void rejectsAPathEquationThatTheForeignKeyClausesDoNotRespect() {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> paired().build(Fixtures.SRC, Fixtures.PAIRED));
      String msg = e.getMessage();
      assertTrue(msg.startsWith("Target equation "), msg);
      assertTrue(msg.endsWith("which is not provable in the sub-query for Person"), msg);
    }
  }

  /**
   * Unless query_remove_redundancy is off, makeQuery removes every from variable that a where
   * clause equates to a term not mentioning it, substituting the term for the variable.
   */
  @Nested
  class MakeQuery {

    @Test
    void removesAnEntityVariableEqualToAPath() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(ROW, from("e", EMP).and("d", DEPT).where(gen("d"), fk(WORKS_IN, gen("e"))))
              .attribute(OUT, att(LABEL, gen("d")))
              .make(Fixtures.SRC, Fixtures.ROWS);
      assertEquals(Map.of("e", EMP), q.ens.get(ROW).gens);
      assertTrue(q.ens.get(ROW).eqs.isEmpty(), "d = e.worksIn becomes reflexive and is dropped");
      assertEquals(att(LABEL, fk(WORKS_IN, gen("e"))), q.atts.get(OUT).l);
    }

    @Test
    void removesATypeVariableEqualToATerm() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(ROW, from("e", EMP).andType("x").where(sk("x"), att(NAME, gen("e"))))
              .attribute(OUT, sk("x"))
              .make(Fixtures.SRC, Fixtures.ROWS);
      assertTrue(q.ens.get(ROW).sks.isEmpty());
      assertEquals(att(NAME, gen("e")), q.atts.get(OUT).l);
    }

    @Test
    void keepsTheVariablesWhenQueryRemoveRedundancyIsOff() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(ROW, from("e", EMP).and("d", DEPT).where(gen("d"), fk(WORKS_IN, gen("e"))))
              .attribute(OUT, att(LABEL, gen("d")))
              .options(new AqlOptions(ops(), AqlOption.query_remove_redundancy, false))
              .make(Fixtures.SRC, Fixtures.ROWS);
      assertEquals(Map.of("e", EMP, "d", DEPT), q.ens.get(ROW).gens);
    }

    @Test
    void cannotRemoveVariablesFromAQueryWithAggregates() {
      Builder b =
          new Builder()
              .entity(ROW, from("e", EMP).and("d", DEPT).where(gen("d"), fk(WORKS_IN, gen("e"))))
              .aggregate(OUT, agg());
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> b.make(Fixtures.SRC, Fixtures.ROWS));
      assertEquals("Elim Redundant", e.getMessage());
    }
  }

  @Nested
  class Accessors {

    @Test
    void isAQuery() {
      assertEquals(Kind.QUERY, rowQuery().kind());
    }

    @Test
    void sizeIsTheSizeOfTheSourceSchema() {
      assertEquals(Fixtures.SRC.size(), rowQuery().size());
    }

    @Test
    void hasAggIsFalseWithoutAggregates() {
      assertFalse(rowQuery().hasAgg());
    }

    @Test
    void hasAggIsTrueWithAnAggregate() {
      assertTrue(aggQuery().hasAgg());
    }

    @Test
    void fksAndAttsOfWhereCollectsEverySymbolInTheWhereClauses() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(
                  ROW,
                  from("e", EMP)
                      .and("d", DEPT)
                      .where(fk(WORKS_IN, gen("e")), gen("d"))
                      .where(att(NAME, gen("e")), att(LABEL, gen("d"))))
              .attribute(OUT, att(NAME, gen("e")))
              .build(Fixtures.SRC, Fixtures.ROWS);
      Pair<Collection<Fk>, Collection<Att>> p = q.fksAndAttsOfWhere();
      assertEquals(Set.of(WORKS_IN), new HashSet<>(p.first));
      assertEquals(Set.of(NAME, LABEL), new HashSet<>(p.second));
    }

    @Test
    void fksAndAttsOfWhereIgnoresTheReturnClauses() {
      Pair<Collection<Fk>, Collection<Att>> p = rowQuery().fksAndAttsOfWhere();
      assertTrue(p.first.isEmpty());
      assertTrue(p.second.isEmpty());
    }
  }

  @Nested
  class ToString {

    @Test
    void rendersEachBlock() {
      String s = rowQuery().toString();
      assertTrue(s.startsWith("entity Row -> {"), s);
      assertTrue(s.contains("e:Employee"), s);
      assertTrue(s.contains("attributes"), s);
      assertTrue(s.contains("out -> e.name"), s);
      assertFalse(s.contains("foreign_keys"), s);
    }

    @Test
    void rendersForeignKeyClauses() {
      String s = peopleQuery().toString();
      assertTrue(s.contains("entity Person"), s);
      assertTrue(s.contains("entity Unit"), s);
      assertTrue(s.contains("foreign_keys"), s);
      assertTrue(s.contains("employer -> {"), s);
    }

    @Test
    void omitsParamsAndBindingsWhenThereAreNone() {
      String s = rowQuery().toString();
      assertFalse(s.contains("params"), s);
      assertFalse(s.contains("bindings"), s);
    }

    @Test
    void rendersParamsAndBindings() {
      String s = rowBuilder().param("p").bind("p", "x").build(Fixtures.SRC, Fixtures.ROWS).toString();
      assertTrue(s.startsWith("params p:Varchar\n\nbindings p:"), s);
    }

    @Test
    void rendersAggregates() {
      String s = aggQuery().toString();
      assertTrue(s.contains("aggregate"), s);
      assertTrue(s.contains("lambda a b. a"), s);
    }
  }

  @Nested
  class EqualsAndHashCode {

    @Test
    void isReflexive() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = peopleQuery();
      assertTrue(q.equals(q));
    }

    @Test
    void rejectsNullAndForeignTypes() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      assertFalse(q.equals(null));
      assertFalse(q.equals("not a query"));
    }

    @Test
    void identicalQueriesWithoutForeignKeysAreEqualOnceHashed() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> a = hashed(rowQuery());
      Query<String, String, Sym, Fk, Att, String, Fk, Att> b = hashed(rowQuery());
      assertEquals(a.hashCode(), b.hashCode());
      assertEquals(a, b);
    }

    @Test
    void equalsThrowsWhenTheArgumentsBlocksHaveNotBuiltTheirCollage() {
      // Instance.equals compares the lazily built collage field without building it first, and a
      // block with no where clause never builds it during construction. THashMap.equals calls
      // equals on the argument's values, so it is the argument's collage that is dereferenced.
      Query<String, String, Sym, Fk, Att, String, Fk, Att> a = hashed(rowQuery());
      Query<String, String, Sym, Fk, Att, String, Fk, Att> b = rowQuery();
      assertThrows(NullPointerException.class, () -> a.equals(b));
    }

    @Test
    void identicalQueriesAreUnequalWhenOnlyTheArgumentIsHashed() {
      // The argument's collage exists but the receiver's is still null, so the blocks disagree.
      Query<String, String, Sym, Fk, Att, String, Fk, Att> a = rowQuery();
      Query<String, String, Sym, Fk, Att, String, Fk, Att> b = hashed(rowQuery());
      assertFalse(a.equals(b));
      a.hashCode();
      assertTrue(a.equals(b));
    }

    @Test
    void distinguishesOnTheReturnClauses() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> other =
          new Builder()
              .entity(ROW, from("e", EMP))
              .attribute(OUT, str("x"))
              .build(Fixtures.SRC, Fixtures.ROWS);
      assertNotEquals(hashed(rowQuery()), hashed(other));
    }

    @Test
    void distinguishesOnTheBindings() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> other =
          rowBuilder().bind("p", "x").build(Fixtures.SRC, Fixtures.ROWS);
      assertNotEquals(hashed(rowQuery()), hashed(other));
    }

    @Test
    void distinguishesOnTheTargetSchema() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> other =
          rowBuilder().entity(BLANK, fromNothing()).build(Fixtures.SRC, Fixtures.BLANKS);
      assertNotEquals(hashed(rowQuery()), hashed(other));
    }

    @Test
    void distinguishesOnTheBlocks() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> other =
          rowBuilder()
              .entity(ROW, from("e", EMP).where(att(NAME, gen("e")), str("x")))
              .build(Fixtures.SRC, Fixtures.ROWS);
      assertNotEquals(hashed(rowQuery()), hashed(other));
    }

    @Test
    void identicalQueriesWithForeignKeysAreNeverEqual() {
      // Transform.equals compares gens() and sks(), which for a LiteralTransform are the lambdas
      // the Query constructor creates, so two separately built queries never agree on fks.
      assertNotEquals(hashed(peopleQuery()), hashed(peopleQuery()));
    }

    /** Forces every block to build its collage; see equalsThrowsWhenTheArguments... above. */
    private Query<String, String, Sym, Fk, Att, String, Fk, Att> hashed(
        Query<String, String, Sym, Fk, Att, String, Fk, Att> q) {
      q.hashCode();
      return q;
    }
  }

  @Nested
  class Sql {

    @Test
    void toSqlSelectsTheRowIdOfEachFromVariable() {
      assertEquals(
          Map.of(ROW, " select e.`rowid` as e from `Employee` as e"),
          rowQuery().toSQL("`", false));
    }

    @Test
    void toSqlRendersTheWhereClause() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(ROW, from("e", EMP).and("d", DEPT).where(fk(WORKS_IN, gen("e")), gen("d")))
              .attribute(OUT, att(NAME, gen("e")))
              .build(Fixtures.SRC, Fixtures.ROWS);
      String sql = q.toSQL("", false).get(ROW);
      assertTrue(sql.startsWith(" select e.rowid as e, d.rowid as d from Employee as e, Department as d where "), sql);
      assertTrue(sql.endsWith(" = d.rowid"), sql);
    }

    @Test
    void toSqlReportsAnEmptyFromClause() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(ROW, fromNothing())
              .attribute(OUT, str("x"))
              .build(Fixtures.SRC, Fixtures.ROWS);
      assertEquals(Map.of(ROW, EMPTY_FROM), q.toSQL("", false));
    }

    @Test
    void toSqlIsComputedOnceAndIgnoresLaterArguments() {
      // The result is cached in a field on the first call; tick and truncate are not part of the
      // cache key, so a second call with different arguments returns the first call's SQL.
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      Map<String, String> first = q.toSQL("`", false);
      Map<String, String> second = q.toSQL("\"", true);
      assertSame(first, second);
      assertTrue(second.get(ROW).contains("`Employee`"), second.get(ROW));
    }

    @Test
    void toSql2SelectsTheReturnClauses() {
      assertEquals(
          Map.of(ROW, "\nselect e.name as out\nfrom Employee as e"), rowQuery().toSQL2());
    }

    @Test
    void toSql2RendersTheWhereClause() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          rowBuilder()
              .entity(ROW, from("e", EMP).where(att(NAME, gen("e")), str("x")))
              .build(Fixtures.SRC, Fixtures.ROWS);
      String sql = q.toSQL2().get(ROW);
      assertTrue(sql.startsWith("\nselect e.name as out\nfrom Employee as e\n\t where "), sql);
    }

    @Test
    void toSql2StopsAtTheFirstEntityWithAnEmptyFromClause() {
      // Entities are visited alphabetically and the loop returns as soon as it meets an empty from
      // clause, so Row, which sorts after Blank, is missing from the result.
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          rowBuilder().entity(BLANK, fromNothing()).build(Fixtures.SRC, Fixtures.BLANKS);
      assertEquals(Map.of(BLANK, EMPTY_FROM), q.toSQL2());
    }

    @Test
    void toSql2IsRecomputedOnEachCall() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      Map<String, String> first = q.toSQL2();
      Map<String, String> second = q.toSQL2();
      assertNotSame(first, second, "toSQL2 does not cache");
      assertEquals(first, second);
    }
  }

  @Nested
  class MakeSelect {

    @Test
    void exposesEveryAttributeOfEveryFromVariable() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(ROW, from("e", EMP).and("d", DEPT).where(fk(WORKS_IN, gen("e")), gen("d")))
              .attribute(OUT, att(NAME, gen("e")))
              .build(Fixtures.SRC, Fixtures.ROWS);
      Query<String, String, Sym, Fk, Att, String, Fk, Att> s = Query.makeSelect(ops(), q);
      Att eName = Att.Att(ROW, "e_name");
      Att dLabel = Att.Att(ROW, "d_label");
      assertEquals(Set.of(eName, dLabel), s.dst.atts.keySet());
      assertEquals(new Pair<>(ROW, TY), s.dst.atts.get(eName));
      assertEquals(att(NAME, gen("e")), s.atts.get(eName).l);
      assertEquals(att(LABEL, gen("d")), s.atts.get(dLabel).l);
    }

    @Test
    void keepsTheFromAndWhereClauses() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(ROW, from("e", EMP).and("d", DEPT).where(fk(WORKS_IN, gen("e")), gen("d")))
              .attribute(OUT, att(NAME, gen("e")))
              .build(Fixtures.SRC, Fixtures.ROWS);
      Query<String, String, Sym, Fk, Att, String, Fk, Att> s = Query.makeSelect(ops(), q);
      assertEquals(q.ens.get(ROW).gens, s.ens.get(ROW).gens);
      assertEquals(q.ens.get(ROW).eqs, s.ens.get(ROW).eqs);
    }

    @Test
    void dropsTheForeignKeys() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> s =
          Query.makeSelect(ops(), Query.id(ops(), Fixtures.SRC, Fixtures.SRC));
      assertEquals(Set.of(EMP, DEPT, CO), s.dst.ens);
      assertTrue(s.dst.fks.isEmpty(), "the select-star schema has no foreign keys");
      assertTrue(s.fks.isEmpty());
      assertEquals(att(NAME, gen("v")), s.atts.get(Att.Att(EMP, "v_name")).l);
    }
  }

  /**
   * unnest repeatedly finds an application u.f.g (or u.f.a) with u a from variable, adds a fresh
   * from variable v with the where clause v = u.f, and replaces u.f by v.
   */
  @Nested
  class Unnest {

    @Test
    void leavesAFlatQueryAlone() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> u = rowQuery().unnest();
      assertEquals(Map.of("e", EMP), u.ens.get(ROW).gens);
      assertTrue(u.ens.get(ROW).eqs.isEmpty());
      assertEquals(att(NAME, gen("e")), u.atts.get(OUT).l);
    }

    @Test
    void returnsANewQuery() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      assertNotSame(q, q.unnest());
    }

    @Test
    void unfoldsAnAttributeOfAForeignKeyInAReturnClause() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> u =
          new Builder()
              .entity(ROW, from("e", EMP))
              .attribute(OUT, att(LABEL, fk(WORKS_IN, gen("e"))))
              .build(Fixtures.SRC, Fixtures.ROWS)
              .unnest();
      assertEquals(Map.of("e", EMP, "unfold0", DEPT), u.ens.get(ROW).gens);
      assertEquals(Set.of(eq(gen("unfold0"), fk(WORKS_IN, gen("e")))), u.ens.get(ROW).eqs);
      assertEquals(att(LABEL, gen("unfold0")), u.atts.get(OUT).l);
    }

    @Test
    void unfoldsAWhereClauseOneStepAtATime() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> u =
          rowBuilder()
              .entity(
                  ROW,
                  from("e", EMP)
                      .where(att(TITLE, fk(PART_OF, fk(WORKS_IN, gen("e")))), att(NAME, gen("e"))))
              .build(Fixtures.SRC, Fixtures.ROWS)
              .unnest();
      assertEquals(Map.of("e", EMP, "unfold0", DEPT, "unfold1", CO), u.ens.get(ROW).gens);
      assertEquals(
          Set.of(
              eq(att(TITLE, gen("unfold1")), att(NAME, gen("e"))),
              eq(gen("unfold0"), fk(WORKS_IN, gen("e"))),
              eq(gen("unfold1"), fk(PART_OF, gen("unfold0")))),
          u.ens.get(ROW).eqs);
    }

    @Test
    void onlyUnfoldsReturnClausesOneLevelDeep() {
      // A return clause is matched only at its root, so e.worksIn.partOf.title, whose root is
      // three applications deep, is left as it is. The where clause, by contrast, is searched
      // through every subterm (see unfoldsAWhereClauseOneStepAtATime).
      Term<String, String, Sym, Fk, Att, String, String> deep =
          att(TITLE, fk(PART_OF, fk(WORKS_IN, gen("e"))));
      Query<String, String, Sym, Fk, Att, String, Fk, Att> u =
          new Builder()
              .entity(ROW, from("e", EMP))
              .attribute(OUT, deep)
              .build(Fixtures.SRC, Fixtures.ROWS)
              .unnest();
      assertEquals(Map.of("e", EMP), u.ens.get(ROW).gens);
      assertEquals(deep, u.atts.get(OUT).l);
    }

    @Test
    void unfoldsAForeignKeyClauseAndRewritesItsImage() {
      // Person: from e:Employee; Unit: from c:Company; employer: c -> e.worksIn.partOf.
      Query<String, String, Sym, Fk, Att, String, Fk, Att> u =
          new Builder()
              .entity(PERSON, from("e", EMP))
              .entity(UNIT, from("c", CO))
              .attribute(PNAME, att(NAME, gen("e")))
              .attribute(UNAME, att(TITLE, gen("c")))
              .foreignKey(EMPLOYER, "c", path("e", WORKS_IN, PART_OF))
              .build(Fixtures.SRC, Fixtures.PEOPLE)
              .unnest();
      assertEquals(Map.of("e", EMP, "unfold0", DEPT), u.ens.get(PERSON).gens);
      assertEquals(Set.of(eq(gen("unfold0"), fk(WORKS_IN, gen("e")))), u.ens.get(PERSON).eqs);
      assertEquals(Map.of("c", CO), u.ens.get(UNIT).gens);
      assertEquals(path("unfold0", PART_OF), u.fks.get(EMPLOYER).gens().apply("c", CO));
    }

    @Test
    void propagatesAnUnfoldingAlongTheForeignKeysIntoTheBlock() {
      // Unfolding d.partOf in the Unit block adds a variable unfold0 that the employer clause must
      // also map; its image e.worksIn.partOf is itself nested, so the Person block is unfolded
      // next. The reflexive where clause becomes unfold0.title = unfold0.title and is dropped.
      Term<String, String, Sym, Fk, Att, String, String> title =
          att(TITLE, fk(PART_OF, gen("d")));
      Query<String, String, Sym, Fk, Att, String, Fk, Att> u =
          peopleBuilder()
              .entity(UNIT, from("d", DEPT).where(title, title))
              .build(Fixtures.SRC, Fixtures.PEOPLE)
              .unnest();
      assertEquals(Map.of("d", DEPT, "unfold0", CO), u.ens.get(UNIT).gens);
      assertEquals(Set.of(eq(gen("unfold0"), fk(PART_OF, gen("d")))), u.ens.get(UNIT).eqs);
      assertEquals(Map.of("e", EMP, "unfold1", DEPT), u.ens.get(PERSON).gens);
      assertEquals(Set.of(eq(gen("unfold1"), fk(WORKS_IN, gen("e")))), u.ens.get(PERSON).eqs);
      var employer = u.fks.get(EMPLOYER);
      assertEquals(path("unfold1"), employer.gens().apply("d", DEPT));
      assertEquals(path("unfold1", PART_OF), employer.gens().apply("unfold0", CO));
    }

    @Test
    void keepsTypeSortedFromVariables() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> u =
          new Builder()
              .entity(ROW, from("e", EMP).andType("x"))
              .attribute(OUT, sk("x"))
              .build(Fixtures.SRC, Fixtures.ROWS)
              .unnest();
      assertEquals(Map.of("e", EMP), u.ens.get(ROW).gens);
      assertEquals(Map.of("x", TY), u.ens.get(ROW).sks);
    }

    @Test
    void keepsTheImagesOfTypeSortedVariablesUnderForeignKeys() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> u =
          peopleBuilder()
              .entity(UNIT, from("d", DEPT).andType("x"))
              .foreignKeySkolem(EMPLOYER, "x", att(NAME, gen("e")))
              .build(Fixtures.SRC, Fixtures.PEOPLE)
              .unnest();
      assertEquals(att(NAME, gen("e")), u.fks.get(EMPLOYER).sks().apply("x", TY));
    }

    @Test
    void rejectsAggregates() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> aggQuery().unnest());
      assertEquals("Unnesting not yet supported with aggregation.", e.getMessage());
    }

    @Test
    void rejectsAggregatesInABlockWhoseWhereClauseNeedsUnfolding() {
      // The where clause is searched before the return clauses, so the nested path is found first
      // and the aggregate is only noticed while rewriting, with a different message.
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(ROW, from("e", EMP).where(att(LABEL, fk(WORKS_IN, gen("e"))), str("x")))
              .aggregate(OUT, agg())
              .build(Fixtures.SRC, Fixtures.ROWS);
      RuntimeException e = assertThrows(RuntimeException.class, q::unnest);
      assertEquals("Elim Nested", e.getMessage());
    }

    @Test
    void doesNotConvergeOnASelfLoopWhoseClauseIsAPath() {
      // Step: from n:Node; next: n -> n.parent.parent. Unfolding n.parent in the Step block adds a
      // variable that next must also map, and since next is a loop its image is again a path of
      // length two, so every round recreates the pattern it removed.
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(STEP, from("n", NODE))
              .foreignKey(NEXT, "n", path("n", PARENT, PARENT))
              .build(Fixtures.TREE, Fixtures.LOOP);
      RuntimeException e = assertThrows(RuntimeException.class, q::unnest);
      assertTrue(
          e.getMessage().startsWith("No convergence after 128 iterations."), e.getMessage());
    }
  }

  @Nested
  class DeParam {

    private Query<String, String, Sym, Fk, Att, String, Fk, Att> parameterized() {
      return rowBuilder()
          .param("p")
          .bind("p", "Alice")
          .entity(ROW, from("e", EMP).where(att(NAME, gen("e")), sk("p")))
          .build(Fixtures.SRC, Fixtures.ROWS);
    }

    @Test
    void substitutesTheBindingsIntoTheWhereClauses() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> d = parameterized().deParam();
      assertEquals(Set.of(eq(att(NAME, gen("e")), str("Alice"))), d.ens.get(ROW).eqs);
    }

    @Test
    void substitutesTheBindingsIntoTheReturnClauses() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> d =
          new Builder()
              .param("p")
              .bind("p", "Alice")
              .entity(ROW, from("e", EMP))
              .attribute(OUT, sk("p"))
              .build(Fixtures.SRC, Fixtures.ROWS)
              .deParam();
      assertEquals(str("Alice"), d.atts.get(OUT).l);
    }

    @Test
    void dropsTheParamsAndBindings() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> d = parameterized().deParam();
      assertTrue(d.params.isEmpty());
      assertTrue(d.consts.isEmpty());
    }

    @Test
    void keepsTheParameterAsATypeSortedFromVariable() {
      // The block's skolems, parameters included, are copied into the new from clause, so p
      // survives as an unused type-sorted variable after its occurrences are substituted.
      Query<String, String, Sym, Fk, Att, String, Fk, Att> d = parameterized().deParam();
      assertEquals(Map.of("e", EMP), d.ens.get(ROW).gens);
      assertEquals(Map.of("p", TY), d.ens.get(ROW).sks);
    }

    @Test
    void rejectsAggregates() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> aggQuery().deParam());
      assertEquals("Anomaly: please report", e.getMessage());
    }
  }

  @Nested
  class Freeze {

    private final Sym pairSym = Sym.Sym("pair", new Pair<>(List.of(TY, TY), TY));

    private Term<String, String, Sym, Fk, Att, String, String> v(String name) {
      return Term.Var(name);
    }

    private Term<String, String, Sym, Fk, Att, String, String> pair(
        Term<String, String, Sym, Fk, Att, String, String> l,
        Term<String, String, Sym, Fk, Att, String, String> r) {
      return Term.Sym(pairSym, List.of(l, r));
    }

    @Test
    void turnsEntityVariablesIntoGenerators() {
      assertEquals(gen("x"), Query.freeze(v("x"), Map.of(), Set.of()));
    }

    @Test
    void turnsParametersIntoSkolems() {
      assertEquals(sk("x"), Query.freeze(v("x"), Map.of("x", TY), Set.of()));
    }

    @Test
    void turnsTypeVariablesIntoSkolems() {
      assertEquals(sk("x"), Query.freeze(v("x"), Map.of(), Set.of("x")));
    }

    @Test
    void freezesUnderApplications() {
      assertEquals(
          att(NAME, fk(WORKS_IN, gen("x"))),
          Query.freeze(Term.Att(NAME, Term.Fk(WORKS_IN, v("x"))), Map.of(), Set.of()));
    }

    @Test
    void leavesClosedTermsAlone() {
      assertEquals(str("x"), Query.freeze(str("x"), Map.of(), Set.of()));
    }

    @Test
    void freezeAggFreezesTermsWithoutTheLambdaVariables() {
      assertEquals(
          pair(gen("x"), sk("y")),
          Query.freezeAgg(pair(v("x"), v("y")), Map.of("y", TY), Set.of(), "a", "b"));
    }

    @Test
    void freezeAggTreatsTypeVariablesAsSkolems() {
      assertEquals(sk("x"), Query.freezeAgg(v("x"), Map.of(), Set.of("x"), "a", "b"));
    }

    @Test
    void freezeAggLeavesTheLambdaVariablesAsVariables() {
      assertEquals(pair(v("a"), v("b")), Query.freezeAgg(pair(v("a"), v("b")), Map.of(), Set.of(), "a", "b"));
    }

    @Test
    void freezeAggFreezesNothingOnceALambdaVariableOccurs() {
      // The loop returns the original term as soon as it meets a or b, so x is not frozen even
      // though it is neither a lambda variable nor a parameter.
      Term<String, String, Sym, Fk, Att, String, String> t = pair(v("a"), v("x"));
      assertEquals(t, Query.freezeAgg(t, Map.of(), Set.of(), "a", "b"));
    }
  }

  @Nested
  class Aggregates {

    @Test
    void toFrozenExtendsTheBlockWithTheLocalFromAndWhere() {
      Frozen<String, String, Sym, Fk, Att> block = rowQuery().ens.get(ROW);
      Frozen<String, String, Sym, Fk, Att> f =
          agg().toFrozen(Collections.emptyMap(), Fixtures.SRC, ops(), block);
      assertEquals(Map.of("e", EMP, "d", DEPT), f.gens);
      assertEquals(Set.of(eq(gen("d"), fk(WORKS_IN, gen("e")))), f.eqs);
      assertEquals(List.of("e", "d"), f.order);
    }

    @Test
    void toFrozenDoesNotChangeTheBlock() {
      Frozen<String, String, Sym, Fk, Att> block = rowQuery().ens.get(ROW);
      agg().toFrozen(Collections.emptyMap(), Fixtures.SRC, ops(), block);
      assertEquals(Map.of("e", EMP), block.gens);
      assertTrue(block.eqs.isEmpty());
    }

    @Test
    void rendersEveryClause() {
      String s = agg().toString();
      assertTrue(s.startsWith("from d:Department\n\twhere d=e.worksIn\n\treturn d.label"), s);
      assertTrue(s.contains("\n\taggregate "), s);
      assertTrue(s.endsWith("\n\tlambda a b. a"), s);
    }

    @Test
    void equalAggregatesAgreeOnHashCode() {
      assertEquals(agg(), agg());
      assertEquals(agg().hashCode(), agg().hashCode());
    }

    @Test
    void equalsIsReflexiveAndRejectsNullAndForeignTypes() {
      Agg<String, String, Sym, Fk, Att> a = agg();
      assertTrue(a.equals(a));
      assertFalse(a.equals(null));
      assertFalse(a.equals("not an aggregate"));
    }

    @Test
    void allNullAggregatesAreEqual() {
      Agg<String, String, Sym, Fk, Att> a = new Agg<>(null, null, null, null, null, null);
      Agg<String, String, Sym, Fk, Att> b = new Agg<>(null, null, null, null, null, null);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void distinguishesOnEveryField() {
      Agg<String, String, Sym, Fk, Att> a = agg();
      List<Agg<String, String, Sym, Fk, Att>> others =
          List.of(
              new Agg<>(str("x"), a.op, a.lgens, a.leqs, a.ret, a.ctx),
              new Agg<>(a.zero, Term.Var("b"), a.lgens, a.leqs, a.ret, a.ctx),
              new Agg<>(a.zero, a.op, Map.of("d2", DEPT), a.leqs, a.ret, a.ctx),
              new Agg<>(a.zero, a.op, a.lgens, Set.of(), a.ret, a.ctx),
              new Agg<>(a.zero, a.op, a.lgens, a.leqs, att(NAME, gen("e")), a.ctx),
              new Agg<>(a.zero, a.op, a.lgens, a.leqs, a.ret, new Pair<>("b", "a")));
      for (Agg<String, String, Sym, Fk, Att> other : others) {
        assertNotEquals(a, other);
        assertNotEquals(other, a);
      }
    }

    @Test
    void distinguishesANullFieldFromAPresentOne() {
      Agg<String, String, Sym, Fk, Att> a = agg();
      List<Agg<String, String, Sym, Fk, Att>> others =
          List.of(
              new Agg<>(null, a.op, a.lgens, a.leqs, a.ret, a.ctx),
              new Agg<>(a.zero, null, a.lgens, a.leqs, a.ret, a.ctx),
              new Agg<>(a.zero, a.op, null, a.leqs, a.ret, a.ctx),
              new Agg<>(a.zero, a.op, a.lgens, null, a.ret, a.ctx),
              new Agg<>(a.zero, a.op, a.lgens, a.leqs, null, a.ctx),
              new Agg<>(a.zero, a.op, a.lgens, a.leqs, a.ret, null));
      for (Agg<String, String, Sym, Fk, Att> other : others) {
        assertNotEquals(a, other);
        assertNotEquals(other, a);
      }
    }
  }

  @Nested
  class Transforms {

    @Test
    void attMapsTheTypeBlockToTheReturnClause() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      var t = q.att(OUT);
      assertSame(q.tys.get(TY), t.src());
      assertSame(q.ens.get(ROW), t.dst());
      assertEquals(att(NAME, gen("e")), t.sks().apply("_y_", TY));
    }

    @Test
    void attRejectsAggregates() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> aggQuery().att(OUT));
      assertEquals("Cannot create transforms with aggregation.", e.getMessage());
    }

    @Test
    void transPListsTheForeignKeysOfAPathOutermostFirst() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      Term<Void, String, Void, Fk, Void, String, Void> p =
          Term.Fk(PART_OF, Term.Fk(WORKS_IN, Term.Var("v")));
      assertEquals(List.of(PART_OF, WORKS_IN), q.transP(p));
    }

    @Test
    void transPOfAVariableOrGeneratorIsEmpty() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      assertTrue(q.transP(Term.<Void, String, Void, Fk, Void, String, Void>Var("v")).isEmpty());
      assertTrue(q.transP(path("v")).isEmpty());
    }

    @Test
    void composeOfTheEmptyPathIsTheIdentityOnTheBlock() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = peopleQuery();
      var t = q.compose(List.of(), PERSON);
      assertSame(q.ens.get(PERSON), t.src());
      assertSame(q.ens.get(PERSON), t.dst());
    }

    @Test
    void composeTOfAnAttributeFollowsTheReturnClause() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      var t = q.composeT(Term.Att(OUT, Term.Var("v")), ROW);
      assertEquals(att(NAME, gen("e")), t.sks().apply("_y_", TY));
    }

    @Test
    void composeTOfAConstantIsTheConstant() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      var t = q.composeT(str("x"), ROW);
      assertSame(q.tys.get(TY), t.src());
      assertSame(q.ens.get(ROW), t.dst());
      assertEquals(str("x"), t.sks().apply("_y_", TY));
    }

    @Test
    void composeTOfAFunctionComposesItsArguments() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      Sym not = Sym.Sym("not", new Pair<>(List.of(BOOL), BOOL));
      Term<String, String, Sym, Fk, Att, String, String> yes = Term.Obj(Optional.of(true), BOOL);
      var t = q.composeT(Term.Sym(not, List.of(yes)), ROW);
      assertSame(q.tys.get(BOOL), t.src());
      assertEquals(Term.Sym(not, List.of(yes)), t.sks().apply("_y_", BOOL));
    }

    @Test
    void composeTOfATypeVariableIsTheIdentityOnItsType() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q =
          new Builder()
              .entity(ROW, from("e", EMP).andType("x"))
              .attribute(OUT, sk("x"))
              .build(Fixtures.SRC, Fixtures.ROWS);
      var byVar = q.composeT(Term.Var("x"), ROW);
      var bySk = q.composeT(sk("x"), ROW);
      assertSame(q.tys.get(TY), byVar.src());
      assertSame(q.tys.get(TY), byVar.dst());
      assertSame(q.tys.get(TY), bySk.src());
      assertSame(q.tys.get(TY), bySk.dst());
    }

    @Test
    void composeTRejectsAGenerator() {
      Query<String, String, Sym, Fk, Att, String, Fk, Att> q = rowQuery();
      RuntimeException e = assertThrows(RuntimeException.class, () -> q.composeT(gen("g"), ROW));
      assertEquals("Anomaly: please report", e.getMessage());
    }
  }
}
