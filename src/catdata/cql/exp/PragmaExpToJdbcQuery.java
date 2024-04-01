package catdata.cql.exp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.ToJdbcPragmaQuery;

@SuppressWarnings("hiding")
public class PragmaExpToJdbcQuery extends PragmaExp {

	public final String jdbcString, prefixSrc, prefixDst;

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.jdbc_default_class);
		set.add(AqlOption.jdbc_default_string);
		set.add(AqlOption.jdbc_query_export_convert_type);
		set.add(AqlOption.id_column_name);
		set.add(AqlOption.varchar_length);
		set.add(AqlOption.jdbc_quote_char);
	}

	public final Map<String, String> options;

	public final QueryExp Q;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		Q.map(f);
	}

	@Override
	public Unit type(AqlTyping G) {
		Q.type(G);
		return Unit.unit;
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public PragmaExpToJdbcQuery(QueryExp Q, String jdbcString, String prefixSrc, String prefixDst,
			List<Pair<String, String>> options) {
		this.jdbcString = jdbcString;
		this.prefixSrc = prefixSrc;
		this.prefixDst = prefixDst;
		// this.clazz = clazz;
		this.options = Util.toMapSafely(options);
		this.Q = Q;
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Q.deps();
	}

	@Override
	public Pragma eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		String toGet = jdbcString;
		// String driver = clazz;
		AqlOptions op = new AqlOptions(options, env.defaults);
		//// if (clazz.trim().isEmpty()) {
		// driver = (String) op.getOrDefault(AqlOption.jdbc_default_class);
		// }
		if (jdbcString.trim().isEmpty()) {
			toGet = (String) op.getOrDefault(AqlOption.jdbc_default_string);
		}
		return new ToJdbcPragmaQuery<>(prefixSrc, prefixDst, Q.eval(env, false), toGet, op);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("export_jdbc_query ").append(Q).append(" ").append(" ")
				.append(Util.quote(jdbcString)).append(" ").append(Util.quote(prefixSrc)).append(" ")
				.append(Util.quote(prefixDst));
		if (!options.isEmpty()) {
			sb.append(" {").append("\n\toptions").append(Util.sep(options, "\n\t\t", " = ")).append("}");
		}
		return sb.toString().trim();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Q == null) ? 0 : Q.hashCode());
		// result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((prefixDst == null) ? 0 : prefixDst.hashCode());
		result = prime * result + ((prefixSrc == null) ? 0 : prefixSrc.hashCode());
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
		PragmaExpToJdbcQuery other = (PragmaExpToJdbcQuery) obj;
		if (Q == null) {
			if (other.Q != null)
				return false;
		} else if (!Q.equals(other.Q))
			return false;
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
		if (prefixDst == null) {
			if (other.prefixDst != null)
				return false;
		} else if (!prefixDst.equals(other.prefixDst))
			return false;
		if (prefixSrc == null) {
			if (other.prefixSrc != null)
				return false;
		} else if (!prefixSrc.equals(other.prefixSrc))
			return false;
		return true;
	}

}