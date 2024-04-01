package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Eq;
import catdata.cql.It;
import catdata.cql.Kind;
import catdata.cql.Mapping;
import catdata.cql.Query;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.It.ID;
import catdata.cql.Query.Agg;
import catdata.cql.fdm.DeltaInstance;
import catdata.cql.fdm.DeltaTransform;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class QueryExpDeltaMigrate extends QueryExp {

	public final MapExp F;
	public final QueryExp Q;

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		F.mapSubExps(f);
		Q.mapSubExps(f);
	}

	public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(F.deps(), Q.deps());
	}

	public QueryExpDeltaMigrate(MapExp F, QueryExp Q) {
		this.F = F;
		this.Q = Q;
	}

	@Override
	public int hashCode() {
		final int prime = 3;
		int result = 1;
		result = prime * result + ((F == null) ? 0 : F.hashCode());
		result = prime * result + ((Q == null) ? 0 : Q.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QueryExpDeltaEval))
			return false;
		QueryExpDeltaMigrate other = (QueryExpDeltaMigrate) obj;
		if (F == null) {
			if (other.F != null)
				return false;
		} else if (!F.equals(other.F))
			return false;
		if (Q == null) {
			if (other.Q != null)
				return false;
		} else if (!Q.equals(other.Q))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "delta " + F + " " + Q;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		var q = Q.type(G);
		var f = F.type(G);
		if (!f.second.equals(q.first)) {
			throw new RuntimeException("Target of mapping:\n\n" + f.second + "\n\nnot equal to source of query:\n\n" + q.first);
		}
		return new Pair<>(f.first, q.second);
	}

	@Override
	public Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> F0 = F.eval(env, isC);
		Query<String, String, Sym, Fk, Att, String, Fk, Att> Q0 = Q.eval(env, isC);
		AqlOptions ops = env.defaults;
		
		Map<String, Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens = new THashMap<>();
		Map<Att, Chc<Term<String, String, Sym, Fk, Att, String, String>, Agg<String, String, Sym, Fk, Att>>> atts = new THashMap<>();
		Map<Fk, Pair<Map<String, Term<Void, String, Void, Fk, Void, String, Void>>, AqlOptions>> fks = new THashMap<>();
		Map<Fk, Map<String, Term<String, String, Sym, Fk, Att, String, String>>> sks = new THashMap<>();

		Map<String, DeltaInstance<String, String, Sym, Fk, Att, String, String, String, Fk, Att, It.ID,Chc<String,Pair<It.ID,Att>>>> ens0 = new HashMap<>();
		for (var en : Q0.dst.ens) {
			var fr = Q0.ens.get(en);
			var d = new DeltaInstance<>(F0, fr);
			var u = d.unfreeze(ops);
			ens0.put(en, d);
			ens.put(en, u);
		}
		for (var att : Q0.atts.keySet()) {
			var t = new DeltaTransform<>(F0, Q0.att(att)); 
			
			var temp = t.sksExtensional();
			var t0 = Util.get0(temp.values());
			
			var en = Q0.dst.atts.get(att).first;
			atts.put(att, Chc.inLeft(ens0.get(en).unfreeze(t0)));
		}
		if (Q0.dst.fks.size() != 0) {
			throw new RuntimeException("todo");
		}
  
		return Query.makeQuery(ens, atts, fks, sks, F0.src, Q0.dst, ops);

	}

	public static Query<String, String, Sym, Fk, Att, String, Fk, Att> extracted(AqlOptions ops,
			Mapping<String, String, Sym, Fk, Att, String, Fk, Att> F0) {
		
		Map<String, Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens = new THashMap<>();
		Map<Att, Chc<Term<String, String, Sym, Fk, Att, String, String>, Agg<String, String, Sym, Fk, Att>>> atts = new THashMap<>();
		Map<Fk, Pair<Map<String, Term<Void, String, Void, Fk, Void, String, Void>>, AqlOptions>> fks = new THashMap<>();
		Map<Fk, Map<String, Term<String, String, Sym, Fk, Att, String, String>>> sks = new THashMap<>();

		String v = ("v");
		for (String en : F0.src.ens) {
			LinkedHashMap<String, Chc<String, String>> fr = new LinkedHashMap<>();
			fr.put(v, Chc.inLeft(F0.ens.get(en)));
			ens.put(en, new Triple<>(fr, new THashSet<>(), ops));
		}
		for (Att att : F0.src.atts.keySet()) {
			Term<String, String, Sym, Fk, Att, String, String> h = F0.atts.get(att).third.mapGenSk(Util.voidFn(),
					Util.voidFn());
			Term<String, String, Sym, Fk, Att, String, String> g = Term.Gen(v);
			Term<String, String, Sym, Fk, Att, String, String> t = h
					.subst(Collections.singletonMap(F0.atts.get(att).first, g));
			atts.put(att, Chc.inLeft(t));
		}
		for (Fk fk : F0.src.fks.keySet()) {
			Map<String, Term<Void, String, Void, Fk, Void, String, Void>> g = new THashMap<>();
			g.put(v, Term.Fks(F0.fks.get(fk).second, Term.Gen(v)));
			fks.put(fk, new Pair<>(g, ops));
			sks.put(fk, new THashMap<>());
		}

		return Query.makeQuery(ens, atts, fks, sks, F0.dst, F0.src, ops);
	}

}