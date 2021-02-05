package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Quad;
import catdata.aql.AqlJs;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.It.ID;
import catdata.aql.Kind;
import catdata.aql.Query;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;

public class TransExpFrozen
		extends TransExp<Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att>>, ID, Chc<Var, Pair<ID, Att>>> {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Q == null) ? 0 : Q.hashCode());
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		result = prime * result + ((term == null) ? 0 : term.hashCode());
		result = prime * result + ((var == null) ? 0 : var.hashCode());
		return result;
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		Q.map(f);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TransExpFrozen other = (TransExpFrozen) obj;
		if (Q == null) {
			if (other.Q != null)
				return false;
		} else if (!Q.equals(other.Q))
			return false;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		if (term == null) {
			if (other.term != null)
				return false;
		} else if (!term.equals(other.term))
			return false;
		if (var == null) {
			if (other.var != null)
				return false;
		} else if (!var.equals(other.var))
			return false;
		return true;
	}

	public final QueryExp Q;
	public final String src, dst, var;
	public final RawTerm term;

	@Override
	public Pair<InstExp<Var, Var, ID, Chc<Var, Pair<ID, Att>>>, InstExp<Var, Var, ID, Chc<Var, Pair<ID, Att>>>> type(
			AqlTyping G) {
		return new Pair<>(new InstExpFrozen(Q, dst), new InstExpFrozen(Q, src));
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) {
		return v.visit(params, this);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	public Transform<Ty, En, Sym, Fk, Att, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att>>, ID, Chc<Var, Pair<ID, Att>>> eval0(
			AqlEnv env, boolean isC) {
		Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q = Q.eval(env, isC);
		if (isC) {
			throw new IgnoreException();
		}

		Map<String, Chc<Ty, En>> Map = new THashMap<>();
		Map.put(var, Chc.inRight(En.En(src)));
		Chc<Ty, En> expected;
		if (!q.tys.keySet().contains(Ty.Ty(dst))) {
			expected = Chc.inRight(En.En(dst));
		} else {
			expected = Chc.inLeft(Ty.Ty(dst));
		}

		Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col = new CCollage<>(q.dst.collage());
		AqlJs<Ty, Sym> js = q.src.typeSide.js;

		Quad<Map<catdata.aql.Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Chc<Ty, En>> result = RawTerm
				.infer1x(Map, term, term, expected, col, "", js);

		if (expected.left) {
			return q.composeT(result.second.convert(), En.En(src));
		}
		return q.compose(q.transP(result.second.convert()), En.En(src));

	}

	public TransExpFrozen(QueryExp q, String var, String src, RawTerm term, String dst) {
		Q = q;
		this.src = src;
		this.dst = dst;
		this.var = var;
		this.term = term;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Q.deps();
	}

	// frozen qTS lambda x:s0. x.ss.att : Integer
	@Override
	public String toString() {
		return "frozen " + Q + "lambda " + var + ":" + src + ". " + term + " : " + dst;
	}

}
