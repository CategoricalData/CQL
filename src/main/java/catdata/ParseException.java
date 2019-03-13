package catdata;

public class ParseException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final int column;
	public final int line;
	
	public ParseException(int column, int line, Exception ex) {
		super(ex);
		this.column = column;
		this.line = line;
	}
	
	
	
}
