package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.cql.Instance;
import catdata.cql.Term;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AnonymizedTest {

  @Nested
  class Constructor {

    @Test
    void constructorSetsSchema() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertSame(inst.schema(), anon.schema());
    }
  }

  @Nested
  class InstanceBehavior {

    @Test
    void gensDelegates() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertNotNull(anon.gens());
    }

    @Test
    void sksDelegates() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertNotNull(anon.sks());
    }

    @Test
    void algebraIsNotNull() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertNotNull(anon.algebra());
    }

    @Test
    void dpIsNotNull() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertNotNull(anon.dp());
    }

    @Test
    void requireConsistencyDelegates() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertEquals(inst.requireConsistency(), anon.requireConsistency());
    }

    @Test
    void allowUnsafeJavaDelegates() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertEquals(inst.allowUnsafeJava(), anon.allowUnsafeJava());
    }

    @Test
    void algebraPreservesEntitySize() {
      var inst = FdmTestHelpers.instanceWithAtt();
      var anon = new Anonymized<>(inst);
      assertEquals(inst.algebra().size("E"), anon.algebra().size("E"));
    }
  }

  /** Instances built by running a program, so that their values are Java objects. */
  @Nested
  class Values {

    private static final String SQL =
        """
        typeside Ty = sql
        schema S = literal : Ty { entities A attributes n : A -> Varchar k : A -> Integer }
        instance I = literal : S {
          generators a b c : A
          equations a.n = hello b.n = world c.n = hello a.k = 5 b.k = 7 c.k = null
        }
        """;

    private static final String STRINGS =
        """
        typeside Ty = literal {
          external_types String -> "java.lang.String"
          external_parsers String -> "x => x"
        }
        schema S = literal : Ty { entities A attributes n : A -> String }
        instance I = literal : S { generators a : A equations a.n = hello }
        """;

    @SuppressWarnings("unchecked")
    private Instance<String, String, Sym, Fk, Att, String, String, Object, Object> instance(
        String program) throws Exception {
      AqlMultiDriver d =
          new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
      d.start();
      return (Instance<String, String, Sym, Fk, Att, String, String, Object, Object>)
          d.env.defs.insts.get("I");
    }

    private Object value(
        Instance<String, String, Sym, Fk, Att, String, String, Object, Object> i,
        String att,
        String gen) {
      return i.algebra().att(Att.Att("A", att), i.algebra().gen(gen)).obj();
    }

    /**
     * k is a Java Integer, r a Java Double, and t of a type without Java values, given as g of the
     * labelled null u, with g(u) = u kept as an equation of the type algebra.
     */
    private static final String MIXED =
        """
        typeside Ty = literal {
          types T
          functions g : T -> T
          external_types Int -> "java.lang.Integer" Dbl -> "java.lang.Double"
          external_parsers Int -> "x => java.lang.Integer.parseInt(x)"
            Dbl -> "x => java.lang.Double.parseDouble(x)"
        }
        schema S = literal : Ty { entities A B foreign_keys f : A -> B
          attributes k : A -> Int r : A -> Dbl t : A -> T }
        instance I = literal : S { generators a1 a2 : A b : B u : T
          equations a1.f = b a2.f = b a1.k = 5 a2.k = 7 a1.r = "1.5" a1.t = g(u) a2.t = u g(u) = u
          options require_consistency = false }
        """;

    @Test
    void renamesIntegersButKeepsOtherValuesAndTerms() throws Exception {
      var inst = instance(MIXED);
      var anon = new Anonymized<>(inst);
      var alg = anon.algebra();
      Object k = value(anon, "k", "a1");
      assertInstanceOf(Integer.class, k);
      assertNotEquals(5, k);
      assertEquals(1.5, value(anon, "r", "a1"));
      assertEquals("g(\"inl u\")", alg.att(Att.Att("A", "t"), alg.gen("a1")).toString());
      assertEquals(inst.algebra().sk("u"), alg.sk("u"));
      assertEquals(1, alg.talg().eqsNoDefns().size());
      assertFalse(alg.hasFreeTypeAlgebra());
      assertTrue(alg.hasFreeTypeAlgebraOnJava());
    }

    @Test
    void decidesEquationsOnRenamedIntegersAndOnRows() throws Exception {
      var anon = new Anonymized<>(instance(MIXED));
      var dp = anon.dp();
      Term<String, String, Sym, Fk, Att, String, String> a1 = Term.Gen("a1");
      Term<String, String, Sym, Fk, Att, String, String> k = Term.Att(Att.Att("A", "k"), a1);
      assertTrue(dp.eq(null, k, Term.Obj(value(anon, "k", "a1"), "Int")));
      Fk f = Fk.Fk("A", "f");
      assertTrue(dp.eq(null, Term.Fk(f, a1), Term.Fk(f, Term.Gen("a2"))));
      RuntimeException e =
          assertThrows(
              RuntimeException.class,
              () -> dp.eq(Map.of("v", Chc.inRight("A")), Term.Var("v"), Term.Var("v")));
      assertEquals("Cannot answer a non-ground equation", e.getMessage());
    }

    @Test
    void delegatesTheRowsAndNamesOfTheAlgebra() throws Exception {
      var inst = instance(MIXED);
      var alg = new Anonymized<>(inst).algebra();
      Object a1 = alg.gen("a1");
      assertEquals(inst.schema(), alg.schema());
      assertEquals(alg.gen("b"), alg.fk(Fk.Fk("A", "f"), a1));
      for (Object y : alg.talg().sks.keySet()) {
        assertEquals(inst.algebra().printY("T", y), alg.printY("T", y));
        assertEquals(inst.algebra().reprT_prot(y), alg.reprT_prot(y));
      }
    }

    @Test
    void renamesEachStringAndIntegerConsistently() throws Exception {
      var anon = new Anonymized<>(instance(SQL));
      Object a = value(anon, "n", "a");
      Object b = value(anon, "n", "b");
      assertEquals(a, value(anon, "n", "c"));
      assertNotEquals(a, b);
      assertTrue(((Optional<?>) a).get().toString().startsWith("Str"), a.toString());
      Set<Object> ks = new HashSet<>();
      ks.add(value(anon, "k", "a"));
      ks.add(value(anon, "k", "b"));
      assertFalse(ks.contains(Optional.of(5)) && ks.contains(Optional.of(7)), ks.toString());
      assertEquals(Optional.empty(), value(anon, "k", "c"));
    }

    @Test
    void delegatesTheRestOfTheAlgebra() throws Exception {
      var inst = instance(SQL);
      var anon = new Anonymized<>(inst);
      var alg = anon.algebra();
      Object a = alg.gen("a");
      assertEquals(inst.algebra().repr("A", a), alg.repr("A", a));
      assertEquals(inst.algebra().printX("A", a), alg.printX("A", a));
      assertEquals(inst.algebra().toStringProver(), alg.toStringProver());
      assertEquals(inst.algebra().hasNulls(), alg.hasNulls());
      assertEquals(inst.dp().toStringProver(), anon.dp().toStringProver());
      assertEquals(3, alg.size("A"));
      assertTrue(alg.hasFreeTypeAlgebra());
      assertTrue(alg.hasFreeTypeAlgebraOnJava());
    }

    /** The decision procedure maps a value back before asking the original instance. */
    @Test
    void decidesAnEquationOnAJavaStringByItsOriginal() throws Exception {
      var anon = new Anonymized<>(instance(STRINGS));
      Term<String, String, Sym, Fk, Att, String, String> n =
          Term.Att(Att.Att("A", "n"), Term.Gen("a"));
      Object renamed = value(anon, "n", "a");
      assertNotEquals("hello", renamed);
      assertTrue(anon.dp().eq(null, n, Term.Obj(renamed, "String")));
    }

    /**
     * An optional value is renamed inside the optional, but mapped back only when the Java type is
     * String or Integer, so on the SQL typeside the procedure rejects the instance's own value.
     */
    @Test
    void rejectsItsOwnValueOnTheSqlTypeside() throws Exception {
      var anon = new Anonymized<>(instance(SQL));
      Term<String, String, Sym, Fk, Att, String, String> n =
          Term.Att(Att.Att("A", "n"), Term.Gen("a"));
      assertFalse(anon.dp().eq(null, n, Term.Obj(value(anon, "n", "a"), "Varchar")));
    }
  }
}
