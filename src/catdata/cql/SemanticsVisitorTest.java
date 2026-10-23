package catdata.cql;

import static catdata.cql.CqlTestHelpers.ops;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import catdata.Unit;
import catdata.apg.ApgInstance;
import catdata.apg.ApgMapping;
import catdata.apg.ApgSchema;
import catdata.apg.ApgTransform;
import catdata.apg.ApgTypeside;
import catdata.cql.fdm.IdentityTransform;
import catdata.graph.DMG;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The default visit, which dispatches a definition's value on its kind, as the viewer does. */
class SemanticsVisitorTest {

  /** Answers with the name of the overload that was called. */
  private static final class Names implements SemanticsVisitor<String, Unit, RuntimeException> {

    @Override
    public <L> String visit(String k, Unit arg, ApgSchema<L> t) {
      return "apg schema " + k;
    }

    @Override
    public <L1, L2> String visit(String k, Unit arg, ApgMapping<L1, L2> t) {
      return "apg mapping " + k;
    }

    @Override
    public String visit(String k, Unit arg, ApgTypeside t) {
      return "apg typeside " + k;
    }

    @Override
    public <L, e> String visit(String k, Unit arg, ApgInstance<L, e> t) {
      return "apg instance " + k;
    }

    @Override
    public <l1, e1, l2, e2> String visit(String k, Unit arg, ApgTransform<l1, e1, l2, e2> t) {
      return "apg transform " + k;
    }

    @Override
    public <T, C, T0, C0> String visit(String k, Unit arg, Mor<T, C, T0, C0> m) {
      return "theory morphism " + k;
    }

    @Override
    public <T, C> String visit(String k, Unit arg, TypeSide<T, C> t) {
      return "typeside " + k;
    }

    @Override
    public <N> String visit(String k, Unit arg, ColimitSchema<N> s) {
      return "schema colimit " + k;
    }

    @Override
    public <Ty, En, Sym, Fk, Att> String visit(
        String k, Unit arg, Schema<Ty, En, Sym, Fk, Att> s) {
      return "schema " + k;
    }

    @Override
    public <Ty, En, Sym, Fk, Att> String visit(String k, Unit arg, Constraints s) {
      return "constraints " + k;
    }

    @Override
    public <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> String visit(
        String k, Unit arg, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i) {
      return "instance " + k;
    }

    @Override
    public <Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(
        String k,
        Unit arg,
        Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h) {
      return "transform " + k;
    }

    @Override
    public String visit(String k, Unit arg, Pragma p) {
      return "pragma " + k;
    }

    @Override
    public String visit(String k, Unit arg, Comment c) {
      return "comment " + k;
    }

    @Override
    public <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> String visit(
        String k, Unit arg, Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q) {
      return "query " + k;
    }

    @Override
    public <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> String visit(
        String k, Unit arg, Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> m) {
      return "mapping " + k;
    }

    @Override
    public <N, e> String visit(String k, Unit arg, Graph<N, e> g) {
      return "graph " + k;
    }
  }

  private static String dispatch(Semantics s) {
    return new Names().visit("x", Unit.unit, s);
  }

  /** A mock of c whose kind is the one given. */
  private static <S extends Semantics> S of(Class<S> c, Kind kind) {
    S s = mock(c);
    when(s.kind()).thenReturn(kind);
    return s;
  }

  @Nested
  class Dispatch {

    @Test
    void sendsTheCqlValuesToTheirOverloads() {
      TypeSide<String, String> ts = TypeSide.initial(ops());
      Schema<String, String, String, String, String> schema = Schema.terminal(ts);
      assertEquals("typeside x", dispatch(ts));
      assertEquals("schema x", dispatch(schema));
      assertEquals("comment x", dispatch(new Comment("c", false)));
      assertEquals("graph x", dispatch(new Graph<>(new DMG<String, String>(Set.of(), Map.of()))));
      var empty = CqlTestHelpers.sqlSchema(List.of("E"), Map.of(), Map.of());
      assertEquals("constraints x", dispatch(new Constraints(ops(), empty)));
      var i = CqlTestHelpers.literal(empty, Map.of(), List.of());
      assertEquals("instance x", dispatch(i));
      assertEquals("transform x", dispatch(new IdentityTransform<>(i, Optional.empty())));
      assertEquals(
          "pragma x",
          dispatch(
              new Pragma() {
                @Override
                public void execute() {}

                @Override
                public String toString() {
                  return "done";
                }
              }));
    }

    @Test
    void sendsTheOtherValuesToTheirOverloads() {
      assertEquals("query x", dispatch(of(Query.class, Kind.QUERY)));
      assertEquals("mapping x", dispatch(of(Mapping.class, Kind.MAPPING)));
      assertEquals("schema colimit x", dispatch(of(ColimitSchema.class, Kind.SCHEMA_COLIMIT)));
      assertEquals("apg instance x", dispatch(of(ApgInstance.class, Kind.APG_instance)));
      assertEquals("apg transform x", dispatch(of(ApgTransform.class, Kind.APG_morphism)));
      assertEquals("apg typeside x", dispatch(of(ApgTypeside.class, Kind.APG_typeside)));
      assertEquals("apg mapping x", dispatch(of(ApgMapping.class, Kind.APG_mapping)));
      assertEquals("apg schema x", dispatch(of(ApgSchema.class, Kind.APG_schema)));
    }

    /**
     * Issue 64: the case passes the value on as a Semantics, which calls this method again. No CQL
     * program can make a Mor, since MorExpRaw returns null.
     */
    @Test
    void loopsOnATheoryMorphism() {
      Mor<?, ?, ?, ?> m = of(Mor.class, Kind.THEORY_MORPHISM);
      assertThrows(StackOverflowError.class, () -> dispatch(m));
    }
  }
}
