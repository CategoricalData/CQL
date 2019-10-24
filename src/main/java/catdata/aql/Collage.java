package catdata.aql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
//import java.util.HashSet;
//import java.util.LinkedHashMap;
import java.util.LinkedList;
//import java.util.HashSet;
//import java.util.LinkedHashMap;
//import java.util.LinkedList;
//import java.util.LinkedHashMap;
//import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.provers.KBExpFactoryNewImpl;
import catdata.provers.KBTheory;
import gnu.trove.set.hash.THashSet;

public interface Collage<Ty, En, Sym, Fk, Att, Gen, Sk> {

	@SuppressWarnings("unchecked")
	public default <Ty, En, Sym, Fk, Att, Gen, Sk> Collage<Ty, En, Sym, Fk, Att, Gen, Sk> convert() {
		return (Collage<Ty, En, Sym, Fk, Att, Gen, Sk>) this;
	}

	public static class CCollage<Ty, En, Sym, Fk, Att, Gen, Sk> implements Collage<Ty, En, Sym, Fk, Att, Gen, Sk> {
		public CCollage() {}
			
		@Override
		public String toString() {
			return toString(new CCollage<>());
		} 
		
		public CCollage(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col) {
			tys().addAll(col.tys());
			syms().putAll(col.syms());
			java_tys().putAll(col.java_tys());
			java_parsers().putAll(col.java_parsers());
			java_fns().putAll(col.java_fns());
			getEns().addAll(col.getEns());
			atts().putAll(col.atts());
			fks().putAll(col.fks());
			gens().putAll(col.gens());
			sks().putAll(col.sks());
			eqs.addAll(col.eqs());
		}
		
		public Set<Ty> tys() {
			return tys;
		}

		public Map<Sym, Pair<List<Ty>, Ty>> syms() {
			return syms;
		}

		public Map<Ty, String> java_tys() {
			return java_tys;
		}

		public Map<Ty, String> java_parsers() {
			return java_parsers;
		}

		public Map<Sym, String> java_fns() {
			return java_fns;
		}

		public Set<En> getEns() {
			return ens;
		}

		public Map<Att, Pair<En, Ty>> atts() {
			return atts;
		}

		public Map<Fk, Pair<En, En>> fks() {
			return fks;
		}

		public Map<Gen, En> gens() {
			return gens;
		}

		public Map<Sk, Ty> sks() {
			return sks;
		}
		
		public Collection<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> eqs() {
			return eqs;
		}
		
		private final Set<Ty> tys = new THashSet<>();
		private final Map<Sym, Pair<List<Ty>, Ty>> syms = Util.mk();
		private final Map<Ty, String> java_tys = Util.mk();
		private final Map<Ty, String> java_parsers = Util.mk();
		private final Map<Sym, String> java_fns = Util.mk();

		private final Set<En> ens = new THashSet<>();
		private final Map<Att, Pair<En, Ty>> atts = Util.mk();
		private final Map<Fk, Pair<En, En>> fks = Util.mk();

		private final Map<Gen, En> gens = Util.mk();
		private final Map<Sk, Ty> sks = Util.mk();

		private final Collection<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> eqs = new LinkedList<>();
		
	}
	

	
	
	public Set<Ty> tys();

	public Map<Sym, Pair<List<Ty>, Ty>> syms();
	
	public Map<Ty, String> java_tys();
	
	public Map<Ty, String> java_parsers();

	public Map<Sym, String> java_fns();
	
	public Set<En> getEns();

	public Map<Att, Pair<En, Ty>> atts();

	public Map<Fk, Pair<En, En>> fks();

	public Map<Gen, En> gens();

	public Map<Sk, Ty> sks();
	
	public Collection<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> eqs();
	
	public default void addEqs(
			Collection<Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>>> set) {
		for (Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>> eq : set) {
			eqs().add(new Eq<>(Util.inLeft(eq.first), eq.second.convert(), eq.third.convert()));
		}
	}

	public default Collection<Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqsAsTriples() {
		List<Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> ret = new ArrayList<>(
				eqs().size());
		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> x : eqs()) {
			ret.add(x.toTriple());
		}
		return ret;
	}

	public default Collection<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqsAsPairs() {
		Collection<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> p = new ArrayList<>(
				eqs().size());
		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> x : eqs()) {
			if (!x.ctx.isEmpty()) {
				continue;
			}
			p.add(new Pair<>(x.lhs, x.rhs));
		}
		return Collections.synchronizedCollection(p);
	}


	/*public default synchronized void validate() {
		
		for (Sym sym : syms().keySet()) {
			Pair<List<Ty>, Ty> ty = syms().get(sym);
			if (!tys().contains(ty.second)) {
				throw new RuntimeException(
						"On typeside symbol " + sym + ", the return type " + ty.second + " is not declared.");
			}
			for (Ty t : ty.first) {
				if (!tys().contains(t)) {
					throw new RuntimeException(
							"On typeside symbol " + sym + ", the argument type " + t + " is not declared.");
				}
			}
			// System.out.println(sym);
		}
		
		for (Ty k : java_parsers().keySet()) {
			if (!java_tys().containsKey(k)) {
				throw new RuntimeException(
						"There is a java parser for " + k + " but it is not declared as a java type");
			}
		}
		for (Sym sym : java_fns().keySet()) {
			if (!syms().containsKey(sym)) {
				throw new RuntimeException("The java function " + sym + " is not a declared function");
			}
		}
		for (Ty ty : java_tys().keySet()) {
			String parser = java_parsers().get(ty);
			if (parser == null) {
				throw new RuntimeException("No constant parser for " + ty);
			}
			String clazz = java_tys().get(ty);
			Util.load(clazz);
		}
		
		
		for (Att att : atts().keySet()) {
			Pair<En, Ty> ty = atts().get(att);
			if (!tys().contains(ty.second)) {
				throw new RuntimeException(
						"On attribute " + att + ", the target type " + ty.second + " is not declared.");
			} else if (!getEns().contains(ty.first)) {
				throw new RuntimeException(
						"On attribute " + att + ", the source entity " + ty.first + " is not declared.");
			}
		}
		for (Fk fk : fks().keySet()) {
			Pair<En, En> ty = fks().get(fk);
			if (!getEns().contains(ty.second)) {
				throw new RuntimeException(
						"On foreign key " + fk + ", the target entity " + ty.second + " is not declared.");
			} else if (!getEns().contains(ty.first)) {
				throw new RuntimeException(
						"On foreign key " + fk + ", the source entity " + ty.first + " is not declared.");
			}
		}
		
		for (Gen gen : gens().keySet()) {
			En en = gens().get(gen);
			if (!getEns().contains(en)) {
				throw new RuntimeException("On generator " + gen + ", the entity " + en + " is not declared.");
			}
		}
		for (Sk sk : sks().keySet()) {
			Ty ty = sks().get(sk);
			if (!tys().contains(ty)) {
				throw new RuntimeException(
						"On labelled null " + sk + ", the type " + ty + " is not declared." + "\n\n" + this);
			}
		}
		
		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : eqs) {
			Chc<Ty, En> x = type(eq.ctx, eq.lhs);
			Chc<Ty, En> y = type(eq.ctx, eq.rhs);
			if (!x.equals(y)) {
				Util.anomaly();
			}
		}
	}

	*/
	

/*
	@SuppressWarnings("unchecked")
	private default Term<Ty, En, Sym, Fk, Att, Gen, Sk> upgradeTypeSide(Term<Ty, Void, Sym, Void, Void, Void, Void> term) {
		return (Term<Ty, En, Sym, Fk, Att, Gen, Sk>) term;
	}
*/
	/*@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((atts() == null) ? 0 : atts().hashCode());
		result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
		result = prime * result + ((fks() == null) ? 0 : fks().hashCode());
		result = prime * result + ((gens() == null) ? 0 : gens().hashCode());
		result = prime * result + ((java_fns() == null) ? 0 : java_fns().hashCode());
		result = prime * result + ((java_parsers() == null) ? 0 : java_parsers().hashCode());
		result = prime * result + ((java_tys() == null) ? 0 : java_tys().hashCode());
		result = prime * result + ((sks() == null) ? 0 : sks().hashCode());
		result = prime * result + ((syms() == null) ? 0 : syms().hashCode());
		result = prime * result + ((tys() == null) ? 0 : tys().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Collage<?, ?, ?, ?, ?, ?, ?> other = (Collage<?, ?, ?, ?, ?, ?, ?>) obj;
		if (atts() == null) {
			if (other.atts() != null)
				return false;
		} else if (!atts().equals(other.atts()))
			return false;
		if (eqs == null) {
			if (other.eqs != null)
				return false;
		} else if (!eqs.equals(other.eqs))
			return false;
		if (fks() == null) {
			if (other.fks() != null)
				return false;
		} else if (!fks().equals(other.fks()))
			return false;
		if (gens() == null) {
			if (other.gens() != null)
				return false;
		} else if (!gens().equals(other.gens()))
			return false;
		if (java_fns() == null) {
			if (other.java_fns() != null)
				return false;
		} else if (!java_fns().equals(other.java_fns()))
			return false;
		if (java_parsers() == null) {
			if (other.java_parsers() != null)
				return false;
		} else if (!java_parsers().equals(other.java_parsers()))
			return false;
		if (java_tys() == null) {
			if (other.java_tys() != null)
				return false;
		} else if (!java_tys().equals(other.java_tys()))
			return false;
		if (sks() == null) {
			if (other.sks() != null)
				return false;
		} else if (!sks().equals(other.sks()))
			return false;
		if (syms() == null) {
			if (other.syms() != null)
				return false;
		} else if (!syms().equals(other.syms()))
			return false;
		if (tys() == null) {
			if (other.tys() != null)
				return false;
		} else if (!tys().equals(other.tys()))
			return false;
		return true;
	} */

	


	public default <Ty1, En1, Sym1, Fk1, Att1, Gen1, Sk1> String toString(Collage<Ty1, En1, Sym1, Fk1, Att1, Gen1, Sk1> skip) {
		StringBuilder toString = new StringBuilder("");

		toString.append("types\n\t");
		toString.append(Util.sep(Util.diff(tys(), skip.tys()), "\n\t"));

		toString.append("\nentities\n\t");
		toString.append(Util.sep(Util.diff(getEns(), skip.getEns()), "\n\t"));

		toString.append("\nfunctions");
		List<String> temp = new LinkedList<>();
		for (Sym sym : Util.diff(syms().keySet(), skip.syms().keySet())) {
			Pair<List<Ty>, Ty> t = syms().get(sym);
			temp.add(sym + " : " + Util.sep(t.first, ", ") + " -> " + t.second);
		}
		toString.append("\n\t" + Util.sep(temp, "\n\t"));

		List<String> fks0 = new LinkedList<>();
		for (Fk fk : Util.diff(fks().keySet(), skip.fks().keySet())) {
			fks0.add(fk + " : " + fks().get(fk).first + " -> " + fks().get(fk).second);
		}
		List<String> atts0 = new LinkedList<>();
		for (Att att : Util.diff(atts().keySet(), skip.atts().keySet())) {
			atts0.add(att + " : " + atts().get(att).first + " -> " + atts().get(att).second);
		}

		toString.append("\nforeign keys");
		toString.append("\n\t");
		toString.append(Util.sep(fks0, "\n\t"));

		toString.append("\nattributes");
		toString.append("\n\t");
		toString.append(Util.sep(atts0, "\n\t"));

		toString.append("\ngenerators for entities");
		toString.append("\n\t");
		toString.append(Util.sep(Util.diff(gens(), skip.gens()), " : ", "\n\t"));

		toString.append("\ngenerators for nulls");
		toString.append("\n\t");
		toString.append(Util.sep(Util.diff(sks(), skip.sks()), " : ", "\n\t"));

		List<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> zz = Util.diff(eqs(), skip.eqs());
		List<String> eqs0 = new ArrayList<>(zz.size());
		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : zz) {
			eqs0.add(eq.toString());
		}
		toString.append("\nequations");
		toString.append("\n\t");
		toString.append(Util.sep(eqs0, "\n\t"));

		return toString.toString();
	}

	public default String tptp() {
		List<String> l = new ArrayList<>(eqs().size());
		int i = 0;
		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : eqs()) {
			l.add("cnf(eq" + i + ",axiom,(" + eq.lhs.tptp() + " = " + eq.rhs.tptp() + ")).");
			i++;
		}
		return Util.sep(l, "\n\n");
	}

	public default Chc<Ty, En> type(Map<Var, Ty> ctx1, Map<Var, En> ctx2, Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		return term.type(ctx1, ctx2, tys(), syms(), java_tys(), getEns(), atts(), fks(), gens(), sks());
	}

	public default Chc<Ty, En> type(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		Pair<LinkedHashMap<Var, Ty>, LinkedHashMap<Var, En>> m = Util.split(ctx);
		return term.type(m.first, m.second, tys(), syms(), java_tys(), getEns(), atts(), fks(), gens(), sks());
	}

	public default KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> toKB() {

		@SuppressWarnings("unchecked")
		KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> kb = new KBTheory<>(
				KBExpFactoryNewImpl.factory);

		for (Ty ty : tys()) {
			kb.tys.add(Chc.inLeft(ty));
		}
		for (En en : getEns()) {
			kb.tys.add(Chc.inRight(en));
		}

		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : eqs()) {
			kb.eqs.add(new Triple<>(eq.ctx, eq.lhs.toKB(), eq.rhs.toKB()));
		}

		toKbObj(this, kb.syms);
		toKbSym(this, kb.syms);
		toKbFk(this, kb.syms);
		toKbAtt(this, kb.syms);
		toKBGen(this, kb.syms);
		toKbSk(this, kb.syms);

		Set<Chc<Ty, En>> sorts = new THashSet<>(tys().size() + getEns().size());
		sorts.addAll(Chc.inLeft(tys()));
		sorts.addAll(Chc.inRight(getEns()));
		kb.validate();
		return kb;
	}

	private void toKbSk(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col,
			Map<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Pair<List<Chc<Ty, En>>, Chc<Ty, En>>> signature) {
		for (Sk sk : col.sks().keySet()) {
			List<Chc<Ty, En>> l = (Collections.emptyList());
			signature.put(Head.SkHead(sk), new Pair<>(l, Chc.inLeft(col.sks().get(sk))));
		}
	}

	private void toKBGen(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col,
			Map<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Pair<List<Chc<Ty, En>>, Chc<Ty, En>>> signature) {
		for (Gen gen : col.gens().keySet()) {
			List<Chc<Ty, En>> l = (Collections.emptyList());
			signature.put(Head.GenHead(gen), new Pair<>(l, Chc.inRight(col.gens().get(gen))));
		}
	}

	private void toKbAtt(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col,
			Map<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Pair<List<Chc<Ty, En>>, Chc<Ty, En>>> signature) {
		for (Att att : col.atts().keySet()) {
			List<Chc<Ty, En>> l = (Collections.singletonList(Chc.inRight(col.atts().get(att).first)));
			signature.put(Head.AttHead(att), new Pair<>(l, Chc.inLeft(col.atts().get(att).second)));
		}
	}

	private void toKbFk(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col,
			Map<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Pair<List<Chc<Ty, En>>, Chc<Ty, En>>> signature) {
		for (Fk fk : col.fks().keySet()) {
			List<Chc<Ty, En>> l = (Collections.singletonList(Chc.inRight(col.fks().get(fk).first)));
			signature.put(Head.FkHead(fk), new Pair<>(l, Chc.inRight(col.fks().get(fk).second)));
		}
	}

	private void toKbSym(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col,
			Map<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Pair<List<Chc<Ty, En>>, Chc<Ty, En>>> signature) {
		for (Sym sym : col.syms().keySet()) {
			List<Chc<Ty, En>> l = (Chc.inLeft(col.syms().get(sym).first));
			signature.put(Head.SymHead(sym), new Pair<>(l, Chc.inLeft(col.syms().get(sym).second)));
		}
	}

	private void toKbObj(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col,
			Map<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Pair<List<Chc<Ty, En>>, Chc<Ty, En>>> signature) {
		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : col.eqs()) {
			Set<Pair<Object, Ty>> objs = new THashSet<>();
			eq.lhs.objs(objs);
			eq.rhs.objs(objs);
			for (Pair<Object, Ty> p : objs) {
				signature.put(Head.ObjHead(p.first, p.second),
						new Pair<>(Collections.emptyList(), Chc.inLeft(p.second)));
			}
		}
	}

	public default boolean isGround() {
		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : eqs()) {
			if (!eq.ctx.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public default boolean isMonoidal() {
		for (Sym sym : syms().keySet()) {
			if (syms().get(sym).first.size() > 1) {
				return false;
			}
		}
		return true;
	}

	public default Collage<Ty, En, Sym, Fk, Att, Gen, Sk> entities_only() {
		Collage<Ty, En, Sym, Fk, Att, Gen, Sk> ret = new CCollage<>();
		ret.getEns().addAll(getEns());
		ret.fks().putAll(fks());
		ret.gens().putAll(gens());
		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : eqs()) {
			if (!type(eq.ctx, eq.lhs).left) {
				ret.eqs().add(eq);
			}
		}
		return ret;
	}

	public default void addAll(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> v) {
		tys().addAll(v.tys());
		getEns().addAll(v.getEns());
		syms().putAll(v.syms());
		atts().putAll(v.atts());
		fks().putAll(v.fks());
		gens().putAll(v.gens());
		sks().putAll(v.sks());
		eqs().addAll(v.eqs());
		java_tys().putAll(v.java_tys());
		java_fns().putAll(v.java_fns());
		java_parsers().putAll(v.java_parsers());

	}

	public default Collection<String> allSymbolsAsStrings() {
		Collection<String> ret = new THashSet<>(syms().size()+atts().size()+fks().size()+gens().size()+sks().size());
		for (Sym k : syms().keySet()) {
			ret.add(k.toString());
		}
		for (Att k : atts().keySet()) {
			ret.add(k.toString());
		}
		for (Fk k : fks().keySet()) {
			ret.add(k.toString());
		}
		for (Gen k : gens().keySet()) {
			ret.add(k.toString());
		}
		for (Sk k : sks().keySet()) {
			ret.add(k.toString());
		}
		return ret;
	}

	public default void remove(Head<Ty, En, Sym, Fk, Att, Gen, Sk> head) {
		if (head.att() != null) {
			atts().remove(head.att());
		} else if (head.fk() != null) {
			fks().remove(head.fk());
		} else if (head.sym() != null) {
			syms().remove(head.sym());
		} else if (head.gen() != null) {
			gens().remove(head.gen());
		} else if (head.sk() != null) {
			sks().remove(head.sk());
		} else {
			throw new RuntimeException("Anomaly: please report");
		}
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> boolean defn(Map<Var, Chc<Ty, En>> ctx,
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs, Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
		if (lhs.var != null || lhs.obj() != null) {
			return false;
		}
		List<Var> vars = getVarArgsUnique(lhs);
		if (vars == null) {
			return false; // f(x,x) kind of thing
		}
		if (!new THashSet<>(vars).equals(ctx.keySet())) {
			return false; // forall x, y. f(y) = ... kind of thing
		}

		Head<Ty, En, Sym, Fk, Att, Gen, Sk> head = Head.mkHead(lhs);
		if (!rhs.contains(head)) {
			return true;
		}
		return false;
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> List<Var> getVarArgsUnique(Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		List<Var> ret = (new LinkedList<>());
		for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : term.args()) {
			if (arg.var == null || ret.contains(arg.var)) {
				return null;
			}
			ret.add(arg.var);
		}
		return ret;
	}

	
}
