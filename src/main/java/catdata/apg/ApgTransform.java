package catdata.apg;

import java.util.Map;
import java.util.Map.Entry;

import catdata.Pair;
import catdata.Util;
import catdata.aql.Kind;
import catdata.aql.Semantics;

public class ApgTransform <L1,E1,L2,E2> implements Semantics {
		
	public final ApgInstance<L1,E1> src;
	public final ApgInstance<L2,E2> dst;
	
	public final Map<L1, L2> lMap;
	public final Map<E1, E2> eMap;
	
	public ApgTransform(ApgInstance<L1, E1> src, ApgInstance<L2, E2> dst, Map<L1, L2> lMap,
			Map<E1, E2> eMap) {
		this.src = src;
		this.dst = dst;
		this.lMap = lMap;
		this.eMap = eMap;
		validate();
	}
	@Override
	public Kind kind() {
		return Kind.APG_morphism;
	}
	@Override
	public int size() {
		return src.size() + dst.size();
	}

	public void validate() {
		for (Entry<E1, Pair<L1, ApgTerm<L1, E1>>> e1 : src.Es.entrySet()) {
			L2 l2 = lMap.get(e1.getValue().first);
			if (l2 == null) {
				throw new RuntimeException("No mapping for label: " + e1.getValue().first);
			}
			if (!dst.Ls.containsKey(l2)) {
				throw new RuntimeException("Not a target label: " + l2);
			}
			E2 e2 = eMap.get(e1.getKey());
			if (e2 == null) {
				throw new RuntimeException("No mapping for element: " + e1.getKey());
			}
			Pair<L2, ApgTerm<L2, E2>> w = dst.Es.get(e2);
			if (w == null) {
				throw new RuntimeException("Not a target element: " + e2);
			}
			if (!l2.equals(w.first)) {
				throw new RuntimeException("On source element " + e1 + ", label via label-component is " + l2 + " but label via element-morphism is " + w.first);
			}
		}
	}
	
	public void assertNaturalData() {
		for (Entry<E1, Pair<L1, ApgTerm<L1, E1>>> e1 : src.Es.entrySet()) {
			ApgTerm<L2, E2> e2 = dst.Es.get(eMap.get(e1.getKey())).second;
			
			ApgTerm<L1, E2> w = e1.getValue().second.map(eMap::get);
			
			
			if (!w.equals(e2)) {
				throw new RuntimeException("On source element " + e1 + ", data via data-morphism is " + e2 + " but data via element-morphism is " + w);
			}
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((eMap == null) ? 0 : eMap.hashCode());
		result = prime * result + ((lMap == null) ? 0 : lMap.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
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
		ApgTransform<?, ?, ?, ?> other = (ApgTransform<?, ?, ?, ?>) obj;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		if (eMap == null) {
			if (other.eMap != null)
				return false;
		} else if (!eMap.equals(other.eMap))
			return false;
		if (lMap == null) {
			if (other.lMap != null)
				return false;
		} else if (!lMap.equals(other.lMap))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		String s = "labels\n\t" + Util.sep(lMap, " -> ", "\n\t") 
				+ "\nelements\n\t" + Util.sep(eMap, " -> ", "\n\t");
		
		return s ;
	}

	
}
