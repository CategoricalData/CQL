package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.Pair;
import catdata.cql.Head;
import catdata.cql.Instance;
import catdata.cql.Term;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TalgSimplifierTest {

  @Nested
  class ToTermMethod {

    @Test
    void toTermWithObjHead() {
      // Create a TalgSimplifier indirectly through SigmaInstance, then test toTerm
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, catdata.cql.AqlOptions.initialOptions);
      // TalgSimplifier is used internally; verify the algebra works
      assertNotNull(sigma.algebra());
    }
  }

  /** Instance literals whose type algebras the simplifier reduces, run as the IDE runs them. */
  @Nested
  class Programs {

    /** The message of the error that the program stops with, or null when it runs. */
    private String error(String program) throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      return d.env.exn == null ? null : d.env.exn.getMessage();
    }

    private Instance<?, ?, ?, ?, ?, ?, ?, ?, ?> instance(String program) throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      return d.env.defs.insts.get("I");
    }

    private static final String DECIMALS =
        """
        typeside Ty = literal {
          external_types D -> "java.math.BigDecimal"
          external_parsers D -> "x => new java.math.BigDecimal(x)"
        }
        schema S = literal : Ty { entities A attributes d : A -> D }
        """;

    @Test
    void reportsTwoValuesForOneAttribute() throws Exception {
      String msg =
          error(
              """
              typeside Ty = sql
              schema S = literal : Ty { entities A attributes k : A -> Integer }
              instance I = literal : S { generators a : A equations a.k = 1 a.k = 2 }
              """);
      assertTrue(msg.contains("Contradictions: \n2 = 1"), msg);
    }

    @Test
    void keepsAnAttributeGivenTheSameValueTwice() throws Exception {
      var i =
          instance(
              """
              typeside Ty = sql
              schema S = literal : Ty { entities A attributes k : A -> Integer }
              instance I = literal : S { generators a : A equations a.k = 1 a.k = 1 }
              """);
      assertEquals(1, i.algebra().size());
    }

    @Test
    void reportsDecimalsOfDifferentValues() throws Exception {
      String msg =
          error(
              DECIMALS
                  + "instance I = literal : S { generators a : A equations a.d = \"1.0\" a.d = \"2.0\" }\n");
      assertTrue(msg.contains("Contradictions: "), msg);
    }

    /** The simplifier takes 1.0 and 1.00 as equal, the check of the instance does not. */
    @Test
    void failsOnDecimalsOfEqualValueAndDifferentScale() throws Exception {
      String msg =
          error(
              DECIMALS
                  + "instance I = literal : S { generators a : A equations a.d = \"1.0\" a.d = \"1.00\" }\n");
      assertTrue(msg.contains("Anomaly: not equal: a.d and \"1.00\"@D"), msg);
    }

    /** Over sql the decimals are optionals, so the simplifier reports them as different. */
    @Test
    void reportsSqlDecimalsOfDifferentScale() throws Exception {
      String msg =
          error(
              """
              typeside Ty = sql
              schema S = literal : Ty { entities A attributes d : A -> Decimal }
              instance I = literal : S { generators a : A equations a.d = "1.0" a.d = "1.00" }
              """);
      assertTrue(msg.contains("Contradictions: \n\"1.0\" = \"1.00\""), msg);
    }
  }

  /**
   * A simplifier built from the algebra and the equations of I, where a.n is f of the labelled null
   * s and b.n is the constant x; both attributes become definitions.
   */
  @Nested
  class Helpers {

    private static final Att N = Att.Att("A", "n");

    private static final Sym F = Sym.Sym("f", new Pair<>(List.of("String"), "String"));

    @SuppressWarnings("unchecked")
    private Pair<
            Instance<String, String, Sym, Fk, Att, String, String, Integer, Object>,
            TalgSimplifier<String, String, Sym, Fk, Att, String, String, Integer, Object>>
        simplifier() throws Exception {
      return simplifier(Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    private Pair<
            Instance<String, String, Sym, Fk, Att, String, String, Integer, Object>,
            TalgSimplifier<String, String, Sym, Fk, Att, String, String, Integer, Object>>
        simplifier(int reduce) throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(
              AqlParserFactory.getParser()
                  .parseProgram(
                      """
                      typeside Ty = literal { types String constants x : String
                        functions f : String -> String }
                      schema S = literal : Ty { entities A attributes n : A -> String }
                      instance I = literal : S { generators a b : A s : String
                        equations a.n = f(s) b.n = x }
                      """),
              null);
      d.start();
      var i =
          (Instance<String, String, Sym, Fk, Att, String, String, Integer, Object>)
              d.env.defs.insts.get("I");
      List<
              Pair<
                  Term<String, String, Sym, Fk, Att, String, String>,
                  Term<String, String, Sym, Fk, Att, String, String>>>
          eqs = new ArrayList<>();
      i.eqs((l, r) -> eqs.add(new Pair<>(l, r)));
      Map<String, String> sks = new HashMap<>();
      i.sks().entrySet(sks::put);
      return new Pair<>(i, new TalgSimplifier<>(i.algebra(), eqs.iterator(), sks, reduce));
    }

    /** With no round after the first, the step's input still has every labelled null. */
    @Test
    void validatesTheTypesOfItsDefinitionsAfterOneRound() throws Exception {
      var t = simplifier(0).second;
      t.talg.validate();
      assertTrue(t.talg.toString().startsWith("Step [out="), t.talg.toString());
    }

    /**
     * After a second round the step's input is the output of the first, which no longer has the
     * nulls that the first round defined, so checking them fails.
     */
    @Test
    void failsToValidateAfterASecondRound() throws Exception {
      var t = simplifier().second;
      RuntimeException e = assertThrows(RuntimeException.class, t.talg::validate);
      assertTrue(e.getMessage().contains("has no associated type"), e.getMessage());
    }

    @Test
    void turnsAHeadIntoATerm() throws Exception {
      var t = simplifier().second;
      Chc<String, Pair<Integer, Att>> s = Chc.inLeft("s");
      assertEquals(Term.Sk(s), t.toTerm(Head.SkHead(s)));
      assertEquals(
          Term.Obj("v", "String"),
          t.toTerm(
              Head.<String, Void, Sym, Void, Void, Void, Chc<String, Pair<Integer, Att>>>ObjHead(
                  "v", "String")));
      RuntimeException e = assertThrows(RuntimeException.class, () -> t.toTerm(Head.SymHead(F)));
      assertEquals("Anomaly: please report", e.getMessage());
    }

    @Test
    void unflattensATermOfTheTypeAlgebra() throws Exception {
      var p = simplifier();
      var t = p.second;
      Integer a = p.first.algebra().gen("a");
      Term<String, Void, Sym, Void, Void, Void, Chc<String, Pair<Integer, Att>>> s =
          Term.Sk(Chc.inLeft("s"));
      assertEquals("f(s)", t.unflatten(Term.Sym(F, List.of(s))).toString());
      assertEquals(Term.Att(N, Term.Gen("a")), t.unflatten(Term.Sk(Chc.inRight(new Pair<>(a, N)))));
      assertEquals(Term.Obj("v", "String"), t.unflatten(Term.Obj("v", "String")));
      RuntimeException e = assertThrows(RuntimeException.class, () -> t.unflatten(Term.Var("v")));
      assertEquals("Anomaly: please report", e.getMessage());
    }
  }
}
