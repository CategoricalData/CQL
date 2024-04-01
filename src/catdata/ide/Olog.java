package catdata.ide;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import catdata.Util;
import catdata.ide.Olog.OlogPresentation;
import catdata.ide.OlogMapping.OlogMappingPresentation;

//TODO: eprover only
//TODO: json format

public abstract class Olog {

	public static record OlogName(String ologName) {
		@Override
		public String toString() {
			return ologName;
		}
	};

	public static record OlogMappingName(String mappingName) {
		@Override
		public String toString() {
			return mappingName;
		}
	};

	public static record OlogEntity(OlogName ologName, String entityName) {
		@Override
		public String toString() {
			return entityName;
		}
	};

	public static record OlogFk(OlogEntity srcEntity, OlogEntity dstEntity, String arrowName) {
		@Override
		public String toString() {
			return arrowName;
		}
	};

	public static record OlogType(String typeName) {
		@Override
		public String toString() {
			return typeName;
		}
	};

	public static record OlogAttribute(OlogEntity srcEntity, OlogType type, String attributeName) {
		@Override
		public String toString() {
			return attributeName;
		}
	};

	public static record OlogUdf(OlogName ologName, List<OlogType> argTypes, OlogType returnType, String udfName) {
		@Override
		public String toString() {
			return udfName;
		}
	};

	////////////////////////////////////////////////////////////

	public static abstract class OlogTermEn {
		@Override
		public abstract boolean equals(Object o);

		@Override
		public abstract int hashCode();
	}

	public static abstract class OlogTermTy {
		@Override
		public abstract boolean equals(Object o);

		@Override
		public abstract int hashCode();
	}

	public static class OlogGenExp extends OlogTermEn {
		public final OlogEntity entity;
		public final String genName;

		@Override
		public int hashCode() {
			return Objects.hash(entity, genName);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			OlogGenExp other = (OlogGenExp) obj;
			return Objects.equals(entity, other.entity) && Objects.equals(genName, other.genName);
		}

		public OlogGenExp(OlogEntity entity, String genName) {
			this.entity = entity;
			this.genName = genName;
		}

		@Override
		public String toString() {
			return genName;
		}
	}

	public static class OlogFkExp extends OlogTermEn {
		public final OlogFk fk;
		public final OlogTermEn term;

		@Override
		public int hashCode() {
			return Objects.hash(fk, term);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			OlogFkExp other = (OlogFkExp) obj;
			return Objects.equals(fk, other.fk) && Objects.equals(term, other.term);
		}

		public OlogFkExp(OlogFk fk, OlogTermEn term) {
			this.fk = fk;
			this.term = term;
		}

		@Override
		public String toString() {
			return term + "." + fk.toString();
		}
	}

	public static class OlogSkExp extends OlogTermTy {
		public final OlogType type;
		public final String skName;

		public OlogSkExp(OlogType type, String skName) {
			this.type = type;
			this.skName = skName;
		}

		@Override
		public int hashCode() {
			return Objects.hash(skName, type);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			OlogSkExp other = (OlogSkExp) obj;
			return Objects.equals(skName, other.skName) && Objects.equals(type, other.type);
		}

		@Override
		public String toString() {
			return skName;
		}
	}

	public static class OlogAttExp extends OlogTermTy {
		public final OlogAttribute att;
		public final OlogTermEn term;

		@Override
		public int hashCode() {
			return Objects.hash(att, term);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			OlogAttExp other = (OlogAttExp) obj;
			return Objects.equals(att, other.att) && Objects.equals(term, other.term);
		}

		public OlogAttExp(OlogAttribute att, OlogTermEn term) {
			this.att = att;
			this.term = term;
		}

		@Override
		public String toString() {
			return term + "." + att;
		}
	}

	public static class OlogUdfExp extends OlogTermTy {
		public final OlogUdf udf;
		public final List<OlogTermTy> args;

		@Override
		public String toString() {
			return udf + "(" + Util.sep(args, ",") + ")";
		}

		@Override
		public int hashCode() {
			return Objects.hash(args, udf);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			OlogUdfExp other = (OlogUdfExp) obj;
			return Objects.equals(args, other.args) && Objects.equals(udf, other.udf);
		}

		public OlogUdfExp(OlogUdf udf, List<OlogTermTy> args) {
			this.udf = udf;
			this.args = args;
		}

	}

////////////////////////////////////////////////////////

	public static record OlogTableau(Set<OlogGenExp> gens, Set<OlogSkExp> sks, List<OlogTermEn> eqs1,
			List<OlogTermEn> eqs2, List<OlogTermTy> eqs3, List<OlogTermTy> eqs4) {

	};

	public static record OlogRule(OlogTableau all, OlogTableau ex, boolean uniq) {
		@Override
		public String toString() {
			return "forall " + all + " -> exists " + (uniq ? "unique " : "") + ex;
		}
	}

	public static record OlogPresentation(Set<OlogType> types, Set<OlogUdf> udfs, Set<OlogEntity> objects,
			Set<OlogFk> arrows, Set<OlogAttribute> attributes, Set<OlogRule> rules, Set<OlogGenExp> gens, Set<OlogSkExp> sks) {
	}

	public abstract OlogPresentation toPresentation();
	
	//////////////////////////////////////////////////////////////////////////////////////////
	
	public static class OlogLiteral extends Olog {
		public final OlogPresentation presentation;

		public OlogLiteral(OlogPresentation p) {
			this.presentation = p;
		}

		@Override
		public OlogPresentation toPresentation() {
			return presentation;
		}
	}

	public static class OlogCompose extends Olog {

		public OlogCompose(String s) {

		}

		@Override
		public OlogPresentation toPresentation() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	
	

}