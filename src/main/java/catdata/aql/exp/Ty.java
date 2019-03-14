package catdata.aql.exp;

import java.util.Map;

import catdata.Util;
import gnu.trove.map.hash.THashMap;

public class Ty {
	public final String str;

	private static Map<String, Ty> cache = new THashMap<>(128);

	public static synchronized Ty Ty(String str) {
		Ty ty = cache.get(str);
		if (ty != null) {
			return ty;
		}
		ty = new Ty(str);
		cache.put(str, ty);
		return ty;
	}

	private Ty(String str) {
		this.str = str;
	}

	@Override
	public String toString() {
		return Util.maybeQuote(str);
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this;
	}

}