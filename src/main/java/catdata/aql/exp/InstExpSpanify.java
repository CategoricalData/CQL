package catdata.aql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.DP;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.exp.SchExp.SchExpInst;
import gnu.trove.map.hash.THashMap;

public class InstExpSpanify<X, Y> extends InstExp<X, Y, X, Y> {

	private final Map<String, String> options;

	private final InstExp<Gen, Sk, X, Y> I;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.emptySet();
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public InstExpSpanify(InstExp<Gen, Sk, X, Y> i, List<Pair<String, String>> options) {
		this.I = i;
		this.options = Util.toMapSafely(options);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	public Schema<Ty, En, Sym, Fk, Att> makeSchema(AqlEnv env, AqlOptions ops) {
		return type(env.typing).eval(env, false);
	}

	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, X, Y, X, Y> eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}

		AqlOptions ops = new AqlOptions(options, env.defaults);
		Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> J = I.eval(env, isC);

		if (!J.schema().typeSide.equals(new TyExpRdf().eval(env, isC))) {
			throw new RuntimeException("Not on RDF typeside: " + I);
		}
//		if (!J.schema().equals(InstExpRdfAll.makeSch().eval(env, isC))) {
//			throw new RuntimeException("Not RDF import: " + I);
//		}

		Collage<Ty, En, Sym, Fk, Att, Void, Void> col = new CCollage<>(J.schema().typeSide.collage());
		Map<En, List<X>> gens = new THashMap<>();

		En en = En.En("R");
//		Att s = Att.Att(en, "subject");
		Att p = Att.Att(en, "predicate");
//		Att o = Att.Att(en, "object");
		Ty ty = Ty.Ty("Dom");
		for (X x : J.algebra().en(en)) {
//			Term<Ty, Void, Sym, Void, Void, Void, Y> ss = J.algebra().att(s, x);
			Term<Ty, Void, Sym, Void, Void, Void, Y> pp = J.algebra().att(p, x);
//			Term<Ty, Void, Sym, Void, Void, Void, Y> oo = J.algebra().att(o, x);
			if (pp.obj() == null) {
				throw new RuntimeException("Encountered predicate that is not IRI: " + pp);
			}
			En en2 = En.En((String) pp.obj());
			col.getEns().add(en2);
			col.atts().put(Att.Att(en2, "subject"), new Pair<>(en2, ty));
			col.atts().put(Att.Att(en2, "object"), new Pair<>(en2, ty));
			List<X> l = gens.get(en2);
			if (l == null) {
				l = new LinkedList<>();
				gens.put(en2, l);
			}
			l.add(x);
		}

		Schema<Ty, En, Sym, Fk, Att> sch = new Schema<>(new TyExpRdf().eval(env, isC), col, ops);

		var ret = new Instance<Ty, En, Sym, Fk, Att, X, Y, X, Y>() {

			@Override
			public Schema<Ty, En, Sym, Fk, Att> schema() {
				return sch;
			}

			@Override
			public IMap<X, En> gens() {
				return new IMap<X, En>() {

					@Override
					public En get(X x) {
						return En.En((String) J.algebra().att(p, x).obj());
					}

					@Override
					public boolean containsKey(X x) {
						return gens.get(En.En((String) J.algebra().att(p, x).obj())).contains(x);
					}

					@Override
					public void entrySet(BiConsumer<? super X, ? super En> f) {
						for (Entry<En, List<X>> entry : gens.entrySet()) {
							entry.getValue().forEach((x -> f.accept(x, entry.getKey())));
						}
					}

					@Override
					public int size() {
						return J.algebra().size();
					}

					@Override
					public En remove(X x) {
						return Util.anomaly();
					}

					@Override
					public void put(X x, En y) {
						Util.anomaly();
					}

				};
			}

			@Override
			public IMap<Y, Ty> sks() {
				return Instance.mapToIMap(J.algebra().talg().sks);
			}

			@Override
			public boolean requireConsistency() {
				return J.requireConsistency();
			}

			@Override
			public boolean allowUnsafeJava() {
				return J.requireConsistency();
			}

			@Override
			public DP<Ty, En, Sym, Fk, Att, X, Y> dp() {
				return new DP<Ty, En, Sym, Fk, Att, X, Y>() {

					@Override
					public String toStringProver() {
						return "Spanify wrapper of " + J.dp().toStringProver();
					}

					private Term<Ty, En, Sym, Fk, Att, Gen, Sk> transL(Term<Ty, En, Sym, Fk, Att, X, Y> term) {
						if (term.obj() != null) {
							return term.convert();
						} else if (term.sym() != null) {
							List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> l = new ArrayList<>(term.args.size());
							for (Term<Ty, En, Sym, Fk, Att, X, Y> eq : term.args) {
								l.add(transL(eq));
							}
							return Term.Sym(term.sym(), l);
						} else if (term.att() != null) {
							return Term.Att(term.att(), transL(term.arg));
						} else if (term.gen() != null) {
							return J.algebra().repr(en, term.gen()).convert();
						} else if (term.sk() != null) {
							return J.algebra().reprT(Term.Sk(term.sk()));
						}
						return Util.anomaly();
					}

					@Override
					public boolean eq(Map<catdata.aql.Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, X, Y> lhs,
							Term<Ty, En, Sym, Fk, Att, X, Y> rhs) {
						return J.dp().eq(ctx, transL(lhs), transL(rhs));
					}

				};
			}

			@Override
			public Algebra<Ty, En, Sym, Fk, Att, X, Y, X, Y> algebra() {
				return new Algebra<Ty, En, Sym, Fk, Att, X, Y, X, Y>() {

					@Override
					public Schema<Ty, En, Sym, Fk, Att> schema() {
						return sch;
					}

					@Override
					public boolean hasNulls() {
						return J.algebra().hasNulls();
					}

					@Override
					public Iterable<X> en(En en) {
						return gens.get(en);
					}

					@Override
					public X gen(X x) {
						return x;
					}

					@Override
					public X fk(Fk fk, X x) {
						return Util.anomaly();
					}

					@Override
					public Term<Ty, Void, Sym, Void, Void, Void, Y> att(Att att, X x) {
						return J.algebra().att(Att.Att(en, att.str), x);
					}

					@Override
					public Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Y sk) {
						return Term.Sk(sk);
					}

					@Override
					public Term<Void, En, Void, Fk, Void, X, Void> repr(En en, X x) {
						return Term.Gen(x);
					}

					@Override
					public int size(En x) {
						return gens.get(x).size();
					}

					@Override
					protected TAlg<Ty, Sym, Y> talg0() {
						return J.algebra().talg();
					}

					@Override
					public Chc<Y, Pair<X, Att>> reprT_prot(Y y) {
						return Chc.inLeftNC(y);
					}

					@Override
					public String toStringProver() {
						return "Spanify algebra wrapper of " + J.algebra().toStringProver();
					}

					@Override
					public Object printX(En ignore, X x) {
						return J.algebra().printX(en, x);
					}

					@Override
					public Object printY(Ty ty, Y y) {
						return J.algebra().printY(ty, y);
					}
				};
			}
		};
//		System.out.println(ret);
		ret.validate();
		return ret;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("spanify ").append(I);
		if (!options.isEmpty()) {
			sb.append(" {\n\t").append("\n\toptions\n\t\t").append(Util.sep(options, " = ", "\n\t\t")).append("}");
		}
		return sb.toString();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return I.deps();
	}

	@Override
	public SchExp type(AqlTyping G) {
		I.type(G);
		return new SchExpInst<>(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((I == null) ? 0 : I.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstExpSpanify other = (InstExpSpanify) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

}
