package catdata.ide;

@FunctionalInterface
public interface Disp {

	void close();

	default Throwable exn() {
		return null;
	}

}
