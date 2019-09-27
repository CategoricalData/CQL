package catdata.aql.fdm;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.iterators.IteratorIterable;

import com.google.common.collect.Iterators;

import catdata.Chc;
import catdata.Pair;
import catdata.aql.Algebra;
import catdata.aql.Collage;
import catdata.aql.DP;
import catdata.aql.Instance;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.graph.UnionFind;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class DistinctInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
		extends Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> {

	private final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;

	private final LinkedList<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqs = new LinkedList<>();

	private final UnionFind<X> uf;
	
	public static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> make(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i) {
		Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> j = new DistinctInstance<>(i);
		if (i.size() == j.size()) {
			return j;
		}
		for (;;) {
			i = new DistinctInstance(j);
			if (i.size() == j.size()) {
				return j;
			}
			j = i;
		}
			
	}

	private DistinctInstance(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i) {
		I = i;
		// eqs.addAll();
		uf = new UnionFind<>(I.size(), I.algebra().allXs());
		for (En en : schema().ens) {
			for (X x : I.algebra().en(en)) {
				for (X y : I.algebra().en(en)) {
					if (!x.equals(y) && obsEq(en, x, y)) {
						uf.union(x, y);
						eqs.add(new Pair<>(I.algebra().repr(en, x).convert(), I.algebra().repr(en, y).convert()));
					}
				}
			}
		}
		
		// validate();
	}

	private boolean obsEq(En en, X x, X y) {
		//System.out.println("At " + en + ": " + x + " = " + y);
		for (Fk fk : schema().fksFrom(en)) {
			if (!I.algebra().fk(fk, x).equals(I.algebra().fk(fk, y))) {
			//	System.out.println("false on " + fk);
				return false;
			}
		}
		for (Att att : schema().attsFrom(en)) {
			if (!I.dp().eq(null, I.reprT(I.algebra().att(att, x)), I.reprT(I.algebra().att(att, y)))) {
			//	System.out.println("false on " + att);
				return false;
			}
		}
		//System.out.println("true ");
		return true;
	}

	private X conv(X x) {
		return uf.find(x);
	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return I.schema();
	}

	@Override
	public Map<Gen, En> gens() {
		return I.gens();
	}

	@Override
	public Map<Sk, Ty> sks() {
		return I.sks();
	}

	@Override
	public Iterable<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqs() {
		return new IteratorIterable<>(Iterators.concat(eqs.iterator(), I.eqs().iterator()), true);
	}

	@Override
	public DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp() {
		return I.dp();
	}

	@Override
	public Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> algebra() {
		return new InnerAlgebra();
	}

	private final class InnerAlgebra extends Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> {

		Map<En, Collection<X>> map = (new THashMap<>());

		private InnerAlgebra() {
			for (En en : schema().ens) {
				Set<X> set = new THashSet<>();
				I.algebra().en(en).forEach(x -> set.add(conv(x)));
				map.put(en, set);
			}
		}

		@Override
		public Schema<Ty, En, Sym, Fk, Att> schema() {
			return I.schema();
		}

		@Override
		public Iterable<X> en(En en) {
			return map.get(en);
		}

		@Override
		public X fk(Fk fk, X x) {
			return conv(I.algebra().fk(fk, x));
		}

		@Override
		public Term<Ty, Void, Sym, Void, Void, Void, Y> att(Att att, X x) {
			return I.algebra().att(att, x);
		}

		@Override
		public Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Sk sk) {
			return I.algebra().sk(sk);
		}

		@Override
		public X gen(Gen gen) {
			return conv(I.algebra().gen(gen));
		}

		@Override
		public synchronized Term<Void, En, Void, Fk, Void, Gen, Void> repr(En en, X x) {
			return I.algebra().repr(en, x);
		}

		@Override
		public Collage<Ty, Void, Sym, Void, Void, Void, Y> talg0() {
			return I.algebra().talg();
		}

		@Override
		public String toStringProver() {
			return "Distinct-instance wrapper of " + I.algebra().toStringProver();
		}

		@Override
		public Object printX(En en, X x) {
			return I.algebra().printX(en, x);
		}

		@Override
		public Object printY(Ty ty, Y y) {
			return I.algebra().printY(ty, y);
		}

		@Override
		public boolean hasFreeTypeAlgebra() {
			return I.algebra().hasFreeTypeAlgebra();
		}

		@Override
		public boolean hasFreeTypeAlgebraOnJava() {
			return I.algebra().hasFreeTypeAlgebraOnJava();
		}

		@Override
		public int size(En en) {
			return map.get(en).size();
		}

		@Override
		public Chc<Sk, Pair<X, Att>> reprT_prot(Y y) {
			return I.algebra().reprT_prot(y);
		}

	}

	@Override
	public boolean requireConsistency() {
		return I.requireConsistency();
	}

	@Override
	public boolean allowUnsafeJava() {
		return I.allowUnsafeJava();
	}

	@Override
	public int numEqs() {
		return eqs.size() + I.numEqs();
	}
}
