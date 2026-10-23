package catdata.cql;

import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import catdata.cql.Instance.IMap;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import catdata.cql.fdm.InitialAlgebra;
import catdata.cql.fdm.LiteralInstance;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class InstanceTest {

  private static final String COLOR = "Color";
  private static final String E = "E";
  private static final String D = "D";

  private static final Sym RED = Sym.Sym("red", new Pair<>(List.of(), COLOR));
  private static final Sym GREEN = Sym.Sym("green", new Pair<>(List.of(), COLOR));
  private static final Sym MIX = Sym.Sym("mix", new Pair<>(List.of(COLOR), COLOR));

  private static final Fk F = Fk.Fk(E, "f");
  private static final Att A = Att.Att(E, "a");

  /** The type and attribute of {@link #booleans()}. */
  private static final String BOOL = "Bool";

  private static final Att B = Att.Att(E, "b");

  private static TypeSide<String, Sym> typeSide(Sym... syms) {
    Map<Sym, Pair<List<String>, String>> m = new LinkedHashMap<>();
    for (Sym s : syms) {
      m.put(s, s.ty);
    }
    return new TypeSide<>(
        new HashSet<>(Set.of(COLOR)), m, new HashSet<>(), Map.of(), Map.of(), Map.of(), ops());
  }

  /** Color with the constants red and green. */
  private static TypeSide<String, Sym> colors() {
    return typeSide(RED, GREEN);
  }

  private static Schema<String, String, Sym, Fk, Att> schema(
      TypeSide<String, Sym> ts,
      List<String> ens,
      Map<Att, Pair<String, String>> atts,
      Map<Fk, Pair<String, String>> fks) {
    Collage<String, String, Sym, Fk, Att, Void, Void> col = new CCollage<>();
    col.tys().addAll(ts.tys);
    col.syms().putAll(ts.syms);
    col.java_tys().putAll(ts.js.java_tys);
    col.getEns().addAll(ens);
    col.atts().putAll(atts);
    col.fks().putAll(fks);
    return new Schema<>(ts, col, ops());
  }

  /** f : E -> D and a : E -> t. */
  private static Schema<String, String, Sym, Fk, Att> ed(
      TypeSide<String, Sym> ts, Att att, String t) {
    return schema(ts, List.of(E, D), Map.of(att, new Pair<>(E, t)), Map.of(F, new Pair<>(E, D)));
  }

  private static Term<String, String, Sym, Fk, Att, String, String> gen(String g) {
    return Term.Gen(g);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> sk(String y) {
    return Term.Sk(y);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> fk(
      Fk f, Term<String, String, Sym, Fk, Att, String, String> t) {
    return Term.Fk(f, t);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> att(
      Att a, Term<String, String, Sym, Fk, Att, String, String> t) {
    return Term.Att(a, t);
  }

  @SafeVarargs
  private static Term<String, String, Sym, Fk, Att, String, String> sym(
      Sym f, Term<String, String, Sym, Fk, Att, String, String>... args) {
    return Term.Sym(f, List.of(args));
  }

  private static Pair<
          Term<String, String, Sym, Fk, Att, String, String>,
          Term<String, String, Sym, Fk, Att, String, String>>
      eq(
          Term<String, String, Sym, Fk, Att, String, String> l,
          Term<String, String, Sym, Fk, Att, String, String> r) {
    return new Pair<>(l, r);
  }

  /** What {@code instance I = literal : S { ... }} evaluates to. */
  private static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      literal(
          Schema<String, String, Sym, Fk, Att> schema,
          Map<String, String> gens,
          Map<String, String> sks,
          List<
                  Pair<
                      Term<String, String, Sym, Fk, Att, String, String>,
                      Term<String, String, Sym, Fk, Att, String, String>>>
              eqs,
          boolean requireConsistency) {
    Collage<String, String, Sym, Fk, Att, String, String> col = new CCollage<>(schema.collage());
    col.gens().putAll(gens);
    col.sks().putAll(sks);
    for (var e : eqs) {
      col.eqs().add(new Eq<>(null, e.first, e.second));
    }
    InitialAlgebra<String, String, Sym, Fk, Att, String, String> alg =
        new InitialAlgebra<>(ops(), schema, col, y -> y, (x, y) -> y);
    return new LiteralInstance<>(
        schema, col.gens(), col.sks(), eqs, alg.dp(), alg, requireConsistency, true);
  }

  /** e1 e2 e3 : E and d : D, with every e.f = d and e1.a = red; e2.a and e3.a are unknown. */
  private static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      threeEs() {
    Map<String, String> gens = new LinkedHashMap<>();
    gens.put("e1", E);
    gens.put("e2", E);
    gens.put("e3", E);
    gens.put("d", D);
    return literal(
        ed(colors(), A, COLOR),
        gens,
        Map.of(),
        List.of(
            eq(fk(F, gen("e1")), gen("d")),
            eq(fk(F, gen("e2")), gen("d")),
            eq(fk(F, gen("e3")), gen("d")),
            eq(att(A, gen("e1")), sym(RED))),
        true);
  }

  /**
   * An instance with the schema, algebra and decision procedure of base, but with the given
   * generators, labelled nulls and equations, and no validation on construction. With eqs null,
   * the equations are the ones Instance derives from the algebra.
   */
  private static final class Given
      extends Instance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>> {

    private final Instance<
            String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
        base;
    private final IMap<String, String> gens;
    private final IMap<String, String> sks;
    private final List<
            Pair<
                Term<String, String, Sym, Fk, Att, String, String>,
                Term<String, String, Sym, Fk, Att, String, String>>>
        eqs;
    private final boolean requireConsistency;

    Given(
        Instance<
                String,
                String,
                Sym,
                Fk,
                Att,
                String,
                String,
                Integer,
                Chc<String, Pair<Integer, Att>>>
            base,
        Map<String, String> gens,
        Map<String, String> sks,
        List<
                Pair<
                    Term<String, String, Sym, Fk, Att, String, String>,
                    Term<String, String, Sym, Fk, Att, String, String>>>
            eqs,
        boolean requireConsistency) {
      this.base = base;
      this.gens = mapToIMap(gens);
      this.sks = mapToIMap(sks);
      this.eqs = eqs;
      this.requireConsistency = requireConsistency;
    }

    Given(
        Instance<
                String,
                String,
                Sym,
                Fk,
                Att,
                String,
                String,
                Integer,
                Chc<String, Pair<Integer, Att>>>
            base) {
      this(base, null);
    }

    Given(
        Instance<
                String,
                String,
                Sym,
                Fk,
                Att,
                String,
                String,
                Integer,
                Chc<String, Pair<Integer, Att>>>
            base,
        List<
                Pair<
                    Term<String, String, Sym, Fk, Att, String, String>,
                    Term<String, String, Sym, Fk, Att, String, String>>>
            eqs) {
      this(base, copy(base.gens()), copy(base.sks()), eqs, true);
    }

    private static Map<String, String> copy(IMap<String, String> m) {
      Map<String, String> ret = new LinkedHashMap<>();
      m.entrySet(ret::put);
      return ret;
    }

    @Override
    public Schema<String, String, Sym, Fk, Att> schema() {
      return base.schema();
    }

    @Override
    public IMap<String, String> gens() {
      return gens;
    }

    @Override
    public IMap<String, String> sks() {
      return sks;
    }

    @Override
    public boolean requireConsistency() {
      return requireConsistency;
    }

    @Override
    public boolean allowUnsafeJava() {
      return true;
    }

    @Override
    public DP<String, String, Sym, Fk, Att, String, String> dp() {
      return base.dp();
    }

    @Override
    public Algebra<
            String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
        algebra() {
      return base.algebra();
    }

    @Override
    public synchronized void eqs(
        BiConsumer<
                Term<String, String, Sym, Fk, Att, String, String>,
                Term<String, String, Sym, Fk, Att, String, String>>
            f) {
      if (eqs == null) {
        super.eqs(f);
      } else {
        eqs.forEach(e -> f.accept(e.first, e.second));
      }
    }
  }

  private static Set<
          Pair<
              Term<String, String, Sym, Fk, Att, String, String>,
              Term<String, String, Sym, Fk, Att, String, String>>>
      eqsOf(
          Instance<
                  String,
                  String,
                  Sym,
                  Fk,
                  Att,
                  String,
                  String,
                  Integer,
                  Chc<String, Pair<Integer, Att>>>
              i) {
    Set<
            Pair<
                Term<String, String, Sym, Fk, Att, String, String>,
                Term<String, String, Sym, Fk, Att, String, String>>>
        ret = new HashSet<>();
    i.eqs((l, r) -> ret.add(new Pair<>(l, r)));
    return ret;
  }

  private static <X> List<X> drain(Iterator<X> it) {
    List<X> ret = new ArrayList<>();
    it.forEachRemaining(ret::add);
    return ret;
  }

  private static String message(Executable e) {
    return assertThrows(RuntimeException.class, e).getMessage();
  }

  @Nested
  class Basics {

    @Test
    void isAnInstanceWithOneRowPerGenerator() {
      var i = threeEs();
      assertEquals(Kind.INSTANCE, i.kind());
      assertEquals(4, i.size());
      assertEquals(3, i.algebra().size(E));
      assertEquals(1, i.algebra().size(D));
    }

    @Test
    void typesItsTerms() {
      var i = threeEs();
      assertEquals(Chc.inRight(E), i.type(gen("e1")));
      assertEquals(Chc.inRight(D), i.type(fk(F, gen("e1"))));
      assertEquals(Chc.inLeft(COLOR), i.type(att(A, gen("e2"))));
    }

    @Test
    void reprTGivesTheTermOfALabelledNull() {
      var i = threeEs();
      Integer e2 = i.algebra().gen("e2");
      assertEquals(att(A, gen("e2")), i.reprT(Term.Sk(Chc.inRight(new Pair<>(e2, A)))));
    }
  }

  /** The equations Instance derives from the algebra, through {@link Given} with no list. */
  @Nested
  class Equations {

    /**
     * The derived equations hold in the instance, and an instance presented by them has the same
     * rows and proves the equations it was written with. Which term represents a row is the
     * algebra's choice (here e1.f for d), so the tests do not fix it.
     */
    private void presents(
        LiteralInstance<
                String,
                String,
                Sym,
                Fk,
                Att,
                String,
                String,
                Integer,
                Chc<String, Pair<Integer, Att>>>
            i,
        List<
                Pair<
                    Term<String, String, Sym, Fk, Att, String, String>,
                    Term<String, String, Sym, Fk, Att, String, String>>>
            written,
        int count) {
      var derived = eqsOf(new Given(i));
      assertEquals(count, derived.size(), "" + derived);
      for (var e : derived) {
        assertTrue(i.dp().eq(null, e.first, e.second), e.toString());
      }
      Map<String, String> gens = new LinkedHashMap<>();
      i.gens().entrySet(gens::put);
      Map<String, String> sks = new LinkedHashMap<>();
      i.sks().entrySet(sks::put);
      var j = literal(i.schema(), gens, sks, new ArrayList<>(derived), false);
      assertEquals(i.size(), j.size());
      for (var e : written) {
        assertTrue(j.dp().eq(null, e.first, e.second), e.toString());
      }
    }

    @Test
    void presentEveryForeignKeyAndEveryKnownAttribute() {
      var i = threeEs();
      List<
              Pair<
                  Term<String, String, Sym, Fk, Att, String, String>,
                  Term<String, String, Sym, Fk, Att, String, String>>>
          written = new ArrayList<>();
      i.eqs((l, r) -> written.add(eq(l, r)));
      presents(i, written, 4);
      assertTrue(eqsOf(new Given(i)).contains(eq(att(A, gen("e1")), sym(RED))));
    }

    @Test
    void presentAGeneratorThatIsNotItsOwnRepresentative() {
      Map<String, String> gens = new LinkedHashMap<>();
      gens.put("e1", E);
      gens.put("d", D);
      gens.put("d2", D);
      List<
              Pair<
                  Term<String, String, Sym, Fk, Att, String, String>,
                  Term<String, String, Sym, Fk, Att, String, String>>>
          written = List.of(eq(fk(F, gen("e1")), gen("d")), eq(gen("d2"), gen("d")));
      presents(literal(ed(colors(), A, COLOR), gens, Map.of(), written, true), written, 2);
    }

    @Test
    void stateTheValueOfADeclaredLabelledNull() {
      var i =
          literal(
              ed(colors(), A, COLOR),
              Map.of("e1", E, "d", D),
              Map.of("y", COLOR),
              List.of(eq(fk(F, gen("e1")), gen("d")), eq(sk("y"), sym(GREEN))),
              true);
      assertTrue(eqsOf(new Given(i)).contains(eq(sym(GREEN), sk("y"))));
    }

    @Test
    void stateTheTypeAlgebraEquations() {
      var i =
          literal(
              ed(typeSide(RED, GREEN, MIX), A, COLOR),
              Map.of("e1", E, "e2", E, "d", D),
              Map.of(),
              List.of(
                  eq(fk(F, gen("e1")), gen("d")),
                  eq(fk(F, gen("e2")), gen("d")),
                  eq(sym(MIX, att(A, gen("e1"))), sym(MIX, att(A, gen("e2"))))),
              false);
      Set<
              Pair<
                  Term<String, String, Sym, Fk, Att, String, String>,
                  Term<String, String, Sym, Fk, Att, String, String>>>
          eqs = eqsOf(new Given(i));
      assertEquals(3, eqs.size(), "" + eqs);
      assertTrue(
          eqs.stream().anyMatch(e -> e.first.sym() == MIX && e.second.sym() == MIX), "" + eqs);
    }
  }

  @Nested
  class Model {

    @Test
    void holdsTheConstantsAndTheLabelledNulls() {
      var i = threeEs();
      assertEquals(
          Set.of(sym(RED), sym(GREEN), att(A, gen("e2")), att(A, gen("e3"))),
          i.getModel().get(COLOR));
      assertSame(i.getModel(), i.getModel());
    }

    @Test
    void enumeratesAJavaTypeItCan() {
      TypeSide<String, Sym> ts =
          new TypeSide<>(
              new HashSet<>(Set.of(BOOL)),
              new LinkedHashMap<>(),
              new HashSet<>(),
              Map.of(BOOL, "java.lang.Boolean"),
              Map.of(BOOL, "x => java.lang.Boolean.parseBoolean(x)"),
              Map.of(),
              ops());
      var i =
          literal(
              ed(ts, B, BOOL),
              Map.of("e1", E, "d", D),
              Map.of(),
              List.of(eq(fk(F, gen("e1")), gen("d"))),
              true);
      assertEquals(
          Set.of(Term.Obj(true, BOOL), Term.Obj(false, BOOL), att(B, gen("e1"))),
          i.getModel().get(BOOL));
    }

    @Test
    void cannotEnumerateTheSqlTypes() {
      Att name = Att.Att(E, "name");
      var i =
          literal(
              sqlSchema(List.of(E), Map.of(name, E), Map.of()),
              Map.of("e1", E),
              Map.of(),
              List.of(),
              true);
      assertTrue(message(i::getModel).startsWith("Cannot enumerate: java.util.Optional"));
    }

    @Test
    void talgNFPicksTheModelTermOfAValue() {
      var i = threeEs();
      assertEquals(sym(RED), i.talgNF(att(A, gen("e1"))));
      assertEquals(att(A, gen("e2")), i.talgNF(att(A, gen("e2"))));
    }

    @Test
    void enOrTyIteratesTheRowsOfAnEntityAndTheModelOfAType() {
      var i = threeEs();
      assertEquals(3, drain(i.enOrTy(Chc.inLeft(E), false)).size());
      assertTrue(drain(i.enOrTy(Chc.inLeft(E), true)).stream().allMatch(x -> x.left));
      var model = drain(i.enOrTy(Chc.inRight(COLOR), false));
      assertEquals(4, model.size());
      assertTrue(model.stream().noneMatch(x -> x.left));
    }

    /** The filter compares the type of each labelled null with the Chc, which never matches. */
    @Test
    void enOrTyFindsNoActiveDomainForAType() {
      var i = threeEs();
      assertEquals(2, i.algebra().talg().sks.size());
      assertEquals(List.of(), drain(i.enOrTy(Chc.inRight(COLOR), true)));
    }
  }

  @Nested
  class Validation {

    @Test
    void acceptsAConsistentInstance() {
      var i = threeEs();
      i.validate();
      i.validateMore();
    }

    @Test
    void requireConsistencyRejectsATypeAlgebraWithEquations() {
      Map<String, String> gens = Map.of("e1", E, "e2", E, "d", D);
      List<
              Pair<
                  Term<String, String, Sym, Fk, Att, String, String>,
                  Term<String, String, Sym, Fk, Att, String, String>>>
          eqs =
              List.of(
                  eq(fk(F, gen("e1")), gen("d")),
                  eq(fk(F, gen("e2")), gen("d")),
                  eq(sym(MIX, att(A, gen("e1"))), sym(MIX, att(A, gen("e2")))));
      Schema<String, String, Sym, Fk, Att> s = ed(typeSide(RED, GREEN, MIX), A, COLOR);
      String m = message(() -> literal(s, gens, Map.of(), eqs, true));
      assertTrue(
          m.startsWith(
              "Not necessarily conservative over the typeside (type algebra has equations)."),
          m);
      literal(s, gens, Map.of(), eqs, false).validate();
    }

    @Test
    void rejectAGeneratorOfAnUndeclaredEntity() {
      var i = new Given(threeEs(), Map.of("g", "X"), Map.of(), List.of(), true);
      assertEquals(
          "On generator g, the entity X is not declared.  Available:\n" + i.schema().ens,
          message(i::validateNoTalg));
      assertEquals("On generator g, the entity X is not declared.", message(i::validateMore));
    }

    @Test
    void rejectALabelledNullOfAnUndeclaredType() {
      var i = new Given(threeEs(), Map.of(), Map.of("y", "Money"), List.of(), true);
      String prefix = "On labelled null y, the type Money is not declared.\n\n";
      assertTrue(message(i::validateNoTalg).startsWith(prefix));
      assertTrue(message(i::validateMore).startsWith(prefix));
    }

    @Test
    void rejectAnEquationBetweenSorts() {
      var i = new Given(threeEs(), List.of(eq(att(A, gen("e1")), gen("d"))));
      assertEquals(
          "In instance equation e1.a = d, lhs sort is Color but rhs sort is D",
          message(i::validateNoTalg));
      assertEquals("Anomaly: please report", message(i::validateMore));
    }

    @Test
    void validateMoreRejectsAnEquationTheProverDoesNotHold() {
      var rows = new Given(threeEs(), List.of(eq(gen("e1"), gen("e2"))));
      assertEquals("Anomaly: not equal: e1 and e2", message(rows::validateMore));
      var values = new Given(threeEs(), List.of(eq(att(A, gen("e1")), sym(GREEN))));
      assertEquals("Anomaly: not equal: e1.a and green", message(values::validateMore));
    }

    /** Over a typeside with java functions, such as sql, type-sorted equations are not checked. */
    @Test
    void validateMoreSkipsValuesOverATypesideWithJavaFunctions() {
      Att name = Att.Att(E, "name");
      var base =
          literal(
              sqlSchema(List.of(E), Map.of(name, E), Map.of()),
              Map.of("e1", E),
              Map.of(),
              List.of(),
              true);
      var i =
          new Given(
              base,
              Map.of("e1", E),
              Map.of(),
              List.of(
                  eq(
                      att(name, gen("e1")),
                      Term.Obj(Optional.of("Bob"), CqlTestHelpers.VARCHAR))),
              true);
      i.validateMore();
    }
  }

  @Nested
  class CollageAndEquality {

    @Test
    void collageHoldsTheGeneratorsLabelledNullsAndEquations() {
      var i = threeEs();
      Collage<String, String, Sym, Fk, Att, String, String> col = i.collage();
      assertSame(col, i.collage());
      assertEquals(Map.of("e1", E, "e2", E, "e3", E, "d", D), col.gens());
      assertTrue(col.sks().isEmpty());
      Set<
              Pair<
                  Term<String, String, Sym, Fk, Att, String, String>,
                  Term<String, String, Sym, Fk, Att, String, String>>>
          eqs = new HashSet<>();
      for (Eq<String, String, Sym, Fk, Att, String, String> e : col.eqs()) {
        assertTrue(e.ctx.isEmpty());
        eqs.add(new Pair<>(e.lhs, e.rhs));
      }
      assertEquals(eqsOf(i), eqs);
      assertEquals(col.hashCode(), i.hashCode());
    }

    @Test
    void equalsComparesCollagesOnceBothAreBuilt() {
      var a = threeEs();
      var b = threeEs();
      assertTrue(a.equals(a));
      assertFalse(a.equals(null));
      assertThrows(NullPointerException.class, () -> a.equals(b));
      a.hashCode();
      b.hashCode();
      assertTrue(a.equals(b));
    }

    /** Issue 1: the argument is cast to Instance without a type check. */
    @Test
    void equalsThrowsOnAnotherKindOfObject() {
      var a = threeEs();
      Object other = "e1";
      assertThrows(ClassCastException.class, () -> a.equals(other));
    }

    @Test
    void toStringListsTheGeneratorsAndTheEquationsOnce() {
      var i = threeEs();
      String s = i.toString();
      assertTrue(s.startsWith("generators\n\te1:E\te2:E\te3:E\td:D\nequations"), s);
      assertTrue(s.contains("e1.a = red"), s);
      assertSame(s, i.toString());
    }

    @Test
    void toStringListsTheLabelledNulls() {
      var i =
          literal(
              ed(colors(), A, COLOR),
              Map.of("e1", E, "d", D),
              Map.of("y", COLOR),
              List.of(eq(fk(F, gen("e1")), gen("d"))),
              true);
      assertTrue(i.toString().startsWith("generators\n\t"));
      assertTrue(i.toString().contains("y:Color"), i.toString());
    }
  }

  @Nested
  class Unfreezing {

    private LiteralInstance<
            String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
        oneValue() {
      return literal(
          ed(colors(), A, COLOR),
          Map.of("e1", E, "d", D),
          Map.of("y", COLOR),
          List.of(eq(fk(F, gen("e1")), gen("d")), eq(att(A, gen("e1")), sk("y"))),
          true);
    }

    @Test
    void numbersTheGeneratorsBeforeTheLabelledNulls() {
      var i = oneValue();
      Triple<
              LinkedHashMap<String, Chc<String, String>>,
              Collection<Eq<String, String, Sym, Fk, Att, String, String>>,
              AqlOptions>
          u = i.unfreeze(ops());
      assertEquals(Set.of("v0", "v1", "v2"), u.first.keySet());
      assertEquals(Chc.inRight(COLOR), u.first.get("v2"));
      assertEquals(
          Set.of(Chc.inLeft(E), Chc.inLeft(D)), Set.of(u.first.get("v0"), u.first.get("v1")));
      assertSame(ops(), u.third);
      String e1 = u.first.get("v0").equals(Chc.inLeft(E)) ? "v0" : "v1";
      String d = e1.equals("v0") ? "v1" : "v0";
      Set<
              Pair<
                  Term<String, String, Sym, Fk, Att, String, String>,
                  Term<String, String, Sym, Fk, Att, String, String>>>
          eqs = new HashSet<>();
      for (var e : u.second) {
        assertTrue(e.ctx.isEmpty());
        eqs.add(new Pair<>(e.lhs, e.rhs));
      }
      assertEquals(Set.of(eq(fk(F, gen(e1)), gen(d)), eq(att(A, gen(e1)), sk("v2"))), eqs);
    }

    @Test
    void reusesTheFirstResultWithTheNewOptions() {
      var i = oneValue();
      var first = i.unfreeze(ops());
      AqlOptions other = new AqlOptions(ops(), AqlOption.timeout, 1L);
      var second = i.unfreeze(other);
      assertSame(first.first, second.first);
      assertSame(first.second, second.second);
      assertSame(other, second.third);
      assertNotSame(first, second);
    }

    @Test
    void unfreezesATermByTheSameNumbering() {
      var i = oneValue();
      var u = i.unfreeze(ops());
      String e1 = u.first.get("v0").equals(Chc.inLeft(E)) ? "v0" : "v1";
      assertEquals(att(A, gen(e1)), i.unfreeze(att(A, gen("e1"))));
      assertEquals(sk("v2"), i.unfreeze(sk("y")));
    }

    @Test
    void unfreezingATermFirstThrows() {
      var i = oneValue();
      assertThrows(NullPointerException.class, () -> i.unfreeze(gen("e1")));
    }
  }

  /** Sample, which the viewer shows with gui_sample_size (default 2). */
  @Nested
  class Sampling {

    @Test
    void showsOneMoreRowAttributeAndEntityThanItsBounds() {
      List<String> ens = List.of("Blank", "Row", "Solo", "Tail");
      Map<Att, Pair<String, String>> atts = new LinkedHashMap<>();
      for (int k = 1; k <= 4; k++) {
        atts.put(Att.Att("Row", "r" + k), new Pair<>("Row", COLOR));
      }
      Map<String, String> gens = new LinkedHashMap<>();
      gens.put("x1", "Row");
      gens.put("x2", "Row");
      gens.put("x3", "Row");
      gens.put("s", "Solo");
      gens.put("t", "Tail");
      var i = literal(schema(colors(), ens, atts, Map.of()), gens, Map.of(), List.of(), true);
      String s = i.sample(1);
      assertFalse(s.contains("Blank"), s);
      assertTrue(s.startsWith("Row (3 rows)\n"), s);
      assertTrue(s.contains("\n\nSolo (1 rows)\n"), s);
      assertFalse(s.contains("Tail"), s);
      assertEquals(3, s.split("ID: ", -1).length - 1, s);
      assertEquals(6, s.lines().filter(l -> l.startsWith("r")).count(), s);
    }
  }

  @Nested
  class Maps {

    @Test
    void mapToIMapDelegatesToTheMap() {
      Map<String, Integer> m = new LinkedHashMap<>();
      m.put("a", 1);
      m.put("b", 2);
      IMap<String, Integer> im = Instance.mapToIMap(m);
      assertEquals(2, im.get("b"));
      assertTrue(im.containsKey("a"));
      assertEquals(2, im.size());
      assertFalse(im.isEmpty());
      assertEquals("{a=1, b=2}", im.toString());
      assertEquals("a:1, b:2", im.sep(":", ", ", Object::toString));
      List<String> keys = new ArrayList<>();
      im.keySet(keys::add);
      List<Integer> values = new ArrayList<>();
      im.values(values::add);
      assertEquals(List.of("a", "b"), keys);
      assertEquals(List.of(1, 2), values);
      Map<String, Integer> copy = new HashMap<>();
      im.putAll(copy);
      assertEquals(m, copy);
      im.put("c", 3);
      assertEquals(1, im.remove("a"));
      assertEquals(Map.of("b", 2, "c", 3), m);
      List<String> seen = new ArrayList<>();
      im.forEach((k, v) -> seen.add(k + v));
      assertEquals(List.of("b2", "c3"), seen);
    }

    @Test
    void transformValuesMapsAndFiltersWithoutCopying() {
      Map<String, Integer> m = new LinkedHashMap<>();
      m.put("a", 1);
      m.put("b", 2);
      IMap<String, Integer> all =
          Instance.transformValues(Instance.mapToIMap(m), (k, v) -> v * 10, null, -1);
      assertEquals(20, all.get("b"));
      assertTrue(all.containsKey("a"));
      assertEquals(2, all.size());
      assertFalse(all.isEmpty());
      IMap<String, Integer> some =
          Instance.transformValues(
              Instance.mapToIMap(m), (k, v) -> v * 10, k -> k.equals("b"), 1);
      List<String> seen = new ArrayList<>();
      some.entrySet((k, v) -> seen.add(k + v));
      assertEquals(List.of("b20"), seen);
      assertEquals(1, some.size());
      assertFalse(some.isEmpty());
      assertTrue(
          Instance.transformValues(Instance.mapToIMap(m), (k, v) -> v, k -> false, 0).isEmpty());
      m.clear();
      assertTrue(all.isEmpty());
    }

    /** Its only caller (SigmaChaseAlgebra.java:48) passes no predicate. */
    @Test
    void transformValuesFiltersOnlyItsEntries() {
      IMap<String, Integer> some =
          Instance.transformValues(
              Instance.mapToIMap(Map.of("a", 1, "b", 2)), (k, v) -> v * 10, k -> k.equals("b"), 1);
      assertTrue(some.containsKey("a"));
      assertEquals(10, some.get("a"));
    }

    @Test
    void transformValuesIsReadOnly() {
      IMap<String, Integer> t =
          Instance.transformValues(
              Instance.mapToIMap(new HashMap<String, Integer>()), (k, v) -> v, null, -1);
      assertEquals("Anomaly: please report", message(() -> t.put("a", 1)));
      assertEquals("Anomaly: please report", message(() -> t.remove("a")));
    }

    @Test
    void imapToMapNoScanReadsThroughTheIMap() {
      Map<String, Integer> m = new LinkedHashMap<>();
      m.put("a", 1);
      Map<String, Integer> view = Instance.imapToMapNoScan(Instance.mapToIMap(m));
      assertEquals(1, view.size());
      assertFalse(view.isEmpty());
      assertTrue(view.containsKey("a"));
      assertEquals(1, view.get("a"));
      assertEquals(Set.of("a"), view.keySet());
      assertEquals("{a=1}", view.toString());
      List<String> seen = new ArrayList<>();
      view.forEach((k, v) -> seen.add(k + v));
      assertEquals(List.of("a1"), seen);
    }

    @Test
    void imapToMapNoScanRejectsEverythingElse() {
      Map<String, Integer> view = Instance.imapToMapNoScan(Instance.mapToIMap(new HashMap<>()));
      for (Executable e :
          List.<Executable>of(
              () -> view.containsValue(1),
              () -> view.put("a", 1),
              () -> view.remove("a"),
              () -> view.putAll(Map.of()),
              view::clear,
              view::values,
              view::entrySet)) {
        assertEquals("Anomaly: please report", message(e));
      }
    }
  }
}
