package catdata.aql.fdm;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.google.common.collect.Iterators;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.AqlProver;
import catdata.aql.Collage;
import catdata.aql.DP;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Mapping;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import catdata.aql.fdm.Chase.BST;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class SigmaChaseAlgebra<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2, Gen, Sk, X, Y>
		extends Algebra<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>>
		implements DP<Ty, En2, Sym, Fk2, Att2, Gen, Sk> {
	
	@Override
	public boolean hasNulls() {
		return talg().sks.isEmpty();
	}

	public final Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>> theInst = new SigmaChaseInstance();

	class SigmaChaseInstance extends Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>> {

		@Override
		public Schema<Ty, En2, Sym, Fk2, Att2> schema() {
			return F.dst;
		}

		@Override
		public IMap<Gen, En2> gens() {
			return Instance.transformValues(X.gens(), (k,v)->F.ens.get(v));
		}

		@Override
		public IMap<Sk, Ty> sks() {
			return X.sks();
		}

		@Override
		public boolean requireConsistency() {
			return (boolean) ops.getOrDefault(AqlOption.require_consistency);
		}

		@Override
		public boolean allowUnsafeJava() {
			return (boolean) ops.getOrDefault(AqlOption.allow_java_eqs_unsafe);
		}
		
		@Override
		public synchronized void eqs(BiConsumer<Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>, Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>> f) {
			X.eqs((x,y)->f.accept(F.trans(x), F.trans(y)));
		}

		@Override
		public DP<Ty, En2, Sym, Fk2, Att2, Gen, Sk> dp() {
			return SigmaChaseAlgebra.this;
		}

		@Override
		public Algebra<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>> algebra() {
			return SigmaChaseAlgebra.this;
		}

	}

	private final Schema<Ty, En2, Sym, Fk2, Att2> B;
	private final Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> F;
	private final Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> X;

	private Chase<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2, Gen, Sk, X, Y> chase;

	private TalgSimplifier<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>> talg;
	public Collage<Ty, En2, Sym, Fk2, Att2, Gen, Sk> col;
	private DP<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att2>>> dp_ty;

	private Map<En2, Integer> offset = new THashMap<>();
	private int size = 0;
	private AqlOptions ops;

	public SigmaChaseAlgebra(Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> f2,
			Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i2, Map<En1, Set<Pair<X, X>>> e, AqlOptions ops) {
		if (!f2.src.equals(i2.schema())) {
			Util.anomaly();
		}
		B = f2.dst;
		F = f2;
		X = i2;
		chase = new Chase<>(F, X, e);
		this.ops = ops;

		for (En2 en2 : F.dst.ens) {
			offset.put(en2, size);
			size += chase.en(en2, size).size();
		}
		//if (X.algebra().talg().eqs.size() >= 0) {
			Collage<Ty, En2, Sym, Fk2, Att2, Gen, Sk> col = new Collage<>(F.dst.collage());
			if (!X.schema().typeSide.tys.isEmpty()) {
				X.sks().entrySet((k,v)->{
					col.sks.put(k, v);
				});
				for (Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>> eq : X.algebra().talg().eqs) {
					col.eqs.add(new Eq<>(null, F.trans(X.reprT(eq.first)), F.trans(X.reprT(eq.second))));
				}
			}
			
			Iterator<Pair<Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>,Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>>>
			it = Iterators.transform(X.algebra().talg().eqs.iterator(), eq-> new Pair<>(F.trans(X.reprT(eq.first)), F.trans(X.reprT(eq.second))));
			talg = new TalgSimplifier<>(this, it, null, (int) ops.getOrDefault(AqlOption.talg_reduction));
		//}
		talg();
		
		this.dp_ty = Util.anomaly(); // AqlProver.createInstance(ops, talg.talg.out, Schema.terminal(B.typeSide));
	}

	@Override
	public synchronized boolean eq(Map<Var, Chc<Ty, En2>> ctx, Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk> lhs,
			Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk> rhs) {
		if (!lhs.hasTypeType()) {
			return this.intoX(lhs).equals(intoX(rhs));
		}
		return dp_ty.eq(null, intoY(lhs.convert()), intoY(rhs.convert()));
	}

	@Override
	public Schema<Ty, En2, Sym, Fk2, Att2> schema() {
		return B;
	}

	@Override
	public String toStringProver() {
		return "Sigma Chase Algebra.\n\n" + toString();
	}

	@Override
	public String toString() {
		return "SigmaChaseAlgebra [chase=" + chase + ", talg=" + talg + "]";
	}

	@Override
	public synchronized Collection<Integer> en(En2 en) {
		return chase.en(en, offset.get(en));
	}

	@Override
	public synchronized Integer gen(Gen gen) {
		En2 en2 = F.ens.get(X.gens().get(gen));
		int o = offset.get(en2);
		int ret = chase.stuff.get(en2).find(chase.stuff.get(en2).iso2.get(chase.findNoAdd(Term.Gen(gen), en2))) + o;
		return ret;
	}

	@Override
	public synchronized Integer fk(Fk2 fk, Integer x) {
		En2 en3 = F.dst.fks.get(fk).first;
		En2 en2 = F.dst.fks.get(fk).second;
		int o = offset.get(en2);
		int o2 = offset.get(en3);
		BST z = chase.stuff.get(en3).fks.get(fk)[x - o2];
		int ret = chase.stuff.get(en2).find(z.node) + o;
		return ret;
	}

	@Override
	public synchronized Term<Void, En2, Void, Fk2, Void, Gen, Void> repr(En2 en2, Integer x) {
		return chase.stuff.get(en2).iso1[x - offset.get(en2)];
	}

	@Override
	public synchronized Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att2>>> att(Att2 att, Integer x) {
		return reprT0(Chc.inRight(new Pair<>(x, att)));
	}

	private synchronized Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att2>>> reprT0(
			Chc<Sk, Pair<Integer, Att2>> y) {
		talg();
		return schema().typeSide.js.java_tys.isEmpty() ? talg.simpl(Term.Sk(y))
				: schema().typeSide.js.reduce(talg.simpl(Term.Sk(y)));
	}

	private Boolean b;

	@Override
	public synchronized boolean hasFreeTypeAlgebra() {
		if (b != null) {
			return b;
		}
		talg();
		Set<Eq<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att2>>>> l = (new THashSet<>(
				schema().typeSide.eqs.size()));
		for (Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>> eq : schema().typeSide.eqs) {
			l.add(new Eq<>(Util.inLeft(eq.first), talg.transX(eq.second.convert()), talg.transX(eq.third.convert())));
		}
		b = Util.diff(talg().eqs, l).isEmpty();
		return b;
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att2>>> sk(Sk sk) {
		return reprT0(Chc.inLeft(sk));
	}

	@Override
	public Object printX(En2 en2, Integer x) {
		return chase.stuff.get(en2).iso1[x - offset.get(en2)].toString();
	}

	@Override
	public String printY(Ty ty, Chc<Sk, Pair<Integer, Att2>> y) {
		if (y.left) {
			return y.l.toString();
		}
		return printX(F.dst.atts.get(y.r.second).first, y.r.first) + "." + y.r.second;
	}

	@Override
	public synchronized TAlg<Ty, Sym, Chc<Sk, Pair<Integer, Att2>>> talg0() {
		return talg.talg.out;
	}

	@Override
	public int size(En2 en) {
		return chase.en(en, offset.get(en)).size();
	}

	@Override
	public Chc<Sk, Pair<Integer, Att2>> reprT_prot(Chc<Sk, Pair<Integer, Att2>> y) {
		return y;
	}

}
