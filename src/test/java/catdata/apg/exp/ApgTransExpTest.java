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
import catdata.apg.exp.ApgInstExp.ApgInstExpEqualize;
import catdata.apg.exp.ApgInstExp.ApgInstExpPlus;
import catdata.apg.exp.ApgInstExp.ApgInstExpTimes;
import catdata.apg.exp.ApgInstExp.ApgInstExpVar;
import catdata.apg.exp.ApgMapExp.ApgMapExpVar;
import catdata.apg.exp.ApgSchExp.ApgSchExpVar;
import catdata.apg.exp.ApgTransExp.ApgTransExpCase;
import catdata.apg.exp.ApgTransExp.ApgTransExpCoEqualize;
import catdata.apg.exp.ApgTransExp.ApgTransExpCoEqualizeU;
import catdata.apg.exp.ApgTransExp.ApgTransExpCompose;
import catdata.apg.exp.ApgTransExp.ApgTransExpDelta;
import catdata.apg.exp.ApgTransExp.ApgTransExpEqualize;
import catdata.apg.exp.ApgTransExp.ApgTransExpEqualizeU;
import catdata.apg.exp.ApgTransExp.ApgTransExpFst;
import catdata.apg.exp.ApgTransExp.ApgTransExpId;
import catdata.apg.exp.ApgTransExp.ApgTransExpInitial;
import catdata.apg.exp.ApgTransExp.ApgTransExpInl;
import catdata.apg.exp.ApgTransExp.ApgTransExpInr;
import catdata.apg.exp.ApgTransExp.ApgTransExpPair;
import catdata.apg.exp.ApgTransExp.ApgTransExpSnd;
import catdata.apg.exp.ApgTransExp.ApgTransExpTerminal;
import catdata.apg.exp.ApgTransExp.ApgTransExpVar;
import catdata.apg.exp.ApgTyExp.ApgTyExpVar;
import catdata.cql.Kind;
import catdata.cql.exp.AqlTyping;
import java.util.Collection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgTransExpTest {

  private AqlTyping setupTypingWithInstances(String... instNames) {
    AqlTyping typing = new AqlTyping();
    typing.defs.apgts.put("ts", Unit.unit);
    typing.defs.apgschemas.put("s", new ApgTyExpVar("ts"));
    for (String name : instNames) {
      typing.defs.apgis.put(name, new ApgSchExpVar("s"));
    }
    return typing;
  }

  private AqlTyping setupTypingWithTransforms(
      String t1, String srcI1, String dstI1, String t2, String srcI2, String dstI2) {
    AqlTyping typing = new AqlTyping();
    typing.defs.apgts.put("ts", Unit.unit);
    typing.defs.apgschemas.put("s", new ApgTyExpVar("ts"));
    typing.defs.apgis.put(srcI1, new ApgSchExpVar("s"));
    typing.defs.apgis.put(dstI1, new ApgSchExpVar("s"));
    typing.defs.apgis.put(srcI2, new ApgSchExpVar("s"));
    typing.defs.apgis.put(dstI2, new ApgSchExpVar("s"));
    typing.defs.apgms.put(t1, new Pair<>(new ApgInstExpVar(srcI1), new ApgInstExpVar(dstI1)));
    typing.defs.apgms.put(t2, new Pair<>(new ApgInstExpVar(srcI2), new ApgInstExpVar(dstI2)));
    return typing;
  }

  @Nested
  class ApgTransExpVarTest {

    @Test
    void constructorSetsVar() {
      assertEquals("t1", new ApgTransExpVar("t1").var);
    }

    @Test
    void kindReturnsApgMorphism() {
      assertEquals(Kind.APG_morphism, new ApgTransExpVar("t").kind());
    }

    @Test
    void isVarReturnsTrue() {
      assertTrue(new ApgTransExpVar("t").isVar());
    }

    @Test
    void toStringReturnsVar() {
      assertEquals("myTrans", new ApgTransExpVar("myTrans").toString());
    }

    @Test
    void depsReturnsSingleton() {
      Collection<Pair<String, Kind>> deps = new ApgTransExpVar("t1").deps();
      assertEquals(1, deps.size());
      assertEquals(Kind.APG_morphism, deps.iterator().next().second);
    }

    @Test
    void equalsSameVar() {
      assertEquals(new ApgTransExpVar("t"), new ApgTransExpVar("t"));
      assertEquals(new ApgTransExpVar("t").hashCode(), new ApgTransExpVar("t").hashCode());
    }

    @Test
    void notEqualsDifferentVar() {
      assertNotEquals(new ApgTransExpVar("a"), new ApgTransExpVar("b"));
    }

    @Test
    void typeThrowsWhenUndefined() {
      AqlTyping typing = new AqlTyping();
      assertThrows(RuntimeException.class, () -> new ApgTransExpVar("missing").type(typing));
    }

    @Test
    void typeReturnsPairWhenDefined() {
      AqlTyping typing = new AqlTyping();
      Pair<ApgInstExp, ApgInstExp> pair =
          new Pair<>(new ApgInstExpVar("i1"), new ApgInstExpVar("i2"));
      typing.defs.apgms.put("t1", pair);
      assertEquals(pair, new ApgTransExpVar("t1").type(typing));
    }

    @Test
    void acceptDelegatesToVisitor() {
      ApgTransExpVar v = new ApgTransExpVar("t");
      @SuppressWarnings("unchecked")
      ApgTransExp.ApgTransExpVisitor<String, Void> visitor =
          mock(ApgTransExp.ApgTransExpVisitor.class);
      v.accept(null, visitor);
      verify(visitor).visit(null, v);
    }

    @Test
    void varMethodCreatesNew() {
      var newVar = new ApgTransExpVar("t1").Var("t2");
      assertTrue(newVar instanceof ApgTransExpVar);
    }
  }

  @Nested
  class ApgTransExpIdTest {

    @Test
    void constructorSetsField() {
      ApgInstExpVar g = new ApgInstExpVar("i");
      assertEquals(g, new ApgTransExpId(g).G);
    }

    @Test
    void equalsSame() {
      assertEquals(
          new ApgTransExpId(new ApgInstExpVar("i")), new ApgTransExpId(new ApgInstExpVar("i")));
    }

    @Test
    void toStringContainsIdentity() {
      assertTrue(new ApgTransExpId(new ApgInstExpVar("i")).toString().contains("identity"));
    }

    @Test
    void typeReturnsSameDomainAndCodomain() {
      AqlTyping typing = setupTypingWithInstances("i");
      Pair<ApgInstExp, ApgInstExp> result = new ApgTransExpId(new ApgInstExpVar("i")).type(typing);
      assertEquals(result.first, result.second);
    }
  }

  @Nested
  class ApgTransExpTerminalTest {

    @Test
    void constructorSetsField() {
      ApgInstExpVar g = new ApgInstExpVar("i");
      assertEquals(g, new ApgTransExpTerminal(g).G);
    }

    @Test
    void equalsSame() {
      assertEquals(
          new ApgTransExpTerminal(new ApgInstExpVar("i")),
          new ApgTransExpTerminal(new ApgInstExpVar("i")));
    }

    @Test
    void toStringContainsUnit() {
      assertTrue(new ApgTransExpTerminal(new ApgInstExpVar("i")).toString().contains("unit"));
    }

    @Test
    void acceptDelegatesToVisitor() {
      ApgTransExpTerminal term = new ApgTransExpTerminal(new ApgInstExpVar("i"));
      @SuppressWarnings("unchecked")
      ApgTransExp.ApgTransExpVisitor<String, Void> visitor =
          mock(ApgTransExp.ApgTransExpVisitor.class);
      term.accept(null, visitor);
      verify(visitor).visit(null, term);
    }
  }

  @Nested
  class ApgTransExpInitialTest {

    @Test
    void constructorSetsField() {
      ApgInstExpVar g = new ApgInstExpVar("i");
      assertEquals(g, new ApgTransExpInitial(g).G);
    }

    @Test
    void equalsSame() {
      assertEquals(
          new ApgTransExpInitial(new ApgInstExpVar("i")),
          new ApgTransExpInitial(new ApgInstExpVar("i")));
    }

    @Test
    void toStringContainsEmpty() {
      assertTrue(new ApgTransExpInitial(new ApgInstExpVar("i")).toString().contains("empty"));
    }
  }

  @Nested
  class ApgTransExpFstTest {

    @Test
    void constructorSetsFields() {
      ApgInstExpVar g1 = new ApgInstExpVar("i1");
      ApgInstExpVar g2 = new ApgInstExpVar("i2");
      ApgTransExpFst fst = new ApgTransExpFst(g1, g2);
      assertEquals(g1, fst.G1);
      assertEquals(g2, fst.G2);
    }

    @Test
    void equalsSame() {
      assertEquals(
          new ApgTransExpFst(new ApgInstExpVar("a"), new ApgInstExpVar("b")),
          new ApgTransExpFst(new ApgInstExpVar("a"), new ApgInstExpVar("b")));
    }

    @Test
    void toStringContainsFst() {
      assertTrue(
          new ApgTransExpFst(new ApgInstExpVar("a"), new ApgInstExpVar("b"))
              .toString()
              .contains("fst"));
    }

    @Test
    void typeReturnsTimes() {
      AqlTyping typing = setupTypingWithInstances("a", "b");
      Pair<ApgInstExp, ApgInstExp> result =
          new ApgTransExpFst(new ApgInstExpVar("a"), new ApgInstExpVar("b")).type(typing);
      assertTrue(result.first instanceof ApgInstExpTimes);
      assertEquals(new ApgInstExpVar("a"), result.second);
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
      assertThrows(
          RuntimeException.class,
          () ->
              new ApgTransExpFst(new ApgInstExpVar("a"), new ApgInstExpVar("b")).type(typing));
    }
  }

  @Nested
  class ApgTransExpSndTest {

    @Test
    void constructorSetsFields() {
      ApgInstExpVar g1 = new ApgInstExpVar("i1");
      ApgInstExpVar g2 = new ApgInstExpVar("i2");
      ApgTransExpSnd snd = new ApgTransExpSnd(g1, g2);
      assertEquals(g1, snd.G1);
      assertEquals(g2, snd.G2);
    }

    @Test
    void typeReturnsSndProjection() {
      AqlTyping typing = setupTypingWithInstances("a", "b");
      Pair<ApgInstExp, ApgInstExp> result =
          new ApgTransExpSnd(new ApgInstExpVar("a"), new ApgInstExpVar("b")).type(typing);
      assertTrue(result.first instanceof ApgInstExpTimes);
      assertEquals(new ApgInstExpVar("b"), result.second);
    }

    @Test
    void toStringContainsSnd() {
      assertTrue(
          new ApgTransExpSnd(new ApgInstExpVar("a"), new ApgInstExpVar("b"))
              .toString()
              .contains("snd"));
    }
  }

  @Nested
  class ApgTransExpPairTest {

    @Test
    void constructorSetsFields() {
      ApgTransExpVar h1 = new ApgTransExpVar("t1");
      ApgTransExpVar h2 = new ApgTransExpVar("t2");
      ApgTransExpPair pair = new ApgTransExpPair(h1, h2);
      assertEquals(h1, pair.h1);
      assertEquals(h2, pair.h2);
    }

    @Test
    void equalsSame() {
      assertEquals(
          new ApgTransExpPair(new ApgTransExpVar("a"), new ApgTransExpVar("b")),
          new ApgTransExpPair(new ApgTransExpVar("a"), new ApgTransExpVar("b")));
    }

    @Test
    void typeThrowsOnDomainMismatch() {
      AqlTyping typing = setupTypingWithTransforms("t1", "i1", "i2", "t2", "i3", "i4");
      assertThrows(
          RuntimeException.class,
          () ->
              new ApgTransExpPair(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"))
                  .type(typing));
    }

    @Test
    void typeReturnsPairWithTimes() {
      AqlTyping typing = setupTypingWithTransforms("t1", "i1", "i2", "t2", "i1", "i3");
      Pair<ApgInstExp, ApgInstExp> result =
          new ApgTransExpPair(new ApgTransExpVar("t1"), new ApgTransExpVar("t2")).type(typing);
      assertEquals(new ApgInstExpVar("i1"), result.first);
      assertTrue(result.second instanceof ApgInstExpTimes);
    }
  }

  @Nested
  class ApgTransExpInlTest {

    @Test
    void typeReturnsInlInjection() {
      AqlTyping typing = setupTypingWithInstances("a", "b");
      Pair<ApgInstExp, ApgInstExp> result =
          new ApgTransExpInl(new ApgInstExpVar("a"), new ApgInstExpVar("b")).type(typing);
      assertEquals(new ApgInstExpVar("a"), result.first);
      assertTrue(result.second instanceof ApgInstExpPlus);
    }

    @Test
    void toStringContainsInl() {
      assertTrue(
          new ApgTransExpInl(new ApgInstExpVar("a"), new ApgInstExpVar("b"))
              .toString()
              .contains("inl"));
    }
  }

  @Nested
  class ApgTransExpInrTest {

    @Test
    void typeReturnsInrInjection() {
      AqlTyping typing = setupTypingWithInstances("a", "b");
      Pair<ApgInstExp, ApgInstExp> result =
          new ApgTransExpInr(new ApgInstExpVar("a"), new ApgInstExpVar("b")).type(typing);
      assertEquals(new ApgInstExpVar("b"), result.first);
      assertTrue(result.second instanceof ApgInstExpPlus);
    }

    @Test
    void toStringContainsInr() {
      assertTrue(
          new ApgTransExpInr(new ApgInstExpVar("a"), new ApgInstExpVar("b"))
              .toString()
              .contains("inr"));
    }
  }

  @Nested
  class ApgTransExpCaseTest {

    @Test
    void constructorSetsFields() {
      ApgTransExpVar h1 = new ApgTransExpVar("t1");
      ApgTransExpVar h2 = new ApgTransExpVar("t2");
      ApgTransExpCase caseExp = new ApgTransExpCase(h1, h2);
      assertEquals(h1, caseExp.h1);
      assertEquals(h2, caseExp.h2);
    }

    @Test
    void typeThrowsOnCodomainMismatch() {
      AqlTyping typing = setupTypingWithTransforms("t1", "i1", "i2", "t2", "i3", "i4");
      assertThrows(
          RuntimeException.class,
          () ->
              new ApgTransExpCase(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"))
                  .type(typing));
    }

    @Test
    void typeReturnsCaseWithPlus() {
      AqlTyping typing = setupTypingWithTransforms("t1", "i1", "i3", "t2", "i2", "i3");
      Pair<ApgInstExp, ApgInstExp> result =
          new ApgTransExpCase(new ApgTransExpVar("t1"), new ApgTransExpVar("t2")).type(typing);
      assertTrue(result.first instanceof ApgInstExpPlus);
      assertEquals(new ApgInstExpVar("i3"), result.second);
    }

    @Test
    void toStringContainsPipe() {
      assertTrue(
          new ApgTransExpCase(new ApgTransExpVar("a"), new ApgTransExpVar("b"))
              .toString()
              .contains("|"));
    }
  }

  @Nested
  class ApgTransExpComposeTest {

    @Test
    void constructorSetsFields() {
      ApgTransExpVar h1 = new ApgTransExpVar("t1");
      ApgTransExpVar h2 = new ApgTransExpVar("t2");
      ApgTransExpCompose compose = new ApgTransExpCompose(h1, h2);
      assertEquals(h1, compose.h1);
      assertEquals(h2, compose.h2);
    }

    @Test
    void typeThrowsOnIntermediateMismatch() {
      AqlTyping typing = setupTypingWithTransforms("t1", "i1", "i2", "t2", "i3", "i4");
      assertThrows(
          RuntimeException.class,
          () ->
              new ApgTransExpCompose(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"))
                  .type(typing));
    }

    @Test
    void typeSucceedsOnMatchingIntermediate() {
      AqlTyping typing = setupTypingWithTransforms("t1", "i1", "i2", "t2", "i2", "i3");
      Pair<ApgInstExp, ApgInstExp> result =
          new ApgTransExpCompose(new ApgTransExpVar("t1"), new ApgTransExpVar("t2")).type(typing);
      assertEquals(new ApgInstExpVar("i1"), result.first);
      assertEquals(new ApgInstExpVar("i3"), result.second);
    }

    @Test
    void toStringContainsSemicolon() {
      assertTrue(
          new ApgTransExpCompose(new ApgTransExpVar("a"), new ApgTransExpVar("b"))
              .toString()
              .contains(";"));
    }
  }

  @Nested
  class ApgTransExpEqualizeTest {

    @Test
    void constructorSetsFields() {
      ApgTransExpVar h1 = new ApgTransExpVar("t1");
      ApgTransExpVar h2 = new ApgTransExpVar("t2");
      ApgTransExpEqualize eq = new ApgTransExpEqualize(h1, h2);
      assertEquals(h1, eq.h1);
      assertEquals(h2, eq.h2);
    }

    @Test
    void equalsSame() {
      assertEquals(
          new ApgTransExpEqualize(new ApgTransExpVar("a"), new ApgTransExpVar("b")),
          new ApgTransExpEqualize(new ApgTransExpVar("a"), new ApgTransExpVar("b")));
    }

    @Test
    void typeThrowsOnDomainMismatch() {
      AqlTyping typing = setupTypingWithTransforms("t1", "i1", "i3", "t2", "i2", "i3");
      assertThrows(
          RuntimeException.class,
          () ->
              new ApgTransExpEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"))
                  .type(typing));
    }

    @Test
    void typeReturnsEqualizeResult() {
      AqlTyping typing = setupTypingWithTransforms("t1", "i1", "i2", "t2", "i1", "i2");
      Pair<ApgInstExp, ApgInstExp> result =
          new ApgTransExpEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2")).type(typing);
      assertTrue(result.first instanceof ApgInstExpEqualize);
      assertEquals(new ApgInstExpVar("i1"), result.second);
    }

    @Test
    void toStringContainsEqualize() {
      assertTrue(
          new ApgTransExpEqualize(new ApgTransExpVar("a"), new ApgTransExpVar("b"))
              .toString()
              .contains("equalize"));
    }
  }

  @Nested
  class ApgTransExpEqualizeUTest {

    @Test
    void constructorSetsFields() {
      ApgTransExpVar h1 = new ApgTransExpVar("t1");
      ApgTransExpVar h2 = new ApgTransExpVar("t2");
      ApgTransExpVar h = new ApgTransExpVar("t3");
      ApgTransExpEqualizeU equ = new ApgTransExpEqualizeU(h1, h2, h);
      assertEquals(h1, equ.h1);
      assertEquals(h2, equ.h2);
      assertEquals(h, equ.h);
    }

    @Test
    void equalsSame() {
      assertEquals(
          new ApgTransExpEqualizeU(
              new ApgTransExpVar("a"), new ApgTransExpVar("b"), new ApgTransExpVar("c")),
          new ApgTransExpEqualizeU(
              new ApgTransExpVar("a"), new ApgTransExpVar("b"), new ApgTransExpVar("c")));
    }

    @Test
    void toStringContainsEqualizeU() {
      assertTrue(
          new ApgTransExpEqualizeU(
                  new ApgTransExpVar("a"), new ApgTransExpVar("b"), new ApgTransExpVar("c"))
              .toString()
              .contains("equalize_u"));
    }
  }

  @Nested
  class ApgTransExpCoEqualizeTest {

    @Test
    void constructorSetsFields() {
      ApgTransExpVar h1 = new ApgTransExpVar("t1");
      ApgTransExpVar h2 = new ApgTransExpVar("t2");
      ApgTransExpCoEqualize ceq = new ApgTransExpCoEqualize(h1, h2);
      assertEquals(h1, ceq.h1);
      assertEquals(h2, ceq.h2);
    }

    @Test
    void typeReturnsCoEqualizeResult() {
      AqlTyping typing = setupTypingWithTransforms("t1", "i1", "i2", "t2", "i1", "i2");
      Pair<ApgInstExp, ApgInstExp> result =
          new ApgTransExpCoEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"))
              .type(typing);
      assertEquals(new ApgInstExpVar("i2"), result.first);
      assertTrue(result.second instanceof ApgInstExpCoEqualize);
    }

    @Test
    void toStringContainsCoequalize() {
      assertTrue(
          new ApgTransExpCoEqualize(new ApgTransExpVar("a"), new ApgTransExpVar("b"))
              .toString()
              .contains("coequalize"));
    }
  }

  @Nested
  class ApgTransExpCoEqualizeUTest {

    @Test
    void constructorSetsFields() {
      ApgTransExpVar h1 = new ApgTransExpVar("t1");
      ApgTransExpVar h2 = new ApgTransExpVar("t2");
      ApgTransExpVar h = new ApgTransExpVar("t3");
      ApgTransExpCoEqualizeU cequ = new ApgTransExpCoEqualizeU(h1, h2, h);
      assertEquals(h1, cequ.h1);
      assertEquals(h2, cequ.h2);
      assertEquals(h, cequ.h);
    }

    @Test
    void toStringContainsCoequalizeU() {
      assertTrue(
          new ApgTransExpCoEqualizeU(
                  new ApgTransExpVar("a"), new ApgTransExpVar("b"), new ApgTransExpVar("c"))
              .toString()
              .contains("coequalize_u"));
    }
  }

  @Nested
  class ApgTransExpDeltaTest {

    @Test
    void constructorSetsFields() {
      ApgMapExpVar f = new ApgMapExpVar("m");
      ApgTransExpVar h = new ApgTransExpVar("t");
      ApgTransExpDelta delta = new ApgTransExpDelta(f, h);
      assertEquals(f, delta.F);
      assertEquals(h, delta.h);
    }

    @Test
    void equalsSame() {
      assertEquals(
          new ApgTransExpDelta(new ApgMapExpVar("m"), new ApgTransExpVar("t")),
          new ApgTransExpDelta(new ApgMapExpVar("m"), new ApgTransExpVar("t")));
    }

    @Test
    void toStringContainsDelta() {
      assertTrue(
          new ApgTransExpDelta(new ApgMapExpVar("m"), new ApgTransExpVar("t"))
              .toString()
              .contains("delta"));
    }

    @Test
    void depsUnionsBoth() {
      ApgTransExpDelta delta =
          new ApgTransExpDelta(new ApgMapExpVar("m"), new ApgTransExpVar("t"));
      assertEquals(2, delta.deps().size());
    }
  }

  @Test
  void optionsReturnsEmptyMap() {
    assertTrue(new ApgTransExpVar("x").options().isEmpty());
  }
}
