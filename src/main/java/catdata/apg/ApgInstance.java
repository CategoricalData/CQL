package catdata.apg;

import java.util.Map;
import java.util.Map.Entry;

import catdata.Pair;
import catdata.Util;
import catdata.aql.Kind;
import catdata.aql.Semantics;

public class ApgInstance<L,E> implements Semantics {
	
	public ApgInstance(ApgTypeside ts, 
			Map<L, ApgTy<L>> ls, Map<E, Pair<L, ApgTerm<E>>> es) {
		typeside = ts;
		Es = es;
		Ls = ls;
		validate();
	}
	public final ApgTypeside typeside;
	
	public final Map<E, Pair<L,ApgTerm<E>>> Es;
	public final Map<L, ApgTy<L>> Ls;

	@Override
	public Kind kind() {
		return Kind.APG_instance;
	}
	@Override
	public int size() {
		return Es.size() + Ls.size();
	}
	
	public void validate() {
		/* for (L l : Ls.keySet()) {
			if (typeside.Bs.contains(l)) { //warning benign
				throw new RuntimeException("Label is also a type: " + l);
			}
		} */
		for (Entry<E, Pair<L, ApgTerm<E>>> e : Es.entrySet()) {
			if (!Ls.containsKey(e.getValue().first)) {
				throw new RuntimeException("Label not in instance: " + e.getValue().first);
			}
			ApgTy<L> t = Ls.get(e.getValue().first);
			wf(t);
			type(e.getValue().second, t);
		}
	}
	
	public void wf(ApgTy<L> ty) {
		if (ty.b != null && ! typeside.Bs.containsKey(ty.b)) {
			throw new RuntimeException("Type not in typeside: " + ty.b);
		}
		if (ty.l != null && ! Ls.containsKey(ty.l)) {
			throw new RuntimeException("Label not in instance: " + ty.l);
		}
		if (ty.m != null) {
			ty.m.forEach((k,v) -> wf(v));
		}
	}
	
	public void type(ApgTerm<E> term, ApgTy<L> ty) {
		if (term.e != null) {
			if (!Es.containsKey(term.e)) {
				throw new RuntimeException("Element " + term.e + " not in " + Es.keySet());
			}
			ApgTy<L> xxx = ApgTy.ApgTyL(Es.get(term.e).first);
			if (!ty.equals(xxx)) {
				throw new RuntimeException("Type error: On " + term + ", expected " + ty + " but actual is " + xxx);
			}
			return;
		}
		//TODO
		/*
		if (term.v != null) {
			if (!typeside.Vs.containsKey(term.v)) {
				throw new RuntimeException("Constant " + term.v + " not in " + typeside.Bs);
			}
			ApgTy<L> xxx = ApgTy.ApgTyB(typeside.Vs.get(term.v));
			if (!ty.equals(xxx)) {
				throw new RuntimeException("Type error: On " + term + ", expected " + ty + " but actual is " + xxx);
			}
			return;
		}
		if (term.m != null) {
			ApgTy<L> xxx = ApgTy.ApgTyP(true, Util.map(term.m, (k,v)->new Pair<>(k,type(v))));
			if (!ty.equals(xxx)) {
				throw new RuntimeException("Type error: On " + term + ", expected " + ty + " but actual is " + xxx);
			}
			return;
		}
		if (term.f != null) {
			ApgTy<L> t = type(term.a);
			if (t.m == null) {
				throw new RuntimeException("Not a product: " + t);
			}
			if (!t.m.containsKey(term.f)) {
				throw new RuntimeException("Field " + term.f + " not in " + t.m.keySet());
			}
			return t.m.get(term.f);
		}
		*/
		 //Util.anomaly();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Es == null) ? 0 : Es.hashCode());
		result = prime * result + ((Ls == null) ? 0 : Ls.hashCode());
		result = prime * result + ((typeside == null) ? 0 : typeside.hashCode());
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
		ApgInstance other = (ApgInstance) obj;
		if (Es == null) {
			if (other.Es != null)
				return false;
		} else if (!Es.equals(other.Es))
			return false;
		if (Ls == null) {
			if (other.Ls != null)
				return false;
		} else if (!Ls.equals(other.Ls))
			return false;
		if (typeside == null) {
			if (other.typeside != null)
				return false;
		} else if (!typeside.equals(other.typeside))
			return false;
		return true;
	}
	@Override
	public String toString() {
		String s = "labels\n\t" + Util.sep(Ls, " -> ", "\n\t") 
				+ "\nelements\n";
		String x = "";
		for (Entry<E, Pair<L, ApgTerm<E>>> e : Es.entrySet()) {
			x += "\t" + e.getKey() + ":" + e.getValue().first + " -> " + e.getValue().second +"\n";
		}

		return s+ x;
	}
	
	
	
}
