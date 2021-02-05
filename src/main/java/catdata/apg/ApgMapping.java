package catdata.apg;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.Kind;
import catdata.aql.Semantics;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class ApgMapping<L1, L2> implements Semantics {

	public final ApgSchema<L1> src;
	public final ApgSchema<L2> dst;

	public final Map<L1, Triple<Var, ApgTy<L2>, ApgTerm<L2, Void>>> mapping;

	public <E1, E2> ApgTransform<L1, Pair<L1, ApgTerm<L2, E1>>, L1, Pair<L1, ApgTerm<L2, E2>>> deltaT(
			ApgTransform<L2, E1, L2, E2> h) {
		for (Entry<L2, L2> x : h.lMap.entrySet()) {
			if (!x.getKey().equals(x.getValue())) {
				throw new RuntimeException("Transform not identity on labels: " + x.getKey() + " -> " + x.getValue());
			}
		}
		h.assertNaturalData();

		Map<Pair<L1, ApgTerm<L2, E1>>, Pair<L1, ApgTerm<L2, E2>>> es = new THashMap<>();

		ApgInstance<L1, Pair<L1, ApgTerm<L2, E1>>> src0 = delta(h.src);
		ApgInstance<L1, Pair<L1, ApgTerm<L2, E2>>> dst0 = delta(h.dst);

		for (Pair<L1, ApgTerm<L2, E1>> x : src0.Es.keySet()) {
			ApgTerm<L2, E2> e = x.second.map(u -> h.eMap.get(u));

			es.put(x, new Pair<>(x.first, e));
		}
		Map<L1, L1> idmap = Util.id(src.schema.keySet());
		return new ApgTransform<>(src0, dst0, idmap, es);
	}

	public synchronized <E> ApgInstance<L1, Pair<L1, ApgTerm<L2, E>>> delta(ApgInstance<L2, E> G) {
		if (!G.Ls.equals(dst)) {
			Util.anomaly();
		}
		Map<Pair<L1, ApgTerm<L2, E>>, Pair<L1, ApgTerm<L1, Pair<L1, ApgTerm<L2, E>>>>> es = new THashMap<>();
		// System.out.println("&&&& " + src);
		for (Entry<L1, Triple<Var, ApgTy<L2>, ApgTerm<L2, Void>>> x : mapping.entrySet()) {
			Triple<Var, ApgTy<L2>, ApgTerm<L2, Void>> t = x.getValue();
			L1 l1 = x.getKey();
			ApgTy<L2> l2 = t.second;
			Var v = t.first;
			ApgTerm<L2, Void> w = t.third;
			ApgTerm<L2, E> term = w.map(Util::abort);

			for (ApgTerm<L2, E> e : G.elemsFor(l2)) {
				// System.out.println("e " + e);

				ApgTerm<L2, E> z = term.subst(v, e.convert());
				// System.out.println("z " + z);
				ApgTerm<L1, E> jj = eval(G, z);
				// System.out.println("jj " + jj);

				ApgTerm<L1, Pair<L1, ApgTerm<L2, E>>> a = jj
						.map((E h) -> new Pair<L1, ApgTerm<L2, E>>(l1, ApgTerm.ApgTermE(h)));

				// System.out.println("a " + a);

				es.put(new Pair<>(l1, e), new Pair<>(l1, a));
			}
		}
//		System.out.println("es " + Util.sep(es, " -> ", " \n "));

		var w = new ApgInstance<>(src, es);
		return w;
	}

	private static <L1, L2, E> ApgTerm<L1, E> eval(ApgInstance<L2, E> g, ApgTerm<L2, E> t) {
		for (;;) {
			ApgTerm<L2, E> t0 = deref(g, t);
			if (t.equals(t0)) {
				return t.convert();
			}
			t = t0;
		}
	}

	private static <L2, E> ApgTerm<L2, E> deref(ApgInstance<L2, E> g, ApgTerm<L2, E> t) {
		if (t.proj != null && t.a.fields != null) {
			return deref(g, t.a.fields.get(t.proj));
		} else if (t.cases != null && t.a.inj != null) {
			Pair<Var, ApgTerm<L2, E>> x = t.cases.get(t.a.inj);
			return deref(g, x.second.subst(x.first, t.a.a));
		} else if (t.deref != null && t.a.e != null) {
			Pair<L2, ApgTerm<L2, E>> w = g.Es.get(t.a.e);
			if (!w.first.equals(t.deref)) {
				Util.anomaly();
			}
			return deref(g, w.second.convert());
		}
		if (t.head != null) {
			List<ApgTerm<L2, E>> l = Util.map(t.args, x -> deref(g, x));
			List<Object> o = new LinkedList<>();
			for (ApgTerm<L2, E> x : l) {
				if (x.value != null) {
					o.add(x.value);
				}
			}
			if (o.size() == l.size()) {
				return ApgTerm.ApgTermV(g.Ls.typeside.udfs.get(t.head).third.apply(o),
						g.Ls.typeside.udfs.get(t.head).second);
			} else {
				return ApgTerm.ApgTermApp(t.head, l);
			}
		}
		if (t.value != null || t.e != null || t.var != null) {
			return t.convert();
		}
		if (t.fields != null) {
			return ApgTerm.ApgTermTuple(Util.map(t.fields, (k, v) -> new Pair<>(k, deref(g, v))));
		}
		if (t.inj != null) {
			return ApgTerm.ApgTermInj(t.inj, deref(g, t.a), t.cases_t);
		}
		if (t.proj != null) {
			return ApgTerm.ApgTermProj(t.proj, deref(g, t.a));
		}
		if (t.cases != null) {
			return ApgTerm.ApgTermCase(deref(g, t.a),
					Util.map(t.cases, (k, v) -> new Pair<>(k, new Pair<>(v.first, deref(g, v.second)))), t.cases_t);
		}
		if (t.deref != null) {
			return ApgTerm.ApgTermDeref(t.deref, deref(g, t.a.convert()));
		}
		throw new RuntimeException("Anomaly: " + t);
	}

	public static <L> ApgMapping<L, L> id(ApgSchema<L> s) {
		Map<L, Triple<Var, ApgTy<L>, ApgTerm<L, Void>>> m = new THashMap<>();
		Var v = Var.Var("v");
		for (L l : s.schema.keySet()) {
			m.put(l, new Triple<>(v, ApgTy.ApgTyL(l), ApgTerm.ApgTermVar(v)));
		}
		return new ApgMapping<>(s, s, m);
	}

	public ApgMapping(ApgSchema<L1> src, ApgSchema<L2> dst,
			Map<L1, Triple<Var, ApgTy<L2>, ApgTerm<L2, Void>>> mapping) {
		this.src = src;
		this.dst = dst;
		this.mapping = mapping;
		validate();
	}

	public void validate() {
		if (!src.typeside.equals(dst.typeside)) {
			throw new RuntimeException("Typeside mismatch.");
		}
		Set<Var> seen = new THashSet<>();
		for (L1 l : src.schema.keySet()) {
			if (!mapping.containsKey(l)) {
				throw new RuntimeException("Missing mapping for label " + l);
			}
			Triple<Var, ApgTy<L2>, ApgTerm<L2, Void>> z = mapping.get(l);
			Map<Var, ApgTy<L2>> ctx = new THashMap<>();
			z.second.validate(dst);
			ctx.put(z.first, z.second);
			ApgTy<L2> t = src.get(l).map(l0 -> mapping.get(l0).second);
			ApgTy<L2> t2 = type(z.third, ctx);
			t.validate(dst);
			if (!t.equals(t2)) {
				throw new RuntimeException("Term " + z.third + " has type " + t2 + ", not " + t + " as expected.");
			}
			seen.add(z.first);
		}
	}

	private ApgTy<L2> type(ApgTerm<L2, Void> term, Map<Var, ApgTy<L2>> ctx) {
		if (term.e != null) {
			return Util.abort(term.e);
		} else if (term.value != null) {
			if (!src.typeside.Bs.containsKey(term.prim)) {
				throw new RuntimeException("Encountered primitive value " + term + " at non-type " + term.prim);
			} else if (!src.typeside.Bs.get(term.prim).first.isInstance(term.value)) {
				throw new RuntimeException("Primitive value " + term + " is not of type "
						+ src.typeside.Bs.get(term.prim) + ", is " + term.value.getClass().getSimpleName());
			}
			return ApgTy.ApgTyB(term.prim);
		} else if (term.var != null) {
			if (!ctx.containsKey(term.var)) {
				throw new RuntimeException("Unbound variable: " + term);
			}
			return ctx.get(term.var);
		} else if (term.head != null) {
			Triple<List<String>, String, Function<List<Object>, Object>> t = src.typeside.udfs.get(term.head);
			if (t == null) {
				throw new RuntimeException("Not a UDF: " + term.head);
			}
			List<ApgTy<L2>> l = Util.map(term.args, x->type(x,ctx));
			if (l.size() != t.first.size()) {
				throw new RuntimeException("In " + term + ", given " + l.size() + " arguments, not " + t.first.size() + " as expected.");
			}
			Iterator<ApgTy<L2>> it = l.iterator();
			Iterator<String> jt = t.first.iterator();
			for (int i = 0; i < l.size(); i++) {
				ApgTy<L2> x = it.next();
				String y = jt.next();
				if (x.b == null || !x.b.equals(y)) {
					throw new RuntimeException("In " + term + ", argument " + i + " (" + term.args.get(i) + ") has type " + x + " not " + y + " as expected.");
				}				
			}
			return ApgTy.ApgTyB(t.second);
		}
		
		
		else if (term.deref != null) {
			ApgTy<L2> x = dst.schema.get(term.deref);
			if (x == null) {
				throw new RuntimeException("In " + term + ", " + term.deref + " is not a target label.");
			}
			ApgTy<L2> y = type(term.a, ctx);
			if (!term.deref.equals(y.l)) {
				throw new RuntimeException("In " + term + ", " + term.deref + " given argument of type " + y);
			}
			return x;
		} else if (term.head != null) {
			Triple<List<String>, String, Function<List<Object>, Object>> x = src.typeside.udfs.get(term.head);
			if (x == null) {
				throw new RuntimeException(
						term.head + " not a UDF in source typeside " + Util.sep(src.typeside.udfs.keySet(), ", "));
			}
			if (x.first.size() != term.args.size()) {
				throw new RuntimeException(
						"In " + this + ", " + x.first + " arguments expected, but given " + term.args.size());
			}
			Iterator<String> it1 = x.first.iterator();
			int i = 1;
			for (ApgTerm<L2, Void> y : term.args) {
				String z = it1.next();
				if (y.prim == null || !y.prim.equals(z)) {
					throw new RuntimeException("In " + this + ", argument " + i + ", a value of base type " + z
							+ " expected, but given " + y);
				}
				i++;
			}
			return ApgTy.ApgTyB(x.second);
		}

		else if (term.fields != null) {
			return ApgTy.ApgTyP(true, Util.map(term.fields, (k, v) -> new Pair<>(k, type(v, ctx))));
		} else if (term.proj != null) {
			ApgTy<L2> x = type(term.a, ctx);
			if (x.m == null || x.all == false) {
				throw new RuntimeException(
						"In " + term + " cannot project " + term.proj + " from " + term.prim + " of type " + x);
			}
			if (!x.m.containsKey(term.proj)) {
				throw new RuntimeException(
						"In " + term + " cannot project " + term.proj + " from " + term.prim + " of type " + x);
			}
			return x.m.get(term.proj);
		} else if (term.inj != null) {
			ApgTy<L2> x = type(term.a, ctx);
			// System.out.println("Type of " + term.a + " is " + x);
			if (term.cases_t == null || term.cases_t.all || !term.cases_t.m.containsKey(term.inj)) {
				// System.out.println(term.inj);
				// System.out.println(term.cases_t.m);
				// for (String w : term.cases_t.m.keySet()) {
				// System.out.println(term.inj + " vs " + w + " is " + term.inj.equals(w));
				// }
				throw new RuntimeException(
						"In " + term + " label " + term.inj + " not found in " + term.cases_t.m.keySet());
			} else if (!term.cases_t.m.get(term.inj).equals(x)) {
				throw new RuntimeException("In " + term + " type of label for " + term.inj + ", namely, "
						+ term.cases_t.m.get(term.inj) + " is not equal to " + x + ", as expected.");
			}
			return term.cases_t;
		} else if (term.cases != null) {
			ApgTy<L2> x = type(term.a, ctx);
			if (x.m == null || x.all) {
				throw new RuntimeException("Cannot perform case analysis on " + term.a + " at non-sum type: " + x);
			}
			if (!term.cases.keySet().equals(x.m.keySet())) {
				throw new RuntimeException(
						"Set of cases " + term.cases.keySet() + " not the same as variants in type: " + x.m.keySet());
			}
			Map<String, ApgTy<L2>> map = new THashMap<>();
			ApgTy<L2> b = term.cases_t;
			for (String branch : term.cases.keySet()) {
				Pair<Var, ApgTerm<L2, Void>> a = term.cases.get(branch);

				if (ctx.containsKey(a.first)) {
					throw new RuntimeException("Duplicate bound variable: " + a.first + " in " + term);
				}
				Map<Var, ApgTy<L2>> z = new THashMap<>(ctx);
				z.put(a.first, x.m.get(branch));
				ApgTy<L2> b2 = type(a.second, z);
				if (!b.equals(b2)) {
					throw new RuntimeException(
							"Type for branch " + branch + " is " + b2 + " and not " + b + " as expected.");
				}
			}
			// System.out.println(map);
			return b;
		}
		throw new RuntimeException(term.toString());
	}

	@Override
	public Kind kind() {
		return Kind.APG_mapping;
	}

	@Override
	public int size() {
		return mapping.size();
	}

	@Override
	public String toString() {
		String s = "labels\n\t"
				+ Util.sep(mapping, " -> ", "\n\t", (Triple<Var, ApgTy<L2>, ApgTerm<L2, Void>> w) -> "lambda " + w.first
						+ " : " + w.second + ". " + w.third);

		return s;
	}

}
