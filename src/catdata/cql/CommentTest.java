package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The value of {@code md { (* "..." *) }} and {@code html { (* "..." *) }} blocks. */
class CommentTest {

  @Test
  void isACommentOfNoSize() {
    Comment c = new Comment("  # Title\n", true);
    assertEquals(Kind.COMMENT, c.kind());
    assertEquals(0, c.size());
    assertEquals("# Title", c.toString());
    assertTrue(c.isMarkdown);
  }

  @Test
  void comparesTheTextAndTheFormat() {
    Comment c = new Comment("a", true);
    assertEquals(c, new Comment("a", true));
    assertEquals(c.hashCode(), new Comment("a", true).hashCode());
    assertNotEquals(c, new Comment("b", true));
    assertNotEquals(c, new Comment("a", false));
    assertNotEquals(new Comment("a", false).hashCode(), c.hashCode());
    assertTrue(c.equals(c));
    assertFalse(c.equals(null));
    assertFalse(c.equals("a"));
  }

  @Test
  void acceptsANullTextThatCannotBePrinted() {
    Comment n = new Comment(null, false);
    assertEquals(n, new Comment(null, false));
    assertEquals(n.hashCode(), new Comment(null, false).hashCode());
    assertNotEquals(n, new Comment("a", false));
    assertNotEquals(new Comment("a", false), n);
    assertThrows(NullPointerException.class, n::toString);
  }
}
