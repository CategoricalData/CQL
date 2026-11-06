package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import catdata.cql.Instance;
import catdata.cql.Mapping;
import catdata.cql.Term;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import catdata.cql.fdm.Chase.BST;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Test
    void hashesItsNodeAndItsSet() {
      assertEquals(new Chase.BST(1).hashCode(), new Chase.BST(1).hashCode());
      Chase.BST withSet = new Chase.BST(1);
      withSet.add(2);
      assertNotEquals(new Chase.BST(1).hashCode(), withSet.hashCode());
    }

    @Test
    void comparesASetAgainstNoSet() {
      Chase.BST withSet = new Chase.BST(1);
      withSet.add(2);
      assertFalse(withSet.equals(new Chase.BST(1)));
      assertFalse(new Chase.BST(1).equals(null));
    }
  }

  /**
   * A chase of {@code sigma F I}, built as {@code sigma_chase F I} builds it, with the given pairs
   * of rows of {@code I} to merge.
   */
  @SuppressWarnings("unchecked")
  private static Chase<
          String, String, Sym, Fk, Att, String, Fk, Att, String, String, Object, Object>
      chase(AqlMultiDriver d, Map<String, Set<Pair<Object, Object>>> extra) {
    return new Chase<>(
        (Mapping<String, String, Sym, Fk, Att, String, Fk, Att>) d.env.defs.maps.get("F"),
        instance(d),
        extra);
  }

  private static Chase<
          String, String, Sym, Fk, Att, String, Fk, Att, String, String, Object, Object>
      chase(String program) throws Exception {
    return chase(run(program), Map.of());
  }

  private static AqlMultiDriver run(String program) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    return d;
  }

  @SuppressWarnings("unchecked")
  private static Instance<String, String, Sym, Fk, Att, String, String, Object, Object> instance(
      AqlMultiDriver d) {
    return (Instance<String, String, Sym, Fk, Att, String, String, Object, Object>)
        d.env.defs.insts.get("I");
  }

  private static int size(Chase<?, ?, ?, ?, ?, String, ?, ?, ?, ?, ?, ?> c, String en) {
    return c.en(en, 0).size();
  }

  private static Term<Void, String, Void, Fk, Void, String, Void> gen(String g) {
    return Term.Gen(g);
  }

  private static Term<Void, String, Void, Fk, Void, String, Void> fk(
      String en, String fk, Term<Void, String, Void, Fk, Void, String, Void> t) {
    return Term.Fk(Fk.Fk(en, fk), t);
  }

  /**
   * a1 and a2 share their image b under f, which F sends to g. Z is reached only through h, and
   * nothing reaches W.
   */
  private static final String SHARED_IMAGE =
      """
      typeside Ty = literal { types String }
      schema S = literal : Ty { entities A B foreign_keys f : A -> B }
      schema T = literal : Ty { entities X Y Z W foreign_keys g : X -> Y h : Y -> Z }
      mapping F = literal : S -> T { entity B -> Y entity A -> X foreign_keys f -> g }
      instance I = literal : S { generators a1 a2 : A b : B equations a1.f = b a2.f = b }
      """;

  @Nested
  class Rows {

    @Test
    void mergesTheImagesOfAForeignKeyOfTheSource() throws Exception {
      var c = chase(SHARED_IMAGE);
      assertEquals(2, size(c, "X"));
      assertEquals(1, size(c, "Y"));
      assertEquals(1, size(c, "Z"));
      assertEquals(0, size(c, "W"));
      assertTrue(c.en("X", 10).stream().allMatch(x -> x >= 10));
      assertEquals(1, c.nt.get("B").size());
    }

    @Test
    void mergesThePairsOfRowsItIsGiven() throws Exception {
      AqlMultiDriver d = run(SHARED_IMAGE);
      List<Object> as = new ArrayList<>();
      instance(d).algebra().en("A").forEach(as::add);
      var c = chase(d, Map.of("A", Set.of(new Pair<>(as.get(0), as.get(1)))));
      assertEquals(1, size(c, "X"));
      assertTrue(c.toStringShort().contains("extra: {A=[("), c.toStringShort());
    }

    @Test
    void growsAnEntityPastItsFirstEstimate() throws Exception {
      var c =
          chase(
              """
              typeside Ty = literal { types String }
              schema S = literal : Ty { entities A }
              schema T = literal : Ty { entities X Y Z foreign_keys g : X -> Y h : Y -> Z }
              mapping F = literal : S -> T { entity A -> X }
              instance I = literal : S { generators a : A }
              """);
      assertEquals(1, size(c, "Z"));
      assertEquals(2, c.eqcs.get("Z").parent.length);
    }

    @Test
    void printsEachEntity() throws Exception {
      var c = chase(SHARED_IMAGE);
      assertTrue(c.toString().startsWith("Chase [stuff={X="), c.toString());
      assertTrue(c.toString().contains("en2=W top= 0"), c.toString());
      assertTrue(c.toStringShort().startsWith("X: top: 2\nY: top: 2\n"), c.toStringShort());
      assertEquals("top: 2", c.eqcs.get("X").toStringShort());
    }
  }

  /**
   * The rows of I are added by the term F gives their representatives, and a second row with the
   * same term gets a second number that nothing merges with the first. With f sent to the identity,
   * a.f = b makes a and b one row of {@code sigma F I}.
   */
  @Nested
  class SameTerm {

    private static final String IDENTITY_PATH =
        """
        typeside Ty = literal { types String }
        schema S = literal : Ty { entities A B foreign_keys f : A -> B }
        schema T = literal : Ty { entities X }
        mapping F = literal : S -> T { entity B -> X entity A -> X foreign_keys f -> identity }
        """;

    @Test
    void keepsTwoRowsWhoseTermsAreEqual() throws Exception {
      var c =
          chase(
              IDENTITY_PATH
                  + "instance I = literal : S { generators a : A b : B equations a.f = b }\n");
      assertEquals(2, size(c, "X"));
      assertEquals(List.of(gen("a"), gen("a")), Arrays.asList(c.eqcs.get("X").iso1).subList(0, 2));
    }

    @Test
    void mergesThemWhenTheOtherGeneratorComesFirst() throws Exception {
      var c =
          chase(
              IDENTITY_PATH
                  + "instance I = literal : S { generators b : B a : A equations a.f = b }\n");
      assertEquals(1, size(c, "X"));
    }
  }

  /** The public helpers, called on a finished chase. */
  @Nested
  class Helpers {

    @Test
    void decidesWhetherTwoTermsAreOneRow() throws Exception {
      var c = chase(SHARED_IMAGE);
      var a1g = fk("X", "g", gen("a1"));
      var a2g = fk("X", "g", gen("a2"));
      assertTrue(c.connected(a1g, a2g, "Y"));
      assertFalse(c.connected(gen("a1"), gen("a2"), "X"));
      assertEquals(c.findNoAdd(a1g, "Y"), c.findNoAdd(a2g, "Y"));
      c.union(gen("a1"), gen("a2"), "X");
      assertTrue(c.connected(gen("a1"), gen("a2"), "X"));
    }

    /** A merged row keeps its foreign keys until merge moves them to the root. */
    @Test
    void movesTheForeignKeysOfAMergedRowToItsRoot() throws Exception {
      var c = chase(SHARED_IMAGE);
      c.union(gen("a1"), gen("a2"), "X");
      var x = c.eqcs.get("X");
      x.merge();
      int root = x.find(0);
      BST[] g = x.fks.get(Fk.Fk("X", "g"));
      assertNotNull(g[root]);
      assertNull(g[1 - root]);
      c.makeFunctional();
      assertEquals(1, size(c, "X"));
    }

    @Test
    void addsTheRowsThatANewTermNeeds() throws Exception {
      var c = chase(SHARED_IMAGE);
      var a3 = gen("a3");
      assertEquals(a3, c.find(a3, "X"));
      assertEquals(3, c.eqcs.get("X").top);
      c.makeArrowsTotalFirst();
      assertEquals(3, c.eqcs.get("Y").top);
      assertEquals(fk("X", "g", a3), c.findNoAdd(fk("X", "g", a3), "Y"));
    }
  }
}
