package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import catdata.InteriorLabel;
import catdata.Pair;
import catdata.Raw;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.TypeSide;
import gnu.trove.set.hash.THashSet;

public final class TyExpAdt extends TyExp implements Raw {
	
	public static void main(String[] args) {
		Set<String> s = new THashSet<>();
	//	s.add("Person");
	//	s.add("Color");
		Set<Pair<String,String>> c = new THashSet<>();
		//c.add(new Pair<>("red"  , "Color"));
		//c.add(new Pair<>("blue" , "Color"));
		//c.add(new Pair<>("green", "Color"));
	//	c.add(new Pair<>("alice", "Person"));
		List<Triple<String,String,Boolean>> t = new LinkedList<>();
		t.add(new Triple<>("1", "1", true));
		
		System.out.println(new Cat(s, c, t).toString());
	}

	@Override
	public Collection<Exp<?>> imports() {
		return Collections.emptySet();
	}

	@Override
	public <R, P, E extends Exception> TyExp coaccept(P params, TyExpCoVisitor<R, P, E> v, R r) throws E {
		return v.visitTyExpAdt(params, r);
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, TyExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return Collections.emptyMap();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Collections.emptySet();
	}

	
	
	public final Map<String, String> options;
	public final List<String> base;
	public final List<Pair<String, String>> values;
	public final List<Triple<String, String, Boolean>> types;
	

	public TyExpAdt(Map<String, String> options, List<String> base, List<Pair<String, String>> values,
			List<Triple<String, String, Boolean>> types) {
		this.options = options;
		this.base = base;
		this.values = values;
		this.types = types;
	}

	
	@Override
	public Map<String, String> options() {
		return options;
	}

	private static String quote(String s) {
		if (s.contains(" ")) {
			return "\"" + s + "\"";
		}
		return s;
	}
	
	private static class Cat {	
	
		Map<String, Set<String>> arrs = Util.mk();
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("schema S = literal : empty {\n");
			sb.append("\tentities\n\t\tE T L V\n");
			sb.append("\tforeign_keys\n");
			sb.append("\t\t typeOf : V -> T\n");
			sb.append("\t\t typeOf : L -> T\n");
			sb.append("\t\tlabelOf : E -> L\n");
			sb.append("\t\tvalueOf : E -> V\n");
			sb.append("\tpath_equations\n");
			sb.append("\t\tE.labelOf.typeOf = E.valueOf.typeOf\n}\n\n");
			sb.append("instance G = literal : S {\n\tgenerators\n");
			for (String t : arrs.keySet()) {
				for (String x : arrs.get(t)) {
					sb.append("\t\t");
					sb.append(quote(x));
					sb.append(" : V\n");	
				}
			}
			for (String x : arrs.keySet()) {
				sb.append("\t\t");
				sb.append(quote(x));
				sb.append(" : T\n");
			}
			sb.append("\tequations\n");
			for (String t : arrs.keySet()) {
				for (String x : arrs.get(t)) {
					sb.append("\t\t");
					sb.append(quote(x));
					sb.append(".typeOf = " + quote(t));
					sb.append("\n");	
				}
			}
			sb.append("\toptions\n\t\tinterpret_as_algebra = true\n}\n");
			
			return sb.toString();
		}
		
	
		public Cat(Set<String> base, Set<Pair<String, String>> consts, List<Triple<String, String, Boolean>> types) {
			arrs.put("0", Collections.emptySet());
			arrs.put("1", Collections.singleton("tt"));
			for (String x : base) {
				arrs.put(x, new THashSet<>());
			}
			for (Pair<String, String> x : consts) {
				arrs.get(x.second).add(x.first);
			}
			for (Triple<String, String, Boolean> x : types) {
				Set<String> set = new THashSet<>();
				if (x.third) {
					arrs.put("(" + x.first + " * " + x.second + ")", set);
					for (String a : arrs.get(x.first)) {
						for (String b : arrs.get(x.second)) {
							set.add("(" + a + ", " + b + ")");
						}
					}
				} else {
					arrs.put("(" + x.first + " + " + x.second + ")", set);
					for (String a : arrs.get(x.first)) {
						set.add("inl_" + x.second + " " + a);
					}
					for (String b : arrs.get(x.second)) {
						set.add("inr_" + x.first + " " + b);
					}
				}
			}
		}
			

	}
	
	@Override
	public synchronized TypeSide<Ty, Sym> eval0(AqlEnv env, boolean isC) {
		AqlOptions ops = new AqlOptions(options, null, env.defaults);
		
		TypeSide<Ty, Sym> ret = null; 

		return ret;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public Object type(AqlTyping G) {
		return Unit.unit;
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((types == null) ? 0 : types.hashCode());
		result = prime * result + ((values == null) ? 0 : values.hashCode());
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
		TyExpAdt other = (TyExpAdt) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

}
