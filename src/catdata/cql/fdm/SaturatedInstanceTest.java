package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.Instance;
import catdata.cql.Term;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SaturatedInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertEquals(FdmTestHelpers.singleEntitySchema().ens, inst.schema().ens);
    }

    @Test
    void constructorSetsConsistencyFlags() {
      var schema = FdmTestHelpers.singleEntitySchema();
      Map<String, Collection<String>> ensMap = new HashMap<>();
      ensMap.put("E", Collections.singletonList("e1"));
      var alg =
          new ImportAlgebra<>(
              schema,
              en -> ensMap.get(en),
              new HashMap<>(),
              (en, x) -> Collections.emptyMap(),
              (en, x) -> Collections.emptyMap(),
              (en, x) -> x,
              (ty, y) -> y,
              true,
              Collections.emptyList());

      var sat = new SaturatedInstance<>(alg, alg, true, true, false, null);
      assertTrue(sat.requireConsistency());
      assertTrue(sat.allowUnsafeJava());
    }
  }

  @Nested
  class GensMethod {

    @Test
    @SuppressWarnings("unchecked")
    void gensContainsAlgebraElements() {
      var inst =
          (SaturatedInstance<
                  String, String, String, String, String, String, String, String, String>)
              FdmTestHelpers.singleElementInstance();
      assertEquals("E", inst.gens().get("e1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void gensEntrySetIteratesOverElements() {
      var inst =
          (SaturatedInstance<
                  String, String, String, String, String, String, String, String, String>)
              FdmTestHelpers.singleElementInstance();
      List<String> elements = new ArrayList<>();
      inst.gens().entrySet((x, en) -> elements.add(x));
      assertTrue(elements.contains("e1"));
    }
  }

  @Nested
  class AlgebraMethod {

    @Test
    void algebraGenReturnsItself() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertEquals("e1", inst.algebra().gen("e1"));
    }

    @Test
    void algebraReprReturnsTermGen() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertEquals(Term.Gen("e1"), inst.algebra().repr("E", "e1"));
    }

    @Test
    void algebraEnReturnsElements() {
      var inst = FdmTestHelpers.singleElementInstance();
      List<String> elements = new ArrayList<>();
      inst.algebra().en("E").forEach(elements::add);
      assertEquals(List.of("e1"), elements);
    }

    @Test
    void algebraSizeReturnsCount() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertEquals(1, inst.algebra().size("E"));
    }

    @Test
    void algebraSkReturnsTermSk() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertEquals(Term.Sk("s1"), inst.algebra().sk("s1"));
    }
  }

  @Nested
  class DPMethod {

    @Test
    void dpToStringProver() {
      var inst = FdmTestHelpers.singleElementInstance();
      assertTrue(inst.dp().toStringProver().contains("Saturated"));
    }
  }

  /** Instance literals read as algebras, with {@code options interpret_as_algebra = true}. */
  @Nested
  class OfAnAlgebra {

    private AqlMultiDriver run(String program) throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      return d;
    }

    /** a has the value hello for n and b none, so b's n is a labelled null. */
    @Test
    void readsTheValuesAndNullsOfTheRows() throws Exception {
      AqlMultiDriver d =
          run(
              """
              typeside Ty = sql
              schema S = literal : Ty { entities A B foreign_keys f : A -> B
                attributes n : A -> Varchar }
              instance I = literal : S { generators a b : A c : B
                equations a.f = c b.f = c a.n = hello options interpret_as_algebra = true }
              """);
      @SuppressWarnings("unchecked")
      var i =
          (Instance<String, String, Sym, Fk, Att, String, String, Object, Object>)
              d.env.defs.insts.get("I");
      var alg = i.algebra();
      assertEquals(2, alg.size("A"));
      Att n = Att.Att("A", "n");
      assertEquals("hello", alg.att(n, alg.gen("a")).toString());
      Object y = alg.talg().sks.keySet().iterator().next();
      assertEquals("b.n", alg.printY("Varchar", y).toString());
      assertTrue(alg.hasFreeTypeAlgebraOnJava());
      assertTrue(alg.hasFreeTypeAlgebra());
    }

    @Test
    void checksThePathEquationsOfTheSchema() throws Exception {
      AqlMultiDriver d =
          run(
              """
              typeside Ty = sql
              schema S = literal : Ty { entities A B foreign_keys f g : A -> B
                path_equations A.f = A.g }
              instance I = literal : S { generators a : A c1 c2 : B
                equations a.f = c1 a.g = c2 options interpret_as_algebra = true }
              """);
      String msg = d.env.exn.getMessage();
      assertTrue(
          msg.contains("Algebra does not satisfy equation forall v. v.f = v.g on ID a"), msg);
    }
  }
}
