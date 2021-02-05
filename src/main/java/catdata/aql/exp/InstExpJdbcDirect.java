package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.LocStr;
import catdata.Null;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;

public class InstExpJdbcDirect extends InstExp<Gen, Null<?>, Gen, Null<?>> {


	public final String jdbcString;
	public final String rowNumStr;
	private Map<String, String> options;
	private SchExp schema;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		this.schema.map(f);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.emptySet();
	}

	public InstExpJdbcDirect(SchExp schema, List<Pair<String, String>> options, String jdbcString, String rowNumStr) {
		this.options = Util.toMapSafely(options);
		this.schema = schema;
		this.jdbcString = jdbcString;
		this.rowNumStr = rowNumStr;
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("import_jdbc_direct ").append(schema).append(" ")
				.append(" ").append(Util.quote(jdbcString));

		if (!options.isEmpty() || !options.isEmpty()) {
			sb.append(" {\n\t").append(Util.sep(options, " -> ", "\n\t", Util::quote));
			sb.append("\n\toptions\n\t\t").append(Util.sep(options, " = ", "\n\t\t"));
			sb.append("\n}");
		}
		return sb.toString();
	}


	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.addAll(AqlOptions.proverOptionNames());
		set.add(AqlOption.jdbc_default_class);
		set.add(AqlOption.jdbc_default_string);
		set.add(AqlOption.import_as_theory);
		set.add(AqlOption.import_missing_is_empty);
		set.add(AqlOption.import_null_on_err_unsafe);
		set.add(AqlOption.import_col_seperator);
		set.add(AqlOption.csv_import_prefix);
		set.add(AqlOption.import_dont_check_closure_unsafe);
		set.add(AqlOption.prepend_entity_on_ids);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.jdbc_quote_char);
		set.add(AqlOption.import_sql_direct_prefix);
	}

	@Override
	public SchExp type(AqlTyping G) {
		schema.type(G);
		return schema;
	}

	@Override
	protected Map<String, String> options() {
		return options;
	}

	@Override
	protected Instance<Ty, En, Sym, Fk, Att, Gen, Null<?>, Gen, Null<?>> eval0(AqlEnv env, boolean isCompileTime) {
		List<Pair<LocStr, String>> map = new LinkedList<>();
		var sch = schema.eval(env, isCompileTime);
		if (!sch.fks.isEmpty()) {
			throw new RuntimeException("Direct JDBC import always lands onto attributes.");
		}
		String prefix = (String) new AqlOptions(options, env.defaults).getOrDefault(AqlOption.import_sql_direct_prefix);
		var lll = Util.toList(options);
		lll.add(new Pair<>(AqlOption.id_column_name.toString(), "CQL_ROW_ID"));
		for (En en : sch.ens) {
			String z =  Util.sep(sch.attsFrom(en).iterator(), ", ", x -> x.str);
			if (sch.attsFrom(en).isEmpty()) {
				z = " " + rowNumStr + " AS CQL_ROW_ID ";
			} else {
				z  += ", " + rowNumStr + " AS CQL_ROW_ID ";
			}
			String query = "SELECT " + z + " FROM " + prefix + en.str + ", (SELECT '0' AS CQL_ZERO) AS CQL_ZERO";
			map.add(new Pair<>(new LocStr(0, en.str), query));
		}
		
		return new InstExpJdbc(schema, lll, jdbcString, map).eval(env, isCompileTime);		
	}

	
	@Override
	public Collection<Pair<String, Kind>> deps() {
		return schema.deps();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((rowNumStr == null) ? 0 : rowNumStr.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
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
		InstExpJdbcDirect other = (InstExpJdbcDirect) obj;
		if (jdbcString == null) {
			if (other.jdbcString != null)
				return false;
		} else if (!jdbcString.equals(other.jdbcString))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (rowNumStr == null) {
			if (other.rowNumStr != null)
				return false;
		} else if (!rowNumStr.equals(other.rowNumStr))
			return false;
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		return true;
	}

	
	
	
	
}
