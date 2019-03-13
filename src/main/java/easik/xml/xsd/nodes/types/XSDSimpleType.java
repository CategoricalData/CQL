package easik.xml.xsd.nodes.types;

/**
 * Created only to hold restrictions
 *
 * Note that generally simple types can have much more
 *
 * @author gilesb
 * @since 12-Sep-2009 Time: 10:59:08 AM
 */
public class XSDSimpleType extends XSDType {
	/**  */
	private XSDRestriction restr;

	/**
	 *
	 */
	public XSDSimpleType() {
		this(null, null);
	}

	/**
	 *
	 *
	 * @param restr
	 */
	public XSDSimpleType(final XSDRestriction restr) {
		this(null, restr);
	}

	/**
	 *
	 *
	 * @param name
	 * @param restr
	 */
	public XSDSimpleType(final String name, final XSDRestriction restr) {
		super(name);

		setTagName("simpleType");

		this.restr = restr;
	}

	/**
	 *
	 *
	 * @return
	 */
	public XSDRestriction getRestr() {
		return restr;
	}

	/**
	 *
	 *
	 * @param restr
	 */
	public void setRestr(final XSDRestriction restr) {
		this.restr = restr;
	}

	/**
	 * Always false, used only for inline restrictions
	 * 
	 * @return false
	 */
	@Override
	public boolean isReferencable() {
		return false;
	}

	/**
	 * Body of the tag, called by toString()
	 *
	 * @return A string containing the body.
	 */
	@Override
	public String getBody() {
		if (null == restr) {
			return null;
		} 
			return restr.toString();
		
	}
}
