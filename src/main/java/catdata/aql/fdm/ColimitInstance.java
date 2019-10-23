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
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.graph.DMG;
import gnu.trove.set.hash.THashSet;

//has to be gen rather than (N,gen) in order to use explicit prover
public class ColimitInstance<N, E, Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> extends
		Instance<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>> {

	private final Schema<Ty, En, Sym, Fk, Att> schema;

	@SuppressWarnings("unused")
	private final DMG<N, E> shape;

	@SuppressWarnings("unused")
	private final Map<N, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> nodes;
	@SuppressWarnings("unused")
	private final Map<E, Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>> edges;

	private final Instance<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>> J;

	public ColimitInstance(Schema<Ty, En, Sym, Fk, Att> schema, DMG<N, E> shape,
			Map<N, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> nodes,
			Map<E, Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>> edges, AqlOptions options) {
		for (N n : nodes.keySet()) {
			if (!nodes.get(n).schema().equals(schema)) {
				throw new RuntimeException("The instance for " + n + " has schema " + nodes.get(n).schema() + ", not "
						+ schema + " as expected");
			}
		}
		for (E e : shape.edges.keySet()) {
			if (!edges.get(e).src().schema().equals(schema)) {
				throw new RuntimeException("On " + e + ", it is on schema \n\n" + edges.get(e).src().schema()
						+ "\n\n, not " + schema + "\n\nas expected");
			}

			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> reqdSrc = nodes.get(shape.edges.get(e).first);
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> reqdDst = nodes.get(shape.edges.get(e).second);

			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> givenSrc = edges.get(e).src();
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> givenDst = edges.get(e).dst();

			// TODO aql in general, this will be too strong - want isomorphism
			// if ((Boolean)new AqlOptions(options,
			// null).getOrDefault(AqlOption.static_typing)) {
			if (!reqdSrc.equals(givenSrc)) {
				throw new RuntimeException(
						"On " + e + ", its source is \n\n" + givenSrc + " \n\n but should be \n\n " + reqdSrc);
			}
			if (!reqdDst.equals(givenDst)) {
				throw new RuntimeException(
						"On " + e + ", its target is \n\n " + givenDst + " \n\n but should be \n\n " + reqdDst);
			}
			// }
		}

		this.schema = schema;
		this.shape = shape;
		this.nodes = nodes;
		this.edges = edges;

		Collage<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>> col = new Collage<>(schema.collage());
		Set<Pair<Term<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>>, Term<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>>>> eqs = (new THashSet<>());

		for (N n : nodes.keySet()) {
			for (Gen gen : nodes.get(n).gens().keySet()) {
				col.gens.put(new Pair<>(n, gen), nodes.get(n).gens().get(gen));
			}
			for (Sk sk : nodes.get(n).sks().keySet()) {
				col.sks.put(new Pair<>(n, sk), nodes.get(n).sks().get(sk));
			}
			nodes.get(n).eqs((a,b)->{
				col.eqs.add(new Eq<>(null, a.mapGenSk(x -> new Pair<>(n, x), x -> new Pair<>(n, x)),
						b.mapGenSk(x -> new Pair<>(n, x), x -> new Pair<>(n, x))));
				eqs.add(new Pair<>(a.mapGenSk(x -> new Pair<>(n, x), x -> new Pair<>(n, x)),
						b.mapGenSk(x -> new Pair<>(n, x), x -> new Pair<>(n, x))));
			});
		}
		for (E e : shape.edges.keySet()) {
			Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y> h = edges.get(e);
			for (Gen gen : h.src().gens().keySet()) {
				Term<Void, En, Void, Fk, Void, Gen, Void> rhs = h.gens().get(gen);
				eqs.add(new Pair<>(Term.Gen(new Pair<>(shape.edges.get(e).first, gen)),
						rhs.map(Util.voidFn(), Util.voidFn(), Function.identity(), Util.voidFn(),
								x -> new Pair<>(shape.edges.get(e).second, x), Util.voidFn())));
				col.eqs.add(new Eq<>(null, Term.Gen(new Pair<>(shape.edges.get(e).first, gen)),
						rhs.map(Util.voidFn(), Util.voidFn(), Function.identity(), Util.voidFn(),
								x -> new Pair<>(shape.edges.get(e).second, x), Util.voidFn())));
			}
			for (Sk sk : h.src().sks().keySet()) {
				Term<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>> rhs = h.sks().get(sk).mapGenSk(
						x -> new Pair<>(shape.edges.get(e).second, x), x -> new Pair<>(shape.edges.get(e).second, x));
				eqs.add(new Pair<>(Term.Sk(new Pair<>(shape.edges.get(e).first, sk)), rhs));
				col.eqs.add(new Eq<>(null, Term.Sk(new Pair<>(shape.edges.get(e).first, sk)), rhs));
			}
		}

		// AqlOptions strat = new AqlOptions(options, col);
	//	 col.validate();
//		BiFunction<En,Pair<N,Gen>,Object> printGen = (y,x) -> nodes.get(x.first).algebra().printX(y,nodes.get(x.first).algebra().nf(Term.Gen(x.second)));
		Function<Pair<N, Gen>, Object> printGen = (x) -> nodes.get(x.first).algebra().nf(Term.Gen(x.second));

		BiFunction<Ty, Pair<N, Sk>, Object> printSk = (y, x) -> nodes.get(x.first).algebra().sk(x.second)
				.toString(w -> nodes.get(x.first).algebra().printY(y, w).toString(), Util.voidFn());

		InitialAlgebra<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>> initial = new InitialAlgebra<>(options,
				schema(), col, printGen, printSk);

		J = new LiteralInstance<>(schema(), col.gens, col.sks, eqs, initial.dp(), initial,
				(Boolean) options.getOrDefault(AqlOption.require_consistency),
				(Boolean) options.getOrDefault(AqlOption.allow_java_eqs_unsafe));
		//J.validate();
		
		
	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return schema;
	}

	@Override
	public Map<Pair<N, Gen>, En> gens() {
		return J.gens();
	}

	@Override
	public Map<Pair<N, Sk>, Ty> sks() {
		return J.sks();
	}

	

	@Override
	public DP<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>> dp() {
		return J.dp();
	}

	@Override
	public Algebra<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>> algebra() {
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
