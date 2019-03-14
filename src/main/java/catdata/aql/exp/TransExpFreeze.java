package catdata.aql.exp;

public class TransExpFreeze {
	/*
	 * <Ty, En1, Sym, Fk1, Att1, Gen1, Sk1, En2, Fk2, Att2, Gen2, Sk2, X1, Y1, X2,
	 * Y2> extends TransExp<catdata.aql.Var, catdata.aql.Var, catdata.aql.Var,
	 * catdata.aql.Var, ID, Chc<catdata.aql.Var, Pair<ID, Att1>>, ID,
	 * Chc<catdata.aql.Var, Pair<ID, Att1>>> {
	 * 
	 * public final QueryExp<Ty,En1,Sym,Fk1,Att1,En2,Fk2,Att2> Q; public Chc<Fk2,
	 * Att2> t;
	 * 
	 * @Override public Map<String, String> options() { return
	 * Collections.emptyMap(); } public TransExpFreeze(QueryExp<Ty, En1, Sym, Fk1,
	 * Att1, En2, Fk2, Att2> q,TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t) {
	 * //this.t = t; Q = q; }
	 * 
	 * @Override public Pair<InstExp<Ty, En1, Sym, Fk1, Att1, X1, Y1, X1, Y1>,
	 * InstExp<Ty, En2, Sym, Fk1, Att1, X2, Y2, X2, Y2>> type(AqlTyping G) { if
	 * (!t.type(G).first.type(G).equals(Q.type(G).first)) { throw new
	 * RuntimeException("Source of query is " + t.type(G).first.type(G) +
	 * " but transform is on " + t.type(G).first); } return null; // return new
	 * Pair<>(new InstExpFreeze<>(Q, t.type(G).first, Collections.emptyList()), new
	 * InstExpEval<>(Q, t.type(G).second, Collections.emptyList())); }
	 * 
	 * @Override public Transform<Ty, En1, Sym, Fk1, Att1, catdata.aql.Var,
	 * catdata.aql.Var, catdata.aql.Var, catdata.aql.Var, ID, Chc<catdata.aql.Var,
	 * Pair<ID, Att1>>, ID, Chc<catdata.aql.Var, Pair<ID, Att1>>> eval0(AqlEnv env)
	 * { if (t.left) { return Q.eval(env).fks.get(t.l); } else { return
	 * Q.eval(env).atts.get(t.r); } }
	 * 
	 * @Override public String toString() { return "frozen " + Q + " " + t.left ? +
	 * " " + t.l.att Q t.l; }
	 * 
	 * @Override public int hashCode() { int prime = 31; int result = 1; result =
	 * prime * result + ((Q == null) ? 0 : Q.hashCode()); result = prime * result +
	 * ((t == null) ? 0 : t.hashCode()); return result; }
	 * 
	 * @Override public boolean equals(Object obj) { if (this == obj) return true;
	 * if (obj == null) return false; if (getClass() != obj.getClass()) return
	 * false; TransExpFreeze<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> other =
	 * (TransExpFreeze<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) obj; if (Q
	 * == null) { if (other.Q != null) return false; } else if (!Q.equals(other.Q))
	 * return false; if (t == null) { if (other.t != null) return false; } else if
	 * (!t.equals(other.t)) return false; return true; }
	 * 
	 * @Override public Collection<Pair<String, Kind>> deps() { return
	 * Util.union(Q.deps(), t.deps()); }
	 * 
	 * public <R,P,E extends Exception> R accept(P params, TransExpVisitor<R, P, E>
	 * v) throws E { return v.visit(params, this); }
	 * 
	 * @Override protected void allowedOptions(Set<AqlOption> set) { // TODO
	 * Auto-generated method stub
	 * 
	 * }
	 */

}