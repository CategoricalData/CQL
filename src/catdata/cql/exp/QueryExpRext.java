package catdata.cql.exp;

import static catdata.Chc.inLeft;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Eq;
import catdata.cql.Frozen;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Query.Agg;
import catdata.cql.fdm.EvalAlgebra;
import catdata.cql.fdm.EvalInstance;
import catdata.cql.fdm.EvalTransform;
import catdata.cql.fdm.LiteralInstance;
import catdata.cql.fdm.LiteralTransform;
import catdata.cql.fdm.SaturatedInstance;
import gnu.trove.map.TObjectIntMap;

public final class QueryExpRext extends QueryExp {

	public final QueryExp Q1, Q2;
	public final Map<String, String> options;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		Q1.map(f);
		Q2.map(f);
	}

	public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(Q1.deps(), Q2.deps());
	}

	public QueryExpRext(QueryExp Q1, QueryExp Q2, List<Pair<String, String>> options) {
		this.Q1 = Q1;
		this.Q2 = Q2;
		this.options = Util.toMapSafely(options);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (Q1.hashCode());
		result = prime * result + (Q2.hashCode());
		result = prime * result + (options.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QueryExpRext))
			return false;
		QueryExpRext other = (QueryExpRext) obj;
		if (Q1 == null) {
			if (other.Q1 != null)
				return false;
		} else if (!Q1.equals(other.Q1))
			return false;
		if (Q2 == null) {
			if (other.Q2 != null)
				return false;
		} else if (!Q2.equals(other.Q2))
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
		return "rext " + Q1 + " " + Q2;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		if (!G.eq(Q1.type(G).first, Q2.type(G).first)) {
			throw new RuntimeException("Cannot Rext: source of first, " + Q1.type(G).first
					+ " is not the same as source of second, " + Q2.type(G).first);
		}
		return new Pair<>(Q1.type(G).second, Q2.type(G).second);
	}

	// private static int i = 0;

	public synchronized Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
		AqlOptions ops = new AqlOptions(options, env.defaults);

		Query<String, String, Sym, Fk, Att, String, Fk, Att> q1 = Q1.eval(env, isC);
		Query<String, String, Sym, Fk, Att, String, Fk, Att> q2 = Q2.eval(env, isC);

		if (isC) {
			throw new IgnoreException();
		}

		return doRext(ops, q1, q2);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Query<String, String, Sym, Fk, Att, String, Fk, Att> doRext(AqlOptions ops,
			Query<String, String, Sym, Fk, Att, String, Fk, Att> qAB,
			Query<String, String, Sym, Fk, Att, String, Fk, Att> qAC) {

		Map<String, Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens = new HashMap<>();
		Map<Att, Chc<Term<String, String, Sym, Fk, Att, String, String>, Agg<String, String, Sym, Fk, Att>>> atts = new HashMap<>();
		Map<Fk, Pair<Map<String, Term<Void, String, Void, Fk, Void, String, Void>>, AqlOptions>> fks = new HashMap<>();
		Map<Fk, Map<String, Term<String, String, Sym, Fk, Att, String, String>>> sks = new HashMap<>();

		Map<String, TObjectIntMap> j = new HashMap<>();
		Map<String, TObjectIntMap> j2 = new HashMap<>();

		int i = 100;
		for (String cen : qAC.dst.ens) {
			Frozen aInst = qAC.ens.get(cen);
			EvalInstance bInst = new EvalInstance(qAB, aInst, ops);
			LinkedHashMap<String, Chc<String, String>> m = new LinkedHashMap<>();

			var p = bInst.algebra().intifyX(i);
			i += bInst.size(); //move to guids
			var q = bInst.algebra().intifyY(i);
			i += bInst.algebra().talg().sks.size();

			
			bInst.gens().forEach((g, t) -> {
				m.put("gen" + Integer.toString(((TObjectIntMap) p.first).get(g)), inLeft((String) t));
			});

			bInst.sks().forEach((g, t) -> {
				var ss = (String) t;
				m.put("sk" + Integer.toString(((TObjectIntMap) q.first).get(g)), Chc.inRight((String) t));
			});
			Collection<Eq<String, String, Sym, Fk, Att, String, String>> eqs = new LinkedList<>();
			bInst.eqs((l, r) -> {
				var ll = Integer.toString(((TObjectIntMap) p.first).get(l));
				var rr = Integer.toString(((TObjectIntMap) p.first).get(r));

				eqs.add(new Eq(Collections.emptyMap(),
						((Term) l).mapGenSk(k -> "gen" + Integer.toString(((TObjectIntMap) p.first).get(k)),
								k -> "sk" + Integer.toString(((TObjectIntMap) q.first).get(k))),
						((Term) r).mapGenSk(k -> "gen" + Integer.toString(((TObjectIntMap) p.first).get(k)),
								k -> "sk" + Integer.toString(((TObjectIntMap) q.first).get(k)))));
			});
			ens.put(cen, new Triple<>(m, eqs, ops));
			j.put(cen, (TObjectIntMap) p.first);
			j2.put(cen, (TObjectIntMap) q.first);

			for (Att batt : qAC.dst.attsFrom(cen)) {
				 
				 Transform t = qAC.att(batt);
				 Term v = t.trans(Term.Sk("_y_"));
				
				// System.out.println("^^ " + v);
				// h.dst().algebra().intoY(h.reprT(sk1)).convert();
				// Term t2 = cInst.algebra().intoY(t).convert();

				 Term t3 = v.mapGenSk(k -> "gen" + Integer.toString(((TObjectIntMap)
				 p.first).get(k)), k -> "sk" + Integer.toString(((TObjectIntMap) q.first).get(k)));
			
				 //System.out.println("^x^ " + t3);
					
				 atts.put(batt, Chc.inLeft(t3));
			}

		}

		for (Entry<Fk, Pair<String, String>> bfk : qAC.dst.fks.entrySet()) {
			Transform t = qAC.fks.get(bfk.getKey());
			EvalTransform h = new EvalTransform(qAB, t, ops);

			Map<String, Term<String, String, Sym, Fk, Att, String, String>> n = new HashMap<>();
			Map<String, Term<Void, String, Void, Fk, Void, String, Void>> m = new HashMap<>();

			h.src().gens().forEach((g, e) -> {
				var xxx = "gen" + Integer.toString(j.get(bfk.getValue().second).get(g));
				var yyy = "gen" + Integer.toString(j.get(bfk.getValue().first).get(h.repr(e, g)));
				m.put(xxx, Term.Gen(yyy));
			});
			h.src().sks().forEach((g, e) -> {
				//chc vs string
				Integer jjj = (Integer) j2.get(bfk.getValue().second).get(g);
				Term jhj = h.reprT(g);
				Term hh =  jhj.mapGenSk(k -> "gen" +  
						Integer.toString(j.get(bfk.getValue().first).get(k)), k -> "sk" + Integer.toString(j2.get(bfk.getValue().first).get(k)));
				n.put("sk" + Integer.toString(jjj), hh);
			});

			fks.put(bfk.getKey(), new Pair<>(m, ops));
			sks.put(bfk.getKey(), n);

			// new LiteralTransform((g,e)->m.get(g),(s,t)->n.get(s),
			// ens.get(bfk.getValue().second), ens.get(bfk.getValue().second), false);
		}
		
	//	System.out.println("AA" + ens);
//		System.out.println(atts);
//		System.out.println("XX" + fks);
//		System.out.println("YY" + sks);
//		System.out.println("j2" + j2);
		
		

		return Query.makeQuery(ens, atts, fks, sks, qAB.dst, qAC.dst, ops);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

}