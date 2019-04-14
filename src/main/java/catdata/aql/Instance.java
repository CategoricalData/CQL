package catdata.aql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import gnu.trove.set.hash.THashSet;

public abstract class Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> implements Semantics {

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

	public abstract Map<Gen, En> gens();

	public abstract Map<Sk, Ty> sks();

	public abstract boolean requireConsistency();

	public abstract boolean allowUnsafeJava();

	public abstract Iterable<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqs();

	public abstract DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp();

	public final Chc<Ty, En> type(Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		return term.type(Collections.emptyMap(), Collections.emptyMap(), schema().typeSide.tys, schema().typeSide.syms,
				schema().typeSide.js.java_tys, schema().ens, schema().atts, schema().fks, gens(), sks());
	}

	public final void validate() {
		validateNoTalg();
		if (requireConsistency() && !algebra().hasFreeTypeAlgebra()) {
			throw new RuntimeException(
					"Not necessarily consistent.  This isn't necessarily an error, but is unusual.  Set require_consistency=false to proceed.  Type algebra is\n\n"
							+ algebra().talg());
		}
		if (!allowUnsafeJava() && !algebra().hasFreeTypeAlgebraOnJava()) {
			throw new RuntimeException(
					"Unsafe use of java - CQL's behavior is undefined.  Possible solution: add allow_java_eqs_unsafe=true, change the equations, or contact support at info@catinf.com.  Type algebra is\n\n"
							+ algebra().talg());
		}
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

	public synchronized Map<Ty, Set<Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> getModel() {
		if (model != null) {
			return model;
		}
		this.model = makeModel();

		return model;
	}

	public final void validateNoTalg() {
		// check that each gen/sk is in tys/ens
		for (Gen gen : gens().keySet()) {
			En en = gens().get(gen);
			if (!schema().ens.contains(en)) {
				throw new RuntimeException("On generator " + gen + ", the entity " + en + " is not declared.");
			}
		}
		for (Sk sk : sks().keySet()) {
			Ty ty = sks().get(sk);
			if (!schema().typeSide.tys.contains(ty)) {
				throw new RuntimeException(
						"On labelled null " + sk + ", the type " + ty + " is not declared." + "\n\n" + this);
			}
		}

		for (Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq : eqs()) {
			// check lhs and rhs types match in all eqs
			Chc<Ty, En> lhs = type(eq.first);
			Chc<Ty, En> rhs = type(eq.second);
			if (!lhs.equals(rhs)) {
				throw new RuntimeException("In instance equation " + toString(eq) + ", lhs sort is "
						+ lhs.toStringMash() + " but rhs sort is " + rhs.toStringMash());
			}
		}
	}

	private String toString(Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq) {
		return eq.first + " = " + eq.second;
	}

	public abstract Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> algebra();

	private Collage<Ty, En, Sym, Fk, Att, Gen, Sk> collage;

	public final synchronized Collage<Ty, En, Sym, Fk, Att, Gen, Sk> collage() {
		if (collage != null) {
			return collage;
		}
		collage = new Collage<>(schema().collage());
		collage.gens.putAll(gens());
		collage.sks.putAll(sks());
		for (Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> x : eqs()) {
			collage.eqs.add(new Eq<>(null, x.first, x.second));
		}
		return collage;
	}

	@Override
	public final int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((eqs() == null) ? 0 : eqs().hashCode());
		result = prime * result + ((gens() == null) ? 0 : gens().hashCode());
		result = prime * result + ((schema() == null) ? 0 : schema().hashCode());
		result = prime * result + ((sks() == null) ? 0 : sks().hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Instance<?, ?, ?, ?, ?, ?, ?, ?, ?> other = (Instance<?, ?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (eqs() == null) {
			if (other.eqs() != null)
				return false;
		} else if (!eqs().equals(other.eqs()))
			return false;
		if (gens() == null) {
			if (other.gens() != null)
				return false;
		} else if (!gens().equals(other.gens()))
			return false;
		if (schema() == null) {
			if (other.schema() != null)
				return false;
		} else if (!schema().equals(other.schema()))
			return false;
		if (sks() == null) {
			if (other.sks() != null)
				return false;
		} else if (!sks().equals(other.sks()))
			return false;
		return true;
	}

	public final String toString(String g, String w) {
		final StringBuilder sb = new StringBuilder();
		final List<String> eqs0 = new LinkedList<>();
		for (Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> x : eqs()) {
			eqs0.add(x.first + " = " + x.second);
		}
		sb.append(g);
		if (!gens().isEmpty()) {
			sb.append("\n\t" + Util.sep(gens(), " : ", "\n\t"));
		}
		if (!sks().isEmpty()) {
			sb.append("\n\t" + Util.sep(sks(), " : ", "\n\t"));
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

	

}
