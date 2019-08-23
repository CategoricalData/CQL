package catdata.apg;

import java.util.Map;
import java.util.function.Function;

import catdata.Pair;
import catdata.Util;
import catdata.aql.Kind;
import catdata.aql.Semantics;

public class ApgTypeside implements Semantics {

	public ApgTypeside(Map<String, Pair<Class<?>, Function<String, Object>>> tys) {
		this.Bs = tys;
	}
	
	//equality 
	public final Map<String, Pair<Class<?>, Function<String, Object>>> Bs;
	

	@Override
	public Kind kind() {
		return Kind.APG_typeside;
	}
	@Override
	public int size() {
		return Bs.size();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Bs == null) ? 0 : Bs.hashCode());
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
		ApgTypeside other = (ApgTypeside) obj;
		if (Bs == null) {
			if (other.Bs != null)
				return false;
		} else if (!Bs.equals(other.Bs))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return Util.sep(Bs, " -> ", "\n", x -> x.first.toString());
	}
	
	
	
}
