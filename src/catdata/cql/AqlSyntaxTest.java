package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import catdata.ParseException;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Exp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The names of the expression classes. Exp.getSyntax looks a class up here by its simple name,
 * and the help generator writes a syntax page for each constant.
 */
class AqlSyntaxTest {

  /** Expression classes without a constant: sixteen that the parser builds, then the rest. */
  private static final Set<String> WITHOUT_A_CONSTANT =
      Set.of(
          "EdsExpFromMySql",
          "EdsExpFromOracle",
          "EdsExpSql",
          "EdsExpSqlNull",
          "InstExpMsError",
          "InstExpSkolem",
          "PragmaExpToExcelInst",
          "QueryExpDeltaMigrate",
          "QueryExpSelectStar",
          "SchExpCsv",
          "SchExpMsError",
          "SchExpMsErrorShallow",
          "TransExpChaseReturn",
          "TransExpSkolem",
          "TyExpExcel",
          "TyExpSqlNull",
          "ApgMapExpCompose",
          "Block",
          "EdExpRaw",
          "EdsExpLit",
          "GraphExpLiteral",
          "InstExpLit",
          "MapExpLit",
          "MorExpRaw",
          "MorExpVar",
          "QueryExpLit",
          "SchExpLit",
          "SchExpPseudo",
          "Trans",
          "TransExpLit",
          "TyExpAdt",
          "TyExpLit");

  private static Exp<?> parse(String declaration) throws ParseException {
    return AqlParserFactory.getParser().parseProgram(declaration).exps.values().iterator().next();
  }

  /** The simple names of the concrete, named expression classes compiled next to Exp. */
  private static Set<String> expressionClasses() throws IOException, URISyntaxException {
    Path root = Path.of(Exp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    assumeTrue(Files.isDirectory(root), "classes are in a jar");
    Set<String> names = new TreeSet<>();
    for (String pkg : new String[] {"catdata/cql/exp", "catdata/apg/exp"}) {
      try (Stream<Path> files = Files.list(root.resolve(pkg))) {
        for (Path f : files.toList()) {
          String file = f.getFileName().toString();
          if (!file.endsWith(".class")) {
            continue;
          }
          String binary = pkg.replace('/', '.') + "." + file.substring(0, file.length() - 6);
          Class<?> c;
          try {
            c = Class.forName(binary, false, AqlSyntaxTest.class.getClassLoader());
          } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
          }
          if (Exp.class.isAssignableFrom(c)
              && !Modifier.isAbstract(c.getModifiers())
              && !c.isAnonymousClass()
              && !c.isLocalClass()) {
            names.add(c.getSimpleName());
          }
        }
      }
    }
    return names;
  }

  private static Set<String> constants() {
    Set<String> names = new TreeSet<>();
    for (AqlSyntax s : AqlSyntax.values()) {
      names.add(s.name());
    }
    return names;
  }

  @Test
  void namesTheClassOfAParsedExpression() throws ParseException {
    assertEquals(AqlSyntax.InstExpSigma, parse("instance I = sigma F J").getSyntax());
    assertEquals(AqlSyntax.TyExpSql, parse("typeside T = sql").getSyntax());
  }

  /** InstExpMarkdown has a constant and a visitor method, but no class. */
  @Test
  void namesAnExpressionClassWithEveryConstantButOne() throws IOException, URISyntaxException {
    Set<String> unnamed = new HashSet<>(constants());
    unnamed.removeAll(expressionClasses());
    assertEquals(Set.of("InstExpMarkdown"), unnamed);
  }

  /** Issue 70: the first sixteen have a keyword and no syntax page in the generated help. */
  @Test
  void leavesOutThirtyTwoExpressionClasses() throws IOException, URISyntaxException {
    Set<String> missing = new HashSet<>(expressionClasses());
    missing.removeAll(constants());
    assertEquals(WITHOUT_A_CONSTANT, missing);
  }

  /** Issue 70. */
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "typeside T = excel | TyExpExcel",
        "typeside T = sqlNull sql | TyExpSqlNull",
        "schema S = ms_error a b : sql | SchExpMsError",
        "schema S = ms_error_shallow a : sql | SchExpMsErrorShallow",
        "schema S = import_csv \"x\" : sql | SchExpCsv",
        "instance I = skolem J | InstExpSkolem",
        "instance I = ms_error a sql b | InstExpMsError",
        "transform t = skolem J | TransExpSkolem",
        "transform t = chase_return C J | TransExpChaseReturn",
        "query Q = delta F P | QueryExpDeltaMigrate",
        "query Q = select_star P | QueryExpSelectStar",
        "constraints C = sql S | EdsExpSql",
        "constraints C = sqlNull sql | EdsExpSqlNull",
        "constraints C = from_oracle a : S | EdsExpFromOracle",
        "constraints C = from_mysql a : S | EdsExpFromMySql",
        "command c = export_excel_instance J \"f\" | PragmaExpToExcelInst"
      })
  void hasNoConstantForSomeKeywords(String declaration, String cls) throws ParseException {
    Exp<?> e = parse(declaration);
    assertEquals(cls, e.getClass().getSimpleName());
    String m = assertThrows(IllegalArgumentException.class, e::getSyntax).getMessage();
    assertEquals("No enum constant catdata.cql.AqlSyntax." + cls, m);
    assertThrows(IllegalArgumentException.class, e::getKeyword);
  }
}
