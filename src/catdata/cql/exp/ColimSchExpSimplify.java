package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.InteriorLabel;
import catdata.Pair;
import catdata.Raw;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.ColimitSchema;
import catdata.cql.Kind;
import catdata.cql.Mapping;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.graph.UnionFind;
import gnu.trove.set.hash.THashSet;

public final class ColimSchExpSimplify extends ColimSchExp implements Raw {

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		colim.map(f);
	}

	@Override
	public <R, P, E extends Exception> R accept(P param, ColimSchExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Set<Pair<SchExp, SchExp>> gotos(ColimSchExp ths) {
		Set<Pair<SchExp, SchExp>> ret = new THashSet<>();
		SchExp t = new SchExpColim(ths);
		SchExp s = new SchExpColim(colim);
		ret.add(new Pair<>(s, t));
		return ret;
	}

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return Collections.emptyMap();
	}

	@Override
	public SchExp getNode(String n, AqlTyping G) {
		return colim.getNode(n, G);
	}

	public final ColimSchExp colim;

	public final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	public ColimSchExpSimplify(ColimSchExp colim, List<Pair<String, String>> options) {
		this.options = Util.toMapSafely(options);
		this.colim = colim;

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((colim == null) ? 0 : colim.hashCode());
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
		ColimSchExpSimplify other = (ColimSchExpSimplify) obj;
		if (colim == null) {
			if (other.colim != null)
				return false;
		} else if (!colim.equals(other.colim))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("simplify ").append(colim).append(" {\n");

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
	public synchronized ColimitSchema<String> eval0(AqlEnv env, boolean isC) {
		ColimitSchema<String> colim0 = colim.eval(env, isC);
		boolean shorten = (boolean) new AqlOptions(options, env.defaults).getOrDefault(AqlOption.simplify_names);
		return extracted(shorten, colim0).first;
	}

	public static <N> Triple<ColimitSchema<N>, Mapping, Mapping> extracted(boolean shorten, ColimitSchema<N> colim0x) {	
		Triple<ColimitSchema<N>, Mapping, Mapping> colim0 = new Triple<>(colim0x, Mapping.id(colim0x.schemaStr), Mapping.id(colim0x.schemaStr));
		for (String en : Util.alphabetical(colim0x.schemaStr.ens)) {
			var x = Util.reverseAlphabetical(colim0x.schemaStr.fksFrom(en));
			if (x.size() < 2) {
				continue;
			}
			UnionFind<Fk> uf = new UnionFind<Fk>(x.size(), x);
			Term<String, String, Sym, Fk, Att, Void, Void> v = Term.Var("v");
			Map<String, Chc<String, String>> ctx = Collections.singletonMap("v", Chc.inRight(en));
			for (var fk1 : x) {
				for (var fk2 : x) {
					if (fk1 == fk2 || uf.connected(fk1, fk2)) {
						continue;
					}
					Term<String, String, Sym, Fk, Att, Void, Void> t1 = Term.Fk(fk1, v);
					Term<String, String, Sym, Fk, Att, Void, Void> t2 = Term.Fk(fk2, v);
					if (colim0x.schemaStr.dp.eq(ctx, t1, t2)) {
						uf.union(fk1, fk2);
					}
				}
			}
			Map<Fk, Fk> mins = uf.toRep((s1, s2) -> s1.str.compareTo(s2.str) <= 0);
			for (var fk1 : x) {
				var fk2 = mins.get(fk1);
				if (!fk2.equals(fk1)) {
					var w = colim0.first.removeFk(fk1, Collections.singletonList(fk2), false);
					colim0 = new Triple<>(w.first, Mapping.compose(colim0.second, w.second), Mapping.compose(w.third, colim0.third));
				}
			}
		}

		for (String en : Util.alphabetical(colim0.first.schemaStr.ens)) {
			var x = Util.reverseAlphabetical(colim0.first.schemaStr.attsFrom(en));
			if (x.size() < 2) {
				continue;
			}
			UnionFind<Att> uf = new UnionFind<Att>(x.size(), x);
			Term<String, String, Sym, Fk, Att, Void, Void> v = Term.Var("v");
			Map<String, Chc<String, String>> ctx = Collections.singletonMap("v", Chc.inRight(en));
			for (var fk1 : x) {
				for (var fk2 : x) {
					if (fk1 == fk2 || uf.connected(fk1, fk2)) {
						continue;
					}
					Term<String, String, Sym, Fk, Att, Void, Void> t1 = Term.Att(fk1, v);
					Term<String, String, Sym, Fk, Att, Void, Void> t2 = Term.Att(fk2, v);
					if (colim0.first.schemaStr.dp.eq(ctx, t1, t2)) {
						uf.union(fk1, fk2);
					}
				}
			}
			Map<Att, Att> mins = uf.toRep((s1, s2) -> s1.str.compareTo(s2.str) >= 0);
			for (var fk1 : x) {
				var fk2 = mins.get(fk1);
				if (!fk2.equals(fk1)) {
					Term<String, String, Sym, Fk, Att, Void, Void> t = Term.Att(fk2, v);
					var w = colim0.first.removeAtt(fk1, "v", t, false);
					colim0 = new Triple<>(w.first, Mapping.compose(colim0.second, w.second), Mapping.compose(w.third, colim0.third));
				}
			}
		}
		if (!shorten) {
			return colim0;
		}
		
		Map<String, List<Pair<N, String>>> nodeMapPreIm = new HashMap<>();
		for (N n : Util.alphabetical(colim0.first.nodes.keySet())) {
			Schema<String, String, Sym, Fk, Att> sch = colim0.first.nodes.get(n);
			for (String en : Util.alphabetical(sch.ens)) {
				String en2 = colim0.first.mappingsStr.get(n).ens.get(en);
				if (!nodeMapPreIm.containsKey(en2)) {
					nodeMapPreIm.put(en2, new LinkedList<>());
				}
				nodeMapPreIm.get(en2).add(new Pair<N, String>(n, en));
				for (Att fk : Util.reverseAlphabetical(sch.attsFrom(en))) {
					Att fkCand = Att.Att(en2, n + "_" + en + "_" + fk.str);
					if (colim0.first.schemaStr.atts.containsKey(fkCand)) {
						Att fkCand2 = Att.Att(en2, fk.str);
						if (!colim0.first.schemaStr.atts.containsKey(fkCand2)) {
							var w = colim0.first.renameAtt(fkCand, fkCand2, false);
							colim0 = new Triple<>(w.first, Mapping.compose(colim0.second, w.second), Mapping.compose(w.third, colim0.third));
						}
					}
				}
				for (Fk fk : Util.reverseAlphabetical(sch.fksFrom(en))) {
					Fk fkCand = Fk.Fk(en2, n + "_" + en + "_" + fk.str);
					Att fkCandX = Att.Att(en2, n + "_" + en + "_" + fk.str);
					if (colim0.first.schemaStr.fks.containsKey(fkCand)) {
						Fk fkCand2 = Fk.Fk(en2, fk.str);
						if (!colim0.first.schemaStr.fks.containsKey(fkCand2) && !colim0.first.schemaStr.atts.containsKey(fkCandX)) {
							var w = colim0.first.renameFk(fkCand, fkCand2, false);
							colim0 = new Triple<>(w.first, Mapping.compose(colim0.second, w.second), Mapping.compose(w.third, colim0.third));

						}
					}
				}
			}
		}
		for (var kv : nodeMapPreIm.entrySet()) {
			List<Pair<N, String>> l = kv.getValue();
			l.sort(new Comparator<Pair<N, String>>() {
				@Override
				public int compare(Pair<N, String> o1, Pair<N, String> o2) {
					if (o1.first.equals(o2.first)) {
						return o1.second.compareTo(o2.second);
					}
					return o1.first.toString().compareTo(o2.first.toString());
				}
			});
			for (var v : l) {
				if (!colim0.first.schemaStr.ens.contains(v.second)) {
					var w = colim0.first.renameEntity(kv.getKey(), v.second, false);
					colim0 = new Triple<>(w.first, Mapping.compose(colim0.second, w.second), Mapping.compose(w.third, colim0.third));

					break;
				}
			}
		}
		return colim0;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return colim.deps();
	}

	@Override
	public Set<String> type(AqlTyping G) {
		return colim.type(G);
	}

	@Override
	public TyExp typeOf(AqlTyping G) {
		return colim.typeOf(G);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.simplify_names);
	}

}
