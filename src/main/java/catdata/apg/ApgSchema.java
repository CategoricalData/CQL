package catdata.apg;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;

import catdata.Pair;
import catdata.Util;
import catdata.aql.Kind;
import catdata.aql.Semantics;

public class ApgSchema<L> implements Map<L, ApgTy<L>>, Semantics {
	
	/*
	public void type(ApgTerm<Void> term, ApgTy<L> ty) {
		if (term.e != null) {
			Util.abort(term.e);
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
		} else if (term.c != null) {
			throw new RuntimeException("Expected an introduction form, encountered case analysis of " + term.a);
		}
		
	} */

	public final Map<L, ApgTy<L>> schema;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
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
		ApgSchema<?> other = (ApgSchema<?>) obj;
		if (typeside == null) {
			if (other.typeside != null)
				return false;
		} else if (!typeside.equals(other.typeside))
			return false;
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		return true;
	}

	public final ApgTypeside typeside;
	
	public ApgSchema(ApgTypeside typeside, Map<L, ApgTy<L>> schema) {
		this.schema = schema;
		this.typeside = typeside;
	}

	@Override
	public String toString() {
		return "labels\n\t" + Util.sep(schema, " -> ", "\n\t");
	}

	@Override
	public int size() {
		return schema.size();
	}

	@Override
	public boolean isEmpty() {
		return schema.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return schema.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return schema.containsValue(value);
	}

	@Override
	public ApgTy<L> get(Object key) {
		return schema.get(key);
	}

	@Override
	public ApgTy<L> put(L key, ApgTy<L> value) {
		return schema.put(key, value);
	}

	@Override
	public ApgTy<L> remove(Object key) {
		return schema.remove(key);
	}

	@Override
	public void putAll(Map<? extends L, ? extends ApgTy<L>> m) {
		schema.putAll(m);
	}

	@Override
	public void clear() {
		schema.clear();
	}

	@Override
	public Set<L> keySet() {
		return schema.keySet();
	}

	@Override
	public Collection<ApgTy<L>> values() {
		return schema.values();
	}

	@Override
	public Set<Entry<L, ApgTy<L>>> entrySet() {
		return schema.entrySet();
	}

	@Override
	public Kind kind() {
		return Kind.APG_schema;
	}
	
	
	
}
