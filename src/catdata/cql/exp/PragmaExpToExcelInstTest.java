package catdata.cql.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.TypeSide;
import catdata.cql.exp.InstExp.InstExpVar;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link PragmaExpToExcelInst}, {@code export_excel_instance I "file"}, which writes
 * an instance to one sheet: a block of rows per entity, with an ID column, a column per foreign key
 * and attribute, and a formula column per observation equation that checks it on each row. Also
 * covers {@link TyExpExcel}, the {@code excel} typeside the export is meant for.
 */
class PragmaExpToExcelInstTest {

  @TempDir Path dir;

  /** c = a + b, a = MIN(a, b) and x.f.name = x.tag hold on both rows of A. */
  private static final String EXCEL =
      """
      options prover = completion
      typeside Ty = excel
      schema S = literal : Ty {
        entities A B
        foreign_keys f : A -> B
        attributes a b c : A -> Number tag : A -> String name : B -> String
        observation_equations
          forall x. "+"(x.a, x.b) = x.c
          forall x. MIN(x.a, x.b) = x.a
          forall x. x.f.name = x.tag
      }
      instance I = literal : S {
        generators p q : A r : B
        equations
          p.a = 1 p.b = 2 p.c = 3 p.f = r p.tag = Bob
          q.a = 4 q.b = 5 q.c = 9 q.f = r q.tag = Bob
          r.name = Bob
      }
      """;

  /** The sheet that the program writes to out.xlsx in dir. */
  private Sheet export(String program) throws Exception {
    File out = dir.resolve("out.xlsx").toFile();
    String command = "command E = export_excel_instance I \"" + out + "\"\n";
    AqlMultiDriver d = ExpTestHelpers.run(program + command);
    assertTrue(d.exn.isEmpty(), d.exn.toString());
    assertTrue(d.env.exn == null, String.valueOf(d.env.exn));
    try (XSSFWorkbook book = new XSSFWorkbook(out)) {
      return book.getSheet("OLOG");
    }
  }

  private static String text(Cell c) {
    return c.getCellType() == CellType.FORMULA ? "=" + c.getCellFormula() : c.toString();
  }

  /** The cells of the row, as text, formulas prefixed with =. */
  private static List<String> cells(Row row) {
    List<String> ret = new ArrayList<>();
    for (Cell c : row) {
      ret.add(text(c));
    }
    return ret;
  }

  @Nested
  class Evaluation {

    @Test
    void writesABlockOfRowsPerEntity() throws Exception {
      Sheet s = export(EXCEL);
      assertEquals("OLOG", s.getSheetName());
      assertEquals(List.of("A", "f", "a", "b", "c", "tag"), cells(s.getRow(0)).subList(0, 6));
      assertEquals(
          Set.of("x.f.name = x.tag", "(x.a \"+\" x.b) = x.c", "(x.a MIN x.b) = x.a"),
          new HashSet<>(cells(s.getRow(0)).subList(6, 9)));
      assertEquals(List.of("B", "name"), cells(s.getRow(4)));
      assertEquals(0, s.getRow(6).getPhysicalNumberOfCells());
    }

    @Test
    void numbersTheRowsAndPointsEachForeignKeyAtItsRow() throws Exception {
      Sheet s = export(EXCEL);
      assertEquals(Set.of(0.0, 1.0), Set.of(id(s, 1), id(s, 2)));
      assertEquals(2.0, id(s, 5));
      assertEquals(2.0, s.getRow(1).getCell(1).getNumericCellValue());
      assertEquals(2.0, s.getRow(2).getCell(1).getNumericCellValue());
    }

    private double id(Sheet s, int row) {
      return s.getRow(row).getCell(0).getNumericCellValue();
    }

    /**
     * Values of the excel typeside are written as CQL prints them, quoted and typed, so they are
     * text cells, and the formulas add text.
     */
    @Test
    void writesTheValuesOfTheExcelTypesideAsTypedText() throws Exception {
      Sheet s = export(EXCEL);
      Set<List<String>> rows =
          Set.of(cells(s.getRow(1)).subList(2, 6), cells(s.getRow(2)).subList(2, 6));
      assertEquals(
          Set.of(
              List.of("\"1.0\"@Number", "\"2.0\"@Number", "\"3.0\"@Number", "Bob@String"),
              List.of("\"4.0\"@Number", "\"5.0\"@Number", "\"9.0\"@Number", "Bob@String")),
          rows);
      assertEquals(CellType.STRING, s.getRow(1).getCell(2).getCellType());
      assertEquals("Bob@String", s.getRow(5).getCell(1).getStringCellValue());
    }

    @Test
    void checksEachEquationWithAFormula() throws Exception {
      Sheet s = export(EXCEL);
      String a = "XLOOKUP($A2, $A$2:$A$3, $C$2:$C$3)";
      String b = "XLOOKUP($A2, $A$2:$A$3, $D$2:$D$3)";
      String c = "XLOOKUP($A2, $A$2:$A$3, $E$2:$E$3)";
      String tag = "XLOOKUP($A2, $A$2:$A$3, $F$2:$F$3)";
      String name = "XLOOKUP(XLOOKUP($A2, $A$2:$A$3,$B$2:$B$3), $A$6:$A$6, $B$6:$B$6)";
      assertEquals(
          Set.of(
              "=((" + a + " + " + b + ") = " + c + ")",
              "=(MIN(" + a + "," + b + ") = " + a + ")",
              "=(" + name + " = " + tag + ")"),
          new HashSet<>(cells(s.getRow(1)).subList(6, 9)));
    }

    /**
     * On the SQL typeside, values are optional, print bare, and numbers become numeric cells. An
     * unknown value leaves its cell blank.
     */
    @Test
    void writesSqlValuesAsTheyAre() throws Exception {
      String sql =
          """
          typeside Ty = sql
          schema S = literal : Ty { entities A attributes n : A -> Integer s : A -> Varchar }
          instance I = literal : S { generators p : A equations p.n = 7 p.s = Bob }
          """;
      Sheet s = export(sql);
      assertEquals(List.of("A", "n", "s"), cells(s.getRow(0)));
      assertEquals(7.0, s.getRow(1).getCell(1).getNumericCellValue());
      assertEquals("Bob", s.getRow(1).getCell(2).getStringCellValue());
      Sheet unknown = export(sql.replace(" p.s = Bob", ""));
      assertEquals(CellType.BLANK, unknown.getRow(1).getCell(2).getCellType());
    }

    @Test
    void startsTheIdsAtTheOption() throws Exception {
      Sheet s = export("options start_ids_at = 10\n" + EXCEL.replace("options prover", "  prover"));
      assertEquals(12.0, s.getRow(5).getCell(0).getNumericCellValue());
    }

    @Test
    void reportsAFileThatCannotBeWritten() throws Exception {
      String command =
          "command E = export_excel_instance I \"" + dir.resolve("no/out.xlsx") + "\"\n";
      String msg = ExpTestHelpers.error(EXCEL + command);
      assertTrue(msg.contains("java.io.FileNotFoundException"), msg);
    }

    @Test
    void isNotEvaluatedAtCompileTime() throws Exception {
      String program =
          EXCEL + "command E = export_excel_instance I \"" + dir.resolve("o.xlsx") + "\"\n";
      AqlEnv env = ExpTestHelpers.run(program).env;
      PragmaExpToExcelInst<?, ?> e = ExpTestHelpers.exp(program, "E");
      assertThrows(IgnoreException.class, () -> e.eval0(env, true));
    }
  }

  @Nested
  class Expression {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PragmaExpToExcelInst<?, ?> exp(String i, String file, List<Pair<String, String>> ops) {
      return new PragmaExpToExcelInst(new InstExpVar(i), file, ops);
    }

    private final PragmaExpToExcelInst<?, ?> exp = exp("I", "f", List.of());

    /** The options are printed with the key and value separators swapped. */
    @Test
    void printsTheInstanceTheFileAndTheOptions() {
      assertEquals("export_excel_instance I \"f\"", exp.toString());
      assertEquals(
          "export_excel_instance I \"f\" {\n\toptionstimeout\n\t\t5}",
          exp("I", "f", List.of(new Pair<>("timeout", "5"))).toString());
    }

    @Test
    void dependsOnTheInstance() throws Exception {
      assertEquals(List.of(new Pair<>("I", Kind.INSTANCE)), new ArrayList<>(exp.deps()));
      assertEquals(Unit.unit, exp.type(ExpTestHelpers.typing(EXCEL)));
      assertTrue(exp.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, exp.allowedOptions());
      List<Exp<?>> seen = new ArrayList<>();
      exp.map(seen::add);
      assertEquals(List.of(exp, new InstExpVar("I")), seen);
    }

    @Test
    void equalityComparesEveryPart() {
      assertEquals(exp, exp);
      assertEquals(exp, exp("I", "f", List.of()));
      assertEquals(exp.hashCode(), exp("I", "f", List.of()).hashCode());
      assertNotEquals(exp, exp("J", "f", List.of()));
      assertNotEquals(exp, exp("I", "g", List.of()));
      assertNotEquals(exp, exp("I", "f", List.of(new Pair<>("timeout", "5"))));
      assertNotEquals(exp, null);
      assertNotEquals(exp, "export_excel_instance");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsAVisitor() {
      PragmaExpVisitor<String, Unit, RuntimeException> v = mock(PragmaExpVisitor.class);
      when(v.visit(Unit.unit, exp)).thenReturn("visited");
      assertEquals("visited", exp.accept(Unit.unit, v));
    }
  }

  @Nested
  class ExcelTypeside {

    private final TyExpExcel excel = new TyExpExcel();

    @Test
    void declaresNumbersStringsAndBooleansWithArithmetic() throws Exception {
      TypeSide<String, Sym> t = ExpTestHelpers.eval("typeside T = excel", "T", Kind.TYPESIDE);
      assertEquals(Set.of("Number", "String", "Boolean"), t.tys);
      assertEquals(
          Set.of("+", "-", "/", "*", "MIN", "MAX"),
          t.syms.keySet().stream().map(Sym::toString).collect(Collectors.toSet()));
    }

    /** The static cache is never filled, so each evaluation builds a new typeside. */
    @Test
    void buildsANewTypesideEachTime() throws Exception {
      AqlEnv env = ExpTestHelpers.run("").env;
      assertNotSame(excel.eval0(env, false), excel.eval0(env, false));
    }

    @Test
    void isAConstantExpression() {
      assertEquals("excel", excel.toString());
      assertEquals(new TyExpExcel(), excel);
      assertEquals(117, excel.hashCode());
      assertNotEquals(excel, new TyExpRdf());
      assertTrue(excel.deps().isEmpty());
      assertTrue(excel.options().isEmpty());
      assertEquals(ExpTestHelpers.BASE_OPTIONS, excel.allowedOptions());
      assertEquals(Unit.unit, excel.type(new AqlTyping()));
      List<Exp<?>> seen = new ArrayList<>();
      excel.map(seen::add);
      assertEquals(List.of(excel), seen);
    }

    /** Neither visitor method is implemented: both return null. */
    @Test
    @SuppressWarnings("unchecked")
    void ignoresItsVisitors() {
      assertNull(excel.accept(Unit.unit, mock(TyExp.TyExpVisitor.class)));
      assertNull(excel.coaccept(Unit.unit, new TypingInversion(), new AqlTyping()));
      assertEquals(Map.of(), excel.options());
    }
  }
}
