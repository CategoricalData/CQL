package catdata.aql;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterators;

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

	public void validate(boolean checkJava) {
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
		for (Eq<Ty, En, Sym, Fk, Att, Object, Object> eq : collage().eqs()) {
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

	public String acyclic() {
		DAG<En> dag = new DAG<>();
		for (Fk fk : fks.keySet()) {
			boolean ok = dag.addEdge(fks.get(fk).first, fks.get(fk).second);
			if (!ok) {
				return "Adding dependency on " + fk + " causes circularity " + dag;
			}
		}
		return null;
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
		this(typeSide, col.getEns(), col.atts(), col.fks(), conv(col.eqs()),
				AqlProver.createSchema(options, col, typeSide),
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

	public final synchronized <Gen, Sk> Collage<Ty, En, Sym, Fk, Att, Gen, Sk> collage() {
		return new Collage<>() {

			@Override
			public Set<Ty> tys() {
				return typeSide.tys;
			}

			@Override
			public Map<Sym, Pair<List<Ty>, Ty>> syms() {
				return typeSide.syms;
			}

			@Override
			public Map<Ty, String> java_tys() {
				return typeSide.js.java_tys;
			}

			@Override
			public Map<Ty, String> java_parsers() {
				return typeSide.js.java_parsers;
			}

			@Override
			public Map<Sym, String> java_fns() {
				return typeSide.js.java_fns;
			}

			@Override
			public Set<En> getEns() {
				return ens;
			}

			@Override
			public Map<Att, Pair<En, Ty>> atts() {
				return atts;
			}

			@Override
			public Map<Fk, Pair<En, En>> fks() {
				return fks;
			}

			@Override
			public Map<Gen, En> gens() {
				return Collections.emptyMap();
			}

			@Override
			public Map<Sk, Ty> sks() {
				return Collections.emptyMap();
			}

			@Override
			public Collection<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> eqs() {
				Collection<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> ret = new AbstractCollection<>() {

					@Override
					public Iterator<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> iterator() {
						Iterator<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> xxx = Iterators.transform(eqs.iterator(),
								(t) -> new Eq<>(Collections.singletonMap(t.first.first, Chc.inRight(t.first.second)),
										t.second.convert(), t.third.convert()));

						Iterator<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> yyy = Iterators.transform(typeSide.eqs.iterator(),
								(t) -> new Eq<Ty, En, Sym, Fk, Att, Gen, Sk>(Util.inLeft(t.first), t.second.convert(),
										t.third.convert()));
						return Iterators.concat(xxx, yyy);
					}

					@Override
					public int size() {
						return eqs.size();
					}

				};

				return ret;
			}

		};
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
		if (atts == null) {
			if (other.atts != null)
				return false;
		} else if (!atts.equals(other.atts))
			return false;
		if (typeSide == null) {
			if (other.typeSide != null)
				return false;
		} else if (!typeSide.equals(other.typeSide))
			return false;
		if (eqs == null) {
			if (other.eqs != null)
				return false;
		} else if (!collectionEq(eqs, other.eqs)) {
			return false;
		}
		return true;
	}

	private static boolean collectionEq(Collection<?> actual, Collection<?> expected) {
		if (actual == expected) {
			return true;
		}
		if (actual == null || expected == null) {
			Util.anomaly();
		}
		if (actual.size() != expected.size()) {
			return false;
		}
		Iterator<?> actIt = actual.iterator();
		Iterator<?> expIt = expected.iterator();
		while (actIt.hasNext() && expIt.hasNext()) {
			Object e = expIt.next();
			Object a = actIt.next();
			if (!e.equals(a)) {
				return false;
			}
		}
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

	private Map<En, List<Att>> attsFrom = new LinkedHashMap<>();

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

	private Map<En, List<Fk>> fksFrom = Util.mk();
	private Map<En, List<Fk>> fksTo = Util.mk();

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

	public synchronized final Collection<Fk> fksTo(En en) {
		if (fksTo.containsKey(en)) {
			return fksTo.get(en);
		}
		List<Fk> l = new LinkedList<>();
		for (Fk fk : fks.keySet()) {
			if (fks.get(fk).second.equals(en)) {
				l.add(fk);
			}
		}
		fksTo.put(en, l);
		return l;
	}

	public static <Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Gen, Sk> fold(List<Fk> fks,
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> head) {
		for (Fk fk : fks) {
			head = Term.Fk(fk, head);
		}
		return head;
	}

	// Chc<Fk, Att>

	private final Map<Object, String> isoC1 = Util.mk();
	private final Map<String, Object> isoC2 = Util.mk();

	private int i = 0;

	private final synchronized String convert(Object e) {
		if (isoC1.containsKey(e)) {
			return isoC1.get(e);
		}
		isoC1.put(e, "s" + i);
		isoC2.put("s" + i, e);
		i++;

		return isoC1.get(e);
	}

	public synchronized String truncate(Chc<Fk, Att> x, boolean b) {
		if (!b) {
			return x.toStringMash();
		}
		return convert(x);

	}

	public synchronized String truncate(En x, boolean b) {
		if (!b) {
			return x.toString();
		}
		return convert(x);
	}

	static int constraint_static = 0;

	// (k,q,f) where q is a bunch of drops and then adds and f is the adding of
	// constraints and
	public synchronized Map<En, Triple<List<Chc<Fk, Att>>, List<String>, List<String>>> toSQL(String prefix,
			String idTy, String idCol, boolean truncate, int vlen, String tick) {
		Map<En, Triple<List<Chc<Fk, Att>>, List<String>, List<String>>> sqlSrcSchs = new LinkedHashMap<>();

		for (En en1 : ens) {
			List<String> l = new LinkedList<>();
			List<Chc<Fk, Att>> k = new LinkedList<>();
			l.add(tick + idCol + tick + " " + idTy + " primary key");
			List<String> f = new LinkedList<>();
			for (Fk fk1 : fksFrom(en1)) {
				l.add(tick + truncate(Chc.inLeft(fk1), truncate) + tick + " " + idTy + " not null ");
				k.add(Chc.inLeft(fk1));
				f.add("alter table " + tick + prefix + truncate(en1, truncate) + tick + " add constraint " + tick
						+ prefix + constraint_static++ + tick + " foreign key (" + tick
						+ truncate(Chc.inLeft(fk1), truncate) + tick + ") references " + tick + prefix
						+ truncate(fks.get(fk1).second, truncate) + tick + "(" + tick + idCol + tick + ");");
			}
			for (Att att1 : attsFrom(en1)) {
				l.add(tick + truncate(Chc.inRight(att1), truncate) + tick + " "
						+ SqlTypeSide.mediate(vlen, atts.get(att1).second.toString()));
				k.add(Chc.inRight(att1));
			}
			String str = "create table " + tick + prefix + truncate(en1, truncate) + tick + "(" + Util.sep(l, ", ")
					+ ");";
			// System.out.println("entity is " + en1 + "(" + truncate(en1,truncate) + ")");
			// System.out.println(str);
			// System.out.println(isoC1);
			List<String> q = new LinkedList<>();
			// q.add("drop table if exists " + prefix + en1 + ";");
			q.add(str);
			sqlSrcSchs.put(en1, new Triple<>(k, q, f));
		}
		return sqlSrcSchs;
	}

	int j = 0;

	public synchronized Map<En, List<String>> toSQL_srcIdxs(Pair<Collection<Fk>, Collection<Att>> indices) {
		Map<En, List<String>> sqlSrcSchsIdxs = new LinkedHashMap<>();
		for (En en1 : ens) {
			List<String> x = new LinkedList<>();
			for (Fk fk1 : fksFrom(en1)) {
				if (indices.first.contains(fk1)) {
					x.add("create index " + "idx" + j++ + " on " + convert(en1) + "(" + convert(Chc.inLeft(fk1)) + ")");
				}
			}
			for (Att att1 : attsFrom(en1)) {
				if (indices.second.contains(att1)) {
					if (!cannotBeIndexed(atts.get(att1).second)) {
						x.add("create index " + "idx" + j++ + " on " + convert(en1) + "(" + convert(Chc.inRight(att1))
								+ ")");
					}
				}
			}
			sqlSrcSchsIdxs.put(en1, x);
		}
		return sqlSrcSchsIdxs;
	}

	static int sql = 0;
	static Map<Object, String> sqlAtt = new THashMap<>();
	static Map<Object, String> sqlAtt0 = new THashMap<>();

	public static synchronized String conv(Object a) {
		String z = sqlAtt.get(a);
		if (z != null) {
			return z;
		}
		String w = "en" + sql++;
		sqlAtt.put(a, w);
		return w;
	}

	public static synchronized String conv0(Object a) {
		String z = sqlAtt0.get(a);
		if (z != null) {
			return z;
		}
		String w = "att" + att++;
		sqlAtt0.put(a, w);
		return w;
	}

	static int att = 0;

	public synchronized String toCheckerTpTp() {
		StringBuffer sb = new StringBuffer();
		if (!fks.isEmpty() || !eqs.isEmpty()) {
			Util.anomaly();
		}
		for (En en : ens) {
			sb.append("tff(" + conv(en) + "_entity, type, " + conv(en) + ": $tType).\n");
			sb.append("tff(" + conv(en) + "_pred, type, " + conv(en) + ": " + conv(en) + " > $o).\n");
		}
		for (Entry<Att, Pair<En, Ty>> k : atts.entrySet()) {
			String b = k.getValue().second.toString();
			String a = conv(k.getValue().first);
			sb.append("tff(" + conv0(k.getKey()) + "_att, type, " + conv0(k.getKey()) + ": " + a + " > " + b + ").\n");
		}
		return sb.toString();
	}

	private boolean cannotBeIndexed(Ty t) {
		String s = t.toString().toLowerCase();
		return s.equals("custom") || s.equals("text");
	}

}
