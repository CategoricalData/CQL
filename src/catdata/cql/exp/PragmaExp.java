package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.AqlOptions.AqlOption;

public abstract class PragmaExp extends Exp<Pragma> {

	@Override
	public Kind kind() {
		return Kind.PRAGMA;
	}

	@Override
	public abstract Unit type(AqlTyping G);

	@Override
	public Exp<Pragma> Var(String v) {
		Exp<Pragma> ret = new PragmaExpVar(v);
		return ret;
	}

	public static interface PragmaExpCoVisitor<R, P, E extends Exception> {
		public <X, Y> PragmaExpConsistent<X, Y> visitPragmaExpConsistent(P params, R exp) throws E;

		public <X, Y> PragmaExpCheck<X, Y> visitPragmaExpCheck(P params, R exp) throws E;

		public PragmaExpMatch visitPragmaExpMatch(P params, R exp) throws E;

		public PragmaExpSql visitPragmaExpSql(P params, R exp) throws E;

		public <X, Y> PragmaExpToCsvInst<X, Y> visitPragmaExpToCsvInst(P params, R exp) throws E;

		public PragmaExpVar visitPragmaExpVar(P params, R exp) throws E;

		public PragmaExpJs visitPragmaExpJs(P params, R exp) throws E;

		public PragmaExpProc visitPragmaExpProc(P params, R exp) throws E;

		public <X, Y> PragmaExpJsonInstExport<X, Y> visitPragmaExpJsonInstExport(P params, R exp) throws E;

		public <X, Y> PragmaExpRdfDirectExport<X, Y> visitPragmaExpRdfDirectExport(P params, R exp) throws E;

		public <X, Y> PragmaExpRdfInstExport<X, Y> visitPragmaExpRdfInstExport(P params, R exp) throws E;

		public <X, Y> PragmaExpToJdbcInst<X, Y> visitPragmaExpToJdbcInst(P params, R exp) throws E;

		public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> PragmaExpToJdbcTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> visitPragmaExpToJdbcTrans(
				P params, R exp) throws E;

		public PragmaExpToJdbcQuery visitPragmaExpToJdbcQuery(P params, R exp) throws E;

		public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> PragmaExpToCsvTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> visitPragmaExpToCsvTrans(
				P params, R exp) throws E;

		public PragmaExpCheck2 visitPragmaExpCheck2(P params, R exp) throws E;

		public PragmaExpBitsy visitPragmaExpBitsy(P params, R exp) throws E;

		public PragmaExpTinkerpop visitPragmaExpTinkerpop(P params, R exp) throws E;

		public PragmaExpTinkerpopInstExport visitPragmaExpTinkerpopInstExport(P params, R exp) throws E;
	}

	public static interface PragmaExpVisitor<R, P, E extends Exception> {
		public <X, Y> R visit(P params, PragmaExpConsistent<X, Y> exp) throws E;

		public <X, Y> R visit(P params, PragmaExpCheck<X, Y> exp) throws E;

		public R visit(P params, PragmaExpMatch exp) throws E;

		public R visit(P params, PragmaExpSql exp) throws E;

		public R visit(P params, PragmaExpVar exp) throws E;

		public R visit(P params, PragmaExpJs exp) throws E;

		public R visit(P params, PragmaExpProc exp) throws E;

		public R visit(P params, PragmaExpBitsy exp) throws E;

		public <X, Y> R visit(P params, PragmaExpJsonInstExport<X, Y> exp) throws E;

		public <X, Y> R visit(P params, PragmaExpRdfInstExport<X, Y> exp) throws E;

		public <X, Y> R visit(P params, PragmaExpRdfDirectExport<X, Y> exp) throws E;

		public <X, Y> R visit(P params, PragmaExpToJdbcInst<X, Y> exp) throws E;

		public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> R visit(P params,
				PragmaExpToJdbcTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> exp) throws E;

		public R visit(P params, PragmaExpToJdbcQuery exp) throws E;

		public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> R visit(P params,
				PragmaExpToCsvTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> exp) throws E;

		public R visit(P params, PragmaExpCheck2 exp) throws E;

		public <X, Y> R visit(P params, PragmaExpToCsvInst<X, Y> exp);

		public R visit(P params, PragmaExpTinkerpopInstExport exp);

		public R visit(P params, PragmaExpTinkerpop exp);

		public <X, Y> R visit(P params, PragmaExpToExcelInst<X, Y> pragmaExpToExcel);

	}

	public abstract <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E;

	//////////

	public static final class PragmaExpVar extends PragmaExp {
		public final String var;

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public boolean isVar() {
			return true;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singleton(new Pair<>(var, Kind.PRAGMA));
		}

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		public PragmaExpVar(String var) {
			this.var = var;
		}

		@Override
		public Pragma eval0(AqlEnv env, boolean isC) {
			return env.defs.ps.get(var);
		}

		@Override
		public int hashCode() {
			return var.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PragmaExpVar other = (PragmaExpVar) obj;
			return var.equals(other.var);
		}

		@Override
		public String toString() {
			return var;
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
		}

		@Override
		public Unit type(AqlTyping G) {
			if (!G.defs.ps.containsKey(var)) {
				throw new RuntimeException("Not a command: " + var);
			}
			return G.defs.ps.get(var);
		}

	}

}
