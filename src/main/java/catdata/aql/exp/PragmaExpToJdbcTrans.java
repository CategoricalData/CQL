package catdata.aql.exp;

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
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Pragma;
import catdata.aql.fdm.ToJdbcPragmaTransform;

public class PragmaExpToJdbcTrans<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> extends PragmaExp {

	public final String jdbcString, prefix;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		h.map(f);
	}

	@Override
	public Unit type(AqlTyping G) {
		h.type(G);
		return Unit.unit;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.jdbc_default_class);
		set.add(AqlOption.jdbc_default_string);
		set.add(AqlOption.start_ids_at);
		set.add(AqlOption.id_column_name);
		set.add(AqlOption.varchar_length);
		set.add(AqlOption.jdbc_quote_char);
	}

	public final Map<String, String> options1, options2;

	public final TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h;

	@Override
	public Map<String, String> options() {
		return options1;
	}

	public PragmaExpToJdbcTrans(TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h, String jdbcString, String prefix,
			List<Pair<String, String>> options1, List<Pair<String, String>> options2) {
		this.jdbcString = jdbcString;
		this.prefix = prefix;
		// this.clazz = clazz;
		this.options1 = Util.toMapSafely(options1);
		this.options2 = Util.toMapSafely(options2);
		this.h = h;
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return h.deps();
	}

	@Override
	public Pragma eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		String toGet = jdbcString;
		// String driver = clazz;
		AqlOptions op1 = new AqlOptions(options1, env.defaults);
		AqlOptions op2 = new AqlOptions(options2, env.defaults);

		// if (clazz.trim().isEmpty()) {
		// driver = (String) op1.getOrDefault(AqlOption.jdbc_default_class);
		// }
		if (jdbcString.trim().isEmpty()) {
			toGet = (String) op1.getOrDefault(AqlOption.jdbc_default_string);
		}
		return new ToJdbcPragmaTransform<>(prefix, h.eval(env, false), toGet, op1, op2);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("export_jdbc_transform ").append(h).append(" ").append(" ")
				.append(Util.quote(jdbcString)).append(" ").append(Util.quote(prefix)).append(" ");
		if (!options1.isEmpty()) {
			sb.append("{");
			sb.append("\n\toptions").append(Util.sep(options1, "\n\t\t", " = ")).append("\n");
			sb.append("}");
		}
		if (!options2.isEmpty()) {
			sb.append("{");
			sb.append("\n\toptions").append(Util.sep(options2, "\n\t\t", " = ")).append("\n");
			sb.append("}");
		}
		return sb.toString().trim();
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((h == null) ? 0 : h.hashCode());
		// result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
		result = prime * result + ((options1 == null) ? 0 : options1.hashCode());
		result = prime * result + ((options2 == null) ? 0 : options2.hashCode());
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
		PragmaExpToJdbcTrans<?, ?, ?, ?, ?, ?, ?, ?> other = (PragmaExpToJdbcTrans<?, ?, ?, ?, ?, ?, ?, ?>) obj;

		if (h == null) {
			if (other.h != null)
				return false;
		} else if (!h.equals(other.h))
			return false;
		if (jdbcString == null) {
			if (other.jdbcString != null)
				return false;
		} else if (!jdbcString.equals(other.jdbcString))
			return false;
		if (options1 == null) {
			if (other.options1 != null)
				return false;
		} else if (!options1.equals(other.options1))
			return false;
		if (options2 == null) {
			if (other.options2 != null)
				return false;
		} else if (!options2.equals(other.options2))
			return false;
		if (prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!prefix.equals(other.prefix))
			return false;
		return true;
	}

}