package catdata.aql;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.iterators.IteratorIterable;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.Util.UpTo;
import catdata.aql.AqlOptions.AqlOption;

public class SkeletonInstanceWrapperInv<Ty, En, Sym, Fk, Att, Gen, Sk>
		extends Instance<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Integer> {

	private final SkeletonInstance<Ty, En, Sym, Fk, Att, Gen, Sk> I;

	private final Map<Sk, Ty> sks;
	private final Map<Gen, En> gens;
	private final Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Integer> alg;
	private final DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp;

	public SkeletonInstanceWrapperInv(SkeletonInstance<Ty, En, Sym, Fk, Att, Gen, Sk> i) {
		I = i;
		alg = new InnerAlg();

		dp = new DP<>() {
			@Override
			public String toStringProver() {
				return "Skeleton Inv Wrapper";
			}

			@Override
			public boolean eq(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs,
					Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
				if (ctx != null && !ctx.isEmpty()) {
					Util.anomaly();
				}
				if (lhs.hasTypeType()) {
					return I.eqT(I.evalT(lhs), I.evalT(rhs));
				}
				return I.eval(lhs.convert()) == I.eval(rhs.convert());
			}

		};

		gens = new AbstractMap<>() {
			@Override
			public Set<Entry<Gen, En>> entrySet() {
				return new AbstractSet<>() {
					@Override
					public Iterator<Entry<Gen, En>> iterator() {
						return I.gens().iterator();
					}

					@Override
					public int size() {
						return I.numGens();
					}
				};
			}
		};
		sks = new AbstractMap<>() {
			@Override
			public Set<Entry<Sk, Ty>> entrySet() {
				return new AbstractSet<>() {
					@Override
					public Iterator<Entry<Sk, Ty>> iterator() {
						return I.sks().iterator();
					}

					@Override
					public int size() {
						return I.ys();
					}
				};
			}
		};

		if (size() < 1024 * 16) {
			validate();
			// checkSatisfaction();
		}
	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return I.schema();
	}

	@Override
	public boolean requireConsistency() {
		return (boolean) I.options().getOrDefault(AqlOption.require_consistency);
	}

	@Override
	public boolean allowUnsafeJava() {
		return (boolean) I.options().getOrDefault(AqlOption.allow_java_eqs_unsafe);
	}

	@Override
	public IMap<Gen, En> gens() {
		return Instance.mapToIMap(gens);
	}

	@Override
	public IMap<Sk, Ty> sks() {
		return Instance.mapToIMap(sks);
	}

	@Override
	public DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp() {
		return dp;
	}

	@Override
	public Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Integer> algebra() {
		return alg;
	}

	class InnerAlg extends Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Integer> {
		@Override
		public Schema<Ty, En, Sym, Fk, Att> schema() {
			return I.schema();
		}

		@Override
		public boolean hasFreeTypeAlgebra() {
			return I.hasFreeTypeAlgebra();
		}

		@Override
		public Integer gen(Gen gen) {
			return I.gen(gen);
		}

		@Override
		public Integer fk(Fk fk, Integer x) {
			return I.fk(fk, x);
		}

		@Override
		public Term<Ty, Void, Sym, Void, Void, Void, Integer> att(Att att, Integer x) {
			return I.att(att, x);
		}

		@Override
		public Term<Ty, Void, Sym, Void, Void, Void, Integer> sk(Sk sk) {
			return I.sk(sk);
		}

		@Override
		public Term<Void, En, Void, Fk, Void, Gen, Void> repr(En en, Integer x) {
			return I.repr(x);
		}

		@Override
		public String toStringProver() {
			return "SkeletonWrapperInv";
		}

		@Override
		public Object printX(En en, Integer x) {
			return I.printX(x).toString();
		}

		@Override
		public Object printY(Ty ty, Integer y) {
			return I.printY(y).toString();
		}

		private Collage<Ty, Void, Sym, Void, Void, Void, Integer> talg;

		@Override
		protected synchronized Collage<Ty, Void, Sym, Void, Void, Void, Integer> talg0() {
			if (talg != null) {
				return talg;
			}
			talg = new Collage<>(I.schema().typeSide.collage());
			for (Ty ty : schema().typeSide.tys) {
				for (int i = I.yo(ty); i < I.yo(ty) + I.ys(ty); i++) {
					talg.sks.put(i, ty);
				}
			}
			I.eqsT((lhs, rhs) -> talg.eqs.add(new Eq<>(null, lhs, rhs)));
			return talg;
		}

		@Override
		public int size(En en) {
			return I.xs(en);
		}

		@Override
		public Iterable<Integer> en(En en) {
			return new IteratorIterable<>(new UpTo(I.xo(en), I.xo(en) + I.xs(en)));
		}

		@Override
		public Chc<Sk, Pair<Integer, Att>> reprT_prot(Integer y) {
			return I.reprT_prot(y);
		}

		@Override
		public boolean hasNulls() {
			return I.hasNulls();
		}

	}

	

}
