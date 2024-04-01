package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.apg.exp.ApgInstExp;
import catdata.apg.exp.ApgInstExp.ApgInstExpVisitor;
import catdata.apg.exp.ApgMapExp;
import catdata.apg.exp.ApgMapExp.ApgMapExpVisitor;
import catdata.apg.exp.ApgSchExp;
import catdata.apg.exp.ApgSchExp.ApgSchExpVisitor;
import catdata.apg.exp.ApgTransExp;
import catdata.apg.exp.ApgTransExp.ApgTransExpVisitor;
import catdata.apg.exp.ApgTyExp;
import catdata.apg.exp.ApgTyExp.ApgTyExpVisitor;
import catdata.cql.AqlSyntax;
import catdata.cql.Kind;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.ColimSchExp.ColimSchExpVisitor;
import catdata.cql.exp.CommentExp.CommentExpVisitor;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import catdata.cql.exp.GraphExp.GraphExpVisitor;
import catdata.cql.exp.InstExp.InstExpVisitor;
import catdata.cql.exp.MapExp.MapExpVisitor;
import catdata.cql.exp.MorExp.MorExpVisitor;
import catdata.cql.exp.PragmaExp.PragmaExpVisitor;
import catdata.cql.exp.QueryExp.QueryExpVisitor;
import catdata.cql.exp.SchExp.SchExpVisitor;
import catdata.cql.exp.TyExp.TyExpVisitor;
import gnu.trove.set.hash.THashSet;

public abstract class Exp<X> {

	public boolean isVar() {
		return false;
	}

	public final void map(Consumer<Exp<?>> f) {
		f.accept(this);
		mapSubExps(f);
	}

	public abstract void mapSubExps(Consumer<Exp<?>> f);

	protected abstract void allowedOptions(Set<AqlOption> set);

	public Set<AqlOption> allowedOptions() {
		Set<AqlOption> ret = new THashSet<>();
		ret.add(AqlOption.always_reload);
		ret.add(AqlOption.timeout);
		ret.add(AqlOption.num_threads);
		ret.add(AqlOption.talg_reduction);
		allowedOptions(ret);
		return ret;
	}

	public AqlSyntax getSyntax() {
		String name = this.getClass().getSimpleName();
		return AqlSyntax.valueOf(name);
	}

	public String getKeyword() {
		AqlSyntax x = getSyntax(); // completeness check
		String t = toString();
		if (t.startsWith("[")) {
			return kind() + "_compose";
		} else if (t.contains("Var")) {
			return kind() + "_var";
		} else if (t.contains("literal")) {
			return kind() + "_literal";
		} else if (t.contains("identity")) {
			return kind() + "_identity";
		} else if (t.startsWith("(")) {
			return kind() + "_product";
		} else if (t.startsWith("<")) {
			return kind() + "_coproduct";
		} else if (t.contains(" ")) {
			return t.toString().substring(0, t.indexOf(" "));
		}

		String z = x.toString().substring(x.toString().indexOf("Exp") + 3, x.toString().length());
		if (z.contains("Var")) {
			return kind() + "_var";
		}
		return z.toLowerCase();
	}

	public Object getOrDefault(AqlEnv env, AqlOption option) {
		return env.defaults.getOrDefault(options(), option);
	}

	public abstract Object type(AqlTyping G);

	public synchronized Chc<String, Object> type0(AqlTyping G, Map<String, ?> errs) {
		Set<String> set = new TreeSet<>();
		for (Pair<String, Kind> x : deps()) {
			if (errs.containsKey(x.first)) {
				set.add(x.first);
			}
		}
		if (!set.isEmpty()) {
			return Chc.inLeftNC("Depends on " + Util.sep(Util.alphabetical(set), ", "));
		}
		try {
			Object t = type(G);
			return Chc.inRightNC(t);
		} catch (Throwable thr) {
			// thr.printStackTrace();
			if (thr.getMessage() == null) {
				thr.printStackTrace();
				return Chc.inLeftNC("Anomaly Please Report");
			}
			return Chc.inLeftNC(thr.getMessage());
		}
	}

	public final synchronized Optional<Chc<String, X>> eval_static(AqlEnv env, Map<String, Optional<String>> exns) {
		Set<String> set = new TreeSet<>();
		for (Pair<String, Kind> x : deps()) {
			if (exns.containsKey(x.first)) {
				set.add(x.first);
			}
			if (!env.defs.keySet().contains(x.first)) {
				return Optional.empty();
			}
		}

		if (!set.isEmpty()) {
			return Optional.empty();
		}
		try {
			X x = eval(env, true);
			Util.assertNotNull(x);
			return Optional.of(Chc.inRightNC(x));
		} catch (IgnoreException ex) {
			return Optional.empty();
		} catch (Exception thr) {
			if (thr.getMessage() == null) {
				thr.printStackTrace();
			}
			String s = thr.getMessage() == null ? "Anomaly: please report.  (Null pointer)" : thr.getMessage();
			// thr.printStackTrace();
			return Optional.of(Chc.inLeftNC(s));
		}
	}

	public static interface ExpVisitor<P, X, Sch extends X, Mor extends X, Ty extends X, Inst extends X, M extends X, Q extends X, Trans extends X, Col extends X, Com extends X, Con extends X, Gr extends X, Prag extends X, E extends Exception, A1, A2, A3, A4, A5>
			extends SchExpVisitor<Sch, P, E>, TyExpVisitor<Ty, P, E>, InstExpVisitor<Inst, P, E>, MapExpVisitor<M, P, E>,
			QueryExpVisitor<Q, P, E>, TransExpVisitor<Trans, P, E>, ColimSchExpVisitor<Col, P, E>, CommentExpVisitor<Com, P, E>,
			EdsExpVisitor<Con, P, E>, GraphExpVisitor<Gr, P, E>, PragmaExpVisitor<Prag, P, E>, MorExpVisitor<Mor, P, E>,
			ApgTyExpVisitor<A1, P, E>, ApgInstExpVisitor<A2, P, E>, ApgTransExpVisitor<A3, P>, ApgSchExpVisitor<A4, P>,
			ApgMapExpVisitor<A5, P> {
	}

	public <P, Z, Sch extends Z, Ty extends Z, Inst extends Z, Mor extends Z, M extends Z, Q extends Z, Trans extends Z, Col extends Z, Com extends Z, Con extends Z, Gr extends Z, Prag extends Z, E extends Exception, A1 extends Z, A2 extends Z, A3 extends Z, A4 extends Z, A5 extends Z> Z accept0(
			P params, ExpVisitor<P, Z, Sch, Mor, Ty, Inst, M, Q, Trans, Col, Com, Con, Gr, Prag, E, A1, A2, A3, A4, A5> v)
			throws E {
		switch (kind()) {
			case COMMENT:
				return ((CommentExp) this).accept(params, v);
			case CONSTRAINTS:
				return ((EdsExp) this).accept(params, v);
			case GRAPH:
				return ((GraphExp) this).accept(params, v);
			case INSTANCE:
				return ((InstExp<?, ?, ?, ?>) this).accept(params, v);
			case MAPPING:
				return ((MapExp) this).accept(params, v);
			case THEORY_MORPHISM:
				return ((MorExp) this).accept(params, v);
			case PRAGMA:
				return ((PragmaExp) this).accept(params, v);
			case QUERY:
				return ((QueryExp) this).accept(params, v);
			case SCHEMA:
				return ((SchExp) this).accept(params, v);
			case SCHEMA_COLIMIT:
				return ((ColimSchExp) this).accept(params, v);
			case TRANSFORM:
				return ((TransExp<?, ?, ?, ?, ?, ?, ?, ?>) this).accept(params, v);
			case TYPESIDE:
				return ((TyExp) this).accept(params, v);
			case APG_typeside:
				return ((ApgTyExp) this).accept(params, v);
			case APG_instance:
				return ((ApgInstExp) this).accept(params, v);
			case APG_morphism:
				return ((ApgTransExp) this).accept(params, v);
			case APG_mapping:
				return ((ApgMapExp) this).accept(params, v);
			case APG_schema:
				return ((ApgSchExp) this).accept(params, v);

			default:
				return Util.anomaly();

		}
	}

	protected abstract Map<String, String> options();

	public abstract Kind kind();

	public abstract Exp<X> Var(String v);

	protected abstract X eval0(AqlEnv env, boolean isCompileTime);

	@SuppressWarnings("unchecked")
	public final X eval(AqlEnv env, boolean isCompileTime) {
		boolean b = (boolean) getOrDefault(env, AqlOption.always_reload);
		if (b) {
			return eval0(env, isCompileTime);
		}
		synchronized (env.cache) {
			X x = (X) env.cache.get(this);
			if (x != null) {
				return x;
			}
		}
		X x;
		synchronized (this) {
			x = eval0(env, isCompileTime);
			Util.assertNotNull(x);

			synchronized (env.cache) {
				X y = (X) env.cache.get(this);
				if (y != null) {
					return y;
				}
				env.cache.put(this, x);
				return x;
			}
		}

	}

	public String printNicely(AqlTyping g) {
		StringBuffer sb = new StringBuffer();
		Object u = type(g);
		sb.append(g.toString());
		sb.append("----------------------------------\n");
		sb.append(kind() + " ");
		if (toString().contains(" ") && !toString().contains("[")) {
			sb.append("(" + this + ")");
		} else {
			sb.append(toString());
		}
		if (!(u instanceof Unit)) {
			if (u instanceof Pair) {
				Pair<?, ?> p = (Pair<?, ?>) u;
				sb.append(" : " + p.first + " -> " + p.second);
			} else if (u instanceof Set) {
				if (!((Set<?>) u).isEmpty()) {
					sb.append(" : " + Util.sep(((Set<?>) u), ", "));
				}
			} else {
				sb.append(" : " + u);
			}
		}
		return sb.toString();
	}

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract String toString();

	/**
	 * This will not capture global order constraints; for example, that commands
	 * form barriers.
	 */
	public abstract Collection<Pair<String, Kind>> deps();

	public Collection<Exp<?>> imports() {
		return Collections.emptySet();
	}

}
