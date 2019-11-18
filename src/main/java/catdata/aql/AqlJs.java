package catdata.aql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import catdata.Pair;
import catdata.Util;
import gnu.trove.map.hash.THashMap;

public class AqlJs<Ty, Sym> {

	private static final String postfix = "\n\nPossibly helpful info: 32-bit Java integers cannot exceed 2 billion; if you need larger numbers please use strings \n\nPossibly helpful info: javascript arguments are accessed as input[0], input[1], etc.\n\nPossibly useful links: http://docs.oracle.com/javase/8/docs/api/ and http://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn/intro.html .";

	private final Map<Ty, String> iso1;
	private final Map<Sym, String> iso2;

	private final Map<Sym, Pair<List<Ty>, Ty>> syms;
	public final Map<Ty, String> java_tys;
	public final Map<Ty, String> java_parsers;
	public final Map<Sym, String> java_fns;

	private final ScriptEngine engine;

	{
		try {
			Class.forName("jdk.nashorn.api.scripting.NashornScriptEngine");
		} catch (Throwable thr) {
			thr.printStackTrace();
		}
	}

	public AqlJs(Map<Sym, Pair<List<Ty>, Ty>> syms, Map<Ty, String> java_tys, Map<Ty, String> java_parsers,
			Map<Sym, String> java_fns) {
		this.syms = syms;
		this.java_fns = java_fns;
		this.java_parsers = java_parsers;
		this.java_tys = java_tys;
		Object last = "";

		
		if (java_tys.isEmpty()) {
			engine = null;
			iso1 = null;
			iso2 = null;
			//cache = null;
			return;
		}
		iso1 = new THashMap<>(java_parsers.size());
		iso2 = new THashMap<>(java_parsers.size());
	//	cache = new THashMap<>(java_parsers.size());
		try {
			engine = new ScriptEngineManager().getEngineByName("nashorn");
			Compilable eng = (Compilable) engine;

			int i = 0;
			for (Ty k : java_parsers.keySet()) {
				String m = java_parsers.get(k);
				String w = "aqljsparser_" + i;
			
				if (m.equals("return input[0]")) {
				//	cache.put(k, new THashMap<>());
					iso1.put(k, "");
				} else {
					String ret = "function aqljsparser_" + i + "(input) { " + m + " }\n\n";
				//	cache.put(k, new THashMap<>());
					iso1.put(k, w);
					CompiledScript x = eng.compile(ret);
					x.eval();
				}
				
				i++;
				last = k;
			}
			i = 0;
			for (Sym k : java_fns.keySet()) {
				String ret = "function aqljsfn_" + i + "(input) { " + java_fns.get(k) + " }\n\n";
				iso2.put(k, "aqljsfn_" + i);
				i++;
				// engine.eval(ret);

				CompiledScript x = eng.compile(ret);
				x.eval();

				last = k;
			}
		} catch (Throwable e) {
			throw new RuntimeException(
					"In javascript execution, " + e.getMessage() + postfix + "\n\nlast binding evaluated: " + last);
		}
	}

	private synchronized Object apply(Sym name, List<Object> args) {
		try {
			// TODO aql check inputs and outputs here?
			Object ret = ((Invocable) engine).invokeFunction(iso2.get(name), args);
			check(syms.get(name).second, ret);
			return ret;
		} catch (Throwable e) {
			throw new RuntimeException("In javascript execution of " + name + " on arguments " + args + ", "
					+ Util.sep(args.stream().map(x -> x.getClass()).collect(Collectors.toList()), ",") + " , "
					+ e.getClass() + " error: " + e.getMessage() + postfix);
		}
	}

	//private Map<Ty, Map<String, Object>> cache = new THashMap<>();
	public synchronized Object parse(Ty name, String o) {
		String z = iso1.get(name);
		if (z == null) {
			throw new RuntimeException("In javascript execution of " + o + " no javascript definition for " + name);
		}
		//Map<String, Object> g = cache.get(name);
		//Object h = g.get(o);
		//if (h != null) {
		//	return h;
		//}
		try {
			Object ret;
			if (z.equals("")) {
				ret = o;
				if (!java_tys.get(name).equals("java.lang.String")) {
					check(name, ret);
				} 
			} else {
				ret = ((Invocable) engine).invokeFunction(z, Collections.singletonList(o));
				check(name, ret);
			}
			
		//	g.put(o, ret);
			return ret;
		} catch (Throwable e) {
			if (e.getMessage() != null && e.getMessage().contains("jdk.nashorn.internal.codegen.TypeMap")) {
				throw new RuntimeException(
						"The Java Runtime has suffered an internal error and the IDE must be restarted.\n\n"
								+ e.getMessage());
			}
			// e.printStackTrace();
			throw new RuntimeException("In javascript execution of " + o + " (of " + o.getClass()
					+ ") cannot convert to " + name + " error: " + e.getMessage() + postfix
					+ "\n\nPossible fix: check the java_constants of the typeside for type conversion errors.");
		}
	}

	private void check(Ty ty, Object o) {
		if (o == null) {
			throw new RuntimeException("evaluation return null." + postfix);
		}
		String clazz = java_tys.get(ty);
		Class<?> c = Util.load(clazz);
		if (!c.isInstance(o)) {
			throw new RuntimeException(o + " does not have type " + c + ", has type " + o.getClass());
		}
	}

	public synchronized <En, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> reduce(
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		if (java_tys.isEmpty()) {
			return term;
		}
		while (true) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> next = reduce1(term);
			if (next.equals(term)) {
				return next;
			}
			term = next;
		}
	}

	private synchronized <En, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> reduce1(
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		if (term.var != null || term.gen() != null || term.sk() != null || term.obj() != null) {
			return term;
		}

		Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg = null;
		if (term.arg != null) {
			arg = reduce1(term.arg);
		}

		if (term.fk() != null) {
			return Term.Fk(term.fk(), arg);
		} else if (term.att() != null) {
			return Term.Att(term.att(), arg);
		} else if (term.args != null) {
			List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> args = (new ArrayList<>(term.args.size()));
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> x : term.args) {
				args.add(reduce1(x));
			}
			if (!java_fns.containsKey(term.sym())) {
				return Term.Sym(term.sym(), args);
			}
			List<Object> unwrapped_args = (new ArrayList<>(args.size()));
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> t : args) {
				if (t.obj() != null) {
					unwrapped_args.add(t.obj());
				}
			}
			if (unwrapped_args.size() != args.size()) {
				return Term.Sym(term.sym(), args);
			}
			Object result = apply(term.sym(), unwrapped_args);
			Ty ty = syms.get(term.sym()).second;
			return Term.Obj(result, ty);
		}
		throw new RuntimeException("Anomaly: please report");
	}

	@Deprecated
	public static synchronized Object exec(String s, Map<String, Object> m) {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		try {
			Bindings b = engine.createBindings();
			b.putAll(m);
			return engine.eval(s, b);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error executing " + s + ": " + e.getMessage() + postfix, e);
		}
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((java_fns == null) ? 0 : java_fns.hashCode());
		result = prime * result + ((java_parsers == null) ? 0 : java_parsers.hashCode());
		result = prime * result + ((java_tys == null) ? 0 : java_tys.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		AqlJs<?, ?> other = (AqlJs<?, ?>) obj;
		if (java_fns == null) {
			if (other.java_fns != null)
				return false;
		} else if (!java_fns.equals(other.java_fns))
			return false;
		if (java_parsers == null) {
			if (other.java_parsers != null)
				return false;
		} else if (!java_parsers.equals(other.java_parsers))
			return false;
		if (java_tys == null) {
			if (other.java_tys != null)
				return false;
		} else if (!java_tys.equals(other.java_tys))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AqlJs [java_tys=" + java_tys + ", java_parsers=" + java_parsers + ", java_fns=" + java_fns + "]";
	}
	
	

}
