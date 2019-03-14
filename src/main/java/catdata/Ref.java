package catdata;

public class Ref<X> {

	public volatile X x;

	private volatile int var; // needed to make sure refs don't compare to equal just because their referents
								// are equal. could have not used value eauality, but var aso useful for
								// printing

	private static int count = 0;

	public Ref() {
		var = count++;
	}

	public synchronized boolean isSet() {
		return x != null;
	}

	public Ref(X x) {
		if (x == null) {
			throw new RuntimeException("Anomaly, please report");
		}
		this.x = x;
	}

	public synchronized void set(X x) {
		if (x == null) {
			throw new RuntimeException("Anomaly, please report");
		}
		if (this.x == null) {
			this.x = x;
			var = -1;
		}
		if (!this.x.equals(x)) {
			throw new RuntimeException("Reference already set to " + this.x + ", cannot set to " + x);
		}
	}

	public synchronized void set(Ref<X> ref) {
		if (ref == null) {
			throw new RuntimeException("Anomaly, please report");
		}
		if (ref.x != null) {
			set(ref.x);
		} else {
			count = ref.var;
		}
	}

	@Override
	public String toString() {
		return x == null ? "?" + var : x.toString();
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + var;
		result = prime * result + ((x == null) ? 0 : x.hashCode());
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
		Ref<?> other = (Ref<?>) obj;
		if (var != other.var)
			return false;
		if (x == null) {
			if (other.x != null)
				return false;
		} else if (!x.equals(other.x))
			return false;
		return true;
	}

}
