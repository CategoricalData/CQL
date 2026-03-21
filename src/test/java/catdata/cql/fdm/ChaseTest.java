package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ChaseTest {

  @Nested
  class BSTTests {

    @Test
    void constructorSetsNode() {
      Chase.BST bst = new Chase.BST(42);
      assertEquals(42, bst.node);
    }

    @Test
    void addReturnsFalseForSameNode() {
      Chase.BST bst = new Chase.BST(5);
      assertFalse(bst.add(5));
    }

    @Test
    void addReturnsTrueForNewNode() {
      Chase.BST bst = new Chase.BST(5);
      assertTrue(bst.add(10));
    }

    @Test
    void addReturnsFalseForDuplicate() {
      Chase.BST bst = new Chase.BST(5);
      assertTrue(bst.add(10));
      assertFalse(bst.add(10));
    }

    @Test
    void foreachVisitsAllNodes() {
      Chase.BST bst = new Chase.BST(1);
      bst.add(2);
      bst.add(3);
      List<Integer> visited = new ArrayList<>();
      bst.foreach(visited::add);
      assertTrue(visited.contains(1));
      assertTrue(visited.contains(2));
      assertTrue(visited.contains(3));
      assertEquals(3, visited.size());
    }

    @Test
    void foreachNoRootSkipsRoot() {
      Chase.BST bst = new Chase.BST(1);
      bst.add(2);
      bst.add(3);
      List<Integer> visited = new ArrayList<>();
      bst.foreachNoRoot(visited::add);
      assertFalse(visited.contains(1));
      assertTrue(visited.contains(2));
      assertTrue(visited.contains(3));
    }

    @Test
    void foreachNoRootEmptySetDoesNothing() {
      Chase.BST bst = new Chase.BST(1);
      List<Integer> visited = new ArrayList<>();
      bst.foreachNoRoot(visited::add);
      assertTrue(visited.isEmpty());
    }

    @Test
    void foreachOnlyRootVisitsRoot() {
      Chase.BST bst = new Chase.BST(7);
      List<Integer> visited = new ArrayList<>();
      bst.foreach(visited::add);
      assertEquals(List.of(7), visited);
    }

    @Test
    void equalBstsAreEqual() {
      Chase.BST a = new Chase.BST(1);
      a.add(2);
      Chase.BST b = new Chase.BST(1);
      b.add(2);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentNodeNotEqual() {
      Chase.BST a = new Chase.BST(1);
      Chase.BST b = new Chase.BST(2);
      assertNotEquals(a, b);
    }

    @Test
    void differentSetNotEqual() {
      Chase.BST a = new Chase.BST(1);
      a.add(2);
      Chase.BST b = new Chase.BST(1);
      b.add(3);
      assertNotEquals(a, b);
    }

    @Test
    void nullSetVsNonNullNotEqual() {
      Chase.BST a = new Chase.BST(1);
      Chase.BST b = new Chase.BST(1);
      b.add(2);
      assertNotEquals(a, b);
    }

    @Test
    void equalToSelf() {
      Chase.BST bst = new Chase.BST(1);
      assertEquals(bst, bst);
    }

    @Test
    void notEqualToNull() {
      assertNotEquals(null, new Chase.BST(1));
    }

    @Test
    void notEqualToDifferentType() {
      assertFalse(new Chase.BST(1).equals("not a BST"));
    }

    @Test
    void toStringWithoutSet() {
      assertEquals("{1}", new Chase.BST(1).toString());
    }

    @Test
    void toStringWithSet() {
      Chase.BST bst = new Chase.BST(1);
      bst.add(2);
      String str = bst.toString();
      assertTrue(str.startsWith("{1,"));
      assertTrue(str.contains("2"));
      assertTrue(str.endsWith("}"));
    }
  }
}
