package catdata.apg.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import catdata.Pair;
import catdata.Unit;
import catdata.apg.exp.ApgMapExp.ApgMapExpCompose;
import catdata.apg.exp.ApgMapExp.ApgMapExpVar;
import catdata.apg.exp.ApgSchExp.ApgSchExpVar;
import catdata.apg.exp.ApgTyExp.ApgTyExpVar;
import catdata.cql.Kind;
import catdata.cql.exp.AqlTyping;
import java.util.Collection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgMapExpTest {

  @Nested
  class ApgMapExpVarTest {

    @Test
    void constructorSetsVar() {
      assertEquals("m1", new ApgMapExpVar("m1").var);
    }

    @Test
    void kindReturnsApgMapping() {
      assertEquals(Kind.APG_mapping, new ApgMapExpVar("m").kind());
    }

    @Test
    void isVarReturnsTrue() {
      assertTrue(new ApgMapExpVar("m").isVar());
    }

    @Test
    void toStringReturnsVar() {
      assertEquals("myMap", new ApgMapExpVar("myMap").toString());
    }

    @Test
    void depsReturnsSingleton() {
      Collection<Pair<String, Kind>> deps = new ApgMapExpVar("m1").deps();
      assertEquals(1, deps.size());
      Pair<String, Kind> dep = deps.iterator().next();
      assertEquals("m1", dep.first);
      assertEquals(Kind.APG_instance, dep.second);
    }

    @Test
    void equalsSameVar() {
      ApgMapExpVar v1 = new ApgMapExpVar("m");
      ApgMapExpVar v2 = new ApgMapExpVar("m");
      assertEquals(v1, v2);
      assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void notEqualsDifferentVar() {
      assertNotEquals(new ApgMapExpVar("a"), new ApgMapExpVar("b"));
    }

    @Test
    void equalsReflexive() {
      ApgMapExpVar v = new ApgMapExpVar("m");
      assertEquals(v, v);
    }

    @Test
    void notEqualsNull() {
      assertNotEquals(null, new ApgMapExpVar("m"));
    }

    @Test
    void notEqualsDifferentType() {
      assertNotEquals("m", new ApgMapExpVar("m"));
    }

    @Test
    void typeThrowsWhenUndefined() {
      AqlTyping typing = new AqlTyping();
      assertThrows(RuntimeException.class, () -> new ApgMapExpVar("missing").type(typing));
    }

    @Test
    void typeReturnsPairWhenDefined() {
      AqlTyping typing = new AqlTyping();
      Pair<ApgSchExp, ApgSchExp> pair =
          new Pair<>(new ApgSchExpVar("s1"), new ApgSchExpVar("s2"));
      typing.defs.apgmappings.put("m1", pair);
      assertEquals(pair, new ApgMapExpVar("m1").type(typing));
    }

    @Test
    void acceptDelegatesToVisitor() {
      ApgMapExpVar v = new ApgMapExpVar("m");
      @SuppressWarnings("unchecked")
      ApgMapExp.ApgMapExpVisitor<String, Void> visitor = mock(ApgMapExp.ApgMapExpVisitor.class);
      v.accept(null, visitor);
      verify(visitor).visit(null, v);
    }

    @Test
    void coAcceptDelegatesToCoVisitor() {
      ApgMapExpVar v = new ApgMapExpVar("m");
      @SuppressWarnings("unchecked")
      ApgMapExp.ApgMapExpCoVisitor<String, Void> coVisitor =
          mock(ApgMapExp.ApgMapExpCoVisitor.class);
      v.coaccept(null, coVisitor, "r");
      verify(coVisitor).visitApgMapExpVar(null, "r");
    }

    @Test
    void varMethodCreatesNew() {
      var newVar = new ApgMapExpVar("m1").Var("m2");
      assertTrue(newVar instanceof ApgMapExpVar);
      assertEquals("m2", ((ApgMapExpVar) newVar).var);
    }
  }

  @Nested
  class ApgMapExpComposeTest {

    @Test
    void constructorSetsFields() {
      ApgMapExpVar h1 = new ApgMapExpVar("m1");
      ApgMapExpVar h2 = new ApgMapExpVar("m2");
      ApgMapExpCompose compose = new ApgMapExpCompose(h1, h2);
      assertEquals(h1, compose.h1);
      assertEquals(h2, compose.h2);
    }

    @Test
    void equalsSame() {
      ApgMapExpCompose c1 = new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b"));
      ApgMapExpCompose c2 = new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b"));
      assertEquals(c1, c2);
      assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void notEqualsSwapped() {
      assertNotEquals(
          new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b")),
          new ApgMapExpCompose(new ApgMapExpVar("b"), new ApgMapExpVar("a")));
    }

    @Test
    void toStringContainsSemicolon() {
      String s = new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b")).toString();
      assertTrue(s.contains(";"));
    }

    @Test
    void depsUnionsBoth() {
      ApgMapExpCompose compose =
          new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b"));
      assertEquals(2, compose.deps().size());
    }

    @Test
    void acceptReturnsNull() {
      // ApgMapExpCompose.accept returns null (commented-out delegation)
      ApgMapExpCompose compose =
          new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b"));
      @SuppressWarnings("unchecked")
      ApgMapExp.ApgMapExpVisitor<String, Void> visitor = mock(ApgMapExp.ApgMapExpVisitor.class);
      assertNull(compose.accept(null, visitor));
    }

    @Test
    void typeThrowsOnIntermediateMismatch() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts", Unit.unit);
      typing.defs.apgschemas.put("s1", new ApgTyExpVar("ts"));
      typing.defs.apgschemas.put("s2", new ApgTyExpVar("ts"));
      typing.defs.apgschemas.put("s3", new ApgTyExpVar("ts"));
      typing.defs.apgmappings.put(
          "m1", new Pair<>(new ApgSchExpVar("s1"), new ApgSchExpVar("s2")));
      typing.defs.apgmappings.put(
          "m2", new Pair<>(new ApgSchExpVar("s3"), new ApgSchExpVar("s1")));
      ApgMapExpCompose compose = new ApgMapExpCompose(new ApgMapExpVar("m1"), new ApgMapExpVar("m2"));
      assertThrows(RuntimeException.class, () -> compose.type(typing));
    }

    @Test
    void typeSucceedsOnMatchingIntermediate() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts", Unit.unit);
      typing.defs.apgschemas.put("s1", new ApgTyExpVar("ts"));
      typing.defs.apgschemas.put("s2", new ApgTyExpVar("ts"));
      typing.defs.apgschemas.put("s3", new ApgTyExpVar("ts"));
      typing.defs.apgmappings.put(
          "m1", new Pair<>(new ApgSchExpVar("s1"), new ApgSchExpVar("s2")));
      typing.defs.apgmappings.put(
          "m2", new Pair<>(new ApgSchExpVar("s2"), new ApgSchExpVar("s3")));
      ApgMapExpCompose compose = new ApgMapExpCompose(new ApgMapExpVar("m1"), new ApgMapExpVar("m2"));
      Pair<ApgSchExp, ApgSchExp> result = compose.type(typing);
      assertEquals(new ApgSchExpVar("s1"), result.first);
      assertEquals(new ApgSchExpVar("s3"), result.second);
    }
  }

  @Test
  void optionsReturnsEmptyMap() {
    assertTrue(new ApgMapExpVar("x").options().isEmpty());
  }
}
