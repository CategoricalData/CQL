package catdata.cql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import catdata.cql.exp.Att;
import catdata.cql.exp.ColimSchExpSimplify;
import catdata.cql.exp.Fk;
import catdata.cql.exp.MapExpToPrefix;
import catdata.cql.exp.RawTerm;
import catdata.cql.exp.Sym;
import catdata.graph.DMG;
import catdata.graph.UnionFind;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class ColimitSchema<N> implements Semantics {

	/**
	 * size of underlying schema
	 */
	@Override
	public int size() {
		return schemaStr.size();
	}

	public final TypeSide<String, Sym> ty;

	public final LinkedHashMap<N, Schema<String, String, Sym, Fk, Att>> nodes;

	// public final Schema<Ty, Set<Pair<N,En>>, Sym, Pair<N,Fk>, Pair<N,Att>>
	// schema;

	// public final Map<N,
	// Mapping<Ty,En,Sym,Fk,Att,Set<Pair<N,En>>,Pair<N,Fk>,Pair<N,Att>>> mappings;

	// actually final
	public final Schema<String, String, Sym, Fk, Att> schemaStr;

	// actually final
	public final Map<N, Mapping<String, String, Sym, Fk, Att, String, Fk, Att>> mappingsStr;

	// TODO: should be triple that also returns mappings
	public Triple<ColimitSchema<N>, Mapping, Mapping> renameEntity(String src, String dst, boolean checkJava) {
		if (!schemaStr.ens.contains(src)) {
			throw new RuntimeException(src + " is not an entity in \n" + schemaStr);
		}
		if (schemaStr.ens.contains(dst)) {
			throw new RuntimeException(dst + " is already an entity in \n" + schemaStr);
		}
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoToUser = Mapping.id(schemaStr);
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoFromUser = Mapping.id(schemaStr);

		Function<Fk, Fk> updateFk = x -> {
			if (x.en.equals(src)) {
				return Fk.Fk(dst, x.str);
			}
			return x;
		};

		Function<Fk, Fk> deUpdateFk = x -> {
			if (x.en.equals(dst)) {
				return Fk.Fk(src, x.str);
			}
			return x;
		};

		Function<Att, Att> updateAtt = x -> {
			if (x.en.equals(src)) {
				return Att.Att(dst, x.str);
			}
			return x;
		};

		Function<Att, Att> deUpdateAtt = x -> {
			if (x.en.equals(dst)) {
				return Att.Att(src, x.str);
			}
			return x;
		};

		Set<String> ens = (new LinkedHashSet<>(schemaStr.ens));
		ens.remove(src);
		ens.add(dst);
		Map<Att, Pair<String, String>> atts = (new LinkedHashMap<>());
		for (Att k : schemaStr.atts.keySet()) {
			Pair<String, String> v = schemaStr.atts.get(k);
			String s = v.first.equals(src) ? dst : v.first;
			atts.put(updateAtt.apply(k), new Pair<>(s, v.second));
		}
		Map<Fk, Pair<String, String>> fks = (new LinkedHashMap<>());
		for (Fk k : schemaStr.fks.keySet()) {
			Pair<String, String> v = schemaStr.fks.get(k);
			String s = v.first.equals(src) ? dst : v.first;
			String t = v.second.equals(src) ? dst : v.second;
			fks.put(updateFk.apply(k), new Pair<>(s, t));
		}
		Set<Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> eqs = new THashSet<>(
				schemaStr.eqs.size());
		for (Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> eq : schemaStr.eqs) {
			Pair<String, String> v = eq.first;
			String t = v.second.equals(src) ? dst : v.second;
			eqs.add(new Triple<>(new Pair<>(v.first, t), eq.second.mapFk(updateFk).mapAtt(updateAtt),
					eq.third.mapFk(updateFk).mapAtt(updateAtt)));
		}
		DP<String, String, Sym, Fk, Att, Void, Void> dp = new DP<>() {
			@Override
			public String toStringProver() {
				return "rename entity of " + schemaStr.dp.toStringProver();
			}

			@Override
			public boolean eq(Map<String, Chc<String, String>> Map, Term<String, String, Sym, Fk, Att, Void, Void> lhs,
					Term<String, String, Sym, Fk, Att, Void, Void> rhs) {
				return schemaStr.dp.eq(
						Util.map(Map, (k, v) -> new Pair<>(k, v.left ? v : (v.r.equals(dst) ? Chc.inRight(src) : v))),
						lhs.mapFk(deUpdateFk).mapAtt(deUpdateAtt), rhs.mapFk(deUpdateFk).mapAtt(deUpdateAtt));
			}
		};
		Schema<String, String, Sym, Fk, Att> schemaStr2 = new Schema<>(ty, ens, atts, fks, eqs, dp, checkJava); // TODO
																												// aql
																												// java
		Map<String, String> ensM = (new LinkedHashMap<>(schemaStr.ens.size()));
		for (String k : schemaStr.ens) {
			ensM.put(k, k.equals(src) ? dst : k);
		}
		Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> attsM = (new LinkedHashMap<>(
				schemaStr.atts.size()));
		for (Att k : schemaStr.atts.keySet()) {
			attsM.put(k,
					new Triple<>(isoToUser.atts.get(k).first,
							isoToUser.atts.get(k).second.equals(src) ? dst : isoToUser.atts.get(k).second,
							isoToUser.atts.get(k).third.mapAtt(updateAtt).mapFk(updateFk)));
		}
		Map<Fk, Pair<String, List<Fk>>> fksM = (new LinkedHashMap<>(schemaStr.fks.size()));
		for (Fk k : schemaStr.fks.keySet()) {
			fksM.put(k, new Pair<>(isoToUser.fks.get(k).first.equals(src) ? dst : isoToUser.fks.get(k).first,
					isoToUser.fks.get(k).second.stream().map(updateFk).collect(Collectors.toList())));
		}
		isoToUser = new Mapping<>(ensM, attsM, fksM, schemaStr, schemaStr2, true);
		Map<String, String> ensM2 = (new LinkedHashMap<>());
		for (String k : schemaStr2.ens) {
			ensM2.put(k, k.equals(dst) ? src : k);
		}
		Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> attsM2 = new LinkedHashMap<>();
		for (Att k : schemaStr2.atts.keySet()) {
			attsM2.put(updateAtt.apply(k),
					new Triple<>(isoFromUser.atts.get(deUpdateAtt.apply(k)).first,
							isoFromUser.atts.get(deUpdateAtt.apply(k)).second.equals(dst) ? src
									: isoFromUser.atts.get(deUpdateAtt.apply(k)).second,
							isoFromUser.atts.get(deUpdateAtt.apply(k)).third));
		}
		Map<Fk, Pair<String, List<Fk>>> fksM2 = new LinkedHashMap<>();
		for (Fk k : schemaStr2.fks.keySet()) {
			fksM2.put(updateFk.apply(k),
					new Pair<>(
							isoFromUser.fks.get(deUpdateFk.apply(k)).first.equals(dst) ? src
									: isoFromUser.fks.get(deUpdateFk.apply(k)).first,
							isoFromUser.fks.get(deUpdateFk.apply(k)).second.stream().map(deUpdateFk)
									.collect(Collectors.toList())));
		}
		isoFromUser = new Mapping<>(ensM2, attsM2, fksM2, schemaStr2, schemaStr, true);

		return new Triple<>(wrap(isoToUser, isoFromUser), isoToUser, isoFromUser);
	}

	public Triple<ColimitSchema<N>, Mapping, Mapping> renameFk(Fk src, Fk dst, boolean checkJava) {
		if (!schemaStr.fks.containsKey(src)) {
			throw new RuntimeException(src.en + "." + src.str + " is not a foreign_key in \n" + schemaStr);
		}
		if (schemaStr.fks.containsKey(dst)) {
			throw new RuntimeException(dst + " is already a foreign_key in \n" + schemaStr);
		}
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoToUser = Mapping.id(schemaStr);
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoFromUser = Mapping.id(schemaStr);
		Function<Fk, Fk> fun = x -> x.equals(src) ? dst : x;
		Function<Fk, Fk> fun2 = x -> x.equals(dst) ? src : x;

		Map<Fk, Pair<String, String>> fks = (new LinkedHashMap<>(schemaStr.fks.keySet().size()));
		for (Fk k : schemaStr.fks.keySet()) {
			fks.put(fun.apply(k), schemaStr.fks.get(k));
		}
		Set<Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> eqs = (new LinkedHashSet<>(
				schemaStr.eqs.size()));
		for (Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> eq : schemaStr.eqs) {
			eqs.add(new Triple<>(eq.first, eq.second.mapFk(fun), eq.third.mapFk(fun)));
		}
		DP<String, String, Sym, Fk, Att, Void, Void> dp = new DP<>() {
			@Override
			public String toStringProver() {
				return "rename foreign key of " + schemaStr.dp.toStringProver();
			}

			@Override
			public boolean eq(Map<String, Chc<String, String>> Map, Term<String, String, Sym, Fk, Att, Void, Void> lhs,
					Term<String, String, Sym, Fk, Att, Void, Void> rhs) {
				return schemaStr.dp.eq(Map, lhs.mapFk(fun2), rhs.mapFk(fun2));
			}
		};
		Schema<String, String, Sym, Fk, Att> schemaStr2 = new Schema<>(ty, schemaStr.ens, schemaStr.atts, fks, eqs, dp,
				true); // TODO aql java
		Map<Fk, Pair<String, List<Fk>>> fksM = new LinkedHashMap<>(schemaStr.fks.size());
		for (Fk k : schemaStr.fks.keySet()) {
			fksM.put(k, new Pair<>(schemaStr.fks.get(k).first,
					k.equals(src) ? Collections.singletonList(dst) : Collections.singletonList(k)));
		}
		isoToUser = new Mapping<>(isoToUser.ens, isoToUser.atts, fksM, schemaStr, schemaStr2, true);
		Map<Fk, Pair<String, List<Fk>>> fksM2 = new LinkedHashMap<>(schemaStr.fks.size());
		for (Fk k : schemaStr2.fks.keySet()) {
			fksM2.put(k, new Pair<>(schemaStr2.fks.get(k).first,
					k.equals(dst) ? Collections.singletonList(src) : Collections.singletonList(k)));
		}
		isoFromUser = new Mapping<>(isoFromUser.ens, isoFromUser.atts, fksM2, schemaStr2, schemaStr, true);

		return new Triple<>(wrap(isoToUser, isoFromUser), isoToUser, isoFromUser);
	}

	public Triple<ColimitSchema<N>, Mapping, Mapping> renameAtt(Att src, Att dst, boolean checkJava) {
		if (!schemaStr.atts.containsKey(src)) {
			throw new RuntimeException(src + " is not an attribute of " + src.en + " in \n" + schemaStr);
		}
		if (schemaStr.atts.containsKey(dst)) {
			throw new RuntimeException(dst + " is already an attribute in \n" + schemaStr);
		}
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoToUser = Mapping.id(schemaStr);
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoFromUser = Mapping.id(schemaStr);
		Function<Att, Att> fun = x -> x.equals(src) ? dst : x;
		Function<Att, Att> fun2 = x -> x.equals(dst) ? src : x;

		Map<Att, Pair<String, String>> atts = new LinkedHashMap<>(schemaStr.atts.size());
		for (Att k : schemaStr.atts.keySet()) {
			atts.put(fun.apply(k), schemaStr.atts.get(k));
		}
		Set<Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> eqs = (new THashSet<>());
		for (Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> eq : schemaStr.eqs) {
			eqs.add(new Triple<>(eq.first, eq.second.mapAtt(fun), eq.third.mapAtt(fun)));
		}
		DP<String, String, Sym, Fk, Att, Void, Void> dp = new DP<>() {
			@Override
			public String toStringProver() {
				return "rename attribute of " + schemaStr.dp.toStringProver();
			}

			@Override
			public boolean eq(Map<String, Chc<String, String>> Map, Term<String, String, Sym, Fk, Att, Void, Void> lhs,
					Term<String, String, Sym, Fk, Att, Void, Void> rhs) {
				return schemaStr.dp.eq(Map, lhs.mapAtt(fun2), rhs.mapAtt(fun2));
			}
		};
		Schema<String, String, Sym, Fk, Att> schemaStr2 = new Schema<>(ty, schemaStr.ens, atts, schemaStr.fks, eqs, dp,
				checkJava);
		Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> attsM = new THashMap<>(
				schemaStr.atts.size());
		for (Att k : schemaStr.atts.keySet()) {
			attsM.put(k, new Triple<>(isoToUser.atts.get(k).first, isoToUser.atts.get(k).second,
					isoToUser.atts.get(k).third.mapAtt(fun)));
		}
		isoToUser = new Mapping<>(isoToUser.ens, attsM, isoToUser.fks, schemaStr, schemaStr2, true);
		Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> attsM2 = new THashMap<>(
				schemaStr2.atts.size());
		String v = ("v");
		for (Att k : schemaStr2.atts.keySet()) {
			attsM2.put(k, new Triple<>(v, schemaStr2.atts.get(k).first, Term.Att(fun2.apply(k), Term.Var(v))));
		}
		isoFromUser = new Mapping<>(isoFromUser.ens, attsM2, isoFromUser.fks, schemaStr2, schemaStr, true);

		return new Triple<>(wrap(isoToUser, isoFromUser), isoToUser, isoFromUser);
	}

	public Triple<ColimitSchema<N>, Mapping, Mapping> removeFk(Fk src, List<Fk> l, boolean checkJava) {
		String v = ("v");
		Term<String, String, Sym, Fk, Att, Void, Void> t = Term.Fks(l, Term.Var(v));
		if (!schemaStr.fks.containsKey(src)) {
			throw new RuntimeException(src + " is not a foreign_key in \n" + schemaStr);
		}
		if (l.contains(src)) {
			throw new RuntimeException(
					"Cannot replace " + src + " with " + Util.sep(l, ".") + " because that path contains " + src);
		}
		String en1 = schemaStr.fks.get(src).first;
		String en2 = schemaStr.fks.get(src).second;
		if (!schemaStr.type(new Pair<>(v, en1), t).equals(Chc.inRight(en2))) {
			throw new RuntimeException("The term " + t + " has type "
					+ schemaStr.type(new Pair<>(v, en1), t).toStringMash() + " and not " + en2 + " as expected.");
		}
		if (!schemaStr.dp.eq(Collections.singletonMap(v, Chc.inRight(en1)), t, Term.Fk(src, Term.Var(v)))) {
			throw new RuntimeException("The term " + t + " is not provably equal to " + Term.Fk(src, Term.Var(v)));
		}
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoToUser = Mapping.id(schemaStr);
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoFromUser = Mapping.id(schemaStr);

		Map<Fk, Pair<String, String>> fks = new LinkedHashMap<>(schemaStr.fks);
		fks.remove(src);
		Set<Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> eqs = new LinkedHashSet<>(
				schemaStr.eqs.size());
		for (Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> eq : schemaStr.eqs) {
			Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> tr = new Triple<>(
					eq.first, eq.second.replaceHead(Head.FkHead(src), Collections.singletonList(v), t),
					eq.third.replaceHead(Head.FkHead(src), Collections.singletonList(v), t));
			if (!tr.second.equals(tr.third) && !eqs.contains(tr)) {
				eqs.add(tr);
			}
		}
		DP<String, String, Sym, Fk, Att, Void, Void> dp = new DP<>() {
			@Override
			public String toStringProver() {
				return "remove foreign key of " + schemaStr.dp.toStringProver();
			}

			@Override
			public boolean eq(Map<String, Chc<String, String>> Map, Term<String, String, Sym, Fk, Att, Void, Void> lhs,
					Term<String, String, Sym, Fk, Att, Void, Void> rhs) {
				return schemaStr.dp.eq(Map, lhs, rhs);
			}
		};
		Schema<String, String, Sym, Fk, Att> schemaStr2 = new Schema<>(ty, schemaStr.ens, schemaStr.atts, fks, eqs, dp,
				checkJava);
		Map<Fk, Pair<String, List<Fk>>> fksM = new LinkedHashMap<>(isoToUser.fks);
		fksM.put(src, new Pair<>(en1, l));
		isoToUser = new Mapping<>(isoToUser.ens, isoToUser.atts, fksM, schemaStr, schemaStr2, checkJava);
		Map<Fk, Pair<String, List<Fk>>> fksM2 = new LinkedHashMap<>(isoFromUser.fks);
		fksM2.remove(src);
		isoFromUser = new Mapping<>(isoFromUser.ens, isoFromUser.atts, fksM2, schemaStr2, schemaStr, checkJava);

		return new Triple<>(wrap(isoToUser, isoFromUser), isoToUser, isoFromUser);
	}

	public Triple<ColimitSchema<N>, Mapping, Mapping> removeAtt(Att src, String v,
			Term<String, String, Sym, Fk, Att, Void, Void> t, boolean checkJava0) {
		if (!schemaStr.atts.containsKey(src)) {
			throw new RuntimeException(src + " is not an attribute in \n" + schemaStr);
		}
		String en1 = schemaStr.atts.get(src).first;
		String ty0 = schemaStr.atts.get(src).second;
		if (!schemaStr.type(new Pair<>(v, en1), t).equals(Chc.inLeft(ty0))) {
			throw new RuntimeException("The term " + t + " has type "
					+ schemaStr.type(new Pair<>(v, en1), t).toStringMash() + " and not " + ty0 + " as expected.");
		}
		if (!schemaStr.dp.eq(Collections.singletonMap(v, Chc.inRight(en1)), t, Term.Att(src, Term.Var(v)))) {
			throw new RuntimeException("The term " + t + " is not provably equal to " + Term.Att(src, Term.Var(v)));
		}
		if (t.contains(Head.AttHead(src))) {
			throw new RuntimeException("Cannot replace " + src + " with " + t + " because that term contains " + src);
		}
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoToUser = Mapping.id(schemaStr);
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoFromUser = Mapping.id(schemaStr);

		Map<Att, Pair<String, String>> atts = new LinkedHashMap<>(schemaStr.atts);
		atts.remove(src);
		Set<Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> eqs = (new THashSet<>());
		for (Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> eq : schemaStr.eqs) {
			Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> tr = new Triple<>(
					eq.first, eq.second.replaceHead(Head.AttHead(src), Collections.singletonList(v), t),
					eq.third.replaceHead(Head.AttHead(src), Collections.singletonList(v), t));
			if (!tr.second.equals(tr.third) && !eqs.contains(tr)) {
				eqs.add(tr);
			}
		}
		DP<String, String, Sym, Fk, Att, Void, Void> dp = new DP<>() {
			@Override
			public String toStringProver() {
				return "remove attribute of " + schemaStr.dp.toStringProver();
			}

			@Override
			public boolean eq(Map<String, Chc<String, String>> Map, Term<String, String, Sym, Fk, Att, Void, Void> lhs,
					Term<String, String, Sym, Fk, Att, Void, Void> rhs) {
				return schemaStr.dp.eq(Map, lhs, rhs);
			}
		};
		Schema<String, String, Sym, Fk, Att> schemaStr2 = new Schema<>(ty, schemaStr.ens, atts, schemaStr.fks, eqs, dp,
				true);
		Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> attsM = new THashMap<>(
				isoToUser.atts);
		attsM.put(src, new Triple<>(v, en1, t));
		isoToUser = new Mapping<>(isoToUser.ens, attsM, isoToUser.fks, schemaStr, schemaStr2, true);
		Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> attsM2 = new LinkedHashMap<>(
				isoFromUser.atts);
		attsM2.remove(src);
		isoFromUser = new Mapping<>(isoFromUser.ens, attsM2, isoFromUser.fks, schemaStr2, schemaStr, true);

		return new Triple<>(wrap(isoToUser, isoFromUser), isoToUser, isoFromUser);
	}

	public ColimitSchema<N> wrap(Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoToUser,
			Mapping<String, String, Sym, Fk, Att, String, Fk, Att> isoFromUser) {
		if (!isoToUser.src.equals(schemaStr)) {
			throw new RuntimeException("Source of " + isoToUser + " \n, namely " + isoToUser.src
					+ "\ndoes not match canonical colimit, namely " + schemaStr);
		}
		checkIso(isoToUser, isoFromUser);
		Map<N, Mapping<String, String, Sym, Fk, Att, String, Fk, Att>> newMapping = Util.map(mappingsStr,
				(k, v) -> new Pair<>(k, Mapping.compose(v, isoToUser)));
		return new ColimitSchema<>(ty, nodes, isoToUser.dst, newMapping);
	}

	private ColimitSchema(TypeSide<String, Sym> ty, LinkedHashMap<N, Schema<String, String, Sym, Fk, Att>> nodes,
			Schema<String, String, Sym, Fk, Att> schemaStr,
			Map<N, Mapping<String, String, Sym, Fk, Att, String, Fk, Att>> mappingsStr) {
		this.ty = ty;
		this.nodes = nodes;
		this.schemaStr = schemaStr;
		this.mappingsStr = mappingsStr;
	}

	public class Renamer2 {

		public final Map<Set<Pair<N, String>>, String> m1 = Util.mk();
		public final Map<String, Set<Pair<N, String>>> m2 = Util.mk();

		public final Collage<String, String, Sym, Fk, Att, Void, Void> colX;
		public final Map<N, Mapping<String, String, Sym, Fk, Att, String, Fk, Att>> mappingsStr0 = new LinkedHashMap<>();

		public final Schema<String, String, Sym, Fk, Att> schemaStr0;

		Map<Set<Pair<N, String>>, Map<Pair<N, Fk>, String>> mFk = new LinkedHashMap<>();
		Map<Set<Pair<N, String>>, Map<Pair<N, Att>, String>> mAtt = new LinkedHashMap<>();

		final Map<Pair<N, String>, Set<Pair<N, String>>> eqcs;
		boolean left = false;
		private Collage<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col;

		/**
		 * Relies on one schema being "first" (s1 + s2): - for any two entities, if they
		 * are equated, then the resulting object's name is taken from the first schema.
		 * - if e1 from s1 shares a name with e2 from s2 yet they are *not* equated,
		 * then we disambiguate the latter with a prefix (doesn't matter what, perhaps
		 * "<s2 name>_<e2 name>")
		 * 
		 * - for any two attributes that are equated, the resulting attribute's name is
		 * taken from the first schema - if attr1 from e1 from s1 is not equated with
		 * attr2 from e2 from s2, yet they share the same name and e1 was equated with
		 * e2, then we disambiguate attr2 with a prefix (doesn't matter, perhaps
		 * "<e2_name>_<attr2_name>"
		 */
		public Renamer2(Collection<N> order, Map<Pair<N, String>, Set<Pair<N, String>>> eqcs,
				Collage<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Map<N, Mapping<String, String, Sym, Fk, Att, Set<Pair<N, String>>, Pair<N, Fk>, Pair<N, Att>>> mappings,
				AqlOptions options) {
			this.eqcs = eqcs;
			if (order.size() != 2) {
				throw new RuntimeException("left_bias requires exactly two schemas as input.");
			}
			Iterator<N> i = order.iterator();
			i.hasNext();
			N s1 = i.next();
			i.hasNext();
			N s2 = i.next();

			doEns(s1, s2);
			this.col = col;
			doFksAndAtts(s1, s2);

			// System.out.println("f1 " + m1);
			// Util.anomaly();
			colX = new CCollage<>(ty.collage());

			colX.getEns().addAll(col.getEns().stream().map(this::conv1).collect(Collectors.toSet()));

			colX.atts().putAll(
					Util.map(col.atts(), (k, v) -> new Pair<>(Att.Att(conv1(col.atts().get(k).first), conv2Att(k)),
							new Pair<>(conv1(v.first), v.second))));

			colX.fks().putAll(Util.map(col.fks(), (k, v) -> new Pair<>(Fk.Fk(conv1(col.fks().get(k).first), conv2Fk(k)),
					new Pair<>(conv1(v.first), conv1(v.second)))));

			colX.eqs().addAll(col.eqs().stream().map(t -> new Eq<>(Util.map(t.ctx, (k, v) -> new Pair<>(k, conv4(v))),
					conv3(col, t.lhs), conv3(col, t.rhs))).collect(Collectors.toSet()));

			// System.out.println("colX " + colX);
			schemaStr0 = new Schema<>(ty, colX, options);

			for (N n : order) {
				// System.out.println(n + " " + mappings.get(n));
				mappingsStr0.put(n, conv5(n, col, schemaStr0, mappings.get(n)));
			}

		}

		private Mapping<String, String, Sym, Fk, Att, String, Fk, Att> conv5(N n,
				Collage<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Schema<String, String, Sym, Fk, Att> s,
				Mapping<String, String, Sym, Fk, Att, Set<Pair<N, String>>, Pair<N, Fk>, Pair<N, Att>> m) {
			Map<String, String> ens = Util.map(m.ens, (k, v) -> new Pair<>(k, conv1(v)));
			Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> atts = Util.map(m.atts,
					(k, v) -> new Pair<>(k, new Triple<>(v.first, conv1(v.second), conv3(col, v.third))));
			Map<Fk, Pair<String, List<Fk>>> fks = Util.map(m.fks, (k, v) -> new Pair<>(k, new Pair<>(conv1(v.first),
					v.second.stream().map(x -> Fk.Fk(conv1(v.first), conv2Fk(x))).collect(Collectors.toList()))));
			// System.out.println(fks);
			return new Mapping<>(ens, atts, fks, m.src, s, false);
		}

		private String conv1(Set<Pair<N, String>> eqc) {
			return (m1.get(eqc));

		}

		// private String conv2En(Set<Pair<N, String>> eqc) {
		// return conv1(eqc);
		// }

		private String conv2Fk(Pair<N, Fk> p) {
			return mFk.get(col.fks().get(p).first).get(p);
		}

		private String conv2Att(Pair<N, Att> p) {
			return mAtt.get(col.atts().get(p).first).get(p);
		}

		private Term<String, String, Sym, Fk, Att, Void, Void> conv3(
				Collage<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Term<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> t) {
			return t.map(Function.identity(), Function.identity(),
					x -> Fk.Fk(conv1(col.fks().get(x).first), conv2Fk(x)),
					x -> Att.Att(conv1(col.atts().get(x).first), conv2Att(x)), Function.identity(),
					Function.identity());
		}

		private Chc<String, String> conv4(Chc<String, Set<Pair<N, String>>> v) {
			if (v.left) {
				return Chc.inLeft(v.l);
			}
			return Chc.inRight(conv1(v.r));
		}

		private void doFksAndAtts(N s1, N s2) {
			// System.out.println(s1 + " and " + s2);
			List<Pair<N, Fk>> x = new ArrayList<>(col.fks().keySet());
			Collections.sort(x, (l, r) -> {
				if (l.first.equals(s1) && l.first.equals(s1)) {
					return Util.ToStringComparator.compare(l, r);
				}
				if (l.first.equals(s2) && l.first.equals(s2)) {
					return Util.ToStringComparator.compare(l, r);
				}
				if (l.first.equals(s1) && l.first.equals(s2)) {
					return 1;
				}
				if (l.first.equals(s1) && l.first.equals(s2)) {
					return -1;
				}
				return Util.anomaly();
			});

			for (Pair<N, Fk> fk : x) {
				Pair<Set<Pair<N, String>>, Set<Pair<N, String>>> st = col.fks().get(fk);
				Set<Pair<N, String>> s = st.first;

				if (!mFk.get(s).containsValue(fk.second.str)) {
					mFk.get(s).put(fk, fk.second.str);
				} else {
					mFk.get(s).put(fk, s2 + "_" + fk.second.str);
				}
			}

			List<Pair<N, Att>> y = new ArrayList<>(col.atts().keySet());
			Collections.sort(x, (l, r) -> {
				if (l.first.equals(s1) && l.first.equals(s1)) {
					return Util.ToStringComparator.compare(l, r);
				}
				if (l.first.equals(s2) && l.first.equals(s2)) {
					return Util.ToStringComparator.compare(l, r);
				}
				if (l.first.equals(s1) && l.first.equals(s2)) {
					return 1;
				}
				if (l.first.equals(s1) && l.first.equals(s2)) {
					return -1;
				}
				return Util.anomaly();
			});
			for (Pair<N, Att> att : y) {
				Pair<Set<Pair<N, String>>, String> st = col.atts().get(att);
				Set<Pair<N, String>> s = st.first;
				if (!mAtt.get(s).containsValue(att.second.str)) {
					mAtt.get(s).put(att, att.second.str);
				} else {
					mAtt.get(s).put(att, s2 + "_" + att.second.str);
				}
			}

		}

		private void doEns(N s1, N s2) {
			for (String e1 : nodes.get(s1).ens) {
				Pair<N, String> len = new Pair<>(s1, e1);
				Set<Pair<N, String>> leqc = eqcs.get(len);
				for (String e2 : nodes.get(s2).ens) {
					Pair<N, String> ren = new Pair<>(s2, e2);
					Set<Pair<N, String>> reqc = eqcs.get(ren);

					if (leqc.equals(reqc)) {
						String other = m1.get(leqc);
						if (other != null && !other.equals(e1)) {
							throw new RuntimeException("Conflict-A on e1 = " + e1 + " and e2 = " + e2
									+ "; previous was " + other + " and current is " + e1);
						}
						m1.put(leqc, e1);
						mFk.put(leqc, new LinkedHashMap<>());
						mAtt.put(leqc, new LinkedHashMap<>());
					}
				}
			}
			for (String e1 : nodes.get(s1).ens) {
				Pair<N, String> len = new Pair<>(s1, e1);
				Set<Pair<N, String>> leqc = eqcs.get(len);
				for (String e2 : nodes.get(s2).ens) {
					Pair<N, String> ren = new Pair<>(s2, e2);
					Set<Pair<N, String>> reqc = eqcs.get(ren);

					if (leqc.equals(reqc)) {
						continue;
					}
					if (e1.equals(e2)) {
						String other = m1.get(leqc);
						if (other != null && !other.equals(e1)) {
							throw new RuntimeException("Conflict-B on e1 = " + e1 + " and e2 = " + e2
									+ "; previous was " + other + " and current is " + e1);
						}
						m1.put(leqc, e1);
						mFk.put(leqc, new LinkedHashMap<>());
						mAtt.put(leqc, new LinkedHashMap<>());

						other = m1.get(reqc);
						if (other != null && !other.equals(s2 + "_" + e2)) {
							throw new RuntimeException("Conflict-C on e1 = " + e1 + " and e2 = " + e2
									+ "; previous was " + other + " and current is " + s2 + "_" + e2);
						}
						m1.put(reqc, s2 + "_" + e2);
						mFk.put(reqc, new LinkedHashMap<>());
						mAtt.put(reqc, new LinkedHashMap<>());

					}
				}

			}
			for (String e1 : nodes.get(s1).ens) {
				Pair<N, String> len = new Pair<>(s1, e1);
				Set<Pair<N, String>> leqc = eqcs.get(len);
				String other = m1.get(leqc);
				if (other != null) {
					continue;
				}

				m1.put(leqc, e1);
				mFk.put(leqc, new LinkedHashMap<>());
				mAtt.put(leqc, new LinkedHashMap<>());
			}
			for (String e2 : nodes.get(s2).ens) {
				Pair<N, String> ren = new Pair<>(s2, e2);
				Set<Pair<N, String>> reqc = eqcs.get(ren);
				String other = m1.get(reqc);
				if (other != null) {
					continue;
				}

				m1.put(reqc, e2);
				mFk.put(reqc, new LinkedHashMap<>());
				mAtt.put(reqc, new LinkedHashMap<>());

			}
			if (m1.size() != new THashSet<>(m1.values()).size()) {
				throw new RuntimeException("Ambiguous renaming: " + Util.sep(m1, "\n", ","));
			}
		}

	}

	public class Renamer {

		public final Map<Set<Pair<N, String>>, String> m1 = new LinkedHashMap<>();
		public final Map<String, Set<Pair<N, String>>> m2 = new LinkedHashMap<>();

		public final Collage<String, String, Sym, Fk, Att, Void, Void> colX;
		public final Map<N, Mapping<String, String, Sym, Fk, Att, String, Fk, Att>> mappingsStr0 = new THashMap<>();

		public final Schema<String, String, Sym, Fk, Att> schemaStr0;

		// Map<Pair<N, En>, Set<Pair<N, En>>> eqcs;
		Map<Pair<Set<Pair<N, String>>, String>, Set<N>> mEn = new LinkedHashMap<>();
		Map<Pair<Set<Pair<N, String>>, String>, Set<N>> mFk = new LinkedHashMap<>();
		Map<Pair<Set<Pair<N, String>>, String>, Set<N>> mAtt = new LinkedHashMap<>();

		final boolean shorten;
		final Map<Pair<N, String>, Set<Pair<N, String>>> eqcs;

		public Renamer(Map<Pair<N, String>, Set<Pair<N, String>>> eqcs,
				Collage<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Map<N, Mapping<String, String, Sym, Fk, Att, Set<Pair<N, String>>, Pair<N, Fk>, Pair<N, Att>>> mappings,
				AqlOptions options) {
			this.eqcs = eqcs;
			this.shorten = (boolean) options.getOrDefault(AqlOption.simplify_names);
			for (N n : mappings.keySet()) {
				for (String en : nodes.get(n).ens) {
					Set<Pair<N, String>> pp = eqcs.get(new Pair<>(n, en));
					Pair<Set<Pair<N, String>>, String> p = new Pair<>(pp, en);

					if (!mEn.containsKey(p)) {
						mEn.put(p, new LinkedHashSet<>());
					}
					mEn.get(p).add(n);
					for (Fk fk : nodes.get(n).fksFrom(en)) {
						Pair<Set<Pair<N, String>>, String> f = new Pair<>(pp, fk.str);
						if (!mFk.containsKey(f)) {
							mFk.put(f, new LinkedHashSet<>());
						}
						mFk.get(f).add(n);
					}
					for (Att att : nodes.get(n).attsFrom(en)) {
						Pair<Set<Pair<N, String>>, String> f = new Pair<>(pp, att.str);
						if (!mAtt.containsKey(f)) {
							mAtt.put(f, new LinkedHashSet<>());
						}
						mAtt.get(f).add(n);
					}
				}
			}

			for (Set<Pair<N, String>> eqc : (new LinkedHashSet<>(eqcs.values()))) {
				if (eqc.size() == 1 && shorten) {
					Pair<N, String> p = Util.get0(eqc);
					String s = p.second;
					// if (m2.containsKey(s)) {
					s = p.first + "_" + p.second;
					// }
					m1.put(eqc, s);
					m2.put(s, eqc);
					continue;
				}
				List<String> l = new ArrayList<>(eqc.size());
				for (Pair<N, String> x : eqc) {
					l.add(x.second.toString());
				}
//				String s = Util.longestCommonPrefix(l);
				// if (s.length() < 1 || m2.containsKey(s) || !shorten) {
				// List<String> ll =
				// eqc.stream().map(this::conv2En).collect(Collectors.toList());
				String s = conv2En(doFind(eqc));
				// }
				m1.put(eqc, s);
				m2.put(s, eqc);
			}

			colX = new CCollage<>(ty.collage());

			colX.getEns().addAll(col.getEns().stream().map(this::conv1).collect(Collectors.toSet()));
			colX.atts().putAll(
					Util.map(col.atts(), (k, v) -> new Pair<>(Att.Att(conv1(col.atts().get(k).first), conv2Att(k)),
							new Pair<>(conv1(v.first), v.second))));
			colX.fks().putAll(Util.map(col.fks(), (k, v) -> new Pair<>(Fk.Fk(conv1(col.fks().get(k).first), conv2Fk(k)),
					new Pair<>(conv1(v.first), conv1(v.second)))));

			colX.eqs().addAll(col.eqs().stream().map(t -> new Eq<>(Util.map(t.ctx, (k, v) -> new Pair<>(k, conv4(v))),
					conv3(col, t.lhs), conv3(col, t.rhs))).collect(Collectors.toSet()));

			schemaStr0 = new Schema<>(ty, colX, options);

			for (N n : mappings.keySet()) {
				mappingsStr0.put(n, conv5(col, schemaStr0, n, mappings.get(n)));
			}
		}

		private Pair<N, String> doFind(Set<Pair<N, String>> eqc) {
			for (N n : nodes.keySet()) {
				for (String en : nodes.get(n).ens) {
					var x = new Pair<>(n, en);
					if (eqc.contains(x)) {
						return x;
					}
				}
			}
			return Util.anomaly();
		}

		private String conv1(Set<Pair<N, String>> eqc) {
			return (m1.get(eqc));

		}

		private String conv2En(Pair<N, String> p) {
			if (!shorten || mEn.get(new Pair<>(eqcs.get(p), p.second)).size() > 1) {
				return p.first + "_" + p.second;
			}
			return p.second;
		}

		private String conv2Fk(Pair<N, Fk> p) {
			Pair<N, String> en = new Pair<>(p.first, p.second.en);
			String s = conv2En(en);
			Pair<Set<Pair<N, String>>, String> f = new Pair<>(eqcs.get(en), p.second.str);

			if (!shorten || mFk.get(f).size() > 1) {
				return s + "_" + p.second.str;
			}
			return p.second.str;
		}

		private String conv2Att(Pair<N, Att> p) {
			Pair<N, String> en = new Pair<>(p.first, p.second.en);
			String s = conv2En(en);
			Pair<Set<Pair<N, String>>, String> f = new Pair<>(eqcs.get(en), p.second.str);

			if (!shorten || mAtt.get(f).size() > 1) {
				return s + "_" + p.second.str;
			}
			return p.second.str;
		}

		private Term<String, String, Sym, Fk, Att, Void, Void> conv3(
				Collage<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Term<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> t) {
			return t.map(Function.identity(), Function.identity(),
					x -> Fk.Fk(conv1(col.fks().get(x).first), conv2Fk(x)),
					x -> Att.Att(conv1(col.atts().get(x).first), conv2Att(x)), Function.identity(),
					Function.identity());
		}

		private Chc<String, String> conv4(Chc<String, Set<Pair<N, String>>> v) {
			if (v.left) {
				return Chc.inLeft(v.l);
			}
			return Chc.inRight(conv1(v.r));
		}

		private Mapping<String, String, Sym, Fk, Att, String, Fk, Att> conv5(
				Collage<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Schema<String, String, Sym, Fk, Att> s, N n,
				Mapping<String, String, Sym, Fk, Att, Set<Pair<N, String>>, Pair<N, Fk>, Pair<N, Att>> m) {
			Map<String, String> ens = Util.map(m.ens, (k, v) -> new Pair<>(k, conv1(v)));
			Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> atts = Util.map(m.atts,
					(k, v) -> new Pair<>(k, new Triple<>(v.first, conv1(v.second), conv3(col, v.third))));
			Map<Fk, Pair<String, List<Fk>>> fks = Util.map(m.fks, (k, v) -> new Pair<>(k, new Pair<>(conv1(v.first),
					v.second.stream().map(x -> Fk.Fk(conv1(v.first), conv2Fk(x))).collect(Collectors.toList()))));

			return new Mapping<>(ens, atts, fks, m.src, s, true);
		}
	}

	// TODO this one
	public ColimitSchema(TypeSide<String, Sym> ty, Map<N, Schema<String, String, Sym, Fk, Att>> nodes,
			Map<String, Quad<N, String, N, String>> eqEn, Collection<Quad<String, Pair<N, String>, RawTerm, RawTerm>> eqTerms,
			AqlOptions options) {
		this.ty = ty;
		this.nodes = new LinkedHashMap<>(nodes);
		options = new AqlOptions(options, AqlOption.simplify_names, false);
		Set<Pair<N, String>> ens = new LinkedHashSet<>(nodes.keySet().size());
		for (N n : nodes.keySet()) {
			Schema<String, String, Sym, Fk, Att> s = nodes.get(n);
			for (String en : s.ens) {
				ens.add(new Pair<>(n, en));
			}
		}
		for (Entry<String, Quad<N, String, N, String>> s0 : eqEn.entrySet()) {
			var s = s0.getValue();
			if (!nodes.containsKey(s.first)) {
				throw new RuntimeException("Not a schema: " + s.first);
			}
			if (!nodes.containsKey(s.third)) {
				throw new RuntimeException("Not a schema: " + s.first);
			}
			if (!nodes.get(s.first).ens.contains(s.second)) {
				throw new RuntimeException("Not an entity in " + s.first + ", " + s.second);
			}
			if (!nodes.get(s.third).ens.contains(s.fourth)) {
				throw new RuntimeException("Not an entity in " + s.third + ", " + s.fourth);
			}
		}

		BiFunction<N, String, String> enFn = (x, y) -> x + "_" + y;
		Function<Chc<String, Pair<N, Fk>>, Fk> fkFn = x -> {
			if (x.left) {
				if (!x.l.endsWith("_inv")) {
					var w = eqEn.get(x.l);
					return Fk.Fk(enFn.apply(w.first, w.second), x.l);
				}
				var w = eqEn.get(x.l.substring(0, x.l.length() - 4));
				return Fk.Fk(enFn.apply(w.third, w.fourth), x.l);
			}
			return Fk.Fk(enFn.apply(x.r.first, x.r.second.en), x.r.second.str);
		};
		BiFunction<N, Att, Att> attFn = (x, y) -> Att.Att(enFn.apply(x, y.en), y.str);

		Collage<String, String, Sym, Fk, Att, Void, Void> col = new CCollage<>(ty.collage());

		for (N n : nodes.keySet()) {
			Schema<String, String, Sym, Fk, Att> s = nodes.get(n);
			for (String en : s.ens) {
				col.getEns().add(enFn.apply(n, en));
			}
			for (Att att : s.atts.keySet()) {
				col.atts().put(attFn.apply(n, att),
						new Pair<>(enFn.apply(n, s.atts.get(att).first), s.atts.get(att).second));
			}
			for (Fk fk : s.fks.keySet()) {
				Pair<String, String> xxx = new Pair<>(enFn.apply(n, s.fks.get(fk).first),
						enFn.apply(n, s.fks.get(fk).second));
				col.fks().put(fkFn.apply(Chc.inRight(new Pair<>(n, fk))), xxx);
			}
			for (Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> eq : s.eqs) {
				Term<String, String, Sym, Fk, Att, Void, Void> lhs = eq.second.map(Function.identity(),
						Function.identity(), z -> fkFn.apply(Chc.inRight(new Pair<>(n, z))), z -> attFn.apply(n, z),
						Function.identity(), Function.identity());
				Term<String, String, Sym, Fk, Att, Void, Void> rhs = eq.third.map(Function.identity(),
						Function.identity(), z -> fkFn.apply(Chc.inRight(new Pair<>(n, z))), z -> attFn.apply(n, z),
						Function.identity(), Function.identity());
				col.eqs()
						.add(new Eq<>(
								Util.inRight(Collections.singletonMap(eq.first.first, enFn.apply(n, eq.first.second))),
								lhs, rhs));
			}
		}

		for (Entry<String, Quad<N, String, N, String>> s0 : eqEn.entrySet()) {
			var s = s0.getValue();
			var k = s0.getKey();
			var a = new Pair<>(s.first, s.second);
			var b = new Pair<>(s.third, s.fourth);
			Chc<String, Pair<N, Fk>> x = Chc.inLeft(k);
			Chc<String, Pair<N, Fk>> y = Chc.inLeft(k + "_inv");
			col.fks().put(fkFn.apply(x), new Pair<>(enFn.apply(a.first, a.second), enFn.apply(b.first, b.second)));
			col.fks().put(fkFn.apply(y), new Pair<>(enFn.apply(b.first, b.second), enFn.apply(a.first, a.second)));

			Term<String, String, Sym, Fk, Att, Void, Void> lhs = Term.Fk(fkFn.apply(y),
					Term.Fk(fkFn.apply(x), Term.Var("v")));

			col.eqs().add(new Eq<>(Util.inRight(Collections.singletonMap("v", enFn.apply(a.first, a.second))), lhs,
					Term.Var("v")));

			Term<String, String, Sym, Fk, Att, Void, Void> lhs2 = Term.Fk(fkFn.apply(x),
					Term.Fk(fkFn.apply(y), Term.Var("v")));
			col.eqs().add(new Eq<>(Util.inRight(Collections.singletonMap("v", enFn.apply(b.first, b.second))), lhs2,
					Term.Var("v")));

		}

		schemaStr = psuedoquotient(new Schema<>(ty, col, options), eqTerms, Collections.emptySet(), options);
		// schemaStr.validate(true);

		mappingsStr = new LinkedHashMap<>();

		for (N n : nodes.keySet()) {
			var m = MapExpToPrefix.toPrefix(nodes.get(n), schemaStr, n + "_");
			mappingsStr.put(n, m);
		}

		Set<Quad<N, String, N, String>> ensX = new HashSet<>();

		for (var n : eqEn.values()) {
			ensX.add(n);
		}
		ColimitSchema<N> other = new ColimitSchema<N>(nodes.keySet(), ty, this.nodes, ensX, Collections.emptySet(),
				Collections.emptySet(), options);

		Map<Att, Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>>> atts0 = new HashMap<>();
		Map<Fk, Pair<String, List<Fk>>> fks0 = new HashMap<>();
		Map<String, String> ens0 = new HashMap<>();

		for (N n : nodes.keySet()) {
			Schema<String, String, Sym, Fk, Att> s = nodes.get(n);
			for (String en : s.ens) {
				ens0.put(enFn.apply(n, en), other.mappingsStr.get(n).ens.get(en));
				for (Fk fk : s.fksFrom(en)) {
					fks0.put(fkFn.apply(Chc.inRight(new Pair<>(n, fk))), other.mappingsStr.get(n).fks.get(fk));
				}
				for (Att att : s.attsFrom(en)) {
					atts0.put(attFn.apply(n, att), other.mappingsStr.get(n).atts.get(att));
				}
			}
		}

		for (Entry<String, Quad<N, String, N, String>> s0 : eqEn.entrySet()) {
			var s = s0.getValue();
			var k = s0.getKey();
			Chc<String, Pair<N, Fk>> x = Chc.inLeft(k);
			Chc<String, Pair<N, Fk>> y = Chc.inLeft(k + "_inv");
	//		System.out.println(enFn.apply(s.first, s.second) + " and " + other.mappingsStr.get(s.first).ens.keySet());
			var l = other.mappingsStr.get(s.first).ens.get(s.second);
			var r = other.mappingsStr.get(s.third).ens.get(s.fourth);
			if (!l.equals(r)) {
				Util.anomaly();
			}

			fks0.put(fkFn.apply(x), new Pair<>(l, Collections.emptyList()));
			fks0.put(fkFn.apply(y), new Pair<>(r, Collections.emptyList()));
		}

		// psuedo->real
		var fromPsuedoNoEqs = new Mapping<>(ens0, atts0, fks0, schemaStr, other.schemaStr, true);

		Set<Quad<String, String, RawTerm, RawTerm>> eqsX = new HashSet<>();
		var eqEnTemp = new HashSet<String>(eqEn.keySet());
		for (var x : eqEn.keySet()) {
			eqEnTemp.add(x + "_inv");
		}
		
		for (var eq : schemaStr.eqs) {
		//	System.out.println("called on " + eq.second + " = " + eq.third);
			var lhs = fromPsuedoNoEqs.trans(eq.second);
			var rhs = fromPsuedoNoEqs.trans(eq.third);
		//	System.out.println("result1 " + lhs + " = " + rhs);
		//	System.out.println("result2 " +toRaw(lhs, eqEnTemp) + " = " + toRaw(rhs, eqEnTemp));

			if (!lhs.equals(rhs)) {
				eqsX.add(new Quad<>(eq.first.first, fromPsuedoNoEqs.ens.get(eq.first.second), toRaw(lhs, eqEnTemp), toRaw(rhs, eqEnTemp)));
			}
		}

		ColimitSchema<N> otherZ = new ColimitSchema<N>(nodes.keySet(), ty, this.nodes, ensX, eqsX,
				Collections.emptySet(), options);

		fromPsuedo = new Mapping<>(ens0, atts0, fks0, schemaStr, otherZ.schemaStr, true);

		// real->simpl
		var simpl = ColimSchExpSimplify.extracted(true, otherZ);

		fromPsuedo = Mapping.compose(fromPsuedo, simpl.second);
	}

	public RawTerm toRaw(Term<String,String,Sym,Fk,Att,Void,Void> term, Set<String> iso) {
		
		if (term.var != null) {
			return new catdata.cql.exp.RawTerm(term.var);
		} else if (term.obj() != null) {
			return new catdata.cql.exp.RawTerm(term.obj().toString(), term.ty());
		} else if (term.att() != null) {
			return new catdata.cql.exp.RawTerm(term.att().str, Collections.singletonList(toRaw(term.arg, iso)));
		} else if (term.fk() != null) {
			if (iso.contains(term.fk().str)) {
				return toRaw(term.arg, iso);
			}
			return new catdata.cql.exp.RawTerm(term.fk().str, Collections.singletonList(toRaw(term.arg, iso)));
		} else if (term.sym() != null) {
			return new catdata.cql.exp.RawTerm(term.sym().toString(), term.args.stream().map(x->toRaw(x, iso)).collect(Collectors.toList()));
		}
		return Util.anomaly();
	}

	public Mapping<String, String, Sym, Fk, Att, String, Fk, Att> fromPsuedo;

	public ColimitSchema(Collection<N> order, TypeSide<String, Sym> ty,
			Map<N, Schema<String, String, Sym, Fk, Att>> nodes, Set<Quad<N, String, N, String>> eqEn,
			Set<Quad<String, String, RawTerm, RawTerm>> eqTerms, Set<Pair<List<String>, List<String>>> eqTerms2,
			AqlOptions options) {
		this.ty = ty;
		this.nodes = new LinkedHashMap<>();
		for (var x : order) {
			this.nodes.put(x, nodes.get(x));
		}

		Set<Pair<N, String>> ens = new LinkedHashSet<>(nodes.keySet().size());
		for (N n : nodes.keySet()) {
			Schema<String, String, Sym, Fk, Att> s = nodes.get(n);
			for (String en : s.ens) {
				ens.add(new Pair<>(n, en));
			}
		}
		UnionFind<Pair<N, String>> uf = new UnionFind<>(ens.size(), ens);
		for (Quad<N, String, N, String> s : eqEn) {
			if (!nodes.containsKey(s.first)) {
				throw new RuntimeException("Not a schema: " + s.first);
			}
			if (!nodes.containsKey(s.third)) {
				throw new RuntimeException("Not a schema: " + s.first);
			}
			if (!nodes.get(s.first).ens.contains(s.second)) {
				throw new RuntimeException("Not an entity in " + s.first + ", " + s.second);
			}
			if (!nodes.get(s.third).ens.contains(s.fourth)) {
				throw new RuntimeException("Not an entity in " + s.third + ", " + s.fourth);
			}
			uf.union(new Pair<>(s.first, s.second), new Pair<>(s.third, s.fourth));
		}

		Collage<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col = new CCollage<>(
				ty.collage());
		Map<Pair<N, String>, Set<Pair<N, String>>> eqcs = uf.toMap();
		col.getEns().addAll(eqcs.values());

		makeCoprodSchema(col, eqcs, this.nodes);

		boolean b = (Boolean) options.getOrDefault(AqlOption.allow_java_eqs_unsafe);

		Schema<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>> schema = new Schema<>(ty, col, options);

		Pair<Schema<String, String, Sym, Fk, Att>, Map<N, Mapping<String, String, Sym, Fk, Att, String, Fk, Att>>> x = initialUser(
				order, options, col, eqcs, schema);

		Schema<String, String, Sym, Fk, Att> q = quotient(x.first, eqTerms, eqTerms2, options);

		schemaStr = q;
		mappingsStr = new LinkedHashMap<>();
		for (N n : x.second.keySet()) {
			Mapping<String, String, Sym, Fk, Att, String, Fk, Att> f = x.second.get(n);
			Mapping<String, String, Sym, Fk, Att, String, Fk, Att> g = new Mapping<>(f.ens, f.atts, f.fks, f.src, q, b);
			mappingsStr.put(n, g);
		}
	}

	private static <N> Schema<String, String, Sym, Fk, Att> psuedoquotient(Schema<String, String, Sym, Fk, Att> sch,
			Collection<Quad<String, Pair<N, String>, RawTerm, RawTerm>> eqTerms,
			Collection<Quad<N, String, List<String>, List<String>>> eqTerms2, AqlOptions options) {
		Collage<String, String, Sym, Fk, Att, Void, Void> col = new CCollage<>(sch.collage());

		for (Quad<N, String, List<String>, List<String>> t : eqTerms2) {
			List<String> a = t.third;
			String aa = t.second;
			N n = t.first;
			List<String> b = t.fourth;
			eqTerms.add(new Quad<>("_v0", new Pair<>(n, aa), RawTerm.fold(a.subList(1, a.size()), "_v0"),
					RawTerm.fold(b.subList(1, b.size()), "_v0")));
		}

		for (Quad<String, Pair<N, String>, RawTerm, RawTerm> eq : eqTerms) {
			Map<String, Chc<String, String>> Map = Collections.singletonMap(eq.first,
					Chc.inRight(eq.second.first + "_" + eq.second.second));

			Triple<Map<String, Chc<String, String>>, Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>> eq0 = RawTerm
					.infer1x(Map, eq.third, eq.fourth, null, col.convert(), "", sch.typeSide.js).first3();
			String cc = (eq.first);
			Chc<String, String> v = eq0.first.get(cc);
			if (v.left) {
				throw new RuntimeException("In " + eq.third + " = " + eq.fourth + ", variable " + eq.first
						+ " has type " + v.l + " which is not an entity");
			}

			col.eqs().add(new Eq<>(Collections.singletonMap(cc, v), eq0.second.convert(), eq0.third.convert()));
		}

		Schema<String, String, Sym, Fk, Att> ret = new Schema<>(sch.typeSide, col, options);
		return ret;
	}

	private static Schema<String, String, Sym, Fk, Att> quotient(Schema<String, String, Sym, Fk, Att> sch,
			Set<Quad<String, String, RawTerm, RawTerm>> eqTerms, Set<Pair<List<String>, List<String>>> eqTerms2,
			AqlOptions options) {
		Collage<String, String, Sym, Fk, Att, Void, Void> col = new CCollage<>(sch.collage());

		for (Pair<List<String>, List<String>> t : eqTerms2) {
			List<String> a = t.first;
			String aa = a.get(0);
			if (!sch.ens.contains((aa))) {
				throw new RuntimeException("Not an entity: " + aa + ". Paths must start with entities.  Available:\n\n"
						+ Util.sep(sch.ens, "\n"));
			}
			List<String> b = t.second;
			String bb = b.get(0);
			if (!sch.ens.contains((bb))) {
				throw new RuntimeException("Not an entity: " + bb + ". Paths must start with entities.  Available:\n\n"
						+ Util.sep(sch.ens, "\n"));
			}
			if (!aa.equals(bb)) {
				throw new RuntimeException("Not equal before renaming: " + aa + " and " + bb); // . Paths must start
				// with entities.");
			}
			eqTerms.add(new Quad<>("_v0", aa, RawTerm.fold(t.first.subList(1, t.first.size()), "_v0"),
					RawTerm.fold(t.second.subList(1, t.second.size()), "_v0")));
		}

		for (Quad<String, String, RawTerm, RawTerm> eq : eqTerms) {
			Map<String, Chc<String, String>> Map = Collections.singletonMap(eq.first,
					eq.second == null ? null : Chc.inRight((eq.second)));

			Triple<Map<String, Chc<String, String>>, Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>> eq0 = RawTerm
					.infer1x(Map, eq.third, eq.fourth, null, col.convert(), "", sch.typeSide.js).first3();
			String cc = (eq.first);
			Chc<String, String> v = eq0.first.get(cc);
			if (v.left) {
				throw new RuntimeException("In " + eq.third + " = " + eq.fourth + ", variable " + eq.first
						+ " has type " + v.l + " which is not an entity");
			}

			col.eqs().add(new Eq<>(Collections.singletonMap(cc, v), eq0.second.convert(), eq0.third.convert()));
		}

		Schema<String, String, Sym, Fk, Att> ret = new Schema<>(sch.typeSide, col, options);
		return ret;
	}

	public <E> ColimitSchema(Collection<N> order, DMG<N, E> shape, TypeSide<String, Sym> ty,
			Map<N, Schema<String, String, Sym, Fk, Att>> nodes,
			Map<E, Mapping<String, String, Sym, Fk, Att, String, Fk, Att>> edges, AqlOptions options) {
		this.ty = ty;
		this.nodes = new LinkedHashMap<>();
		for (var x : order) {
			this.nodes.put(x, nodes.get(x));
		}

		Collage<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col = new CCollage<>(
				ty.collage());

		Set<Pair<N, String>> ens = (new LinkedHashSet<>());
		for (N n : shape.nodes) {
			if (!nodes.containsKey(n)) {
				throw new RuntimeException("No schema for node " + n);
			}
			Schema<String, String, Sym, Fk, Att> s = nodes.get(n);
			for (String en : s.ens) {
				ens.add(new Pair<>(n, en));
			}
		}
		UnionFind<Pair<N, String>> uf = new UnionFind<>(ens.size(), ens);
		for (E e : shape.edges.keySet()) {
			Mapping<String, String, Sym, Fk, Att, String, Fk, Att> s = edges.get(e);
			for (String en : s.src.ens) {
				uf.union(new Pair<>(shape.edges.get(e).first, en),
						new Pair<>(shape.edges.get(e).second, s.ens.get(en)));
			}
		}

		Map<Pair<N, String>, Set<Pair<N, String>>> eqcs = uf.toMap();
		col.getEns().addAll(eqcs.values());

		makeCoprodSchema(col, eqcs, this.nodes);
		String v = ("v");
		for (E e : shape.edges.keySet()) {
			Mapping<String, String, Sym, Fk, Att, String, Fk, Att> s = edges.get(e);
			N src = shape.edges.get(e).first;
			N dst = shape.edges.get(e).second;

			for (Fk fk : s.src.fks.keySet()) {
				Pair<String, List<Fk>> fk2 = s.fks.get(fk);

				Term<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> lhs = Term
						.Fk(new Pair<>(src, fk), Term.Var(v));
				Term<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> rhs = Term.Fks(
						fk2.second.stream().map(z -> new Pair<>(dst, z)).collect(Collectors.toList()), Term.Var(v));
				col.eqs().add(new Eq<>(Util.inRight(Collections.singletonMap(v, eqcs.get(new Pair<>(dst, fk2.first)))),
						lhs, rhs));
			}
			for (Att att : s.src.atts.keySet()) {
				Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>> fk2 = s.atts.get(att);
				Term<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> lhs = Term
						.Att(new Pair<>(src, att), Term.Var(fk2.first));
				Term<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> rhs = fk2.third.map(
						Function.identity(), Function.identity(), z -> new Pair<>(dst, z), z -> new Pair<>(dst, z),
						Function.identity(), Function.identity());
				col.eqs()
						.add(new Eq<>(
								Util.inRight(
										Collections.singletonMap(fk2.first, eqcs.get(new Pair<>(dst, fk2.second)))),
								lhs, rhs));
			}
		}

		Schema<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>> schema = new Schema<>(ty, col, options);

		Pair<Schema<String, String, Sym, Fk, Att>, Map<N, Mapping<String, String, Sym, Fk, Att, String, Fk, Att>>> x = initialUser(
				order, options, col, eqcs, schema);
		schemaStr = x.first;
		mappingsStr = x.second;

	}

	private synchronized Pair<Schema<String, String, Sym, Fk, Att>, Map<N, Mapping<String, String, Sym, Fk, Att, String, Fk, Att>>> initialUser(
			Collection<N> order, AqlOptions options,
			Collage<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
			Map<Pair<N, String>, Set<Pair<N, String>>> eqcs,
			Schema<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>> schema) {
		Map<N, Mapping<String, String, Sym, Fk, Att, Set<Pair<N, String>>, Pair<N, Fk>, Pair<N, Att>>> mappings = new LinkedHashMap<>();
		String v = ("v");
		for (N n : nodes.keySet()) {
			Map<Att, Triple<String, Set<Pair<N, String>>, Term<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void>>> atts = Util
					.mk();
			Map<Fk, Pair<Set<Pair<N, String>>, List<Pair<N, Fk>>>> fks = Util.mk();
			Map<String, Set<Pair<N, String>>> ens0 = Util.mk();

			Schema<String, String, Sym, Fk, Att> s = nodes.get(n);
			for (String en : s.ens) {
				ens0.put(en, eqcs.get(new Pair<>(n, en)));
			}
			for (Fk fk : s.fks.keySet()) {
				fks.put(fk, new Pair<>(eqcs.get(new Pair<>(n, s.fks.get(fk).first)),
						Collections.singletonList(new Pair<>(n, fk))));
			}
			for (Att att : s.atts.keySet()) {

				Term<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> t = Term
						.Att(new Pair<>(n, att), Term.Var(v));
				atts.put(att, new Triple<>(v, eqcs.get(new Pair<>(n, s.atts.get(att).first)), t));
			}

			Mapping<String, String, Sym, Fk, Att, Set<Pair<N, String>>, Pair<N, Fk>, Pair<N, Att>> m = new Mapping<>(
					ens0, atts, fks, nodes.get(n), schema, true);
			mappings.put(n, m);
		}

		boolean shorten = (boolean) options.getOrDefault(AqlOption.simplify_names);
		boolean left = (boolean) options.getOrDefault(AqlOption.left_bias);
		if (left && shorten) {
			throw new RuntimeException("simplify_names and left_bias cannot both be true");
		}
		if (left) {
			Renamer2 r2 = new Renamer2(order, new THashMap<>(eqcs), col, mappings, options);
			return new Pair<>(r2.schemaStr0, r2.mappingsStr0);
		}
		Renamer r = new Renamer(new THashMap<>(eqcs), col, mappings, options);
		return new Pair<>(r.schemaStr0, r.mappingsStr0);

	}

	private static <N> void makeCoprodSchema(
			Collage<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
			Map<Pair<N, String>, Set<Pair<N, String>>> eqcs,
			LinkedHashMap<N, Schema<String, String, Sym, Fk, Att>> nodes) {
		for (N n : nodes.keySet()) {
			Schema<String, String, Sym, Fk, Att> s = nodes.get(n);
			for (Att att : s.atts.keySet()) {
				col.atts().put(new Pair<>(n, att),
						new Pair<>(eqcs.get(new Pair<>(n, s.atts.get(att).first)), s.atts.get(att).second));
			}
			for (Fk fk : s.fks.keySet()) {
				col.fks().put(new Pair<>(n, fk), new Pair<>(eqcs.get(new Pair<>(n, s.fks.get(fk).first)),
						eqcs.get(new Pair<>(n, s.fks.get(fk).second))));
			}
			for (Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> eq : s.eqs) {
				Term<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> lhs = eq.second.map(
						Function.identity(), Function.identity(), z -> new Pair<>(n, z), z -> new Pair<>(n, z),
						Function.identity(), Function.identity());
				Term<String, Set<Pair<N, String>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> rhs = eq.third.map(
						Function.identity(), Function.identity(), z -> new Pair<>(n, z), z -> new Pair<>(n, z),
						Function.identity(), Function.identity());
//        Pair<Var, Set<Pair<N, En>>> x = new Pair<>(eq.first.first, eqcs.get(new Pair<>(n, eq.first.second)));
				// eqs.add(new Triple<>(x, lhs, rhs));
				col.eqs()
						.add(new Eq<>(Util.inRight(
								Collections.singletonMap(eq.first.first, eqcs.get(new Pair<>(n, eq.first.second)))),
								lhs, rhs));
			}
		}

	}

	private static void checkIso(Mapping<String, String, Sym, Fk, Att, String, Fk, Att> F,
			Mapping<String, String, Sym, Fk, Att, String, Fk, Att> G) {
		isoOneWay(F, G, " when composing (toUser ; fromUser)");
		isoOneWay(G, F, " when composing (fromUser ; toUser)");
	}

	private static void isoOneWay(Mapping<String, String, Sym, Fk, Att, String, Fk, Att> F,
			Mapping<String, String, Sym, Fk, Att, String, Fk, Att> G, String str) {
		if (!F.dst.equals(G.src)) {
			throw new RuntimeException("Target of " + F + " \n, namely " + F.dst + "\ndoes not match source of " + G
					+ ", namely " + F.src + "\n" + str);
		}
		Mapping<String, String, Sym, Fk, Att, String, Fk, Att> f = Mapping.compose(F, G);
		for (String en : f.src.ens) {
			String en2 = f.ens.get(en);
			if (!en.equals(en2)) {
				throw new RuntimeException(en + " taken to " + en2 + ", rather than itself, " + str);
			}
		}
		String vv = ("v");
		for (Fk fk : f.src.fks.keySet()) {
			Pair<String, List<Fk>> fk2 = f.fks.get(fk);
			Term<String, String, Sym, Fk, Att, Void, Void> t = Term.Fks(fk2.second, Term.Var(vv));
			Term<String, String, Sym, Fk, Att, Void, Void> s = Term.Fk(fk, Term.Var(vv));
			boolean eq = F.src.dp.eq(Collections.singletonMap(vv, Chc.inRight(fk2.first)), s, t);
			if (!eq) {
				throw new RuntimeException(fk + " taken to " + t + ", which is not provably equal to itself, " + str);
			}
		}
		for (Att att : f.src.atts.keySet()) {
			Triple<String, String, Term<String, String, Sym, Fk, Att, Void, Void>> att2 = f.atts.get(att);
			String v = att2.first;
			Term<String, String, Sym, Fk, Att, Void, Void> t = att2.third; // Term.Fks(att2.second, Term.Var(v));
			Term<String, String, Sym, Fk, Att, Void, Void> s = Term.Att(att, Term.Var(v));
			boolean eq = F.src.dp.eq(Collections.singletonMap(v, Chc.inRight(att2.second)), s, t);
			if (!eq) {
				throw new RuntimeException(att + " taken to " + t + ", which is not provably equal to itself, " + str);
			}
		}
	}

	@Override
	public String toString() {
		return schemaStr.toString();

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
		result = prime * result + ((ty == null) ? 0 : ty.hashCode());
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
		ColimitSchema<?> other = (ColimitSchema<?>) obj;
		if (nodes == null) {
			if (other.nodes != null)
				return false;
		} else if (!nodes.equals(other.nodes))
			return false;
		if (ty == null) {
			if (other.ty != null)
				return false;
		} else if (!ty.equals(other.ty))
			return false;
		return true;
	}

	@Override
	public Kind kind() {
		return Kind.SCHEMA_COLIMIT;
	}

//  

}