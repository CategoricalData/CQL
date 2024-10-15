package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Constraints;
import catdata.cql.Eq;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Query.Agg;
import catdata.cql.Term;
import catdata.cql.Transform;

public class QueryExpChase extends QueryExp {

	public final QueryExp Q;
	public final EdsExp C;

	public QueryExpChase(QueryExp q, EdsExp c) {
		Q = q;
		C = c;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		Pair<SchExp, SchExp> p = Q.type(G);
		SchExp q = C.type(G);
		if (!p.first.equals(q)) {
			throw new RuntimeException("Source of query,\n" + p.first + "\nis not that of constraints,\n" + q + "\n");
		}
		return p;
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		Q.mapSubExps(f);
		C.mapSubExps(f);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	protected Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isCompileTime) {
		var q = Q.eval(env, isCompileTime);
		var c = C.eval(env, isCompileTime);

		if (!q.src.fks.isEmpty() || !q.dst.fks.isEmpty()) {
			throw new RuntimeException("Fks not supported");
		}

		return chase(q, c);

	}

	static Query<String, String, Sym, Fk, Att, String, Fk, Att> chase(
			Query<String, String, Sym, Fk, Att, String, Fk, Att> q, Constraints c) {
		Map<String, Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens = new HashMap<>();
		Map<Fk, Map<String, Term<String, String, Sym, Fk, Att, String, String>>> sks = new HashMap<>();

		Map<Object, String> m1 = new HashMap<>();
		Map<Object, String> m2 = new HashMap<>();
		int[] i = { 0 };
		Map<Att, Chc<Term<String, String, Sym, Fk, Att, String, String>, Agg<String, String, Sym, Fk, Att>>> atts = new HashMap<>();
		for (var b : q.ens.entrySet()) {

			LinkedHashMap<String, Chc<String, String>> fr = new LinkedHashMap<>();
			Collection<Eq<String, String, Sym, Fk, Att, String, String>> wh = new HashSet<>();

			var ret = c.chase(b.getValue(), AqlOptions.initialOptions);
			ret.first.gens().forEach((t, u) -> {
				String x = "v" + i[0];
				i[0]++;
				m1.put(t, x);
				fr.put(x, Chc.inLeft(u));
			});
			ret.first.sks().forEach((t, u) -> {
				String x = "v" + i[0];
				i[0]++;
				m2.put(t, x);
				fr.put(x, Chc.inRight(u));
			});

			ret.first.eqs(
					(t1, t2) -> wh.add(new Eq<>(null, t1.mapGenSk(m1::get, m2::get), t2.mapGenSk(m1::get, m2::get))));

			for (var att : q.dst.attsFrom(b.getKey())) {

				Term<String, String, Sym, Fk, Att, String, String> kkk = q.atts.get(att).l;
				// kkk = Query.freeze(kkk, Collections.emptyMap(), Collections.emptySet());
				b.getValue().type(kkk);
				// Transform jjj = ret.second;

				ret.first.type(ret.second.trans(kkk));

				atts.put(att, Chc.inLeft(ret.second.trans(kkk).mapGenSk(m1::get, m2::get)));
			}

			ens.put(b.getKey(), new Triple<>(fr, wh, AqlOptions.initialOptions));
		}

//		 var ret = new Query<>(q.params, q.consts, ens, q.atts, Collections.emptyMap(), Collections.emptyMap(), q.src, q.dst, AqlOptions.initialOptions);
		return Query.makeQuery(ens, atts, Collections.emptyMap(), Collections.emptyMap(), q.src, q.dst,
				AqlOptions.initialOptions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(C, Q);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryExpChase other = (QueryExpChase) obj;
		return Objects.equals(C, other.C) && Objects.equals(Q, other.Q);
	}

	@Override
	public String toString() {
		return "chase " + C + " " + Q;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(Q.deps(), C.deps());
	}

}
