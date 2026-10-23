package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Hash-consed lists. The cache is static, so every test uses fresh element values, and lists built
 * here stay in it for the rest of the test JVM.
 */
class ConsListTest {

  private static List<String> fresh(int n) {
    String p = UUID.randomUUID().toString();
    List<String> l = new ArrayList<>();
    for (int k = 0; k < n; k++) {
      l.add(p + k);
    }
    return l;
  }

  @Test
  void buildsOneConsListPerContent() {
    List<String> l = fresh(2);
    ConsList<String> a = ConsList.new0(l, true);
    assertSame(a, ConsList.new0(new ArrayList<>(l), true));
    assertNotSame(a, ConsList.new0(fresh(2), true));
    assertEquals(System.identityHashCode(a), a.hashCode());
    assertTrue(a.equals(a));
    assertNotEquals(a, ConsList.new0(fresh(2), true));
  }

  @Test
  void copiesTheListOnlyWhenAsked() {
    List<String> l = fresh(2);
    assertNotSame(l, ConsList.new0(l, true).list);
    List<String> m = fresh(2);
    assertSame(m, ConsList.new0(m, false).list);
  }

  /** Without a copy the cached list is the caller's, and the size is fixed at construction. */
  @Test
  void anUncopiedListFollowsTheCaller() {
    List<String> m = fresh(1);
    ConsList<String> c = ConsList.new0(m, false);
    m.add("more");
    assertEquals(2, c.list.size());
    assertEquals(1, c.size());
  }

  @Test
  void concatenatesTwoOrThreeLists() {
    List<String> a = fresh(1);
    List<String> b = fresh(2);
    List<String> c = fresh(1);
    List<String> ab = new ArrayList<>(a);
    ab.addAll(b);
    assertEquals(ab, ConsList.new0(a, b).list);
    assertSame(ConsList.new0(a, b), ConsList.new0(ConsList.new0(a, true), ConsList.new0(b, true)));
    List<String> abc = new ArrayList<>(ab);
    abc.addAll(c);
    assertEquals(abc, ConsList.new0(a, b, c).list);
    assertSame(
        ConsList.new0(a, b, c),
        ConsList.new0(ConsList.new0(a, true), ConsList.new0(b, true), ConsList.new0(c, true)));
  }

  @Test
  void readsLikeAList() {
    List<String> l = fresh(3);
    l.add(l.get(0));
    ConsList<String> c = ConsList.new0(l, true);
    assertEquals(4, c.size());
    assertFalse(c.isEmpty());
    assertTrue(ConsList.new0(new ArrayList<String>(), true).isEmpty());
    assertEquals(l.get(1), c.get(1));
    assertEquals(0, c.indexOf(l.get(0)));
    assertEquals(3, c.lastIndexOf(l.get(0)));
    Iterator<String> it = c.iterator();
    assertEquals(l.get(0), it.next());
    assertEquals(l.subList(1, 3), c.subList(1, 3, true).list);
    assertEquals(l.toString(), c.toString());
  }
}
