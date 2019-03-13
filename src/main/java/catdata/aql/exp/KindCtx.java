package catdata.aql.exp;

import java.util.Map;
import java.util.Set;

import catdata.Util;
import catdata.aql.Kind;
import gnu.trove.set.hash.THashSet;

public class KindCtx<V,G,T,S,I,H,F,Q,P,C,SC,ED> {

	public final Map<V, G> gs = Util.mk();
	public final Map<V, T> tys = Util.mk();
	public final Map<V, S> schs = Util.mk();
	public final Map<V, I> insts =Util.mk();
	public final Map<V, H> trans = Util.mk();
	public final Map<V, F> maps = Util.mk();
	public final Map<V, Q> qs = Util.mk();
	public final Map<V, P> ps = Util.mk();
	public final Map<V, C> cs = Util.mk();
	public final Map<V, SC> scs = Util.mk();
	public final Map<V, ED> eds = Util.mk();
	
	
	
	
	public Map<V, ?> get(Kind kind) {
		switch (kind) {
		case INSTANCE:
			return insts;
		case MAPPING:
			return maps;
		case SCHEMA:
			return schs;
		case TRANSFORM:
			return trans;
		case TYPESIDE:
			return tys;
		case QUERY:
			return qs;
		case PRAGMA:
			return ps;
		case GRAPH:
			return gs;
		case COMMENT:
			return cs;
		case SCHEMA_COLIMIT:
			return scs;
		case CONSTRAINTS:
			return eds;
		default:
			throw new RuntimeException();
		}
	}
	
	public Object get(V k, Kind kind) {
		return get(kind).get(k);
	}
	
	@SuppressWarnings("unchecked")
	public void put(V k, Kind kind, Object o) {
		switch (kind) {
		case INSTANCE:
			insts.put(k, (I) o);
			break;
		case MAPPING:
			maps.put(k, (F) o);
			break;
		case SCHEMA:
			schs.put(k, (S) o);
			break;
		case TRANSFORM:
			trans.put(k, (H) o);
			break;
		case TYPESIDE:
			tys.put(k, (T)o);
			break;
		case QUERY:
			qs.put(k, (Q) o);
			break;
		case PRAGMA:
			ps.put(k, (P) o);
			break;
		case GRAPH:
			gs.put(k, (G) o);
			break;
		case COMMENT:
			cs.put(k, (C) o);
			break;	
		case SCHEMA_COLIMIT:
			scs.put(k, (SC) o);
			break;
		case CONSTRAINTS:
			eds.put(k, (ED) o);
			break;
		default:
			throw new RuntimeException();
		}
	}

	
	public Set<V> keySet() { 
		Set<V> ret = new THashSet<>();
		ret.addAll(insts.keySet());
		ret.addAll(maps.keySet());
		ret.addAll(ps.keySet());
		ret.addAll(qs.keySet());
		ret.addAll(schs.keySet());
		ret.addAll(trans.keySet());
		ret.addAll(tys.keySet());
		ret.addAll(qs.keySet()); 
		ret.addAll(gs.keySet());
		ret.addAll(cs.keySet());
		ret.addAll(scs.keySet());
		ret.addAll(eds.keySet());
		return ret;
	}

	public int size(Kind k) {
		switch (k) {
		case COMMENT:
			return cs.size();
		case CONSTRAINTS:
			return eds.size();
		case GRAPH:
			return gs.size();
		case INSTANCE:
			return insts.size();
		case MAPPING:
			return maps.size();
		case PRAGMA:
			return ps.size();
		case QUERY:
			return qs.size();
		case SCHEMA:
			return schs.size();
		case SCHEMA_COLIMIT:
			return scs.size();
		case TRANSFORM:
			return trans.size();
		case TYPESIDE:
			return tys.size();
		
		}
		return Util.anomaly();
	}
}
