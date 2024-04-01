package catdata.cql.fdm;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.Algebra;
import catdata.cql.AqlOptions;
import catdata.cql.Collage;
import catdata.cql.DP;
import catdata.cql.Eq;
import catdata.cql.Instance;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import catdata.graph.DMG;

//has to be gen rather than (N,gen) in order to use explicit prover
public class ColimitInstance<N, E, Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>
		extends Instance<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>> {

	private final Schema<Ty, En, Sym, Fk, Att> schema;

	@SuppressWarnings("unused")
	private final DMG<N, E> shape;

	@SuppressWarnings("unused")
	private final Map<N, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> nodes;
	@SuppressWarnings("unused")
	private final Map<E, Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>> edges;

	public final Instance<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>> J;

	// TODO: override eqs?

	public ColimitInstance(Schema<Ty, En, Sym, Fk, Att> schema, DMG<N, E> shape,
			Map<N, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>> nodes,
			Map<E, Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>> edges, AqlOptions options) {
		for (N n : nodes.keySet()) {
			if (!nodes.get(n).schema().equals(schema)) {
				throw new RuntimeException(
						"The instance for " + n + " has schema " + nodes.get(n).schema() + ", not " + schema + " as expected");
			}
		}
		for (E e : shape.edges.keySet()) {
			if (!edges.get(e).src().schema().equals(schema)) {
				throw new RuntimeException("On " + e + ", it is on schema \n\n" + edges.get(e).src().schema() + "\n\n, not "
						+ schema + "\n\nas expected");
			}

		}

		this.schema = schema;
		this.shape = shape;
		this.nodes = nodes;
		this.edges = edges;

		Collage<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>> col = new CCollage<>(schema().collage());

		List<Pair<Term<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>>, Term<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>>>> l = new LinkedList<>();

		for (N n : nodes.keySet()) {
			nodes.get(n).gens().keySet(gen -> {
				col.gens().put(new Pair<>(n, gen), nodes.get(n).gens().get(gen));
			});
			nodes.get(n).sks().keySet(sk -> {
				col.sks().put(new Pair<>(n, sk), nodes.get(n).sks().get(sk));
			});
			nodes.get(n).eqs((a, b) -> {
				col.eqs().add(new Eq<>(null, a.mapGenSk(x -> new Pair<>(n, x), x -> new Pair<>(n, x)),
						b.mapGenSk(x -> new Pair<>(n, x), x -> new Pair<>(n, x))));

				l.add(new Pair<>(a.mapGenSk(x -> new Pair<>(n, x), x -> new Pair<>(n, x)),
						b.mapGenSk(x -> new Pair<>(n, x), x -> new Pair<>(n, x))));
			});
		}
		for (E e : shape.edges.keySet()) {
			Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y> h = edges.get(e);

			h.src().gens().entrySet((gen, t) -> {
				col.eqs()
						.add(new Eq<>(null, Term.Gen(new Pair<>(shape.edges.get(e).first, gen)),
								h.gens().apply(gen, t).map(Util.voidFn(), Util.voidFn(), Function.identity(), Util.voidFn(),
										x -> new Pair<>(shape.edges.get(e).second, x), Util.voidFn())));

				l.add(new Pair<>(Term.Gen(new Pair<>(shape.edges.get(e).first, gen)),
						h.gens().apply(gen, t).map(Util.voidFn(), Util.voidFn(), Function.identity(), Util.voidFn(),
								x -> new Pair<>(shape.edges.get(e).second, x), Util.voidFn())));
			});
			h.src().sks().entrySet((sk, t) -> {
				col.eqs().add(new Eq<>(null, Term.Sk(new Pair<>(shape.edges.get(e).first, sk)), h.sks().apply(sk, t).mapGenSk(
						x -> new Pair<>(shape.edges.get(e).second, x), x -> new Pair<>(shape.edges.get(e).second, x))));
				l.add(new Pair<>(Term.Sk(new Pair<>(shape.edges.get(e).first, sk)), h.sks().apply(sk, t).mapGenSk(
						x -> new Pair<>(shape.edges.get(e).second, x), x -> new Pair<>(shape.edges.get(e).second, x))));

			});

		}

		Function<Pair<N, Gen>, Object> printGen = (x) -> "(" + x.first + ", "
				+ nodes.get(x.first).algebra().nf(Term.Gen(x.second)) + ")";

		BiFunction<Ty, Pair<N, Sk>, Object> printSk = (y, x) -> "(" + x.first + ", " + nodes.get(x.first).algebra().sk(x.second)
				.toString(w -> nodes.get(x.first).algebra().printY(y, w).toString(), Util.voidFn(), false) + ")";

		InitialAlgebra<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>> initial = new InitialAlgebra<>(options, schema(), col,
				printGen, printSk);

		J = new LiteralInstance<>(schema(), col.gens(), col.sks(), l, initial.dp(), initial,
				(Boolean) options.getOrDefault(AqlOption.require_consistency),
				(Boolean) options.getOrDefault(AqlOption.allow_java_eqs_unsafe));
	//	J.validateMore();
		//validateMore();		
	}
	
	public Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Pair<N, Gen>, Pair<N, Sk>, X, Y, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>> get(N n) {
		Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I = nodes.get(n);
		Instance<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>> t = this;
		return new Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Pair<N, Gen>, Pair<N, Sk>, X, Y, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>>() {

			@Override
			public BiFunction<Gen, En, Term<Void, En, Void, Fk, Void, Pair<N, Gen>, Void>> gens() {
				return (gen, en) -> Term.Gen(new Pair<>(n, gen));
			}

			@Override
			public BiFunction<Sk, Ty, Term<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>>> sks() {
				return (sk, ty) -> Term.Sk(new Pair<>(n, sk));
			}

			@Override
			public Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> src() {
				return I;
			}

			@Override
			public Instance<Ty, En, Sym, Fk, Att, Pair<N, Gen>, Pair<N, Sk>, Integer, Chc<Pair<N, Sk>, Pair<Integer, Att>>> dst() {
				return t;
			}			
		};
	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return schema;
	}

	@Override
	public IMap<Pair<N, Gen>, En> gens() {
		return J.gens();
	}

	@Override
	public IMap<Pair<N, Sk>, Ty> sks() {
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
