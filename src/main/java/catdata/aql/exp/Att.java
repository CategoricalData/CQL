package catdata.aql.exp;

import java.util.Map;

import catdata.Util;
import gnu.trove.map.hash.THashMap;

public class Att  {
	public final String str;
	public final En en;
	
	public synchronized static Att Att(En en, String str) {
		Map<String, Att> m = SchExpRaw.attCache.get(en);
		if (m == null) {
			m = new THashMap<>();
			SchExpRaw.attCache.put(en, m);
		}
		Att att = m.get(str);
		if (att != null) {
			return att;
		}
		att = new Att(en, str);
		m.put(str, att);
		return att;
	}
	
	private Att(En en, String str) {
		this.str = str;
		this.en = en;
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
		return str;
	}
}