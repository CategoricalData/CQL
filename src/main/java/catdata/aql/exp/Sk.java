package catdata.aql.exp;

import catdata.Util;

public class Sk {
	public final String str;

	public synchronized static Sk Sk(String str) {
		Sk sk = InstExpRaw.skCache.get(str);
		if (sk != null) {
			return sk;
		}
		sk = new Sk(str);
		InstExpRaw.skCache.put(str, sk);
		return sk;
	}

	private Sk(String str) {
		this.str = str;
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