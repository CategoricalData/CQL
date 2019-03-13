package catdata.aql;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;

public class SkeletonExtensionalInstance<Ty, En, Sym, Fk, Att> extends SkeletonSaturatedInstance<Ty, En, Sym, Fk, Att> {

	private final Schema<Ty, En, Sym, Fk, Att> schema;
	private final int ens;
	private final int tys;

	private final Map<En, Integer> xs;
	private final Map<Ty, Integer> ys;
	private final Map<En, Integer> xo;
	private final Map<Ty, Integer> yo;

	private final Map<Fk, int[]> fks;
	private final Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Integer>[]> atts;

	private final Term<Ty, Void, Sym, Void, Void, Void, Integer>[][] talg_eqs;
	private final BiPredicate<Term<Ty, Void, Sym, Void, Void, Void, Integer>, Term<Ty, Void, Sym, Void, Void, Void, Integer>> talg_dp;

	private final Function<Integer,Chc<Integer, Pair<Integer, Att>>> sks;
	
	private final AqlOptions ops;

	//check if empty atts and if so don't store
	public SkeletonExtensionalInstance(Schema<Ty, En, Sym, Fk, Att> schema, Map<En, Integer> en, Map<Ty, Integer> ty,
			Map<En, Integer> m, 
			Map<Ty, Integer> n,
			Map<Fk, int[]> fks,
			Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Integer>[]> atts,
			Term<Ty, Void, Sym, Void, Void, Void, Integer>[][] talg_eqs,
			BiPredicate<Term<Ty, Void, Sym, Void, Void, Void, Integer>, Term<Ty, Void, Sym, Void, Void, Void, Integer>> talg_dp,
			Function<Integer, Chc<Integer, Pair<Integer, Att>>> sks, AqlOptions ops) {
		this.schema = schema;
		this.xs = en;
		this.ys = ty;
		this.xo = m;
		this.yo = n;
		this.fks = fks;
		this.atts = atts;
		this.talg_eqs = talg_eqs;
		this.talg_dp = talg_dp;
		this.sks = sks;
		this.ops = ops;
		int x = 0;
		for (En t : en.keySet()) {
			x += en.get(t);
		}
		this.ens = x;
		for (Ty t : ty.keySet()) {
			x += ty.get(t);
		}
		this.tys = x - ens;		
	}
	
	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return schema;
	}

	@Override
	public int xs() {
		return ens;
	}

	@Override
	public int ys() {
		return tys;
	}
	
	@Override
	public int ys(Ty ty) {
		return this.ys.get(ty);
	}

	@Override
	public int xs(En en) {
		return this.xs.get(en);
	}

	@Override
	public String toStringProver() {
		return "Skeleton Instance";
	}

	@Override
	public boolean hasFreeTypeAlgebra() {
		return talg_eqs.length == 0;
	}

	@Override
	public Object printX(int x) {
		return x;
	}

	@Override
	public Object printY(int y) {
		return y;
	}

	@Override
	public int fk(Fk fk, int x) {
		return fks.get(fk)[x - this.xo.get(schema.fks.get(fk).first)];
	}

	@Override
	public int yo(Ty ty) {
		return this.yo.get(ty);
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Integer> att(Att att, int x) {
		return this.atts.get(att)[x - this.xo.get(schema.atts.get(att).first)];
	}

	@Override
	public boolean eqT(Term<Ty, Void, Sym, Void, Void, Void, Integer> lhs,
			Term<Ty, Void, Sym, Void, Void, Void, Integer> rhs) {
		return talg_dp.test(lhs, rhs);
	}

	@Override
	public void eqsT(
			BiConsumer<Term<Ty, Void, Sym, Void, Void, Void, Integer>, Term<Ty, Void, Sym, Void, Void, Void, Integer>> f) {
		for (Term<Ty, Void, Sym, Void, Void, Void, Integer>[] x : talg_eqs) {
			f.accept(x[0], x[1]);
		}
	}

	/*@Override
	public Term<Ty, En, Sym, Fk, Att, Integer, Integer> reprT_protected(
			Term<Ty, Void, Sym, Void, Void, Void, Integer> y) {
		if (y.sk() != null) {
			return sks.apply(y.sk());
		}
		List<Term<Ty, En, Sym, Fk, Att, Integer, Integer>> l = new ArrayList<>(y.args.size());
		for (Term<Ty, Void, Sym, Void, Void, Void, Integer> z : y.args) {
			l.add(reprT_protected(z));
		}
		return schema().typeSide.js.reduce(Term.Sym(y.sym(), l));
	}*/

	@Override
	public AqlOptions options() {
		return ops;
	}

	@Override
	protected int numEqsT() {
		return talg_eqs.length;
	}

	@Override
	public int xo(En en) {
		return xo.get(en);
	}

	@Override
	public Chc<Integer, Pair<Integer, Att>> reprT_prot(int y) {
		return sks.apply(y);
		/*if (y.sk() != null) {
			return sks.apply(y.sk());
		}
		List<Term<Ty, En, Sym, Fk, Att, Integer, Integer>> l = new ArrayList<>(y.args.size());
		for (Term<Ty, Void, Sym, Void, Void, Void, Integer> z : y.args) {
			l.add(reprT_protected(z));
		}
		return schema().typeSide.js.reduce(Term.Sym(y.sym(), l)); */
	}

	

	
}
