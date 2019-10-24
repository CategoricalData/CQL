package catdata.aql.fdm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.DP;
import catdata.aql.Eq;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;

public class ImportAlgebra<Ty, En, Sym, Fk, Att, X, Y> extends Algebra<Ty, En, Sym, Fk, Att, X, Y, X, Y>
		implements DP<Ty, En, Sym, Fk, Att, X, Y> {
	
	@Override
	public boolean hasNulls() {
		return talg.sks.isEmpty();
	}

	private final Schema<Ty, En, Sym, Fk, Att> schema;
	private final Map<En, Collection<X>> ens;
	private final Map<Ty, Collection<Y>> tys;
	private final Map<X, Map<Fk, X>> fks;
	private final Map<X, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Y>>> atts;

	private final BiFunction<En, X, Object> printX;
	private final BiFunction<Ty, Y, Object> printY;

	private final TAlg<Ty, Sym, Y> talg;

	private Map<X, En> gens = new THashMap<>();

	public ImportAlgebra(Schema<Ty, En, Sym, Fk, Att> schema, Map<En, Collection<X>> ens, Map<Ty, Collection<Y>> tys,
			Map<X, Map<Fk, X>> fks, Map<X, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Y>>> atts,
			BiFunction<En, X, Object> printX, BiFunction<Ty, Y, Object> printY, boolean dontCheckClosure,
			Collection<Eq<Ty, Void, Sym, Void, Void, Void, Y>> eqs) {
		this.schema = schema;
		this.ens = ens;
		this.tys = tys;
		this.fks = fks;
		this.atts = atts;
		this.printX = printX;
		this.printY = printY;

		if (!dontCheckClosure) {
			checkClosure();
		}
		
		int ts = 0;
		for (Collection<Y> x : tys.values()) {
			ts += x.size();
		}

		talg = new TAlg<>(new THashMap<>(ts), new ArrayList<>(eqs.size()));
		for (Entry<Ty, Collection<Y>> ty : tys.entrySet()) {
			for (Y y : ty.getValue()) {
				talg.sks.put(y, ty.getKey());
			}
		}
		for (Eq<Ty, Void, Sym, Void, Void, Void, Y> x : eqs) {
			talg.eqs.add(new Pair<>(x.lhs, x.rhs));
		}
		
		

		for (Entry<En, Collection<X>> en : ens.entrySet()) {
			for (X x : en.getValue()) {
				gens.put(x, en.getKey());
			}
		}

	}

	private void checkClosure() {
		for (En en : schema.ens) {
			for (X x : ens.get(en)) {
				for (Fk fk : schema.fksFrom(en)) {
					if (!fks.containsKey(x)) {
						// moved inside fk loop because don't have Ctx if no fks/atts out
						throw new RuntimeException(
								"Incomplete import: no foreign key values specified for ID " + x + " in entity " + en);
					}
					if (!fks.get(x).containsKey(fk)) {
						throw new RuntimeException("Incomplete import: no value for foreign key " + fk
								+ " specified for  ID " + x + " in entity " + en);
					}
					X y = fk(fk, x);
					if (!ens.get(schema.fks.get(fk).second).contains(y)) {
						throw new RuntimeException("Incomplete import: value for " + x + "'s foreign key " + fk + " is "
								+ y + " which is not an ID imported into " + schema.fks.get(fk).second);
					}
				}
				for (Att att : schema.attsFrom(en)) {
					if (!atts.containsKey(x)) {
						throw new RuntimeException("Incomplete import: no attribute " + att + " specified for ID " + x
								+ " in entity " + en);
					}
					if (!atts.get(x).containsKey(att)) {
						throw new RuntimeException("Incomplete import: no value for attribute " + att
								+ " specified for  ID " + x + " in entity " + en);
					}
					Term<Ty, Void, Sym, Void, Void, Void, Y> y = att(att, x);
					if (y.sk() != null && !tys.get(schema.atts.get(att).second).contains(y.sk())) {
						throw new RuntimeException("Incomplete import: value for " + x + "'s attribute " + att + " is "
								+ y.sk() + " which is not an ID imported into " + schema.atts.get(att).second);
					}
				}
			}
		}
		for (Ty ty : schema.typeSide.tys) {
			if (!tys.containsKey(ty)) {
				throw new RuntimeException("Incomplete import: no skolem values for " + ty);
			}
		}

	}

	

	@Override
	public synchronized boolean eq(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, X, Y> lhs,
			Term<Ty, En, Sym, Fk, Att, X, Y> rhs) {
		if (ctx != null && !ctx.isEmpty()) {
			Util.anomaly();
		} else if (lhs.hasTypeType()) {
			return intoY(lhs).equals(intoY(rhs)); // free talg
		}
		return intoX(lhs).equals(intoX(rhs));
	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return schema;
	}

	@Override
	public Collection<X> en(En en) {
		return ens.get(en);
	}

	@Override
	public X gen(X gen) {
		return gen;
	}

	@Override
	public X fk(Fk fk, X x) {
		return fks.get(x).get(fk);
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Y> att(Att att, X x) {
		return atts.get(x).get(att);
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Y sk) {
		return Term.Sk(sk);
	}

	@Override
	public Term<Void, En, Void, Fk, Void, X, Void> repr(En en, X x) {
		return Term.Gen(x);
	}

	@Override
	public String toStringProver() {
		return "Import algebra prover";
	}

	@Override
	public Object printX(En en, X x) {
		return printX.apply(en, x);
	}

	@Override
	public Object printY(Ty ty, Y y) {
		return printY.apply(ty, y);
	}

	@Override
	public TAlg<Ty, Sym, Y> talg0() {
		return talg;
	}

	@Override
	public boolean hasFreeTypeAlgebra() {
		return talg.eqs.isEmpty();
	}

	@Override
	public int size(En en) {
		return ens.get(en).size();
	}

	@Override
	public Chc<Y, Pair<X, Att>> reprT_prot(Y y) {
		return Chc.inLeft(y);
	}

}
