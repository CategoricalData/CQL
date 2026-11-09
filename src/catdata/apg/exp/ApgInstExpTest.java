package catdata.apg.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import catdata.Pair;
import catdata.Unit;
import catdata.apg.exp.ApgInstExp.ApgInstExpCoEqualize;
import catdata.apg.exp.ApgInstExp.ApgInstExpDelta;
import catdata.apg.exp.ApgInstExp.ApgInstExpEqualize;
import catdata.apg.exp.ApgInstExp.ApgInstExpInitial;
import catdata.apg.exp.ApgInstExp.ApgInstExpPlus;
import catdata.apg.exp.ApgInstExp.ApgInstExpTerminal;
import catdata.apg.exp.ApgInstExp.ApgInstExpTimes;
import catdata.apg.exp.ApgInstExp.ApgInstExpVar;
import catdata.apg.exp.ApgMapExp.ApgMapExpVar;
import catdata.apg.exp.ApgSchExp.ApgSchExpVar;
import catdata.apg.exp.ApgTransExp.ApgTransExpVar;
import catdata.apg.exp.ApgTyExp.ApgTyExpVar;
import catdata.cql.Kind;
import catdata.cql.exp.AqlTyping;
import catdata.cql.exp.Exp;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApgInstExpTest {

  @Nested
  class ApgInstExpVarTest {

    @Test
    void constructorSetsVar() {
      assertEquals("i1", new ApgInstExpVar("i1").var);
    }

    @Test
    void kindReturnsApgInstance() {
      assertEquals(Kind.APG_instance, new ApgInstExpVar("i").kind());
    }

    @Test
    void isVarReturnsTrue() {
      assertTrue(new ApgInstExpVar("i").isVar());
    }

    @Test
    void toStringReturnsVar() {
      assertEquals("myInst", new ApgInstExpVar("myInst").toString());
    }

    @Test
    void depsReturnsSingleton() {
      Collection<Pair<String, Kind>> deps = new ApgInstExpVar("i1").deps();
      assertEquals(1, deps.size());
      Pair<String, Kind> dep = deps.iterator().next();
      assertEquals("i1", dep.first);
      assertEquals(Kind.APG_instance, dep.second);
    }

    @Test
    void equalsSameVar() {
      ApgInstExpVar v1 = new ApgInstExpVar("i");
      ApgInstExpVar v2 = new ApgInstExpVar("i");
      assertEquals(v1, v2);
      assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void notEqualsDifferentVar() {
      assertNotEquals(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
    }

    @Test
    void equalsReflexive() {
      ApgInstExpVar v = new ApgInstExpVar("i");
      assertEquals(v, v);
    }

    @Test
    void notEqualsNull() {
      assertNotEquals(null, new ApgInstExpVar("i"));
    }

    @Test
    void typeThrowsWhenUndefined() {
      AqlTyping typing = new AqlTyping();
      assertThrows(RuntimeException.class, () -> new ApgInstExpVar("missing").type(typing));
    }

    @Test
    void typeReturnsSchExpWhenDefined() {
      AqlTyping typing = new AqlTyping();
      ApgSchExpVar schExp = new ApgSchExpVar("s1");
      typing.defs.apgis.put("i1", schExp);
      assertEquals(schExp, new ApgInstExpVar("i1").type(typing));
    }

    @Test
    void acceptDelegatesToVisitor() throws Exception {
      ApgInstExpVar v = new ApgInstExpVar("i");
      @SuppressWarnings("unchecked")
      ApgInstExp.ApgInstExpVisitor<String, Void, RuntimeException> visitor =
          mock(ApgInstExp.ApgInstExpVisitor.class);
      v.accept(null, visitor);
      verify(visitor).visit(null, v);
    }

    @Test
    void varMethodCreatesNew() {
      var newVar = new ApgInstExpVar("i1").Var("i2");
      assertTrue(newVar instanceof ApgInstExpVar);
      assertEquals("i2", ((ApgInstExpVar) newVar).var);
    }
  }

  @Nested
  class ApgInstExpInitialTest {

    @Test
    void constructorSetsTypeside() {
      ApgSchExpVar sch = new ApgSchExpVar("s");
      ApgInstExpInitial init = new ApgInstExpInitial(sch);
      assertEquals(sch, init.typeside);
    }

    @Test
    void equalsSame() {
      ApgInstExpInitial i1 = new ApgInstExpInitial(new ApgSchExpVar("s"));
      ApgInstExpInitial i2 = new ApgInstExpInitial(new ApgSchExpVar("s"));
      assertEquals(i1, i2);
      assertEquals(i1.hashCode(), i2.hashCode());
    }

    @Test
    void notEqualsDifferent() {
      assertNotEquals(
          new ApgInstExpInitial(new ApgSchExpVar("a")),
          new ApgInstExpInitial(new ApgSchExpVar("b")));
    }

    @Test
    void toStringContainsEmpty() {
      assertTrue(new ApgInstExpInitial(new ApgSchExpVar("s")).toString().contains("empty"));
    }

    @Test
    void typeReturnsTypeside() {
      AqlTyping typing = new AqlTyping();
      ApgTyExpVar tyExp = new ApgTyExpVar("ts");
      typing.defs.apgschemas.put("s", tyExp);
      ApgSchExpVar sch = new ApgSchExpVar("s");
      assertEquals(sch, new ApgInstExpInitial(sch).type(typing));
    }

    @Test
    void acceptDelegatesToVisitor() throws Exception {
      ApgInstExpInitial init = new ApgInstExpInitial(new ApgSchExpVar("s"));
      @SuppressWarnings("unchecked")
      ApgInstExp.ApgInstExpVisitor<String, Void, RuntimeException> visitor =
          mock(ApgInstExp.ApgInstExpVisitor.class);
      init.accept(null, visitor);
      verify(visitor).visit(null, init);
    }
  }

  @Nested
  class ApgInstExpTerminalTest {

    @Test
    void constructorSetsTypeside() {
      ApgTyExpVar ts = new ApgTyExpVar("ts");
      ApgInstExpTerminal term = new ApgInstExpTerminal(ts);
      assertEquals(ts, term.typeside);
    }

    @Test
    void equalsSame() {
      ApgInstExpTerminal t1 = new ApgInstExpTerminal(new ApgTyExpVar("ts"));
      ApgInstExpTerminal t2 = new ApgInstExpTerminal(new ApgTyExpVar("ts"));
      assertEquals(t1, t2);
    }

    @Test
    void toStringContainsUnit() {
      assertTrue(new ApgInstExpTerminal(new ApgTyExpVar("ts")).toString().contains("unit"));
    }

    @Test
    void typeReturnsSchExpTerminal() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts", Unit.unit);
      ApgTyExpVar ts = new ApgTyExpVar("ts");
      ApgSchExp result = new ApgInstExpTerminal(ts).type(typing);
      assertTrue(result instanceof ApgSchExp.ApgSchExpTerminal);
    }
  }

  @Nested
  class ApgInstExpTimesTest {

    @Test
    void constructorSetsFields() {
      ApgInstExpVar l = new ApgInstExpVar("i1");
      ApgInstExpVar r = new ApgInstExpVar("i2");
      ApgInstExpTimes times = new ApgInstExpTimes(l, r);
      assertEquals(l, times.l);
      assertEquals(r, times.r);
    }

    @Test
    void equalsSame() {
      ApgInstExpTimes t1 = new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      ApgInstExpTimes t2 = new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      assertEquals(t1, t2);
      assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void notEqualsSwapped() {
      assertNotEquals(
          new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b")),
          new ApgInstExpTimes(new ApgInstExpVar("b"), new ApgInstExpVar("a")));
    }

    @Test
    void toStringContainsStar() {
      assertTrue(
          new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b"))
              .toString()
              .contains("*"));
    }

    @Test
    void depsUnionsBoth() {
      ApgInstExpTimes times = new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      assertEquals(2, times.deps().size());
    }

    @Test
    void typeThrowsOnDifferentTypesides() {
      AqlTyping typing = new AqlTyping();
      typing.defs.apgts.put("ts1", Unit.unit);
      typing.defs.apgts.put("ts2", Unit.unit);
      typing.defs.apgschemas.put("s1", new ApgTyExpVar("ts1"));
      typing.defs.apgschemas.put("s2", new ApgTyExpVar("ts2"));
      typing.defs.apgis.put("a", new ApgSchExpVar("s1"));
      typing.defs.apgis.put("b", new ApgSchExpVar("s2"));
      ApgInstExpTimes times = new ApgInstExpTimes(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      assertThrows(RuntimeException.class, () -> times.type(typing));
    }
  }

  @Nested
  class ApgInstExpPlusTest {

    @Test
    void constructorSetsFields() {
      ApgInstExpVar l = new ApgInstExpVar("i1");
      ApgInstExpVar r = new ApgInstExpVar("i2");
      ApgInstExpPlus plus = new ApgInstExpPlus(l, r);
      assertEquals(l, plus.l);
      assertEquals(r, plus.r);
    }

    @Test
    void equalsSame() {
      ApgInstExpPlus p1 = new ApgInstExpPlus(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      ApgInstExpPlus p2 = new ApgInstExpPlus(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      assertEquals(p1, p2);
      assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void toStringContainsPlus() {
      assertTrue(
          new ApgInstExpPlus(new ApgInstExpVar("a"), new ApgInstExpVar("b"))
              .toString()
              .contains("+"));
    }

    @Test
    void acceptDelegatesToVisitor() throws Exception {
      ApgInstExpPlus plus = new ApgInstExpPlus(new ApgInstExpVar("a"), new ApgInstExpVar("b"));
      @SuppressWarnings("unchecked")
      ApgInstExp.ApgInstExpVisitor<String, Void, RuntimeException> visitor =
          mock(ApgInstExp.ApgInstExpVisitor.class);
      plus.accept(null, visitor);
      verify(visitor).visit(null, plus);
    }
  }

  @Nested
  class ApgInstExpEqualizeTest {

    @Test
    void constructorSetsFields() {
      ApgTransExpVar l = new ApgTransExpVar("t1");
      ApgTransExpVar r = new ApgTransExpVar("t2");
      ApgInstExpEqualize eq = new ApgInstExpEqualize(l, r);
      assertEquals(l, eq.l);
      assertEquals(r, eq.r);
    }

    @Test
    void equalsSame() {
      ApgInstExpEqualize e1 =
          new ApgInstExpEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"));
      ApgInstExpEqualize e2 =
          new ApgInstExpEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"));
      assertEquals(e1, e2);
      assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    void toStringContainsEqualize() {
      assertTrue(
          new ApgInstExpEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"))
              .toString()
              .contains("equalize"));
    }

    @Test
    void depsUnionsBoth() {
      ApgInstExpEqualize eq =
          new ApgInstExpEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"));
      assertEquals(2, eq.deps().size());
    }
  }

  @Nested
  class ApgInstExpCoEqualizeTest {

    @Test
    void constructorSetsFields() {
      ApgTransExpVar l = new ApgTransExpVar("t1");
      ApgTransExpVar r = new ApgTransExpVar("t2");
      ApgInstExpCoEqualize ceq = new ApgInstExpCoEqualize(l, r);
      assertEquals(l, ceq.l);
      assertEquals(r, ceq.r);
    }

    @Test
    void equalsSame() {
      ApgInstExpCoEqualize c1 =
          new ApgInstExpCoEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"));
      ApgInstExpCoEqualize c2 =
          new ApgInstExpCoEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"));
      assertEquals(c1, c2);
    }

    @Test
    void toStringContainsCoequalize() {
      assertTrue(
          new ApgInstExpCoEqualize(new ApgTransExpVar("t1"), new ApgTransExpVar("t2"))
              .toString()
              .contains("coequalize"));
    }
  }

  @Nested
  class ApgInstExpDeltaTest {

    @Test
    void constructorSetsFields() {
      ApgMapExpVar f = new ApgMapExpVar("m");
      ApgInstExpVar j = new ApgInstExpVar("i");
      ApgInstExpDelta delta = new ApgInstExpDelta(f, j);
      assertEquals(f, delta.F);
      assertEquals(j, delta.J);
    }

    @Test
    void equalsSame() {
      ApgInstExpDelta d1 = new ApgInstExpDelta(new ApgMapExpVar("m"), new ApgInstExpVar("i"));
      ApgInstExpDelta d2 = new ApgInstExpDelta(new ApgMapExpVar("m"), new ApgInstExpVar("i"));
      assertEquals(d1, d2);
      assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    void toStringContainsDelta() {
      assertTrue(
          new ApgInstExpDelta(new ApgMapExpVar("m"), new ApgInstExpVar("i"))
              .toString()
              .contains("delta"));
    }

    @Test
    void depsUnionsBoth() {
      ApgInstExpDelta delta = new ApgInstExpDelta(new ApgMapExpVar("m"), new ApgInstExpVar("i"));
      assertEquals(2, delta.deps().size());
    }
  }

  @Test
  void optionsReturnsEmptyMap() {
    assertTrue(new ApgInstExpVar("x").options().isEmpty());
  }

  @Nested
  class IllTypedOperations {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "<I + I2>|Different typesides: Ty and Ty2",
          "(I * I2)|Different typesides: Ty and Ty2",
          "equalize h k|Domain mismatch: I is not equal to J",
          "equalize h ii|CoDomain mismatch: J is not equal to I",
          "coequalize h k|Domain mismatch: I is not equal to J",
          "coequalize h ii|CoDomain mismatch: J is not equal to I",
          "delta F I2|Typeside mismatch",
          "delta F (empty T)|Instance is on schema T but mapping target is S"
        })
    void reportsTheMismatch(String exp, String message) {
      assertEquals(message, ApgExpTestHelpers.typeError("apg_instance X = " + exp));
    }
  }

  /**
   * Literal instances on the schema of a user {@code User -> ()}, a name {@code name -> (User:
   * label User * name: base Str)} and a choice {@code choice -> <a: base Str + b: base Str>}, each
   * with the user {@code u1} and one more element.
   */
  @Nested
  class IllFormedLiterals {

    private static final String SCHEMA =
        """
        apg_typeside Ty = literal { types Str -> "java.lang.String" "x => x" }
        apg_schema S = literal : Ty { labels User -> ()
          name -> (User: label User * name: base Str) choice -> <a: base Str + b: base Str> }
        """;

    private String error(String element) throws Exception {
      return ApgExpTestHelpers.runError(
          SCHEMA + "apg_instance I = literal : S { elements u1 : User -> () " + element + " }\n");
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "n1 : Nobody -> ()|Nobody not found in ",
          "n1 : name -> (User: u9, name: A)|Not an element: u9",
          "n1 : name -> (User: u1, name: ())|Expecting data at type Str, but received ()",
          "n1 : name -> (User: (), name: A)|Expecting element at label User, but received ()",
          "n1 : name -> (User: n1, name: A)"
              + "|Expecting element at label User, but received element n1 at label name",
          "n1 : name -> (User: u1)|In (User: u1), no field for name"
        })
    void reportsTheElementThatDoesNotFit(String element, String message) throws Exception {
      String msg = error(element);
      assertTrue(msg.contains(message), msg);
    }

    /** These messages print a tuple or a variant type, whose fields can come in either order. */
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "n1 : name -> A"
              + "|Expecting tuple at type (name:Str * User:User), but received A"
              + "|Expecting tuple at type (User:User * name:Str), but received A",
          "n1 : name -> (User: u1, name: A, extra: B)"
              + "|In (User: u1 , name: A , extra: B), extra, is not a field in (name:Str * User:User)"
              + "|In (User: u1 , name: A , extra: B), extra, is not a field in (User:User * name:Str)",
          "c1 : choice -> A"
              + "|Expecting injection at type <b:Str + a:Str>, but received A"
              + "|Expecting injection at type <a:Str + b:Str>, but received A",
          "c1 : choice -> <c: A>"
              + "|In <c: A>, c, is not a field in <b:Str + a:Str>"
              + "|In <c: A>, c, is not a field in <a:Str + b:Str>"
        })
    void reportsTheTypeThatTheElementDoesNotFit(String element, String message, String swapped)
        throws Exception {
      ApgExpTestHelpers.assertContainsOneOf(error(element), message, swapped);
    }

    @Test
    void acceptsAnInjectionIntoAVariant() throws Exception {
      assertEquals("", error("c1 : choice -> <a: A>"));
    }
  }

  /**
   * A literal instance may import an instance on a schema over another typeside: the check of its
   * imports reads {@code imports()}, which the literal does not override, so it checks none of
   * them.
   */
  @Nested
  class ImportsAcrossTypesides {

    @Test
    void mergesTheElementsOfAnImportOverAnotherTypeside() throws Exception {
      String program =
          ApgExpTestHelpers.BASE
              + "apg_instance X = literal : S { imports I2 elements c : L -> z }\n";
      ApgExpTestHelpers.typing(program);
      var d = ApgExpTestHelpers.run(program);
      assertNull(d.env.exn);
      String x = d.env.defs.apgis.get("X").toString();
      assertTrue(x.contains("a:L -> x"), x);
      assertTrue(x.contains("c:L -> z"), x);
    }

    @Test
    void failsOnlyWhenTheImportedDataDoesNotFit() throws Exception {
      String program =
          """
          apg_typeside Ty = literal { types Str -> "java.lang.String" "x => x" }
          apg_typeside Ty2 = literal { types Nat -> "java.lang.Integer" "parseInt" }
          apg_schema S = literal : Ty { labels L -> base Str }
          apg_schema S2 = literal : Ty2 { labels L -> base Nat }
          apg_instance I2 = literal : S2 { elements a : L -> 5 }
          apg_instance X = literal : S { imports I2 elements c : L -> z }
          """;
      ApgExpTestHelpers.typing(program);
      String msg = ApgExpTestHelpers.runError(program);
      assertTrue(msg.contains("Not an instance of class java.lang.String: 5, is Integer"), msg);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void coVisitsEachKindInItsOwnCase() throws Exception {
    var exps =
        ApgExpTestHelpers.exps(
            ApgExpTestHelpers.BASE
                + """
                apg_instance V = I
                apg_instance E0 = empty S
                apg_instance U0 = unit Ty
                apg_instance Tm = (I * J)
                apg_instance Pl = <I + J>
                apg_instance Eq = equalize h h
                apg_instance Co = coequalize h h
                apg_instance De = delta F I
                """);
    Set<Class<?>> kinds = new HashSet<>();
    for (Exp<?> e : exps.values()) {
      if (e instanceof ApgInstExp i) {
        ApgInstExp.ApgInstExpCoVisitor<String, Void, RuntimeException> v =
            mock(ApgInstExp.ApgInstExpCoVisitor.class);
        i.coaccept(null, v, "r");
        ApgExpTestHelpers.assertCoVisitedItsCase(v, i);
        kinds.add(i.getClass());
      }
    }
    assertEquals(9, kinds.size());
  }

  /**
   * The pullback along a mapping whose case branch reads the lambda's own variable. Each element
   * has a successor or a stop value, and the mapping should read the name of the element itself in
   * the successor branch.
   */
  @Nested
  class DeltaThroughACase {

    private static final String PROGRAM =
        """
        apg_typeside Ty = literal { types Str -> "java.lang.String" "x => x" }
        apg_schema T = literal : Ty {
          labels tLbl -> (k: <next: label tLbl + stop: base Str> * n: base Str) }
        apg_schema S = literal : Ty { labels sLbl -> base Str }
        apg_mapping F = literal : S -> T { labels sLbl -> lambda v: label tLbl.
          case .k(!tLbl(v)) where next -> lambda x. .n(!tLbl(v)) stop -> lambda y. y : base Str }
        apg_instance J = literal : T {
          elements e : tLbl -> (k: <next: f>, n: Bob) f : tLbl -> (k: <stop: Alice>, n: Carol) }
        apg_instance I = delta F J
        """;

    /** e should map to its own name, Bob, but gets Carol, the name of its successor f. */
    @Test
    void readsTheNameOfTheSuccessorInstead() throws Exception {
      var d = ApgExpTestHelpers.run(PROGRAM);
      assertNull(d.env.exn);
      String i = d.env.defs.apgis.get("I").toString();
      assertTrue(i.contains("(sLbl, e):sLbl -> Carol"), i);
      assertTrue(i.contains("(sLbl, f):sLbl -> Alice"), i);
    }
  }

  /**
   * Equalizers of morphisms {@code I -> J} that disagree: k sends b elsewhere than h, and k2 also
   * sends the label L to L2. The equalizer keeps the labels and elements on which the two agree,
   * and a reference to a dropped element or label becomes a unit.
   */
  @Nested
  class Equalizers {

    private static final String PROGRAM =
        """
        apg_typeside Ty = literal { types Str -> "java.lang.String" "x => x" }
        apg_schema S = literal : Ty { labels L -> base Str P -> label L }
        apg_schema S2 = literal : Ty { labels L -> base Str L2 -> base Str P -> label L }
        apg_instance I = literal : S { elements a : L -> x b : L -> y p : P -> b }
        apg_instance J = literal : S2 { elements c : L -> x d : L -> y c2 : L2 -> x r : P -> c }
        apg_morphism h = literal : I -> J { labels L -> L P -> P elements a -> c b -> c p -> r }
        apg_morphism k = literal : I -> J { labels L -> L P -> P elements a -> c b -> d p -> r }
        apg_morphism k2 = literal : I -> J { labels L -> L2 P -> P elements a -> c2 b -> c2 p -> r }
        """;

    private String equalizer(String exp) throws Exception {
      var d = ApgExpTestHelpers.run(PROGRAM + "apg_instance E = " + exp + "\n");
      assertNull(d.env.exn);
      return d.env.defs.apgis.get("E").toString();
    }

    @Test
    void dropsTheElementsOnWhichTheMorphismsDisagree() throws Exception {
      String e = equalizer("equalize h k");
      assertTrue(e.contains("a:L -> x"), e);
      assertTrue(e.contains("p:P -> <unit:()>"), e);
      assertFalse(e.contains("b:L"), e);
    }

    @Test
    void dropsTheLabelsOnWhichTheMorphismsDisagree() throws Exception {
      String e = equalizer("equalize h k2");
      assertEquals("elements\n\tp:P -> ()\n", e);
    }

    @Test
    void refusesMorphismsThatTheMapDoesNotEqualize() throws Exception {
      String msg =
          ApgExpTestHelpers.runError(PROGRAM + "apg_morphism U = equalize_u h k (identity I)\n");
      assertTrue(msg.contains("Not equal: "), msg);
      msg =
          ApgExpTestHelpers.runError(PROGRAM + "apg_morphism U = coequalize_u h k (identity J)\n");
      assertTrue(msg.contains("Not equal: "), msg);
      assertEquals(
          "",
          ApgExpTestHelpers.runError(PROGRAM + "apg_morphism U = equalize_u h h (identity I)\n"));
    }
  }
}
