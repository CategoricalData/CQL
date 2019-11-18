package catdata.aql.fdm;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.google.common.collect.Iterators;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.AqlProver;
import catdata.aql.Collage;
import catdata.aql.Algebra.TAlg;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.DP;
import catdata.aql.Head;
import catdata.aql.Instance;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.TIntHashSet;

public class CoprodInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
		extends Instance<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Pair<String, X>, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> {

	private final Map<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> insts;
	private final Schema<Ty, En, Sym, Fk, Att> sch;
	
	//TAlg<Ty, Sym, Pair<String, Y>> col;
	
	
	public static <X,Y,Z> IMap<X,Z> mapValues(IMap<X,Y> map, BiFunction<X,Y,Z> f) {
		return new IMap<>() {

			@Override
			public Z get(X x) {
				return f.apply(x,map.get(x));
			}

			@Override
			public boolean containsKey(X x) {
				return map.containsKey(x);
			}

			@Override
			public void entrySet(BiConsumer<? super X, ? super Z> g) {
				map.entrySet((x,y)->g.accept(x, f.apply(x, y)));
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



	
	//final int eqsize2;
	
	public CoprodInstance(Map<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> insts0,
			Schema<Ty, En, Sym, Fk, Att> sch0, boolean uj, boolean rc) {
		this.insts = insts0;
		this.sch = sch0;
		this.uj = uj;
		this.rc = rc;
	
		
		for (String x : insts0.keySet()) {
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i = insts0.get(x);
			if (!i.algebra().hasFreeTypeAlgebra() || !i.schema().equals(sch)) {
				Util.anomaly();
			}
		}

//		Util.anomaly();
		
//		validate();

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
					x.getValue().gens().forEach((p,q)->f.accept(new Pair<>(x.getKey(),p), q));
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
					x.getValue().sks().forEach((p,q)->f.accept(new Pair<>(x.getKey(),p), q));
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
				//print
				System.out.print("eq " + lhs + " = " + rhs);
				if (lhs.hasTypeType()) {
					if (schema().typeSide.js.java_tys.isEmpty()) {
						boolean b = algebra().intoY(rhs).equals(algebra().intoY(lhs));
						System.out.println(" " + b);
						return b;
					}
					boolean b = schema().typeSide.js.reduce(algebra().intoY(rhs))
							.equals(schema().typeSide.js.reduce(algebra().intoY(lhs)));
					System.out.println(" " + b);
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
				return true;
			}

			@Override
			public boolean hasFreeTypeAlgebraOnJava() {
				return true;
			}

			@Override
			public Collection<Pair<String, X>> en(En en) {
				Collection<Pair<String, X>> ret = new AbstractCollection<>() {

					@Override
					public synchronized Iterator<Pair<String, X>> iterator() {
						Iterator<Pair<String, X>> ret = Collections.emptyIterator();
						for (Entry<String, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> x : insts.entrySet()) {
							ret = Iterators.concat(Iterators.transform(x.getValue().algebra().en(en).iterator(),i->new Pair<>(x.getKey(),i)), ret);
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
				
				int j = 0;
				for (Pair<String, X> i : ret) {
					j++;
				}
				if (j != ret.size()) {
					Util.anomaly();
				}
				return ret;
//				return ens.get(en);
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
			public Term<Ty, Void, Sym, Void, Void, Void, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> att(Att att, Pair<String, X> x) {
				//return insts.get(x.first).algebra().att(att, x.second).mapGenSk(x0 -> x0,
					//	(x0 -> new Pair<>(x.first, x0)));
				return reprT0(Chc.inRight(new Pair<>(x, att)));
			}

			private Term<Ty, Void, Sym, Void, Void, Void, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> reprT0(Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>> chc) {
				talg();
				return schema().typeSide.js.java_tys.isEmpty() ? talg.simpl(Term.Sk(chc))
						: schema().typeSide.js.reduce(talg.simpl(Term.Sk(chc)));
			}

			@Override
			public Term<Ty, Void, Sym, Void, Void, Void, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> sk(Pair<String, Sk> sk) {
				return reprT0(Chc.inLeft(sk));
				//return insts.get(sk.first).algebra().sk(sk.second).mapGenSk(x0 -> x0, (x0 -> new Pair<>(sk.first, x0))); // .ma
			}

			@Override
			public Term<Void, En, Void, Fk, Void, Pair<String, Gen>, Void> repr(En en, Pair<String, X> x) {
				return insts.get(x.first).algebra().repr(en, x.second).mapGen(y -> new Pair<>(x.first, y));
			}

			 TalgSimplifier 	<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Pair<String, X>, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>>	
			talg; //= new TalgSimplifier(This, null, null, 9999);

			@Override
			public synchronized TAlg<Ty, Sym, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>> talg0() {
				if (talg != null) {
					return talg.talg.out;
				}
				Algebra<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>, Pair<String, X>, Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>>>
				This = this;
				
				Iterator<Pair<Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>, Term<Ty, En, Sym, Fk, Att, Pair<String, Gen>, Pair<String, Sk>>>> 
				w = CoprodInstance.this.collage().eqsAsPairs().iterator();
				
				Map<Pair<String, Sk>, Ty> m = Instance.imapToMapNoScan(sks());
				

				talg = new TalgSimplifier<>(This, w, m, 9999);
				return talg.talg.out;
				
				/*if (talg != null) {
					return talg;
				}
				
				talg = new TAlg<>(new THashMap<>(), new LinkedList<>());

				for (String x : insts.keySet()) {
					Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i = insts.get(x);
					for (Y y : i.algebra().talg().sks.keySet()) {
						talg.sks.put(new Pair<>(x, y), i.algebra().talg().sks.get(y));
					}
				}
				return talg; */
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
				//if (y.left) {
				//	return insts.get(y.r.first).algebra().printY(ty, y.r.second);
				//}
				return "todo " + y; //
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
			public Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>> reprT_prot(Chc<Pair<String, Sk>, Pair<Pair<String, X>, Att>> y) {
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
