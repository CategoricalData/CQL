package catdata.aql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.graph.DAG;
import gnu.trove.map.hash.THashMap;

public final class Schema<Ty, En, Sym, Fk, Att> implements Semantics {

	@SuppressWarnings("unchecked")
	public Schema<Ty, En, Sym, Void, Void> discretize(Set<En> ensX) {
		Schema<Ty, En, Sym, Fk, Att> x = new Schema<>(typeSide, ensX, Collections.emptyMap(), Collections.emptyMap(),
				Collections.emptySet(), dp, false);
		return (Schema<Ty, En, Sym, Void, Void>) x;
	}

	@Override
	public int size() {
		return ens.size() + atts.size() + fks.size() + eqs.size();
	}

	@Override
	public Kind kind() {
		return Kind.SCHEMA;
	}

	public final TypeSide<Ty, Sym> typeSide;

	public final Set<En> ens;
	public final Map<Att, Pair<En, Ty>> atts;
	public final Map<Fk, Pair<En, En>> fks;

	public final Collection<Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> eqs;

	private void validate(boolean checkJava) {
		// check that each att/fk is in tys/ens
		for (Att att : atts.keySet()) {
			Pair<En, Ty> ty = atts.get(att);
			if (!typeSide.tys.contains(ty.second)) {
				throw new RuntimeException(
						"On attribute " + att + ", the target type " + ty.second + " is not declared.");
			} else if (!ens.contains(ty.first)) {
				throw new RuntimeException(
						"On attribute " + att + ", the source entity " + ty.first + " is not declared.");
			}
		}
		for (Fk fk : fks.keySet()) {
			Pair<En, En> ty = fks.get(fk);
			if (!ens.contains(ty.second)) {
				throw new RuntimeException(
						"On foreign key " + fk + ", the target entity " + ty.second + " is not declared.");
			} else if (!ens.contains(ty.first)) {
				throw new RuntimeException(
						"On foreign key " + fk + ", the source entity " + ty.first + " is not declared.");
			}
		}
		for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : eqs) {
			// check that the context is valid for each eq
			if (!ens.contains(eq.first.second)) {
				throw new RuntimeException("In schema equation " + toString(eq) + ", context sort " + eq.first.second
						+ " is not a declared entity.");
			}
			// check lhs and rhs types match in all eqs
			try {
				Chc<Ty, En> lhs = type(eq.first, eq.second);
				Chc<Ty, En> rhs = type(eq.first, eq.third);
				if (!lhs.equals(rhs)) {
					throw new RuntimeException(
							"lhs sort is " + lhs.toStringMash() + " but rhs sort is " + rhs.toStringMash());
				}

			} catch (Exception ex) {
				throw new RuntimeException("In schema equation " + toString(eq) + " : " + ex.getMessage());
			}

		}

		if (typeSide.js.java_tys.isEmpty()) {
			return;
		}
		for (Eq<Ty, En, Sym, Fk, Att, Object, Object> eq : collage().eqs) {
			if (checkJava) {
				Chc<Ty, En> lhs = collage().type(eq.ctx, eq.lhs);
				if (Collage.defn(eq.ctx, eq.lhs, eq.rhs) || Collage.defn(eq.ctx, eq.rhs, eq.lhs)) {
					continue;
				}
				if (lhs.left && typeSide.js.java_tys.containsKey(lhs.l)) {
					throw new RuntimeException("In schema equation " + eq.lhs + " = " + eq.rhs + ", the return type is "
							+ lhs.l
							+ " which is a java type.  \n\nPossible solution: add options allow_java_eqs_unsafe=true ");
				}
				typeSide.assertNoJava(eq.lhs);
				typeSide.assertNoJava(eq.rhs);
			}
		}

	}

	public boolean acyclic() {
		DAG<En> dag = new DAG<>();
		for (Fk fk : fks.keySet()) {
			boolean ok = dag.addEdge(fks.get(fk).first, fks.get(fk).second);
			if (!ok) {
				return false;
				// throw new RuntimeException("Adding dependency on " + fk + " causes
				// circularity " + dag);
			}
		}
		return true;
	}

	private String toString(
			Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq) {
		return "forall " + eq.first.first + ":" + eq.first.second + ", " + eq.second + " = " + eq.third;
	}

	public final Chc<Ty, En> type(Pair<Var, En> p, Term<Ty, En, Sym, Fk, Att, ?, ?> term) {
		return term.type(Collections.emptyMap(), Collections.singletonMap(p.first, p.second), typeSide.tys,
				typeSide.syms, typeSide.js.java_tys, ens, atts, fks, Collections.emptyMap(), Collections.emptyMap());
	}

	@SuppressWarnings("unchecked")
	public static <Ty, En, Sym, Fk, Att> Schema<Ty, En, Sym, Fk, Att> terminal(TypeSide<Ty, Sym> t) {
		return new Schema<>(t, Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap(),
				Collections.emptySet(), (DP<Ty, En, Sym, Fk, Att, Void, Void>) t.semantics(), false);
	}

	public Schema(TypeSide<Ty, Sym> typeSide, Collage<Ty, En, Sym, Fk, Att, Void, Void> col, AqlOptions options) {
		this(typeSide, col.ens, col.atts, col.fks, conv(col.eqs), AqlProver.createSchema(options, col, typeSide),
				!(boolean) options.getOrDefault(AqlOption.allow_java_eqs_unsafe));
	}

	private static <Ty, En, Sym, Fk, Att> Collection<Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> conv(
			Collection<Eq<Ty, En, Sym, Fk, Att, Void, Void>> eqs2) {
		Collection<Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> ret = new ArrayList<>(
				eqs2.size());
		for (Eq<Ty, En, Sym, Fk, Att, Void, Void> s : eqs2) {
			if (s.ctx.size() != 1) {
				continue;
			}
			Entry<Var, Chc<Ty, En>> yy = Util.get0(s.ctx.entrySet());
			if (yy.getValue().left) {
				continue;
			}
			ret.add(new Triple<>(conv2(yy), s.lhs, s.rhs));
		}
		return Collections.synchronizedCollection(ret);
	}

	private static <Ty, En> Pair<Var, En> conv2(Entry<Var, Chc<Ty, En>> x) {
		return new Pair<>(x.getKey(), x.getValue().r);
	}

	public Schema(TypeSide<Ty, Sym> typeSide, Set<En> ens, Map<Att, Pair<En, Ty>> atts, Map<Fk, Pair<En, En>> fks,
			Collection<Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> eqs,
			DP<Ty, En, Sym, Fk, Att, Void, Void> semantics, boolean checkJava) {
		Util.assertNotNull(semantics, typeSide, ens, fks, atts, eqs);
		this.typeSide = typeSide;
		this.atts = atts;
		this.fks = fks;
		this.eqs = eqs;
		this.ens = ens;
		dp = semantics;
		validate(checkJava);
	}

	public final DP<Ty, En, Sym, Fk, Att, Void, Void> dp;

	// this could take a while, so make sure two threads don't accidentally do
	// it at the same time
	@SuppressWarnings("unchecked")
	public <Gen, Sk> DP<Ty, En, Sym, Fk, Att, Gen, Sk> dp() {
		return (DP<Ty, En, Sym, Fk, Att, Gen, Sk>) dp;
	}

	private Collage<Ty, En, Sym, Fk, Att, Void, Void> collage;

	@SuppressWarnings("unchecked")
	public final synchronized <Gen, Sk> Collage<Ty, En, Sym, Fk, Att, Gen, Sk> collage() {
		if (collage != null) {
			if (!collage.gens.isEmpty() || !collage.sks.isEmpty()) {
				throw new RuntimeException("Anomaly: please report");
			}
			return (Collage<Ty, En, Sym, Fk, Att, Gen, Sk>) collage;
		}
		collage = new Collage<>(typeSide.collage());
		collage.ens.addAll(ens);
		collage.atts.putAll(atts);
		collage.fks.putAll(fks);
		for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> x : eqs) {
			collage.eqs.add(new Eq<>(Collections.singletonMap(x.first.first, Chc.inRight(x.first.second)),
					upgrade(x.second), upgrade(x.third)));
		}
		return (Collage<Ty, En, Sym, Fk, Att, Gen, Sk>) collage;
	}

	private <Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> upgrade(Term<Ty, En, Sym, Fk, Att, Void, Void> term) {
		return term.convert();
	}

	@Override
	public final int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((atts == null) ? 0 : atts.hashCode());
		result = prime * result + ((ens == null) ? 0 : ens.hashCode());
		result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
		result = prime * result + ((fks == null) ? 0 : fks.hashCode());
		result = prime * result + ((typeSide == null) ? 0 : typeSide.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Schema<?, ?, ?, ?, ?> other = (Schema<?, ?, ?, ?, ?>) obj;
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
		if (typeSide == null) {
			if (other.typeSide != null)
				return false;
		} else if (!typeSide.equals(other.typeSide))
			return false;
		return true;
	}

	private String toString = null;

	@Override
	public final synchronized String toString() {
		if (toString != null) {
			return toString;
		}
		List<En> ens0 = Util.alphabetical(ens);

		List<String> obsEqs = eqs.stream().filter(x -> type(x.first, x.second).left)
				.map(x -> "forall " + x.first.first + ":" + x.first.second + ". " + x.second + " = " + x.third)
				.collect(Collectors.toList());

		List<String> pathEqs = eqs.stream().filter(x -> !type(x.first, x.second).left).map(x -> {
			List<Fk> l = new LinkedList<>(), r = new LinkedList<>();
			x.second.toFkList(l);
			x.third.toFkList(r);

			return x.first.second + "." + Util.sep(l, ".") + " = " + x.first.second + "." + Util.sep(r, ".");
		}).collect(Collectors.toList());

		List<String> fks0 = new LinkedList<>();
		for (Fk fk : fks.keySet()) {
			fks0.add(fk + " : " + fks.get(fk).first + " -> " + fks.get(fk).second);
		}
		List<String> atts0 = new LinkedList<>();
		for (Att att : atts.keySet()) {
			atts0.add(att + " : " + atts.get(att).first + " -> " + atts.get(att).second);
		}
		toString = "";

		if (!ens0.isEmpty()) {
			toString += "entities";
			toString += "\n\t" + Util.sep(ens0, " ");
		}
		if (!fks0.isEmpty()) {
			toString += "\nforeign_keys";
			toString += "\n\t" + Util.sep(fks0, "\n\t");
		}
		if (!pathEqs.isEmpty()) {
			toString += "\npath_equations";
			toString += "\n\t" + Util.sep(pathEqs, "\n\t");
		}
		if (!atts0.isEmpty()) {
			toString += "\nattributes";
			toString += "\n\t" + Util.sep(atts0, "\n\t");
		}
		if (!obsEqs.isEmpty()) {
			toString += "\nobservation_equations";
			toString += "\n\t" + Util.sep(obsEqs, "\n\t");
		}
		return toString;
	}

	private Map<En, List<Att>> attsFrom = new THashMap<>();

	public synchronized final Collection<Att> attsFrom(En en) {
		if (attsFrom.containsKey(en)) {
			return attsFrom.get(en);
		}
		List<Att> l = new ArrayList<>();
		for (Att att : atts.keySet()) {
			if (atts.get(att).first.equals(en)) {
				l.add(att);
			}
		}
		attsFrom.put(en, l);
		return l;
	}

	private Map<En, List<Fk>> fksFrom = new THashMap<>();

	public synchronized final Collection<Fk> fksFrom(En en) {
		if (fksFrom.containsKey(en)) {
			return fksFrom.get(en);
		}
		List<Fk> l = new LinkedList<>();
		for (Fk fk : fks.keySet()) {
			if (fks.get(fk).first.equals(en)) {
				l.add(fk);
			}
		}
		fksFrom.put(en, l);
		return l;
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> fold(List<Fk> fks,
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> head) {
		for (Fk fk : fks) {
			head = Term.Fk(fk, head);
		}
		return head;
	}

	public static String truncate(String x, int truncate) {
		if (truncate == -1) {
			return x;
		}
		if (x.length() > truncate) {
			return x.substring(x.length() - truncate, x.length());
		}
		return x;
	}

	static int constraint_static = 0;

	// (k,q,f) where q is a bunch of drops and then adds and f is the adding of
	// constraints and
	public Map<En, Triple<List<Chc<Fk, Att>>, List<String>, List<String>>> toSQL(String prefix, String idTy,
			String idCol, int truncate, Function<Fk, String> fun, int vlen, String tick) {
		Map<En, Triple<List<Chc<Fk, Att>>, List<String>, List<String>>> sqlSrcSchs = new THashMap<>();

		for (En en1 : ens) {
			List<String> l = new LinkedList<>();
			List<Chc<Fk, Att>> k = new LinkedList<>();
			l.add(tick + idCol + tick + " " + idTy + " primary key");
			List<String> f = new LinkedList<>();
			for (Fk fk1 : fksFrom(en1)) {
				l.add(tick + truncate(fun.apply(fk1), truncate) + tick + " " + idTy + " not null ");
				k.add(Chc.inLeft(fk1));
				f.add("alter table " + tick + truncate(prefix + en1, truncate) + tick + " add constraint " + tick
						+ truncate(prefix + en1 + fk1 + constraint_static++, truncate) + tick + " foreign key (" + tick
						+ truncate(fun.apply(fk1), truncate) + tick + ") references " + tick
						+ truncate(prefix + fks.get(fk1).second, truncate) + tick + "(" + tick + idCol + tick + ");");
			}
			for (Att att1 : attsFrom(en1)) {
				l.add(tick + truncate(att1.toString(), truncate) + tick + " "
						+ SqlTypeSide.mediate(vlen, atts.get(att1).second.toString()));
				k.add(Chc.inRight(att1));
			}
			String str = "create table " + tick + prefix + en1 + tick + "(" + Util.sep(l, ", ") + ");";
			List<String> q = new LinkedList<>();
			// q.add("drop table if exists " + prefix + en1 + ";");
			q.add(str);
			sqlSrcSchs.put(en1, new Triple<>(k, q, f));
		}
		return sqlSrcSchs;
	}

	public Map<En, List<String>> toSQL_srcIdxs(Pair<Collection<Fk>, Collection<Att>> indices) {
		Map<En, List<String>> sqlSrcSchsIdxs = new THashMap<>();
		for (En en1 : ens) {
			List<String> x = new LinkedList<>();
			for (Fk fk1 : fksFrom(en1)) {
				if (indices.first.contains(fk1)) {
					x.add("create index " + en1 + fk1 + " on " + en1 + "(" + fk1 + ")");
				}
			}
			for (Att att1 : attsFrom(en1)) {
				if (indices.second.contains(att1)) {
					if (!cannotBeIndexed(atts.get(att1).second)) {
						x.add("create index " + en1 + att1 + " on " + en1 + "(" + att1 + ")");
					}
				}
			}
			sqlSrcSchsIdxs.put(en1, x);
		}
		return sqlSrcSchsIdxs;
	}

	private boolean cannotBeIndexed(Ty t) {
		String s = t.toString().toLowerCase();
		return s.equals("custom") || s.equals("text");
	}

}
