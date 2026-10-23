package catdata.cql;

import static catdata.cql.CqlTestHelpers.COLOR;
import static catdata.cql.CqlTestHelpers.MIX;
import static catdata.cql.CqlTestHelpers.RED;
import static catdata.cql.CqlTestHelpers.SKELETON_A;
import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.arr;
import static catdata.cql.CqlTestHelpers.colorSchema;
import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.skeleton;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static catdata.cql.CqlTestHelpers.sqlTypeSide;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** SkeletonInstance's own code, reprT_protected, through its only concrete subclass. */
class SkeletonInstanceTest {

  private static Term<String, Void, Sym, Void, Void, Void, Integer> y(int i) {
    return Term.Sk(i);
  }

  /**
   * One row 0 of E with a : E -> Color, whose value is the labelled null 1; reprT_prot decides
   * what 1 stands for.
   */
  private static SkeletonExtensionalInstance<String, String, Sym, Fk, Att> oneRow(
      Function<Integer, Chc<Integer, Pair<Integer, Att>>> sks) {
    return new SkeletonExtensionalInstance<>(
        colorSchema(List.of("E"), Map.of(SKELETON_A, new Pair<>("E", COLOR)), Map.of()),
        Map.of("E", 1),
        Map.of(COLOR, 1),
        Map.of("E", 0),
        Map.of(COLOR, 1),
        Map.of(),
        Map.of(SKELETON_A, arr(y(1))),
        arr(),
        (l, r) -> l.equals(r),
        sks,
        ops());
  }

  @Nested
  class ReprT {

    @Test
    void keepsALabelledNullThatIsItsOwnNull() {
      assertEquals(Term.Sk(5), skeleton().reprT_protected(y(5)));
    }

    @Test
    void rebuildsSymbolsOverTheirArguments() {
      var s = skeleton();
      assertEquals(Term.Sym(RED, List.of()), s.reprT_protected(Term.Sym(RED, List.of())));
      assertEquals(
          Term.Sym(MIX, List.of(Term.Sk(6))), s.reprT_protected(Term.Sym(MIX, List.of(y(6)))));
    }

    /** With java types the result is reduced, which leaves a symbol of labelled nulls alone. */
    @Test
    void reducesOverATypesideWithJavaTypes() {
      Att n = Att.Att("E", "n");
      Map<String, Integer> zero = new HashMap<>();
      for (String ty : sqlTypeSide().tys) {
        zero.put(ty, 0);
      }
      zero.put(VARCHAR, 1);
      Map<String, Integer> start = new HashMap<>(zero);
      start.replaceAll((ty, k) -> 1);
      var s =
          new SkeletonExtensionalInstance<String, String, Sym, Fk, Att>(
              sqlSchema(List.of("E"), Map.of(n, "E"), Map.of()),
              Map.of("E", 1),
              zero,
              Map.of("E", 0),
              start,
              Map.of(),
              Map.of(n, arr(y(1))),
              arr(),
              (l, r) -> l.equals(r),
              Chc::inLeft,
              ops());
      Sym isNull = Sym.Sym("isNull", new Pair<>(List.of(VARCHAR), CqlTestHelpers.BOOLEAN));
      Term<String, Void, Sym, Void, Void, Void, Integer> t = Term.Sym(isNull, List.of(y(1)));
      assertEquals(Term.Sym(isNull, List.of(Term.Sk(1))), s.reprT_protected(t));
    }

    /** A constant, alone or under a symbol, is neither a labelled null nor a symbol. */
    @Test
    void rejectsAJavaConstant() {
      var s = skeleton();
      Term<String, Void, Sym, Void, Void, Void, Integer> ann =
          Term.Obj(Optional.of("ann"), VARCHAR);
      assertEquals(
          "Anomaly: please report",
          assertThrows(RuntimeException.class, () -> s.reprT_protected(ann)).getMessage());
      assertThrows(RuntimeException.class, () -> s.reprT_protected(Term.Sym(MIX, List.of(ann))));
    }

    /**
     * Issue 58: a null that stands for the value of an attribute is replaced by that value, which
     * is the same null, so the recursion never ends.
     */
    @Test
    void loopsOnANullThatStandsForAnAttribute() {
      var s = oneRow(i -> Chc.inRight(new Pair<>(0, SKELETON_A)));
      assertThrows(StackOverflowError.class, () -> s.reprT_protected(y(1)));
      assertEquals(Term.Sk(1), oneRow(Chc::inLeft).reprT_protected(y(1)));
    }
  }
}
