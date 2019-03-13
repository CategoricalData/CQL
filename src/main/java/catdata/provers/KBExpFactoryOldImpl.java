package catdata.provers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Util;
import gnu.trove.set.hash.THashSet;

public class KBExpFactoryOldImpl<T,C,V> implements KBExpFactory<T,C,V> {
	
	public static final KBExpFactoryOldImpl factory = new KBExpFactoryOldImpl<>();
	
	private KBExpFactoryOldImpl() { }
	
	/*static class NonConsExp<C, V> {
		final V v;
		final List<KBExp<C, V>> args;
		final C c;
		final int code;

		public NonConsExp(V v, C c, List<KBExp<C, V>> args) {
			this.v = v;
			this.args = args;
			this.c = c;
			this.code = hashCode2();
		}

		@Override
		public int hashCode() {
			return code;
		}

		public int hashCode2() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((args == null) ? 0 : args.hashCode());
			result = prime * result + ((c == null) ? 0 : c.hashCode());
			result = prime * result + ((v == null) ? 0 : v.hashCode());
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
			if (hashCode() != obj.hashCode()) {
				return false;
			}
			NonConsExp other = (NonConsExp) obj;

			if (v == null) {
				if (other.v != null)
					return false;
			} else if (!v.equals(other.v))
				return false;
			if (c == null) {
				if (other.c != null)
					return false;
			} else if (!c.equals(other.c))
				return false;
			if (args == null) {
				if (other.args != null)
					return false;
			} else if (!args.equals(other.args))
				return false;

			return true;

		}

	}*/

	

	//static Map<NonConsExp, WeakReference> cache = Collections.synchronizedMap(new THashMap<>());
	
	@Override
	public KBExp<C, V> KBApp(C c, List<KBExp<C, V>> in) {
		//synchronized (cache) {			
		//	NonConsExp<C, V> nce = new NonConsExp<>(null, c, in);
			//WeakReference<KBExp> ret = cache.get(nce);
			//if (ret != null) {
			////	KBExp x = ret.get();
			//	if (x != null) {
			//		return x;
			//	}
			//}
			KBApp<C,V> retX = new KBApp<>(c, in);
			//retX.back = nce;
			//cache.put(nce, new WeakReference(retX));
			return retX;		
		//}
		
	}

	
	public static class KBVar<C, V> implements KBExp<C, V> {
		public final V var;

		private KBVar(V var) {
			this.var = var;
		}
		
		

		@Override
		public KBExp<C, V> substitute(Map<V, KBExp<C, V>> sigma) {
			KBExp<C, V> ret = sigma.get(getVar());
			if (ret == null) {
				return this;
			}
			return ret;
		}

		@Override
		public KBExp<C, V> replace(List<Integer> l, KBExp<C, V> r) {
			if (l.isEmpty()) {
				return r;
			}
			throw new RuntimeException("Cannot replace");		
		}
		
		//private NonConsExp back;

		@Override
		public int hashCode() {
			return System.identityHashCode(this);
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}

		@Override
		public String toString() {
			return var.toString();
		}

		Set<V> vars;

		@Override
		public synchronized Set<V> getVars() {
			if (vars != null) {
				return vars;
			}
			vars = new THashSet<>(1);
			vars.add(var);
			return vars;
		}

		@Override
		public boolean isVar() {
			return true;
		}

		@Override
		public V getVar() {
			return var;
		}

		@Override
		public C f() {
			return Util.anomaly();
		}

		@Override
		public List<KBExp<C, V>> getArgs() {
			return Util.anomaly();
		}
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////////

	public static class KBApp<C, V> implements KBExp<C, V> {
		
		@Override
		public  KBExp<C, V> substitute(Map<V, KBExp<C, V>> sigma) {
			List<KBExp<C, V>> n = new ArrayList<>(getArgs().size());
			for (KBExp<C, V> arg : getArgs()) {
				n.add(arg.substitute(sigma));
			}
			return factory.KBApp(f(), n);
		}

		@Override
		public  KBExp<C, V> replace(List<Integer> l, KBExp<C, V> r) {
			if (l.isEmpty()) {
				return r;
			}
			Integer x = l.get(0);
			List<KBExp<C, V>> new_args = new ArrayList<>(getArgs().size());
			Iterator<KBExp<C, V>> it = getArgs().iterator();
			int i = 0;
			while (it.hasNext()) {
				KBExp<C, V> a = it.next(); 
				if (i == x) {
					a = a.replace(l.subList(1, l.size()), r);
				}
				new_args.add(a);
				i++;
			}
			return factory.KBApp(f(), new_args);
		}
		
		public final C f;
		public final List<KBExp<C, V>> args;

		
	
		Set<V> vars;

		@Override
		public synchronized Set<V> getVars() {
			if (vars != null) {
				return vars;
			}
			vars = (new THashSet<>());
			for (KBExp<C, V> arg : args) {
				arg.vars(vars);
			}
			return vars;
		}

	//	private NonConsExp back;
		
		KBApp(C f, List<KBExp<C, V>> args) {
			this.f = f;
			this.args = args;
			// hash = hash();
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(this);
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj;
			
		}

		@Override
		public String toString() {
			if (args.isEmpty()) {
				return f.toString();
			}
			return f + "(" + Util.sep(args, ",") + ")";
		}


		
		@Override
		public boolean isVar() {
			return false;
		}

		Collection<C> m;

		@Override
		public V getVar() {
			return Util.anomaly();
		}

		@Override
		public C f() {
			return f;
		}

		@Override
		public List<KBExp<C, V>> getArgs() {
			return args;
		}

	}

	@Override
	public KBExp<C, V> KBVar(V v) {
//		synchronized (cache) {
			//NonConsExp<C, V> nce = new NonConsExp<>(v, null, null);
			//WeakReference<KBExp> ret = cache.get(nce);
			//if (ret != null) {
			//	KBExp x = ret.get();
			//	if (x != null) {
			//		return x;
			//	}
			//}
			KBVar<C,V> retX = new KBVar<>(v);
			//retX.back = nce;
			//cache.put(nce, new WeakReference(retX));
			return retX;		
		//}
	}

	@Override
	public KBExp<C, V> freshConst(T ty, int n) {
		return Util.anomaly();
	}

	

	
	
}
