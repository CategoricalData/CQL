package catdata.aql.exp;

import catdata.Util;

public class En {
	public final String str;

	private En(String str) {
		this.str = str;
	}

	public synchronized static En En(String str) {
		/* if (!str.equals(str.toLowerCase())) {
			Util.anomaly();
		} */
		En en = SchExpRaw.enCache.get(str);
		if (en != null) {
			return en;
		}
		en = new En(str);
		SchExpRaw.enCache.put(str, en);
		return en;
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

	public String convert() {
		return Util.maybeQuote(str);
	}
}