package catdata.apg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApgPreTermTest {

  @Nested
  class StrFactory {

    @Test
    void strFactorySetsString() {
      ApgPreTerm t = ApgPreTerm.ApgPreTermStr("hello");
      assertEquals("hello", t.str);
      assertNull(t.fields);
      assertNull(t.inj);
      assertNull(t.ty);
    }

    @Test
    void strToString() {
      ApgPreTerm t = ApgPreTerm.ApgPreTermStr("hello");
      assertEquals("hello", t.toString());
    }
  }

  @Nested
  class TupleFactory {

    @Test
    void tupleFactorySetsFields() {
      List<Pair<String, ApgPreTerm>> fields = List.of(
          new Pair<>("a", ApgPreTerm.ApgPreTermStr("x")),
          new Pair<>("b", ApgPreTerm.ApgPreTermStr("y")));
      ApgPreTerm t = ApgPreTerm.ApgPreTermTuple(fields);
      assertNotNull(t.fields);
      assertEquals(2, t.fields.size());
      assertNull(t.str);
    }

    @Test
    void tupleToStringContainsComma() {
      List<Pair<String, ApgPreTerm>> fields = List.of(
          new Pair<>("a", ApgPreTerm.ApgPreTermStr("x")),
          new Pair<>("b", ApgPreTerm.ApgPreTermStr("y")));
      ApgPreTerm t = ApgPreTerm.ApgPreTermTuple(fields);
      assertTrue(t.toString().contains(","));
    }
  }

  @Nested
  class InjFactory {

    @Test
    void injFactorySetsFields() {
      ApgPreTerm inner = ApgPreTerm.ApgPreTermStr("val");
      ApgPreTerm t = ApgPreTerm.ApgPreTermInj("tag", inner);
      assertEquals("tag", t.inj);
      assertEquals(inner, t.arg);
    }

    @Test
    void injToStringContainsAngleBrackets() {
      ApgPreTerm t = ApgPreTerm.ApgPreTermInj("tag", ApgPreTerm.ApgPreTermStr("v"));
      String s = t.toString();
      assertTrue(s.contains("<"));
      assertTrue(s.contains(">"));
    }
  }

  @Nested
  class BaseFactory {

    @Test
    void baseFactorySetsStringAndType() {
      ApgTy<Object> ty = ApgTy.ApgTyB("Int");
      ApgPreTerm t = ApgPreTerm.ApgPreTermBase("42", ty);
      assertEquals("42", t.str);
      assertEquals(ty, t.ty);
    }

    @Test
    void baseToStringContainsAtSign() {
      ApgTy<Object> ty = ApgTy.ApgTyB("Int");
      ApgPreTerm t = ApgPreTerm.ApgPreTermBase("42", ty);
      assertTrue(t.toString().contains("@"));
    }
  }

  @Nested
  class ProjFactory {

    @Test
    void projFactorySetsFields() {
      ApgPreTerm inner = ApgPreTerm.ApgPreTermStr("x");
      ApgPreTerm t = ApgPreTerm.ApgPreTermProj("name", inner);
      assertEquals("name", t.proj);
      assertEquals(inner, t.arg);
    }

    @Test
    void projToStringContainsDot() {
      ApgPreTerm t = ApgPreTerm.ApgPreTermProj("name", ApgPreTerm.ApgPreTermStr("x"));
      assertTrue(t.toString().contains("."));
    }
  }

  @Nested
  class DerefFactory {

    @Test
    void derefFactorySetsFields() {
      ApgPreTerm inner = ApgPreTerm.ApgPreTermStr("x");
      ApgPreTerm t = ApgPreTerm.ApgPreTermDeref("ref", inner);
      assertEquals("ref", t.deref);
      assertEquals(inner, t.arg);
    }

    @Test
    void derefToStringContainsBang() {
      ApgPreTerm t = ApgPreTerm.ApgPreTermDeref("ref", ApgPreTerm.ApgPreTermStr("x"));
      assertTrue(t.toString().contains("!"));
    }
  }

  @Nested
  class AppFactory {

    @Test
    void appFactorySetsFields() {
      List<ApgPreTerm> args = List.of(
          ApgPreTerm.ApgPreTermStr("a"),
          ApgPreTerm.ApgPreTermStr("b"));
      ApgPreTerm t = ApgPreTerm.ApgPreTermApp("fn", args);
      assertEquals("fn", t.head);
      assertEquals(2, t.args.size());
    }

    @Test
    void appToStringContainsParens() {
      ApgPreTerm t = ApgPreTerm.ApgPreTermApp("fn", List.of(ApgPreTerm.ApgPreTermStr("a")));
      String s = t.toString();
      assertTrue(s.contains("("));
      assertTrue(s.contains(")"));
    }
  }

  @Nested
  class Equality {

    @Test
    void equalsSameStr() {
      ApgPreTerm t1 = ApgPreTerm.ApgPreTermStr("hello");
      ApgPreTerm t2 = ApgPreTerm.ApgPreTermStr("hello");
      assertEquals(t1, t2);
    }

    @Test
    void notEqualsDifferentStr() {
      ApgPreTerm t1 = ApgPreTerm.ApgPreTermStr("hello");
      ApgPreTerm t2 = ApgPreTerm.ApgPreTermStr("world");
      assertNotEquals(t1, t2);
    }

    @Test
    void equalsReflexive() {
      ApgPreTerm t = ApgPreTerm.ApgPreTermStr("x");
      assertEquals(t, t);
    }

    @Test
    void notEqualsNull() {
      ApgPreTerm t = ApgPreTerm.ApgPreTermStr("x");
      assertNotEquals(null, t);
    }

    @Test
    void hashCodeConsistent() {
      ApgPreTerm t1 = ApgPreTerm.ApgPreTermStr("hello");
      ApgPreTerm t2 = ApgPreTerm.ApgPreTermStr("hello");
      assertEquals(t1.hashCode(), t2.hashCode());
    }
  }
}
