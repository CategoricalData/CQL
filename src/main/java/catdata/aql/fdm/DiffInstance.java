package catdata.aql.fdm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.BiFunction;

import catdata.Pair;
import catdata.aql.Algebra;
import catdata.aql.DP;
import catdata.aql.Instance;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Transform;
import gnu.trove.map.hash.THashMap;

public class DiffInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> extends Instance<Ty, En, Sym, Fk, Att, X, Y, X, Y> {

	private final Instance<Ty, En, Sym, Fk, Att, X, Y, X, Y> K;

	public final Transform<Ty, En, Sym, Fk, Att, X, Y, Gen, Sk, X, Y, X, Y> h;

	public <Z> DiffInstance(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I,
			Instance<Ty, En, Sym, Fk, Att, ?, ?, Z, Y> J, boolean uj, boolean rc) {
		if (!I.schema().fks.isEmpty()) {
			throw new RuntimeException("Can't diff with fks.");
		}
		if (!I.schema().equals(J.schema())) {
			throw new RuntimeException("Schemas differ.");
		}
		if (!I.algebra().talg().equals(J.algebra().talg())) {
			throw new RuntimeException("Type algebras not the same.");
		}

		Map<En, Collection<X>> ens = new THashMap<>(J.schema().ens.size());
		Map<Ty, Collection<Y>> tys = new THashMap<>(J.schema().typeSide.tys.size());

		Map<X, Term<Void, En, Void, Fk, Void, Gen, Void>> m = new THashMap<>(J.size());
		Map<Y, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> n = new THashMap<>(J.algebra().talg().sks.size());

		for (Ty ty : I.schema().typeSide.tys) {
			tys.put(ty, new LinkedList<>());
		}
		for (Y k : I.algebra().talg().sks.keySet()) {
			tys.get(I.algebra().talg().sks.get(k)).add(k);
			n.put(k, I.reprT(Term.Sk(k)));
		}

		THashMap<X, Map<Fk, X>> fks = new THashMap<>(J.size());
		THashMap<X, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Y>>> atts = new THashMap<>(J.size());

		BiFunction<En, X, Boolean> inOther = (en, x) -> {
			outer: for (Z y : J.algebra().en(en)) {
				for (Att att : I.schema().attsFrom(en)) {
					Term<Ty, Void, Sym, Void, Void, Void, Y> l = J.algebra().att(att, y);
					Term<Ty, Void, Sym, Void, Void, Void, Y> r = I.algebra().att(att, x);
					Term<Ty, En, Sym, Fk, Att, Gen, Sk> l2 = I.reprT(l);
					Term<Ty, En, Sym, Fk, Att, Gen, Sk> r2 = I.reprT(r);
					if (!I.dp().eq(null, l2, r2)) {
						continue outer;
					}
				}
				return true;
			}
			return false;
		};

		for (En en : I.schema().ens) {
			Collection<X> c = new ArrayList<>(I.algebra().size(en));
			ens.put(en, c);
			for (X x : I.algebra().en(en)) {
				if (inOther.apply(en, x)) {
					continue;
				}
				fks.put(x, Collections.emptyMap()); // no fks
				atts.put(x, new THashMap<>());
				m.put(x, I.algebra().repr(en, x));
				c.add(x);

				for (Att att : I.schema().attsFrom(en)) {
					atts.get(x).put(att, I.algebra().att(att, x));
				}
			}
		}

		boolean dontCheckClosure = false;

		ImportAlgebra<Ty, En, Sym, Fk, Att, X, Y> alg = new ImportAlgebra<>(I.schema(), ens, tys, fks, atts,
				I.algebra()::printX, I.algebra()::printY, dontCheckClosure, I.algebra().talg().eqs);

		K = new SaturatedInstance<>(alg, alg, rc, uj, false, Collections.EMPTY_MAP);
		validate();

		h = new LiteralTransform<>(m, n, this, I, true);
	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return K.schema();
	}

	@Override
	public Map<X, En> gens() {
		return K.gens();
	}

	@Override
	public Map<Y, Ty> sks() {
		return K.sks();
	}

	@Override
	public boolean requireConsistency() {
		return K.requireConsistency();
	}

	@Override
	public boolean allowUnsafeJava() {
		return K.requireConsistency();
	}

	@Override
	public Iterable<Pair<Term<Ty, En, Sym, Fk, Att, X, Y>, Term<Ty, En, Sym, Fk, Att, X, Y>>> eqs() {
		return K.eqs();
	}

	@Override
	public DP<Ty, En, Sym, Fk, Att, X, Y> dp() {
		return K.dp();
	}

	@Override
	public Algebra<Ty, En, Sym, Fk, Att, X, Y, X, Y> algebra() {
		return K.algebra();
	}

	@Override
	public int numEqs() {
		return K.numEqs();
	}

}
