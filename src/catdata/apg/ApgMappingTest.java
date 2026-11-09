package catdata.apg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Pair;
import catdata.Triple;
import catdata.cql.Kind;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApgMapping}, built directly from terms. The target schema has a label
 * {@code User -> ()} and a label {@code name -> (User: label User * name: base Str)} that refers to
 * it.
 */
class ApgMappingTest {

  private static final ApgTypeside TY =
      new ApgTypeside(
          Map.of(
              "Str", new Pair<>(String.class, (Function<String, Object>) x -> x),
              "Nat", new Pair<>(Integer.class, (Function<String, Object>) Integer::parseInt)),
          Map.of(
              "append",
              new Triple<>(
                  List.of("Str", "Str"),
                  "Str",
                  (Function<List<Object>, Object>) l -> (String) l.get(0) + l.get(1))));

  private static final ApgTy<String> UNIT = ApgTy.ApgTyP(true, Map.of());
  private static final ApgTy<String> STR = ApgTy.ApgTyB("Str");
  private static final ApgTy<String> NAT = ApgTy.ApgTyB("Nat");

  private static ApgTy<String> label(String l) {
    return ApgTy.ApgTyL(l);
  }

  private static ApgTerm<String, Void> v(String x) {
    return ApgTerm.ApgTermVar(x);
  }

  private static ApgTerm<String, Void> str(String s) {
    return ApgTerm.ApgTermV(s, "Str");
  }

  private static ApgTerm<String, Void> field(String f, ApgTerm<String, Void> t) {
    return ApgTerm.ApgTermProj(f, t);
  }

  private static ApgTerm<String, Void> deref(String l, ApgTerm<String, Void> t) {
    return ApgTerm.ApgTermDeref(l, t);
  }

  private static ApgSchema<String> target() {
    Map<String, ApgTy<String>> labels = new HashMap<>();
    labels.put("User", UNIT);
    labels.put("name", ApgTy.ApgTyP(true, Map.of("User", label("User"), "name", STR)));
    return new ApgSchema<>(TY, labels);
  }

  /** The instance with one user {@code u1} and one name {@code n1 -> (User: u1, name: A)}. */
  private static ApgInstance<String, String> instance() {
    Map<String, Pair<String, ApgTerm<String, String>>> es = new HashMap<>();
    es.put("u1", new Pair<>("User", ApgTerm.ApgTermTuple(Map.of())));
    es.put(
        "n1",
        new Pair<>(
            "name",
            ApgTerm.ApgTermTuple(
                Map.of("User", ApgTerm.ApgTermE("u1"), "name", ApgTerm.ApgTermV("A", "Str")))));
    return new ApgInstance<>(target(), es);
  }

  /** The source schema {@code U -> ()}, {@code N -> (owner: label U * nm: base Str)}. */
  private static ApgSchema<String> source() {
    Map<String, ApgTy<String>> labels = new HashMap<>();
    labels.put("U", UNIT);
    labels.put("N", ApgTy.ApgTyP(true, Map.of("owner", label("U"), "nm", STR)));
    return new ApgSchema<>(TY, labels);
  }

  /** Sends U to the users and N to the names, reading the owner and the name of each name. */
  private static Map<String, Triple<String, ApgTy<String>, ApgTerm<String, Void>>> mapping() {
    Map<String, Triple<String, ApgTy<String>, ApgTerm<String, Void>>> m = new HashMap<>();
    m.put("U", new Triple<>("v", label("User"), ApgTerm.ApgTermTuple(Map.of())));
    m.put(
        "N",
        new Triple<>(
            "v",
            label("name"),
            ApgTerm.ApgTermTuple(
                Map.of(
                    "owner", field("User", deref("name", v("v"))),
                    "nm", field("name", deref("name", v("v")))))));
    return m;
  }

  @Nested
  class Validation {

    @Test
    void acceptsAWellTypedMapping() {
      var f = new ApgMapping<>(source(), target(), mapping());
      assertEquals(Kind.APG_mapping, f.kind());
      assertEquals(2, f.size());
      assertTrue(f.toString().startsWith("labels\n\t"), f.toString());
      assertTrue(f.toString().contains("lambda v : User. ()"), f.toString());
    }

    @Test
    void rejectsADifferentTypesideOrAMissingLabel() {
      ApgSchema<String> other =
          new ApgSchema<>(new ApgTypeside(Map.of(), Map.of()), new HashMap<>(source().schema));
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> new ApgMapping<>(other, target(), mapping()));
      assertEquals("Typeside mismatch.", e.getMessage());
      var m = mapping();
      m.remove("N");
      e = assertThrows(RuntimeException.class, () -> new ApgMapping<>(source(), target(), m));
      assertEquals("Missing mapping for label N", e.getMessage());
    }

    @Test
    void rejectsATermOfTheWrongTypeOrAnUnknownLabel() {
      var m = mapping();
      m.put("N", new Triple<>("v", label("name"), v("v")));
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> new ApgMapping<>(source(), target(), m));
      assertTrue(e.getMessage().startsWith("Term v has type name, not ("), e.getMessage());
      m.put("N", new Triple<>("v", label("nobody"), v("v")));
      e = assertThrows(RuntimeException.class, () -> new ApgMapping<>(source(), target(), m));
      assertEquals("Not a schema abel: nobody", e.getMessage());
    }
  }

  /**
   * The type checker of the terms, on a source with the single label {@code X -> base Str}, mapped
   * by {@code lambda v: label name}. Most of these checks repeat those that a literal mapping makes
   * on its pre-terms, so a program never reaches them. Terms that differ only in their annotation
   * are shared, so each injection and case analysis here differs from the others in its body.
   */
  @Nested
  class Typing {

    private final ApgTy<String> strOrNat = ApgTy.ApgTyP(false, Map.of("a", STR, "b", NAT));

    private final ApgTy<String> onlyStr = ApgTy.ApgTyP(false, Map.of("a", STR));

    private ApgMapping<String, String> map(ApgTerm<String, Void> term) {
      return new ApgMapping<>(
          new ApgSchema<>(TY, new HashMap<>(Map.of("X", STR))),
          target(),
          Map.of("X", new Triple<>("v", label("name"), term)));
    }

    private String error(ApgTerm<String, Void> term) {
      return assertThrows(RuntimeException.class, () -> map(term)).getMessage();
    }

    @Test
    void checksValuesAndVariables() {
      assertEquals(1, map(str("a")).size());
      assertEquals(
          "Encountered primitive value a at non-type Foo", error(ApgTerm.ApgTermV("a", "Foo")));
      String msg = error(ApgTerm.ApgTermV(5, "Str"));
      assertTrue(msg.startsWith("Primitive value 5 is not of type "), msg);
      assertTrue(msg.endsWith(", is Integer"), msg);
      assertEquals("Unbound variable: w", error(v("w")));
    }

    @Test
    void checksFunctionApplications() {
      assertEquals(1, map(ApgTerm.ApgTermApp("append", List.of(str("a"), str("b")))).size());
      assertEquals("Not a UDF: foo", error(ApgTerm.ApgTermApp("foo", List.of())));
      assertEquals(
          "In append(a), given 1 arguments, not 2 as expected.",
          error(ApgTerm.ApgTermApp("append", List.of(str("a")))));
      assertEquals(
          "In append(1,a), argument 0 (1) has type Nat not Str as expected.",
          error(ApgTerm.ApgTermApp("append", List.of(ApgTerm.ApgTermV(1, "Nat"), str("a")))));
    }

    @Test
    void checksForeignKeysAndProjections() {
      assertEquals(1, map(field("name", deref("name", v("v")))).size());
      assertEquals("In !nope(v), nope is not a target label.", error(deref("nope", v("v"))));
      assertEquals("In !User(v), User given argument of type name", error(deref("User", v("v"))));
      assertEquals("In .x(v) cannot project x from null of type name", error(field("x", v("v"))));
      String msg = error(field("zz", deref("name", v("v"))));
      assertTrue(msg.startsWith("In .zz(!name(v)) cannot project zz from null of type ("), msg);
    }

    @Test
    void checksInjections() {
      assertEquals(
          "In <z:inj-z> label z not found in [a]",
          error(ApgTerm.ApgTermInj("z", str("inj-z"), onlyStr)));
      String msg = error(ApgTerm.ApgTermInj("b", str("inj-b"), strOrNat));
      assertEquals(
          "In <b:inj-b> type of label for b, namely, Nat is not equal to Str, as expected.", msg);
    }

    /** An injection without a sum type fails while it builds its own error message. */
    @Test
    void failsOnAnInjectionWithoutASumType() {
      assertThrows(
          NullPointerException.class, () -> map(ApgTerm.ApgTermInj("a", str("no-sum-1"), null)));
      assertThrows(
          NullPointerException.class, () -> map(ApgTerm.ApgTermInj("a", str("no-sum-2"), STR)));
      ApgTy<String> product = ApgTy.ApgTyP(true, Map.of("a", STR));
      String msg = error(ApgTerm.ApgTermInj("a", str("no-sum-3"), product));
      assertEquals("In <a:no-sum-3> label a not found in [a]", msg);
    }

    @Test
    void checksCaseAnalyses() {
      ApgTerm<String, Void> a = ApgTerm.ApgTermInj("a", str("s"), onlyStr);
      assertEquals(
          1, map(ApgTerm.ApgTermCase(a, Map.of("a", new Pair<>("x", v("x"))), STR)).size());
      assertEquals(
          "Cannot perform case analysis on v at non-sum type: name",
          error(ApgTerm.ApgTermCase(v("v"), Map.of(), STR)));
      assertEquals(
          "Set of cases [b] not the same as variants in type: [a]",
          error(ApgTerm.ApgTermCase(a, Map.of("b", new Pair<>("x", v("x"))), STR)));
      String msg = error(ApgTerm.ApgTermCase(a, Map.of("a", new Pair<>("v", str("t"))), STR));
      assertTrue(msg.startsWith("Duplicate bound variable: v in case "), msg);
      assertEquals(
          "Type for branch a is Str and not Nat as expected.",
          error(ApgTerm.ApgTermCase(a, Map.of("a", new Pair<>("y", v("y"))), NAT)));
    }
  }

  @Nested
  class Delta {

    /**
     * The source label {@code Q -> (up: label Q * nm: base Str)} reads a person and their boss, so
     * the only foreign key of the source refers to the label it is in.
     */
    @Test
    void pullsBackAnInstanceWhoseForeignKeysStayInTheirLabel() {
      Map<String, ApgTy<String>> people = new HashMap<>();
      people.put("P", ApgTy.ApgTyP(true, Map.of("boss", label("P"), "name", STR)));
      ApgSchema<String> t = new ApgSchema<>(TY, people);
      Map<String, ApgTy<String>> qs = new HashMap<>();
      qs.put("Q", ApgTy.ApgTyP(true, Map.of("up", label("Q"), "nm", STR)));
      ApgSchema<String> s = new ApgSchema<>(TY, qs);
      var f =
          new ApgMapping<>(
              s,
              t,
              Map.of(
                  "Q",
                  new Triple<>(
                      "v",
                      label("P"),
                      ApgTerm.ApgTermTuple(
                          Map.of(
                              "up", field("boss", deref("P", v("v"))),
                              "nm", field("name", deref("P", v("v"))))))));
      Map<String, Pair<String, ApgTerm<String, String>>> es = new HashMap<>();
      for (String[] p : new String[][] {{"p1", "A"}, {"p2", "B"}}) {
        es.put(
            p[0],
            new Pair<>(
                "P",
                ApgTerm.ApgTermTuple(
                    Map.of(
                        "boss", ApgTerm.ApgTermE("p1"), "name", ApgTerm.ApgTermV(p[1], "Str")))));
      }
      var d = f.delta(new ApgInstance<>(t, es));
      assertEquals(2, d.Es.size());
      var p2 = d.Es.get(new Pair<>("Q", ApgTerm.ApgTermE("p2")));
      assertEquals("Q", p2.first);
      assertEquals(
          ApgTerm.ApgTermE(new Pair<>("Q", ApgTerm.ApgTermE("p1"))), p2.second.fields.get("up"));
      assertEquals("B", p2.second.fields.get("nm").value);
    }

    /**
     * The owner of the name {@code (N, n1)} should be the user {@code (U, u1)}, but every element
     * that the value of a row refers to is named with the label of that row.
     */
    @Test
    void namesTheTargetOfAForeignKeyWithTheLabelOfItsSource() {
      var f = new ApgMapping<>(source(), target(), mapping());
      RuntimeException e = assertThrows(RuntimeException.class, () -> f.delta(instance()));
      assertTrue(e.getMessage().startsWith("Element (N, u1) not in "), e.getMessage());
    }

    @Test
    void refusesAMorphismThatRenamesALabel() {
      Map<String, ApgTy<String>> labels = new HashMap<>();
      labels.put("A", UNIT);
      labels.put("B", UNIT);
      ApgSchema<String> ab = new ApgSchema<>(TY, labels);
      var i = new ApgInstance<>(ab, Map.of("a", new Pair<>("A", ApgTerm.ApgTermTuple(Map.of()))));
      var j = new ApgInstance<>(ab, Map.of("b", new Pair<>("B", ApgTerm.ApgTermTuple(Map.of()))));
      var h = new ApgTransform<>(i, j, Map.of("A", "B", "B", "B"), Map.of("a", "b"));
      Map<String, ApgTy<String>> xs = new HashMap<>();
      xs.put("X", UNIT);
      var f =
          new ApgMapping<>(
              new ApgSchema<>(TY, xs),
              ab,
              Map.of("X", new Triple<>("v", label("A"), ApgTerm.ApgTermTuple(Map.of()))));
      RuntimeException e = assertThrows(RuntimeException.class, () -> f.deltaT(h));
      assertEquals("Transform not identity on labels: A -> B", e.getMessage());
    }
  }

  /**
   * The identity sends each label {@code l} to {@code lambda v: label l. v}, whose type is the
   * label rather than the type of the label, so it is well typed only on a label whose type is
   * itself.
   */
  @Nested
  class Identity {

    @Test
    void failsOnALabelWhoseTypeIsNotTheLabel() {
      RuntimeException e = assertThrows(RuntimeException.class, () -> ApgMapping.id(target()));
      assertTrue(e.getMessage().startsWith("Term v has type "), e.getMessage());
    }

    @Test
    void acceptsALabelWhoseTypeIsTheLabel() {
      Map<String, ApgTy<String>> loop = new HashMap<>();
      loop.put("L", label("L"));
      var id = ApgMapping.id(new ApgSchema<>(TY, loop));
      assertEquals(1, id.size());
      assertEquals("v", id.mapping.get("L").first);
    }
  }
}
