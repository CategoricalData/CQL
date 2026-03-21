package catdata.cql.fdm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.cql.AqlJs;
import catdata.cql.AqlOptions;
import catdata.cql.DP;
import catdata.cql.Instance;
import catdata.cql.Mapping;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.TypeSide;

class FdmTestHelpers {

  static TypeSide<String, String> emptyTypeSide() {
    AqlJs<String, String> js = new AqlJs<>(
        AqlOptions.initialOptions,
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap());
    DP<String, Void, String, Void, Void, Void, Void> dp = new DP<>() {
      @Override
      public String toStringProver() { return "test"; }
      @Override
      public boolean eq(Map<String, Chc<String, Void>> ctx,
          Term<String, Void, String, Void, Void, Void, Void> lhs,
          Term<String, Void, String, Void, Void, Void, Void> rhs) {
        return lhs.equals(rhs);
      }
    };
    return new TypeSide<>(Collections.emptySet(), Collections.emptyMap(),
        Collections.emptySet(), js, dp, AqlOptions.initialOptions);
  }

  static TypeSide<String, String> simpleTypeSide() {
    Set<String> tys = new HashSet<>(Arrays.asList("String"));
    Map<String, String> javaTys = new HashMap<>();
    javaTys.put("String", "java.lang.String");
    Map<String, String> javaParsers = new HashMap<>();
    javaParsers.put("String", "x => x");
    AqlJs<String, String> js = new AqlJs<>(
        AqlOptions.initialOptions,
        Collections.emptyMap(),
        javaTys,
        javaParsers,
        Collections.emptyMap());
    DP<String, Void, String, Void, Void, Void, Void> dp = new DP<>() {
      @Override
      public String toStringProver() { return "test"; }
      @Override
      public boolean eq(Map<String, Chc<String, Void>> ctx,
          Term<String, Void, String, Void, Void, Void, Void> lhs,
          Term<String, Void, String, Void, Void, Void, Void> rhs) {
        return lhs.equals(rhs);
      }
    };
    return new TypeSide<>(tys, Collections.emptyMap(),
        Collections.emptySet(), js, dp, AqlOptions.initialOptions);
  }

  static Schema<String, String, String, String, String> emptySchema() {
    return Schema.terminal(emptyTypeSide());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  static Schema<String, String, String, String, String> singleEntitySchema() {
    TypeSide<String, String> ts = emptyTypeSide();
    DP dp = ts.semantics();
    return new Schema<>(ts, Collections.singleton("E"),
        Collections.emptyMap(), Collections.emptyMap(),
        Collections.emptySet(), dp, false);
  }

  static Schema<String, String, String, String, String> schemaWithAtt() {
    TypeSide<String, String> ts = simpleTypeSide();
    Map<String, Pair<String, String>> atts = new HashMap<>();
    atts.put("name", new Pair<>("E", "String"));
    DP<String, String, String, String, String, Void, Void> dp = new DP<>() {
      @Override
      public String toStringProver() { return "test"; }
      @Override
      public boolean eq(Map<String, Chc<String, String>> ctx,
          Term<String, String, String, String, String, Void, Void> lhs,
          Term<String, String, String, String, String, Void, Void> rhs) {
        return lhs.equals(rhs);
      }
    };
    return new Schema<>(ts, Collections.singleton("E"),
        atts, Collections.emptyMap(),
        Collections.emptySet(), dp, false);
  }

  static Schema<String, String, String, String, String> schemaWithFk() {
    TypeSide<String, String> ts = emptyTypeSide();
    Set<String> ens = new HashSet<>(Arrays.asList("A", "B"));
    Map<String, Pair<String, String>> fks = new HashMap<>();
    fks.put("f", new Pair<>("A", "B"));
    DP<String, String, String, String, String, Void, Void> dp = new DP<>() {
      @Override
      public String toStringProver() { return "test"; }
      @Override
      public boolean eq(Map<String, Chc<String, String>> ctx,
          Term<String, String, String, String, String, Void, Void> lhs,
          Term<String, String, String, String, String, Void, Void> rhs) {
        return lhs.equals(rhs);
      }
    };
    return new Schema<>(ts, ens, Collections.emptyMap(), fks,
        Collections.emptySet(), dp, false);
  }

  static Schema<String, String, String, String, String> schemaWithAttAndFk() {
    TypeSide<String, String> ts = simpleTypeSide();
    Set<String> ens = Collections.singleton("E");
    Map<String, Pair<String, String>> atts = new HashMap<>();
    atts.put("name", new Pair<>("E", "String"));
    Map<String, Pair<String, String>> fks = new HashMap<>();
    fks.put("f", new Pair<>("E", "E"));
    DP<String, String, String, String, String, Void, Void> dp = new DP<>() {
      @Override
      public String toStringProver() { return "test"; }
      @Override
      public boolean eq(Map<String, Chc<String, String>> ctx,
          Term<String, String, String, String, String, Void, Void> lhs,
          Term<String, String, String, String, String, Void, Void> rhs) {
        return lhs.equals(rhs);
      }
    };
    return new Schema<>(ts, ens, atts, fks,
        Collections.emptySet(), dp, false);
  }

  static Instance<String, String, String, String, String, String, String, String, String> emptyInstance(
      Schema<String, String, String, String, String> schema) {
    Map<String, Collection<String>> ensMap = new HashMap<>();
    Map<String, Collection<String>> tysMap = new HashMap<>();

    for (String en : schema.ens) {
      ensMap.put(en, Collections.emptyList());
    }
    for (String ty : schema.typeSide.tys) {
      tysMap.put(ty, Collections.emptyList());
    }

    ImportAlgebra<String, String, String, String, String, String, String> alg =
        new ImportAlgebra<>(schema,
            en -> ensMap.get(en), tysMap,
            (en, x) -> Collections.emptyMap(),
            (en, x) -> Collections.emptyMap(),
            (en, x) -> x, (ty, y) -> y,
            true, Collections.emptyList());

    return new SaturatedInstance<>(alg, alg, false, false, false, null);
  }

  static Instance<String, String, String, String, String, String, String, String, String> singleElementInstance() {
    Schema<String, String, String, String, String> schema = singleEntitySchema();
    Map<String, Collection<String>> ensMap = new HashMap<>();
    ensMap.put("E", Collections.singletonList("e1"));
    Map<String, Collection<String>> tysMap = new HashMap<>();

    ImportAlgebra<String, String, String, String, String, String, String> alg =
        new ImportAlgebra<>(schema,
            en -> ensMap.get(en), tysMap,
            (en, x) -> Collections.emptyMap(),
            (en, x) -> Collections.emptyMap(),
            (en, x) -> x, (ty, y) -> y,
            true, Collections.emptyList());

    return new SaturatedInstance<>(alg, alg, false, false, false, null);
  }

  static Instance<String, String, String, String, String, String, String, String, String> twoElementInstance() {
    Schema<String, String, String, String, String> schema = singleEntitySchema();
    Map<String, Collection<String>> ensMap = new HashMap<>();
    ensMap.put("E", Arrays.asList("e1", "e2"));
    Map<String, Collection<String>> tysMap = new HashMap<>();

    ImportAlgebra<String, String, String, String, String, String, String> alg =
        new ImportAlgebra<>(schema,
            en -> ensMap.get(en), tysMap,
            (en, x) -> Collections.emptyMap(),
            (en, x) -> Collections.emptyMap(),
            (en, x) -> x, (ty, y) -> y,
            true, Collections.emptyList());

    return new SaturatedInstance<>(alg, alg, false, false, false, null);
  }

  static Instance<String, String, String, String, String, String, String, String, String> instanceWithAtt() {
    Schema<String, String, String, String, String> schema = schemaWithAtt();
    Map<String, Collection<String>> ensMap = new HashMap<>();
    ensMap.put("E", Collections.singletonList("e1"));
    Map<String, Collection<String>> tysMap = new HashMap<>();
    tysMap.put("String", Collections.emptyList());

    Map<String, Term<String, Void, String, Void, Void, Void, String>> e1Atts = new HashMap<>();
    e1Atts.put("name", Term.Obj("Alice", "String"));

    ImportAlgebra<String, String, String, String, String, String, String> alg =
        new ImportAlgebra<>(schema,
            en -> ensMap.get(en), tysMap,
            (en, x) -> Collections.emptyMap(),
            (en, x) -> e1Atts,
            (en, x) -> x, (ty, y) -> y,
            true, Collections.emptyList());

    return new SaturatedInstance<>(alg, alg, false, false, false, null);
  }

  static Instance<String, String, String, String, String, String, String, String, String> instanceWithFk() {
    Schema<String, String, String, String, String> schema = schemaWithFk();
    Map<String, Collection<String>> ensMap = new HashMap<>();
    ensMap.put("A", Collections.singletonList("a1"));
    ensMap.put("B", Collections.singletonList("b1"));
    Map<String, Collection<String>> tysMap = new HashMap<>();

    Map<String, String> a1Fks = new HashMap<>();
    a1Fks.put("f", "b1");

    ImportAlgebra<String, String, String, String, String, String, String> alg =
        new ImportAlgebra<>(schema,
            en -> ensMap.get(en), tysMap,
            (en, x) -> {
              if (en.equals("A")) return a1Fks;
              return Collections.emptyMap();
            },
            (en, x) -> Collections.emptyMap(),
            (en, x) -> x, (ty, y) -> y,
            true, Collections.emptyList());

    return new SaturatedInstance<>(alg, alg, false, false, false, null);
  }

  static Mapping<String, String, String, String, String, String, String, String> identityMapping(
      Schema<String, String, String, String, String> schema) {
    Map<String, String> ens = new HashMap<>();
    for (String en : schema.ens) {
      ens.put(en, en);
    }
    Map<String, Pair<String, List<String>>> fks = new HashMap<>();
    for (String fk : schema.fks.keySet()) {
      fks.put(fk, new Pair<>(schema.fks.get(fk).first, Collections.singletonList(fk)));
    }
    Map<String, Triple<String, String, Term<String, String, String, String, String, Void, Void>>> atts = new HashMap<>();
    for (String att : schema.atts.keySet()) {
      atts.put(att, new Triple<>("x", schema.atts.get(att).first,
          Term.Att(att, Term.Var("x"))));
    }
    return new Mapping<>(ens, atts, fks, schema, schema, false);
  }
}
