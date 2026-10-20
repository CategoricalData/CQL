package catdata.cql;

import static catdata.cql.CqlTestHelpers.BOOLEAN;
import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.forall;
import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static catdata.cql.CqlTestHelpers.typedSqlSchema;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.cql.Collage.CCollage;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MappingTest {

  private static final String EMP = "Employee";
  private static final String DEPT = "Department";
  private static final Fk WORKS_IN = Fk.Fk(EMP, "worksIn");
  private static final Fk MANAGES = Fk.Fk(EMP, "manages");
  private static final Att NAME = Att.Att(EMP, "name");
  private static final Att LABEL = Att.Att(DEPT, "label");
  private static final Att ACTIVE = Att.Att(EMP, "active");
  private static final Att CODE = Att.Att(EMP, "code");

  private static final String PERSON = "Person";
  private static final String TEAM = "Team";
  private static final String DIVISION = "Division";
  private static final Fk MEMBER_OF = Fk.Fk(PERSON, "memberOf");
  private static final Fk PART_OF = Fk.Fk(TEAM, "partOf");
  private static final Fk LEADS = Fk.Fk(PERSON, "leads");
  private static final Att PNAME = Att.Att(PERSON, "pname");
  private static final Att DNAME = Att.Att(DIVISION, "dname");

  private static final String ROW = "Row";
  private static final Att FIRST = Att.Att(ROW, "first");
  private static final Att SECOND = Att.Att(ROW, "second");

  private static final String LONELY = "Lonely";

  private static final Sym IS_NULL =Sym.Sym("isNull", new Pair<>(List.of(VARCHAR), BOOLEAN));

  /** The schemas, built once and shared. */
  private static final class Fixtures {

    /** Employee -worksIn-> Department, with a Varchar attribute on each. */
    private static final Schema<String, String, Sym, Fk, Att> SRC =
        sqlSchema(
            List.of(EMP, DEPT),
            Map.of(NAME, EMP, LABEL, DEPT),
            Map.of(WORKS_IN, new Pair<>(EMP, DEPT)));

    /** SRC plus Employee -manages-> Department and forall e:Employee. e.worksIn = e.manages. */
    private static final Schema<String, String, Sym, Fk, Att> SRC_EQ =
        sqlSchema(
            List.of(EMP, DEPT),
            Map.of(NAME, EMP, LABEL, DEPT),
            Map.of(WORKS_IN, new Pair<>(EMP, DEPT), MANAGES, new Pair<>(EMP, DEPT)),
            List.of(forall("e", EMP, Term.Fk(WORKS_IN, Term.Var("e")), Term.Fk(MANAGES, Term.Var("e")))));

    /** A single Employee with a Boolean and a Varchar attribute. */
    private static final Schema<String, String, Sym, Fk, Att> FLAGS =
        typedSqlSchema(
            List.of(EMP),
            Map.of(ACTIVE, new Pair<>(EMP, BOOLEAN), CODE, new Pair<>(EMP, VARCHAR)),
            Map.of(),
            List.of());

    /** Person -memberOf-> Team -partOf-> Division and Person -leads-> Division. */
    private static final Schema<String, String, Sym, Fk, Att> MID =
        sqlSchema(
            List.of(PERSON, TEAM, DIVISION),
            Map.of(PNAME, PERSON, DNAME, DIVISION),
            Map.of(
                MEMBER_OF, new Pair<>(PERSON, TEAM),
                PART_OF, new Pair<>(TEAM, DIVISION),
                LEADS, new Pair<>(PERSON, DIVISION)));

    /** MID plus forall p:Person. p.memberOf.partOf = p.leads. */
    private static final Schema<String, String, Sym, Fk, Att> MID_EQ =
        sqlSchema(
            List.of(PERSON, TEAM, DIVISION),
            Map.of(PNAME, PERSON, DNAME, DIVISION),
            Map.of(
                MEMBER_OF, new Pair<>(PERSON, TEAM),
                PART_OF, new Pair<>(TEAM, DIVISION),
                LEADS, new Pair<>(PERSON, DIVISION)),
            List.of(
                forall(
                    "p",
                    PERSON,
                    Term.Fk(PART_OF, Term.Fk(MEMBER_OF, Term.Var("p"))),
                    Term.Fk(LEADS, Term.Var("p")))));

    /** A single entity with two Varchar attributes. */
    private static final Schema<String, String, Sym, Fk, Att> FLAT =
        sqlSchema(List.of(ROW), Map.of(FIRST, ROW, SECOND, ROW), Map.of());

    /** A single entity with no attributes and no foreign keys. */
    private static final Schema<String, String, Sym, Fk, Att> LONE =
        sqlSchema(List.of(LONELY), Map.of(), Map.of());
  }

  private static Term<String, String, Sym, Fk, Att, Void, Void> v(String name) {
    return Term.Var(name);
  }

  private static Term<String, String, Sym, Fk, Att, Void, Void> fk(
      Fk f, Term<String, String, Sym, Fk, Att, Void, Void> t) {
    return Term.Fk(f, t);
  }

  private static Term<String, String, Sym, Fk, Att, Void, Void> att(
      Att a, Term<String, String, Sym, Fk, Att, Void, Void> t) {
    return Term.Att(a, t);
  }

  private static Term<String, String, Sym, Fk, Att, Void, Void> str(String s) {
    return Term.Obj(Optional.of(s), VARCHAR);
  }

  /** Assembles the maps the {@link Mapping} constructor takes. */
  private static final class Builder {
    private final Map<String, String> ens = new HashMap<>();
    private final Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> atts =
        new HashMap<>();
    private final Map<Fk, Pair<String, List<Fk>>> fks = new HashMap<>();

    Builder entity(String from, String to) {
      ens.put(from, to);
      return this;
    }

    /** Maps a to lambda v:en. t. */
    Builder attribute(Att a, String en, Term<String, String, Sym, Fk, Att, Void, Void> t) {
      atts.put(a, new Triple<>("v", en, t));
      return this;
    }

    /** Maps f to the path in the target starting at en; no path is the identity at en. */
    Builder foreignKey(Fk f, String en, Fk... path) {
      fks.put(f, new Pair<>(en, List.of(path)));
      return this;
    }

    Builder forgetEntity(String en) {
      ens.remove(en);
      return this;
    }

    Builder forgetAttribute(Att a) {
      atts.remove(a);
      return this;
    }

    Builder forgetForeignKey(Fk f) {
      fks.remove(f);
      return this;
    }

    Mapping<String, String, Sym, Fk, Att, String, Fk, Att> build(
        Schema<String, String, Sym, Fk, Att> src, Schema<String, String, Sym, Fk, Att> dst) {
      return new Mapping<>(ens, atts, fks, src, dst, false);
    }

    Mapping<String, String, Sym, Fk, Att, String, Fk, Att> buildWithoutEquations(
        Schema<String, String, Sym, Fk, Att> src, Schema<String, String, Sym, Fk, Att> dst) {
      return new Mapping<>(ens, atts, fks, src, dst, true);
    }
  }

  /**
   * F : SRC -> MID. Employee -> Person, Department -> Division, worksIn -> memberOf.partOf, name ->
   * lambda v:Person. v.pname, label -> lambda v:Division. v.dname.
   */
  private static Builder fBuilder() {
    return new Builder()
        .entity(EMP, PERSON)
        .entity(DEPT, DIVISION)
        .foreignKey(WORKS_IN, PERSON, MEMBER_OF, PART_OF)
        .attribute(NAME, PERSON, att(PNAME, v("v")))
        .attribute(LABEL, DIVISION, att(DNAME, v("v")));
  }

  private static Mapping<String, String, Sym, Fk, Att, String, Fk, Att> f() {
    return fBuilder().build(Fixtures.SRC, Fixtures.MID);
  }

  /** F with name -> lambda v:Person. v.leads.dname, an attribute lambda through a foreign key. */
  private static Mapping<String, String, Sym, Fk, Att, String, Fk, Att> fViaLeads() {
    return fBuilder()
        .attribute(NAME, PERSON, att(DNAME, fk(LEADS, v("v"))))
        .build(Fixtures.SRC, Fixtures.MID);
  }

  /** G : MID -> FLAT. Everything goes to Row, every foreign key to the identity. */
  private static Mapping<String, String, Sym, Fk, Att, String, Fk, Att> g() {
    return new Builder()
        .entity(PERSON, ROW)
        .entity(TEAM, ROW)
        .entity(DIVISION, ROW)
        .foreignKey(MEMBER_OF, ROW)
        .foreignKey(PART_OF, ROW)
        .foreignKey(LEADS, ROW)
        .attribute(PNAME, ROW, att(FIRST, v("v")))
        .attribute(DNAME, ROW, att(SECOND, v("v")))
        .build(Fixtures.MID, Fixtures.FLAT);
  }

  /** K : FLAT -> FLAT, swapping the two attributes. */
  private static Mapping<String, String, Sym, Fk, Att, String, Fk, Att> swap() {
    return new Builder()
        .entity(ROW, ROW)
        .attribute(FIRST, ROW, att(SECOND, v("v")))
        .attribute(SECOND, ROW, att(FIRST, v("v")))
        .build(Fixtures.FLAT, Fixtures.FLAT);
  }

  /** SRC -> FLAT directly, with name and label sent to the given attributes. */
  private static Mapping<String, String, Sym, Fk, Att, String, Fk, Att> flat(
      Att nameTo, Att labelTo) {
    return new Builder()
        .entity(EMP, ROW)
        .entity(DEPT, ROW)
        .foreignKey(WORKS_IN, ROW)
        .attribute(NAME, ROW, att(nameTo, v("v")))
        .attribute(LABEL, ROW, att(labelTo, v("v")))
        .build(Fixtures.SRC, Fixtures.FLAT);
  }

  @Nested
  class Construction {

    @Test
    void keepsTheMapsItIsGiven() {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> m = f();
      assertEquals(Map.of(EMP, PERSON, DEPT, DIVISION), m.ens);
      assertEquals(new Pair<>(PERSON, List.of(MEMBER_OF, PART_OF)), m.fks.get(WORKS_IN));
      assertEquals(new Triple<>("v", PERSON, att(PNAME, v("v"))), m.atts.get(NAME));
      assertSame(Fixtures.SRC, m.src);
      assertSame(Fixtures.MID, m.dst);
    }

    @Test
    void isAMapping() {
      assertEquals(Kind.MAPPING, f().kind());
    }

    @Test
    void sizeIsTheSizeOfTheSourceSchema() {
      assertEquals(Fixtures.SRC.size(), f().size());
    }
  }

  @Nested
  class Identity {

    @Test
    void sendsEverySymbolToItself() {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> id = Mapping.id(Fixtures.SRC);
      assertEquals(Map.of(EMP, EMP, DEPT, DEPT), id.ens);
      assertEquals(new Pair<>(EMP, List.of(WORKS_IN)), id.fks.get(WORKS_IN));
      assertEquals(new Triple<>("v", EMP, att(NAME, v("v"))), id.atts.get(NAME));
      assertSame(Fixtures.SRC, id.dst);
    }

    @Test
    void canIncludeASchemaIntoALargerOne() {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> id =
          Mapping.id(Fixtures.SRC, Optional.of(Fixtures.SRC_EQ));
      assertSame(Fixtures.SRC, id.src);
      assertSame(Fixtures.SRC_EQ, id.dst);
    }

    @Test
    void anEmptyTargetMeansTheSourceItself() {
      assertSame(Fixtures.SRC, Mapping.id(Fixtures.SRC, Optional.empty()).dst);
    }
  }

  @Nested
  class Validation {

    private String failure(Builder b, Schema<String, String, Sym, Fk, Att> dst) {
      return assertThrows(RuntimeException.class, () -> b.build(Fixtures.SRC, dst)).getMessage();
    }

    @Test
    void acceptsAWellFormedMapping() {
      assertDoesNotThrow(MappingTest::f);
    }

    @Test
    void rejectsAnUnmappedEntity() {
      assertEquals(
          "source entity Department has no mapping",
          failure(fBuilder().forgetEntity(DEPT), Fixtures.MID));
    }

    @Test
    void rejectsAnEntityImageOutsideTheTarget() {
      assertEquals(
          "source entity Employee maps to Ghost, which is not in target",
          failure(fBuilder().entity(EMP, "Ghost"), Fixtures.MID));
    }

    @Test
    void rejectsAnUnmappedAttribute() {
      assertEquals(
          "source attribute name has no mapping",
          failure(fBuilder().forgetAttribute(NAME), Fixtures.MID));
    }

    @Test
    void rejectsAnAttributeLambdaWithoutASort() {
      assertEquals(
          "in mapping for attribute name, not given a sort for v",
          failure(fBuilder().attribute(NAME, null, att(PNAME, v("v"))), Fixtures.MID));
    }

    @Test
    void rejectsAnAttributeLambdaOverTheWrongEntity() {
      assertEquals(
          "in mapping for attribute name the given sort for v is Division but it is expected to be"
              + " Person",
          failure(fBuilder().attribute(NAME, DIVISION, att(DNAME, v("v"))), Fixtures.MID));
    }

    @Test
    void rejectsAnAttributeImageOfTheWrongType() {
      // v.memberOf is a Team, not a Varchar.
      String msg = failure(fBuilder().attribute(NAME, PERSON, fk(MEMBER_OF, v("v"))), Fixtures.MID);
      assertTrue(msg.startsWith("source attribute name goes to target observation "), msg);
      assertTrue(msg.endsWith(", which has type Team, not Varchar as expected"), msg);
    }

    @Test
    void rejectsAnUnmappedForeignKey() {
      assertEquals(
          "source foreign key worksIn : Employee -> Department has no mapping",
          failure(fBuilder().forgetForeignKey(WORKS_IN), Fixtures.MID));
    }

    @Test
    void rejectsAPathThatStartsAtTheWrongEntity() {
      assertEquals(
          "proposed source of foreign key mapping for worksIn is Team and not Person as expected",
          failure(fBuilder().foreignKey(WORKS_IN, TEAM, PART_OF), Fixtures.MID));
    }

    @Test
    void rejectsAPathThatEndsAtTheWrongEntity() {
      assertEquals(
          "source foreign key worksIn maps to target path memberOf, which has target entity Team,"
              + " not Division as expected",
          failure(fBuilder().foreignKey(WORKS_IN, PERSON, MEMBER_OF), Fixtures.MID));
    }

    @Test
    void rejectsAMappingForAnUnknownEntity() {
      assertEquals(
          "there is a mapping for Ghost which is not a source entity",
          failure(fBuilder().entity("Ghost", PERSON), Fixtures.MID));
    }

    @Test
    void rejectsAMappingForAnUnknownAttribute() {
      assertEquals(
          "there is a mapping for ghost which is not a source attribute",
          failure(
              fBuilder().attribute(Att.Att(EMP, "ghost"), PERSON, att(PNAME, v("v"))),
              Fixtures.MID));
    }

    @Test
    void rejectsAMappingForAnUnknownForeignKey() {
      assertEquals(
          "there is a mapping for ghost which is not a source foreign key",
          failure(fBuilder().foreignKey(Fk.Fk(EMP, "ghost"), PERSON, LEADS), Fixtures.MID));
    }
  }

  /** A mapping must send every equation of its source to one provable in its target. */
  @Nested
  class Equations {

    /** F extended with manages -> leads, so worksIn = manages becomes memberOf.partOf = leads. */
    private Builder withManages() {
      return fBuilder().foreignKey(MANAGES, PERSON, LEADS);
    }

    @Test
    void rejectsASourceEquationTheTargetDoesNotProve() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class, () -> withManages().build(Fixtures.SRC_EQ, Fixtures.MID));
      String msg = e.getMessage();
      assertTrue(msg.startsWith("Source schema equation e.worksIn = e.manages translates to"), msg);
      assertTrue(msg.contains("translates to e.memberOf.partOf = e.leads, which is not provable"), msg);
    }

    @Test
    void acceptsItWhenTheTargetProvesIt() {
      assertDoesNotThrow(() -> withManages().build(Fixtures.SRC_EQ, Fixtures.MID_EQ));
    }

    @Test
    void skipsTheCheckWhenAsked() {
      assertDoesNotThrow(() -> withManages().buildWithoutEquations(Fixtures.SRC_EQ, Fixtures.MID));
    }
  }

  @Nested
  class Translation {

    @Test
    void sendsAForeignKeyToItsPathAppliedInOrder() {
      assertEquals(fk(PART_OF, fk(MEMBER_OF, v("e"))), f().trans(fk(WORKS_IN, v("e"))));
    }

    @Test
    void substitutesTheArgumentIntoTheAttributeLambda() {
      assertEquals(
          att(DNAME, fk(PART_OF, fk(MEMBER_OF, v("e")))),
          f().trans(att(LABEL, fk(WORKS_IN, v("e")))));
    }

    @Test
    void translatesUnderFunctionSymbols() {
      assertEquals(
          Term.Sym(IS_NULL, List.of(att(PNAME, v("e")))),
          f().trans(Term.Sym(IS_NULL, List.of(att(NAME, v("e"))))));
    }

    @Test
    void substitutesThroughAnAttributeLambdaThatUsesAForeignKey() {
      assertEquals(att(DNAME, fk(LEADS, v("e"))), fViaLeads().trans(att(NAME, v("e"))));
    }

    @Test
    void keepsVariablesAndConstants() {
      assertEquals(v("e"), f().trans(v("e")));
      assertEquals(str("k"), f().trans(str("k")));
    }

    @Test
    void keepsGeneratorsAndSkolems() {
      Term<String, String, Sym, Fk, Att, String, String> g = Term.Gen("g");
      Term<String, String, Sym, Fk, Att, String, String> s = Term.Sk("s");
      assertEquals(g, f().trans(g));
      assertEquals(s, f().trans(s));
      assertEquals(
          Term.Fk(PART_OF, Term.Fk(MEMBER_OF, g)), f().trans(Term.Fk(WORKS_IN, g)));
    }

    @Test
    void concatenatesTheImagesOfAPath() {
      assertEquals(List.of(MEMBER_OF, PART_OF), f().trans(List.of(WORKS_IN)));
      assertEquals(List.of(), g().trans(List.of(MEMBER_OF, PART_OF)));
    }

    @Test
    void mapsEntitySortedVariablesAndKeepsTypeSortedOnes() {
      Map<String, Chc<String, String>> ctx = Map.of("e", Chc.inRight(EMP), "x", Chc.inLeft(VARCHAR));
      assertEquals(
          Map.of("e", Chc.inRight(PERSON), "x", Chc.inLeft(VARCHAR)), new HashMap<>(f().trans(ctx)));
    }

    @Test
    void translationIsFunctorial() {
      // G(F(t)) = (F;G)(t) for a term that uses a foreign key and an attribute.
      Term<String, String, Sym, Fk, Att, Void, Void> t = att(LABEL, fk(WORKS_IN, v("e")));
      assertEquals(Mapping.compose(f(), g()).trans(t), g().trans(f().trans(t)));
      assertEquals(att(SECOND, v("e")), g().trans(f().trans(t)));
    }
  }

  @Nested
  class Semantics {

    @Test
    void isComputedOnce() {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> m = f();
      assertSame(m.semantics(), m.semantics());
    }

    @Test
    void runsBetweenTheCollagesOfTheTwoSchemas() {
      // Schema.collage() returns a fresh view on every call, so compare structural copies.
      var s = f().semantics();
      assertEquals(new CCollage<>(Fixtures.SRC.collage()), new CCollage<>(s.src()));
      assertEquals(new CCollage<>(Fixtures.MID.collage()), new CCollage<>(s.dst()));
    }

    @Test
    void translatesAContextAndATermTogether() {
      Map<String, Chc<String, String>> ctx = Map.of("e", Chc.inRight(EMP), "x", Chc.inLeft(VARCHAR));
      var p = f().semantics().translate(ctx, fk(WORKS_IN, v("e")));
      assertEquals(Map.of("e", Chc.inRight(PERSON), "x", Chc.inLeft(VARCHAR)), new HashMap<>(p.first));
      assertEquals(fk(PART_OF, fk(MEMBER_OF, v("e"))), p.second);
    }
  }

  /**
   * Mappings form a category: composition is associative and Mapping.id is a unit on both sides.
   * Equality here is Mapping.equals, which compares the three maps and the two schemas.
   */
  @Nested
  class Composition {

    @Test
    void composesEntitiesPathsAndAttributes() {
      assertEquals(flat(FIRST, SECOND), Mapping.compose(f(), g()));
    }

    @Test
    void identityIsALeftUnit() {
      assertEquals(f(), Mapping.compose(Mapping.id(Fixtures.SRC), f()));
    }

    @Test
    void identityIsARightUnit() {
      assertEquals(f(), Mapping.compose(f(), Mapping.id(Fixtures.MID)));
    }

    @Test
    void isAssociative() {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> left =
          Mapping.compose(Mapping.compose(f(), g()), swap());
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> right =
          Mapping.compose(f(), Mapping.compose(g(), swap()));
      assertEquals(left, right);
      assertEquals(flat(SECOND, FIRST), left);
    }

    @Test
    void substitutesThroughFunctionSymbolsAndConstants() {
      // FLAGS -> MID with active -> isNull(v.pname) and code -> "k", then G.
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> flags =
          new Builder()
              .entity(EMP, PERSON)
              .attribute(ACTIVE, PERSON, Term.Sym(IS_NULL, List.of(att(PNAME, v("v")))))
              .attribute(CODE, PERSON, str("k"))
              .build(Fixtures.FLAGS, Fixtures.MID);
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> c = Mapping.compose(flags, g());
      assertEquals(
          new Triple<>("v", ROW, Term.Sym(IS_NULL, List.of(att(FIRST, v("v"))))), c.atts.get(ACTIVE));
      assertEquals(new Triple<>("v", ROW, str("k")), c.atts.get(CODE));
    }

    @Test
    void substitutesThroughForeignKeysInAttributeLambdas() {
      // name -> v.leads.dname, then G sends leads to the identity and dname to second.
      assertEquals(
          new Triple<>("v", ROW, att(SECOND, v("v"))),
          Mapping.compose(fViaLeads(), g()).atts.get(NAME));
    }

    @Test
    void rejectsMappingsThatDoNotMeet() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> Mapping.compose(f(), f()));
      assertTrue(e.getMessage().startsWith("Anomaly, please report."), e.getMessage());
    }
  }

  /**
   * The collage of F : S -> T is the schema S + T with one extra foreign key m_e : e -> F(e) per
   * source entity e, and equations making m a natural transformation: x.f.m_w = x.m_v.F(f) for
   * f : v -> w, and x.a = F(a)[x.m_v] for every source attribute a on v.
   */
  @Nested
  class Collages {

    private Chc<String, String> left(String en) {
      return Chc.inLeft(en);
    }

    private Chc<String, String> right(String en) {
      return Chc.inRight(en);
    }

    private Chc<Chc<Fk, Fk>, String> srcFk(Fk f) {
      return Chc.inLeft(Chc.inLeft(f));
    }

    private Chc<Chc<Fk, Fk>, String> dstFk(Fk f) {
      return Chc.inLeft(Chc.inRight(f));
    }

    /** The component m_en : en -> F(en). */
    private Chc<Chc<Fk, Fk>, String> component(String en) {
      return Chc.inRight(en);
    }

    private Term<String, Chc<String, String>, Sym, Chc<Chc<Fk, Fk>, String>, Chc<Att, Att>, Void, Void> x(
        String name) {
      return Term.Var(name);
    }

    @Test
    void hasTheDisjointUnionOfTheEntities() {
      assertEquals(
          Set.of(left(EMP), left(DEPT), right(PERSON), right(TEAM), right(DIVISION)),
          new HashSet<>(f().collage().ens));
    }

    @Test
    void hasTheAttributesOfBothSchemas() {
      var c = f().collage();
      assertEquals(new Pair<>(left(EMP), VARCHAR), c.atts.get(Chc.inLeft(NAME)));
      assertEquals(new Pair<>(right(PERSON), VARCHAR), c.atts.get(Chc.inRight(PNAME)));
      assertEquals(4, c.atts.size());
    }

    @Test
    void hasTheForeignKeysOfBothSchemasAndOneComponentPerSourceEntity() {
      var c = f().collage();
      assertEquals(new Pair<>(left(EMP), left(DEPT)), c.fks.get(srcFk(WORKS_IN)));
      assertEquals(new Pair<>(right(PERSON), right(TEAM)), c.fks.get(dstFk(MEMBER_OF)));
      assertEquals(new Pair<>(left(EMP), right(PERSON)), c.fks.get(component(EMP)));
      assertEquals(new Pair<>(left(DEPT), right(DIVISION)), c.fks.get(component(DEPT)));
      assertEquals(1 + 3 + 2, c.fks.size());
    }

    @Test
    void makesTheComponentsNaturalInTheForeignKeys() {
      // x.worksIn.m_Department = x.m_Employee.memberOf.partOf
      var c = f().collage();
      assertTrue(
          new HashSet<>(c.eqs)
              .contains(
                  new Triple<>(
                      new Pair<>("x", left(EMP)),
                      Term.Fk(component(DEPT), Term.Fk(srcFk(WORKS_IN), x("x"))),
                      Term.Fk(
                          dstFk(PART_OF),
                          Term.Fk(dstFk(MEMBER_OF), Term.Fk(component(EMP), x("x")))))),
          c.eqs.toString());
    }

    @Test
    void makesTheComponentsNaturalInTheAttributes() {
      // v.name = v.m_Employee.pname
      var c = f().collage();
      assertTrue(
          new HashSet<>(c.eqs)
              .contains(
                  new Triple<>(
                      new Pair<>("v", left(EMP)),
                      Term.Att(Chc.inLeft(NAME), x("v")),
                      Term.Att(Chc.inRight(PNAME), Term.Fk(component(EMP), x("v"))))),
          c.eqs.toString());
    }

    @Test
    void relabelsTheForeignKeysInsideAttributeLambdas() {
      // v.name = v.m_Employee.leads.dname
      var c = fViaLeads().collage();
      assertTrue(
          new HashSet<>(c.eqs)
              .contains(
                  new Triple<>(
                      new Pair<>("v", left(EMP)),
                      Term.Att(Chc.inLeft(NAME), x("v")),
                      Term.Att(
                          Chc.inRight(DNAME),
                          Term.Fk(dstFk(LEADS), Term.Fk(component(EMP), x("v")))))),
          c.eqs.toString());
    }

    @Test
    void hasOneEquationPerForeignKeyAndAttributeWhenTheSchemasHaveNone() {
      assertEquals(1 + 2, f().collage().eqs.size());
    }

    @Test
    void carriesTheEquationsOfBothSchemas() {
      var c =
          fBuilder()
              .foreignKey(MANAGES, PERSON, LEADS)
              .build(Fixtures.SRC_EQ, Fixtures.MID_EQ)
              .collage();
      var eqs = new HashSet<>(c.eqs);
      assertTrue(
          eqs.contains(
              new Triple<>(
                  new Pair<>("e", left(EMP)),
                  Term.Fk(srcFk(WORKS_IN), x("e")),
                  Term.Fk(srcFk(MANAGES), x("e")))),
          c.eqs.toString());
      assertTrue(
          eqs.contains(
              new Triple<>(
                  new Pair<>("p", right(PERSON)),
                  Term.Fk(dstFk(PART_OF), Term.Fk(dstFk(MEMBER_OF), x("p"))),
                  Term.Fk(dstFk(LEADS), x("p")))),
          c.eqs.toString());
      assertEquals(2 + 2 + 2, c.eqs.size());
    }

    @Test
    void isComputedOnce() {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> m = f();
      assertSame(m.collage(), m.collage());
    }

    @Test
    void cannotDecideItsOwnEquations() {
      // The collage is built with a placeholder decision procedure that rejects every use.
      var dp = f().collage().dp;
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> dp.eq(Map.of(), x("x"), x("x")));
      assertEquals("Anomaly: please report", e.getMessage());
      assertThrows(RuntimeException.class, dp::toStringProver);
      assertFalse(dp.supportsTrivialityCheck());
    }
  }

  @Nested
  class ToString {

    @Test
    void rendersEachSourceEntityWithItsForeignKeysAndAttributes() {
      String s = f().toString();
      assertTrue(s.startsWith("entity\n\t"), s);
      assertTrue(s.contains("entity\n\tEmployee -> Person"), s);
      assertTrue(s.contains("foreign_keys\n\tworksIn -> memberOf.partOf"), s);
      assertTrue(s.contains("attributes\n\tname -> lambda v:Person. v.pname"), s);
      assertTrue(s.contains("entity\n\tDepartment -> Division\nattributes"), s);
    }

    @Test
    void rendersAnEmptyPathAsTheIdentity() {
      assertTrue(
          flat(FIRST, SECOND).toString().contains("worksIn -> identity Row"),
          flat(FIRST, SECOND).toString());
    }

    @Test
    void isComputedOnce() {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> m = f();
      assertSame(m.toString(), m.toString());
    }
  }

  @Nested
  class EqualsAndHashCode {

    @Test
    void separatelyBuiltCopiesAreEqual() {
      assertEquals(f(), f());
      assertEquals(f().hashCode(), f().hashCode());
    }

    @Test
    void isReflexiveAndRejectsNullAndForeignTypes() {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> m = f();
      assertTrue(m.equals(m));
      assertFalse(m.equals(null));
      assertFalse(m.equals("not a mapping"));
    }

    @Test
    void distinguishesOnTheAttributes() {
      assertNotEquals(flat(FIRST, SECOND), flat(SECOND, FIRST));
    }

    @Test
    void distinguishesOnTheForeignKeys() {
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> viaLeads =
          fBuilder().foreignKey(WORKS_IN, PERSON, LEADS).build(Fixtures.SRC, Fixtures.MID);
      assertNotEquals(f(), viaLeads);
    }

    @Test
    void distinguishesOnTheEntities() {
      // An entity with no attributes, so the attribute lambdas (compared first) agree.
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> toPerson =
          new Builder().entity(LONELY, PERSON).build(Fixtures.LONE, Fixtures.MID);
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> toTeam =
          new Builder().entity(LONELY, TEAM).build(Fixtures.LONE, Fixtures.MID);
      assertNotEquals(toPerson, toTeam);
    }

    @Test
    void distinguishesOnTheTarget() {
      assertNotEquals(
          Mapping.id(Fixtures.SRC), Mapping.id(Fixtures.SRC, Optional.of(Fixtures.SRC_EQ)));
    }
  }

  @Nested
  class Tptp {

    @Test
    void isTheTheoryOfTheTargetSchema() {
      assertEquals(Fixtures.MID.collage().tptp(), f().toTptp());
    }

    @Test
    void doesNotDependOnTheMapping() {
      // Two different mappings into the same target render the same TPTP.
      Mapping<String, String, Sym, Fk, Att, String, Fk, Att> viaLeads =
          fBuilder().foreignKey(WORKS_IN, PERSON, LEADS).build(Fixtures.SRC, Fixtures.MID);
      assertNotEquals(f(), viaLeads);
      assertEquals(f().toTptp(), viaLeads.toTptp());
    }
  }

  @Nested
  class CoqNames {

    @Test
    void namesTheArithmeticOperators() {
      assertEquals("plus", Mapping.coq("+"));
      assertEquals("times", Mapping.coq("*"));
      assertEquals("minus", Mapping.coq("-"));
      assertEquals("divide", Mapping.coq("/"));
    }

    @Test
    void keepsTheEmptyName() {
      assertEquals("", Mapping.coq(""));
    }

    @Test
    void dropsCharactersThatAreNotAlphanumeric() {
      assertEquals("Doubleprecision", Mapping.coq("Double precision"));
      assertEquals("Optionalk", Mapping.coq("Optional[k]"));
    }

    @Test
    void prefixesNamesThatDoNotStartWithALetter() {
      assertEquals("cql1x", Mapping.coq("1x"));
    }

    @Test
    void failsOnANameWithNoAlphanumericCharacters() {
      // The empty-name check runs before the non-alphanumeric characters are dropped, so "<"
      // becomes "" and the first-character test reads past its end.
      assertThrows(StringIndexOutOfBoundsException.class, () -> Mapping.coq("<"));
    }

    @Test
    void sendsDistinctNamesToTheSameIdentifier() {
      assertEquals(Mapping.coq("ab"), Mapping.coq("a_b"));
      assertEquals("cql5", Mapping.coq("5"));
      assertEquals("cql5", Mapping.coq("-5"));
    }

    @Test
    void rendersAContextAsAForallPrefix() {
      assertEquals("", Mapping.toCoq(Map.of()));
      Map<String, String> ctx = new LinkedHashMap<>();
      ctx.put("x", "A");
      ctx.put("y", "B");
      assertEquals("forall (x : A)(y : B), ", Mapping.toCoq(ctx));
      assertEquals("forall (x : Person), ", Mapping.toCoq("x", PERSON));
    }
  }

  /**
   * The Coq export of a mapping, over a small typeside with a binary function, a constant and one
   * equation, so that every kind of declaration appears once.
   */
  @Nested
  class CoqExport {

    private static final String STR = "Str";

    /** Decides equations by syntactic equality, so no prover is involved. */
    private static <En, F, A> DP<String, En, String, F, A, Void, Void> syntactic() {
      return new DP<>() {
        @Override
        public String toStringProver() {
          return "syntactic";
        }

        @Override
        public boolean eq(
            Map<String, Chc<String, En>> ctx,
            Term<String, En, String, F, A, Void, Void> lhs,
            Term<String, En, String, F, A, Void, Void> rhs) {
          return lhs.equals(rhs);
        }
      };
    }

    /** Str with concat : Str * Str -> Str, empty : Str, and forall x. concat(x, empty) = x. */
    private static TypeSide<String, String> typeSide() {
      Map<String, Pair<List<String>, String>> syms =
          Map.of("concat", new Pair<>(List.of(STR, STR), STR), "empty", new Pair<>(List.of(), STR));
      Term<String, Void, String, Void, Void, Void, Void> x = Term.Var("x");
      Term<String, Void, String, Void, Void, Void, Void> empty = Term.Sym("empty", List.of());
      AqlJs<String, String> js =
          new AqlJs<>(ops(), syms, Map.of(STR, "java.lang.String"), Map.of(STR, "x => x"), Map.of());
      return new TypeSide<>(
          Set.of(STR),
          syms,
          Set.of(new Triple<>(Map.of("x", STR), Term.Sym("concat", List.of(x, empty)), x)),
          js,
          syntactic(),
          ops());
    }

    /**
     * A -f-> B with a : A -> Str, b : B -> Str and forall x:A. x.a = x.f.b, sent into C -g-> C with
     * c : C -> Str and forall y:C. y.c = y.g.c by A, B -> C, f -> g, a -> v.c, b -> concat(v.c, k).
     */
    private Mapping<String, String, String, String, String, String, String, String> mapping(
        Term<String, String, String, String, String, Void, Void> constant) {
      TypeSide<String, String> ts = typeSide();
      Term<String, String, String, String, String, Void, Void> x = Term.Var("x");
      Term<String, String, String, String, String, Void, Void> y = Term.Var("y");
      Term<String, String, String, String, String, Void, Void> xa = Term.Att("a", x);
      Term<String, String, String, String, String, Void, Void> xfb = Term.Att("b", Term.Fk("f", x));
      Term<String, String, String, String, String, Void, Void> yc = Term.Att("c", y);
      Term<String, String, String, String, String, Void, Void> ygc = Term.Att("c", Term.Fk("g", y));
      Schema<String, String, String, String, String> src =
          new Schema<>(
              ts,
              Set.of("A", "B"),
              Map.of("a", new Pair<>("A", STR), "b", new Pair<>("B", STR)),
              Map.of("f", new Pair<>("A", "B")),
              Set.of(new Triple<>(new Pair<>("x", "A"), xa, xfb)),
              syntactic(),
              false);
      Schema<String, String, String, String, String> dst =
          new Schema<>(
              ts,
              Set.of("C"),
              Map.of("c", new Pair<>("C", STR)),
              Map.of("g", new Pair<>("C", "C")),
              Set.of(new Triple<>(new Pair<>("y", "C"), yc, ygc)),
              syntactic(),
              false);
      Term<String, String, String, String, String, Void, Void> vc = Term.Att("c", Term.Var("v"));
      return new Mapping<>(
          Map.of("A", "C", "B", "C"),
          Map.of(
              "a",
              new Triple<>("v", "C", vc),
              "b",
              new Triple<>("v", "C", Term.Sym("concat", List.of(vc, constant)))),
          Map.of("f", new Pair<>("C", List.of("g"))),
          src,
          dst,
          true);
    }

    @Test
    void declaresTheTypesideTheTargetTheTranslationAndTheSourceEquations() {
      String coq = mapping(Term.Obj("k", STR)).toCoq();
      List<String> lines =
          Arrays.stream(coq.split("\n")).filter(l -> !l.isEmpty()).collect(Collectors.toList());
      assertEquals(
          Set.of(
              "Parameter Str : Type.",
              "Parameter concat : Str * Str -> Str.",
              "Parameter empty :  Str.",
              "Axiom ax0 : forall (x : Str),  concat(x, empty) = x.",
              "Parameter k : Str.",
              "Parameter C : Set.",
              "Parameter C_g : C -> C.",
              "Parameter C_c : C -> Str.",
              "Axiom ax1 : forall (y : C),  C_c(y) = C_c(C_g(y)).",
              "Definition t_A_f : C -> C := fun x => C_g(x).",
              "Definition t_A_a : C -> Str := fun v => C_c(v).",
              "Definition t_B_b : C -> Str := fun v => concat(C_c(v), k).",
              "Conjecture conj2 : forall (x : C),  t_A_a(x) = t_B_b(t_A_f(x))."),
          new HashSet<>(lines),
          coq);
      assertEquals(13, lines.size(), "every declaration appears once: " + coq);
    }

    @Test
    void failsOnAConstantWithNoAlphanumericCharacters() {
      Mapping<String, String, String, String, String, String, String, String> m =
          mapping(Term.Obj("!", STR));
      assertThrows(StringIndexOutOfBoundsException.class, m::toCoq);
    }

    @Test
    void declaresAnOverloadedSymbolOncePerOverload() {
      // SqlTypeSide overloads + across its numeric types; Coq names drop the argument types, so
      // the same Parameter is declared several times, which Coq rejects.
      long plus =
          Arrays.stream(f().toCoq().split("\n")).filter(l -> l.startsWith("Parameter plus :")).count();
      assertEquals(9, plus, "one Parameter plus per numeric type that SqlTypeSide.syms() declares");
    }
  }
}
