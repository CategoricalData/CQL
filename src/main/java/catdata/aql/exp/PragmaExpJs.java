package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Pragma;
import catdata.aql.fdm.JsPragma;

public final class PragmaExpJs extends PragmaExp {
	private final List<String> jss;

	private final Map<String, String> options;

	@Override
	public Unit type(AqlTyping G) {
		return Unit.unit;
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public PragmaExpJs(List<String> jss, List<Pair<String, String>> options) {
		this.options = Util.toMapSafely(options);
		this.jss = jss;
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((jss == null) ? 0 : jss.hashCode());
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
		PragmaExpJs other = (PragmaExpJs) obj;

		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (jss == null) {
			if (other.jss != null)
				return false;
		} else if (!jss.equals(other.jss))
			return false;
		return true;
	}

	@Override
	public Pragma eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		return new JsPragma(jss, options, env);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.js_env_name);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("exec_js {")
				.append(Util.sep(jss.stream().map(Util::quote).collect(Collectors.toList()), ""));

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
	public void mapSubExps(Consumer<Exp<?>> f) {

	}

}