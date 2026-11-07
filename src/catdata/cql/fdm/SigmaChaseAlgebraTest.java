package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SigmaChaseAlgebraTest {

  @Nested
  class Constructor {

    @Test
    void constructorCreatesAlgebra() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      Map<String, Set<Pair<String, String>>> extra = new HashMap<>();
      extra.put("E", Collections.emptySet());
      var alg = new SigmaChaseAlgebra<>(mapping, inst, extra, AqlOptions.initialOptions);
      assertNotNull(alg);
    }

    @Test
    void schemaReturnsMappingDst() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      Map<String, Set<Pair<String, String>>> extra = new HashMap<>();
      extra.put("E", Collections.emptySet());
      var alg = new SigmaChaseAlgebra<>(mapping, inst, extra, AqlOptions.initialOptions);
      assertEquals(schema.ens, alg.schema().ens);
    }

    @Test
    void algebraHasElements() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      Map<String, Set<Pair<String, String>>> extra = new HashMap<>();
      extra.put("E", Collections.emptySet());
      var alg = new SigmaChaseAlgebra<>(mapping, inst, extra, AqlOptions.initialOptions);
      assertEquals(1, alg.size("E"));
    }
  }

  @Nested
  class TheInstance {

    @Test
    void theInstIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      Map<String, Set<Pair<String, String>>> extra = new HashMap<>();
      extra.put("E", Collections.emptySet());
      var alg = new SigmaChaseAlgebra<>(mapping, inst, extra, AqlOptions.initialOptions);
      assertNotNull(alg.theInst);
    }

    @Test
    void theInstSchemaMatchesDst() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      Map<String, Set<Pair<String, String>>> extra = new HashMap<>();
      extra.put("E", Collections.emptySet());
      var alg = new SigmaChaseAlgebra<>(mapping, inst, extra, AqlOptions.initialOptions);
      assertEquals(schema.ens, alg.theInst.schema().ens);
    }
  }

  /**
   * {@code sigma_chase F I} of a program: a1 and a2 share b, so their g is one row, a1.n is the
   * constant x and a2.n is f(s), with s a labelled null.
   */
  @Nested
  class OfAProgram {

    private static final String PROGRAM =
        """
        typeside Ty = literal { types String constants x : String functions f : String -> String
          equations forall v. f(f(v)) = f(v) options prover = completion }
        schema S = literal : Ty { entities A B foreign_keys f : A -> B attributes n : A -> String }
        schema T = literal : Ty { entities X Y foreign_keys g : X -> Y attributes p : X -> String }
        mapping F = literal : S -> T { entity B -> Y entity A -> X foreign_keys f -> g
          attributes n -> lambda v. v.p }
        instance I = literal : S { generators a1 a2 : A b : B s : String
          equations a1.f = b a2.f = b a1.n = x a2.n = f(s) }
        instance K = sigma_chase F I
        """;

    private static final Att P = Att.Att("X", "p");

    private static Instance<String, String, Sym, Fk, Att, String, String, Integer, Object> k()
        throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(PROGRAM), null);
      d.start();
      @SuppressWarnings("unchecked")
      var k =
          (Instance<String, String, Sym, Fk, Att, String, String, Integer, Object>)
              d.env.defs.insts.get("K");
      return k;
    }

    private static Term<String, String, Sym, Fk, Att, String, String> gen(String g) {
      return Term.Gen(g);
    }

    @Test
    void printsEachRowByItsRepresentative() throws Exception {
      var k = k();
      var alg = k.algebra();
      Integer a1 = alg.gen("a1");
      assertEquals("a1", alg.printX("X", a1));
      assertEquals(Term.Gen("a1"), alg.repr("X", a1));
      Integer b = alg.gen("b");
      assertEquals(b, alg.fk(Fk.Fk("X", "g"), a1));
      assertEquals(alg.repr("Y", b).toString(), alg.printX("Y", b));
      assertEquals("a1.p", alg.printY("String", Chc.inRight(new Pair<>(a1, P))));
      assertEquals("s", alg.printY("String", Chc.inLeft("s")));
    }

    @Test
    void failsToPrintANumberThatIsNotARow() throws Exception {
      var alg = k().algebra();
      RuntimeException e = assertThrows(RuntimeException.class, () -> alg.printX("X", 99));
      assertEquals("Can't get int 99 at entity X v is 99 and offset 0 and top 2 ", e.getMessage());
    }

    @Test
    void givesEachAttributeItsValue() throws Exception {
      var alg = k().algebra();
      assertEquals("x", alg.att(P, alg.gen("a1")).toString());
      assertEquals("f(\"inl s\")", alg.att(P, alg.gen("a2")).toString());
      assertEquals("\"inl s\"", alg.sk("s").toString());
    }

    @Test
    void decidesEquationsByRowsAndByTheTypeAlgebra() throws Exception {
      var dp = k().dp();
      var g1 = Term.Fk(Fk.Fk("X", "g"), gen("a1"));
      var g2 = Term.Fk(Fk.Fk("X", "g"), gen("a2"));
      assertTrue(dp.eq(null, g1, g2));
      assertFalse(dp.eq(null, gen("a1"), gen("a2")));
      assertTrue(dp.eq(null, Term.Att(P, gen("a1")), Term.Att(P, gen("a1"))));
      assertFalse(dp.eq(null, Term.Att(P, gen("a1")), Term.Att(P, gen("a2"))));
      assertTrue(
          dp.toStringProver().startsWith("Sigma Chase Algebra.\n\nSigmaChaseAlgebra [chase="));
    }

    /** {@code hasNulls} is false although s is a labelled null. */
    @Test
    void reportsItsOptionsAndItsTypeAlgebra() throws Exception {
      var k = k();
      assertTrue(k.requireConsistency());
      assertTrue(k.allowUnsafeJava());
      assertFalse(k.algebra().hasNulls());
      assertTrue(k.algebra().hasFreeTypeAlgebra());
    }
  }
}
