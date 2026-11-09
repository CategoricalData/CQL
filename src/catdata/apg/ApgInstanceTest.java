package catdata.apg;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import catdata.cql.Kind;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgInstanceTest {

  private static final Map<String, Pair<Class<?>, Function<String, Object>>> INT_TYS =
      Map.of("Int", new Pair<>(Integer.class, (Function<String, Object>) Integer::parseInt));

  private static ApgTypeside emptyTypeside() {
    return new ApgTypeside(Collections.emptyMap(), Collections.emptyMap());
  }

  private static ApgTypeside intTypeside() {
    return new ApgTypeside(new HashMap<>(INT_TYS), Collections.emptyMap());
  }

  private static ApgSchema<String> emptySchema() {
    return new ApgSchema<>(emptyTypeside(), new HashMap<>());
  }

  private static ApgSchema<String> personSchema() {
    return new ApgSchema<>(intTypeside(), new HashMap<>(Map.of("person", ApgTy.ApgTyB("Int"))));
  }

  @Nested
  class ConstructorTest {

    @Test
    void emptyInstanceCreatesSuccessfully() {
      assertDoesNotThrow(() -> new ApgInstance<>(emptySchema(), Collections.emptyMap()));
    }

    @Test
    void instanceWithElementCreatesSuccessfully() {
      ApgSchema<String> schema = personSchema();
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int")));
      assertDoesNotThrow(() -> new ApgInstance<>(schema, es));
    }

    @Test
    void constructorValidates() {
      ApgSchema<String> schema = emptySchema();
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of("e1", new Pair<>("missing", ApgTerm.ApgTermV(42, "Int")));
      assertThrows(RuntimeException.class, () -> new ApgInstance<>(schema, es));
    }
  }

  @Nested
  class KindAndSizeTest {

    @Test
    void kindReturnsApgInstance() {
      ApgInstance<String, String> inst = new ApgInstance<>(emptySchema(), Collections.emptyMap());
      assertEquals(Kind.APG_instance, inst.kind());
    }

    @Test
    void sizeReturnsSumOfEsAndLs() {
      ApgSchema<String> schema = personSchema();
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int")));
      ApgInstance<String, String> inst = new ApgInstance<>(schema, es);
      assertEquals(1 + 1, inst.size());
    }
  }

  @Nested
  class ValidateTest {

    @Test
    void throwsForUnknownLabel() {
      ApgSchema<String> schema = personSchema();
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of("e1", new Pair<>("unknown", ApgTerm.ApgTermV(42, "Int")));
      assertThrows(RuntimeException.class, () -> new ApgInstance<>(schema, es));
    }

    @Test
    void throwsForTypeMismatch() {
      ApgSchema<String> schema = personSchema();
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV("notAnInt", "Int")));
      assertThrows(RuntimeException.class, () -> new ApgInstance<>(schema, es));
    }
  }

  @Nested
  class TypeCheckTest {

    @Test
    void typeChecksValueAgainstBaseType() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst =
          new ApgInstance<>(
              schema, Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int"))));
      assertDoesNotThrow(() -> inst.type(ApgTerm.ApgTermV(99, "Int"), ApgTy.ApgTyB("Int")));
    }

    @Test
    void typeChecksElementReference() {
      ApgSchema<String> schema = personSchema();
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int")));
      ApgInstance<String, String> inst = new ApgInstance<>(schema, es);
      assertDoesNotThrow(() -> inst.type(ApgTerm.ApgTermE("e1"), ApgTy.ApgTyL("person")));
    }

    @Test
    void typeChecksTupleAgainstProductType() {
      ApgSchema<String> schema =
          new ApgSchema<>(
              intTypeside(),
              new HashMap<>(Map.of("rec", ApgTy.ApgTyP(true, Map.of("age", ApgTy.ApgTyB("Int"))))));
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of(
              "e1",
              new Pair<>("rec", ApgTerm.ApgTermTuple(Map.of("age", ApgTerm.ApgTermV(30, "Int")))));
      ApgInstance<String, String> inst = new ApgInstance<>(schema, es);
      ApgTerm<String, String> tuple =
          ApgTerm.ApgTermTuple(Map.of("age", ApgTerm.ApgTermV(25, "Int")));
      assertDoesNotThrow(
          () -> inst.type(tuple, ApgTy.ApgTyP(true, Map.of("age", ApgTy.ApgTyB("Int")))));
    }

    @Test
    void typeChecksInjectionAgainstSumType() {
      ApgTy<String> sumTy = ApgTy.ApgTyP(false, Map.of("left", ApgTy.ApgTyB("Int")));
      ApgSchema<String> schema =
          new ApgSchema<>(intTypeside(), new HashMap<>(Map.of("choice", sumTy)));
      ApgTerm<String, String> inj =
          ApgTerm.ApgTermInj("left", ApgTerm.ApgTermV(1, "Int"), ApgTy.ApgTyB("Int"));
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of("e1", new Pair<>("choice", inj));
      ApgInstance<String, String> inst = new ApgInstance<>(schema, es);
      ApgTerm<String, String> inj2 =
          ApgTerm.ApgTermInj("left", ApgTerm.ApgTermV(2, "Int"), ApgTy.ApgTyB("Int"));
      assertDoesNotThrow(() -> inst.type(inj2, sumTy));
    }

    @Test
    void throwsForVariableTerm() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = new ApgInstance<>(schema, Collections.emptyMap());
      ApgTerm<String, String> varTerm = ApgTerm.ApgTermVar("x");
      assertThrows(RuntimeException.class, () -> inst.type(varTerm, ApgTy.ApgTyB("Int")));
    }

    @Test
    void throwsForProjectionTerm() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = new ApgInstance<>(schema, Collections.emptyMap());
      ApgTerm<String, String> projTerm =
          ApgTerm.ApgTermProj("f", ApgTerm.ApgTermTuple(Collections.emptyMap()));
      assertThrows(RuntimeException.class, () -> inst.type(projTerm, ApgTy.ApgTyB("Int")));
    }
  }

  @Nested
  class ElemsForTest {

    @Test
    void throwsForBaseType() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = new ApgInstance<>(schema, Collections.emptyMap());
      assertThrows(RuntimeException.class, () -> inst.elemsFor(ApgTy.ApgTyB("Int")));
    }

    @Test
    void returnsElementsForLabelType() {
      ApgSchema<String> schema = personSchema();
      Map<String, Pair<String, ApgTerm<String, String>>> es = new HashMap<>();
      es.put("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int")));
      es.put("e2", new Pair<>("person", ApgTerm.ApgTermV(99, "Int")));
      ApgInstance<String, String> inst = new ApgInstance<>(schema, es);
      List<ApgTerm<String, String>> result = inst.elemsFor(ApgTy.ApgTyL("person"));
      assertEquals(2, result.size());
    }

    @Test
    void returnsEmptyForLabelWithNoElements() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = new ApgInstance<>(schema, Collections.emptyMap());
      List<ApgTerm<String, String>> result = inst.elemsFor(ApgTy.ApgTyL("person"));
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  class EqualsTest {

    @Test
    void equalsSame() {
      ApgSchema<String> schema = personSchema();
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int")));
      ApgInstance<String, String> i1 = new ApgInstance<>(schema, es);
      ApgInstance<String, String> i2 = new ApgInstance<>(schema, new HashMap<>(es));
      assertEquals(i1, i2);
    }

    @Test
    void notEqualsDifferent() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> i1 =
          new ApgInstance<>(
              schema, Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int"))));
      ApgInstance<String, String> i2 =
          new ApgInstance<>(
              schema, Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(99, "Int"))));
      assertNotEquals(i1, i2);
    }

    @Test
    void equalsReflexive() {
      ApgInstance<String, String> inst = new ApgInstance<>(emptySchema(), Collections.emptyMap());
      assertEquals(inst, inst);
    }

    @Test
    void hashCodeConsistent() {
      ApgSchema<String> schema = personSchema();
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int")));
      ApgInstance<String, String> i1 = new ApgInstance<>(schema, es);
      ApgInstance<String, String> i2 = new ApgInstance<>(schema, new HashMap<>(es));
      assertEquals(i1.hashCode(), i2.hashCode());
    }
  }

  /**
   * An instance over the labels person (an Int), pair (a product of two persons) and choice (a sum
   * of a person and an Int), with the persons p1 and p2, the pair pr and the choice c.
   */
  @Nested
  class ElementsAndTyping {

    private static ApgTy<String> label(String l) {
      return ApgTy.ApgTyL(l);
    }

    private static ApgTy<String> product(Map<String, ApgTy<String>> m) {
      return ApgTy.ApgTyP(true, m);
    }

    private static ApgTy<String> sum(Map<String, ApgTy<String>> m) {
      return ApgTy.ApgTyP(false, m);
    }

    private static ApgSchema<String> schema() {
      Map<String, ApgTy<String>> labels = new HashMap<>();
      labels.put("person", ApgTy.ApgTyB("Int"));
      labels.put("pair", product(Map.of("a", label("person"), "b", label("person"))));
      labels.put("choice", sum(Map.of("x", label("person"), "y", ApgTy.ApgTyB("Int"))));
      return new ApgSchema<>(intTypeside(), labels);
    }

    private static Map<String, Pair<String, ApgTerm<String, String>>> elements() {
      Map<String, Pair<String, ApgTerm<String, String>>> es = new HashMap<>();
      es.put("p1", new Pair<>("person", ApgTerm.ApgTermV(1, "Int")));
      es.put("p2", new Pair<>("person", ApgTerm.ApgTermV(2, "Int")));
      es.put(
          "pr",
          new Pair<>(
              "pair",
              ApgTerm.ApgTermTuple(
                  Map.of("a", ApgTerm.ApgTermE("p1"), "b", ApgTerm.ApgTermE("p2")))));
      es.put(
          "c",
          new Pair<>("choice", ApgTerm.ApgTermInj("x", ApgTerm.ApgTermE("p1"), label("person"))));
      return es;
    }

    private static ApgInstance<String, String> instance() {
      return new ApgInstance<>(schema(), elements());
    }

    @Test
    void enumeratesTheElementsOfEachKindOfType() {
      var i = instance();
      assertEquals(2, i.elemsFor(label("person")).size());
      assertEquals(
          4, i.elemsFor(product(Map.of("u", label("person"), "v", label("person")))).size());
      assertEquals(1, i.elemsFor(product(Map.of())).size());
      assertEquals(3, i.elemsFor(sum(Map.of("x", label("person"), "y", label("pair")))).size());
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> i.elemsFor(ApgTy.ApgTyB("Int")));
      assertEquals("Cannot enumerate types: Int", e.getMessage());
      assertTrue(i.toString().startsWith("elements\n"), i.toString());
    }

    private String fails(String label, ApgTerm<String, String> term) {
      var es = elements();
      es.put("bad", new Pair<>(label, term));
      return assertThrows(RuntimeException.class, () -> new ApgInstance<>(schema(), es))
          .getMessage();
    }

    @Test
    void rejectsAValueOfTheWrongKind() {
      assertTrue(
          fails("person", ApgTerm.ApgTermTuple(Map.of()))
              .startsWith("Expecting data at type Int, but received "));
      assertTrue(
          fails("person", ApgTerm.ApgTermV("one", "Int"))
              .startsWith("Not an instance of class java.lang.Integer: one, is String"));
      assertTrue(fails("pair", ApgTerm.ApgTermV(1, "Int")).startsWith("Expecting tuple at type "));
      assertTrue(
          fails("choice", ApgTerm.ApgTermV(1, "Int")).startsWith("Expecting injection at type "));
    }

    @Test
    void rejectsAnElementOrFieldThatDoesNotFit() {
      assertTrue(fails("pair", ApgTerm.ApgTermE("p1")).startsWith("Type error: On p1, expected "));
      assertTrue(fails("person", ApgTerm.ApgTermE("zz")).startsWith("Element zz not in "));
      String msg =
          fails(
              "pair",
              ApgTerm.ApgTermTuple(
                  Map.of(
                      "a",
                      ApgTerm.ApgTermE("p1"),
                      "b",
                      ApgTerm.ApgTermE("p2"),
                      "c",
                      ApgTerm.ApgTermE("p1"))));
      assertTrue(msg.contains(", c, is not a field in "), msg);
      msg = fails("pair", ApgTerm.ApgTermTuple(Map.of("a", ApgTerm.ApgTermE("p1"))));
      assertTrue(msg.endsWith(", no field for b"), msg);
      msg = fails("choice", ApgTerm.ApgTermInj("z", ApgTerm.ApgTermE("p1"), label("person")));
      assertTrue(msg.contains(", z, is not a field in "), msg);
      msg =
          fails(
              "pair",
              ApgTerm.ApgTermTuple(
                  Map.of("a", ApgTerm.ApgTermV(1, "Int"), "b", ApgTerm.ApgTermE("p2"))));
      assertTrue(msg.startsWith("Expecting element at label person, but received "), msg);
    }

    @Test
    void rejectsATypeOrLabelItDoesNotKnow() {
      Map<String, ApgTy<String>> labels = new HashMap<>();
      labels.put("odd", ApgTy.ApgTyB("Str"));
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () ->
                  new ApgInstance<>(
                      new ApgSchema<>(intTypeside(), labels),
                      Map.of("o", new Pair<>("odd", ApgTerm.ApgTermV("s", "Str")))));
      assertEquals("Type not in typeside: Str", e.getMessage());
      Map<String, ApgTy<String>> dangling = new HashMap<>();
      dangling.put("ref", label("nowhere"));
      e =
          assertThrows(
              RuntimeException.class,
              () ->
                  new ApgInstance<>(
                      new ApgSchema<>(intTypeside(), dangling),
                      Map.of("r", new Pair<>("ref", ApgTerm.ApgTermE("r")))));
      assertEquals("Label not in instance: nowhere", e.getMessage());
    }
  }

  /**
   * Arguments that no schema produces. Every type of a schema has a label, a base type or fields,
   * so the checks for open terms and eliminations are reached only by calling {@code type} directly
   * with a type that has none of them.
   */
  @Nested
  class DegenerateArguments {

    private final ApgInstance<String, String> inst =
        new ApgInstance<>(personSchema(), Collections.emptyMap());

    private String typeError(ApgTerm<String, String> term, ApgTy<String> ty) {
      return assertThrows(RuntimeException.class, () -> inst.type(term, ty)).getMessage();
    }

    @Test
    void cannotEvaluateAType() {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> inst.eval(ApgTy.ApgTyB("Int")));
      assertEquals("Anomaly: please report", e.getMessage());
    }

    @Test
    void rejectsABaseTypeOutsideTheTypeside() {
      assertEquals("Not a type: Str", typeError(ApgTerm.ApgTermV("s", "Str"), ApgTy.ApgTyB("Str")));
    }

    @Test
    void rejectsOpenTermsAndEliminationsAtATypeWithoutStructure() {
      ApgTy<String> none = ApgTy.ApgTyP(false, null);
      ApgTerm<String, String> unit = ApgTerm.ApgTermTuple(Collections.emptyMap());
      assertEquals(
          "Expected a closed term, encountered variable x",
          typeError(ApgTerm.ApgTermVar("x"), none));
      assertEquals(
          "Expected an introduction form, encountered projection of f",
          typeError(ApgTerm.ApgTermProj("f", unit), none));
      assertEquals(
          "Expected an introduction form, encountered case analysis of ()",
          typeError(ApgTerm.ApgTermCase(unit, Map.of(), none), none));
      assertEquals(
          "Expected an introduction form, encountered foreign key of person",
          typeError(ApgTerm.ApgTermDeref("person", unit), none));
      assertDoesNotThrow(() -> inst.type(unit, none));
    }

    @Test
    void comparesInstancesWithoutASchema() {
      ApgInstance<String, String> a = new ApgInstance<>(null, Collections.emptyMap());
      ApgInstance<String, String> b = new ApgInstance<>(null, Collections.emptyMap());
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
      assertNotEquals(a, inst);
      assertNotEquals(inst, a);
      assertNotEquals(a, null);
      assertNotEquals(a, "instance");
    }
  }
}
