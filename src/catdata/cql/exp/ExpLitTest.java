package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Constraints;
import catdata.cql.Instance;
import catdata.cql.Mapping;
import catdata.cql.Schema;
import catdata.cql.Transform;
import catdata.cql.TypeSide;
import catdata.cql.exp.EdsExp.EdsExpLit;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.InstExp.InstExpLit;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.MapExp.MapExpCoVisitor;
import catdata.cql.exp.MapExp.MapExpLit;
import catdata.cql.exp.MapExp.MapExpVisitor;
import catdata.cql.exp.SchExp.SchExpCoVisitor;
import catdata.cql.exp.SchExp.SchExpLit;
import catdata.cql.exp.SchExp.SchExpVisitor;
import catdata.cql.exp.TransExp.TransExpLit;
import catdata.cql.exp.TyExp.TyExpCoVisitor;
import catdata.cql.exp.TyExp.TyExpLit;
import catdata.cql.exp.TyExp.TyExpVisitor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the literal expressions {@code SchExpLit}, {@code TyExpLit}, {@code InstExpLit},
 * {@code MapExpLit}, {@code TransExpLit} and {@code EdsExpLit}, which wrap a value that is already
 * evaluated: they evaluate to it, compare and hash by it, and have no dependencies or options.
 */
class ExpLitTest {

  private static final String PROGRAM =
      """
      typeside Ty = literal { types String constants x : String }
      typeside Ty2 = literal { types Int }
      schema S = literal : Ty { entities A attributes n : A -> String }
      schema S2 = literal : Ty { entities B }
      instance I = literal : S { generators a : A equations a.n = x }
      instance I2 = literal : S { generators a b : A }
      mapping F = identity S
      mapping F2 = identity S2
      transform t = identity I
      transform t2 = identity I2
      constraints C = literal : S { }
      constraints C2 = literal : S2 { }
      """;

  private static AqlEnv env;

  @BeforeAll
  static void evaluate() throws Exception {
    env = ExpTestHelpers.run(PROGRAM).env;
  }

  @SuppressWarnings("unchecked")
  private static <X> X def(String name) {
    return (X) env.defs.get(name, env.prog.exps.get(name).kind());
  }

  /** The parts that every literal shares: no dependencies, options or children. */
  private static void assertInert(Exp<?> e) {
    assertTrue(e.deps().isEmpty());
    assertTrue(e.options().isEmpty());
    assertEquals(ExpTestHelpers.BASE_OPTIONS, e.allowedOptions());
    List<Exp<?>> seen = new ArrayList<>();
    e.map(seen::add);
    assertEquals(List.of(e), seen);
  }

  @Nested
  class Schemas {

    private final Schema<String, String, Sym, Fk, Att> s = def("S");
    private final SchExpLit lit = new SchExpLit(s);

    @Test
    void evaluatesToItsSchema() {
      assertSame(s, lit.eval0(env, false));
      assertEquals(new TyExpLit(s.typeSide), lit.type(new AqlTyping()));
      assertSame(lit, lit.resolve(new AqlTyping(), env.prog));
      assertTrue(lit.toString().startsWith("constant entities"), lit.toString());
      assertInert(lit);
    }

    @Test
    void equalityComparesTheSchema() {
      assertEquals(lit, lit);
      assertEquals(lit, new SchExpLit(s));
      assertEquals(s.hashCode(), lit.hashCode());
      assertNotEquals(lit, new SchExpLit(def("S2")));
      assertNotEquals(lit, null);
      assertNotEquals(lit, "constant");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitorAndACoVisitor() {
      SchExpVisitor<String, Unit, RuntimeException> v = mock(SchExpVisitor.class);
      when(v.visit(Unit.unit, lit)).thenReturn("visited");
      assertEquals("visited", lit.accept(Unit.unit, v));
      SchExpCoVisitor<Unit, Unit, RuntimeException> cv = mock(SchExpCoVisitor.class);
      when(cv.visitSchExpLit(Unit.unit, Unit.unit)).thenReturn(lit);
      assertEquals(lit, lit.coaccept(Unit.unit, cv, Unit.unit));
    }
  }

  @Nested
  class Typesides {

    private final TypeSide<String, Sym> t = def("Ty");
    private final TyExpLit lit = new TyExpLit(t);

    @Test
    void evaluatesToItsTypeside() {
      assertSame(t, lit.eval0(env, false));
      assertEquals(Unit.unit, lit.type(new AqlTyping()));
      assertTrue(lit.toString().startsWith("constant types"), lit.toString());
      assertInert(lit);
    }

    @Test
    void equalityComparesTheTypeside() {
      assertEquals(lit, lit);
      assertEquals(lit, new TyExpLit(t));
      assertEquals(t.hashCode(), lit.hashCode());
      assertNotEquals(lit, new TyExpLit(def("Ty2")));
      assertNotEquals(lit, null);
      assertNotEquals(lit, "constant");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitorAndACoVisitor() {
      TyExpVisitor<String, Unit, RuntimeException> v = mock(TyExpVisitor.class);
      when(v.visit(Unit.unit, lit)).thenReturn("visited");
      assertEquals("visited", lit.accept(Unit.unit, v));
      TyExpCoVisitor<Unit, Unit, RuntimeException> cv = mock(TyExpCoVisitor.class);
      when(cv.visitTyExpLit(Unit.unit, Unit.unit)).thenReturn(lit);
      assertEquals(lit, lit.coaccept(Unit.unit, cv, Unit.unit));
    }
  }

  @Nested
  class Instances {

    private final Instance<String, String, Sym, Fk, Att, String, String, String, String> i =
        def("I");
    private final InstExpLit<String, String, String, String> lit = new InstExpLit<>(i);

    @Test
    void evaluatesToItsInstance() {
      assertSame(i, lit.eval0(env, false));
      assertEquals(new SchExpLit(i.schema()), lit.type(new AqlTyping()));
      assertTrue(lit.direct(new AqlTyping()).isEmpty());
      assertTrue(lit.toString().startsWith("InstExpLit "), lit.toString());
      assertInert(lit);
    }

    /** Instance equality reads a field that hashCode fills in, so both are hashed first. */
    @Test
    void equalityComparesTheInstance() {
      InstExpLit<String, String, String, String> same = new InstExpLit<>(i);
      InstExpLit<String, String, String, String> other = new InstExpLit<>(def("I2"));
      assertEquals(i.hashCode(), lit.hashCode());
      same.hashCode();
      other.hashCode();
      assertEquals(lit, lit);
      assertEquals(lit, same);
      assertNotEquals(lit, other);
      assertNotEquals(lit, null);
      assertNotEquals(lit, "InstExpLit");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      InstExpVisitor<String, Unit, RuntimeException> v = mock(InstExpVisitor.class);
      when(v.visit(Unit.unit, lit)).thenReturn("visited");
      assertEquals("visited", lit.accept(Unit.unit, v));
    }
  }

  @Nested
  class Mappings {

    private final Mapping<String, String, Sym, Fk, Att, String, Fk, Att> m = def("F");
    private final MapExpLit lit = new MapExpLit(m);

    @Test
    void evaluatesToItsMapping() {
      assertSame(m, lit.eval0(env, false));
      assertEquals(
          new Pair<>(new SchExpLit(m.src), new SchExpLit(m.dst)), lit.type(new AqlTyping()));
      assertTrue(lit.toString().startsWith("constant "), lit.toString());
      assertInert(lit);
    }

    @Test
    void equalityComparesTheMapping() {
      assertEquals(lit, lit);
      assertEquals(lit, new MapExpLit(m));
      assertEquals(m.hashCode(), lit.hashCode());
      assertNotEquals(lit, new MapExpLit(def("F2")));
      assertNotEquals(lit, null);
      assertNotEquals(lit, "constant");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitorAndACoVisitor() {
      MapExpVisitor<String, Unit, RuntimeException> v = mock(MapExpVisitor.class);
      when(v.visit(Unit.unit, lit)).thenReturn("visited");
      assertEquals("visited", lit.accept(Unit.unit, v));
      MapExpCoVisitor<Unit, Unit, RuntimeException> cv = mock(MapExpCoVisitor.class);
      when(cv.visitMapExpLit(Unit.unit, Unit.unit)).thenReturn(lit);
      assertEquals(lit, lit.coaccept(Unit.unit, cv, Unit.unit));
    }
  }

  @Nested
  class Transforms {

    private final Transform<
            String,
            String,
            Sym,
            Fk,
            Att,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String>
        t = def("t");
    private final TransExpLit<String, String, String, String, String, String, String, String> lit =
        new TransExpLit<>(t);

    @Test
    void evaluatesToItsTransform() {
      assertSame(t, lit.eval0(env, false));
      Pair<InstExp<String, String, String, String>, InstExp<String, String, String, String>> type =
          lit.type(new AqlTyping());
      assertEquals(new InstExpLit<>(t.src()).toString(), type.first.toString());
      assertEquals(t.toString(), lit.toString());
      assertInert(lit);
    }

    @Test
    void equalityComparesTheTransform() {
      TransExpLit<String, String, String, String, String, String, String, String> other =
          new TransExpLit<>(def("t2"));
      assertEquals(t.hashCode(), lit.hashCode());
      other.hashCode();
      assertEquals(lit, lit);
      assertEquals(lit, new TransExpLit<>(t));
      assertNotEquals(lit, other);
      assertNotEquals(lit, null);
      assertFalse(lit.equals("transform"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      TransExpVisitor<String, Unit, RuntimeException> v = mock(TransExpVisitor.class);
      when(v.visit(Unit.unit, lit)).thenReturn("visited");
      assertEquals("visited", lit.accept(Unit.unit, v));
    }
  }

  @Nested
  class ConstraintSets {

    private final Constraints c = def("C");
    private final EdsExpLit lit = new EdsExpLit(c);

    @Test
    void evaluatesToItsConstraints() {
      assertSame(c, lit.eval0(env, false));
      assertEquals(new SchExpLit(c.schema), lit.type(new AqlTyping()));
      assertFalse(lit.isVar());
      assertEquals(c.toString(), lit.toString());
      assertInert(lit);
    }

    @Test
    void equalityComparesTheConstraints() {
      assertEquals(lit, lit);
      assertEquals(lit, new EdsExpLit(c));
      assertEquals(c.hashCode(), lit.hashCode());
      assertNotEquals(lit, new EdsExpLit(def("C2")));
      assertNotEquals(lit, null);
      assertNotEquals(lit, "constraints");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      EdsExpVisitor<String, Unit, RuntimeException> v = mock(EdsExpVisitor.class);
      when(v.visit(Unit.unit, lit)).thenReturn("visited");
      assertEquals("visited", lit.accept(Unit.unit, v));
    }
  }
}
