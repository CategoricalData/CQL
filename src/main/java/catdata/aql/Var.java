package catdata.aql;

import gnu.trove.map.hash.THashMap;

public final class Var implements Comparable<Var> {

	public final String var;

	static THashMap<String, Var> cache = new THashMap<>();
	
	public static synchronized Var Var(String var) {
		Var v = cache.get(var);
		if (v != null) {
			return v;
		}
		v = new Var(var);
		cache.put(var, v);
		return v;
	}
	
	private Var(String var) {
		this.var = var;
	}
	
	@Override
	public String toString() {
		return var;
	}
	
	
	
	@Override
	public int hashCode() {
		return var.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Var other = (Var) obj;
        return var.equals(other.var);
    }

	@Override
	public int compareTo(Var o) {
		return this.var.compareTo(o.var);
	}
	
	
	
}
