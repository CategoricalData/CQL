package catdata.cql;

import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.literal;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import catdata.cql.fdm.IdentityTransform;
import catdata.cql.fdm.LiteralInstance;
import catdata.cql.fdm.LiteralTransform;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class TransformTest {

  private static final String E = "E";
  private static final String D = "D";

  private static final Att A = Att.Att(E, "a");
  private static final Fk F = Fk.Fk(E, "f");

  /** f : E -> D and a : E -> Varchar. */
  private static final Schema<String, String, Sym, Fk, Att> SCHEMA =
      sqlSchema(List.of(E, D), Map.of(A, E), Map.of(F, new Pair<>(E, D)));

  private static Term<String, String, Sym, Fk, Att, String, String> gen(String g) {
    return Term.Gen(g);
  }

  private static Term<String, String, Sym, Fk, Att, String, String> sk(String y) {
    return Term.Sk(y);
  }

  private static Term<Void, String, Void, Fk, Void, String, Void> row(String g) {
    return Term.Gen(g);
  }

  private static Pair<
          Term<String, String, Sym, Fk, Att, String, String>,
          Term<String, String, Sym, Fk, Att, String, String>>
      eq(
          Term<String, String, Sym, Fk, Att, String, String> l,
          Term<String, String, Sym, Fk, Att, String, String> r) {
    return new Pair<>(l, r);
  }

  private static String message(Executable e) {
    return assertThrows(RuntimeException.class, e).getMessage();
  }

  /** e1 e2 : E and d : D, with e1.f = e2.f = d and e1.a = y for a labelled null y : Varchar. */
  private static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      source() {
    Map<String, String> gens = new LinkedHashMap<>();
    gens.put("e1", E);
    gens.put("e2", E);
    gens.put("d", D);
    return literal(
        SCHEMA,
        gens,
        Map.of("y", VARCHAR),
        List.of(
            eq(Term.Fk(F, gen("e1")), gen("d")),
            eq(Term.Fk(F, gen("e2")), gen("d")),
            eq(Term.Att(A, gen("e1")), sk("y"))));
  }

  /** x : E and c : D, with x.f = c and x.a = z for a labelled null z : Varchar. */
  private static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      target() {
    return literal(
        SCHEMA,
        Map.of("x", E, "c", D),
        Map.of("z", VARCHAR),
        List.of(eq(Term.Fk(F, gen("x")), gen("c")), eq(Term.Att(A, gen("x")), sk("z"))));
  }

  /** A transform given by tables, which validates only when asked. */
  private static final class Mapped
      extends Transform<
          String,
          String,
          Sym,
          Fk,
          Att,
          String,
          String,
          String,
          String,
          Integer,
          Chc<String, Pair<Integer, Att>>,
          Integer,
          Chc<String, Pair<Integer, Att>>> {

    private final Instance<
            String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
        src;
    private final Instance<
            String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
        dst;
    private final BiFunction<String, String, Term<Void, String, Void, Fk, Void, String, Void>> gens;
    private final BiFunction<String, String, Term<String, String, Sym, Fk, Att, String, String>>
        sks;

    Mapped(
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
            src,
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
            dst,
        Map<String, Term<Void, String, Void, Fk, Void, String, Void>> gens,
        Map<String, Term<String, String, Sym, Fk, Att, String, String>> sks) {
      this(src, dst, (g, en) -> gens.get(g), (y, ty) -> sks.get(y));
    }

    Mapped(
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
            src,
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
            dst,
        BiFunction<String, String, Term<Void, String, Void, Fk, Void, String, Void>> gens,
        BiFunction<String, String, Term<String, String, Sym, Fk, Att, String, String>> sks) {
      this.src = src;
      this.dst = dst;
      this.gens = gens;
      this.sks = sks;
    }

    @Override
    public BiFunction<String, String, Term<Void, String, Void, Fk, Void, String, Void>> gens() {
      return gens;
    }

    @Override
    public BiFunction<String, String, Term<String, String, Sym, Fk, Att, String, String>> sks() {
      return sks;
    }

    @Override
    public Instance<
            String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
        src() {
      return src;
    }

    @Override
    public Instance<
            String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
        dst() {
      return dst;
    }
  }

  /** e1 and e2 go to x, d to c and y to z. */
  private static Mapped collapse() {
    return new Mapped(
        source(),
        target(),
        Map.of("e1", row("x"), "e2", row("x"), "d", row("c")),
        Map.of("y", sk("z")));
  }

  @Nested
  class Translation {

    @Test
    void isATransformOfTheSizeOfBothInstances() {
      Mapped t = collapse();
      assertEquals(Kind.TRANSFORM, t.kind());
      assertEquals(3 + 2, t.size());
    }

    @Test
    void transReplacesGeneratorsAndLabelledNulls() {
      Mapped t = collapse();
      assertEquals(gen("x"), t.trans(gen("e2")));
      assertEquals(sk("z"), t.trans(sk("y")));
      assertEquals(Term.Fk(F, gen("x")), t.trans(Term.Fk(F, gen("e1"))));
      assertEquals(Term.Att(A, gen("x")), t.trans(Term.Att(A, gen("e2"))));
    }

    @Test
    void transKeepsVariablesConstantsAndSymbols() {
      Mapped t = collapse();
      Term<String, String, Sym, Fk, Att, String, String> x = Term.Var("v");
      Term<String, String, Sym, Fk, Att, String, String> ann =
          Term.Obj(Optional.of("ann"), VARCHAR);
      assertEquals(x, t.trans(x));
      assertEquals(ann, t.trans(ann));
      Sym isNull = Sym.Sym("isNull", new Pair<>(List.of(VARCHAR), CqlTestHelpers.BOOLEAN));
      assertEquals(
          Term.Sym(isNull, List.of(Term.Att(A, gen("x")))),
          t.trans(Term.Sym(isNull, List.of(Term.Att(A, gen("e1"))))));
    }

    @Test
    void reprSendsEveryRowToItsImage() {
      Mapped t = collapse();
      var src = t.src().algebra();
      Integer x = t.dst().algebra().gen("x");
      assertEquals(x, t.repr(E, src.gen("e1")));
      assertEquals(x, t.repr(E, src.gen("e2")));
      assertEquals(t.dst().algebra().gen("c"), t.repr(D, src.gen("d")));
    }

    @Test
    void reprTTranslatesTheTermOfAnUnknownValue() {
      Mapped t = collapse();
      Integer e2 = t.src().algebra().gen("e2");
      assertEquals(Term.Att(A, gen("x")), t.reprT(Chc.inRight(new Pair<>(e2, A))));
    }

    @Test
    void preImageGroupsTheRowsOfAnEntityByImage() {
      Mapped t = collapse();
      var src = t.src().algebra();
      var dst = t.dst().algebra();
      assertEquals(
          Map.of(dst.gen("x"), Set.of(src.gen("e1"), src.gen("e2"))), t.preImage(E));
      assertEquals(Map.of(dst.gen("c"), Set.of(src.gen("d"))), t.preImage(D));
    }

    @Test
    void sksExtensionalTabulatesTheLabelledNulls() {
      assertEquals(Map.of(new Pair<>("y", VARCHAR), sk("z")), collapse().sksExtensional());
    }
  }

  @Nested
  class Validation {

    @Test
    void acceptsATransformThatPreservesTheEquations() {
      collapse().validate(false);
    }

    @Test
    void rejectsInstancesOfDifferentSchemas() {
      var other = literal(sqlSchema(List.of(E), Map.of(), Map.of()), Map.of(), List.of());
      var t = new Mapped(source(), other, Map.of(), Map.of());
      assertTrue(message(() -> t.validate(false)).startsWith("Differing instance schemas\n\nsrc "));
    }

    @Test
    void rejectsAGeneratorWithoutAnImageOrOfAnotherEntity() {
      var missing =
          new Mapped(
              source(), target(), Map.of("e1", row("x"), "d", row("c")), Map.of("y", sk("z")));
      assertEquals(
          "source generator e2 has no transform. E", message(() -> missing.validate(false)));
      var wrong =
          new Mapped(
              source(),
              target(),
              Map.of("e1", row("c"), "e2", row("x"), "d", row("c")),
              Map.of("y", sk("z")));
      assertEquals(
          "source generator e1 transforms to c, which has sort D, not E as expected",
          message(() -> wrong.validate(false)));
    }

    @Test
    void rejectsALabelledNullWithoutAnImageOrOfAnotherSort() {
      Map<String, Term<Void, String, Void, Fk, Void, String, Void>> gens =
          Map.of("e1", row("x"), "e2", row("x"), "d", row("c"));
      var missing = new Mapped(source(), target(), gens, Map.of());
      assertEquals(
          "source labelled null y has no transform", message(() -> missing.validate(false)));
      var wrong = new Mapped(source(), target(), gens, Map.of("y", gen("x")));
      assertEquals(
          "source labelled null y transforms to x, which has sort E, not Varchar as expected",
          message(() -> wrong.validate(false)));
    }

    /**
     * Issue 57: the message promises the target instance but cuts its text after as many
     * characters as the target has rows.
     */
    @Test
    void rejectsAnEquationTheTargetDoesNotProve() {
      var src =
          literal(
              SCHEMA,
              Map.of("e1", E, "e2", E, "d", D),
              List.of(
                  eq(Term.Fk(F, gen("e1")), gen("d")),
                  eq(Term.Fk(F, gen("e2")), gen("d")),
                  eq(Term.Att(A, gen("e1")), Term.Att(A, gen("e2")))));
      var dst =
          literal(
              SCHEMA,
              Map.of("x1", E, "x2", E, "c", D),
              List.of(
                  eq(Term.Fk(F, gen("x1")), gen("c")),
                  eq(Term.Fk(F, gen("x2")), gen("c")),
                  eq(Term.Att(A, gen("x1")), Term.Obj(Optional.of("p"), VARCHAR)),
                  eq(Term.Att(A, gen("x2")), Term.Obj(Optional.of("q"), VARCHAR))));
      var t =
          new Mapped(
              src, dst, Map.of("e1", row("x1"), "e2", row("x2"), "d", row("c")), Map.of());
      String m = message(() -> t.validate(false));
      assertTrue(
          m.startsWith(
              "Source instance equation e1.a = e2.a translates to x1.a = x2.a, which is not"
                  + " provable in the target instance, displayed below."),
          m);
      assertTrue(m.endsWith("\n\n" + dst.toString().substring(0, dst.size())), m);
      t.validate(true);
    }
  }

  @Nested
  class Concrete {

    @Test
    void aLiteralTransformValidatesOnConstruction() {
      var i = source();
      var j = target();
      LiteralTransform<
              String,
              String,
              Sym,
              Fk,
              Att,
              String,
              String,
              String,
              String,
              Integer,
              Chc<String, Pair<Integer, Att>>,
              Integer,
              Chc<String, Pair<Integer, Att>>>
          t =
              new LiteralTransform<>(
                  (g, en) -> row(g.equals("d") ? "c" : "x"), (y, ty) -> sk("z"), i, j, false);
      assertEquals(gen("x"), t.trans(gen("e1")));
      assertThrows(
          RuntimeException.class,
          () -> new LiteralTransform<>((g, en) -> row("c"), (y, ty) -> sk("z"), i, j, false));
    }

    @Test
    void theIdentityMapsEveryRowToItself() {
      var i = source();
      var t = new IdentityTransform<>(i, Optional.empty());
      assertSame(i, t.src());
      assertSame(i, t.dst());
      assertEquals(Map.of(i.algebra().gen("d"), Set.of(i.algebra().gen("d"))), t.preImage(D));
    }
  }

  @Nested
  class Equality {

    @Test
    void comparesTheFunctionsByIdentity() {
      Mapped t = collapse();
      assertTrue(t.equals(t));
      assertFalse(t.equals(null));
      assertFalse(t.equals("t"));
      assertEquals(t.hashCode(), t.hashCode());
      var i = source();
      var j = target();
      i.hashCode();
      j.hashCode();
      Map<String, Term<Void, String, Void, Fk, Void, String, Void>> gens =
          Map.of("e1", row("x"), "e2", row("x"), "d", row("c"));
      Map<String, Term<String, String, Sym, Fk, Att, String, String>> sks = Map.of("y", sk("z"));
      assertNotEquals(new Mapped(i, j, gens, sks), new Mapped(i, j, gens, sks));
    }

    @Test
    void comparesTheTargetTheFunctionsAndTheSource() {
      var i = source();
      var i2 = source();
      var j = target();
      i.hashCode();
      i2.hashCode();
      j.hashCode();
      Mapped t = collapse();
      var g = t.gens();
      var k = t.sks();
      BiFunction<String, String, Term<String, String, Sym, Fk, Att, String, String>> other =
          (y, ty) -> sk("z");
      Mapped base = new Mapped(i, j, g, k);
      assertEquals(base, new Mapped(i, j, g, k));
      assertEquals(base.hashCode(), new Mapped(i, j, g, k).hashCode());
      assertNotEquals(base, new Mapped(i, j, g, other));
      assertTrue(i.equals(i2));
      Map<String, String> more = Map.of("e1", E, "e2", E, "e3", E, "d", D);
      var bigger = literal(SCHEMA, more, Map.of("y", VARCHAR), List.of());
      bigger.hashCode();
      assertNotEquals(base, new Mapped(bigger, j, g, k));
    }

    @Test
    void comparesTheInstancesAndFunctions() {
      var i = source();
      var j = target();
      i.hashCode();
      j.hashCode();
      var a = new IdentityTransform<>(i, Optional.empty());
      var b = new IdentityTransform<>(j, Optional.empty());
      assertNotEquals(a, b);
    }
  }

  @Nested
  class Printing {

    @Test
    void listsTheImageOfEveryGeneratorAndLabelledNull() {
      Mapped t = collapse();
      String s = t.toString();
      assertTrue(s.startsWith("generators\n\t"), s);
      assertTrue(s.contains("\n\te1 -> x\n\t"), s);
      assertTrue(s.contains("\n\td -> c\n\t"), s);
      assertTrue(s.endsWith("\n\ty -> z\n\t"), s);
      assertSame(s, t.toString());
    }

    @Test
    void namesTheTwoSectionsAsAsked() {
      String s = collapse().toString("gens", "nulls");
      assertTrue(s.startsWith("gens\n\t"), s);
      assertTrue(s.contains("\nnulls\n\ty -> z"), s);
    }
  }
}
