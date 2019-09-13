package catdata.apg;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import catdata.Pair;
import catdata.Util;
import catdata.aql.Kind;
import catdata.aql.Semantics;
import gnu.trove.map.hash.THashMap;

public class ApgInstance<L,E> implements Semantics {
	
	 
	/*public static <L,E>  ApgInstance<L,Pair<L,E>> alt( 
			ApgSchema<L> ls, Map<L, Map<E, ApgTerm<L,Pair<L,E>>>> es) {
		Map<Pair<L, E>, Pair<L, ApgTerm<L,Pair<L, E>>>> m = new THashMap<>();
		for (Entry<L, Map<E, ApgTerm<L,Pair<L,E>>>> x : es.entrySet()) {
			for (Entry<E, ApgTerm<L,Pair<L,E>>> y : x.getValue().entrySet()) {
				Pair<L, ApgTerm<L,Pair<L, E>>> p = new Pair<>(x.getKey(), y.getValue());
				m.put(new Pair<>(x.getKey(), y.getKey()), p);		
			}
		}
		return new ApgInstance<>(ls, m);
	}*/
	
	public ApgInstance(ApgSchema<L> ls, Map<E, Pair<L, ApgTerm<L, E>>> es) {
		Es = es;
		Ls = ls;
		validate();
	}
	
	public final List<ApgTerm<L,E>> elemsFor(ApgTy<L> t) {
		if (t.b != null) {
			throw new RuntimeException("Cannot enumerate types: " + t.b);
		} else if (t.l != null) {
			List<ApgTerm<L,E>> ret = new LinkedList<>();
			for (Entry<E, Pair<L, ApgTerm<L, E>>> e : Es.entrySet()) {
				if (e.getValue().first.equals(t.l)) {
					ret.add(ApgTerm.ApgTermE(e.getKey()));
				}
			}
			return ret;
		} else if (!t.all) {
			List<ApgTerm<L,E>> ret = new LinkedList<>();
			for (Entry<String, ApgTy<L>> x : t.m.entrySet()) {
				for (ApgTerm<L, E> y : elemsFor(x.getValue())) {
					ApgTerm<L, E> m = y.convert();
					ret.add(ApgTerm.ApgTermInj(x.getKey(), m, x.getValue()).convert());
				}
			}
			return ret;
		} 
		
		List<ApgTerm<L,E>> ret = new LinkedList<>();
		ret.add(ApgTerm.ApgTermTuple(Collections.emptyMap()));
		
		for (Entry<String, ApgTy<L>> x : t.m.entrySet()) {
			List<ApgTerm<L,E>> ret2 = new LinkedList<>();
			for (ApgTerm<L, E> y : elemsFor(x.getValue())) {
				for (ApgTerm<L, E> w : ret) {
					Map<String, ApgTerm<L,E>> map = new HashMap<>(w.fields);
					map.put(x.getKey(), y);
					ret2.add(ApgTerm.ApgTermTuple(map));
				}
			}
			ret = ret2;
		}
		return ret;
		
	}
	
	public final Map<E, Pair<L,ApgTerm<L,E>>> Es;
	public final ApgSchema<L> Ls;

	@Override
	public Kind kind() {
		return Kind.APG_instance;
	}
	@Override
	public int size() {
		return Es.size() + Ls.schema.size();
	}
	
	public void validate() {
		for (Entry<E, Pair<L, ApgTerm<L, E>>> e : Es.entrySet()) {
			if (!Ls.schema.containsKey(e.getValue().first)) {
				throw new RuntimeException("Label not in instance: " + e.getValue().first);
			}
			ApgTy<L> t = Ls.schema.get(e.getValue().first);
			wf(t);
			type(e.getValue().second, t);
		}
	}
	
	public Set<ApgTerm<L,E>> eval(ApgTy<L> ty) {
		
		return Util.anomaly();
	}
	
	public void wf(ApgTy<L> ty) {
		if (ty.b != null && ! Ls.typeside.Bs.containsKey(ty.b)) {
			throw new RuntimeException("Type not in typeside: " + ty.b);
		}
		if (ty.l != null && ! Ls.schema.containsKey(ty.l)) {
			throw new RuntimeException("Label not in instance: " + ty.l);
		}
		if (ty.m != null) {
			ty.m.forEach((k,v) -> wf(v));
		}
	}
	
	public void type(ApgTerm<L, E> term, ApgTy<L> ty) {
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
			if (term.value == null) {
				throw new RuntimeException("Expecting data at type " + ty.b + ", but received " + term);
			}
			Pair<Class<?>, Function<String, Object>> x = Ls.typeside.Bs.get(ty.b);
			if (x == null) {
				throw new RuntimeException("Not a type: " + ty.b);
			} else if (!x.first.isInstance(term.value)) {
				throw new RuntimeException("Not an instance of " + x.first + ": " + term.value + ", is " + term.value.getClass().getSimpleName());
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
			if (term.fields == null) {
				throw new RuntimeException("Expecting tuple at type " + ty + ", but received " + term);
			}
			
			for (Entry<String, ApgTerm<L, E>> x : term.fields.entrySet()) {
				ApgTy<L> w = ty.m.get(x.getKey());
				if (w == null) {
					throw new RuntimeException("In " + term + ", " + x.getKey() + ", is not a field in " + x.getValue());
				}
				type(x.getValue(), w);
			}
			for (String w : ty.m.keySet()) {
				if (!term.fields.containsKey(w)) {
					throw new RuntimeException("In " + term + ", no field for " + w);
				}
			}				
			
		} else if (ty.m != null && !ty.all) {
			if (term.inj == null) {
				throw new RuntimeException("Expecting injection at type " + ty + ", but received " + term);
			}
		
			ApgTy<L> w = ty.m.get(term.inj);
			if (w == null) {
				throw new RuntimeException("In " + term + ", " + term.inj + ", is not a field in " + ty);
			}
			type(term.a, w);
		} else if (term.var != null) {
			throw new RuntimeException("Expected a closed term, encountered variable " + term.var);
		} else if (term.proj != null) {
			throw new RuntimeException("Expected an introduction form, encountered projection of " + term.proj);
		} else if (term.cases != null) {
			throw new RuntimeException("Expected an introduction form, encountered case analysis of " + term.a);
		} else if (term.deref != null) {
			throw new RuntimeException("Expected an introduction form, encountered foreign key of " + term.deref);
		}
		
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Es == null) ? 0 : Es.hashCode());
		result = prime * result + ((Ls == null) ? 0 : Ls.hashCode());
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
		return true;
	}
	@Override
	public String toString() {
	String x = "";
		for (Entry<E, Pair<L, ApgTerm<L, E>>> e : Es.entrySet()) {
			x += "\t" + e.getKey() + ":" + e.getValue().first + " -> " + e.getValue().second +"\n";
		}

		return "elements\n" + x;
	}
	
	
	
}
