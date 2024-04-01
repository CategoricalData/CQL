package catdata.cql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Eq;
import catdata.cql.Frozen;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.It.ID;
import catdata.cql.Query.Agg;
import gnu.trove.map.hash.THashMap;

public final class QueryExpCompose extends QueryExp {

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

	public QueryExpCompose(QueryExp Q1, QueryExp Q2, List<Pair<String, String>> options) {
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
		if (!(obj instanceof QueryExpCompose))
			return false;
		QueryExpCompose other = (QueryExpCompose) obj;
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
		return "[" + Q1 + " ; " + Q2 + "]";
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		if (!G.eq(Q1.type(G).second, Q2.type(G).first)) {
			throw new RuntimeException("Cannot compose: target of first, " + Q1.type(G).second
					+ " is not the same as source of second, " + Q2.type(G).first);
		}
		return new Pair<>(Q1.type(G).first, Q2.type(G).second);
	}

	// private static int i = 0;

	public synchronized Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
		AqlOptions ops = new AqlOptions(options, env.defaults);

		Query<String, String, Sym, Fk, Att, String, Fk, Att> q1 = Q1.eval(env, isC);
		Query<String, String, Sym, Fk, Att, String, Fk, Att> q2 = Q2.eval(env, isC);

		if (isC) {
			throw new IgnoreException();
		}

		return doCompose(ops, q1, q2);

	}

	public static <Ty, En, Sym, Fk, Att> Query<Ty, En, Sym, Fk, Att, En, Fk, Att> doCompose(AqlOptions ops,
			Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q1, Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q2) {
		Map<En, Triple<LinkedHashMap<String, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att, String, String>>, AqlOptions>> ens = new THashMap<>();
		Map<Att, Chc<Term<Ty, En, Sym, Fk, Att, String, String>, Agg<Ty, En, Sym, Fk, Att>>> atts = new THashMap<>();
		Map<Fk, Pair<Map<String, Term<Void, En, Void, Fk, Void, String, Void>>, AqlOptions>> fks = new THashMap<>();
		Map<Fk, Map<String, Term<Ty, En, Sym, Fk, Att, String, String>>> sks = new THashMap<>();

		Map<En, Pair<Map<String, Pair<String, String>>, Map<Pair<String, String>, String>>> isos = (new THashMap<>());

		for (En En : Util.alphabetical(q2.dst.ens)) {
			int i = 0;
			Pair<Map<String, Pair<String, String>>, Map<Pair<String, String>, String>> iso = new Pair<>(
					new THashMap<>(), new THashMap<>());

			LinkedHashMap<String, Chc<En, Ty>> fr = new LinkedHashMap<>();
			Collection<Eq<Ty, En, Sym, Fk, Att, String, String>> wh = new LinkedList<>();
			for (Entry<String, En> v : Util.alphabetical(q2.ens.get(En).gens.entrySet())) {
				Frozen<Ty, En, Sym, Fk, Att> I = q1.ens.get(v.getValue());
				for (Entry<String, En> u : Util.alphabetical(I.gens.entrySet())) {
					String newV = ("vv" + (i++));
					iso.first.put(newV, new Pair<>(v.getKey(), u.getKey()));
					iso.second.put(new Pair<>(v.getKey(), u.getKey()), newV);
					fr.put(newV, Chc.inLeft(u.getValue()));
				}
				for (Entry<String, Ty> u : Util.alphabetical(I.sks.entrySet())) {
					String newV = ("vv" + (i++));
					iso.first.put(newV, new Pair<>(v.getKey(), u.getKey()));
					iso.second.put(new Pair<>(v.getKey(), u.getKey()), newV);
					fr.put(newV, Chc.inRight(u.getValue()));
				}

				Function<String, String> genf = u -> iso.second.get(new Pair<>(v.getKey(), u));
				for (Pair<Term<Ty, En, Sym, Fk, Att, String, String>, Term<Ty, En, Sym, Fk, Att, String, String>> eq : I.eqs) {
					wh.add(new Eq<>(null, eq.first.mapGenSk(genf, genf), eq.second.mapGenSk(genf, genf)));
				}
			}
			for (Entry<String, Ty> v : Util.alphabetical(q2.ens.get(En).sks.entrySet())) {

				Frozen<Ty, En, Sym, Fk, Att> I = q1.tys.get(v.getValue());
				for (Entry<String, Ty> u : Util.alphabetical(I.sks.entrySet())) {
					String newV = ("vv" + (i++));
					iso.first.put(newV, new Pair<>(v.getKey(), u.getKey()));
					iso.second.put(new Pair<>(v.getKey(), u.getKey()), newV);
					fr.put(newV, Chc.inRight(u.getValue()));
				}

			}

			for (Entry<String, En> v : Util.alphabetical(q2.ens.get(En).gens.entrySet())) {
				Frozen<Ty, En, Sym, Fk, Att> I = q1.ens.get(v.getValue());

				Function<String, String> genf = u -> iso.second.get(new Pair<>(v.getKey(), u));
				for (Pair<Term<Ty, En, Sym, Fk, Att, String, String>, Term<Ty, En, Sym, Fk, Att, String, String>> eq : I.eqs) {
					wh.add(new Eq<>(null, eq.first.mapGenSk(genf, genf), eq.second.mapGenSk(genf, genf)));
				}
			}

			ens.put(En, new Triple<>(fr, wh, ops));
			isos.put(En, iso);
		}

		for (En En : Util.alphabetical(q2.dst.ens)) {
			for (Pair<Term<Ty, En, Sym, Fk, Att, String, String>, Term<Ty, En, Sym, Fk, Att, String, String>> eq : q2.ens
					.get(En).eqs) {
				Chc<Ty, En> ty = q2.ens.get(En).type(eq.first);
				if (!ty.left) {
					q1.ens.get(ty.r).gens().keySet(v -> {

						Term<Ty, En, Sym, Fk, Att, String, String> xl = trans(q1, q2, isos, v, eq.first.asArgForFk(),
								En);

						Term<Ty, En, Sym, Fk, Att, String, String> xr = trans(q1, q2, isos, v, eq.second.asArgForFk(),
								En);

						ens.get(En).second.add(new Eq<>(null, xl.convert(), xr.convert()));
					});

					q1.ens.get(ty.r).sks().keySet(v -> {
						Term<Ty, En, Sym, Fk, Att, String, String> xl = trans(q1, q2, isos, v, eq.first.asArgForFk(),
								En);

						Term<Ty, En, Sym, Fk, Att, String, String> xr = trans(q1, q2, isos, v, eq.second.asArgForFk(),
								En);

						ens.get(En).second.add(new Eq<>(null, xl.convert(), xr.convert()));

					});

					// todo: add eqs from sks?
				} else {
					// exactly one thing, _y_
					q1.tys.get(ty.l).sks().keySet(v -> {

						Term<Ty, En, Sym, Fk, Att, String, String> xl = transT(q1, q2, isos, eq.first, En, v);

						Term<Ty, En, Sym, Fk, Att, String, String> xr = transT(q1, q2, isos, eq.second, En, v);

						ens.get(En).second.add(new Eq<>(null, xl.convert(), xr.convert()));
					});
					if (!q1.tys.get(ty.l).gens().isEmpty()) {
						Util.anomaly();
					}
				}
			}
		}

		for (Fk Fk : q2.dst.fks.keySet()) {
			Transform<Ty, En, Sym, Fk, Att, String, String, String, String, ID, Chc<String, Pair<ID, Att>>, ID, Chc<String, Pair<ID, Att>>> h = q2.fks
					.get(Fk);

			Map<String, Term<Ty, En, Sym, Fk, Att, String, String>> hh = new THashMap<>();
			Map<String, Term<Void, En, Void, Fk, Void, String, Void>> g = new THashMap<>();
			En En = q2.dst.fks.get(Fk).second;
			for (Entry<String, Chc<En, Ty>> v : ens.get(En).first.entrySet()) {
				Pair<String, String> p = isos.get(q2.dst.fks.get(Fk).second).first.get(v.getKey());
				// System.out.println(h);
				// System.out.println(p.first + " " + v.getValue());

				Term<Ty, En, Sym, Fk, Att, String, String> xl;
				if (h.src().gens().containsKey(p.first)) { // not value.left
					Term<Void, En, Void, Fk, Void, String, Void> t = h.gens().apply(p.first,
							h.src().gens().get(p.first));
					xl = trans(q1, q2, isos, p.second, t, q2.dst.fks.get(Fk).first);
				} else {
					Term<Ty, En, Sym, Fk, Att, String, String> t = h.sks().apply(p.first, h.src().sks().get(p.first));
					xl = transT(q1, q2, isos, t, q2.dst.fks.get(Fk).first, p.second);
				}

				if (v.getValue().left) {
					g.put(v.getKey(), xl.convert());
				} else {
					hh.put(v.getKey(), xl);
				}

			}
			sks.put(Fk, hh);

			fks.put(Fk, new Pair<>(g, ops));
		}
		String yy = ("_y_");
		for (Att Att : q2.dst.atts.keySet()) {
			if (!q2.atts.get(Att).left) {
				throw new RuntimeException("Cannot compose with aggregation");
			}
			Term<Ty, En, Sym, Fk, Att, String, String> h = q2.atts.get(Att).l;

			Term<Ty, En, Sym, Fk, Att, String, String> xl = transT(q1, q2, isos, h, q2.dst.atts.get(Att).first, yy);

			atts.put(Att, Chc.inLeft(xl));
		}

		if (!Util.agreeOnOverlap(q1.params, q2.params)) {
			throw new RuntimeException("Incompatible parameters: [" + q1.params + "] and [" + q2.params + "]");
		}
		Map<String, Ty> zzz = new THashMap<>(q1.params);
		for (String v : q2.params.keySet()) {
			if (!zzz.containsKey(v)) {
				zzz.put(v, q2.params.get(v));
			}
		}
		// System.out.println("ens " + ens);
		// System.out.println("atts " + atts);
		// System.out.println("fks " + fks);
		// System.out.println("sks " + sks);
		return Query.makeQuery2(zzz, Collections.emptyMap(), ens, atts, fks, sks, q1.src, q2.dst, ops);
	}

	public static synchronized <Ty, En, Sym, Fk, Att> Term<Ty, En, Sym, Fk, Att, String, String> transT(
			Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q1, Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q2,
			Map<En, Pair<Map<String, Pair<String, String>>, Map<Pair<String, String>, String>>> iso,
			Term<Ty, En, Sym, Fk, Att, String, String> t, En En, String v) {
		if (t.obj() != null) {
			return Term.Obj(t.obj(), t.ty());
		} else if (t.sym() != null) {
			List<Term<Ty, En, Sym, Fk, Att, String, String>> l = new ArrayList<>(t.args.size());
			for (Term<Ty, En, Sym, Fk, Att, String, String> arg : t.args) {
				l.add(transT(q1, q2, iso, arg, En, v));
			}
			return Term.Sym(t.sym(), l);
		} else if (t.att() != null) {
			if (!q1.atts.get(t.att()).left) {
				throw new RuntimeException("Cannot compose with aggregation");
			}
			Term<Ty, En, Sym, Fk, Att, String, String> ret = q1.atts.get(t.att()).l;
			for (String head : q1.atts.get(t.att()).l.gens()) {
				Term<Ty, En, Sym, Fk, Att, String, String> u = trans(q1, q2, iso, head, t.arg.asArgForAtt(), En);
				ret = ret.replace(Term.Gen(head), u.convert());
			}
			for (String head : q1.atts.get(t.att()).l.sks()) {
				Term<Ty, En, Sym, Fk, Att, String, String> u = trans(q1, q2, iso, head, t.arg.asArgForAtt(), En);
				ret = ret.replace(Term.Sk(head), u.convert());
			}

			return ret;
		} else if (t.sk() != null) {
			return Term.Sk(iso.get(En).second.get(new Pair<>(t.sk(), v)));
			// return Term.Sk(t.sk);
		}
		return Util.anomaly();
	}

	public static synchronized <Ty, En, Sym, Fk, Att> Term<Ty, En, Sym, Fk, Att, String, String> trans(
			Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q1, Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q2,
			Map<En, Pair<Map<String, Pair<String, String>>, Map<Pair<String, String>, String>>> iso, String p,
			Term<Void, En, Void, Fk, Void, String, Void> t, En En) {

		En Enn = q2.ens.get(En).type(t.convert()).r;
		Transform<Ty, En, Sym, Fk, Att, String, String, String, String, ID, Chc<String, Pair<ID, Att>>, ID, Chc<String, Pair<ID, Att>>> rhs;
		rhs = q1.compose(q1.transP(t.convert()), Enn);

		Function<String, String> genf = u -> {
			String lhsGen = Util.get0(t.gens());
			return iso.get(En).second.get(new Pair<>(lhsGen, u));
		};

		if (rhs.src().gens().containsKey(p)) {

			Term<Void, En, Void, Fk, Void, String, Void> z = rhs.gens().apply(p, rhs.src().gens().get(p));
			Term<Void, En, Void, Fk, Void, String, Void> xl = z.mapGen(genf);
			return xl.convert();
		} else if (rhs.src().sks().containsKey(p)) {

			Term<Ty, En, Sym, Fk, Att, String, String> z = rhs.sks().apply(p, rhs.src().sks().get(p));
			Term<Ty, En, Sym, Fk, Att, String, String> xl = z.mapGenSk(genf, genf);
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