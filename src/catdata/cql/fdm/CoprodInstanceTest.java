package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.Pair;
import catdata.cql.Instance;
import catdata.cql.SqlTypeSide;
import catdata.cql.Term;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CoprodInstanceTest {

  private CoprodInstance<String, String, String, String, String, String, String, String, String>
      createCoprod(
          Map<
                  String,
                  Instance<String, String, String, String, String, String, String, String, String>>
              insts) {
    var schema = FdmTestHelpers.singleEntitySchema();
    return new CoprodInstance<>(insts, schema, false, false);
  }

  @Nested
  class Constructor {

    @Test
    void emptyMapProducesEmptyInstance() {
      var coprod = createCoprod(Collections.emptyMap());
      assertEquals(0, coprod.algebra().size("E"));
    }

    @Test
    void singleInstanceCoprod() {
      Map<String, Instance<String, String, String, String, String, String, String, String, String>>
          insts = new HashMap<>();
      insts.put("i1", FdmTestHelpers.singleElementInstance());
      var coprod = createCoprod(insts);
      assertEquals(1, coprod.algebra().size("E"));
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void schemaIsProvided() {
      var coprod = createCoprod(Collections.emptyMap());
      assertEquals(FdmTestHelpers.singleEntitySchema().ens, coprod.schema().ens);
    }

    @Test
    void dpIsNotNull() {
      var coprod = createCoprod(Collections.emptyMap());
      assertNotNull(coprod.dp());
    }

    @Test
    void algebraIsNotNull() {
      var coprod = createCoprod(Collections.emptyMap());
      assertNotNull(coprod.algebra());
    }

    @Test
    void requireConsistencyMatchesInput() {
      var coprod = createCoprod(Collections.emptyMap());
      assertFalse(coprod.requireConsistency());
    }

    @Test
    void allowUnsafeJavaMatchesInput() {
      var coprod = createCoprod(Collections.emptyMap());
      assertFalse(coprod.allowUnsafeJava());
    }
  }

  /** The coproduct of two instances over sql, one with a value and one with a labelled null. */
  @Nested
  class OfSqlInstances {

    private static final String PROGRAM =
        """
        typeside Ty = sql
        schema S = literal : Ty { entities A attributes n : A -> Varchar }
        instance I = literal : S { generators a : A equations a.n = hello }
        instance J = literal : S { generators b : A s : Varchar equations b.n = s }
        instance K = coproduct I + J : S
        """;

    private CoprodInstance<String, String, Sym, Fk, Att, String, String, Object, Object> k;

    @SuppressWarnings("unchecked")
    private CoprodInstance<String, String, Sym, Fk, Att, String, String, Object, Object> coproduct()
        throws Exception {
      if (k == null) {
        AqlMultiDriver d =
            new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(PROGRAM), null);
        d.start();
        Object o = d.env.defs.insts.get("K");
        assertInstanceOf(CoprodInstance.class, o);
        k = (CoprodInstance<String, String, Sym, Fk, Att, String, String, Object, Object>) o;
      }
      return k;
    }

    private Term<String, String, Sym, Fk, Att, Pair<String, String>, Pair<String, String>> n(
        String inst, String gen) {
      return Term.Att(Att.Att("A", "n"), Term.Gen(new Pair<>(inst, gen)));
    }

    private Term<String, String, Sym, Fk, Att, Pair<String, String>, Pair<String, String>> isNull(
        Term<String, String, Sym, Fk, Att, Pair<String, String>, Pair<String, String>> t) {
      return Term.Sym(Sym.Sym("isNull", new Pair<>(List.of("Varchar"), "Boolean")), List.of(t));
    }

    private Term<String, String, Sym, Fk, Att, Pair<String, String>, Pair<String, String>> bool(
        boolean b) {
      return (b ? SqlTypeSide.t : SqlTypeSide.f).convert();
    }

    @Test
    void tagsEachGeneratorAndNullWithItsInstance() throws Exception {
      var c = coproduct();
      assertEquals(2, c.gens().size());
      assertEquals(1, c.sks().size());
      assertEquals("A", c.gens().get(new Pair<>("I", "a")));
      assertTrue(c.gens().containsKey(new Pair<>("J", "b")));
      assertEquals("Varchar", c.sks().get(new Pair<>("J", "s")));
      assertTrue(c.sks().containsKey(new Pair<>("J", "s")));
      assertThrows(RuntimeException.class, () -> c.gens().remove(new Pair<>("I", "a")));
      assertThrows(RuntimeException.class, () -> c.gens().put(new Pair<>("I", "x"), "A"));
      assertThrows(RuntimeException.class, () -> c.sks().remove(new Pair<>("J", "s")));
      assertThrows(RuntimeException.class, () -> c.sks().put(new Pair<>("J", "t"), "Varchar"));
      List<String> eqs = new ArrayList<>();
      c.eqs((a, b) -> eqs.add(a + " = " + b));
      assertEquals(2, eqs.size());
      c.validateMore();
    }

    @Test
    void decidesValuesThroughEachInstance() throws Exception {
      var c = coproduct();
      assertEquals("free (coproduct)", c.dp().toStringProver());
      Term<String, String, Sym, Fk, Att, Pair<String, String>, Pair<String, String>> hello =
          Term.Obj(Optional.of("hello"), "Varchar");
      assertTrue(c.dp().eq(null, n("I", "a"), hello));
      assertFalse(c.dp().eq(null, n("J", "b"), hello));
      assertTrue(c.dp().eq(null, Term.Gen(new Pair<>("I", "a")), Term.Gen(new Pair<>("I", "a"))));
    }

    /**
     * isNull is rewritten to the truth value its own instance gives, but the rewrite wraps the term
     * SqlTypeSide.f, not its symbol, in a new symbol term, so the result equals neither true nor
     * false.
     */
    @Test
    void decidesIsNullAsNeitherTrueNorFalse() throws Exception {
      var c = coproduct();
      Term<String, String, Sym, Fk, Att, Pair<String, String>, Pair<String, String>> s =
          Term.Sk(new Pair<>("J", "s"));
      for (var arg : List.of(n("I", "a"), s)) {
        assertFalse(c.dp().eq(null, isNull(arg), bool(false)), arg.toString());
        assertFalse(c.dp().eq(null, isNull(arg), bool(true)), arg.toString());
        Object head = c.rewriteIsNull(isNull(arg)).sym();
        assertInstanceOf(Term.class, head);
        assertEquals("false", head.toString());
      }
      assertEquals(n("I", "a"), c.rewriteIsNull(n("I", "a")));
    }

    @Test
    void cannotDecideAnEquationWithVariables() throws Exception {
      var c = coproduct();
      assertThrows(
          RuntimeException.class,
          () -> c.dp().eq(Map.of("v", Chc.inRight("A")), n("I", "a"), n("I", "a")));
    }
  }

  /**
   * K is the coproduct of I and J over a typeside without Java types: in I, a.n is the constant x;
   * in J, a.n is a labelled null and s is a labelled null of its own.
   */
  @Nested
  class OfLiteralInstances {

    private static final String PROGRAM =
        """
        typeside Ty = literal { types String constants x : String }
        schema S = literal : Ty { entities A B foreign_keys f : A -> B attributes n : A -> String }
        instance I = literal : S { generators a : A b : B equations a.f = b a.n = x }
        instance J = literal : S { generators a : A b : B s : String equations a.f = b }
        instance K = coproduct I + J : S
        """;

    private static final Att N = Att.Att("A", "n");

    private AqlMultiDriver driver;

    private AqlMultiDriver driver() throws Exception {
      if (driver == null) {
        driver = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(PROGRAM), null);
        driver.start();
      }
      return driver;
    }

    @SuppressWarnings("unchecked")
    private Instance<
            String,
            String,
            Sym,
            Fk,
            Att,
            Pair<String, String>,
            Pair<String, String>,
            Object,
            Object>
        k() throws Exception {
      return (Instance<
              String,
              String,
              Sym,
              Fk,
              Att,
              Pair<String, String>,
              Pair<String, String>,
              Object,
              Object>)
          driver().env.defs.insts.get("K");
    }

    private static Term<String, String, Sym, Fk, Att, Pair<String, String>, Pair<String, String>>
        gen(String inst, String gen) {
      return Term.Gen(new Pair<>(inst, gen));
    }

    @Test
    void keepsTheRowsAndEquationsOfBoth() throws Exception {
      var k = k();
      assertEquals(2, k.algebra().size("A"));
      assertEquals(2, k.algebra().size("B"));
      List<String> eqs = new ArrayList<>();
      k.eqs((a, b) -> eqs.add(a + " = " + b));
      assertEquals(3, eqs.size());
      assertTrue(eqs.contains("\"(I, a)\".n = x"), eqs.toString());
    }

    @Test
    void decidesEquationsWithinEachInstance() throws Exception {
      var dp = k().dp();
      Term<String, String, Sym, Fk, Att, Pair<String, String>, Pair<String, String>> x =
          Term.Sym(Sym.Sym("x", new Pair<>(Collections.emptyList(), "String")), List.of());
      assertTrue(dp.eq(null, Term.Att(N, gen("I", "a")), x));
      assertFalse(dp.eq(null, Term.Att(N, gen("J", "a")), x));
      assertTrue(dp.eq(null, Term.Fk(Fk.Fk("A", "f"), gen("I", "a")), gen("I", "b")));
      assertFalse(dp.eq(null, gen("I", "a"), gen("J", "a")));
    }

    @Test
    void printsAndNamesItsValuesByTheirInstances() throws Exception {
      var alg = k().algebra();
      for (Object a : alg.en("A")) {
        assertEquals("a", alg.printX("A", a));
      }
      var j = driver().env.defs.insts.get("J").algebra();
      Chc<Pair<String, String>, Object> s = Chc.inLeft(new Pair<>("J", "s"));
      assertTrue(alg.talg().sks.containsKey(s));
      assertEquals(s, alg.reprT_prot(s));
      assertEquals(j.sk("s"), alg.printY("String", s));
      assertEquals("Coprod talg", alg.toStringProver());
      assertTrue(alg.hasFreeTypeAlgebra());
      assertTrue(alg.hasFreeTypeAlgebraOnJava());
      var i = driver().env.defs.insts.get("I").algebra();
      assertEquals(i.hasNulls() || j.hasNulls(), alg.hasNulls());
    }

    @Test
    void refusesToChangeItsGeneratorsOrNulls() throws Exception {
      var k = k();
      Pair<String, String> p = new Pair<>("I", "a");
      for (Runnable r :
          List.<Runnable>of(
              () -> k.gens().remove(p),
              () -> k.gens().put(p, "A"),
              () -> k.sks().remove(p),
              () -> k.sks().put(p, "String"))) {
        RuntimeException e = assertThrows(RuntimeException.class, r::run);
        assertEquals("Anomaly: please report", e.getMessage());
      }
    }
  }
}
