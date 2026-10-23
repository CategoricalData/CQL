package catdata.cql;

import static catdata.cql.CqlTestHelpers.ops;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.Collage.CCollage;
import catdata.provers.DPKB;
import catdata.provers.KBExp;
import catdata.provers.KBTheory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The wrapper that turns a prover into a decision procedure: reflexivity, simplification, java
 * reduction and fresh java constants, over a type Str of java.lang.String and a prover that
 * records what it is asked.
 */
class KBtoDPTest {

  private static final String STR = "Str";

  /** twice : Str -> Str, with javascript, and opaque : Str -> Str, without. */
  private static final Map<String, Pair<List<String>, String>> SYMS = new LinkedHashMap<>();

  static {
    SYMS.put("twice", new Pair<>(List.of(STR), STR));
    SYMS.put("opaque", new Pair<>(List.of(STR), STR));
  }

  private static final AqlJs<String, String> JAVA =
      new AqlJs<>(
          ops(),
          SYMS,
          Map.of(STR, "java.lang.String"),
          Map.of(STR, "x => x"),
          Map.of("twice", "x => x + x"));

  private static final AqlJs<String, String> NO_JAVA =
      new AqlJs<>(ops(), SYMS, Map.of(), Map.of(), Map.of());

  private static final Term<String, String, String, String, String, String, String> Y =
      Term.Sk("y");
  private static final Term<String, String, String, String, String, String, String> Z =
      Term.Sk("z");

  private static Term<String, String, String, String, String, String, String> str(String s) {
    return Term.Obj(s, STR);
  }

  private static Term<String, String, String, String, String, String, String> app(
      String f, Term<String, String, String, String, String, String, String> a) {
    return Term.Sym(f, List.of(a));
  }

  private static Head<String, String, String, String, String, String, String> obj(String s) {
    return Head.ObjHead(s, STR);
  }

  /** The empty theory over Str, as a prover would hold it. */
  private static KBTheory<
          Chc<String, String>, Head<String, String, String, String, String, String, String>, String>
      theory() {
    Collage<String, String, String, String, String, String, String> col = new CCollage<>();
    col.tys().add(STR);
    return col.toKB();
  }

  /**
   * Answers from a queue, false once it is empty, and records the questions, their contexts and
   * the constants it is told about. Like the real provers, it adds those to its theory.
   */
  private static final class Recording
      extends DPKB<
          Chc<String, String>,
          Head<String, String, String, String, String, String, String>,
          String> {

    final List<
            Pair<
                KBExp<Head<String, String, String, String, String, String, String>, String>,
                KBExp<Head<String, String, String, String, String, String, String>, String>>>
        asked = new ArrayList<>();
    final List<Map<String, Chc<String, String>>> contexts = new ArrayList<>();
    final List<Head<String, String, String, String, String, String, String>> added =
        new ArrayList<>();
    final Deque<Boolean> answers = new ArrayDeque<>();
    boolean trivialityCheck;

    Recording(
        KBTheory<
                Chc<String, String>,
                Head<String, String, String, String, String, String, String>,
                String>
            kb,
        Boolean... answers) {
      super(kb);
      this.answers.addAll(List.of(answers));
    }

    @Override
    public boolean eq(
        Map<String, Chc<String, String>> ctx,
        KBExp<Head<String, String, String, String, String, String, String>, String> lhs,
        KBExp<Head<String, String, String, String, String, String, String>, String> rhs) {
      contexts.add(ctx);
      asked.add(new Pair<>(lhs, rhs));
      return !answers.isEmpty() && answers.pop();
    }

    @Override
    public void add(
        Head<String, String, String, String, String, String, String> c, Chc<String, String> t) {
      added.add(c);
      kb.syms.put(c, new Pair<>(List.of(), t));
    }

    @Override
    public boolean supportsTrivialityCheck() {
      return trivialityCheck;
    }

    @Override
    public String toString() {
      return "recording";
    }
  }

  private static KBtoDP<String, String, String, String, String, String, String> dp(
      AqlJs<String, String> js,
      Function<
              Term<String, String, String, String, String, String, String>,
              Term<String, String, String, String, String, String, String>>
          simp,
      Recording dpkb,
      boolean allowNew) {
    return new KBtoDP<>(js, simp, dpkb, allowNew, new CCollage<>());
  }

  private static KBtoDP<String, String, String, String, String, String, String> dp(
      AqlJs<String, String> js, Recording dpkb) {
    return dp(js, Function.identity(), dpkb, true);
  }

  private static Pair<
          KBExp<Head<String, String, String, String, String, String, String>, String>,
          KBExp<Head<String, String, String, String, String, String, String>, String>>
      question(
          Term<String, String, String, String, String, String, String> l,
          Term<String, String, String, String, String, String, String> r) {
    return new Pair<>(l.toKB(), r.toKB());
  }

  @Nested
  class Deciding {

    @Test
    void answersEqualSidesWithoutSimplifyingOrAsking() {
      AtomicInteger simplified = new AtomicInteger();
      Recording kb = new Recording(theory());
      var dp =
          dp(
              JAVA,
              t -> {
                simplified.incrementAndGet();
                return t;
              },
              kb,
              true);
      assertTrue(dp.eq(Map.of(), app("opaque", Y), app("opaque", Y)));
      assertEquals(0, simplified.get());
      assertTrue(kb.asked.isEmpty());
    }

    @Test
    void answersSidesThatSimplifyAlikeWithoutAsking() {
      Recording kb = new Recording(theory());
      var dp = dp(JAVA, t -> t == Y ? Z : t, kb, true);
      assertTrue(dp.eq(Map.of(), Y, Z));
      assertTrue(kb.asked.isEmpty());
    }

    @Test
    void asksTheProverAboutTheSimplifiedSidesInAnEmptyContext() {
      Recording kb = new Recording(theory(), true);
      var dp = dp(NO_JAVA, t -> t == Y ? app("opaque", Y) : t, kb, true);
      assertTrue(dp.eq(null, Y, Z));
      assertEquals(List.of(question(app("opaque", Y), Z)), kb.asked);
      assertSame(Collections.emptyMap(), kb.contexts.get(0));
    }

    @Test
    void reducesTypeSortedSidesWithJavascript() {
      Recording kb = new Recording(theory());
      assertTrue(dp(JAVA, kb).eq(Map.of(), app("twice", str("ab")), str("abab")));
      assertTrue(kb.asked.isEmpty());
    }

    @Test
    void asksAgainWithoutJavascriptWhenTheProverSaysNo() {
      Recording kb = new Recording(theory(), false, true);
      var lhs = app("opaque", app("twice", str("ab")));
      assertTrue(dp(JAVA, kb).eq(Map.of(), lhs, Y));
      assertEquals(List.of(question(app("opaque", str("abab")), Y), question(lhs, Y)), kb.asked);
    }

    /** Entity-sorted sides are not reduced, and the second question repeats the first. */
    @Test
    void asksTheSameEntitySortedQuestionTwice() {
      Recording kb = new Recording(theory());
      Term<String, String, String, String, String, String, String> e = Term.Gen("e");
      Term<String, String, String, String, String, String, String> d = Term.Gen("d");
      assertFalse(dp(JAVA, kb).eq(Map.of(), e, d));
      assertEquals(List.of(question(e, d), question(e, d)), kb.asked);
    }

    @Test
    void asksOnceWithoutJavaTypes() {
      Recording kb = new Recording(theory());
      var lhs = app("opaque", app("twice", str("ab")));
      assertFalse(dp(NO_JAVA, kb).eq(Map.of(), lhs, Y));
      assertEquals(List.of(question(lhs, Y)), kb.asked);
    }
  }

  @Nested
  class FreshConstants {

    @Test
    void tellsTheProverAboutConstantsItDoesNotKnow() {
      var th = theory();
      th.syms.put(obj("ab"), new Pair<>(List.of(), Chc.inLeft(STR)));
      Recording kb = new Recording(th);
      assertFalse(dp(NO_JAVA, kb).eq(Map.of(), app("opaque", str("ab")), str("cd")));
      assertEquals(List.of(obj("cd")), kb.added);
      assertEquals(new Pair<>(List.of(), Chc.inLeft(STR)), th.syms.get(obj("cd")));
    }

    /** The second question finds both constants already in the theory. */
    @Test
    void tellsItOnceWhenItAsksTwice() {
      Recording kb = new Recording(theory());
      var lhs = app("opaque", app("twice", str("ab")));
      assertFalse(dp(JAVA, kb).eq(Map.of(), lhs, Y));
      assertEquals(2, kb.asked.size());
      assertEquals(List.of(obj("abab"), obj("ab")), kb.added);
    }

    @Test
    void tellsAProverWithoutATheoryNothing() {
      Recording kb = new Recording(null, true);
      assertTrue(dp(NO_JAVA, kb).eq(Map.of(), str("ab"), str("cd")));
      assertTrue(kb.added.isEmpty());
    }

    /** Issue 69: a term without any java constant is rejected too. */
    @Test
    void rejectsEveryEquationItMustAskAboutWhenNotAllowed() {
      Recording kb = new Recording(theory(), true);
      var dp = dp(NO_JAVA, t -> t == Y ? Z : t, kb, false);
      assertTrue(dp.eq(Map.of(), Y, Y));
      assertTrue(dp.eq(Map.of(), Y, Z));
      var opaque = app("opaque", Y);
      String m =
          assertThrows(RuntimeException.class, () -> dp.eq(Map.of(), opaque, Z)).getMessage();
      assertEquals("New lhs java object: " + opaque.toStringUnambig(), m);
      assertTrue(kb.asked.isEmpty());
    }
  }

  @Nested
  class Description {

    @Test
    void namesTheProverItWraps() {
      Recording kb = new Recording(theory());
      assertEquals(
          "Definitional simplification and reflexivity wrapping plus java of "
              + Recording.class.getName()
              + "\n\nrecording",
          dp(NO_JAVA, kb).toStringProver());
    }

    @Test
    void checksTrivialityWhenTheProverDoes() {
      Recording kb = new Recording(theory());
      var dp = dp(NO_JAVA, kb);
      assertFalse(dp.supportsTrivialityCheck());
      kb.trivialityCheck = true;
      assertTrue(dp.supportsTrivialityCheck());
    }
  }
}
