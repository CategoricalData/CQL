package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.cql.Transform;

public class TransExpSubseteq<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> extends TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> {

	public final QueryExp Q1, Q2;

	public TransExpSubseteq(QueryExp q1, QueryExp q2) {
		Q1 = q1;
		Q2 = q2;
	}

	@Override
	public Pair type(AqlTyping G) {
		if (Q1.type(G).equals(Q2.type(G))) {
			return new Pair<>(new InstExpFrozen(Q1, "Q"), new InstExpFrozen(Q2, "Q"));
		}
		throw new RuntimeException("Bad subset todo");
	}

	@Override
	public Object accept(Object params, TransExpVisitor v) throws Exception {
		return v.visit(params, this);
	}

	@Override
	public void mapSubExps(Consumer f) {
		Q1.mapSubExps(f);
		Q2.mapSubExps(f);
	}

	@Override
	protected void allowedOptions(Set set) {
	}

	@Override
	protected Map options() {
		return Collections.emptyMap();
	}

	@Override
	protected Transform<String, String, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> eval0(AqlEnv env, boolean isCompileTime) {
		var q1 = Q1.eval(env, isCompileTime);
		var q2 = Q2.eval(env, isCompileTime);
		
		Transform t = QueryExpReformulate.hom(q1, q2);
		if (t == null) {
			throw new RuntimeException("No hom exists"); 
		}
		return t;
	}

	@Override
	public int hashCode() {
		return Objects.hash(Q1, Q2);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TransExpSubseteq other = (TransExpSubseteq) obj;
		return Objects.equals(Q1, other.Q1) && Objects.equals(Q2, other.Q2);
	}

	@Override
	public String toString() {
		return "subseteq " + Q1 + " " + Q2;
	}

	@Override
	public Collection deps() {
		return Util.union(Q1.deps(), Q2.deps());
	}

}
