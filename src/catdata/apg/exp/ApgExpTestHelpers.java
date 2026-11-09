package catdata.apg.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockingDetails;

import catdata.LineException;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.AqlTyping;
import catdata.cql.exp.Exp;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.mockito.invocation.Invocation;

/**
 * A program shared by the tests of the APG expressions, and the ways the IDE checks and runs it.
 */
final class ApgExpTestHelpers {

  private ApgExpTestHelpers() {}

  /**
   * The typesides Ty and Ty2, which have the same base type, the schemas S and T on Ty and S2 on
   * Ty2, the instances I and J on S and I2 on S2, the morphisms {@code h : I -> J}, {@code k : J ->
   * J}, {@code ii : I -> I} and {@code h2 : I2 -> I2}, and the mapping {@code F : T -> S}.
   */
  static final String BASE =
      """
      apg_typeside Ty = literal { types Str -> "java.lang.String" "x => x" }
      apg_typeside Ty2 = literal { types Str -> "java.lang.String" "x => x" }
      apg_schema S = literal : Ty { labels L -> base Str }
      apg_schema S2 = literal : Ty2 { labels L -> base Str }
      apg_schema T = literal : Ty { labels M -> base Str }
      apg_instance I = literal : S { elements a : L -> x }
      apg_instance J = literal : S { elements b : L -> y }
      apg_instance I2 = literal : S2 { elements a : L -> x }
      apg_morphism h = literal : I -> J { labels L -> L elements a -> b }
      apg_morphism k = literal : J -> J { labels L -> L elements b -> b }
      apg_morphism ii = literal : I -> I { labels L -> L elements a -> a }
      apg_morphism h2 = literal : I2 -> I2 { labels L -> L elements a -> a }
      apg_mapping F = literal : T -> S { labels M -> lambda v: label L. !L(v) }
      """;

  /** Type checks the program as the IDE does before running it. */
  static AqlTyping typing(String program) throws Exception {
    return new AqlTyping(AqlParserFactory.getParser().parseProgram(program), false);
  }

  /** The message of the type error that the definition, added to {@link #BASE}, raises. */
  static String typeError(String definition) {
    return assertThrows(LineException.class, () -> typing(BASE + definition + "\n")).getMessage();
  }

  /** Runs the program and returns the message of its first error, or the empty string. */
  static String runError(String program) throws Exception {
    AqlMultiDriver d = run(program);
    return d.env.exn == null ? "" : d.env.exn.getMessage();
  }

  static AqlMultiDriver run(String program) throws Exception {
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    return d;
  }

  /** The expressions of the program, by name. */
  static Map<String, Exp<?>> exps(String program) throws Exception {
    return AqlParserFactory.getParser().parseProgram(program).exps;
  }

  /**
   * Asserts that the message contains one of the texts. Types that differ only in the order of
   * their fields are one cached {@code ApgTy}, which prints its fields in the order of the first
   * one built. That order depends on the tests that ran before, so a message that prints a tuple or
   * a variant type is checked against each order.
   */
  static void assertContainsOneOf(String message, String... texts) {
    assertTrue(Arrays.stream(texts).anyMatch(message::contains), message);
  }

  /** Asserts that the mocked co-visitor got one call, to the case named after the class of e. */
  static void assertCoVisitedItsCase(Object coVisitor, Exp<?> e) {
    Collection<Invocation> calls = mockingDetails(coVisitor).getInvocations();
    List<String> names = calls.stream().map(c -> c.getMethod().getName()).toList();
    assertEquals(List.of("visit" + e.getClass().getSimpleName()), names, e.toString());
  }
}
