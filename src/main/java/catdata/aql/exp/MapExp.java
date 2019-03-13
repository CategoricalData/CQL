package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.exp.SchExp.SchExpLit;


public abstract class MapExp extends Exp<Mapping<Ty,En,Sym,Fk,Att,En,Fk,Att>> {
	
	@Override
	public Kind kind() {
		return Kind.MAPPING;
	}
	
	public abstract Pair<SchExp, SchExp> type(AqlTyping G);
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Exp<Mapping<Ty,En,Sym,Fk,Att,En,Fk,Att>> Var(String v) {
		Exp ret = new MapExpVar(v);
		return ret;
	}
	
	public static interface MapExpCoVisitor<R,P, E extends Exception> {
		 public MapExpComp visitMapExpComp(P params, R exp) throws E;
		 public  MapExpId visitMapExpId(P params, R exp) throws E;
		 public  MapExpLit visitMapExpLit(P params, R exp) throws E;
		 public <Gen,Sk,X,Y> MapExpPivot<Gen,Sk,X,Y> visitMapExpPivot(P params, R exp) throws E;
		 public MapExpVar visitMapExpVar(P params, R exp) throws E; 
		 public MapExpRaw visitMapExpRaw(P params, R exp) throws E;
		 public <N> MapExpColim<N> visitMapExpColim(P params, R exp) throws E;
	}

	public abstract <R,P,E extends Exception> MapExp
	coaccept(P params, MapExpCoVisitor<R,P,E> v, R r) throws E;

	
	public static interface MapExpVisitor<R,P, E extends Exception> {
		 public <Gen, Sk, X, Y> R visit(P params, MapExpPivot<Gen, Sk, X, Y> mapExpPivot) throws E;
		 public R visit(P params, MapExpId exp) throws E;
		 public R visit(P params, MapExpLit exp) throws E;
		 public R visit(P params, MapExpComp exp) throws E;
		 public R visit(P params, MapExpVar exp) throws E; 
		 public R visit(P params, MapExpRaw exp) throws E;
		 public <N> R visit(P params, MapExpColim<N> exp) throws E;
	}
	
	public abstract <R,P,E extends Exception> R accept(P params, MapExpVisitor<R,P, E> v) throws E;
	
	

	
	public static final class MapExpVar extends MapExp {
		public final String var;
		
		@Override
		public boolean isVar() {
			return true;
		}
		public <R,P,E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}
		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			
		}
		@Override
		public <R, P, E extends Exception> MapExp coaccept(
				P params, MapExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitMapExpVar(params, r);
		}
		
		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}
		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singleton(new Pair<>(var, Kind.MAPPING));
		}
	
		public MapExpVar(String var) {
			this.var = var;
		}

		@Override
		public Mapping<catdata.aql.exp.Ty, En, catdata.aql.exp.Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
			return env.defs.maps.get(var);
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
			MapExpVar other = (MapExpVar) obj;
			return var.equals(other.var);
		}

		@Override
		public String toString() {
			return var;
		}
		@SuppressWarnings("unchecked")
		@Override
		public Pair<SchExp, SchExp> type(AqlTyping G) {		
			if (!G.defs.maps.containsKey(var)) {
				throw new RuntimeException("Not a mapping: " + var);
			}
			return (Pair<SchExp, SchExp>) ((Object)G.defs.maps.get(var));
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			
		}

	}

/////////////////////////////////////////////////////////////////////
	
	public static final class MapExpLit extends MapExp {
		
		public <R,P,E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}
		
		@Override
		public <R, P, E extends Exception> MapExp coaccept(
				P params, MapExpCoVisitor<R, P, E> v, R r) throws E {
			return v.visitMapExpLit(params, r);
		}
		
		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}
		
		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}
		
		public final Mapping<Ty,En,Sym,Fk,Att,En,Fk,Att> map;
		
		public MapExpLit(Mapping<Ty,En,Sym,Fk,Att,En,Fk,Att> map) {
			this.map = map;
		}

		@Override
		public Mapping<Ty,En,Sym,Fk,Att,En,Fk,Att> eval0(AqlEnv env, boolean isC) {
			return map;
		}

		@Override
		public int hashCode() {
			return map.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MapExpLit other = (MapExpLit) obj;
            return map.equals(other.map);
        }

		@Override
		public String toString() {
			return ("constant " + map).trim();
		}

		@Override
		public Pair<SchExp, SchExp> type(AqlTyping G) {
			return new Pair<>(new SchExpLit(map.src), new SchExpLit(map.dst));
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			
		}


		
	}
}