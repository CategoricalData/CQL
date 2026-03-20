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
          new ApgInstance<>(schema, Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int"))));
      assertDoesNotThrow(() -> inst.type(ApgTerm.ApgTermV(99, "Int"), ApgTy.ApgTyB("Int")));
    }

    @Test
    void typeChecksElementReference() {
      ApgSchema<String> schema = personSchema();
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int")));
      ApgInstance<String, String> inst = new ApgInstance<>(schema, es);
      assertDoesNotThrow(
          () -> inst.type(ApgTerm.ApgTermE("e1"), ApgTy.ApgTyL("person")));
    }

    @Test
    void typeChecksTupleAgainstProductType() {
      ApgSchema<String> schema =
          new ApgSchema<>(
              intTypeside(),
              new HashMap<>(
                  Map.of("rec", ApgTy.ApgTyP(true, Map.of("age", ApgTy.ApgTyB("Int"))))));
      Map<String, Pair<String, ApgTerm<String, String>>> es =
          Map.of(
              "e1",
              new Pair<>(
                  "rec",
                  ApgTerm.ApgTermTuple(Map.of("age", ApgTerm.ApgTermV(30, "Int")))));
      ApgInstance<String, String> inst = new ApgInstance<>(schema, es);
      ApgTerm<String, String> tuple = ApgTerm.ApgTermTuple(Map.of("age", ApgTerm.ApgTermV(25, "Int")));
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
      assertThrows(
          RuntimeException.class,
          () -> inst.type(varTerm, ApgTy.ApgTyB("Int")));
    }

    @Test
    void throwsForProjectionTerm() {
      ApgSchema<String> schema = personSchema();
      ApgInstance<String, String> inst = new ApgInstance<>(schema, Collections.emptyMap());
      ApgTerm<String, String> projTerm =
          ApgTerm.ApgTermProj("f", ApgTerm.ApgTermTuple(Collections.emptyMap()));
      assertThrows(
          RuntimeException.class,
          () -> inst.type(projTerm, ApgTy.ApgTyB("Int")));
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
          new ApgInstance<>(schema, Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(42, "Int"))));
      ApgInstance<String, String> i2 =
          new ApgInstance<>(schema, Map.of("e1", new Pair<>("person", ApgTerm.ApgTermV(99, "Int"))));
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
}
