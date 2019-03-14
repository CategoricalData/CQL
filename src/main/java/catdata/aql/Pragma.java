package catdata.aql;

public abstract class Pragma implements Semantics {

	@Override
	public Kind kind() {
		return Kind.PRAGMA;
	}

	public abstract void execute();

	@Override
	public abstract String toString();

	/**
	 * @return length of text string
	 */
	@Override
	public int size() {
		return toString().length();
	}

}
