package catdata.apg;

import java.util.List;

import catdata.Pair;
import catdata.Util;

public class ApgPreTerm {

	@Override
	public String toString() {
		if (str != null & ty == null) {
			return str;
		}
		if (str != null) {
			return str + "@" + ty;
		}
		if (fields != null) {
			return "(" + Util.sep(fields.iterator(), " , ", x -> x.first + ": " + x.second) + ")";
		}
		if (inj != null) {
			return "<" + inj + ": " + arg + ">"; // + Util.sep(m, ": ", " ");
		}
		if (proj != null) {
			return "." + proj + "(" + arg + ")";
		}
		if (deref != null) {
			return "!" + deref + "(" + arg + ")";
		}
		if (cases != null) {
			String s = "case " + arg + " where ";
			for (Pair<String, Pair<String, ApgPreTerm>> w : cases) {
				s += "\n" + w.first + " -> lambda " + w.second.first + ". " + w.second.second;
			}
			return s;
		}
		if (head != null) {
			return head + "(" + Util.sep(args, ", ") + ")";
		}
		return Util.anomaly();
	}

	private ApgPreTerm(String str, List<Pair<String, ApgPreTerm>> fields, ApgPreTerm inj, String field,
			ApgTy<Object> ty, String proj, String deref, List<Pair<String, Pair<String, ApgPreTerm>>> cases,
			String head, List<ApgPreTerm> args) {
		this.str = str;
		this.fields = fields;
		this.arg = inj;
		this.inj = field;
		this.ty = ty;
		this.proj = proj;
		this.deref = deref;
		this.cases = cases;
		this.head = head;
		this.args = args;
	}

	public static ApgPreTerm ApgPreTermStr(String str) {
		return new ApgPreTerm(str, null, null, null, null, null, null, null, null, null);
	}

	public static ApgPreTerm ApgPreTermTuple(List<Pair<String, ApgPreTerm>> fields) {
		return new ApgPreTerm(null, fields, null, null, null, null, null, null, null, null);
	}

	public static ApgPreTerm ApgPreTermInj(String field, ApgPreTerm inj) {
		return new ApgPreTerm(null, null, inj, field, null, null, null, null, null, null);
	}

	public static ApgPreTerm ApgPreTermBase(String str, ApgTy<Object> ty) {
		return new ApgPreTerm(str, null, null, null, ty, null, null, null, null, null);
	}

	public static ApgPreTerm ApgPreTermInjAnnot(String field, ApgPreTerm inj, ApgTy ty) {
		return new ApgPreTerm(null, null, inj, field, ty, null, null, null, null, null);
	}

	public static ApgPreTerm ApgPreTermProj(String proj, ApgPreTerm e) {
		return new ApgPreTerm(null, null, e, null, null, proj, null, null, null, null);
	}

	public static ApgPreTerm ApgPreTermDeref(String deref, ApgPreTerm e) {
		return new ApgPreTerm(null, null, e, null, null, null, deref, null, null, null);
	}

	public static ApgPreTerm ApgPreTermCase(ApgPreTerm x, List<Pair<String, Pair<String, ApgPreTerm>>> cases,
			ApgTy ty) {
		return new ApgPreTerm(null, null, x, null, ty, null, null, cases, null, null);
	}

	public static ApgPreTerm ApgPreTermApp(String head, List<ApgPreTerm> l) {
		return new ApgPreTerm(null, null, null, null, null, null, null, null, head, l);
	}

	public final ApgTy<Object> ty;
	public final String str;
	public final List<Pair<String, ApgPreTerm>> fields;

	public final List<ApgPreTerm> args;
	public final String head;

	public final ApgPreTerm arg;
	public final String inj;
	public final String proj;
	public final String deref;

	public final List<Pair<String, Pair<String, ApgPreTerm>>> cases;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((inj == null) ? 0 : inj.hashCode());
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		result = prime * result + ((arg == null) ? 0 : arg.hashCode());
		result = prime * result + ((str == null) ? 0 : str.hashCode());
		result = prime * result + ((ty == null) ? 0 : ty.hashCode());
		result = prime * result + ((proj == null) ? 0 : proj.hashCode());
		result = prime * result + ((deref == null) ? 0 : deref.hashCode());
		result = prime * result + ((cases == null) ? 0 : cases.hashCode());
		result = prime * result + ((args == null) ? 0 : args.hashCode());
		result = prime * result + ((head == null) ? 0 : head.hashCode());
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
		if (inj == null) {
			if (other.inj != null)
				return false;
		} else if (!inj.equals(other.inj))
			return false;
		if (cases == null) {
			if (other.cases != null)
				return false;
		} else if (!cases.equals(other.cases))
			return false;
		if (proj == null) {
			if (other.proj != null)
				return false;
		} else if (!proj.equals(other.proj))
			return false;
		if (deref == null) {
			if (other.deref != null)
				return false;
		} else if (!deref.equals(other.deref))
			return false;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		if (ty == null) {
			if (other.ty != null)
				return false;
		} else if (!ty.equals(other.ty))
			return false;
		if (arg == null) {
			if (other.arg != null)
				return false;
		} else if (!arg.equals(other.arg))
			return false;
		if (str == null) {
			if (other.str != null)
				return false;
		} else if (!str.equals(other.str))
			return false;
		if (args == null) {
			if (other.args != null)
				return false;
		} else if (!args.equals(other.args))
			return false;
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head))
			return false;
		return true;
	}

}
