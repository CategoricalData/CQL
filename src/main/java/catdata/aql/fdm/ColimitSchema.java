package catdata.aql.fdm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.DP;
import catdata.aql.Eq;
import catdata.aql.Head;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.RawTerm;
import catdata.aql.Schema;
import catdata.aql.Semantics;
import catdata.aql.Term;
import catdata.aql.TypeSide;
import catdata.aql.Var;
import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Gen;
import catdata.aql.exp.Sk;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;
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

	public final TypeSide<Ty, Sym> ty;

	public final Map<N, Schema<Ty, En, Sym, Fk, Att>> nodes;

	// public final Schema<Ty, Set<Pair<N,En>>, Sym, Pair<N,Fk>, Pair<N,Att>>
	// schema;

	// public final Map<N,
	// Mapping<Ty,En,Sym,Fk,Att,Set<Pair<N,En>>,Pair<N,Fk>,Pair<N,Att>>> mappings;

	// actually final
	public final Schema<Ty, En, Sym, Fk, Att> schemaStr;

	// actually final
	public final Map<N, Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att>> mappingsStr;

	public ColimitSchema<N> renameEntity(En src, En dst, boolean checkJava) {
		if (!schemaStr.ens.contains(src)) {
			throw new RuntimeException(src + " is not an entity in \n" + schemaStr);
		}
		if (schemaStr.ens.contains(dst)) {
			throw new RuntimeException(dst + " is already an entity in \n" + schemaStr);
		}
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoToUser = Mapping.id(schemaStr);
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoFromUser = Mapping.id(schemaStr);

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

		Set<En> ens = (new THashSet<>(schemaStr.ens));
		ens.remove(src);
		ens.add(dst);
		Map<Att, Pair<En, Ty>> atts = (new THashMap<>());
		for (Att k : schemaStr.atts.keySet()) {
			Pair<En, Ty> v = schemaStr.atts.get(k);
			En s = v.first.equals(src) ? dst : v.first;
			atts.put(updateAtt.apply(k), new Pair<>(s, v.second));
		}
		Map<Fk, Pair<En, En>> fks = (new THashMap<>());
		for (Fk k : schemaStr.fks.keySet()) {
			Pair<En, En> v = schemaStr.fks.get(k);
			En s = v.first.equals(src) ? dst : v.first;
			En t = v.second.equals(src) ? dst : v.second;
			fks.put(updateFk.apply(k), new Pair<>(s, t));
		}
		Set<Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> eqs = new THashSet<>(
				schemaStr.eqs.size());
		for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : schemaStr.eqs) {
			Pair<Var, En> v = eq.first;
			En t = v.second.equals(src) ? dst : v.second;
			eqs.add(new Triple<>(new Pair<>(v.first, t), eq.second.mapFk(updateFk).mapAtt(updateAtt),
					eq.third.mapFk(updateFk).mapAtt(updateAtt)));
		}
		DP<Ty, En, Sym, Fk, Att, Void, Void> dp = new DP<>() {
			@Override
			public String toStringProver() {
				return "rename entity of " + schemaStr.dp.toStringProver();
			}

			@Override
			public boolean eq(Map<Var, Chc<Ty, En>> Map, Term<Ty, En, Sym, Fk, Att, Void, Void> lhs,
					Term<Ty, En, Sym, Fk, Att, Void, Void> rhs) {
				return schemaStr.dp.eq(
						Util.map(Map, (k, v) -> new Pair<>(k, v.left ? v : (v.r.equals(dst) ? Chc.inRight(src) : v))),
						lhs.mapFk(deUpdateFk).mapAtt(deUpdateAtt), rhs.mapFk(deUpdateFk).mapAtt(deUpdateAtt));
			}
		};
		Schema<Ty, En, Sym, Fk, Att> schemaStr2 = new Schema<>(ty, ens, atts, fks, eqs, dp, checkJava); // TODO aql java
		Map<En, En> ensM = (new THashMap<>(schemaStr.ens.size()));
		for (En k : schemaStr.ens) {
			ensM.put(k, k.equals(src) ? dst : k);
		}
		Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> attsM = (new THashMap<>(
				schemaStr.atts.size()));
		for (Att k : schemaStr.atts.keySet()) {
			attsM.put(k,
					new Triple<>(isoToUser.atts.get(k).first,
							isoToUser.atts.get(k).second.equals(src) ? dst : isoToUser.atts.get(k).second,
							isoToUser.atts.get(k).third.mapAtt(updateAtt).mapFk(updateFk)));
		}
		Map<Fk, Pair<En, List<Fk>>> fksM = (new THashMap<>(schemaStr.fks.size()));
		for (Fk k : schemaStr.fks.keySet()) {
			fksM.put(k, new Pair<>(isoToUser.fks.get(k).first.equals(src) ? dst : isoToUser.fks.get(k).first,
					isoToUser.fks.get(k).second.stream().map(updateFk).collect(Collectors.toList())));
		}
		isoToUser = new Mapping<>(ensM, attsM, fksM, schemaStr, schemaStr2, checkJava);
		Map<En, En> ensM2 = (new THashMap<>());
		for (En k : schemaStr2.ens) {
			ensM2.put(k, k.equals(dst) ? src : k);
		}
		Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> attsM2 = (new THashMap<>());
		for (Att k : schemaStr2.atts.keySet()) {
			attsM2.put(updateAtt.apply(k),
					new Triple<>(isoFromUser.atts.get(deUpdateAtt.apply(k)).first,
							isoFromUser.atts.get(deUpdateAtt.apply(k)).second.equals(dst) ? src
									: isoFromUser.atts.get(deUpdateAtt.apply(k)).second,
							isoFromUser.atts.get(deUpdateAtt.apply(k)).third));
		}
		Map<Fk, Pair<En, List<Fk>>> fksM2 = (new THashMap<>());
		for (Fk k : schemaStr2.fks.keySet()) {
			fksM2.put(updateFk.apply(k),
					new Pair<>(
							isoFromUser.fks.get(deUpdateFk.apply(k)).first.equals(dst) ? src
									: isoFromUser.fks.get(deUpdateFk.apply(k)).first,
							isoFromUser.fks.get(deUpdateFk.apply(k)).second.stream().map(deUpdateFk)
									.collect(Collectors.toList())));
		}
		isoFromUser = new Mapping<>(ensM2, attsM2, fksM2, schemaStr2, schemaStr, checkJava);

		return wrap(isoToUser, isoFromUser);
	}

	public ColimitSchema<N> renameFk(Fk src, Fk dst, boolean checkJava) {
		if (!schemaStr.fks.containsKey(src)) {
			throw new RuntimeException(src.en + "." + src.str + " is not a foreign_key in \n" + schemaStr);
		}
		if (schemaStr.fks.containsKey(dst)) {
			throw new RuntimeException(dst + " is already a foreign_key in \n" + schemaStr);
		}
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoToUser = Mapping.id(schemaStr);
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoFromUser = Mapping.id(schemaStr);
		Function<Fk, Fk> fun = x -> x.equals(src) ? dst : x;
		Function<Fk, Fk> fun2 = x -> x.equals(dst) ? src : x;

		Map<Fk, Pair<En, En>> fks = (new THashMap<>(schemaStr.fks.keySet().size()));
		for (Fk k : schemaStr.fks.keySet()) {
			fks.put(fun.apply(k), schemaStr.fks.get(k));
		}
		Set<Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> eqs = (new THashSet<>(
				schemaStr.eqs.size()));
		for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : schemaStr.eqs) {
			eqs.add(new Triple<>(eq.first, eq.second.mapFk(fun), eq.third.mapFk(fun)));
		}
		DP<Ty, En, Sym, Fk, Att, Void, Void> dp = new DP<>() {
			@Override
			public String toStringProver() {
				return "rename foreign key of " + schemaStr.dp.toStringProver();
			}

			@Override
			public boolean eq(Map<Var, Chc<Ty, En>> Map, Term<Ty, En, Sym, Fk, Att, Void, Void> lhs,
					Term<Ty, En, Sym, Fk, Att, Void, Void> rhs) {
				return schemaStr.dp.eq(Map, lhs.mapFk(fun2), rhs.mapFk(fun2));
			}
		};
		Schema<Ty, En, Sym, Fk, Att> schemaStr2 = new Schema<>(ty, schemaStr.ens, schemaStr.atts, fks, eqs, dp,
				checkJava); // TODO aql java
		Map<Fk, Pair<En, List<Fk>>> fksM = new THashMap<>(schemaStr.fks.size());
		for (Fk k : schemaStr.fks.keySet()) {
			fksM.put(k, new Pair<>(schemaStr.fks.get(k).first,
					k.equals(src) ? Collections.singletonList(dst) : Collections.singletonList(k)));
		}
		isoToUser = new Mapping<>(isoToUser.ens, isoToUser.atts, fksM, schemaStr, schemaStr2, checkJava);
		Map<Fk, Pair<En, List<Fk>>> fksM2 = new THashMap<>(schemaStr.fks.size());
		for (Fk k : schemaStr2.fks.keySet()) {
			fksM2.put(k, new Pair<>(schemaStr2.fks.get(k).first,
					k.equals(dst) ? Collections.singletonList(src) : Collections.singletonList(k)));
		}
		isoFromUser = new Mapping<>(isoFromUser.ens, isoFromUser.atts, fksM2, schemaStr2, schemaStr, checkJava);

		return wrap(isoToUser, isoFromUser);
	}

	public ColimitSchema<N> renameAtt(Att src, Att dst, boolean checkJava) {
		if (!schemaStr.atts.containsKey(src)) {
			throw new RuntimeException(src + " is not an attribute of " + src.en + " in \n" + schemaStr);
		}
		if (schemaStr.atts.containsKey(dst)) {
			throw new RuntimeException(dst + " is already an attribute in \n" + schemaStr);
		}
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoToUser = Mapping.id(schemaStr);
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoFromUser = Mapping.id(schemaStr);
		Function<Att, Att> fun = x -> x.equals(src) ? dst : x;
		Function<Att, Att> fun2 = x -> x.equals(dst) ? src : x;

		Map<Att, Pair<En, Ty>> atts = new THashMap<>(schemaStr.atts.size());
		for (Att k : schemaStr.atts.keySet()) {
			atts.put(fun.apply(k), schemaStr.atts.get(k));
		}
		Set<Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> eqs = (new THashSet<>());
		for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : schemaStr.eqs) {
			eqs.add(new Triple<>(eq.first, eq.second.mapAtt(fun), eq.third.mapAtt(fun)));
		}
		DP<Ty, En, Sym, Fk, Att, Void, Void> dp = new DP<>() {
			@Override
			public String toStringProver() {
				return "rename attribute of " + schemaStr.dp.toStringProver();
			}

			@Override
			public boolean eq(Map<Var, Chc<Ty, En>> Map, Term<Ty, En, Sym, Fk, Att, Void, Void> lhs,
					Term<Ty, En, Sym, Fk, Att, Void, Void> rhs) {
				return schemaStr.dp.eq(Map, lhs.mapAtt(fun2), rhs.mapAtt(fun2));
			}
		};
		Schema<Ty, En, Sym, Fk, Att> schemaStr2 = new Schema<>(ty, schemaStr.ens, atts, schemaStr.fks, eqs, dp,
				checkJava);
		Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> attsM = new THashMap<>(schemaStr.atts.size());
		for (Att k : schemaStr.atts.keySet()) {
			attsM.put(k, new Triple<>(isoToUser.atts.get(k).first, isoToUser.atts.get(k).second,
					isoToUser.atts.get(k).third.mapAtt(fun)));
		}
		isoToUser = new Mapping<>(isoToUser.ens, attsM, isoToUser.fks, schemaStr, schemaStr2, checkJava);
		Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> attsM2 = new THashMap<>(
				schemaStr2.atts.size());
		Var v = Var.Var("v");
		for (Att k : schemaStr2.atts.keySet()) {
			attsM2.put(k, new Triple<>(v, schemaStr2.atts.get(k).first, Term.Att(fun2.apply(k), Term.Var(v))));
		}
		isoFromUser = new Mapping<>(isoFromUser.ens, attsM2, isoFromUser.fks, schemaStr2, schemaStr, checkJava);

		return wrap(isoToUser, isoFromUser);
	}

	public ColimitSchema<N> removeFk(Fk src, List<Fk> l, boolean checkJava) {
		Var v = Var.Var("v");
		Term<Ty, En, Sym, Fk, Att, Void, Void> t = Term.Fks(l, Term.Var(v));
		if (!schemaStr.fks.containsKey(src)) {
			throw new RuntimeException(src + " is not a foreign_key in \n" + schemaStr);
		}
		if (l.contains(src)) {
			throw new RuntimeException(
					"Cannot replace " + src + " with " + Util.sep(l, ".") + " because that path contains " + src);
		}
		En en1 = schemaStr.fks.get(src).first;
		En en2 = schemaStr.fks.get(src).second;
		if (!schemaStr.type(new Pair<>(v, en1), t).equals(Chc.inRight(en2))) {
			throw new RuntimeException("The term " + t + " has type "
					+ schemaStr.type(new Pair<>(v, en1), t).toStringMash() + " and not " + en2 + " as expected.");
		}
		if (!schemaStr.dp.eq(Collections.singletonMap(v, Chc.inRight(en1)), t, Term.Fk(src, Term.Var(v)))) {
			throw new RuntimeException("The term " + t + " is not provably equal to " + Term.Fk(src, Term.Var(v)));
		}
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoToUser = Mapping.id(schemaStr);
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoFromUser = Mapping.id(schemaStr);

		Map<Fk, Pair<En, En>> fks = new THashMap<>(schemaStr.fks);
		fks.remove(src);
		Set<Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> eqs = new THashSet<>(
				schemaStr.eqs.size());
		for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : schemaStr.eqs) {
			Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> tr = new Triple<>(
					eq.first, eq.second.replaceHead(Head.FkHead(src), Collections.singletonList(v), t),
					eq.third.replaceHead(Head.FkHead(src), Collections.singletonList(v), t));
			if (!tr.second.equals(tr.third) && !eqs.contains(tr)) {
				eqs.add(tr);
			}
		}
		DP<Ty, En, Sym, Fk, Att, Void, Void> dp = new DP<>() {
			@Override
			public String toStringProver() {
				return "remove foreign key of " + schemaStr.dp.toStringProver();
			}

			@Override
			public boolean eq(Map<Var, Chc<Ty, En>> Map, Term<Ty, En, Sym, Fk, Att, Void, Void> lhs,
					Term<Ty, En, Sym, Fk, Att, Void, Void> rhs) {
				return schemaStr.dp.eq(Map, lhs, rhs);
			}
		};
		Schema<Ty, En, Sym, Fk, Att> schemaStr2 = new Schema<>(ty, schemaStr.ens, schemaStr.atts, fks, eqs, dp,
				checkJava);
		Map<Fk, Pair<En, List<Fk>>> fksM = new THashMap<>(isoToUser.fks);
		fksM.put(src, new Pair<>(en1, l));
		isoToUser = new Mapping<>(isoToUser.ens, isoToUser.atts, fksM, schemaStr, schemaStr2, checkJava);
		Map<Fk, Pair<En, List<Fk>>> fksM2 = new THashMap<>(isoFromUser.fks);
		fksM2.remove(src);
		isoFromUser = new Mapping<>(isoFromUser.ens, isoFromUser.atts, fksM2, schemaStr2, schemaStr, checkJava);

		return wrap(isoToUser, isoFromUser);
	}

	public ColimitSchema<N> removeAtt(Att src, Var v, Term<Ty, En, Sym, Fk, Att, Void, Void> t, boolean checkJava) {
		if (!schemaStr.atts.containsKey(src)) {
			throw new RuntimeException(src + " is not an attribute in \n" + schemaStr);
		}
		En en1 = schemaStr.atts.get(src).first;
		Ty ty0 = schemaStr.atts.get(src).second;
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
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoToUser = Mapping.id(schemaStr);
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoFromUser = Mapping.id(schemaStr);

		Map<Att, Pair<En, Ty>> atts = new THashMap<>(schemaStr.atts);
		atts.remove(src);
		Set<Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> eqs = (new THashSet<>());
		for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : schemaStr.eqs) {
			Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> tr = new Triple<>(
					eq.first, eq.second.replaceHead(Head.AttHead(src), Collections.singletonList(v), t),
					eq.third.replaceHead(Head.AttHead(src), Collections.singletonList(v), t));
			if (!tr.second.equals(tr.third) && !eqs.contains(tr)) {
				eqs.add(tr);
			}
		}
		DP<Ty, En, Sym, Fk, Att, Void, Void> dp = new DP<>() {
			@Override
			public String toStringProver() {
				return "remove attribute of " + schemaStr.dp.toStringProver();
			}

			@Override
			public boolean eq(Map<Var, Chc<Ty, En>> Map, Term<Ty, En, Sym, Fk, Att, Void, Void> lhs,
					Term<Ty, En, Sym, Fk, Att, Void, Void> rhs) {
				return schemaStr.dp.eq(Map, lhs, rhs);
			}
		};
		Schema<Ty, En, Sym, Fk, Att> schemaStr2 = new Schema<>(ty, schemaStr.ens, atts, schemaStr.fks, eqs, dp,
				checkJava);
		Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> attsM = new THashMap<>(isoToUser.atts);
		attsM.put(src, new Triple<>(v, en1, t));
		isoToUser = new Mapping<>(isoToUser.ens, attsM, isoToUser.fks, schemaStr, schemaStr2, checkJava);
		Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> attsM2 = new THashMap<>(isoFromUser.atts);
		attsM2.remove(src);
		isoFromUser = new Mapping<>(isoFromUser.ens, attsM2, isoFromUser.fks, schemaStr2, schemaStr, checkJava);

		return wrap(isoToUser, isoFromUser);
	}

	public ColimitSchema<N> wrap(Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoToUser,
			Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> isoFromUser) {
		if (!isoToUser.src.equals(schemaStr)) {
			throw new RuntimeException("Source of " + isoToUser + " \n, namely " + isoToUser.src
					+ "\ndoes not match canonical colimit, namely " + schemaStr);
		}
		checkIso(isoToUser, isoFromUser);
		Map<N, Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att>> newMapping = Util.map(mappingsStr,
				(k, v) -> new Pair<>(k, Mapping.compose(v, isoToUser)));
		return new ColimitSchema<>(ty, nodes, isoToUser.dst, newMapping);
	}

	private ColimitSchema(TypeSide<Ty, Sym> ty, Map<N, Schema<Ty, En, Sym, Fk, Att>> nodes,
			Schema<Ty, En, Sym, Fk, Att> schemaStr, Map<N, Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att>> mappingsStr) {
		this.ty = ty;
		this.nodes = nodes;
		this.schemaStr = schemaStr;
		this.mappingsStr = mappingsStr;
	}

	public class Renamer2 {

		public final Map<Set<Pair<N, En>>, String> m1 = new THashMap<>();
		public final Map<String, Set<Pair<N, En>>> m2 = new THashMap<>();

		public final Collage<Ty, En, Sym, Fk, Att, Void, Void> colX;
		public final Map<N, Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att>> mappingsStr0 = new THashMap<>();

		public final Schema<Ty, En, Sym, Fk, Att> schemaStr0;

		Map<Pair<Set<Pair<N, En>>, String>, Set<N>> mEn = new THashMap<>();
		Map<Pair<Set<Pair<N, En>>, String>, Set<N>> mFk = new THashMap<>();
		Map<Pair<Set<Pair<N, En>>, String>, Set<N>> mAtt = new THashMap<>();
		final Map<Pair<N, En>, Set<Pair<N, En>>> eqcs;
		boolean left = false;

		public Renamer2(Collection<N> order, Map<Pair<N, En>, Set<Pair<N, En>>> eqcs,
				Collage<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Map<N, Mapping<Ty, En, Sym, Fk, Att, Set<Pair<N, En>>, Pair<N, Fk>, Pair<N, Att>>> mappings,
				AqlOptions options) {
			this.eqcs = eqcs;
			for (N n : order) {
				for (En en : nodes.get(n).ens) {
					Set<Pair<N, En>> pp = eqcs.get(new Pair<>(n, en));
					Pair<Set<Pair<N, En>>, String> p = new Pair<>(pp, en.str);
					if (!mEn.containsKey(p)) {
						mEn.put(p, new THashSet<>());
					}
					mEn.get(p).add(n);

					for (Fk fk : nodes.get(n).fksFrom(en)) {
						Pair<Set<Pair<N, En>>, String> f = new Pair<>(pp, fk.str);
						if (!mFk.containsKey(f)) {
							mFk.put(f, new THashSet<>());
						}
						mFk.get(f).add(n);
					}
					for (Att att : nodes.get(n).attsFrom(en)) {
						Pair<Set<Pair<N, En>>, String> a = new Pair<>(pp, att.str);
						if (!mAtt.containsKey(a)) {
							mAtt.put(a, new THashSet<>());
						}
						mAtt.get(a).add(n);
					}
				}
			}
			colX = new Collage<>(ty.collage());
			List<N> ww = new ArrayList<>(order);
			for (Set<Pair<N, En>> eqc : new THashSet<>(eqcs.values())) {
				Pair<N, En> p = smallest(eqc, ww);
				String s = p.second.str;
				if (m2.containsKey(s)) {
					s = conv2En(p);
				}
				m1.put(eqc, s);
				m2.put(s, eqc);
			}

			colX.ens.addAll(col.ens.stream().map(this::conv1).collect(Collectors.toSet()));
			colX.atts.putAll(Util.map(col.atts, (k, v) -> new Pair<>(Att.Att(conv1(col.atts.get(k).first), conv2Att(k)),
					new Pair<>(conv1(v.first), v.second))));
			colX.fks.putAll(Util.map(col.fks, (k, v) -> new Pair<>(Fk.Fk(conv1(col.fks.get(k).first), conv2Fk(k)),
					new Pair<>(conv1(v.first), conv1(v.second)))));

			colX.eqs.addAll(col.eqs.stream().map(t -> new Eq<>(Util.map(t.ctx, (k, v) -> new Pair<>(k, conv4(v))),
					conv3(col, t.lhs), conv3(col, t.rhs))).collect(Collectors.toSet()));

			schemaStr0 = new Schema<>(ty, colX, options);

			for (N n : order) {
				mappingsStr0.put(n, conv5(col, schemaStr0, mappings.get(n)));
			}
			return;
		}

		private Pair<N, En> smallest(Set<Pair<N, En>> x, List<N> l) {
			List<Pair<N, En>> r = new ArrayList<>(x);
			r.sort((a, b) -> Integer.compare(l.indexOf(a.first), l.indexOf(b.first)));
			for (Pair<N, En> y : r) {
				return y;
			}
			return Util.anomaly();
		}

		private En conv1(Set<Pair<N, En>> eqc) {
			return En.En(m1.get(eqc));

		}

		private String conv2En(Pair<N, En> p) {
			Set<Pair<N, En>> x = eqcs.get(p);
			Pair<Set<Pair<N, En>>, String> pp = new Pair<>(x, p.second.str);

			if (mEn.get(pp).size() > 1) {
				return p.first + "_" + p.second.str;
			}
			return p.second.str;
		}

		private String conv2Fk(Pair<N, Fk> p) {
			Pair<N, En> en = new Pair<>(p.first, p.second.en);
			String s = conv2En(en);

			Pair<Set<Pair<N, En>>, String> f = new Pair<>(eqcs.get(en), p.second.str);

			if (mFk.get(f).size() > 1) {
				if (p.first.equals(Util.get0X(nodes.keySet()))) {
					return p.second.str;
				}
				return s + "_" + p.second.str;
			}
			return p.second.str;
		}

		private String conv2Att(Pair<N, Att> p) {
			Pair<N, En> en = new Pair<>(p.first, p.second.en);
			String s = conv2En(en);

			Pair<Set<Pair<N, En>>, String> f = new Pair<>(eqcs.get(en), p.second.str);

			if (mAtt.get(f).size() > 1) {
				if (p.first.equals(Util.get0X(nodes.keySet()))) {
					return p.second.str;
				}

				return s + "_" + p.second.str;
			}
			return p.second.str;
		}

		private Term<Ty, En, Sym, Fk, Att, Void, Void> conv3(
				Collage<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Term<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> t) {
			return t.map(Function.identity(), Function.identity(), x -> Fk.Fk(conv1(col.fks.get(x).first), conv2Fk(x)),
					x -> Att.Att(conv1(col.atts.get(x).first), conv2Att(x)), Function.identity(), Function.identity());
		}

		private Chc<Ty, En> conv4(Chc<Ty, Set<Pair<N, En>>> v) {
			if (v.left) {
				return Chc.inLeft(v.l);
			}
			return Chc.inRight(conv1(v.r));
		}

		private Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> conv5(
				Collage<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Schema<Ty, En, Sym, Fk, Att> s,
				Mapping<Ty, En, Sym, Fk, Att, Set<Pair<N, En>>, Pair<N, Fk>, Pair<N, Att>> m) {
			Map<En, En> ens = Util.map(m.ens, (k, v) -> new Pair<>(k, conv1(v)));
			Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> atts = Util.map(m.atts,
					(k, v) -> new Pair<>(k, new Triple<>(v.first, conv1(v.second), conv3(col, v.third))));
			Map<Fk, Pair<En, List<Fk>>> fks = Util.map(m.fks, (k, v) -> new Pair<>(k, new Pair<>(conv1(v.first),
					v.second.stream().map(x -> Fk.Fk(conv1(v.first), conv2Fk(x))).collect(Collectors.toList()))));

			return new Mapping<>(ens, atts, fks, m.src, s, false);
		}
	}

	public class Renamer {

		public final Map<Set<Pair<N, En>>, String> m1 = new THashMap<>();
		public final Map<String, Set<Pair<N, En>>> m2 = new THashMap<>();

		public final Collage<Ty, En, Sym, Fk, Att, Void, Void> colX;
		public final Map<N, Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att>> mappingsStr0 = new THashMap<>();

		public final Schema<Ty, En, Sym, Fk, Att> schemaStr0;

		// Map<Pair<N, En>, Set<Pair<N, En>>> eqcs;
		Map<Pair<Set<Pair<N, En>>, String>, Set<N>> mEn = new THashMap<>();
		Map<Pair<Set<Pair<N, En>>, String>, Set<N>> mFk = new THashMap<>();
		Map<Pair<Set<Pair<N, En>>, String>, Set<N>> mAtt = new THashMap<>();

		final boolean shorten;
		final Map<Pair<N, En>, Set<Pair<N, En>>> eqcs;

		public Renamer(Map<Pair<N, En>, Set<Pair<N, En>>> eqcs,
				Collage<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Map<N, Mapping<Ty, En, Sym, Fk, Att, Set<Pair<N, En>>, Pair<N, Fk>, Pair<N, Att>>> mappings,
				AqlOptions options) {
			this.eqcs = eqcs;
			this.shorten = (boolean) options.getOrDefault(AqlOption.simplify_names);
			for (N n : mappings.keySet()) {
				for (En en : nodes.get(n).ens) {
					Set<Pair<N, En>> pp = eqcs.get(new Pair<>(n, en));
					Pair<Set<Pair<N, En>>, String> p = new Pair<>(pp, en.str);

					if (!mEn.containsKey(p)) {
						mEn.put(p, new THashSet<>());
					}
					mEn.get(p).add(n);
					for (Fk fk : nodes.get(n).fksFrom(en)) {
						Pair<Set<Pair<N, En>>, String> f = new Pair<>(pp, fk.str);
						if (!mFk.containsKey(f)) {
							mFk.put(f, new THashSet<>());
						}
						mFk.get(f).add(n);
					}
					for (Att att : nodes.get(n).attsFrom(en)) {
						Pair<Set<Pair<N, En>>, String> f = new Pair<>(pp, att.str);
						if (!mAtt.containsKey(f)) {
							mAtt.put(f, new THashSet<>());
						}
						mAtt.get(f).add(n);
					}
				}
			}

			for (Set<Pair<N, En>> eqc : Util.alphabetical(new THashSet<>(eqcs.values()))) {
				if (eqc.size() == 1 && shorten) {
					Pair<N, En> p = Util.get0(eqc);
					String s = p.second.str;
					if (m2.containsKey(s)) {
						s = p.first + "_" + p.second.str;
					}
					m1.put(eqc, s);
					m2.put(s, eqc);
					continue;
				}
				List<String> l = new ArrayList<>(eqc.size());
				for (Pair<N, En> x : eqc) {
					l.add(x.second.toString());
				}
				String s = Util.longestCommonPrefix(l);
				if (s.length() < 1 || m2.containsKey(s) || !shorten) {
					List<String> ll = eqc.stream().map(this::conv2En).collect(Collectors.toList());
					s = Util.sep(Util.alphabetical(ll), "__");
				}
				m1.put(eqc, s);
				m2.put(s, eqc);
			}

			colX = new Collage<>(ty.collage());

			colX.ens.addAll(col.ens.stream().map(this::conv1).collect(Collectors.toSet()));
			colX.atts.putAll(Util.map(col.atts, (k, v) -> new Pair<>(Att.Att(conv1(col.atts.get(k).first), conv2Att(k)),
					new Pair<>(conv1(v.first), v.second))));
			colX.fks.putAll(Util.map(col.fks, (k, v) -> new Pair<>(Fk.Fk(conv1(col.fks.get(k).first), conv2Fk(k)),
					new Pair<>(conv1(v.first), conv1(v.second)))));

			colX.eqs.addAll(col.eqs.stream().map(t -> new Eq<>(Util.map(t.ctx, (k, v) -> new Pair<>(k, conv4(v))),
					conv3(col, t.lhs), conv3(col, t.rhs))).collect(Collectors.toSet()));

			schemaStr0 = new Schema<>(ty, colX, options);

			for (N n : mappings.keySet()) {
				mappingsStr0.put(n, conv5(col, schemaStr0, mappings.get(n)));
			}
		}

		private En conv1(Set<Pair<N, En>> eqc) {
			return En.En(m1.get(eqc));

		}

		private String conv2En(Pair<N, En> p) {
			if (!shorten || mEn.get(new Pair<>(eqcs.get(p), p.second.str)).size() > 1) {
				return p.first + "_" + p.second.str;
			}
			return p.second.str;
		}

		private String conv2Fk(Pair<N, Fk> p) {
			Pair<N, En> en = new Pair<>(p.first, p.second.en);
			String s = conv2En(en);
			Pair<Set<Pair<N, En>>, String> f = new Pair<>(eqcs.get(en), p.second.str);

			if (!shorten || mFk.get(f).size() > 1) {
				return s + "_" + p.second.str;
			}
			return p.second.str;
		}

		private String conv2Att(Pair<N, Att> p) {
			Pair<N, En> en = new Pair<>(p.first, p.second.en);
			String s = conv2En(en);
			Pair<Set<Pair<N, En>>, String> f = new Pair<>(eqcs.get(en), p.second.str);

			if (!shorten || mAtt.get(f).size() > 1) {
				return s + "_" + p.second.str;
			}
			return p.second.str;
		}

		private Term<Ty, En, Sym, Fk, Att, Void, Void> conv3(
				Collage<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Term<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> t) {
			return t.map(Function.identity(), Function.identity(), x -> Fk.Fk(conv1(col.fks.get(x).first), conv2Fk(x)),
					x -> Att.Att(conv1(col.atts.get(x).first), conv2Att(x)), Function.identity(), Function.identity());
		}

		private Chc<Ty, En> conv4(Chc<Ty, Set<Pair<N, En>>> v) {
			if (v.left) {
				return Chc.inLeft(v.l);
			}
			return Chc.inRight(conv1(v.r));
		}

		private Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> conv5(
				Collage<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
				Schema<Ty, En, Sym, Fk, Att> s,
				Mapping<Ty, En, Sym, Fk, Att, Set<Pair<N, En>>, Pair<N, Fk>, Pair<N, Att>> m) {
			Map<En, En> ens = Util.map(m.ens, (k, v) -> new Pair<>(k, conv1(v)));
			Map<Att, Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>>> atts = Util.map(m.atts,
					(k, v) -> new Pair<>(k, new Triple<>(v.first, conv1(v.second), conv3(col, v.third))));
			Map<Fk, Pair<En, List<Fk>>> fks = Util.map(m.fks, (k, v) -> new Pair<>(k, new Pair<>(conv1(v.first),
					v.second.stream().map(x -> Fk.Fk(conv1(v.first), conv2Fk(x))).collect(Collectors.toList()))));

			return new Mapping<>(ens, atts, fks, m.src, s, false);
		}
	}

	public ColimitSchema(Collection<N> order, TypeSide<Ty, Sym> ty, Map<N, Schema<Ty, En, Sym, Fk, Att>> nodes,
			Set<Quad<N, En, N, En>> eqEn, Set<Quad<String, String, RawTerm, RawTerm>> eqTerms,
			Set<Pair<List<String>, List<String>>> eqTerms2, AqlOptions options) {
		this.ty = ty;
		this.nodes = nodes;

		Set<Pair<N, En>> ens = new THashSet<>(nodes.keySet().size());
		for (N n : nodes.keySet()) {
			Schema<Ty, En, Sym, Fk, Att> s = nodes.get(n);
			for (En en : s.ens) {
				ens.add(new Pair<>(n, en));
			}
		}
		UnionFind<Pair<N, En>> uf = new UnionFind<>(ens.size(), ens);
		for (Quad<N, En, N, En> s : eqEn) {
			if (!nodes.get(s.first).ens.contains(s.second)) {
				throw new RuntimeException("Not an entity in " + s.first + ", " + s.second);
			}
			if (!nodes.get(s.third).ens.contains(s.fourth)) {
				throw new RuntimeException("Not an entity in " + s.third + ", " + s.fourth);
			}
			uf.union(new Pair<>(s.first, s.second), new Pair<>(s.third, s.fourth));
		}

		Collage<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col = new Collage<>(ty.collage());
		Map<Pair<N, En>, Set<Pair<N, En>>> eqcs = uf.toMap();
		col.ens.addAll(eqcs.values());

		makeCoprodSchema(col, eqcs);

		boolean b = !(Boolean) options.getOrDefault(AqlOption.allow_java_eqs_unsafe);

		Schema<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>> schema = new Schema<>(ty, col, options);

		Pair<Schema<Ty, En, Sym, Fk, Att>, Map<N, Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att>>> x = initialUser(order,
				options, col, eqcs, schema);

		Schema<Ty, En, Sym, Fk, Att> q = quotient(x.first, eqTerms, eqTerms2, options);

		schemaStr = q;
		mappingsStr = new THashMap<>();
		for (N n : x.second.keySet()) {
			Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> f = x.second.get(n);
			Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> g = new Mapping<>(f.ens, f.atts, f.fks, f.src, q, b);
			mappingsStr.put(n, g);
		}
	}

	private static Schema<Ty, En, Sym, Fk, Att> quotient(Schema<Ty, En, Sym, Fk, Att> sch,
			Set<Quad<String, String, RawTerm, RawTerm>> eqTerms, Set<Pair<List<String>, List<String>>> eqTerms2,
			AqlOptions options) {
		Collage<Ty, En, Sym, Fk, Att, Void, Void> col = new Collage<>(sch.collage());

		for (Pair<List<String>, List<String>> t : eqTerms2) {
			List<String> a = t.first;
			String aa = a.get(0);
			if (!sch.ens.contains(En.En(aa))) {
				throw new RuntimeException("Not an entity: " + aa + ". Paths must start with entities.  Available:\n\n"
						+ Util.sep(sch.ens, "\n"));
			}
			List<String> b = t.second;
			String bb = b.get(0);
			if (!sch.ens.contains(En.En(bb))) {
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
			Map<String, Chc<Ty, En>> Map = Collections.singletonMap(eq.first,
					eq.second == null ? null : Chc.inRight(En.En(eq.second)));

			Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq0 = RawTerm
					.infer1x(Map, eq.third, eq.fourth, null, col.convert(), "", sch.typeSide.js).first3();
			Var cc = Var.Var(eq.first);
			Chc<Ty, En> v = eq0.first.get(cc);
			if (v.left) {
				throw new RuntimeException("In " + eq.third + " = " + eq.fourth + ", variable " + eq.first
						+ " has type " + v.l + " which is not an entity");
			}

			col.eqs.add(new Eq<>(Collections.singletonMap(cc, v), eq0.second.convert(), eq0.third.convert()));
		}

		Schema<Ty, En, Sym, Fk, Att> ret = new Schema<>(sch.typeSide, col, options);
		return ret;
	}

	public <E> ColimitSchema(Collection<N> order, DMG<N, E> shape, TypeSide<Ty, Sym> ty,
			Map<N, Schema<Ty, En, Sym, Fk, Att>> nodes, Map<E, Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att>> edges,
			AqlOptions options) {
		this.ty = ty;
		this.nodes = nodes;

		Collage<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col = new Collage<>(ty.collage());

		Set<Pair<N, En>> ens = (new THashSet<>());
		for (N n : shape.nodes) {
			Schema<Ty, En, Sym, Fk, Att> s = nodes.get(n);
			for (En en : s.ens) {
				ens.add(new Pair<>(n, en));
			}
		}
		UnionFind<Pair<N, En>> uf = new UnionFind<>(ens.size(), ens);
		for (E e : shape.edges.keySet()) {
			Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> s = edges.get(e);
			for (En en : s.src.ens) {
				uf.union(new Pair<>(shape.edges.get(e).first, en),
						new Pair<>(shape.edges.get(e).second, s.ens.get(en)));
			}
		}

		Map<Pair<N, En>, Set<Pair<N, En>>> eqcs = uf.toMap();
		col.ens.addAll(eqcs.values());

		makeCoprodSchema(col, eqcs);
		Var v = Var.Var("v");
		for (E e : shape.edges.keySet()) {
			Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> s = edges.get(e);
			N src = shape.edges.get(e).first;
			N dst = shape.edges.get(e).second;

			for (Fk fk : s.src.fks.keySet()) {
				Pair<En, List<Fk>> fk2 = s.fks.get(fk);

//				Pair<Var, Set<Pair<N, En>>> x = new Pair<>(v, );
				Term<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> lhs = Term
						.Fk(new Pair<>(src, fk), Term.Var(v));
				Term<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> rhs = Term.Fks(
						fk2.second.stream().map(z -> new Pair<>(dst, z)).collect(Collectors.toList()), Term.Var(v));
				col.eqs.add(new Eq<>(Util.inRight(Collections.singletonMap(v, eqcs.get(new Pair<>(dst, fk2.first)))),
						lhs, rhs));
			}
			for (Att att : s.src.atts.keySet()) {
				Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>> fk2 = s.atts.get(att);
				// Pair<Var, Set<Pair<N, En>>> x = new Pair<>(fk2.first, eqcs.get(new
				// Pair<>(dst, fk2.second)));
				Term<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> lhs = Term
						.Att(new Pair<>(src, att), Term.Var(fk2.first));
				Term<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> rhs = fk2.third.map(
						Function.identity(), Function.identity(), z -> new Pair<>(dst, z), z -> new Pair<>(dst, z),
						Function.identity(), Function.identity());
				col.eqs.add(new Eq<>(
						Util.inRight(Collections.singletonMap(fk2.first, eqcs.get(new Pair<>(dst, fk2.second)))), lhs,
						rhs));
			}
		}

		Schema<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>> schema = new Schema<>(ty, col, options);

		Pair<Schema<Ty, En, Sym, Fk, Att>, Map<N, Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att>>> x = initialUser(order,
				options, col, eqcs, schema);
		schemaStr = x.first;
		mappingsStr = x.second;

	}

	private synchronized Pair<Schema<Ty, En, Sym, Fk, Att>, Map<N, Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att>>> initialUser(
			Collection<N> order, AqlOptions options,
			Collage<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
			Map<Pair<N, En>, Set<Pair<N, En>>> eqcs,
			Schema<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>> schema) {
		Map<N, Mapping<Ty, En, Sym, Fk, Att, Set<Pair<N, En>>, Pair<N, Fk>, Pair<N, Att>>> mappings = new THashMap<>();
		Var v = Var.Var("v");
		for (N n : nodes.keySet()) {
			Map<Att, Triple<Var, Set<Pair<N, En>>, Term<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void>>> atts = Util
					.mk();
			Map<Fk, Pair<Set<Pair<N, En>>, List<Pair<N, Fk>>>> fks = Util.mk();
			Map<En, Set<Pair<N, En>>> ens0 = Util.mk();

			Schema<Ty, En, Sym, Fk, Att> s = nodes.get(n);
			for (En en : s.ens) {
				ens0.put(en, eqcs.get(new Pair<>(n, en)));
			}
			for (Fk fk : s.fks.keySet()) {
				fks.put(fk, new Pair<>(eqcs.get(new Pair<>(n, s.fks.get(fk).first)),
						Collections.singletonList(new Pair<>(n, fk))));
			}
			for (Att att : s.atts.keySet()) {

				Term<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> t = Term.Att(new Pair<>(n, att),
						Term.Var(v));
				atts.put(att, new Triple<>(v, eqcs.get(new Pair<>(n, s.atts.get(att).first)), t));
			}

			Mapping<Ty, En, Sym, Fk, Att, Set<Pair<N, En>>, Pair<N, Fk>, Pair<N, Att>> m = new Mapping<>(ens0, atts,
					fks, nodes.get(n), schema, false);
			mappings.put(n, m);
		}

		boolean shorten = (boolean) options.getOrDefault(AqlOption.simplify_names);
		boolean left = (boolean) options.getOrDefault(AqlOption.left_bias);
		if (left && shorten) {
			throw new RuntimeException("Shorten and left bias are incompatible");
		}
		if (left) {
			Renamer2 r2 = new Renamer2(order, new THashMap<>(eqcs), col, mappings, options);
			return new Pair<>(r2.schemaStr0, r2.mappingsStr0);
		}
		Renamer r = new Renamer(new THashMap<>(eqcs), col, mappings, options);
		return new Pair<>(r.schemaStr0, r.mappingsStr0);

	}

	private void makeCoprodSchema(Collage<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> col,
			Map<Pair<N, En>, Set<Pair<N, En>>> eqcs) {
		for (N n : nodes.keySet()) {
			Schema<Ty, En, Sym, Fk, Att> s = nodes.get(n);
			for (Att att : s.atts.keySet()) {
				col.atts.put(new Pair<>(n, att),
						new Pair<>(eqcs.get(new Pair<>(n, s.atts.get(att).first)), s.atts.get(att).second));
			}
			for (Fk fk : s.fks.keySet()) {
				col.fks.put(new Pair<>(n, fk), new Pair<>(eqcs.get(new Pair<>(n, s.fks.get(fk).first)),
						eqcs.get(new Pair<>(n, s.fks.get(fk).second))));
			}
			for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : s.eqs) {
				Term<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> lhs = eq.second.map(
						Function.identity(), Function.identity(), z -> new Pair<>(n, z), z -> new Pair<>(n, z),
						Function.identity(), Function.identity());
				Term<Ty, Set<Pair<N, En>>, Sym, Pair<N, Fk>, Pair<N, Att>, Void, Void> rhs = eq.third.map(
						Function.identity(), Function.identity(), z -> new Pair<>(n, z), z -> new Pair<>(n, z),
						Function.identity(), Function.identity());
//				Pair<Var, Set<Pair<N, En>>> x = new Pair<>(eq.first.first, eqcs.get(new Pair<>(n, eq.first.second)));
				// eqs.add(new Triple<>(x, lhs, rhs));
				col.eqs.add(new Eq<>(
						Util.inRight(
								Collections.singletonMap(eq.first.first, eqcs.get(new Pair<>(n, eq.first.second)))),
						lhs, rhs));
			}
		}

	}

	private void checkIso(Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> F, Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> G) {
		isoOneWay(F, G, " when composing (toUser ; fromUser)");
		isoOneWay(G, F, " when composing (fromUser ; toUser)");
	}

	private void isoOneWay(Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> F, Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> G,
			String str) {
		if (!F.dst.equals(G.src)) {
			throw new RuntimeException("Target of " + F + " \n, namely " + F.dst + "\ndoes not match source of " + G
					+ ", namely " + F.src + "\n" + str);
		}
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> f = Mapping.compose(F, G);
		for (En en : f.src.ens) {
			En en2 = f.ens.get(en);
			if (!en.equals(en2)) {
				throw new RuntimeException(en + " taken to " + en2 + ", rather than itself, " + str);
			}
		}
		Var vv = Var.Var("v");
		for (Fk fk : f.src.fks.keySet()) {
			Pair<En, List<Fk>> fk2 = f.fks.get(fk);
			Term<Ty, En, Sym, Fk, Att, Void, Void> t = Term.Fks(fk2.second, Term.Var(vv));
			Term<Ty, En, Sym, Fk, Att, Void, Void> s = Term.Fk(fk, Term.Var(vv));
			boolean eq = F.src.dp.eq(Collections.singletonMap(vv, Chc.inRight(fk2.first)), s, t);
			if (!eq) {
				throw new RuntimeException(fk + " taken to " + t + ", which is not provably equal to itself, " + str);
			}
		}
		for (Att att : f.src.atts.keySet()) {
			Triple<Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>> att2 = f.atts.get(att);
			Var v = att2.first;
			Term<Ty, En, Sym, Fk, Att, Void, Void> t = att2.third; // Term.Fks(att2.second, Term.Var(v));
			Term<Ty, En, Sym, Fk, Att, Void, Void> s = Term.Att(att, Term.Var(v));
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