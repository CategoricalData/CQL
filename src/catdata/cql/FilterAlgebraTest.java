package catdata.cql;

import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.forall;
import static catdata.cql.CqlTestHelpers.literal;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import catdata.cql.fdm.LiteralInstance;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** What {@code cascade_delete I : T} evaluates to. */
class FilterAlgebraTest {

  private static final Fk F = Fk.Fk("A", "f");
  private static final Fk G = Fk.Fk("A", "g");
  private static final Att B = Att.Att("B", "b");
  private static final Att C = Att.Att("B", "c");

  private static final Map<Fk, Pair<String, String>> FKS =
      Map.of(F, new Pair<>("A", "B"), G, new Pair<>("A", "B"));

  /** f g : A -> B and b c : B -> Varchar. */
  private static final Schema<String, String, Sym, Fk, Att> S =
      sqlSchema(List.of("A", "B"), Map.of(B, "B", C, "B"), FKS);

  /** S with forall x:B. x.b = ok. */
  private static final Schema<String, String, Sym, Fk, Att> T =
      sqlSchema(
          List.of("A", "B"),
          Map.of(B, "B", C, "B"),
          FKS,
          List.of(
              forall(
                  "x", "B", Term.Att(B, Term.Var("x")), Term.Obj(Optional.of("ok"), VARCHAR))));

  private static Term<String, String, Sym, Fk, Att, String, String> gen(String g) {
    return Term.Gen(g);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> str(String s) {
    return Term.Obj(Optional.of(s), VARCHAR);
  }

  private static Pair<
          Term<String, String, Sym, Fk, Att, String, String>,
          Term<String, String, Sym, Fk, Att, String, String>>
      eq(
          Term<String, String, Sym, Fk, Att, String, String> l,
          Term<String, String, Sym, Fk, Att, String, String> r) {
    return new Pair<>(l, r);
  }

  /**
   * b1.b = bad and b2.b = ok; a1 points at b1 through both f and g, a2 at b2. b1.c is unknown and
   * b2.c is the labelled null y.
   */
  private static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      instance() {
    Map<String, String> gens = new LinkedHashMap<>();
    gens.put("a1", "A");
    gens.put("a2", "A");
    gens.put("b1", "B");
    gens.put("b2", "B");
    return literal(
        S,
        gens,
        Map.of("y", VARCHAR),
        List.of(
            eq(Term.Att(B, gen("b1")), str("bad")),
            eq(Term.Att(B, gen("b2")), str("ok")),
            eq(Term.Att(C, gen("b2")), Term.Sk("y")),
            eq(Term.Fk(F, gen("a1")), gen("b1")),
            eq(Term.Fk(G, gen("a1")), gen("b1")),
            eq(Term.Fk(F, gen("a2")), gen("b2")),
            eq(Term.Fk(G, gen("a2")), gen("b2"))));
  }

  private static String message(Executable e) {
    return assertThrows(RuntimeException.class, e).getMessage();
  }

  private static <X> Set<X> rows(Iterable<X> it) {
    Set<X> ret = new HashSet<>();
    it.forEach(ret::add);
    return ret;
  }

  @Nested
  class Deletion {

    @Test
    void findsTheRowsThatBreakAnEquation() {
      var i = instance();
      var a = new FilterAlgebra<>(i, T);
      assertEquals(
          Map.of("A", Set.of(), "B", Set.of(i.algebra().gen("b1"))), a.getUnsatisfying());
    }

    @Test
    void deletesTheRowsThatPointAtDeletedRows() {
      var i = instance();
      var a = new FilterAlgebra<>(i, T);
      assertEquals(Set.of(i.algebra().gen("a2")), rows(a.en("A")));
      assertEquals(Set.of(i.algebra().gen("b2")), rows(a.en("B")));
      assertEquals(1, a.size("A"));
      assertEquals(1, a.size("B"));
      Integer a1 = i.algebra().gen("a1");
      Integer b1 = i.algebra().gen("b1");
      assertEquals(Map.of("A", Set.of(a1), "B", Set.of(b1)), a.m);
    }

    /**
     * a1 points at the deleted b1 through f and g, so its row is removed through the iterator
     * twice, and the set of rows of A counts one row too few. size(A) is counted from the deleted
     * rows.
     */
    @Test
    void removesARowTwiceWhenTwoForeignKeysPointAtDeletedRows() {
      var a = new FilterAlgebra<>(instance(), T);
      Collection<Integer> rowsOfA = (Collection<Integer>) a.en("A");
      assertEquals(1, rows(rowsOfA).size());
      assertEquals(0, rowsOfA.size());
      assertEquals(1, a.size("A"));
    }

    @Test
    void keepsEverythingWhenTheTargetHasNoEquations() {
      var i = instance();
      var a = new FilterAlgebra<>(i, S);
      assertEquals(2, a.size("A"));
      assertEquals(rows(i.algebra().en("B")), rows(a.en("B")));
    }
  }

  @Nested
  class Checks {

    @Test
    void rejectsATargetWithAnotherEntityForeignKeyOrAttribute() {
      var i = instance();
      var extraEntity = sqlSchema(List.of("A", "B", "Z"), Map.of(), Map.of());
      assertEquals("Missing entity: Z", message(() -> new FilterAlgebra<>(i, extraEntity)));
      Fk h = Fk.Fk("A", "h");
      var extraFk = sqlSchema(List.of("A", "B"), Map.of(), Map.of(h, new Pair<>("A", "B")));
      assertEquals("Missing fk: h", message(() -> new FilterAlgebra<>(i, extraFk)));
      Att z = Att.Att("B", "z");
      var extraAtt = sqlSchema(List.of("A", "B"), Map.of(z, "B"), Map.of());
      assertEquals("Missing att: z", message(() -> new FilterAlgebra<>(i, extraAtt)));
    }
  }

  @Nested
  class Delegation {

    @Test
    void readsTheSourceAlgebra() {
      var i = instance();
      var a = new FilterAlgebra<>(i, T);
      var s = i.algebra();
      Integer a2 = s.gen("a2");
      Integer b2 = s.gen("b2");
      assertSame(T, a.schema());
      assertEquals(a2, a.gen("a2"));
      assertEquals(b2, a.fk(G, a2));
      assertEquals(s.att(B, b2), a.att(B, b2));
      assertEquals(s.sk("y"), a.sk("y"));
      assertEquals(s.repr("B", b2), a.repr("B", b2));
      assertEquals(s.printX("A", a2), a.printX("A", a2));
      assertEquals(s.hasFreeTypeAlgebra(), a.hasFreeTypeAlgebra());
      assertEquals(s.hasNulls(), a.hasNulls());
      assertEquals("Cascade delete of " + s.toStringProver(), a.toStringProver());
    }

    /** Issue 61: the type algebra is the source's, including the nulls of deleted rows. */
    @Test
    void keepsTheTypeAlgebraOfTheSource() {
      var i = instance();
      var a = new FilterAlgebra<>(i, T);
      assertSame(i.algebra().talg(), a.talg());
      Chc<String, Pair<Integer, Att>> deleted = Chc.inRight(new Pair<>(i.algebra().gen("b1"), C));
      assertTrue(a.talg().sks.containsKey(deleted));
      assertEquals(i.algebra().reprT_prot(deleted), a.reprT_prot(deleted));
      assertEquals(
          i.algebra().printY(VARCHAR, deleted), a.printY(VARCHAR, deleted));
    }
  }

  @Nested
  class Instances {

    @Test
    void filterInstanceKeepsTheSurvivingRows() {
      var j = FilterAlgebra.filterInstance(instance(), T);
      assertEquals(2, j.size());
      assertEquals(1, j.algebra().size("A"));
      assertSame(T, j.schema());
    }

    /** Issue 61: the null of b1.c is still a labelled null of the result. */
    @Test
    void filterInstanceListsANullOfADeletedRow() {
      var i = instance();
      var j = FilterAlgebra.filterInstance(i, T);
      Chc<String, Pair<Integer, Att>> deleted = Chc.inRight(new Pair<>(i.algebra().gen("b1"), C));
      List<Chc<String, Pair<Integer, Att>>> sks = new ArrayList<>();
      j.sks().keySet(sks::add);
      assertTrue(sks.contains(deleted), "" + sks);
    }
  }
}
