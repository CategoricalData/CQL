package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Constraints;
import catdata.cql.Eq;
import catdata.cql.Frozen;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Query.Agg;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.fdm.LiteralTransform;

public class QueryExpReformulate extends QueryExp {

	public final QueryExp Q;
	public final EdsExp C;
	public final SchExp T;
	public final String idx;

	public QueryExpReformulate(QueryExp q, EdsExp c, SchExp t, String i) {
		Q = q;
		C = c;
		T = t;
		idx = i;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		Pair<SchExp, SchExp> p = Q.type(G);
		SchExp q = C.type(G);
		if (!p.first.equals(q)) {
			throw new RuntimeException("Source of query,\n" + p.first + "\nis not that of constraints,\n" + q + "\n");
		}
		// todo: check if Q src contains T, otherwise, no effect
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
		T.mapSubExps(f);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	public static Term<String, String, Sym, Fk, Att, String, String> helper0(Frozen<String, String, Sym, Fk, Att> i,
			Map<String, Chc<String, String>> gens, Term<String, String, Sym, Fk, Att, String, String> t) {
		if (t.obj() != null) {
			return t;
		}
		if (t.sym() != null) {
			return Term.Sym(t.sym(), t.args.stream().map(x -> helper0(i, gens, x)).collect(Collectors.toList()));
		}
		if (t.att() != null) {
			if (gens.containsKey(t.arg.asGen().gen())) {
				return t;
			}
//			System.out.println("no " + t.arg.asGen() + " in " + gens.keySet());
			for (Entry<String, Chc<String, String>> x : gens.entrySet()) {
				if (!x.getValue().left) {
					continue;
				}
				for (Att att : i.schema.attsFrom(x.getValue().l)) {
					if (!i.schema.atts.get(att).second.equals(i.schema.atts.get(t.att()).second)) {
						continue;
					}
					Term<String, String, Sym, Fk, Att, String, String> test = Term.Att(att, Term.Gen(x.getKey()));
					if (i.dp().eq(null, test, t)) {
						return test;
					}
				}
			}
		}
		if (t.sk() != null) {
			if (gens.containsKey(t.arg.asSk().sk())) {
				return t;
			}
			for (Entry<String, Chc<String, String>> x : gens.entrySet()) {
				if (x.getValue().left) {
					continue;
				}

				if (!x.getValue().r.equals(i.sks.get(t.sk()))) {
					continue;
				}
				Term<String, String, Sym, Fk, Att, String, String> test = Term.Sk(x.getKey());
				if (i.dp().eq(null, test, t)) {
					return test;
				}
			}
		}

		return null;

	}

	public static Frozen<String, String, Sym, Fk, Att> helper1(Frozen<String, String, Sym, Fk, Att> i,
			Set<Chc<Pair<String, String>, Pair<String, String>>> cand) {
		Collection<Eq<String, String, Sym, Fk, Att, String, String>> eqs = new HashSet<>();
		Map<String, Chc<String, String>> gens = new HashMap<>();
		for (var x : cand) {
			if (x.left) {
				gens.put(x.l.first, Chc.inLeft(x.l.second));
			} else {
				gens.put(x.r.first, Chc.inRight(x.r.second));
			}
		}
		List<String> order = new LinkedList<>();
		for (var x : gens.entrySet()) {
			order.add(x.getKey());
		}
		for (var eq : i.eqs) {
//	System.out.println("On " + eq);
			Term l = helper0(i, gens, eq.first);
			if (l != null) {
//				System.out.println("l " + l);

				Term r = helper0(i, gens, eq.second);
				if (r != null) {
					// System.out.println("r" + r);

					if (!l.equals(r)) {
						eqs.add(new Eq<>(null, l, r));
					}
				}
			}
		}

		return new Frozen<String, String, Sym, Fk, Att>(Collections.emptyMap(), gens, eqs, i.schema, i.options, order);

	}

	public static List<Pair<Att, Term<String, String, Sym, Fk, Att, String, String>>> helper2(
			Frozen<String, String, Sym, Fk, Att> i,
			List<Pair<Att, Term<String, String, Sym, Fk, Att, String, String>>> ret,
			Set<Chc<Pair<String, String>, Pair<String, String>>> cand) {

		List<Pair<Att, Term<String, String, Sym, Fk, Att, String, String>>> newRet = new LinkedList<>();

		Map<String, Chc<String, String>> gens = new HashMap<>();
		for (var x : cand) {
			if (x.left) {
				gens.put(x.l.first, Chc.inLeft(x.l.second));
			} else {
				gens.put(x.r.first, Chc.inRight(x.r.second));
			}
		}

		for (var r : ret) {
			Term<String, String, Sym, Fk, Att, String, String> t = helper0(i, gens, r.second);
			if (t == null) {
				return null;
			}
			newRet.add(new Pair<>(r.first, t));
		}

		return newRet;
	}

	public static List<Query<String, String, Sym, Fk, Att, String, Fk, Att>> minimal(
			Query<String, String, Sym, Fk, Att, String, Fk, Att> q, Constraints c) {
		if (q.ens.size() != 1) {
			Util.anomaly();
		}
		var i = Util.get0(q.ens.values());
		List<Pair<Att, Term<String, String, Sym, Fk, Att, String, String>>> r = q.atts.entrySet().stream()
				.map(x -> new Pair<>(x.getKey(), x.getValue().l)).collect(Collectors.toList());

		Set<Chc<Pair<String, String>, Pair<String, String>>> set = new HashSet<>();

		i.gens().forEach((g, t) -> {
			set.add(Chc.inLeft(new Pair<>(g, t)));
		});
		i.sks().forEach((g, t) -> {
			set.add(Chc.inRight(new Pair<>(g, t)));
		});
		Set<Set<Chc<Pair<String, String>, Pair<String, String>>>> cands = Util.powerSet(set);

		// System.out.println("Set " + Util.sep(set, ", "));

		Set<Set<Chc<Pair<String, String>, Pair<String, String>>>> ignoreList = new HashSet<>();
		List<Query<String, String, Sym, Fk, Att, String, Fk, Att>> ret = new LinkedList<>();

		List<Set<Chc<Pair<String, String>, Pair<String, String>>>> cands0 = new LinkedList<>(cands);
		cands0.sort(new Comparator<>() {

			@Override
			public int compare(Set<Chc<Pair<String, String>, Pair<String, String>>> o1,
					Set<Chc<Pair<String, String>, Pair<String, String>>> o2) {
				return Integer.compare(o1.size(), o2.size());
			}

		});
		for (Set<Chc<Pair<String, String>, Pair<String, String>>> can : cands0) {
			// System.out.println("Consider " + Util.sep(can, " , "));
			for (var x : ignoreList) {
				if (can.containsAll(x)) {
					continue;
				}
			}

			var i2 = helper1(i, can);
			if (i2 == null) {
				continue;
			}
			var r2 = helper2(i, r, can);
			if (r2 == null) {
				continue;
			}
			ignoreList.add(can);

			Map<String, Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens = new HashMap<>();
			LinkedHashMap<String, Chc<String, String>> map = new LinkedHashMap<>();
			Set<Eq<String, String, Sym, Fk, Att, String, String>> eqs = new HashSet<>();

			i2.gens.forEach((v, t) -> map.put(v, Chc.inLeft(t)));
			i2.sks.forEach((v, t) -> map.put(v, Chc.inRight(t)));

			i2.eqs.forEach(x -> eqs.add(new Eq<>(null, x.first, x.second)));
			// System.out.println(i);

			// System.out.println(i2);

			Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions> tr = new Triple<>(
					map, eqs, AqlOptions.initialOptions);

			ens.put(Util.get0(q.ens.keySet()), tr);

			Map<Att, Chc<Term<String, String, Sym, Fk, Att, String, String>, Agg<String, String, Sym, Fk, Att>>> atts = new HashMap<>();
			for (var x : r2) {
				atts.put(x.first, Chc.inLeft(x.second));
			}

			Query<String, String, Sym, Fk, Att, String, Fk, Att> sc = Query.makeQuery(ens, atts, Collections.emptyMap(),
					Collections.emptyMap(), q.src, q.dst, AqlOptions.initialOptions);

			

			if (null == hom(sc, QueryExpChase.chase(q, c))) {
				Util.anomaly(); // subsquery should be syntactically part of universal plan
			}

			if (null == hom(q, QueryExpChase.chase(sc, c))) {

			} else {
				ret.add(sc);
			} // else {
				// System.out.println(" is not subseteq " + q);
				// }

		}

		return ret;
	}

	/*
	 * Checking sq ⊆C q reduces according to classical results to finding a
	 * containment mapping from q into the result of chasing sq with C [2]
	 */

	public static boolean subseteq(Query<String, String, Sym, Fk, Att, String, Fk, Att> sq,
			Query<String, String, Sym, Fk, Att, String, Fk, Att> q, Constraints c) {
		return hom(q, QueryExpChase.chase(sq, c)) != null;
	}

	private static <X, Y> List<Map<X, Y>> extend(List<Map<X, Y>> ms, X x, Y y) {
		List<Map<X, Y>> ret = new LinkedList<>();

		for (Map<X, Y> m : ms) {
			Map<X, Y> n = new HashMap<>(m);
			n.put(x, y);
			ret.add(n);
		}

		return ret;
	}

	public static Transform hom(Query<String, String, Sym, Fk, Att, String, Fk, Att> q1,
			Query<String, String, Sym, Fk, Att, String, Fk, Att> q2) {
		if (!q1.ens.keySet().equals(q2.ens.keySet())) {
			Util.anomaly();
		}
		if (q1.ens.size() != 1 || q2.ens.size() != 1) {
			Util.anomaly();
		}

		for (String name : q1.ens.keySet()) {
			var i1 = q1.ens.get(name);
			var i2 = q2.ens.get(name);

			List<Map<Chc<String, String>, Chc<String, String>>>[] n1 = new List[] {
					Collections.singletonList(Collections.emptyMap()) };

			boolean[] br = { false };
			i1.gens().forEach((g, t) -> {
				int[] rr = { 0 };
				List<Map<Chc<String, String>, Chc<String, String>>> ll = new LinkedList<>();
				i2.gens.forEach((g0, t0) -> {
					if (t.equals(t0)) {
						ll.addAll(extend(n1[0], Chc.inLeft(g), Chc.inLeft(g0)));
						rr[0]++;
					}
				});
				if (rr[0] == 0) {
					br[0] = true;
				}
				n1[0] = ll;
			});
			if (br[0]) {
				return null;
			}
			i1.sks().forEach((g, t) -> {
				int[] rr = { 0 };
				List<Map<Chc<String, String>, Chc<String, String>>> ll = new LinkedList<>();
				i2.sks().forEach((g0, t0) -> {
					if (t.equals(t0)) {
						ll.addAll(extend(n1[0], Chc.inLeft(g), Chc.inLeft(g0)));
						rr[0]++;
					}
				});
				if (rr[0] == 0) {
					br[0] = true;
				}
				n1[0] = ll;
			});
			if (br[0]) {
				return null;
			}

			outer: for (var cand : n1[0]) {

				Map<String, Term> m1 = new HashMap<>();
				Map<String, Term> m2 = new HashMap<>();
				for (var x : cand.entrySet()) {
					if (x.getKey().left) {
						m1.put(x.getKey().l, Term.Gen(x.getValue().l));
					} else {
						m2.put(x.getKey().r, Term.Sk(x.getValue().r));
					}
				}

				BiFunction<String, String, Term> f = (n, t) -> m1.get(n);
				BiFunction<String, String, Term> g = (n, t) -> m2.get(n);
				try {
					var h = new LiteralTransform(f, g, i1, i2, false);
					// System.out.println("\t ***********");
					for (var x : q1.atts.entrySet()) {
						var att2 = q2.atts.get(x.getKey()).l;
						var att1 = x.getValue().l;
						if (!i2.dp().eq(null, h.trans(att1), att2)) {
							continue outer;
						}
					}

					return h;
				} catch (Exception ex) {
					// ex.printStackTrace();

				}
			}
			return null;

		}

		return null;

	}

	private static int score(Query<String, String, Sym, Fk, Att, String, Fk, Att> q,
			Schema<String, String, Sym, Fk, Att> T) {
		int[] j = { 0 };
		for (var ten : q.ens.entrySet()) {
			ten.getValue().gens.forEach((g, t) -> {
				if (!T.ens.contains(t)) {
					j[0]++;
				}
			});
		}
		return j[0];
	}

	public Query<String, String, Sym, Fk, Att, String, Fk, Att> pickMinimal(
			List<Query<String, String, Sym, Fk, Att, String, Fk, Att>> qs, Schema<String, String, Sym, Fk, Att> T,
			int idx) {
//		System.out.println("size " + qs.size());
		qs.sort(new Comparator<>() {

			@Override
			public int compare(Query<String, String, Sym, Fk, Att, String, Fk, Att> o1,
					Query<String, String, Sym, Fk, Att, String, Fk, Att> o2) {
				int i = Integer.compare(score(o1, T), score(o2, T));
				if (i != 0) {
					return i;
				}
				return Integer.compare(Util.get0(o1.ens.values()).size(), Util.get0(o2.ens.values()).size()); 
			}

		});

	//System.out.println(	Util.sep(qs, "\n") );
		
		return qs.get(idx);
	}

	@Override
	protected Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isCompileTime) {
		var q = Q.eval(env, isCompileTime);
		var c = C.eval(env, isCompileTime);
		var t = T.eval(env, isCompileTime);

		if (!q.src.fks.isEmpty() || !q.dst.fks.isEmpty()) {
			throw new RuntimeException("Fks not supported");
		}

		return pickMinimal(minimal(q, c), t, Integer.parseInt(idx));

	}

	@Override
	public int hashCode() {
		return Objects.hash(C, Q, T, idx);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryExpReformulate other = (QueryExpReformulate) obj;
		return Objects.equals(C, other.C) && Objects.equals(Q, other.Q) && Objects.equals(T, other.T) && Objects.equals(idx, other.idx);
	}

	@Override
	public String toString() {
		return "reformulate " + C + " " + Q + " " + T;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(T.deps(), Util.union(Q.deps(), C.deps()));
	}

}