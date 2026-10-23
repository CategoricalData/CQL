package catdata.cql;

import static catdata.cql.CqlTestHelpers.COLOR;
import static catdata.cql.CqlTestHelpers.MIX;
import static catdata.cql.CqlTestHelpers.RED;
import static catdata.cql.CqlTestHelpers.SKELETON_A;
import static catdata.cql.CqlTestHelpers.SKELETON_F;
import static catdata.cql.CqlTestHelpers.arr;
import static catdata.cql.CqlTestHelpers.skeleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import catdata.Pair;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** SkeletonSaturatedInstance through its only concrete subclass, on CqlTestHelpers.skeleton(). */
class SkeletonSaturatedInstanceTest {

  private static Term<Void, String, Void, Fk, Void, Integer, Void> row(int x) {
    return Term.Gen(x);
  }

  private static Term<String, String, Sym, Fk, Att, Integer, Integer> gen(int x) {
    return Term.Gen(x);
  }

  @Nested
  class Generators {

    @Test
    void numberTheRowsOfEachEntityInTurn() {
      List<Map.Entry<Integer, String>> gens = new ArrayList<>();
      skeleton().gens().forEach(gens::add);
      assertEquals(
          List.of(
              Map.entry(0, "E"),
              Map.entry(1, "E"),
              Map.entry(2, "E"),
              Map.entry(3, "D"),
              Map.entry(4, "D")),
          gens);
      assertEquals(5, skeleton().numGens());
    }

    @Test
    void numberTheLabelledNullsOfEachTypeInTurn() {
      List<Map.Entry<Integer, String>> sks = new ArrayList<>();
      skeleton().sks().forEach(sks::add);
      assertEquals(List.of(Map.entry(5, COLOR), Map.entry(6, COLOR)), sks);
    }

    @Test
    void areTheirOwnRowsAndRepresentatives() {
      var s = skeleton();
      assertEquals(4, s.gen(4));
      assertEquals(Term.Sk(6), s.sk(6));
      assertEquals(row(2), s.repr(2));
    }
  }

  @Nested
  class Evaluation {

    @Test
    void evalFollowsTheForeignKeys() {
      var s = skeleton();
      assertEquals(1, s.eval(row(1)));
      assertEquals(4, s.eval(Term.Fk(SKELETON_F, row(1))));
      assertTrue(s.eq(Term.Fk(SKELETON_F, row(0)), Term.Fk(SKELETON_F, row(2))));
      assertFalse(s.eq(row(0), row(2)));
    }

    @Test
    void evalTReplacesAttributesByTheirValues() {
      var s = skeleton();
      assertEquals(Term.Sk(5), s.evalT(Term.Att(SKELETON_A, gen(0))));
      assertEquals(Term.Sym(RED, List.of()), s.evalT(Term.Att(SKELETON_A, gen(1))));
      assertEquals(Term.Sk(6), s.evalT(Term.Sk(6)));
      assertEquals(
          Term.Sym(MIX, List.of(Term.Sk(6))),
          s.evalT(Term.Sym(MIX, List.of(Term.Att(SKELETON_A, gen(2))))));
      Term<String, String, Sym, Fk, Att, Integer, Integer> ann =
          Term.Obj(Optional.of("ann"), COLOR);
      assertEquals(ann, s.evalT(ann));
    }

    @Test
    void evalTRejectsARow() {
      var s = skeleton();
      assertEquals(
          "Anomaly: please report",
          assertThrows(RuntimeException.class, () -> s.evalT(gen(0))).getMessage());
    }
  }

  @Nested
  class Equations {

    private List<
            Pair<
                Term<String, String, Sym, Fk, Att, Integer, Integer>,
                Term<String, String, Sym, Fk, Att, Integer, Integer>>>
        eqsOf(SkeletonExtensionalInstance<String, String, Sym, Fk, Att> s) {
      List<
              Pair<
                  Term<String, String, Sym, Fk, Att, Integer, Integer>,
                  Term<String, String, Sym, Fk, Att, Integer, Integer>>>
          ret = new ArrayList<>();
      s.eqs((l, r) -> ret.add(new Pair<>(l, r)));
      return ret;
    }

    @Test
    void stateEveryForeignKeyAndAttributeOfEveryRow() {
      var eqs = eqsOf(skeleton());
      Set<
              Pair<
                  Term<String, String, Sym, Fk, Att, Integer, Integer>,
                  Term<String, String, Sym, Fk, Att, Integer, Integer>>>
          expected = new HashSet<>();
      int[] f = {3, 4, 3};
      for (int x = 0; x < 3; x++) {
        expected.add(new Pair<>(Term.Fk(SKELETON_F, gen(x)), gen(f[x])));
      }
      expected.add(new Pair<>(Term.Att(SKELETON_A, gen(0)), Term.Sk(5)));
      expected.add(new Pair<>(Term.Att(SKELETON_A, gen(1)), Term.Sym(RED, List.of())));
      expected.add(new Pair<>(Term.Att(SKELETON_A, gen(2)), Term.Sk(6)));
      assertEquals(expected, new HashSet<>(eqs));
      assertEquals(6, eqs.size());
      assertEquals(6, skeleton().numEqs());
    }

    @Test
    void endWithTheTypeAlgebraEquations() {
      Term<String, Void, Sym, Void, Void, Void, Integer> y5 = Term.Sk(5);
      Term<String, Void, Sym, Void, Void, Void, Integer> mix6 = Term.Sym(MIX, List.of(Term.Sk(6)));
      Term<String, Void, Sym, Void, Void, Void, Integer>[] eq = arr(y5, mix6);
      var s =
          skeleton(CqlTestHelpers.<Term<String, Void, Sym, Void, Void, Void, Integer>[]>arr(eq));
      var eqs = eqsOf(s);
      assertEquals(7, eqs.size());
      assertEquals(7, s.numEqs());
      assertEquals(
          new Pair<>(Term.Sk(5), Term.Sym(MIX, List.of(Term.Sk(6)))), eqs.get(eqs.size() - 1));
    }
  }
}
