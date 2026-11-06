package catdata.provers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.Chc;
import catdata.cql.Head;
import catdata.cql.Term;
import catdata.cql.TypeSide;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Sym;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CongruenceProver}, congruence closure over ground equations, which nothing
 * in CQL constructs; {@code prover = congruence} builds {@link CongruenceProverUniform}, which it
 * is compared with. The theory is a typeside with constants a b c d, a function f, and the
 * equations a = b and f(a) = c.
 */
class CongruenceProverTest {

  private static final String TYPESIDE =
      "typeside Ty = literal { types T constants a b c d : T functions f : T -> T equations ";

  private TypeSide<String, Sym> typeside;

  @SuppressWarnings("unchecked")
  private KBTheory<Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String> kb(
      String equations) throws Exception {
    AqlMultiDriver d =
        new AqlMultiDriver(
            AqlParserFactory.getParser().parseProgram(TYPESIDE + equations + " }\n"), null);
    d.start();
    typeside = (TypeSide<String, Sym>) d.env.defs.tys.get("Ty");
    return (KBTheory<Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String>)
        (Object) typeside.collage().toKB();
  }

  private Sym sym(String name) {
    return typeside.syms.keySet().stream()
        .filter(s -> s.toString().equals(name))
        .findFirst()
        .orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> c(String name) {
    return (KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>)
        (Object) Term.Sym(sym(name), List.of());
  }

  @SuppressWarnings("unchecked")
  private KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> f(
      KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String> x) {
    return (KBExp<Head<String, Void, Sym, Void, Void, Void, Void>, String>)
        (Object) Term.Sym(sym("f"), List.of((Term<String, Void, Sym, Void, Void, Void, Void>) x));
  }

  @Test
  void provesWhatTheGroundEquationsImply() throws Exception {
    var kb = kb("a = b f(a) = c");
    var p = new CongruenceProver<>(kb);
    var u = new CongruenceProverUniform<>(kb);
    for (DPKB<Chc<String, Void>, Head<String, Void, Sym, Void, Void, Void, Void>, String> dp :
        List.of(p, u)) {
      assertTrue(dp.eq(Map.of(), f(c("b")), c("c")));
      assertTrue(dp.eq(Map.of(), f(f(c("a"))), f(c("c"))));
      assertFalse(dp.eq(Map.of(), c("a"), c("d")));
    }
    assertFalse(p.supportsTrivialityCheck());
    assertTrue(p.toString().startsWith("CongruenceProver [uf="), p.toString());
  }

  /** A term the theory never mentions is added, with its subterms, before it is compared. */
  @Test
  void takesInTermsItHasNotSeen() throws Exception {
    var p = new CongruenceProver<>(kb("a = b f(a) = c"));
    assertFalse(p.eq(Map.of(), f(f(f(c("d")))), c("d")));
    assertTrue(p.eq(Map.of(), f(f(f(c("b")))), f(f(c("c")))));
  }

  @Test
  void answersOnlyGroundQuestions() throws Exception {
    var p = new CongruenceProver<>(kb("a = b"));
    RuntimeException e =
        assertThrows(
            RuntimeException.class, () -> p.eq(Map.of("v", Chc.inLeft("T")), c("a"), c("b")));
    assertEquals("Congruence prover can only be used with ground equations.", e.getMessage());
  }

  @Test
  void refusesAUniversalEquation() throws Exception {
    var kb = kb("forall x. f(x) = x");
    RuntimeException e = assertThrows(RuntimeException.class, () -> new CongruenceProver<>(kb));
    assertEquals("Congruence closure does not work with universal quantification.", e.getMessage());
  }

  /** The uniform prover refuses the same inputs and adds a constant to its graph as well. */
  @Test
  void refusesTheSameInputsInTheUniformProver() throws Exception {
    var ground = kb("a = b");
    var u = new CongruenceProverUniform<>(ground);
    RuntimeException e =
        assertThrows(
            RuntimeException.class, () -> u.eq(Map.of("v", Chc.inLeft("T")), c("a"), c("b")));
    assertEquals("Congruence prover can only be used with ground equations.", e.getMessage());
    var universal = kb("forall x. f(x) = x");
    e = assertThrows(RuntimeException.class, () -> new CongruenceProverUniform<>(universal));
    assertEquals("Congruence closure does not work with universal quantification.", e.getMessage());
    Head<String, Void, Sym, Void, Void, Void, Void> h =
        Head.SymHead(Sym.Sym("e", new catdata.Pair<>(List.of(), "T")));
    u.add(h, Chc.inLeft("T"));
    assertTrue(ground.syms.containsKey(h));
  }

  @Test
  void addsAConstant() throws Exception {
    var kb = kb("a = b");
    var p = new CongruenceProver<>(kb);
    Head<String, Void, Sym, Void, Void, Void, Void> e =
        Head.SymHead(Sym.Sym("e", new catdata.Pair<>(List.of(), "T")));
    p.add(e, Chc.inLeft("T"));
    assertTrue(kb.syms.containsKey(e));
  }
}
