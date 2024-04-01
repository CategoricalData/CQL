package catdata.cql.exp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.LocStr;
import catdata.Pair;
import catdata.Triple;
import catdata.Unit;
import catdata.cql.Kind;
import catdata.cql.TypeSide;
import catdata.cql.AqlOptions.AqlOption;

public class TyExpExcel extends TyExp {

	@Override
	public <R, P, E extends Exception> TyExp coaccept(P params, TyExpCoVisitor<R, P, E> v, R r) throws E {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, TyExpVisitor<R, P, E> v) throws E {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		
	}

	@Override
	public Object type(AqlTyping G) {
		return Unit.unit;
	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}
	static TypeSide<String,Sym> it;
	@Override
	protected TypeSide<String, Sym> eval0(AqlEnv env, boolean isCompileTime) {
		if (it != null) return it;
		
		List<TyExp> imports = new LinkedList<>();
		List<LocStr> types = Collections.emptyList();
		List<Pair<LocStr, Pair<List<String>, String>>> functions = Collections.emptyList();
	    List<Pair<Integer, Triple<List<Pair<String, String>>, RawTerm, RawTerm>>> eqsX = new LinkedList<>();
	    List<Pair<LocStr, String>> java_tys_string = new LinkedList<>();
	    java_tys_string.add(new Pair<>(new LocStr(0, "Number"), "java.lang.Double"));
	    java_tys_string.add(new Pair<>(new LocStr(0, "String"), "java.lang.String"));
	    java_tys_string.add(new Pair<>(new LocStr(0, "Boolean"), "java.lang.Boolean"));
	    
	    List<Pair<LocStr, String>> java_parser_string = new LinkedList<>();
	    java_parser_string.add(new Pair<>(new LocStr(0, "Number"), "x => java.lang.Double.parseDouble(x)"));
	    java_parser_string.add(new Pair<>(new LocStr(0, "String"), "x => x"));
	    java_parser_string.add(new Pair<>(new LocStr(0, "Boolean"), "x => java.lang.Boolean.parseBoolean(x)"));
	    
	    List<Pair<LocStr, Triple<List<String>, String, String>>> java_fns_string = new LinkedList<>();
	    List<String> l = new LinkedList<>();
	    l.add("Number");
	    l.add("Number");
	    java_fns_string.add(new Pair<>(new LocStr(0, "+"), new Triple<>(l,"Number","(x,y) => x+y")));
	    java_fns_string.add(new Pair<>(new LocStr(0, "-"), new Triple<>(l,"Number","(x,y) => x-y")));
	    java_fns_string.add(new Pair<>(new LocStr(0, "/"), new Triple<>(l,"Number","(x,y) => x/y")));
	    java_fns_string.add(new Pair<>(new LocStr(0, "*"), new Triple<>(l,"Number","(x,y) => x*y")));
	    java_fns_string.add(new Pair<>(new LocStr(0, "MIN"), new Triple<>(l,"Number","(x,y) => java.lang.Double.min(x,y)")));
	    java_fns_string.add(new Pair<>(new LocStr(0, "MAX"), new Triple<>(l,"Number","(x,y) => java.lang.Double.max(x,y)")));

	    List<Pair<String, String>> options = new LinkedList<>();
	      
		return new TyExpRaw(imports, types, functions, eqsX, java_tys_string, java_parser_string, java_fns_string, options).eval0(env, isCompileTime);
		
	}

	@Override
	public int hashCode() {
		return 117;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof TyExpExcel);
	}

	@Override
	public String toString() {
		return "excel";
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Collections.emptyList();
	}

}
