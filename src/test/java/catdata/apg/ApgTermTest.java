package catdata.apg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgTermTest {

  @Nested
  class ApgTermETest {

    @Test
    void factorySetsElement() {
      ApgTerm<String, String> t = ApgTerm.ApgTermE("elem1");
      assertEquals("elem1", t.e);
      assertNull(t.value);
      assertNull(t.fields);
      assertNull(t.inj);
      assertNull(t.var);
    }

    @Test
    void cachedSameReference() {
      ApgTerm<String, String> t1 = ApgTerm.ApgTermE("cached_elem");
      ApgTerm<String, String> t2 = ApgTerm.ApgTermE("cached_elem");
      assertSame(t1, t2);
    }

    @Test
    void toStringReturnsElement() {
      ApgTerm<String, String> t = ApgTerm.ApgTermE("hello");
      assertEquals("hello", t.toString());
    }
  }

  @Nested
  class ApgTermVTest {

    @Test
    void factorySetsValueAndPrim() {
      ApgTerm<String, String> t = ApgTerm.ApgTermV(42, "Int");
      assertEquals(42, t.value);
      assertEquals("Int", t.prim);
      assertNull(t.e);
      assertNull(t.fields);
    }

    @Test
    void toStringReturnsValue() {
      ApgTerm<String, String> t = ApgTerm.ApgTermV(42, "Int");
      assertEquals("42", t.toString());
    }
  }

  @Nested
  class ApgTermTupleTest {

    @Test
    void factorySetsFields() {
      Map<String, ApgTerm<String, String>> fields = new LinkedHashMap<>();
      fields.put("x", ApgTerm.ApgTermV(1, "Int"));
      fields.put("y", ApgTerm.ApgTermV(2, "Int"));
      ApgTerm<String, String> t = ApgTerm.ApgTermTuple(fields);
      assertNotNull(t.fields);
      assertEquals(2, t.fields.size());
      assertNull(t.e);
    }

    @Test
    void toStringContainsColon() {
      Map<String, ApgTerm<String, String>> fields = new LinkedHashMap<>();
      fields.put("a", ApgTerm.ApgTermV(1, "Int"));
      ApgTerm<String, String> t = ApgTerm.ApgTermTuple(fields);
      assertTrue(t.toString().contains(":"));
    }
  }

  @Nested
  class ApgTermInjTest {

    @Test
    void factorySetsInjAndArg() {
      ApgTy<String> ty = ApgTy.ApgTyB("Bool");
      ApgTerm<String, String> inner = ApgTerm.ApgTermV(true, "Bool");
      ApgTerm<String, String> t = ApgTerm.ApgTermInj("left", inner, ty);
      assertEquals("left", t.inj);
      assertSame(inner, t.a);
      assertSame(ty, t.cases_t);
    }

    @Test
    void toStringContainsAngleBrackets() {
      ApgTy<String> ty = ApgTy.ApgTyB("Bool");
      ApgTerm<String, String> inner = ApgTerm.ApgTermV(true, "Bool");
      ApgTerm<String, String> t = ApgTerm.ApgTermInj("left", inner, ty);
      String s = t.toString();
      assertTrue(s.contains("<"));
      assertTrue(s.contains(">"));
    }
  }

  @Nested
  class ApgTermVarTest {

    @Test
    void factorySetsVar() {
      ApgTerm<String, String> t = ApgTerm.ApgTermVar("x");
      assertEquals("x", t.var);
      assertNull(t.e);
      assertNull(t.value);
    }

    @Test
    void toStringReturnsVar() {
      ApgTerm<String, String> t = ApgTerm.ApgTermVar("myVar");
      assertEquals("myVar", t.toString());
    }

    @Test
    void substReplacesMatchingVar() {
      ApgTerm<String, String> varTerm = ApgTerm.ApgTermVar("x");
      ApgTerm<String, String> replacement = ApgTerm.ApgTermV(99, "Int");
      ApgTerm<String, String> result = varTerm.subst("x", replacement);
      assertSame(replacement, result);
    }

    @Test
    void substPreservesNonMatchingVar() {
      ApgTerm<String, String> varTerm = ApgTerm.ApgTermVar("y");
      ApgTerm<String, String> replacement = ApgTerm.ApgTermV(99, "Int");
      ApgTerm<String, String> result = varTerm.subst("x", replacement);
      assertEquals("y", result.var);
    }

    @Test
    void renameChangesMatchingVar() {
      ApgTerm<String, String> varTerm = ApgTerm.ApgTermVar("x");
      ApgTerm<String, String> result = varTerm.rename("x", "z");
      assertEquals("z", result.var);
    }

    @Test
    void renamePreservesNonMatchingVar() {
      ApgTerm<String, String> varTerm = ApgTerm.ApgTermVar("y");
      ApgTerm<String, String> result = varTerm.rename("x", "z");
      assertSame(varTerm, result);
    }
  }

  @Nested
  class ApgTermProjTest {

    @Test
    void factorySetsProjAndArg() {
      ApgTerm<String, String> inner = ApgTerm.ApgTermVar("t");
      ApgTerm<String, String> t = ApgTerm.ApgTermProj("field1", inner);
      assertEquals("field1", t.proj);
      assertSame(inner, t.a);
    }

    @Test
    void toStringContainsDot() {
      ApgTerm<String, String> inner = ApgTerm.ApgTermVar("t");
      ApgTerm<String, String> t = ApgTerm.ApgTermProj("field1", inner);
      assertTrue(t.toString().contains("."));
    }
  }

  @Nested
  class ApgTermDerefTest {

    @Test
    void factorySetsDerefAndArg() {
      ApgTerm<String, String> inner = ApgTerm.ApgTermVar("t");
      ApgTerm<String, String> t = ApgTerm.ApgTermDeref("label", inner);
      assertEquals("label", t.deref);
      assertSame(inner, t.a);
    }

    @Test
    void toStringContainsBang() {
      ApgTerm<String, String> inner = ApgTerm.ApgTermVar("t");
      ApgTerm<String, String> t = ApgTerm.ApgTermDeref("label", inner);
      assertTrue(t.toString().contains("!"));
    }
  }

  @Nested
  class MapTest {

    @Test
    void mapTransformsElement() {
      ApgTerm<String, Integer> t = ApgTerm.ApgTermE(5);
      ApgTerm<String, String> result = t.map(Object::toString);
      assertEquals("5", result.e);
    }

    @Test
    void mapPreservesValue() {
      ApgTerm<String, Integer> t = ApgTerm.ApgTermV(42, "Int");
      ApgTerm<String, String> result = t.map(Object::toString);
      assertEquals(42, result.value);
      assertEquals("Int", result.prim);
    }

    @Test
    void mapPreservesVar() {
      ApgTerm<String, Integer> t = ApgTerm.ApgTermVar("x");
      ApgTerm<String, String> result = t.map(Object::toString);
      assertEquals("x", result.var);
    }

    @Test
    void mapTransformsTupleElements() {
      Map<String, ApgTerm<String, Integer>> fields = new LinkedHashMap<>();
      fields.put("a", ApgTerm.ApgTermE(1));
      fields.put("b", ApgTerm.ApgTermE(2));
      ApgTerm<String, Integer> t = ApgTerm.ApgTermTuple(fields);
      ApgTerm<String, String> result = t.map(Object::toString);
      assertNotNull(result.fields);
      assertEquals("1", result.fields.get("a").e);
      assertEquals("2", result.fields.get("b").e);
    }

    @Test
    void mapTransformsInjElement() {
      ApgTy<String> ty = ApgTy.ApgTyB("Bool");
      ApgTerm<String, Integer> inner = ApgTerm.ApgTermE(10);
      ApgTerm<String, Integer> t = ApgTerm.ApgTermInj("left", inner, ty);
      ApgTerm<String, String> result = t.map(Object::toString);
      assertEquals("left", result.inj);
      assertEquals("10", result.a.e);
    }
  }

  @Nested
  class Equals2Test {

    @Test
    void sameStructureReturnsTrue() {
      ApgTerm<String, String> t1 = ApgTerm.ApgTermE("a");
      ApgTerm<String, String> t2 = ApgTerm.ApgTermE("a");
      assertTrue(t1.equals2(t2));
    }

    @Test
    void differentStructureReturnsFalse() {
      ApgTerm<String, String> t1 = ApgTerm.ApgTermE("a");
      ApgTerm<String, String> t2 = ApgTerm.ApgTermE("b");
      assertFalse(t1.equals2(t2));
    }

    @Test
    void reflexive() {
      ApgTerm<String, String> t = ApgTerm.ApgTermV(42, "Int");
      assertTrue(t.equals2(t));
    }

    @Test
    void nullReturnsFalse() {
      ApgTerm<String, String> t = ApgTerm.ApgTermVar("x");
      assertFalse(t.equals2(null));
    }
  }
}
