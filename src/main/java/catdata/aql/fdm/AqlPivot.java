package catdata.aql.fdm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.DP;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Mapping;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Fk;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class AqlPivot<Ty, En0, Sym, Fk0, Att0, Gen, Sk, X, Y> {

	public final Instance<Ty, En0, Sym, Fk0, Att0, Gen, Sk, X, Y> I;

	public final Schema<Ty, En, Sym, Fk, Att> intI;

	public final Mapping<Ty, En, Sym, Fk, Att, En0, Fk0, Att0> F;

	public Instance<Ty, En, Sym, Fk, Att, X, Y, X, Y> J;

	public AqlPivot(Instance<Ty, En0, Sym, Fk0, Att0, Gen, Sk, X, Y> i, AqlOptions strat) {
		I = i;

		Set<En> ens = new THashSet<>();
		Map<Att, Pair<En, Ty>> atts = new THashMap<>();
		Map<Fk, Pair<En, En>> fks = new THashMap<>();

		Map<En, En0> ens0 = new THashMap<>();
		Map<Att, Triple<Var, En0, Term<Ty, En0, Sym, Fk0, Att0, Void, Void>>> atts0 = new THashMap<>();
		Map<Fk, Pair<En0, List<Fk0>>> fks0 = new THashMap<>();

		Set<Pair<Term<Ty, En, Sym, Fk, Att, X, Y>, Term<Ty, En, Sym, Fk, Att, X, Y>>> eqs0 = new THashSet<>();
		Collage<Ty, En, Sym, Fk, Att, X, Y> col = new CCollage<>();
		
		for (Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>> x : I.algebra().talg().eqs) {
			col.eqs().add(new Eq<>(null, x.first.convert(), x.second.convert()));
		}
		col.sks().putAll(I.algebra().talg().sks);

		Map<En, Collection<X>> ensX = new THashMap<>();
		Map<Ty, Collection<Y>> tysX = new THashMap<>();
		THashMap<X, Map<Fk, X>> fksX = new THashMap<>();
		Set<Eq<Ty, Void, Sym, Void, Void, Void, Y>> eqsX = new THashSet<>();
		THashMap<X, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Y>>> attsX = new THashMap<>();
		Var vx = Var.Var("x");
		for (En0 en : I.schema().ens) {
			for (X x0 : I.algebra().en(en)) {
				En x = En.En(x0.toString());
				ens.add(x);
				ens0.put(x, en);
				col.gens().put(x0, x);
				ensX.put(x, Collections.singleton(x0));
				for (Att0 att : I.schema().attsFrom(en)) {
					Att xxx = Att.Att(x, att.toString());
					atts.put(xxx, new Pair<>(x, I.schema().atts.get(att).second));
					atts0.put(xxx, new Triple<>(vx, en, Term.Att(att, Term.Var(vx))));
					Term<Ty, En, Sym, Fk, Att, X, Y> l = Term.Att(xxx, Term.Gen(x0));
					Term<Ty, En, Sym, Fk, Att, X, Y> r = I.algebra().att(att, x0).convert();
					col.eqs().add(new Eq<>(null, l, r));
					eqs0.add(new Pair<>(l, r));

					Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Y>> ctx0 = attsX.get(x0);
					if (ctx0 == null) {
						ctx0 = new THashMap<>();
					}
					ctx0.put(xxx, I.algebra().att(att, x0));
					attsX.put(x0, ctx0);
				}
				for (Fk0 fk : I.schema().fksFrom(en)) {
					Fk xxx = Fk.Fk(x, fk.toString());
					fks.put(xxx, new Pair<>(x, En.En(I.algebra().fk(fk, x0).toString())));
					fks0.put(xxx, new Pair<>(en, Collections.singletonList(fk)));
					Term<Ty, En, Sym, Fk, Att, X, Y> l = Term.Fk(xxx, Term.Gen(x0));
					Term<Ty, En, Sym, Fk, Att, X, Y> r = Term.Gen(I.algebra().fk(fk, x0));
					col.eqs().add(new Eq<>(null, l, r));
					eqs0.add(new Pair<>(l, r));

					Map<Fk, X> ctx0 = fksX.get(x0);
					if (ctx0 == null) {
						ctx0 = new THashMap<>();
					}
					ctx0.put(xxx, I.algebra().fk(fk, x0));
					fksX.put(x0, ctx0);
				}
			}
		}

		for (Ty ty : I.schema().typeSide.tys) {
			tysX.put(ty, new THashSet<>());
		}
		for (Y y : I.algebra().talg().sks.keySet()) {
			Term<Ty, En, Sym, Fk, Att, X, Y> l = Term.Sk(y);
			Term<Ty, En, Sym, Fk, Att, X, Y> r = foo(I.reprT(Term.Sk(y)));

			col.eqs().add(new Eq<>(null, l, r));
			eqs0.add(new Pair<>(l, r));
			Ty ty = I.algebra().talg().sks.get(y);
			Collection<Y> ctx0 = tysX.get(ty);
			ctx0.add(y);
			tysX.put(ty, ctx0);
		}
		for (Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>> x : I.algebra().talg().eqs) {
			eqsX.add(new Eq<>(null, x.first, x.second));
		}
		DP<Ty, En, Sym, Fk, Att, Void, Void> dp1 = new DP<>() {

			@Override
			public String toStringProver() {
				return "Pivot prover (sch)";
			}

			@Override
			public boolean eq(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Void, Void> lhs,
					Term<Ty, En, Sym, Fk, Att, Void, Void> rhs) {
				Var v = Util.get0(ctx.keySet());
				En en = ctx.get(v).r;

				return I.dp().eq(null, trans(en, lhs), trans(en, rhs));
			}

		};

		intI = new Schema<>(I.schema().typeSide, ens, atts, fks, Collections.emptySet(), dp1, I.allowUnsafeJava());

		F = new Mapping<>(ens0, atts0, fks0, intI, I.schema(), false);

		col.addAll(intI.collage());

		Algebra<Ty, En, Sym, Fk, Att, X, Y, X, Y> initial = new ImportAlgebra<>(intI, ensX, tysX, fksX, attsX,
				(x, y) -> y, (x, y) -> y, (Boolean) strat.getOrDefault(AqlOption.allow_java_eqs_unsafe), eqsX);

		DP<Ty, En, Sym, Fk, Att, X, Y> dp2 = new DP<>() {

			@Override
			public String toStringProver() {
				return "Pivot prover (inst)";
			}

			@Override
			public boolean eq(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, X, Y> lhs,
					Term<Ty, En, Sym, Fk, Att, X, Y> rhs) {
				if (ctx != null && !ctx.isEmpty()) {
					return Util.anomaly();
				} else if (lhs.hasTypeType()) {
					Term<Ty, Void, Sym, Void, Void, Void, Y> y1 = initial.intoY(lhs);
					Term<Ty, Void, Sym, Void, Void, Void, Y> y2 = initial.intoY(rhs);
					return I.dp().eq(null, I.reprT(y1), I.reprT(y2));
				} else {
					return initial.intoX(lhs).equals(initial.intoX(rhs));
				}
			}

		};

		J = new LiteralInstance<>(intI, col.gens(), col.sks(), eqs0, dp2, initial,
				(Boolean) strat.getOrDefault(AqlOption.require_consistency),
				(Boolean) strat.getOrDefault(AqlOption.allow_java_eqs_unsafe));

		J.validate();
	}

	@SuppressWarnings("unchecked")
	private Term<Void, En0, Void, Fk0, Void, Gen, Void> trans1(En en, Term<Void, En, Void, Fk, Void, Void, Void> t) {
		if (t.gen() != null) {
			return Util.abort(t.gen());
		} else if (t.var != null) {
			return I.algebra().repr((En0) en, (X) en.str);
		} else if (t.fk() != null) {
			Term<Void, En0, Void, Fk0, Void, Gen, Void> x = trans1(en, t.arg);
			return Term.Fk((Fk0) Fk.Fk(t.fk().en, t.fk().str), x); // TODO check
		}
		return Util.anomaly();
	}

	@SuppressWarnings("unchecked")
	private Term<Ty, En0, Sym, Fk0, Att0, Gen, Sk> trans(En en, Term<Ty, En, Sym, Fk, Att, Void, Void> t) {
		if (t.obj() != null) {
			return Term.Obj(t.obj(), t.ty());
		} else if (t.sym() != null) {
			List<Term<Ty, En0, Sym, Fk0, Att0, Gen, Sk>> l = new LinkedList<>();
			for (Term<Ty, En, Sym, Fk, Att, Void, Void> s : t.args) {
				l.add(trans(en, s));
			}
			return Term.Sym(t.sym(), l);
		} else if (t.sk() != null) {
			return Util.abort(t.sk());
		} else if (t.att() != null) {
			Term<Void, En0, Void, Fk0, Void, Gen, Void> x = trans1(en, t.arg.asArgForAtt());
			return Term.Att((Att0) Att.Att(t.att().en, t.att().str), x.map(Util::abort, Util::abort,
					Function.identity(), Util::abort, Function.identity(), Util::abort));
		}
		return Util.anomaly();
	}

	private X bar(Term<Void, En0, Void, Fk0, Void, Gen, Void> t) {
		if (t.gen() != null) {
			return I.algebra().gen(t.gen());
		} else if (t.fk() != null) {
			X x = bar(t.arg);
			return I.algebra().fk(t.fk(), x);
		}
		return Util.anomaly();
	}

	private Term<Ty, En, Sym, Fk, Att, X, Y> foo(Term<Ty, En0, Sym, Fk0, Att0, Gen, Sk> t) {
		if (t.obj() != null) {
			return Term.Obj(t.obj(), t.ty());
		} else if (t.sym() != null) {
			List<Term<Ty, En, Sym, Fk, Att, X, Y>> l = new LinkedList<>();
			for (Term<Ty, En0, Sym, Fk0, Att0, Gen, Sk> s : t.args) {
				l.add(foo(s));
			}
			return Term.Sym(t.sym(), l);
		} else if (t.sk() != null) {
			return I.algebra().sk(t.sk()).convert();
		} else if (t.att() != null) {
			X x = bar(t.arg.convert());
			return Term.Att(Att.Att(En.En(x.toString()), t.att().toString()), Term.Gen(x));
		}
		return Util.anomaly();
	}

}
