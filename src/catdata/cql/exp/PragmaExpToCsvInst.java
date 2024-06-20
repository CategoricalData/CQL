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
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.ToCsvPragmaInstance;

@SuppressWarnings("hiding")
public final class PragmaExpToCsvInst<X, Y> extends PragmaExp {

	public final String file;

	public final Map<String, String> options;

	public final InstExp<String, String, X, Y> inst;

	@Override
	public Unit type(AqlTyping G) {
		inst.type(G);
		return Unit.unit;
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		inst.map(f);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.csv_field_delim_char);
		set.add(AqlOption.csv_escape_char);
		set.add(AqlOption.csv_quote_char);
		set.add(AqlOption.csv_file_extension);
		set.add(AqlOption.csv_generate_ids);
		set.add(AqlOption.emit_ids);
		set.add(AqlOption.csv_prepend_entity);
		set.add(AqlOption.prepend_entity_on_ids);
		set.add(AqlOption.id_column_name);
		set.add(AqlOption.csv_row_sort_order);

	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	public PragmaExpToCsvInst(InstExp<String, String, X, Y> inst, String file, List<Pair<String, String>> options) {
		Util.assertNotNull(file, options, inst);
		this.file = file;
		this.options = Util.toMapSafely(options);
		this.inst = inst;
	}

	@Override
	public Pragma eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		AqlOptions op = new AqlOptions(options, env.defaults);
		return new ToCsvPragmaInstance<>(inst.eval(env, false), file, op);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("export_csv_instance ").append(inst).append(" ")
				.append(Util.quote(file));

		if (!options.isEmpty()) {
			sb.append(" {").append("\n\toptions").append(Util.sep(options, "\n\t\t", " = ")).append("}");
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + ((inst == null) ? 0 : inst.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
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
		PragmaExpToCsvInst<?, ?> other = (PragmaExpToCsvInst<?, ?>) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (inst == null) {
			if (other.inst != null)
				return false;
		} else if (!inst.equals(other.inst))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return inst.deps();
	}

}