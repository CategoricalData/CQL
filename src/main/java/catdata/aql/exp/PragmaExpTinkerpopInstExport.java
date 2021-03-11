
package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Pragma;
import catdata.aql.Term;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;

public final class PragmaExpTinkerpopInstExport<Gen, Sk, X, Y> extends PragmaExp {
	private final InstExp<Gen, Sk, X, Y> jss;

	private final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	@Override
	public Unit type(AqlTyping G) {
		jss.type(G);
		return Unit.unit;
	}

	public PragmaExpTinkerpopInstExport(InstExp jss, List<Pair<String, String>> options) {
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
		PragmaExpTinkerpopInstExport other = (PragmaExpTinkerpopInstExport) obj;

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
		return new Pragma() {
			String str;

			@Override
			public void execute() {
				if (str != null) {
					return;
				}

				AqlOptions ops = new AqlOptions(options, env.defaults);
				Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> i = jss.eval(env, isC);
				Pair<TObjectIntMap<X>, TIntObjectMap<X>> m = i.algebra().intifyX(0);
				String gname = (String) ops.getOrDefault(AqlOption.tinkerpop_graph_name);
				StringBuffer sb = new StringBuffer(gname);

				for (String en : i.schema().ens) {
					for (X x : i.algebra().en(en)) {
						sb.append(".addV('" + en + "')");
						for (Att att : i.schema().attsFrom(en)) {
							String xxx = termToString(i.algebra().att(att, x));
							if (xxx != null) {
								sb.append(".property('" + att.str + "','" + xxx + "')");
							}
						}
						sb.append(".as('" + m.first.get(x) + "')");
					}
				}
				for (String en : i.schema().ens) {
					for (X x : i.algebra().en(en)) {
						int a = m.first.get(x);
						for (Fk fk : i.schema().fksFrom(en)) {
							sb.append(".addE('" + fk + "')");
							sb.append(".from('" + a + "')");
							sb.append(".to('" + m.first.get(i.algebra().fk(fk, x)) + "')");
						}
					}
				}

				str = sb.toString();

				try {
					List<Object> o = PragmaExpTinkerpop.execGremlin(ops, Collections.singletonList(str));

					str += PragmaExpTinkerpop.format(o, Collections.singletonList(str));

				} catch (Exception e) {
					e.printStackTrace();
					str += e.getMessage();
				}

			}

			@Override
			public String toString() {
				return str;
			}

		};
	}

	private String termToString(Term<String, Void, Sym, Void, Void, Void, Y> t) {
		if (t.sk() != null) {
			return null;
		} else if (t.obj() instanceof Optional) {
			Optional o = (Optional) t.obj();
			if (o.isEmpty()) {
				return "";
			}
			return o.get().toString();
		}
		return t.toString();
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.tinkerpop_host);
		set.add(AqlOption.tinkerpop_port);
		set.add(AqlOption.tinkerpop_graph_name);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("export_tinkerop_instance " + jss + " {");

		if (!options.isEmpty()) {
			sb.append("\n\toptions").append(Util.sep(options, "\n\t\t", " = "));
		}
		return sb.append("}").toString();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return jss.deps();
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		jss.mapSubExps(f);
	}

}