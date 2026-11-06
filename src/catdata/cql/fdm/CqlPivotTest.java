package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CqlPivotTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsInputInstance() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertSame(inst, pivot.I);
    }

    @Test
    void constructorCreatesIntermediateSchema() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertNotNull(pivot.intI);
    }

    @Test
    void constructorCreatesMapping() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertNotNull(pivot.F);
    }

    @Test
    void constructorCreatesTargetInstance() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertNotNull(pivot.J);
    }
  }

  @Nested
  class PivotBehavior {

    @Test
    void pivotSchemaHasOneEntityPerElement() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      // single entity "E" with one element "e1" -> intermediate schema has entity "e1"
      assertTrue(pivot.intI.ens.contains("e1"));
    }

    @Test
    void pivotInstanceHasData() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      // J should have data corresponding to the pivot
      assertNotNull(pivot.J.algebra());
    }

    @Test
    void pivotMappingSourceIsIntermediateSchema() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertSame(pivot.intI, pivot.F.src);
    }

    @Test
    void pivotMappingTargetIsOriginalSchema() {
      var inst = FdmTestHelpers.singleElementInstance();
      var pivot = new CqlPivot<>(inst, AqlOptions.initialOptions);
      assertSame(inst.schema(), pivot.F.dst);
    }
  }

  /** The pivot of an instance with two rows of A sharing the row of B, as a program builds it. */
  @Nested
  class OfAProgram {

    private static final String PROGRAM =
        """
        typeside Ty = literal { types String constants x y : String }
        schema S = literal : Ty { entities A B foreign_keys f : A -> B attributes n : A -> String }
        instance I = literal : S {
          generators a1 a2 : A b : B
          equations a1.f = b a2.f = b a1.n = x a2.n = y
        }
        """;

    @SuppressWarnings("unchecked")
    private CqlPivot<String, String, Sym, Fk, Att, String, String, Object, Object> pivot()
        throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(PROGRAM), null);
      d.start();
      var i =
          (Instance<String, String, Sym, Fk, Att, String, String, Object, Object>)
              d.env.defs.insts.get("I");
      return new CqlPivot<>(i, AqlOptions.initialOptions);
    }

    private Term<String, String, Sym, Fk, Att, Object, Object> x() {
      return Term.Sym(Sym.Sym("x", new Pair<>(List.of(), "String")), List.of());
    }

    @Test
    void makesAnEntityPerRow() throws Exception {
      var p = pivot();
      assertEquals(3, p.intI.ens.size());
      assertEquals(2, p.intI.atts.size());
      assertEquals(2, p.intI.fks.size());
      assertEquals(3, p.J.algebra().size());
      assertEquals("Pivot prover (sch)", p.intI.dp().toStringProver());
      assertEquals("Pivot prover (inst)", p.J.dp().toStringProver());
    }

    /** The instance prover asks the original instance, through the row of each generator. */
    @Test
    void decidesAValueOfARowByTheOriginalInstance() throws Exception {
      var p = pivot();
      for (Object row : p.J.algebra().allXs()) {
        String en = p.J.algebra().printX(null, row).toString();
        if (p.intI.atts.containsKey(Att.Att(en, "n"))) {
          Term<String, String, Sym, Fk, Att, Object, Object> n =
              Term.Att(Att.Att(en, "n"), Term.Gen(row));
          String v = p.J.algebra().att(Att.Att(en, "n"), row).toString();
          assertEquals(v.equals("x"), p.J.dp().eq(null, n, x()), v);
        }
      }
      Object row = p.J.algebra().allXs().iterator().next();
      assertTrue(p.J.dp().eq(null, Term.Gen(row), Term.Gen(row)));
      assertThrows(
          RuntimeException.class,
          () -> p.J.dp().eq(Map.of("v", Chc.inRight("0")), Term.Gen(row), Term.Gen(row)));
    }

    /**
     * The schema prover reads its variable's entity, a row's name, as a row of the original
     * instance, so any attribute fails with a ClassCastException.
     */
    @Test
    void cannotDecideAnEquationOfThePivotSchema() throws Exception {
      var p = pivot();
      String en = p.intI.atts.keySet().iterator().next().en;
      Term<String, String, Sym, Fk, Att, Object, Object> n =
          Term.Att(Att.Att(en, "n"), Term.Var("v"));
      assertThrows(
          ClassCastException.class, () -> p.intI.dp().eq(Map.of("v", Chc.inRight(en)), n, x()));
    }
  }

  /** The pivot of an instance read back from a program, with the options it is given. */
  @Nested
  class OfValues {

    @SuppressWarnings("unchecked")
    private Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> j(String program)
        throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      assertNull(d.env.exn);
      return (Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>)
          d.env.defs.insts.get("J");
    }

    /**
     * n is f applied to the labelled null s, and m is a labelled null that the equation f(m) = g(m)
     * constrains; the pivot keeps both values and the equation.
     */
    @Test
    void keepsTheFunctionsNullsAndEquationsOfTheTypeAlgebra() throws Exception {
      var alg =
          j("""
              typeside Ty = literal { types String constants x : String
                functions f g : String -> String }
              schema S = literal : Ty { entities A attributes n m : A -> String }
              instance I = literal : S { generators a : A s : String
                equations a.n = f(s) f(a.m) = g(a.m) options require_consistency = false }
              instance J = pivot I { options require_consistency = false }
              """)
              .algebra();
      Object row = alg.en("0").iterator().next();
      assertEquals("f(\"inl s\")", alg.att(Att.Att("0", "n"), row).toString());
      assertEquals(1, alg.talg().eqsNoDefns().size());
    }

    @Test
    void keepsAValueOfTheSqlTypeside() throws Exception {
      var alg =
          j("""
              typeside Ty = sql
              schema S = literal : Ty { entities A attributes n : A -> Varchar }
              instance I = literal : S { generators a : A equations a.n = hello }
              instance J = pivot I
              """)
              .algebra();
      Object row = alg.en("0").iterator().next();
      assertEquals("hello", alg.att(Att.Att("0", "n"), row).toString());
    }
  }
}
