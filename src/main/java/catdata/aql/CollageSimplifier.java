package catdata.aql;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.collections4.list.TreeList;

import catdata.Chc;
import catdata.Triple;
import catdata.aql.Collage.CCollage;
import gnu.trove.set.hash.THashSet;

public class CollageSimplifier<Ty, En, Sym, Fk, Att, Gen, Sk> {

	public Collage<Ty, En, Sym, Fk, Att, Gen, Sk> in;

	public Collage<Ty, En, Sym, Fk, Att, Gen, Sk> simplified;

	private List<Triple<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, List<Var>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> list = (new TreeList<>());

	public Function<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>

	simp = term -> {
		for (Triple<Head<Ty, En, Sym, Fk, Att, Gen, Sk>, List<Var>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> t : list) {
			term = term.replaceHead(t.first, t.second, t.third);
		}
		return term;
	};

	public CollageSimplifier(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> in) {
		simplified = new CCollage<>(in);
		this.in = in;

		while (simplify1()) {

		}

		Iterator<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> it = simplified.eqs().iterator();
		while (it.hasNext()) {
			Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq = it.next();
			if (eq.lhs.equals(eq.rhs)) {
				it.remove();
			}
		}

	}

	private boolean simplify1() {
		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : simplified.eqs()) {
			if (simplify2(eq.ctx, eq.lhs, eq.rhs) || simplify2(eq.ctx, eq.rhs, eq.lhs)) {
				return true;
			}
		}
		return false;
	}

	private boolean simplify2(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs,
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
		if (lhs.var != null || lhs.obj() != null) {
			return false;
		}
		List<Var> vars = Collage.getVarArgsUnique(lhs);
		if (vars == null) {
			return false; // f(x,x) kind of thing
		}
		if (!new THashSet<>(vars).equals(ctx.keySet())) {
			return false; // forall x, y. f(y) = ... kind of thing
		}

		Set<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> neweqs = new THashSet<>(simplified.eqs().size());
		Head<Ty, En, Sym, Fk, Att, Gen, Sk> head = Head.mkHead(lhs);

		if (!rhs.contains(head)) {
			simplified.remove(head);
			list.add(new Triple<>(head, vars, rhs));
			for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : simplified.eqs()) {
				neweqs.add(new Eq<>(eq.ctx, eq.lhs.replaceHead(head, vars, rhs), eq.rhs.replaceHead(head, vars, rhs)));
			}
			simplified.eqs().clear();
			simplified.eqs().addAll(neweqs);
			return true;
		}
		return false;
	}

}
