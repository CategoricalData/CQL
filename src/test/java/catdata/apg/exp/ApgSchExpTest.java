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
import catdata.apg.ApgTy;
import catdata.apg.exp.ApgSchExp.ApgSchExpInitial;
import catdata.apg.exp.ApgSchExp.ApgSchExpPlus;
import catdata.apg.exp.ApgSchExp.ApgSchExpRaw;
import catdata.apg.exp.ApgSchExp.ApgSchExpTerminal;
import catdata.apg.exp.ApgSchExp.ApgSchExpTimes;
import catdata.apg.exp.ApgSchExp.ApgSchExpVar;
import catdata.apg.exp.ApgTyExp.ApgTyExpVar;
import catdata.cql.Kind;
import catdata.cql.exp.AqlTyping;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgSchExpTest {

  @Nested
  class ApgSchExpVarTest {

    @Test
    void constructorSetsVar() {
      ApgSchExpVar v = new ApgSchExpVar("s1");
      assertEquals("s1", v.var);
    }

    @Test
    void kindReturnsApgSchema() {
      ApgSchExpVar v = new ApgSchExpVar("s");
      assertEquals(Kind.APG_schema, v.kind());
    }

    @Test
    void isVarReturnsTrue() {
      assertTrue(new ApgSchExpVar("s").isVar());
    }

    @Test
    void toStringReturnsVar() {
      assertEquals("mySchema", new ApgSchExpVar("mySchema").toString());
    }

    @Test
    void depsReturnsSingleton() {
      Collection<Pair<String, Kind>> deps = new ApgSchExpVar("s1").deps();
      assertEquals(1, deps.size());
      Pair<String, Kind> dep = deps.iterator().next();
      assertEquals("s1", dep.first);
      assertEquals(Kind.APG_instance, dep.second);
    }

    @Test
    void equalsSameVar() {
      ApgSchExpVar v1 = new ApgSchExpVar("s");
      ApgSchExpVar v2 = new ApgSchExpVar("s");
      assertEquals(v1, v2);
      assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void notEqualsDifferentVar() {
      assertNotEquals(new ApgSchExpVar("a"), new ApgSchExpVar("b"));
    }

    @Test
    void equalsReflexive() {
      ApgSchExpVar v = new ApgSchExpVar("s");
      assertEquals(v, v);
    }

    @Test
    void notEqualsNull() {
      assertNotEquals(null, new ApgSchExpVar("s"));
    }

    @Test
    void notEqualsDifferentType() {
      assertNotEquals("s", new ApgSchExpVar("s"));
    }

    @Test
    void typeThrowsWhenUndefined() {
      AqlTyping typing = new AqlTyping();
      assertThrows(RuntimeException.class, () -> new ApgSchExpVar("missing").type(typing));
    }

    @Test
    void typeReturnsExpWhenDefined() {
      AqlTyping typing = new AqlTyping();
      ApgTyExpVar tyExp = new ApgTyExpVar("ts1");
      typing.defs.apgschemas.put("s1", tyExp);
      ApgTyExp result = new ApgSchExpVar("s1").type(typing);
      assertEquals(tyExp, result);
    }

    @Test
    void acceptDelegatesToVisitor() {
      ApgSchExpVar v = new ApgSchExpVar("s");
      @SuppressWarnings("unchecked")
      ApgSchExp.ApgSchExpVisitor<String, Void> visitor = mock(ApgSchExp.ApgSchExpVisitor.class);
      v.accept(null, visitor);
      verify(visitor).visit(null, v);
    }

    @Test
    void coAcceptDelegatesToCoVisitor() {
      ApgSchExpVar v = new ApgSchExpVar("s");
      @SuppressWarnings("unchecked")
      ApgSchExp.ApgSchExpCoVisitor<String, Void> coVisitor =
          mock(ApgSchExp.ApgSchExpCoVisitor.class);
      v.coaccept(null, coVisitor, "r");
      verify(coVisitor).visitApgSchExpVar(null, "r");
    }

    @Test
    void varMethodCreatesNewSchExpVar() {
      ApgSchExpVar v = new ApgSchExpVar("s1");
      var newVar = v.Var("s2");
      assertTrue(newVar instanceof ApgSchExpVar);
      assertEquals("s2", ((ApgSchExpVar) newVar).var);
    }
  }

  @Nested
  class ApgSchExpRawTest {

    private ApgSchExpRaw createEmptyRaw() {
      return new ApgSchExpRaw(
          new ApgTyExpVar("ts"), Collections.emptyList(), Collections.emptyList());
    }

    @Test
    void constructorSetsFields() {
      ApgSchExpRaw raw = createEmptyRaw();
      assertNotNull(raw.typeside);
      assertNotNull(raw.imports);
      assertNotNull(raw.Ls);
      assertTrue(raw.imports.isEmpty());
      assertTrue(raw.Ls.isEmpty());
    }

    @Test
    void constructorWithLabels() {
      ApgTy<String> ty = ApgTy.ApgTyB("Int");
      List<Pair<LocStr, ApgTy<String>>> labels =
          List.of(new Pair<>(new LocStr(0, "age"), ty));
      ApgSchExpRaw raw = new ApgSchExpRaw(new ApgTyExpVar("ts"), Collections.emptyList(), labels);
      assertEquals(1, raw.Ls.size());
      assertTrue(raw.Ls.containsKey("age"));
    }

    @Test
    void depsIncludesTypesideDeps() {
      ApgSchExpRaw raw = createEmptyRaw();
      Collection<Pair<String, Kind>> deps = raw.deps();
      assertEquals(1, deps.size());
      assertEquals("ts", deps.iterator().next().first);
    }

    @Test
    void equalsSameContent() {
      ApgSchExpRaw r1 = createEmptyRaw();
      ApgSchExpRaw r2 = createEmptyRaw();
      assertEquals(r1, r2);
      assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void notEqualsDifferentTypeside() {
      ApgSchExpRaw r1 = createEmptyRaw();
      ApgSchExpRaw r2 =
          new ApgSchExpRaw(
              new ApgTyExpVar("other"), Collections.emptyList(), Collections.emptyList());
      assertNotEquals(r1, r2);
    }

    @Test
    void toStringContainsLiteral() {
      assertTrue(createEmptyRaw().toString().contains("literal"));
    }

    @Test
    void rawReturnsNonNull() {
      assertNotNull(createEmptyRaw().raw());
    }

    @Test
    void acceptDelegatesToVisitor() {
      ApgSchExpRaw raw = createEmptyRaw();
      @SuppressWarnings("unchecked")
      ApgSchExp.ApgSchExpVisitor<String, Void> visitor = mock(ApgSchExp.ApgSchExpVisitor.class);
      raw.accept(null, visitor);
      verify(visitor).visit(null, raw);
    }

    @Test
    void typeReturnsTypeside() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts", Unit.unit);
      ApgSchExpRaw raw = createEmptyRaw();
      ApgTyExp result = raw.type(typing);
      assertEquals(new ApgTyExpVar("ts"), result);
    }

    @Test
    void isVarReturnsFalse() {
      assertFalse(createEmptyRaw().isVar());
    }
  }

  @Nested
  class ApgSchExpInitialTest {

    @Test
    void constructorSetsTypeside() {
      ApgTyExpVar ts = new ApgTyExpVar("ts");
      ApgSchExpInitial init = new ApgSchExpInitial(ts);
      assertEquals(ts, init.typeside);
    }

    @Test
    void equalsSame() {
      ApgSchExpInitial i1 = new ApgSchExpInitial(new ApgTyExpVar("ts"));
      ApgSchExpInitial i2 = new ApgSchExpInitial(new ApgTyExpVar("ts"));
      assertEquals(i1, i2);
      assertEquals(i1.hashCode(), i2.hashCode());
    }

    @Test
    void notEqualsDifferent() {
      assertNotEquals(
          new ApgSchExpInitial(new ApgTyExpVar("a")),
          new ApgSchExpInitial(new ApgTyExpVar("b")));
    }

    @Test
    void toStringContainsEmpty() {
      assertTrue(new ApgSchExpInitial(new ApgTyExpVar("ts")).toString().contains("empty"));
    }

    @Test
    void depsMatchesTypesideDeps() {
      ApgSchExpInitial init = new ApgSchExpInitial(new ApgTyExpVar("ts"));
      assertEquals(new ApgTyExpVar("ts").deps(), init.deps());
    }

    @Test
    void typeReturnsTypeside() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts", Unit.unit);
      ApgTyExpVar ts = new ApgTyExpVar("ts");
      ApgSchExpInitial init = new ApgSchExpInitial(ts);
      assertEquals(ts, init.type(typing));
    }

    @Test
    void acceptDelegatesToVisitor() {
      ApgSchExpInitial init = new ApgSchExpInitial(new ApgTyExpVar("ts"));
      @SuppressWarnings("unchecked")
      ApgSchExp.ApgSchExpVisitor<String, Void> visitor = mock(ApgSchExp.ApgSchExpVisitor.class);
      init.accept(null, visitor);
      verify(visitor).visit(null, init);
    }
  }

  @Nested
  class ApgSchExpTerminalTest {

    @Test
    void constructorSetsTypeside() {
      ApgTyExpVar ts = new ApgTyExpVar("ts");
      ApgSchExpTerminal term = new ApgSchExpTerminal(ts);
      assertEquals(ts, term.typeside);
    }

    @Test
    void equalsSame() {
      ApgSchExpTerminal t1 = new ApgSchExpTerminal(new ApgTyExpVar("ts"));
      ApgSchExpTerminal t2 = new ApgSchExpTerminal(new ApgTyExpVar("ts"));
      assertEquals(t1, t2);
    }

    @Test
    void toStringContainsUnit() {
      assertTrue(new ApgSchExpTerminal(new ApgTyExpVar("ts")).toString().contains("unit"));
    }

    @Test
    void typeReturnsTypeside() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts", Unit.unit);
      ApgTyExpVar ts = new ApgTyExpVar("ts");
      assertEquals(ts, new ApgSchExpTerminal(ts).type(typing));
    }

    @Test
    void acceptDelegatesToVisitor() {
      ApgSchExpTerminal term = new ApgSchExpTerminal(new ApgTyExpVar("ts"));
      @SuppressWarnings("unchecked")
      ApgSchExp.ApgSchExpVisitor<String, Void> visitor = mock(ApgSchExp.ApgSchExpVisitor.class);
      term.accept(null, visitor);
      verify(visitor).visit(null, term);
    }
  }

  @Nested
  class ApgSchExpTimesTest {

    @Test
    void constructorSetsFields() {
      ApgSchExpVar l = new ApgSchExpVar("s1");
      ApgSchExpVar r = new ApgSchExpVar("s2");
      ApgSchExpTimes times = new ApgSchExpTimes(l, r);
      assertEquals(l, times.l);
      assertEquals(r, times.r);
    }

    @Test
    void equalsSame() {
      ApgSchExpTimes t1 = new ApgSchExpTimes(new ApgSchExpVar("a"), new ApgSchExpVar("b"));
      ApgSchExpTimes t2 = new ApgSchExpTimes(new ApgSchExpVar("a"), new ApgSchExpVar("b"));
      assertEquals(t1, t2);
      assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void notEqualsSwapped() {
      ApgSchExpTimes t1 = new ApgSchExpTimes(new ApgSchExpVar("a"), new ApgSchExpVar("b"));
      ApgSchExpTimes t2 = new ApgSchExpTimes(new ApgSchExpVar("b"), new ApgSchExpVar("a"));
      assertNotEquals(t1, t2);
    }

    @Test
    void toStringContainsStar() {
      String s = new ApgSchExpTimes(new ApgSchExpVar("a"), new ApgSchExpVar("b")).toString();
      assertTrue(s.contains("*"));
    }

    @Test
    void depsUnionsBothSides() {
      ApgSchExpTimes times =
          new ApgSchExpTimes(new ApgSchExpVar("a"), new ApgSchExpVar("b"));
      Collection<Pair<String, Kind>> deps = times.deps();
      assertEquals(2, deps.size());
    }

    @Test
    void typeThrowsOnDifferentTypesides() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts1", Unit.unit);
      typing.defs.apgts.put("ts2", Unit.unit);
      typing.defs.apgschemas.put("a", new ApgTyExpVar("ts1"));
      typing.defs.apgschemas.put("b", new ApgTyExpVar("ts2"));
      ApgSchExpTimes times = new ApgSchExpTimes(new ApgSchExpVar("a"), new ApgSchExpVar("b"));
      assertThrows(RuntimeException.class, () -> times.type(typing));
    }

    @Test
    void typeSucceedsWithSameTypeside() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts", Unit.unit);
      typing.defs.apgschemas.put("a", new ApgTyExpVar("ts"));
      typing.defs.apgschemas.put("b", new ApgTyExpVar("ts"));
      ApgSchExpTimes times = new ApgSchExpTimes(new ApgSchExpVar("a"), new ApgSchExpVar("b"));
      ApgTyExp result = times.type(typing);
      assertEquals(new ApgTyExpVar("ts"), result);
    }
  }

  @Nested
  class ApgSchExpPlusTest {

    @Test
    void constructorSetsFields() {
      ApgSchExpVar l = new ApgSchExpVar("s1");
      ApgSchExpVar r = new ApgSchExpVar("s2");
      ApgSchExpPlus plus = new ApgSchExpPlus(l, r);
      assertEquals(l, plus.l);
      assertEquals(r, plus.r);
    }

    @Test
    void equalsSame() {
      ApgSchExpPlus p1 = new ApgSchExpPlus(new ApgSchExpVar("a"), new ApgSchExpVar("b"));
      ApgSchExpPlus p2 = new ApgSchExpPlus(new ApgSchExpVar("a"), new ApgSchExpVar("b"));
      assertEquals(p1, p2);
      assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void toStringContainsPlus() {
      String s = new ApgSchExpPlus(new ApgSchExpVar("a"), new ApgSchExpVar("b")).toString();
      assertTrue(s.contains("+"));
    }

    @Test
    void typeThrowsOnDifferentTypesides() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts1", Unit.unit);
      typing.defs.apgts.put("ts2", Unit.unit);
      typing.defs.apgschemas.put("a", new ApgTyExpVar("ts1"));
      typing.defs.apgschemas.put("b", new ApgTyExpVar("ts2"));
      ApgSchExpPlus plus = new ApgSchExpPlus(new ApgSchExpVar("a"), new ApgSchExpVar("b"));
      assertThrows(RuntimeException.class, () -> plus.type(typing));
    }

    @Test
    void acceptDelegatesToVisitor() {
      ApgSchExpPlus plus = new ApgSchExpPlus(new ApgSchExpVar("a"), new ApgSchExpVar("b"));
      @SuppressWarnings("unchecked")
      ApgSchExp.ApgSchExpVisitor<String, Void> visitor = mock(ApgSchExp.ApgSchExpVisitor.class);
      plus.accept(null, visitor);
      verify(visitor).visit(null, plus);
    }
  }

  @Test
  void optionsReturnsEmptyMap() {
    assertTrue(new ApgSchExpVar("x").options().isEmpty());
  }
}
