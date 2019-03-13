package catdata.provers;

import java.util.Collections;
import java.util.Map;

import catdata.Pair;

public class FreeProver<T,C,V> extends DPKB<T,C,V> {

	public FreeProver(KBTheory<T,C,V> th) { 
		super(th);
		if (!th.eqs.isEmpty()) {
			throw new RuntimeException("not an empty theory, as required by free proving strategy");
		}
	}

	@Override
	public boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
		return lhs.equals(rhs);
	}

	
	
	@Override
	public String toString() {
		return "Free prover";
	}

	@Override
	public synchronized void add(C c, T t) {
		this.kb.syms.put(c, new Pair<>(Collections.emptyList(), t));
	}

	
}
