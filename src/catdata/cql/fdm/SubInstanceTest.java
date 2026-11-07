package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SubInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorWithFullSetPreservesAll() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertEquals(1, sub.algebra().size("E"));
    }

    @Test
    void constructorWithEmptySetCreatesEmpty() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = Collections.emptySet();
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertEquals(0, sub.algebra().size("E"));
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void schemaDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertEquals(inst.schema().ens, sub.schema().ens);
    }

    @Test
    void sksDelegates() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertNotNull(sub.sks());
    }

    @Test
    void dpIsNotNull() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertNotNull(sub.dp());
    }
  }

  @Nested
  class GensMethod {

    @Test
    void gensContainsKeyForIncludedElement() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertTrue(sub.gens().containsKey(new Pair<>("E", "e1")));
    }

    @Test
    void gensGetReturnsEntity() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertEquals("E", sub.gens().get(new Pair<>("E", "e1")));
    }

    @Test
    void gensSizeMatchesSubset() {
      var inst = FdmTestHelpers.twoElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      assertEquals(1, sub.gens().size());
    }
  }

  @Nested
  class GetInclusionMethod {

    @Test
    void inclusionSourceIsSubInstance() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      var inclusion = sub.getInclusion();
      assertSame(sub, inclusion.src());
    }

    @Test
    void inclusionDestIsOriginal() {
      var inst = FdmTestHelpers.singleElementInstance();
      Set<Pair<String, String>> xs = new HashSet<>();
      xs.add(new Pair<>("E", "e1"));
      var sub = new SubInstance<>(xs, inst, AqlOptions.initialOptions);
      var inclusion = sub.getInclusion();
      assertSame(inst, inclusion.dst());
    }
  }

  private static AqlMultiDriver run(String program) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    assertNull(d.env.exn);
    return d;
  }

  @SuppressWarnings("unchecked")
  private static Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> instance(
      AqlMultiDriver d, String name) {
    return (Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>)
        d.env.defs.insts.get(name);
  }

  /**
   * K is {@code core I}: a1 and a2 both reach b and have nothing else, so they fold to one row. The
   * queries over K compare a function of a path, two rows, and a constant that differs.
   */
  @Nested
  class OfACore {

    private static final String PROGRAM =
        """
        typeside Ty = literal { types String constants x y : String functions g : String -> String }
        schema S = literal : Ty { entities A B foreign_keys f : A -> B attributes m : B -> String }
        instance I = literal : S { generators a1 a2 : A b : B s : String
          equations a1.f = b a2.f = b b.m = x }
        instance K = core I
        schema T = literal : Ty { entities R P Q }
        query Q = literal : S -> T {
          entity R -> { from a:A where g(a.f.m) = g(x) }
          entity P -> { from a:A b:B where a.f = b }
          entity Q -> { from a:A where a.f.m = y }
        }
        instance QK = eval Q K
        """;

    @Test
    void answersQueriesThroughTheRowsOfTheInstance() throws Exception {
      AqlMultiDriver d = run(PROGRAM);
      var k = instance(d, "K").algebra();
      assertEquals(List.of(1, 1), List.of(k.size("A"), k.size("B")));
      var qk = instance(d, "QK").algebra();
      assertEquals(List.of(1, 1, 0), List.of(qk.size("R"), qk.size("P"), qk.size("Q")));
    }

    @Test
    void comparesAValueOfTheSqlTypeside() throws Exception {
      AqlMultiDriver d =
          run(
              """
              typeside Ty = sql
              schema S = literal : Ty { entities A B foreign_keys f : A -> B
                attributes m : B -> Varchar }
              instance I = literal : S { generators a1 a2 : A b : B
                equations a1.f = b a2.f = b b.m = hello }
              instance K = core I
              schema T = literal : Ty { entities R }
              query Q = literal : S -> T { entity R -> { from a:A where a.f.m = hello } }
              instance QK = eval Q K
              """);
      assertEquals(1, instance(d, "QK").algebra().size("R"));
    }

    @Test
    void wrapsTheAlgebraOfTheInstance() throws Exception {
      AqlMultiDriver d = run(PROGRAM);
      var i = instance(d, "I");
      var k = instance(d, "K");
      var alg = k.algebra();
      Object a = alg.en("A").iterator().next();
      Object b = alg.fk(Fk.Fk("A", "f"), a);
      assertEquals(alg.en("B").iterator().next(), b);
      assertEquals(Term.Gen(b), alg.repr("B", b));
      assertEquals(i.algebra().printX("B", i.algebra().gen("b")), alg.printX("B", b));
      assertTrue(
          alg.toStringProver().startsWith("Sub-instance algebra wrapper of "),
          alg.toStringProver());
      assertEquals(i.algebra().sk("s").toString(), alg.sk("s").toString());
      assertEquals(i.algebra().hasNulls(), alg.hasNulls());
      assertEquals(i.algebra().hasFreeTypeAlgebra(), alg.hasFreeTypeAlgebra());
      assertEquals(i.requireConsistency(), k.requireConsistency());
      assertEquals(i.allowUnsafeJava(), k.allowUnsafeJava());
      Object s = alg.talg().sks.keySet().iterator().next();
      assertEquals(i.algebra().printY("String", s), alg.printY("String", s));
      assertEquals(Chc.inLeft("s"), alg.reprT_prot(Chc.inLeft("s")));
    }

    @Test
    void namesTheNullOfARowByTheRowOfTheSubInstance() throws Exception {
      var k = instance(run(PROGRAM.replace(" b.m = x }", " }").replace(" s : String", "")), "K");
      var alg = k.algebra();
      Object b = alg.en("B").iterator().next();
      Att m = Att.Att("B", "m");
      Object y = alg.talg().sks.keySet().iterator().next();
      assertEquals(Chc.inRight(new Pair<>(b, m)), alg.reprT_prot(y));
    }

    @Test
    void decidesOnlyGroundEquations() throws Exception {
      var dp = instance(run(PROGRAM), "K").dp();
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> dp.eq(Map.of("v", Chc.inRight("A")), Term.Var("v"), Term.Var("v")));
      assertEquals("Cannot answer a non-ground equation", e.getMessage());
    }

    /** I folds onto one of its rows of A, so it is not a core; K is. */
    @Test
    void tellsWhetherAnInstanceIsACore() throws Exception {
      AqlMultiDriver d = run(PROGRAM);
      assertFalse(SubInstance.core0(instance(d, "I"), AqlOptions.initialOptions));
      assertTrue(SubInstance.core0(instance(d, "K"), AqlOptions.initialOptions));
    }
  }
}
