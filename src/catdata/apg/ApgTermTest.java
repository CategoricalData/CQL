package catdata.apg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import java.util.LinkedHashMap;
import java.util.List;
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

    /**
     * Terms are shared through a cache keyed by this equality, which ignores the annotation of an
     * injection or a case analysis, so a term gets the annotation of the first equal term built.
     */
    @Test
    void ignoresTheAnnotationOfAnInjectionOrACaseAnalysis() {
      ApgTy<String> one = ApgTy.ApgTyB("Annotated");
      ApgTy<String> narrow = ApgTy.ApgTyP(false, Map.of("one", one));
      ApgTy<String> wide = ApgTy.ApgTyP(false, Map.of("one", one, "two", one));
      ApgTerm<String, String> arg = ApgTerm.ApgTermV("equals2-annotation-probe", "Annotated");
      ApgTerm<String, String> first = ApgTerm.ApgTermInj("one", arg, narrow);
      ApgTerm<String, String> second = ApgTerm.ApgTermInj("one", arg, wide);
      assertTrue(first.equals2(second));
      assertSame(first, second);
      assertSame(narrow, second.cases_t);
      Map<String, Pair<String, ApgTerm<String, String>>> cases =
          Map.of("one", new Pair<>("x", ApgTerm.ApgTermVar("x")));
      ApgTerm<String, String> c1 = ApgTerm.ApgTermCase(first, cases, one);
      ApgTerm<String, String> c2 = ApgTerm.ApgTermCase(first, cases, ApgTy.ApgTyB("Other"));
      assertSame(c1, c2);
      assertSame(one, c2.cases_t);
    }
  }

  /**
   * Substitution into a case branch {@code lambda x. body} whose body mentions the substituted
   * variable renames that variable, instead of x, to a fresh name and makes it the binder, so the
   * substitution is lost and x becomes free.
   */
  @Nested
  class SubstitutionUnderACase {

    private final ApgTy<String> sum = ApgTy.ApgTyP(false, Map.of("l", ApgTy.ApgTyB("capture-ty")));

    private final ApgTerm<String, String> v = ApgTerm.ApgTermVar("capture-v");

    private ApgTerm<String, String> body() {
      return ApgTerm.ApgTermTuple(
          Map.of(
              "p", ApgTerm.ApgTermProj("f", v),
              "q", ApgTerm.ApgTermInj("l", ApgTerm.ApgTermDeref("L", v), sum),
              "r", ApgTerm.ApgTermApp("h", List.of(v, ApgTerm.ApgTermV("one", "capture-ty"))),
              "s", ApgTerm.ApgTermVar("capture-x"),
              "t",
                  ApgTerm.ApgTermCase(
                      v,
                      Map.of("l", new Pair<>("capture-y", ApgTerm.ApgTermVar("capture-y"))),
                      sum)));
    }

    private ApgTerm<String, String> caseOf(String binder, ApgTerm<String, String> body) {
      return ApgTerm.ApgTermCase(
          ApgTerm.ApgTermVar("capture-w"), Map.of("l", new Pair<>(binder, body)), sum);
    }

    @Test
    void bindsTheSubstitutedVariableAndFreesTheBinder() {
      ApgTerm<String, String> r =
          caseOf("capture-x", body()).subst("capture-v", ApgTerm.ApgTermE("e"));
      Pair<String, ApgTerm<String, String>> branch = r.cases.get("l");
      assertTrue(branch.first.startsWith("_gensym_"), branch.first);
      ApgTerm<String, String> z = ApgTerm.ApgTermVar(branch.first);
      assertSame(z, branch.second.fields.get("p").a);
      assertSame(z, branch.second.fields.get("q").a.a);
      assertSame(z, branch.second.fields.get("r").args.get(0));
      assertSame(z, branch.second.fields.get("t").a);
      assertSame(ApgTerm.ApgTermVar("capture-x"), branch.second.fields.get("s"));
      assertFalse(r.toString().contains("(e)"), r.toString());
    }

    @Test
    void leavesABranchAloneThatBindsOrDoesNotMentionTheVariable() {
      ApgTerm<String, String> shadow = caseOf("capture-v", body());
      assertSame(shadow, shadow.subst("capture-v", ApgTerm.ApgTermE("e")));
      ApgTerm<String, String> other = caseOf("capture-x", ApgTerm.ApgTermVar("capture-x"));
      assertSame(other, other.subst("capture-v", ApgTerm.ApgTermE("e")));
    }
  }
}
