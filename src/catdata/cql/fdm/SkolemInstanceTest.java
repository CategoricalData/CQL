package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import catdata.cql.SqlTypeSide;
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

class SkolemInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSourceInstance() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertSame(inst, skolem.I);
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void schemaDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertSame(inst.schema(), skolem.schema());
    }

    @Test
    void gensDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertNotNull(skolem.gens());
    }

    @Test
    void requireConsistencyIsFalse() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertFalse(skolem.requireConsistency());
    }

    @Test
    void allowUnsafeJavaIsTrue() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertTrue(skolem.allowUnsafeJava());
    }

    @Test
    void dpToStringProver() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertEquals("skolem", skolem.dp().toStringProver());
    }

    @Test
    void transIsNotNull() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      assertNotNull(skolem.trans);
    }
  }

  @Nested
  class SksMethod {

    @Test
    void sksContainsKeyReturnsTrue() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      // sks.containsKey always returns true
      assertTrue(skolem.sks().containsKey(new Pair<>("anything", "any")));
    }

    @Test
    void sksSizeForNoAtts() {
      var inst = FdmTestHelpers.singleElementInstance();
      var skolem = new SkolemInstance<>(inst);
      // schema has no atts, so sks size is 0
      assertEquals(0, skolem.sks().size());
    }
  }

  /** K is {@code skolem I}: the n of a is hello and the n of b is a SQL null; both map to c. */
  @Nested
  class OfAProgram {

    private static final String PROGRAM =
        """
        typeside Ty = sql
        schema S = literal : Ty { entities A B foreign_keys f : A -> B attributes n : A -> Varchar }
        instance I = literal : S { generators a b : A c : B
          equations a.n = hello b.n = null a.f = c b.f = c }
        instance K = skolem I
        schema T = literal : Ty { entities R }
        query Null = literal : S -> T { entity R -> { from x:A where isNull(x.n) = true } }
        query NotNull = literal : S -> T { entity R -> { from x:A where not(isNull(x.n)) = true } }
        query Pairs = literal : S -> T { entity R -> { from x:A y:A where x.f = y.f } }
        instance KNull = eval Null K
        instance KNotNull = eval NotNull K
        instance KPairs = eval Pairs K
        """;

    private static final Att N = Att.Att("A", "n");

    private static final Sym IS_NULL =
        Sym.Sym("isNull", new Pair<>(Collections.singletonList("Varchar"), "Boolean"));

    private static AqlMultiDriver run() throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(PROGRAM), null);
      d.start();
      assertNull(d.env.exn);
      return d;
    }

    @SuppressWarnings("unchecked")
    private static SkolemInstance<String, String, Sym, Fk, Att, String, String, Object, Object> k(
        AqlMultiDriver d) {
      return (SkolemInstance<String, String, Sym, Fk, Att, String, String, Object, Object>)
          d.env.defs.insts.get("K");
    }

    private static int rows(AqlMultiDriver d, String instance) {
      return d.env.defs.insts.get(instance).algebra().size("R");
    }

    /** a has a value and b has none; every pair of rows shares c. */
    @Test
    void answersQueriesThroughIsNullAndTheRowsOfTheInstance() throws Exception {
      AqlMultiDriver d = run();
      assertEquals(1, rows(d, "KNull"));
      assertEquals(1, rows(d, "KNotNull"));
      assertEquals(4, rows(d, "KPairs"));
    }

    @Test
    void decidesIsNullOfALabelledNull() throws Exception {
      var k = k(run());
      var dp = k.dp();
      Object a = k.algebra().gen("a");
      Object b = k.algebra().gen("b");
      Term<String, String, Sym, Fk, Att, String, Pair<Object, Att>> t = SqlTypeSide.t.convert();
      Term<String, String, Sym, Fk, Att, String, Pair<Object, Att>> isNullOfB =
          Term.Sym(IS_NULL, List.of(Term.Sk(new Pair<>(b, N))));
      Term<String, String, Sym, Fk, Att, String, Pair<Object, Att>> isNullOfA =
          Term.Sym(IS_NULL, List.of(Term.Sk(new Pair<>(a, N))));
      assertTrue(dp.eq(null, isNullOfB, t));
      assertFalse(dp.eq(null, isNullOfA, t));
    }

    @Test
    void wrapsTheAlgebraOfTheInstance() throws Exception {
      var k = k(run());
      var alg = k.algebra();
      Object a = alg.gen("a");
      Object c = alg.gen("c");
      assertEquals(c, alg.fk(Fk.Fk("A", "f"), a));
      assertEquals("a", alg.printX("A", a));
      assertEquals(a + ".n", alg.printY("Varchar", new Pair<>(a, N)));
      assertEquals("Skolem Instance Algebra", alg.toStringProver());
      assertTrue(alg.hasNulls());
    }

    @Test
    void refusesToChangeItsLabelledNulls() throws Exception {
      var sks = k(run()).sks();
      Pair<Object, Att> sk = new Pair<>(0, N);
      RuntimeException e = assertThrows(RuntimeException.class, () -> sks.remove(sk));
      assertEquals("Anomaly: please report", e.getMessage());
      e = assertThrows(RuntimeException.class, () -> sks.put(sk, "Varchar"));
      assertEquals("Anomaly: please report", e.getMessage());
    }
  }
}
