package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.cql.Constraints;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.SchExp.SchExpLit;

public abstract class EdsExp extends Exp<Constraints> {

	@Override
	public Kind kind() {
		return Kind.CONSTRAINTS;
	}

	public abstract SchExp type(AqlTyping G);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Exp<Constraints> Var(String v) {
		Exp ret = new EdsExpVar(v);
		return ret;
	}

	/////////////////////////////////////////////////////////////////////////////////////////

	public static interface EdsExpCoVisitor<R, P, E extends Exception> {
		public EdsExpVar visitEdsExpVar(P params, R exp) throws E;

		public EdsExpRaw visitEdsExpRaw(P params, R exp) throws E;

		public EdsExpSch visitEdsExpSch(P params, R exp) throws E;

		public EdsExpSqlNull visitEdsExpSqlNull(P params, R exp) throws E;

		public EdsExpInclude visitEdsExpInclude(P params, R exp) throws E;

		public EdsExpTinkerpop visitEdsExpTinkerpop(P params, R exp) throws E;

		public EdsExpFromMsCatalog visitEdsExpFromMsCatalog(P params, R exp) throws E;

		public EdsExpSigma visitEdsExpSigma(P params, R exp) throws E;

		public EdsExpSql visitEdsExpSql(P params, R exp) throws E;

		public EdsExpLearn visitEdsExpLearn(P params, R exp) throws E;
	}

	public static interface EdsExpVisitor<R, P, E extends Exception> {
		public R visit(P params, EdsExpVar exp) throws E;

		public R visit(P params, EdsExpRaw exp) throws E;

		public R visit(P params, EdsExpSigma exp) throws E;

		public R visit(P params, EdsExpSch exp) throws E;

		public R visit(P params, EdsExpSqlNull exp) throws E;

		public R visit(P params, EdsExpInclude exp) throws E;

		public R visit(P params, EdsExpTinkerpop exp) throws E;

		public R visit(P params, EdsExpFromMsCatalog exp) throws E;

		public R visit(P params, EdsExpSql exp) throws E;
		
		public R visit(P params, EdsExpLit exp) throws E;

		public R visit(P params, EdsExpFromOracle exp) throws E;

		public R visit(P param, EdsExpFromMySql exp) throws E;
		
		public R visit(P param, EdsExpLearn exp) throws E;

	}
	
	
	public abstract <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E;

	/////////////////////////////////////////////////////////////////////////////////////////

	public static final class EdsExpLit extends EdsExp {

		@Override
		public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public boolean isVar() {
			return false;
		}

		public final Constraints c;

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}

		public EdsExpLit(Constraints c) {
			this.c = c;
		}

		@Override
		public synchronized Constraints eval0(AqlEnv env, boolean isC) {
			return c;
		}

		@Override
		public int hashCode() {
			return c.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EdsExpLit other = (EdsExpLit) obj;
			return c.equals(other.c);
		}

		@Override
		public String toString() {
			return c.toString();
		}

		@Override
		public SchExp type(AqlTyping G) {
			return new SchExpLit(c.schema);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
		}
	}
	/////

	public static final class EdsExpVar extends EdsExp {

		@Override
		public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public boolean isVar() {
			return true;
		}

		public final String var;

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singleton(new Pair<>(var, Kind.CONSTRAINTS));
		}

		public EdsExpVar(String var) {
			this.var = var;
		}

		@Override
		public synchronized Constraints eval0(AqlEnv env, boolean isC) {
			Constraints ret = env.defs.eds.get(var);
			if (ret == null) {
				Util.anomaly();
			}
			return ret;
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
			EdsExpVar other = (EdsExpVar) obj;
			return var.equals(other.var);
		}

		@Override
		public String toString() {
			return var;
		}

		@Override
		public SchExp type(AqlTyping G) {
			if (!G.defs.eds.containsKey(var)) {
				throw new RuntimeException("Not constraints: " + var);
			}
			return G.defs.eds.get(var);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
		}
	}

	public static final class EdsExpSch extends EdsExp {

		@Override
		public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public boolean isVar() {
			return false;
		}

		public final SchExp sch;

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return sch.deps();
		}

		public EdsExpSch(SchExp sch) {
			this.sch = sch;
		}

		@Override
		public synchronized Constraints eval0(AqlEnv env, boolean isC) {
			Schema<String, String, Sym, Fk, Att> ret = sch.eval(env, isC);
			return new Constraints(env.defaults, ret);
		}

		@Override
		public int hashCode() {
			return sch.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EdsExpSch other = (EdsExpSch) obj;
			return sch.equals(other.sch);
		}

		@Override
		public String toString() {
			return "fromSchema " + sch;
		}

		@Override
		public SchExp type(AqlTyping G) {
			sch.type(G);
			return sch;
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			sch.map(f);
		}
	}

	///////

	///////////////////////////////////

}
