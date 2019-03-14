package catdata.aql.exp;

import java.util.List;
import java.util.Map;

public interface Raw {

	public Map<String, List<InteriorLabel<Object>>> raw();

	public default int find(String section, Object o) {
		for (InteriorLabel<Object> il : raw().get(section)) {
			if (il.s.equals(o)) {
				return il.loc;
			}
		}
		throw new RuntimeException("For section " + section + ", cannot find " + o);
	}

}
