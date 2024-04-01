package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Kind;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;

public class EdsExpLearn<Gen, Sk, X, Y> extends EdsExp {
	InstExp<Gen, Sk, X, Y> I, J;

	public EdsExpLearn(InstExp<Gen, Sk, X, Y> i, InstExp<Gen, Sk, X, Y> j) {
		I = i;
		J = j;
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public boolean isVar() {
		return false;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(I.deps(), J.deps());
	}

	@Override
	public synchronized Constraints eval0(AqlEnv env, boolean isC) {
		var i = I.eval(env, isC);
		var j = J.eval(env, isC);
		List<ED> ls = new LinkedList<>();
		
		Map<String, Chc<String, String>> as = new HashMap<>();
		Map<String, Chc<String, String>> es = new HashMap<>();
		Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> awh = new HashSet<>();
		Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> ewh = new HashSet<>();
		
		int n = 0;
		int w = 0;
		Map<Object, Object> m = new HashMap<>();
		for (String en : i.algebra().schema().ens) {
			System.out.println(en);
			for (X x : i.algebra().en(en)) {
				System.out.println(x);
				as.put("v" + w, Chc.inRight(en));
				for (Att att : i.algebra().schema().attsFrom(en)) {
					Term<String, Void, Sym, Void, Void, Void, Y> t1 = i.algebra().att(att, x);
					if (t1.obj() != null && t1.obj() instanceof Optional && ((Optional)t1.obj()).isEmpty() ) {
						continue;
					}
					if (t1.obj() != null && t1.obj() instanceof Optional && !((Optional)t1.obj()).isEmpty() ) {
						var g = ((Optional)t1.obj()).get();
						if (g instanceof String) {
							String h = ((String) g).toLowerCase();
							//00:00.0 Y N NUll
							if ( h.equals("true") || h.equals("false") || h.equals("null") || h.equals("y") || h.equals("n") || h.equals("00:00.0")) {
								continue;
							}
						}
					}
					if (!m.containsKey(t1)) { //should be up to equiv
						m.put(t1, Term.Att(att, Term.Var("v" + w)));
					} else {
						awh.add(new Pair(Term.Att(att, Term.Var("v" + w)), m.get(t1)));
					}
				}
				w++;
			}
		}
	
		for (String en : i.algebra().schema().ens) {
			System.out.println("X" + en);
			for (X x : j.algebra().en(en)) {
				System.out.println("X" + x);
				es.put("v" + w, Chc.inRight(en));
				for (Att att : j.algebra().schema().attsFrom(en)) {
					Term<String, Void, Sym, Void, Void, Void, Y> t1 = j.algebra().att(att, x);
					if (t1.obj() != null && t1.obj() instanceof Optional && ((Optional)t1.obj()).isEmpty()) {
			//			ewh.add(new Pair<>(Term.Att(att, Term.Var("v" + w)), Term.Obj(Optional.empty(), j.schema().atts.get(att).second)));
					} else if (!m.containsKey(t1)) {
						m.put(t1, Term.Att(att, Term.Var("v" + w)));
					} else if (m.containsKey(t1)) {
						ewh.add(new Pair(Term.Att(att, Term.Var("v" + w)), m.get(t1)));
					} 
				}
				w++;
			}
		}
		
		
		
		ls.add(new ED(as, es, awh, ewh, false, env.defaults));
		
		var y = new Constraints(i.schema(), ls, env.defaults);
		return y;
	}

	@Override
	public String toString() {
		return "learn " + I + " " + J;
	}

	@Override
	public SchExp type(AqlTyping G) {
		var st = I.type(G);
		var s = J.type(G);
		if (!st.equals(s)) {
			throw new RuntimeException("In " + this + ", mapping source is " + st + " but target schema is " + s);
		}
		return st;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.mapSubExps(f);
		J.mapSubExps(f);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((I == null) ? 0 : I.hashCode());
		result = prime * result + ((J == null) ? 0 : J.hashCode());
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
		EdsExpLearn other = (EdsExpLearn) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		if (J == null) {
			if (other.J != null)
				return false;
		} else if (!J.equals(other.J))
			return false;
		return true;
	}

}