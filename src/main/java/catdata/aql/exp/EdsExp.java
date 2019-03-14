package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Constraints;
import catdata.aql.Kind;
import catdata.aql.Schema;

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
	}

	// public abstract <R, P, E extends Exception> EdsExp coaccept(P params,
	// EdsExpCoVisitor<R,P,E> v) throws E;

	public static interface EdsExpVisitor<R, P, E extends Exception> {
		public R visit(P params, EdsExpVar exp) throws E;

		public R visit(P params, EdsExpRaw exp) throws E;

		public R visit(P params, EdsExpSch exp) throws E;
	}

	public abstract <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E;

	/////////////////////////////////////////////////////////////////////////////////////////

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
			return env.defs.eds.get(var);
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
			return true;
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
			Schema<Ty, En, Sym, Fk, Att> ret = sch.eval(env, isC);
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
}
