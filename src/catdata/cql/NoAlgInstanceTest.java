package catdata.cql;

import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import catdata.Pair;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Test;

/**
 * An instance kept as its presentation, without an algebra. InstExpRaw builds one only when its
 * local interpret_as_frozen is true, and that is fixed to false (InstExpRaw.java:284).
 */
class NoAlgInstanceTest {

  private static final Att N = Att.Att("E", "n");
  private static final Fk F = Fk.Fk("E", "f");

  /** f : E -> D and n : E -> Varchar. */
  private static final Schema<String, String, Sym, Fk, Att> SCHEMA =
      sqlSchema(List.of("D", "E"), Map.of(N, "E"), Map.of(F, new Pair<>("E", "D")));

  private static Term<String, String, Sym, Fk, Att, String, String> gen(String g) {
    return Term.Gen(g);
  }

  /** e : E and d : D with e.f = d and e.n = y for a labelled null y. */
  private static Collage<String, String, Sym, Fk, Att, String, String> presentation() {
    Collage<String, String, Sym, Fk, Att, String, String> col = new CCollage<>(SCHEMA.collage());
    col.gens().put("e", "E");
    col.gens().put("d", "D");
    col.sks().put("y", VARCHAR);
    col.eqs().add(new Eq<>(null, Term.Fk(F, gen("e")), gen("d")));
    col.eqs().add(new Eq<>(null, Term.Att(N, gen("e")), Term.Sk("y")));
    return col;
  }

  @Test
  void keepsThePresentation() {
    var i = new NoAlgInstance(presentation(), SCHEMA, ops());
    assertSame(SCHEMA, i.schema());
    assertEquals("D", i.gens().get("d"));
    assertEquals(VARCHAR, i.sks().get("y"));
    List<
            Pair<
                Term<String, String, Sym, Fk, Att, String, String>,
                Term<String, String, Sym, Fk, Att, String, String>>>
        eqs = new ArrayList<>();
    i.eqs((l, r) -> eqs.add(new Pair<>(l, r)));
    assertEquals(
        List.of(
            new Pair<>(Term.Fk(F, gen("e")), gen("d")),
            new Pair<>(Term.Att(N, gen("e")), Term.Sk("y"))),
        eqs);
  }

  @Test
  void readsItsFlagsFromTheOptions() {
    var i = new NoAlgInstance(presentation(), SCHEMA, ops());
    assertTrue(i.requireConsistency());
    assertTrue(i.allowUnsafeJava());
    var strict =
        new NoAlgInstance(
            presentation(),
            SCHEMA,
            new AqlOptions(ops(), AqlOption.require_consistency, false));
    assertFalse(strict.requireConsistency());
  }

  @Test
  void decidesWithAProverBuiltOnFirstUse() {
    var i = new NoAlgInstance(presentation(), SCHEMA, ops());
    var dp = i.dp();
    assertSame(dp, i.dp());
    assertTrue(dp.eq(null, Term.Fk(F, gen("e")), gen("d")));
    assertFalse(dp.eq(null, Term.Att(N, gen("e")), Term.Obj(Optional.of("x"), VARCHAR)));
  }

  /** Instance.size and Instance.toString go through the algebra. */
  @Test
  void hasNoAlgebraSoItCannotBeCountedOrPrinted() {
    var i = new NoAlgInstance(presentation(), SCHEMA, ops());
    String m = assertThrows(RuntimeException.class, i::algebra).getMessage();
    assertEquals("Anomaly: please report", m);
    assertThrows(RuntimeException.class, i::size);
    assertThrows(RuntimeException.class, i::toString);
  }

  @Test
  void validatesThePresentationOnConstruction() {
    var col = presentation();
    col.gens().put("o", "Office");
    String m =
        assertThrows(RuntimeException.class, () -> new NoAlgInstance(col, SCHEMA, ops()))
            .getMessage();
    assertTrue(m.startsWith("On generator o, the entity Office is not declared."), m);
  }

  @Test
  void requiresOptions() {
    assertThrows(RuntimeException.class, () -> new NoAlgInstance(presentation(), SCHEMA, null));
  }
}
