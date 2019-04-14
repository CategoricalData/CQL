package catdata.aql.fdm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.Collage;
import catdata.aql.Eq;
import catdata.aql.Head;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;

public class TalgSimplifier<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> {

	class Step {
		@SuppressWarnings("hiding")
		volatile Collage<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> in, out;
		@SuppressWarnings("hiding")
		final Map<Head<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>, Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>> subst = new THashMap<>();
		volatile boolean changed;

		public Step(Collage<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> col) {
			// col.validate();
			in = col;
			out = col;
			// validate();
		}

		public Step(Step s) {
			this.in = s.out;
			// this.in.validate();
//			this.subst = new Ctx<>();

			out = new Collage<>();
			out.sks.putAll(this.in.sks);
			out.syms.putAll(this.in.syms);
			out.tys.addAll(this.in.tys);
			out.java_fns.putAll(this.in.java_fns);
			out.java_parsers.putAll(this.in.java_parsers);
			out.java_tys.putAll(this.in.java_tys);
			// validate();
//			System.out.println("A " + Util.sep(new LinkedList<>(in.eqs).subList(0,Integer.min(in.eqs.size(), 16)), "\n"));

			if (!talg_h1()) {
				// System.out.println("XX " + Util.sep(new
				// LinkedList<>(in.eqs).subList(0,Integer.min(in.eqs.size(), 16)), "\n"));
				changed = false;
				out = in;
				// validate();
				return;
			}

			changed = true;
			Iterator<Eq<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>> it = new Util.FilterTransfomIterator<>(
					in.eqs.iterator(), this::fn);
			while (it.hasNext()) {
				out.eqs.add(it.next());
			}
			// System.out.println("B " + Util.sep(new
			// LinkedList<>(out.eqs).subList(0,Integer.min(out.eqs.size(), 16)), "\n"));

			// validate();
		}

		private synchronized Optional<Eq<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>> fn(
				Eq<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> x) {
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> l = simpl(x.lhs), r = simpl(x.rhs);
			if (l.equals(r)) {
				return Optional.empty();
			}
			return Optional.of(new Eq<>(null, l, r));
		}

		public synchronized void validate() {
//			System.out.println(out);
//			System.out.println(subst);
			for (Head<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> x : subst.keySet()) {
				Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> y = subst.get(x);
				out.type(Collections.emptyMap(), y);
				in.type(Collections.emptyMap(), toTerm(x));
			}
			for (Chc<Sk, Pair<X, Att>> x : in.sks.keySet()) {
				if (out.sks.keySet().contains(x)) {
					continue;
				}
				// if (!subst.containsKey(Head.SkHead(x))) {
				// Util.anomaly();
				// }
			}
		}

		private synchronized Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> simpl(
				Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> t) {
			// in.type(new Ctx<>(), t);
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> x = t.replaceHead(subst, null);
			// out.type(new Ctx<>(), x);
			return x;
		}

		private synchronized boolean talg_h1() {
			boolean b = false;
			for (Eq<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> eq : in.eqs) {
				b = talg_h(eq) | b;
			}
			return b;
		}

		private synchronized boolean talg_h(Eq<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> eq) {
			// in.type(new Ctx<>(), eq.lhs);
			// in.type(new Ctx<>(), eq.rhs);

			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> l = simpl(eq.lhs);
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> r = simpl(eq.rhs);

			// out.type(new Ctx<>(), l);
			// out.type(new Ctx<>(), r);

			if (l.sk() != null && !r.contains(Head.SkHead(l.sk())) && out.sks.containsKey(l.sk())) {
				// out.type(new Ctx<>(), l);
				// out.type(new Ctx<>(), r);
				out.sks.remove(l.sk());
//				out.type(new Ctx<>(), l);
				// out.type(new Ctx<>(), r);
				compose(l.sk(), r);
				// System.out.println("pp " + eq);
				return true;
			} else if (r.sk() != null && !l.contains(Head.SkHead(r.sk())) && out.sks.containsKey(r.sk())) {
				// out.type(new Ctx<>(), l);
				// out.type(new Ctx<>(), r);
				out.sks.remove(r.sk());
				// out.type(new Ctx<>(), l);
				compose(r.sk(), l);
				// System.out.println("qq " + eq);

				return true;
			}
			// System.out.println("ff " + eq);

			return false; // !l.equals(eq.lhs) || !r.equals(eq.rhs);
		}

		@SuppressWarnings("unchecked")
		private synchronized void compose(Chc<Sk, Pair<X, Att>> sk,
				Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> replacer) {
			// System.out.println("AA" + subst);
			// out.type(new Ctx<>(), replacer);
			Head<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> hSk = Head.SkHead(sk);
			subst.replaceAll((h, t) -> t.replaceHead(hSk, Collections.EMPTY_LIST, replacer));
			// if (!list.containsKey(hSk)) {
			subst.put(hSk, replacer);
			// }
			// for (Head<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> v :
			// subst.keySet()) {
			// if (out.sks.containsKey(v.sk)) {
			// Util.anomaly();
			// }
			/// Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> w =
			// subst.get(v);
			// out.type(new Ctx<>(), w);
			// }
			// System.out.println("BB" + subst);
		}

		@Override
		public String toString() {
			return "Step [out=" + out + "]";
		}
	}

	public Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> toTerm(
			Head<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> head) {
		if (head.obj() != null) {
			return Term.Obj(head.obj(), head.ty());
		} else if (head.sk() != null) {
			return Term.Sk(head.sk());
		}
		return Util.anomaly();
	}

	private final Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col;
	private final Collage<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> in = new Collage<>();

	public synchronized Term<Ty, En, Sym, Fk, Att, Gen, Sk> unflatten(
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> term) {
		if (term.obj() != null) {
			return term.convert();
		} else if (term.sym() != null) {
			List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> l = new ArrayList<>(term.args.size());
			for (Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> t : term.args) {
				l.add(unflatten(t));
			}
			return Term.Sym(term.sym(), l);
		} else if (term.sk() != null) {
			return term.sk().left ? Term.Sk(term.sk().l)
					: Term.Att(term.sk().r.second,
							alg.repr(alg.schema().atts.get(term.sk().r.second).first, term.sk().r.first).convert());
		}
		throw new RuntimeException("Anomaly: please report");
	}

	public synchronized Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> transX(
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		if (term.obj() != null) {
			return term.convert();
		} else if (term.sym() != null) {
			List<Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>> l = new ArrayList<>(term.args.size());
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> y : term.args) {
				l.add(transX(y));
			}
			return Term.Sym(term.sym(), l);
		} else if (term.sk() != null) {
			return Term.Sk(Chc.inLeft(term.sk()));
		} else if (term.att() != null) {
			X xx = alg.nf(term.arg.asArgForAtt());
			return Term.Sk(Chc.inRight(new Pair<>(xx, term.att())));
		} else if (term.var != null) {
			return term.convert();
		}
		throw new RuntimeException(
				"Anomaly: please report: " + term + ", gen " + term.gen() + " fk " + term.fk() + ", var " + term.var);
	}

	Step talg;

	public final Map<Head<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>, Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>> subst = new THashMap<>();

	private final Schema<Ty, En, Sym, Fk, Att> sch;
	private final Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg;

	public TalgSimplifier(Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg, Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col,
			int reduce) {
		this.alg = alg;
		this.sch = alg.schema();
		this.col = col;
		// col.validate();

		talg_h0();
		// in.validate();

		talg = new Step(in);
		subst.putAll(talg.subst);

		for (int i = 0; i < reduce; i++) {
			talg = new Step(talg);
			if (!talg.changed) {
				return;
			}
			subst.replaceAll((h, t) -> t.replaceHead(talg.subst, null));
			for (Head<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> x : talg.subst.keySet()) {
				if (!subst.containsKey(x)) {
					subst.put(x, talg.subst.get(x));
				}
			}
		}
	}

	private synchronized void talg_h0() {
		in.syms.putAll(col.syms);
		in.tys.addAll(sch.typeSide.tys);
		in.java_fns.putAll(sch.typeSide.js.java_fns);
		in.java_parsers.putAll(sch.typeSide.js.java_parsers);
		in.java_tys.putAll(sch.typeSide.js.java_tys);

		for (Entry<Sk, Ty> sk : col.sks.entrySet()) {
			in.sks.put(Chc.inLeft(sk.getKey()), sk.getValue());
		}
		for (En en : col.ens) {
			for (X x : alg.en(en)) {
				for (Att att : sch.attsFrom(en)) {
					in.sks.put(Chc.inRight(new Pair<>(x, att)), sch.atts.get(att).second);
				}
			}
		}
//		in.validate();
		for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> eq : col.eqs) {
			if (!eq.lhs.hasTypeType(eq.ctx) || !eq.ctx.isEmpty()) {
				continue; // entity
			}
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> l = transX(eq.lhs);
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> r = transX(eq.rhs);

			if (!l.equals(r)) {
				in.eqs.add(new Eq<>(null, l, r));
			}
		}
		for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : alg
				.schema().eqs) {
			if (!sch.type(eq.first, eq.second).left) {
				continue;
			}
			for (X x : alg.en(eq.first.second)) {
				Term<Ty, En, Sym, Fk, Att, Void, Void> q = alg.repr(eq.first.second, x).convert();
				Map<Var, Term<Ty, En, Sym, Fk, Att, Void, Void>> map = Collections.singletonMap(eq.first.first, q);

				Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> l = transX(eq.second.subst(map).convert());
				Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> r = transX(eq.third.subst(map).convert());

				if (!l.equals(r)) {
					// System.out.println("-- " + l + " = " + r);
					in.eqs.add(new Eq<>(null, l, r));
					// in.validate();
				}
			}
		}
	}

	public Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> simpl(
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> t) {
		Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> x = t.replaceHead(subst, null);
		return x;
	}

	@Override
	public String toString() {
		return talg.out + "\n\nDefinitions:\n" + Util.sep(subst, " -> ", "\n") + "\n\nOriginal Sks:" + in.sks.keySet();
	}

}
