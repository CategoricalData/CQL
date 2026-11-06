package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DistinctInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstance<>(inst, AqlOptions.initialOptions);
      assertEquals(inst.schema().ens, distinct.schema().ens);
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void gensDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstance<>(inst, AqlOptions.initialOptions);
      assertNotNull(distinct.gens());
    }

    @Test
    void sksDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstance<>(inst, AqlOptions.initialOptions);
      assertNotNull(distinct.sks());
    }

    @Test
    void dpRejectsNonGroundEquation() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstance<>(inst, AqlOptions.initialOptions);
      var dp = distinct.dp();
      assertNotNull(dp.toStringProver());
    }

    @Test
    void algebraSizePreservedOrReduced() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstance<>(inst, AqlOptions.initialOptions);
      assertTrue(distinct.algebra().size("E") <= inst.algebra().size("E"));
    }

    @Test
    void requireConsistencyDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstance<>(inst, AqlOptions.initialOptions);
      assertEquals(inst.requireConsistency(), distinct.requireConsistency());
    }

    @Test
    void allowUnsafeJavaDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstance<>(inst, AqlOptions.initialOptions);
      assertEquals(inst.allowUnsafeJava(), distinct.allowUnsafeJava());
    }
  }

  @Nested
  class EqsMethod {

    @Test
    void eqsIncludesOriginalEquations() {
      var inst = FdmTestHelpers.singleElementInstance();
      var distinct = new DistinctInstance<>(inst, AqlOptions.initialOptions);
      // eqs should not throw
      distinct.eqs((l, r) -> {});
    }
  }

  @SuppressWarnings("unchecked")
  private static Instance<String, String, Sym, Fk, Att, String, String, Object, Object> run(
      String program, String name) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    assertNull(d.env.exn);
    return (Instance<String, String, Sym, Fk, Att, String, String, Object, Object>)
        d.env.defs.insts.get(name);
  }

  /**
   * a1 and a2 share b1 and have nothing else to tell them apart, so {@code distinct I} merges them.
   * b1 and b2 differ by m, which is x for b1 and the labelled null s for b2.
   */
  @Nested
  class OfAProgram {

    private static final String PROGRAM =
        """
        typeside Ty = literal { types String constants x : String }
        schema S = literal : Ty { entities A B foreign_keys f : A -> B attributes m : B -> String }
        instance I = literal : S { generators a1 a2 a3 : A b1 b2 : B s : String
          equations a1.f = b1 a2.f = b1 a3.f = b2 b1.m = x b2.m = s }
        instance D = distinct I
        """;

    private static final Fk F = Fk.Fk("A", "f");
    private static final Att M = Att.Att("B", "m");

    @Test
    void mergesTheRowsThatTheirForeignKeysCannotTellApart() throws Exception {
      var alg = run(PROGRAM, "D").algebra();
      assertEquals(2, alg.size("A"));
      assertEquals(2, alg.size("A"));
      assertEquals(2, alg.size("B"));
      assertEquals(alg.gen("a1"), alg.gen("a2"));
      assertNotEquals(alg.gen("a1"), alg.gen("a3"));
      List<Object> as = new ArrayList<>();
      alg.en("A").forEach(as::add);
      assertEquals(2, as.size());
      assertEquals(alg.gen("b1"), alg.fk(F, alg.gen("a2")));
    }

    /** Two rows reached in two steps are told apart in the second round. */
    @Test
    void followsForeignKeysForAsManyRoundsAsItNeeds() throws Exception {
      var alg =
          run(
                  """
                  typeside Ty = literal { types String constants x y : String }
                  schema S = literal : Ty { entities A B C foreign_keys f : A -> B g : B -> C
                    attributes m : C -> String }
                  instance I = literal : S { generators a1 a2 a3 : A b1 b2 b3 : B c1 c2 : C
                    equations a1.f = b1 a2.f = b2 a3.f = b3 b1.g = c1 b2.g = c1 b3.g = c2
                    c1.m = x c2.m = y }
                  instance D = distinct I
                  """,
                  "D")
              .algebra();
      assertEquals(List.of(2, 2, 2), List.of(alg.size("A"), alg.size("B"), alg.size("C")));
      assertEquals(alg.gen("a1"), alg.gen("a2"));
      assertEquals(alg.gen("b1"), alg.gen("b2"));
    }

    @Test
    void decidesEquationsByTheMergedRows() throws Exception {
      var dp = run(PROGRAM, "D").dp();
      Term<String, String, Sym, Fk, Att, String, String> a1 = Term.Gen("a1");
      assertTrue(dp.eq(null, a1, Term.Gen("a2")));
      assertFalse(dp.eq(null, a1, Term.Gen("a3")));
      Term<String, String, Sym, Fk, Att, String, String> x =
          Term.Sym(Sym.Sym("x", new Pair<>(Collections.emptyList(), "String")), List.of());
      assertTrue(dp.eq(null, Term.Att(M, Term.Fk(F, a1)), x));
    }

    @Test
    void wrapsTheAlgebraOfTheInstance() throws Exception {
      var i = run(PROGRAM, "I").algebra();
      var alg = run(PROGRAM, "D").algebra();
      Object b2 = alg.gen("b2");
      assertEquals(i.att(M, i.gen("b2")).toString(), alg.att(M, b2).toString());
      assertEquals(i.sk("s").toString(), alg.sk("s").toString());
      assertEquals(i.repr("B", i.gen("b2")), alg.repr("B", b2));
      assertEquals(i.printX("B", i.gen("b2")), alg.printX("B", b2));
      assertTrue(
          alg.toStringProver().startsWith("Distinct-instance algebra wrapper of "),
          alg.toStringProver());
      assertEquals(i.hasNulls(), alg.hasNulls());
      assertEquals(i.hasFreeTypeAlgebra(), alg.hasFreeTypeAlgebra());
      assertEquals(i.talg().sks, alg.talg().sks);
      Object s = alg.talg().sks.keySet().iterator().next();
      assertEquals(i.printY("String", s), alg.printY("String", s));
      assertEquals(i.reprT_prot(s), alg.reprT_prot(s));
    }

    /** The null of b2's m, given as a row and an attribute, names the row's representative. */
    @Test
    void namesTheNullOfARowByItsRepresentative() throws Exception {
      var alg = run(PROGRAM.replace(" b2.m = s }", " }").replace(" s : String", ""), "D").algebra();
      Object b2 = alg.gen("b2");
      Object y = alg.talg().sks.keySet().iterator().next();
      assertEquals(Chc.inRight(new Pair<>(b2, M)), alg.reprT_prot(y));
    }
  }
}
