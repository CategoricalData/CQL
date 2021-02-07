package catdata;


public class UPair<T> {

	public final T a;
	public final T b;

	public UPair(T x, T y) {
		a = x;
		b = y;
	}

	@Override
	public String toString() {
		return "{" + a + ", " + b + "}";

	}

	private int code = -1;

	@Override
	public synchronized int hashCode() {
		if (code != -1) {
			return code;
		}
		final int prime = 31;
		int result = 1;
		result += prime * ((a == null) ? 0 : a.hashCode() + b.hashCode());
		code = result;
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
		UPair<?> other = (UPair<?>) obj;

		return (a.equals(other.a) && b.equals(other.b)) || (b.equals(other.a) && a.equals(other.b));
	}

	

}
