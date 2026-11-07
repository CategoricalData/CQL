package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Transform;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import catdata.graph.DMG;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ColimitInstanceTest {

  @Nested
  class Constructor {

    @Test
    void emptyColimitCreatesInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      DMG<String, String> shape = new DMG<>(Collections.emptySet(), Collections.emptyMap());
      Map<String, Instance<String, String, String, String, String, String, String, String, String>>
          nodes = Collections.emptyMap();
      Map<
              String,
              Transform<
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
                  String,
                  String>>
          edges = Collections.emptyMap();
      var colimit = new ColimitInstance<>(schema, shape, nodes, edges, AqlOptions.initialOptions);
      assertNotNull(colimit);
    }

    @Test
    void singleNodeColimit() {
      var schema = FdmTestHelpers.singleEntitySchema();
      DMG<String, String> shape = new DMG<>(Collections.singleton("n1"), Collections.emptyMap());
      Map<String, Instance<String, String, String, String, String, String, String, String, String>>
          nodes = new HashMap<>();
      nodes.put("n1", FdmTestHelpers.singleElementInstance());
      Map<
              String,
              Transform<
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
                  String,
                  String>>
          edges = Collections.emptyMap();
      var colimit = new ColimitInstance<>(schema, shape, nodes, edges, AqlOptions.initialOptions);
      assertNotNull(colimit.algebra());
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void schemaMatchesInput() {
      var schema = FdmTestHelpers.singleEntitySchema();
      DMG<String, String> shape = new DMG<>(Collections.singleton("n1"), Collections.emptyMap());
      Map<String, Instance<String, String, String, String, String, String, String, String, String>>
          nodes = new HashMap<>();
      nodes.put("n1", FdmTestHelpers.singleElementInstance());
      Map<
              String,
              Transform<
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
                  String,
                  String>>
          edges = Collections.emptyMap();
      var colimit = new ColimitInstance<>(schema, shape, nodes, edges, AqlOptions.initialOptions);
      assertEquals(schema.ens, colimit.schema().ens);
    }

    @Test
    void dpIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      DMG<String, String> shape = new DMG<>(Collections.singleton("n1"), Collections.emptyMap());
      Map<String, Instance<String, String, String, String, String, String, String, String, String>>
          nodes = new HashMap<>();
      nodes.put("n1", FdmTestHelpers.singleElementInstance());
      Map<
              String,
              Transform<
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
                  String,
                  String>>
          edges = Collections.emptyMap();
      var colimit = new ColimitInstance<>(schema, shape, nodes, edges, AqlOptions.initialOptions);
      assertNotNull(colimit.dp());
    }

    @Test
    void getReturnsInclusionTransform() {
      var schema = FdmTestHelpers.singleEntitySchema();
      DMG<String, String> shape = new DMG<>(Collections.singleton("n1"), Collections.emptyMap());
      Map<String, Instance<String, String, String, String, String, String, String, String, String>>
          nodes = new HashMap<>();
      nodes.put("n1", FdmTestHelpers.singleElementInstance());
      Map<
              String,
              Transform<
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
                  String,
                  String>>
          edges = Collections.emptyMap();
      var colimit = new ColimitInstance<>(schema, shape, nodes, edges, AqlOptions.initialOptions);
      assertNotNull(colimit.get("n1"));
    }

    @Test
    void twoNodeColimit() {
      var schema = FdmTestHelpers.singleEntitySchema();
      var nodesSet = new HashSet<String>();
      nodesSet.add("n1");
      nodesSet.add("n2");
      DMG<String, String> shape = new DMG<>(nodesSet, Collections.emptyMap());
      Map<String, Instance<String, String, String, String, String, String, String, String, String>>
          nodes = new HashMap<>();
      nodes.put("n1", FdmTestHelpers.singleElementInstance());
      nodes.put("n2", FdmTestHelpers.singleElementInstance());
      Map<
              String,
              Transform<
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
                  String,
                  String>>
          edges = Collections.emptyMap();
      var colimit = new ColimitInstance<>(schema, shape, nodes, edges, AqlOptions.initialOptions);
      assertEquals(2, colimit.algebra().size("E"));
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

  /**
   * C glues I and J along t, which sends a to a2, b to b2 and the labelled null s to s2; both
   * instances have the equation a.f = b of their own.
   */
  @Nested
  class OfAProgram {

    private static final String PROGRAM =
        """
        typeside Ty = literal { types String constants x : String }
        schema S = literal : Ty { entities A B foreign_keys f : A -> B attributes n : A -> String }
        instance I = literal : S { generators a : A b : B s : String equations a.f = b a.n = s }
        instance J = literal : S { generators a2 : A b2 : B s2 : String equations a2.f = b2 a2.n = s2 }
        transform t = literal : I -> J { generators a -> a2 b -> b2 s -> s2 }
        graph G = literal { nodes n1 n2 edges e : n1 -> n2 }
        instance C = colimit G S { nodes n1 -> I n2 -> J edges e -> t }
        """;

    @SuppressWarnings("unchecked")
    private Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> c()
        throws Exception {
      AqlMultiDriver d = run(PROGRAM);
      assertEquals("", error(d));
      return (Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>)
          d.env.defs.insts.get("C");
    }

    @Test
    void gluesTheRowsAlongTheTransform() throws Exception {
      var alg = c().algebra();
      assertEquals(1, alg.size("A"));
      assertEquals(1, alg.size("B"));
      Object a = alg.en("A").iterator().next();
      assertEquals("(n1, 0)", alg.printX("A", a));
      assertEquals("(n1, 0).f", alg.printX("B", alg.fk(Fk.Fk("A", "f"), a)));
    }

    @Test
    void printsItsNullsByTheirNodes() throws Exception {
      var alg = c().algebra();
      List<String> printed = new ArrayList<>();
      for (Object y : alg.talg().sks.keySet()) {
        printed.add(alg.printY("String", y).toString());
      }
      assertTrue(printed.stream().anyMatch(p -> p.startsWith("(n")), printed.toString());
    }
  }
}
