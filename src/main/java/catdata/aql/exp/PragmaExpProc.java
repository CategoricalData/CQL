package catdata.aql.exp;

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
import catdata.aql.Kind;
import catdata.aql.Pragma;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.fdm.ProcPragma;

public final class PragmaExpProc extends PragmaExp {
	private final List<String> cmds;

	@Override
	public Map<String, String> options() {
		return options;
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
	}

	@Override
	public Unit type(AqlTyping G) {
		return Unit.unit;
	}

	private final Map<String, String> options;

	public PragmaExpProc(List<String> cmds, List<Pair<String, String>> options) {
		this.options = Util.toMapSafely(options);
		this.cmds = cmds;
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((cmds == null) ? 0 : cmds.hashCode());
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
		PragmaExpProc other = (PragmaExpProc) obj;

		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (cmds == null) {
			if (other.cmds != null)
				return false;
		} else if (!cmds.equals(other.cmds))
			return false;
		return true;
	}

	@Override
	public Pragma eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		return new ProcPragma(cmds);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("exec_cmdline {")
				.append(Util.sep(cmds.stream().map(Util::quote).collect(Collectors.toList()), "\n"));

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
	}

}