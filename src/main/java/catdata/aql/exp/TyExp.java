package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Program;
import catdata.Unit;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.TypeSide;

public abstract class TyExp extends Exp<TypeSide<Ty, Sym>> {
	
	public static interface TyExpCoVisitor<R, P, E extends Exception> {
		public abstract  TyExpSch visitTyExpSch(P params, R r) throws E;
		public abstract TyExpEmpty visitTyExpEmpty(P params, R r) throws E;
		public abstract  TyExpLit visitTyExpLit(P params, R r) throws E;
		public abstract  TyExpVar visitTyExpVar(P params, R r) throws E;
		public abstract TyExpRaw visitTyExpRaw(P params, R r) throws E;
		public abstract TyExpSql visitTyExpSql(P params, R r) throws E;		
	}
	
	public abstract <R,P,E extends Exception> TyExp
		coaccept(P params, TyExpCoVisitor<R,P,E> v, R r) throws E;

	public static interface TyExpVisitor<R, P, E extends Exception> {
		public abstract  R visit(P params, TyExpSch exp) throws E;
		public abstract R visit(P params, TyExpEmpty exp) throws E;
		public abstract  R visit(P params, TyExpLit exp) throws E;
		public abstract  R visit(P params, TyExpVar exp) throws E;
		public abstract R visit(P params, TyExpRaw exp) throws E;
		public abstract R visit(P params, TyExpSql exp) throws E;
	}
	
	public abstract <R,P,E extends Exception> R accept(P params, TyExpVisitor<R,P,E> v) throws E;
	
	public TyExp resolve(@SuppressWarnings("unused") Program<Exp<?>> prog) {
		return this;
	}
	
	@Override
	public Kind kind() {
		return Kind.TYPESIDE;
	}
	
	@Override
	public Exp<TypeSide<Ty, Sym>> Var(String v) {
		Exp<TypeSide<Ty, Sym>> ret = new TyExpVar(v);
		return ret;
	}
 
////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class TyExpSch extends TyExp {
		
		@Override
		public <R,P,E extends Exception> R accept(P params, TyExpVisitor<R,P,E> v) throws E {
			return v.visit(params, this);
		}
		
		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}
		
		public final SchExp schema;

		public TyExpSch(SchExp schema) {
			this.schema = schema;
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
			TyExpSch other = (TyExpSch) obj;
            return schema.equals(other.schema);
        }

		@Override
		public String toString() {
			return "typesideOf " + schema;
		}

		@Override
		public synchronized TypeSide<Ty, Sym> eval0(AqlEnv env, boolean isC) {
			return schema.eval(env, isC).typeSide;
		}
		
		
		@Override
		public Collection<Pair<String, Kind>> deps() {
			return schema.deps();
		}

		@Override
		public <R, P, E extends Exception> TyExp coaccept(P params, TyExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitTyExpSch(params, r);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}
		
		public Unit type(AqlTyping t) {
			schema.type(t);
			return Unit.unit;
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			schema.map(f);
		}
		
	}
	 
////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class TyExpEmpty extends TyExp {
		public Unit type(AqlTyping t) {
			return Unit.unit;
		}
		@Override
		public <R,P,E extends Exception> R accept(P params, TyExpVisitor<R,P,E> v) throws E {
			return v.visit(params, this);
		}
		
		@Override
		public <R, P, E extends Exception> TyExpEmpty coaccept(P params, TyExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitTyExpEmpty(params, r);
		}
		
		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}
		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			
		}
		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}
			
		@Override
		public synchronized TypeSide<Ty, Sym> eval0(AqlEnv env, boolean isC) {
			return TypeSide.initial(env.defaults);
		}
	
		@Override
		public String toString() {
			return "empty";
		}
	
		@Override
		public int hashCode() {
			return 0;
		}
	
		@Override
		public boolean equals(Object o) {
			return (o != null && o instanceof TyExpEmpty);
		}
		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
		}
		
		
	}
	
	//////////////////////////////////////////////////////////
	

	
////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class TyExpLit extends TyExp {
		public final TypeSide<Ty,Sym> typeSide;
		
		public Unit type(AqlTyping t) {
			return Unit.unit;
		}
		
		@Override
		public <R,P,E extends Exception> R accept(P params, TyExpVisitor<R,P,E> v) throws E {
			return v.visit(params, this);
		}
		
		@Override
		public <R, P, E extends Exception> TyExpLit coaccept(P params, TyExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitTyExpLit(params, r);
		}
		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			
		}
		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}
		
		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}
		
		public TyExpLit(TypeSide<Ty, Sym> typeSide) {
			this.typeSide = typeSide;
		}

		@Override
		public synchronized TypeSide eval0(AqlEnv env, boolean isC) {
			return typeSide;
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
			TyExpLit other = (TyExpLit) obj;
            return typeSide.equals(other.typeSide);
        }

		@Override
		public String toString() {
			return ("constant " + typeSide).trim();
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			
		}

		
		
	}
	
////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static final class TyExpVar extends TyExp {
		public final String var;
		
		@Override
		public <R,P,E extends Exception> R accept(P params, TyExpVisitor<R,P,E> v) throws E {
			return v.visit(params, this);
		}
		
		@Override
		public <R, P, E extends Exception> TyExpVar coaccept(P params, TyExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitTyExpVar(params, r);
		}
		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			
		}
		@Override
		public TyExp resolve(Program<Exp<?>> prog) {
			if (!prog.exps.containsKey(var)) {
				throw new RuntimeException("Unbound typeside variable: " + var);
			}
			Exp<?> x = prog.exps.get(var);
			if (!(x instanceof TyExp)) {
				throw new RuntimeException("Variable " + var + " is bound to something that is not a typeside, namely\n\n" + x);
			}
			@SuppressWarnings("unchecked")
			TyExp texp = (TyExp) x;
			return texp.resolve(prog);
		}
		
		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}
		
		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singletonList(new Pair<>(var, Kind.TYPESIDE));
		}
		
		public TyExpVar(String var) {
			this.var = var;
		}

		@Override
		public synchronized TypeSide<Ty, Sym> eval0(AqlEnv env, boolean isC) {
			return env.defs.tys.get(var);
		}
	
		public Unit type(AqlTyping t) {
			if (!t.defs.tys.containsKey(var)) {
				throw new RuntimeException("Undefined variable: " + var);
			}
			return Unit.unit;
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
			TyExpVar other = (TyExpVar) obj;
            return var.equals(other.var);
        }

		@Override
		public String toString() {
			return var;
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			
		}

		@Override
		public boolean isVar() {
			return true;
		}
		
	}

	
	
}