package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.fdm.InitialAlgebra;
import catdata.aql.fdm.LiteralInstance;

public final class InstExpCoEq<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>
		extends InstExp<Gen2, Sk2, Integer, Chc<Sk2, Pair<Integer, Att>>> {

	public final TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t1, t2;

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		t1.map(f);
		t2.map(f);
	}

	public final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.emptySet();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(t1.deps(), t2.deps());
	}

	public InstExpCoEq(TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t1,
			TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t2, List<Pair<String, String>> options) {
		this.t1 = t1;
		this.t2 = t2;
		this.options = Util.toMapSafely(options);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((t1 == null) ? 0 : t1.hashCode());
		result = prime * result + ((t2 == null) ? 0 : t2.hashCode());
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
		InstExpCoEq<?, ?, ?, ?, ?, ?, ?, ?> other = (InstExpCoEq<?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (t1 == null) {
			if (other.t1 != null)
				return false;
		} else if (!t1.equals(other.t1))
			return false;
		if (t2 == null) {
			if (other.t2 != null)
				return false;
		} else if (!t2.equals(other.t2))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "coequalize " + t1 + " " + t2;
	}

	@Override
	public SchExp type(AqlTyping G) {
		if (!t1.type(G).first.equals(t2.type(G).first)) {
			throw new RuntimeException("Domains do not match: " + t1.type(G).first + " and \n\n" + t2.type(G).first);
		} else if (!t1.type(G).second.equals(t2.type(G).second)) {
			throw new RuntimeException(
					"CoDomains do not match: " + t1.type(G).second + " and \n\n" + t2.type(G).second);
		}
		return t1.type(G).first.type(G);
	}

	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, Integer, Chc<Sk2, Pair<Integer, Att>>> eval0(
			AqlEnv env, boolean isC) {
		Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h1 = t1.eval(env, isC);
		Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h2 = t2.eval(env, isC);
		if (isC) {
			throw new IgnoreException();
		}
		Collage<Ty, En, Sym, Fk, Att, Gen2, Sk2> col = new CCollage<>(h1.dst().collage());
		AqlOptions strat = new AqlOptions(options, env.defaults);
		h1.dst().eqs((a,b) -> {
			col.eqs().add(new Eq<>(null, a, b));
		});

		h1.src().gens().entrySet((k,t) -> {
			Term<Ty, En, Sym, Fk, Att, Gen2, Sk2> l = h1.gens().apply(k,t).convert();
			Term<Ty, En, Sym, Fk, Att, Gen2, Sk2> r = h2.gens().apply(k,t).convert();
			col.eqs().add(new Eq<>(null, l, r));
		});
		h1.src().sks().entrySet((k,t) -> {
			Term<Ty, En, Sym, Fk, Att, Gen2, Sk2> l = h1.sks().apply(k,t);
			Term<Ty, En, Sym, Fk, Att, Gen2, Sk2> r = h2.sks().apply(k,t);
			col.eqs().add(new Eq<>(null, l, r));
		});
		InitialAlgebra<Ty, En, Sym, Fk, Att, Gen2, Sk2> initial0 = new InitialAlgebra<>(strat, h1.src().schema(), col,
				(y) -> y, (x, y) -> y);

		return new LiteralInstance<>(h1.src().schema(), col.gens(), col.sks(), col.eqsAsPairs(), initial0.dp(), initial0,
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