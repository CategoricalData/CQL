package catdata.provers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.Util;
import gnu.trove.map.hash.THashMap;

public abstract class KBFO<S,C,V> {
	
	public interface KBFOVisitor<S, C, V, R, E> {
		R visit(E env, AndOr<S, C, V> e);
		R visit(E env, Not<S, C, V> e);
		R visit(E env, Implies<S, C, V> e);
		R visit(E env, Bind<S, C, V> e);
		R visit(E env, Eq<S, C, V> e);
	}

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

	public abstract <R, E> R accept(E env, KBFOVisitor<S, C, V, R, E> e);
	
	protected abstract void type(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur);
	protected abstract void typeInf(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur);

	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static class AndOr<S,C,V> extends KBFO<S,C,V> {
		private final boolean isAnd;
		private final List<KBFO<S,C,V>> es;
		
		public AndOr(boolean isAnd, List<KBFO<S, C, V>> es) {
			this.isAnd = isAnd;
			this.es = es;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((es == null) ? 0 : es.hashCode());
			result = prime * result + (isAnd ? 1231 : 1237);
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
			AndOr<?,?,?> other = (AndOr<?,?,?>) obj;
			if (es == null) {
				if (other.es != null)
					return false;
			} else if (!es.equals(other.es))
				return false;
			return (isAnd == other.isAnd);
		}
		
		@Override
		public <R, E> R accept(E env, KBFOVisitor<S, C, V, R, E> v) {
			return v.visit(env, this);
		}

		@Override
		public void type(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur) {
			for (KBFO<S, C, V> e : es) {
				e.type(Map, cur);
			}
		}
		
		@Override
		public void typeInf(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur) {
			for (KBFO<S, C, V> e : es) {
				e.typeInf(Map, cur);
			}
		}
		
		@Override
		public String toString() {
			String sep = isAnd ? " and " : " or ";
			if (es.isEmpty() && isAnd) {
				return "true";
			}
			if (es.isEmpty() && !isAnd) {
				return "false";
			}
			return "(" + Util.sep(es, sep) + ")";
		}
	}
	
	public static class Implies<S,C,V> extends KBFO<S,C,V> {
		private final KBFO<S,C,V> a, c;
		
		public Implies(KBFO<S, C, V> a, KBFO<S, C, V> c) {
			this.a = a;
			this.c = c;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + ((c == null) ? 0 : c.hashCode());
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
			Implies<?,?,?> other = (Implies<?,?,?>) obj;
			if (a == null) {
				if (other.a != null)
					return false;
			} else if (!a.equals(other.a))
				return false;
			if (c == null) {
				if (other.c != null)
					return false;
			} else if (!c.equals(other.c))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, KBFOVisitor<S, C, V, R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public void type(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur) {
			a.type(Map, cur);
			c.type(Map, cur);
		}
		
		@Override
		public void typeInf(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur) {
			a.typeInf(Map, cur);
			c.typeInf(Map, cur);
		}
		
		@Override
		public String toString() {
			return a + " -> " + c;
		}
	}
	
	public static class Not<S,C,V> extends KBFO<S,C,V> {
		private final KBFO<S,C,V> e;
		
		public Not(KBFO<S, C, V> e) {
			this.e = e;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((e == null) ? 0 : e.hashCode());
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
			Not<?,?,?> other = (Not<?,?,?>) obj;
			if (e == null) {
				if (other.e != null)
					return false;
			} else if (!e.equals(other.e))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, KBFOVisitor<S, C, V, R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public void type(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur) {
			e.type(Map, cur);
		}
		
		@Override
		public void typeInf(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur) {
			e.typeInf(Map, cur);
		}
		
		@Override
		public String toString() {
			return "not " + e;
		}
	}
	
	public static class Bind<S,C,V> extends KBFO<S,C,V> {
		private final boolean isForall;
		private final List<Pair<V, S>> vars;
		private final KBFO<S,C,V> e;
		
		public Bind(boolean isForall, List<Pair<V, S>> vars, KBFO<S, C, V> e) {
			this.isForall = isForall;
			this.vars = vars;
			this.e = e;
			if (vars.isEmpty()) {
				throw new RuntimeException("Empty bind " + this);
			}
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((e == null) ? 0 : e.hashCode());
			result = prime * result + (isForall ? 1231 : 1237);
			result = prime * result + ((vars == null) ? 0 : vars.hashCode());
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
			Bind<?,?,?> other = (Bind<?,?,?>) obj;
			if (e == null) {
				if (other.e != null)
					return false;
			} else if (!e.equals(other.e))
				return false;
			if (isForall != other.isForall)
				return false;
			if (vars == null) {
				if (other.vars != null)
					return false;
			} else if (!vars.equals(other.vars))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, KBFOVisitor<S, C, V, R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public void type(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur) {
			Map<V, S> cur0 = new THashMap<>(cur);
			
			for (Pair<V, S> vs : vars) {
				V v = vs.first;
				S s = vs.second;
				if (s == null) {
					throw new RuntimeException("No sort for variable " + v);
				}
				if (cur0.containsKey(v)) {
					throw new RuntimeException("Shadowed variable: " + v);
				}
				cur0.put(v, s);
			}
			
			e.type(Map, cur);
		}
		
		@Override
		public void typeInf(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur) {
			Map<V, S> cur0 = new THashMap<>(cur);
			
			for (Pair<V, S> vs : vars) {
				V v = vs.first;
				S s = vs.second; //for inference, can be null
				if (cur0.containsKey(v)) {
					throw new RuntimeException("Shadowed variable: " + v);
				}
				cur0.put(v, s);
			}
			
			e.type(Map, cur);
			
			for (Pair<V, S> vs : vars) {
				V v = vs.first;
				S s = vs.second; 
				
				if (s != null) {
					continue;
				}
				
				s = cur.get(v);
				
				if (s == null) {
					throw new RuntimeException("Variable with unconstrainted sort: " + v);
				}
				
				vs.setSecond(s);
			}
		}
		
		@Override
		public String toString() {
			List<String> vars0 = vars.stream().map(x -> x.first + ":" + x.second).collect(Collectors.toList());
			String pre = isForall ? "forall " : "exists ";
			return pre + Util.sep(vars0, ", ") + " . " + e;
		}
	}
	
	public static class Eq<S,C,V> extends KBFO<S,C,V> {
		private final KBExp<C,V> l, r;
		
		public Eq(KBExp<C, V> l, KBExp<C, V> r) {
			this.l = l;
			this.r = r;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((l == null) ? 0 : l.hashCode());
			result = prime * result + ((r == null) ? 0 : r.hashCode());
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
			Eq<?,?,?> other = (Eq<?,?,?>) obj;
			if (l == null) {
				if (other.l != null)
					return false;
			} else if (!l.equals(other.l))
				return false;
			if (r == null) {
				if (other.r != null)
					return false;
			} else if (!r.equals(other.r))
				return false;
			return true;
		}

		@Override
		public <R, E> R accept(E env, KBFOVisitor<S, C, V, R, E> v) {
			return v.visit(env, this);
		}
		
		@Override
		public void type(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur) {
			S s0 = l.type(Map, cur);
			S s1 = r.type(Map, cur);
			if (!s0.equals(s1)) {
				throw new RuntimeException("Equality not at same sort, lhs is " + s0 + " and " + " rhs is " + s1);
			}
		}
		
		@Override
		public void typeInf(Map<C, Pair<List<S>, S>> Map, Map<V, S> cur) {
			S s0 = typeInf(l, Map, cur);
			S s1 = typeInf(r, Map, cur);
			if (s0 != null && s1 != null && !s0.equals(s1)) {
				throw new RuntimeException("Equality not at same sort, lhs is " + s0 + " and " + " rhs is " + s1);
			}
			if (s0 != null && s1 == null) {
				V v = r.getVar(); //can't be in cur, otherwise wouldn't be null
				cur.put(v, s0);
			}
			if (s0 == null && s1 != null) {
				V v = l.getVar(); //can't be in cur, otherwise wouldn't be null
				cur.put(v, s1);
			}			
		}
		
		@Override
		public String toString() {
			return l + " = " + r;
		}
		
	}

	
	public static <C,V,S> S typeInf(KBExp<C, V> x, Map<C, Pair<List<S>, S>> Map, Map<V, S> cur) {
		if (x.isVar()) {
			return cur.get(x.getVar());
		}
		Pair<List<S>, S> p = Map.get(x.f());
		if (p == null) {
			throw new RuntimeException("On " + x + ", missing symbol " + x.f() + " in Map " + Map);
		}
		List<S> sorts = p.first;
		int i = 0;
		for (KBExp<C, V> arg : x.getArgs()) {
			if (i >= sorts.size()) {
				throw new RuntimeException("On " + x + ", too many arguments: " + i + " >= " + sorts.size());
			}
			S s = sorts.get(i);
			if (arg.isVar()) {
				V v = arg.getVar();
				S s0 = cur.get(v);
				if (s0 == null) {
					cur.put(v, s);
				} else if (!s.equals(s0)) {
					throw new RuntimeException(
							"Variable " + arg + " requires two different sorts, " + s + " and " + s0);
				}
			} else {
				S s0 = typeInf(arg, Map, cur);
				if (!s.equals(s0)) {
					throw new RuntimeException(
							"Argument " + arg + " expected at sort " + s + " but actually at sort " + s0);
				}
			}
			i++;
		}

		return p.second;
	}

}
