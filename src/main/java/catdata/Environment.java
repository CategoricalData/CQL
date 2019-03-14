package catdata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

//TODO aql replace with Ctx
public class Environment<X> {

	private final Map<String, X> objs = Collections.synchronizedMap(new LinkedHashMap<>());

	public Set<String> keys() {
		return objs.keySet();
	}

	public synchronized void put(String k, X v) {
		if (k == null || v == null) {
			throw new RuntimeException();
		}
		if (objs.containsKey(k)) {
			throw new RuntimeException("Duplicate name: " + k);
		}
		objs.put(k, v);
	}

	public synchronized X get(String name) {
		X ret = objs.get(name);
		if (ret == null) {
			throw new RuntimeException("Unbound variable: " + name);
		}
		return ret;
	}

}
