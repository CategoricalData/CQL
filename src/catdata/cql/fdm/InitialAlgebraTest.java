package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.Pair;
import catdata.cql.AqlOptions;
import catdata.cql.AqlProver.ProverName;
import catdata.cql.Collage;
import catdata.cql.Collage.CCollage;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Term;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InitialAlgebraTest {

  @Nested
  class ViaSigmaInstance {

    @Test
    void initialAlgebraCreatedBySigma() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      // SigmaInstance internally creates an InitialAlgebra
      assertNotNull(sigma.algebra());
      assertTrue(sigma.algebra().size("E") > 0);
    }
  }

  private static AqlMultiDriver run(String program) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    return d;
  }

  private static String error(AqlMultiDriver d) {
    return d.env.exn == null ? "" : d.env.exn.getMessage();
  }

  @SuppressWarnings("unchecked")
  private static Instance<String, String, Sym, Fk, Att, String, String, Integer, Object> instance(
      AqlMultiDriver d) {
    return (Instance<String, String, Sym, Fk, Att, String, String, Integer, Object>)
        d.env.defs.insts.get("I");
  }

  /**
   * I proves tt = ff, which the typeside does not. The typeside has an equation of its own, which
   * is what sends {@code fast_consistency_check = false} to a branch that checks nothing.
   */
  @Nested
  class Consistency {

    private static final String TYPESIDE =
        """
        typeside Ty = literal { types Bool constants tt ff : Bool functions not : Bool -> Bool
          equations not(not(tt)) = tt options prover = completion }
        """;

    private static final String REST =
        """
        schema S = literal : Ty { entities A attributes n : A -> Bool }
        instance I = literal : S { generators a : A equations a.n = tt a.n = ff }
        """;

    private static final String SLOW = "options fast_consistency_check = false\n";

    @Test
    void rejectsATypeAlgebraThatEquatesTwoConstants() throws Exception {
      String msg = error(run(TYPESIDE + REST));
      assertTrue(msg.contains("Not necessarily conservative over the typeside"), msg);
    }

    @Test
    void acceptsItWithTheSlowCheck() throws Exception {
      AqlMultiDriver d = run(SLOW + TYPESIDE + REST);
      assertEquals("", error(d));
      var alg = instance(d).algebra();
      assertEquals(2, alg.talg().eqsNoDefns().size());
      assertTrue(alg.hasFreeTypeAlgebra());
    }

    @Test
    void reportsItConsistentWithTheSlowCheck() throws Exception {
      String program =
          TYPESIDE
              + REST.replace("a.n = ff }", "a.n = ff options require_consistency = false }")
              + "command c = assert_consistent I\n";
      assertTrue(error(run(program)).startsWith("Not necessarily consistent: type algebra is"));
      AqlMultiDriver d = run(SLOW + program);
      assertEquals("", error(d));
      assertEquals("Consistent", d.env.defs.get("c", Kind.PRAGMA).toString());
    }

    @Test
    void checksATypesideWithoutEquationsEitherWay() throws Exception {
      String typeside = "typeside Ty = literal { types Bool constants tt ff : Bool }\n";
      String msg = error(run(SLOW + typeside + REST));
      assertTrue(msg.contains("Not necessarily conservative over the typeside"), msg);
    }
  }

  /** a has the value x for n, and s is a labelled null for b's. */
  @Nested
  class Accessors {

    private static final String PROGRAM =
        """
        typeside Ty = literal { types String constants x : String }
        schema S = literal : Ty { entities A attributes n : A -> String }
        instance I = literal : S { generators a b : A s : String equations a.n = x b.n = s }
        """;

    private static final Att N = Att.Att("A", "n");

    @Test
    void printsANullByItsNameAndAnAttributeByItsRow() throws Exception {
      var alg = instance(run(PROGRAM)).algebra();
      assertEquals("s", alg.printY("String", Chc.inLeft("s")));
      Integer a = alg.gen("a");
      assertEquals("a.n", alg.printY("String", Chc.inRight(new Pair<>(a, N))));
      assertFalse(alg.talgToString().isEmpty());
    }

    @Test
    void decidesOnlyGroundEquations() throws Exception {
      var dp = instance(run(PROGRAM)).dp();
      Term<String, String, Sym, Fk, Att, String, String> a = Term.Gen("a");
      assertTrue(dp.eq(null, a, a));
      RuntimeException e =
          assertThrows(
              RuntimeException.class, () -> dp.eq(Map.of("v", Chc.inRight("A")), a, Term.Var("v")));
      assertEquals("Cannot answer a non-ground equation", e.getMessage());
    }

    @Test
    void refusesACyclicSchemaWithoutEquations() throws Exception {
      String msg =
          error(
              run(
                  """
                  typeside Ty = literal { types String }
                  schema S = literal : Ty { entities A foreign_keys f : A -> A }
                  instance I = literal : S { generators a : A }
                  """));
      assertTrue(
          msg.contains(
              "An instance with a cyclic schema, generators, and no equations may diverge."),
          msg);
    }

    /** The constructor that EgglogProver uses, given the instance's own decision procedure. */
    @Test
    void buildsTheSameRowsFromAGivenDecisionProcedure() throws Exception {
      Instance<String, String, Sym, Fk, Att, String, String, Integer, Object> i =
          instance(run(PROGRAM));
      Collage<String, String, Sym, Fk, Att, String, String> col = new CCollage<>(i.collage());
      var alg =
          new InitialAlgebra<>(
              AqlOptions.initialOptions,
              i.schema(),
              col,
              z -> z,
              (t, s) -> s,
              i.dp(),
              ProverName.auto);
      assertEquals(i.algebra().size("A"), alg.size("A"));
      assertEquals("a.n", alg.printY("String", Chc.inRight(new Pair<>(alg.gen("a"), N))));
      assertEquals("x", alg.att(N, alg.gen("a")).toString());
    }
  }
}
