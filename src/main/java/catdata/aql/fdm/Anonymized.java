package catdata.aql.fdm;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.iterators.IteratorIterable;

import com.google.common.collect.Iterators;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.Collage;
import catdata.aql.DP;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;

public class Anonymized<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> extends Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> {

	private final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;

	int fresh = 0;
	private final Map<String, String> iso_string_1 = Util.mk(), iso_string_2 = Util.mk();
	private final Map<Integer, Integer> iso_int_1 = Util.mk(), iso_int_2 = Util.mk();

	private Anonymized<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y>.InnerAlgebra algebra;

	private DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp;

	private class InnerDP implements DP<Ty, En, Sym, Fk, Att, Gen, Sk> {

		@Override
		public String toStringProver() {
			return I.dp().toStringProver();
		}

		@Override
		public boolean eq(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs,
				Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
			return I.dp().eq(ctx, iso2(lhs), iso2(rhs));
		}

	}

	private class InnerAlgebra extends Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> {

		@Override
		public Schema<Ty, En, Sym, Fk, Att> schema() {
			return I.schema();
		}

		@Override
		public Iterable<X> en(En en) {
			return I.algebra().en(en);
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
			return iso1(I.algebra().att(att, x)).convert();
		}

		@Override
		public Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Sk sk) {
			return iso1(I.algebra().sk(sk)).convert();
		}

		@Override
		public Term<Void, En, Void, Fk, Void, Gen, Void> repr(En en, X x) {
			return I.algebra().repr(en, x);
		}

		@Override
		public Collage<Ty, Void, Sym, Void, Void, Void, Y> talg0() {
			Collage<Ty, Void, Sym, Void, Void, Void, Y> col = new Collage<>(I.algebra().talg());
			col.eqs.clear();
			for (Eq<Ty, Void, Sym, Void, Void, Void, Y> eq : I.algebra().talg().eqs) {
				col.eqs.add(new Eq<>(eq.ctx, iso1(eq.lhs), iso1(eq.rhs)));
			}
			return col;
		}

		public boolean hasFreeTypeAlgebra() {
			return talg().eqs.isEmpty();
		}

		public boolean hasFreeTypeAlgebraOnJava() {
			return talg().eqs.stream().filter(x -> talg().java_tys.containsKey(talg().type(x.ctx, x.lhs).l))
					.collect(Collectors.toList()).isEmpty();
		}

		@Override
		public String toStringProver() {
			return I.algebra().toStringProver();
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
		public int size(En en) {
			return I.algebra().size(en);
		}

		@Override
		public Chc<Sk, Pair<X, Att>> reprT_prot(Y y) {
			return I.algebra().reprT_prot(y);

		}

		@Override
		public boolean hasNulls() {
			return I.algebra().hasNulls();
		}

	};

	@SuppressWarnings("hiding")
	private Object iso1(Object obj, Ty ty) {
		if (I.schema().typeSide.js.java_tys.containsKey(ty)) {
			String ty2 = I.schema().typeSide.js.java_tys.get(ty);
			if (ty2.equals("java.lang.String")) {
				if (!iso_string_1.containsKey(obj)) {
					int i = fresh++;
					iso_string_1.put((String) obj, "Str" + i);
					iso_string_2.put("Str" + i, (String) obj);
				}
				return iso_string_1.get(obj);
			} else if (ty2.equals("java.lang.Integer")) {
				if (!iso_int_1.containsKey(obj)) {
					int i = fresh++;
					iso_int_1.put((Integer) obj, i);
					iso_int_2.put(i, (Integer) obj);
				}
				return iso_int_1.get(obj);
			}
		}
		return obj;
	}

	private Object iso2(Object obj, Ty ty) {
		if (I.schema().typeSide.js.java_tys.containsKey(ty)) {
			String ty2 = I.schema().typeSide.js.java_tys.get(ty);
			if (ty2.equals("java.lang.String")) {
				return iso_string_2.get(obj);
			} else if (ty2.equals("java.lang.Integer")) {
				return iso_int_2.get(obj);
			}
		}
		return obj;
	}

	private <En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> iso1(
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> t) {
		return t.visit(x -> Term.Var(x), (obj, ty) -> Term.Obj(iso1(obj, ty), ty), (sym, x) -> Term.Sym(sym, x),
				(fk, x) -> Term.Fk(fk, x), (att, x) -> Term.Att(att, x), x -> Term.Gen(x), x -> Term.Sk(x));
	}

	private Term<Ty, En, Sym, Fk, Att, Gen, Sk> iso2(Term<Ty, En, Sym, Fk, Att, Gen, Sk> t) {
		return t.visit(x -> Term.Var(x), (obj, ty) -> Term.Obj(iso2(obj, ty), ty), (sym, x) -> Term.Sym(sym, x),
				(fk, x) -> Term.Fk(fk, x), (att, x) -> Term.Att(att, x), x -> Term.Gen(x), x -> Term.Sk(x));
	}

	// TODO aql note this can fail at runtime
	public Anonymized(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i) {
		I = i;
		for (Att att : i.schema().atts.keySet()) {
			for (Pair<X, Term<Ty, Void, Sym, Void, Void, Void, Y>> p : i.algebra().attAsSet(att)) {
				iso1(p.second);
			}
		}
		this.algebra = new InnerAlgebra();
		this.dp = new InnerDP();
		this.validate();
	}

	@Override
	public Schema<Ty, En, Sym, Fk, Att> schema() {
		return I.schema();
	}

	@Override
	public Map<Gen, En> gens() {
		return I.gens();
	}

	@Override
	public Map<Sk, Ty> sks() {
		return I.sks();
	}

	

	@Override
	public DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp() {
		return dp;
	}

	@Override
	public Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> algebra() {
		return algebra;
	}

	@Override
	public boolean requireConsistency() {
		return I.requireConsistency();
	}

	@Override
	public boolean allowUnsafeJava() {
		return I.allowUnsafeJava();
	}

	

}
