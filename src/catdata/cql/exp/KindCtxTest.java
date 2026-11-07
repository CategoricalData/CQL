package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import catdata.cql.Kind;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Unit tests for {@link KindCtx}, the definitions of a program kept in one map per kind. */
class KindCtxTest {

  private static KindCtx<
          String,
          Object,
          Object,
          Object,
          Object,
          Object,
          Object,
          Object,
          Object,
          Object,
          Object,
          Object,
          Object,
          Object,
          Object,
          Object,
          Object>
      ctx() {
    return new KindCtx<>();
  }

  /** The map of each kind, as the fields name them. */
  private static final Map<
          Kind,
          Function<KindCtx<String, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>, Map<String, ?>>>
      FIELDS =
          Map.ofEntries(
              Map.entry(Kind.INSTANCE, c -> c.insts),
              Map.entry(Kind.MAPPING, c -> c.maps),
              Map.entry(Kind.SCHEMA, c -> c.schs),
              Map.entry(Kind.TRANSFORM, c -> c.trans),
              Map.entry(Kind.TYPESIDE, c -> c.tys),
              Map.entry(Kind.QUERY, c -> c.qs),
              Map.entry(Kind.PRAGMA, c -> c.ps),
              Map.entry(Kind.GRAPH, c -> c.gs),
              Map.entry(Kind.COMMENT, c -> c.cs),
              Map.entry(Kind.SCHEMA_COLIMIT, c -> c.scs),
              Map.entry(Kind.CONSTRAINTS, c -> c.eds),
              Map.entry(Kind.THEORY_MORPHISM, c -> c.tms),
              Map.entry(Kind.APG_instance, c -> c.apgis),
              Map.entry(Kind.APG_morphism, c -> c.apgms),
              Map.entry(Kind.APG_typeside, c -> c.apgts),
              Map.entry(Kind.APG_mapping, c -> c.apgmappings),
              Map.entry(Kind.APG_schema, c -> c.apgschemas));

  @ParameterizedTest
  @EnumSource(Kind.class)
  void storesEachKindInItsOwnMap(Kind kind) {
    var c = ctx();
    Object v = new Object();
    c.put("x", kind, v);
    assertSame(v, c.get("x", kind));
    assertSame(FIELDS.get(kind).apply(c), c.get(kind));
    assertEquals(Map.of("x", v), c.get(kind));
    assertEquals(1, c.size(kind));
    for (Kind other : Kind.values()) {
      if (other != kind) {
        assertEquals(0, c.size(other), other.toString());
      }
    }
  }

  /** Theory morphisms are stored, but keySet leaves them out. */
  @ParameterizedTest
  @EnumSource(Kind.class)
  void listsTheNamesOfEveryKindButTheoryMorphisms(Kind kind) {
    var c = ctx();
    c.put("x", kind, "v");
    assertEquals(kind != Kind.THEORY_MORPHISM, c.keySet().contains("x"));
  }

  @Test
  void listsEachNameOnce() {
    var c = ctx();
    c.put("x", Kind.QUERY, "q");
    c.put("x", Kind.SCHEMA, "s");
    c.put("y", Kind.INSTANCE, "i");
    assertEquals(Set.of("x", "y"), c.keySet());
  }

  /** Equality ignores theory morphisms, APG schemas and APG mappings. */
  @ParameterizedTest
  @EnumSource(Kind.class)
  void comparesEveryKindButThree(Kind kind) {
    var a = ctx();
    var b = ctx();
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    b.put("x", kind, "v");
    boolean ignored =
        kind == Kind.THEORY_MORPHISM || kind == Kind.APG_schema || kind == Kind.APG_mapping;
    assertEquals(ignored, a.equals(b), kind.toString());
    assertEquals(ignored, b.equals(a), kind.toString());
    if (ignored) {
      assertEquals(a.hashCode(), b.hashCode());
    }
  }

  @Test
  void isEqualToItselfAndNotToOtherObjects() {
    var a = ctx();
    assertEquals(a, a);
    assertNotEquals(a, null);
    assertFalse(a.equals("KindCtx"));
  }

  @Test
  void printsEveryMap() {
    var c = ctx();
    c.put("S", Kind.SCHEMA, "s");
    String s = c.toString();
    assertEquals(
        "KindCtx [gs=, tys=, schs=S:s, insts=, trans=, maps=, qs=, ps=, cs=, scs=, eds=, tms=,"
            + " apgts=, apgis=, apgms=, apgschemas=, apgmappings=]",
        s);
  }
}
