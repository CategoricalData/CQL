package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.Pair;
import catdata.cql.AqlOptions;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage;
import catdata.cql.Collage.CCollage;
import catdata.cql.Instance;
import catdata.cql.Mapping;
import catdata.cql.Term;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SigmaLeftKanAlgebra}, the algebra that {@code sigma_chase F I { options
 * chase_style = sequential }} builds. Its {@code compute} stops after the first round that changes
 * anything, so only inputs that need a single round come out complete, and an input that needs no
 * round at all never finishes, which is why no test starts from one.
 */
class SigmaLeftKanAlgebraTest {

  private static final Fk G = Fk.Fk("X", "g");

  /**
   * The instance {@code I} has one row a, which the mapping sends to the entity X of the target.
   */
  private static String oneRow(String target) {
    return "typeside Ty = literal { types String }\n"
        + "schema S = literal : Ty { entities A }\n"
        + "schema T = literal : Ty { "
        + target
        + " }\n"
        + "mapping F = literal : S -> T { entity A -> X }\n"
        + "instance I = literal : S { generators a : A }\n";
  }

  private static final String COLLAPSE =
      """
      typeside Ty = literal { types String }
      schema S = literal : Ty { entities A B foreign_keys f : A -> B }
      schema T = literal : Ty { entities X }
      mapping F = literal : S -> T { entity B -> X entity A -> X foreign_keys f -> identity }
      """;

  /**
   * Builds the algebra of {@code sigma F I} as the sequential style of {@code sigma_chase} does:
   * the collage of the target schema with the generators and labelled nulls of {@code I}.
   */
  @SuppressWarnings("unchecked")
  private static SigmaLeftKanAlgebra<
          String, String, Sym, Fk, Att, String, Fk, Att, String, String, Object, Object>
      algebra(String program) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    Mapping<String, String, Sym, Fk, Att, String, Fk, Att> f =
        (Mapping<String, String, Sym, Fk, Att, String, Fk, Att>) d.env.defs.maps.get("F");
    Instance<String, String, Sym, Fk, Att, String, String, Object, Object> i =
        (Instance<String, String, Sym, Fk, Att, String, String, Object, Object>)
            d.env.defs.insts.get("I");
    Collage<String, String, Sym, Fk, Att, String, String> col = new CCollage<>(f.dst.collage());
    i.sks().entrySet((a, b) -> col.sks().put(a, b));
    i.gens().entrySet((a, b) -> col.gens().put(a, f.ens.get(b)));
    int reduce = (Integer) AqlOptions.initialOptions.getOrDefault(AqlOption.talg_reduction);
    return new SigmaLeftKanAlgebra<>(f, i, col, reduce);
  }

  private static String message(String program) {
    return assertThrows(RuntimeException.class, () -> algebra(program)).getMessage();
  }

  @Nested
  class ViaSigmaInstance {

    @Test
    void algebraComputesCorrectly() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var mapping = FdmTestHelpers.identityMapping(schema);
      var inst = FdmTestHelpers.singleElementInstance();
      var sigma = new SigmaInstance<>(mapping, inst, AqlOptions.initialOptions);
      assertEquals(1, sigma.algebra().size("E"));
    }
  }

  @Nested
  class Rows {

    @Test
    void addsTheRowThatAForeignKeyOfTheTargetNeeds() throws Exception {
      var alg = algebra(oneRow("entities X Y foreign_keys g : X -> Y"));
      assertEquals(1, alg.size("X"));
      assertEquals(1, alg.size("Y"));
      Integer a = alg.gen("a");
      assertEquals(List.of(a), List.copyOf(alg.en("X")));
      Integer y = alg.fk(G, a);
      assertEquals(List.of(y), List.copyOf(alg.en("Y")));
      assertEquals(Term.Fk(G, Term.Gen("a")), alg.repr("Y", y));
      assertEquals(y.toString(), alg.printX("Y", y));
      assertEquals(2, alg.fresh);
      assertEquals("Sigma Left Kan Sequential", alg.toStringProver());
      assertTrue(alg.toString().startsWith("LeftKan [Pb="), alg.toString());
    }

    /** The default {@code sigma F I} gives Z one row, a.g.h. */
    @Test
    void stopsAfterTheFirstRoundThatAddsARow() throws Exception {
      var alg = algebra(oneRow("entities X Y Z foreign_keys g : X -> Y h : Y -> Z"));
      assertEquals(1, alg.size("Y"));
      assertEquals(0, alg.size("Z"));
      assertNull(alg.fk(Fk.Fk("Y", "h"), alg.fk(G, alg.gen("a"))));
    }

    /**
     * X.g = X.h is never imposed, so one of g and h gets the new row and the other stays undefined.
     * Which one depends on the order of a hash map keyed by foreign keys.
     */
    @Test
    void stopsBeforeAPathEquationOfTheTargetHolds() throws Exception {
      var alg = algebra(oneRow("entities X Y foreign_keys g h : X -> Y path_equations X.g = X.h"));
      Integer a = alg.gen("a");
      assertEquals(1, alg.size("Y"));
      Integer g = alg.fk(G, a);
      Integer h = alg.fk(Fk.Fk("X", "h"), a);
      assertTrue((g == null) ^ (h == null), g + " " + h);
      assertEquals(List.copyOf(alg.en("Y")), List.of(g == null ? h : g));
    }

    @Test
    void mergesTheRowsThatTheMappingIdentifies() throws Exception {
      var alg =
          algebra(
              COLLAPSE + "instance I = literal : S { generators b : B a : A equations a.f = b }\n");
      assertEquals(1, alg.size("X"));
      assertEquals(alg.gen("a"), alg.gen("b"));
    }
  }

  /** Rows are named by terms, so two rows whose terms coincide collide in a map that refuses it. */
  @Nested
  class Collisions {

    /**
     * The same program as the one merged above, with the generators declared in the other order.
     */
    @Test
    void failsWhenTheMappingSendsTwoRowsToOneTerm() {
      String msg =
          message(
              COLLAPSE + "instance I = literal : S { generators a : A b : B equations a.f = b }\n");
      assertEquals("Conflict on a, was 0 now is 1", msg);
    }

    @Test
    void failsWhenANewRowIsNamedAsAnExistingRow() {
      String msg =
          message(
              """
              typeside Ty = literal { types String }
              schema S = literal : Ty { entities A B foreign_keys f : A -> B }
              mapping F = identity S
              instance I = literal : S { generators a : A }
              """);
      assertEquals("Conflict on a.f, was 1 now is 2", msg);
    }
  }

  @Nested
  class TypeAlgebra {

    private static final Att N = Att.Att("X", "n");
    private static final Att M = Att.Att("X", "m");

    /** n is the constant x and m the labelled null s. */
    private static final String ATTRIBUTES =
        """
        typeside Ty = literal { types String constants x : String }
        schema S = literal : Ty { entities A attributes n m : A -> String }
        schema T = literal : Ty { entities X Y foreign_keys g : X -> Y attributes n m : X -> String }
        mapping F = literal : S -> T { entity A -> X attributes n -> lambda v. v.n m -> lambda v. v.m }
        instance I = literal : S { generators a : A s : String equations a.n = x a.m = s }
        """;

    private static final Term<String, String, Sym, Fk, Att, String, String> X =
        Term.Sym(Sym.Sym("x", new Pair<>(Collections.emptyList(), "String")), List.of());

    @Test
    void givesEachAttributeItsValueOrItsLabelledNull() throws Exception {
      var alg = algebra(ATTRIBUTES);
      Integer a = alg.gen("a");
      assertEquals(X.convert(), alg.att(N, a));
      Chc<String, Pair<Integer, Att>> s = Chc.inLeft("s");
      assertEquals(Term.Sk(s), alg.att(M, a));
      assertEquals(Term.Sk(s), alg.sk("s"));
      assertEquals(s, alg.reprT_prot(s));
      assertEquals("inl s", alg.printY("String", s));
      assertEquals("inr (0, n)", alg.printY("String", Chc.inRight(new Pair<>(0, N))));
    }

    /** {@code hasNulls} is true exactly when the type algebra has no labelled null. */
    @Test
    void reportsItsTypeAlgebra() throws Exception {
      var alg = algebra(ATTRIBUTES);
      assertTrue(alg.hasFreeTypeAlgebra());
      assertTrue(alg.hasFreeTypeAlgebraOnJava());
      assertFalse(alg.hasNulls());
      assertTrue(algebra(oneRow("entities X Y foreign_keys g : X -> Y")).hasNulls());
    }

    @Test
    void decidesEquationsByTheirRows() throws Exception {
      var alg = algebra(ATTRIBUTES);
      Term<String, String, Sym, Fk, Att, String, String> a = Term.Gen("a");
      Term<String, String, Sym, Fk, Att, String, String> ag = Term.Fk(G, a);
      assertTrue(alg.eq(null, Term.Att(N, a), X));
      assertFalse(alg.eq(null, Term.Att(N, a), Term.Att(M, a)));
      assertTrue(alg.eq(null, ag, ag));
      assertFalse(alg.eq(null, a, ag));
    }

    @Test
    void rejectsAnInstanceWhoseTypeAlgebraIsNotFree() {
      String msg =
          message(
              """
              typeside Ty = literal { types String functions f g : String -> String }
              schema S = literal : Ty { entities A attributes n : A -> String }
              mapping F = identity S
              instance I = literal : S { generators a : A equations f(a.n) = g(a.n)
                options require_consistency = false }
              """);
      assertEquals("Chase cannot be used: type algebra is not free", msg);
    }
  }
}
