package catdata.aql.exp;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.It.ID;
import catdata.aql.Kind;
import catdata.aql.Query;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.Var;

public final class QueryExpFromCoSpan extends QueryExp {

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		M1.map(f);
		M2.map(f);
	}

	public final MapExp M1;
	public final MapExp M2;
	public final Map<String, String> options;

	public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(M1.deps(), M2.deps());
	}

	public QueryExpFromCoSpan(MapExp M1, MapExp M2, List<Pair<String, String>> options) {
		this.M1 = M1;
		this.M2 = M2;
		this.options = Util.toMapSafely(options);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (M1.hashCode());
		result = prime * result + (M2.hashCode());
		result = prime * result + (options.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QueryExpCompose))
			return false;
		QueryExpFromCoSpan other = (QueryExpFromCoSpan) obj;
		if (M1 == null) {
			if (other.M1 != null)
				return false;
		} else if (!M1.equals(other.M1))
			return false;
		if (M2 == null) {
			if (other.M2 != null)
				return false;
		} else if (!M2.equals(other.M2))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "fromCoSpan " + M1 + " " + M2;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		if (!G.eq(M1.type(G).second, M2.type(G).second)) {
			throw new RuntimeException("Cannot co-span: target of first, " + M1.type(G).second
					+ " is not the same as target of second, " + M2.type(G).second);
		}
		return new Pair<>(M1.type(G).first, M2.type(G).first);
	}

	public Query<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
		QueryExp a = new QueryExpDeltaCoEval(M1, Util.toList(options));
		QueryExp b = new QueryExpDeltaEval(M2, Util.toList(options));
		return new QueryExpCompose(a, b, Util.toList(options)).eval(env, isC);
	}

	public Term<Ty, En, Sym, Fk, Att, Var, Var> transT(Query<Ty, En, Sym, Fk, Att, En, Fk, Att> M1,
			Map<En, Pair<Map<Var, Pair<Var, Var>>, Map<Pair<Var, Var>, Var>>> iso,
			Term<Ty, En, Sym, Fk, Att, Var, Var> t, En en3, Var v) {
		if (t.obj() != null) {
			return Term.Obj(t.obj(), t.ty());
		} else if (t.sym() != null) {
			List<Term<Ty, En, Sym, Fk, Att, Var, Var>> l = new LinkedList<>();
			for (Term<Ty, En, Sym, Fk, Att, Var, Var> arg : t.args) {
				l.add(transT(M1, iso, arg, en3, v));
			}
			return Term.Sym(t.sym(), l);
		} else if (t.att() != null) {
			En en2 = M1.dst.atts.get(t.att()).first;
			Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var> ret = M1.atts.get(t.att());
			for (Var head : M1.atts.get(t.att()).gens()) {
				Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var> u = trans(M1, iso, head, t.arg, en3, en2);
				ret = ret.replace(Term.Gen(head), u.convert());
			}
			for (Var head : M1.atts.get(t.att()).sks()) {
				Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var> u = trans(M1, iso, head, t.arg, en3, en2);
				ret = ret.replace(Term.Sk(head), u.convert());
			}

			return ret;
		} else if (t.sk() != null) {
			return Term.Sk(iso.get(en3).second.get(new Pair<>(t.sk(), v)));
			// return Term.Sk(t.sk);
		}
		return Util.anomaly();
	}

	public Term<Ty, En, Sym, Fk, Att, Var, Var> trans(Query<Ty, En, Sym, Fk, Att, En, Fk, Att> M1,
			Map<En, Pair<Map<Var, Pair<Var, Var>>, Map<Pair<Var, Var>, Var>>> iso, Var p,
			Term<Ty, En, Sym, Fk, Att, Var, Var> t, En en3, En en2) {

		Transform<Ty, En, Sym, Fk, Att, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att>>, ID, Chc<Var, Pair<ID, Att>>> rhs;
		if (!t.hasTypeType()) {
			rhs = M1.compose(M1.transP(t.convert()), en2);
		} else {
			rhs = M1.composeT(t, en2);
//			rhs = M1.composeT(t, en2)
		}

		if (rhs.gens().containsKey(p)) {
			Var lhsGen = Util.get0(t.gens());
			Function<Var, Var> genf = u -> {
				return iso.get(en3).second.get(new Pair<>(lhsGen, u));
			};

			Term<Void, En, Void, Fk, Void, Var, Void> z = rhs.gens().get(p);
			Term<Void, En, Void, Fk, Void, Var, Void> xl = z.mapGen(genf);
			return xl.convert();
		} else if (rhs.sks().containsKey(p)) {

			Var lhsGen = Util.get0(t.gens());
			Function<Var, Var> genf = u -> {
				return iso.get(en3).second.get(new Pair<>(lhsGen, u));
			};

			Term<Ty, En, Sym, Fk, Att, Var, Var> z = rhs.sks().get(p);
			Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var> xl = z.mapGenSk(Function.identity(), genf);
			return xl;
		}
		return Util.anomaly();

	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.dont_validate_unsafe);
		set.add(AqlOption.query_remove_redundancy);

	}

}