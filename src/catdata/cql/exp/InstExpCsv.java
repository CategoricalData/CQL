package catdata.cql.exp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;

import catdata.LocStr;
import catdata.Pair;
import catdata.Util;
import catdata.cql.AqlJs;
import catdata.cql.AqlOptions;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage;
import catdata.cql.SqlTypeSide;
//import catdata.cql.SqlTypeSide;
import catdata.cql.Term;
import gnu.trove.map.hash.THashMap;

//TODO aql reflection uses == instead of equals
public class InstExpCsv extends
		InstExpImport<Map<String, List<String[]>>, Pair<List<Pair<String, String>>, List<Pair<String, String>>>> {

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

	public InstExpCsv(TyExp schema,
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

	
	public static Map<String, List<String[]>> start2(Map<String, Reader> map, AqlOptions op,
			Collage<String, String, Sym, Fk, Att, Void, Void> sch, boolean omitCheck, Map<String, Pair<List<Pair<String, String>>, List<Pair<String, String>>>> map2) throws Exception {
		Character sepChar = (Character) op.getOrDefault(AqlOption.csv_field_delim_char);
		Character quoteChar = (Character) op.getOrDefault(AqlOption.csv_quote_char);
		Character escapeChar = (Character) op.getOrDefault(AqlOption.csv_escape_char);

		final CSVParser parser = new CSVParserBuilder().withSeparator(sepChar).withQuoteChar(quoteChar)
				.withEscapeChar(escapeChar).withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS).build();

		Map<String, List<String[]>> ret = new THashMap<>();
		for (String k : map.keySet()) {

			Reader r = map.get(k);

			final CSVReader reader = new CSVReaderBuilder(r).withCSVParser(parser)
					.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS).build();
			sch.getEns().add(k);

			List<String[]> rows = reader.readAll();

			if (!rows.isEmpty()) {
				String[] hdrs = rows.get(0);
				for (int i = 0; i < hdrs.length; i++) {
					Att att = Att.Att(k, hdrs[i]);
					sch.atts().put(att, new Pair<>(k, "String"));
				}
			}
			map2.put(k, new Pair<>(new LinkedList<>(), new LinkedList<>()));
			ret.put((k), rows);
			reader.close();
			r.close();
		}

		//System.out.println(sch);

		return ret;
	}

	@Override
	protected Map<String, List<String[]>> start(AqlJs<String, Sym> js, Collage<String, String, Sym, Fk, Att, Void, Void> sch)
			throws Exception {
		 if (!js.syms.keySet().containsAll(SqlTypeSide.syms().keySet())) {
		 throw new RuntimeException("CSV import must be onto sql typeside.");
		 }

		Map<String, Reader> m = new THashMap<>();
		// Boolean b = (Boolean) op.getOrDefault(AqlOption.csv_prepend_entity);
		File dir = new File(f);
		if (!dir.isDirectory()) {
			throw new RuntimeException("Expects folder of CSV files.");
		}

		for (File en : dir.listFiles()) {
			String ext = (String) op.getOrDefault(AqlOption.csv_file_extension);
			if (!en.getName().toLowerCase().endsWith(ext)) {
				continue;
			}
			try {
				InputStream is = makeURL(en.getAbsolutePath());
				Reader r = new InputStreamReader(is);
				m.put(en.getName().substring(0, (en.getName().length() - ext.length()) - 1), r);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		}
		Map<String, List<String[]>> ret = start2(m, op, sch, false, this.map);
		tys0 = new THashMap<>(sch.tys().size(), 2);
		data = new THashMap<>(sch.getEns().size(), 2);

		for (Entry<String, List<String[]>> en : ret.entrySet()) {
			data.put(en.getKey(), new THashMap<>(en.getValue().size(), 2));
		}

		for (String ty : sch.tys()) {
			tys0.put(ty, new LinkedList<>());
		}

		return ret;
	}

	private synchronized InputStream makeURL(String x) throws Exception {
		if (f.startsWith("file://") || f.startsWith("http://") || f.startsWith("https://")) {
			return new BOMInputStream(new URL(x).openStream());
		}
//    inputStreamReader = new InputStreamReader(new BOMInputStream(fileInputStream), StandardCharsets.UTF_8);
		return new BOMInputStream(new FileInputStream(new File(x))); // , StandardCharsets.UTF_8);
	}

	@Override
	protected void end(Map<String, List<String[]>> h) throws Exception {
		// clear h?
	}

	@Override
	protected void joinedEn(AqlJs<String, Sym> js, Map<String, List<String[]>> rows, String en0,
			Pair<List<Pair<String, String>>, List<Pair<String, String>>> s,
			Collage<String, String, Sym, Fk, Att, Void, Void> sch) {

		String en = en0; // .replaceAll("[\uFEFF-\uFFFF]", "").trim();
		Map<String, String> inner;
		if (s == null) {
			inner = new THashMap<>();
		} else {
			inner = Util.toMapSafely(s.second);
		}
		//boolean autoGenIds = (Boolean) op.getOrDefault(inner, AqlOption.csv_generate_ids);
		if (rows.get(en0).size() == 0) {
			throw new RuntimeException("No header in CSV file for " + en0);
		}

		// index of each column name
		Map<String, Integer> m = new THashMap<>(sch.getEns().size(), 2);

		for (int i = 0; i < rows.get(en0).get(0).length; i++) {
			// System.out.println(rows.get(en0).get(0)[i]);
			m.put(rows.get(en0).get(0)[i].trim(), i);
		}
		boolean prepend = (boolean) op.getOrDefault(inner, AqlOption.csv_prepend_entity);
		String sep = (String) op.getOrDefault(inner, AqlOption.import_col_seperator);
		String pre = (String) op.getOrDefault(inner, AqlOption.csv_import_prefix);
		boolean skol = (boolean) op.getOrDefault(inner, AqlOption.csv_import_skolem_nulls);

		Map<String, String> map;
		if (s != null) {
			map = Util.toMapSafely(s.first);
		} else {
			map = new THashMap<>();
		}
		int startId = 0;

		String[] cols = rows.get(en0).get(0);
		for (String[] row : rows.get(en0).subList(1, rows.get(en0).size())) {
			String l0;

			l0 = toGen(en0, "" + startId++);

			data.get(en0).put(l0, new Pair<>(new THashMap<>(0, 2), new THashMap<>(row.length, 2)));

			for (String att : cols) {
				String zz = mediate(en, prepend, sep, pre, map, att);
				if (!m.containsKey(zz) && !m.isEmpty()) {
					throw new RuntimeException("No column " + att + " in file for " + en + " nor explicit mapping for "
							+ att + " given. Tried " + zz + " and options are " + Util.alphabetical(m.keySet()));
				}
				int z = m.get(zz);
				if (z >= row.length) {
					throw new RuntimeException("Cannot get index " + z + " from " + Arrays.toString(row));
				}
				String o = row[z];
				//var js = SqlTypeSide.SqlTypeSide(op).js;
				var attt = Att.Att(en, att);
				Term<String, Void, Sym, Void, Void, Void, String> r = skol
						? objectToSk2(sch, o, l0, attt, tys0, nullOnErr, js)
						: objectToSk(sch, o, l0, attt, tys0, nullOnErr, js);
				data.get(en0).get(l0).second.put(Att.Att(en, att), r);
			}
		}

	}

	static int sk = 0;

	private static Term<String, Void, Sym, Void, Void, Void, String> objectToSk2(
			Collage<String, String, Sym, Fk, Att, Void, Void> sch, String rhs, String l0, Att att,
			Map<String, Collection<String>> tys0, boolean errMeansNull, AqlJs<String, Sym> js) {
		String ty = sch.atts().get(att).second;
		if (rhs == null) {
			Term<String, Void, Sym, Void, Void, Void, String> r = Term.Sk("?" + sk);
			tys0.get(ty).add("?" + sk);
			sk++;
			return r;
		} else if (sch.java_tys().containsKey(ty)) {
			try {
				return Term.Obj(js.parse(ty, rhs), ty);
			} catch (Exception ex) {
				if (errMeansNull) {
					return Term.Obj(Optional.empty(), ty);
				}
				ex.printStackTrace();
				throw new RuntimeException("On att " + att.en + "." + att.str + ", error while importing " + rhs
						+ " of class " + rhs.getClass() + ".  Consider option import_null_on_err_unsafe.  Error was "
						+ ex.getMessage());
			}
		} else {
			return Util.anomaly();
		}
	}

	private static Term<String, Void, Sym, Void, Void, Void, String> objectToSk(
			Collage<String, String, Sym, Fk, Att, Void, Void> sch, String rhs, String l0, Att att,
			Map<String, Collection<String>> tys0, boolean errMeansNull, AqlJs js) {
		String ty = sch.atts().get(att).second;
		if (rhs == null) {
			return Term.Obj(Optional.empty(), ty);
		} else if (sch.java_tys().containsKey(ty)) {
			try {
				return Term.Obj(js.parse(ty, rhs), ty);
			} catch (Exception ex) {
				if (errMeansNull) {
					return Term.Obj(Optional.empty(), ty);
				}
				ex.printStackTrace();
				throw new RuntimeException("On att " + att.en + "." + att.str + ", error while importing " + rhs
						+ " of class " + rhs.getClass() + ".  Consider option import_null_on_err_unsafe.  Error was "
						+ ex.getMessage());
			}
		} else {
			return Util.anomaly();
		}
	}

	private static String mediate(String en, boolean prepend, String sep, String pre, Map<String, String> map,
			String x) {
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

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.csv_field_delim_char);
		set.add(AqlOption.csv_escape_char);
		set.add(AqlOption.csv_quote_char);
		set.add(AqlOption.csv_file_extension);
		set.add(AqlOption.csv_generate_ids);
		set.add(AqlOption.emit_ids);
		set.add(AqlOption.import_col_seperator);
		set.add(AqlOption.csv_import_prefix);
		set.add(AqlOption.csv_prepend_entity);
		set.add(AqlOption.prepend_entity_on_ids);
		set.add(AqlOption.import_missing_is_empty);
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

	@Override
	public SchExp type(AqlTyping G) {
		schema.type(G);
		return new SchExpInst<>(this);
	}

}
