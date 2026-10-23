package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.cql.Collage.CCollage;
import catdata.provers.KBTheory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class CollageTest {

  private static final String INT = "Int";
  private static final String STR = "Str";
  private static final String EMP = "Emp";
  private static final String DEPT = "Dept";

  /** A collage over String sorts, the way the generic code sees any collage. */
  private static final class Col
      extends CCollage<String, String, String, String, String, String, String> {
    Col() {}

    Col(Collage<String, String, String, String, String, String, String> c) {
      super(c);
    }
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

  private static Eq<String, String, String, String, String, String, String> ground(
      Term<String, String, String, String, String, String, String> l,
      Term<String, String, String, String, String, String, String> r) {
    return new Eq<>(null, l, r);
  }

  private static final Map<String, Chc<String, String>> X_EMP = Map.of("x", Chc.inRight(EMP));

  private static final Eq<String, String, String, String, String, String, String> WORKS_IN =
      ground(fk("worksIn", gen("e")), gen("d"));
  private static final Eq<String, String, String, String, String, String, String> AGE =
      ground(att("age", gen("e")), sym("plus", sym("zero"), sk("y")));
  private static final Eq<String, String, String, String, String, String, String> NAME =
      new Eq<>(X_EMP, att("name", v("x")), obj("ann", STR));

  /**
   * Int and Str (backed by java.lang.String), zero and plus; Emp and Dept, worksIn : Emp -> Dept,
   * name : Emp -> Str and age : Emp -> Int; e : Emp, d : Dept and y : Int; and three equations,
   * e.worksIn = d, e.age = plus(zero, y) and forall x:Emp. x.name = "ann".
   */
  private static Col col() {
    Col c = new Col();
    c.tys().addAll(List.of(INT, STR));
    c.syms().put("zero", new Pair<>(List.of(), INT));
    c.syms().put("plus", new Pair<>(List.of(INT, INT), INT));
    c.java_tys().put(STR, "java.lang.String");
    c.java_parsers().put(STR, "x => x");
    c.getEns().addAll(List.of(EMP, DEPT));
    c.fks().put("worksIn", new Pair<>(EMP, DEPT));
    c.atts().put("name", new Pair<>(EMP, STR));
    c.atts().put("age", new Pair<>(EMP, INT));
    c.gens().put("e", EMP);
    c.gens().put("d", DEPT);
    c.sks().put("y", INT);
    c.eqs().addAll(List.of(WORKS_IN, AGE, NAME));
    return c;
  }

  private static String message(Executable e) {
    return assertThrows(RuntimeException.class, e).getMessage();
  }

  private static String validationError(Consumer<Col> defect) {
    Col c = col();
    defect.accept(c);
    return message(c::validate);
  }

  @Nested
  class Copying {

    @Test
    void copiesEveryComponent() {
      Col c = col();
      Col d = new Col(c);
      assertEquals(c, d);
      assertEquals(c.tys(), d.tys());
      assertEquals(c.syms(), d.syms());
      assertEquals(c.java_tys(), d.java_tys());
      assertEquals(c.java_parsers(), d.java_parsers());
      assertEquals(c.java_fns(), d.java_fns());
      assertEquals(new ArrayList<>(c.getEns()), new ArrayList<>(d.getEns()));
      assertEquals(c.atts(), d.atts());
      assertEquals(c.fks(), d.fks());
      assertEquals(c.gens(), d.gens());
      assertEquals(c.sks(), d.sks());
      assertEquals(new ArrayList<>(c.eqs()), new ArrayList<>(d.eqs()));
    }

    @Test
    void aCopyIsIndependent() {
      Col c = col();
      Col d = new Col(c);
      d.tys().add("Real");
      d.eqs().clear();
      assertFalse(c.tys().contains("Real"));
      assertEquals(3, c.eqs().size());
    }

    @Test
    void addAllMergesEveryComponent() {
      Col c = new Col();
      c.addAll(col());
      assertEquals(col(), c);
    }

    @Test
    void convertIsACast() {
      Col c = col();
      assertSame(c, c.convert());
    }
  }

  @Nested
  class Equality {

    @Test
    void comparesEveryComponent() {
      assertEquals(col(), col());
      assertEquals(col().hashCode(), col().hashCode());
      Col c = col();
      assertTrue(c.equals(c));
      assertFalse(c.equals(null));
      assertFalse(c.equals("collage"));
      List<Consumer<Col>> changes =
          List.of(
              x -> x.atts().put("salary", new Pair<>(EMP, INT)),
              x -> x.getEns().add("Office"),
              x -> x.eqs().remove(NAME),
              x -> x.fks().put("manager", new Pair<>(EMP, EMP)),
              x -> x.gens().put("e2", EMP),
              x -> x.java_fns().put("plus", "(x, y) => x + y"),
              x -> x.java_parsers().put(INT, "x => x"),
              x -> x.java_tys().put(INT, "java.lang.Integer"),
              x -> x.sks().put("y2", INT),
              x -> x.syms().put("one", new Pair<>(List.of(), INT)),
              x -> x.tys().add("Real"));
      for (Consumer<Col> change : changes) {
        Col d = col();
        change.accept(d);
        assertNotEquals(col(), d);
      }
    }

    /** Every map of a CCollage comes from Util.mk(), a NiceMap. */
    @Test
    void refusesToRebindAName() {
      Col c = col();
      c.java_tys().put(STR, "java.lang.String");
      assertEquals(
          "Conflict on Str, was java.lang.String now is java.lang.Object",
          message(() -> c.java_tys().put(STR, "java.lang.Object")));
      assertEquals(
          "Conflict on e, was Emp now is Dept", message(() -> c.addAll(rebound())));
    }

    private Col rebound() {
      Col d = new Col();
      d.gens().put("e", DEPT);
      return d;
    }

    @Test
    void requiresTheSameClass() {
      Col c = col();
      CCollage<String, String, String, String, String, String, String> plain = new CCollage<>(c);
      assertNotEquals(c, plain);
      assertNotEquals(plain, c);
    }

    /** The equations are a list, so their order matters. */
    @Test
    void comparesTheEquationsInOrder() {
      Col c = col();
      Col d = col();
      d.eqs().clear();
      d.eqs().addAll(List.of(NAME, AGE, WORKS_IN));
      assertEquals(new HashSet<>(c.eqs()), new HashSet<>(d.eqs()));
      assertNotEquals(c, d);
    }
  }

  @Nested
  class Equations {

    @Test
    void asTriplesKeepTheirContext() {
      assertEquals(
          List.of(
              new Triple<>(Map.of(), WORKS_IN.lhs, WORKS_IN.rhs),
              new Triple<>(Map.of(), AGE.lhs, AGE.rhs),
              new Triple<>(X_EMP, NAME.lhs, NAME.rhs)),
          col().eqsAsTriples());
    }

    @Test
    void asPairsDropTheEquationsWithVariables() {
      assertEquals(
          List.of(new Pair<>(WORKS_IN.lhs, WORKS_IN.rhs), new Pair<>(AGE.lhs, AGE.rhs)),
          new ArrayList<>(col().eqsAsPairs()));
    }

    @Test
    void isGroundWithoutVariables() {
      Col c = col();
      assertFalse(c.isGround());
      c.eqs().remove(NAME);
      assertTrue(c.isGround());
      assertTrue(new Col().isGround());
    }

    @Test
    void addEqsTurnsTypesideEquationsIntoTypeContexts() {
      Col c = new Col();
      Term<String, Void, String, Void, Void, Void, Void> x = Term.Var("x");
      Term<String, Void, String, Void, Void, Void, Void> zero = Term.Sym("zero", List.of());
      c.addEqs(List.of(new Triple<>(Map.of("x", INT), Term.Sym("plus", List.of(x, zero)), x)));
      assertEquals(
          List.of(new Eq<>(Map.of("x", Chc.inLeft(INT)), sym("plus", v("x"), sym("zero")), v("x"))),
          new ArrayList<>(c.eqs()));
    }

    /** Only typeside symbols count; foreign keys and attributes are unary anyway. */
    @Test
    void isMonoidalWithoutSymbolsOfTwoOrMoreArguments() {
      Col c = col();
      assertFalse(c.isMonoidal());
      c.syms().remove("plus");
      assertTrue(c.isMonoidal());
    }
  }

  @Nested
  class Validation {

    @Test
    void acceptsAWellFormedCollage() {
      col().validate();
    }

    @Test
    void checksTheTypesideSymbolsAndJavaBindings() {
      assertEquals(
          "On typeside symbol real, the return type Real is not declared.",
          validationError(c -> c.syms().put("real", new Pair<>(List.of(), "Real"))));
      assertEquals(
          "On typeside symbol len, the argument type Text is not declared.",
          validationError(c -> c.syms().put("len", new Pair<>(List.of("Text"), INT))));
      assertEquals(
          "There is a java parser for Int but it is not declared as a java type",
          validationError(c -> c.java_parsers().put(INT, "x => x")));
      assertEquals(
          "The java function neg is not a declared function",
          validationError(c -> c.java_fns().put("neg", "x => -x")));
      assertEquals(
          "No constant parser for Int",
          validationError(c -> c.java_tys().put(INT, "java.lang.Integer")));
      assertEquals(
          "no.such.Clazz is not on the java classpath",
          validationError(
              c -> {
                c.tys().add("Blob");
                c.java_tys().put("Blob", "no.such.Clazz");
                c.java_parsers().put("Blob", "x => x");
              }));
    }

    @Test
    void checksTheSchema() {
      assertEquals(
          "On attribute salary, the target type Money is not declared.",
          validationError(c -> c.atts().put("salary", new Pair<>(EMP, "Money"))));
      assertEquals(
          "On attribute floor, the source entity Office is not declared.",
          validationError(c -> c.atts().put("floor", new Pair<>("Office", INT))));
      assertEquals(
          "On foreign key office, the target entity Office is not declared.",
          validationError(c -> c.fks().put("office", new Pair<>(EMP, "Office"))));
      assertEquals(
          "On foreign key head, the source entity Office is not declared.",
          validationError(c -> c.fks().put("head", new Pair<>("Office", EMP))));
    }

    @Test
    void checksTheGeneratorsAndLabelledNulls() {
      assertEquals(
          "On generator o, the entity Office is not declared.",
          validationError(c -> c.gens().put("o", "Office")));
      String m = validationError(c -> c.sks().put("m", "Money"));
      assertTrue(m.startsWith("On labelled null m, the type Money is not declared.\n\ntypes\n"), m);
    }

    @Test
    void checksThatBothSidesOfAnEquationHaveOneSort() {
      assertEquals(
          "On e = y, lhs and rhs types are inr Emp and inl Int",
          validationError(c -> c.eqs().add(ground(gen("e"), sk("y")))));
    }
  }

  @Nested
  class Printing {

    private Col small() {
      Col c = new Col();
      c.tys().add(INT);
      c.syms().put("zero", new Pair<>(List.of(), INT));
      c.syms().put("plus", new Pair<>(List.of(INT, INT), INT));
      c.getEns().addAll(List.of(EMP, DEPT));
      c.fks().put("worksIn", new Pair<>(EMP, DEPT));
      c.atts().put("age", new Pair<>(EMP, INT));
      c.gens().put("e", EMP);
      c.sks().put("y", INT);
      c.eqs().add(AGE);
      return c;
    }

    @Test
    void listsEverySection() {
      assertEquals(
          "types\n\tInt"
              + "\nentities\n\tEmp\n\tDept"
              + "\nfunctions\n\tzero :  -> Int\n\tplus : Int, Int -> Int"
              + "\nforeign keys\n\tworksIn : Emp -> Dept"
              + "\nattributes\n\tage : Emp -> Int"
              + "\ngenerators for entities\n\te : Emp"
              + "\ngenerators for nulls\n\ty : Int"
              + "\nequations\n\te.age = (zero plus y)",
          small().toString());
    }

    @Test
    void leavesOutWhatTheSkippedCollageHas() {
      Col c = small();
      Col skip = new Col();
      skip.tys().add(INT);
      skip.syms().putAll(c.syms());
      assertTrue(
          c.toString(skip).startsWith("types\n\t\nentities\n\tEmp\n\tDept\nfunctions\n\t\n"));
      assertEquals(
          "types\n\t\nentities\n\t\nfunctions\n\t\nforeign keys\n\t\nattributes\n\t"
              + "\ngenerators for entities\n\t\ngenerators for nulls\n\t\nequations\n\t",
          c.toString(c));
    }

    @Test
    void writesOneTptpAxiomPerEquation() {
      Col c = new Col();
      c.eqs().addAll(List.of(WORKS_IN, ground(gen("d"), gen("d"))));
      assertEquals("cnf(eq0,axiom,(worksin(e) = d)).\n\ncnf(eq1,axiom,(d = d)).", c.tptp());
      assertEquals("", new Col().tptp());
    }
  }

  @Nested
  class Typing {

    @Test
    void typesWithSplitContexts() {
      Col c = col();
      assertEquals(Chc.inLeft(STR), c.type(Map.of(), Map.of("x", EMP), att("name", v("x"))));
      assertEquals(Chc.inRight(DEPT), c.type(Map.of(), Map.of(), fk("worksIn", gen("e"))));
      assertEquals(
          Chc.inLeft(INT), c.type(Map.of("n", INT), Map.of(), sym("plus", v("n"), sk("y"))));
    }

    @Test
    void typesWithAMixedContext() {
      Col c = col();
      assertEquals(Chc.inLeft(STR), c.type(X_EMP, att("name", v("x"))));
      assertEquals(
          Chc.inLeft(INT),
          c.type(Map.of("n", Chc.inLeft(INT)), sym("plus", v("n"), sym("zero"))));
    }
  }

  @Nested
  class KnuthBendix {

    @Test
    void declaresEverySortAndHead() {
      KBTheory<
              Chc<String, String>,
              Head<String, String, String, String, String, String, String>,
              String>
          kb = col().toKB();
      assertEquals(
          Set.of(Chc.inLeft(INT), Chc.inLeft(STR), Chc.inRight(EMP), Chc.inRight(DEPT)),
          new HashSet<>(kb.tys));
      Chc<String, String> i = Chc.inLeft(INT);
      Chc<String, String> emp = Chc.inRight(EMP);
      Map<
              Head<String, String, String, String, String, String, String>,
              Pair<List<Chc<String, String>>, Chc<String, String>>>
          expected = new HashMap<>();
      expected.put(Head.SymHead("zero"), new Pair<>(List.of(), i));
      expected.put(Head.SymHead("plus"), new Pair<>(List.of(i, i), i));
      expected.put(Head.FkHead("worksIn"), new Pair<>(List.of(emp), Chc.inRight(DEPT)));
      expected.put(Head.AttHead("name"), new Pair<>(List.of(emp), Chc.inLeft(STR)));
      expected.put(Head.AttHead("age"), new Pair<>(List.of(emp), i));
      expected.put(Head.GenHead("e"), new Pair<>(List.of(), Chc.inRight(EMP)));
      expected.put(Head.GenHead("d"), new Pair<>(List.of(), Chc.inRight(DEPT)));
      expected.put(Head.SkHead("y"), new Pair<>(List.of(), Chc.inLeft(INT)));
      expected.put(Head.ObjHead("ann", STR), new Pair<>(List.of(), Chc.inLeft(STR)));
      assertEquals(expected, kb.syms);
      List<Object> eqs = new ArrayList<>();
      kb.eqs.forEach(eqs::add);
      assertEquals(3, eqs.size());
    }

    /** Constants get a head only if they appear in an equation. */
    @Test
    void declaresOnlyTheConstantsOfTheEquations() {
      Col c = col();
      c.eqs().remove(NAME);
      assertFalse(c.toKB().syms.containsKey(Head.ObjHead("ann", STR)));
    }

    @Test
    void rejectsAnEquationBetweenSorts() {
      Col c = col();
      c.eqs().add(ground(gen("e"), sk("y")));
      String m = message(c::toKB);
      assertTrue(m.startsWith("In equation "), m);
      assertTrue(m.endsWith("lhs type is inr Emp but rhs type is inl Int"), m);
    }
  }

  @Nested
  class Symbols {

    /** Used in RawTerm's "Available symbols" error, closest first. */
    @Test
    void listsTheNamesOfEverySymbolOnceInOrder() {
      Col c = col();
      c.gens().put("zero", EMP);
      assertEquals(
          List.of("age", "d", "e", "name", "plus", "worksIn", "y", "zero"),
          new ArrayList<>(c.allSymbolsAsStrings()));
    }

    @Test
    void leavesOutTypesAndEntities() {
      Collection<String> all = col().allSymbolsAsStrings();
      assertFalse(all.contains(INT));
      assertFalse(all.contains(EMP));
    }
  }

  @Nested
  class Removal {

    @Test
    void removesTheDeclarationOfEachKindOfHead() {
      Col c = col();
      c.remove(Head.AttHead("name"));
      c.remove(Head.FkHead("worksIn"));
      c.remove(Head.SymHead("zero"));
      c.remove(Head.GenHead("d"));
      c.remove(Head.SkHead("y"));
      assertEquals(Map.of("age", new Pair<>(EMP, INT)), c.atts());
      assertTrue(c.fks().isEmpty());
      assertEquals(Set.of("plus"), c.syms().keySet());
      assertEquals(Map.of("e", EMP), c.gens());
      assertTrue(c.sks().isEmpty());
    }

    /** remove has no callers; the equations still mention what was removed. */
    @Test
    void keepsTheEquationsThatUseIt() {
      Col c = col();
      c.remove(Head.AttHead("name"));
      assertTrue(c.eqs().contains(NAME));
      assertThrows(RuntimeException.class, c::validate);
    }

    @Test
    void rejectsAConstant() {
      Col c = col();
      assertEquals("Anomaly: please report", message(() -> c.remove(Head.ObjHead("ann", STR))));
    }
  }

  /** defn decides which schema equations Schema treats as definitions (Schema.java:104). */
  @Nested
  class Definitions {

    private final Map<String, Chc<String, String>> none = Collections.emptyMap();
    private final Map<String, Chc<String, String>> xy =
        Map.of("x", Chc.inLeft(INT), "y", Chc.inLeft(INT));

    @Test
    void aHeadOnDistinctVariablesNotUsedOnTheRightIsADefinition() {
      assertTrue(Collage.defn(xy, sym("plus", v("x"), v("y")), sym("zero")));
      assertTrue(Collage.defn(X_EMP, att("name", v("x")), obj("ann", STR)));
      assertTrue(Collage.defn(none, gen("e"), gen("d")));
      assertTrue(Collage.defn(none, sym("zero"), sk("y")));
    }

    @Test
    void aVariableOrAConstantIsNot() {
      assertFalse(Collage.defn(X_EMP, v("x"), v("x")));
      assertFalse(Collage.defn(none, obj("ann", STR), obj("bob", STR)));
    }

    @Test
    void repeatedNestedOrMissingVariablesAreNot() {
      assertFalse(Collage.defn(xy, sym("plus", v("x"), v("x")), sym("zero")));
      assertFalse(Collage.defn(xy, sym("plus", v("x"), sym("zero")), sym("zero")));
      assertFalse(Collage.defn(xy, att("age", v("x")), sym("zero")));
    }

    @Test
    void aRecursiveEquationIsNot() {
      assertFalse(
          Collage.defn(X_EMP, att("age", v("x")), sym("plus", att("age", v("x")), sym("zero"))));
    }

    @Test
    void getVarArgsUniqueListsTheArgumentsWhenAllAreDistinctVariables() {
      assertEquals(List.of("x", "y"), Collage.getVarArgsUnique(sym("plus", v("x"), v("y"))));
      assertEquals(List.of(), Collage.getVarArgsUnique(gen("e")));
      assertNull(Collage.getVarArgsUnique(sym("plus", v("x"), v("x"))));
      assertNull(Collage.getVarArgsUnique(fk("worksIn", gen("e"))));
    }
  }
}
