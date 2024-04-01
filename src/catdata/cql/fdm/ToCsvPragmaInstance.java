package catdata.cql.fdm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import catdata.Pair;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Pragma;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;

public class ToCsvPragmaInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> extends Pragma {

	private final String fil;

	private final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;

	private final AqlOptions op;

	public static CSVFormat getFormat(AqlOptions op) {
		String format0 = "Default";
		CSVFormat format = CSVFormat.valueOf(format0);

		format = format.withDelimiter((Character) op.getOrDefault(AqlOption.csv_field_delim_char));
		format = format.withQuote((Character) op.getOrDefault(AqlOption.csv_quote_char));
		format = format.withEscape((Character) op.getOrDefault(AqlOption.csv_escape_char));
		format = format.withQuoteMode(QuoteMode.MINIMAL);
		format = format.withNullString(null);

		return format;
	}

	public ToCsvPragmaInstance(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I, String s, AqlOptions op) {
		if (!s.endsWith("/")) {
			s += "/";
		}
		fil = s;
		this.op = op;
		this.I = I;
	}

	public static <Ty, Sym, Y> String print(Term<Ty, Void, Sym, Void, Void, Void, Y> term) {
		if (term.obj() != null) {
			if (term.obj() instanceof Optional) {
				if (((Optional) term.obj()).isPresent()) {
					return ((Optional) term.obj()).get().toString();
				}
				return "";
			}

			return term.toString(x -> x.toString(), y -> y.toString(), false); // optional doesn't get printed, empty
																				// comes out as empty string
		} else if (term.sym() != null && term.args.isEmpty()) {
			return term.toString();
		}
		return null;
	}

	private void delete() {
		File file = new File(fil);
		if (!file.exists()) {
			if (file.mkdirs()) {
				return;
			}
			throw new RuntimeException("Cannot create directory: " + file);

		}
		if (!file.isDirectory()) {
			if (!file.delete()) {
				throw new RuntimeException("Cannot delete file: " + file);
			}
			if (file.mkdirs()) {
				return;
			}
			throw new RuntimeException("Cannot create directory: " + file);

		}
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files == null) {
				throw new RuntimeException("Anomaly: please report");
			}
			for (File f : files) {
				if (f.isDirectory()) {
					throw new RuntimeException("Cannot delete directory: " + f);
				}
				if (!f.delete()) {
					throw new RuntimeException("Cannot delete file: " + f);
				}

			}
		}
	}

	@Override
	public void execute() {
		try {
			Map<En, String> ens = new THashMap<>();

			String idCol = (String) op.getOrDefault(AqlOption.id_column_name);
			int startId = (int) op.getOrDefault(AqlOption.start_ids_at);
			boolean csv_emit_ids = (boolean) op.getOrDefault(AqlOption.csv_emit_ids);
			String order = (String) op.getOrDefault(AqlOption.csv_row_sort_order);
			
			for (En en : I.schema().ens) {
				StringBuffer sb = new StringBuffer();
				CSVPrinter printer = new CSVPrinter(sb, getFormat(op));

				List<String> header = new LinkedList<>();
				if (!csv_emit_ids && idCol.equals("id")) {

				} else {
					header.add(idCol);
				}
				for (Fk fk : Util.alphabetical(I.schema().fksFrom(en))) {
					header.add(fk.toString());
				}
				for (Att att : (I.schema().attsFrom(en))) {
					header.add(att.toString());
				}
				printer.printRecord(header);
				Pair<TObjectIntMap<X>, TIntObjectMap<X>> J = I.algebra().intifyX(startId);
				
				Iterable<X> xs = I.algebra().en(en);
				if (!order.trim().isBlank()) {
					List<X> vs0 = new LinkedList<X>();
					for (var x : xs) {
						vs0.add(x);
					}
					String[] xx = order.split("\\,");
					List<catdata.cql.exp.Att> xxx = new LinkedList<>();
					for (String x : xx) {
						String[] yy = x.split("\\.");
						if (yy[0].trim().equals(en.toString())) {
							xxx.add(catdata.cql.exp.Att.Att(en.toString(), yy[1].toString()));
						}
					}
					
					Comparator<X> comp = new Comparator<X>() {
						@Override
						public int compare(X o1, X o2) {
							for (catdata.cql.exp.Att att : xxx) {
								String u1 = I.algebra().att((Att)att, o1).toString();
								String u2 = I.algebra().att((Att)att, o2).toString();
								int i = u1.compareTo(u2);
								if (i < 0 || i > 0) {
									return i;
								}

							}
							return 0;
						}
					};
					
					vs0.sort(comp );
					xs = vs0;
				}
				
				for (X x : xs) {
					List<String> row = new LinkedList<>();

					if (!csv_emit_ids && idCol.equals("id")) {

					} else {
						row.add(Integer.toString(J.first.get(x)));
					}
					for (Fk fk : Util.alphabetical(I.schema().fksFrom(en))) {
						row.add(Integer.toString(J.first.get(I.algebra().fk(fk, x))));
					}
					for (Att att : (I.schema().attsFrom(en))) {
						row.add(print(I.algebra().att(att, x)));
					}
					printer.printRecord(row);
				}
				ens.put(en, sb.toString());
				printer.close();
			}

			delete();
			String ext = (String) op.getOrDefault(AqlOption.csv_file_extension);
			for (En en : ens.keySet()) {
				if (I.algebra().size(en) > 0) {
					var out = new OutputStreamWriter(new FileOutputStream(fil + en + "." + ext),
							StandardCharsets.UTF_8);
					if ((Boolean)op.getOrDefault(AqlOption.csv_utf8_bom)) {
						out.write('\ufeff');
					}
					out.write(ens.get(en));
					out.close();
				}
			}
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
