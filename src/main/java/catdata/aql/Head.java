package catdata.aql;

import java.util.Map;

import catdata.Pair;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.strategy.HashingStrategy;

public class Head<Ty, En, Sym, Fk, Att, Gen, Sk>  {

	protected static enum WH {
		SYM, FK, ATT, GEN, SK, OBJ
	};
	
	private final Object object;
	private final WH which;
	
/*	private final Sym sym;
	private final Fk fk;
	private final Att att;
	private final Gen gen;
	private final Sk sk;
	private final Object obj;
	private final Ty ty; */
	

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Head<Ty, En, Sym, Fk, Att, Gen, Sk> SymHead(Sym sym) {
		return mkHead(sym, null, null, null, null, null, null);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Head<Ty, En, Sym, Fk, Att, Gen, Sk> FkHead(Fk fk) {
		return mkHead(null, fk, null, null, null, null, null);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Head<Ty, En, Sym, Fk, Att, Gen, Sk> AttHead(Att att) {
		return mkHead(null, null, att, null, null, null, null);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Head<Ty, En, Sym, Fk, Att, Gen, Sk> GenHead(Gen gen) {
		return mkHead(null, null, null, gen, null, null, null);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Head<Ty, En, Sym, Fk, Att, Gen, Sk> SkHead(Sk sk) {
		return mkHead(null, null, null, null, sk, null, null);
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Head<Ty, En, Sym, Fk, Att, Gen, Sk> ObjHead(Object obj, Ty ty) {
		return mkHead(null, null, null, null, null, obj, ty);
	}

	private static HashingStrategy<Head> strategy = new HashingStrategy<>() {
		@Override
		public int computeHashCode(Head t) {
			return t.hashCode2();
		}

		@Override
		public boolean equals(Head s, Head t) {
			return s.equals2(t);
		}
	};
		


	static Map<Head, Head> cache = (new TCustomHashMap<>(strategy));

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized static <Ty, En, Sym, Fk, Att, Gen, Sk> Head<Ty, En, Sym, Fk, Att, Gen, Sk> mkHead(Sym sym,
			Fk fk, Att att, Gen gen, Sk sk, Object obj, Ty ty) {
		WH o;
		Object x;
		if (gen != null) {
			o = WH.GEN;
			x = gen;
		} else if (sk != null) {
			o = WH.SK;
			x = sk;
		} else if (fk != null) {
			o = WH.FK;
			x = fk;
		} else if (att != null) {
			o = WH.ATT;
			x = att;
		} else if (sym != null) {
			o = WH.SYM;
			x = sym;
		} else if (obj != null) {
			o = WH.OBJ;
			x = new Pair(obj, ty);
		} else {
			throw new RuntimeException("|| " + fk + " " + att + " " + gen + " " + sk + " " + obj + " " + ty);
		}
		Head h = new Head<>(x, o); 
		Head h2 = cache.get(h);
		if (h2 != null) {
			return h2;
		}
		//h.back = p;
		cache.put(h, h);
		return h;
	}
	
	protected Head(Object o, WH wh) {
		this.object = o;
		this.which = wh;
	//	this.code = hashCode2();
	}
	//private final int code;

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Head<Ty, En, Sym, Fk, Att, Gen, Sk> mkHead(
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		return mkHead(term.sym(), term.fk(), term.att(), term.gen(), term.sk(), term.obj(), term.ty());
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Head<Ty, En, Sym, Fk, Att, Gen, Sk> mkHead(Object obj, Ty ty) {
		return mkHead(null, null, null, null, null, obj, ty);
	}

	@Override
	public boolean equals(Object x) { 
		return this == x;
	}

	public boolean equals2(Object x) { 
		Head<Ty, En, Sym, Fk, Att, Gen, Sk> o = (Head<Ty, En, Sym, Fk, Att, Gen, Sk>) x;
		//if (code != o.code) {
		//	return false;
		//}
		if (!which.equals(o.which)) {
			return false;
		}
		return object.equals(o.object);
	} 
	
	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
	
	public int hashCode2() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result + ((which == null) ? 0 : which.hashCode());
		return result;
	}

	@Override
	public String toString() {
		if (att() != null) {
			return att().toString();
		} else if (fk() != null) {
			return fk().toString();
		} else if (sym() != null) {
			return sym().toString();
		} else if (gen() != null) {
			return gen().toString();
		} else if (sk() != null) {
			return sk().toString();
		} else if (obj() != null) {
			return obj() + "@" + ty();
		}
		throw new RuntimeException("Anomaly: please report");
	}

	///////////////////////

	
	
//	 @Override
//	public void finalize() {
//		synchronized (Term.class) {
//			cache.remove(back);
////		}
	//} 

	public Sym sym() {
		if (which != WH.SYM) {
			return null;
		}
		return (Sym) object;
	}

	public Fk fk() {
		if (which != WH.FK) {
			return null;
		}
		return (Fk) object;
	}

	public Att att() {
		if (which != WH.ATT) {
			return null;
		}
		return (Att) object;
	}

	public Gen gen() {
		if (which != WH.GEN) {
			return null;
		}
		return (Gen) object;
	}

	public Sk sk() {
		if (which != WH.SK) {
			return null;
		}
		return (Sk) object;
	}

	public Object obj() {
		if (which != WH.OBJ) {
			return null;
		}
		return ((Pair)object).first;
	}

	public Ty ty() {
		if (which != WH.OBJ) {
			return null;
		}
		return (Ty) ((Pair)object).second;
	}

}
