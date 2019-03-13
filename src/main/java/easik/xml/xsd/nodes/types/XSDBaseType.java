package easik.xml.xsd.nodes.types;

//~--- non-JDK imports --------------------------------------------------------

//~--- JDK imports ------------------------------------------------------------
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import easik.xml.xsd.XMLNameSpace;
import easik.xml.xsd.XMLSchema;

/**
 * Corresponds to the "SimpleType" in XML Schema
 *
 * XML Schema has a plethora of simple types that can be used "as is" or
 * extended. This class provides a number of static objects, one for each of the
 * most likely used simple types.
 *
 * Note that not all are currently used.
 *
 * Also note that there is no tag name set here. The simple types are always
 * referenceble and therefore only appear in attributes in the form type='xxxx',
 * rather than as an XML element.
 *
 * @author brett.giles@drogar.com Date: Aug 14, 2009 Time: 10:36:00 AM
 * @version $$Id$$
 */
public class XSDBaseType extends XSDType {
	/**  */
	public final static XSDBaseType xsString = new XSDBaseType(XMLSchema.getXSDNameSpace(), "string", FacetEnum.LENGTH, FacetEnum.MINLENGTH, FacetEnum.MAXLENGTH);

	/**  */
	public final static XSDBaseType xsNormalizedString = new XSDBaseType(XMLSchema.getXSDNameSpace(), "normalizedString", FacetEnum.LENGTH, FacetEnum.MINLENGTH, FacetEnum.MAXLENGTH);

	/**  */
	public final static XSDBaseType xsIDREFS = new XSDBaseType(XMLSchema.getXSDNameSpace(), "IDREFS");

	/**  */
	public final static XSDBaseType xsIDREF = new XSDBaseType(XMLSchema.getXSDNameSpace(), "IDREF");

	/**  */
	public final static XSDBaseType xsID = new XSDBaseType(XMLSchema.getXSDNameSpace(), "ID");

	/**  */
	public final static XSDBaseType xsDuration = new XSDBaseType(XMLSchema.getXSDNameSpace(), "duration");

	/**  */
	public final static XSDBaseType xsDateTime = new XSDBaseType(XMLSchema.getXSDNameSpace(), "dateTime", FacetEnum.MININCLUSIVE, FacetEnum.MAXINCLUSIVE, FacetEnum.MINEXCLUSIVE, FacetEnum.MAXEXCLUSIVE);

	/**  */
	public final static XSDBaseType xsTime = new XSDBaseType(XMLSchema.getXSDNameSpace(), "time", FacetEnum.MININCLUSIVE, FacetEnum.MAXINCLUSIVE, FacetEnum.MINEXCLUSIVE, FacetEnum.MAXEXCLUSIVE);

	/**  */
	public final static XSDBaseType xsShort = new XSDBaseType(XMLSchema.getXSDNameSpace(), "short", FacetEnum.TOTALDIGITS, FacetEnum.MININCLUSIVE, FacetEnum.MAXINCLUSIVE, FacetEnum.MINEXCLUSIVE, FacetEnum.MAXEXCLUSIVE);

	/**  */
	public final static XSDBaseType xsLong = new XSDBaseType(XMLSchema.getXSDNameSpace(), "long", FacetEnum.TOTALDIGITS, FacetEnum.MININCLUSIVE, FacetEnum.MAXINCLUSIVE, FacetEnum.MINEXCLUSIVE, FacetEnum.MAXEXCLUSIVE);

	/**  */
	public final static XSDBaseType xsInteger = new XSDBaseType(XMLSchema.getXSDNameSpace(), "integer", FacetEnum.TOTALDIGITS, FacetEnum.MININCLUSIVE, FacetEnum.MAXINCLUSIVE, FacetEnum.MINEXCLUSIVE, FacetEnum.MAXEXCLUSIVE);

	/**  */
	public final static XSDBaseType xsInt = new XSDBaseType(XMLSchema.getXSDNameSpace(), "int", FacetEnum.TOTALDIGITS, FacetEnum.MININCLUSIVE, FacetEnum.MAXINCLUSIVE, FacetEnum.MINEXCLUSIVE, FacetEnum.MAXEXCLUSIVE);

	/**  */
	public final static XSDBaseType xsHexBinary = new XSDBaseType(XMLSchema.getXSDNameSpace(), "hexBinary");

	/**  */
	public final static XSDBaseType xsFloat = new XSDBaseType(XMLSchema.getXSDNameSpace(), "float", FacetEnum.MININCLUSIVE, FacetEnum.MAXINCLUSIVE, FacetEnum.MINEXCLUSIVE, FacetEnum.MAXEXCLUSIVE);

	/**  */
	public final static XSDBaseType xsDouble = new XSDBaseType(XMLSchema.getXSDNameSpace(), "double", FacetEnum.MININCLUSIVE, FacetEnum.MAXINCLUSIVE, FacetEnum.MINEXCLUSIVE, FacetEnum.MAXEXCLUSIVE);

	/**  */
	public final static XSDBaseType xsDecimal = new XSDBaseType(XMLSchema.getXSDNameSpace(), "decimal", FacetEnum.TOTALDIGITS, FacetEnum.FRACTIONDIGITS, FacetEnum.MININCLUSIVE, FacetEnum.MAXINCLUSIVE, FacetEnum.MINEXCLUSIVE, FacetEnum.MAXEXCLUSIVE);

	/**  */
	public final static XSDBaseType xsDate = new XSDBaseType(XMLSchema.getXSDNameSpace(), "date", FacetEnum.MININCLUSIVE, FacetEnum.MAXINCLUSIVE, FacetEnum.MINEXCLUSIVE, FacetEnum.MAXEXCLUSIVE);

	/**  */
	public final static XSDBaseType xsByte = new XSDBaseType(XMLSchema.getXSDNameSpace(), "byte", FacetEnum.TOTALDIGITS, FacetEnum.MININCLUSIVE, FacetEnum.MAXINCLUSIVE, FacetEnum.MINEXCLUSIVE, FacetEnum.MAXEXCLUSIVE);

	/**  */
	public final static XSDBaseType xsBoolean = new XSDBaseType(XMLSchema.getXSDNameSpace(), "boolean");

	/**  */
	private final Set<FacetEnum> facets;

	/**
	 *
	 *
	 * @param ns
	 * @param t
	 * @param allowedFacets
	 */
	protected XSDBaseType(final XMLNameSpace ns, final String t, final FacetEnum... allowedFacets) {
		super(ns, t);

		facets = EnumSet.noneOf(FacetEnum.class);

		facets.addAll(Arrays.asList(allowedFacets));
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public boolean isReferencable() {
		return true;
	}

	/**
	 *
	 *
	 * @param facet
	 *
	 * @return
	 */
	public boolean facetAllowed(final FacetEnum facet) {
		return facets.contains(facet);
	}

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public String getBody() {
		return "";
	}
}
