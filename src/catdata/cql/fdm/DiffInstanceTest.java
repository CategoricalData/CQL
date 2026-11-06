package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.Instance;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DiffInstanceTest {

  @Nested
  class Constructor {

    @Test
    void constructorRejectsSchemaWithFks() {
      var inst1 = FdmTestHelpers.instanceWithFk();
      var inst2 = FdmTestHelpers.instanceWithFk();
      assertThrows(RuntimeException.class, () -> new DiffInstance<>(inst1, inst2, false, false));
    }

    @Test
    void constructorRejectsDifferentSchemas() {
      var schema1 = FdmTestHelpers.singleEntitySchema();
      var schema2 = FdmTestHelpers.emptySchema();
      var inst1 = FdmTestHelpers.emptyInstance(schema1);
      var inst2 = FdmTestHelpers.emptyInstance(schema2);
      assertThrows(RuntimeException.class, () -> new DiffInstance<>(inst1, inst2, false, false));
    }
  }

  @Nested
  class DiffBehavior {

    @Test
    void diffOfSameInstanceIsEmpty() {
      var inst = FdmTestHelpers.singleElementInstance();
      var diff = new DiffInstance<>(inst, inst, false, false);
      assertEquals(0, diff.algebra().size("E"));
    }

    @Test
    void diffWithEmptyKeepsAll() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var inst = FdmTestHelpers.singleElementInstance();
      var empty = FdmTestHelpers.emptyInstance(schema);
      var diff = new DiffInstance<>(inst, empty, false, false);
      assertEquals(1, diff.algebra().size("E"));
    }
  }

  @Nested
  class InstanceMethods {

    @Test
    void schemaMatchesInput() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var inst = FdmTestHelpers.singleElementInstance();
      var empty = FdmTestHelpers.emptyInstance(schema);
      var diff = new DiffInstance<>(inst, empty, false, false);
      assertEquals(schema.ens, diff.schema().ens);
    }

    @Test
    void dpIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var inst = FdmTestHelpers.singleElementInstance();
      var empty = FdmTestHelpers.emptyInstance(schema);
      var diff = new DiffInstance<>(inst, empty, false, false);
      assertNotNull(diff.dp());
    }

    @Test
    void transformIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var inst = FdmTestHelpers.singleElementInstance();
      var empty = FdmTestHelpers.emptyInstance(schema);
      var diff = new DiffInstance<>(inst, empty, false, false);
      assertNotNull(diff.h);
    }
  }

  @SuppressWarnings("unchecked")
  private static Instance<String, String, Sym, Fk, Att, String, String, Object, Object> instance(
      AqlMultiDriver d, String name) {
    return (Instance<String, String, Sym, Fk, Att, String, String, Object, Object>)
        d.env.defs.insts.get(name);
  }

  private static AqlMultiDriver run(String program) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    return d;
  }

  /** {@code except I J} keeps the rows of I that no row of J matches on every attribute. */
  @Nested
  class OfAProgram {

    private static final String TYPESIDE =
        "typeside Ty = literal { types String constants x y z : String"
            + " functions g : String -> String }\n"
            + "schema S = literal : Ty { entities A attributes n : A -> String }\n";

    @Test
    void keepsTheRowsThatTheOtherInstanceLacks() throws Exception {
      AqlMultiDriver d =
          run(
              TYPESIDE
                  + """
                  instance I = literal : S { generators a1 a2 : A equations a1.n = x a2.n = y }
                  instance J = literal : S { generators c1 c2 : A equations c1.n = y c2.n = z }
                  instance K = except I J
                  """);
      assertNull(d.env.exn);
      var k = instance(d, "K");
      assertEquals(1, k.algebra().size("A"));
      Object row = k.algebra().en("A").iterator().next();
      assertEquals("x", k.algebra().att(Att.Att("A", "n"), row).toString());
      assertFalse(k.requireConsistency());
    }

    /** The difference is built with allowUnsafeJava true, but reports its requireConsistency. */
    @Test
    void reportsItsConsistencyFlagForBothFlags() throws Exception {
      AqlMultiDriver d =
          run(
              TYPESIDE
                  + """
                  instance I = literal : S { generators a1 : A equations a1.n = x }
                  instance J = literal : S { generators c1 : A equations c1.n = y }
                  """);
      var k = new DiffInstance<>(instance(d, "I"), instance(d, "J"), true, false);
      assertFalse(k.requireConsistency());
      assertFalse(k.allowUnsafeJava());
    }

    @Test
    void rejectsAnotherTypeAlgebra() throws Exception {
      AqlMultiDriver d =
          run(
              TYPESIDE
                  + """
                  instance I = literal : S { generators a : A }
                  instance J = literal : S { generators c : A equations c.n = x }
                  """);
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> new DiffInstance<>(instance(d, "I"), instance(d, "J"), true, false));
      assertEquals("Type algebras not the same.", e.getMessage());
    }

    /** I and J share the equation g(x) = x, which the difference cannot prove. */
    @Test
    void failsOnATypeAlgebraWithAnEquation() throws Exception {
      String opts = " options require_consistency = false }";
      AqlMultiDriver d =
          run(
              TYPESIDE
                  + "instance I = literal : S { generators a1 a2 : A"
                  + " equations a1.n = x a2.n = y g(x) = x"
                  + opts
                  + "\ninstance J = literal : S { generators c1 : A equations c1.n = y g(x) = x"
                  + opts
                  + "\ninstance K = except I J\n");
      String msg = d.env.exn.getMessage();
      assertTrue(msg.contains("Anomaly: not equal: g(x) and x"), msg);
    }
  }
}
