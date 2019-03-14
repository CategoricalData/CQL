package catdata.aql.exp;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class InteriorLabel<X> {

	public final String section;

	public final X s;

	public Integer loc;

	private Function<X, String> toString;

	@SuppressWarnings("unchecked")
	public InteriorLabel<Object> conv() {
		return (InteriorLabel<Object>) this;
	}

	@Override
	public String toString() {
		return toString.apply(s);
	}

	public InteriorLabel(String section, X s, Integer loc, Function<X, String> toString) {
		this.s = s;
		this.loc = loc;
		this.toString = toString;
		this.section = section;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((s == null) ? 0 : s.hashCode());
		result = prime * result + ((section == null) ? 0 : section.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InteriorLabel<?> other = (InteriorLabel<?>) obj;
		if (s == null) {
			if (other.s != null)
				return false;
		} else if (!s.equals(other.s))
			return false;
		if (section == null) {
			if (other.section != null)
				return false;
		} else if (!section.equals(other.section))
			return false;
		return true;
	}

	public static List<InteriorLabel<Object>> imports(String section, List<LocStr> imports) {
		List<InteriorLabel<Object>> ret = new LinkedList<>();
		for (LocStr str : imports) {
			ret.add(new InteriorLabel<>(section, str.str, str.loc, x -> x.toString()));
		}
		return ret;
	}

}