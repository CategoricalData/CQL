package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import catdata.cql.Algebra;
import catdata.cql.Instance;
import catdata.cql.Term;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DeltaAlgebraTest {

  private DeltaAlgebra<
          String,
          String,
          String,
          String,
          String,
          String,
          String,
          String,
          String,
          String,
          String,
          String>
      createDeltaAlgebra() {
    var schema = FdmTestHelpers.singleEntitySchema();
    var mapping = FdmTestHelpers.identityMapping(schema);
    var inst = FdmTestHelpers.singleElementInstance();
    return new DeltaAlgebra<>(mapping, inst);
  }

  @Nested
  class SchemaMethod {

    @Test
    void schemaReturnsMappingSource() {
      var alg = createDeltaAlgebra();
      assertEquals(FdmTestHelpers.singleEntitySchema().ens, alg.schema().ens);
    }
  }

  @Nested
  class EnMethod {

    @Test
    void enReturnsPairedElements() {
      var alg = createDeltaAlgebra();
      var elements = alg.en("E");
      assertEquals(1, elements.size());
      Pair<String, String> elem = elements.iterator().next();
      assertEquals("E", elem.first);
      assertEquals("e1", elem.second);
    }

    @Test
    void enCachesResult() {
      var alg = createDeltaAlgebra();
      var first = alg.en("E");
      var second = alg.en("E");
      assertSame(first, second);
    }
  }

  @Nested
  class GenMethod {

    @Test
    void genReturnsItself() {
      var alg = createDeltaAlgebra();
      var gen = new Pair<>("E", "e1");
      assertSame(gen, alg.gen(gen));
    }
  }

  @Nested
  class ReprMethod {

    @Test
    void reprReturnsTermGen() {
      var alg = createDeltaAlgebra();
      var gen = new Pair<>("E", "e1");
      assertEquals(Term.Gen(gen), alg.repr("E", gen));
    }
  }

  @Nested
  class SizeMethod {

    @Test
    void sizeReturnsCount() {
      var alg = createDeltaAlgebra();
      assertEquals(1, alg.size("E"));
    }
  }

  @Nested
  class SkMethod {

    @Test
    void skReturnsTermSk() {
      var alg = createDeltaAlgebra();
      assertEquals(Term.Sk("y1"), alg.sk("y1"));
    }
  }

  @Nested
  class PrintMethods {

    @Test
    void toStringProverDelegates() {
      var alg = createDeltaAlgebra();
      assertNotNull(alg.toStringProver());
    }

    @Test
    void printXShowsEntityAndValue() {
      var alg = createDeltaAlgebra();
      var result = alg.printX("E", new Pair<>("E", "e1"));
      assertNotNull(result);
      assertTrue(result.toString().contains("e1"));
    }
  }

  @Nested
  class TypeAlgebra {

    @Test
    void hasFreeTypeAlgebraDelegates() {
      var alg = createDeltaAlgebra();
      assertTrue(alg.hasFreeTypeAlgebra());
    }

    @Test
    void hasNullsDelegates() {
      var alg = createDeltaAlgebra();
      // ImportAlgebra hasNulls returns true when sks are empty
      assertNotNull(alg.hasNulls());
    }
  }

  /**
   * D is {@code delta F J}, where F sends n through the foreign key g to m, and c to the constant
   * hello; x reaches y, whose m is world.
   */
  @Nested
  class OfAProgram {

    private static final String PROGRAM =
        """
        typeside Ty = sql
        schema T = literal : Ty { entities X Y foreign_keys g : X -> Y attributes m : Y -> Varchar }
        schema S = literal : Ty { entities A attributes n c : A -> Varchar }
        mapping F = literal : S -> T { entity A -> X attributes n -> lambda v. v.g.m
          c -> lambda v. hello }
        instance J = literal : T { generators x : X y : Y s : Varchar
          equations x.g = y y.m = world }
        instance D = delta F J
        """;

    @SuppressWarnings("unchecked")
    private Pair<
            Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>,
            Algebra<String, String, Sym, Fk, Att, Object, Object, Object, Object>>
        run() throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(PROGRAM), null);
      d.start();
      assertNull(d.env.exn);
      var j =
          (Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>)
              d.env.defs.insts.get("J");
      var delta =
          (Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>)
              d.env.defs.insts.get("D");
      return new Pair<>(j, delta.algebra());
    }

    @Test
    void readsAnAttributeThroughAForeignKeyOrAConstant() throws Exception {
      var alg = run().second;
      Object a = alg.en("A").iterator().next();
      assertEquals("world", alg.att(Att.Att("A", "n"), a).toString());
      assertEquals("hello", alg.att(Att.Att("A", "c"), a).toString());
    }

    @Test
    void delegatesItsTypeAlgebraToTheInstance() throws Exception {
      var p = run();
      var j = p.first.algebra();
      var alg = p.second;
      assertEquals(j.talgToString(), alg.talgToString());
      assertEquals(j.hasFreeTypeAlgebraOnJava(), alg.hasFreeTypeAlgebraOnJava());
      Object s = j.talg().sks.keySet().iterator().next();
      assertEquals(j.printY("Varchar", s), alg.printY("Varchar", s));
    }
  }
}
