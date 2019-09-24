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
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Eq;
import catdata.aql.Frozen;
import catdata.aql.It.ID;
import catdata.aql.Kind;
import catdata.aql.Query;
import catdata.aql.Query.Agg;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.Var;
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

	public synchronized Query<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
		AqlOptions ops = new AqlOptions(options, null, env.defaults);

		Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q1 = Q1.eval(env, isC);
		Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q2 = Q2.eval(env, isC);

		if (isC) {
			throw new IgnoreException();
		}

		Map<En, Triple<Map<Var, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>>, AqlOptions>> ens = new THashMap<>();
		Map<Att, Chc<Term<Ty, En, Sym, Fk, Att, Var, Var>,Agg<Ty, En, Sym, Fk, Att>>> atts = new THashMap<>();
		Map<Fk, Pair<Map<Var, Term<Void, En, Void, Fk, Void, Var, Void>>, AqlOptions>> fks = new THashMap<>();
		Map<Fk, Map<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>>> sks = new THashMap<>();

		Map<En, Pair<Map<Var, Pair<Var, Var>>, Map<Pair<Var, Var>, Var>>> isos = (new THashMap<>());

		for (En En : Util.alphabetical(q2.dst.ens)) {
			int i = 0;
			Pair<Map<Var, Pair<Var, Var>>, Map<Pair<Var, Var>, Var>> iso = new Pair<>(new THashMap<>(),
					new THashMap<>());

			Map<Var, Chc<En, Ty>> fr = new THashMap<>();
			Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>> wh = new LinkedList<>();
			for (Entry<Var, En> v : Util.alphabetical(q2.ens.get(En).gens.entrySet())) {
				Frozen<Ty, En, Sym, Fk, Att> I = q1.ens.get(v.getValue());
				for (Entry<Var, En> u : Util.alphabetical(I.gens.entrySet())) {
					Var newV = Var.Var("vv" + (i++));
					iso.first.put(newV, new Pair<>(v.getKey(), u.getKey()));
					iso.second.put(new Pair<>(v.getKey(), u.getKey()), newV);
					fr.put(newV, Chc.inLeft(u.getValue()));
				}
				for (Entry<Var, Ty> u : Util.alphabetical(I.sks.entrySet())) {
					Var newV = Var.Var("vv" + (i++));
					iso.first.put(newV, new Pair<>(v.getKey(), u.getKey()));
					iso.second.put(new Pair<>(v.getKey(), u.getKey()), newV);
					fr.put(newV, Chc.inRight(u.getValue()));
				}

				Function<Var, Var> genf = u -> iso.second.get(new Pair<>(v.getKey(), u));
				for (Pair<Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>, Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>> eq : I.eqs) {
					wh.add(new Eq<>(null, eq.first.mapGenSk(genf, genf), eq.second.mapGenSk(genf, genf)));
				}
			}
			for (Entry<catdata.aql.Var, Ty> v : Util.alphabetical(q2.ens.get(En).sks.entrySet())) {

				Frozen<Ty, En, Sym, Fk, Att> I = q1.tys.get(v.getValue());
				for (Entry<catdata.aql.Var, Ty> u : Util.alphabetical(I.sks.entrySet())) {
					Var newV = Var.Var("vv" + (i++));
					iso.first.put(newV, new Pair<>(v.getKey(), u.getKey()));
					iso.second.put(new Pair<>(v.getKey(), u.getKey()), newV);
					fr.put(newV, Chc.inRight(u.getValue()));
				}

			}

			for (Entry<Var, En> v : Util.alphabetical(q2.ens.get(En).gens.entrySet())) {
				Frozen<Ty, En, Sym, Fk, Att> I = q1.ens.get(v.getValue());

				Function<Var, Var> genf = u -> iso.second.get(new Pair<>(v.getKey(), u));
				for (Pair<Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>, Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>> eq : I.eqs) {
					wh.add(new Eq<>(null, eq.first.mapGenSk(genf, genf), eq.second.mapGenSk(genf, genf)));
				}
			}

			ens.put(En, new Triple<>(fr, wh, ops));
			isos.put(En, iso);
		}

		for (En En : Util.alphabetical(q2.dst.ens)) {
			for (Pair<Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>, Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>> eq : q2.ens
					.get(En).eqs) {
				Chc<Ty, En> ty = q2.ens.get(En).type(eq.first);
				if (!ty.left) {
					for (Var v : q1.ens.get(ty.r).gens().keySet()) {

						Term<Ty, En, Sym, Fk, Att, Var, Var> xl = trans(q1, q2, isos, v, eq.first.asArgForFk(), En);

						Term<Ty, En, Sym, Fk, Att, Var, Var> xr = trans(q1, q2, isos, v, eq.second.asArgForFk(), En);

						ens.get(En).second.add(new Eq<>(null, xl.convert(), xr.convert()));
					}

					for (Var v : Util.alphabetical(q1.ens.get(ty.r).sks().keySet())) {
						Term<Ty, En, Sym, Fk, Att, Var, Var> xl = trans(q1, q2, isos, v, eq.first.asArgForFk(), En);

						Term<Ty, En, Sym, Fk, Att, Var, Var> xr = trans(q1, q2, isos, v, eq.second.asArgForFk(), En);

						ens.get(En).second.add(new Eq<>(null, xl.convert(), xr.convert()));

					}

					// todo: add eqs from sks?
				} else {
					// exactly one thing, _y_
					for (Var v : q1.tys.get(ty.l).sks().keySet()) {

						Term<Ty, En, Sym, Fk, Att, Var, Var> xl = transT(q1, q2, isos, eq.first, En, v);

						Term<Ty, En, Sym, Fk, Att, Var, Var> xr = transT(q1, q2, isos, eq.second, En, v);

						ens.get(En).second.add(new Eq<>(null, xl.convert(), xr.convert()));
					}
					if (!q1.tys.get(ty.l).gens().isEmpty()) {
						Util.anomaly();
					}
				}
			}
		}

		for (Fk Fk : q2.dst.fks.keySet()) {
			Transform<Ty, En, Sym, Fk, Att, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att>>, ID, Chc<Var, Pair<ID, Att>>> h = q2.fks
					.get(Fk);

			Map<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>> hh = new THashMap<>();
			Map<Var, Term<Void, En, Void, Fk, Void, Var, Void>> g = new THashMap<>();
			En En = q2.dst.fks.get(Fk).second;
			for (Entry<catdata.aql.Var, Chc<En, Ty>> v : ens.get(En).first.entrySet()) {
				Pair<Var, Var> p = isos.get(q2.dst.fks.get(Fk).second).first.get(v.getKey());
				// System.out.println(h);
				// System.out.println(p.first + " " + v.getValue());

				Term<Ty, En, Sym, Fk, Att, Var, Var> xl;
				if (h.gens().containsKey(p.first)) { // not value.left
					Term<Void, En, Void, Fk, Void, Var, Void> t = h.gens().get(p.first);
					xl = trans(q1, q2, isos, p.second, t, q2.dst.fks.get(Fk).first);
				} else {
					Term<Ty, En, Sym, Fk, Att, Var, Var> t = h.sks().get(p.first);
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
		Var yy = Var.Var("_y_");
		for (Att Att : q2.dst.atts.keySet()) {
			if (!q2.atts.get(Att).left) {
				throw new RuntimeException("Cannot compose with aggregation");
			}
			Term<Ty, En, Sym, Fk, Att, Var, Var> h = q2.atts.get(Att).l;

			Term<Ty, En, Sym, Fk, Att, Var, Var> xl = transT(q1, q2, isos, h, q2.dst.atts.get(Att).first, yy);

			atts.put(Att, Chc.inLeft(xl));
		}

		if (!Util.agreeOnOverlap(q1.params, q2.params)) {
			throw new RuntimeException("Incompatible parameters: [" + q1.params + "] and [" + q2.params + "]");
		}
		Map<Var, Ty> zzz = new THashMap<>(q1.params);
		for (Var v : q2.params.keySet()) {
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

	public synchronized Term<Ty, En, Sym, Fk, Att, Var, Var> transT(Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q1,
			Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q2,
			Map<En, Pair<Map<Var, Pair<Var, Var>>, Map<Pair<Var, Var>, Var>>> iso,
			Term<Ty, En, Sym, Fk, Att, Var, Var> t, En En, Var v) {
		if (t.obj() != null) {
			return Term.Obj(t.obj(), t.ty());
		} else if (t.sym() != null) {
			List<Term<Ty, En, Sym, Fk, Att, Var, Var>> l = new ArrayList<>(t.args.size());
			for (Term<Ty, En, Sym, Fk, Att, Var, Var> arg : t.args) {
				l.add(transT(q1, q2, iso, arg, En, v));
			}
			return Term.Sym(t.sym(), l);
		} else if (t.att() != null) {
			if (!q1.atts.get(t.att()).left) {
				throw new RuntimeException("Cannot compose with aggregation");
			}
			Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var> ret = q1.atts.get(t.att()).l;
			for (Var head : q1.atts.get(t.att()).l.gens()) {
				Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var> u = trans(q1, q2, iso, head,
						t.arg.asArgForAtt(), En);
				ret = ret.replace(Term.Gen(head), u.convert());
			}
			for (Var head : q1.atts.get(t.att()).l.sks()) {
				Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var> u = trans(q1, q2, iso, head,
						t.arg.asArgForAtt(), En);
				ret = ret.replace(Term.Sk(head), u.convert());
			}

			return ret;
		} else if (t.sk() != null) {
			return Term.Sk(iso.get(En).second.get(new Pair<>(t.sk(), v)));
			// return Term.Sk(t.sk);
		}
		return Util.anomaly();
	}

	public synchronized Term<Ty, En, Sym, Fk, Att, Var, Var> trans(Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q1,
			Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q2,
			Map<En, Pair<Map<Var, Pair<Var, Var>>, Map<Pair<Var, Var>, Var>>> iso, Var p,
			Term<Void, En, Void, Fk, Void, Var, Void> t, En En) {

		En Enn = q2.ens.get(En).type(t.convert()).r;
		Transform<Ty, En, Sym, Fk, Att, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att>>, ID, Chc<Var, Pair<ID, Att>>> rhs;
		rhs = q1.compose(q1.transP(t.convert()), Enn);

		Function<Var, Var> genf = u -> {
			Var lhsGen = Util.get0(t.gens());
			return iso.get(En).second.get(new Pair<>(lhsGen, u));
		};
//		Function<Var, Var> skf = u -> {
//			System.out.println(t + " and " + u);
//			Var lhsGen = Util.get0(t.gens());
//			return iso.get(En).second.get(new Pair<>(lhsGen, u));
//		};

		if (rhs.gens().containsKey(p)) {

			Term<Void, En, Void, Fk, Void, Var, Void> z = rhs.gens().get(p);
			Term<Void, En, Void, Fk, Void, Var, Void> xl = z.mapGen(genf);
			return xl.convert();
		} else if (rhs.sks().containsKey(p)) {

			Term<Ty, En, Sym, Fk, Att, Var, Var> z = rhs.sks().get(p);
			Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var> xl = z.mapGenSk(genf, genf);
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