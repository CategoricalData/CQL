package catdata.cql;

import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.syntactic;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Unit;
import catdata.cql.AqlOptions.AqlOption;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TypeSideTest {

  /** A type with no java backing. */
  private static final String B = "B";

  /** A second type with no java backing. */
  private static final String N = "N";

  /** A type backed by java.lang.Boolean. */
  private static final String BOOL = "Bool";

  private static final String BOOLEAN_CLASS = "java.lang.Boolean";
  private static final String BOOLEAN_PARSER = "x => java.lang.Boolean.parseBoolean(x)";

  private static AqlOptions strictJava() {
    return new AqlOptions(ops(), AqlOption.allow_java_eqs_unsafe, false);
  }

  private static Term<String, Void, String, Void, Void, Void, Void> v(String name) {
    return Term.Var(name);
  }

  @SafeVarargs
  private static Term<String, Void, String, Void, Void, Void, Void> app(
      String f, Term<String, Void, String, Void, Void, Void, Void>... args) {
    return Term.Sym(f, List.of(args));
  }

  private static Term<String, Void, String, Void, Void, Void, Void> c(String name) {
    return app(name);
  }

  private static Pair<List<String>, String> sort(String result, String... args) {
    return new Pair<>(List.of(args), result);
  }

  private static Triple<Map<String, String>, Term<String, Void, String, Void, Void, Void, Void>, Term<String, Void, String, Void, Void, Void, Void>>
      eq(
          Map<String, String> ctx,
          Term<String, Void, String, Void, Void, Void, Void> l,
          Term<String, Void, String, Void, Void, Void, Void> r) {
    return new Triple<>(ctx, l, r);
  }

  /** The normal form for not(not(x)) = x, not(t) = f and not(f) = t, read as rewrite rules. */
  private static Term<String, Void, String, Void, Void, Void, Void> normal(
      Term<String, Void, String, Void, Void, Void, Void> t) {
    if (!"not".equals(t.sym())) {
      return t;
    }
    Term<String, Void, String, Void, Void, Void, Void> a = normal(t.args.get(0));
    if ("not".equals(a.sym())) {
      return a.args.get(0);
    } else if ("t".equals(a.sym())) {
      return c("f");
    } else if ("f".equals(a.sym())) {
      return c("t");
    }
    return app("not", a);
  }

  /** Decides the theory of bools() by comparing normal forms. */
  private static DP<String, Void, String, Void, Void, Void, Void> booleanAlgebraOfNot() {
    return new DP<>() {
      @Override
      public String toStringProver() {
        return "normalize";
      }

      @Override
      public boolean eq(
          Map<String, Chc<String, Void>> ctx,
          Term<String, Void, String, Void, Void, Void, Void> lhs,
          Term<String, Void, String, Void, Void, Void, Void> rhs) {
        return normal(lhs).equals(normal(rhs));
      }
    };
  }

  private static AqlJs<String, String> js(
      Map<String, Pair<List<String>, String>> syms,
      Map<String, String> javaTys,
      Map<String, String> parsers,
      Map<String, String> fns) {
    return new AqlJs<>(ops(), syms, javaTys, parsers, fns);
  }

  private static AqlJs<String, String> noJava(Map<String, Pair<List<String>, String>> syms) {
    return js(syms, Map.of(), Map.of(), Map.of());
  }

  /**
   * B with t, f : B and not : B -> B, and forall x. not(not(x)) = x, not(t) = f, not(f) = t. The
   * collections are mutable, and the symbols keep their insertion order.
   */
  private static TypeSide<String, String> bools() {
    Map<String, Pair<List<String>, String>> syms = new LinkedHashMap<>();
    syms.put("t", sort(B));
    syms.put("f", sort(B));
    syms.put("not", sort(B, B));
    Set<Triple<Map<String, String>, Term<String, Void, String, Void, Void, Void, Void>, Term<String, Void, String, Void, Void, Void, Void>>>
        eqs = new LinkedHashSet<>();
    eqs.add(eq(Map.of("x", B), app("not", app("not", v("x"))), v("x")));
    eqs.add(eq(Map.of(), app("not", c("t")), c("f")));
    eqs.add(eq(Map.of(), app("not", c("f")), c("t")));
    return new TypeSide<>(
        new LinkedHashSet<>(Set.of(B)), syms, eqs, noJava(syms), booleanAlgebraOfNot(), ops());
  }

  /** N with zero : N and succ : N -> N, in that order, and no equations. */
  private static TypeSide<String, String> nat() {
    Map<String, Pair<List<String>, String>> syms = new LinkedHashMap<>();
    syms.put("zero", sort(N));
    syms.put("succ", sort(N, N));
    return new TypeSide<>(Set.of(N), syms, Set.of(), noJava(syms), syntactic(), ops());
  }

  /** Bool backed by java.lang.Boolean, with the given symbols and java functions. */
  private static TypeSide<String, String> javaBools(
      Set<String> tys,
      Map<String, Pair<List<String>, String>> syms,
      Map<String, String> fns,
      AqlOptions options) {
    return new TypeSide<>(
        tys,
        syms,
        Set.of(),
        js(syms, Map.of(BOOL, BOOLEAN_CLASS), Map.of(BOOL, BOOLEAN_PARSER), fns),
        syntactic(),
        options);
  }

  private static TypeSide<String, String> javaBools() {
    return javaBools(Set.of(BOOL), Map.of(), Map.of(), ops());
  }

  private static Set<Term<String, Void, String, Void, Void, Void, Void>> normals(
      Set<Term<String, Void, String, Void, Void, Void, Void>> terms) {
    return terms.stream().map(TypeSideTest::normal).collect(Collectors.toSet());
  }

  @Nested
  class Construction {

    @Test
    void keepsItsComponents() {
      DP<String, Void, String, Void, Void, Void, Void> dp = syntactic();
      Map<String, Pair<List<String>, String>> syms = Map.of("zero", sort(N));
      AqlJs<String, String> js = noJava(syms);
      TypeSide<String, String> ts = new TypeSide<>(Set.of(N), syms, Set.of(), js, dp, ops());
      assertEquals(Set.of(N), ts.tys);
      assertSame(syms, ts.syms);
      assertSame(js, ts.js);
      assertSame(dp, ts.semantics());
      assertSame(ops(), ts.strat);
    }

    @Test
    void isATypeSide() {
      assertEquals(Kind.TYPESIDE, bools().kind());
    }

    @Test
    void sizeCountsTypesSymbolsAndEquations() {
      assertEquals(1 + 3 + 3, bools().size());
    }

    @Test
    void buildsTheJavaBindingsFromStrings() {
      TypeSide<String, String> ts =
          new TypeSide<>(
              Set.of(BOOL),
              Map.of(),
              Set.of(),
              Map.of(BOOL, BOOLEAN_CLASS),
              Map.of(BOOL, BOOLEAN_PARSER),
              Map.of(),
              ops());
      assertEquals(Map.of(BOOL, BOOLEAN_CLASS), ts.js.java_tys);
      assertEquals(Map.of(BOOL, BOOLEAN_PARSER), ts.js.java_parsers);
    }

    /** B with t, f and not, and the ground equations not(t) = f and not(f) = t. */
    private TypeSide<String, String> groundBools(AqlOptions options) {
      Map<String, Pair<List<String>, String>> syms = new LinkedHashMap<>();
      syms.put("t", sort(B));
      syms.put("f", sort(B));
      syms.put("not", sort(B, B));
      return new TypeSide<>(
          Set.of(B),
          syms,
          new LinkedHashSet<>(
              List.of(
                  eq(Map.of(), app("not", c("t")), c("f")), eq(Map.of(), app("not", c("f")), c("t")))),
          Map.of(),
          Map.of(),
          Map.of(),
          options);
    }

    @Test
    void buildsADecisionProcedureForItsEquations() {
      // Ground equations, so the automatic prover choice is congruence closure.
      DP<String, Void, String, Void, Void, Void, Void> dp = groundBools(ops()).semantics();
      assertTrue(dp.eq(Map.of(), app("not", app("not", c("t"))), c("t")));
      assertFalse(dp.eq(Map.of(), c("t"), c("f")));
    }

    @Test
    void buildsTheDecisionProcedureOnDemandWhenValidationIsOff() {
      TypeSide<String, String> ts =
          groundBools(new AqlOptions(ops(), AqlOption.dont_validate_unsafe, true));
      assertTrue(ts.semantics().eq(Map.of(), app("not", c("t")), c("f")));
      assertSame(ts.semantics(), ts.semantics());
    }

    @Test
    void theDecisionProcedureIsNotRebuiltAfterAMutation() {
      // semantics() is cached, and addEq does not clear it, so t = f stays unprovable.
      TypeSide<String, String> ts = groundBools(ops());
      DP<String, Void, String, Void, Void, Void, Void> before = ts.semantics();
      ts.addEq(Map.of(), c("t"), c("f"));
      assertSame(before, ts.semantics());
      assertFalse(ts.semantics().eq(Map.of(), c("t"), c("f")));
    }

    @Test
    void theInitialTypeSideIsEmpty() {
      TypeSide<String, String> ts = TypeSide.initial(ops());
      assertEquals(0, ts.size());
      assertEquals("", ts.toString());
      assertEquals("Theorem prover for empty theory", ts.semantics().toStringProver());
      assertThrows(RuntimeException.class, () -> ts.semantics().eq(Map.of(), v("x"), v("x")));
    }
  }

  @Nested
  class Validation {

    private String failure(
        Set<String> tys,
        Map<String, Pair<List<String>, String>> syms,
        Set<Triple<Map<String, String>, Term<String, Void, String, Void, Void, Void, Void>, Term<String, Void, String, Void, Void, Void, Void>>>
            eqs,
        AqlJs<String, String> js) {
      return assertThrows(
              RuntimeException.class, () -> new TypeSide<>(tys, syms, eqs, js, syntactic(), ops()))
          .getMessage();
    }

    @Test
    void rejectsASymbolWithAnUndeclaredResultType() {
      Map<String, Pair<List<String>, String>> syms = Map.of("c", sort("Ghost"));
      assertEquals(
          "On typeside symbol c, the return type Ghost is not declared.",
          failure(Set.of(N), syms, Set.of(), noJava(syms)));
    }

    @Test
    void rejectsASymbolWithAnUndeclaredArgumentType() {
      Map<String, Pair<List<String>, String>> syms = Map.of("f", sort(N, "Ghost"));
      assertEquals(
          "On typeside symbol f, the argument type Ghost is not declared.",
          failure(Set.of(N), syms, Set.of(), noJava(syms)));
    }

    @Test
    void rejectsAnEquationOverAnUndeclaredType() {
      String msg =
          failure(Set.of(N), Map.of(), Set.of(eq(Map.of("x", "Ghost"), v("x"), v("x"))), noJava(Map.of()));
      assertEquals(
          "In typeside equation forall {x=Ghost}. x = x, context uses types {Ghost} that are not"
              + " declared.",
          msg);
    }

    @Test
    void rejectsAnEquationWhoseSidesHaveDifferentTypes() {
      Map<String, Pair<List<String>, String>> syms = Map.of("t", sort(B));
      String msg =
          failure(Set.of(B, N), syms, Set.of(eq(Map.of("x", N), v("x"), c("t"))), noJava(syms));
      assertEquals("In typeside equation forall {x=N}. x = t, lhs type is N but rhs type is B", msg);
    }

    @Test
    void printsAGroundEquationWithAnEmptyContext() {
      // toString(eq) adds "forall " only for a non-empty context but always prints the context.
      Map<String, Pair<List<String>, String>> syms = Map.of("t", sort(B), "zero", sort(N));
      String msg =
          failure(Set.of(B, N), syms, Set.of(eq(Map.of(), c("t"), c("zero"))), noJava(syms));
      assertEquals("In typeside equation {}. t = zero, lhs type is B but rhs type is N", msg);
    }

    @Test
    void rejectsAParserForATypeThatIsNotJava() {
      assertEquals(
          "There is a java parser for N but it is not declared as a java type",
          failure(Set.of(N), Map.of(), Set.of(), js(Map.of(), Map.of(), Map.of(N, "x => x"), Map.of())));
    }

    @Test
    void rejectsAJavaFunctionThatIsNotASymbol() {
      assertEquals(
          "The java function ghost is not a declared function",
          failure(Set.of(N), Map.of(), Set.of(), js(Map.of(), Map.of(), Map.of(), Map.of("ghost", "x => x"))));
    }

    @Test
    void rejectsAJavaTypeWithoutAParser() {
      assertEquals(
          "No constant parser for Bool",
          failure(
              Set.of(BOOL), Map.of(), Set.of(), js(Map.of(), Map.of(BOOL, BOOLEAN_CLASS), Map.of(), Map.of())));
    }

    @Test
    void rejectsAJavaTypeWhoseClassIsMissing() {
      assertEquals(
          "no.such.Clazz is not on the java classpath",
          failure(
              Set.of(BOOL),
              Map.of(),
              Set.of(),
              js(Map.of(), Map.of(BOOL, "no.such.Clazz"), Map.of(BOOL, BOOLEAN_PARSER), Map.of())));
    }
  }

  /** With allow_java_eqs_unsafe=false, equations must stay away from java types entirely. */
  @Nested
  class JavaValidation {

    private final Map<String, Pair<List<String>, String>> and =
        Map.of("and", sort(BOOL, BOOL, BOOL));

    @Test
    void rejectsAnEquationOfJavaType() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  new TypeSide<>(
                      Set.of(BOOL),
                      and,
                      Set.of(eq(Map.of("x", BOOL), app("and", v("x"), v("x")), v("x"))),
                      js(and, Map.of(BOOL, BOOLEAN_CLASS), Map.of(BOOL, BOOLEAN_PARSER), Map.of()),
                      syntactic(),
                      strictJava()));
      assertTrue(
          e.getMessage()
              .startsWith(
                  "In typeside equation forall {x=Bool}. (x and x) = x, the return type is Bool which"
                      + " is a java type"),
          e.getMessage());
    }

    @Test
    void acceptsTheSameEquationByDefault() {
      assertDoesNotThrow(
          () ->
              new TypeSide<>(
                  Set.of(BOOL),
                  and,
                  Set.of(eq(Map.of("x", BOOL), app("and", v("x"), v("x")), v("x"))),
                  js(and, Map.of(BOOL, BOOLEAN_CLASS), Map.of(BOOL, BOOLEAN_PARSER), Map.of()),
                  syntactic(),
                  ops()));
    }

    @Test
    void rejectsAnEquationThatBindsAJavaVariable() {
      Map<String, Pair<List<String>, String>> syms = Map.of("zero", sort(N));
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  new TypeSide<>(
                      Set.of(N, BOOL),
                      syms,
                      Set.of(eq(Map.of("b", BOOL), c("zero"), c("zero"))),
                      js(syms, Map.of(BOOL, BOOLEAN_CLASS), Map.of(BOOL, BOOLEAN_PARSER), Map.of()),
                      syntactic(),
                      strictJava()));
      assertTrue(
          e.getMessage().contains(", the context variable(s) bind java type(s)"), e.getMessage());
    }

    @Test
    void rejectsAJavaFunctionInsideANonJavaEquation() {
      // pick(isZero(zero)) = zero has type N, but pick takes a Bool.
      Map<String, Pair<List<String>, String>> syms = new LinkedHashMap<>();
      syms.put("zero", sort(N));
      syms.put("isZero", sort(BOOL, N));
      syms.put("pick", sort(N, BOOL));
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  new TypeSide<>(
                      Set.of(N, BOOL),
                      syms,
                      Set.of(eq(Map.of(), app("pick", app("isZero", c("zero"))), c("zero"))),
                      js(syms, Map.of(BOOL, BOOLEAN_CLASS), Map.of(BOOL, BOOLEAN_PARSER), Map.of()),
                      syntactic(),
                      strictJava()));
      assertEquals(
          "In pick(isZero(zero)), functions with java types are not allowed ", e.getMessage());
    }

    @Test
    void rejectsASymbolThatMixesJavaAndNonJavaTypes() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  javaBools(
                      Set.of(N, BOOL), Map.of("isZero", sort(BOOL, N)), Map.of(), strictJava()));
      assertTrue(
          e.getMessage()
              .startsWith("In symbol isZero, functions must not mix java and non-java types"),
          e.getMessage());
    }

    @Test
    void acceptsAnEquationAwayFromTheJavaTypes() {
      Map<String, Pair<List<String>, String>> syms = Map.of("zero", sort(N), "succ", sort(N, N));
      assertDoesNotThrow(
          () ->
              new TypeSide<>(
                  Set.of(N, BOOL),
                  syms,
                  Set.of(eq(Map.of("x", N), app("succ", v("x")), app("succ", v("x")))),
                  js(syms, Map.of(BOOL, BOOLEAN_CLASS), Map.of(BOOL, BOOLEAN_PARSER), Map.of()),
                  syntactic(),
                  strictJava()));
    }

    @Test
    void acceptsSymbolsThatAreAllJavaOrNoJava() {
      Map<String, Pair<List<String>, String>> syms =
          Map.of("succ", sort(N, N), "and", sort(BOOL, BOOL, BOOL));
      assertDoesNotThrow(() -> javaBools(Set.of(N, BOOL), syms, Map.of(), strictJava()));
    }

    @Test
    void acceptsAMixedSymbolByDefault() {
      assertDoesNotThrow(
          () -> javaBools(Set.of(N, BOOL), Map.of("isZero", sort(BOOL, N)), Map.of(), ops()));
    }
  }

  /** assertNoJava is public and accepts any term, so each branch can be reached directly. */
  @Nested
  class AssertNoJava {

    private final TypeSide<String, String> ts =
        javaBools(
            Set.of(N, BOOL),
            Map.of(
                "zero", sort(N), "succ", sort(N, N), "isZero", sort(BOOL, N), "pick", sort(N, BOOL)),
            Map.of(),
            ops());

    @Test
    void acceptsVariablesAndNonJavaFunctions() {
      assertDoesNotThrow(() -> ts.assertNoJava(v("x")));
      assertDoesNotThrow(() -> ts.assertNoJava(app("succ", app("succ", c("zero")))));
    }

    @Test
    void looksInsideTheArgumentsOfANonJavaFunction() {
      RuntimeException e =
          assertThrows(
              RuntimeException.class, () -> ts.assertNoJava(app("succ", app("pick", v("b")))));
      assertEquals("In pick(b), functions with java types are not allowed ", e.getMessage());
    }

    @Test
    void looksThroughAttributesAndForeignKeys() {
      Term<String, String, String, String, String, Void, Void> x = Term.Var("x");
      assertDoesNotThrow(() -> ts.assertNoJava(Term.Att("a", Term.Fk("f", x))));
    }

    @Test
    void rejectsAFunctionWithAJavaArgument() {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ts.assertNoJava(app("pick", v("b"))));
      assertEquals("In pick(b), functions with java types are not allowed ", e.getMessage());
    }

    @Test
    void rejectsAFunctionWithAJavaResult() {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> ts.assertNoJava(app("isZero", c("zero"))));
      assertEquals("In isZero(zero), functions with java types are not allowed", e.getMessage());
    }

    @Test
    void rejectsAJavaConstant() {
      Term<String, Void, String, Void, Void, Void, Void> yes = Term.Obj(true, BOOL);
      RuntimeException e = assertThrows(RuntimeException.class, () -> ts.assertNoJava(yes));
      assertEquals("In true@Bool, java constants are not allowed ", e.getMessage());
    }

    @Test
    void rejectsGenerators() {
      Term<String, Void, String, Void, Void, String, Void> g = Term.Gen("g");
      RuntimeException e = assertThrows(RuntimeException.class, () -> ts.assertNoJava(g));
      assertEquals("Anomaly: please report.", e.getMessage());
    }
  }

  @Nested
  class Typing {

    @Test
    void typesATermInAContext() {
      assertEquals(B, bools().type(Map.of("x", B), app("not", v("x"))));
    }

    @Test
    void rejectsATermThatIsNotATypesideTerm() {
      Term<String, Void, String, Void, Void, String, Void> g = Term.Gen("g");
      RuntimeException e = assertThrows(RuntimeException.class, () -> bools().type(Map.of(), g));
      assertEquals("g is not a typeside term", e.getMessage());
    }
  }

  @Nested
  class Mutation {

    @Test
    void addsTypesSymbolsAndEquations() {
      TypeSide<String, String> ts = bools();
      ts.addTy(N);
      ts.addSym("zero", sort(N));
      ts.addEq(Map.of(), c("zero"), c("zero"));
      assertTrue(ts.tys.contains(N));
      assertEquals(sort(N), ts.syms.get("zero"));
      assertEquals(2 + 4 + 4, ts.size());
    }

    @Test
    void toStringIsNotRefreshedAfterAMutation() {
      // toString is cached on first use and the mutators do not clear the cache.
      TypeSide<String, String> ts = bools();
      String before = ts.toString();
      ts.addTy(N);
      assertSame(before, ts.toString());
      assertFalse(ts.toString().contains(N), ts.toString());
    }

    @Test
    void theInitialTypeSideCannotTakeEquations() {
      // initial() passes Collections.emptySet() for the equations, which is immutable.
      TypeSide<String, String> ts = TypeSide.initial(ops());
      ts.addTy(N);
      assertThrows(UnsupportedOperationException.class, () -> ts.addEq(Map.of(), v("x"), v("x")));
    }
  }

  @Nested
  class ToString {

    @Test
    void listsTypesFunctionsAndEquationsAlphabetically() {
      assertEquals(
          "types\n\tB"
              + "\nfunctions\n\tf :  -> B\n\tnot : B -> B\n\tt :  -> B"
              + "\nequations\n\tforall x:B. not(not(x)) = x\n\tnot(f) = t\n\tnot(t) = f",
          bools().toString());
    }

    @Test
    void listsTheJavaBindings() {
      Map<String, Pair<List<String>, String>> syms = Map.of("and", sort(BOOL, BOOL, BOOL));
      String s = javaBools(Set.of(BOOL), syms, Map.of("and", "(x, y) => x && y"), ops()).toString();
      assertTrue(s.contains("\nexternal_types\n\tBool = "), s);
      assertTrue(s.contains(BOOLEAN_CLASS), s);
      assertTrue(s.contains("\nexternal_constants\n\tBool = "), s);
      assertTrue(s.contains("\nexternal_functions\n\tand = "), s);
    }

    @Test
    void isComputedOnce() {
      TypeSide<String, String> ts = bools();
      assertSame(ts.toString(), ts.toString());
    }
  }

  @Nested
  class EqualsAndHashCode {

    @Test
    void separatelyBuiltCopiesAreEqual() {
      // The decision procedure is not compared, so two copies with distinct DP objects agree.
      assertEquals(bools(), bools());
      assertEquals(bools().hashCode(), bools().hashCode());
    }

    @Test
    void isReflexiveAndRejectsNull() {
      TypeSide<String, String> ts = bools();
      assertTrue(ts.equals(ts));
      assertFalse(ts.equals(null));
    }

    @Test
    void throwsOnAForeignType() {
      // equals casts its argument to TypeSide without checking its class.
      TypeSide<String, String> ts = bools();
      assertThrows(ClassCastException.class, () -> ts.equals("not a typeside"));
    }

    @Test
    void distinguishesOnEveryComponent() {
      TypeSide<String, String> withType = bools();
      withType.addTy(N);
      TypeSide<String, String> withSym = bools();
      withSym.addSym("u", sort(B));
      TypeSide<String, String> withEq = bools();
      withEq.addEq(Map.of(), c("t"), c("t"));
      for (TypeSide<String, String> other : List.of(withType, withSym, withEq)) {
        assertNotEquals(bools(), other);
      }
      assertNotEquals(nat(), javaBools());
    }

    @Test
    void distinguishesOnTheJavaBindings() {
      TypeSide<String, String> a = javaBools();
      TypeSide<String, String> b =
          new TypeSide<>(
              Set.of(BOOL),
              Map.of(),
              Set.of(),
              js(Map.of(), Map.of(BOOL, BOOLEAN_CLASS), Map.of(BOOL, "x => true"), Map.of()),
              syntactic(),
              ops());
      assertNotEquals(a, b);
    }
  }

  @Nested
  class JavaFunctions {

    @Test
    void noJavaFunctionsMeansNoImplicitEquations() {
      assertFalse(bools().hasImplicitJavaEqs());
    }

    @Test
    void javaConstantsDoNotCount() {
      Map<String, Pair<List<String>, String>> syms = Map.of("yes", sort(BOOL));
      assertFalse(javaBools(Set.of(BOOL), syms, Map.of("yes", "true"), ops()).hasImplicitJavaEqs());
    }

    @Test
    void aJavaFunctionWithArgumentsDoes() {
      Map<String, Pair<List<String>, String>> syms = Map.of("and", sort(BOOL, BOOL, BOOL));
      assertTrue(
          javaBools(Set.of(BOOL), syms, Map.of("and", "(x, y) => x && y"), ops())
              .hasImplicitJavaEqs());
    }
  }

  @Nested
  class Collages {

    @Test
    void sharesTheTypeSidesSymbols() {
      TypeSide<String, String> ts = bools();
      Collage<String, Void, String, Void, Void, Void, Void> col = ts.collage();
      assertSame(ts.tys, col.tys());
      assertSame(ts.syms, col.syms());
      assertSame(ts.js.java_tys, col.java_tys());
      assertSame(ts.js.java_parsers, col.java_parsers());
      assertSame(ts.js.java_fns, col.java_fns());
    }

    @Test
    void hasNoEntitiesOrGenerators() {
      Collage<String, Void, String, Void, Void, Void, Void> col = bools().collage();
      assertTrue(col.getEns().isEmpty());
      assertTrue(col.atts().isEmpty());
      assertTrue(col.fks().isEmpty());
      assertTrue(col.gens().isEmpty());
      assertTrue(col.sks().isEmpty());
    }

    @Test
    void presentsEachEquationWithItsVariablesTypeSorted() {
      Collage<String, Void, String, Void, Void, Void, Void> col = bools().collage();
      assertEquals(3, col.eqs().size());
      Set<Triple<Map<String, Chc<String, Void>>, Term<String, Void, String, Void, Void, Void, Void>, Term<String, Void, String, Void, Void, Void, Void>>>
          eqs = col.eqs().stream().map(Eq::toTriple).collect(Collectors.toSet());
      assertTrue(
          eqs.contains(
              new Triple<>(Map.of("x", Chc.inLeft(B)), app("not", app("not", v("x"))), v("x"))),
          eqs.toString());
    }

    @Test
    void rendersAsACollage() {
      String s = bools().collage().toString();
      assertTrue(s.contains("not"), s);
    }
  }

  @Nested
  class Enumerate {

    @Test
    void listsTheConstantsOfAnEnum() {
      assertEquals(7, TypeSide.enumerate("java.time.DayOfWeek").size());
    }

    @Test
    void listsBothBooleans() {
      assertEquals(Set.of(true, false), new HashSet<>(TypeSide.enumerate(BOOLEAN_CLASS)));
    }

    @Test
    void listsTheUnitAndNothingForVoid() {
      assertEquals(List.of(Unit.unit), List.copyOf(TypeSide.enumerate("catdata.Unit")));
      assertTrue(TypeSide.enumerate("java.lang.Void").isEmpty());
    }

    @Test
    void refusesAnInfiniteType() {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> TypeSide.enumerate("java.lang.String"));
      assertTrue(e.getMessage().startsWith("Cannot enumerate: java.lang.String."), e.getMessage());
    }

    @Test
    void wrapsAMissingClass() {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> TypeSide.enumerate("no.such.Clazz"));
      assertInstanceOf(ClassNotFoundException.class, e.getCause());
    }
  }

  /**
   * makeModel enumerates closed terms (over a context) up to provable equality, applying every
   * symbol once per round for at most the given number of rounds, and stopping early at a fixed
   * point.
   */
  @Nested
  class Models {

    @Test
    void withNoRoundsHoldsOnlyTheContext() {
      assertEquals(Set.of(v("x")), bools().makeModel(Map.of("x", Chc.inLeft(B)), 0).get(B));
    }

    @Test
    void closesUnderTheFunctionsUpToProvableEquality() {
      Set<Term<String, Void, String, Void, Void, Void, Void>> closed = bools().makeModel(Map.of(), 64).get(B);
      assertEquals(2, closed.size(), closed.toString());
      assertEquals(Set.of(c("t"), c("f")), normals(closed));
    }

    @Test
    void homHasOneTermPerUnaryBooleanFunction() {
      // Over one variable B0 the terms are B0, not(B0), t and f: the four functions B -> B.
      Set<Term<String, Void, String, Void, Void, Void, Void>> hom = bools().hom(List.of(B), B);
      assertEquals(4, hom.size(), hom.toString());
      assertEquals(Set.of(v("B0"), app("not", v("B0")), c("t"), c("f")), normals(hom));
    }

    @Test
    void homOfNoArgumentsIsTheClosedTerms() {
      assertEquals(Set.of(c("t"), c("f")), normals(bools().hom(List.of(), B)));
    }

    @Test
    void stopsAfterTheGivenNumberOfRoundsInAnInfiniteTheory() {
      // zero is applied before succ in each round, so round k adds succ^(k+1)(zero).
      Term<String, Void, String, Void, Void, Void, Void> one = app("succ", c("zero"));
      Term<String, Void, String, Void, Void, Void, Void> two = app("succ", one);
      Term<String, Void, String, Void, Void, Void, Void> three = app("succ", two);
      assertEquals(Set.of(c("zero"), one, two, three), nat().makeModel(Map.of(), 3).get(N));
    }

    @Test
    void includesTheEnumeratedJavaConstants() {
      assertEquals(
          Set.of(Term.Obj(true, BOOL), Term.Obj(false, BOOL)),
          javaBools().makeModel(Map.of(), 0).get(BOOL));
    }

    @Test
    void failsOnAJavaTypeThatCannotBeEnumerated() {
      TypeSide<String, String> strings =
          new TypeSide<>(
              Set.of("Str"),
              Map.of(),
              Set.of(),
              js(Map.of(), Map.of("Str", "java.lang.String"), Map.of("Str", "x => x"), Map.of()),
              syntactic(),
              ops());
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> strings.makeModel(Map.of(), 0));
      assertTrue(e.getMessage().startsWith("Cannot enumerate: java.lang.String."), e.getMessage());
    }

    @Test
    void getModelNeverAppliesTheFunctions() {
      // getModel calls makeModel with -1 rounds, so the closure loop never runs: only the java
      // constants are listed, and t and f, which are terms of B, are not.
      assertTrue(bools().getModel().get(B).isEmpty());
      assertEquals(
          Set.of(Term.Obj(true, BOOL), Term.Obj(false, BOOL)), javaBools().getModel().get(BOOL));
    }

    @Test
    void getModelIsComputedOnceAndSeedsLaterModels() {
      TypeSide<String, String> ts = javaBools();
      assertSame(ts.getModel(), ts.getModel());
      assertEquals(ts.getModel().get(BOOL), ts.makeModel(Map.of(), 0).get(BOOL));
    }
  }

  @Nested
  class CheckerTptp {

    /** B with t, f, not and and, and the single ground equation not(t) = f. */
    private TypeSide<String, String> ground(String t) {
      Map<String, Pair<List<String>, String>> syms = new LinkedHashMap<>();
      syms.put(t, sort(B));
      syms.put("f", sort(B));
      syms.put("not", sort(B, B));
      syms.put("and", sort(B, B, B));
      return new TypeSide<>(
          Set.of(B), syms, Set.of(eq(Map.of(), app("not", c(t)), c("f"))), noJava(syms), syntactic(), ops());
    }

    private Set<String> lines(String s) {
      return Arrays.stream(s.split("\n")).collect(Collectors.toSet());
    }

    @Test
    void declaresEveryTypeAndSymbolAndStatesTheEquations() {
      assertEquals(
          Set.of(
              "tff(B_type, type, B: $tType).",
              "tff(t_type, type, t: B).",
              "tff(f_type, type, f: B).",
              "tff(not_type, type, not: B > B).",
              "tff(and_type, type, and: (B*B) > B).",
              "tff(ts0, axiom, (not(t) = f))."),
          lines(ground("t").toCheckerTpTp()));
    }

    @Test
    void declaresAConstantWithItsOwnCaseButUsesItLowercased() {
      // The declaration keeps T, but Term.toTpTpForChecker lowercases constants, so the axiom
      // mentions t, which is never declared.
      Set<String> lines = lines(ground("T").toCheckerTpTp());
      assertTrue(lines.contains("tff(T_type, type, T: B)."), lines.toString());
      assertTrue(lines.contains("tff(ts0, axiom, (not(t) = f))."), lines.toString());
    }

    @Test
    void rejectsEquationsWithVariables() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> bools().toCheckerTpTp());
      assertEquals("Anomaly: please report", e.getMessage());
    }

    @Test
    void rejectsJavaTypes() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> javaBools().toCheckerTpTp());
      assertEquals("Anomaly: please report", e.getMessage());
    }
  }
}
