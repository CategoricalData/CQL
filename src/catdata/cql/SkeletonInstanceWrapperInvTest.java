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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The Instance that random returns, here over CqlTestHelpers.skeleton(). */
class SkeletonInstanceWrapperInvTest {

  private static SkeletonInstanceWrapperInv<String, String, Sym, Fk, Att, Integer, Integer>
      wrapped() {
    return new SkeletonInstanceWrapperInv<>(skeleton());
  }

  private static Term<String, String, Sym, Fk, Att, Integer, Integer> gen(int x) {
    return Term.Gen(x);
  }

  @Nested
  class AsAnInstance {

    @Test
    void hasTheRowsAndNullsOfTheSkeleton() {
      var w = wrapped();
      assertEquals(5, w.size());
      assertEquals(List.of("E", "D"), new ArrayList<>(w.schema().ens));
      assertEquals(5, w.gens().size());
      assertEquals("D", w.gens().get(3));
      assertTrue(w.gens().containsKey(0));
      assertEquals(2, w.sks().size());
      assertEquals(COLOR, w.sks().get(6));
    }

    @Test
    void readsItsFlagsFromTheOptions() {
      var w = wrapped();
      assertTrue(w.requireConsistency());
      assertTrue(w.allowUnsafeJava());
    }

    @Test
    void derivesItsEquationsFromTheTables() {
      List<
              Pair<
                  Term<String, String, Sym, Fk, Att, Integer, Integer>,
                  Term<String, String, Sym, Fk, Att, Integer, Integer>>>
          eqs = new ArrayList<>();
      wrapped().eqs((l, r) -> eqs.add(new Pair<>(l, r)));
      assertTrue(eqs.contains(new Pair<>(Term.Fk(SKELETON_F, gen(1)), gen(4))));
      assertTrue(eqs.contains(new Pair<>(Term.Att(SKELETON_A, gen(1)), Term.Sym(RED, List.of()))));
    }

    /** The type algebra has an equation, and require_consistency defaults to true. */
    @Test
    void validatesOnConstruction() {
      Term<String, Void, Sym, Void, Void, Void, Integer>[] eq =
          arr(Term.Sk(5), Term.Sym(MIX, List.of(Term.Sk(6))));
      var s =
          skeleton(CqlTestHelpers.<Term<String, Void, Sym, Void, Void, Void, Integer>[]>arr(eq));
      String m =
          assertThrows(RuntimeException.class, () -> new SkeletonInstanceWrapperInv<>(s))
              .getMessage();
      assertTrue(m.startsWith("Not necessarily conservative over the typeside"), m);
    }
  }

  @Nested
  class DecisionProcedure {

    @Test
    void comparesRowsByWhereTheirPathsLead() {
      var dp = wrapped().dp();
      assertTrue(dp.eq(null, Term.Fk(SKELETON_F, gen(0)), Term.Fk(SKELETON_F, gen(2))));
      assertFalse(dp.eq(Map.of(), gen(0), gen(2)));
      assertEquals("Skeleton Inv Wrapper", dp.toStringProver());
    }

    @Test
    void comparesValuesWithTheSkeletonsPredicate() {
      var dp = wrapped().dp();
      assertTrue(dp.eq(null, Term.Att(SKELETON_A, gen(0)), Term.Sk(5)));
      assertFalse(dp.eq(null, Term.Att(SKELETON_A, gen(0)), Term.Att(SKELETON_A, gen(1))));
    }

    @Test
    void rejectsAContext() {
      var dp = wrapped().dp();
      assertEquals(
          "Anomaly: please report",
          assertThrows(
                  RuntimeException.class,
                  () -> dp.eq(Map.of("x", Chc.inRight("E")), gen(0), gen(0)))
              .getMessage());
    }
  }

  @Nested
  class Algebra {

    @Test
    void readsTheTables() {
      var a = wrapped().algebra();
      assertEquals(2, a.gen(2));
      assertEquals(4, a.fk(SKELETON_F, 1));
      assertEquals(Term.Sk(5), a.att(SKELETON_A, 0));
      assertEquals(Term.Sk(6), a.sk(6));
      assertEquals(Term.Gen(2), a.repr("E", 2));
      assertEquals(Chc.inLeft(5), a.reprT_prot(5));
    }

    @Test
    void numbersTheRowsOfAnEntityFromItsOffset() {
      var a = wrapped().algebra();
      assertEquals(3, a.size("E"));
      List<Integer> d = new ArrayList<>();
      a.en("D").forEach(d::add);
      assertEquals(List.of(3, 4), d);
    }

    @Test
    void printsRowsAndNullsAsStrings() {
      var a = wrapped().algebra();
      assertEquals("2", a.printX("E", 2));
      assertEquals("5", a.printY(COLOR, 5));
      assertEquals("SkeletonWrapperInv", a.toStringProver());
    }

    @Test
    void buildsTheTypeAlgebraOnce() {
      var a = wrapped().algebra();
      assertTrue(a.hasFreeTypeAlgebra());
      assertTrue(a.hasNulls());
      var talg = a.talg();
      assertEquals(Map.of(5, COLOR, 6, COLOR), talg.sks);
      assertTrue(talg.eqsNoDefns().isEmpty());
      assertSame(talg, a.talg());
      assertSame(a.talg0(), a.talg0());
    }
  }
}
