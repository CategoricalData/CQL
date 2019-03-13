package catdata.graph;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import catdata.Util;
import gnu.trove.map.hash.THashMap;

class UnionFindSimple<X> {
    private final Map<X,X> id;    // id[i] = component identifier of i
    private int count;   // number of components

    public UnionFindSimple(Collection<X> xs) {
    	id = new THashMap<>(Util.id(xs));
    	count = xs.size();
    }

    public int count() {
        return count;
    }
  
    private X find(X p) {
        X x = id.get(p);
        if (x == null) {
        	count++;
        	id.put(p, p);
        	x = p;
        }
        return x;
    }


    public boolean connected(X p, X q) {
        return find(p).equals(find(q));
    }
  
    public void union(X p, X q) {
        X pID = find(p);   // needed for correctness
        X qID = find(q);   // to reduce the number of array accesses

        // p and q are already in the same component
        if (pID.equals(qID)) return;

        Set<X> set = id.keySet();
        for (X i : set) {
            if (find(i).equals(pID)) {
            	id.put(i, qID);
            }
        }
        count--;
    }

	@Override
	public String toString() {
		return "UnionFindSimple [id=" + id + ", count=" + count + "]";
	}

   

}
