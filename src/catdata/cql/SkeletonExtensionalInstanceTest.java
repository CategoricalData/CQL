package catdata.cql;

import static catdata.cql.CqlTestHelpers.COLOR;
import static catdata.cql.CqlTestHelpers.MIX;
import static catdata.cql.CqlTestHelpers.RED;
import static catdata.cql.CqlTestHelpers.SKELETON_A;
import static catdata.cql.CqlTestHelpers.SKELETON_F;
import static catdata.cql.CqlTestHelpers.arr;
import static catdata.cql.CqlTestHelpers.colorSchema;
import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.skeleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** On CqlTestHelpers.skeleton(): rows 0-2 of E, 3-4 of D, and the Color nulls 5 and 6. */
class SkeletonExtensionalInstanceTest {

  private static Term<String, Void, Sym, Void, Void, Void, Integer> y(int i) {
    return Term.Sk(i);
  }

  @Nested
  class Sizes {

    @Test
    void countRowsAndNullsInTotalAndPerSort() {
      var s = skeleton();
      assertEquals(5, s.xs());
      assertEquals(3, s.xs("E"));
      assertEquals(2, s.xs("D"));
      assertEquals(2, s.ys());
      assertEquals(2, s.ys(COLOR));
    }

    @Test
    void startEachSortAtItsOffset() {
      var s = skeleton();
      assertEquals(0, s.xo("E"));
      assertEquals(3, s.xo("D"));
      assertEquals(5, s.yo(COLOR));
    }

    @Test
    void hasNullsOnlyWhenSomeTypeHasOne() {
      assertTrue(skeleton().hasNulls());
      var none =
          new SkeletonExtensionalInstance<>(
              colorSchema(List.of("E"), Map.of(), Map.of()),
              Map.of("E", 1),
              Map.of(COLOR, 0),
              Map.of("E", 0),
              Map.of(COLOR, 1),
              Map.of(),
              Map.of(),
              arr(),
              (l, r) -> l.equals(r),
              Chc::inLeft,
              ops());
      assertFalse(none.hasNulls());
      assertEquals(0, none.ys());
    }
  }

  @Nested
  class Tables {

    @Test
    void readForeignKeysAndAttributesFromTheRowsOffset() {
      var s = skeleton();
      assertEquals(3, s.fk(SKELETON_F, 0));
      assertEquals(4, s.fk(SKELETON_F, 1));
      assertEquals(y(5), s.att(SKELETON_A, 0));
      assertEquals(Term.Sym(RED, List.of()), s.att(SKELETON_A, 1));
    }

    @Test
    void printRowsAndNullsAsTheirNumbers() {
      var s = skeleton();
      assertEquals(2, s.printX(2));
      assertEquals(6, s.printY(6));
      assertEquals("Skeleton Instance", s.toStringProver());
    }

    @Test
    void reprTProtGivesWhatEachNullStandsFor() {
      assertEquals(Chc.inLeft(6), skeleton().reprT_prot(6));
    }

    @Test
    void keepsTheOptionsAndTheSchema() {
      var s = skeleton();
      assertSame(ops(), s.options());
      assertEquals(List.of("E", "D"), new ArrayList<>(s.schema().ens));
    }
  }

  @Nested
  class TypeAlgebra {

    @Test
    void isFreeWithoutEquations() {
      var s = skeleton();
      assertTrue(s.hasFreeTypeAlgebra());
      assertEquals(0, s.numEqsT());
      List<Term<String, Void, Sym, Void, Void, Void, Integer>> eqs = new ArrayList<>();
      s.eqsT((l, r) -> eqs.add(l));
      assertTrue(eqs.isEmpty());
    }

    @Test
    void listsItsEquationsAndDecidesWithItsPredicate() {
      Term<String, Void, Sym, Void, Void, Void, Integer>[] eq =
          arr(y(5), Term.Sym(MIX, List.of(y(6))));
      var s =
          skeleton(CqlTestHelpers.<Term<String, Void, Sym, Void, Void, Void, Integer>[]>arr(eq));
      assertFalse(s.hasFreeTypeAlgebra());
      assertEquals(1, s.numEqsT());
      List<
              Pair<
                  Term<String, Void, Sym, Void, Void, Void, Integer>,
                  Term<String, Void, Sym, Void, Void, Void, Integer>>>
          eqs = new ArrayList<>();
      s.eqsT((l, r) -> eqs.add(new Pair<>(l, r)));
      assertEquals(List.of(new Pair<>(y(5), Term.Sym(MIX, List.of(y(6))))), eqs);
      assertTrue(s.eqT(y(5), y(5)));
      assertFalse(s.eqT(y(5), y(6)));
    }
  }
}
