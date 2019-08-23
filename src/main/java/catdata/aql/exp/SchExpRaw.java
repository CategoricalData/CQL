package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.InteriorLabel;
import catdata.LocException;
import catdata.LocStr;
import catdata.Pair;
import catdata.Program;
import catdata.Quad;
import catdata.Raw;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Eq;
import catdata.aql.Kind;
import catdata.aql.RawTerm;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.TypeSide;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class SchExpRaw extends SchExp implements Raw {

	public <R, P, E extends Exception> R accept(P params, SchExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Exp<?>> imports() {
		return (Collection<Exp<?>>) (Object) imports;
	}

	@Override
	public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
		return v.visitSchExpRaw(params, r);
	}

	static Map<String, En> enCache = new THashMap<>();

	static Map<En, Map<String, Fk>> fkCache = new THashMap<>();

	static Map<En, Map<String, Att>> attCache = new THashMap<>();

	public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
		return this;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		Set<Pair<String, Kind>> ret = new THashSet<>();
		for (SchExp y : imports) {
			ret.addAll(y.deps());
		}
		ret.addAll(typeSide.deps());
		return ret;
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	@SuppressWarnings("unused")
	@Override
	public synchronized Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
		TypeSide<Ty, Sym> ts = typeSide.eval(env, isC);
		Collage<Ty, En, Sym, Fk, Att, Void, Void> col = new Collage<>(ts.collage());

		Set<Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> eqs0 = (new THashSet<>());

		for (SchExp k : imports) {
			Schema<Ty, En, Sym, Fk, Att> v = k.eval(env, isC);
			col.addAll(v.collage());
			eqs0.addAll(v.eqs);
		}

		col.ens.addAll(ens.stream().map(x -> En.En(x)).collect(Collectors.toList()));
		col.fks.putAll(conv1(fks));
		col.atts.putAll(conv2(atts));

		for (Quad<String, String, RawTerm, RawTerm> eq : t_eqs) {
			try {
				Map<String, Chc<Ty, En>> ctx = Collections.singletonMap(eq.first,
						eq.second == null ? null : Chc.inRight(En.En(eq.second)));

				Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq0 = RawTerm
						.infer1x(ctx, eq.third, eq.fourth, null, col.convert(), "", ts.js).first3();

				Var eee = Var.Var(eq.first);
				Chc<Ty, En> v = eq0.first.get(eee);
				if (v.left) {
					throw new RuntimeException(eq.first + " has type " + v.l + " which is not an entity");
				}
				En t = v.r;

				eqs0.add(new Triple<>(new Pair<>(eee, t), eq0.second.convert(), eq0.third.convert()));
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				throw new LocException(find("obs equations", eq),
						"In equation " + eq.third + " = " + eq.fourth + ", " + ex.getMessage());
			}
		}
		String vv = "v";
		Var var = Var.Var(vv);
		for (Pair<List<String>, List<String>> eq : p_eqs) {
			try {

				Map<String, Chc<Ty, En>> ctx = Collections.singletonMap(vv, null);

				RawTerm lhs = RawTerm.fold(col.ens, eq.first, vv);
				RawTerm rhs = RawTerm.fold(col.ens, eq.second, vv);

				Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq0 = RawTerm
						.infer1x(ctx, lhs, rhs, null, col.convert(), "", ts.js).first3();

				Chc<Ty, En> v = eq0.first.get(var);
				if (v.left) {
					throw new RuntimeException(
							"the equation's source " + eq.first + " is type " + v.l + " which is not an entity");
				}
				En t = v.r;

				if (eq0.first.size() != 1) {
					throw new RuntimeException("java constants cannot be used ");
				}

				eqs0.add(new Triple<>(new Pair<>(var, t), eq0.second.convert(), eq0.third.convert()));
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				throw new LocException(find("path equations", eq), "In equation " + Util.sep(eq.first, ".") + " = "
						+ Util.sep(eq.second, ".") + ", " + ex.getMessage());
			}
		}
		for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : eqs0) {
			col.eqs.add(new Eq<>(Collections.singletonMap(eq.first.first, Chc.inRight(eq.first.second)), eq.second,
					eq.third));
		}

		// forces type checking before prover construction
		col.validate();
		
		Schema<Ty, En, Sym, Fk, Att> ret = new Schema<>(ts, col, new AqlOptions(options, col, env.defaults));
		return ret;

	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.addAll(AqlOptions.proverOptionNames());
	}

	private Map<Att, Pair<En, Ty>> conv2(Set<Pair<String, Pair<String, String>>> map) {
		Set<Pair<Att, Pair<En, Ty>>> x = map.stream().map(p -> new Pair<>(Att.Att(En.En(p.second.first), p.first),
				new Pair<>(En.En(p.second.first), Ty.Ty(p.second.second)))).collect(Collectors.toSet());
		return Util.toMapSafely(x);
	}

	private Map<Fk, Pair<En, En>> conv1(Set<Pair<String, Pair<String, String>>> map) {
		Set<Pair<Fk, Pair<En, En>>> x = map.stream().map(p -> new Pair<>(Fk.Fk(En.En(p.second.first), p.first),
				new Pair<>(En.En(p.second.first), En.En(p.second.second)))).collect(Collectors.toSet());
		return Util.toMapSafely(x);
	}

	public final TyExp typeSide;

	public final Set<SchExp> imports;

	public final Set<String> ens;

	public final Set<Pair<String, Pair<String, String>>> fks;
	public final Set<Pair<List<String>, List<String>>> p_eqs;

	public final Set<Pair<String, Pair<String, String>>> atts;
	public final Set<Quad<String, String, RawTerm, RawTerm>> t_eqs;

	public final Map<String, String> options;

	private final Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

	@Override
	public String makeString() {
		final StringBuilder sb = new StringBuilder().append("literal : ").append(typeSide).append(" {\n");

		if (!imports.isEmpty()) {
			sb.append("\timports");
			sb.append("\n\t\t").append(Util.sep(imports, " ")).append("\n");
		}

		if (!ens.isEmpty()) {
			sb.append("\tentities");
			sb.append("\n\t\t").append(Util.sep(Util.alphabetical(ens), " ")).append("\n");
		}

		List<String> temp = new LinkedList<>();

		if (!fks.isEmpty()) {
			sb.append("\tforeign_keys");
			temp = new LinkedList<>();
			for (Pair<String, Pair<String, String>> sym : Util.alphabetical(fks)) {
				temp.add(sym.first + " : " + sym.second.first + " -> " + sym.second.second);
			}
			sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
		}

		if (!p_eqs.isEmpty()) {
			sb.append("\tpath_equations");
			temp = new LinkedList<>();
			for (Pair<List<String>, List<String>> sym : Util.alphabetical(p_eqs)) {
				temp.add(Util.sep(sym.first, ".") + " = " + Util.sep(sym.second, "."));
			}
			sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
		}

		if (!atts.isEmpty()) {
			sb.append("\tattributes");
			temp = new LinkedList<>();
			for (Pair<String, Pair<String, String>> sym : Util.alphabetical((atts))) {
				temp.add(sym.first + " : " + sym.second.first + " -> " + sym.second.second);
			}
			sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
		}

		if (!t_eqs.isEmpty()) {
			sb.append("\tobservation_equations");
			temp = new LinkedList<>();
			for (Quad<String, String, RawTerm, RawTerm> sym : Util.alphabetical(t_eqs)) {
				temp.add("forall " + sym.first + ". " + sym.third + " = " + sym.fourth);
			}
			sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
		}

		if (!options.isEmpty()) {
			sb.append("\toptions");
			temp = new LinkedList<>();
			for (Entry<String, String> sym : options.entrySet()) {
				temp.add(sym.getKey() + " = " + sym.getValue());
			}

			sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
		}

		return sb.toString().trim() + "}";
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((atts == null) ? 0 : atts.hashCode());
		result = prime * result + ((ens == null) ? 0 : ens.hashCode());
		result = prime * result + ((fks == null) ? 0 : fks.hashCode());
		result = prime * result + ((imports == null) ? 0 : imports.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((p_eqs == null) ? 0 : p_eqs.hashCode());
		result = prime * result + ((t_eqs == null) ? 0 : t_eqs.hashCode());
		result = prime * result + ((typeSide == null) ? 0 : typeSide.hashCode());
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
		SchExpRaw other = (SchExpRaw) obj;
		if (atts == null) {
			if (other.atts != null)
				return false;
		} else if (!atts.equals(other.atts))
			return false;
		if (ens == null) {
			if (other.ens != null)
				return false;
		} else if (!ens.equals(other.ens))
			return false;
		if (fks == null) {
			if (other.fks != null)
				return false;
		} else if (!fks.equals(other.fks))
			return false;
		if (imports == null) {
			if (other.imports != null)
				return false;
		} else if (!imports.equals(other.imports))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (p_eqs == null) {
			if (other.p_eqs != null)
				return false;
		} else if (!p_eqs.equals(other.p_eqs))
			return false;
		if (t_eqs == null) {
			if (other.t_eqs != null)
				return false;
		} else if (!t_eqs.equals(other.t_eqs))
			return false;
		if (typeSide == null) {
			if (other.typeSide != null)
				return false;
		} else if (!typeSide.equals(other.typeSide))
			return false;
		return true;
	}

	public SchExpRaw(TyExp typeSide, List<SchExp> imports, List<LocStr> ens,
			List<Pair<LocStr, Pair<String, String>>> fks, List<Pair<Integer, Pair<List<String>, List<String>>>> list,
			List<Pair<LocStr, Pair<String, String>>> atts,
			List<Pair<Integer, Quad<String, String, RawTerm, RawTerm>>> list2, List<Pair<String, String>> options) {
		this.typeSide = typeSide;
		this.imports = new THashSet<>(imports);
		this.ens = LocStr.set1(ens);
		this.fks = LocStr.set2(fks);
		this.p_eqs = LocStr.proj2(list);
		this.atts = LocStr.set2(atts);
		this.t_eqs = LocStr.proj2(list2);
		this.options = Util.toMapSafely(options);
		
		if (this.fks.size() != fks.size()) {
			throw new RuntimeException("Error: schema literal contains duplicate foreign keys.");
		}
		if (this.atts.size() != atts.size()) {
			throw new RuntimeException("Error: schema literal contains duplicate attributes.");
		}
	
//		Util.toMapSafely(fks); // check no dups here rather than wait until eval
//		Util.toMapSafely(atts);

		doGuiIndexing(ens, fks, list, atts, list2);

	}

	public void doGuiIndexing(List<LocStr> ens, List<Pair<LocStr, Pair<String, String>>> fks,
			List<Pair<Integer, Pair<List<String>, List<String>>>> list, List<Pair<LocStr, Pair<String, String>>> atts,
			List<Pair<Integer, Quad<String, String, RawTerm, RawTerm>>> list2) {
		// List<InteriorLabel<Object>> i = InteriorLabel.imports("imports", imports);
		// raw.put("imports", i);
		List<InteriorLabel<Object>> t = InteriorLabel.imports("entities", ens);
		raw.put("entities", t);

		List<InteriorLabel<Object>> f = new LinkedList<>();
		for (Pair<LocStr, Pair<String, String>> p : fks) {
			f.add(new InteriorLabel<>("foreign keys", new Triple<>(p.first.str, p.second.first, p.second.second),
					p.first.loc, x -> x.first + " : " + x.second + " -> " + x.third).conv());
		}
		raw.put("foreign keys", f);

		List<InteriorLabel<Object>> e = new LinkedList<>();
		for (Pair<Integer, Pair<List<String>, List<String>>> p : list) {
			e.add(new InteriorLabel<>("path equations", p.second, p.first,
					x -> Util.sep(x.first, ".") + " = " + Util.sep(x.second, ".")).conv());
		}
		raw.put("path equations", e);

		List<InteriorLabel<Object>> jt = new LinkedList<>();
		raw.put("attributes", jt);
		for (Pair<LocStr, Pair<String, String>> p : atts) {
			jt.add(new InteriorLabel<>("attributes", new Pair<>(p.first.str, p.second), p.first.loc,
					x -> x.first + " : " + x.second.first + " -> " + x.second.second).conv());
		}

		List<InteriorLabel<Object>> jc = new LinkedList<>();
		for (Pair<Integer, Quad<String, String, RawTerm, RawTerm>> p : list2) {
			jc.add(new InteriorLabel<>("obs equations", p.second, p.first, x -> x.third + " = " + x.fourth).conv());
		}
		raw.put("obs equations", jc);
	}

	// for easik
	public SchExpRaw(TyExp typeSide, List<SchExp> imports, List<String> ens,
			List<Pair<String, Pair<String, String>>> fks, List<Pair<List<String>, List<String>>> list,
			List<Pair<String, Pair<String, Ty>>> atts, List<Quad<String, String, RawTerm, RawTerm>> list2,
			List<Pair<String, String>> options, @SuppressWarnings("unused") Object o) {
		this.typeSide = typeSide;
		
		this.imports = new THashSet<>(imports);
		this.ens = (new THashSet<>(ens));
		this.fks = (new THashSet<>(fks));
		if (this.fks.size() != fks.size()) {
			throw new RuntimeException("Error: schema literal contains duplicate foreign keys.");
		}
		this.atts = (atts.stream().map(x -> new Pair<>(x.first, new Pair<>(x.second.first, x.second.second.str)))
				.collect(Collectors.toSet()));
		if (this.atts.size() != atts.size()) {
			throw new RuntimeException("Error: schema literal contains duplicate attributes.");
		}
		this.p_eqs = (new THashSet<>(list));
		
		this.t_eqs = (new THashSet<>(list2));
		this.options = Util.toMapSafely(options);
		// Util.toMapSafely(fks); // check no dups here rather than wait until eval
		// Util.toMapSafely(atts);
	}

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return raw;
	}

	@Override
	public TyExp type(AqlTyping G) {
		typeSide.type(G);
		for (Exp<?> z : imports()) {
			if (z.kind() != Kind.SCHEMA) {
				throw new RuntimeException("Import of wrong kind: " + z);
			}
			TyExp u = ((SchExp)z).type(G);
			if (!typeSide.equals(u)) {
				throw new RuntimeException("Import schema typeside mismatch on " + z + ", is " + u + " and not " + typeSide + " as expected.");
			}
		}
		return typeSide;	
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		typeSide.map(f);
	}

}
