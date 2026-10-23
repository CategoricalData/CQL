package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The polyglot context that runs a typeside's javascript. */
class ExternalCodeUtilsTest {

  @Test
  void evaluatesSourceAsAJavaValue() {
    try (ExternalCodeUtils js = new ExternalCodeUtils()) {
      assertEquals(3, js.eval(ExternalCodeUtils.LANG_JS, Integer.class, "1 + 2"));
      assertEquals("ab", js.eval("js", String.class, "'a' + 'b'"));
    }
  }

  @Test
  void invokesAFunctionOnJavaArguments() {
    try (ExternalCodeUtils js = new ExternalCodeUtils()) {
      assertEquals("xy", js.invoke("js", String.class, "(a, b) => a + b", "x", "y"));
    }
  }

  @Test
  void bindsAJavaValueToAName() {
    try (ExternalCodeUtils js = new ExternalCodeUtils()) {
      js.bind("js", "k", 5);
      assertEquals(10, js.eval("js", Integer.class, "k * 2"));
    }
  }

  @Test
  void looksUpJavaClasses() {
    try (ExternalCodeUtils js = new ExternalCodeUtils()) {
      assertEquals(2, js.eval("js", Integer.class, "Java.type('java.lang.Math').max(1, 2)"));
    }
  }

  @Test
  void namesItsLanguages() {
    try (ExternalCodeUtils js = new ExternalCodeUtils()) {
      assertEquals(Optional.of("JavaScript"), js.getLanguageName("js"));
      assertEquals(Optional.empty(), js.getLanguageName("nope"));
    }
  }

  @Test
  void evaluatesNothingOnceClosed() {
    ExternalCodeUtils js = new ExternalCodeUtils();
    js.close();
    assertThrows(IllegalStateException.class, () -> js.eval("js", Integer.class, "1"));
  }
}
