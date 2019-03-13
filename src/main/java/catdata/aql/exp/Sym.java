package catdata.aql.exp;

import java.util.Map;

import catdata.Util;
import gnu.trove.map.hash.THashMap;

public class Sym {
	public final String str;
	
	private static Map<String, Sym> cache = new THashMap<>(128);
	
	public static synchronized Sym Sym(String str) {
		Sym sym = cache.get(str);
		if (sym != null) {
			return sym;
		}
		sym = new Sym(str);
		cache.put(str, sym);
		return sym;
	}

	private Sym(String str) {
		this.str = str;
	}

	 @Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this;
	} 
	

	@Override
	public String toString() {
		return Util.maybeQuote(str);
	}

}