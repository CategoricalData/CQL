package catdata;

import java.util.LinkedHashMap;
import java.util.Map;

public class Triple<S1, S2, S3> implements Comparable<Triple<S1, S2, S3>> {

	public Triple(S1 a, S2 b, S3 c) {
		first = a;
		second = b;
		third = c;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		result = prime * result + ((third == null) ? 0 : third.hashCode());
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
		Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		if (third == null) {
			if (other.third != null)
				return false;
		} else if (!third.equals(other.third))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "(" + first + ", " + second + ", " + third + ")";
	}

	public S1 first;
	public final S2 second;
	public S3 third;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public int compareTo(Triple<S1, S2, S3> o) {
		Comparable x = (Comparable) first;
		Comparable y = (Comparable) o.first;
		int c = x.compareTo(y);
		if (c == 0) {
			Comparable a = (Comparable) second;
			Comparable b = (Comparable) o.second;
			c = a.compareTo(b);
			if (c == 0) {
				Comparable i = (Comparable) third;
				Comparable j = (Comparable) o.third;
				return i.compareTo(j);
			}
			return c;

		}
		return c;

	}

	public static <K, A, B, C> Triple<LinkedHashMap<K, A>, LinkedHashMap<K, B>, LinkedHashMap<K, C>> proj(Map<K, Triple<A, B, C>> map) {
		LinkedHashMap<K, A> ret1 = new LinkedHashMap<>(map.size());
		LinkedHashMap<K, B> ret2 = new LinkedHashMap<>(map.size());
		LinkedHashMap<K, C> ret3 = new LinkedHashMap<>(map.size());
		for (var x : map.entrySet()) {
			ret1.put(x.getKey(), x.getValue().first);
			ret2.put(x.getKey(), x.getValue().second);
			ret3.put(x.getKey(), x.getValue().third);
		}
		return new Triple<>(ret1, ret2, ret3);
	}

	public Pair<S1, S2> first2() {
		return new Pair<>(first, second);
	}

	public Triple<S2, S1, S3> reverse12() {
		return new Triple<>(second, first, third);
	}
}
