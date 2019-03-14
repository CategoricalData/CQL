package catdata;

@SuppressWarnings("serial")
public class RuntimeInterruptedException extends RuntimeException {

	private final Exception ex;

	public RuntimeInterruptedException(InterruptedException ie) {
		ex = ie;
	}

	public RuntimeInterruptedException(@SuppressWarnings("unused") ThreadDeath ie) {
		ex = new RuntimeException("Thread death");
	}

	@Override
	public String toString() {
		return ex.toString();
	}

	@Override
	public void printStackTrace() {
	}

}
