package catdata.cql;

import static catdata.cql.CqlTestHelpers.colors;
import static catdata.cql.CqlTestHelpers.ops;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import catdata.Pair;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Test;

/** A theory morphism. MorExpRaw returns null instead of one, so no CQL program makes a Mor. */
class MorTest {

  @Test
  void keepsItsTablesAndHasTheSizeOfItsSource() {
    TypeSide<String, Sym> src = colors();
    TypeSide<String, Sym> dst = TypeSide.initial(ops());
    Map<String, String> tys = Map.of(CqlTestHelpers.COLOR, "Hue");
    Map<Sym, Pair<List<String>, Term<String, Void, Sym, Void, Void, Void, Void>>> syms =
        Map.of(CqlTestHelpers.RED, new Pair<>(List.of(), Term.Var("x")));
    Mor<String, Sym, String, Sym> m = new Mor<>(tys, syms, src, dst, false);
    assertEquals(Kind.THEORY_MORPHISM, m.kind());
    assertEquals(4, m.size());
    assertEquals(src.size(), m.size());
    assertSame(tys, m.tys);
    assertSame(syms, m.syms);
    assertSame(src, m.src);
    assertSame(dst, m.dst);
  }
}
