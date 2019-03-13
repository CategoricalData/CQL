package catdata.aql;

import java.util.Map;

import catdata.Chc;

public interface DP<Ty,En,Sym,Fk,Att,Gen,Sk> {
	
	public abstract String toStringProver();
		
	boolean eq(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs, Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs);
	
	
	@SuppressWarnings("unused")
	default Term<Ty,En,Sym,Fk,Att,Gen,Sk> nf(Map<Var, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		throw new RuntimeException("Anomaly: please report");
	}
	
	static <Ty,En,Sym,Fk,Att,Gen,Sk> DP<Ty,En,Sym,Fk,Att,Gen,Sk> initial() {
		return new DP<>() {
	
			@Override
		public boolean eq(Map<Var, Chc<Ty, En>> ctx, Term<Ty,En,Sym,Fk,Att,Gen,Sk> lhs, Term<Ty,En,Sym,Fk,Att,Gen,Sk> rhs) {
			throw new RuntimeException();
		}

	

		@Override
		public String toStringProver() {
			return "Theorem prover for empty theory";
		}

	}; }
	
	
	
}
