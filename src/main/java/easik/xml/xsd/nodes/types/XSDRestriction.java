package easik.xml.xsd.nodes.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.List;

import easik.xml.xsd.XMLNameSpace;

/**
 * XML Schema restrictions (extensions) of base types.
 * <p/>
 * Note that we extend XSDtype directly and only reference the XSDBaseType
 *
 * @author gilesb Date: 16-Aug-2009 Time: 4:15:40 PM
 */
public class XSDRestriction extends XSDType {
	/**
	 * The facets we restrict on
	 */
	private List<FacetEnum> facets;

	/**
	 * The string values of the restrictions
	 */
	private List<String> restrictionValues;

	/**
	 *
	 *
	 * @param t
	 * @param base
	 * @param facet
	 * @param restr
	 */
	public XSDRestriction(final String t, final XSDBaseType base, final FacetEnum facet, final String restr) {
		super(base.getNs(), t);

		setBase(base);

		facets = new ArrayList<>(2);

		facets.add(facet);

		restrictionValues = new ArrayList<>(2);

		restrictionValues.add(restr);
		setTagName("restriction");
		removeTagAttribute("name");
	}

	/**
	 *
	 *
	 * @param ns
	 * @param t
	 * @param base
	 * @param facet
	 * @param restr
	 */
	public XSDRestriction(final XMLNameSpace ns, final String t, final XSDBaseType base, final FacetEnum facet,
			final String restr) {
		super(ns, t);

		setBase(base);

		facets = new ArrayList<>(2);

		facets.add(facet);

		restrictionValues = new ArrayList<>(2);

		restrictionValues.add(restr);
		setTagName("restriction");
		removeTagAttribute("name");
	}

	/**
	 *
	 *
	 * @param base
	 */
	protected void setBase(final XSDBaseType base) {
		if (null == base) {
			return;
		}

		addTagAttribute("base", base.getQualifiedName());
	}

	/**
	 * Chosen never to be referencable. Simply a design choice, always used as an
	 * inline anonymous type.
	 *
	 * @return false.
	 */
	@Override
	public boolean isReferencable() {
		return false;
	}

	/**
	 * Add a facet and its restriction value
	 *
	 * @param facet The facet
	 * @param value The restriction value
	 * @todo add check that the facet is in the base.
	 * @todo (bigger) add check that value is OK for facet.
	 */
	public void addFacet(final FacetEnum facet, final String value) {
		facets.add(facet);
		restrictionValues.add(value);
	}

	/**
	 * Return the restriction body
	 *
	 * This consists of creating tags for each facet.
	 * 
	 * @see easik.xml.xsd.XSDGeneralTag
	 * @return the restriction body.
	 */
	@Override
	public String getBody() {
		final StringBuilder ret = new StringBuilder();

		for (int i = 0; i < facets.size(); i++) {
			ret.append(facets.get(i).getAsTag(restrictionValues.get(i)));

			if (i < facets.size() - 1) {
				ret.append(lineSep);
			}
		}

		return ret.toString();
	}
}
