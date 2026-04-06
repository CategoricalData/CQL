package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Transform;
import catdata.graph.DMG;

class ColimitInstanceTest {

  @Nested
  class Constructor {

    @Test
    void emptyColimitCreatesInstance() {
      var schema = FdmTestHelpers.singleEntitySchema();
      DMG<String, String> shape = new DMG<>(Collections.emptySet(), Collections.emptyMap());
      Map<String, Instance<String, String, String, String, String, String, String, String, String>> nodes = Collections.emptyMap();
      Map<String, Transform<String, String, String, String, String, String, String, String, String, String, String, String, String>> edges = Collections.emptyMap();
      var colimit = new ColimitInstance<>(schema, shape, nodes, edges, AqlOptions.initialOptions);
      assertNotNull(colimit);
    }

    @Test
    void singleNodeColimit() {
      var schema = FdmTestHelpers.singleEntitySchema();
      DMG<String, String> shape = new DMG<>(Collections.singleton("n1"), Collections.emptyMap());
      Map<String, Instance<String, String, String, String, String, String, String, String, String>> nodes = new HashMap<>();
      nodes.put("n1", FdmTestHelpers.singleElementInstance());
      Map<String, Transform<String, String, String, String, String, String, String, String, String, String, String, String, String>> edges = Collections.emptyMap();
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
      Map<String, Instance<String, String, String, String, String, String, String, String, String>> nodes = new HashMap<>();
      nodes.put("n1", FdmTestHelpers.singleElementInstance());
      Map<String, Transform<String, String, String, String, String, String, String, String, String, String, String, String, String>> edges = Collections.emptyMap();
      var colimit = new ColimitInstance<>(schema, shape, nodes, edges, AqlOptions.initialOptions);
      assertEquals(schema.ens, colimit.schema().ens);
    }

    @Test
    void dpIsNotNull() {
      var schema = FdmTestHelpers.singleEntitySchema();
      DMG<String, String> shape = new DMG<>(Collections.singleton("n1"), Collections.emptyMap());
      Map<String, Instance<String, String, String, String, String, String, String, String, String>> nodes = new HashMap<>();
      nodes.put("n1", FdmTestHelpers.singleElementInstance());
      Map<String, Transform<String, String, String, String, String, String, String, String, String, String, String, String, String>> edges = Collections.emptyMap();
      var colimit = new ColimitInstance<>(schema, shape, nodes, edges, AqlOptions.initialOptions);
      assertNotNull(colimit.dp());
    }

    @Test
    void getReturnsInclusionTransform() {
      var schema = FdmTestHelpers.singleEntitySchema();
      DMG<String, String> shape = new DMG<>(Collections.singleton("n1"), Collections.emptyMap());
      Map<String, Instance<String, String, String, String, String, String, String, String, String>> nodes = new HashMap<>();
      nodes.put("n1", FdmTestHelpers.singleElementInstance());
      Map<String, Transform<String, String, String, String, String, String, String, String, String, String, String, String, String>> edges = Collections.emptyMap();
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
      Map<String, Instance<String, String, String, String, String, String, String, String, String>> nodes = new HashMap<>();
      nodes.put("n1", FdmTestHelpers.singleElementInstance());
      nodes.put("n2", FdmTestHelpers.singleElementInstance());
      Map<String, Transform<String, String, String, String, String, String, String, String, String, String, String, String, String>> edges = Collections.emptyMap();
      var colimit = new ColimitInstance<>(schema, shape, nodes, edges, AqlOptions.initialOptions);
      assertEquals(2, colimit.algebra().size("E"));
    }
  }
}
