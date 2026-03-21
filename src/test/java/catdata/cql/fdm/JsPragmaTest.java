package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import catdata.Program;
import catdata.cql.exp.AqlEnv;
import catdata.cql.exp.Exp;

class JsPragmaTest {

  @Test
  void constructorCreatesInstance() {
    Program<Exp<?>> prog = new Program<>(Collections.emptyList(), "");
    AqlEnv env = new AqlEnv(prog);
    var pragma = new JsPragma(Collections.emptyList(), Collections.emptyMap(), env);
    assertNotNull(pragma);
  }

  @Test
  void toStringReturnsEmptyBeforeExecute() {
    Program<Exp<?>> prog = new Program<>(Collections.emptyList(), "");
    AqlEnv env = new AqlEnv(prog);
    var pragma = new JsPragma(Collections.emptyList(), Collections.emptyMap(), env);
    assertEquals("", pragma.toString());
  }
}
