package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.JdbcPragma;

public final class PragmaExpSql extends PragmaExp {
	public final List<String> sqls;

	private final String jdbcString;

	@Override
	public Unit type(AqlTyping G) {
		return Unit.unit;
	}

	private final Map<String, String> options;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public PragmaExpSql(String jdbcString, List<String> sqls, List<Pair<String, String>> options) {
		this.jdbcString = jdbcString;
		this.options = Util.toMapSafely(options);
		this.sqls = sqls;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((sqls == null) ? 0 : sqls.hashCode());
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
		PragmaExpSql other = (PragmaExpSql) obj;
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
		if (sqls == null) {
			if (other.sqls != null)
				return false;
		} else if (!sqls.equals(other.sqls))
			return false;
		return true;
	}

	@Override
	public Pragma eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		String toGet = jdbcString;
		AqlOptions op = new AqlOptions(options, env.defaults);
		if (jdbcString.trim().isEmpty()) {
			toGet = (String) op.getOrDefault(AqlOption.jdbc_default_string);
		}
		return new JdbcPragma(toGet, sqls);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("exec_jdbc ").append(" ").append(Util.quote(jdbcString))
				.append(" {").append(Util.sep(sqls.stream().map(Util::quote).collect(Collectors.toList()), "\n"));

		if (!options.isEmpty()) {
			sb.append("\n\toptions").append(Util.sep(options, "\n\t\t", " = "));
		}
		return sb.append("}").toString();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Collections.emptyList();
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.jdbc_default_class);
		set.add(AqlOption.jdbc_default_string);
	}

}