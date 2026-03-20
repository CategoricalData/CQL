package catdata.apg;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import catdata.Pair;
import catdata.cql.Kind;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgTransformTest {

  private static final Map<String, Pair<Class<?>, Function<String, Object>>> INT_TYS =
      Map.of("Int", new Pair<>(Integer.class, (Function<String, Object>) Integer::parseInt));

  private static ApgTypeside intTypeside() {
    return new ApgTypeside(new HashMap<>(INT_TYS), Collections.emptyMap());
  }

  private static ApgSchema<String> personSchema() {
    return new ApgSchema<>(intTypeside(), new HashMap<>(Map.of("person", ApgTy.ApgTyB("Int"))));
  }

  private static ApgInstance<String, String> emptyInstance(ApgSchema<String> schema) {
    return new ApgInstance<>(schema, Collections.emptyMap());
  }

  private static ApgInstance<String, String> singletonInstance(ApgSchema<String> schema) {
    return new ApgInstance<>(
        schema, Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int"))));
  }

  @Nested
  class ConstructorTest {

    @Test
    void emptyTransformCreatesSuccessfully() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> src = emptyInstance(schema);
      ApgInstance<String, String> dst = emptyInstance(schema);
      assertDoesNotThrow(
          () ->
              new ApgTransform<>(
                  src, dst, Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    void identityTransformCreatesSuccessfully() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = singletonInstance(schema);
      assertDoesNotThrow(
          () ->
              new ApgTransform<>(
                  inst, inst, Map.of("person", "person"), Map.of("e1", "e1")));
    }

    @Test
    void throwsForMissingLabelMapping() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = singletonInstance(schema);
      assertThrows(
          RuntimeException.class,
          () ->
              new ApgTransform<>(
                  inst, inst, Collections.emptyMap(), Map.of("e1", "e1")));
    }

    @Test
    void throwsForMissingElementMapping() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = singletonInstance(schema);
      assertThrows(
          RuntimeException.class,
          () ->
              new ApgTransform<>(
                  inst, inst, Map.of("person", "person"), Collections.emptyMap()));
    }
  }

  @Nested
  class KindAndSizeTest {

    @Test
    void kindReturnsApgMorphism() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> src = emptyInstance(schema);
      ApgInstance<String, String> dst = emptyInstance(schema);
      ApgTransform<String, String, String, String> t =
          new ApgTransform<>(src, dst, Collections.emptyMap(), Collections.emptyMap());
      assertEquals(Kind.APG_morphism, t.kind());
    }

    @Test
    void sizeReturnsSumOfSrcAndDst() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = singletonInstance(schema);
      ApgTransform<String, String, String, String> t =
          new ApgTransform<>(inst, inst, Map.of("person", "person"), Map.of("e1", "e1"));
      assertEquals(inst.size() + inst.size(), t.size());
    }
  }

  @Nested
  class ValidateTest {

    @Test
    void throwsWhenLabelNotMapped() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = singletonInstance(schema);
      assertThrows(
          RuntimeException.class,
          () ->
              new ApgTransform<>(
                  inst, inst, Collections.emptyMap(), Map.of("e1", "e1")));
    }

    @Test
    void throwsWhenElementNotMapped() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = singletonInstance(schema);
      assertThrows(
          RuntimeException.class,
          () ->
              new ApgTransform<>(
                  inst, inst, Map.of("person", "person"), Collections.emptyMap()));
    }

    @Test
    void throwsWhenLabelsInconsistent() {
      ApgTypeside ts = intTypeside();
      ApgSchema<String> schema =
          new ApgSchema<>(
              ts,
              new HashMap<>(
                  Map.of(
                      "person", ApgTy.ApgTyB("Int"),
                      "animal", ApgTy.ApgTyB("Int"))));
      Map<String, Pair<String, ApgTerm<String, String>>> srcEs =
          Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int")));
      Map<String, Pair<String, ApgTerm<String, String>>> dstEs =
          Map.of("e2", new Pair<>("animal", ApgTerm.ApgTermV(42, "Int")));
      ApgInstance<String, String> src = new ApgInstance<>(schema, srcEs);
      ApgInstance<String, String> dst = new ApgInstance<>(schema, dstEs);
      // Label maps person -> person, but target element e2 has label "animal"
      assertThrows(
          RuntimeException.class,
          () ->
              new ApgTransform<>(
                  src, dst, Map.of("person", "person"), Map.of("e1", "e2")));
    }
  }

  @Nested
  class EqualsTest {

    @Test
    void equalsSame() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = singletonInstance(schema);
      ApgTransform<String, String, String, String> t1 =
          new ApgTransform<>(inst, inst, Map.of("person", "person"), Map.of("e1", "e1"));
      ApgTransform<String, String, String, String> t2 =
          new ApgTransform<>(inst, inst, Map.of("person", "person"), Map.of("e1", "e1"));
      assertEquals(t1, t2);
    }

    @Test
    void notEqualsDifferent() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> src = emptyInstance(schema);
      ApgInstance<String, String> dst = singletonInstance(schema);
      ApgTransform<String, String, String, String> t1 =
          new ApgTransform<>(src, src, Collections.emptyMap(), Collections.emptyMap());
      ApgTransform<String, String, String, String> t2 =
          new ApgTransform<>(src, dst, Collections.emptyMap(), Collections.emptyMap());
      assertNotEquals(t1, t2);
    }

    @Test
    void equalsReflexive() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = emptyInstance(schema);
      ApgTransform<String, String, String, String> t =
          new ApgTransform<>(inst, inst, Collections.emptyMap(), Collections.emptyMap());
      assertEquals(t, t);
    }

    @Test
    void hashCodeConsistent() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = singletonInstance(schema);
      ApgTransform<String, String, String, String> t1 =
          new ApgTransform<>(inst, inst, Map.of("person", "person"), Map.of("e1", "e1"));
      ApgTransform<String, String, String, String> t2 =
          new ApgTransform<>(inst, inst, Map.of("person", "person"), Map.of("e1", "e1"));
      assertEquals(t1.hashCode(), t2.hashCode());
    }
  }
}
