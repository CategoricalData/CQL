package catdata.cql.exp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.fdm.ToJdbcPragmaInstance;

@SuppressWarnings("hiding")
public class PragmaExpToJdbcInst<X, Y> extends PragmaExp {

	public final String jdbcString;
	public final String prefix;

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.jdbc_default_class);
		set.add(AqlOption.jdbc_default_string);
		set.add(AqlOption.start_ids_at);
		set.add(AqlOption.is_oracle);
		set.add(AqlOption.emit_ids);
		set.add(AqlOption.id_column_name);
		set.add(AqlOption.varchar_length);
		set.add(AqlOption.jdbc_export_truncate_after);
		set.add(AqlOption.jdbc_quote_char);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.map(f);
	}

	public final Map<String, String> options;

	public final InstExp<String, String, X, Y> I;

	@Override
	public Unit type(AqlTyping G) {
		I.type(G);
		return Unit.unit;
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	public PragmaExpToJdbcInst(InstExp<String, String, X, Y> i, String jdbcString, String prefix,
			List<Pair<String, String>> options) {
		this.jdbcString = jdbcString;
		this.prefix = prefix;
		this.options = Util.toMapSafely(options);
		I = i;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return I.deps();
	}

	@Override
	public synchronized Pragma eval0(AqlEnv env, boolean isC) {
		String toGet = jdbcString;
		AqlOptions op = new AqlOptions(options, env.defaults);
		if (jdbcString.trim().isEmpty()) {
			toGet = (String) op.getOrDefault(AqlOption.jdbc_default_string);
		}
		if (isC) {
			throw new IgnoreException();
		}
		return new ToJdbcPragmaInstance<>(prefix, I.eval(env, false), toGet, op);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("export_jdbc_instance ").append(I).append(" ").append(" ")
				.append(Util.quote(jdbcString)).append(" ").append(Util.quote(prefix));
		if (!options.isEmpty()) {
			sb.append(" {").append("\n\toptions").append(Util.sep(options, "\n\t\t", " = ")).append("}");
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((I == null) ? 0 : I.hashCode());
		result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
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
		PragmaExpToJdbcInst<?, ?> other = (PragmaExpToJdbcInst<?, ?>) obj;

		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
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
		if (prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!prefix.equals(other.prefix))
			return false;
		return true;
	}

}