package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Query;
import catdata.aql.Schema;
import catdata.aql.exp.SchExp.SchExpLit;

public abstract class QueryExp extends Exp<Query<Ty, En, Sym, Fk, Att, En, Fk, Att>> {

	@Override
	public Kind kind() {
		return Kind.QUERY;
	}

	public abstract Pair<SchExp, SchExp> type(AqlTyping G);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Exp<Query<Ty, En, Sym, Fk, Att, En, Fk, Att>> Var(String v) {
		Exp ret = new QueryExpVar(v);
		return ret;
	}

	public static interface QueryExpVisitor<R, P, E extends Exception> {
		public R visit(P params, QueryExpCompose exp) throws E;

		public R visit(P params, QueryExpId exp) throws E;

		public R visit(P params, QueryExpLit exp) throws E;

		public R visit(P params, QueryExpVar exp) throws E;

		public R visit(P params, QueryExpRaw exp) throws E;

		public R visit(P params, QueryExpDeltaCoEval exp) throws E;

		public R visit(P params, QueryExpDeltaEval exp) throws E;

		public R visit(P params, QueryExpRawSimple exp) throws E;

		public R visit(P params, QueryExpFromCoSpan exp) throws E;

		public R visit(P params, QueryExpFromEds exp);
	}

	public abstract <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E;

	///////////////////////////////////////////////////////////////////////////////////////////

	public static final class QueryExpId extends QueryExp {

		public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			if (sch2.isEmpty()) {
				return sch.deps();
			}
			return Util.union(sch.deps(), sch2.get().deps());
		}

		public final SchExp sch;
		public final Optional<SchExp> sch2;

		public QueryExpId(SchExp sch) {
			this.sch = sch;
			this.sch2 = Optional.empty();
		}

		public QueryExpId(SchExp sch, SchExp sch2) {
			this.sch = sch;
			this.sch2 = Optional.of(sch2);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + (sch.hashCode());
			result = prime * result + (sch2.hashCode());
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
			QueryExpId other = (QueryExpId) obj;
			return sch.equals(other.sch) && sch2.equals(other.sch2);
		}

		@Override
		public String toString() {
			if (sch2.isEmpty()) {
				return "identity " + sch;
			}
			return "include " + sch + " " + sch2.get();
		}

		@Override
		public Query<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
			Schema<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att> s = sch
					.eval(env, isC);
			if (sch2.isEmpty()) {
				return Query.id(new AqlOptions(env.defaults, AqlOption.dont_validate_unsafe, true), s, s);
			}
			return Query.id(new AqlOptions(env.defaults, AqlOption.dont_validate_unsafe, true),
					sch2.get().eval(env, isC), s);

		}

		@Override
		public Pair<SchExp, SchExp> type(AqlTyping G) {
			if (sch2.isEmpty()) {
				return new Pair<>(sch, sch);
			}
			return new Pair<>(sch2.get(), sch); // reverse directed vs mapping, transform
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			sch.map(f);
			if (sch2.isPresent()) {
				sch2.get().map(f);
			}
		}

	}

	////////////////////////////////////////////////////////////

	public static final class QueryExpVar extends QueryExp {
		public final String var;

		public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

		@Override
		public boolean isVar() {
			return true;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singleton(new Pair<>(var, Kind.QUERY));
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public Query<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, catdata.aql.exp.En, catdata.aql.exp.Fk, catdata.aql.exp.Att> eval0(
				AqlEnv env, boolean isC) {
			return env.defs.qs.get(var);
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
			QueryExpVar other = (QueryExpVar) obj;
			return var.equals(other.var);
		}

		@Override
		public String toString() {
			return var;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Pair<SchExp, SchExp> type(AqlTyping G) {
			if (!G.defs.qs.containsKey(var)) {
				throw new RuntimeException("Not a query: " + var);
			}
			return (Pair<SchExp, SchExp>) ((Object) G.defs.qs.get(var));
		}

		public QueryExpVar(String var) {
			this.var = var;
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

	}

	public static final class QueryExpLit extends QueryExp {

		public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		public final Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q;

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}

		public QueryExpLit(Query<Ty, En, Sym, Fk, Att, En, Fk, Att> q) {
			this.q = q;
		}

		@Override
		public Query<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
			return q;
		}

		@Override
		public int hashCode() {
			return q.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			QueryExpLit other = (QueryExpLit) obj;
			return q.equals(other.q);
		}

		@Override
		public String toString() {
			return "QueryExpLit " + q;
		}

		@Override
		public Pair<SchExp, SchExp> type(AqlTyping G) {
			return new Pair<>(new SchExpLit(q.src), new SchExpLit(q.dst));
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

	}

}
