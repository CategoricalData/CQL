package catdata.apg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgTyTest {

  @Nested
  class FactoryMethods {

    @Test
    void apgTyLSetsLabel() {
      ApgTy<String> ty = ApgTy.ApgTyL("myLabel");
      assertEquals("myLabel", ty.l);
      assertNull(ty.b);
      assertNull(ty.m);
    }

    @Test
    void apgTyBSetsBaseType() {
      ApgTy<String> ty = ApgTy.ApgTyB("Int");
      assertEquals("Int", ty.b);
      assertNull(ty.l);
      assertNull(ty.m);
    }

    @Test
    void apgTyPSetsProductFields() {
      Map<String, ApgTy<String>> fields = new HashMap<>();
      fields.put("name", ApgTy.ApgTyB("String"));
      fields.put("age", ApgTy.ApgTyB("Int"));
      ApgTy<String> ty = ApgTy.ApgTyP(true, fields);
      assertTrue(ty.all);
      assertNotNull(ty.m);
      assertEquals(2, ty.m.size());
      assertNull(ty.l);
      assertNull(ty.b);
    }

    @Test
    void apgTyPSetsSumFields() {
      Map<String, ApgTy<String>> fields = new HashMap<>();
      fields.put("left", ApgTy.ApgTyB("Int"));
      fields.put("right", ApgTy.ApgTyB("String"));
      ApgTy<String> ty = ApgTy.ApgTyP(false, fields);
      assertFalse(ty.all);
      assertNotNull(ty.m);
      assertEquals(2, ty.m.size());
    }
  }

  @Nested
  class Caching {

    @Test
    void cachedInstancesSameReference() {
      ApgTy<String> ty1 = ApgTy.ApgTyB("CachedType");
      ApgTy<String> ty2 = ApgTy.ApgTyB("CachedType");
      assertSame(ty1, ty2);
    }
  }

  @Nested
  class EqualsHashCode {

    @Test
    void identityEquals() {
      ApgTy<String> ty1 = ApgTy.ApgTyB("X");
      ApgTy<String> ty2 = ApgTy.ApgTyB("Y");
      // identity-based equals: same ref returns true
      assertEquals(ty1, ty1);
      // different structural types are different references
      assertNotEquals(ty1, ty2);
      // even structurally different objects are not equal
      assertFalse(ty1.equals(ty2));
    }

    @Test
    void equals2MatchesStructurally() {
      // Since the cache returns the same instance for same args,
      // we test equals2 with two instances that are the same ref
      ApgTy<String> ty = ApgTy.ApgTyB("Str");
      assertTrue(ty.equals2(ty));

      // Also test with a label type
      ApgTy<String> tyL1 = ApgTy.ApgTyL("lab");
      assertTrue(tyL1.equals2(tyL1));
    }

    @Test
    void equals2DifferentStructure() {
      ApgTy<String> tyB = ApgTy.ApgTyB("Int");
      ApgTy<String> tyL = ApgTy.ApgTyL("label");
      assertFalse(tyB.equals2(tyL));
      assertFalse(tyL.equals2(tyB));
    }

    @Test
    void hashCode2ConsistentWithEquals2() {
      ApgTy<String> ty1 = ApgTy.ApgTyB("HC");
      ApgTy<String> ty2 = ApgTy.ApgTyB("HC");
      // Same instance from cache, so equals2 is true
      assertTrue(ty1.equals2(ty2));
      assertEquals(ty1.hashCode2(), ty2.hashCode2());
    }
  }

  @Nested
  class ToStringTest {

    @Test
    void toStringLabel() {
      ApgTy<String> ty = ApgTy.ApgTyL("Person");
      assertEquals("Person", ty.toString());
    }

    @Test
    void toStringBaseType() {
      ApgTy<String> ty = ApgTy.ApgTyB("Int");
      assertEquals("Int", ty.toString());
    }

    @Test
    void toStringProduct() {
      Map<String, ApgTy<String>> fields = new HashMap<>();
      fields.put("x", ApgTy.ApgTyB("Int"));
      fields.put("y", ApgTy.ApgTyB("Str"));
      ApgTy<String> ty = ApgTy.ApgTyP(true, fields);
      String str = ty.toString();
      assertTrue(str.startsWith("("), "product toString should start with '(' but was: " + str);
      assertTrue(str.endsWith(")"), "product toString should end with ')' but was: " + str);
      assertTrue(str.contains("x"), "product toString should contain field 'x' but was: " + str);
      assertTrue(str.contains("*"), "product toString should contain '*' separator but was: " + str);
    }

    @Test
    void toStringSum() {
      Map<String, ApgTy<String>> fields = new HashMap<>();
      fields.put("a", ApgTy.ApgTyB("Int"));
      fields.put("b", ApgTy.ApgTyB("Str"));
      ApgTy<String> ty = ApgTy.ApgTyP(false, fields);
      String str = ty.toString();
      assertTrue(str.startsWith("<"), "sum toString should start with '<' but was: " + str);
      assertTrue(str.endsWith(">"), "sum toString should end with '>' but was: " + str);
      assertTrue(str.contains("a"), "sum toString should contain field 'a' but was: " + str);
      assertTrue(str.contains("+"), "sum toString should contain '+' separator but was: " + str);
    }
  }

  @Nested
  class MapTest {

    @Test
    void mapTransformsLabel() {
      ApgTy<String> ty = ApgTy.ApgTyL("old");
      ApgTy<Integer> mapped = ty.map(label -> ApgTy.ApgTyL(label.length()));
      assertEquals(3, mapped.l);
    }

    @Test
    void mapPreservesBase() {
      ApgTy<String> ty = ApgTy.ApgTyB("Int");
      ApgTy<Integer> mapped = ty.map(label -> ApgTy.ApgTyL(label.length()));
      assertEquals("Int", mapped.b);
      assertNull(mapped.l);
    }

    @Test
    void mapTransformsProduct() {
      Map<String, ApgTy<String>> fields = new HashMap<>();
      fields.put("f", ApgTy.ApgTyL("lbl"));
      ApgTy<String> ty = ApgTy.ApgTyP(true, fields);
      ApgTy<String> mapped = ty.map(label -> ApgTy.ApgTyL(label.toUpperCase()));
      assertNotNull(mapped.m);
      assertTrue(mapped.all);
      assertEquals("LBL", mapped.m.get("f").l);
    }
  }

  @Nested
  class ValidateTest {

    private ApgSchema<String> createSchema(Map<String, ApgTy<String>> schemaMap) {
      ApgTypeside typeside = new ApgTypeside(Collections.emptyMap(), Collections.emptyMap());
      return new ApgSchema<>(typeside, schemaMap);
    }

    @Test
    void validateSucceedsForValidLabel() {
      Map<String, ApgTy<String>> schemaMap = new HashMap<>();
      schemaMap.put("Person", ApgTy.ApgTyB("String"));
      ApgSchema<String> schema = createSchema(schemaMap);

      ApgTy<String> ty = ApgTy.ApgTyL("Person");
      ty.validate(schema); // should not throw
    }

    @Test
    void validateThrowsForInvalidLabel() {
      Map<String, ApgTy<String>> schemaMap = new HashMap<>();
      ApgSchema<String> schema = createSchema(schemaMap);

      ApgTy<String> ty = ApgTy.ApgTyL("Missing");
      assertThrows(RuntimeException.class, () -> ty.validate(schema));
    }

    @Test
    void validateRecursesIntoFields() {
      Map<String, ApgTy<String>> schemaMap = new HashMap<>();
      schemaMap.put("Name", ApgTy.ApgTyB("String"));
      ApgSchema<String> schema = createSchema(schemaMap);

      Map<String, ApgTy<String>> fields = new HashMap<>();
      fields.put("name", ApgTy.ApgTyL("Name"));
      ApgTy<String> product = ApgTy.ApgTyP(true, fields);
      product.validate(schema); // should not throw, "Name" exists in schema

      // Now with invalid nested label
      Map<String, ApgTy<String>> badFields = new HashMap<>();
      badFields.put("missing", ApgTy.ApgTyL("NoSuchLabel"));
      ApgTy<String> badProduct = ApgTy.ApgTyP(true, badFields);
      assertThrows(RuntimeException.class, () -> badProduct.validate(schema));
    }
  }
}
