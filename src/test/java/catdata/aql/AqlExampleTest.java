package catdata.aql;

import catdata.ParseException;
import catdata.Program;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlMultiDriver;
import catdata.aql.exp.AqlParserFactory;
import catdata.aql.exp.Exp;
import catdata.ide.Examples;
import catdata.ide.Language;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class AqlExampleTest {

  public static Stream<Arguments> loadExamples(Language lang) {
    return Examples
            .getExamples(lang).stream()
            .map(example ->
                    new Arguments[]{
                            Arguments.of(
                                    example.getName(),
                                    example.getText(),
                                    AqlParserFactory.Mode.COMBINATOR,
                                    new String[1]),
                            Arguments.of(
                                    example.getName(),
                                    example.getText(),
                                    AqlParserFactory.Mode.ANTLR4,
                                    new String[1])})
            .flatMap(a -> Arrays.stream(a));
  }
  public void runSourceText(String description, String src, AqlParserFactory.Mode mode, String[] args)
          throws ParseException
  {
    System.out.println("testing example: " + description + " parser: " + mode + " args: " + Arrays.toString(args));

      final Program<Exp<?>> prog = AqlParserFactory.getParser(mode).parseProgram(src);
      final AqlMultiDriver driver = new AqlMultiDriver(prog, args, null);
      driver.start();
      final AqlEnv lastEnv = driver.env;
      if (lastEnv.exn != null) {
        throw lastEnv.exn;
      }
    }

    @ParameterizedTest
    @MethodSource("loadEffectiveExamples")
    public void testSourceText(String description, String src, AqlParserFactory.Mode mode, String[] args) {
        System.out.println("testing example: " + description + " parser: " + mode + " args: " + Arrays.toString(args));
        try {
            runSourceText(description,src,mode,args);
        } catch (Exception e) {
            final String msg = new StringBuilder()
                    .append("Test failed for test case '")
                    .append(description)
                    .append("' \n")
                    .append(src)
                    .append("'.\n")
                    .append(e.getStackTrace())
                    .toString();
            fail(msg);
        }
    }

  public static Stream<Arguments> loadEffectiveExamples() {
    return loadExamples(Language.AQL);
  }

  @ParameterizedTest
  @MethodSource("loadDefectiveExamples")
  public void testDefectiveSourceText(String description, String src, AqlParserFactory.Mode mode, String[] args) {
      System.out.println("testing example: " + description);
      try {
        runSourceText(description,src,mode,args);
          final String msg = new StringBuilder()
                  .append("Test succeeded when expected to fail for test case '")
                  .append(description)
                  .append("' \n")
                  .toString();
          fail(msg);
      } catch (Exception e) {
      }
  }

  public static Stream<Arguments> loadDefectiveExamples() {
    return loadExamples(Language.AQL_ALT);
  }


}
