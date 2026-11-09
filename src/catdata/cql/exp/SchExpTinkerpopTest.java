package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Unit;
import catdata.cql.Constraints;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.SchExp.SchExpVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SchExpTinkerpop} and {@link EdsExpTinkerpop}: {@code schema S = tinkerpop},
 * the schema of every TinkerPop graph, and {@code constraints C = tinkerpop}, which makes vertex
 * and edge ids keys.
 */
class SchExpTinkerpopTest {

  private static final String PROGRAM =
      """
      schema S = tinkerpop
      constraints C = tinkerpop
      """;

  @Test
  void declaresVerticesEdgesAndProperties() throws Exception {
    Schema<String, String, Sym, Fk, Att> s = ExpTestHelpers.eval(PROGRAM, "S", Kind.SCHEMA);
    assertEquals(Set.of("Vertex", "Edge", "HasVertexProperty", "HasEdgeProperty"), s.ens);
    assertEquals(4, s.fks.size());
    assertEquals(8, s.atts.size());
    assertEquals(Set.of("Dom"), Set.copyOf(s.typeSide.tys));
  }

  @Test
  void makesIdsKeys() throws Exception {
    Constraints c = ExpTestHelpers.eval(PROGRAM, "C", Kind.CONSTRAINTS);
    assertEquals(2, c.eds.size());
    assertEquals(new SchExpTinkerpop(), new EdsExpTinkerpop().type(new AqlTyping()));
  }

  @Test
  void schemaIsTypedByTheRdfTypeside() {
    assertEquals(new TyExpRdf(), new SchExpTinkerpop().type(new AqlTyping()));
  }

  @Test
  void bothPrintAsTheKeyword() {
    assertEquals("tinkerpop", new SchExpTinkerpop().toString());
    assertEquals("tinkerpop", new EdsExpTinkerpop().toString());
  }

  @Test
  void bothHaveNoDependenciesOrOptions() {
    for (Exp<?> e : List.<Exp<?>>of(new SchExpTinkerpop(), new EdsExpTinkerpop())) {
      assertTrue(e.deps().isEmpty());
      assertTrue(e.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, e.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      e.map(seen::add);
      assertEquals(List.of(e), seen);
    }
  }

  @Test
  void allInstancesAreEqual() {
    assertEquals(new SchExpTinkerpop(), new SchExpTinkerpop());
    assertEquals(new SchExpTinkerpop().hashCode(), new SchExpTinkerpop().hashCode());
    assertNotEquals(new SchExpTinkerpop(), null);
    assertNotEquals(new SchExpTinkerpop(), "tinkerpop");
    EdsExpTinkerpop e = new EdsExpTinkerpop();
    assertEquals(e, e);
    assertEquals(e, new EdsExpTinkerpop());
    assertEquals(e.hashCode(), new EdsExpTinkerpop().hashCode());
    assertNotEquals(e, null);
    assertNotEquals(e, "tinkerpop");
  }

  @Test
  void schemaResolvesToItself() {
    SchExpTinkerpop s = new SchExpTinkerpop();
    assertSame(s, s.resolve(new AqlTyping(), null));
  }

  @Test
  @SuppressWarnings("unchecked")
  void bothAcceptAVisitor() {
    SchExpTinkerpop s = new SchExpTinkerpop();
    SchExpVisitor<String, Unit, RuntimeException> v = mock(SchExpVisitor.class);
    when(v.visit(Unit.unit, s)).thenReturn("schema");
    assertEquals("schema", s.accept(Unit.unit, v));
    EdsExpTinkerpop e = new EdsExpTinkerpop();
    EdsExpVisitor<String, Unit, RuntimeException> w = mock(EdsExpVisitor.class);
    when(w.visit(Unit.unit, e)).thenReturn("constraints");
    assertEquals("constraints", e.accept(Unit.unit, w));
  }

  /** Coaccept calls the visitor's method for the rdf schema. */
  @Test
  void schemaCoacceptsAsTheRdfSchema() {
    assertEquals(
        new SchExpRdf(new TyExpRdf()),
        new SchExpTinkerpop().coaccept(Unit.unit, new TypingInversion(), new AqlTyping()));
  }
}
