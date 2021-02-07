package easik.xml.xsd;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.net.URI;

/**
 * Class to handle a namespace
 *
 * Not particularly complete or functional. Probably need to revise and review
 * at some point.
 *
 * @author brett.giles@drogar.com Date: Aug 13, 2009 Time: 1:07:29 PM
 * @version $$Id$$
 */
public class XMLNameSpace {
	/**  */
	private final String ns;

	/**  */
	private final URI nsURI;

	/**
	 *
	 *
	 * @param ns
	 * @param nsURI
	 */
	public XMLNameSpace(final String ns, final URI nsURI) {
		this.ns = ns;
		this.nsURI = nsURI;
	}

	/**
	 *
	 *
	 * @return
	 */
	public String getNs() {
		return ns;
	}

	/**
	 *
	 *
	 * @return
	 */
	public URI getNsURI() {
		return nsURI;
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return (null == nsURI) ? "" : "xmlns:" + ns + "=\"" + nsURI.toString() + '"';
	}

	/**
	 *
	 *
	 * @param prepend
	 * @param append
	 *
	 * @return
	 */
	public String prettyString(final String prepend, final String append) {
		final String val = toString();

		return (val == null || val.length() == 0) ? "" : prepend + val + append;
	}
}
