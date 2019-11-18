package catdata.aql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.Collage.CCollage;
import gnu.trove.set.hash.THashSet;

public abstract class Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> implements Semantics {

	// public abstract int numEqs();

	@Override
	public String sample(int size) {
		int en_i = 0;
		List<String> u = new LinkedList<>();
		for (En en : Util.alphabetical(schema().ens)) {
			if (algebra().size(en) == 0) {
				continue;
			}
			int x_i = 0;
			List<String> h = new LinkedList<>();
			h.add(en.toString() + " (" + algebra().size(en) + " rows)");
			// h.add("========");
			for (X x : algebra().en(en)) {
				List<String> l = new LinkedList<>();
				l.add("");
				l.add("ID: " + x);

				int att_i = 0;
				for (Att att : schema().attsFrom(en)) {
					l.add(att.toString() + ": " + algebra().att(att, x));
					att_i++;
					if (att_i > (2 * size)) {
						break;
					}
				}

				x_i++;
				h.add(Util.sep(l, "\n"));

				if (x_i > size) {
					break;
				}
			}

			en_i++;
			u.add(Util.sep(h, "\n"));

			if (en_i > size * size) {
				break;
			}
		}

		return Util.sep(u, "\n\n");
	}

	@Override
	public Kind kind() {
		return Kind.INSTANCE;
	}

	/**
	 * @return sum of rows in the algebra
	 */
	@Override
	public int size() {
		return algebra().size();
	}

	public abstract Schema<Ty, En, Sym, Fk, Att> schema();

	public abstract IMap<Gen, En> gens();

	public abstract IMap<Sk, Ty> sks();

	public abstract boolean requireConsistency();

	public abstract boolean allowUnsafeJava();

	public synchronized void eqs(
			BiConsumer<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> f) {
		for (En en : schema().ens) {
			if (schema().fksFrom(en).size() + schema().attsFrom(en).size() == 0) {
				continue;
			}
			for (X x : algebra().en(en)) {
				Term<Ty, En, Sym, Fk, Att, Gen, Sk> q = algebra().repr(en, x).convert();
				for (Fk fk : schema().fksFrom(en)) {
					En en2 = schema().fks.get(fk).second;
					Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs = Term.Fk(fk, q);
					Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs = algebra().repr(en2, algebra().fk(fk, x)).convert();
					Chc<Ty, En> a = type(lhs);
					Chc<Ty, En> b = type(rhs);
					if (!a.equals(b) || !en2.equals(a.r)) {
						throw new RuntimeException("Equation return type mismatch: " + lhs + " = " + rhs + " has " + a + " and " + b);
					}
					if (!lhs.equals(rhs)) {
						//add type check
						f.accept(lhs, rhs);
					}
				}
				for (Att att : schema().attsFrom(en)) {
					Ty ty2 = schema().atts.get(att).second;
					Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs = Term.Att(att, q);
					Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs = algebra().reprT(algebra().att(att, x));
					Chc<Ty, En> a = type(lhs);
					Chc<Ty, En> b = type(rhs);
					if (!a.equals(b) || !ty2.equals(a.l)) {
						throw new RuntimeException("Equation return type mismatch: " + lhs + " = " + rhs + " has " + a + " and " + b);
					}
					if (!lhs.equals(rhs)) {
						f.accept(lhs, rhs);
					}
				}
			}
		}
		for (Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>> eq : algebra().talg().eqs) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> a = algebra().reprT(eq.first);
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> b = algebra().reprT(eq.second);
			if (!a.equals(b)) {
				f.accept(a, b);
			}
		}
		//what about equations in talg?
	}

	public abstract DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp();

	public final synchronized Chc<Ty, En> type(Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		Util.assertNotNull(term);
//		schema().validate(true);
		return term.type(Collections.emptyMap(), Collections.emptyMap(), schema().typeSide.tys, schema().typeSide.syms,
				schema().typeSide.js.java_tys, schema().ens, schema().atts, schema().fks, imapToMapNoScan(gens()), imapToMapNoScan(sks()));
	}

	public final void validate() {
		validateNoTalg();
		if (requireConsistency() && !algebra().hasFreeTypeAlgebra()) {
			RuntimeException ex = new RuntimeException(
					"Not necessarily consistent.  This isn't necessarily an error, but is unusual.  Set require_consistency=false to proceed.  Type algebra is\n\n"
							+ algebra().talg());
			ex.printStackTrace();
			throw ex;
		}
		if (!allowUnsafeJava() && !algebra().hasFreeTypeAlgebraOnJava()) {
			throw new RuntimeException(
					"Unsafe use of java - CQL's behavior is undefined.  Possible solution: add allow_java_eqs_unsafe=true, change the equations, or contact support at info@catinf.com.  Type algebra is\n\n"
							+ algebra().talg());
		}
		toString();
	}
	

	public synchronized final Term<Ty, En, Sym, Fk, Att, Gen, Sk> reprT(Term<Ty, Void, Sym, Void, Void, Void, Y> y) {
		Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret = algebra().reprT(y);
		return ret;
	}

	private Map<Ty, Set<Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> makeModel() {
		Map<Ty, Set<Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> model = Util.mk();
		for (Ty ty : schema().typeSide.tys) {
			model.put(ty, new THashSet<>());
		}
		for (Ty ty : schema().typeSide.js.java_tys.keySet()) {
			for (Object o : TypeSide.enumerate(schema().typeSide.js.java_tys.get(ty))) {
				model.get(ty).add(Term.Obj(o, ty));
			}
		}
		for (Y y : algebra().talg().sks.keySet()) {
			model.get(algebra().talg().sks.get(y)).add(reprT(Term.Sk(y)));
		}

		for (;;) {
			//System.out.println(model);
			boolean changed = false;
			for (Sym f : schema().typeSide.syms.keySet()) {
				List<Ty> s = schema().typeSide.syms.get(f).first;
				Ty t = schema().typeSide.syms.get(f).second;
				List<Set<Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> l = new ArrayList<>(s.size());
				for (Ty ty : s) {
					l.add(model.get(ty));
				}
				List<List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> r = Util.prod(l);
				outer: for (List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args : r) {
					Term<Ty, En, Sym, Fk, Att, Gen, Sk> cand = Term.Sym(f, args);
					for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> other : model.get(t)) {
						if (dp().eq(null, cand, other)) {
							continue outer;
						}
					}
					model.get(t).add(cand);
					changed = true;
				}
			}
			if (!changed) {
				break;
			}
		}

		return model;
	}

	public Term<Ty, En, Sym, Fk, Att, Gen, Sk> talgNF(Term<Ty, En, Sym, Fk, Att, Gen, Sk> t) {
		for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> x : getModel().get(type(t).l)) {
			if (dp().eq(null, t, x)) {
				return x;
			}
		}
		return Util.anomaly();
	}

	public Map<Ty, Set<Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> model;

	public static interface IMap<X, Y> {
		public default String sep(String x, String y) {
			StringBuffer sb = new StringBuffer();
			boolean[] first = new boolean[] { true };
			if (size() > 8096) {
				return "too big to show";
			}
			entrySet((k, v) -> {
				if (first[0]) {
					first[0] = false;
				} else {
					sb.append(y);
				}
				sb.append(k);
				sb.append(x);
				sb.append(v);
			});
			return sb.toString();

		}

		public Y get(X x);

		public boolean containsKey(X x);

		public default void forEach(BiConsumer<? super X, ? super Y> f) {
			entrySet(f);
		}
		
		public void entrySet(BiConsumer<? super X, ? super Y> f);

		public default void keySet(Consumer<X> f) {
			entrySet((k, l) -> f.accept(k));
		}

		public default void values(Consumer<Y> f) {
			entrySet((k, l) -> f.accept(l));
		}

		public default boolean isEmpty() {
			return size() == 0;
		}

		public default void putAll(Map<X, Y> m) {
			entrySet((k, v) -> m.put(k, v));
		}

		public int size();

		public Y remove(X x);

		public void put(X x, Y y);

	}

	public synchronized Map<Ty, Set<Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> getModel() {
		if (model != null) {
			return model;
		}
		this.model = makeModel();

		return model;
	}

	public final void validateNoTalg() {
		// check that each gen/sk is in tys/ens
		gens().forEach((gen, en) -> {
			if (!schema().ens.contains(en)) {
				throw new RuntimeException("On generator " + gen + ", the entity " + en + " is not declared.  Available:\n" + schema().ens);
			}
		});

		sks().forEach((sk, ty) -> {
//					Ty ty = sks().get(sk);
			if (!schema().typeSide.tys.contains(ty)) {
				throw new RuntimeException(
						"On labelled null " + sk + ", the type " + ty + " is not declared." + "\n\n" + this);
			}
		});

		eqs((a, b) -> {
			Chc<Ty, En> lhs = type(a);
			Chc<Ty, En> rhs = type(b);
			if (!lhs.equals(rhs)) {
				throw new RuntimeException("In instance equation " + a + " = " + b + ", lhs sort is "
						+ lhs.toStringMash() + " but rhs sort is " + rhs.toStringMash());
			}
		});
	}

	//private String toString(Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq) {
	//	return eq.first + " = " + eq.second;
	//}

	public abstract Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> algebra();

	private Collage<Ty, En, Sym, Fk, Att, Gen, Sk> collage;

	public final synchronized Collage<Ty, En, Sym, Fk, Att, Gen, Sk> collage() {
		if (collage != null) {
			return collage;
		}
		collage = new CCollage<>(schema().collage());
		
		gens().entrySet((k,v) -> {
			collage().gens().put(k, v);
		});
		sks().entrySet((k,v) -> {
			collage().sks().put(k, v);
		});
		
		eqs((a,b) -> {
			collage.eqs().add(new Eq<>(null, a, b));
		});
		return collage;
	}

	@Override
	public final int hashCode() {
		return collage().hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Instance<?, ?, ?, ?, ?, ?, ?, ?, ?> other = (Instance<?, ?, ?, ?, ?, ?, ?, ?, ?>) obj;
		return collage.equals(other.collage);
	}

	
	
	public static <X,Y,Z> IMap<X,Z> transformValues(IMap<X,Y> map, BiFunction<X,Y,Z> g) {
		return new IMap<>() {

			@Override
			public int size() {
				return map.size();
			}

			@Override
			public boolean isEmpty() {
				return map.isEmpty();
			}

			@SuppressWarnings("unchecked")
			@Override
			public boolean containsKey(Object key) {
				return map.containsKey((X)key);
			}

			@SuppressWarnings("unchecked")
			@Override
			public synchronized Z get(Object key) {
				return g.apply((X)key, map.get((X)key));
			}
	
			@Override
			public synchronized void entrySet(BiConsumer<? super X, ? super Z> f) {
				map.entrySet((a,b)->f.accept(a, g.apply(a,b)));
			}

			@Override
			public void put(X x, Z y) {
				Util.anomaly();
			}

			@Override
			public Z remove(X x) {
				return Util.anomaly();
			}
		
		};
	} 
	
	public static <X,Y> Map<X,Y> imapToMapNoScan(IMap<X,Y> map) {
		return new Map<X,Y>() {

			@Override 
			public synchronized void forEach(BiConsumer<? super X, ? super Y> f) {
				map.entrySet(f);
			}
			
			@Override
			public synchronized int size() {
				return map.size();
			}

			@Override
			public synchronized boolean isEmpty() {
				return map.isEmpty();
			}

			@SuppressWarnings("unchecked")
			@Override
			public synchronized boolean containsKey(Object key) {
				return map.containsKey((X)key);
			}

			@Override
			public synchronized boolean containsValue(Object value) {
				return Util.anomaly();
			}

			@SuppressWarnings("unchecked")
			@Override
			public synchronized Y get(Object key) {
				return map.get((X)key);
			}

			@Override
			public synchronized Y put(X key, Y value) {
				return Util.anomaly();
			}

			@Override
			public synchronized Y remove(Object key) {
				return Util.anomaly();
			}

			@Override
			public synchronized void putAll(Map<? extends X, ? extends Y> m) {
				Util.anomaly();
			}

			@Override
			public synchronized void clear() {
				Util.anomaly();
			}

			@Override
			public synchronized Set<X> keySet() {
				return Util.anomaly();
			}

			@Override
			public synchronized Collection<Y> values() {
				return Util.anomaly();
			}

			@Override
			public synchronized Set<Entry<X, Y>> entrySet() {
				return Util.anomaly();
			}
			
			@Override
			public synchronized String toString() {
				return map.toString();
			}

		
		};
	} 

	public static <X,Y> IMap<X,Y> mapToIMap(Map<X,Y> map) {
		return new IMap<>() {

			@Override
			public synchronized Y get(X x) {
				return map.get(x);
			}

			@Override
			public synchronized boolean containsKey(X x) {
				return map.containsKey(x);
			}

			@Override
			public synchronized void entrySet(BiConsumer<? super X, ? super Y> f) {
				map.forEach(f);
			}

			@Override
			public synchronized int size() {
				return map.size();
			}

			@Override
			public synchronized Y remove(X x) {
				return map.remove(x);
			}

		
			@Override
			public synchronized void put(X x, Y y) {
				map.put(x, y);
			}
			
			@Override
			public synchronized String toString() {
				return map.toString();
			}
		};
	}
	
	public final String toString(String g, String w) {
		if (size() > 8096) {
			return "too big to display";
		}
		final StringBuilder sb = new StringBuilder();
		final List<String> eqs0 = new ArrayList<>(size());
		eqs((a,b) -> {
			eqs0.add(a + " = " + b);
		});
		sb.append(g);
		if (!gens().isEmpty()) {
			sb.append("\n\t" + gens().sep(":", "\n\t"));
		}
		if (!sks().isEmpty()) {
			sb.append("\n\t" + sks().sep(":", "\n\t"));
		}
		sb.append(w);
		if (!eqs0.isEmpty()) {
			sb.append("\n\t" + Util.sep(eqs0, "\n\t"));
		}
		return sb.toString().trim();
	}

	@Override
	public String toString() {
		return toString("generators", "\nequations");
	}

	public Iterator<Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> enOrTy(Chc<En, Ty> enOrTy) {
		if (enOrTy.left) {
			return Chc.leftIterator(algebra().en(enOrTy.l).iterator());
		}
		return Chc.rightIterator(getModel().get(enOrTy.r).iterator());
	}

	public void validateMore() {
		
		gens().entrySet((gen,en)-> {
			if (!schema().ens.contains(en)) {
				throw new RuntimeException("On generator " + gen + ", the entity " + en + " is not declared.");
			}
		});
		
		 sks().entrySet((sk,ty)-> {
			if (!schema().typeSide.tys.contains(ty)) {
				throw new RuntimeException(
						"On labelled null " + sk + ", the type " + ty + " is not declared." + "\n\n" + this);
			}
		});
		
		eqs((a,b) -> {
			Chc<Ty, En> x = type(a);
			Chc<Ty, En> y = type(b);
			if (!x.equals(y)) {
				Util.anomaly();
			}
		});
	}

}
