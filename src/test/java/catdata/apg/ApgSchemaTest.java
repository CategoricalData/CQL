package catdata.apg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.Kind;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgSchemaTest {

  private static ApgTypeside emptyTypeside() {
    return new ApgTypeside(Collections.emptyMap(), Collections.emptyMap());
  }

  private static ApgSchema<String> emptySchema() {
    return new ApgSchema<>(emptyTypeside(), new HashMap<>());
  }

  private static ApgSchema<String> singleLabelSchema() {
    Map<String, ApgTy<String>> map = new HashMap<>();
    map.put("Person", ApgTy.ApgTyB("Int"));
    return new ApgSchema<>(emptyTypeside(), map);
  }

  @Nested
  class Constructor {

    @Test
    void constructorSetsFields() {
      ApgTypeside ts = emptyTypeside();
      Map<String, ApgTy<String>> map = new HashMap<>();
      ApgSchema<String> schema = new ApgSchema<>(ts, map);
      assertEquals(ts, schema.typeside);
      assertEquals(map, schema.schema);
    }
  }

  @Nested
  class SemanticsMethods {

    @Test
    void kindReturnsApgSchema() {
      assertEquals(Kind.APG_schema, emptySchema().kind());
    }

    @Test
    void sizeMatchesSchemaSize() {
      assertEquals(0, emptySchema().size());
      assertEquals(1, singleLabelSchema().size());
    }
  }

  @Nested
  class MapDelegation {

    @Test
    void isEmptyWhenSchemaEmpty() {
      assertTrue(emptySchema().isEmpty());
      assertFalse(singleLabelSchema().isEmpty());
    }

    @Test
    void containsKeyDelegates() {
      ApgSchema<String> schema = singleLabelSchema();
      assertTrue(schema.containsKey("Person"));
      assertFalse(schema.containsKey("Animal"));
    }

    @Test
    void getDelegates() {
      ApgSchema<String> schema = singleLabelSchema();
      ApgTy<String> ty = schema.get("Person");
      assertEquals(ApgTy.ApgTyB("Int"), ty);
      assertNull(schema.get("Missing"));
    }

    @Test
    void putDelegates() {
      ApgSchema<String> schema = emptySchema();
      schema.put("Name", ApgTy.ApgTyB("String"));
      assertEquals(1, schema.size());
      assertTrue(schema.containsKey("Name"));
    }

    @Test
    void removeDelegates() {
      ApgSchema<String> schema = singleLabelSchema();
      schema.remove("Person");
      assertTrue(schema.isEmpty());
    }

    @Test
    void keySetDelegates() {
      ApgSchema<String> schema = singleLabelSchema();
      assertTrue(schema.keySet().contains("Person"));
      assertEquals(1, schema.keySet().size());
    }
  }

  @Nested
  class Equality {

    @Test
    void equalsSame() {
      ApgTypeside ts = emptyTypeside();
      Map<String, ApgTy<String>> map = new HashMap<>();
      map.put("X", ApgTy.ApgTyB("Int"));
      ApgSchema<String> s1 = new ApgSchema<>(ts, map);
      ApgSchema<String> s2 = new ApgSchema<>(ts, new HashMap<>(map));
      assertEquals(s1, s2);
    }

    @Test
    void notEqualsDifferentSchema() {
      ApgTypeside ts = emptyTypeside();
      Map<String, ApgTy<String>> map1 = new HashMap<>();
      map1.put("X", ApgTy.ApgTyB("Int"));
      Map<String, ApgTy<String>> map2 = new HashMap<>();
      map2.put("Y", ApgTy.ApgTyB("Int"));
      assertNotEquals(new ApgSchema<>(ts, map1), new ApgSchema<>(ts, map2));
    }

    @Test
    void notEqualsDifferentTypeside() {
      Map<String, ApgTy<String>> map = new HashMap<>();
      ApgSchema<String> s1 = new ApgSchema<>(emptyTypeside(), map);
      ApgSchema<String> s2 = new ApgSchema<>(null, map);
      assertNotEquals(s1, s2);
    }

    @Test
    void equalsReflexive() {
      ApgSchema<String> s = singleLabelSchema();
      assertEquals(s, s);
    }

    @Test
    void notEqualsNull() {
      assertNotEquals(null, singleLabelSchema());
    }

    @Test
    void hashCodeConsistent() {
      ApgTypeside ts = emptyTypeside();
      Map<String, ApgTy<String>> map = new HashMap<>();
      map.put("X", ApgTy.ApgTyB("Int"));
      ApgSchema<String> s1 = new ApgSchema<>(ts, map);
      ApgSchema<String> s2 = new ApgSchema<>(ts, new HashMap<>(map));
      assertEquals(s1.hashCode(), s2.hashCode());
    }
  }

  @Nested
  class ToStringTest {

    @Test
    void toStringContainsLabels() {
      ApgSchema<String> schema = singleLabelSchema();
      String str = schema.toString();
      assertTrue(str.contains("labels"));
    }
  }
}
