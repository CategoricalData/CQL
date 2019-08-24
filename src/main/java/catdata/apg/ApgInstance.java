package catdata.apg;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import catdata.Pair;
import catdata.Util;
import catdata.aql.Kind;
import catdata.aql.Semantics;
import gnu.trove.map.hash.THashMap;

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
		if (ty.b != null) {
			if (term.v == null) {
				throw new RuntimeException("Expecting data at type " + ty.b + ", but received " + term);
			}
			Pair<Class<?>, Function<String, Object>> x = typeside.Bs.get(ty.b);
			if (x == null || !x.first.isInstance(term.v)) {
				Util.anomaly(); //should already be checked
			}
			
		} else if (ty.l != null) {
			if (term.e == null) {
				throw new RuntimeException("Expecting element at label " + ty.l + ", but received " + term);
			}
			if (!(Es.containsKey(term.e) || Es.containsKey(term.e))) {
				throw new RuntimeException("Not an element: " + term.e);
			}
			L l2 = Es.get(term.e).first;
			if (!ty.l.equals(l2)) {
				throw new RuntimeException("Expecting element at label " + ty.l + ", but received element " + term + " at label " + l2);
			}
			
		} else if (ty.m != null && ty.all) {
			if (term.m == null) {
				throw new RuntimeException("Expecting tuple at type " + ty + ", but received " + term);
			}
			
			for (Entry<String, ApgTerm<E>> x : term.m.entrySet()) {
				ApgTy<L> w = ty.m.get(x.getKey());
				if (w == null) {
					throw new RuntimeException("In " + term + ", " + x.getKey() + ", is not a field in " + x.getValue());
				}
				type(x.getValue(), w);
			}
			for (String w : ty.m.keySet()) {
				if (!term.m.containsKey(w)) {
					throw new RuntimeException("In " + term + ", no field for " + w);
				}
			}				
			
		} else if (ty.m != null && !ty.all) {
			if (term.f == null) {
				throw new RuntimeException("Expecting injection at type " + ty + ", but received " + term);
			}
		
			ApgTy<L> w = ty.m.get(term.f);
			if (w == null) {
				throw new RuntimeException("In " + term + ", " + term.f + ", is not a field in " + ty);
			}
			type(term.a, w);
		}
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
		ApgInstance<?, ?> other = (ApgInstance<?, ?>) obj;
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
