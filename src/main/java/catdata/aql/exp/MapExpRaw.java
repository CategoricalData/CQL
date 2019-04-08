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
import catdata.InteriorLabel;
import catdata.LocException;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.RawTerm;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class MapExpRaw extends MapExp implements Raw {

	@Override
	public <R, P, E extends Exception> MapExp coaccept(P params, MapExpCoVisitor<R, P, E> v, R r) throws E {
		return v.visitMapExpRaw(params, r);
	}

	@Override
	public Collection<Exp<?>> imports() {
		return (Collection<Exp<?>>) (Object) imports;
	}

	public <R, P, E extends Exception> R accept(P params, MapExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		Set<Pair<String, Kind>> ret = new THashSet<>();
		ret.addAll(src.deps());
		ret.addAll(dst.deps());
		for (MapExp x : imports) {
			ret.addAll(x.deps());
		}
		return ret;
	}

	public final SchExp src;
	public final SchExp dst;

	public final Set<MapExp> imports;

	public final Set<Pair<String, String>> ens;
	public final Set<Pair<Pair<String, String>, List<String>>> fks;
	public final Set<Pair<Pair<String, String>, Triple<String, String, RawTerm>>> atts;

	public final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	private Map<En, Integer> enPos = new THashMap<>();
	private Map<Fk, Integer> fkPos = new THashMap<>();
	private Map<Att, Integer> attPos = new THashMap<>();

	public MapExpRaw(SchExp src, SchExp dst, List<MapExp> a,
			List<Pair<LocStr, Triple<String, List<Pair<LocStr, List<String>>>, List<Pair<LocStr, Triple<String, String, RawTerm>>>>>> list,
			List<Pair<String, String>> options) {
		this.src = src;
		this.dst = dst;
		this.imports = new THashSet<>(a);

		Map<LocStr, Triple<String, List<Pair<LocStr, List<String>>>, List<Pair<LocStr, Triple<String, String, RawTerm>>>>> list2 = Util
				.toMapSafely(list);

		this.ens = new THashSet<>();
		this.fks = new THashSet<>();
		this.atts = new THashSet<>();

		for (LocStr en : list2.keySet()) {
			Triple<String, List<Pair<LocStr, List<String>>>, List<Pair<LocStr, Triple<String, String, RawTerm>>>> v = list2
					.get(en);
			this.ens.add(new Pair<>(en.str, v.first));
			En x = En.En(en.str);
			enPos.put(x, en.loc);

			for (Pair<LocStr, List<String>> fk : v.second) {
				this.fks.add(new Pair<>(new Pair<>(en.str, fk.first.str), fk.second));
				fkPos.put(Fk.Fk(x, fk.first.str), fk.first.loc);
			}
			for (Pair<LocStr, Triple<String, String, RawTerm>> att : v.third) {
				this.atts.add(new Pair<>(new Pair<>(en.str, att.first.str), att.second));
				attPos.put(Att.Att(x, att.first.str), att.first.loc);
			}
		}

		this.options = Util.toMapSafely(options);
		Util.toMapSafely(this.ens);
		Util.toMapSafely(this.fks);
		Util.toMapSafely(this.atts); // do here rather than wait

		// List<InteriorLabel<Object>> t = InteriorLabel.imports("imports", imports);
		// raw.put("imports", t);

		raw.put("entities", new LinkedList<>());
		raw.put("foreign keys", new LinkedList<>());
		raw.put("attributes", new LinkedList<>());

		for (Pair<String, String> p : ens) {
			List<InteriorLabel<Object>> inner = new LinkedList<>();
			raw.put(p.first, inner);
			En x = En.En(p.first);
			inner.add(new InteriorLabel<>("entities", new Pair<>(p.first, p.second), enPos.get(x),
					xx -> xx.first + " -> " + xx.second).conv());

			for (Pair<Pair<String, String>, List<String>> q : this.fks.stream()
					.filter(xx -> xx.first.first.equals(p.first)).collect(Collectors.toList())) {
				inner.add(new InteriorLabel<>("foreign keys", q /* new Pair<>(p.first.second.str, p.second) */,
						fkPos.get(Fk.Fk(x, q.first.second)), xx -> xx.first + " -> " + Util.sep(xx.second, "."))
								.conv());
			}

			for (Pair<Pair<String, String>, Triple<String, String, RawTerm>> q : atts.stream()
					.filter(xx -> xx.first.first.equals(p.first)).collect(Collectors.toList())) {
				inner.add(new InteriorLabel<>("attributes", new Pair<>(q.first.second, q.second),
						attPos.get(Att.Att(x, q.first.second)),
						xx -> xx.first + " -> \\" + xx.second.first + ". " + xx.second.third).conv());
			}
		}
	}

	Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return raw;
	}

	@Override
	public String makeString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("literal : ").append(src).append(" -> ").append(dst).append(" {\n");

		if (!imports.isEmpty()) {
			sb.append("\timports");
			sb.append("\n\t\t").append(Util.sep(imports, " ")).append("\n");
		}

		for (Pair<String, String> en : Util.alphabetical(ens)) {
			List<String> temp = new LinkedList<>();

			sb.append("\tentity");

			// for (Pair<LocStr, String> x : Util.alphabetical(ens)) {
			temp.add(en.first + " -> " + en.second);
			// }

			sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");

			List<Pair<Pair<String, String>, List<String>>> x = fks.stream().filter(z -> z.first.first.equals(en.first))
					.collect(Collectors.toList());
			if (!x.isEmpty()) {
				sb.append("\tforeign_keys");
				temp = new LinkedList<>();
				for (Pair<Pair<String, String>, List<String>> sym : Util.alphabetical(x)) {
					if (sym.second.isEmpty()) {
						temp.add(sym.first.second + " -> identity");
					} else {
						temp.add(sym.first.second + " -> " + Util.sep(sym.second, "."));
					}
				}
				sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
			}

			List<Pair<Pair<String, String>, Triple<String, String, RawTerm>>> y = atts.stream()
					.filter(z -> z.first.first.equals(en.first)).collect(Collectors.toList());

			if (!y.isEmpty()) {
				sb.append("\tattributes");
				temp = new LinkedList<>();
				for (Pair<Pair<String, String>, Triple<String, String, RawTerm>> sym : Util.alphabetical(y)) {
					temp.add(sym.first.second + " -> lambda " + sym.second.first + ". " + sym.second.third);
				}
				sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
			}
		}

		if (!options.isEmpty()) {
			sb.append("\toptions");
			List<String> temp = new LinkedList<>();
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
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((ens == null) ? 0 : ens.hashCode());
		result = prime * result + ((fks == null) ? 0 : fks.hashCode());
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
		MapExpRaw other = (MapExpRaw) obj;
		if (atts == null) {
			if (other.atts != null)
				return false;
		} else if (!atts.equals(other.atts))
			return false;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
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
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		return true;
	}

	@Override
	public Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isC) {
		Schema<Ty, En, Sym, Fk, Att> src0 = src.eval(env, isC);
		Schema<Ty, En, Sym, Fk, Att> dst0 = dst.eval(env, isC);
		Collage<Ty, En, Sym, Fk, Att, Void, Void> dcol = new Collage<>(dst0.collage());

		Map<En, En> ens0 = new THashMap<>(ens.size());
		// Map<String, Pair<String, List<String>>> fks0 = new HashMap<>();
		Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> atts0 = new THashMap<>(atts.size());
		Map<Fk, Pair<En, List<Fk>>> fksX = new THashMap<>(fks.size());

		for (MapExp k : imports) {
			Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> v = k.eval(env, isC); // env.defs.maps.get(k);
			Util.putAllSafely(ens0, v.ens);
			Util.putAllSafely(fksX, v.fks);
			Util.putAllSafely(atts0, v.atts);
		}

		Util.putAllSafely(ens0, Util.toMapSafely(
				ens.stream().map(x -> new Pair<>(En.En(x.first), En.En(x.second))).collect(Collectors.toList())));
		for (En k : ens0.keySet()) {
			if (!dst0.ens.contains(ens0.get(k))) {
				throw new LocException(find("entities", new Pair<>(k, ens0.get(k))),
						"The mapping for " + k + ", namely " + ens0.get(k) + ", does not appear in the target schema");
			} else if (!src0.ens.contains(k)) {
				throw new LocException(find("entities", new Pair<>(k, ens0.get(k))),
						k + " does not appear in the source schema");
			}
		}

		for (Pair<Pair<String, String>, List<String>> p : this.fks) {
			En x = En.En(p.first.first);
			Fk theFk = Fk.Fk(x, p.first.second);
			if (!src0.fks.containsKey(theFk)) {
				throw new RuntimeException("Not a foreign key in source: " + theFk.en + "." + theFk.str);
			}
			try {
				En start_en_fixed = ens0.get(x);

				En start_en = ens0.get(x);

				List<Fk> r = new ArrayList<>(p.second.size());
				for (String o : p.second) {
					r.add(Fk.Fk(start_en, o));
					start_en = dst0.fks.get(Fk.Fk(start_en, o)).second;
				}
				fksX.put(Fk.Fk(x, p.first.second), new Pair<>(start_en_fixed, r));
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				throw new LocException(fkPos.get(Fk.Fk(x, p.first.second)), "In foreign key mapping " + p.first + " -> "
						+ Util.sep(p.second, ".") + ", " + ex.getMessage());
			}
		}

		for (Pair<Pair<String, String>, Triple<String, String, RawTerm>> att : atts) {
			try {
				String var = att.second.first;
				String var_en = att.second.second;
				RawTerm term = att.second.third;

				Pair<En, Ty> p = src0.atts.get(Att.Att(En.En(att.first.first), att.first.second));
				if (p == null) {
					throw new RuntimeException(att.first + " is not a source attribute.");
				}
				En src_att_dom_en = p.first;
				En dst_att_dom_en = ens0.get(src_att_dom_en);
				if (dst_att_dom_en == null) {
					throw new RuntimeException(
							"no entity mapping for " + src_att_dom_en + " , required for domain for " + att.first);
				}

				if (var_en != null && !En.En(var_en).equals(dst_att_dom_en)) {
					throw new RuntimeException("the given source entity for the variable, " + var_en + ", is not "
							+ dst_att_dom_en + " as expected.");
				}

				Ty src_att_cod_ty = p.second;
				if (!dst0.typeSide.tys.contains(src_att_cod_ty)) {
					throw new RuntimeException("type " + p.second + " does not exist in target typeside.");
				}
				Chc<Ty, En> proposed_ty2 = Chc.inLeft(src_att_cod_ty);

				Chc<Ty, En> var_en2 = Chc.inRight(dst_att_dom_en);

				Map<String, Chc<Ty, En>> ctx = Collections.singletonMap(var, var_en2);

				Term<Ty, En, Sym, Fk, Att, Gen, Sk> term0 = RawTerm.infer1x(ctx, term, null, proposed_ty2,
						dcol.convert(), "", src0.typeSide.js).second;

				Util.putSafely(atts0, Att.Att(En.En(att.first.first), att.first.second),
						new Triple<>(Var.Var(var), dst_att_dom_en, term0.convert()));
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				throw new LocException(attPos.get(Att.Att(En.En(att.first.first), att.first.second)),
						"in mapping for " + att.first + ", " + ex.getMessage());
			}
		}

		AqlOptions ops = new AqlOptions(options, null, env.defaults);

		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> ret = new Mapping<>(ens0, atts0, fksX, src0, dst0,
				(Boolean) ops.getOrDefault(AqlOption.dont_validate_unsafe));
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
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.dont_validate_unsafe);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		src.map(f);
		dst.map(f);
	}
}
