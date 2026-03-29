package catdata.apg.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import catdata.LocStr;
import catdata.Pair;
import catdata.Unit;
import catdata.apg.exp.ApgTyExp.ApgTyExpRaw;
import catdata.apg.exp.ApgTyExp.ApgTyExpVar;
import catdata.cql.Kind;
import catdata.cql.exp.AqlTyping;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgTyExpTest {

  @Nested
  class ApgTyExpVarTest {

    @Test
    void constructorSetsVar() {
      ApgTyExpVar v = new ApgTyExpVar("myVar");
      assertEquals("myVar", v.var);
    }

    @Test
    void kindReturnsApgTypeside() {
      ApgTyExpVar v = new ApgTyExpVar("x");
      assertEquals(Kind.APG_typeside, v.kind());
    }

    @Test
    void isVarReturnsTrue() {
      ApgTyExpVar v = new ApgTyExpVar("x");
      assertTrue(v.isVar());
    }

    @Test
    void toStringReturnsVar() {
      ApgTyExpVar v = new ApgTyExpVar("myTypeside");
      assertEquals("myTypeside", v.toString());
    }

    @Test
    void depsReturnsSingletonWithCorrectKind() {
      ApgTyExpVar v = new ApgTyExpVar("ts1");
      Collection<Pair<String, Kind>> deps = v.deps();
      assertEquals(1, deps.size());
      Pair<String, Kind> dep = deps.iterator().next();
      assertEquals("ts1", dep.first);
      assertEquals(Kind.APG_typeside, dep.second);
    }

    @Test
    void equalsSameVar() {
      ApgTyExpVar v1 = new ApgTyExpVar("x");
      ApgTyExpVar v2 = new ApgTyExpVar("x");
      assertEquals(v1, v2);
      assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void notEqualsDifferentVar() {
      ApgTyExpVar v1 = new ApgTyExpVar("x");
      ApgTyExpVar v2 = new ApgTyExpVar("y");
      assertNotEquals(v1, v2);
    }

    @Test
    void equalsReflexive() {
      ApgTyExpVar v = new ApgTyExpVar("x");
      assertEquals(v, v);
    }

    @Test
    void notEqualsNull() {
      ApgTyExpVar v = new ApgTyExpVar("x");
      assertNotEquals(null, v);
    }

    @Test
    void notEqualsDifferentType() {
      ApgTyExpVar v = new ApgTyExpVar("x");
      assertNotEquals("x", v);
    }

    @Test
    void typeReturnsUnitWhenDefined() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("x", Unit.unit);
      ApgTyExpVar v = new ApgTyExpVar("x");
      assertEquals(Unit.unit, v.type(typing));
    }

    @Test
    void typeThrowsWhenUndefined() {
      AqlTyping typing = new AqlTyping();
      ApgTyExpVar v = new ApgTyExpVar("missing");
      assertThrows(RuntimeException.class, () -> v.type(typing));
    }

    @Test
    void acceptDelegatesToVisitor() throws Exception {
      ApgTyExpVar v = new ApgTyExpVar("x");
      @SuppressWarnings("unchecked")
      ApgTyExp.ApgTyExpVisitor<String, Void, RuntimeException> visitor =
          mock(ApgTyExp.ApgTyExpVisitor.class);
      v.accept(null, visitor);
      verify(visitor).visit(null, v);
    }

    @Test
    void coAcceptDelegatesToCoVisitor() throws Exception {
      ApgTyExpVar v = new ApgTyExpVar("x");
      @SuppressWarnings("unchecked")
      ApgTyExp.ApgTyExpCoVisitor<String, Void, RuntimeException> coVisitor =
          mock(ApgTyExp.ApgTyExpCoVisitor.class);
      v.coaccept(null, coVisitor, "result");
      verify(coVisitor).visitApgTyExpVar(null, "result");
    }

    @Test
    void varMethodCreatesNewVar() {
      ApgTyExpVar v = new ApgTyExpVar("x");
      var newVar = v.Var("y");
      assertNotNull(newVar);
      assertTrue(newVar instanceof ApgTyExpVar);
      assertEquals("y", ((ApgTyExpVar) newVar).var);
    }

    @Test
    void optionsReturnsEmptyMap() {
      ApgTyExpVar v = new ApgTyExpVar("x");
      assertTrue(v.options().isEmpty());
    }
  }

  @Nested
  class ApgTyExpRawTest {

    private ApgTyExpRaw createEmptyRaw() {
      return new ApgTyExpRaw(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    private ApgTyExpRaw createRawWithType(String name, String className, String parseFn) {
      List<Pair<LocStr, Pair<String, String>>> types =
          List.of(new Pair<>(new LocStr(0, name), new Pair<>(className, parseFn)));
      return new ApgTyExpRaw(Collections.emptyList(), types, Collections.emptyList());
    }

    @Test
    void constructorSetsFields() {
      ApgTyExpRaw raw = createEmptyRaw();
      assertNotNull(raw.imports);
      assertNotNull(raw.types);
      assertNotNull(raw.udfs);
      assertTrue(raw.imports.isEmpty());
      assertTrue(raw.types.isEmpty());
      assertTrue(raw.udfs.isEmpty());
    }

    @Test
    void constructorWithTypes() {
      ApgTyExpRaw raw = createRawWithType("Int", "java.lang.Integer", "parseInt");
      assertEquals(1, raw.types.size());
      assertTrue(raw.types.containsKey("Int"));
      assertEquals("java.lang.Integer", raw.types.get("Int").first);
    }

    @Test
    void kindReturnsApgTypeside() {
      ApgTyExpRaw raw = createEmptyRaw();
      assertEquals(Kind.APG_typeside, raw.kind());
    }

    @Test
    void depsEmptyWhenNoImports() {
      ApgTyExpRaw raw = createEmptyRaw();
      assertTrue(raw.deps().isEmpty());
    }

    @Test
    void depsIncludesImportDeps() {
      ApgTyExpVar importVar = new ApgTyExpVar("imported");
      ApgTyExpRaw raw =
          new ApgTyExpRaw(List.of(importVar), Collections.emptyList(), Collections.emptyList());
      Collection<Pair<String, Kind>> deps = raw.deps();
      assertEquals(1, deps.size());
      assertEquals("imported", deps.iterator().next().first);
    }

    @Test
    void equalsSameContent() {
      ApgTyExpRaw r1 = createEmptyRaw();
      ApgTyExpRaw r2 = createEmptyRaw();
      assertEquals(r1, r2);
      assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void notEqualsDifferentTypes() {
      ApgTyExpRaw r1 = createEmptyRaw();
      ApgTyExpRaw r2 = createRawWithType("Int", "java.lang.Integer", "parseInt");
      assertNotEquals(r1, r2);
    }

    @Test
    void equalsReflexive() {
      ApgTyExpRaw r = createEmptyRaw();
      assertEquals(r, r);
    }

    @Test
    void notEqualsNull() {
      ApgTyExpRaw r = createEmptyRaw();
      assertNotEquals(null, r);
    }

    @Test
    void toStringContainsLiteral() {
      ApgTyExpRaw raw = createEmptyRaw();
      assertTrue(raw.toString().contains("literal"));
    }

    @Test
    void rawReturnsNonNullMap() {
      ApgTyExpRaw raw = createEmptyRaw();
      assertNotNull(raw.raw());
    }

    @Test
    void acceptDelegatesToVisitor() throws Exception {
      ApgTyExpRaw raw = createEmptyRaw();
      @SuppressWarnings("unchecked")
      ApgTyExp.ApgTyExpVisitor<String, Void, RuntimeException> visitor =
          mock(ApgTyExp.ApgTyExpVisitor.class);
      raw.accept(null, visitor);
      verify(visitor).visit(null, raw);
    }

    @Test
    void coAcceptDelegatesToCoVisitor() throws Exception {
      ApgTyExpRaw raw = createEmptyRaw();
      @SuppressWarnings("unchecked")
      ApgTyExp.ApgTyExpCoVisitor<String, Void, RuntimeException> coVisitor =
          mock(ApgTyExp.ApgTyExpCoVisitor.class);
      raw.coaccept(null, coVisitor, "result");
      verify(coVisitor).visitApgTyExpRaw(null, "result");
    }

    @Test
    void isVarReturnsFalse() {
      ApgTyExpRaw raw = createEmptyRaw();
      assertFalse(raw.isVar());
    }
  }

  @Test
  void baseTypeReturnsUnit() {
    ApgTyExpVar v = new ApgTyExpVar("x");
    AqlTyping typing = new AqlTyping();
    typing.defs.apgts.put("x", Unit.unit);
    assertEquals(Unit.unit, v.type(typing));
  }
}
