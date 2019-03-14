package catdata.aql.exp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.fdm.CoprodInstance;
import catdata.aql.fdm.InitialAlgebra;
import catdata.aql.fdm.LiteralInstance;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class InstExpCoProdFull<Gen, Sk, X, Y>
		extends InstExp<Pair<String, Gen>, Pair<String, Sk>, Integer, Chc<Pair<String, Sk>, Pair<Integer, Att>>> {

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	public final List<String> Is;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		sch.map(f);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Is.stream().map(x -> new InstExpVar(x)).collect(Collectors.toSet());
	}

	public final SchExp sch;

	public final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	public InstExpCoProdFull(List<String> is, SchExp sch, List<Pair<String, String>> options) {
		Is = is;
		if (is.size() != new THashSet<>(Is).size()) {
			throw new RuntimeException("Duplicate name in " + Util.sep(is, ", "));
		}
		this.sch = sch;
		this.options = Util.toMapSafely(options);
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		Set<Pair<String, Kind>> ret = new THashSet<>(sch.deps());
		for (String i : Is) {
			ret.add(new Pair<>(i, Kind.INSTANCE));
		}
		return ret;
	}

	@Override
	public String toString() {
		return "coproduct " + Util.sep(Is, " + ") + " : " + sch;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Is == null) ? 0 : Is.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((sch == null) ? 0 : sch.hashCode());
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
		InstExpCoProdFull<?, ?, ?, ?> other = (InstExpCoProdFull<?, ?, ?, ?>) obj;
		if (Is == null) {
			if (other.Is != null)
				return false;
		} else if (!Is.equals(other.Is))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (sch == null) {
			if (other.sch != null)
				return false;
		} else if (!sch.equals(other.sch))
			return false;
		return true;
	}

	@Override
	public SchExp type(AqlTyping G) {
		for (String x : Is) {
			SchExp t = new InstExpVar(x).type(G);
			if (!G.eq(t, sch)) {
				throw new RuntimeException(
						"Instance " + x + " has schema " + t + ",\n\nnot " + sch + "\n\nas expected");
			}
		}
		return sch;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Integer, Chc<Pair<String, Sk>, Pair<Integer, Att>>> eval0(
			AqlEnv env, boolean isC) {
		Schema<Ty, En, Sym, Fk, Att> sch0 = sch.eval(env, isC);

		Collage<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>> col = new Collage<>(sch0.collage());
		AqlOptions strat = new AqlOptions(options, col, env.defaults);
		Set<Pair<Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>, Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>>> eqs0 = (new THashSet<>());

		Map<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> m = (new THashMap<>());
		boolean onlyFree = true;
		for (String x : Is) {
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I = (Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>) new InstExpVar(
					x).eval(env, isC);
			if (!I.schema().equals(sch0)) {
				throw new RuntimeException(x + " not on given schema ");
			}
			m.put(x, I);
			if (!I.algebra().hasFreeTypeAlgebra()) {
				onlyFree = false;
			}
		}
		if (isC) {
			throw new IgnoreException();
		}

		if (onlyFree) {
			CoprodInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> k = new CoprodInstance<>(m, sch0,
					(boolean) strat.getOrDefault(AqlOption.allow_java_eqs_unsafe),
					(boolean) strat.getOrDefault(AqlOption.require_consistency));

			Object o = k;
			return (Instance<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Integer, Chc<Pair<String, Sk>, Pair<Integer, Att>>>) o;
		}

		for (String x : Is) {
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I = m.get(x);
			for (Gen g : I.gens().keySet()) {
				col.gens.put(new Pair<>(x, g), I.gens().get(g));
			}
			for (Sk g : I.sks().keySet()) {
				col.sks.put(new Pair<>(x, g), I.sks().get(g));
			}
			Function<Gen, Pair<String, Gen>> f1 = z -> new Pair<>(x, z);
			Function<Sk, Pair<String, Sk>> f2 = z -> new Pair<>(x, z);
			for (Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq : I.eqs()) {
				eqs0.add(new Pair<>(eq.first.mapGenSk(f1, f2), eq.second.mapGenSk(f1, f2)));
				col.eqs.add(new Eq<>(null, eq.first.mapGenSk(f1, f2), eq.second.mapGenSk(f1, f2)));
			}
		}
		InitialAlgebra<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>> initial0 = new InitialAlgebra<>(strat,
				sch0, col, (y) -> y, (x, y) -> y);

		return new LiteralInstance<>(sch0, col.gens, col.sks, eqs0, initial0.dp(), initial0,
				(Boolean) strat.getOrDefault(AqlOption.require_consistency),
				(Boolean) strat.getOrDefault(AqlOption.allow_java_eqs_unsafe));
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.addAll(AqlOptions.proverOptionNames());
	}

}