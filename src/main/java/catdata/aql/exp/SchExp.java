package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Program;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.exp.TyExp.TyExpLit;
import catdata.aql.exp.TyExp.TyExpSch;
import catdata.aql.fdm.AqlPivot;

public abstract class SchExp extends Exp<Schema<Ty, En, Sym, Fk, Att>> {

	public static interface SchExpCoVisitor<R, P, E extends Exception> {
		public abstract SchExpEmpty visitSchExpEmpty(P params, R exp) throws E;

		public abstract <Gen, Sk, X, Y> SchExpInst<Gen, Sk, X, Y> visitSchExpInst(P params, R exp) throws E;

		public abstract SchExpLit visitSchExpLit(P params, R exp) throws E;

		public abstract <Gen, Sk, X, Y> SchExpPivot<Gen, Sk, X, Y> visitSchExpPivot(P params, R exp) throws E;

		public abstract SchExpVar visitSchExpVar(P params, R exp) throws E;

		public abstract SchExpRaw visitSchExpRaw(P params, R exp) throws E;

		public abstract SchExpColim visitSchExpColim(P params, R exp) throws E;

		public abstract SchExpDom visitSchExpDom(P params, R exp) throws E;

		public abstract SchExpCod visitSchExpCod(P params, R exp) throws E;

		public abstract SchExpSrc visitSchExpSrc(P params, R exp) throws E;

		public abstract SchExpDst visitSchExpDst(P params, R exp) throws E;

		public abstract SchExpJdbcAll visitSchExpJdbcAll(P params, R r);

		public abstract SchExpCsv visitSchExpCsv(P params, R r);

	}

	public abstract <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E;

	public static interface SchExpVisitor<R, P, E extends Exception> {
		public abstract R visit(P params, SchExpEmpty exp) throws E;

		public abstract <Gen, Sk, X, Y> R visit(P params, SchExpInst<Gen, Sk, X, Y> exp) throws E;

		public abstract R visit(P params, SchExpLit exp) throws E;

		public abstract <Gen, Sk, X, Y> R visit(P params, SchExpPivot<Gen, Sk, X, Y> exp) throws E;

		public abstract R visit(P params, SchExpVar exp) throws E;

		public abstract R visit(P params, SchExpRaw exp) throws E;

		public abstract <N> R visit(P params, SchExpColim exp) throws E;

		public abstract R visit(P param, SchExpDom schExpDom);

		public abstract R visit(P params, SchExpCod exp) throws E;

		public abstract R visit(P param, SchExpSrc schExpDom);

		public abstract R visit(P params, SchExpDst exp) throws E;

		public abstract R visit(P param, SchExpJdbcAll schExpJdbcAll);

		public abstract R visit(P param, SchExpCsv schExpCsv);

	}

	public abstract <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E;

	public abstract SchExp resolve(AqlTyping G, Program<Exp<?>> prog);

	public abstract TyExp type(AqlTyping G);

	@Override
	public Kind kind() {
		return Kind.SCHEMA;
	}

	@Override
	public Exp<Schema<Ty, En, Sym, Fk, Att>> Var(String v) {
		Exp<Schema<Ty, En, Sym, Fk, Att>> ret = new SchExpVar(v);
		return ret;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	public static class SchExpDom extends SchExp {

		public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
			return v.visit(param, this);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			exp.map(f);
		}

		public final QueryExp exp;

		public SchExpDom(QueryExp exp) {
			this.exp = exp;
		}

		@Override
		public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
			return this;
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public int hashCode() {
			return exp.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SchExpDom other = (SchExpDom) obj;
			if (exp == null) {
				if (other.exp != null)
					return false;
			} else if (!exp.equals(other.exp))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "dom_q " + exp;
		}

		@Override
		public Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
			return exp.eval(env, isC).src;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return exp.deps();
		}

		@Override
		public TyExp type(AqlTyping G) {
			return exp.type(G).first.type(G);
		}

		@Override
		public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitSchExpCod(params, r);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

	}

	public static class SchExpCod extends SchExp {

		public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
			return v.visit(param, this);
		}

		public final QueryExp exp;

		public SchExpCod(QueryExp exp) {
			this.exp = exp;
		}

		@Override
		public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
			return this;
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public int hashCode() {
			return exp.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SchExpCod other = (SchExpCod) obj;
			return exp.equals(other.exp);
		}

		@Override
		public String toString() {
			return "cod_q " + exp;
		}

		@Override
		public Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
			return exp.eval(env, isC).dst;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return exp.deps();
		}

		@Override
		public TyExp type(AqlTyping G) {
			return exp.type(G).second.type(G);
		}

		@Override
		public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitSchExpCod(params, r);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			exp.map(f);
		}

	}

	public static class SchExpSrc extends SchExp {

		public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
			return v.visit(param, this);
		}

		public final MapExp exp;

		public SchExpSrc(MapExp exp) {
			this.exp = exp;
		}

		@Override
		public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
			return this;
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public int hashCode() {
			return exp.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SchExpSrc other = (SchExpSrc) obj;
			return exp.equals(other.exp);
		}

		@Override
		public String toString() {
			return "dom_m " + exp;
		}

		@Override
		public Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
			return exp.eval(env, isC).src;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return exp.deps();
		}

		@Override
		public TyExp type(AqlTyping G) {
			return exp.type(G).first.type(G);
		}

		@Override
		public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitSchExpSrc(params, r);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			exp.map(f);
		}

	}

	public static class SchExpDst extends SchExp {

		public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
			return v.visit(param, this);
		}

		public final MapExp exp;

		public SchExpDst(MapExp exp) {
			this.exp = exp;
		}

		@Override
		public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
			return this;
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public int hashCode() {
			return exp.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SchExpDst other = (SchExpDst) obj;
			return exp.equals(other.exp);
		}

		@Override
		public String toString() {
			return "cod_m " + exp;
		}

		@Override
		public Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
			return exp.eval(env, isC).dst;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return exp.deps();
		}

		@Override
		public TyExp type(AqlTyping G) {
			return exp.type(G).second.type(G);
		}

		@Override
		public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitSchExpDst(params, r);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			exp.map(f);
		}

	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class SchExpPivot<Gen, Sk, X, Y> extends SchExp {
		public final InstExp<Gen, Sk, X, Y> I;
		public final Map<String, String> options;

		public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
			return v.visit(param, this);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			I.map(f);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			set.add(AqlOption.require_consistency);
			set.add(AqlOption.allow_java_eqs_unsafe);
		}

		@Override
		public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitSchExpPivot(params, r);
		}

		public SchExpPivot(InstExp<Gen, Sk, X, Y> I, List<Pair<String, String>> options) {
			this.options = Util.toMapSafely(options);
			this.I = I;
		}

		@Override
		public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
			return this;
		}

		@Override
		protected Map<String, String> options() {
			return options;
		}

		@Override
		public Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
			AqlOptions strat = new AqlOptions(options, env.defaults);
			Schema<Ty, En, Sym, Fk, Att> l = new AqlPivot<>(I.eval(env, isC), strat).intI;
			return l;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			result = prime * result + ((options == null) ? 0 : options.hashCode());
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
			SchExpPivot<?, ?, ?, ?> other = (SchExpPivot<?, ?, ?, ?>) obj;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			return true;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return I.deps();
		}

		@Override
		public String toString() {
			return "pivot " + I;
		}

		@Override
		public TyExp type(AqlTyping G) {
			return I.type(G).type(G);
		}

	}
	//////////////////////////////////////////////////////////////

	public static final class SchExpInst<Gen, Sk, X, Y> extends SchExp {
		public final InstExp<Gen, Sk, X, Y> inst;

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			inst.map(f);
		}

		@Override
		public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitSchExpInst(params, r);
		}

		public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
			return v.visit(param, this);
		}

		@Override
		public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
			return inst.type(G);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return inst.deps();
		}

		public SchExpInst(InstExp<Gen, Sk, X, Y> inst) {
			if (inst == null) {
				throw new RuntimeException("Attempt to get schema for null instance");
			}
			this.inst = inst;
		}

		@Override
		public int hashCode() {
			return inst.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SchExpInst<?, ?, ?, ?> other = (SchExpInst<?, ?, ?, ?>) obj;
			return inst.equals(other.inst);
		}

		@Override
		public String toString() {
			return "schemaOf " + inst;
		}

		@Override
		public Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
			return inst.eval(env, isC).schema();
		}

		@Override
		public TyExp type(AqlTyping G) {
			return new TyExpSch(this);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class SchExpEmpty extends SchExp {

		public final TyExp typeSide;

		public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
			return new SchExpEmpty(typeSide.resolve(prog));
		}

		@Override
		public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitSchExpEmpty(params, r);
		}

		public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
			return v.visit(param, this);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return typeSide.deps();
		}

		public SchExpEmpty(TyExp typeSide) {
			this.typeSide = typeSide;
		}

		@Override
		public int hashCode() {
			return typeSide.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SchExpEmpty other = (SchExpEmpty) obj;
			return typeSide.equals(other.typeSide);
		}

		@Override
		public String toString() {
			return "empty : " + typeSide;
		}

		@Override
		public Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
			return Schema.terminal(typeSide.eval(env, isC));
		}

		@Override
		public TyExp type(AqlTyping G) {
			return typeSide;
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			typeSide.map(f);
		}

	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class SchExpVar extends SchExp {
		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
			return v.visit(param, this);
		}

		@Override
		public boolean isVar() {
			return true;
		}

		@Override
		public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitSchExpVar(params, r);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
		}

		@Override
		public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
			if (!prog.exps.containsKey(var)) {
				throw new RuntimeException("Unbound typeside variable: " + var);
			}
			Exp<?> x = prog.exps.get(var);
			if (!(x instanceof SchExp)) {
				throw new RuntimeException(
						"Variable " + var + " is bound to something that is not a schema, namely\n\n" + x);
			}
			SchExp texp = (SchExp) x;
			return texp.resolve(G, prog);
		}

		public final String var;

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singleton(new Pair<>(var, Kind.SCHEMA));
		}

		public SchExpVar(String var) {
			this.var = var;
		}

		@Override
		public Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
			return env.defs.schs.get(var);
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
			SchExpVar other = (SchExpVar) obj;
			return var.equals(other.var);
		}

		@Override
		public String toString() {
			return var;
		}

		@Override
		public TyExp type(AqlTyping G) {
			TyExp e = G.defs.schs.get(var);
			if (e == null) {
				throw new RuntimeException("Not a schema: " + var);
			}
			return e;
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class SchExpLit extends SchExp {

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}

		public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
			return v.visit(param, this);
		}

		@Override
		public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitSchExpLit(params, r);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		public final Schema<Ty, En, Sym, Fk, Att> schema;

		public SchExpLit(Schema<Ty, En, Sym, Fk, Att> schema) {
			this.schema = schema;
		}

		@Override
		public Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
			return schema;
		}

		@Override
		public int hashCode() {
			return schema.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SchExpLit other = (SchExpLit) obj;
			return schema.equals(other.schema);
		}

		@Override
		public String toString() {
			return ("constant " + schema).trim();
		}

		@Override
		public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
			return this;
		}

		@Override
		public TyExp type(AqlTyping G) {
			return new TyExpLit(schema.typeSide);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}
	}
}
