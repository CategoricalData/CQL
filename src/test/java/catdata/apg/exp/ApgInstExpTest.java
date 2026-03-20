package catdata.apg.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import catdata.Pair;
import catdata.Unit;
import catdata.apg.exp.ApgInstExp.ApgInstExpCoEqualize;
import catdata.apg.exp.ApgInstExp.ApgInstExpDelta;
import catdata.apg.exp.ApgInstExp.ApgInstExpEqualize;
import catdata.apg.exp.ApgInstExp.ApgInstExpInitial;
import catdata.apg.exp.ApgInstExp.ApgInstExpPlus;
import catdata.apg.exp.ApgInstExp.ApgInstExpTerminal;
import catdata.apg.exp.ApgInstExp.ApgInstExpTimes;
import catdata.apg.exp.ApgInstExp.ApgInstExpVar;
import catdata.apg.exp.ApgMapExp.ApgMapExpVar;
import catdata.apg.exp.ApgSchExp.ApgSchExpVar;
import catdata.apg.exp.ApgTransExp.ApgTransExpVar;
import catdata.apg.exp.ApgTyExp.ApgTyExpVar;
import catdata.cql.Kind;
import catdata.cql.exp.AqlTyping;
import java.util.Collection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgInstExpTest {

  @Nested
  class ApgInstExpVarTest {

    @Test
    void constructorSetsVar() {
      assertEquals("i1", new ApgInstExpVar("i1").var);
    }

    @Test
    void kindReturnsApgInstance() {
      assertEquals(Kind.APG_instance, new ApgInstExpVar("i").kind());
    }

    @Test
    void isVarReturnsTrue() {
      assertTrue(new ApgInstExpVar("i").isVar());
    }

    @Test
    void toStringReturnsVar() {
      assertEquals("myInst", new ApgInstExpVar("myInst").toString());
    }

    @Test
    void depsReturnsSingleton() {
      Collection<Pair<String, Kind>> deps = new ApgInstExpVar("i1").deps();
      assertEquals(1, deps.size());
      Pair<String, Kind> dep = deps.iterator().next();
      assertEquals("i1", dep.first);
      assertEquals(Kind.APG_instance, dep.second);
    }

    @Test
    void equalsSameVar() {
      ApgInstExpVar v1 = new ApgInstExpVar("i");
      ApgInstExpVar v2 = new ApgInstExpVar("i");
      assertEquals(v1, v2);
      assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void notEqualsDifferentVar() {
      assertNotEquals(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
    }

    @Test
    void equalsReflexive() {
      ApgInstExpVar v = new ApgInstExpVar("i");
      assertEquals(v, v);
    }

    @Test
    void notEqualsNull() {
      assertNotEquals(null, new ApgInstExpVar("i"));
    }

    @Test
    void typeThrowsWhenUndefined() {
      AqlTyping typing = new AqlTyping();
      assertThrows(RuntimeException.class, () -> new ApgInstExpVar("missing").type(typing));
    }

    @Test
    void typeReturnsSchExpWhenDefined() {
      AqlTyping typing = new AqlTyping();
      ApgSchExpVar schExp = new ApgSchExpVar("s1");
      typing.defs.apgis.put("i1", schExp);
      assertEquals(schExp, new ApgInstExpVar("i1").type(typing));
    }

    @Test
    void acceptDelegatesToVisitor() throws Exception {
      ApgInstExpVar v = new ApgInstExpVar("i");
      @SuppressWarnings("unchecked")
      ApgInstExp.ApgInstExpVisitor<String, Void, RuntimeException> visitor =
          mock(ApgInstExp.ApgInstExpVisitor.class);
      v.accept(null, visitor);
      verify(visitor).visit(null, v);
    }

    @Test
    void varMethodCreatesNew() {
      var newVar = new ApgInstExpVar("i1").Var("i2");
      assertTrue(newVar instanceof ApgInstExpVar);
      assertEquals("i2", ((ApgInstExpVar) newVar).var);
    }
  }

  @Nested
  class ApgInstExpInitialTest {

    @Test
    void constructorSetsTypeside() {
      ApgSchExpVar sch = new ApgSchExpVar("s");
      ApgInstExpInitial init = new ApgInstExpInitial(sch);
      assertEquals(sch, init.typeside);
    }

    @Test
    void equalsSame() {
      ApgInstExpInitial i1 = new ApgInstExpInitial(new ApgSchExpVar("s"));
      ApgInstExpInitial i2 = new ApgInstExpInitial(new ApgSchExpVar("s"));
      assertEquals(i1, i2);
      assertEquals(i1.hashCode(), i2.hashCode());
    }

    @Test
    void notEqualsDifferent() {
      assertNotEquals(
          new ApgInstExpInitial(new ApgSchExpVar("a")),
          new ApgInstExpInitial(new ApgSchExpVar("b")));
    }

    @Test
    void toStringContainsEmpty() {
      assertTrue(new ApgInstExpInitial(new ApgSchExpVar("s")).toString().contains("empty"));
    }

    @Test
    void typeReturnsTypeside() {
      AqlTyping typing = new AqlTyping();
      ApgTyExpVar tyExp = new ApgTyExpVar("ts");
      typing.defs.apgschemas.put("s", tyExp);
      ApgSchExpVar sch = new ApgSchExpVar("s");
      assertEquals(sch, new ApgInstExpInitial(sch).type(typing));
    }

    @Test
    void acceptDelegatesToVisitor() throws Exception {
      ApgInstExpInitial init = new ApgInstExpInitial(new ApgSchExpVar("s"));
      @SuppressWarnings("unchecked")
      ApgInstExp.ApgInstExpVisitor<String, Void, RuntimeException> visitor =
          mock(ApgInstExp.ApgInstExpVisitor.class);
      init.accept(null, visitor);
      verify(visitor).visit(null, init);
    }
  }

  @Nested
  class ApgInstExpTerminalTest {

    @Test
    void constructorSetsTypeside() {
      ApgTyExpVar ts = new ApgTyExpVar("ts");
      ApgInstExpTerminal term = new ApgInstExpTerminal(ts);
      assertEquals(ts, term.typeside);
    }

    @Test
    void equalsSame() {
      ApgInstExpTerminal t1 = new ApgInstExpTerminal(new ApgTyExpVar("ts"));
      ApgInstExpTerminal t2 = new ApgInstExpTerminal(new ApgTyExpVar("ts"));
      assertEquals(t1, t2);
    }

    @Test
    void toStringContainsUnit() {
      assertTrue(new ApgInstExpTerminal(new ApgTyExpVar("ts")).toString().contains("unit"));
    }

    @Test
    void typeReturnsSchExpTerminal() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts", Unit.unit);
      ApgTyExpVar ts = new ApgTyExpVar("ts");
      ApgSchExp result = new ApgInstExpTerminal(ts).type(typing);
      assertTrue(result instanceof ApgSchExp.ApgSchExpTerminal);
    }
  }

  @Nested
  class ApgInstExpTimesTest {

    @Test
    void constructorSetsFields() {
      ApgInstExpVar l = new ApgInstExpVar("i1");
      ApgInstExpVar r = new ApgInstExpVar("i2");
      ApgInstExpTimes times = new ApgInstExpTimes(l, r);
      assertEquals(l, times.l);
      assertEquals(r, times.r);
    }

    @Test
    void equalsSame() {
      ApgInstExpTimes t1 = new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      ApgInstExpTimes t2 = new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      assertEquals(t1, t2);
      assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void notEqualsSwapped() {
      assertNotEquals(
          new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b")),
          new ApgInstExpTimes(new ApgInstExpVar("b"), new ApgInstExpVar("a")));
    }

    @Test
    void toStringContainsStar() {
      assertTrue(
          new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b"))
              .toString()
              .contains("*"));
    }

    @Test
    void depsUnionsBoth() {
      ApgInstExpTimes times =
          new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      assertEquals(2, times.deps().size());
    }

    @Test
    void typeThrowsOnDifferentTypesides() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts1", Unit.unit);
      typing.defs.apgts.put("ts2", Unit.unit);
      typing.defs.apgschemas.put("s1", new ApgTyExpVar("ts1"));
      typing.defs.apgschemas.put("s2", new ApgTyExpVar("ts2"));
      typing.defs.apgis.put("a", new ApgSchExpVar("s1"));
      typing.defs.apgis.put("b", new ApgSchExpVar("s2"));
      ApgInstExpTimes times = new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      assertThrows(RuntimeException.class, () -> times.type(typing));
    }
  }

  @Nested
  class ApgInstExpPlusTest {

    @Test
    void constructorSetsFields() {
      ApgInstExpVar l = new ApgInstExpVar("i1");
      ApgInstExpVar r = new ApgInstExpVar("i2");
      ApgInstExpPlus plus = new ApgInstExpPlus(l, r);
      assertEquals(l, plus.l);
      assertEquals(r, plus.r);
    }

    @Test
    void equalsSame() {
      ApgInstExpPlus p1 = new ApgInstExpPlus(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      ApgInstExpPlus p2 = new ApgInstExpPlus(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      assertEquals(p1, p2);
      assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void toStringContainsPlus() {
      assertTrue(
          new ApgInstExpPlus(new ApgInstExpVar("a"), new ApgInstExpVar("b"))
              .toString()
              .contains("+"));
    }

    @Test
    void acceptDelegatesToVisitor() throws Exception {
      ApgInstExpPlus plus = new ApgInstExpPlus(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      @SuppressWarnings("unchecked")
      ApgInstExp.ApgInstExpVisitor<String, Void, RuntimeException> visitor =
          mock(ApgInstExp.ApgInstExpVisitor.class);
      plus.accept(null, visitor);
      verify(visitor).visit(null, plus);
    }
  }

  @Nested
  class ApgInstExpEqualizeTest {

    @Test
    void constructorSetsFields() {
      ApgTransExpVar l = new ApgTransExpVar("t1");
      ApgTransExpVar r = new ApgTransExpVar("t2");
      ApgInstExpEqualize eq = new ApgInstExpEqualize(l, r);
      assertEquals(l, eq.l);
      assertEquals(r, eq.r);
    }

    @Test
    void equalsSame() {
      ApgInstExpEqualize e1 =
          new ApgInstExpEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"));
      ApgInstExpEqualize e2 =
          new ApgInstExpEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"));
      assertEquals(e1, e2);
      assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    void toStringContainsEqualize() {
      assertTrue(
          new ApgInstExpEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"))
              .toString()
              .contains("equalize"));
    }

    @Test
    void depsUnionsBoth() {
      ApgInstExpEqualize eq =
          new ApgInstExpEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"));
      assertEquals(2, eq.deps().size());
    }
  }

  @Nested
  class ApgInstExpCoEqualizeTest {

    @Test
    void constructorSetsFields() {
      ApgTransExpVar l = new ApgTransExpVar("t1");
      ApgTransExpVar r = new ApgTransExpVar("t2");
      ApgInstExpCoEqualize ceq = new ApgInstExpCoEqualize(l, r);
      assertEquals(l, ceq.l);
      assertEquals(r, ceq.r);
    }

    @Test
    void equalsSame() {
      ApgInstExpCoEqualize c1 =
          new ApgInstExpCoEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"));
      ApgInstExpCoEqualize c2 =
          new ApgInstExpCoEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"));
      assertEquals(c1, c2);
    }

    @Test
    void toStringContainsCoequalize() {
      assertTrue(
          new ApgInstExpCoEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"))
              .toString()
              .contains("coequalize"));
    }
  }

  @Nested
  class ApgInstExpDeltaTest {

    @Test
    void constructorSetsFields() {
      ApgMapExpVar f = new ApgMapExpVar("m");
      ApgInstExpVar j = new ApgInstExpVar("i");
      ApgInstExpDelta delta = new ApgInstExpDelta(f, j);
      assertEquals(f, delta.F);
      assertEquals(j, delta.J);
    }

    @Test
    void equalsSame() {
      ApgInstExpDelta d1 = new ApgInstExpDelta(new ApgMapExpVar("m"), new ApgInstExpVar("i"));
      ApgInstExpDelta d2 = new ApgInstExpDelta(new ApgMapExpVar("m"), new ApgInstExpVar("i"));
      assertEquals(d1, d2);
      assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    void toStringContainsDelta() {
      assertTrue(
          new ApgInstExpDelta(new ApgMapExpVar("m"), new ApgInstExpVar("i"))
              .toString()
              .contains("delta"));
    }

    @Test
    void depsUnionsBoth() {
      ApgInstExpDelta delta = new ApgInstExpDelta(new ApgMapExpVar("m"), new ApgInstExpVar("i"));
      assertEquals(2, delta.deps().size());
    }
  }

  @Test
  void optionsReturnsEmptyMap() {
    assertTrue(new ApgInstExpVar("x").options().isEmpty());
  }
}
