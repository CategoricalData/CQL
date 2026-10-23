package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import catdata.Pair;
import catdata.Util;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.AqlProver.ProverName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class AqlOptionsTest {

  private static Object dflt(AqlOption op) {
    return AqlOptions.initialOptions.getOrDefault(op);
  }

  /** What {@code options op = value} in a CQL expression stores for op. */
  private static Object parse(AqlOption op, String value) {
    return new AqlOptions(Map.of(op.toString(), value), AqlOptions.initialOptions).options.get(op);
  }

  private static String message(Executable e) {
    return assertThrows(RuntimeException.class, e).getMessage();
  }

  /**
   * A value of the same type as d, as a CQL program writes it, and the value it should parse to.
   * Booleans are negated so that a parse that returned the default would be caught.
   */
  private static Pair<String, Object> sample(Object d) {
    if (d instanceof Boolean b) {
      return new Pair<>(Boolean.toString(!b), !b);
    } else if (d instanceof Integer) {
      return new Pair<>("7", 7);
    } else if (d instanceof Long) {
      return new Pair<>("7", 7L);
    } else if (d instanceof Float) {
      return new Pair<>("0.25", 0.25f);
    } else if (d instanceof Character) {
      return new Pair<>("x", 'x');
    } else if (d instanceof ProverName) {
      return new Pair<>("free", ProverName.free);
    } else if (d instanceof String) {
      return new Pair<>("abc", "abc");
    }
    throw new IllegalArgumentException("no sample for " + d);
  }

  @Nested
  class Defaults {

    @Test
    void initialOptionsHoldsNoExplicitOption() {
      assertTrue(AqlOptions.initialOptions.options.isEmpty());
    }

    @Test
    void givesTheDocumentedDefaults() {
      assertEquals(ProverName.auto, dflt(AqlOption.prover));
      assertEquals(ProverName.auto, dflt(AqlOption.second_prover));
      assertEquals(16 * 1024, dflt(AqlOption.eval_use_sql_above));
      assertEquals(30L, dflt(AqlOption.timeout));
      assertEquals(10L, dflt(AqlOption.static_timeout));
      assertEquals(Integer.MAX_VALUE, dflt(AqlOption.talg_reduction));
      assertEquals(Runtime.getRuntime().availableProcessors(), dflt(AqlOption.num_threads));
      assertEquals('"', dflt(AqlOption.csv_quote_char));
      assertEquals(0.5f, dflt(AqlOption.eval_join_selectivity));
      assertEquals(ExternalCodeUtils.LANG_JS, dflt(AqlOption.graal_language));
      assertEquals("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1", dflt(AqlOption.jdbc_default_string));
      assertEquals(3, dflt(AqlOption.toCoQuery_max_term_size));
    }

    @Test
    void completionPrecedenceIsTheOnlyNullDefault() {
      Set<AqlOption> nulls =
          Arrays.stream(AqlOption.values()).filter(op -> dflt(op) == null).collect(Collectors.toSet());
      assertEquals(Set.of(AqlOption.completion_precedence), nulls);
    }

    /** The comment above getDefault says anything unsafe should default to false. */
    @Test
    void twoUnsafeOptionsDefaultToTrue() {
      Set<AqlOption> on =
          Arrays.stream(AqlOption.values())
              .filter(op -> op.toString().contains("unsafe") && Boolean.TRUE.equals(dflt(op)))
              .collect(Collectors.toSet());
      assertEquals(Set.of(AqlOption.allow_sql_import_all_unsafe, AqlOption.allow_java_eqs_unsafe), on);
    }
  }

  @Nested
  class Parsing {

    @ParameterizedTest
    @EnumSource(
        value = AqlOption.class,
        mode = Mode.EXCLUDE,
        names = {"toCoQuery_max_term_size", "completion_precedence"})
    void parsesAValueOfTheTypeOfTheDefault(AqlOption op) {
      Pair<String, Object> s = sample(dflt(op));
      assertEquals(s.second, parse(op, s.first));
    }

    @Test
    void keepsTheCompletionPrecedenceAsWritten() {
      assertEquals("a b c", parse(AqlOption.completion_precedence, "a b c"));
    }

    /** The default and the only reader (QueryExpDeltaCoEval) use an integer. */
    @Test
    void toCoQueryMaxTermSizeRejectsAnInteger() {
      assertEquals(
          "In {toCoQuery_max_term_size=5}, neither true nor false: 5",
          message(() -> parse(AqlOption.toCoQuery_max_term_size, "5")));
    }

    @Test
    void toCoQueryMaxTermSizeParsesABoolean() {
      assertEquals(true, parse(AqlOption.toCoQuery_max_term_size, "true"));
    }

    @Test
    void readsBooleansInAnyCase() {
      assertEquals(true, parse(AqlOption.emit_ids, "TRUE"));
      assertEquals(false, parse(AqlOption.emit_ids, "False"));
    }

    @Test
    void rejectsAWordThatIsNotABoolean() {
      assertEquals(
          "In {emit_ids=yes}, neither true nor false: yes",
          message(() -> parse(AqlOption.emit_ids, "yes")));
    }

    @Test
    void rejectsACharacterOptionOfAnotherLength() {
      assertEquals(
          "Expected a character, instead received ab",
          message(() -> parse(AqlOption.csv_quote_char, "ab")));
      assertEquals(
          "Expected a character, instead received ",
          message(() -> parse(AqlOption.csv_quote_char, "")));
    }

    @Test
    void reportsABadNumberWithoutTheOptionName() {
      assertEquals(
          "For input string: \"abc\"", message(() -> parse(AqlOption.eval_use_sql_above, "abc")));
      assertEquals("For input string: \"1.5\"", message(() -> parse(AqlOption.timeout, "1.5")));
    }

    @Test
    void rejectsAnUnknownProver() {
      assertEquals(
          "No enum constant catdata.cql.AqlProver.ProverName.magic",
          message(() -> parse(AqlOption.prover, "magic")));
    }

    /** The parser accepts only option names, so a CQL program cannot reach this. */
    @Test
    void rejectsAnUnknownOptionName() {
      assertEquals(
          "No enum constant catdata.cql.AqlOptions.AqlOption.no_such_option",
          message(() -> new AqlOptions(Map.of("no_such_option", "1"), AqlOptions.initialOptions)));
    }

    @Test
    void rejectsAMissingValue() {
      Map<String, String> m = new HashMap<>();
      m.put("emit_ids", null);
      assertEquals(
          "No option named emit_ids in options",
          message(() -> new AqlOptions(m, AqlOptions.initialOptions)));
    }

    @Test
    void overridesTheDefaultsAndLeavesThemUnchanged() {
      AqlOptions defaults = new AqlOptions(ProverName.free);
      AqlOptions ops = new AqlOptions(Map.of("timeout", "5"), defaults);
      assertEquals(Map.of(AqlOption.prover, ProverName.free, AqlOption.timeout, 5L), ops.options);
      assertEquals(Map.of(AqlOption.prover, ProverName.free), defaults.options);
    }
  }

  @Nested
  class Getters {

    @Test
    void readEachTypeFromAMapOfStrings() {
      assertEquals(true, AqlOption.emit_ids.getBoolean(Map.of("emit_ids", "true")));
      assertEquals('x', AqlOption.csv_quote_char.getChar(Map.of("csv_quote_char", "x")));
      assertEquals(0.25f, AqlOption.eval_join_selectivity.getFloat(Map.of("eval_join_selectivity", "0.25")));
      assertEquals(7, AqlOption.start_ids_at.getInteger(Map.of("start_ids_at", "7")));
      assertEquals(7L, AqlOption.timeout.getLong(Map.of("timeout", "7")));
      assertEquals(ProverName.e, AqlOption.prover.getDPName(Map.of("prover", "e")));
    }

    @Test
    void requireTheirOwnName() {
      assertEquals(
          "No option named timeout in options",
          message(() -> AqlOption.timeout.getLong(Map.of("static_timeout", "7"))));
    }

    /** getNat has no callers; IdeOptions has its own. */
    @Test
    void getNatAcceptsZeroAndRejectsNegativesAsNotNonZero() {
      assertEquals(0, AqlOption.start_ids_at.getNat(Map.of("start_ids_at", "0")));
      assertEquals(
          "Expected non-zero integer for start_ids_at",
          message(() -> AqlOption.start_ids_at.getNat(Map.of("start_ids_at", "-1"))));
    }
  }

  @Nested
  class Lookup {

    private final AqlOptions ops = new AqlOptions(AqlOptions.initialOptions, AqlOption.timeout, 5L);

    @Test
    void prefersTheExpressionMapThenTheOptionsThenTheDefault() {
      assertEquals(9L, ops.getOrDefault(Map.of("timeout", "9"), AqlOption.timeout));
      assertEquals(5L, ops.getOrDefault(Map.of("static_timeout", "9"), AqlOption.timeout));
      assertEquals(10L, ops.getOrDefault(Map.of(), AqlOption.static_timeout));
    }

    @Test
    void getOrDefaultFallsBackOnlyForAMissingOption() {
      assertEquals(5L, ops.getOrDefault(AqlOption.timeout));
      assertEquals(10L, ops.getOrDefault(AqlOption.static_timeout));
    }

    @Test
    void theTwoLookupsDisagreeOnAStoredNull() {
      AqlOptions nul = new AqlOptions(AqlOptions.initialOptions, AqlOption.timeout, null);
      assertEquals(30L, nul.getOrDefault(AqlOption.timeout));
      assertNull(nul.getOrDefault(Map.of(), AqlOption.timeout));
    }

    @Test
    void getRequiresAnExplicitOption() {
      assertEquals(5L, ops.get(AqlOption.timeout));
      assertEquals(
          "Missing required option static_timeout", message(() -> ops.get(AqlOption.static_timeout)));
    }

    @Test
    void addingAnOptionCopiesTheRest() {
      AqlOptions more = new AqlOptions(ops, AqlOption.emit_ids, false);
      assertEquals(Map.of(AqlOption.timeout, 5L, AqlOption.emit_ids, false), more.options);
      assertEquals(Map.of(AqlOption.timeout, 5L), ops.options);
    }

    @Test
    void aProverNameIsTheOnlyOption() {
      assertEquals(Map.of(AqlOption.prover, ProverName.e), new AqlOptions(ProverName.e).options);
    }
  }

  @Nested
  class Names {

    @Test
    void optionNamesListsEveryOptionInAlphanumericOrder() {
      List<String> names = new ArrayList<>(AqlOptions.optionNames());
      assertEquals(
          Arrays.stream(AqlOption.values()).map(AqlOption::toString).collect(Collectors.toSet()),
          new HashSet<>(names));
      assertEquals(AqlOption.values().length, names.size());
      for (int i = 1; i < names.size(); i++) {
        assertTrue(Util.AlphabeticalComparator.compare(names.get(i - 1), names.get(i)) < 0);
      }
      assertEquals("active_domain", names.get(0));
      assertEquals("varchar_length", names.get(names.size() - 1));
    }

    /** The local list shadows the static field, which stays null. */
    @Test
    void optionNamesIsRebuiltOnEveryCall() {
      assertNotSame(AqlOptions.optionNames(), AqlOptions.optionNames());
    }

    @Test
    void proverOptionNamesIsBuiltOnce() {
      assertSame(AqlOptions.proverOptionNames(), AqlOptions.proverOptionNames());
    }

    @Test
    void proverOptionNamesListsTheEgglogOptionsTwice() {
      List<AqlOption> names = new ArrayList<>(AqlOptions.proverOptionNames());
      assertEquals(23, names.size());
      assertEquals(21, new HashSet<>(names).size());
      assertEquals(2, names.stream().filter(op -> op == AqlOption.egglog_bound).count());
      assertEquals(2, names.stream().filter(op -> op == AqlOption.egglog_path).count());
      assertTrue(names.contains(AqlOption.prover));
      assertFalse(names.contains(AqlOption.timeout));
    }
  }

  @Nested
  class Equality {

    @Test
    void comparesTheOptions() {
      AqlOptions a = new AqlOptions(ProverName.free);
      AqlOptions b = new AqlOptions(ProverName.free);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
      assertEquals(a, a);
      assertNotEquals(a, new AqlOptions(ProverName.e));
      assertFalse(a.equals(null));
      assertFalse(a.equals("prover = free"));
    }

    @Test
    void printsOneOptionPerLine() {
      assertEquals("prover = free", new AqlOptions(ProverName.free).toString());
    }
  }

  @Nested
  class Help {

    private final String msg = AqlOptions.getMsg();

    @Test
    void listsEveryDefault() {
      assertTrue(
          msg.startsWith(
              "Options are specified in each CQL expression.\n"
                  + "Here are the available options and their defaults:\n\n\t"),
          msg);
      for (AqlOption op : AqlOption.values()) {
        Object d = dflt(op);
        String line = d == null ? op + " has a null default " : op + " = " + d;
        assertTrue(msg.contains("\n\t" + line + "\n"), line);
      }
    }

    @Test
    void endsWithTheProversTheLanguagesAndAPointerToTheManual() {
      assertTrue(msg.contains("completion_precedence = \"a b c\" means a < b < c"));
      assertTrue(msg.contains("Available provers: " + Arrays.toString(ProverName.values())));
      assertTrue(msg.contains("Available Graal languages: ["));
      assertTrue(
          msg.endsWith(
              "Option descriptions are available in the CQL manual, see http://categoricaldata.net"));
    }
  }
}
