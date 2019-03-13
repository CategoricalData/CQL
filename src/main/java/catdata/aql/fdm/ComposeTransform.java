package catdata.aql.fdm;

import java.util.Map;

import catdata.aql.Instance;
import catdata.aql.Term;
import catdata.aql.Transform;
import gnu.trove.map.hash.THashMap;

public class ComposeTransform<Ty,En,Sym,Fk,Att,Gen1,Sk1,Gen2,Sk2,X1,Y1,X2,Y2,Gen3,Sk3,X3,Y3> 
	extends Transform<Ty,En,Sym,Fk,Att,Gen1,Sk1,Gen3,Sk3,X1,Y1,X3,Y3> {
	
	private final Transform<Ty,En,Sym,Fk,Att,Gen1,Sk1,Gen2,Sk2,X1,Y1,X2,Y2> t1;
	private final Transform<Ty,En,Sym,Fk,Att,Gen2,Sk2,Gen3,Sk3,X2,Y2,X3,Y3> t2;

	private final Map<Gen1, Term<Void, En, Void, Fk, Void, Gen3, Void>> gens = new THashMap<>();
	private final Map<Sk1, Term<Ty, En, Sym, Fk, Att, Gen3, Sk3>> sks = new THashMap<>();

	public ComposeTransform(Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t1, Transform<Ty, En, Sym, Fk, Att, Gen2, Sk2, Gen3, Sk3, X2, Y2, X3, Y3> t2) {
		this.t1 = t1;
		this.t2 = t2;
		if (!t1.dst().equals(t2.src())) {
			throw new RuntimeException("Anomaly: in compose transform, dst of t1 is \n\n" + t1.dst() +" \n\n but src of t2 is \n\n" + t2.src());
		}
		for (Gen1 gen1 : src().gens().keySet()) {
			gens.put(gen1, t2.trans(t1.gens().get(gen1).convert()).convert());
		}
		for (Sk1 sk1 : src().sks().keySet()) {
			sks.put(sk1, t2.trans(t1.sks().get(sk1)));
		}
	}

	@Override
	public Map<Gen1, Term<Void, En, Void, Fk, Void, Gen3, Void>> gens() {
		return gens;
	}

	@Override
	public Map<Sk1, Term<Ty, En, Sym, Fk, Att, Gen3, Sk3>> sks() {
		return sks;
	}

	@Override
	public Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src() {
		return t1.src();
	}

	@Override
	public Instance<Ty, En, Sym, Fk, Att, Gen3, Sk3, X3, Y3> dst() {
		return t2.dst();
	}
	
	
}
