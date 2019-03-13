package catdata.aql.fdm;

import java.util.Map;
import java.util.Map.Entry;

import catdata.Pair;
import catdata.aql.Instance;
import catdata.aql.Mapping;
import catdata.aql.Term;
import catdata.aql.Transform;
import gnu.trove.map.hash.THashMap;

public class DeltaTransform<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1, En2, Fk2, Att2, Gen2, Sk2, X1, Y1, X2, Y2>
extends Transform<Ty, En1, Sym, Fk1, Att1, Pair<En1, X1>, Y1, Pair<En1, X2>, Y2, Pair<En1, X1>, Y1, Pair<En1, X2>, Y2> {

	private final Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> F;
	@SuppressWarnings("unused")
	private final Transform<Ty, En2, Sym, Fk2, Att2, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h;
	
	private final DeltaInstance<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1, En2, Fk2, Att2, X1, Y1> src;
	private final DeltaInstance<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2, En2, Fk2, Att2, X2, Y2> dst;

	private final Map<Pair<En1, X1>, Term<Void, En1, Void, Fk1, Void, Pair<En1, X2>, Void>> gens = new THashMap<>();
	private final Map<Y1, Term<Ty, En1, Sym, Fk1, Att1, Pair<En1, X2>, Y2>> sks = new THashMap<>();
	
	public DeltaTransform(Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> f, Transform<Ty, En2, Sym, Fk2, Att2, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h) {
		if (!h.src().schema().equals(f.dst)) {
			throw new RuntimeException("Target of mapping is " + f.dst + " but instances are on " + h.src().schema());
		}
        F = f;
		this.h = h;
		src = new DeltaInstance<>(f, h.src());
		dst = new DeltaInstance<>(f, h.dst());
							
		for (Entry<Pair<En1, X1>, En1> gen1 : src.gens().entrySet()) {
			gens.put(gen1.getKey(), Term.Gen(new Pair<>(gen1.getKey().first, h.repr(F.ens.get(gen1.getValue()), gen1.getKey().second)))); 
		}
		for (Y1 sk1 : src.sks().keySet()) {
			sks.put(sk1, h.dst().algebra().intoY(h.reprT(sk1)).convert()); 
		}
		
		validate(true);
	}
	
	
	
	@Override
	public Map<Pair<En1, X1>, Term<Void, En1, Void, Fk1, Void, Pair<En1, X2>, Void>> gens() {
		return gens;
	}
	
	@Override
	public Map<Y1, Term<Ty, En1, Sym, Fk1, Att1, Pair<En1, X2>, Y2>> sks() {
		return sks;
	}
	
	@Override
	public Instance<Ty, En1, Sym, Fk1, Att1, Pair<En1, X1>, Y1, Pair<En1, X1>, Y1> src() {
		return src;
	}
	
	@Override
	public Instance<Ty, En1, Sym, Fk1, Att1, Pair<En1, X2>, Y2, Pair<En1, X2>, Y2> dst() {
		return dst;
	}

}
