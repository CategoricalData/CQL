package catdata.aql.fdm;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.DP;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Mapping;
import catdata.aql.Schema;
import catdata.aql.Term;
import gnu.trove.set.hash.THashSet;

public class SigmaInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y>
		extends Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>> {

	private final Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> F;
	private final Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I;
	private final LiteralInstance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>> J;

	// options has to come in as a list, because conversion to AqlOptions requires
	// the sigma'd collage
	public SigmaInstance(Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> f,
			Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i, AqlOptions strat) {
		// if (!f.src.equals(i.schema())) {
		// throw new RuntimeException("In sigma instance, source of mapping is " + f.src
		// + ", but instance has type " + i.schema());
		// }
		F = f;
		I = i;

		Collage<Ty, En2, Sym, Fk2, Att2, Gen, Sk> col = new Collage<>(F.dst.collage());

		col.sks.putAll(I.sks());
		for (Gen gen : I.gens().keySet()) {
			col.gens.put(gen, F.ens.get(I.gens().get(gen)));
		}

		Set<Pair<Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>, Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>>> eqs = new THashSet<>();
		for (Pair<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> eq : I.eqs()) {
			eqs.add(new Pair<>(F.trans(eq.first), F.trans(eq.second)));
			col.eqs.add(new Eq<>(null, F.trans(eq.first), F.trans(eq.second)));
		}

		Function<Gen, Object> printGen = (x) -> I.algebra().printX(I.type(Term.Gen(x)).r, I.algebra().nf(Term.Gen(x)));
		BiFunction<Ty, Sk, Object> printSk = (y, x) -> I.algebra().sk(x)
				.toString(z -> I.algebra().printY(y, z).toString(), Util.voidFn());
		InitialAlgebra<Ty, En2, Sym, Fk2, Att2, Gen, Sk> initial = new InitialAlgebra<>(strat, schema(), col, printGen,
				printSk);
		J = new LiteralInstance<>(schema(), col.gens, col.sks, eqs, initial.dp(), initial,
				(Boolean) strat.getOrDefault(AqlOption.require_consistency),
				(Boolean) strat.getOrDefault(AqlOption.allow_java_eqs_unsafe));
		validate();
	}

	@Override
	public Schema<Ty, En2, Sym, Fk2, Att2> schema() {
		return F.dst;
	}

	@Override
	public Map<Gen, En2> gens() {
		return J.gens();
	}

	@Override
	public Map<Sk, Ty> sks() {
		return J.sks();
	}

	@Override
	public Iterable<Pair<Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>, Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>>> eqs() {
		return J.eqs();
	}

	@Override
	public DP<Ty, En2, Sym, Fk2, Att2, Gen, Sk> dp() {
		return J.dp();
	}

	@Override
	public Algebra<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>> algebra() {
		return J.algebra();
	}

	@Override
	public boolean requireConsistency() {
		return J.requireConsistency();
	}

	@Override
	public boolean allowUnsafeJava() {
		return J.allowUnsafeJava();
	}

	

}
