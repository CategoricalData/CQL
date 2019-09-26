package catdata.aql.exp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;

import catdata.LocStr;
import catdata.Null;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Schema;
import catdata.aql.Term;
import gnu.trove.map.hash.THashMap;

//TODO aql reflection uses == instead of equals
public class InstExpCsv
		extends InstExpImport<Map<En, List<String[]>>, Pair<List<Pair<String, String>>, List<Pair<String, String>>>> {

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(f).append(map).append(options).append(schema).toHashCode();
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		InstExpCsv rhs = (InstExpCsv) obj;
		return new EqualsBuilder().append(f, rhs.f).append(map, rhs.map).append(options, rhs.options)
				.append(schema, rhs.schema).isEquals();
	}

	public String f;

	private static List<Pair<LocStr, Pair<List<Pair<String, String>>, List<Pair<String, String>>>>> conv(
			List<Pair<LocStr, Pair<List<Pair<LocStr, String>>, List<Pair<String, String>>>>> l) {
		List<Pair<LocStr, Pair<List<Pair<String, String>>, List<Pair<String, String>>>>> ret = new LinkedList<>();
		for (Pair<LocStr, Pair<List<Pair<LocStr, String>>, List<Pair<String, String>>>> x : l) {
			ret.add(new Pair<>(x.first, new Pair<>(conv2(x.second.first), x.second.second)));
		}

		return ret;
	}

	private static List<Pair<String, String>> conv2(List<Pair<LocStr, String>> l) {
		return l.stream().map(x -> new Pair<>(x.first.str, x.second)).collect(Collectors.toList());
	}

	public InstExpCsv(SchExp schema,
			List<Pair<LocStr, Pair<List<Pair<LocStr, String>>, List<Pair<String, String>>>>> map,
			List<Pair<String, String>> options, String f) {
		super(schema, conv(map), options);
		this.f = f;

	}

	@Override
	protected String getHelpStr() {
		return "";
	}

	@Override
	public String toString() {
		if (map.isEmpty()) {
			return new StringBuilder().append("import_csv \"" + f + "\" : " + schema).toString();
		}
		Function<Pair<List<Pair<String, String>>, List<Pair<String, String>>>, String> fun = p -> {
			String z = p.second.isEmpty() ? "" : " options " + Util.sep(Util.toMapSafely(p.second), " = ", "\n\t");
			return "{" + Util.sep(Util.toMapSafely(p.first), " -> ", "\n\t") + z + " }";
		};
		return new StringBuilder().append("import_csv \"" + f + "\" : " + schema + " {\n\t")
				.append(Util.sep(map, " -> ", "\n\t", fun) + "\n}").toString();
	}

	/**
	 * Expects filenames in the map
	 */
	public static Map<En, List<String[]>> start2(Map<String, Reader> map, AqlOptions op,
			Schema<Ty, En, Sym, Fk, Att> sch, boolean omitCheck) throws Exception {
		Character sepChar = (Character) op.getOrDefault(AqlOption.csv_field_delim_char);
		Character quoteChar = (Character) op.getOrDefault(AqlOption.csv_quote_char);
		Character escapeChar = (Character) op.getOrDefault(AqlOption.csv_escape_char);

		final CSVParser parser = new CSVParserBuilder().withSeparator(sepChar).withQuoteChar(quoteChar)
				.withEscapeChar(escapeChar).withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS).build();

		Map<En, List<String[]>> ret = new THashMap<>();
		for (String k : map.keySet()) {
			if (!omitCheck) {
				if (!sch.ens.contains(En.En(k))) {
					throw new RuntimeException("Not an entity: " + k);
				}
			}
			Reader r = map.get(k);

			// File file = new File(map.get(k));
			// BufferedReader fileReader = new BufferedReader(r);
			// String s;
			/// while ((s = fileReader.readLine()) != null) {
			// System.out.println(s);
			// }

			final CSVReader reader = new CSVReaderBuilder(r).withCSVParser(parser)
					.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS).build();

			List<String[]> rows = reader.readAll();
			
			ret.put(En.En(k), rows);
			reader.close();
			r.close();
		}

		if (!omitCheck) {
			for (En en : sch.ens) {
				if (!ret.containsKey(en)) {
					ret.put(en,
							new LinkedList<>(Collections.singletonList(Util
									.union(sch.attsFrom(en).stream().map(Object::toString).collect(Collectors.toList()),
											sch.fksFrom(en).stream().map(Object::toString).collect(Collectors.toList()))
									.toArray(new String[0]))));
				}
			}
		}
		return ret;
	}

	@Override
	protected Map<En, List<String[]>> start(Schema<Ty, En, Sym, Fk, Att> sch) throws Exception {
		Map<String, Reader> m = new THashMap<>();
		Boolean b = (Boolean) op.getOrDefault(AqlOption.csv_prepend_entity);
		for (En en : sch.ens) {
			String x = f + op.getOrDefault(AqlOption.csv_import_prefix) + "/" + en.toString() + "."
					+ op.getOrDefault(AqlOption.csv_file_extension);
			InputStream is = makeURL(x);
			try {
				Reader r = new InputStreamReader(is);
				m.put(en.str, r);
			} catch (Exception ex) {
				ex.printStackTrace();
				if (!(boolean) op.getOrDefault(AqlOption.import_missing_is_empty)) {
					throw new RuntimeException(
							"Missing: " + x + ". \n\nPossible options to consider: " + AqlOption.import_missing_is_empty
									+ " and " + AqlOption.csv_import_prefix + " and " + AqlOption.csv_file_extension);
				}
			}
		}
		return start2(m, op, sch, false);
	}

	private synchronized InputStream makeURL(String x) throws Exception {
		if (f.startsWith("file://") || f.startsWith("http://") || f.startsWith("https://")) {
			return new URL(x).openStream();
		}
		return new FileInputStream(new File(x));
	}

	@Override
	protected void end(Map<En, List<String[]>> h) throws Exception {
		// clear h?
	}

	@Override
	protected void joinedEn(Map<En, List<String[]>> rows, En en0,
			Pair<List<Pair<String, String>>, List<Pair<String, String>>> s, Schema<Ty, En, Sym, Fk, Att> sch)
			throws Exception {
		String en = en0.convert().replaceAll("[\uFEFF-\uFFFF]", "").trim();
		Map<String, String> inner;
		if (s == null) {
			inner = new THashMap<>();
		} else {
			inner = Util.toMapSafely(s.second);
		}
		boolean autoGenIds = (Boolean) op.getOrDefault(inner, AqlOption.csv_generate_ids);
		// for (En en2 : rows.keySet()) {
		if (rows.get(en0).size() == 0) {
			throw new RuntimeException("No header in CSV file for " + en0);
		}
		// }

		// index of each column name
		Map<String, Integer> m = new THashMap<>(rows.size());
		for (int i = 0; i < rows.get(en0).get(0).length; i++) {
			m.put(rows.get(en0).get(0)[i].replaceAll("[\uFEFF-\uFFFF]", "").trim(), i);
		}
		boolean prepend = (boolean) op.getOrDefault(inner, AqlOption.csv_prepend_entity);
		String sep = (String) op.getOrDefault(inner, AqlOption.import_col_seperator);
		String pre = (String) op.getOrDefault(inner, AqlOption.csv_import_prefix);

		Map<String, String> map;
		if (s != null) {
			map = Util.toMapSafely(s.first);
		} else {
			map = new THashMap<>();
		}

//		Function<String, String> mediate = mediate(en, prepend, sep, pre, map);
		int startId = 0;

		for (String[] row : rows.get(en0).subList(1, rows.get(en0).size())) {
			Gen l0;

			String idCol = map.containsKey(en) ? map.get(en)
					: (String) op.getOrDefault(inner, AqlOption.id_column_name);

			if (autoGenIds && !m.containsKey(idCol)) {
				l0 = toGen(en0, "" + startId++);
			} else if (!autoGenIds && !m.containsKey(idCol)) {
				throw new RuntimeException("On " + en + ", ID column " + idCol + " not found in headers " + m.keySet()
						+ ". \n\nPossible solution: provide a mapping.\n\nPossible solution: set csv_generate_ids=true to auto-generate IDs.\n\nPossible solution: rename the headers in the CSV file.\n\nPossible solution: add an ID column to the CSV file.");
			} else {
				l0 = toGen(en0, row[m.get(idCol)]);
			}

			ens0.get(en0).add(l0);

			for (Fk fk : sch.fksFrom(en0)) {
				if (!fks0.containsKey(l0)) {
					fks0.put(l0, new THashMap<>());
				}
				String zz = row[m.get(mediate(en, prepend, sep, pre, map, fk.convert()))];
				if (zz == null && !import_as_theory) {
					throw new RuntimeException("FK has null value, " + fk + " on " + Arrays.toString(row));
				} else if (zz != null) {
					Gen g = toGen(sch.fks.get(fk).second, zz);
					ens0.get(sch.fks.get(fk).second).add(g);
					fks0.get(l0).put(fk, g);
				}
			}

			for (Att att : sch.attsFrom(en0)) {
				if (!atts0.containsKey(l0)) {
					atts0.put(l0, new THashMap<>());
				}
				String zz = mediate(en, prepend, sep, pre, map, att.convert());
				if (!m.containsKey(zz)) {
					throw new RuntimeException("No column " + att + " in file for " + en + " nor explicit mapping for "
							+ att + " given. Tried " + zz + " and options are " + Util.alphabetical(m.keySet()));
				}
				int z = m.get(zz);
				if (z >= row.length) {
					throw new RuntimeException("Cannot get index " + z + " from " + Arrays.toString(row));
				}
				String o = row[z];
				Term<Ty, Void, Sym, Void, Void, Void, Null<?>> r = objectToSk(sch, o, l0, att, tys0, extraRepr, true,
						nullOnErr);
				atts0.get(l0).put(att, r);
			}
		}

	}

	private String mediate(String en, boolean prepend, String sep, String pre,
			Map<String, String> map, String x) {
			if (map.containsKey(x)) {
				return map.get(x);
			}
			String z = x;
			if (prepend) {
				int i = x.indexOf(en + sep);
				if (i != 0) {
					map.put(x, pre + z);
					return pre + z;
				}
				String temp = x.substring((en + sep).length());
				map.put(x, pre + temp);
				return pre + temp;
			}
			map.put(x, pre + z);
			return pre + z;
	}

	// TODO aql shredded input format for CSV
	@Override
	protected void shreddedAtt(Map<En, List<String[]>> h, Att att,
			Pair<List<Pair<String, String>>, List<Pair<String, String>>> s, Schema<Ty, En, Sym, Fk, Att> sch)
			throws Exception {
		throw new RuntimeException(
				"Shredded input format not avaiable for CSV (if desired, please email info@catinf.com)");
	}

	@Override
	protected void shreddedFk(Map<En, List<String[]>> h, Fk fk,
			Pair<List<Pair<String, String>>, List<Pair<String, String>>> s, Schema<Ty, En, Sym, Fk, Att> sch)
			throws Exception {
		throw new RuntimeException(
				"Shredded input format not avaiable for CSV (if desired, please email info@catinf.com)");
	}

	@Override
	protected void shreddedEn(Map<En, List<String[]>> h, En en,
			Pair<List<Pair<String, String>>, List<Pair<String, String>>> s, Schema<Ty, En, Sym, Fk, Att> sch)
			throws Exception {
		throw new RuntimeException(
				"Shredded input format not avaiable for CSV (if desired, please email info@catinf.com)");
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.csv_field_delim_char);
		set.add(AqlOption.csv_escape_char);
		set.add(AqlOption.csv_quote_char);
		set.add(AqlOption.csv_file_extension);
		set.add(AqlOption.csv_generate_ids);
		set.add(AqlOption.csv_emit_ids);
		set.add(AqlOption.import_col_seperator);
		set.add(AqlOption.csv_import_prefix);
		set.add(AqlOption.csv_prepend_entity);
		set.add(AqlOption.prepend_entity_on_ids);
		set.add(AqlOption.import_missing_is_empty);
		set.add(AqlOption.import_as_theory);
		set.add(AqlOption.id_column_name);
		set.add(AqlOption.import_null_on_err_unsafe);
		set.add(AqlOption.prepend_entity_on_ids);
		set.add(AqlOption.csv_import_prefix);
		set.add(AqlOption.import_dont_check_closure_unsafe);

	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		this.schema.map(f);
	}

}
