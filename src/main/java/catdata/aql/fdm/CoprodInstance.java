package catdata.aql.fdm;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.Collage;
import catdata.aql.DP;
import catdata.aql.Instance;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class CoprodInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
		extends Instance<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Pair<String, X>, Pair<String, Y>> {

	private final Map<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> insts;
	private final Schema<Ty, En, Sym, Fk, Att> sch;
	private final Set<Pair<Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>, Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>>> eqs = new THashSet<>();
	private final Map<Pair<String, Gen>, En> gens = new THashMap<>();
	private final Map<Pair<String, Sk>, Ty> sks = new THashMap<>();
	private Map<En, Collection<Pair<String, X>>> ens = new THashMap<>();
	Collage<Ty, Void, Sym, Void, Void, Void, Pair<String, Y>> col;

	public CoprodInstance(Map<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> insts0,
			Schema<Ty, En, Sym, Fk, Att> sch0, boolean uj, boolean rc) {
		this.insts = insts0;
		this.sch = sch0;
		this.uj = uj;
		this.rc = rc;
		for (En en : sch.ens) {
			ens.put(en, new THashSet<>());
		}
		col = new Collage<>(sch.typeSide.collage());
		col.eqs.clear();

		for (String x : insts0.keySet()) {
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i = insts0.get(x);
			for (Gen s : i.gens().keySet()) {
				gens.put(new Pair<>(x, s), i.gens().get(s));
			}
			for (Sk s : i.sks().keySet()) {
				sks.put(new Pair<>(x, s), i.sks().get(s));
			}
			for (Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq : i.eqs()) {
				eqs.add(new Pair<>(eq.first.mapGenSk(x0 -> new Pair<>(x, x0), x0 -> new Pair<>(x, x0)),
						eq.second.mapGenSk(x0 -> new Pair<>(x, x0), x0 -> new Pair<>(x, x0))));
			}
			for (En en : sch0.ens) {
				for (X y : i.algebra().en(en)) {
					ens.get(en).add(new Pair<>(x, y));
				}
			}
			for (Y y : i.algebra().talg().sks.keySet()) {
				col.sks.put(new Pair<>(x, y), i.algebra().talg().sks.get(y));
			}
			if (!i.algebra().hasFreeTypeAlgebra() || !i.schema().equals(sch)) {
				Util.anomaly();
			}
		}
		validate();

	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return sch;
	}

	@Override
	public Map<Pair<String, Gen>, En> gens() {
		return gens;
	}

	@Override
	public Map<Pair<String, Sk>, Ty> sks() {
		return sks;
	}

	private final boolean rc;

	@Override
	public boolean requireConsistency() {
		return rc;
	}

	private final boolean uj;

	@Override
	public boolean allowUnsafeJava() {
		return uj;
	}

	@Override
	public Set<Pair<Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>, Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>>> eqs() {
		return eqs;
	}

	@Override
	public DP<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>> dp() {
		return new DP<>() {

			@Override
			public String toStringProver() {
				return "free (coproduct)";
			}

			@Override
			public boolean eq(Map<Var, Chc<Ty, En>> ctx,
					Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>> lhs,
					Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>> rhs) {
				if (ctx != null && !ctx.isEmpty()) {
					return Util.anomaly();
				}
				if (lhs.hasTypeType()) {
					return algebra().intoY(rhs).equals(algebra().intoY(lhs));
				}
				return algebra().intoX(rhs).equals(algebra().intoX(lhs));
			}
		};

	}

	@Override
	public Algebra<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Pair<String, X>, Pair<String, Y>> algebra() {
		return new Algebra<>() {

			@Override
			public Schema<Ty, En, Sym, Fk, Att> schema() {
				return sch;
			}

			@Override
			public boolean hasFreeTypeAlgebra() {
				return true;
			}

			@Override
			public boolean hasFreeTypeAlgebraOnJava() {
				return true;
			}

			@Override
			public Collection<Pair<String, X>> en(En en) {
				return ens.get(en);
			}

			@Override
			public Pair<String, X> gen(Pair<String, Gen> gen) {
				return new Pair<>(gen.first, insts.get(gen.first).algebra().gen(gen.second));
			}

			@Override
			public Pair<String, X> fk(Fk fk, Pair<String, X> x) {
				return new Pair<>(x.first, insts.get(x.first).algebra().fk(fk, x.second));
			}

			@Override
			public Term<Ty, Void, Sym, Void, Void, Void, Pair<String, Y>> att(Att att, Pair<String, X> x) {
				return insts.get(x.first).algebra().att(att, x.second).mapGenSk(x0 -> x0,
						(x0 -> new Pair<>(x.first, x0)));
			}

			@Override
			public Term<Ty, Void, Sym, Void, Void, Void, Pair<String, Y>> sk(Pair<String, Sk> sk) {
				return insts.get(sk.first).algebra().sk(sk.second).mapGenSk(x0 -> x0, (x0 -> new Pair<>(sk.first, x0))); // .ma
																															// Term.Sk(new
																															// Pair<>(sk.first,
																															// );
			}

			@Override
			public Term<Void, En, Void, Fk, Void, Pair<String, Gen>, Void> repr(En en, Pair<String, X> x) {
				return insts.get(x.first).algebra().repr(en, x.second).mapGen(y -> new Pair<>(x.first, y));
			}

			@Override
			public Collage<Ty, Void, Sym, Void, Void, Void, Pair<String, Y>> talg0() {
				return col;
			}

			@Override
			public String toStringProver() {
				return "Coprod talg";
			}

			@Override
			public Object printX(En en, Pair<String, X> x) {
				return insts.get(x.first).algebra().printX(en, x.second);
			}

			@Override
			public Object printY(Ty ty, Pair<String, Y> y) {
				return insts.get(y.first).algebra().printY(ty, y.second);
			}

			@Override
			public int size(En en) {
				return ens.get(en).size();
			}

			@Override
			public Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>> reprT_prot(Pair<String, Y> y) {
				Chc<Sk, Pair<X, Att>> x = insts.get(y.first).algebra().reprT_prot(y.second);
				if (x.left) {
					return Chc.inLeft(new Pair<>(y.first, x.l));
				}
				return Chc.inRight(new Pair<>(new Pair<>(y.first, x.r.first), x.r.second));
			}

		};
	}

}
