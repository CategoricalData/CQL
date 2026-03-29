package catdata.apg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgOpsTest {

  private ApgTypeside emptyTs;
  private ApgTypeside intTs;

  @BeforeEach
  void setUp() {
    emptyTs = new ApgTypeside(Collections.emptyMap(), Collections.emptyMap());

    Map<String, Pair<Class<?>, Function<String, Object>>> tys = new HashMap<>();
    tys.put("Int", new Pair<>(Integer.class, (Function<String, Object>) Integer::parseInt));
    intTs = new ApgTypeside(tys, Collections.emptyMap());
  }

  private ApgSchema<String> singleLabelSchema(String label) {
    Map<String, ApgTy<String>> m = new HashMap<>();
    m.put(label, ApgTy.ApgTyB("Int"));
    return new ApgSchema<>(intTs, m);
  }

  private ApgInstance<String, String> emptyInstance(ApgSchema<String> schema) {
    return new ApgInstance<>(schema, Collections.emptyMap());
  }

  private ApgInstance<String, String> singleElementInstance(String label, String elemKey,
      int value) {
    ApgSchema<String> schema = singleLabelSchema(label);
    Map<String, Pair<String, ApgTerm<String, String>>> es = new HashMap<>();
    es.put(elemKey, new Pair<>(label, ApgTerm.ApgTermV(value, "Int")));
    return new ApgInstance<>(schema, es);
  }

  @Nested
  class IdTest {

    @Test
    void idReturnsIdentityTransform() {
      ApgInstance<String, String> inst = singleElementInstance("person", "e1", 42);
      ApgTransform<String, String, String, String> id = ApgOps.id(inst);
      assertNotNull(id);
      assertEquals(inst, id.src);
      assertEquals(inst, id.dst);
    }

    @Test
    void idMapsLabelsToSelf() {
      ApgInstance<String, String> inst = singleElementInstance("person", "e1", 42);
      ApgTransform<String, String, String, String> id = ApgOps.id(inst);
      assertEquals("person", id.lMap.get("person"));
    }

    @Test
    void idMapsElementsToSelf() {
      ApgInstance<String, String> inst = singleElementInstance("person", "e1", 42);
      ApgTransform<String, String, String, String> id = ApgOps.id(inst);
      assertEquals("e1", id.eMap.get("e1"));
    }
  }

  @Nested
  class ComposeTest {

    @Test
    void composeTwoIdentities() {
      ApgInstance<String, String> inst = singleElementInstance("person", "e1", 42);
      ApgTransform<String, String, String, String> id = ApgOps.id(inst);
      ApgTransform<String, String, String, String> composed = ApgOps.compose(id, id);
      assertEquals("person", composed.lMap.get("person"));
      assertEquals("e1", composed.eMap.get("e1"));
    }

    @Test
    void composeProducesCorrectMaps() {
      ApgSchema<String> schema = singleLabelSchema("person");
      Map<String, Pair<String, ApgTerm<String, String>>> es1 = new HashMap<>();
      es1.put("a", new Pair<>("person", ApgTerm.ApgTermV(1, "Int")));
      ApgInstance<String, String> inst1 = new ApgInstance<>(schema, es1);

      Map<String, Pair<String, ApgTerm<String, String>>> es2 = new HashMap<>();
      es2.put("b", new Pair<>("person", ApgTerm.ApgTermV(2, "Int")));
      ApgInstance<String, String> inst2 = new ApgInstance<>(schema, es2);

      Map<String, Pair<String, ApgTerm<String, String>>> es3 = new HashMap<>();
      es3.put("c", new Pair<>("person", ApgTerm.ApgTermV(3, "Int")));
      ApgInstance<String, String> inst3 = new ApgInstance<>(schema, es3);

      Map<String, String> lMap12 = new HashMap<>();
      lMap12.put("person", "person");
      Map<String, String> eMap12 = new HashMap<>();
      eMap12.put("a", "b");
      ApgTransform<String, String, String, String> t12 =
          new ApgTransform<>(inst1, inst2, lMap12, eMap12);

      Map<String, String> lMap23 = new HashMap<>();
      lMap23.put("person", "person");
      Map<String, String> eMap23 = new HashMap<>();
      eMap23.put("b", "c");
      ApgTransform<String, String, String, String> t23 =
          new ApgTransform<>(inst2, inst3, lMap23, eMap23);

      ApgTransform<String, String, String, String> composed = ApgOps.compose(t12, t23);
      assertEquals(inst1, composed.src);
      assertEquals(inst3, composed.dst);
      assertEquals("person", composed.lMap.get("person"));
      assertEquals("c", composed.eMap.get("a"));
    }
  }

  @Nested
  class InitialTest {

    @Test
    void initialSchemaReturnsEmptyInstance() {
      ApgSchema<String> schema = singleLabelSchema("person");
      ApgInstance<String, Void> init = ApgOps.initial(schema);
      assertNotNull(init);
      assertTrue(init.Es.isEmpty());
    }

    @Test
    void initialTransformFromEmptyToInstance() {
      ApgInstance<String, String> inst = singleElementInstance("person", "e1", 42);
      ApgTransform<String, Void, String, String> t = ApgOps.initial(inst);
      assertNotNull(t);
      assertTrue(t.src.Es.isEmpty());
      assertEquals(inst, t.dst);
    }
  }

  @Nested
  class TerminalTest {

    @Test
    void terminalSchemaHasSingleLabel() {
      ApgSchema<Unit> ts = ApgOps.terminalSchema(emptyTs);
      assertEquals(1, ts.size());
      assertTrue(ts.containsKey(Unit.unit));
    }

    @Test
    void terminalInstanceHasSingleElement() {
      ApgInstance<Unit, Unit> term = ApgOps.terminal(emptyTs);
      assertEquals(1, term.Es.size());
      assertTrue(term.Es.containsKey(Unit.unit));
    }

    @Test
    void terminalTransformMapsAllToUnit() {
      ApgInstance<String, String> inst = singleElementInstance("person", "e1", 42);
      ApgTransform<String, String, Unit, Unit> t = ApgOps.terminal(inst);
      assertEquals(Unit.unit, t.lMap.get("person"));
      assertEquals(Unit.unit, t.eMap.get("e1"));
    }
  }

  @Nested
  class CoproductTest {

    @Test
    void coproductOfEmptyInstances() {
      ApgSchema<String> schema = singleLabelSchema("person");
      ApgInstance<String, String> empty1 = emptyInstance(schema);
      ApgInstance<String, String> empty2 = emptyInstance(schema);
      ApgInstance<Chc<String, String>, Chc<String, String>> coprod =
          ApgOps.coproduct(empty1, empty2);
      assertNotNull(coprod);
      assertTrue(coprod.Es.isEmpty());
    }

    @Test
    void coproductCombinesElements() {
      ApgInstance<String, String> inst1 = singleElementInstance("person", "e1", 1);
      ApgInstance<String, String> inst2 = singleElementInstance("person", "e2", 2);
      ApgInstance<Chc<String, String>, Chc<String, String>> coprod =
          ApgOps.coproduct(inst1, inst2);
      assertEquals(2, coprod.Es.size());
      assertTrue(coprod.Es.containsKey(Chc.inLeft("e1")));
      assertTrue(coprod.Es.containsKey(Chc.inRight("e2")));
    }

    @Test
    void inlMapsToLeft() {
      ApgInstance<String, String> inst1 = singleElementInstance("person", "e1", 1);
      ApgInstance<String, String> inst2 = singleElementInstance("person", "e2", 2);
      ApgTransform<String, String, Chc<String, String>, Chc<String, String>> inl =
          ApgOps.inl(inst1, inst2);
      assertEquals(Chc.inLeft("person"), inl.lMap.get("person"));
      assertEquals(Chc.inLeft("e1"), inl.eMap.get("e1"));
    }

    @Test
    void inrMapsToRight() {
      ApgInstance<String, String> inst1 = singleElementInstance("person", "e1", 1);
      ApgInstance<String, String> inst2 = singleElementInstance("person", "e2", 2);
      ApgTransform<String, String, Chc<String, String>, Chc<String, String>> inr =
          ApgOps.inr(inst1, inst2);
      assertEquals(Chc.inRight("person"), inr.lMap.get("person"));
      assertEquals(Chc.inRight("e2"), inr.eMap.get("e2"));
    }
  }

  @Nested
  class ProductTest {

    @Test
    void productOfEmptyInstances() {
      ApgSchema<String> schema = singleLabelSchema("person");
      ApgInstance<String, String> empty1 = emptyInstance(schema);
      ApgInstance<String, String> empty2 = emptyInstance(schema);
      ApgInstance<Pair<String, String>, Pair<String, String>> prod =
          ApgOps.product(empty1, empty2);
      assertNotNull(prod);
      assertTrue(prod.Es.isEmpty());
    }

    @Test
    void productCombinesElements() {
      ApgInstance<String, String> inst1 = singleElementInstance("person", "e1", 1);
      ApgInstance<String, String> inst2 = singleElementInstance("person", "e2", 2);
      ApgInstance<Pair<String, String>, Pair<String, String>> prod =
          ApgOps.product(inst1, inst2);
      assertEquals(1, prod.Es.size());
      assertTrue(prod.Es.containsKey(new Pair<>("e1", "e2")));
    }

    @Test
    void fstProjectsToFirst() {
      ApgInstance<String, String> inst1 = singleElementInstance("person", "e1", 1);
      ApgInstance<String, String> inst2 = singleElementInstance("person", "e2", 2);
      ApgTransform<Pair<String, String>, Pair<String, String>, String, String> fst =
          ApgOps.fst(inst1, inst2);
      assertEquals("person", fst.lMap.get(new Pair<>("person", "person")));
      assertEquals("e1", fst.eMap.get(new Pair<>("e1", "e2")));
    }

    @Test
    void sndProjectsToSecond() {
      ApgInstance<String, String> inst1 = singleElementInstance("person", "e1", 1);
      ApgInstance<String, String> inst2 = singleElementInstance("person", "e2", 2);
      ApgTransform<Pair<String, String>, Pair<String, String>, String, String> snd =
          ApgOps.snd(inst1, inst2);
      assertEquals("person", snd.lMap.get(new Pair<>("person", "person")));
      assertEquals("e2", snd.eMap.get(new Pair<>("e1", "e2")));
    }
  }

  @Nested
  class SchemaOpsTest {

    @Test
    void initialSchemaIsEmpty() {
      ApgSchema<String> schema = ApgOps.initialSchema(intTs);
      assertTrue(schema.isEmpty());
      assertEquals(intTs, schema.typeside);
    }

    @Test
    void terminalSchemaHasOneLabel() {
      ApgSchema<Unit> schema = ApgOps.terminalSchema(emptyTs);
      assertEquals(1, schema.size());
      assertTrue(schema.containsKey(Unit.unit));
    }

    @Test
    void coproductSchemaUnifiesLabels() {
      ApgSchema<String> s1 = singleLabelSchema("a");
      ApgSchema<String> s2 = singleLabelSchema("b");
      ApgSchema<Chc<String, String>> coprod = ApgOps.coproductSchema(s1, s2);
      assertEquals(2, coprod.size());
      assertTrue(coprod.containsKey(Chc.inLeft("a")));
      assertTrue(coprod.containsKey(Chc.inRight("b")));
    }

    @Test
    void productSchemaCrossesLabels() {
      ApgSchema<String> s1 = singleLabelSchema("a");
      ApgSchema<String> s2 = singleLabelSchema("b");
      ApgSchema<Pair<String, String>> prod = ApgOps.productSchema(s1, s2);
      assertEquals(1, prod.size());
      assertTrue(prod.containsKey(new Pair<>("a", "b")));
    }
  }
}
