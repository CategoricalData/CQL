package catdata.aql.fdm;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.AqlProver;
import catdata.aql.AqlProver.ProverName;
import catdata.aql.Collage;
import catdata.aql.DP;
import catdata.aql.Eq;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import catdata.aql.exp.IgnoreException;
import catdata.graph.DAG;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

public class InitialAlgebra<Ty, En, Sym, Fk, Att, Gen, Sk>
		extends Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>>
		implements DP<Ty, En, Sym, Fk, Att, Gen, Sk> {
	
	@Override
	public boolean hasNulls() {
		return talg().sks.isEmpty();
	}

	public Object printX(En en, Integer x) {
		return repr(en, x).toString(Util.voidFn(), z -> printGen.apply(z).toString());
	}

	@Override
	public Object printY(Ty ty, Chc<Sk, Pair<Integer, Att>> y) {
		return y.left ? printSk.apply(ty, y.l)
				: printX(schema.atts.get(y.r.second).first, y.r.first) + "." + y.r.second;
	}

	private final Map<En, TIntHashSet> ens;
	private final TIntObjectHashMap<TObjectIntHashMap<Fk>> fks = new TIntObjectHashMap<>(16, .75f, -1);

	private final TIntObjectHashMap<Term<Void, En, Void, Fk, Void, Gen, Void>> reprs = new TIntObjectHashMap<>(16, .75f,
			-1);

	private final TObjectIntHashMap<Term<Void, En, Void, Fk, Void, Gen, Void>> nfs = new TObjectIntHashMap<>(16, .75f,
			-1);

	private final Schema<Ty, En, Sym, Fk, Att> schema;

	private Function<Gen, Object> printGen;
	private BiFunction<Ty, Sk, Object> printSk;

	Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col;

	private final DP<Void, En, Void, Fk, Void, Gen, Void> dp_en;
	private final DP<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>> dp_ty;

	private TalgSimplifier<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>> talg;

	int fresh = 0;
	private final boolean talg_is_cons;

	@SuppressWarnings("unchecked")
	public InitialAlgebra(AqlOptions ops, Schema<Ty, En, Sym, Fk, Att> schema,
			Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col, Function<Gen, Object> printGen,
			BiFunction<Ty, Sk, Object> printSk) {
		col.validate();
		this.schema = schema;
		Util.assertNotNull(printGen, printSk);
		this.printGen = printGen;
		this.printSk = printSk;
		this.col = col;
		Collage<Ty, En, Sym, Fk, Att, Gen, Sk> zzz = col.entities_only();
		int limit = (int) ops.getOrDefault(AqlOption.diverge_limit);
		boolean warn = (boolean) ops.getOrDefault(AqlOption.diverge_warn);
		boolean fast = (boolean) ops.getOrDefault(AqlOption.fast_consistency_check);

		// System.out.println(col.eqs);
		checkTermination(schema, zzz.gens.size(), col.eqs.size(), warn, limit);
		this.dp_en = (DP<Void, En, Void, Fk, Void, Gen, Void>) AqlProver.createInstance(ops, zzz, schema);
		ens = new THashMap<>(16, .75f);
		for (En en : schema.ens) {
			ens.put(en, new TIntHashSet(16, .75f, -1));
		}
		while (saturate1());

		talg = null ;//new TalgSimplifier<>(this, col, (Integer) ops.getOrDefault(AqlOption.talg_reduction));

		ProverName p = (ProverName) ops.getOrDefault(AqlOption.second_prover);
		AqlOptions lll = new AqlOptions(ops, AqlOption.prover, p);
		lll = new AqlOptions(lll, AqlOption.completion_precedence, null);
	
		
		//TODO AQL performance
		if (!fast && !talg.talg.out.eqs.isEmpty() && !schema().typeSide.eqs.isEmpty()) {
		Util.anomaly();
		/*	Collage<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>> talg2 = new Collage<>(talg());
			talg2.eqs.clear();
			talg2.addAll(schema().typeSide.collage());
			//System.out.println(talg2);
			DP<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>> z = AqlProver.createInstance(lll, talg2,
					Schema.terminal(schema.typeSide));
			*/
		boolean b = false; /*
			Iterator<Eq<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>>> it = talg().eqs.iterator();
			while (it.hasNext()) {
				//System.out.print(".");
				Eq<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>> x = it.next();
				if (!z.eq(x.ctx, x.lhs, x.rhs)) {
					b = true;
				//	break;
				} else {
					it.remove();
				}
				
			}
			//System.out.println("."); */
			talg_is_cons = !b; 
		} else { 
			talg_is_cons = talg.talg.out.eqs.isEmpty();
		}
		Collage<Ty, Void, Sym, Void, Void, Void, Chc<Sk,Pair<Integer,Att>>> z = schema.typeSide.collage();
		Util.anomaly();
/*		talg().eqs.addAll(z.eqs);
 * */
		this.dp_ty = null; //AqlProver.createInstance(lll, talg(),Schema.terminal(schema.typeSide));
		//talg().eqs.removeAll(z.eqs); 
		
	}

	private void checkTermination(Schema<Ty, En, Sym, Fk, Att> c, int genSize, int eqSize, boolean check, int limit) {
		if (!check || c.fks.size() > limit || genSize == 0 || c.eqs.size() > 0 || eqSize > 0) {
			return;
		}
		DAG<En> dag = new DAG<>();

		for (Entry<Fk, Pair<En, En>> fk : c.fks.entrySet()) {
			if (!dag.addEdge(fk.getValue().second, fk.getValue().first)) {
				throw new RuntimeException(
						"An instance with a cyclic schema, generators, and no equations may diverge.  Set diverge_warn=false to continue.  This warning may be the benign consequence of theory simplification; consider prover_simplify_max = 0 as well.");
			}
		}

	}

	private boolean add(En en, Term<Void, En, Void, Fk, Void, Gen, Void> term) {
		int x = nf0(en, term);
		if (x != -1) {
			return false;
		}
		x = fresh++;

		nfs.put(term, x);
		ens.get(en).add(x);
		reprs.put(x, term);

		TObjectIntHashMap<Fk> map = new TObjectIntHashMap<>(16, .75f, -1);
		for (Fk fk : schema().fksFrom(en)) {
			En e = schema().fks.get(fk).second;
			Term<Void, En, Void, Fk, Void, Gen, Void> z = Term.Fk(fk, term);
			add(e, z);
			map.put(fk, nf0(e, z));
		}
		fks.put(x, map);

		if (fresh % 10000 == 0) {
			if (Thread.currentThread().isInterrupted() ) {
				
				throw new IgnoreException();
			}
		}
		return true;
	}

	private boolean saturate1() {
		boolean changed = false;
		for (Gen gen : col.gens.keySet()) {
			En en = col.gens.get(gen);
			Term<Void, En, Void, Fk, Void, Gen, Void> xx = Term.Gen(gen);
			changed = changed | add(en, xx);
		}
		for (Fk fk : col.fks.keySet()) {
			Pair<En, En> e = schema().fks.get(fk);
			TIntIterator it = ens.get(e.first).iterator();
			while (it.hasNext()) {
				int x = it.next();
				changed = changed | add(e.second, Term.Fk(fk, repr(e.first, x)));
			}
		}
		return changed;
	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return schema;
	}

	@Override
	public Collection<Integer> en(En en) {
		return new AbstractCollection<>() {
			@Override
			public Iterator<Integer> iterator() {
				TIntIterator it = ens.get(en).iterator();
				return new Iterator<>() {
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public Integer next() {
						return it.next();
					}

				};
			}

			@Override
			public int size() {
				return ens.get(en).size();
			}
		};
	}

	@Override
	public Integer fk(Fk fk, Integer x) {
		Integer r = fks.get(x).get(fk);
		return r;
	}

	@Override
	public Term<Void, En, Void, Fk, Void, Gen, Void> repr(En en, Integer x) {
		Term<Void, En, Void, Fk, Void, Gen, Void> ret = reprs.get(x);
		return ret;
	}

	private synchronized int nf0(En en, Term<Void, En, Void, Fk, Void, Gen, Void> term) {
		int xx = nfs.get(term);
		if (xx != -1) {
			return xx;
		}
		TIntIterator it = ens.get(en).iterator();
		while (it.hasNext()) {
			int x = it.next();
			if (dp_en.eq(null, term, repr(en, x))) {
				nfs.put(term, x);
				return x;
			}
		}
		return -1;
	}

	@Override
	public Integer gen(Gen gen) {
		Integer x = nf0(col.gens.get(gen), Term.Gen(gen));
		return x;
	}

	@Override
	public synchronized boolean eq(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs,
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
		Chc<Ty, En> x = col.type(Collections.emptyMap(), lhs);
		if (!x.left) {
			return nf0(x.r, lhs.convert()) == nf0(x.r, rhs.convert());
		}
		return dp_ty.eq(null, intoY(lhs.convert()), intoY(rhs.convert()));
	}

	@Override
	public synchronized TAlg<Ty, Sym, Chc<Sk, Pair<Integer, Att>>> talg0() {
		return talg.talg.out;
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>> att(Att att, Integer x) {
		return reprT0(Chc.inRight(new Pair<>(x, att)));
	}

	private Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>> reprT0(Chc<Sk, Pair<Integer, Att>> y) {
		talg();
		return schema().typeSide.js.java_tys.isEmpty() ? talg.simpl(Term.Sk(y))
				: schema.typeSide.js.reduce(talg.simpl(Term.Sk(y)));
	}

	public boolean hasFreeTypeAlgebra() {
		return talg_is_cons;
	}

	public boolean hasFreeTypeAlgebraOnJava() {
		return talg().eqs.stream().filter(x -> schema().typeSide.js.java_tys.containsKey(talg().type(x.first)))
				.collect(Collectors.toList()).isEmpty();
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att>>> sk(Sk sk) {
		return reprT0(Chc.inLeft(sk));
	}

	@Override
	public String toStringProver() {
		return dp_en + "\n\n-------------\n\n" + dp_ty.toStringProver();
	}

//		@Override
	public DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp() {
		return this; // definitely this - not dp bc dp may be for entity side only
	}

	public String talgToString() {
		return this.talg.toString();
	}

	@Override
	public int size(En en) {
		return ens.get(en).size();
	}

	@Override
	public Chc<Sk, Pair<Integer, Att>> reprT_prot(Chc<Sk, Pair<Integer, Att>> y) {
		return y;
	}

}
