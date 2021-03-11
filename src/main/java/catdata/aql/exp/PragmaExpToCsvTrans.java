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
import catdata.aql.fdm.ToCsvPragmaTransform;

@SuppressWarnings("hiding")
public final class PragmaExpToCsvTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> extends PragmaExp {

	public final String file;

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.csv_field_delim_char);
		set.add(AqlOption.csv_escape_char);
		set.add(AqlOption.csv_quote_char);
		set.add(AqlOption.csv_file_extension);
		set.add(AqlOption.csv_generate_ids);
		set.add(AqlOption.csv_emit_ids);
		set.add(AqlOption.csv_prepend_entity);
		set.add(AqlOption.prepend_entity_on_ids);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		trans.map(f);
	}

	public final Map<String, String> options1;
	public final Map<String, String> options2;

	public final TransExp<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> trans;

	@Override
	public Unit type(AqlTyping G) {
		trans.type(G);
		return Unit.unit;
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Map<String, String> options() {
		return options1;
	}

	public PragmaExpToCsvTrans(TransExp<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> trans, String file,
			List<Pair<String, String>> options1, List<Pair<String, String>> options2) {
		this.file = file;
		this.options1 = Util.toMapSafely(options1);
		this.options2 = Util.toMapSafely(options2);
		this.trans = trans;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + ((options1 == null) ? 0 : options1.hashCode());
		result = prime * result + ((options2 == null) ? 0 : options2.hashCode());
		result = prime * result + ((trans == null) ? 0 : trans.hashCode());
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
		PragmaExpToCsvTrans<?, ?, ?, ?, ?, ?, ?, ?> other = (PragmaExpToCsvTrans<?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
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
		if (trans == null) {
			if (other.trans != null)
				return false;
		} else if (!trans.equals(other.trans))
			return false;
		return true;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return trans.deps();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("export_csv_transform ").append(trans).append(" ")
				.append(Util.quote(file));

		if (!options1.isEmpty()) {
			sb.append("{\n\toptions").append(Util.sep(options1, "\n\t\t", " = ")).append("\n}");
		}
		if (!options2.isEmpty()) {
			sb.append("\n {\n\toptions").append(Util.sep(options2, "\n\t\t", " = ")).append("\n}");
		}
		return sb.toString();
	}

	@Override
	public Pragma eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		AqlOptions op1 = new AqlOptions(options1, env.defaults);
		AqlOptions op2 = new AqlOptions(options2, env.defaults);
		return new ToCsvPragmaTransform<>(trans.eval(env, false), file, op1, op2);
	}
}