package catdata.apg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import catdata.Triple;
import catdata.cql.Kind;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgTypesideTest {

  private static final Map<String, Pair<Class<?>, Function<String, Object>>> EMPTY_BS =
      Collections.emptyMap();

  private static final Map<String, Triple<List<String>, String, Function<List<Object>, Object>>> EMPTY_UDFS =
      Collections.emptyMap();

  private ApgTypeside createEmpty() {
    return new ApgTypeside(EMPTY_BS, EMPTY_UDFS);
  }

  private ApgTypeside createWithOneType() {
    Map<String, Pair<Class<?>, Function<String, Object>>> bs =
        Map.of("Int", new Pair<>(Integer.class, Integer::parseInt));
    return new ApgTypeside(bs, EMPTY_UDFS);
  }

  @Nested
  class Constructor {

    @Test
    void constructorSetsFields() {
      Map<String, Pair<Class<?>, Function<String, Object>>> bs =
          Map.of("Int", new Pair<>(Integer.class, Integer::parseInt));
      Map<String, Triple<List<String>, String, Function<List<Object>, Object>>> udfs =
          Map.of("add", new Triple<>(List.of("Int", "Int"), "Int", args -> args.get(0)));
      ApgTypeside ts = new ApgTypeside(bs, udfs);
      assertNotNull(ts.Bs);
      assertNotNull(ts.udfs);
      assertEquals(1, ts.Bs.size());
      assertTrue(ts.Bs.containsKey("Int"));
      assertEquals(1, ts.udfs.size());
      assertTrue(ts.udfs.containsKey("add"));
    }
  }

  @Nested
  class KindTest {

    @Test
    void kindReturnsApgTypeside() {
      ApgTypeside ts = createEmpty();
      assertEquals(Kind.APG_typeside, ts.kind());
    }
  }

  @Nested
  class SizeTest {

    @Test
    void sizeReturnsSumOfBsAndUdfs() {
      Map<String, Pair<Class<?>, Function<String, Object>>> bs =
          Map.of("Int", new Pair<>(Integer.class, Integer::parseInt));
      Map<String, Triple<List<String>, String, Function<List<Object>, Object>>> udfs =
          Map.of("add", new Triple<>(List.of("Int", "Int"), "Int", args -> args.get(0)),
              "sub", new Triple<>(List.of("Int", "Int"), "Int", args -> args.get(0)));
      ApgTypeside ts = new ApgTypeside(bs, udfs);
      assertEquals(3, ts.size());
    }

    @Test
    void sizeEmptyReturnsZero() {
      ApgTypeside ts = createEmpty();
      assertEquals(0, ts.size());
    }
  }

  @Nested
  class EqualsHashCode {

    @Test
    void equalsSame() {
      ApgTypeside ts1 = createEmpty();
      ApgTypeside ts2 = createEmpty();
      assertEquals(ts1, ts2);
    }

    @Test
    void notEqualsDifferentBs() {
      ApgTypeside ts1 = createEmpty();
      ApgTypeside ts2 = createWithOneType();
      assertNotEquals(ts1, ts2);
    }

    @Test
    void equalsReflexive() {
      ApgTypeside ts = createEmpty();
      assertEquals(ts, ts);
    }

    @Test
    void notEqualsNull() {
      ApgTypeside ts = createEmpty();
      assertNotEquals(null, ts);
    }

    @Test
    void notEqualsDifferentType() {
      ApgTypeside ts = createEmpty();
      assertNotEquals("not a typeside", ts);
    }

    @Test
    void hashCodeConsistent() {
      ApgTypeside ts1 = createEmpty();
      ApgTypeside ts2 = createEmpty();
      assertEquals(ts1.hashCode(), ts2.hashCode());
    }
  }

  @Nested
  class ToStringTest {

    @Test
    void toStringContainsTypeInfo() {
      ApgTypeside ts = createWithOneType();
      String str = ts.toString();
      assertTrue(str.contains("Int"));
    }
  }
}
