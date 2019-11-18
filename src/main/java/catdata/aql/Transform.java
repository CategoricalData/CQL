package catdata.aql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import catdata.Chc;

public abstract class Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> implements Semantics {

	@Override
	public Kind kind() {
		return Kind.TRANSFORM;
	}

	@Override
	public int size() {
		return src().size() + dst().size();
	}

	public abstract BiFunction<Gen1, En, Term<Void, En, Void, Fk, Void, Gen2, Void>> gens();
	
	public abstract BiFunction<Sk1, Ty, Term<Ty, En, Sym, Fk, Att, Gen2, Sk2>> sks();

	public abstract Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src();

	public abstract Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> dst();

	public synchronized void validate(boolean dontValidateEqs) {
		if (!src().schema().equals(dst().schema())) {
			throw new RuntimeException(
					"Differing instance schemas\n\nsrc " + src().schema() + "\n\ndst " + dst().schema());
		}
		src().gens().keySet((Gen1 gen1) ->  {
			En en1 = src().gens().get(gen1);
			if (null == gens().apply(gen1,en1)) {
				throw new RuntimeException("source generator " + gen1 + " has no transform");
			}
			Term<Void, En, Void, Fk, Void, Gen2, Void> gen2 = gens().apply(gen1,en1).convert();
			Chc<Ty, En> en2 = dst().type(gen2.convert());
			if (!en2.equals(Chc.inRight(en1))) {
				throw new RuntimeException("source generator " + gen1 + " transforms to " + gen2 + ", which has sort "
						+ en2.toStringMash() + ", not " + en1 + " as expected");
			}
		});
		src().sks().keySet((Sk1 sk1) -> {
			Ty ty1 = src().sks().get(sk1);
			Term<Ty, En, Sym, Fk, Att, Gen2, Sk2> sk2 = sks().apply(sk1,ty1);
			if (sk2 == null) {
				throw new RuntimeException("source labelled null " + sk1 + " has no transform");
			}
			Chc<Ty, En> ty2 = dst().type(sk2);
			if (!ty2.equals(Chc.inLeft(ty1))) {
				throw new RuntimeException("source labelled null " + sk1 + " transforms to " + sk2 + ", which has sort "
						+ ty2.toStringMash() + ", not " + ty1 + " as expected");
			}
		});
			
		src().eqs((aa,bb)->{
			Term<Ty, En, Sym, Fk, Att, Gen2, Sk2> lhs = trans(aa), rhs = trans(bb);
			Chc<Ty, En> a = dst().type(lhs);
			Chc<Ty, En> b = dst().type(rhs);
			if (!a.equals(b)) {
				throw new RuntimeException("Equation " + aa + " = " + bb + " has two different types, "
						+ a.toStringMash() + " and " + b.toStringMash());
			}
		});

		if (!dontValidateEqs) {
			src().eqs((a,b) -> {
				Term<Ty, En, Sym, Fk, Att, Gen2, Sk2> lhs = trans(a), rhs = trans(b);
				boolean ok = dst().dp().eq(null, lhs, rhs);
				if (!ok) {
					String xxx = ""; // ", (and further, " + dst().collage().simplify().second.apply(lhs) + " = " +
										// dst().collage().simplify().second.apply(rhs) + ")";
					throw new RuntimeException("Source instance equation " + a + " = " + b + " translates to " + lhs
							+ " = " + rhs + xxx + ", which is not provable in the target instance, displayed below.  To proceed, consider removing it or adding more equations to the target instance.\n\n" + dst());
				}
			});
		}

	}

	@Override
	public final int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((dst() == null) ? 0 : dst().hashCode());
		result = prime * result + ((gens() == null) ? 0 : gens().hashCode());
		result = prime * result + ((sks() == null) ? 0 : sks().hashCode());
		result = prime * result + ((src() == null) ? 0 : src().hashCode());
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
		Transform<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> other = (Transform<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (dst() == null) {
			if (other.dst() != null)
				return false;
		} else if (!dst().equals(other.dst()))
			return false;
		if (gens() == null) {
			if (other.gens() != null)
				return false;
		} else if (!gens().equals(other.gens()))
			return false;
		if (sks() == null) {
			if (other.sks() != null)
				return false;
		} else if (!sks().equals(other.sks()))
			return false;
		if (src() == null) {
			if (other.src() != null)
				return false;
		} else if (!src().equals(other.src()))
			return false;
		return true;
	}

	// TODO aql alphabetical
	private String toString = null;

	@Override
	public final synchronized String toString() {
		if (toString != null) {
			return toString;
		}

		toString = toString("generators", "nulls");

		return toString;
	}

	public final String toString(String s, String t) {
		StringBuffer sb = new StringBuffer(s);
		sb.append("\n\t");
		src().gens().entrySet((k,v) -> {
			sb.append("\n\t");
			sb.append(k);
			sb.append(" -> ");
			sb.append(v);
			sb.append("\n\t");
		});
		sb.append("\n");
		sb.append(t);
		src().sks().entrySet((k,v) -> {
			sb.append("\n\t");
			sb.append(k);
			sb.append(" -> ");
			sb.append(v);
			sb.append("\n\t");
		});
		return sb.toString();
	}

	public final Term<Ty, En, Sym, Fk, Att, Gen2, Sk2> trans(Term<Ty, En, Sym, Fk, Att, Gen1, Sk1> term) {
		if (term.var != null) {
			return term.convert();
		} else if (term.obj() != null) {
			return term.convert();
		} else if (term.fk() != null) {
			return Term.Fk(term.fk(), trans(term.arg));
		} else if (term.att() != null) {
			return Term.Att(term.att(), trans(term.arg));
		} else if (term.sym() != null) {
			List<Term<Ty, En, Sym, Fk, Att, Gen2, Sk2>> l = new ArrayList<>(term.args.size());
			for (Term<Ty, En, Sym, Fk, Att, Gen1, Sk1> x : term.args) {
				l.add(trans(x));
			}
			return Term.Sym(term.sym(), l);
		} else if (term.gen() != null) {
			return gens().apply(term.gen(),src().gens().get(term.gen())).convert();
		} else if (term.sk() != null) {
			return sks().apply(term.sk(),src().sks().get(term.sk()));
		}
		throw new RuntimeException("Anomaly: please report");
	}

	public X2 repr(En en1, X1 x1) {
		Term<Void, En, Void, Fk, Void, Gen1, Void> a = src().algebra().repr(en1, x1);
		Term<Void, En, Void, Fk, Void, Gen2, Void> b = trans0(a);
		return dst().algebra().nf(b);
	}

	private Term<Void, En, Void, Fk, Void, Gen2, Void> trans0(Term<Void, En, Void, Fk, Void, Gen1, Void> term) {
		if (term.fk() != null) {
			return Term.Fk(term.fk(), trans0(term.arg));
		} else if (term.gen() != null) {
			return gens().apply(term.gen(),src().gens().get(term.gen())).convert();
		}
		throw new RuntimeException("Anomaly: please report");
	}

	public Term<Ty, En, Sym, Fk, Att, Gen2, Sk2> reprT(Y1 y1) {
		Term<Ty, En, Sym, Fk, Att, Gen1, Sk1> a = src().reprT(Term.Sk(y1));
		return trans(a);
	}

}
