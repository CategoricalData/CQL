package catdata.aql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Constraints;
import catdata.aql.ED;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.SqlTypeSide2;
import catdata.aql.Term;
import catdata.aql.TypeSide;
import catdata.aql.Var;
import catdata.aql.exp.SchExp.SchExpEmpty;
import gnu.trove.map.hash.THashMap;

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

		public R visit(P params, EdsExpSqlNull exp) throws E;
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

	///////

	public static final class EdsExpSqlNull extends EdsExp {

		private final TyExp parent;

		public EdsExpSqlNull(TyExp parent) {
			this.parent = parent;
		}

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

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return parent.deps();
		}

		@Override
		public synchronized Constraints eval0(AqlEnv env, boolean isC) {
			TypeSide<Ty,Sym> ts = parent.eval(env, isC);
			AqlOptions op = new AqlOptions(options(), env.defaults);

			return makeEds(Schema.terminal(SqlTypeSide2.FOR_TY.make(ts, op)), op);
		}

		public static Constraints makeEds(Schema<Ty, En, Sym, Fk, Att> schT, AqlOptions op) {
			LinkedList<ED> ret = new LinkedList<>();
			Var x = Var.Var("x");
			Var y = Var.Var("y");
			Var z = Var.Var("z");
			Term<Ty, En, Sym, Fk, Att, Void, Void> t = Term.Sym(Sym.Sym("true"), Collections.emptyList());
			Term<Ty, En, Sym, Fk, Att, Void, Void> f = Term.Sym(Sym.Sym("false"), Collections.emptyList());

			//	Term<Ty, En, Sym, Fk, Att, Void, Void> u = Term.Sym(Sym.Sym("null_Boolean"), Collections.emptyList());

			List<Term<Ty, En, Sym, Fk, Att, Void, Void>> lxx = new ArrayList<>(2);
			lxx.add(Term.Var(x));
			lxx.add(Term.Var(x));

			List<Term<Ty, En, Sym, Fk, Att, Void, Void>> lxy = new ArrayList<>(2);
			lxy.add(Term.Var(x));
			lxy.add(Term.Var(y));

			List<Term<Ty, En, Sym, Fk, Att, Void, Void>> lyx = new ArrayList<>(2);
			lyx.add(Term.Var(y));
			lyx.add(Term.Var(x));

			List<Term<Ty, En, Sym, Fk, Att, Void, Void>> lyz = new ArrayList<>(2);
			lyz.add(Term.Var(y));
			lyz.add(Term.Var(z));

			List<Term<Ty, En, Sym, Fk, Att, Void, Void>> lxz = new ArrayList<>(2);
			lxz.add(Term.Var(x));
			lxz.add(Term.Var(z));

			for (Ty ty : schT.typeSide.tys) {
				if (ty.str.equals("Bool")) {
					continue;
				}
				Term<Ty, En, Sym, Fk, Att, Void, Void> xx  = Term.Sym(Sym.Sym("eq_" + ty.str), lxx);
				Term<Ty, En, Sym, Fk, Att, Void, Void> xx0 = Term.Sym(Sym.Sym("isFalse"), Util.list(xx));
				//Term<Ty, En, Sym, Fk, Att, Void, Void> xx1 = Term.Sym(Sym.Sym("not"), Util.list(xx0));
				ret.add(new ED(Collections.singletonMap(x, Chc.inLeft(ty)), Collections.emptyMap(),
						Collections.emptySet(), Collections.singleton(new Pair<>(xx0, f)), false, op));

				Term<Ty, En, Sym, Fk, Att, Void, Void> xy = Term.Sym(Sym.Sym("eq_" + ty.str), lxy);
				Term<Ty, En, Sym, Fk, Att, Void, Void> yx = Term.Sym(Sym.Sym("eq_" + ty.str), lyx);
				Map<Var, Chc<Ty, En>> m2 = new THashMap<>(2);
				m2.put(x, Chc.inLeft(ty));
				m2.put(y, Chc.inLeft(ty));
				ret.add(new ED(m2, Collections.emptyMap(), Collections.emptySet(),
						Collections.singleton(new Pair<>(xy, yx)), false, op));

				Map<Var, Chc<Ty, En>> m3 = new THashMap<>(2);
				m3.put(x, Chc.inLeft(ty));
				m3.put(y, Chc.inLeft(ty));
				m3.put(z, Chc.inLeft(ty));

				Term<Ty, En, Sym, Fk, Att, Void, Void> xy0 = Term.Sym(Sym.Sym("eq_" + ty.str), Util.list(Term.Var(x), Term.Var(y)));

				ret.add(new ED(m2, Collections.emptyMap(), Collections.singleton(new Pair<>(xy0, t)),
						Collections.singleton(new Pair<>(Term.Var(x), Term.Var(y))), false, op));
				//other congruences
				//todo: eq(a,b)=false -> x<>y?

			}

			return new Constraints(schT, ret, op);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
			EdsExpSqlNull other = (EdsExpSqlNull) obj;
			if (parent == null) {
				if (other.parent != null)
					return false;
			} else if (!parent.equals(other.parent))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "sqlNull " + parent;
		}

		@Override
		public SchExp type(AqlTyping G) {
			return new SchExpEmpty(new TyExpSqlNull(parent));
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			f.accept(parent);
			parent.mapSubExps(f);
		}
	}
}
