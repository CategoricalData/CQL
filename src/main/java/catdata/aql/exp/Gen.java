package catdata.aql.exp;

import catdata.Util;

public class Gen {
	public final String str;

	private Gen(String str) {
		this.str = str;
	}

	public synchronized static Gen Gen(String str) {
		Gen gen = InstExpRaw.genCache.get(str);
		if (gen != null) {
			return gen;
		}
		gen = new Gen(str);
		InstExpRaw.genCache.put(str, gen);
		return gen;
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
		return Util.maybeQuote(str);
	}

}