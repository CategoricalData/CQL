package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.Pair;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Term;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DistinctInstanceParallelTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertEquals(inst.schema().ens, distinct.schema().ens);
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void gensDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertNotNull(distinct.gens());
    }

    @Test
    void sksDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertNotNull(distinct.sks());
    }

    @Test
    void algebraSizePreservedOrReduced() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertTrue(distinct.algebra().size("E") <= inst.algebra().size("E"));
    }

    @Test
    void requireConsistencyDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertEquals(inst.requireConsistency(), distinct.requireConsistency());
    }

    @Test
    void allowUnsafeJavaDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      assertEquals(inst.allowUnsafeJava(), distinct.allowUnsafeJava());
    }
  }

  @Nested
  class EqsMethod {

    @Test
    void eqsDoesNotThrow() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstanceParallel<>(inst, AqlOptions.initialOptions);
      distinct.eqs((l, r) -> {});
    }
  }

  /**
   * The parallel implementation, which nothing in CQL calls, against {@code distinct I}. On rows
   * that only attributes tell apart the two agree; across a foreign key the parallel one merges
   * rows that {@code distinct I} keeps apart, and its own final check rejects the result.
   */
  @Nested
  class AgainstDistinctInstance {

    /** a1 and a2 share x and b, a3 has y. */
    private static final String ATTRIBUTES =
        """
        typeside Ty = literal { types String constants x y : String }
        schema S = literal : Ty { entities A B attributes n : A -> String m : B -> String }
        instance I = literal : S { generators a1 a2 a3 : A b : B
          equations a1.n = x a2.n = x a3.n = y b.m = x }
        """;

    private static final String ONE_STEP =
        """
        typeside Ty = literal { types String constants x y : String }
        schema S = literal : Ty { entities A B foreign_keys f : A -> B attributes m : B -> String }
        instance I = literal : S { generators a1 a2 a3 : A b1 b2 : B
          equations a1.f = b1 a2.f = b1 a3.f = b2 b1.m = x b2.m = y }
        """;

    @SuppressWarnings("unchecked")
    private Instance<String, String, Sym, Fk, Att, String, String, Object, Object> instance(
        String program) throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      return (Instance<String, String, Sym, Fk, Att, String, String, Object, Object>)
          d.env.defs.insts.get("I");
    }

    @Test
    void mergesTheRowsThatAttributesCannotTellApart() throws Exception {
      var i = instance(ATTRIBUTES);
      var parallel = new DistinctInstanceParallel<>(i, AqlOptions.initialOptions).algebra();
      var sequential = new DistinctInstance<>(i, AqlOptions.initialOptions).algebra();
      assertEquals(2, parallel.size("A"));
      assertEquals(sequential.size("A"), parallel.size("A"));
      assertEquals(sequential.size("B"), parallel.size("B"));
      assertEquals(parallel.gen("a1"), parallel.gen("a2"));
      assertNotEquals(parallel.gen("a1"), parallel.gen("a3"));
    }

    @Test
    void failsWhenAForeignKeyTellsRowsApart() throws Exception {
      var i = instance(ONE_STEP);
      assertEquals(2, new DistinctInstance<>(i, AqlOptions.initialOptions).algebra().size("A"));
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> new DistinctInstanceParallel<>(i, AqlOptions.initialOptions));
      assertTrue(e.getMessage().endsWith(" not preserved in quotient (anomaly)."), e.getMessage());
    }

    @Test
    void decidesEquationsByTheMergedRows() throws Exception {
      var d = new DistinctInstanceParallel<>(instance(ATTRIBUTES), AqlOptions.initialOptions);
      Term<String, String, Sym, Fk, Att, String, String> a1 = Term.Gen("a1");
      assertTrue(d.dp().eq(null, a1, Term.Gen("a2")));
      assertFalse(d.dp().eq(null, a1, Term.Gen("a3")));
      Term<String, String, Sym, Fk, Att, String, String> x =
          Term.Sym(Sym.Sym("x", new Pair<>(Collections.emptyList(), "String")), List.of());
      assertTrue(d.dp().eq(null, Term.Att(Att.Att("A", "n"), a1), x));
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> d.dp().eq(Map.of("v", Chc.inRight("A")), Term.Var("v"), Term.Var("v")));
      assertEquals("Cannot answer a non-ground equation", e.getMessage());
    }

    @Test
    void wrapsTheAlgebraOfTheInstance() throws Exception {
      var i = instance(ATTRIBUTES);
      var alg = new DistinctInstanceParallel<>(i, AqlOptions.initialOptions).algebra();
      var ia = i.algebra();
      Object a3 = alg.gen("a3");
      assertEquals(ia.printX("A", a3), alg.printX("A", a3));
      assertEquals(ia.repr("A", a3), alg.repr("A", a3));
      Att n = Att.Att("A", "n");
      assertEquals(ia.att(n, a3).toString(), alg.att(n, a3).toString());
      assertEquals(ia.hasNulls(), alg.hasNulls());
      assertEquals(ia.hasFreeTypeAlgebra(), alg.hasFreeTypeAlgebra());
      assertEquals(ia.talg().sks, alg.talg().sks);
      assertTrue(alg.toStringProver().length() > 0);
    }
  }
}
