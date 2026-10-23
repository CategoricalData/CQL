package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HeadTest {

  @Test
  void buildsEachHeadOnce() {
    assertSame(Head.SymHead("f"), Head.SymHead("f"));
    assertSame(Head.ObjHead(5, "Int"), Head.mkHead(5, "Int"));
    assertSame(Head.SymHead("f"), Head.mkHead(Term.Sym("f", List.of())));
  }

  @Test
  void keepsKindsApart() {
    assertNotSame(Head.SymHead("x"), Head.GenHead("x"));
    assertFalse(Head.SymHead("x").equals2(Head.GenHead("x")));
    assertFalse(Head.SymHead("x").equals(Head.GenHead("x")));
    assertTrue(Head.SkHead("x").equals2(Head.SkHead("x")));
    assertNotSame(Head.ObjHead(5, "Int"), Head.ObjHead(5, "Str"));
  }

  @Test
  void hashesByIdentity() {
    Head<String, String, String, String, String, String, String> h = Head.FkHead("worksIn");
    assertEquals(System.identityHashCode(h), h.hashCode());
  }

  @Test
  void exposesOnlyItsOwnKind() {
    Head<String, String, String, String, String, String, String> h = Head.AttHead("name");
    assertEquals("name", h.att());
    assertNull(h.sym());
    assertNull(h.fk());
    assertNull(h.gen());
    assertNull(h.sk());
    assertNull(h.obj());
    assertNull(h.ty());
    Head<String, String, String, String, String, String, String> o = Head.ObjHead(5, "Int");
    assertEquals(5, o.obj());
    assertEquals("Int", o.ty());
    assertNull(o.att());
  }

  @Test
  void printsItsName() {
    assertEquals("name", Head.AttHead("name").toString());
    assertEquals("worksIn", Head.FkHead("worksIn").toString());
    assertEquals("plus", Head.SymHead("plus").toString());
    assertEquals("e", Head.GenHead("e").toString());
    assertEquals("y", Head.SkHead("y").toString());
    assertEquals("5@Int", Head.ObjHead(5, "Int").toString());
  }

  @Test
  void rejectsAHeadOfNoKind() {
    RuntimeException e =
        assertThrows(
            RuntimeException.class, () -> Head.mkHead(null, null, null, null, null, null, null));
    assertEquals("|| null null null null null null", e.getMessage());
  }
}
