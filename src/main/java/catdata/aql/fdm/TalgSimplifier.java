package catdata.aql.fdm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.Algebra.TAlg;
import catdata.aql.Head;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class TalgSimplifier<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> {

	class Step {
		@SuppressWarnings("hiding")
		public TAlg<Ty, Sym, Chc<Sk, Pair<X, Att>>> in, out;
		@SuppressWarnings("hiding")
	//	public final Map<Head<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>, Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>> s0 = subst;
		volatile boolean changed;

		public Step(TAlg<Ty, Sym, Chc<Sk, Pair<X, Att>>> col) {
			in = col;
			out = col;
		}

		public Step(Step s) {
			this.in = s.out;
			
			out = new TAlg<>(new THashMap<>(s.out.sks.size()), new THashSet<>(s.out.eqsNoDefns().size()));
			out.sks.putAll(this.in.sks);

			if (!talg_h1()) {
				changed = false;
				out = in;
				return;
			}

			changed = true;
			Iterator<Pair<Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>,Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>>>
			it = new Util.FilterTransfomIterator<>(in.eqsNoDefns().iterator(), this::fn);
			while (it.hasNext()) {
				out.eqsNoDefns().add(it.next());
			}
		}

		private synchronized Optional<Pair<Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>,Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>>> fn(
				Pair<Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>,Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>> x) {
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> l = simpl(x.first), r = simpl(x.second);
			if (l.equals(r)) {
				return Optional.empty();
			}
			return Optional.of(new Pair<>(l, r));
		}

		public synchronized void validate() {
			for (Head<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> x : subst.keySet()) {
				Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> y = subst.get(x);
				out.type(sch.typeSide, y);
				in.type(sch.typeSide, toTerm(x));
			}
		}

		private synchronized boolean talg_h1() {
			boolean b = false;
			for (Pair<Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>, Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>> eq : in.eqsNoDefns()) {
				b = talg_h(eq) | b;
			}
			return b;
		}

		private synchronized boolean talg_h(Pair<Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>, Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>> eq) {
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> l = simpl(eq.first);
			
 
			if (l.sk() != null && out.sks.containsKey(l.sk())) {
				Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> r = simpl(eq.second);
				
					if (!r.contains(Head.SkHead(l.sk()))) {
						out.sks.remove(l.sk());
						compose(l.sk(), r);
						return true;
					}
					return false;
			} 
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> r = simpl(eq.second);
			if (r.sk() != null && out.sks.containsKey(r.sk()) && !l.contains(Head.SkHead(r.sk())) ) {
				out.sks.remove(r.sk());
				compose(r.sk(), l);
				return true;
			}
	
			return false; // !l.equals(eq.lhs) || !r.equals(eq.rhs);
		}

		@SuppressWarnings("unchecked")
		private synchronized void compose(Chc<Sk, Pair<X, Att>> sk,
				Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> replacer) {
			
			Head<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> hSk = Head.SkHead(sk);
			subst.replaceAll((h, t) -> t.replaceHead(hSk, Collections.EMPTY_LIST, replacer));
			subst.put(hSk, replacer);
		}

		@Override
		public String toString() {
			return "Step [out=" + out + " and " + subst + "]";
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

	//private final Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col;
	//private final TAlg<Ty, Sym, Chc<Sk, Pair<X, Att>>> in = new Collage<>();

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

	public Step talg;
	
	private final Map<Head<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>, Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>>> subst;

	private final Schema<Ty, En, Sym, Fk, Att> sch;
	private final Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg;

	public TalgSimplifier(Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> alg, 
			Iterator<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>,Term<Ty, En, Sym, Fk, Att, Gen, Sk>>> it, Map<Sk, Ty> colsks,
			int reduce) {
		this.alg = alg;
		
		
		subst = new THashMap<>(alg.estimateNullSize(.1f));
		this.sch = alg.schema();

		TAlg<Ty, Sym, Chc<Sk, Pair<X, Att>>> in = new TAlg<>(new THashMap<>(alg.estimateNullSize(.1f)), new LinkedList<>());

		talg_h0(in, it, colsks);
		// in.validate();

		
		talg = new Step(in);
		//subst.putAll(talg.subst);
		
		for (int i = 0; i < reduce; i++) {
			//System.out.println("** " + talg);
			talg = new Step(talg);
			if (!talg.changed) {
				return;
			}
		}
	}

	private synchronized void talg_h0(TAlg<Ty, Sym, Chc<Sk, Pair<X, Att>>> in, Iterator<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>  it, Map<Sk, Ty> colsks) {
		colsks.forEach((a,b)->{
			in.sks.put(Chc.inLeftNC(a), b);
		});
		for (En en : alg.schema().ens) {
			for (X x : alg.en(en)) {
				for (Att att : sch.attsFrom(en)) {
					in.sks.put(Chc.inRightNC(new Pair<>(x, att)), sch.atts.get(att).second);
				}
			}
		}
//		in.validate();
		while (it.hasNext()) {
			Pair<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq = it.next();
			//System.out.println("&&&& " + eq);
			if (!eq.first.hasTypeType()) {
				continue; // entity
			}
			
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> l = transX(eq.first);
			Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> r = transX(eq.second);

			if (!l.equals(r)) {
				in.eqsNoDefns().add(new Pair<>(l, r));
			}
		}
		for (Triple<Pair<Var, En>, Term<Ty, En, Sym, Fk, Att, Void, Void>, Term<Ty, En, Sym, Fk, Att, Void, Void>> eq : alg
				.schema().eqs) {
			if (!sch.type(eq.first, eq.second).left) {
				continue;
			}
			//System.out.println("processing schema eq " + eq);
			for (X x : alg.en(eq.first.second)) {
				Term<Ty, En, Sym, Fk, Att, Void, Void> q = alg.repr(eq.first.second, x).convert();
				Map<Var, Term<Ty, En, Sym, Fk, Att, Void, Void>> map = Collections.singletonMap(eq.first.first, q);

				Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> l = transX(eq.second.subst(map).convert());
				Term<Ty, Void, Sym, Void, Void, Void, Chc<Sk, Pair<X, Att>>> r = transX(eq.third.subst(map).convert());

				if (!l.equals(r)) {
					// System.out.println("-- " + l + " = " + r);
					in.eqsNoDefns().add(new Pair<>(l, r));
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
		return talg.out + "\n\nDefinitions:\n" + Util.sep(subst, " -> ", "\n");
	}

}
