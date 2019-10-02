package catdata.aql.exp;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;

import catdata.Pair;
import catdata.Program;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.SqlTypeSide;

//return onto SQL typeside
public class SchExpCsv extends SchExp {
	
	public final String str;
	public final Map<String, String> options;	

	public SchExpCsv(String str, List<Pair<String, String>> options) {
		this.str = str;
		this.options = Util.toMapSafely(options);
	}

	@Override
	public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) {
		return v.visitSchExpCsv(params, r);
	}

	public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) {
		return v.visit(param, this);
	}

	@Override
	public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
		return this;
	}

	@Override
	public TyExp type(AqlTyping G) {
		return new TyExpSql();
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.csv_field_delim_char);
		set.add(AqlOption.csv_escape_char);
		set.add(AqlOption.csv_quote_char);
//		set.add(AqlOption.id_column_name);
		set.add(AqlOption.csv_import_prefix);
		set.add(AqlOption.csv_file_extension);

	}

	@Override
	protected Map<String, String> options() {
		return options;
	}

	@Override
	protected Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isCompileTime) {
		AqlOptions op = new AqlOptions(options, null, env.defaults);
		SqlTypeSide ts = SqlTypeSide.SqlTypeSide(op);
		
		Character sepChar = (Character) op.getOrDefault(AqlOption.csv_field_delim_char);
		Character quoteChar = (Character) op.getOrDefault(AqlOption.csv_quote_char);
		Character escapeChar = (Character) op.getOrDefault(AqlOption.csv_escape_char);
		String colSep = (String) op.getOrDefault(AqlOption.import_col_seperator);
		String pre = (String) op.getOrDefault(AqlOption.csv_import_prefix);
		String ext = (String) op.getOrDefault(AqlOption.csv_file_extension);

		boolean prepend = (boolean)	op.getOrDefault(AqlOption.csv_prepend_entity);

		final CSVParser parser = new CSVParserBuilder().withSeparator(sepChar).withQuoteChar(quoteChar)
				.withEscapeChar(escapeChar).withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS).build();
	
		File f = new File(str);
		if (!f.exists()) {
			throw new RuntimeException("Does not exist: " + str);			
		} else if (!f.isDirectory()) {
			throw new RuntimeException("Not a directory: " + str);
		}
		Collage<Ty, En, Sym, Fk, Att, Void, Void> col = new Collage<>(ts.collage());
		File[] files = f.listFiles();
		Ty vc = Ty.Ty("Varchar");
		for (File xx : files) {
			try {
				Reader r = new FileReader(xx);
				final CSVReader reader = new CSVReaderBuilder(r).withCSVParser(parser)
						.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS).build();
	
				String[] rows = reader.readNext();
				if (rows == null) {
					reader.close();
					r.close();
					throw new RuntimeException("No header in: " + xx.getPath());
				}				
				
				String en = xx.getName();
				if (en.endsWith("." + ext)) {
					en = en.substring(0, en.length() - (ext.length() + 1));
				}
				xx.list(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith("." + ext);
					}					
				});
				En e = En.En(en.substring(0, en.length()-(1+ext.length())).replaceAll("[\uFEFF-\uFFFF]", "").trim());
				col.ens.add(e);
				for (String c0 : rows) {
					String c = c0.replace("\t"," ").replaceAll("[\uFEFF-\uFFFF]", "").trim();
					String d = (prepend ? en + colSep : "").trim() + c;
					Att att = Att.Att(En.En(en), d.trim());
					col.atts.put(att, new Pair<>(e, vc));
				}
				reader.close();
				r.close();		
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex.getMessage());
			}
			
		}

		return new Schema<>(ts, col, op);
		
	}

	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((str == null) ? 0 : str.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SchExpCsv other = (SchExpCsv) obj;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (str == null) {
			if (other.str != null)
				return false;
		} else if (!str.equals(other.str))
			return false;
		return true;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Collections.emptyList();
	}
	
	

}
