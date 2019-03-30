package catdata.aql.exp;

import java.util.ArrayList;
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
import catdata.Pair;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Eq;
import catdata.aql.Kind;
import catdata.aql.Query;
import catdata.aql.RawTerm;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class QueryExpRaw extends QueryExp implements Raw {

	@Override
	public Collection<Exp<?>> imports() {
		return (Collection<Exp<?>>) (Object) imports;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.dont_validate_unsafe);
		set.add(AqlOption.query_remove_redundancy);
		set.addAll(AqlOptions.proverOptionNames());
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	private final SchExp src;
	private final SchExp dst;

	private final Set<QueryExp> imports;

	private final Map<String, String> options;

	private final Map<String, Block> blocks;

	public final Map<String, String> params;
	public final Map<String, RawTerm> consts;

	private final Map<En, Integer> b1 = new THashMap<>();
	private final Map<Fk, Integer> b2 = new THashMap<>();
	private final Map<Att, Integer> b3 = new THashMap<>();

	@Override
	public Map<String, String> options() {
		return options;
	}

	public static class Trans extends Exp<Void> implements Raw {

		@Override
		public Object type(AqlTyping G) {
			return Unit.unit;
		}

		private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

		@Override
		public Map<String, List<InteriorLabel<Object>>> raw() {
			return raw;
		}

		@Override
		protected Map<String, String> options() {
			return null;
		}

		@Override
		public Kind kind() {
			return null;
		}

		@Override
		public Void eval0(AqlEnv env, boolean isC) {
			return null;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return null;
		}

		public final Set<Pair<Var, RawTerm>> gens;

		public final Map<String, String> options;

		public Trans(List<Pair<LocStr, RawTerm>> gens, List<Pair<String, String>> options) {
			this.gens = new THashSet<>();
			for (Pair<LocStr, RawTerm> gen : gens) {
				this.gens.add(new Pair<>(Var.Var(gen.first.str), gen.second));
			}
			this.options = Util.toMapSafely(options);

			List<InteriorLabel<Object>> f = new LinkedList<>();
			for (Pair<LocStr, RawTerm> p : gens) {
				f.add(new InteriorLabel<>("generators", new Pair<>(p.first.str, p.second), p.first.loc,
						x -> x.first + " -> " + x.second).conv());
			}
			raw.put("generators", f);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((gens == null) ? 0 : gens.hashCode());
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
			Trans other = (Trans) obj;
			if (gens == null) {
				if (other.gens != null)
					return false;
			} else if (!gens.equals(other.gens))
				return false;
			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			return true;
		}

		@Override
		public String makeString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("{");

			sb.append("").append(
					this.gens.stream().map(en -> en.first + " -> " + en.second).collect(Collectors.joining("\n\t")));

			sb.append(" options ").append(this.options.entrySet().stream()
					.map(opt -> opt.getKey() + " = " + opt.getValue()).collect(Collectors.joining("\n\t")));

			sb.append("}");
			return sb.toString();
		}

		@Override
		public Exp<Void> Var(String v) {
			return null;
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

	}

	public static class PreBlock {
		public final List<Pair<LocStr, String>> gens;
		public final List<Pair<Integer, Pair<RawTerm, RawTerm>>> eqs;
		public final List<Pair<String, String>> options;
		public final List<Pair<LocStr, RawTerm>> atts;
		public final List<Pair<LocStr, Trans>> fks;
		public final boolean star;

		public PreBlock(List<Pair<LocStr, String>> gens, List<Pair<Integer, Pair<RawTerm, RawTerm>>> eqs,
				List<Pair<LocStr, RawTerm>> atts, List<Pair<LocStr, Trans>> fks, List<Pair<String, String>> options,
				boolean star) {
			this.gens = gens;
			this.eqs = eqs;
			this.atts = atts;
			this.fks = fks;
			this.options = options;
			this.star = star;
		}

	}

	public static class Block extends Exp<Void> implements Raw {

		@Override
		public Object type(AqlTyping G) {
			return Unit.unit;
		}

		public Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

		@Override
		public Kind kind() {
			return null;
		}

		@Override
		public Void eval0(AqlEnv env, boolean isC) {
			return null;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((atts == null) ? 0 : atts.hashCode());
			result = prime * result + ((en == null) ? 0 : en.hashCode());
			result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
			result = prime * result + ((fks == null) ? 0 : fks.hashCode());
			result = prime * result + ((gens == null) ? 0 : gens.hashCode());
			result = prime * result + ((options == null) ? 0 : options.hashCode());
			result = prime * result + (star ? 1231 : 1237);
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
			Block other = (Block) obj;
			if (atts == null) {
				if (other.atts != null)
					return false;
			} else if (!atts.equals(other.atts))
				return false;
			if (en == null) {
				if (other.en != null)
					return false;
			} else if (!en.equals(other.en))
				return false;
			if (eqs == null) {
				if (other.eqs != null)
					return false;
			} else if (!eqs.equals(other.eqs))
				return false;
			if (fks == null) {
				if (other.fks != null)
					return false;
			} else if (!fks.equals(other.fks))
				return false;
			if (gens == null) {
				if (other.gens != null)
					return false;
			} else if (!gens.equals(other.gens))
				return false;
			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			if (star != other.star)
				return false;
			return true;
		}

		public final boolean star;
		public final Set<Pair<Att, RawTerm>> atts;
		public final Set<Pair<Fk, Trans>> fks;
		public En en;
		public final Set<Pair<Var, String>> gens;
		// public final Set<Pair<Var, Ty>> sks;
		public final Set<Pair<RawTerm, RawTerm>> eqs;
		public final Map<String, String> options;
		public final Integer enLoc;

		public Block(PreBlock b, LocStr en, boolean star) {
			this.en = En.En(en.str);
			this.enLoc = en.loc;
			this.star = star;
			this.gens = new THashSet<>();
			this.atts = LocStr.set2(b.atts).stream().map(x -> new Pair<>(Att.Att(this.en, x.first), x.second))
					.collect(Collectors.toSet());
			this.fks = LocStr.set2(b.fks).stream().map(x -> new Pair<>(Fk.Fk(this.en, x.first), x.second))
					.collect(Collectors.toSet());

			for (Pair<LocStr, String> gen : b.gens) {
				this.gens.add(new Pair<>(Var.Var(gen.first.str), gen.second));
			}
			this.eqs = LocStr.proj2(b.eqs);
			this.options = Util.toMapSafely(b.options);

			List<InteriorLabel<Object>> e = new ArrayList<>(b.gens.size());
			for (Pair<LocStr, String> p : b.gens) {
				e.add(new InteriorLabel<>("from", new Pair<>(p.first.str, p.second), p.first.loc,
						x -> x.first + " : " + x.second).conv());
			}
			this.raw.put("from", e);

			List<InteriorLabel<Object>> xx = new ArrayList<>(b.eqs.size());
			for (Pair<Integer, Pair<RawTerm, RawTerm>> p : b.eqs) {
				xx.add(new InteriorLabel<>("where", p.second, p.first, x -> x.first + " = " + x.second).conv());
			}
			this.raw.put("where", xx);

			xx = new ArrayList<>(b.atts.size());
			for (Pair<LocStr, RawTerm> p : b.atts) {
				xx.add(new InteriorLabel<>("attributes", new Pair<>(p.first.str, p.second), p.first.loc,
						x -> x.first + " -> " + x.second).conv());
			}
			raw.put("attributes", xx);

			xx = new LinkedList<>();
			for (Pair<LocStr, Trans> p : b.fks) {
				xx.add(new InteriorLabel<>("foreign_keys", new Pair<>(p.first.str, p.second), p.first.loc,
						x -> x.first + " -> " + x.second).conv());
			}
			raw.put("foreign_keys", xx);
		}

		@Override
		public synchronized String makeString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("\nentity ").append(this.en.str).append(" -> { ");

			if (!this.gens.isEmpty()) {
				final Map<String, Set<Var>> x = Util.revS(Util.toMapSafely(this.gens));
				sb.append("\n\tfrom \n\t\t");
				sb.append(Util.alphabetical(x.keySet()).stream().map(en -> Util.sep(x.get(en), " ") + " : " + en)
						.collect(Collectors.joining("\n\t\t")));
			}

			if (!this.eqs.isEmpty()) {
				sb.append("\n\twhere\n\t\t");
				sb.append(Util.alphabetical(this.eqs).stream().map(sym -> sym.first + " = " + sym.second)
						.collect(Collectors.joining("\n\t\t")));
			}

			if (!this.atts.isEmpty()) {
				sb.append("\n\tattributes\n\t\t");
				if (star) {
					sb.append(" * ");
				}
				sb.append(Util.alphabetical(this.atts).stream().map(sym -> sym.first + " -> " + sym.second)
						.collect(Collectors.joining("\n\t\t")));
			}

			if (!fks.isEmpty()) {
				sb.append("\n\tforeign_keys\n\t\t");
				sb.append(Util.alphabetical(this.fks).stream().map(sym -> sym.first + " -> " + sym.second)
						.collect(Collectors.joining("\n\t\t")));
			}

			if (!this.options.isEmpty()) {
				sb.append("\n\toptions \n\t\t");
				sb.append(this.options.entrySet().stream().map(sym -> sym.getKey() + " = " + sym.getValue())
						.collect(Collectors.joining("\n\t\t")));
			}

			sb.append("}");
			return sb.toString();
		}

		private String toString2;

		public synchronized String toString2() {
			if (toString2 != null) {
				return toString2;
			}
			toString2 = "";

			List<String> temp = new LinkedList<>();

			if (!gens.isEmpty()) {
				toString2 += "\t\t\t\tfrom ";

				Map<String, Set<Var>> x = Util.revS(Util.toMapSafely(gens));
				temp = new LinkedList<>();
				for (String En : Util.alphabetical(x.keySet())) {
					temp.add(Util.sep(x.get(En), " ") + " : " + En);
				}

				toString2 += Util.sep(temp, "\n\t\t\t\t\t");
			}

			if (!eqs.isEmpty()) {
				toString2 += "\n\t\t\t\twhere\t";
				temp = new LinkedList<>();
				for (Pair<RawTerm, RawTerm> sym : Util.alphabetical(eqs)) {
					temp.add(sym.first + " = " + sym.second);
				}
				toString2 += Util.sep(temp, "\n\t\t\t\t\t");
			}

			if (!atts.isEmpty()) {
				toString2 += "\n\t\t\t\tattributes\t";
				if (star) {
					toString2 += " * ";
				}
				temp = new LinkedList<>();
				for (Pair<Att, RawTerm> sym : Util.alphabetical(atts)) {
					temp.add(sym.first + " -> " + sym.second);
				}
				toString2 += Util.sep(temp, "\n\t\t\t\t\t");
			}

			if (!fks.isEmpty()) {
				toString2 += "\n\t\t\t\tforeign_keys\t";
				temp = new LinkedList<>();
				for (Pair<catdata.aql.exp.Fk, Trans> sym : Util.alphabetical(fks)) {
					temp.add(sym.first.str + " -> {" + sym.second + "}");
				}
				toString2 += Util.sep(temp, "\n\t\t\t\t\t");
			}

			if (!options.isEmpty()) {
				toString2 += "\n\t\t\t\toptions ";
				temp = new LinkedList<>();
				for (Entry<String, String> sym : options.entrySet()) {
					temp.add(sym.getKey() + " = " + sym.getValue());
				}

				toString2 += "\n\t\t\t\t" + Util.sep(temp, "\n\t\t\t\t\t");
			}

			toString2 = "\t" + toString2;
			return toString2;
		}

		@Override
		public Map<String, List<InteriorLabel<Object>>> raw() {
			return raw;
		}

		@Override
		protected Map<String, String> options() {
			return options;
		}

		@Override
		public Exp<Void> Var(String v) {
			return null;
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((params == null) ? 0 : params.hashCode());
		result = prime * result + ((consts == null) ? 0 : consts.hashCode());
		result = prime * result + ((blocks == null) ? 0 : blocks.hashCode());
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((imports == null) ? 0 : imports.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
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
		QueryExpRaw other = (QueryExpRaw) obj;
		if (blocks == null) {
			if (other.blocks != null)
				return false;
		} else if (!blocks.equals(other.blocks))
			return false;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
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
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		if (params == null) {
			if (other.params != null)
				return false;
		} else if (!params.equals(other.params))
			return false;
		if (consts == null) {
			if (other.consts != null)
				return false;
		} else if (!consts.equals(other.consts))
			return false;
		return true;
	}

	@Override
	public String makeString() {
		final StringBuilder sb = new StringBuilder("literal : ");
		sb.append(this.src).append(" -> ").append(this.dst);
		sb.append(" {");

		if (!this.imports.isEmpty()) {
			sb.append("\nimports ")
					.append(this.imports.stream().map(x -> x.toString()).collect(Collectors.joining("\n\t")));
		}

		sb.append(this.blocks.values().stream().map(x -> x.toString()).collect(Collectors.joining("\n\t")));

		if (!this.options.isEmpty()) {
			sb.append(" options ").append(this.options.entrySet().stream()
					.map(sym -> sym.getKey() + " = " + sym.getValue()).collect(Collectors.joining("\n\t")));
		}

		return sb.toString() + "}";
	}

	public QueryExpRaw(List<Pair<LocStr, String>> params, List<Pair<LocStr, RawTerm>> consts, SchExp c, SchExp d,
			List<QueryExp> imports, List<Pair<LocStr, PreBlock>> list, List<Pair<String, String>> options) {
		this.src = c;
		this.dst = d;
		this.imports = new THashSet<>(imports);
		this.options = Util.toMapSafely(options);
		this.consts = new THashMap<>(Util.toMapSafely(LocStr.set2(consts)));
		this.params = new THashMap<>(Util.toMapSafely(LocStr.set2(params)));
	
		Set<Block> bb = Util.toSetSafely(list).stream().map(x -> new Block(x.second, x.first, x.second.star))
				.collect(Collectors.toSet());
		blocks = new THashMap<>();
		for (Block x : bb) {
			blocks.put(x.en.str, x);
		}

		for (Pair<LocStr, PreBlock> x : list) {
			En z = En.En(x.first.str);
			if (x.second.star) {
				throw new RuntimeException("Cannot use * in non-simple queries");
			}
			b1.put(z, x.first.loc);

			for (Pair<LocStr, Trans> y : x.second.fks) {
				b2.put(Fk.Fk(z, y.first.str), y.first.loc);
			}

			for (Pair<LocStr, RawTerm> y : x.second.atts) {
				b3.put(Att.Att(z, y.first.str), y.first.loc);
			}
			
			List<InteriorLabel<Object>> f = new LinkedList<>();
			
			f.add(new InteriorLabel<>("entities", blocks.get(z.str), x.first.loc, y -> x.first.str).conv());

			raw.put(x.first.str, f);
		}



	}

	private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return (raw);
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		Collection<Pair<String, Kind>> ret = new THashSet<>(src.deps());
		ret.addAll(dst.deps());
		for (QueryExp x : imports) {
			ret.addAll(x.deps());
		}
		return ret;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		TyExp t1 = src.type(G);
		TyExp t2 = dst.type(G);
		if (!t1.equals(t2)) {
			throw new RuntimeException("Non-equal typesides: " + t1 + " and " + t2);
		}
		return new Pair<>(src, dst);
	}

	@Override
	public synchronized Query<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
		Schema<Ty, En, Sym, Fk, Att> src0 = src.eval(env, isC);
		Schema<Ty, En, Sym, Fk, Att> dst0 = dst.eval(env, isC);

		Map<En, Triple<Map<Var, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>>, AqlOptions>> ens0 = new THashMap<>();
		Map<Att, Term<Ty, En, Sym, Fk, Att, Var, Var>> atts0 = new THashMap<>();
		Map<Fk, Pair<Map<Var, Term<Void, En, Void, Fk, Void, Var, Void>>, AqlOptions>> fks0 = new THashMap<>();
		Map<Fk, Map<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>>> sks0 = new THashMap<>();

		Map<Var, Ty> xxx = new THashMap<>();
		Map<Var, Term<Ty, Void, Sym, Void, Void, Void, Void>> yyy = new THashMap<>();

		for (QueryExp k : imports) {
			Query<Ty, En, Sym, Fk, Att, En, Fk, Att> v = k.eval(env, isC);

			for (Var var : v.params.keySet()) {
				xxx.put(var, v.params.get(var)); // allow benign collisions
			}
			for (Var var : v.consts.keySet()) {
				yyy.put(var, v.consts.get(var));
			}
			for (En En : v.ens.keySet()) {
				Set<Pair<Term<Ty, catdata.aql.exp.En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>, Term<Ty, catdata.aql.exp.En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>>> x = v.ens
						.get(En).eqs;
				Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>> z = new ArrayList<>(x.size());
				for (Pair<Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>, Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>> a : x) {
					z.add(new Eq<>(null, a.first, a.second));
				}
				ens0.put(En, new Triple<>(new THashMap<>(Util.inLeft(v.ens.get(En).gens)), z, v.ens.get(En).options));
			}
			for (Att Att : v.atts.keySet()) {
				atts0.put(Att, v.atts.get(Att));
			}
			for (Fk Fk : v.fks.keySet()) {
				fks0.put(Fk, new Pair<>(v.fks.get(Fk).gens(), v.doNotValidate.get(Fk)));
			}

		}

		Map<En, Collage<Ty, En, Sym, Fk, Att, Var, Var>> cols = new THashMap<>();
		for (Block p : blocks.values()) {
			try {
				if (!dst0.ens.contains(p.en)) {
					throw new RuntimeException(
							"the proposed target entity " + p.en + " does not actually appear in the target schema");
				}
				processBlock(options, env, src0, ens0, cols, p, params);
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				throw new LocException(b1.get(p.en), "In block for target entity " + p.en + ", " + ex.getMessage());
			}
			Set<Var> set = new THashSet<>(p.gens.size());
			for (Pair<catdata.aql.Var, String> z : p.gens) {
				if (src0.typeSide.tys.contains(Ty.Ty(z.second))) {
					set.add(z.first);
				}
			}
			for (Pair<catdata.aql.exp.Att, RawTerm> pp : p.atts) {
				try {
					processAtt(src0, dst0, ens0, atts0, cols, pp, params, set);
				} catch (Exception ex) {
					ex.printStackTrace();
					throw new LocException(b3.get(pp.first),
							"In return clause for " + pp.first + ", " + ex.getMessage());
				}
			}
		}

		// two loops bc need stuff in en to do this part
		for (Block p : blocks.values()) {
			Set<Var> set = new THashSet<>();
			for (Pair<catdata.aql.Var, String> z : p.gens) {
				if (src0.typeSide.tys.contains(Ty.Ty(z.second))) {
					set.add(z.first);
				}
			}
			for (Pair<catdata.aql.exp.Fk, Trans> pp : p.fks) {
				try {
					Map<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>> sks = new THashMap<>();
					Map<Var, Term<Void, En, Void, Fk, Void, Var, Void>> trans = new THashMap<>();
					for (Pair<Var, RawTerm> v : pp.second.gens) {
						Map<String, Chc<catdata.aql.exp.En, catdata.aql.exp.Ty>> Map = unVar(
								ens0.get(dst0.fks.get(pp.first).first).first);
						Map<String, Chc<Ty, En>> Map1 = Util.map(Map, (k, x) -> new Pair<>(k, x.reverse()));
						Collage<Ty, En, Sym, Fk, Att, Var, Var> col = cols.get(dst0.fks.get(pp.first).first);
						Chc<catdata.aql.exp.En, catdata.aql.exp.Ty> required = ens0
								.get(dst0.fks.get(pp.first).second).first.get(v.first);
						Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk> term = RawTerm
								.infer1x(Map1, v.second, null, required.reverse(), col.convert(),
										"in foreign key " + pp.first.str + ", ", src0.typeSide.js).second;
						Term<Ty, En, Sym, Fk, Att, Var, Var> l = term.convert();
						Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, catdata.aql.Var, catdata.aql.Var> r = Query
								.freeze(l, params, set);

						if (!r.hasTypeType()) {
							trans.put(v.first, r.convert());
						} else {
							sks.put(v.first, r);
						}
					}
//					boolean doNotCheckEqs = (Boolean) 
//							.getOrDefault(AqlOption.dont_validate_unsafe);
//					System.out.println("++++++++" + doNotCheckEqs);
					fks0.put(pp.first, new Pair<>(trans, new AqlOptions(pp.second.options, null, env.defaults)));
					sks0.put(pp.first, sks);

				} catch (RuntimeException ex) {
					ex.printStackTrace();
					throw new LocException(b2.get(pp.first), ex.getMessage());
				}
			}
		}

		for (String s : params.keySet()) {
			xxx.put(Var.Var(s), Ty.Ty(params.get(s)));
		}
		for (String s : consts.keySet()) {
			Chc<Ty, En> required = Chc.inLeft(xxx.get(Var.Var(s)));
			Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk> term = RawTerm
					.infer1x(Collections.emptyMap(), consts.get(s), null, required, src0.collage().convert(), "",
							src0.typeSide.js).second;

			yyy.put(Var.Var(s), term.convert());
		}
		// System.out.println("---------" + doNotCheckEqs);
		return Query.makeQuery2(xxx, yyy, ens0, atts0, fks0, sks0, src0, dst0,

				new AqlOptions(options, null, env.defaults));
	}

	public static synchronized void processAtt(Schema<Ty, En, Sym, Fk, Att> src0, Schema<Ty, En, Sym, Fk, Att> dst0,
			Map<En, Triple<Map<Var, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>>, AqlOptions>> ens0,
			Map<Att, Term<Ty, En, Sym, Fk, Att, Var, Var>> atts0, Map<En, Collage<Ty, En, Sym, Fk, Att, Var, Var>> cols,
			Pair<Att, RawTerm> p, Map<String, String> params, Set<Var> set) {
		Map<String, Chc<En, Ty>> Map = unVar(ens0.get(dst0.atts.get(p.first).first).first);
		Collage<Ty, En, Sym, Fk, Att, Var, Var> col = cols.get(dst0.atts.get(p.first).first);
		Chc<Ty, En> required = Chc.inLeft(dst0.atts.get(p.first).second);
		for (String q : params.keySet()) {
			Ty tt = Ty.Ty(params.get(q));
			Map.put(q, Chc.inRight(tt));
			col.sks.put(Var.Var(q), tt);
		}
		Map<String, Chc<Ty, En>> ens1 = Util.map(Map, (y, x) -> new Pair<>(y, x.reverse()));
		Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk> term = RawTerm
				.infer1x(ens1, p.second, null, required, col.convert(), "", src0.typeSide.js).second;
		atts0.put(p.first, Query.freeze(term.convert(), params, set));
	}

	public static synchronized void processBlock(Map<String, String> options, AqlEnv env,
			Schema<Ty, En, Sym, Fk, Att> src0,
			Map<En, Triple<Map<catdata.aql.Var, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>>, AqlOptions>> ens,
			Map<En, Collage<Ty, En, Sym, Fk, Att, Var, Var>> cols, Block p, Map<String, String> params) {
		Map<catdata.aql.Var, String> xx = Util.toMapSafely(p.gens);
		Map<Var, Chc<En, Ty>> Map = new THashMap<>();
		Collage<Ty, En, Sym, Fk, Att, Var, Var> col = new Collage<>(src0.collage());
		Set<Var> set = new THashSet<>(p.gens.size());
		for (Pair<catdata.aql.Var, String> z : p.gens) {
			if (src0.typeSide.tys.contains(Ty.Ty(z.second))) {
				set.add(z.first);
			}
		}
		for (Var v : xx.keySet()) {
			En en = En.En(xx.get(v));
			Ty tt = Ty.Ty(en.str);
			if (src0.ens.contains(en)) {
				Map.put(v, Chc.inLeft(en));
				col.gens.put(v, en);
			} else if (src0.typeSide.tys.contains(tt)) {
				Map.put(v, Chc.inRight(tt));
				col.sks.put(v, tt);
			} else {
				throw new RuntimeException("From clause contains " + v + ":" + en + ", but " + en
						+ " is not a source entity.  Available: " + Util.sep(src0.ens, ", ") + ". ");
			}
		}

		for (String q : params.keySet()) {
			Var vv = Var.Var(q);
			Ty tt = Ty.Ty(params.get(q));
			Map.put(vv, Chc.inRight(tt));
			col.sks.put(vv, tt);
		}

		cols.put(p.en, col);
		Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>> eqs = new THashSet<>(p.eqs.size());

		for (Pair<RawTerm, RawTerm> eq : p.eqs) {
			Triple<Map<catdata.aql.Var, Chc<catdata.aql.exp.Ty, catdata.aql.exp.En>>, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk>> x = RawTerm
					.infer1x(Util.map(Map, (v, c) -> new Pair<>(v.var, c.reverse())), eq.first, eq.second, null,
							col.convert(), "In equation " + eq.first + " = " + eq.second + ", ", src0.typeSide.js)
					.first3();
			eqs.add(new Eq<>(null, Query.freeze(x.second.convert(), params, set),
					Query.freeze(x.third.convert(), params, set)));
		}
		Map<String, String> uu = new THashMap<>(options);
		uu.putAll(p.options);
		AqlOptions theops = new AqlOptions(uu, null, env.defaults);
		Triple<Map<Var, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>>, AqlOptions> b = new Triple<>(Map,
				eqs, theops);
		ens.put(p.en, b);
	}

	public static <X> Map<String, X> unVar(Map<Var, X> Map) {
		Map<String, X> ret = new THashMap<>(Map.size());
		for (Var v : Map.keySet()) {
			ret.put(v.var, Map.get(v));
		}
		return ret;
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		this.src.map(f);
		this.dst.map(f);
	}

}
