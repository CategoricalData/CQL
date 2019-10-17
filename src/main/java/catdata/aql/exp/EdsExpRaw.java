package catdata.aql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Constraints;
import catdata.aql.ED;
import catdata.aql.Kind;
import catdata.aql.RawTerm;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class EdsExpRaw extends EdsExp implements Raw {

	@Override
	public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Collection<Exp<?>> imports() {
		return (Collection<Exp<?>>) (Object) imports;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eds == null) ? 0 : eds.hashCode());
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
		EdsExpRaw other = (EdsExpRaw) obj;
		if (eds == null) {
			if (other.eds != null)
				return false;
		} else if (!eds.equals(other.eds))
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

	@Override
	public Collection<Pair<String, Kind>> deps() {
		Set<Pair<String, Kind>> ret = new THashSet<>(schema.deps());
		for (EdsExp x : imports) {
			ret.addAll(x.deps());
		}
		return ret;
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		schema.map(f);
		for (EdExpRaw e : eds) {
			e.map(f);
		}
	}

	public final SchExp schema;

	public final Set<EdsExp> imports;

	public final Set<EdExpRaw> eds;

	public final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	public EdsExpRaw(SchExp schema, List<EdsExp> imports, List<Pair<Integer, EdExpRaw>> eds,
			List<Pair<String, String>> options) {
		this.schema = schema;
		this.imports = new THashSet<>(imports);
		this.eds = LocStr.proj2(eds);
		this.options = Util.toMapSafely(options);

		// raw.put("imports", InteriorLabel.imports("imports", imports));

		List<InteriorLabel<Object>> f = new LinkedList<>();
		for (Pair<Integer, EdExpRaw> p : eds) {
			f.add(new InteriorLabel<>("constraints", p.second, p.first, x -> "...").conv());
		}
		raw.put("constraints", f);
	}

	private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return raw;
	}

	public EdsExpRaw(SchExp schema, List<EdsExp> imports, List<EdExpRaw> eds, @SuppressWarnings("unused") Object o) {
		this.schema = schema;
		this.imports = new THashSet<>(imports);
		this.eds = new THashSet<>(eds);
		this.options = Collections.emptyMap();
	}

	@Override
	public String makeString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("literal : " + schema + " {\n");

		if (!imports.isEmpty()) {
			sb.append("\timports");
			sb.append("\n\t\t" + Util.sep(imports, " ") + "\n");
		}
		List<String> l = eds.stream().map(x -> x.toString()).collect(Collectors.toList());
		sb.append(Util.sep(l, "\n\n"));

		if (!this.options.isEmpty()) {
			sb.append(" options ").append(this.options.entrySet().stream()
					.map(sym -> sym.getKey() + " = " + sym.getValue()).collect(Collectors.joining("\n\t")));
		}

		return sb.toString().trim() + "}";
	}

	@Override
	public synchronized Constraints eval0(AqlEnv env, boolean isC) {
		Schema<Ty, En, Sym, Fk, Att> sch = schema.eval(env, isC);
		List<ED> l = new LinkedList<>();
		for (EdsExp k : imports) {
			Constraints v = k.eval(env, isC);
			l.addAll(v.eds);
		}
		for (EdExpRaw e : eds) {
			l.add(e.eval(sch, new AqlOptions(options, env.defaults)));
		}

		return new Constraints(sch, l, new AqlOptions(options, env.defaults));

	}

	@Override
	public SchExp type(AqlTyping G) {
		return schema;
	}

	////////////////////////

	public static class EdExpRaw extends Exp<Void> implements Raw {

		@Override
		public Optional<Chc<String, Object>> type(AqlTyping G) {
			return Optional.of(Chc.inRight(Unit.unit));
		}

		private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

		@Override
		public Map<String, List<InteriorLabel<Object>>> raw() {
			return raw;
		}

		@Override
		public String makeString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("");

			if (!As.isEmpty()) {
				sb.append("\tforall");
				List<String> temp = new LinkedList<>();
				for (Pair<String, String> p : Util.alphabetical(As)) {
					temp.add(p.first + ":" + p.second);
				}

				sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") + "\n");
			}
			if (!Awh.isEmpty()) {
				sb.append("\twhere");
				List<String> temp = new LinkedList<>();
				for (Pair<RawTerm, RawTerm> p : Util.alphabetical(Awh)) {
					temp.add(p.first + " = " + p.second);
				}

				sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") + "\n");
			}
			sb.append("->\n");
			if (!Es.isEmpty()) {
				sb.append("\texists");
				if (isUnique) {
					sb.append(" unique");
				}
				List<String> temp = new LinkedList<>();
				for (Pair<String, String> p : Util.alphabetical(Es)) {
					temp.add(p.first + ":" + p.second);
				}

				sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") + "\n");
			}
			if (!Ewh.isEmpty()) {
				sb.append("\twhere");
				List<String> temp = new LinkedList<>();
				for (Pair<RawTerm, RawTerm> p : Util.alphabetical(Ewh)) {
					temp.add(p.first + " = " + p.second);
				}

				sb.append("\n\t\t" + Util.sep(temp, "\n\t\t") + "\n");
			}
			return sb.toString();
		}

		private final List<Pair<String, String>> As;

		private final List<Pair<String, String>> Es;

		private final Set<Pair<RawTerm, RawTerm>> Awh;

		private final Set<Pair<RawTerm, RawTerm>> Ewh;

		public final boolean isUnique;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((As == null) ? 0 : As.hashCode());
			result = prime * result + ((Awh == null) ? 0 : Awh.hashCode());
			result = prime * result + ((Es == null) ? 0 : Es.hashCode());
			result = prime * result + ((Ewh == null) ? 0 : Ewh.hashCode());
			result = prime * result + (isUnique ? 1231 : 1237);
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
			EdExpRaw other = (EdExpRaw) obj;
			if (As == null) {
				if (other.As != null)
					return false;
			} else if (!As.equals(other.As))
				return false;
			if (Awh == null) {
				if (other.Awh != null)
					return false;
			} else if (!Awh.equals(other.Awh))
				return false;
			if (Es == null) {
				if (other.Es != null)
					return false;
			} else if (!Es.equals(other.Es))
				return false;
			if (Ewh == null) {
				if (other.Ewh != null)
					return false;
			} else if (!Ewh.equals(other.Ewh))
				return false;
			if (isUnique != other.isUnique)
				return false;
			return true;
		}

		public EdExpRaw(List<Pair<String, String>> as, List<Pair<RawTerm, RawTerm>> list, List<Pair<String, String>> es,
				List<Pair<RawTerm, RawTerm>> list2, boolean isUnique, @SuppressWarnings("unused") Object u) {
			As = new ArrayList<>(as);
			Es = new ArrayList<>(es);
			Awh = new THashSet<>(list);
			Ewh = new THashSet<>(list2);
			Util.toMapSafely(As);
			Util.toMapSafely(Es);
			this.isUnique = isUnique;
		}

		public EdExpRaw(List<Pair<LocStr, String>> as, List<Pair<Integer, Pair<RawTerm, RawTerm>>> list,
				List<Pair<LocStr, String>> es, List<Pair<Integer, Pair<RawTerm, RawTerm>>> list2, boolean isUnique) {
			As = LocStr.list2(as);
			Es = LocStr.list2(es);
			Util.toMapSafely(As);
			Util.toMapSafely(Es);
			Awh = LocStr.proj2(list);
			Ewh = LocStr.proj2(list2);
			this.isUnique = isUnique;

			List<InteriorLabel<Object>> f = new LinkedList<>();
			for (Pair<LocStr, String> p : as) {
				f.add(new InteriorLabel<>("forall", new Pair<>(p.first.str, p.second), p.first.loc,
						x -> x.first + " : " + x.second).conv());
			}
			raw.put("forall", f);

			f = new LinkedList<>();
			for (Pair<Integer, Pair<RawTerm, RawTerm>> p : list) {
				f.add(new InteriorLabel<>("where", p.second, p.first, x -> x.first + " = " + x.second).conv());
			}
			raw.put("where", f);

			String ex = isUnique ? "exists unique" : "exists";
			f = new LinkedList<>();
			for (Pair<LocStr, String> p : es) {
				f.add(new InteriorLabel<>(ex, new Pair<>(p.first.str, p.second), p.first.loc,
						x -> x.first + " : " + x.second).conv());
			}
			raw.put(ex, f);

			f = new LinkedList<>();
			for (Pair<Integer, Pair<RawTerm, RawTerm>> p : list2) {
				f.add(new InteriorLabel<>("where ", p.second, p.first, x -> x.first + " = " + x.second).conv());
			}
			raw.put("where ", f);
		}

		public ED eval(Schema<Ty, En, Sym, Fk, Att> sch, AqlOptions ops) {
			Pair<Map<Var, Chc<Ty, En>>, Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>>> x = eval1(
					sch, As, Awh);

			Pair<Map<Var, Chc<Ty, En>>, Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>>> y = eval1(
					sch, Util.append(As, Es), Ewh);

			for (Var k : x.first.keySet()) {
				y.first.remove(k);
			}
			return new ED(x.first, y.first, x.second, y.second, isUnique, ops);
		}

		private static Pair<Map<Var, Chc<Ty, En>>, Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>>> eval1(
				Schema<Ty, En, Sym, Fk, Att> sch, List<Pair<String, String>> As, Set<Pair<RawTerm, RawTerm>> Awh) {
			Map<Var, Chc<Ty, En>> As0 = new THashMap<>();
			Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> Awh0 = new THashSet<>();
			Map<String, Chc<Ty, En>> As1 = new THashMap<>();

			Collage<Ty, En, Sym, Fk, Att, Void, Void> col = new Collage<>(sch.collage());

			for (Pair<String, String> p : As) {
				String gen = p.first;
				String ty = p.second;
				Ty tt = Ty.Ty(ty);
				if (col.ens.contains(En.En(ty))) {
					As0.put(Var.Var(gen), Chc.inRight(En.En(ty)));
					As1.put(gen, Chc.inRight(En.En(ty)));
				} else if (col.tys.contains(tt)) {
					As0.put(Var.Var(gen), Chc.inLeft(tt));
					As1.put(gen, Chc.inLeft(tt));
				} else {
					throw new RuntimeException(
							"The sort for " + gen + ", namely " + ty + ", is not declared as an entity");
				}
			}

			for (Pair<RawTerm, RawTerm> eq : Awh) {
				Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq0 = RawTerm
						.infer1x(As1, eq.first, eq.second, null, col.convert(), "", sch.typeSide.js).first3();

				Awh0.add(new Pair<>(eq0.second.convert(), eq0.third.convert()));
			}

			return new Pair<>(As0, Awh0);
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
		public synchronized Void eval0(AqlEnv env, boolean isC) {
			return null;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return null;
		}

		@Override
		public Exp<Void> Var(String v) {
			return null;
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			set.addAll(AqlOptions.proverOptionNames());
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.addAll(AqlOptions.proverOptionNames());
	}
}
