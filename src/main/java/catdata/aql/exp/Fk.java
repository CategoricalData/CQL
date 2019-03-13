package catdata.aql.exp;

import java.util.Map;

import catdata.Util;
import gnu.trove.map.hash.THashMap;

public class Fk {
	public final String str;
	public final En en;

	private Fk(En en, String str) {
		this.str = str;
		this.en = en;
	}
	
	public synchronized static Fk Fk(En en, String str) {
		Map<String, Fk> m = SchExpRaw.fkCache.get(en);
		if (m == null) {
			m = new THashMap<>();
			SchExpRaw.fkCache.put(en, m);
		}
		Fk fk = m.get(str);
		if (fk != null) {
			return fk;
		}
		fk = new Fk(en, str);
		m.put(str, fk);
		return fk;
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
		return this == obj;
	}

	public String convert() {
		return str;
	}

}