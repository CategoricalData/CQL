package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RowTest {

  private static Row<String, Integer, String> emptyRow() {
    return new Row<>("entity");
  }

  private static Row<String, Integer, String> singleRow() {
    return new Row<>(emptyRow(), "a", 1, "entity", "typeA");
  }

  private static Row<String, Integer, String> multiRow() {
    Row<String, Integer, String> r = emptyRow();
    r = new Row<>(r, "a", 1, "entity", "typeA");
    r = new Row<>(r, "b", 2, "entity", "typeB");
    return r;
  }

  @Nested
  class Constructor {

    @Test
    void emptyRowSetsEn2() {
      Row<String, Integer, String> row = new Row<>("myEntity");
      assertEquals("myEntity", row.en2());
    }

    @Test
    void fullConstructorSetsFields() {
      Row<String, Integer, String> tail = emptyRow();
      Row<String, Integer, String> row = new Row<>(tail, "col", 42, "entity", "type");
      assertEquals("entity", row.en2());
      assertEquals("type", row.t);
      assertEquals(42, row.get("col"));
    }

    @Test
    void constructorRejectsNullVariable() {
      Row<String, Integer, String> tail = emptyRow();
      assertThrows(RuntimeException.class, () -> new Row<>(tail, null, 1, "entity", "type"));
    }
  }

  @Nested
  class GetMethod {

    @Test
    void getReturnsValueForMatchingKey() {
      assertEquals(1, singleRow().get("a"));
    }

    @Test
    void getSearchesThroughTail() {
      Row<String, Integer, String> row = multiRow();
      assertEquals(1, row.get("a"));
      assertEquals(2, row.get("b"));
    }

    @Test
    void getThrowsForMissingKey() {
      assertThrows(RuntimeException.class, () -> singleRow().get("nonexistent"));
    }
  }

  @Nested
  class AsMapMethod {

    @Test
    void emptyRowReturnsEmptyMap() {
      assertTrue(emptyRow().asMap().isEmpty());
    }

    @Test
    void singleRowReturnsOneEntry() {
      Map<String, Integer> map = singleRow().asMap();
      assertEquals(1, map.size());
      assertEquals(1, map.get("a"));
    }

    @Test
    void multiRowReturnsAllEntries() {
      Map<String, Integer> map = multiRow().asMap();
      assertEquals(2, map.size());
      assertEquals(1, map.get("a"));
      assertEquals(2, map.get("b"));
    }

    @Test
    void asMapReturnsCachedInstance() {
      Row<String, Integer, String> row = multiRow();
      Map<String, Integer> first = row.asMap();
      Map<String, Integer> second = row.asMap();
      assertTrue(first == second);
    }
  }

  @Nested
  class MapMethod {

    @Test
    void mapOnEmptyRowReturnsEmptyRow() {
      Row<String, String, String> mapped = emptyRow().map((x, t) -> "mapped");
      assertTrue(mapped.asMap().isEmpty());
    }

    @Test
    void mapTransformsValues() {
      Row<String, String, String> mapped = multiRow().map((x, t) -> "v" + x);
      assertEquals("v1", mapped.get("a"));
      assertEquals("v2", mapped.get("b"));
    }

    @Test
    void mapPreservesEntity() {
      Row<String, String, String> mapped = singleRow().map((x, t) -> x.toString());
      assertEquals("entity", mapped.en2());
    }
  }

  @Nested
  class MkRowMethod {

    @Test
    void mkRowBuildsFromMaps() {
      List<String> order = Arrays.asList("x", "y");
      Map<String, Integer> values = new HashMap<>();
      values.put("x", 10);
      values.put("y", 20);
      Map<String, String> types1 = new HashMap<>();
      types1.put("x", "Int");
      Map<String, String> types2 = new HashMap<>();
      types2.put("y", "String");

      Row<String, Integer, String> row = Row.mkRow(order, values, "e", types1, types2);
      assertEquals(10, row.get("x"));
      assertEquals(20, row.get("y"));
      assertEquals("e", row.en2());
    }

    @Test
    void mkRowFallsBackToCtx3ForType() {
      List<String> order = Arrays.asList("a");
      Map<String, Integer> values = new HashMap<>();
      values.put("a", 5);
      Map<String, String> types1 = new HashMap<>();
      Map<String, String> types2 = new HashMap<>();
      types2.put("a", "FallbackType");

      Row<String, Integer, String> row = Row.mkRow(order, values, "e", types1, types2);
      assertEquals("FallbackType", row.t);
    }

    @Test
    void mkRowEmptyOrderReturnsEmptyRow() {
      Row<String, Integer, String> row = Row.mkRow(
          List.of(), new HashMap<>(), "e", new HashMap<>(), new HashMap<>());
      assertTrue(row.asMap().isEmpty());
    }
  }

  @Nested
  class ToStringMethod {

    @Test
    void emptyRowToStringIsEmpty() {
      assertEquals("", emptyRow().toString());
    }

    @Test
    void singleRowToStringShowsKeyValue() {
      assertEquals("(a=1)", singleRow().toString());
    }

    @Test
    void multiRowToStringShowsAllPairs() {
      String str = multiRow().toString();
      assertTrue(str.contains("(a=1)"));
      assertTrue(str.contains("(b=2)"));
    }

    @Test
    void toStringWithFunctionAppliesTransform() {
      String str = singleRow().toString(x -> "[" + x + "]");
      assertTrue(str.contains("[1]"));
    }
  }

  @Nested
  class EqualsAndHashCode {

    @Test
    void equalRowsAreEqual() {
      assertEquals(singleRow(), singleRow());
    }

    @Test
    void differentValuesNotEqual() {
      Row<String, Integer, String> a = new Row<>(emptyRow(), "a", 1, "entity", "typeA");
      Row<String, Integer, String> b = new Row<>(emptyRow(), "a", 99, "entity", "typeA");
      assertNotEquals(a, b);
    }

    @Test
    void differentEntitiesNotEqual() {
      Row<String, Integer, String> a = new Row<>("e1");
      Row<String, Integer, String> b = new Row<>("e2");
      assertNotEquals(a, b);
    }

    @Test
    void equalRowsHaveSameHashCode() {
      assertEquals(singleRow().hashCode(), singleRow().hashCode());
    }

    @Test
    void notEqualToNull() {
      assertNotEquals(null, singleRow());
    }

    @Test
    void equalToSelf() {
      Row<String, Integer, String> row = singleRow();
      assertEquals(row, row);
    }

    @Test
    void notEqualToDifferentType() {
      assertFalse(singleRow().equals("not a row"));
    }
  }

  @Nested
  class RowEqualsMethod {

    @Test
    void rowEqualsWithSameValues() {
      assertTrue(singleRow().rowEquals((a, b) -> a.equals(b), singleRow()));
    }

    @Test
    void rowEqualsWithCustomPredicate() {
      Row<String, Integer, String> a = new Row<>(emptyRow(), "a", 1, "entity", "typeA");
      Row<String, Integer, String> b = new Row<>(emptyRow(), "a", 2, "entity", "typeA");
      assertTrue(a.rowEquals((x, y) -> true, b));
      assertFalse(a.rowEquals((x, y) -> false, b));
    }

    @Test
    void rowEqualsReturnsFalseForDifferentEntities() {
      Row<String, Integer, String> a = new Row<>("e1");
      Row<String, Integer, String> b = new Row<>("e2");
      assertFalse(a.rowEquals((x, y) -> true, b));
    }

    @Test
    void rowEqualsEmptyRowsAreEqual() {
      Row<String, Integer, String> a = new Row<>("entity");
      Row<String, Integer, String> b = new Row<>("entity");
      assertTrue(a.rowEquals((x, y) -> true, b));
    }
  }

  @Nested
  class En2Method {

    @Test
    void en2ReturnsEntity() {
      assertEquals("entity", singleRow().en2());
    }
  }
}
