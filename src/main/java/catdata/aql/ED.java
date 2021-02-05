package catdata.aql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage.CCollage;
import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;
import catdata.aql.fdm.InitialAlgebra;
import catdata.aql.fdm.LiteralInstance;
import catdata.aql.fdm.LiteralTransform;
import catdata.provers.KBTheory;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class ED {

	private String conv(Schema<Ty, En, Sym, Fk, Att> sch, Chc<Ty, En> x) {
		return x.left ? x.l.toString() : Schema.conv(x.r);
	}

	public synchronized <X, Y> String tptp(String x, boolean preamble,
			KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, X, Y>, Var> kb) {
		StringBuffer sb = new StringBuffer();
		if (preamble) {
			sb.append(kb.tptp_preamble());
			sb.append("\n");
		}
		sb.append(this.tptp(x, KBTheory.j++, kb));
		sb.append("\n");
		String tptp = sb.toString();
		return tptp;
	}

	public <Gen, Sk> String tptpXSorted(Schema<Ty, En, Sym, Fk, Att> sch) {
		StringBuffer sb = new StringBuffer("");
		List<String> w = new LinkedList<>();
		if (!As.isEmpty()) {
			sb.append("(! [");
			sb.append(Util.sep(As, ":", ", ", x -> x.left ? x.l.toString() : Schema.conv(x.r),
					x -> Term.sqlVar((Var) x)));
			sb.append("] : ");
			for (Var v : As.keySet()) {
				if (!As.get(v).left) {
					w.add(conv(sch, As.get(v)) + "(" + Term.sqlVar(v) + ")");
				}
			}
		}
		List<String> l1 = new LinkedList<>();
		for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : Awh) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> l = eq.first.mapGenSk(Util.voidFn(), Util.voidFn());
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> r = eq.second.mapGenSk(Util.voidFn(), Util.voidFn());
			l1.add(l.toTpTpForChecker() + " = " + r.toTpTpForChecker());
		}
		sb.append("((");
		if (!Util.union(l1, w).isEmpty()) {
			sb.append("(" + Util.sep(Util.union(l1, w), " & ") + ")");
			sb.append(" => ");
		}

		List<String> u = new LinkedList<>();
		if (!Es.isEmpty()) {
			sb.append("? [");
			sb.append(Util.sep(Es, ":", ", ", x -> x.left ? x.l.toString() : Schema.conv(x.r),
					x -> Term.sqlVar((Var) x)));
			sb.append("] : ");
			for (Var v : Es.keySet()) {
				if (!Es.get(v).left) {
					u.add(conv(sch, Es.get(v)) + "(" + Term.sqlVar(v) + ")");
				}
			}
		}
		List<String> l2 = new LinkedList<>();
		for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : Ewh) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> l = eq.first.mapGenSk(Util.voidFn(), Util.voidFn());
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> r = eq.second.mapGenSk(Util.voidFn(), Util.voidFn());
			l2.add(l.toTpTpForChecker() + " = " + r.toTpTpForChecker());
		}
		if (!Util.union(l2, u).isEmpty()) {
			sb.append("(" + Util.sep(Util.union(l2, u), " & ") + ")");
		} else {
			sb.append("$true"); // TODO
		}
		sb.append(")))");

		if (isUnique) {
			Util.anomaly();
		}

		return sb.toString();
	}

	public <Gen, Sk> String tptpX(KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> th) {
		StringBuffer sb = new StringBuffer("");
		List<String> w = new LinkedList<>();
		if (!As.isEmpty()) {
			sb.append("(! [");
			sb.append(Util.sep(As.keySet().stream().map(th::convertV).collect(Collectors.toList()), ","));
			sb.append("] : ");
			for (Var v : As.keySet()) {
				w.add(th.convertT(As.get(v)) + "(" + th.convertV(v) + ")");
			}
		}
		List<String> l1 = new LinkedList<>();
		for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : Awh) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> l = eq.first.mapGenSk(Util.voidFn(), Util.voidFn());
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> r = eq.second.mapGenSk(Util.voidFn(), Util.voidFn());
			l1.add(th.convert(l.toKB()) + " = " + th.convert(r.toKB()));
		}
		sb.append("(");
		if (!Util.union(l1, w).isEmpty()) {
			sb.append("(" + Util.sep(Util.union(l1, w), " & ") + ")");
			sb.append(" => ");
		}

		List<String> u = new LinkedList<>();
		if (!Es.isEmpty()) {
			sb.append("? [");
			sb.append(Util.sep(Es.keySet().stream().map(th::convertV).collect(Collectors.toList()), ","));
			sb.append("] : ");
			for (Var v : Es.keySet()) {
				u.add(th.convertT(Es.get(v)) + "(" + th.convertV(v) + ")");
			}
		}
		List<String> l2 = new LinkedList<>();
		for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : Ewh) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> l = eq.first.mapGenSk(Util.voidFn(), Util.voidFn());
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> r = eq.second.mapGenSk(Util.voidFn(), Util.voidFn());
			l2.add(th.convert(l.toKB()) + " = " + th.convert(r.toKB()));
		}
		if (!Util.union(l2, u).isEmpty()) {
			sb.append("(" + Util.sep(Util.union(l2, u), " & ") + ")");
		} else {
			sb.append("$true"); // TODO
		}
		sb.append("))");

		if (isUnique) {
			Util.anomaly();
		}
		return sb.toString();
	}

	public <Gen, Sk> String tptp(String x, int i, KBTheory<Chc<Ty, En>, Head<Ty, En, Sym, Fk, Att, Gen, Sk>, Var> th) {
		return "fof(eq" + i + "," + x + ",(" + tptpX(th) + ")).";
	}

	public LiteralTransform<Ty, En, Sym, Fk, Att, Var, Var, Var, Var, Integer, Chc<Var, Pair<Integer, Att>>, Integer, Chc<Var, Pair<Integer, Att>>> asTransform(
			Schema<Ty, En, Sym, Fk, Att> sch) {
		LiteralInstance<Ty, En, Sym, Fk, Att, Var, Var, Integer, Chc<Var, Pair<Integer, Att>>> I = front(sch),
				J = back(sch);

		return new LiteralTransform<>((x, t) -> Term.Gen(x), (x, t) -> Term.Sk(x), I, J, true);
	}

	public LiteralInstance<Ty, En, Sym, Fk, Att, Var, Var, Integer, Chc<Var, Pair<Integer, Att>>> front(
			Schema<Ty, En, Sym, Fk, Att> sch) {
		Collage<Ty, En, Sym, Fk, Att, Var, Var> col = new CCollage<>();

		Set<Pair<Term<Ty, En, Sym, Fk, Att, Var, Var>, Term<Ty, En, Sym, Fk, Att, Var, Var>>> eqs0 = (new THashSet<>());

		for (Entry<Var, Chc<Ty, En>> p : As.entrySet()) {
			Var gen = p.getKey();
			Chc<Ty, En> ty = p.getValue();
			if (ty.left) {
				col.sks().put(gen, ty.l);
			} else {
				col.gens().put(gen, ty.r);
			}
		}
		for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq0 : Awh) {
			eqs0.add(new Pair<>(freeze(eq0.first), freeze(eq0.second)));
			col.eqs().add(new Eq<>(null, freeze(eq0.first), freeze(eq0.second)));
		}

		InitialAlgebra<Ty, En, Sym, Fk, Att, Var, Var> initial = new InitialAlgebra<>(options, sch, col, (y) -> y,
				(x, y) -> y);
//col.validate();
		LiteralInstance<Ty, En, Sym, Fk, Att, Var, Var, Integer, Chc<Var, Pair<Integer, Att>>> x = new LiteralInstance<>(
				sch, col.gens(), col.sks(), eqs0, initial.dp(), initial,
				(Boolean) options.getOrDefault(AqlOption.require_consistency),
				(Boolean) options.getOrDefault(AqlOption.allow_java_eqs_unsafe));

		x.validate();
		// System.out.println("front " + x);
		return x;
	}

	public LiteralInstance<Ty, En, Sym, Fk, Att, Var, Var, Integer, Chc<Var, Pair<Integer, Att>>> back(
			Schema<Ty, En, Sym, Fk, Att> sch) {
		Collage<Ty, En, Sym, Fk, Att, Var, Var> col = new CCollage<>();

		Set<Pair<Term<Ty, En, Sym, Fk, Att, Var, Var>, Term<Ty, En, Sym, Fk, Att, Var, Var>>> eqs0 = (new THashSet<>());

		for (Entry<Var, Chc<Ty, En>> p : As.entrySet()) {
			Var gen = p.getKey();
			Chc<Ty, En> ty = p.getValue();
			if (ty.left) {
				col.sks().put(gen, ty.l);
			} else {
				col.gens().put(gen, ty.r);
			}
		}
		for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq0 : Awh) {
			eqs0.add(new Pair<>(freeze(eq0.first), freeze(eq0.second)));
			col.eqs().add(new Eq<>(null, freeze(eq0.first), freeze(eq0.second)));
		}
		for (Entry<Var, Chc<Ty, En>> p : Es.entrySet()) {
			Var gen = p.getKey();
			if (p.getValue().left) {
				Ty ty = p.getValue().l;
				col.sks().put(gen, ty);
			} else {
				En ty = p.getValue().r;
				col.gens().put(gen, ty);
			}
		}

		for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq0 : Ewh) {
			eqs0.add(new Pair<>(freeze(eq0.first), freeze(eq0.second)));
			col.eqs().add(new Eq<>(null, freeze(eq0.first), freeze(eq0.second)));
		}
		InitialAlgebra<Ty, En, Sym, Fk, Att, Var, Var> initial = new InitialAlgebra<>(options, sch, col, (y) -> y,
				(x, y) -> y);

		// System.out.println("===== " + sch + " \n **** " + col + "&&&&&");
		// col.validate();
		LiteralInstance<Ty, En, Sym, Fk, Att, Var, Var, Integer, Chc<Var, Pair<Integer, Att>>> x = new LiteralInstance<>(
				sch, col.gens(), col.sks(), eqs0, initial.dp(), initial,
				(Boolean) options.getOrDefault(AqlOption.require_consistency),
				(Boolean) options.getOrDefault(AqlOption.allow_java_eqs_unsafe));

		// System.out.println("back " + x);
		x.validate();
		return x;
	}

	public final static En FRONT = En.En("front"), BACK = En.En("back");
	public final static Fk UNIT = Fk.Fk(BACK, "unit");

	public final boolean isUnique;

	public static <Ty, Sym> Schema<Ty, En, Sym, Fk, Att> getEDSchema(TypeSide<Ty, Sym> ty, AqlOptions ops) {
		Collage<Ty, En, Sym, Fk, Att, Void, Void> col = new CCollage<>();
		col.getEns().add(FRONT);
		col.getEns().add(BACK);
		col.fks().put(UNIT, new Pair<>(BACK, FRONT));
		Schema<Ty, En, Sym, Fk, Att> ret = new Schema<>(ty, col, ops);
		return ret;
	}

	private Map<Schema<Ty, En, Sym, Fk, Att>, Query<Ty, En, Sym, Fk, Att, En, Fk, Att>> cache = new THashMap<>();

	public synchronized final Query<Ty, En, Sym, Fk, Att, En, Fk, Att> getQ(Schema<Ty, En, Sym, Fk, Att> schema) {
		if (!cache.containsKey(schema)) {
			Schema<Ty, En, Sym, Fk, Att> zzz = getEDSchema(schema.typeSide, options);

			Map<En, Triple<Map<Var, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>>, AqlOptions>> is2 = Util
					.map(is, (k, x) -> new Pair<>(k, new Triple<>(
							Util.map(x.first, (kk, z) -> new Pair<>(kk, z.reverse())), x.second, x.third)));

			cache.put(schema, Query.makeQuery(is2, new THashMap<>(), fks, sks, schema, zzz, options));
		}
		return cache.get(schema);
	}

	@Override
	public String toString() {

		String toString = "";

		if (!As.isEmpty()) {
			toString += "\tforall";
			List<String> temp = new LinkedList<>();
			for (Entry<Var, Chc<Ty, En>> p : As.entrySet()) {
				temp.add(p.getKey() + ":" + p.getValue().toStringMash());
			}

			toString += "\n\t\t" + Util.sep(temp, "\n\t\t") + "\n";
		}
		if (!Awh.isEmpty()) {
			toString += "\twhere";
			List<String> temp = new LinkedList<>();
			for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> p : Awh) {
				temp.add(p.first + " = " + p.second);
			}

			toString += "\n\t\t" + Util.sep(temp, "\n\t\t") + "\n";
		}
		toString += "->\n";
		if (!Es.isEmpty()) {
			toString += "\texists";
			if (isUnique) {
				toString += " unique";
			}
			List<String> temp = new LinkedList<>();
			for (Entry<Var, Chc<Ty, En>> p : Es.entrySet()) {
				temp.add(p.getKey() + ":" + p.getValue().toStringMash());
			}

			toString += "\n\t\t" + Util.sep(temp, "\n\t\t") + "\n";
		}
		if (!Ewh.isEmpty()) {
			toString += "\twhere";
			List<String> temp = new LinkedList<>();
			for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> p : Ewh) {
				temp.add(p.first + " = " + p.second);
			}

			toString += "\n\t\t" + Util.sep(temp, "\n\t\t") + "\n";
		}
		return toString;

	}

//	public final Schema<Ty, En, Sym, Fk, Att> schema;

	public final Map<Var, Chc<Ty, En>> As;

	public final Map<Var, Chc<Ty, En>> Es;

	public Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> Awh;

	public Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> Ewh;

	private final Term<Ty, En, Sym, Fk, Att, Var, Var> freeze(Term<Ty, En, Sym, Fk, Att, Void, Void> t) {
		Term<Ty, En, Sym, Fk, Att, Var, Var> ret = t.mapGenSk(Util.voidFn(), Util.voidFn());
		Map<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>> m = (new THashMap<>());
		for (Var v : As.keySet()) {
			if (As.get(v).left) {
				m.put(v, Term.Sk(v));
			} else {
				m.put(v, Term.Gen(v));
			}
		}
		for (Var v : Es.keySet()) {
			if (Es.get(v).left) {
				m.put(v, Term.Sk(v));
			} else {
				m.put(v, Term.Gen(v));
			}
		}
		return ret.subst(m);
	}

	private final Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>> freeze(
			Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> eqs) {
		Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>> ret = (new ArrayList<>(eqs.size()));
		for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : eqs) {
			ret.add(new Eq<>(null, freeze(eq.first), freeze(eq.second)));
		}
		return ret;
	}

	Map<En, Triple<Map<Var, Chc<Ty, En>>, Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>>, AqlOptions>> is = new THashMap<>();

	Map<Fk, Pair<Map<Var, Term<Void, En, Void, Fk, Void, Var, Void>>, AqlOptions>> fks = new THashMap<>();
	Map<Fk, Map<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>>> sks = new THashMap<>();

	<Ty, En, Sym, Fk, Att, Gen, Sk> Term<Ty, En, Sym, Fk, Att, Void, Void> unfreeze(String prefix,
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> r) {
		if (r.var != null) {
			return Util.anomaly();
		} else if (r.gen() != null) {
			return Term.Var(Var.Var(prefix + r.gen()));
		} else if (r.sk() != null) {
			return Term.Var(Var.Var(prefix + r.sk()));
		} else if (r.fk() != null) {
			return Term.Fk(r.fk(), unfreeze(prefix, r.arg));
		} else if (r.att() != null) {
			return Term.Att(r.att(), unfreeze(prefix, r.arg));
		} else if (r.sym() != null) {
			List<Term<Ty, En, Sym, Fk, Att, Void, Void>> l = new ArrayList<>(r.args.size());
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> x : r.args) {
				l.add(unfreeze(prefix, x));
			}
			return Term.Sym(r.sym(), l);
		} else if (r.obj() != null) {
			return r.convert();
		}
		return Util.anomaly();
	};

	public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> ED(AqlOptions options,
			Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h) {
		// this.schema = schema;
		As = new THashMap<>();
		Es = new THashMap<>();
		Awh = new THashSet<>();
		Ewh = new THashSet<>();
		// System.out.println("In ED constructor " + h.src() + "\n\n" + h.dst());

		h.src().gens().entrySet((gen1, t) -> {
			As.put(Var.Var("A" + gen1), Chc.inRight(h.src().gens().get(gen1)));
			Term<Ty, En, Sym, Fk, Att, Void, Void> l = unfreeze("A", Term.Gen(gen1));
			Term<Ty, En, Sym, Fk, Att, Void, Void> r = unfreeze("E", h.gens().apply(gen1, t).convert());
			Ewh.add(new Pair<>(l, r));
		});
		h.src().sks().entrySet((sk1, t) -> {
			As.put(Var.Var("A" + sk1), Chc.inLeft(h.src().sks().get(sk1)));
			Term<Ty, En, Sym, Fk, Att, Void, Void> l = unfreeze("A", Term.Sk(sk1));
			Term<Ty, En, Sym, Fk, Att, Void, Void> r = unfreeze("E", h.sks().apply(sk1, t));
			Ewh.add(new Pair<>(l, r));
		});
		h.dst().gens().entrySet((gen2, x) -> {
			Es.put(Var.Var("E" + gen2), Chc.inRight(x));
		});
		h.dst().sks().entrySet((sk2, x) -> {
			Es.put(Var.Var("E" + sk2), Chc.inLeft(h.dst().sks().get(sk2)));
		});
		// System.out.println(h);
		// System.out.println(h.src());
		// System.out.println(h.dst());

		h.src().eqs((a, b) -> {
			Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> z = new Pair<>(
					unfreeze("A", a), unfreeze("A", b));
			Awh.add(z);
		});
		h.dst().eqs((a, b) -> {
			Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> z = new Pair<>(
					unfreeze("E", a), unfreeze("E", b));
			Ewh.add(z);
		});

		this.isUnique = false;
		if (!Collections.disjoint(As.keySet(), Es.keySet())) {
			throw new RuntimeException("The forall and exists clauses do not use disjoint variables.");
		}

		for (;;) {
			Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> z2 = simplify(Es,
					Ewh);
			if (z2 == null) {
				break;
			}
			Ewh = z2;
		}
		for (;;) {
			Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> z2 = simplify2(Es,
					Ewh);
			if (z2 == null) {
				break;
			}
			Ewh = z2;
		}
		/**
		 * for (;;) { vars must be removed from target as well Set<Pair<Term<Ty, En,
		 * Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> z2 =
		 * simplify(As, Awh); if (z2 == null) { break; } Awh = z2; }
		 */
		Iterator<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> it = Ewh
				.iterator();

		while (it.hasNext()) {
			Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> j = it.next();
			if (j.first.equals(j.second) || Awh.contains(j) || Awh.contains(new Pair<>(j.second, j.first))) {
				it.remove();
			}

		}

		Iterator<Entry<Var, Chc<Ty, En>>> itt = As.entrySet().iterator();
		while (itt.hasNext()) {
			Entry<Var, Chc<Ty, En>> e = itt.next();
			for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : Awh) {
				if (eq.first.isVar() && eq.first.var.equals(e.getKey())) {
					itt.remove();
					Awh = subst(eq.first.var, eq.second, Awh);
					Ewh = subst(eq.first.var, eq.second, Ewh);
				} else if (eq.second.isVar() && eq.second.var.equals(e.getKey())) {
					itt.remove();
					Awh = subst(eq.second.var, eq.first, Awh);
					Ewh = subst(eq.second.var, eq.first, Ewh);
				}
			}
		}

		it = Ewh.iterator();
		while (it.hasNext()) {
			Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> j = it.next();
			if (j.first.att() != null && j.second.att() != null && j.first.att().equals(j.second.att())) {
				for (Pair<Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Void, Void>, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Void, Void>> eq2 : Ewh) {
					if (eq2.first.equals(j.first.arg) && eq2.second.equals(j.second.arg)) {
						it.remove();
						break;
					}
					if (eq2.second.equals(j.first.arg) && eq2.first.equals(j.second.arg)) {
						it.remove();
						break;
					}
				}
			}
		}
		it = Awh.iterator();
		while (it.hasNext()) {
			Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> j = it.next();
			if (j.first.att() != null && j.second.att() != null && j.first.att().equals(j.second.att())) {
				for (Pair<Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Void, Void>, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Void, Void>> eq2 : Awh) {
					if (eq2.first.equals(j.first.arg) && eq2.second.equals(j.second.arg)) {
						it.remove();
						break;
					}
					if (eq2.second.equals(j.first.arg) && eq2.first.equals(j.second.arg)) {
						it.remove();
						break;
					}
				}
			}
		}

		is.put(FRONT, new Triple<>(As, freeze(Awh), options));
		Map<Var, Chc<Ty, En>> AsEs = new THashMap<>();
		AsEs.putAll(As);
		AsEs.putAll(Es);

		is.put(BACK, new Triple<>(AsEs, freeze(Util.union(Awh, Ewh)), options));

		Map<Var, Term<Void, En, Void, Fk, Void, Var, Void>> Map1 = new THashMap<>();
		Map<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>> Map2 = new THashMap<>();

		for (Var v : As.keySet()) {
			if (As.get(v).left) {
				Map2.put(v, Term.Sk(v));
			} else {
				Map1.put(v, Term.Gen(v));
			}
		}
		fks.put(ED.UNIT, new Pair<>(Map1, options));
		sks.put(ED.UNIT, Map2);

		this.options = options;

		// asTransform(h.src().schema());
	}

	private Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> subst(Var var,
			Term<Ty, En, Sym, Fk, Att, Void, Void> t,
			Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> wh) {

		Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> ret = new THashSet<>(
				wh.size());
		Map<Var, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Void, Void>> m = Collections
				.singletonMap(var, t);
		for (Pair<Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Void, Void>, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Void, Void>> eq : wh) {
			Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Void, Void> a = eq.first
					.subst(m);
			Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Void, Void> b = eq.second
					.subst(m);
			if (!a.equals(b)) {
				ret.add(new Pair<>(a, b));
			}
		}
		return ret;

	}

	private Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> simplify2(
			Map<Var, Chc<Ty, En>> AsEs,
			Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> in) {
		Iterator<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> it = in
				.iterator();
		while (it.hasNext()) {
			Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq = it.next();
			if (eq.second.var != null && !eq.first.getVars().contains(eq.second.var)
					&& AsEs.containsKey(eq.second.var)) {
				// System.out.println("HIT " + eq.second.var);
				it.remove();
				AsEs.remove(eq.second.var);
				Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> set = new THashSet<>();
				Map<Var, Term<Ty, En, Sym, Fk, Att, Void, Void>> m = Collections.singletonMap(eq.second.var, eq.first);

				for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq2 : in) {
					set.add(new Pair<>(eq2.first.subst(m), eq2.second.subst(m)));
				}
				return set;
			} else if (eq.first.var != null && !eq.second.getVars().contains(eq.first.var)
					&& AsEs.containsKey(eq.first.var)) {
				// System.out.println("HIT " + eq.second.var);
				it.remove();
				AsEs.remove(eq.first.var);
				Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> set = new THashSet<>();
				Map<Var, Term<Ty, En, Sym, Fk, Att, Void, Void>> m = Collections.singletonMap(eq.first.var, eq.second);

				for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq2 : in) {
					set.add(new Pair<>(eq2.first.subst(m), eq2.second.subst(m)));
				}
				return set;
			}
		}
		// TODO: reverse of this
		return null;
	}

	private Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> simplify(
			Map<Var, Chc<Ty, En>> AsEs,
			Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> in) {
		Iterator<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> it = in
				.iterator();
		outer: while (it.hasNext()) {
			Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq = it.next();
			if (eq.second.var != null && Es.containsKey(eq.second.var)) {
				for (Var v : eq.first.vars()) {
					if (Es.containsKey(v)) {
						continue outer;
					}
				}

				// System.out.println("HIT " + eq.second.var);
				it.remove();
				// System.out.println("before " + AsEs.size());
				Chc<Ty, En> b = AsEs.remove(eq.second.var);
				// System.out.println("after " + AsEs.size());
				if (b == null) {
					Util.anomaly();
				}
				Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> set = new THashSet<>();
				for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq2 : in) {
					Map<Var, Term<Ty, En, Sym, Fk, Att, Void, Void>> m = Collections.singletonMap(eq.second.var,
							eq.first);
					set.add(new Pair<>(eq2.first.subst(m), eq2.second.subst(m)));
				}
				return set;
			}
			if (eq.first.var != null && Es.containsKey(eq.first.var)) {
				for (Var v : eq.second.vars()) {
					if (Es.containsKey(v)) {
						continue outer;
					}
				}

				// System.out.println("HIT " + eq.second.var);
				it.remove();
				// System.out.println("before " + AsEs.size());
				Chc<Ty, En> b = AsEs.remove(eq.first.var);
				// System.out.println("after " + AsEs.size());
				if (b == null) {
					Util.anomaly();
				}
				Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> set = new THashSet<>();
				for (Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq2 : in) {
					Map<Var, Term<Ty, En, Sym, Fk, Att, Void, Void>> m = Collections.singletonMap(eq.first.var,
							eq.second);
					set.add(new Pair<>(eq2.first.subst(m), eq2.second.subst(m)));
				}
				return set;
			}
		}
		// TODO: reverse of this
		return null;
	}

	public void validate(Schema<Ty, En, Sym, Fk, Att> sch) {
		for (Pair<Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Void, Void>, Term<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Void, Void>> x : Awh) {

			Map<Var, catdata.aql.exp.Ty> m = new THashMap<>();
			Map<Var, catdata.aql.exp.En> n = new THashMap<>();
			for (Entry<Var, Chc<catdata.aql.exp.Ty, catdata.aql.exp.En>> v : As.entrySet()) {
				if (v.getValue().left) {
					m.put(v.getKey(), v.getValue().l);
				} else {
					n.put(v.getKey(), v.getValue().r);
				}
			}
			x.first.type(m, n, sch.typeSide.tys, sch.typeSide.syms, sch.typeSide.js.java_tys, sch.ens, sch.atts,
					sch.fks, null, null);

		}
	}

	public ED(/* Schema<Ty, En, Sym, Fk, Att> schema, */ Map<Var, Chc<Ty, En>> as, Map<Var, Chc<Ty, En>> es,
			Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> awh,
			Set<Pair<Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>>> ewh,
			boolean isUnique, AqlOptions options) {
		// this.schema = schema;
		As = new THashMap<>(as);
		Es = new THashMap<>(es);
		Awh = new THashSet<>(awh);
		Ewh = new THashSet<>(ewh);
		this.isUnique = isUnique;
		if (isUnique && Es.isEmpty()) {
			Util.anomaly();
		}
		if (!Collections.disjoint(As.keySet(), Es.keySet())) {
			throw new RuntimeException("The forall and exists clauses do not use disjoint variables.");
		}

		is.put(FRONT, new Triple<>(As, freeze(Awh), options));
		Map<Var, Chc<Ty, En>> AsEs = new THashMap<>();
		AsEs.putAll(As);
		AsEs.putAll(Es);
		is.put(BACK, new Triple<>(AsEs, freeze(Util.union(Awh, Ewh)), options));

		Map<Var, Term<Void, En, Void, Fk, Void, Var, Void>> Map1 = new THashMap<>(As.size());
		Map<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>> Map2 = new THashMap<>();

		for (Var v : As.keySet()) {
			if (As.get(v).left) {
				Map2.put(v, Term.Sk(v));
			} else {
				Map1.put(v, Term.Gen(v));
			}
		}
		fks.put(UNIT, new Pair<>(Map1, options));
		sks.put(UNIT, Map2);

		this.options = options;
	}

	AqlOptions options;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((As == null) ? 0 : As.hashCode());
		result = prime * result + ((Awh == null) ? 0 : Awh.hashCode());
		result = prime * result + ((Es == null) ? 0 : Es.hashCode());
		result = prime * result + ((Ewh == null) ? 0 : Ewh.hashCode());
		result = prime * result + (isUnique ? 1231 : 1237);
		// result = prime * result + ((schema == null) ? 0 : schema.hashCode());
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
		ED other = (ED) obj;
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

}
