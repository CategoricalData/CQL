package catdata.aql.exp;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.InteriorLabel;
import catdata.LocException;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlJs;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.Eq;
import catdata.aql.Kind;
import catdata.aql.Term;
import catdata.aql.TypeSide;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class TyExpRaw extends TyExp implements Raw {

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Exp<?>> imports() {
		return (Collection<Exp<?>>) (Object) imports;
	}

	@Override
	public <R, P, E extends Exception> TyExp coaccept(P params, TyExpCoVisitor<R, P, E> v, R r) throws E {
		return v.visitTyExpRaw(params, r);
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, TyExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

	public Map<String, List<InteriorLabel<Object>>> raw() {
		return raw;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		Set<Pair<String, Kind>> ret = new THashSet<>();
		for (TyExp x : imports) {
			ret.addAll(x.deps());
		}
		return ret;
	}

	public final Set<TyExp> imports;
	public final Set<String> types;
	public final Set<Pair<String, Pair<List<String>, String>>> functions;
	public final Set<Triple<List<Pair<String, String>>, RawTerm, RawTerm>> eqs;

	public final Set<Pair<String, String>> java_tys;
	public final Set<Pair<String, String>> java_parser;
	public final Set<Pair<String, Triple<List<String>, String, String>>> java_fns;

	public final Map<String, String> options;

	private final Collage<Ty, Void, Sym, Void, Void, Void, Void> col;

	@Override
	public Map<String, String> options() {
		return options;
	}

	public TyExpRaw(List<TyExp> imports, List<LocStr> types, List<Pair<LocStr, Pair<List<String>, String>>> functions,
			List<Pair<Integer, Triple<List<Pair<String, String>>, RawTerm, RawTerm>>> eqsX,
			List<Pair<LocStr, String>> java_tys_string, List<Pair<LocStr, String>> java_parser_string,
			List<Pair<LocStr, Triple<List<String>, String, String>>> java_fns_string,
			List<Pair<String, String>> options) {
		this.imports = Util.toSetSafely(imports);
		this.types = LocStr.set1(types);
		this.functions = LocStr.functions1(functions);
		this.eqs = RawTerm.eqs1(eqsX);
		this.java_tys = LocStr.set2(java_tys_string);
		this.java_parser = LocStr.set2(java_parser_string);
		this.java_fns = LocStr.functions2(java_fns_string);
		this.options = Util.toMapSafely(options);

		this.col = new CCollage<>();
		col.tys().addAll(this.types.stream().map(x -> Ty.Ty(x)).collect(Collectors.toList()));
		col.syms().putAll(conv1(Util.toMapSafely(this.functions)));
		col.java_tys().putAll(conv3(Util.toMapSafely(this.java_tys)));
		col.tys().addAll(col.java_tys().keySet());
		col.java_parsers().putAll(conv3(Util.toMapSafely(this.java_parser)));
		for (Entry<String, Triple<List<String>, String, String>> kv : Util.toMapSafely(this.java_fns).entrySet()) {
			List<Ty> l1 = kv.getValue().first.stream().map(x -> Ty.Ty(x)).collect(Collectors.toList());
			Sym s = Sym.Sym(kv.getKey());
			col.syms().put(s, new Pair<>(l1, Ty.Ty(kv.getValue().second)));
			col.java_fns().put(s, kv.getValue().third);
		}

		// do above because find() requires the index
		doGuiIndex(types, functions, eqsX, java_tys_string, java_parser_string, java_fns_string);
	}

	@SuppressWarnings("unchecked")
	static Triple<Map<Var, Chc<Ty, Void>>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>> infer1x(
			Map<String, Chc<Ty, En>> Map0, RawTerm e0, RawTerm f, Chc<Ty, Void> expected,
			@SuppressWarnings("rawtypes") Collage col, String pre, AqlJs<Ty, Sym> js) {
		return RawTerm.infer1x(Map0, e0, f, (Chc<Ty, En>) ((Object) expected), col, pre, js).first3();
	}

	private Map<Ty, String> conv3(Map<String, String> m) {
		return Util.map(m, (x, y) -> new Pair<>(Ty.Ty(x), y));
	}

	private Pair<List<Ty>, Ty> conv2(List<String> k, String v) {
		List<Ty> l1 = k.stream().map(x -> Ty.Ty(x)).collect(Collectors.toList());
		return new Pair<>(l1, Ty.Ty(v));
	}

	private Map<Sym, Pair<List<Ty>, Ty>> conv1(Map<String, Pair<List<String>, String>> m) {
		return Util.map(m, (k, v) -> new Pair<>(Sym.Sym(k), conv2(v.first, v.second)));
	}

	public void doGuiIndex(List<LocStr> types,
			List<Pair<LocStr, Pair<List<String>, String>>> functions,
			List<Pair<Integer, Triple<List<Pair<String, String>>, RawTerm, RawTerm>>> eqs,
			List<Pair<LocStr, String>> java_tys_string, List<Pair<LocStr, String>> java_parser_string,
			List<Pair<LocStr, Triple<List<String>, String, String>>> java_fns_string) {
		List<InteriorLabel<Object>> t = InteriorLabel.imports("types", types);
		raw.put("types", t);

		List<InteriorLabel<Object>> f = new LinkedList<>();
		for (Pair<LocStr, Pair<List<String>, String>> p : functions) {
			f.add(new InteriorLabel<>("functions", new Triple<>(p.first.str, p.second.first, p.second.second),
					p.first.loc,
					x -> x.first + " : " + Util.sep(x.second, ",") + (x.second.isEmpty() ? "" : " -> ") + x.third)
							.conv());
		}
		raw.put("functions", f);

		List<InteriorLabel<Object>> e = new LinkedList<>();
		for (Pair<Integer, Triple<List<Pair<String, String>>, RawTerm, RawTerm>> p : eqs) {
			e.add(new InteriorLabel<>("equations", p.second, p.first, x -> x.second + " = " + x.third).conv());
		}
		raw.put("equations", e);

		List<InteriorLabel<Object>> jt = new LinkedList<>();
		raw.put("java_types", jt);
		for (Pair<LocStr, String> p : java_tys_string) {
			jt.add(new InteriorLabel<>("java_types", new Pair<>(p.first.str, p.second), p.first.loc,
					x -> x.first + " = " + x.second).conv());
		}

		List<InteriorLabel<Object>> jc = new LinkedList<>();
		for (Pair<LocStr, String> p : java_parser_string) {
			jc.add(new InteriorLabel<>("java_constants", new Pair<>(p.first.str, p.second), p.first.loc,
					x -> x.first + " = " + x.second).conv());
		}
		raw.put("java_constants", jc);

		List<InteriorLabel<Object>> jf = new LinkedList<>();
		raw.put("java_functions", jf);
		for (Pair<LocStr, Triple<List<String>, String, String>> p : java_fns_string) {
			jf.add(new InteriorLabel<>("java_functions", new Triple<>(p.first.str, p.second.first, p.second.second),
					p.first.loc, x -> x.first + " : " + Util.sep(x.second, ",") + " -> " + x.third).conv());
		}
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
		result = prime * result + ((functions == null) ? 0 : functions.hashCode());
		result = prime * result + ((imports == null) ? 0 : imports.hashCode());
		result = prime * result + ((java_fns == null) ? 0 : java_fns.hashCode());
		result = prime * result + ((java_parser == null) ? 0 : java_parser.hashCode());
		result = prime * result + ((java_tys == null) ? 0 : java_tys.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((types == null) ? 0 : types.hashCode());
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
		TyExpRaw other = (TyExpRaw) obj;
		if (eqs == null) {
			if (other.eqs != null)
				return false;
		} else if (!eqs.equals(other.eqs))
			return false;
		if (functions == null) {
			if (other.functions != null)
				return false;
		} else if (!functions.equals(other.functions))
			return false;
		if (imports == null) {
			if (other.imports != null)
				return false;
		} else if (!imports.equals(other.imports))
			return false;
		if (java_fns == null) {
			if (other.java_fns != null)
				return false;
		} else if (!java_fns.equals(other.java_fns))
			return false;
		if (java_parser == null) {
			if (other.java_parser != null)
				return false;
		} else if (!java_parser.equals(other.java_parser))
			return false;
		if (java_tys == null) {
			if (other.java_tys != null)
				return false;
		} else if (!java_tys.equals(other.java_tys))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}

	@Override
	public synchronized String makeString() {
		final StringBuilder sb = new StringBuilder();

		if (!imports.isEmpty()) {
			sb.append("\n\timports");
			sb.append("\n\t\t" + Util.sep(imports, " ") + "\n");
		}

		if (!types.isEmpty()) {
			sb.append("\n\ttypes");
			sb.append("\n\t\t" + Util.sep(Util.alphabetical(types), " ") + "\n");
		}

		List<String> temp = new LinkedList<>();

		Map<Object, Object> m = new THashMap<>();
		temp = new LinkedList<>();
		for (Pair<String, Pair<List<String>, String>> sym : Util.alphabetical(functions)) {
			if (sym.second.first.isEmpty()) {
				m.put(sym.first, sym.second.second);
			}
		}
		Map<Object, Set<Object>> n = Util.revS(m);

		if (!n.isEmpty()) {
			sb.append("\tconstants");

			for (Object x : Util.alphabetical(n.keySet())) {
				temp.add(Util.sep(n.get(x), " ") + " : " + x);
			}
			sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") + "\n");
		}

		if (functions.size() != n.size()) {
			sb.append("\tfunctions");
			temp = new LinkedList<>();
			for (Pair<String, Pair<List<String>, String>> sym : Util.alphabetical(functions)) {
				if (!sym.second.first.isEmpty()) {
					temp.add(sym.first + " : " + Util.sep(sym.second.first, ", ") + " -> " + sym.second.second);
				}
			}
			sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") + "\n");
		}

		if (!java_tys.isEmpty()) {
			sb.append("\tjava_types");
			temp = new LinkedList<>();
			for (Pair<String, String> sym : Util.alphabetical(java_tys)) {
				temp.add(sym.first + " = " + Util.quote(sym.second));
			}
			sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") + "\n");
		}

		if (!java_parser.isEmpty()) {
			sb.append("\tjava_constants");
			temp = new LinkedList<>();
			for (Pair<String, String> sym : Util.alphabetical(java_parser)) {
				temp.add(sym.first + " = " + Util.quote(sym.second));
			}
			sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") + "\n");
		}

		Function<List<String>, String> fff = x -> x.isEmpty() ? "" : (Util.sep(x, ", ") + " -> ");
		if (!java_fns.isEmpty()) {
			sb.append("\tjava_functions");
			temp = new LinkedList<>();
			for (Pair<String, Triple<List<String>, String, String>> sym : Util.alphabetical(java_fns)) {
				temp.add(sym.first + " : " + fff.apply(sym.second.first) + sym.second.second + " = "
						+ Util.quote(sym.second.third));
			}

			sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") + "\n");
		}

		if (!eqs.isEmpty()) {
			sb.append("\tequations");
			temp = new LinkedList<>();
			for (Triple<List<Pair<String, String>>, RawTerm, RawTerm> sym : Util.alphabetical(eqs)) {
				List<String> vars = sym.first.stream().map(x -> (x.second == null) ? x.first : x.first + ":" + x.second)
						.collect(Collectors.toList());
				temp.add("forall " + Util.sep(vars, ", ") + ". " + sym.second + " = " + sym.third);
			}
			sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") + "\n");
		}

		if (!options.isEmpty()) {
			sb.append("\toptions");
			temp = new LinkedList<>();
			for (Entry<String, String> sym : Util.alphabetical(options.entrySet())) {
				temp.add(sym.getKey() + " = " + sym.getValue());
			}

			sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") + "\n");
		}

		return "literal {\n" + sb.toString() + "}";
	}

	@Override
	public synchronized TypeSide<Ty, Sym> eval0(AqlEnv env, boolean isC) {
		AqlOptions ops = new AqlOptions(options, env.defaults);

		for (TyExp k : imports) {
			col.addAll(k.eval(env, isC).collage());
		}
		AqlJs<Ty, Sym> js = new AqlJs<>(ops, col.syms(), col.java_tys(), col.java_parsers(), col.java_fns());

		for (Triple<List<Pair<String, String>>, RawTerm, RawTerm> eq : eqs) {
			try {
				Triple<Map<Var, Chc<Ty, Void>>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>> tr = infer1x(
						yyy(eq.first), eq.second, eq.third, null, col, "", js);
				col.eqs().add(new Eq<>(tr.first, tr.second, tr.third));
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new LocException(find("equations", eq),
						"In equation " + eq.second + " = " + eq.third + ", " + ex.getMessage());
			}
		}

		Set<Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>>> eqs0 = col
				.eqsAsTriples().stream().map(x -> new Triple<>(xxx(x.first), x.second, x.third))
				.collect(Collectors.toSet());

		TypeSide<Ty, Sym> ret = new TypeSide<>(col.tys(), col.syms(), (eqs0), col.java_tys(), col.java_parsers(), col.java_fns(),
				ops);

		return ret;
	}

	private static Map<Var, Ty> xxx(Map<Var, Chc<Ty, Void>> x) {
		return Util.map(x, (k, v) -> new Pair<>(k, v.l));
	}

	static Map<String, Chc<Ty, En>> yyy(List<Pair<String, String>> l) {
		Map<String, Chc<Ty, En>> ret = (new THashMap<>(l.size()));
		for (Pair<String, String> p : l) {
			if (ret.containsKey(p.first)) {
				throw new RuntimeException("Duplicate bound variable: " + p.first);
			}
			Chc<Ty, En> x = p.second == null ? null : Chc.inLeft(Ty.Ty(p.second));
			ret.put(p.first, x);
		}
		return ret;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.addAll(AqlOptions.proverOptionNames());
    set.add(AqlOption.graal_language);
	}

	@Override
	public Object type(AqlTyping G) {
		return Unit.unit;
	} 

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {

	}

}
