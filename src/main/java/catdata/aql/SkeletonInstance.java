package catdata.aql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;

public abstract class SkeletonInstance<Ty, En, Sym, Fk, Att, Gen, Sk>  { 
	
	public abstract Schema<Ty, En, Sym, Fk, Att> schema();

	public abstract int xs();
	
	public abstract int xs(En en);
	
	public abstract int xo(En en);

	public abstract int fk(Fk fk, int x);

	public abstract int ys();
	
	public abstract int ys(Ty ty);

	public abstract int yo(Ty ty);

	public abstract Term<Ty, Void, Sym, Void, Void, Void, Integer> att(Att att, int x);

	public abstract boolean eqT(Term<Ty, Void, Sym, Void, Void, Void, Integer> lhs, 
			Term<Ty, Void, Sym, Void, Void, Void, Integer> rhs);
	
	public abstract void eqsT(BiConsumer<Term<Ty, Void, Sym, Void, Void, Void, Integer>,Term<Ty, Void, Sym, Void, Void, Void, Integer>> f);
	
	public abstract String toStringProver();

	public abstract boolean hasFreeTypeAlgebra();
	
	public abstract AqlOptions options();


	//////////////////////////////////////////////////////////////////
	
	public abstract Object printX(int x);

	public abstract Object printY(int y);
	
	////////////////////////////

	public abstract Iterable<Entry<Gen,En>> gens();

	public abstract Iterable<Entry<Sk,Ty>> sks();
	
	public abstract Term<Void, En, Void, Fk, Void, Gen, Void> repr(int x);

	public abstract Term<Ty, Void, Sym, Void, Void, Void, Integer> sk(Sk sk);
	
	public abstract int numGens();
	
	public abstract int gen(Gen gen);
	
	public abstract Chc<Sk, Pair<Integer, Att>> reprT_prot(int y);
	
	public final Term<Ty, En, Sym, Fk, Att, Gen, Sk> reprT_protected(Term<Ty, Void, Sym, Void, Void, Void, Integer> y) {
		if (y.sk() != null) {
			Chc<Sk, Pair<Integer, Att>> x = reprT_prot(y.sk());
			if (x.left) {
				return Term.Sk(x.l);
			}
			return reprT_protected(att(x.r.second, x.r.first));			
		} else if (y.sym() != null) {
			List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> arr = new ArrayList<>(y.args.size());
			for (Term<Ty, Void, Sym, Void, Void, Void, Integer> x : y.args) {
				arr.add(reprT_protected(x));
			}
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret = Term.Sym(y.sym(), arr);
			if (schema().typeSide.js.java_tys.isEmpty()) {
				return ret; 
			}
			return schema().typeSide.js.reduce(ret);
		}
		return Util.anomaly();
	}
	

	public abstract void eqs(BiConsumer<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> f);

	public abstract boolean eq(Term<Void, En, Void, Fk, Void, Gen, Void> lhs, 
			Term<Void, En, Void, Fk, Void, Gen, Void> rhs);
	
	public abstract int eval(Term<Void, En, Void, Fk, Void, Gen, Void> t);
	
	public abstract Term<Ty, Void, Sym, Void, Void, Void, Integer> evalT(Term<Ty, En, Sym, Fk, Att, Gen, Sk> t);

	public abstract int numEqs();
	
	protected abstract int numEqsT();
}
