package catdata.cql;

import static catdata.cql.CqlTestHelpers.sqlSchema;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TermTest {

  private static final String INT = "Int";
  private static final String STR = "Str";
  private static final String VARCHAR = "Varchar";
  private static final String EMP = "Emp";
  private static final String DEPT = "Dept";

  /** Int and Str, in that order; only Str has a java binding. */
  private static final Set<String> TYS = new LinkedHashSet<>(List.of(INT, STR));

  private static final Map<String, String> JAVA_TYS = Map.of(STR, "java.lang.String");

  /** zero : Int, plus : Int, Int -> Int, len : Str -> Int, and real : Real, whose type is undeclared. */
  private static final Map<String, Pair<List<String>, String>> SYMS = new LinkedHashMap<>();

  private static final List<String> ENS = List.of(EMP, DEPT);

  /** name : Emp -> Str and age : Emp -> Int, in that order. */
  private static final Map<String, Pair<String, String>> ATTS = new LinkedHashMap<>();

  /** worksIn : Emp -> Dept and owner : Dept -> Office, whose target is undeclared. */
  private static final Map<String, Pair<String, String>> FKS = new LinkedHashMap<>();

  /** e : Emp and d : Dept, in that order. */
  private static final Map<String, String> GENS = new LinkedHashMap<>();

  private static final Map<String, String> SKS = Map.of("y", INT);

  static {
    SYMS.put("zero", new Pair<>(List.of(), INT));
    SYMS.put("plus", new Pair<>(List.of(INT, INT), INT));
    SYMS.put("len", new Pair<>(List.of(STR), INT));
    SYMS.put("real", new Pair<>(List.of(), "Real"));
    ATTS.put("name", new Pair<>(EMP, STR));
    ATTS.put("age", new Pair<>(EMP, INT));
    FKS.put("worksIn", new Pair<>(EMP, DEPT));
    FKS.put("owner", new Pair<>(DEPT, "Office"));
    GENS.put("e", EMP);
    GENS.put("d", DEPT);
  }

  private static Term<String, String, String, String, String, String, String> v(String x) {
    return Term.Var(x);
  }

  private static Term<String, String, String, String, String, String, String> gen(String g) {
    return Term.Gen(g);
  }

  private static Term<String, String, String, String, String, String, String> sk(String y) {
    return Term.Sk(y);
  }

  private static Term<String, String, String, String, String, String, String> obj(
      Object o, String ty) {
    return Term.Obj(o, ty);
  }

  @SafeVarargs
  private static Term<String, String, String, String, String, String, String> sym(
      String f, Term<String, String, String, String, String, String, String>... args) {
    return Term.Sym(f, List.of(args));
  }

  private static Term<String, String, String, String, String, String, String> fk(
      String f, Term<String, String, String, String, String, String, String> a) {
    return Term.Fk(f, a);
  }

  private static Term<String, String, String, String, String, String, String> att(
      String a, Term<String, String, String, String, String, String, String> x) {
    return Term.Att(a, x);
  }

  private static Chc<String, String> type(
      Term<String, String, String, String, String, String, String> t,
      Map<String, String> ctxt,
      Map<String, String> ctxe) {
    return t.type(ctxt, ctxe, TYS, SYMS, JAVA_TYS, ENS, ATTS, FKS, GENS, SKS);
  }

  private static Chc<String, String> type(
      Term<String, String, String, String, String, String, String> t) {
    return type(t, Map.of(), Map.of());
  }

  private static String typeError(Term<String, String, String, String, String, String, String> t) {
    return assertThrows(RuntimeException.class, () -> type(t)).getMessage();
  }

  @Nested
  class HashConsing {

    @Test
    void buildsEachTermOnce() {
      assertSame(v("x"), v("x"));
      assertSame(sym("plus", v("x"), sym("zero")), sym("plus", v("x"), sym("zero")));
      assertSame(fk("worksIn", gen("e")), fk("worksIn", gen("e")));
      assertSame(obj(5, INT), obj(5, INT));
    }

    @Test
    void comparesByIdentity() {
      Term<String, String, String, String, String, String, String> t = att("name", gen("e"));
      assertEquals(System.identityHashCode(t), t.hashCode());
      assertEquals(t, att("name", gen("e")));
      assertNotEquals(t, att("name", gen("d")));
    }

    @Test
    void keepsAConstantApartFromTheSameValueOfAnotherType() {
      assertNotEquals(obj(5, INT), obj(5, STR));
      assertFalse(obj(5, INT).equals2(obj(5, STR)));
    }

    @Test
    void equals2IsFalseForAnotherClass() {
      assertFalse(v("x").equals2("x"));
    }

    @Test
    void equals2ComparesTheArityOfASymbol() {
      assertFalse(sym("plus", v("x"), v("y")).equals2(sym("plus", v("x"))));
      assertFalse(sym("plus", v("x"), v("y")).equals2(sym("plus", v("x"), v("z"))));
    }

    @Test
    void rejectsANullConstant() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> obj(null, INT));
      assertEquals("Encountered null external value", e.getMessage());
    }

    @Test
    void buildsATermFromItsHead() {
      assertSame(gen("e"), Term.Head(Head.GenHead("e"), List.of()));
      assertSame(sk("y"), Term.Head(Head.SkHead("y"), List.of()));
      assertSame(obj(5, INT), Term.Head(Head.ObjHead(5, INT), List.of()));
      assertSame(att("name", gen("e")), Term.Head(Head.AttHead("name"), List.of(gen("e"))));
      assertSame(fk("worksIn", gen("e")), Term.Head(Head.FkHead("worksIn"), List.of(gen("e"))));
      assertSame(
          sym("plus", v("x"), v("y")), Term.Head(Head.SymHead("plus"), List.of(v("x"), v("y"))));
    }

    @Test
    void fksAppliesTheForeignKeysInOrder() {
      assertSame(
          fk("owner", fk("worksIn", gen("e"))), Term.Fks(List.of("worksIn", "owner"), gen("e")));
      assertSame(gen("e"), Term.Fks(List.of(), gen("e")));
    }

    @Test
    void castsAreIdentities() {
      Term<String, String, String, String, String, String, String> t = sym("len", v("x"));
      assertSame(t, t.convert());
      assertSame(t, t.mapEn());
      assertSame(t, t.toKB());
      assertSame(t, Term.fromKB(t));
      Term<String, Void, String, Void, Void, Void, String> talg = Term.Sk("y");
      assertSame(talg, Term.upTalg(talg));
    }
  }

  @Nested
  class Narrowing {

    @Test
    void asArgForAttKeepsGeneratorsVariablesAndForeignKeys() {
      assertSame(gen("e"), gen("e").asArgForAtt());
      assertSame(v("x"), v("x").asArgForAtt());
      assertSame(fk("worksIn", gen("e")), fk("worksIn", gen("e")).asArgForAtt());
    }

    @Test
    void asArgForAttRejectsALabelledNull() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> sk("y").asArgForAtt());
      assertEquals("Anomaly: please report y", e.getMessage());
    }

    @Test
    void asArgForFkKeepsForeignKeysGeneratorsAndVariables() {
      assertSame(fk("worksIn", v("x")), fk("worksIn", v("x")).asArgForFk());
      assertSame(gen("e"), gen("e").asArgForFk());
      assertSame(v("x"), v("x").asArgForFk());
    }

    @Test
    void asArgForFkRejectsASymbol() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> sym("zero").asArgForFk());
      assertEquals("Anomaly: please report zero", e.getMessage());
    }

    @Test
    void asGenAsSkAndAsObjKeepTheirKind() {
      assertSame(gen("e"), gen("e").asGen());
      assertSame(sk("y"), sk("y").asSk());
      assertSame(obj(5, INT), obj(5, INT).asObj());
    }

    @Test
    void asGenAsSkAndAsObjRejectOtherKinds() {
      assertEquals(
          "Anomaly: please report",
          assertThrows(RuntimeException.class, () -> sk("y").asGen()).getMessage());
      assertEquals(
          "Anomaly: please report",
          assertThrows(RuntimeException.class, () -> gen("e").asSk()).getMessage());
      assertEquals(
          "Anomaly: please report",
          assertThrows(RuntimeException.class, () -> gen("e").asObj()).getMessage());
    }
  }

  @Nested
  class Kinds {

    @Test
    void isTypeSideAcceptsVariablesConstantsAndSymbolsOverThem() {
      assertTrue(v("x").isTypeSide());
      assertTrue(obj(5, INT).isTypeSide());
      assertTrue(sym("plus", v("x"), obj(5, INT)).isTypeSide());
    }

    @Test
    void isTypeSideRejectsInstanceTerms() {
      assertFalse(gen("e").isTypeSide());
      assertFalse(att("name", v("x")).isTypeSide());
      assertFalse(sym("len", att("name", gen("e"))).isTypeSide());
    }

    @Test
    void hasTypeTypeWithAContextReadsTheVariableSort() {
      Map<String, Chc<String, String>> ctx = Map.of("i", Chc.inLeft(INT), "x", Chc.inRight(EMP));
      assertTrue(v("i").hasTypeType(ctx));
      assertFalse(v("x").hasTypeType(ctx));
      assertFalse(fk("worksIn", v("x")).hasTypeType(ctx));
      assertTrue(att("name", v("x")).hasTypeType(ctx));
      assertTrue(obj(5, INT).hasTypeType(ctx));
      assertTrue(sk("y").hasTypeType(ctx));
      assertTrue(sym("zero").hasTypeType(ctx));
      assertFalse(gen("e").hasTypeType(ctx));
    }

    @Test
    void hasTypeTypeSeparatesTypesFromEntities() {
      assertTrue(obj(5, INT).hasTypeType());
      assertTrue(sk("y").hasTypeType());
      assertTrue(sym("zero").hasTypeType());
      assertTrue(att("name", gen("e")).hasTypeType());
      assertFalse(gen("e").hasTypeType());
      assertFalse(fk("worksIn", gen("e")).hasTypeType());
    }

    @Test
    void hasTypeTypeRejectsAVariable() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> v("x").hasTypeType());
      assertEquals("Encountered variable: x in hasTypeType, please report.", e.getMessage());
    }

    @Test
    void monoidalExcludesGeneratorsAndAttributes() {
      assertTrue(obj(5, INT).monoidal(false));
      assertTrue(sk("y").monoidal(false));
      assertTrue(sym("zero").monoidal(false));
      assertFalse(gen("e").monoidal(true));
      assertFalse(att("name", v("x")).monoidal(true));
      assertFalse(fk("worksIn", gen("e")).monoidal(true));
    }

    @Test
    void monoidalReadsVariablesFromTheFlag() {
      assertTrue(v("x").monoidal(true));
      assertFalse(v("x").monoidal(false));
      assertTrue(fk("worksIn", v("x")).monoidal(true));
    }

    @Test
    void monoidalReadsVariablesFromTheContext() {
      Map<String, Chc<String, String>> ctx = Map.of("i", Chc.inLeft(INT), "x", Chc.inRight(EMP));
      assertTrue(v("i").monoidal(ctx));
      assertFalse(v("x").monoidal(ctx));
      assertFalse(fk("worksIn", v("x")).monoidal(ctx));
      assertTrue(obj(5, INT).monoidal(ctx));
      assertTrue(sk("y").monoidal(ctx));
      assertTrue(sym("zero").monoidal(ctx));
      assertFalse(gen("e").monoidal(ctx));
      assertFalse(att("name", v("x")).monoidal(ctx));
    }

    @Test
    void isGroundRejectsVariables() {
      assertFalse(v("x").isGround());
      assertFalse(sym("plus", v("x"), obj(5, INT)).isGround());
      assertFalse(fk("worksIn", v("x")).isGround());
    }

    @Test
    void isGroundAcceptsConstantsAndSymbolsOverThem() {
      assertTrue(obj(5, INT).isGround());
      assertTrue(sym("zero").isGround());
      assertTrue(sym("plus", obj(5, INT), sym("zero")).isGround());
    }

    @Test
    void isGroundFailsOnGeneratorsAndLabelledNulls() {
      assertThrows(NullPointerException.class, () -> gen("e").isGround());
      assertThrows(NullPointerException.class, () -> sk("y").isGround());
      assertThrows(NullPointerException.class, () -> att("name", gen("e")).isGround());
    }
  }

  @Nested
  class Typing {

    @Test
    void typesVariablesFromTheirContext() {
      assertEquals(Chc.inRight(EMP), type(v("x"), Map.of(), Map.of("x", EMP)));
      assertEquals(Chc.inLeft(INT), type(v("i"), Map.of("i", INT), Map.of()));
    }

    @Test
    void typesEachKindOfTerm() {
      assertEquals(Chc.inLeft(STR), type(obj("a", STR)));
      assertEquals(Chc.inLeft(INT), type(sym("plus", sym("zero"), sk("y"))));
      assertEquals(Chc.inLeft(INT), type(sym("len", obj("a", STR))));
      assertEquals(Chc.inLeft(STR), type(att("name", gen("e"))));
      assertEquals(Chc.inRight(DEPT), type(fk("worksIn", gen("e"))));
      assertEquals(Chc.inRight(DEPT), type(gen("d")));
      assertEquals(Chc.inLeft(INT), type(sk("y")));
      assertEquals(Chc.inLeft(INT), type(att("age", v("x")), Map.of(), Map.of("x", EMP)));
    }

    @Test
    void rejectsAVariableInBothContexts() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class, () -> type(v("x"), Map.of("x", INT), Map.of("x", EMP)));
      assertEquals("In x, name collision on x in {x=Int} and {x=Emp}", e.getMessage());
    }

    @Test
    void rejectsAnUnboundVariable() {
      assertEquals(
          "In z, z is not a variable in type context [{}] and entity context [{}]",
          typeError(v("z")));
    }

    @Test
    void rejectsAConstantOfAnUndeclaredType() {
      assertEquals("In a@Real, not a declared type: Real", typeError(obj("a", "Real")));
    }

    @Test
    void rejectsAConstantOfTheWrongJavaClass() {
      assertEquals(
          "In \"5\"@Str, primitive 5 is given type Str but is not an instance of class"
              + " java.lang.String, is an instance of class java.lang.Integer",
          typeError(obj(5, STR)));
    }

    @Test
    void rejectsAnUnknownSymbol() {
      assertTrue(
          typeError(sym("minus"))
              .startsWith("In minus, minus is not a typeside symbol.  Typeside symbols:\n\n"));
    }

    @Test
    void reportsTheArityOfASymbolWithoutASpace() {
      assertEquals(
          "In plus(zero), plus given 1arguments but requires 2", typeError(sym("plus", sym("zero"))));
    }

    @Test
    void rejectsASymbolArgumentOfTheWrongSort() {
      assertEquals(
          "In len(zero), Argument zero has sort Int but requires Str",
          typeError(sym("len", sym("zero"))));
    }

    @Test
    void rejectsAnUnknownAttribute() {
      assertEquals(
          "In e.salary, salary is not an attribute. Available: [name, age]",
          typeError(att("salary", gen("e"))));
    }

    @Test
    void rejectsAnAttributeOnTheWrongEntity() {
      assertEquals(
          "In d.name, argument d has sort Dept but requires Emp", typeError(att("name", gen("d"))));
    }

    @Test
    void rejectsAForeignKeyOnATypedTerm() {
      assertEquals(
          "In y.worksIn, y has type Int which is not an entity",
          typeError(fk("worksIn", sk("y"))));
    }

    @Test
    void rejectsAnUnknownForeignKey() {
      assertEquals(
          "In e.manages, manages is not a foreign key.  Possibilities: [worksIn, owner]",
          typeError(fk("manages", gen("e"))));
    }

    @Test
    void rejectsAForeignKeyOnTheWrongEntity() {
      assertEquals(
          "In d.worksIn, argument d has sort Dept but requires Emp",
          typeError(fk("worksIn", gen("d"))));
    }

    @Test
    void rejectsAnUnknownGenerator() {
      assertEquals(
          "In GEN g[class java.lang.String], the entity for generator g is not defined.  Types of"
              + " available generators are:\n{e=Emp, d=Dept}",
          typeError(gen("g")));
    }

    @Test
    void rejectsAnUnknownLabelledNull() {
      assertEquals(
          "In z, the labelled null z has no associated type.\n\nAvailable: y:Int",
          typeError(sk("z")));
    }

    @Test
    void doesNotListMoreThan1024LabelledNulls() {
      Map<String, String> many = new LinkedHashMap<>();
      for (int i = 0; i <= 1024; i++) {
        many.put("y" + i, INT);
      }
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  sk("z")
                      .type(Map.of(), Map.of(), TYS, SYMS, JAVA_TYS, ENS, ATTS, FKS, GENS, many));
      assertEquals(
          "In z, the labelled null z has no associated type.\n\nAvailable:  too big to print ",
          e.getMessage());
    }

    @Test
    void rejectsAResultTypeThatIsNotDeclared() {
      assertEquals(
          "In real, return type is Real which is not a type.  Available: Int, Str",
          typeError(sym("real")));
    }

    @Test
    void rejectsAResultEntityThatIsNotDeclared() {
      assertEquals(
          "In d.owner, return type is Office which is not a entity.    Available: Emp, Dept",
          typeError(fk("owner", gen("d"))));
    }
  }

  @Nested
  class Printing {

    @Test
    void printsVariablesSymbolsAndPaths() {
      assertEquals("x", v("x").toString());
      assertEquals("zero", sym("zero").toString());
      assertEquals("len(x)", sym("len", v("x")).toString());
      assertEquals("(x plus y)", sym("plus", v("x"), v("y")).toString());
      assertEquals("f(x, y, z)", sym("f", v("x"), v("y"), v("z")).toString());
      assertEquals("e.worksIn.owner", fk("owner", fk("worksIn", gen("e"))).toString());
      assertEquals("e.name", att("name", gen("e")).toString());
      assertEquals("y", sk("y").toString());
    }

    @Test
    void quotesNamesThatAreNotIdentifiers() {
      assertEquals("(x \"+\" y)", sym("+", v("x"), v("y")).toString());
      assertEquals("\"first name\"(x)", sym("first name", v("x")).toString());
      assertEquals("e.\"first name\"", att("first name", gen("e")).toString());
      assertEquals("e.\"works in\"", fk("works in", gen("e")).toString());
      assertEquals("\"1e\"", gen("1e").toString());
      assertEquals("\"1y\"", sk("1y").toString());
    }

    @Test
    void doesNotQuoteWhenAskedNotTo() {
      assertEquals("(x + y)", sym("+", v("x"), v("y")).toString(false));
      assertEquals("first name(x)", sym("first name", v("x")).toString(false));
      assertEquals("e.first name", att("first name", gen("e")).toString(false));
      assertEquals("1e", gen("1e").toString(false));
    }

    @Test
    void printsGeneratorsAndLabelledNullsThroughThePrinters() {
      assertEquals(
          "(#e plus ?y)",
          sym("plus", gen("e"), sk("y")).toString(y -> "?" + y, g -> "#" + g, false));
      assertEquals("\"#e\"", gen("e").toString(y -> "?" + y, g -> "#" + g, true));
    }

    @Test
    void printsJavaConstantsWithTheirType() {
      assertEquals("\"5\"@Int", obj(5, INT).toString());
      assertEquals("Ann@Str", obj("Ann", STR).toString());
      assertEquals("5", obj(5, INT).toString(false));
    }

    @Test
    void printsOptionalConstantsByTheirValue() {
      assertEquals("Ann", obj(Optional.of("Ann"), VARCHAR).toString());
      assertEquals("\"a b\"", obj(Optional.of("a b"), VARCHAR).toString());
      assertEquals("a b", obj(Optional.of("a b"), VARCHAR).toString(false));
      assertEquals("", obj(Optional.empty(), VARCHAR).toString());
    }

    @Test
    void quotesAnOptionalWithASpaceAndADoubleQuoteWithoutEscaping() {
      assertEquals("\"say \"hi\"\"", obj(Optional.of("say \"hi\""), VARCHAR).toString());
    }

    @Test
    void toStringUnambigNamesTheJavaClasses() {
      assertEquals("VAR x[class java.lang.String]", v("x").toStringUnambig());
      assertEquals(
          "SYM plus[class java.lang.String](VAR x[class java.lang.String], GEN e[class"
              + " java.lang.String])",
          sym("plus", v("x"), gen("e")).toStringUnambig());
      assertEquals("SK y[class java.lang.String]", sk("y").toStringUnambig());
      assertEquals("5@Int[class java.lang.Integer]", obj(5, INT).toStringUnambig());
    }

    @Test
    void toStringUnambigSwapsTheClassesOfAPath() {
      assertEquals(
          "ATT e[class java.lang.String].name[class catdata.cql.Term]",
          att("name", gen("e")).toStringUnambig());
      assertEquals(
          "FK e[class java.lang.String].worksIn[class catdata.cql.Term]",
          fk("worksIn", gen("e")).toStringUnambig());
    }

    @Test
    void tptpLowercasesNamesAndUppercasesVariables() {
      assertEquals("X", v("x").tptp());
      assertEquals("e1", gen("E1").tptp());
      assertEquals("y", sk("Y").tptp());
      assertEquals("ann", obj("Ann", STR).tptp());
      assertEquals("zero", sym("Zero").tptp());
      assertEquals("plus(X,Y)", sym("plus", v("x"), v("y")).tptp());
      assertEquals("owner(worksin(e))", fk("owner", fk("worksIn", gen("e"))).tptp());
      assertEquals("name(e)", att("name", gen("e")).tptp());
    }

    @Test
    void tptpGivesDistinctTermsTheSameName() {
      assertEquals(gen("A").tptp(), gen("a").tptp());
      assertEquals(v("x").tptp(), v("X").tptp());
    }

    @Test
    void tptpWritesAnOptionalByItsJavaToString() {
      assertEquals("optional[ann]", obj(Optional.of("Ann"), VARCHAR).tptp());
    }

    @Test
    void toTpTpForCheckerNamesVariablesGlobally() {
      String x = Term.sqlVar("x");
      assertEquals(x, Term.sqlVar("x"));
      assertNotEquals(x, Term.sqlVar("someOtherVariable"));
      assertEquals(x, v("x").toTpTpForChecker());
      assertEquals("zero", sym("Zero").toTpTpForChecker());
      assertEquals("plus(" + x + "," + x + ")", sym("plus", v("x"), v("x")).toTpTpForChecker());
      assertEquals(Schema.conv0("name") + "(" + x + ")", att("name", v("x")).toTpTpForChecker());
    }

    @Test
    void toTpTpForCheckerRejectsGeneratorsAndConstants() {
      assertThrows(RuntimeException.class, () -> gen("e").toTpTpForChecker());
      assertThrows(RuntimeException.class, () -> obj(5, INT).toTpTpForChecker());
    }

    @Test
    void toCoqPrefixesPathsAndDeclaresConstants() {
      Set<String> decls = new LinkedHashSet<>();
      Term<String, String, String, String, String, String, String> t =
          sym("plus", att("age", gen("e")), obj(5, INT));
      assertEquals("c_plus(p_c_Emp_c_age(c_e), c_5)", t.toCoq(s -> "c_" + s, decls, a -> EMP, f -> EMP, "p_"));
      assertEquals(Set.of("Parameter c_5 : c_Int."), decls);
    }

    @Test
    void toCoqOmitsTheEntityWithoutItsFunction() {
      Set<String> decls = new HashSet<>();
      assertEquals(
          "p_c_name(c_e)", att("name", gen("e")).toCoq(s -> "c_" + s, decls, null, null, "p_"));
      assertEquals(
          "p_c_worksIn(c_x)", fk("worksIn", v("x")).toCoq(s -> "c_" + s, decls, null, null, "p_"));
      assertEquals(
          "p_c_Emp_c_worksIn(c_x)",
          fk("worksIn", v("x")).toCoq(s -> "c_" + s, decls, null, f -> EMP, "p_"));
    }

    @Test
    void toCoqWritesConstantsByName() {
      Set<String> decls = new HashSet<>();
      assertEquals("c_zero", sym("zero").toCoq(s -> "c_" + s, decls, null, null, ""));
      assertEquals("c_y", sk("y").toCoq(s -> "c_" + s, decls, null, null, ""));
      assertEquals("c_d", gen("d").toCoq(s -> "c_" + s, decls, null, null, ""));
      assertTrue(decls.isEmpty());
    }
  }

  @Nested
  class Sql {

    /** A fresh schema, since truncation numbers names per schema instance. */
    private Schema<String, String, Sym, Fk, Att> schema() {
      return sqlSchema(List.of(EMP), Map.of(), Map.of());
    }

    private String sql(Term<String, String, String, String, String, String, String> t) {
      return t.toStringSql(schema(), "", false);
    }

    @Test
    void writesVariablesGeneratorsAndLabelledNullsByName() {
      assertEquals("x", sql(v("x")));
      assertEquals("e", sql(gen("e")));
      assertEquals("y", sql(sk("y")));
    }

    @Test
    void writesPathsWithTheQuote() {
      Schema<String, String, Sym, Fk, Att> s = schema();
      assertEquals(
          "x.\"worksIn\".\"name\"",
          att("name", fk("worksIn", v("x"))).toStringSql(s, "\"", false));
    }

    @Test
    void writesPathsWithTruncatedNames() {
      Schema<String, String, Sym, Fk, Att> s = schema();
      Term<String, String, String, String, String, String, String> t =
          att("name", fk("worksIn", v("x")));
      assertEquals("x.s0.s1", t.toStringSql(s, "", true));
      assertEquals("x.s0.s1", t.toStringSql(s, "", true));
    }

    @Test
    void writesSymbolsAsFunctionCalls() {
      assertEquals("zero", sql(sym("zero")));
      assertEquals("or(x, y)", sql(sym("or", v("x"), v("y"))));
      assertEquals("and(x)", sql(sym("and", v("x"))));
    }

    @Test
    void writesTwoArgumentSymbolsByTheirNamePrefix() {
      assertEquals("x and y", sql(sym("and", v("x"), v("y"))));
      assertEquals("x and y", sql(sym("andThen", v("x"), v("y"))));
      assertEquals("x = y", sql(sym("eq_Varchar", v("x"), v("y"))));
      assertEquals("null", sql(sym("NULL_Varchar", v("x"), v("y"))));
    }

    @Test
    void doesNotParenthesizeAConjunction() {
      assertEquals(
          "x and y = z", sql(sym("eq_Boolean", sym("and", v("x"), v("y")), v("z"))));
    }

    @Test
    void quotesEveryOptionalConstantAsAString() {
      assertEquals("'Ann'", sql(obj(Optional.of("Ann"), VARCHAR)));
      assertEquals("'true'", sql(obj(Optional.of(true), "Boolean")));
      assertEquals("'30'", sql(obj(Optional.of(30), "Integer")));
    }

    @Test
    void doesNotEscapeAQuoteInsideAConstant() {
      assertEquals("'O'Brien'", sql(obj(Optional.of("O'Brien"), VARCHAR)));
    }

    @Test
    void writesAnEmptyOptionalAsNothing() {
      assertEquals("", sql(obj(Optional.empty(), VARCHAR)));
    }

    @Test
    void writesOtherConstantsUnquoted() {
      assertEquals("Ann", sql(obj("Ann", STR)));
      assertEquals("5", sql(obj(5, INT)));
    }
  }

  @Nested
  class Traversal {

    @Test
    void visitFoldsOverEveryKindOfNode() {
      Term<String, String, String, String, String, String, String> t =
          sym("f", v("x"), obj(5, INT), att("name", fk("worksIn", gen("e"))), sk("y"));
      String folded =
          t.visit(
              x -> "v:" + x,
              (o, ty) -> "o:" + o + ":" + ty,
              (s, args) -> s + args,
              (f, a) -> f + "(" + a + ")",
              (a, x) -> a + "(" + x + ")",
              g -> "g:" + g,
              y -> "s:" + y);
      assertEquals("f[v:x, o:5:Int, name(worksIn(g:e)), s:y]", folded);
    }

    @Test
    void mapRenamesEveryKindOfName() {
      Term<String, String, String, String, String, String, String> t =
          sym("f", v("x"), obj(5, INT), att("name", fk("worksIn", gen("e"))), sk("y"));
      Term<String, String, String, String, String, String, String> expected =
          sym("F", v("x"), obj(5, "INT"), att("NAME", fk("WORKSIN", gen("E"))), sk("Y"));
      assertSame(
          expected,
          t.map(
              String::toUpperCase,
              String::toUpperCase,
              String::toUpperCase,
              String::toUpperCase,
              String::toUpperCase,
              String::toUpperCase));
    }

    @Test
    void mapFkMapAttMapGenAndMapGenSkRenameOneKind() {
      Term<String, String, String, String, String, String, String> t =
          sym("f", att("name", fk("worksIn", gen("e"))), sk("y"));
      assertSame(sym("f", att("name", fk("WI", gen("e"))), sk("y")), t.mapFk(f -> "WI"));
      assertSame(sym("f", att("N", fk("worksIn", gen("e"))), sk("y")), t.mapAtt(a -> "N"));
      assertSame(sym("f", att("name", fk("worksIn", gen("E"))), sk("y")), t.mapGen(g -> "E"));
      assertSame(
          sym("f", att("name", fk("worksIn", gen("E"))), sk("Y")),
          t.mapGenSk(g -> "E", y -> "Y"));
    }

    @Test
    void collectsTheNamesOfEachKind() {
      Term<String, String, String, String, String, String, String> t =
          sym("plus", att("age", fk("worksIn", gen("e"))), sym("len", sk("y")), v("x"));
      assertEquals(Set.of("e"), t.gens());
      assertEquals(Set.of("y"), t.sks());
      assertEquals(Set.of("worksIn"), t.fks());
      assertEquals(Set.of("age"), t.atts());
      assertEquals(Set.of("plus"), t.syms());
      assertEquals(Set.of("x"), t.getVars());
    }

    @Test
    void symsAndFksDoNotLookBelowAMatch() {
      assertEquals(Set.of("plus"), sym("plus", sym("len", v("x")), sym("zero")).syms());
      assertEquals(Set.of("owner"), fk("owner", fk("worksIn", gen("e"))).fks());
      Set<String> fks = new HashSet<>();
      att("name", fk("owner", fk("worksIn", v("x")))).fks(fks);
      assertEquals(Set.of("owner"), fks);
    }

    @Test
    void returnsUnmodifiableNameSets() {
      Term<String, String, String, String, String, String, String> t = fk("worksIn", gen("e"));
      assertThrows(UnsupportedOperationException.class, () -> t.gens().add("d"));
      assertThrows(UnsupportedOperationException.class, () -> t.fks().add("owner"));
      assertThrows(UnsupportedOperationException.class, () -> t.atts().add("name"));
      assertThrows(UnsupportedOperationException.class, () -> t.syms().add("zero"));
    }

    @Test
    void objsCollectsTheConstantsOnce() {
      Term<String, String, String, String, String, String, String> t =
          sym("plus", obj(5, INT), sym("len", obj("a", STR)), v("x"));
      Set<Pair<Object, String>> objs = t.objs();
      assertEquals(Set.of(new Pair<>(5, INT), new Pair<>("a", STR)), objs);
      assertSame(objs, t.objs());
    }

    @Test
    void objsAndGetVarsShareTheirCacheWithTheirCallers() {
      Term<String, String, String, String, String, String, String> t =
          sym("f", v("cacheA"), obj(17, INT));
      t.getVars().add("intruder");
      t.objs().add(new Pair<>("intruder", STR));
      Term<String, String, String, String, String, String, String> same =
          sym("f", v("cacheA"), obj(17, INT));
      assertTrue(same.getVars().contains("intruder"));
      assertTrue(same.vars().contains("intruder"));
      assertTrue(same.objs().contains(new Pair<>("intruder", STR)));
    }

    @Test
    void varsAreEmptyForAGroundTerm() {
      assertTrue(sym("plus", obj(5, INT), sk("y")).vars().isEmpty());
    }

    @Test
    void toFkListReadsAPathFromTheInside() {
      assertEquals(List.of("worksIn", "owner"), fk("owner", fk("worksIn", gen("e"))).toFkList());
      assertEquals(List.of("worksIn"), fk("worksIn", v("x")).toFkList());
      assertTrue(gen("e").toFkList().isEmpty());
      assertThrows(
          UnsupportedOperationException.class,
          () -> fk("worksIn", gen("e")).toFkList().add("owner"));
    }

    @Test
    void toFkListRejectsATermThatIsNotAPath() {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> att("name", gen("e")).toFkList());
      assertEquals("Anomaly: please report", e.getMessage());
    }

    @Test
    void getOnlyVarFindsTheSingleVariable() {
      assertEquals("x", v("x").getOnlyVar());
      assertEquals("x", sym("plus", v("x"), v("x")).getOnlyVar());
      assertEquals("x", sym("plus", obj(5, INT), v("x")).getOnlyVar());
      assertEquals("x", att("name", fk("worksIn", v("x"))).getOnlyVar());
    }

    @Test
    void getOnlyVarIsNullWithoutExactlyOneVariable() {
      assertNull(sym("plus", v("x"), v("y")).getOnlyVar());
      assertNull(sym("zero").getOnlyVar());
      assertNull(gen("e").getOnlyVar());
      assertNull(sk("y").getOnlyVar());
      assertNull(obj(5, INT).getOnlyVar());
    }

    @Test
    void containsLooksForAHeadAnywhere() {
      Term<String, String, String, String, String, String, String> t =
          sym("plus", fk("worksIn", gen("e")), sym("zero"));
      assertTrue(t.contains(Head.SymHead("plus")));
      assertTrue(t.contains(Head.SymHead("zero")));
      assertTrue(t.contains(Head.GenHead("e")));
      assertFalse(t.contains(Head.SymHead("len")));
      assertFalse(v("x").contains(Head.SymHead("plus")));
    }

    @Test
    void containsProperExcludesTheRoot() {
      Term<String, String, String, String, String, String, String> t = sym("plus", sym("zero"));
      assertFalse(t.containsProper(Head.SymHead("plus")));
      assertTrue(t.containsProper(Head.SymHead("zero")));
    }

    @Test
    void argsListsTheArgumentsOfEachKind() {
      Term<String, String, String, String, String, String, String> s = sym("plus", v("x"), v("y"));
      assertSame(s.args, s.args());
      assertEquals(List.of(gen("e")), fk("worksIn", gen("e")).args());
      assertTrue(gen("e").args().isEmpty());
      assertEquals(s.args(), s.getArgs());
    }

    @Test
    void forEachArgVisitsTheArguments() {
      List<Term<String, String, String, String, String, String, String>> seen = new ArrayList<>();
      sym("plus", v("x"), v("y")).forEachArg(a -> add(seen, a));
      fk("worksIn", gen("e")).forEachArg(a -> add(seen, a));
      gen("e").forEachArg(a -> add(seen, a));
      assertEquals(List.of(v("x"), v("y"), gen("e")), seen);
    }

    private Unit add(
        List<Term<String, String, String, String, String, String, String>> seen,
        Term<String, String, String, String, String, String, String> a) {
      seen.add(a);
      return Unit.unit;
    }

    @Test
    void exposesItsHeadAndVariable() {
      assertSame(Head.SymHead("zero"), sym("zero").f());
      assertNull(v("x").f());
      assertTrue(v("x").isVar());
      assertEquals("x", v("x").getVar());
      assertFalse(gen("e").isVar());
      assertNull(gen("e").getVar());
    }
  }

  @Nested
  class Rewriting {

    @Test
    void substReplacesVariables() {
      Map<String, Term<String, String, String, String, String, String, String>> map =
          Map.of("x", gen("e"), "i", obj(5, INT));
      assertSame(
          sym("plus", att("age", gen("e")), obj(5, INT)),
          sym("plus", att("age", v("x")), v("i")).subst(map));
      assertSame(fk("worksIn", gen("e")), fk("worksIn", v("x")).subst(map));
      assertSame(v("z"), v("z").subst(map));
      assertSame(obj(7, INT), obj(7, INT).subst(map));
    }

    @Test
    void substWithoutBindingsIsTheIdentity() {
      Term<String, String, String, String, String, String, String> t = sym("len", v("x"));
      assertSame(t, t.subst(null));
      assertSame(t, t.subst(Map.of()));
      assertSame(gen("e"), t.substitute(Map.of("x", gen("e"))).getArgs().get(0));
    }

    @Test
    void replaceSwapsWholeSubterms() {
      Term<String, String, String, String, String, String, String> t =
          sym("plus", att("age", fk("worksIn", gen("e"))), sym("zero"));
      assertSame(
          sym("plus", att("age", gen("d")), sym("zero")),
          t.replace(fk("worksIn", gen("e")), gen("d")));
      assertSame(obj(1, INT), t.replace(Map.of(t, obj(1, INT))));
    }

    @Test
    void replaceLeavesLeavesAlone() {
      Map<Term<String, String, String, String, String, String, String>, Term<String, String, String, String, String, String, String>>
          g = Map.of(gen("e"), gen("d"));
      assertSame(sym("zero"), sym("zero").replace(g));
      assertSame(obj(5, INT), obj(5, INT).replace(g));
      assertSame(sk("y"), sk("y").replace(g));
      assertSame(v("x"), v("x").replace(g));
      assertSame(gen("d"), gen("e").replace(g));
    }

    @Test
    void replaceHeadSubstitutesTheArgumentsIntoTheReplacement() {
      Term<String, String, String, String, String, String, String> swapped =
          sym("plus", v("b"), v("a"));
      assertSame(
          sym("plus", sym("zero"), v("x")),
          sym("plus", v("x"), sym("zero"))
              .replaceHead(Map.of(Head.SymHead("plus"), swapped), List.of("a", "b")));
      assertSame(
          fk("owner", gen("e")),
          fk("worksIn", gen("e"))
              .replaceHead(Map.of(Head.FkHead("worksIn"), fk("owner", v("p"))), List.of("p")));
    }

    @Test
    void replaceHeadWithoutVariablesReplacesLeaves() {
      assertSame(
          sym("plus", obj(5, INT), sym("zero")),
          sym("plus", sk("y"), sym("zero")).replaceHead(Map.of(Head.SkHead("y"), obj(5, INT)), null));
    }

    @Test
    void replaceHeadWithoutVariablesKeepsSymbolsForeignKeysAndAttributes() {
      assertSame(
          sym("len", v("x")),
          sym("len", v("x")).replaceHead(Map.of(Head.SymHead("len"), sym("zero")), null));
      assertSame(
          fk("worksIn", gen("e")),
          fk("worksIn", gen("e")).replaceHead(Map.of(Head.FkHead("worksIn"), gen("d")), null));
      assertSame(
          att("name", gen("e")),
          att("name", gen("e")).replaceHead(Map.of(Head.AttHead("name"), sk("y")), null));
    }

    @Test
    void replaceHeadKeepsVariables() {
      assertSame(v("x"), v("x").replaceHead(Map.of(Head.SymHead("plus"), sym("zero")), null));
      assertSame(v("x"), v("x").replaceHead(Head.SymHead("plus"), List.of(), sym("zero")));
    }

    @Test
    void replaceHeadOfOneHeadRewritesEachOccurrence() {
      Term<String, String, String, String, String, String, String> t =
          sym("plus", fk("worksIn", gen("e")), fk("worksIn", v("x")));
      assertSame(
          sym("plus", fk("owner", gen("e")), fk("owner", v("x"))),
          t.replaceHead(Head.FkHead("worksIn"), List.of("p"), fk("owner", v("p"))));
      assertSame(
          sym("plus", fk("worksIn", gen("d")), fk("worksIn", v("x"))),
          t.replaceHead(Head.GenHead("e"), List.of(), gen("d")));
      assertSame(t, t.replaceHead(Head.SymHead("len"), List.of(), sym("zero")));
    }

    @Test
    void replaceAtAPathRewritesThatPosition() {
      Term<String, String, String, String, String, String, String> t =
          sym("plus", sym("len", v("x")), v("b"));
      assertSame(
          sym("plus", sym("len", v("y")), v("b")), Term.fromKB(t.replace(List.of(0, 0), v("y"))));
      assertSame(sym("plus", sym("len", v("x")), gen("e")), Term.fromKB(t.replace(List.of(1), gen("e"))));
      assertSame(gen("e"), Term.fromKB(t.replace(List.of(), gen("e"))));
      assertSame(
          fk("worksIn", gen("d")), Term.fromKB(fk("worksIn", gen("e")).replace(List.of(0), gen("d"))));
    }

    @Test
    void replaceAtAPathInsideAVariableFails() {
      assertSame(gen("e"), Term.fromKB(v("x").replace(List.of(), gen("e"))));
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> v("x").replace(List.of(0), gen("e")));
      assertEquals("Cannot replace", e.getMessage());
    }
  }
}
