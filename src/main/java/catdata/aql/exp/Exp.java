package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.AqlSyntax;
import catdata.aql.Kind;
import catdata.aql.exp.ColimSchExp.ColimSchExpVisitor;
import catdata.aql.exp.CommentExp.CommentExpVisitor;
import catdata.aql.exp.EdsExp.EdsExpVisitor;
import catdata.aql.exp.GraphExp.GraphExpVisitor;
import catdata.aql.exp.InstExp.InstExpVisitor;
import catdata.aql.exp.MapExp.MapExpVisitor;
import catdata.aql.exp.PragmaExp.PragmaExpVisitor;
import catdata.aql.exp.QueryExp.QueryExpVisitor;
import catdata.aql.exp.SchExp.SchExpVisitor;
import catdata.aql.exp.TyExp.TyExpVisitor;
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
			if (thr.getMessage() == null) {
				thr.printStackTrace();
				return Util.anomaly();
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
			// thr.printStackTrace();
			return Optional.of(Chc.inLeftNC(thr.getMessage()));
		}
	}

	public static interface ExpVisitor<P, X, Sch extends X, Ty extends X, Inst extends X, M extends X, Q extends X, Trans extends X, Col extends X, Com extends X, Con extends X, Gr extends X, Prag extends X, E extends Exception>
			extends SchExpVisitor<Sch, P, E>, TyExpVisitor<Ty, P, E>, InstExpVisitor<Inst, P, E>,
			MapExpVisitor<M, P, E>, QueryExpVisitor<Q, P, E>, TransExpVisitor<Trans, P, E>,
			ColimSchExpVisitor<Col, P, E>, CommentExpVisitor<Com, P, E>, EdsExpVisitor<Con, P, E>,
			GraphExpVisitor<Gr, P, E>, PragmaExpVisitor<Prag, P, E> {
	}

	public <P, Z, Sch extends Z, Ty extends Z, Inst extends Z, M extends Z, Q extends Z, Trans extends Z, Col extends Z, Com extends Z, Con extends Z, Gr extends Z, Prag extends Z, E extends Exception> Z accept0(
			P params, ExpVisitor<P, Z, Sch, Ty, Inst, M, Q, Trans, Col, Com, Con, Gr, Prag, E> v) throws E {
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

	private final Supplier<String> latestToString = Suppliers.memoizeWithExpiration(this::makeString, 20,
			TimeUnit.SECONDS);

	@Override
	public String toString() {
		return this.latestToString.get();
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

	public String makeString() {
		return "undefined expression";
	}

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object o);

	/**
	 * This will not capture global order constraints; for example, that commands
	 * form barriers.
	 */
	public abstract Collection<Pair<String, Kind>> deps();

	public Collection<Exp<?>> imports() {
		return Collections.emptySet();
	}

}
