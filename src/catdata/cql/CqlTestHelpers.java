package catdata.cql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
}
