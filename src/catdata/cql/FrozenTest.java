package catdata.cql;

import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.literal;
import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import catdata.cql.fdm.LiteralInstance;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** A query block's from and where clauses as an instance, and its join order. */
class FrozenTest {

  private static final Att N = Att.Att("A", "n");
  private static final Fk F = Fk.Fk("A", "f");

  /** A, B and C, with f : A -> B and n : A -> Varchar. */
  private static final Schema<String, String, Sym, Fk, Att> SCHEMA =
      sqlSchema(List.of("A", "B", "C"), Map.of(N, "A"), Map.of(F, new Pair<>("A", "B")));

  private static Term<String, String, Sym, Fk, Att, String, String> gen(String g) {
    return Term.Gen(g);
  }

  /** from a:A b:B c:C, in that order, with no where clause. */
  private static Frozen<String, String, Sym, Fk, Att> abc() {
    Map<String, String> gens = new LinkedHashMap<>();
    gens.put("a", "A");
    gens.put("b", "B");
    gens.put("c", "C");
    return new Frozen<>(gens, Map.of(), List.of("a", "b", "c"), new HashSet<>(), SCHEMA, ops());
  }

  /** An instance with the given numbers of rows in A, B and C. */
  private static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      sized(int a, int b, int c) {
    Map<String, String> gens = new LinkedHashMap<>();
    for (int k = 0; k < b; k++) {
      gens.put("b" + k, "B");
    }
    for (int k = 0; k < c; k++) {
      gens.put("c" + k, "C");
    }
    List<
            Pair<
                Term<String, String, Sym, Fk, Att, String, String>,
                Term<String, String, Sym, Fk, Att, String, String>>>
        eqs = new ArrayList<>();
    for (int k = 0; k < a; k++) {
      gens.put("a" + k, "A");
      eqs.add(new Pair<>(Term.Fk(F, gen("a" + k)), gen("b0")));
    }
    return literal(SCHEMA, gens, eqs);
  }

  @Nested
  class Construction {

    @Test
    void keepsItsVariablesEquationsAndOrder() {
      Set<
              Pair<
                  Term<String, String, Sym, Fk, Att, String, String>,
                  Term<String, String, Sym, Fk, Att, String, String>>>
          eqs = new HashSet<>();
      eqs.add(new Pair<>(Term.Fk(F, gen("a")), gen("b")));
      var f =
          new Frozen<>(
              Map.of("a", "A", "b", "B"),
              Map.of("y", VARCHAR),
              List.of("b", "a"),
              eqs,
              SCHEMA,
              ops());
      assertEquals("A", f.gens().get("a"));
      assertEquals(VARCHAR, f.sks().get("y"));
      assertEquals(List.of("b", "a"), f.order);
      assertSame(eqs, f.eqsAsIterable());
      List<
              Pair<
                  Term<String, String, Sym, Fk, Att, String, String>,
                  Term<String, String, Sym, Fk, Att, String, String>>>
          seen = new ArrayList<>();
      f.eqs((l, r) -> seen.add(new Pair<>(l, r)));
      assertEquals(List.of(new Pair<>(Term.Fk(F, gen("a")), gen("b"))), seen);
      assertSame(SCHEMA, f.schema());
      assertFalse(f.requireConsistency());
      assertTrue(f.allowUnsafeJava());
    }

    /** Entity-sorted variables become generators, the rest and the parameters labelled nulls. */
    @Test
    void splitsAMixedContext() {
      Map<String, Chc<String, String>> vars = new LinkedHashMap<>();
      vars.put("a", Chc.inLeft("A"));
      vars.put("v", Chc.inRight(VARCHAR));
      var eq =
          new Eq<String, String, Sym, Fk, Att, String, String>(
              null, Term.Att(N, gen("a")), Term.Sk("v"));
      var f =
          new Frozen<>(Map.of("p", VARCHAR), vars, List.of(eq), SCHEMA, ops(), List.of("a", "v"));
      assertEquals(Map.of("a", "A"), f.gens);
      assertEquals(Map.of("p", VARCHAR, "v", VARCHAR), f.sks);
      assertEquals(Set.of(new Pair<>(eq.lhs, eq.rhs)), f.eqs);
    }

    @Test
    void buildsItsProverAndAlgebraOnce() {
      var f = abc();
      assertSame(f.dp(), f.dp());
      var alg = f.algebra();
      assertSame(alg, f.algebra());
      assertEquals(4, f.size(), "a, b, c and a.f");
      It.ID a = alg.gen("a");
      assertEquals("a", alg.printX("A", a).toString());
      var y = Chc.<String, Pair<It.ID, Att>>inRight(new Pair<>(a, N));
      assertEquals("a.n", alg.printY(VARCHAR, y).toString());
      var withNull =
          new Frozen<>(
              Map.of("a", "A"), Map.of("p", VARCHAR), List.of("a"), new HashSet<>(), SCHEMA, ops());
      var nul = Chc.<String, Pair<It.ID, Att>>inLeft("p");
      assertEquals("p", withNull.algebra().printY(VARCHAR, nul).toString());
    }
  }

  @Nested
  class Order {

    @Test
    void keepsTheGivenOrderUnlessAskedToReorder() {
      var f = abc();
      AqlOptions off = new AqlOptions(ops(), AqlOption.eval_reorder_joins, false);
      assertSame(f.order, f.order(off, sized(1, 1, 1)));
      AqlOptions shallow = new AqlOptions(ops(), AqlOption.eval_max_plan_depth, 2);
      assertSame(f.order, f.order(shallow, sized(1, 1, 1)));
    }

    @Test
    void hasNothingToOrderWithoutVariables() {
      var f =
          new Frozen<String, String, Sym, Fk, Att>(
              Map.of(), Map.of(), List.of(), new HashSet<>(), SCHEMA, ops());
      assertEquals(List.of(), f.order(ops(), sized(1, 1, 1)));
    }

    /**
     * Issue 68: with no where clause every order costs the size of the product, but the estimate
     * leaves out the last variable, so the biggest entity, B, always comes last.
     */
    @Test
    void putsTheBiggestEntityLast() {
      List<String> plan = abc().order(ops(), sized(1, 100, 10));
      assertEquals(Set.of("a", "b", "c"), new HashSet<>(plan));
      assertEquals("b", plan.get(2));
    }

    @Test
    void ordersASingleVariable() {
      var one =
          new Frozen<String, String, Sym, Fk, Att>(
              Map.of("a", "A"), Map.of(), List.of("a"), new HashSet<>(), SCHEMA, ops());
      assertEquals(List.of("a"), one.order(ops(), sized(2, 1, 1)));
      var nul =
          new Frozen<String, String, Sym, Fk, Att>(
              Map.of(), Map.of("y", VARCHAR), List.of("y"), new HashSet<>(), SCHEMA, ops());
      assertEquals(List.of("y"), nul.order(ops(), sized(2, 1, 1)));
    }

    /** a.f = b halves the selectivity of a and b; y, a labelled null, is costly anywhere. */
    @Test
    void weighsTheWhereClauseAndTheLabelledNulls() {
      Set<
              Pair<
                  Term<String, String, Sym, Fk, Att, String, String>,
                  Term<String, String, Sym, Fk, Att, String, String>>>
          eqs = new HashSet<>();
      eqs.add(new Pair<>(Term.Fk(F, gen("a")), gen("b")));
      Map<String, String> gens = new LinkedHashMap<>();
      gens.put("a", "A");
      gens.put("b", "B");
      var f =
          new Frozen<>(gens, Map.of("y", VARCHAR), List.of("a", "b", "y"), eqs, SCHEMA, ops());
      List<String> plan = f.order(ops(), sized(3, 2, 1));
      assertEquals(Set.of("a", "b", "y"), new HashSet<>(plan));
      assertEquals("y", plan.get(2));
    }

    @Test
    void putsALabelledNullAfterTheGenerators() {
      Map<String, String> gens = Map.of("a", "A");
      var f =
          new Frozen<>(
              gens, Map.of("y", VARCHAR), List.of("y", "a"), new HashSet<>(), SCHEMA, ops());
      assertEquals(List.of("a", "y"), f.order(ops(), sized(2, 1, 1)));
    }
  }
}
