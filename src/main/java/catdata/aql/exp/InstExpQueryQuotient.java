package catdata.aql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.list.TreeList;

import catdata.Chc;
import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.Query;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import catdata.aql.exp.QueryExpRaw.Block;
import catdata.aql.exp.QueryExpRaw.PreBlock;
import catdata.aql.fdm.EvalInstance;
import catdata.aql.fdm.InitialAlgebra;
import catdata.aql.fdm.LiteralInstance;
import catdata.aql.fdm.Row;
import catdata.aql.fdm.SaturatedInstance;
import catdata.aql.fdm.SigmaChaseAlgebra;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class InstExpQueryQuotient<Gen, Sk, X, Y> extends InstExp<Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>>
		implements Raw {

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.singleton(I);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.map(f);
	}

	public final InstExp<Gen, Sk, X, Y> I;

	public final Map<String, String> options;

	public final Set<Block> queries;

	public InstExpQueryQuotient(InstExp<Gen, Sk, X, Y> i, List<Pair<LocStr, PreBlock>> list,
			List<Pair<String, String>> options) {
		I = i;
		this.options = Util.toMapSafely(options);

		this.queries = Util.toSetSafely(list).stream().map(x -> new Block(x.second, x.first, x.second.star))
				.collect(Collectors.toSet());

		for (Pair<LocStr, PreBlock> p : list) {
			List<InteriorLabel<Object>> f = new TreeList<>();
			if (p.second.star) {
				throw new RuntimeException("Cannot use *");
			}
			f.add(new InteriorLabel<>("entities", p.second, p.first.loc, x -> p.first.str).conv());

			raw.put(p.first.str, f);
		}

	}

	private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return raw;
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	@Override
	public SchExp type(AqlTyping G) {
		for (Block b : queries) {
			if (!b.atts.isEmpty()) {
				throw new RuntimeException("From should not have attributes");
			}
			if (!b.fks.isEmpty()) {
				throw new RuntimeException("From should not have foreign keys");
			}
			if (b.gens.size() != 2) {
				throw new RuntimeException("From should have two variables");
			}
			for (Pair<catdata.aql.Var, String> s : b.gens) {
				if (!s.second.equals(b.en.str)) {
					throw new RuntimeException("From clause should match (the written down) entity of " + b.en.str);
				}
			}
		}
		return I.type(G);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>> eval0(AqlEnv env,
			boolean isC) {
		Schema<Ty, En, Sym, Fk, Att> sss = I.type(env.typing).eval(env, isC);
		Set<En> ensX = new THashSet<>();
		for (Block b : queries) {
			ensX.add(b.en);
		}
		Schema<Ty, En, Sym, Void, Void> dst = sss.discretize(ensX);
		Map<En, Triple<Map<catdata.aql.Var, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>>, AqlOptions>> ens = new THashMap<>();
		Map<En, Collage<Ty, En, Sym, Fk, Att, Var, Var>> cols = new THashMap<>();

		for (Block b : queries) {
			QueryExpRaw.processBlock(b.options, env, sss, ens, cols, b, Collections.emptyMap());
		}

		AqlOptions op = new AqlOptions(options, null, env.defaults);

		Query<Ty, En, Sym, Fk, Att, En, Void, Void> q = Query.makeQuery(ens, Collections.emptyMap(),
				Collections.emptyMap(), Collections.emptyMap(), sss, dst, op);
		if (isC) {
			throw new IgnoreException();
		}

		Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I0 = I.eval(env, isC);

		EvalInstance<Ty, En, Sym, Fk, Att, Gen, Sk, En, Void, Void, X, Y> J = new EvalInstance<>(q, I0, op);

		boolean useChase = (boolean) op.getOrDefault(AqlOption.quotient_use_chase);

		if (useChase) {
			Map<En, Set<Pair<X, X>>> mm = new THashMap<>(ens.size());
			for (Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> p : J.gens().keySet()) {
				Map<catdata.aql.Var, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> m = p.asMap();

				List<Var> vs = new TreeList<>(m.keySet());
				Var v1 = vs.get(0);
				Var v2 = vs.get(1);
				Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> x1 = p.get(v1);
				Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> x2 = p.get(v2);
				En en = J.gens().get(p);
				if (!mm.containsKey(en)) {
					mm.put(en, new THashSet<>());
				}
				mm.get(en).add(new Pair<>(x1.l, x2.l));
			}

			SigmaChaseAlgebra<Ty, En, Sym, Fk, Att, En, Fk, Att, Gen, Sk, X, Y> alg = new SigmaChaseAlgebra<>(
					Mapping.id(I0.schema()), I0, mm, op);

			return new SaturatedInstance(alg, alg, (Boolean) op.getOrDefault(AqlOption.require_consistency),
					(Boolean) op.getOrDefault(AqlOption.allow_java_eqs_unsafe), false, null);

		}
		return evalProver(env, I0, J);

	}

	private Instance<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>> evalProver(AqlEnv env,
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I0,
			EvalInstance<Ty, En, Sym, Fk, Att, Gen, Sk, En, Void, Void, X, Y> J) {
		Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col = new Collage<>(I0.collage());

		List<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqs0 = new ArrayList<>(
				J.gens().size());

		AqlOptions strat = new AqlOptions(options, col, env.defaults);

		for (Row<En, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> p : J.gens().keySet()) {
			Map<catdata.aql.Var, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> m = p.asMap();

			List<Var> vs = new TreeList<>(m.keySet());
			Var v1 = vs.get(0);
			Var v2 = vs.get(1);
			Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> x1 = p.get(v1);
			Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> x2 = p.get(v2);
			Term<Void, En, Void, Fk, Void, Gen, Void> t1 = I0.algebra().repr(p.en2(), x1.l);
			Term<Void, En, Void, Fk, Void, Gen, Void> t2 = I0.algebra().repr(p.en2(), x2.l);
			eqs0.add(new Pair<>(t1.convert(), t2.convert()));
			col.eqs.add(new Eq<>(null, t1.convert(), t2.convert()));
		}

		InitialAlgebra<Ty, En, Sym, Fk, Att, Gen, Sk> initial0 = new InitialAlgebra<>(strat, I0.schema(), col, (y) -> y,
				(x, y) -> y);

		LiteralInstance<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>> ret = new LiteralInstance<>(
				I0.schema(), col.gens, col.sks, Util.iterConcat(I0.eqs(), eqs0), initial0.dp(), initial0,
				(Boolean) strat.getOrDefault(AqlOption.require_consistency),
				(Boolean) strat.getOrDefault(AqlOption.allow_java_eqs_unsafe));

		return ret;
	}

	private String toString;

	@Override
	public synchronized String toString() {
		if (toString != null) {
			return toString;
		}
		toString = "";

		List<String> temp = new LinkedList<>();

		if (!queries.isEmpty()) {
			for (Block x : queries) {
				temp.add(x.toString());
			}
			toString += "\n\t\t" + Util.sep(temp, "\n\n\t\t") + "\n";
		}

		if (!options.isEmpty()) {
			toString += "\toptions";
			temp = new LinkedList<>();
			for (Entry<String, String> sym : options.entrySet()) {
				temp.add(sym.getKey() + " = " + sym.getValue());
			}

			toString += "\n\t\t" + Util.sep(temp, "\n\t\t") + "\n";
		}

		toString = "quotient_query " + I + " {" + toString + "}";
		return toString;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((I == null) ? 0 : I.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((queries == null) ? 0 : queries.hashCode());
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
		InstExpQueryQuotient<?, ?, ?, ?> other = (InstExpQueryQuotient<?, ?, ?, ?>) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (queries == null) {
			if (other.queries != null)
				return false;
		} else if (!queries.equals(other.queries))
			return false;
		return true;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return I.deps();
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.add(AqlOption.quotient_use_chase);
	}

	/*
	 * public void validate(AqlEnv env) { SchExp sch0 = I.type(env.typing);
	 * Schema<Ty, En, Sym, Fk, Att> sch = sch0.eval0(env, true); Set<En> ensX =
	 * Collections.synchronizedSet(new THashSet<>()); for (Block b : queries) {
	 * ensX.add(b.en); }
	 * 
	 * Ctx<En, Triple<Ctx<Var, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att,
	 * Var, Var>>, AqlOptions>> ens = new Ctx<>(); Ctx<En, Collage<Ty, En, Sym, Fk,
	 * Att, Var, Var>> cols = new Ctx<>();
	 * 
	 * for (Block b : queries) { QueryExpRaw.processBlock(b.options, env, sch, ens,
	 * cols, b, new Ctx<>()); }
	 * 
	 * AqlOptions op = new AqlOptions(options, null, env.defaults); Schema<Ty, En,
	 * Sym, Void, Void> dst = sch.discretize(ensX);
	 * 
	 * Query<Ty, En, Sym, Fk, Att, En, Void, Void> q = Query.makeQuery(ens, new
	 * Ctx<>(), new Ctx<>(), new Ctx<>(), sch, dst, op);
	 * q.validate((boolean)op.getOrDefault(AqlOption.dont_validate_unsafe));
	 * 
	 * }
	 */

}
