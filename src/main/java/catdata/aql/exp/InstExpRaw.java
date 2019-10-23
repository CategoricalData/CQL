package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.InteriorLabel;
import catdata.LocException;
import catdata.LocStr;
import catdata.Null;
import catdata.Pair;
import catdata.Raw;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.It.ID;
import catdata.aql.Kind;
import catdata.aql.NoAlgInstance;
import catdata.aql.RawTerm;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import catdata.aql.fdm.ImportAlgebra;
import catdata.aql.fdm.InitialAlgebra;
import catdata.aql.fdm.LiteralInstance;
import catdata.aql.fdm.SaturatedInstance;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class InstExpRaw extends InstExp<Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>> implements Raw {

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		schema.map(f);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Exp<?>> imports() {
		return (Collection<Exp<?>>) (Object) imports;
	}

	static Map<String, Gen> genCache = new THashMap<>();
	static Map<String, Sk> skCache = new THashMap<>();
	private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

	public Map<String, List<InteriorLabel<Object>>> raw() {
		return raw;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		Set<Pair<String, Kind>> ret = new THashSet<>(schema.deps());
		for (InstExp<?, ?, ?, ?> x : imports) {
			ret.addAll(x.deps());
		}
		return ret;
	}

	public final SchExp schema;

	public final Set<InstExp<?, ?, ?, ?>> imports;

	public final Set<Pair<String, String>> gens;

	public final Set<Pair<RawTerm, RawTerm>> eqs;

	public final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	// typesafe by covariance of read-only collections
	public InstExpRaw(SchExp schema, List<InstExp<?, ?, ?, ?>> imports, List<Pair<LocStr, String>> gens,
			List<Pair<Integer, Pair<RawTerm, RawTerm>>> eqs, List<Pair<String, String>> options) {
		this.schema = schema;
		this.imports = new THashSet<>(imports);
		this.gens = LocStr.set2(gens);
		this.eqs = LocStr.proj2(eqs);
		this.options = Util.toMapSafely(options);

		// List<InteriorLabel<Object>> i = InteriorLabel.imports("imports", imports);
		// raw.put("imports", i);

		List<InteriorLabel<Object>> e = new LinkedList<>();
		for (Pair<LocStr, String> p : gens) {
			e.add(new InteriorLabel<>("generators", new Pair<>(p.first.str, p.second), p.first.loc,
					x -> x.first + " : " + x.second).conv());
		}
		raw.put("generators", e);

		List<InteriorLabel<Object>> xx = new LinkedList<>();
		for (Pair<Integer, Pair<RawTerm, RawTerm>> p : eqs) {
			xx.add(new InteriorLabel<>("equations", p.second, p.first, x -> x.first + " = " + x.second).conv());
		}
		raw.put("equations", xx);
	}

	public synchronized String makeString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("literal : " + schema + " {");

		if (!imports.isEmpty()) {
			sb.append("\n\timports");
			sb.append("\n\t\t" + Util.sep(imports, " ") + "\n");
		}

		List<String> temp = new LinkedList<>();

		if (!gens.isEmpty()) {
			sb.append("\n\tgenerators");

			Map<String, Set<String>> n = Util.revS(Util.toMapSafely(gens));

			temp = new LinkedList<>();
			for (Object x : Util.alphabetical(n.keySet())) {
				temp.add(Util.sep(Util.alphabetical(n.get(x)), " ") + " : " + x);
			}

			sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") );
		}

		if (!eqs.isEmpty()) {
			sb.append("\n\tequations");
			temp = new LinkedList<>();
			for (Pair<RawTerm, RawTerm> sym : Util.alphabetical(eqs)) {
				temp.add(sym.first + " = " + sym.second);
			}
			if (eqs.size() < 9) {
				sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") );
			} else {
				int step = 3;
				int longest = 32;
				for (String s : temp) {
					if (s.length() > longest) {
						longest = s.length() + 4;
					}
				}
				for (int i = 0; i < temp.size(); i += step) {
					Formatter formatter = new Formatter(new StringBuilder(), Locale.US);
					List<String> args = new LinkedList<>();
					List<String> format = new LinkedList<>();
					for (int j = i; j < Integer.min(temp.size(), i + step); j++) {
						args.add(temp.get(j));
						format.add("%-" + longest + "s");
					}
					final String formatStr = Util.sep(format, "");
					final Object[] formatTgt = args.toArray(new String[0]);
					final String x = formatter.format(formatStr, formatTgt).toString();
					formatter.close();
					sb.append("\n\t\t" + x);
				}
				sb.append("\n");
			}
		}

		if (!options.isEmpty()) {
			sb.append("\n\toptions");
			temp = new LinkedList<>();
			for (Entry<String, String> sym : options.entrySet()) {
				temp.add(sym.getKey() + " = " + Util.maybeQuote(sym.getValue()));
			}

			sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") );
		}

		return sb.toString().trim() + "}";
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
		result = prime * result + ((gens == null) ? 0 : gens.hashCode());
		result = prime * result + ((imports == null) ? 0 : imports.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
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
		InstExpRaw other = (InstExpRaw) obj;
		if (eqs == null) {
			if (other.eqs != null)
				return false;
		} else if (!eqs.equals(other.eqs))
			return false;
		if (gens == null) {
			if (other.gens != null)
				return false;
		} else if (!gens.equals(other.gens))
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
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>> eval0(AqlEnv env,
			boolean isC) {
		Schema<Ty, En, Sym, Fk, Att> sch = schema.eval(env, isC);
		Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col = new Collage<>(sch.collage());

		Set<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqs0 = (new THashSet<>());

		for (InstExp<?, ?, ?, ?> k : imports) {
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, ID, Chc<Sk, Pair<ID, Att>>> v = (Instance<Ty, En, Sym, Fk, Att, Gen, Sk, ID, Chc<Sk, Pair<ID, Att>>>) k
					.eval(env, isC);
			col.addAll(v.collage());
			v.eqs((a,b)-> {
				eqs0.add(new Pair<>(a,b));
			});
		}

		for (Pair<String, String> p : gens) {
			String gen = p.first;
			String ty = p.second;
			if (col.ens.contains(En.En(ty))) {
				col.gens.put(Gen.Gen(gen), En.En(ty));
			} else if (col.tys.contains(Ty.Ty(ty))) {
				col.sks.put(Sk.Sk(gen), Ty.Ty(ty));
			} else {
				throw new LocException(find("generators", p),
						"The sort for " + gen + ", namely " + ty + ", is not declared as a type or entity");
			}
		}

		for (Pair<RawTerm, RawTerm> eq : eqs) {
			try {
				Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq0 = RawTerm
						.infer1x(Collections.emptyMap(), eq.first, eq.second, null, col, "", sch.typeSide.js).first3();

				eqs0.add(new Pair<>(eq0.second, eq0.third));
				col.eqs.add(new Eq<>(null, eq0.second, eq0.third));

			} catch (RuntimeException ex) {
				ex.printStackTrace();
				throw new LocException(find("equations", eq),
						"In equation " + eq.first + " = " + eq.second + ", " + ex.getMessage());
			}
		}

		AqlOptions strat = new AqlOptions(options, env.defaults);

		boolean interpret_as_algebra = (boolean) strat.getOrDefault(AqlOption.interpret_as_algebra);
		boolean dont_check_closure = (boolean) strat.getOrDefault(AqlOption.import_dont_check_closure_unsafe);
		boolean interpret_as_frozen = false;

		if (interpret_as_algebra) {
			return eval0_algebra(sch, col, eqs0, strat, dont_check_closure);
		}

		if (interpret_as_frozen) {
			return (Instance<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>>) ((Object) new NoAlgInstance(
					col, sch, strat));
		}

		col.validate();

		InitialAlgebra<Ty, En, Sym, Fk, Att, Gen, Sk> initial = new InitialAlgebra<>(strat, sch, col, (y) -> y,
				(x, y) -> y);

		return new LiteralInstance<>(sch, col.gens, col.sks, eqs0, initial.dp(), initial,
				(Boolean) strat.getOrDefault(AqlOption.require_consistency),
				(Boolean) strat.getOrDefault(AqlOption.allow_java_eqs_unsafe));
	}

	private Instance<Ty, En, Sym, Fk, Att, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>> eval0_algebra(
			Schema<Ty, En, Sym, Fk, Att> sch, Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col,
			Set<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> eqs0, AqlOptions strat,
			boolean dont_check_closure) {
		@SuppressWarnings({ "unchecked" })
		Map<En, Collection<Gen>> ens0 = (Map<En, Collection<Gen>>) (Object) Util.newSetsFor(col.ens);

		if (!col.sks.isEmpty()) {
			throw new RuntimeException("Cannot have generating labelled nulls with import_as_theory");
		}
		Map<Ty, Collection<Null<?>>> tys0 = Util.mk();
		for (Ty ty : sch.typeSide.tys) {
			tys0.put(ty, (new THashSet<>()));
		}
		Map<Gen, Map<Fk, Gen>> fks0 = new THashMap<>();
		Map<Gen, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Null<?>>>> atts0 = new THashMap<>();
		for (Gen gen : col.gens.keySet()) {
			fks0.put(gen, new THashMap<>());
			atts0.put(gen, new THashMap<>());
			ens0.get(col.gens.get(gen)).add(gen);
		}

		for (Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> e : eqs0) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> lhs = e.first;
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> rhs = e.second;
			if (rhs.gen() != null && lhs.fk() != null && lhs.arg.gen() != null) {
				fks0.get(lhs.arg.gen()).put(lhs.fk(), rhs.gen());
			} else if (lhs.gen() != null && rhs.fk() != null && rhs.arg.gen() != null) {
				fks0.get(rhs.arg.gen()).put(rhs.fk(), lhs.gen());
			} else if (rhs.obj() != null && lhs.att() != null && lhs.arg.gen() != null) {
				atts0.get(lhs.arg.gen()).put(lhs.att(), Term.Obj(rhs.obj(), rhs.ty()));
			} else if (lhs.obj() != null && rhs.att() != null && rhs.arg.gen() != null) {
				atts0.get(rhs.arg.gen()).put(rhs.att(), Term.Obj(lhs.obj(), lhs.ty()));
			} else if (rhs.sym() != null && rhs.args.isEmpty() && lhs.att() != null && lhs.arg.gen() != null) {
				atts0.get(lhs.arg.gen()).put(lhs.att(), Term.Sym(rhs.sym(), Collections.emptyList()));
			} else if (lhs.sym() != null && lhs.args.isEmpty() && rhs.att() != null && rhs.arg.gen() != null) {
				atts0.get(rhs.arg.gen()).put(rhs.att(), Term.Sym(lhs.sym(), Collections.emptyList()));
			} else {
				throw new RuntimeException("interpret_as_algebra not compatible with equation " + lhs + " = " + rhs
						+ "; each equation must be of the form gen.fk=gen or gen.att=javaobject");
			}
		}

		// Map<Null<?>, Term<Ty, En, Sym, Fk, Att, Gen, Null<?>>> extraRepr = null;
		// //Collections.synchronizedMap(new THashMap<>());
		for (Gen gen : col.gens.keySet()) {
			for (Att att : sch.attsFrom(col.gens.get(gen))) {
				if (!atts0.get(gen).containsKey(att)) {
					atts0.get(gen).put(att, InstExpImport.objectToSk(sch, null, gen, att, tys0, null, false, false));
				}
			}
		}

		ImportAlgebra<Ty, En, Sym, Fk, Att, Gen, Null<?>> alg = new ImportAlgebra<>(sch, ens0, tys0, fks0, atts0,
				(x, y) -> y, (x, y) -> y, dont_check_closure, Collections.emptySet());

		return new SaturatedInstance(alg, alg, (Boolean) strat.getOrDefault(AqlOption.require_consistency),
				(Boolean) strat.getOrDefault(AqlOption.allow_java_eqs_unsafe), true, null);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.interpret_as_algebra);
		set.add(AqlOption.import_dont_check_closure_unsafe);
		set.add(AqlOption.diverge_limit);
		set.add(AqlOption.diverge_warn);
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.addAll(AqlOptions.proverOptionNames());
	}

	@Override
	public SchExp type(AqlTyping G) {
		schema.type(G);
		for (Exp<?> z : imports()) {
			if (z.kind() != Kind.INSTANCE) {
				throw new RuntimeException("Import of wrong kind: " + z);
			}
			//SchExp u = ((InstExp)z).type(G);
			//if (!schema.equals(u)) {
			//	throw new RuntimeException("Import instance schema mismatch on " + z + ", is " + u + " and not " + schema + " as expected.");
			//}
		}
		return schema;
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.emptySet();
	}

}
