package catdata.cql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.Collage.CCollage;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import catdata.cql.fdm.InitialAlgebra;
import catdata.cql.fdm.LiteralInstance;

/** Fixtures shared by the tests of the catdata.cql package. */
class CqlTestHelpers {

  static final String VARCHAR = "Varchar";
  static final String BOOLEAN = "Boolean";

  static AqlOptions ops() {
    return AqlOptions.initialOptions;
  }

  /**
   * {@link SqlTypeSide}, built once per test JVM: constructing it spins up the JS engine and a
   * prover, which is slow.
   */
  static TypeSide<String, Sym> sqlTypeSide() {
    return TypeSideHolder.INSTANCE;
  }

  private static final class TypeSideHolder {
    private static final TypeSide<String, Sym> INSTANCE = SqlTypeSide.SqlTypeSide(ops());
  }

  /** A schema over {@link #sqlTypeSide()} whose attributes are all Varchar, keyed to their entity. */
  static Schema<String, String, Sym, Fk, Att> sqlSchema(
      List<String> ens, Map<Att, String> atts, Map<Fk, Pair<String, String>> fks) {
    return sqlSchema(ens, atts, fks, List.of());
  }

  static Schema<String, String, Sym, Fk, Att> sqlSchema(
      List<String> ens,
      Map<Att, String> atts,
      Map<Fk, Pair<String, String>> fks,
      List<Eq<String, String, Sym, Fk, Att, Void, Void>> eqs) {
    Map<Att, Pair<String, String>> typed = new HashMap<>();
    atts.forEach((a, en) -> typed.put(a, new Pair<>(en, VARCHAR)));
    return typedSqlSchema(ens, typed, fks, eqs);
  }

  /** A schema over {@link #sqlTypeSide()} whose attributes are given as (entity, type). */
  static Schema<String, String, Sym, Fk, Att> typedSqlSchema(
      List<String> ens,
      Map<Att, Pair<String, String>> atts,
      Map<Fk, Pair<String, String>> fks,
      List<Eq<String, String, Sym, Fk, Att, Void, Void>> eqs) {
    TypeSide<String, Sym> ts = sqlTypeSide();
    Collage<String, String, Sym, Fk, Att, Void, Void> col = new CCollage<>();
    col.tys().addAll(ts.tys);
    col.syms().putAll(ts.syms);
    col.java_tys().putAll(ts.js.java_tys);
    col.getEns().addAll(ens);
    col.atts().putAll(atts);
    col.fks().putAll(fks);
    col.eqs().addAll(eqs);
    return new Schema<>(ts, col, ops());
  }

  /** Decides equations by syntactic equality, which is exact for a free theory. */
  static <T, E, S, F, A> DP<T, E, S, F, A, Void, Void> syntactic() {
    return new DP<>() {
      @Override
      public String toStringProver() {
        return "syntactic";
      }

      @Override
      public boolean eq(
          Map<String, Chc<T, E>> ctx,
          Term<T, E, S, F, A, Void, Void> lhs,
          Term<T, E, S, F, A, Void, Void> rhs) {
        return lhs.equals(rhs);
      }
    };
  }

  /** A schema equation in a single entity-sorted variable. */
  static Eq<String, String, Sym, Fk, Att, Void, Void> forall(
      String v,
      String en,
      Term<String, String, Sym, Fk, Att, Void, Void> l,
      Term<String, String, Sym, Fk, Att, Void, Void> r) {
    return new Eq<>(Map.of(v, Chc.inRight(en)), l, r);
  }

  /**
   * What {@code instance I = literal : S} evaluates to, as InstExpRaw builds it, with
   * require_consistency and allow_java_eqs_unsafe true.
   */
  static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      literal(
          Schema<String, String, Sym, Fk, Att> schema,
          Map<String, String> gens,
          List<
                  Pair<
                      Term<String, String, Sym, Fk, Att, String, String>,
                      Term<String, String, Sym, Fk, Att, String, String>>>
              eqs) {
    return literal(schema, gens, Map.of(), eqs);
  }

  /** {@link #literal(Schema, Map, List)} with labelled nulls declared as generators of types. */
  static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      literal(
          Schema<String, String, Sym, Fk, Att> schema,
          Map<String, String> gens,
          Map<String, String> sks,
          List<
                  Pair<
                      Term<String, String, Sym, Fk, Att, String, String>,
                      Term<String, String, Sym, Fk, Att, String, String>>>
              eqs) {
    Collage<String, String, Sym, Fk, Att, String, String> col = new CCollage<>(schema.collage());
    col.gens().putAll(gens);
    col.sks().putAll(sks);
    for (var e : eqs) {
      col.eqs().add(new Eq<>(null, e.first, e.second));
    }
    InitialAlgebra<String, String, Sym, Fk, Att, String, String> alg =
        new InitialAlgebra<>(ops(), schema, col, y -> y, (x, y) -> y);
    return new LiteralInstance<>(
        schema, col.gens(), col.sks(), eqs, alg.dp(), alg, true, true);
  }

  static final String COLOR = "Color";

  /** red, green : Color. */
  static final Sym RED = Sym.Sym("red", new Pair<>(List.of(), COLOR));

  static final Sym GREEN = Sym.Sym("green", new Pair<>(List.of(), COLOR));

  /** mix : Color -> Color. */
  static final Sym MIX = Sym.Sym("mix", new Pair<>(List.of(COLOR), COLOR));

  /** A typeside of one type, Color, without java types; free, so equality is syntactic. */
  static TypeSide<String, Sym> colors() {
    Map<Sym, Pair<List<String>, String>> syms = new LinkedHashMap<>();
    for (Sym s : List.of(RED, GREEN, MIX)) {
      syms.put(s, s.ty);
    }
    return new TypeSide<>(
        new HashSet<>(Set.of(COLOR)), syms, new HashSet<>(), Map.of(), Map.of(), Map.of(), ops());
  }

  /** A schema over {@link #colors()}. */
  static Schema<String, String, Sym, Fk, Att> colorSchema(
      List<String> ens, Map<Att, Pair<String, String>> atts, Map<Fk, Pair<String, String>> fks) {
    TypeSide<String, Sym> ts = colors();
    Collage<String, String, Sym, Fk, Att, Void, Void> col = new CCollage<>();
    col.tys().addAll(ts.tys);
    col.syms().putAll(ts.syms);
    col.getEns().addAll(ens);
    col.atts().putAll(atts);
    col.fks().putAll(fks);
    return new Schema<>(ts, col, ops());
  }

  @SafeVarargs
  static <T> T[] arr(T... xs) {
    return xs;
  }

  static final Fk SKELETON_F = Fk.Fk("E", "f");
  static final Att SKELETON_A = Att.Att("E", "a");

  /**
   * A skeleton over E and D, with f : E -> D and a : E -> Color. E has the rows 0, 1, 2 and D the
   * rows 3, 4; Color has the labelled nulls 5 and 6, each its own null. f sends 0, 1, 2 to 3, 4, 3,
   * and a sends them to 5, red and 6. talgEqs are the type algebra's equations, decided
   * syntactically.
   */
  static SkeletonExtensionalInstance<String, String, Sym, Fk, Att> skeleton(
      Term<String, Void, Sym, Void, Void, Void, Integer>[][] talgEqs) {
    Map<String, Integer> xs = new LinkedHashMap<>();
    xs.put("E", 3);
    xs.put("D", 2);
    Map<String, Integer> xo = new LinkedHashMap<>();
    xo.put("E", 0);
    xo.put("D", 3);
    BiPredicate<
            Term<String, Void, Sym, Void, Void, Void, Integer>,
            Term<String, Void, Sym, Void, Void, Void, Integer>>
        dp = (l, r) -> l.equals(r);
    return new SkeletonExtensionalInstance<>(
        colorSchema(
            List.of("E", "D"),
            Map.of(SKELETON_A, new Pair<>("E", COLOR)),
            Map.of(SKELETON_F, new Pair<>("E", "D"))),
        xs,
        Map.of(COLOR, 2),
        xo,
        Map.of(COLOR, 5),
        Map.of(SKELETON_F, new int[] {3, 4, 3}),
        Map.of(SKELETON_A, arr(Term.Sk(5), Term.Sym(RED, List.of()), Term.Sk(6))),
        talgEqs,
        dp,
        Chc::inLeft,
        ops());
  }

  /** {@link #skeleton(Term[][])} with a free type algebra. */
  static SkeletonExtensionalInstance<String, String, Sym, Fk, Att> skeleton() {
    return skeleton(arr());
  }
}
