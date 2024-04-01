package catdata.cql.fdm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Pragma;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;

public class ToExcelPragmaInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> extends Pragma {

	private final String fil;

	private final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;

	private final AqlOptions op;

	public ToExcelPragmaInstance(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I, String s, AqlOptions op) {
		fil = s;
		this.op = op;
		this.I = I;
	}

	public static <Ty, Sym, Y> void print(Term<Ty, Void, Sym, Void, Void, Void, Y> term, Cell cell) {
		if (term.obj() != null) {
			String x = term.toString(); // optional doesn't get printed, empty comes out as empty string
			try {
				Double d = Double.parseDouble(x);
				cell.setCellValue(d);
				return;
			} catch (Exception r) {
			}
		}
		Set<Y> set = new HashSet<>();
		term.sks(set);
		if (!set.isEmpty()) {
			return;
		} 

		cell.setCellValue(term.toString());
		return;
	}

	Map<En, String> enRange = new HashMap<>();
	Map<En, Integer> enRange0 = new HashMap<>();
	Map<Att, String> attRange = new HashMap<>();
	Map<Fk, String> fkRange = new HashMap<>();

	private String trans(Term<Ty, En, Sym, Fk, Att, Void, Void> t, String v) {
		if (t.isVar()) {
			return v;
		} else if (t.fk() != null) {
			return "XLOOKUP(" + trans(t.arg, v) + ", " +  enRange.get(I.schema().fks.get(t.fk()).first) + "," 
					+ fkRange.get(t.fk()) + ")";
		} else if (t.att() != null) {
			return "XLOOKUP(" + trans(t.arg, v) + ", " + enRange.get(I.schema().atts.get(t.att()).first) + ", "
					+ attRange.get(t.att()) + ")";
		} else if (t.obj() != null) {
			return t.obj().toString();
		} else if (t.sym() != null) {
			
			
			String x = t.sym().toString();
			if (x.equals("+") || x.equals("*") || x.equals("-") || x.equals("/")) {
				return "(" + trans(t.args().get(0), v) + " " + x + " " + trans(t.args().get(1), v) + ")";
			}
			return x + "(" + Util.sep(t.args().stream().map((e) -> trans(e, v)).collect(Collectors.toList()), ",")
					+ ")";
		}
		return "???? " + t + " at " + v;
	}

	@Override
	public void execute() {
		try {
			int startId = (int) op.getOrDefault(AqlOption.start_ids_at);

			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet = workbook.createSheet("OLOG");

			int rowCount = 0;

			Map<En, List<Triple<Pair<String, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>>> m = new HashMap<>();

			for (En en : I.schema().ens) {
				int start = rowCount;
				XSSFRow row = sheet.createRow(rowCount++);

				int columnCount = 0;
				Cell cell = row.createCell(columnCount++);
				cell.setCellValue(en.toString());

				for (Fk fk : Util.alphabetical(I.schema().fksFrom(en))) {
					cell = row.createCell(columnCount++);
					cell.setCellValue(fk.toString());
				}
				for (Att att : Util.alphabetical(I.schema().attsFrom(en))) {
					cell = row.createCell(columnCount++);
					cell.setCellValue(att.toString());
				}

				Pair<TObjectIntMap<X>, TIntObjectMap<X>> J = I.algebra().intifyX(startId);

			
				int j = I.algebra().size(en);
				for (X x : I.algebra().en(en)) {
					row = sheet.createRow(rowCount++);

					columnCount = 0;
					cell = row.createCell(columnCount++);
					cell.setCellValue((Integer) J.first.get(x));

					for (Fk fk : Util.alphabetical(I.schema().fksFrom(en))) {
						cell = row.createCell(columnCount);
						cell.setCellValue((Integer) (J.first.get(I.algebra().fk(fk, x))));
						fkRange.put(fk, "$" + CellReference.convertNumToColString(columnCount) + "$" + (1+1+start) + ":$"
								+ CellReference.convertNumToColString(columnCount) + "$" + (1 + start + j));
						columnCount++;
						
					}
					for (Att att : Util.alphabetical(I.schema().attsFrom(en))) {
						cell = row.createCell(columnCount);
						attRange.put(att, "$" + CellReference.convertNumToColString(columnCount) + "$" + (1+1+start) + ":$"
								+ CellReference.convertNumToColString(columnCount) + "$" + (1 + start + j));
						print(I.algebra().att(att, x), cell);
					
						columnCount++;
					}
				}
				
					

				enRange0.put(en, start);
				enRange.put(en, "$A$" + (1+1+start) + ":$A$" + rowCount);
				sheet.createRow(rowCount++);
				m.put(en, new LinkedList<>());
			}
		//	System.out.println(enRange);	
			//System.out.println(attRange);	
		//	System.out.println(fkRange);	

			for (var eq : I.schema().eqs) {
				m.get(eq.first.second).add(eq);
			}
			for (En en : I.schema().ens) {
				int ccount = I.schema().fksFrom(en).size() + I.schema().attsFrom(en).size() + 1;
				var r = sheet.getRow(enRange0.get(en));
				
				
				for (int i = 0; i <= I.algebra().size(en); i++) {
					r = sheet.getRow(enRange0.get(en) + i);
					int j = 0;
					for (var eq : m.get(en)) {
						var c = r.createCell(ccount + j);
						if (i == 0) {
							c.setCellValue(eq.second + " = " + eq.third);
						} else {
							var lhs = trans(eq.second, "$A" + (1+i+enRange0.get(en)));
							var rhs = trans(eq.third, "$A" + (1+i+enRange0.get(en)));
			//				c.setCellValue(lhs + " = " + rhs);
							c.setCellFormula("(" + lhs + " = " + rhs + ")");
						}
						j++;
					}
				}
			}
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			for (var r : sheet) {
				for (Cell c : r) {
					if (c.getCellType() == CellType.FORMULA) {
			//			evaluator.evaluateFormulaCell(c);
					}
				}
			}
			try (FileOutputStream outputStream = new FileOutputStream(fil)) {
				workbook.write(outputStream);
			}
			workbook.close();

		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return "Export to " + fil + ".";
	}

}
