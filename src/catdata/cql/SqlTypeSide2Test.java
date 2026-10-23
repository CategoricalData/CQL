package catdata.cql;

import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlTypeSide;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.AqlProver.ProverName;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class SqlTypeSide2Test {

  private static final String INT = "Int";
  private static final String STR = "Str";

  /** The type FOR_TY adds; lowercase, unlike the Boolean of {@link SqlTypeSide}. */
  private static final String BOOLEAN = "boolean";

  private static final Sym TT = sym("tt", List.of(), BOOLEAN);
  private static final Sym FF = sym("ff", List.of(), BOOLEAN);
  private static final Sym PLUS = sym("plus", List.of(INT, INT), INT);

  private static final SqlTypeSide2<String> FOR_TY = SqlTypeSide2.FOR_TY;

  private static Sym sym(String name, List<String> dom, String cod) {
    return Sym.Sym(name, new Pair<>(dom, cod));
  }

  private static Term<String, Void, Sym, Void, Void, Void, Void> v(String x) {
    return Term.Var(x);
  }

  @SafeVarargs
  private static Term<String, Void, Sym, Void, Void, Void, Void> app(
      Sym f, Term<String, Void, Sym, Void, Void, Void, Void>... args) {
    return Term.Sym(f, List.of(args));
  }

  private static Term<String, Void, Sym, Void, Void, Void, Void> nul(String ty) {
    return app(FOR_TY.typedNull(ty));
  }

  private static Map<Sym, Pair<List<String>, String>> sorts(Sym... syms) {
    Map<Sym, Pair<List<String>, String>> m = new LinkedHashMap<>();
    for (Sym s : syms) {
      m.put(s, s.ty);
    }
    return m;
  }

  /** A typeside without java types, over mutable collections so addTy and addSym can extend it. */
  private static TypeSide<String, Sym> typeSide(Set<String> tys, Sym... syms) {
    return new TypeSide<>(
        new LinkedHashSet<>(tys),
        sorts(syms),
        new HashSet<>(),
        Map.of(),
        Map.of(),
        Map.of(),
        ops());
  }

  /** Int and boolean, with the constants tt, ff : boolean and plus : Int, Int -> Int. */
  private static TypeSide<String, Sym> intsAndBooleans() {
    return typeSide(Set.of(INT, BOOLEAN), TT, FF, PLUS);
  }

  /**
   * FOR_TY's construction with true and false replaced by the declared constants tt and ff. This
   * is the only way to get a typeside out of make: see {@link Make#rejectsEveryTypesideWithoutJavaTypes}.
   */
  private static SqlTypeSide2<String> withConstants() {
    SqlTypeSide2<String> s = new SqlTypeSide2<>(ty -> ty, BOOLEAN);
    s.t = app(TT);
    s.f = app(FF);
    return s;
  }

  /** The auto prover finds no prover for a nullified theory with a binary symbol. */
  private static AqlOptions programProver() {
    return new AqlOptions(
        new AqlOptions(ops(), AqlOption.prover, ProverName.program),
        AqlOption.program_allow_nontermination_unsafe,
        true);
  }

  private static Triple<
          Map<String, String>,
          Term<String, Void, Sym, Void, Void, Void, Void>,
          Term<String, Void, Sym, Void, Void, Void, Void>>
      eq(
          Map<String, String> ctx,
          Term<String, Void, Sym, Void, Void, Void, Void> lhs,
          Term<String, Void, Sym, Void, Void, Void, Void> rhs) {
    return new Triple<>(ctx, lhs, rhs);
  }

  private static String message(Executable e) {
    return assertThrows(RuntimeException.class, e).getMessage();
  }

  /** Runs r and returns what it printed on standard output. */
  private static String stdout(Runnable r) {
    PrintStream old = System.out;
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
    try {
      r.run();
    } finally {
      System.setOut(old);
    }
    return buf.toString(StandardCharsets.UTF_8);
  }

  @Nested
  class Symbols {

    @Test
    void forTyKeepsTypeNamesAndAddsLowercaseBoolean() {
      assertEquals(INT, FOR_TY.toCql.apply(INT));
      assertEquals(BOOLEAN, FOR_TY.boolTy);
    }

    @Test
    void connectivesCarryTheSqlTypeSideBooleanSort() {
      assertSame(Sym.Sym("not", SqlTypeSide.boolSort1), SqlTypeSide2.NOT_SYM);
      assertSame(Sym.Sym("isTrue", SqlTypeSide.boolSort1), SqlTypeSide2.IS_TRUE_SYM);
      assertSame(Sym.Sym("isFalse", SqlTypeSide.boolSort1), SqlTypeSide2.IS_FALSE_SYM);
      assertSame(Sym.Sym("and", SqlTypeSide.boolSort2), SqlTypeSide2.AND_SYM);
      assertSame(Sym.Sym("or", SqlTypeSide.boolSort2), SqlTypeSide2.OR_SYM);
      assertEquals(new Pair<>(List.of("Boolean"), "Boolean"), SqlTypeSide2.NOT_SYM.ty);
    }

    @Test
    void typedNullIsAConstantOfItsType() {
      Sym n = FOR_TY.typedNull(INT);
      assertEquals("null_Int", n.str);
      assertEquals(new Pair<>(List.of(), INT), n.ty);
      assertSame(n, FOR_TY.typedNull(INT));
    }

    @Test
    void typedSymbolsAreNamedThroughToCql() {
      SqlTypeSide2<Integer> numbered = new SqlTypeSide2<>(i -> "T" + i, 0);
      assertSame(sym("null_T7", List.of(), "T7"), numbered.typedNull(7));
      assertSame(sym("eq_T7", List.of("T7", "T7"), BOOLEAN), numbered.typedEq(7));
      assertSame(sym("isNull_T7", List.of("T7"), BOOLEAN), numbered.typedIsNull(7));
    }

    @Test
    void typedPredicatesReturnLowercaseBooleanWhateverTheBooleanType() {
      SqlTypeSide2<String> bool = new SqlTypeSide2<>(ty -> ty, "Bool");
      assertEquals(BOOLEAN, bool.typedEq(INT).ty.second);
      assertEquals(BOOLEAN, bool.typedIsNull(INT).ty.second);
    }

    @Test
    void trueAndFalseAreJavaConstantsOfTypeBoolean() {
      assertEquals(Optional.of(true), FOR_TY.t.obj());
      assertEquals(BOOLEAN, FOR_TY.t.ty());
      assertEquals("true", FOR_TY.t.toString());
      assertEquals(Optional.of(false), FOR_TY.f.obj());
      assertEquals(BOOLEAN, FOR_TY.f.ty());
    }

    @Test
    void tysAddsTheBooleanType() {
      assertEquals(Set.of(INT, BOOLEAN), FOR_TY.tys(typeSide(Set.of(INT))));
      assertEquals(Set.of(INT, BOOLEAN), FOR_TY.tys(intsAndBooleans()));
      assertEquals(Set.of(BOOLEAN), FOR_TY.tys(TypeSide.initial(ops())));
    }
  }

  @Nested
  class Make {

    @Test
    void rejectsATypesideWithJavaTypes() {
      assertEquals(
          "Cannot nullify java typesides", message(() -> FOR_TY.make(sqlTypeSide(), ops())));
    }

    /** true and false are java constants, but make gives the new typeside no java types. */
    @Test
    void rejectsEveryTypesideWithoutJavaTypes() {
      TypeSide<String, Sym> empty = TypeSide.initial(ops());
      for (TypeSide<String, Sym> parent : List.of(empty, intsAndBooleans())) {
        String m = message(() -> FOR_TY.make(parent, programProver()));
        assertTrue(m.matches("In (true|false), not a declared type: boolean"), m);
      }
    }

    @Test
    void printsTheTypesOnStandardOutput() {
      String out = stdout(() -> withConstants().make(intsAndBooleans(), programProver()));
      String first = out.lines().findFirst().orElse("");
      assertTrue(first.startsWith("{") && first.endsWith("}"), first);
      assertEquals(
          Set.of(INT, BOOLEAN),
          Arrays.stream(first.substring(1, first.length() - 1).split(", "))
              .collect(Collectors.toSet()));
    }

    @Test
    void findsNoAutomaticProverOnceASymbolHasTwoArguments() {
      String m = message(() -> withConstants().make(intsAndBooleans(), ops()));
      assertTrue(m.startsWith("Cannot automatically chose prover"), m);
    }

    @Test
    void addsTheBooleanTypeAndNoJavaTypes() {
      TypeSide<String, Sym> ts = withConstants().make(intsAndBooleans(), programProver());
      assertEquals(Set.of(INT, BOOLEAN), ts.tys);
      assertTrue(ts.js.java_tys.isEmpty());
    }

    @Test
    void declaresConnectivesOverItsOwnBooleanType() {
      TypeSide<String, Sym> ts = withConstants().make(intsAndBooleans(), programProver());
      Pair<List<String>, String> unary = new Pair<>(List.of(BOOLEAN), BOOLEAN);
      Pair<List<String>, String> binary = new Pair<>(List.of(BOOLEAN, BOOLEAN), BOOLEAN);
      Map<Sym, Pair<List<String>, String>> expected = sorts(TT, FF, PLUS);
      expected.put(SqlTypeSide2.NOT_SYM, unary);
      expected.put(SqlTypeSide2.IS_TRUE_SYM, unary);
      expected.put(SqlTypeSide2.IS_FALSE_SYM, unary);
      expected.put(SqlTypeSide2.AND_SYM, binary);
      expected.put(SqlTypeSide2.OR_SYM, binary);
      for (String ty : List.of(INT, BOOLEAN)) {
        expected.put(FOR_TY.typedNull(ty), new Pair<>(List.of(), ty));
        expected.put(FOR_TY.typedEq(ty), new Pair<>(List.of(ty, ty), BOOLEAN));
        expected.put(FOR_TY.typedIsNull(ty), new Pair<>(List.of(ty), BOOLEAN));
      }
      assertEquals(expected, ts.syms);
    }

    @Test
    void leavesTheParentUnchanged() {
      TypeSide<String, Sym> parent = intsAndBooleans();
      withConstants().make(parent, programProver());
      assertEquals(Set.of(INT, BOOLEAN), parent.tys);
      assertEquals(sorts(TT, FF, PLUS), parent.syms);
      assertTrue(parent.eqs.isEmpty());
    }
  }

  /** The equations of the typeside make builds over {@link #intsAndBooleans()}. */
  @Nested
  class Equations {

    private final Term<String, Void, Sym, Void, Void, Void, Void> t = app(TT);
    private final Term<String, Void, Sym, Void, Void, Void, Void> f = app(FF);
    private final Term<String, Void, Sym, Void, Void, Void, Void> u = nul(BOOLEAN);

    /** The arguments of the truth tables below, in order. */
    private final List<Term<String, Void, Sym, Void, Void, Void, Void>> tfu = List.of(t, f, u);

    /** A set of typeside equations, filled from truth tables over {@link #tfu}. */
    private final class Theory {

      final Set<
              Triple<
                  Map<String, String>,
                  Term<String, Void, Sym, Void, Void, Void, Void>,
                  Term<String, Void, Sym, Void, Void, Void, Void>>>
          eqs = new HashSet<>();

      Theory add(
          Map<String, String> ctx,
          Term<String, Void, Sym, Void, Void, Void, Void> lhs,
          Term<String, Void, Sym, Void, Void, Void, Void> rhs) {
        eqs.add(eq(ctx, lhs, rhs));
        return this;
      }

      /** results.get(i) is s(tfu[i]). */
      Theory unary(Sym s, List<Term<String, Void, Sym, Void, Void, Void, Void>> results) {
        for (int i = 0; i < 3; i++) {
          add(Map.of(), app(s, tfu.get(i)), results.get(i));
        }
        return this;
      }

      /** rows.get(i).get(j) is s(tfu[i], tfu[j]). */
      Theory binary(Sym s, List<List<Term<String, Void, Sym, Void, Void, Void, Void>>> rows) {
        for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 3; j++) {
            add(Map.of(), app(s, tfu.get(i), tfu.get(j)), rows.get(i).get(j));
          }
        }
        return this;
      }
    }

    @Test
    void encodeThreeValuedLogicIsNullAndNullPropagation() {
      TypeSide<String, Sym> ts = withConstants().make(intsAndBooleans(), programProver());
      Theory expected =
          new Theory()
              .unary(SqlTypeSide2.NOT_SYM, List.of(f, t, u))
              .unary(SqlTypeSide2.IS_TRUE_SYM, List.of(t, f, f))
              .unary(SqlTypeSide2.IS_FALSE_SYM, List.of(f, t, f))
              .binary(
                  SqlTypeSide2.AND_SYM, List.of(List.of(t, f, u), List.of(f, f, f), List.of(u, f, u)))
              .binary(
                  SqlTypeSide2.OR_SYM, List.of(List.of(t, t, t), List.of(t, f, u), List.of(t, u, u)))
              .binary(
                  FOR_TY.typedEq(BOOLEAN),
                  List.of(List.of(t, f, u), List.of(f, t, u), List.of(u, u, u)))
              .add(Map.of(), app(FOR_TY.typedIsNull(INT), nul(INT)), t)
              .add(Map.of(), app(FOR_TY.typedIsNull(BOOLEAN), u), t)
              .add(Map.of(), app(FOR_TY.typedIsNull(BOOLEAN), t), f)
              .add(Map.of(), app(FOR_TY.typedIsNull(BOOLEAN), f), f)
              .add(Map.of(), app(PLUS, nul(INT), nul(INT)), nul(INT))
              .add(Map.of("x0", INT), app(PLUS, v("x0"), nul(INT)), nul(INT));
      assertEquals(expected.eqs, new HashSet<>(ts.eqs));
    }

    /**
     * For position j, the loop compares j with the count of variables so far, so every argument
     * from j on becomes null: f(null, x0) = null is never generated.
     */
    @Test
    void propagateNullsOnlyFromATrailingRunOfArguments() {
      Sym if3 = sym("if3", List.of(INT, INT, INT), INT);
      Sym neg = sym("neg", List.of(INT), INT);
      Sym zero = sym("zero", List.of(), INT);
      TypeSide<String, Sym> ts =
          withConstants()
              .make(typeSide(Set.of(INT, BOOLEAN), TT, FF, if3, neg, zero), programProver());
      Set<Sym> heads = Set.of(if3, neg, zero);
      var generated =
          ts.eqs.stream().filter(e -> heads.contains(e.second.sym())).collect(Collectors.toSet());
      Term<String, Void, Sym, Void, Void, Void, Void> n = nul(INT);
      assertEquals(
          Set.of(
              eq(Map.of(), app(if3, n, n, n), n),
              eq(Map.of("x0", INT), app(if3, v("x0"), n, n), n),
              eq(Map.of("x0", INT, "x1", INT), app(if3, v("x0"), v("x1"), n), n),
              eq(Map.of(), app(neg, n), n)),
          generated);
    }

    @Test
    void decideThreeValuedLogic() {
      DP<String, Void, Sym, Void, Void, Void, Void> dp =
          withConstants().make(intsAndBooleans(), programProver()).semantics();
      assertTrue(dp.eq(Map.of(), app(SqlTypeSide2.NOT_SYM, u), u));
      assertTrue(dp.eq(Map.of(), app(SqlTypeSide2.AND_SYM, u, f), f));
      assertTrue(dp.eq(Map.of(), app(SqlTypeSide2.OR_SYM, t, u), t));
      assertTrue(dp.eq(Map.of(), app(SqlTypeSide2.IS_TRUE_SYM, u), f));
      assertFalse(dp.eq(Map.of(), app(SqlTypeSide2.AND_SYM, u, t), t));
    }

    @Test
    void decideNullPropagationInTheLastArgumentOnly() {
      DP<String, Void, Sym, Void, Void, Void, Void> dp =
          withConstants().make(intsAndBooleans(), programProver()).semantics();
      Map<String, Chc<String, Void>> x = Map.of("x", Chc.inLeft(INT));
      assertTrue(dp.eq(x, app(PLUS, v("x"), nul(INT)), nul(INT)));
      assertTrue(dp.eq(Map.of(), app(PLUS, nul(INT), nul(INT)), nul(INT)));
      assertFalse(dp.eq(x, app(PLUS, nul(INT), v("x")), nul(INT)));
    }
  }

  /** addTy and addSym, which nothing in src calls. */
  @Nested
  class Extension {

    @Test
    void addTyDeclaresTheTypeAndItsNullSymbols() {
      TypeSide<String, Sym> ts = typeSide(Set.of(BOOLEAN));
      assertTrue(FOR_TY.addTy(ts, STR));
      assertEquals(Set.of(BOOLEAN, STR), ts.tys);
      Map<Sym, Pair<List<String>, String>> expected = new LinkedHashMap<>();
      expected.put(FOR_TY.typedNull(STR), new Pair<>(List.of(), STR));
      expected.put(FOR_TY.typedEq(STR), new Pair<>(List.of(STR, STR), BOOLEAN));
      expected.put(FOR_TY.typedIsNull(STR), new Pair<>(List.of(STR), BOOLEAN));
      assertEquals(expected, ts.syms);
    }

    @Test
    void addTyIgnoresAKnownType() {
      TypeSide<String, Sym> ts = typeSide(Set.of(INT, BOOLEAN), PLUS);
      assertFalse(FOR_TY.addTy(ts, INT));
      assertEquals(sorts(PLUS), ts.syms);
    }

    @Test
    void addTyDoesNotDeclareTheBooleanType() {
      TypeSide<String, Sym> ts = typeSide(Set.of());
      FOR_TY.addTy(ts, STR);
      assertEquals(Set.of(STR), ts.tys);
      assertEquals(BOOLEAN, ts.syms.get(FOR_TY.typedEq(STR)).second);
    }

    @Test
    void addSymDeclaresItsNewTypesFirst() {
      TypeSide<String, Sym> ts = typeSide(Set.of(INT, BOOLEAN));
      Sym len = sym("len", List.of(STR), INT);
      assertTrue(FOR_TY.addSym(ts, len, List.of(STR), INT));
      assertEquals(Set.of(INT, BOOLEAN, STR), ts.tys);
      Map<Sym, Pair<List<String>, String>> expected = new LinkedHashMap<>();
      expected.put(FOR_TY.typedNull(STR), new Pair<>(List.of(), STR));
      expected.put(FOR_TY.typedEq(STR), new Pair<>(List.of(STR, STR), BOOLEAN));
      expected.put(FOR_TY.typedIsNull(STR), new Pair<>(List.of(STR), BOOLEAN));
      expected.put(len, new Pair<>(List.of(STR), INT));
      assertEquals(expected, ts.syms);
    }

    @Test
    void addSymIgnoresAKnownSymbol() {
      TypeSide<String, Sym> ts = typeSide(Set.of(INT, BOOLEAN), PLUS);
      assertFalse(FOR_TY.addSym(ts, PLUS, List.of(STR), STR));
      assertEquals(Set.of(INT, BOOLEAN), ts.tys);
      assertEquals(sorts(PLUS), ts.syms);
    }
  }
}
