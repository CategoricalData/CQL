package catdata.aql.fdm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterators;

import catdata.BinRelSet;
import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.Collage;
import catdata.aql.DP;
import catdata.aql.Instance;
import catdata.aql.Mapping;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class SigmaLeftKanAlgebra<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2, Gen, Sk, X, Y>
		extends Algebra<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>>
		implements DP<Ty, En2, Sym, Fk2, Att2, Gen, Sk> {

	public boolean hasFreeTypeAlgebra() {
		return talg().eqs.isEmpty();
	}
	
	
	@Override
	public boolean hasNulls() {
		return talg().sks.isEmpty();
	}

	public boolean hasFreeTypeAlgebraOnJava() {
		return talg().eqs.stream().filter(x -> schema().typeSide.js.java_tys.containsKey(talg().type(x.first)))
				.collect(Collectors.toList()).isEmpty();
	}

	private static class Path<Ty, En, Sym, Fk, Att> {
		public final En src;
		public final En dst;
		public final List<Fk> edges;

		public Path(Schema<Ty, En, Sym, Fk, Att> sch, En src, List<Fk> edges) {
			this.src = src;
			this.edges = edges;
			if (edges.isEmpty()) {
				dst = src;
			} else {
				dst = sch.fks.get(edges.get(edges.size() - 1)).second;
			}
		}

		public Path(Schema<Ty, En, Sym, Fk, Att> sch, Term<Ty, En, Sym, Fk, Att, Void, Void> t, En src) {
			this(sch, src, t.toFkList());
		}
	}

	private final Schema<Ty, En1, Sym, Fk1, Att1> A;
	private final Schema<Ty, En2, Sym, Fk2, Att2> B;
	public final Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> F;
	private final Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> X;
	public int fresh;

	private boolean gamma() {
		boolean ret = false;

		while (true) {
			Pair<En2, Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>>> k = gamma0();
			if (k == null) {
				return ret;
			}
			ret = true;
			gamma1(k.first, k.second);
		}
	}

	private static <En2, Fk2, Gen> void filter(
			BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> set,
			Term<Void, En2, Void, Fk2, Void, Gen, Void> d) {
		set.removeIf(p -> p.first.equals(d) || p.second.equals(d));
	}

	private void gamma1(En2 b1,
			Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> xy) {
		if (xy.first.equals(xy.second)) {
			Sb.get(b1).remove(xy.first, xy.second);
			return;
		}
		Term<Void, En2, Void, Fk2, Void, Gen, Void> x, y;
		if (rank.get(xy.first) > rank.get(xy.second)) {
			x = xy.second;
			y = xy.first;
		} else {
			x = xy.first;
			y = xy.second;
		}

		Pb.get(b1).remove(y);

		replace(x, y);

		BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> set0 = new BinRelSet<>(
				(new THashSet<>(Sb.get(b1).R)));

		for (Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> k : Sb
				.get(b1)) {
			if (k.first.equals(y)) {
				set0.add(x, k.second);
			}
			if (k.second.equals(y)) {
				set0.add(k.first, x);
			}
		}
		filter(set0, y);
		Sb.put(b1, set0);

		for (Fk2 g : Pg.keySet()) {
			BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> set = Pg
					.get(g);
			BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> a = new BinRelSet<>(
					(new THashSet<>()));
			En2 gs = schema().fks.get(g).first;
			En2 gt = schema().fks.get(g).second;

			if (gs.equals(b1) && gt.equals(b1)) {
				for (Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> k : set) {
					if (k.first.equals(y) && k.second.equals(y)) {
						a.add(x, x);
					}
					if (k.first.equals(y) && !k.second.equals(y)) {
						a.add(x, k.second);
					}
					if (k.second.equals(y) && !k.first.equals(y)) {
						a.add(k.first, x);
					}
				}
			} else if (gs.equals(b1)) {
				for (Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> k : set) {
					if (k.first.equals(y) && !k.second.equals(y)) {
						a.add(x, k.second);
					}
				}
			} else if (gt.equals(b1)) {
				for (Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> k : set) {
					if (k.second.equals(y) && !k.first.equals(y)) {
						a.add(k.first, x);
					}
				}
			}
			set.addAll(a);
			filter(set, y);
		}
	}

	private void replace(Term<Void, En2, Void, Fk2, Void, Gen, Void> x, Term<Void, En2, Void, Fk2, Void, Gen, Void> y) {

		for (List<Pair<X, Term<Void, En2, Void, Fk2, Void, Gen, Void>>> a : ua.values()) {
			for (Pair<X, Term<Void, En2, Void, Fk2, Void, Gen, Void>> s : a) {
				if (s.second.equals(y)) {
					s.setSecond(x);
				}
			}
		}
	}

	private Pair<En2, Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>>> gamma0() {
		for (Entry<En2, BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>>> c : Sb
				.entrySet()) {
			if (c.getValue().isEmpty()) {
				continue;
			}
			for (Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> p0 : c
					.getValue()) {
				return new Pair<>(c.getKey(), p0);
			}
		}

		return null;
	}

	private boolean bgd() {
		return gamma() || delta() || beta1() || beta2();
	}

	private boolean step() {
		boolean ret = false;
		while (bgd()) {
			ret = true;
		}
		return ret || alpha();
	}

	// true = success
	public boolean compute() {
		while (!step())
			;
		return true;
	}

	// beta, delta, gamma
	private boolean beta2() {
		boolean ret = false;
		for (Fk1 e : A.fks.keySet()) {
			Path<Ty, En2, Sym, Fk2, Att2> g = new Path<>(F.dst, F.fks.get(e).first, F.fks.get(e).second);
			BinRelSet<X, Term<Void, En2, Void, Fk2, Void, Gen, Void>> lhs = new BinRelSet<>(X.algebra().fkAsSet(e))
					.compose(new BinRelSet<>((new THashSet<>(ua.get(A.fks.get(e).second)))));
			BinRelSet<X, Term<Void, En2, Void, Fk2, Void, Gen, Void>> rhs = new BinRelSet<>(
					(new THashSet<>(ua.get(A.fks.get(e).first)))).compose(eval2(g));

			En2 n = g.dst;
			ret = ret || addCoincidences(lhs, rhs, n);
		}
		return ret;
	}

	private boolean beta1() {
		boolean ret = false;
		for (Triple<Pair<Var, En2>, Term<Ty, En2, Sym, Fk2, Att2, Void, Void>, Term<Ty, En2, Sym, Fk2, Att2, Void, Void>> eq : B.eqs) {
			Chc<Ty, En2> nn = schema().type(eq.first, eq.second);
			if (nn.left) {
				// defer to consistency check
				continue;
			}

			BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> lhs = eval2(
					new Path<>(B, eq.second, eq.first.second));
			BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> rhs = eval2(
					new Path<>(B, eq.third, eq.first.second));

			En2 n = nn.r;
			ret = ret || addCoincidences(lhs, rhs, n);
		}
		return ret;
	}

	private <Z> boolean addCoincidences(BinRelSet<Z, Term<Void, En2, Void, Fk2, Void, Gen, Void>> lhs,
			BinRelSet<Z, Term<Void, En2, Void, Fk2, Void, Gen, Void>> rhs, En2 n) {
		boolean ret = false;
		for (Pair<Z, Term<Void, En2, Void, Fk2, Void, Gen, Void>> l : lhs) {
			for (Pair<Z, Term<Void, En2, Void, Fk2, Void, Gen, Void>> r : rhs) {
				if (!l.first.equals(r.first)) {
					continue;
				}
				if (l.second.equals(r.second)) {
					continue;
				}
				ret = Sb.get(n).add(l.second, r.second) || ret;
				ret = Sb.get(n).add(r.second, l.second) || ret;
			}
		}
		return ret;
	}

	public BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> eval2(
			Path<Ty, En2, Sym, Fk2, Att2> p) {
		BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> ret = new BinRelSet<>(
				Util.refl(Pb.get(p.src)));
		for (Fk2 e : p.edges) {
			ret = ret.compose(Pg.get(e));
		}
		return ret;
	}

	// private final It fr = new It();
	private int fresh() {
		return fresh++;
	}

	private Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Fk2> smallest() {
		Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Fk2> ret = null;
		for (Fk2 g : Pg.keySet()) {
			BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> pg = Pg
					.get(g);
			outer: for (Term<Void, En2, Void, Fk2, Void, Gen, Void> x : Pb.get(schema().fks.get(g).first)) {
				for (Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> p : pg) {
					if (p.first.equals(x)) {
						continue outer;
					}
				}
				if (ret == null || rank.get(x) < rank.get(ret.first)) {
					ret = new Pair<>(x, g);
				}
			}
		}
		return ret;
	}

	private boolean alpha() {
		Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Fk2> p = smallest();
		if (p == null) {
			return false;
		}
		Term<Void, En2, Void, Fk2, Void, Gen, Void> x = p.first;

		Fk2 g = p.second;
		En2 b2 = schema().fks.get(g).second;
		Term<Void, En2, Void, Fk2, Void, Gen, Void> y = Term.Fk(p.second, x);
		int z = fresh();
		rank.put(y, z);
		rankInv.add(y);

		Pb.get(b2).add(y);
		Pg.get(g).add(x, y);

		return true;
	}

	private boolean delta() {
		boolean ret = false;
		for (Fk2 g : B.fks.keySet()) {
			for (Term<Void, En2, Void, Fk2, Void, Gen, Void> x : Pb.get(B.fks.get(g).first)) {
				Term<Void, En2, Void, Fk2, Void, Gen, Void> y = null;
				Iterator<Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>>> it = Pg
						.get(g).iterator();
				while (it.hasNext()) {
					Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> z = it
							.next();
					if (!x.equals(z.first)) {
						continue;
					}
					if (y == null) {
						y = z.second;
						continue;
					}
					ret = true;
					it.remove();
					Sb.get(B.fks.get(g).second).add(y, z.second);
					Sb.get(B.fks.get(g).second).add(z.second, y);
				}
			}
		}
		return ret;
	}

	// private final int max;

	@SuppressWarnings("rawtypes")
	Map<Term, Integer> rank = Util.mk();
	@SuppressWarnings("rawtypes")
	List<Term> rankInv = new ArrayList<>();

	private final Collage<Ty, En2, Sym, Fk2, Att2, Gen, Sk> col;
	private int reduce;

	public SigmaLeftKanAlgebra(Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> f2,
			Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i2, Collage<Ty, En2, Sym, Fk2, Att2, Gen, Sk> col,
			int reduce) {
		A = f2.src;
		B = f2.dst;
		F = f2;
		X = i2;
		this.fresh = 0;
		this.col = col;
		this.reduce = reduce;

		if (!X.algebra().hasFreeTypeAlgebra()) {
			throw new RuntimeException("Chase cannot be used: type algebra is not free");
		}

		for (En2 n : B.ens) {
			Pb.put(n, (new THashSet<>()));
			Sb.put(n, new BinRelSet<>());
		}
		for (Fk2 e : B.fks.keySet()) {
			Pg.put(e, new BinRelSet<>());
		}

		for (En1 n : A.ens) {
			List<Pair<X, Term<Void, En2, Void, Fk2, Void, Gen, Void>>> j = (new ArrayList<>(X.algebra().size(n)));
			Set<Term<Void, En2, Void, Fk2, Void, Gen, Void>> i = Pb.get(F.ens.get(n));
			for (X v : X.algebra().en(n)) {
				Term<Ty, En2, Sym, Fk2, Att2, Gen, Void> tt = F.trans(X.algebra().repr(n, v).convert());

				int fr = fresh();
				Term<Void, En2, Void, Fk2, Void, Gen, Void> ii = tt.convert();
				rank.put(ii, fr);
				rankInv.add(ii);
				j.add(new Pair<>(v, ii));
				i.add(ii);
			}
			ua.put(n, j);
		}

		if (!compute()) {
			throw new RuntimeException("Fixed point not reached");
		}

		for (Fk2 fk : Pg.keySet()) {
			Map<Integer, Integer> m = new THashMap<>();
			for (Pair<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>> k : Pg
					.get(fk).R) {
				m.put(rank.get(k.first), rank.get(k.second));
			}
			fkMap.put(fk, m);
		}

		talg0();

	}

	@Override
	public String toString() {
		return "LeftKan [Pb=" + Pb + ", Pg=" + Pg + ", ua=" + ua + ", Sb=" + Sb + "]";
	}

	public final Map<En2, Set<Term<Void, En2, Void, Fk2, Void, Gen, Void>>> Pb = new THashMap<>();
	public final Map<Fk2, BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>>> Pg = new THashMap<>();
	public final Map<En1, List<Pair<X, Term<Void, En2, Void, Fk2, Void, Gen, Void>>>> ua = new THashMap<>();
	private final Map<En2, BinRelSet<Term<Void, En2, Void, Fk2, Void, Gen, Void>, Term<Void, En2, Void, Fk2, Void, Gen, Void>>> Sb = new THashMap<>();

	@Override
	public boolean eq(Map<Var, Chc<Ty, En2>> Map, Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk> lhs,
			Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk> rhs) {
		if (!lhs.hasTypeType()) {
			return this.intoX(lhs).equals(intoX(rhs));
		}
		return this.intoY(lhs).equals(intoY(rhs));
	}

	//////////////////////////////////////////////////////////////////////////////

	@Override
	public Schema<Ty, En2, Sym, Fk2, Att2> schema() {
		return B;
	}

	private final Map<En2, Collection<Integer>> enCache = Util.mk();

	@Override
	public synchronized Collection<Integer> en(En2 en) {
		Collection<Integer> ret = enCache.get(en);
		if (ret != null) {
			return ret;
		}
		ret = new ArrayList<>(Pb.get(en).size());
		for (Term<Void, En2, Void, Fk2, Void, Gen, Void> x : Pb.get(en)) {
			ret.add(rank.get(x));
		}
		enCache.put(en, ret);
		return ret;
	}

	@Override
	public Integer gen(Gen x) {
		return rank.get(Util.lookup(ua.get(X.gens().get(x)), X.algebra().gen(x)));
	}

	Map<Fk2, Map<Integer, Integer>> fkMap = Util.mk();

	private Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att2>>> reprT0(Chc<Sk, Pair<Integer, Att2>> y) {
		return schema().typeSide.js.java_tys.isEmpty() ? talg.simpl(Term.Sk(y))
				: schema().typeSide.js.reduce(talg.simpl(Term.Sk(y)));
	}

	@Override
	public Integer fk(Fk2 fk, Integer x) {
		return fkMap.get(fk).get(x);
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att2>>> att(Att2 att, Integer x) {
		return reprT0(Chc.inRight(new Pair<>(x, att)));
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att2>>> sk(Sk sk) {
		return reprT0(Chc.inLeft(sk));
	}

	@Override
	public Term<Void, En2, Void, Fk2, Void, Gen, Void> repr(En2 en2, Integer x) {
		return rankInv.get(x);
	}

	TalgSimplifier<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>> talg = null;

	private Iterable<Pair<Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>, Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>>> eqsIt() {
		return new Iterable<>() {
			@Override
			public Iterator<Pair<Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>, Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>>> iterator() {
				return Iterators.transform(col.eqs().iterator(), x->new Pair<>(x.lhs,x.rhs));
			}
		};
	}
	
	@Override
	public synchronized TAlg<Ty, Sym, Chc<Sk, Pair<Integer, Att2>>> talg0() {
		if (talg == null) {
			talg = new TalgSimplifier<>(this, eqsIt().iterator(), col.sks(), reduce);
		}
		return talg.talg.out;
	}

	@Override
	public String toStringProver() {
		return "Sigma Left Kan Sequential";
	}

	@Override
	public Object printX(En2 en2, Integer x) {
		return x.toString();
	}

	@Override
	public Object printY(Ty ty, Chc<Sk, Pair<Integer, Att2>> y) {
		return y.toString();
	}

	@Override
	public int size(En2 en) {
		return Pb.get(en).size();
	}

	@Override
	public Chc<Sk, Pair<Integer, Att2>> reprT_prot(Chc<Sk, Pair<Integer, Att2>> y) {
		return y;
	}

}
