package catdata.cql.fdm;

import java.util.ArrayList;
import java.util.Collections;
//import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.Algebra;
import catdata.cql.DP;
import catdata.cql.Instance;
import catdata.cql.Schema;
import catdata.cql.Term;

public class SaturatedInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> extends Instance<Ty, En, Sym, Fk, Att, X, Y, X, Y> {

	private final IMap<X, En> gens;

	private final Map<Y, Ty> sks;

	private final DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp;
	public final Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg;
	private final InnerAlgebra inner_alg;
	private final InnerDP inner_dp;

	boolean requireConsistency, allowUnsafeJava;
	// private final Map<Y, Term<Ty, En, Sym, Fk, Att, X, Y>> reprT_extra;

	@Override
	public DP<Ty, En, Sym, Fk, Att, X, Y> dp() {
		return inner_dp;
	}

	@Override
	public Algebra<Ty, En, Sym, Fk, Att, X, Y, X, Y> algebra() {
		return inner_alg;
	}

	final int size2;

	public SaturatedInstance(Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg, DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp,
			boolean requireConsistency, boolean allowUnsafeJava, boolean labelledNulls0,
			Map<Y, Term<Ty, En, Sym, Fk, Att, X, Y>> reprT_extra) {
		this.alg = alg;
		this.dp = dp;
		this.requireConsistency = requireConsistency;
		this.allowUnsafeJava = allowUnsafeJava;

		int size = 0;
		for (En en : schema().ens) {
			size += alg.size(en) * (schema().attsFrom(en).size() + schema().fksFrom(en).size());
		}
		size2 = size;

		gens = new IMap<X, En>() {

			@Override
			public En get(X x) {
				for (En en : alg.schema().ens) {
					for (X y : alg.en(en)) {
						if (x.equals(y)) {
							return en;
						}
					}
				}
				return Util.anomaly();
			}

			@Override
			public boolean containsKey(X x) {
				return get(x) != null;
			}

			@Override
			public void entrySet(BiConsumer<? super X, ? super En> f) {
				for (En en : alg.schema().ens) {
					for (X y : alg.en(en)) {
						f.accept(y, en);
					}
				}
			}

			@Override
			public int size() {
				return size2;
			}

			@Override
			public En remove(X x) {
				return Util.anomaly();
			}

			@Override
			public void put(X x, En y) {
				Util.anomaly();
			}

		};

		sks = alg.talg().sks;

		inner_dp = new InnerDP();
		inner_alg = new InnerAlgebra();

		if (size < 1024 * 4) {
			validate();
			validateMore();
			checkSatisfaction();
		}
	}

	class InnerIt implements Iterator<Pair<Term<Ty, En, Sym, Fk, Att, X, Y>, Term<Ty, En, Sym, Fk, Att, X, Y>>> {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Pair<Term<Ty, En, Sym, Fk, Att, X, Y>, Term<Ty, En, Sym, Fk, Att, X, Y>> next() {
			return null;
		}

	}

	public void checkSatisfaction() {
		for (Triple<Pair<String, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : schema().eqs) {
			// Chc<Ty, En> l = schema().type(eq.first, eq.second);
			for (X x : algebra().en(eq.first.second)) {
				Term<Ty, En, Sym, Fk, Att, X, Y> lhs = eq.second.mapGenSk(Util.<X>voidFn(), Util.<Y>voidFn())
						.subst(Collections.singletonMap(eq.first.first, Term.Gen(x)));
				Term<Ty, En, Sym, Fk, Att, X, Y> rhs = eq.third.mapGenSk(Util.<X>voidFn(), Util.<Y>voidFn())
						.subst(Collections.singletonMap(eq.first.first, Term.Gen(x)));
				if (!dp().eq(null, lhs, rhs)) {
					throw new RuntimeException("Algebra does not satisfy equation forall " + eq.first.first + ". "
							+ eq.second + " = " + eq.third + " on ID " + alg.printX(eq.first.second, x)
							+ ", yields unequal IDs " + lhs.toString() + " and " + rhs.toString());
				}
			}
		}
	}

	public synchronized IMap<X, En> gens() {
		return gens;
	}

	@Override
	public synchronized IMap<Y, Ty> sks() {
		return Instance.mapToIMap(sks);
	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return alg.schema();
	}

	private class InnerAlgebra extends Algebra<Ty, En, Sym, Fk, Att, X, Y, X, Y> {

		@Override
		public Object printX(En en, X x) {
			return alg.printX(en, x);
		}

		@Override
		public Object printY(Ty ty, Y y) {
			return alg.printY(ty, y);
		}

		@Override
		public Iterable<X> en(En en) {
			return alg.en(en);
		}

		@Override
		public X fk(Fk fk, X x) {
			return alg.fk(fk, x);
		}

		@Override
		public Term<Ty, Void, Sym, Void, Void, Void, Y> att(Att att, X x) {
			return alg.att(att, x);
		}

		@Override
		public Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Y sk) {
			return Term.Sk(sk);
		}

		@Override
		public X gen(X x) {
			return x;
		}

		@Override
		public Term<Void, En, Void, Fk, Void, X, Void> repr(En en, X x) {
			return Term.Gen(x);
		}

		@Override
		public TAlg<Ty, Sym, Y> talg0() {
			return alg.talg();
		}

		@Override
		public Schema<Ty, En, Sym, Fk, Att> schema() {
			return alg.schema();
		}

		@Override
		public String toStringProver() {
			return "Saturated Inner Algebra wrapper of " + alg.toStringProver();
		}

		@Override
		public boolean hasFreeTypeAlgebra() {
			return alg.hasFreeTypeAlgebra();
		}

		@Override
		public boolean hasFreeTypeAlgebraOnJava() {
			return alg.hasFreeTypeAlgebraOnJava();
		}

		@Override
		public int size(En en) {
			return alg.size(en);
		}

		@Override
		public Chc<Y, Pair<X, Att>> reprT_prot(Y y) {
			return Chc.inLeft(y);
		}

		@Override
		public boolean hasNulls() {
			return alg.hasNulls();
		}

	}

	private class InnerDP implements DP<Ty, En, Sym, Fk, Att, X, Y> {

		@Override
		public synchronized boolean eq(Map<String, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, X, Y> lhs,
				Term<Ty, En, Sym, Fk, Att, X, Y> rhs) {
			return dp.eq(ctx, transL(lhs), transL(rhs));
		}

		private Term<Ty, En, Sym, Fk, Att, Gen, Sk> transL(Term<Ty, En, Sym, Fk, Att, X, Y> term) {
			if (term.obj() != null) {
				return term.convert();
			} else if (term.var != null) {
				return term.convert();
			} else if (term.sym() != null) {
				List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> l = new ArrayList<>(term.args.size());
				for (Term<Ty, En, Sym, Fk, Att, X, Y> eq : term.args) {
					l.add(transL(eq));
				}
				return Term.Sym(term.sym(), l);
			} else if (term.att() != null) {
				return Term.Att(term.att(), transL(term.arg));
			} else if (term.fk() != null) {
				return Term.Fk(term.fk(), transL(term.arg));
			} else if (term.gen() != null) {
				return alg.repr(gens.get(term.gen()), term.gen()).convert();
			} else if (term.sk() != null) {
				return alg.reprT(Term.Sk(term.sk()));
			}
			throw new RuntimeException("Anomaly: please report");
		}

		@Override
		public String toStringProver() {
			return "Saturated Inner DP wrapper of " + dp.toStringProver();
		}

	}

	@Override
	public boolean requireConsistency() {
		return requireConsistency;
	}

	@Override
	public boolean allowUnsafeJava() {
		return allowUnsafeJava;
	}

}
