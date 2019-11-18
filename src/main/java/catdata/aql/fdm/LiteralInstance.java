package catdata.aql.fdm;

import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.collections4.iterators.IteratorIterable;

import com.google.common.collect.Iterators;

import catdata.Pair;
import catdata.Unit;
import catdata.aql.Algebra;
import catdata.aql.Collage;
import catdata.aql.DP;
import catdata.aql.Instance;
import catdata.aql.Schema;
import catdata.aql.Term;

//TODO aql rename to constant?
public class LiteralInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
		extends Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> {

	private final Schema<Ty, En, Sym, Fk, Att> schema;

	private final IMap<Gen, En> gens;
	private final IMap<Sk, Ty> sks;

	private final Iterable<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqs;

	private final DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp;

	private final Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg;

	boolean requireConsistency, allowUnsafeJava;

	public LiteralInstance(Schema<Ty, En, Sym, Fk, Att> schema, Map<Gen, En> gens, Map<Sk, Ty> sks,
			Iterable<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqs,
			DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp, Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg,
			boolean requireConsistency, boolean allowUnsafeJava) {
		this.schema = schema;
		this.gens = mapToIMap(gens);
		this.sks = mapToIMap(sks);
		this.eqs = eqs;
		this.dp = dp;
		this.alg = alg;
		this.requireConsistency = requireConsistency;
		this.allowUnsafeJava = allowUnsafeJava;
		if (size() < 16 * 1024) {
			validate();
			validateMore();
		}

	}

	

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return schema;
	}

	@Override
	public IMap<Gen, En> gens() {
		return gens;
	}

	@Override
	public IMap<Sk, Ty> sks() {
		return sks;
	}

	public synchronized void eqs(
			BiConsumer<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> f) {
		for (Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> x : eqs) {
			f.accept(x.first, x.second);
		}
	}

	@Override
	public DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp() {
		return dp;
	}

	@Override
	public Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> algebra() {
		return alg;
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
