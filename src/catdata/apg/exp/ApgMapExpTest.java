package catdata.apg.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import catdata.Pair;
import catdata.Unit;
import catdata.apg.exp.ApgMapExp.ApgMapExpCompose;
import catdata.apg.exp.ApgMapExp.ApgMapExpVar;
import catdata.apg.exp.ApgSchExp.ApgSchExpVar;
import catdata.apg.exp.ApgTyExp.ApgTyExpVar;
import catdata.cql.Kind;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.AqlTyping;
import java.util.Collection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApgMapExpTest {

  @Nested
  class ApgMapExpVarTest {

    @Test
    void constructorSetsVar() {
      assertEquals("m1", new ApgMapExpVar("m1").var);
    }

    @Test
    void kindReturnsApgMapping() {
      assertEquals(Kind.APG_mapping, new ApgMapExpVar("m").kind());
    }

    @Test
    void isVarReturnsTrue() {
      assertTrue(new ApgMapExpVar("m").isVar());
    }

    @Test
    void toStringReturnsVar() {
      assertEquals("myMap", new ApgMapExpVar("myMap").toString());
    }

    @Test
    void depsReturnsSingleton() {
      Collection<Pair<String, Kind>> deps = new ApgMapExpVar("m1").deps();
      assertEquals(1, deps.size());
      Pair<String, Kind> dep = deps.iterator().next();
      assertEquals("m1", dep.first);
      assertEquals(Kind.APG_instance, dep.second);
    }

    @Test
    void equalsSameVar() {
      ApgMapExpVar v1 = new ApgMapExpVar("m");
      ApgMapExpVar v2 = new ApgMapExpVar("m");
      assertEquals(v1, v2);
      assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void notEqualsDifferentVar() {
      assertNotEquals(new ApgMapExpVar("a"), new ApgMapExpVar("b"));
    }

    @Test
    void equalsReflexive() {
      ApgMapExpVar v = new ApgMapExpVar("m");
      assertEquals(v, v);
    }

    @Test
    void notEqualsNull() {
      assertNotEquals(null, new ApgMapExpVar("m"));
    }

    @Test
    void notEqualsDifferentType() {
      assertNotEquals("m", new ApgMapExpVar("m"));
    }

    @Test
    void typeThrowsWhenUndefined() {
      AqlTyping typing = new AqlTyping();
      assertThrows(RuntimeException.class, () -> new ApgMapExpVar("missing").type(typing));
    }

    @Test
    void typeReturnsPairWhenDefined() {
      AqlTyping typing = new AqlTyping();
      Pair<ApgSchExp, ApgSchExp> pair = new Pair<>(new ApgSchExpVar("s1"), new ApgSchExpVar("s2"));
      typing.defs.apgmappings.put("m1", pair);
      assertEquals(pair, new ApgMapExpVar("m1").type(typing));
    }

    @Test
    void acceptDelegatesToVisitor() {
      ApgMapExpVar v = new ApgMapExpVar("m");
      @SuppressWarnings("unchecked")
      ApgMapExp.ApgMapExpVisitor<String, Void> visitor = mock(ApgMapExp.ApgMapExpVisitor.class);
      v.accept(null, visitor);
      verify(visitor).visit(null, v);
    }

    @Test
    void coAcceptDelegatesToCoVisitor() {
      ApgMapExpVar v = new ApgMapExpVar("m");
      @SuppressWarnings("unchecked")
      ApgMapExp.ApgMapExpCoVisitor<String, Void> coVisitor =
          mock(ApgMapExp.ApgMapExpCoVisitor.class);
      v.coaccept(null, coVisitor, "r");
      verify(coVisitor).visitApgMapExpVar(null, "r");
    }

    @Test
    void varMethodCreatesNew() {
      var newVar = new ApgMapExpVar("m1").Var("m2");
      assertTrue(newVar instanceof ApgMapExpVar);
      assertEquals("m2", ((ApgMapExpVar) newVar).var);
    }
  }

  @Nested
  class ApgMapExpComposeTest {

    @Test
    void constructorSetsFields() {
      ApgMapExpVar h1 = new ApgMapExpVar("m1");
      ApgMapExpVar h2 = new ApgMapExpVar("m2");
      ApgMapExpCompose compose = new ApgMapExpCompose(h1, h2);
      assertEquals(h1, compose.h1);
      assertEquals(h2, compose.h2);
    }

    @Test
    void equalsSame() {
      ApgMapExpCompose c1 = new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b"));
      ApgMapExpCompose c2 = new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b"));
      assertEquals(c1, c2);
      assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void notEqualsSwapped() {
      assertNotEquals(
          new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b")),
          new ApgMapExpCompose(new ApgMapExpVar("b"), new ApgMapExpVar("a")));
    }

    @Test
    void toStringContainsSemicolon() {
      String s = new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b")).toString();
      assertTrue(s.contains(";"));
    }

    @Test
    void depsUnionsBoth() {
      ApgMapExpCompose compose = new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b"));
      assertEquals(2, compose.deps().size());
    }

    @Test
    void acceptReturnsNull() {
      // ApgMapExpCompose.accept returns null (commented-out delegation)
      ApgMapExpCompose compose = new ApgMapExpCompose(new ApgMapExpVar("a"), new ApgMapExpVar("b"));
      @SuppressWarnings("unchecked")
      ApgMapExp.ApgMapExpVisitor<String, Void> visitor = mock(ApgMapExp.ApgMapExpVisitor.class);
      assertNull(compose.accept(null, visitor));
    }

    @Test
    void typeThrowsOnIntermediateMismatch() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts", Unit.unit);
      typing.defs.apgschemas.put("s1", new ApgTyExpVar("ts"));
      typing.defs.apgschemas.put("s2", new ApgTyExpVar("ts"));
      typing.defs.apgschemas.put("s3", new ApgTyExpVar("ts"));
      typing.defs.apgmappings.put("m1", new Pair<>(new ApgSchExpVar("s1"), new ApgSchExpVar("s2")));
      typing.defs.apgmappings.put("m2", new Pair<>(new ApgSchExpVar("s3"), new ApgSchExpVar("s1")));
      ApgMapExpCompose compose =
          new ApgMapExpCompose(new ApgMapExpVar("m1"), new ApgMapExpVar("m2"));
      assertThrows(RuntimeException.class, () -> compose.type(typing));
    }

    @Test
    void typeSucceedsOnMatchingIntermediate() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts", Unit.unit);
      typing.defs.apgschemas.put("s1", new ApgTyExpVar("ts"));
      typing.defs.apgschemas.put("s2", new ApgTyExpVar("ts"));
      typing.defs.apgschemas.put("s3", new ApgTyExpVar("ts"));
      typing.defs.apgmappings.put("m1", new Pair<>(new ApgSchExpVar("s1"), new ApgSchExpVar("s2")));
      typing.defs.apgmappings.put("m2", new Pair<>(new ApgSchExpVar("s2"), new ApgSchExpVar("s3")));
      ApgMapExpCompose compose =
          new ApgMapExpCompose(new ApgMapExpVar("m1"), new ApgMapExpVar("m2"));
      Pair<ApgSchExp, ApgSchExp> result = compose.type(typing);
      assertEquals(new ApgSchExpVar("s1"), result.first);
      assertEquals(new ApgSchExpVar("s3"), result.second);
    }
  }

  @Test
  void optionsReturnsEmptyMap() {
    assertTrue(new ApgMapExpVar("x").options().isEmpty());
  }

  /**
   * Literal mappings from S to T whose lambda breaks one typing rule. The first group is caught
   * while the lambda is inferred, the second while the mapping checks it.
   */
  @Nested
  class IllTypedLiterals {

    private static final String SCHEMAS =
        """
        apg_typeside Ty = literal { types Str -> "java.lang.String" "x => x"
          Nat -> "java.lang.Integer" "parseInt"
          functions append : Str,Str -> Str = "(x, y) => x.concat(y)" }
        apg_schema T = literal : Ty { labels tLbl -> (age: base Nat * name: base Str) }
        apg_schema S = literal : Ty { labels sLbl -> (age: base Str * name: base Nat * ssn: base Nat) }
        """;

    private String error(String body) throws Exception {
      String program =
          SCHEMAS
              + "apg_mapping F = literal : S -> T { labels sLbl -> lambda v: label tLbl. "
              + body
              + " }\n";
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      return d.env.exn == null ? "" : d.env.exn.getMessage();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "(ssn: 0@Nat, name: .age(!tLbl(w)), age: .name(!tLbl(v)))"
              + "|Unbound variable or un-annotated value: w",
          "(ssn: 0@Nat, name: .age(!nope(v)), age: .name(!tLbl(v)))"
              + "|Argument type tLbl is not label nope",
          "(ssn: 0@Nat, name: .age(!tLbl(.age(!tLbl(v)))), age: .name(!tLbl(v)))"
              + "|Argument type Nat is not label tLbl",
          "(ssn: 0@Nat, name: .age(v), age: .name(!tLbl(v)))"
              + "|Argument type tLbl is not a product type containing age",
          "(ssn: 0@Nat, name: .foo(!tLbl(v)), age: .name(!tLbl(v)))"
              + "|In .foo(!tLbl(v)) cannot project foo from null of type",
          "(ssn: 0@Nat, name: .age(!tLbl(v)), age: append(.name(!tLbl(v))))"
              + "|In append(.name(!tLbl(v))), given 1 arguments, not 2 as expected.",
          "(ssn: 0@Nat, name: .age(!tLbl(v)), age: append(.age(!tLbl(v)), .name(!tLbl(v))))"
              + "|argument 0 (.age(!tLbl(v))) has type",
          "(ssn: 0@Nat, name: .age(!tLbl(v)), age: foo(.name(!tLbl(v))))|Not a UDF: foo"
        })
    void reportsTheRuleThatTheLambdaBreaks(String body, String message) throws Exception {
      String msg = error(body);
      assertTrue(msg.contains(message), msg);
    }

    @Test
    void acceptsTheWellTypedLambda() throws Exception {
      assertEquals(
          "",
          error(
              "(ssn: 0@Nat, name: .age(!tLbl(v)), age: append(.name(!tLbl(v)), .name(!tLbl(v))))"));
    }
  }

  /**
   * Two programs run one after the other in one JVM, as in one session of the IDE. Each is well
   * typed on its own, but both build the injection {@code <hcA: !hcLbl(v)>}, and terms equal up to
   * their annotation are shared, so the second program checks it against the sum of the first.
   */
  @Nested
  class TermsSharedAcrossPrograms {

    private String program(String other) {
      String sum = "<hcA: base Str + " + other + ">";
      return """
          apg_typeside Ty = literal { types Str -> "java.lang.String" "x => x"
            Nat -> "java.lang.Integer" "parseInt" }
          apg_schema T = literal : Ty { labels hcLbl -> base Str }
          apg_schema S = literal : Ty { labels sLbl -> %s }
          apg_mapping F = literal : S -> T { labels sLbl -> lambda v: label hcLbl. <hcA: !hcLbl(v)> : %s }
          """
          .formatted(sum, sum);
    }

    private String error(String program) throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      return d.env.exn == null ? "" : d.env.exn.getMessage();
    }

    @Test
    void checksAnInjectionAgainstTheSumOfAnEarlierProgram() throws Exception {
      assertEquals("", error(program("hcB: base Nat")));
      String msg = error(program("hcC: base Str"));
      assertTrue(msg.contains("Term <hcA:!hcLbl(v)> has type <"), msg);
      assertTrue(msg.contains("hcB:Nat"), msg);
    }
  }

  /**
   * Lambdas from a natural number into the variant {@code cLbl -> <age: base Nat + name: base
   * Str>}.
   */
  @Nested
  class IllTypedVariants {

    private static final String SCHEMAS =
        """
        apg_typeside Ty = literal { types Str -> "java.lang.String" "x => x"
          Nat -> "java.lang.Integer" "parseInt" }
        apg_schema T = literal : Ty { labels cLbl -> <age: base Nat + name: base Str> }
        apg_schema S = literal : Ty { labels sLbl -> base Nat }
        """;

    private static final String SUM = "<age: base Nat + name: base Str>";

    private String error(String body) throws Exception {
      return ApgExpTestHelpers.runError(
          SCHEMAS
              + "apg_mapping F = literal : S -> T { labels sLbl -> lambda v: label cLbl. "
              + body
              + " }\n");
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "case v where age -> lambda x. x name -> lambda y. 0@Nat : base Nat"
              + "|Argument type cLbl is not a variant.",
          "case !cLbl(v) where age -> lambda x. x : base Nat"
              + "|Branches are [age], not {name, age} as expected from argument.",
          "case !cLbl(v) where age -> lambda v. v name -> lambda y. 0@Nat : base Nat"
              + "|Re-bound variable: v",
          "case !cLbl(v) where age -> lambda x. x name -> lambda y. y : base Nat"
              + "|Branch return type Str is not Nat as expected.",
          "0@Foo|Not a base type: Foo",
          ".a((a: 0@Nat, a: 1@Nat))|Duplicate field: a"
        })
    void reportsTheRuleThatTheLambdaBreaks(String body, String message) throws Exception {
      String msg = error(body);
      assertTrue(msg.contains(message), msg);
    }

    /** The message for an injection of the wrong type has no space after the injected term. */
    @Test
    void checksTheAnnotationOfAnInjection() throws Exception {
      String cases = " where age -> lambda x. x name -> lambda y. 0@Nat : base Nat";
      String msg = error("case <age: x@Str> : " + SUM + cases);
      assertTrue(msg.contains("xhas type Str and not Nat as expected."), msg);
      msg = error("case <zz: 0@Nat> : " + SUM + cases);
      assertTrue(msg.contains("Not a variant type containing zz: <name:Str + age:Nat>"), msg);
      assertEquals("", error("case !cLbl(v)" + cases));
    }

    @Test
    void refusesSchemasOnDifferentTypesides() {
      assertEquals(
          "Typeside mismatch: Ty2 and Ty",
          ApgExpTestHelpers.typeError(
              "apg_mapping X = literal : S2 -> S { labels L -> lambda v: label L. !L(v) }"));
    }
  }
}
