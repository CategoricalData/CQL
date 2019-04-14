package catdata.aql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> implements Semantics {

	private Schema<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>> collage;

	public synchronized Schema<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>> collage() {
		if (collage != null) {
			return collage;
		}
		DP<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> dp = new DP<>() {
			@Override
			public String toStringProver() {
				return Util.anomaly();
			}

			@Override
			public boolean eq(Map<Var, Chc<Ty, Chc<En1, En2>>> ctx,
					Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> lhs,
					Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> rhs) {
				return Util.anomaly();
			}

		};

		Set<Chc<En1, En2>> ens2 = Chc.or(src.ens, dst.ens);
		Map<Chc<Att1, Att2>, Pair<Chc<En1, En2>, Ty>> atts2 = or(src.atts, dst.atts); // TODO aql these don't need to be
																						// passed as params
		Map<Chc<Chc<Fk1, Fk2>, En1>, Pair<Chc<En1, En2>, Chc<En1, En2>>> fks2 = or2(src.fks, dst.fks);
		Set<Triple<Pair<Var, Chc<En1, En2>>, Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void>, Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void>>> eqs2 = new THashSet<>();
		for (Triple<Pair<Var, En1>, Term<Ty, En1, Sym, Fk1, Att1, Void, Void>, Term<Ty, En1, Sym, Fk1, Att1, Void, Void>> eq : src.eqs) {
			Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> t1 = eq.second
					.<Chc<En1, En2>>mapEn().mapFk(x -> Chc.<Chc<Fk1, Fk2>, En1>inLeft(Chc.inLeft(x)))
					.mapAtt(x -> Chc.inLeft(x));
			Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> t2 = eq.third
					.<Chc<En1, En2>>mapEn().mapFk(x -> Chc.<Chc<Fk1, Fk2>, En1>inLeft(Chc.inLeft(x)))
					.mapAtt(x -> Chc.inLeft(x));
			eqs2.add(new Triple<>(new Pair<>(eq.first.first, Chc.inLeft(eq.first.second)), t1, t2));
		}
		for (Triple<Pair<Var, En2>, Term<Ty, En2, Sym, Fk2, Att2, Void, Void>, Term<Ty, En2, Sym, Fk2, Att2, Void, Void>> eq : dst.eqs) {
			Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> t1 = eq.second
					.<Chc<En1, En2>>mapEn().mapFk(x -> Chc.<Chc<Fk1, Fk2>, En1>inLeft(Chc.inRight(x)))
					.mapAtt(x -> Chc.inRight(x));
			Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> t2 = eq.third
					.<Chc<En1, En2>>mapEn().mapFk(x -> Chc.<Chc<Fk1, Fk2>, En1>inLeft(Chc.inRight(x)))
					.mapAtt(x -> Chc.inRight(x));

			eqs2.add(new Triple<>(new Pair<>(eq.first.first, Chc.inRight(eq.first.second)), t1, t2));
		}
		Var xxx = Var.Var("x");
		for (Fk1 a : src.fks.keySet()) {
			En1 v = src.fks.get(a).first;
			En1 w = src.fks.get(a).second;
			// a.m_w = m_v.F(a)

			Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> lhs = Term
					.Fk(Chc.inRight(w), Term.Fk(Chc.inLeft(Chc.inLeft(a)), Term.Var(xxx)));

			Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> rhs = Term
					.Fks(Chc.inLeft(Chc.inRight(fks.get(a).second)), Term.Fk(Chc.inRight(v), Term.Var(xxx)));

			eqs2.add(new Triple<>(new Pair<>(xxx, Chc.inLeft(v)), lhs, rhs));
		}
		for (Att1 a : src.atts.keySet()) {
			En1 v = src.atts.get(a).first;
			@SuppressWarnings("unused")
			Ty w = src.atts.get(a).second;
			// a = m_v.F(a)
			Var x = atts.get(a).first;
			Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> lhs = Term
					.Att(Chc.inLeft(a), Term.Var(x));

			@SuppressWarnings("unused")
			En2 en2 = atts.get(a).second;
			Term<Ty, Chc<En1, En2>, Sym, Fk2, Att2, Void, Void> l = atts.get(a).third.mapEn();
			Function<Fk2, Chc<Chc<Fk1, Fk2>, En1>> f = xx -> Chc.inLeft(Chc.inRight(xx));
			Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> term = l.mapFk(f)
					.mapAtt(xx -> Chc.inRight(xx));

			Term<Ty, Chc<En1, En2>, Sym, Chc<Chc<Fk1, Fk2>, En1>, Chc<Att1, Att2>, Void, Void> rhs = term
					.subst(Collections.singletonMap(x, Term.Fk(Chc.inRight(v), Term.Var(x))));

			eqs2.add(new Triple<>(new Pair<>(x, Chc.inLeft(v)), lhs, rhs));
		}

		collage = new Schema<>(src.typeSide, ens2, atts2, fks2, eqs2, dp, false);
		return collage;
	}

	private Map<Chc<Att1, Att2>, Pair<Chc<En1, En2>, Ty>> or(Map<Att1, Pair<En1, Ty>> xs, Map<Att2, Pair<En2, Ty>> ys) {
		Map<Chc<Att1, Att2>, Pair<Chc<En1, En2>, Ty>> ret = new THashMap<>(xs.size() + ys.size());
		for (Att1 att : xs.keySet()) {
			ret.put(Chc.inLeft(att), new Pair<>(Chc.inLeft(xs.get(att).first), xs.get(att).second));
		}
		for (Att2 att : ys.keySet()) {
			ret.put(Chc.inRight(att), new Pair<>(Chc.inRight(ys.get(att).first), ys.get(att).second));
		}
		return ret;
	}

	@SuppressWarnings("hiding")
	private <Att1, Att2> Map<Chc<Chc<Att1, Att2>, En1>, Pair<Chc<En1, En2>, Chc<En1, En2>>> or2(
			Map<Att1, Pair<En1, En1>> xs, Map<Att2, Pair<En2, En2>> ys) {
		Map<Chc<Chc<Att1, Att2>, En1>, Pair<Chc<En1, En2>, Chc<En1, En2>>> ret = new THashMap<>(
				xs.size() + ys.size() + src.ens.size());
		for (Att1 att : xs.keySet()) {
			ret.put(Chc.inLeft(Chc.inLeft(att)),
					new Pair<>(Chc.inLeft(xs.get(att).first), Chc.inLeft(xs.get(att).second)));
		}
		for (Att2 att : ys.keySet()) {
			ret.put(Chc.inLeft(Chc.inRight(att)),
					new Pair<>(Chc.inRight(ys.get(att).first), Chc.inRight(ys.get(att).second)));
		}
		for (En1 en1 : src.ens) {
			ret.put(Chc.inRight(en1), new Pair<>(Chc.inLeft(en1), Chc.inRight(ens.get(en1))));
		}
		return ret;
	}

	@Override
	public int size() {
		return src.size();
	}

	public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2, En3, Fk3, Att3> Mapping<Ty, En1, Sym, Fk1, Att1, En3, Fk3, Att3> compose(
			Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> m1, Mapping<Ty, En2, Sym, Fk2, Att2, En3, Fk3, Att3> m2) {
		if (!m1.dst.equals(m2.src)) {
			throw new RuntimeException("Anomaly, please report.\n\n" + m1.dst + "\n\n" + m2.src);
		}
		Map<En1, En3> ens0 = new THashMap<>(m1.ens.size());
		for (En1 en1 : m1.ens.keySet()) {
			ens0.put(en1, m2.ens.get(m1.ens.get(en1)));
		}
		Map<Fk1, Pair<En3, List<Fk3>>> fks0 = new THashMap<>(m1.fks.size());
		for (Fk1 fk1 : m1.fks.keySet()) {
			En3 en3 = m2.ens.get(m1.fks.get(fk1).first);
			List<Fk3> l = new LinkedList<>();
			for (Fk2 fk2 : m1.fks.get(fk1).second) {
				l.addAll(m2.fks.get(fk2).second);
			}
			fks0.put(fk1, new Pair<>(en3, l));
		}
		Map<Att1, Triple<Var, En3, Term<Ty, En3, Sym, Fk3, Att3, Void, Void>>> atts0 = new THashMap<>(m1.atts.size());
		for (Att1 att1 : m1.atts.keySet()) {
			En3 en3 = m2.ens.get(m1.atts.get(att1).second);
			Var v = m1.atts.get(att1).first;
			Term<Ty, En3, Sym, Fk3, Att3, Void, Void> t = subst(m1.atts.get(att1).third, m2);
			atts0.put(att1, new Triple<>(v, en3, t));
		}

		return new Mapping<>(ens0, atts0, fks0, m1.src, m2.dst, true);
	}

	private static <Ty, En2, Sym, Fk2, Att2, En3, Fk3, Att3> Term<Ty, En3, Sym, Fk3, Att3, Void, Void> subst(
			Term<Ty, En2, Sym, Fk2, Att2, Void, Void> t, Mapping<Ty, En2, Sym, Fk2, Att2, En3, Fk3, Att3> m2) {
		if (t.var != null) {
			return Term.Var(t.var);
		} else if (t.gen() != null) {
			return Util.abort(t.gen());
		} else if (t.sk() != null) {
			return Util.abort(t.sk());
		} else if (t.obj() != null) {
			return Term.Obj(t.obj(), t.ty());
		} else if (t.sym() != null) {
			List<Term<Ty, En3, Sym, Fk3, Att3, Void, Void>> l = new ArrayList<>(t.args.size());
			for (Term<Ty, En2, Sym, Fk2, Att2, Void, Void> x : t.args) {
				l.add(subst(x, m2));
			}
			return Term.Sym(t.sym(), l);
		} else if (t.fk() != null) {
			return Term.Fks(m2.fks.get(t.fk()).second, subst(t.arg, m2));
		} else if (t.att() != null) {
			Triple<Var, En3, Term<Ty, En3, Sym, Fk3, Att3, Void, Void>> x = m2.atts.get(t.att());
			return x.third.subst(Collections.singletonMap(x.first, subst(t.arg, m2)));
		}
		return Util.anomaly();
	}

	@Override
	public Kind kind() {
		return Kind.MAPPING;
	}

	private Morphism<Ty, En1, Sym, Fk1, Att1, Void, Void, En2, Sym, Fk2, Att2, Void, Void> semantics;

	public Morphism<Ty, En1, Sym, Fk1, Att1, Void, Void, En2, Sym, Fk2, Att2, Void, Void> semantics() {
		if (semantics != null) {
			return semantics;
		}

		semantics = new Morphism<>() {

			@Override
			public Collage<Ty, En1, Sym, Fk1, Att1, Void, Void> src() {
				return src.collage();
			}

			@Override
			public Collage<Ty, En2, Sym, Fk2, Att2, Void, Void> dst() {
				return dst.collage();
			}

			@Override
			public Pair<Map<Var, Chc<Ty, En2>>, Term<Ty, En2, Sym, Fk2, Att2, Void, Void>> translate(
					Map<Var, Chc<Ty, En1>> ctx, Term<Ty, En1, Sym, Fk1, Att1, Void, Void> term) {
				Map<Var, Chc<Ty, En2>> map = new THashMap<>(ctx.size());
				for (Var var : ctx.keySet()) {
					if (ctx.get(var).left) {
						map.put(var, Chc.inLeft(ctx.get(var).l));
					} else {
						map.put(var, Chc.inRight(ens.get(ctx.get(var).r)));
					}
				}
				Map<Var, Chc<Ty, En2>> ret = map;
				return new Pair<>(ret, trans(term));
			}

		};
		return semantics;
	}

	public Map<Var, Chc<Ty, En2>> trans(Map<Var, Chc<Ty, En1>> ctx) {
		Map<Var, Chc<Ty, En2>> ret = new THashMap<>(ctx.size());
		for (Var v : ctx.keySet()) {
			Chc<Ty, En1> c = ctx.get(v);
			if (c.left) {
				ret.put(v, Chc.inLeft(c.l));
			} else {
				ret.put(v, Chc.inRight(ens.get(c.r)));
			}
		}
		return ret;
	}

	public List<Fk2> trans(List<Fk1> fks1) {
		List<Fk2> ret = new LinkedList<>();
		for (Fk1 fk1 : fks1) {
			ret.addAll(fks.get(fk1).second);
		}
		return ret;
	}

	public synchronized <Gen, Sk> Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk> trans(
			Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk> term) {
		if (term.var != null) {
			return term.convert();
		} else if (term.obj() != null) {
			return term.convert();
		} else if (term.gen() != null) {
			return Term.Gen(term.gen());
		} else if (term.sk() != null) {
			return Term.Sk(term.sk());
		} else if (term.fk() != null) {
			Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk> ret = trans(term.arg);
			for (Fk2 fk : fks.get(term.fk()).second) {
				ret = Term.Fk(fk, ret);
			}
			return ret;
		} else if (term.att() != null) {
			Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk> ret = trans(term.arg);
			Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk> conv = atts.get(term.att()).third.convert();
			return conv.subst(Collections.singletonMap(atts.get(term.att()).first, ret));
		} else if (term.sym() != null) {
			List<Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>> l = new ArrayList<>(term.args.size());
			for (Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk> x : term.args) {
				l.add(trans(x));
			}
			return Term.Sym(term.sym(), l);
		}
		return Util.anomaly();
	}

	public final Map<En1, En2> ens;
	public final Map<Att1, Triple<Var, En2, Term<Ty, En2, Sym, Fk2, Att2, Void, Void>>> atts;

	public final Map<Fk1, Pair<En2, List<Fk2>>> fks;

	public final Schema<Ty, En1, Sym, Fk1, Att1> src;
	public final Schema<Ty, En2, Sym, Fk2, Att2> dst;

	public static <Ty, En, Sym, Fk, Att> Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> id(Schema<Ty, En, Sym, Fk, Att> s) {
		return id(s, Optional.of(s));
	}

	public static <Ty, En, Sym, Fk, Att> Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> id(Schema<Ty, En, Sym, Fk, Att> s,
			Optional<Schema<Ty, En, Sym, Fk, Att>> t) {
		Map<En, En> ens = Util.id(s.ens);
		Map<Fk, Pair<En, List<Fk>>> fks = Util.mk();
		for (Fk fk : s.fks.keySet()) {
			fks.put(fk, new Pair<>(s.fks.get(fk).first, Collections.singletonList(fk)));
		}
		Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> atts = Util.mk();
		Var v = Var.Var("v");
		for (Att att : s.atts.keySet()) {
			atts.put(att, new Triple<>(v, s.atts.get(att).first, Term.Att(att, Term.Var(v))));
		}
		if (t.isEmpty()) {
			t = Optional.of(s);
		}
		return new Mapping<>(ens, atts, fks, s, t.get(), t.isEmpty());
	}

	public Mapping(Map<En1, En2> ens, Map<Att1, Triple<Var, En2, Term<Ty, En2, Sym, Fk2, Att2, Void, Void>>> atts,
			Map<Fk1, Pair<En2, List<Fk2>>> fks, Schema<Ty, En1, Sym, Fk1, Att1> src,
			Schema<Ty, En2, Sym, Fk2, Att2> dst, boolean doNotCheckEquations) {
		this.ens = ens;
		this.atts = atts;
		this.fks = fks;
		this.src = src;
		this.dst = dst;
		validate(doNotCheckEquations);
		// semantics();
		// collage();
	}

	public void validate(boolean doNotCheckEquations) {
		// for each (k,v) in ens/atts/fks, k must be in src and dst must be in target
		for (En1 en1 : src.ens) {
			En2 en2 = ens.get(en1);
			if (en2 == null) {
				throw new RuntimeException("source entity " + en1 + " has no mapping");
			}
			if (!dst.ens.contains(en2)) {
				throw new RuntimeException("source entity " + en1 + " maps to " + en2 + ", which is not in target");
			}
		}
		for (Att1 att1 : src.atts.keySet()) {
			Triple<Var, En2, Term<Ty, En2, Sym, Fk2, Att2, Void, Void>> att2 = atts.get(att1);
			if (att2 == null) {
				throw new RuntimeException("source attribute " + att1 + " has no mapping");
			}
			Var v = att2.first;
			En2 proposed_en = att2.second;
			if (proposed_en == null) {
				throw new RuntimeException("in mapping for attribute " + att1 + ", not given a sort for " + v);
			}
			Term<Ty, En2, Sym, Fk2, Att2, ?, ?> term = att2.third;

			En1 en1 = src.atts.get(att1).first;
			Ty ty1 = src.atts.get(att1).second;
			En2 en2 = ens.get(en1);
			if (!proposed_en.equals(en2)) {
				throw new RuntimeException("in mapping for attribute " + att1 + " the given sort for " + v + " is "
						+ proposed_en + " but it is expected to be " + en2);
			}
			Chc<Ty, En2> ty2 = dst.type(new Pair<>(v, en2), term);
			if (!ty2.equals(Chc.inLeft(ty1))) {
				throw new RuntimeException("source attribute " + att1 + " goes to target observation " + att2
						+ ", which has type " + ty2.toStringMash() + ", not " + ty1 + " as expected");
			}
		}
		Var v = Var.Var("v");

		for (Fk1 fk1 : src.fks.keySet()) {
			Pair<En2, List<Fk2>> p = fks.get(fk1);
			if (p == null) {
				throw new RuntimeException("source foreign key " + fk1 + " : " + src.fks.get(fk1).first + " -> "
						+ src.fks.get(fk1).second + " has no mapping");
			}
			En1 en1_s = src.fks.get(fk1).first;
			En1 en1_t = src.fks.get(fk1).second;
			En2 en2_s = ens.get(en1_s);
			En2 en2_t = ens.get(en1_t);
			if (!p.first.equals(en2_s)) {
				throw new RuntimeException("proposed source of foreign key mapping for " + fk1 + " is " + p.first
						+ " and not " + en2_s + " as expected");
			}
			Term<Ty, En2, Sym, Fk2, Att2, ?, ?> fk2 = Term.Fks(p.second, Term.Var(v));
			Chc<Ty, En2> en2_t_actual = dst.type(new Pair<>(v, en2_s), fk2);
			if (!en2_t_actual.equals(Chc.inRight(en2_t))) {
				throw new RuntimeException("source foreign key " + fk1 + " maps to target path "
						+ Util.sep(p.second, ".") + ", which has target entity " + en2_t_actual.toStringMash()
						+ ", not " + en2_t + " as expected");
			}
		}
		for (En1 en1 : ens.keySet()) {
			if (!src.ens.contains(en1)) {
				throw new RuntimeException("there is a mapping for " + en1 + " which is not a source entity");
			}
		}
		for (Att1 att1 : atts.keySet()) {
			if (!src.atts.containsKey(att1)) {
				throw new RuntimeException("there is a mapping for " + att1 + " which is not a source attribute");
			}
		}
		for (Fk1 fk1 : fks.keySet()) {
			if (!src.fks.containsKey(fk1)) {
				throw new RuntimeException("there is a mapping for " + fk1 + " which is not a source foreign key");
			}
		}

		if (!doNotCheckEquations) {
			for (Triple<Pair<Var, En1>, Term<Ty, En1, Sym, Fk1, Att1, Void, Void>, Term<Ty, En1, Sym, Fk1, Att1, Void, Void>> eq : src.eqs) {
				Term<Ty, En2, Sym, Fk2, Att2, Void, Void> lhs = trans(eq.second), rhs = trans(eq.third);
				boolean ok = dst.dp.eq(Collections.singletonMap(eq.first.first, Chc.inRight(ens.get(eq.first.second))),
						lhs, rhs);
				if (!ok) {
					throw new RuntimeException("Source schema equation " + eq.second + " = " + eq.third + " translates to " + lhs
							+ " = " + rhs + ", which is not provable in the target schema.  To proceed, consider removing it from the source or adding more equations to the target.");
				}
			}
		}

	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((atts == null) ? 0 : atts.hashCode());
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((ens == null) ? 0 : ens.hashCode());
		result = prime * result + ((fks == null) ? 0 : fks.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
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
		Mapping<?, ?, ?, ?, ?, ?, ?, ?> other = (Mapping<?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (atts == null) {
			if (other.atts != null)
				return false;
		} else if (!atts.equals(other.atts))
			return false;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		if (ens == null) {
			if (other.ens != null)
				return false;
		} else if (!ens.equals(other.ens))
			return false;
		if (fks == null) {
			if (other.fks != null)
				return false;
		} else if (!fks.equals(other.fks))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		return true;
	}

	private String toString = null;

	@Override
	public synchronized String toString() {
		if (toString != null) {
			return toString;
		}

		toString = "";
		for (En1 en : src.ens) {
			toString += "\n\nentity";
			toString += "\n\t" + en + " -> " + ens.get(en);

			List<String> fks0 = new LinkedList<>();
			for (Fk1 fk : src.fksFrom(en)) {
				fks0.add(fk + " -> " + fks.get(fk).first
						+ (fks.get(fk).second.isEmpty() ? "" : "." + Util.sep(fks.get(fk).second, ".")));
			}
			List<String> atts0 = new LinkedList<>();
			for (Att1 att : src.attsFrom(en)) {
				atts0.add(att + " -> lambda " + atts.get(att).first + ":" + atts.get(att).second + ". "
						+ atts.get(att).third);
			}

			if (!fks0.isEmpty()) {
				toString += "\nforeign_keys";
				toString += "\n\t" + Util.sep(fks0, "\n\t");
			}
			if (!atts0.isEmpty()) {
				toString += "\nattributes";
				toString += "\n\t" + Util.sep(atts0, "\n\t");
			}
		}

		toString = toString.trim();
		return toString;
	}

}
