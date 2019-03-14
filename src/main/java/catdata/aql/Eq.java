package catdata.aql;

import java.util.Collections;
import java.util.Map;

import catdata.Chc;
import catdata.Triple;
import catdata.Util;

public class Eq<Ty, En, Sym, Fk, Att, Gen, Sk> {

	public final Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs, rhs;
	public final Map<Var, Chc<Ty, En>> ctx;
	private int code;

	public Eq(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs,
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs) {
		if (ctx == null || ctx.isEmpty()) {
			this.ctx = Collections.emptyMap();
		} else {
			this.ctx = ctx;
		}
		this.lhs = lhs;
		this.rhs = rhs;
		code = hashCode2();
	}

	@Override
	public int hashCode() {
		return code;
	}

	public int hashCode2() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((ctx == null) ? 0 : ctx.hashCode());
		result = prime * result + ((lhs == null) ? 0 : lhs.hashCode());
		result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
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
		Eq<?, ?, ?, ?, ?, ?, ?> other = (Eq<?, ?, ?, ?, ?, ?, ?>) obj;
		if (code != other.code) {
			return false;
		}
		if (ctx == null) {
			if (other.ctx != null)
				return false;
		} else if (!ctx.equals(other.ctx))
			return false;
		if (lhs == null) {
			if (other.lhs != null)
				return false;
		} else if (!lhs.equals(other.lhs))
			return false;
		if (rhs == null) {
			if (other.rhs != null)
				return false;
		} else if (!rhs.equals(other.rhs))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Util.sep(ctx, ":", ", ");
	}

	public final Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> toTriple() {
		return new Triple<>(ctx, lhs, rhs);
	}

}
