package easik.xml.xsd.nodes.types;

//~--- non-JDK imports --------------------------------------------------------

import easik.xml.xsd.XSDGeneralTag;

/**
 * Enumeration of the "facets" applicable to extending simple types in XML
 * Schema
 * <p/>
 * As with all the classes in Easik, this is purpose built to only include the
 * ones we would possibly use when creating a restriction type for an attribute
 * of a Node.
 * <p/>
 * <p/>
 * Facets not included - xs:enumeration, xs:pattern, xs:whiteSpace
 *
 * @author gilesb Date: 16-Aug-2009 Time: 4:39:06 PM
 * @see easik.xml.xsd.nodes.types.XSDRestriction
 * @see XSDBaseType
 * @see easik.database.types.Char
 * @see easik.database.types.Decimal
 * @see easik.database.types.Varchar
 */
public enum FacetEnum {
	LENGTH {
		/**
		 *
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return "length";
		}
	},
	MINLENGTH {
		/**
		 *
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return "minLength";
		}
	},
	MAXLENGTH {
		/**
		 *
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return "maxLength";
		}
	},
	TOTALDIGITS {
		/**
		 *
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return "totalDigits";
		}
	},
	FRACTIONDIGITS {
		/**
		 *
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return "fractionDigits";
		}
	},
	MININCLUSIVE {
		/**
		 *
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return "minExclusive";
		}
	},
	MAXINCLUSIVE {
		/**
		 *
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return "maxExclusive";
		}
	},
	MINEXCLUSIVE {
		/**
		 *
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return "minExclusive";
		}
	},
	MAXEXCLUSIVE {
		/**
		 *
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return "maxExclusive";
		}
	};

	/**
	 *
	 *
	 * @return
	 */
	@Override
	public abstract String toString();

	/**
	 *
	 *
	 * @param attrValValue
	 *
	 * @return
	 */
	public XSDGeneralTag getAsTag(final String attrValValue) {
		return new XSDGeneralTag(this.toString(), "value", attrValValue);
	}

	/**
	 *
	 *
	 * @param t
	 *
	 * @return
	 */
	public boolean isAllowedWithType(final XSDBaseType t) {
		return t.facetAllowed(this);
	}
}
