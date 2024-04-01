package catdata.cql.fdm;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.google.common.collect.Iterators;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.Algebra;
import catdata.cql.DP;
import catdata.cql.Instance;
import catdata.cql.Schema;
import catdata.cql.SqlTypeSide;
import catdata.cql.Term;

public class CoprodInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> extends
		Instance<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Pair<String, X>, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> {

	private final Map<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> insts;
	private final Schema<Ty, En, Sym, Fk, Att> sch;

	private boolean isSql;
	// TAlg<Ty, Sym, Pair<String, Y>> col;

	@Override
	public synchronized void eqs(
			BiConsumer<Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>, Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>> f) {
		for (Entry<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> x : insts.entrySet()) {
			x.getValue()
					.eqs((a, b) -> f.accept(a.mapGenSk(p -> new Pair<>(x.getKey(), p), p -> new Pair<>(x.getKey(), p)),
							b.mapGenSk(p -> new Pair<>(x.getKey(), p), p -> new Pair<>(x.getKey(), p))));
		}

	}
	
	@Override
	public void validateMore() {

		gens().entrySet((gen, en) -> {
			if (!schema().ens.contains(en)) {
				throw new RuntimeException("On generator " + gen + ", the entity " + en + " is not declared.");
			}
		});

		sks().entrySet((sk, ty) -> {
			if (!schema().typeSide.tys.contains(ty)) {
				throw new RuntimeException(
						"On labelled null " + sk + ", the type " + ty + " is not declared." + "\n\n" + this);
			}
		});

		eqs((a, b) -> {
			Chc<Ty, En> x = type(a);
			Chc<Ty, En> y = type(b);
			if (!x.equals(y)) {
				Util.anomaly();
			}
			if (!dp().eq(null, a, b)) {
				throw new RuntimeException("Anomaly: not equal: " + a + " and " + b);
			}
		});

	}

	public static <X, Y, Z> IMap<X, Z> mapValues(IMap<X, Y> map, BiFunction<X, Y, Z> f) {
		return new IMap<>() {

			@Override
			public Z get(X x) {
				return f.apply(x, map.get(x));
			}

			@Override
			public boolean containsKey(X x) {
				return map.containsKey(x);
			}

			@Override
			public void entrySet(BiConsumer<? super X, ? super Z> g) {
				map.entrySet((x, y) -> g.accept(x, f.apply(x, y)));
			}

			@Override
			public int size() {
				return map.size();
			}

			@Override
			public Z remove(X x) {
				return f.apply(x, map.remove(x));
			}

			@Override
			public void put(X x, Z y) {
				// TODO Auto-generated method stub

			}

		};
	}

	// final int eqsize2;

	public CoprodInstance(Map<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> insts0,
			Schema<Ty, En, Sym, Fk, Att> sch0, boolean uj, boolean rc) {
		this.insts = insts0;
		this.sch = sch0;
		this.uj = uj;
		this.rc = rc;

		for (String x : insts0.keySet()) {
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i = insts0.get(x);
			if (!i.schema().equals(sch)) {
				Util.anomaly();
			}
//      if (!i.algebra().hasFreeTypeAlgebra()) {
			// Util.anomaly();
			// }
			// TODO CQL ryan
		}

//    Util.anomaly();
		
		isSql = false;
		for (Sym sym : sch.typeSide.syms.keySet()) {
			if (sym.toString().equals("isNull")) {
				isSql = true;
			} 
//			System.out.println(sym.toString());
		}
//		System.out.println("isSql " + isSql);
		
		algebra().talg();
	//	validate();
	//	validateMore();
		
	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return sch;
	}

	@Override
	public IMap<Pair<String, Gen>, En> gens() {
		return new IMap<>() {

			@Override
			public En get(Pair<String, Gen> x) {
				return insts.get(x.first).gens().get(x.second);
			}

			@Override
			public boolean containsKey(Pair<String, Gen> x) {
				return insts.get(x.first).gens().containsKey(x.second);
			}

			@Override
			public void entrySet(BiConsumer<? super Pair<String, Gen>, ? super En> f) {
				for (Entry<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> x : insts.entrySet()) {
					x.getValue().gens().forEach((p, q) -> f.accept(new Pair<>(x.getKey(), p), q));
				}
			}

			@Override
			public int size() {
				int ret = 0;
				for (Entry<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> x : insts.entrySet()) {
					ret += x.getValue().gens().size();
				}
				return ret;
			}

			@Override
			public En remove(Pair<String, Gen> x) {
				return Util.anomaly();
			}

			@Override
			public void put(Pair<String, Gen> x, En y) {
				Util.anomaly();
			}

		};
	}

	@Override
	public IMap<Pair<String, Sk>, Ty> sks() {
		return new IMap<>() {

			@Override
			public Ty get(Pair<String, Sk> x) {
				return insts.get(x.first).sks().get(x.second);
			}

			@Override
			public boolean containsKey(Pair<String, Sk> x) {
				return insts.get(x.first).sks().containsKey(x.second);
			}

			@Override
			public void entrySet(BiConsumer<? super Pair<String, Sk>, ? super Ty> f) {
				for (Entry<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> x : insts.entrySet()) {
					x.getValue().sks().forEach((p, q) -> f.accept(new Pair<>(x.getKey(), p), q));
				}
			}

			@Override
			public int size() {
				int ret = 0;
				for (Entry<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> x : insts.entrySet()) {
					ret += x.getValue().sks().size();
				}
				return ret;
			}

			@Override
			public Ty remove(Pair<String, Sk> x) {
				return Util.anomaly();
			}

			@Override
			public void put(Pair<String, Sk> x, Ty y) {
				Util.anomaly();
			}

		};
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

	public Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>> rewriteIsNull(
			Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>> rhs) {

		if (!isSql) {
		//	System.out.println("Ret no sql");
			return rhs;

		}

		if (rhs.obj() != null || rhs.gen() != null || rhs.var != null || rhs.sk() != null) {
			return rhs;
		} else if (rhs.att() != null) {
			return Term.Att(rhs.att(), rewriteIsNull(rhs.arg));
		} else if (rhs.fk() != null) {
			return Term.Fk(rhs.fk(), rewriteIsNull(rhs.arg));
		} else if (rhs.sym() != null) {
			List<Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>> l = new LinkedList<>();
			for (var x : rhs.args) {
				l.add(rewriteIsNull(x));
			}
			if (rhs.sym().toString().equals("isNull")) {
				if (l.size() != 1) {
					Util.anomaly();
				}
//				var ty = ((catdata.aql.exp.Sym)rhs.sym()).ty;
				if (l.get(0).sk() != null) {
					Pair<String, Object> sk = (Pair<String, Object>) l.get(0).sk();
					var w = insts.get(sk.first);
				//	System.out.println("1into y on " + rhs);

					// var t = w.algebra().intoY(rhs.mapGenSk(x -> x.second, x -> x.second));
					boolean xx = w.dp().eq(null, rhs.mapGenSk(x -> x.second, x -> x.second),
							(Term<Ty, En, Sym, Fk, Att, Gen, Sk>) Term.Sym(SqlTypeSide.t, Collections.emptyList()));

					if (xx) {
						return Term.Sym(SqlTypeSide.t, Collections.EMPTY_LIST);
					} else {
						return Term.Sym(SqlTypeSide.f, Collections.EMPTY_LIST);
					}
				} else if (l.get(0).att() != null) {

					Pair<String, X> sk = algebra().intoX(l.get(0).arg);
					var w = insts.get(sk.first);
					
					boolean xx = w.dp().eq(null, rhs.mapGenSk(x -> x.second, x -> x.second),
							(Term<Ty, En, Sym, Fk, Att, Gen, Sk>) Term.Sym(SqlTypeSide.t, Collections.emptyList()));

				//	 System.out.println("2into y on " + rhs + " at " + sk.first + " dp " + xx);

					// Optional<Object> o = (Optional<Object>) ret.obj();
					if (xx) {
						return Term.Sym(SqlTypeSide.t, Collections.EMPTY_LIST);
					} else {
						return Term.Sym(SqlTypeSide.f, Collections.EMPTY_LIST);
					}

				} else {
					Util.anomaly();
				}
			} else {
				return Term.Sym(rhs.sym(), l);
			}
		}
		throw new RuntimeException(rhs.toString());
	}

	@Override
	public DP<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>> dp() {
		return new DP<>() {

			@Override
			public String toStringProver() {
				return "free (coproduct)";
			}

			@Override
			public boolean eq(Map<String, Chc<Ty, En>> ctx,
					Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>> lhs,
					Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>> rhs) {
				if (ctx != null && !ctx.isEmpty()) {
					return Util.anomaly();
				}
				// print
			//	System.out.println("eq " + lhs + " = " + rhs);
				if (lhs.hasTypeType()) {
					if (schema().typeSide.js.java_tys.isEmpty()) {
						boolean b = algebra().intoY(rhs).equals(algebra().intoY(lhs));
						// System.out.println(" " + b);
						return b;
					}
			//		System.out.println("rew " + rewriteIsNull(lhs) + " = " + rewriteIsNull(rhs));

					boolean b = schema().typeSide.js.reduce(algebra().intoY(rewriteIsNull(rhs)))
							.equals(schema().typeSide.js.reduce(algebra().intoY(rewriteIsNull(lhs))));
			//		 System.out.println(" " + b);
					return b;

				}
				return algebra().intoX(rhs).equals(algebra().intoX(lhs));

			}
		};

	}

	private Algebra<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Pair<String, X>, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> alg;

	@Override
	public synchronized Algebra<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Pair<String, X>, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> algebra() {
		if (alg != null) {
			return alg;
		}

		alg = new Algebra<>() {

			@Override
			public Schema<Ty, En, Sym, Fk, Att> schema() {
				return sch;
			}

			@Override
			public boolean hasFreeTypeAlgebra() {
				return talg.talg.out.eqsNoDefns().isEmpty();
			}

			@Override
			public boolean hasFreeTypeAlgebraOnJava() {
				return talg.talg.out.eqsNoDefns().isEmpty(); // TODO
			}

			@Override
			public Collection<Pair<String, X>> en(En en) {
				Collection<Pair<String, X>> ret = new AbstractCollection<>() {

					@Override
					public synchronized Iterator<Pair<String, X>> iterator() {
						Iterator<Pair<String, X>> ret = Collections.emptyIterator();
						for (Entry<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> x : insts.entrySet()) {
							ret = Iterators.concat(Iterators.transform(x.getValue().algebra().en(en).iterator(),
									i -> new Pair<>(x.getKey(), i)), ret);
						}
						return ret;

					}

					@Override
					public int size() {
						int ret = 0;
						for (Entry<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> x : insts.entrySet()) {
							ret += x.getValue().algebra().size(en);
						}
						return ret;
					}

				};

				return ret;
//        return ens.get(en);
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
			public Term<Ty, Void, Sym, Void, Void, Void, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> att(Att att,
					Pair<String, X> x) {
				return reprT0(Chc.inRight(new Pair<>(x, att)));
			}

			private Term<Ty, Void, Sym, Void, Void, Void, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> reprT0(
					Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>> chc) {
				talg();
				return schema().typeSide.js.java_tys.isEmpty() ? talg.simpl(Term.Sk(chc))
						: schema().typeSide.js.reduce(talg.simpl(Term.Sk(chc)));
			}

			@Override
			public Term<Ty, Void, Sym, Void, Void, Void, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> sk(
					Pair<String, Sk> sk) {
//				Term<Ty, Void, Sym, Void, Void, Void, Y> ret = insts.get(sk.first).algebra().sk(sk.second);
				return reprT0(Chc.inLeft(sk));
			}

			@Override
			public Term<Void, En, Void, Fk, Void, Pair<String, Gen>, Void> repr(En en, Pair<String, X> x) {
				return insts.get(x.first).algebra().repr(en, x.second).mapGen(y -> new Pair<>(x.first, y));
			}

			TalgSimplifier<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Pair<String, X>, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> talg;

			@Override
			public synchronized TAlg<Ty, Sym, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> talg0() {
				if (talg != null) {
					return talg.talg.out;
				}
				Algebra<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Pair<String, X>, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> This = this;

				Iterator<Pair<Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>, Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>>> w = CoprodInstance.this
						.collage().eqsAsPairs().iterator();

				Map<Pair<String, Sk>, Ty> m = Instance.imapToMapNoScan(sks());

				talg = new TalgSimplifier<>(This, w, m, 1); // TODO
				// System.out.println(talg);
				return talg.talg.out;

				/*
				 * if (talg != null) { return talg; }
				 * 
				 * talg = new TAlg<>(new THashMap<>(), new LinkedList<>());
				 * 
				 * for (String x : insts.keySet()) { Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X,
				 * Y> i = insts.get(x); for (Y y : i.algebra().talg().sks.keySet()) {
				 * talg.sks.put(new Pair<>(x, y), i.algebra().talg().sks.get(y)); } } return
				 * talg;
				 */
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
			public Object printY(Ty ty, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>> y) {
				if (!y.left) {
					return insts.get(y.r.first.first).algebra().att(y.r.second, y.r.first.second);
				}
				return insts.get(y.l.first).algebra().sk(y.l.second);

			}

			@Override
			public int size(En en) {
				int ret = 0;
				for (Entry<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> i : insts.entrySet()) {
					ret += i.getValue().algebra().size(en);
				}
				return ret;
			}

			@Override
			public Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>> reprT_prot(
					Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>> y) {
				return y;
			}

			@Override
			public boolean hasNulls() {
				for (Entry<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> x : insts.entrySet()) {
					if (x.getValue().algebra().hasNulls()) {
						return true;
					}
				}
				return false;
			}

		};

		return alg;
	}

}
