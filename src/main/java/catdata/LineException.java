
package catdata;

@SuppressWarnings("serial")
public class LineException extends RuntimeException {

	public final String decl;
	public final String kind;
	private final String str;

	public LineException(String string, String decl, String kind) {
		super(string == null ? "" : string);
		this.decl = decl == null ? "" : decl;
		this.kind = kind == null ? "" : kind;
		str = string == null ? "" : string;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LineException)) {
			return false;
		}
		// if (o == null) {
		// return false;
		// } //dead code, apparently
		LineException oe = (LineException) o;
		return (oe.decl.equals(decl) && oe.kind.equals(kind) && oe.str.equals(str));
	}

	@Override
	public int hashCode() {
		return 0;
	}

}
