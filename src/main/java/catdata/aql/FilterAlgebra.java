package catdata.aql;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.aql.fdm.SaturatedInstance;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class FilterAlgebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> extends Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> {

	final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;
	private Map<En, Iterable<X>> ens = new THashMap<>();
	Schema<Ty, En, Sym, Fk, Att> schema;
	
	public static <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>  
	Instance<Ty, En, Sym, Fk, Att, X, Y, X, Y> 
	filterInstance(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I, 
			Schema<Ty, En, Sym, Fk, Att> schema) {
		Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg = new FilterAlgebra<>(I, schema);
		Instance<Ty, En, Sym, Fk, Att, X, Y, X, Y> ret = new SaturatedInstance<>(alg, I.dp(), I.requireConsistency(), I.allowUnsafeJava(), false, Collections.emptyMap());

		return ret;
	}
	
	public FilterAlgebra(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i, 
			Schema<Ty, En, Sym, Fk, Att> schema) {
		I = i;
		this.schema = schema;
		for (En en : schema.ens) {
			if (!I.schema().ens.contains(en)) {
				throw new RuntimeException("Missing entity: " + en);
			}
			for (Fk fk : schema.fksFrom(en)) {
				if (!I.schema().fksFrom(en).contains(fk)) {
					throw new RuntimeException("Missing fk: " + fk);					
				}
			}
			for (Att att : schema.attsFrom(en)) {
				if (!I.schema().attsFrom(en).contains(att)) {
					throw new RuntimeException("Missing att: " + att);					
				}
			}
		}
		
		m = getUnsatisfying();
		for (En en : schema.ens) {
			UnmodifiableIterator<X> z = Iterators.filter(I.algebra().en(en).iterator(), x->!m.get(en).contains(x));
			Set<X> s = new THashSet<>(I.algebra().size(en));
			z.forEachRemaining(s::add);
			ens.put(en, s);
		}
	
		
		for (;;) {
			boolean hit = false;
			for (En en : schema.ens) {
				Iterator<X> it = ens.get(en).iterator();
				while (it.hasNext()) {
					X x = it.next();
					for (Fk fk : schema.fksFrom(en)) {
						X y = I.algebra().fk(fk, x);
						if (m.get(schema.fks.get(fk).second).contains(y)) {
							m.get(en).add(x);
							hit = true;
							it.remove();
						}
					}
				}
			}
			if (!hit) {
				break;
			}
		}

	}
	
	Map<En,Set<X>> m;
	@Override
	public int size(En en) {
		return I.algebra().size(en) - m.get(en).size();
	}

	public Map<En,Set<X>> getUnsatisfying() {
		Map<En,Set<X>> ret = new THashMap<>();
		for (En en : schema.ens) {
			ret.put(en, new THashSet<>(I.algebra().size(en)));
		}
		for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : schema().eqs) {
			for (X x : I.algebra().en(eq.first.second)) {
				Term<Void, En, Void, Fk, Void, Gen, Void> xx = I.algebra().repr(eq.first.second, x);
				
				Map m = Collections.singletonMap(eq.first.first, xx);
				
				Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs = eq.second.subst(m);
				Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs = eq.third.subst(m);
				
				if (!I.dp().eq(null, lhs, rhs)) {
					ret.get(eq.first.second).add(x);
				}
			}
		}
		
		return ret;
	}
	
	@Override
	public Iterable<X> en(En en) {
		return ens.get(en);
	}
	
	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return schema;
	}

	@Override
	public boolean hasFreeTypeAlgebra() {
		return I.algebra().hasFreeTypeAlgebra();
	}

	@Override
	public X gen(Gen gen) {
		return I.algebra().gen(gen);
	}

	@Override
	public X fk(Fk fk, X x) {
		return I.algebra().fk(fk, x);
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Y> att(Att att, X x) {
		return I.algebra().att(att, x);
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Sk sk) {
		return I.algebra().sk(sk);
	}

	@Override
	public Term<Void, En, Void, Fk, Void, Gen, Void> repr(En en, X x) {
		return I.algebra().repr(en, x);
	}

	
	@Override
	protected Collage<Ty, Void, Sym, Void, Void, Void, Y> talg0() {
		return I.algebra().talg();

	}

	@Override
	public String toStringProver() {
		return "Cascade delete of " + I.algebra().toStringProver();
	}

	@Override
	public Object printX(En en, X x) {
		return I.algebra().printX(en, x);
	}

	@Override
	public Object printY(Ty ty, Y y) {
		return I.algebra().printY(ty, y);
	}

	@Override
	public Chc<Sk, Pair<X, Att>> reprT_prot(Y y) {
		return I.algebra().reprT_prot(y);
	}
	
	
}