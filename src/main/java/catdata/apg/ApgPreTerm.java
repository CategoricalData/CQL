package catdata.apg;

import java.util.List;

import catdata.Pair;
import catdata.Util;

public class ApgPreTerm {

	

	@Override
	public String toString() {
		if (str != null) {
			return str;
		}
		if (fields != null) {
			return "(" + Util.sep(fields.iterator(), " , ", x->x.first + ": " + x.second) + ")";
		}
		if (field != null) {
			return "<" + field + ": " + inj + ">"; // + Util.sep(m, ": ", " ");
		}
		return Util.anomaly();}


	private ApgPreTerm(String str, List<Pair<String, ApgPreTerm>> fields, ApgPreTerm inj, String field) {
		this.str = str;
		this.fields = fields;
		this.inj = inj;
		this.field = field;
	}
	
	
	public static ApgPreTerm ApgPreTermStr(String str) {
		return new ApgPreTerm(str, null, null, null);
	}
	public static ApgPreTerm ApgPreTermTuple(List<Pair<String, ApgPreTerm>> fields) {
		return new ApgPreTerm(null, fields, null, null);
	}
	public static ApgPreTerm ApgPreTermInj(String field, ApgPreTerm inj) {
		return new ApgPreTerm(null, null, inj, field);
	}
	
	public final String str;
	public final List<Pair<String, ApgPreTerm>> fields;
	
	public final ApgPreTerm inj;
	public final String field;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		result = prime * result + ((inj == null) ? 0 : inj.hashCode());
		result = prime * result + ((str == null) ? 0 : str.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApgPreTerm other = (ApgPreTerm) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		if (inj == null) {
			if (other.inj != null)
				return false;
		} else if (!inj.equals(other.inj))
			return false;
		if (str == null) {
			if (other.str != null)
				return false;
		} else if (!str.equals(other.str))
			return false;
		return true;
	}
	
	
	
}
