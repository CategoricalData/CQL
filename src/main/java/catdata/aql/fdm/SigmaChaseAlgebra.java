package catdata.aql.fdm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.AqlProver;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.DP;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Mapping;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import catdata.aql.fdm.Chase.BST;
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
		public synchronized IMap<Gen, En2> gens() {
			return Instance.transformValues(X.gens(), (k, v) -> F.ens.get(v), null, -1);
		}

		@Override
		public synchronized IMap<Sk, Ty> sks() {
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

		/*
		 * @Override public synchronized void eqs(BiConsumer<Term<Ty, En2, Sym, Fk2,
		 * Att2, Gen, Sk>, Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>> f) {
		 * X.eqs((x,y)->f.accept(F.trans(x), F.trans(y))); }
		 */

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

	private Map<En2, Integer> offset = Collections.synchronizedMap(new LinkedHashMap<>());
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

		// System.out.println("### " + chase.toStringShort());
		// System.out.println("### " + chase.toString());

		this.ops = ops;

		for (En2 en2 : chase.eqcs.keySet()) {
			offset.put(en2, size); ///////////////////
			size += chase.en(en2, size).size();
		}
		// System.out.println("offsets " + offset);
		// System.out.println("dst ens " + F.dst.ens);

		Collage<Ty, En2, Sym, Fk2, Att2, Gen, Sk> col = new CCollage<>(F.dst.collage());
		if (!X.schema().typeSide.tys.isEmpty()) {
			X.sks().forEach((k, v) -> {
				col.sks().put(k, v);
			});
			X.eqs((k, v) -> {
				col.eqs().add(new Eq<>(null, F.trans(k), F.trans(v)));
			});
		}
		X.gens().forEach((gen, en) -> {
			gen(gen);
			col.gens().put(gen, F.ens.get(en));
		});
		// System.out.println("--------");
		// System.out.println(col);
		// System.out.println("++");
		X.gens().forEach((gen, en) -> {
			col.gens().put(gen, F.ens.get(en));
		});
		// System.out.println("tostr: " + talg );
		for (En2 en2 : theInst.schema().ens) {
			for (int x : theInst.algebra().en(en2)) {
				// System.out.println(en2 + " : " + x + " : " + theInst.algebra().printX(en2,
				// x));
				Term<Void, En2, Void, Fk2, Void, Gen, Void> w = theInst.algebra().repr(en2, x);

				Chc<Ty, En2> en2x = theInst.type(w.convert());
				if (!en2x.r.equals(en2)) {
					// System.out.println("w " + w + " and " + en2x.r);
					Util.anomaly();
				}
			}
		}

		talg = new TalgSimplifier<>(this, col.eqsAsPairs().iterator(), col.sks(),
				(Integer) ops.getOrDefault(AqlOption.talg_reduction));

//		talg();

		this.dp_ty = AqlProver.createInstance(ops, talg().toCollage(schema().typeSide, true),
				Schema.terminal(schema().typeSide));

		//System.out.println("theinst " + theInst);
//		AqlViewer.makeEnTables(this, false, 99999, new LinkedHashMap<>());

//		AqlViewer.makeEnTables(this, true, 99999, new LinkedHashMap<>());

		theInst.validate();
		
//		this.dp_ty = AqlProver.createInstance(ops, talg.talg.out, Schema.terminal(B.typeSide));
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
		En1 en1 = X.gens().get(gen);
		X x = X.algebra().gen(gen);
		return chase.nt.get(en1).get(x) + offset.get(F.ens.get(en1));		
	}

	@Override
	public synchronized Integer fk(Fk2 fk, Integer x) {
		En2 en3 = F.dst.fks.get(fk).first;
		En2 en2 = F.dst.fks.get(fk).second;
		int o = offset.get(en2);
		int o2 = offset.get(en3);
		BST z = chase.eqcs.get(en3).fks.get(fk)[x - o2];
		int ret = chase.eqcs.get(en2).find(z.node) + o;
		return ret;
	}

	@Override
	public synchronized Term<Void, En2, Void, Fk2, Void, Gen, Void> repr(En2 en2, Integer x) {
		return chase.eqcs.get(en2).iso1[x - offset.get(en2)];
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
		b = Util.diff(talg().eqsNoDefns(), l).isEmpty();
		return b;
	}

	@Override
	public synchronized Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<Integer, Att2>>> sk(Sk sk) {
		return reprT0(Chc.inLeft(sk));
	}

	@Override
	public synchronized Object printX(En2 en2, Integer x) {
		Chase<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2, Gen, Sk, X, Y>.En2Stuff u = chase.eqcs.get(en2);
		int v = x - offset.get(en2);
		if (v < 0 || v >= u.iso1.length || u.iso1[v] == null) {
			throw new RuntimeException("Can't get int " + x + " at entity " + en2 + " v is " + v + " and offset "
					+ offset.get(en2) + " and top " + u.top + " ");
		}
		return u.iso1[v].toString();
	}

	@Override
	public synchronized String printY(Ty ty, Chc<Sk, Pair<Integer, Att2>> y) {
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
	public synchronized int size(En2 en) {
		return chase.en(en, offset.get(en)).size();
	}

	@Override
	public synchronized Chc<Sk, Pair<Integer, Att2>> reprT_prot(Chc<Sk, Pair<Integer, Att2>> y) {
		return y;
	}

}
