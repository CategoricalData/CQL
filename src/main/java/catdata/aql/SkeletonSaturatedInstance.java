package catdata.aql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.collections4.iterators.IteratorIterable;

import catdata.Util;
import catdata.Util.UpToF;

public abstract class SkeletonSaturatedInstance<Ty, En, Sym, Fk, Att>
		extends SkeletonInstance<Ty, En, Sym, Fk, Att, Integer, Integer> {

	@Override
	public Iterable<Entry<Integer, En>> gens() {
		IteratorChain<Entry<Integer, En>> r = new IteratorChain<>();
		for (En en : schema().ens) {
			r.addIterator(new UpToF<>(xo(en), xo(en) + xs(en), gen -> Map.entry(gen, en)));
		}
		return new IteratorIterable<>(r, true);
	}

	@Override
	public Iterable<Entry<Integer, Ty>> sks() {
		IteratorChain<Entry<Integer, Ty>> r = new IteratorChain<>();
		for (Ty en : schema().typeSide.tys) {
			r.addIterator(new UpToF<>(yo(en), yo(en) + ys(en), gen -> Map.entry(gen, en)));
		}
		return new IteratorIterable<>(r, true);
	}

	@Override
	public int gen(Integer gen) {
		return gen;
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Integer> sk(Integer sk) {
		return Term.Sk(sk);
	}

	@Override
	public Term<Void, En, Void, Fk, Void, Integer, Void> repr(int x) {
		return Term.Gen(x);
	}

	@Override
	public boolean eq(Term<Void, En, Void, Fk, Void, Integer, Void> lhs,
			Term<Void, En, Void, Fk, Void, Integer, Void> rhs) {
		return eval(lhs) == eval(rhs);
	}

	@Override
	public int eval(Term<Void, En, Void, Fk, Void, Integer, Void> t) {
		if (t.gen() != null) {
			return t.gen();
		}
		return fk(t.fk(), eval(t.arg));
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Integer> evalT(Term<Ty, En, Sym, Fk, Att, Integer, Integer> t) {
		if (t.obj() != null) {
			return t.convert();
		} else if (t.sym() != null) {
			List<Term<Ty, Void, Sym, Void, Void, Void, Integer>> l = new ArrayList<>(t.args.size());
			for (Term<Ty, En, Sym, Fk, Att, Integer, Integer> x : t.args) {
				l.add(evalT(x));
			}
			return Term.Sym(t.sym(), l);
		} else if (t.att() != null) {
			int x = eval(t.arg.asArgForAtt());
			return att(t.att(), x);
		} else if (t.sk() != null) {
			return sk(t.sk());
		}
		return Util.anomaly();
	}

	@Override
	public int numEqs() {
		int n = 0;
		for (En en : schema().ens) {
			n += (xs(en) * (schema().fksFrom(en).size() + schema().attsFrom(en).size()));
		}
		return n + numEqsT();
	}

	@Override
	public void eqs(
			BiConsumer<Term<Ty, En, Sym, Fk, Att, Integer, Integer>, Term<Ty, En, Sym, Fk, Att, Integer, Integer>> f) {
		for (Entry<Integer, En> x : gens()) {
			En en = x.getValue();
			Integer i = x.getKey();
			for (Fk fk : schema().fksFrom(en)) {
				Term<Void, En, Void, Fk, Void, Integer, Void> lhs = Term.Fk(fk, Term.Gen(i));
				Term<Void, En, Void, Fk, Void, Integer, Void> rhs = repr(fk(fk, i));
				f.accept(lhs.convert(), rhs.convert());
			}
			for (Att att : schema().attsFrom(en)) {
				Term<Ty, En, Sym, Fk, Att, Integer, Integer> lhs = Term.Att(att, Term.Gen(i));
				Term<Ty, En, Sym, Fk, Att, Integer, Integer> rhs = reprT_protected(att(att, i));
				f.accept(lhs, rhs);
			}
		}
		this.eqsT((x, y) -> f.accept(reprT_protected(x), reprT_protected(y)));
	}

	@Override
	public int numGens() {
		return xs();
	}
}
