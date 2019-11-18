package catdata.aql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.It.ID;
import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;
import catdata.aql.fdm.ComposeTransform;
import catdata.aql.fdm.IdentityTransform;
import catdata.aql.fdm.LiteralTransform; //TODO aql why depend fdm
import catdata.provers.KBExp;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> implements Semantics {

	public static class Agg<Ty, En1, Sym, Fk1, Att1> {
		public final Term<Ty, En1, Sym, Fk1, Att1, Var, Var> zero, op;
		
		public final Pair<Var, Var> ctx;

		public final Map<Var, En1> lgens;
		
		public final Set<Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> leqs;
		
		public final Term<Ty, En1, Sym, Fk1, Att1, Var, Var> ret;

		public Agg(Term<Ty, En1, Sym, Fk1, Att1, Var, Var> zero, Term<Ty, En1, Sym, Fk1, Att1, Var, Var> op, Map<Var, En1> lgens,
				Set<Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> leqs,
				Term<Ty, En1, Sym, Fk1, Att1, Var, Var> ret, Pair<Var, Var> ctx) {
			this.zero = zero;
			this.op = op;
			this.lgens = lgens;
			this.leqs = leqs;
			this.ret = ret;
			this.ctx = ctx;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((leqs == null) ? 0 : leqs.hashCode());
			result = prime * result + ((lgens == null) ? 0 : lgens.hashCode());
			result = prime * result + ((op == null) ? 0 : op.hashCode());
			result = prime * result + ((ret == null) ? 0 : ret.hashCode());
			result = prime * result + ((zero == null) ? 0 : zero.hashCode());
			result = prime * result + ((ctx == null) ? 0 : ctx.hashCode());
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
			Agg<?,?,?,?,?> other = (Agg<?,?,?,?,?>) obj;
			if (leqs == null) {
				if (other.leqs != null)
					return false;
			} else if (!leqs.equals(other.leqs))
				return false;
			if (lgens == null) {
				if (other.lgens != null)
					return false;
			} else if (!lgens.equals(other.lgens))
				return false;
			if (op == null) {
				if (other.op != null)
					return false;
			} else if (!op.equals(other.op))
				return false;
			if (ret == null) {
				if (other.ret != null)
					return false;
			} else if (!ret.equals(other.ret))
				return false;
			if (zero == null) {
				if (other.zero != null)
					return false;
			} else if (!zero.equals(other.zero))
				return false;
			if (ctx == null) {
				if (other.ctx != null)
					return false;
			} else if (!ctx.equals(other.ctx))
				return false;
			return true;
		}

		//public void type(Frozen<Ty, En1, Sym, Fk1, Att1> fr) {
			
			
		//}

	
		public Frozen<Ty, En1, Sym, Fk1, Att1> toFrozen(Map<Var, Ty> params, Schema<Ty, En1, Sym, Fk1, Att1> schema,
				AqlOptions options, Frozen<Ty, En1, Sym, Fk1, Att1> frozen) {
			Map<Var, En1> fr = new THashMap<>(frozen.gens);
			Map<Var, Ty> fr2 = new THashMap<>(frozen.sks);
			Set<Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> wh = new THashSet<>(frozen.eqs);
			List<Var> order = new LinkedList<>(frozen.order);
			
			order.addAll(lgens.keySet());
			Util.putAllSafely(fr, lgens);
			wh.addAll(leqs);
		
			return new Frozen<>(fr,fr2,order,wh,schema,options);
				
		}
		
		public String toString() {
			return "from " + Util.sep(lgens, ":", " ") + "\n\twhere " + Util.sep(leqs.iterator(), "\t", x->x.first+ "=" + x.second) + "\n\treturn " + ret + "\n\taggregate " + zero + "\n\tlambda " + ctx.first + " " + ctx.second + ". " + op; 
		}
		
	}
	
	@Override
	public int size() {
		return src.size();
	}

	@Override
	public Kind kind() {
		return Kind.QUERY;
	}

	public final Map<Var, Ty> params;
	public final Map<Var, Term<Ty, Void, Sym, Void, Void, Void, Void>> consts;

	public final Map<En2, Frozen<Ty, En1, Sym, Fk1, Att1>> ens = new THashMap<>();
	public final Map<Att2, Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Agg<Ty, En1, Sym, Fk1, Att1>>> atts;

	public final Map<Ty, Frozen<Ty, En1, Sym, Fk1, Att1>> tys = new THashMap<>();

	public final Map<Fk2, Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>>> fks = new THashMap<>();

	public final Map<Fk2, AqlOptions> doNotValidate = new THashMap<>();
	// public final AqlOptions doNotValidateAll = null;

	public final Schema<Ty, En1, Sym, Fk1, Att1> src;
	public final Schema<Ty, En2, Sym, Fk2, Att2> dst;

	public Map<En2, Triple<Map<Var, Chc<En1, Ty>>, Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>>, AqlOptions>> conv1(
			Map<En2, Frozen<Ty, En1, Sym, Fk1, Att1>> ens) {
		Map<En2, Triple<Map<Var, Chc<En1, Ty>>, Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>>, AqlOptions>> ret = new THashMap<>();
		for (En2 en2 : ens.keySet()) {
			Map<Var, Chc<En1, Ty>> ctx = new THashMap<>();
			ens.get(en2).sks().entrySet((z,t)->{
				ctx.put(z,Chc.inRight(t));				
			});
			ens.get(en2).gens().entrySet((z,t)->{
				ctx.put(z,Chc.inLeft(t));	
			});
			
			Set<Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> z = ens
					.get(en2).eqs;
			Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>> l = new ArrayList<>(z.size());
			for (Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> y : z) {
				l.add(new Eq<>(null, y.first, y.second));
			}
			ret.put(en2, new Triple<>(ctx, l, ens.get(en2).options));
		}
		return ret;
	}

	private static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Map<Fk2, Pair<Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>>, AqlOptions>> conv2(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q) {
		Map<Fk2, Pair<Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>>, AqlOptions>> ret = new THashMap<>(q.fks.size());
		for (Fk2 fk2 : q.fks.keySet()) {
			Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>>
			w = q.fks.get(fk2);
			Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>> m = new THashMap<>();
			w.src().gens().forEach((pp,qq)->{
				m.put(pp, w.gens().apply(pp, qq));
			});
			ret.put(fk2, new Pair<>(m, q.doNotValidate.get(fk2))); // TODO aql true is correct here?
		}
		return ret;
	}

	private static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Map<Fk2, Map<Var, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> conv3(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q) {
		Map<Fk2, Map<Var, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> ret = new THashMap<>(q.fks.size());
		for (Fk2 fk2 : q.fks.keySet()) {
			Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>>
			w = q.fks.get(fk2);
			Map<Var, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> m = new THashMap<>();
			w.src().sks().forEach((pp,qq)->{
				m.put(pp, w.sks().apply(pp, qq));
			});
			ret.put(fk2, m); // TODO aql true is correct here?
		}
		return ret;
	} 

	public synchronized Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> unnest() {

		Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> b = new Blob<>(conv1(ens), atts, conv2(this), conv3(this), src, dst);
		b = unfoldNestedApplications(b);
		// System.out.println(b);
		Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> p = new Query<>(params, consts, b.ens, b.atts, b.fks, b.sks,
				b.src, dst, doNotCheckPathEqs);
		return p;
	}

	public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> makeQuery(
			Map<En2, Triple<Map<Var, Chc<En1, Ty>>, Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>>, AqlOptions>> ens2,
			Map<Att2, Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Agg<Ty, En1, Sym, Fk1, Att1>>> atts2,
			Map<Fk2, Pair<Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>>, AqlOptions>> map,
			Map<Fk2, Map<Var, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> map2, Schema<Ty, En1, Sym, Fk1, Att1> src2,
			Schema<Ty, En2, Sym, Fk2, Att2> dst2, AqlOptions doNotCheckPathEqs) {
		return makeQuery2(new THashMap<>(), new THashMap<>(), ens2, atts2, map, map2, src2, dst2, doNotCheckPathEqs);
	}

	public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> makeQuery2(
			Map<Var, Ty> tHashMap, Map<Var, Term<Ty, Void, Sym, Void, Void, Void, Void>> tHashMap2,
			Map<En2, Triple<Map<Var, Chc<En1, Ty>>, Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>>, AqlOptions>> ens2,
			Map<Att2, Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Agg<Ty, En1, Sym, Fk1, Att1>>> atts2,
			Map<Fk2, Pair<Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>>, AqlOptions>> map,
			Map<Fk2, Map<Var, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> map2, Schema<Ty, En1, Sym, Fk1, Att1> src2,
			Schema<Ty, En2, Sym, Fk2, Att2> dst2, AqlOptions doNotCheckPathEqs) {
		
		Map<Att2, Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>,Agg<Ty, En1, Sym, Fk1, Att1>>> 
		 atts = new THashMap<>();
		for (Entry<Att2, Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Agg<Ty, En1, Sym, Fk1, Att1>>> x : atts2.entrySet()) {
			atts.put(x.getKey(), x.getValue());
		}

		boolean removeRedundantVars = (boolean) doNotCheckPathEqs.getOrDefault(AqlOption.query_remove_redundancy);
		if (!removeRedundantVars) {
			return new Query<>(tHashMap, tHashMap2, ens2, atts, map, map2, src2, dst2, doNotCheckPathEqs);
		}

		// do this first to type check
		// @SuppressWarnings("unused")
		// Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q = new Query<>(params,
		// consts, ens, atts, fks, sks, src, dst,
		// doNotCheckPathEqs);

		Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> b = new Blob<>(ens2, atts, map, map2, src2, dst2);
		b = removeRedundantVars(b);

		// for testing
		// b = unfoldNestedApplications(b);

		Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> p = new Query<>(tHashMap, tHashMap2, b.ens, b.atts, b.fks, map2, b.src,
				dst2, doNotCheckPathEqs);

		return p;
	}

	private static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> unfoldNestedApplications(
			Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> b) {

		int i = 0;
		for (;;) {
			Triple<En2, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Head<Ty, En1, Sym, Fk1, Att1, Var, Var>> p = findNested(
					b);
			if (p == null) {
				return b;
			}
			b = elimNested(Var.Var("unfold" + i), p.first, b, p.second, p.third);

			i++;

			if (i == 128) {
				throw new RuntimeException(
						"No convergence after 128 iterations.  Note: this 1-pass SQL generation algorithm is neccessarily incomplete.");
			}
		}

	}

	private static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> removeRedundantVars(
			Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> b) {
		for (;;) {
			Triple<Var, En2, Term<Void, En1, Void, Fk1, Void, Var, Void>> p = findRedundant(b);
			if (p == null) {
				return b;
			}
			b = elimRedundant(p.first, p.second, b, p.third);

		}

	}

	private static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> elimNested(
			Var v, En2 second, Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> b,
			Term<Ty, En1, Sym, Fk1, Att1, Var, Var> third, Head<Ty, En1, Sym, Fk1, Att1, Var, Var> head) {

		Map<En2, Triple<Map<Var, Chc<En1, Ty>>, Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>>, AqlOptions>> xens = new THashMap<>();
		Map<Att2, Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>,Agg<Ty, En1, Sym, Fk1, Att1>>> xatts = new THashMap<>();
		Map<Fk2, Pair<Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>>, AqlOptions>> xfks = new THashMap<>();

		En1 srcEn = null;
		if (head.fk() != null) {
			srcEn = b.src.fks.get(head.fk()).first;
		} else if (head.att() != null) {
			srcEn = b.src.atts.get(head.att()).first;
		}

		for (Att2 att2 : b.atts.keySet()) {
			En2 atts_en2 = b.dst.atts.get(att2).first;
			if (second.equals(atts_en2)) {
				if (!b.atts.get(att2).left) {
					throw new RuntimeException("Elim Nested");
				}
				
				xatts.put(att2, Chc.inLeft(b.atts.get(att2).l.replace(third, Term.Gen(v))));
			} else {
				if (!b.atts.get(att2).left) {
					throw new RuntimeException("Elim Redundant");
				}
				
				xatts.put(att2, b.atts.get(att2));
			}
		}

		// a from v where v.f.g
		// b from u
		// f : b->a
		// from v v' where v' = v.f v' = g
		//
		for (Fk2 fk2 : b.fks.keySet()) {
			En2 src = b.dst.fks.get(fk2).first;
			En2 dst = b.dst.fks.get(fk2).second;
			Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>> g = new THashMap<>(b.fks.get(fk2).first);
			if (second.equals(dst)) {
				Term<Ty, En1, Sym, Fk1, Att1, Var, Var> zz = third
						.replace(Util.map(g, (s, t) -> new Pair<>(Term.Gen(s), t.convert())));
				g.put(v, zz.convert());
			}
			if (second.equals(src)) {
				g = Util.map(g, (k, t) -> new Pair<>(k, t.replace(third.convert(), Term.Gen(v))));
			}

			xfks.put(fk2, new Pair<>(g, b.fks.get(fk2).second));

		}

		for (En2 en : b.ens.keySet()) {
			if (en.equals(second)) {
				Map<Var, Chc<En1, Ty>> ctx = new THashMap<>(b.ens.get(en).first);
				ctx.put(v, Chc.inLeft(srcEn));
				Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>> eqs = (new ArrayList<>(b.ens.get(en).second.size()));
				for (Eq<Ty, En1, Sym, Fk1, Att1, Var, Var> eq : b.ens.get(en).second) {
					Term<Ty, En1, Sym, Fk1, Att1, Var, Var> l = eq.lhs.replace(third, Term.Gen(v)),
							r = eq.rhs.replace(third, Term.Gen(v));
					if (!l.equals(r)) {
						eqs.add(new Eq<>(null, l, r));
					}
				}
				eqs.add(new Eq<>(null, Term.Gen(v), third));
				xens.put(en, new Triple<>(ctx, eqs, b.ens.get(en).third));
			} else {
				xens.put(en, b.ens.get(en));
			}
		}

		return new Blob<>(xens, xatts, xfks, b.sks, b.src, b.dst); // TODO aql no unfolding at type
	}

	private static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> elimRedundant(
			Var v, En2 en2, Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> b,
			Term<Void, En1, Void, Fk1, Void, Var, Void> term) {

		Map<En2, Triple<Map<Var, Chc<En1, Ty>>, Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>>, AqlOptions>> xens = new THashMap<>();
		Map<Att2, Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>,Agg<Ty, En1, Sym, Fk1, Att1>>> xatts = new THashMap<>();
		Map<Fk2, Pair<Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>>, AqlOptions>> xfks = new THashMap<>();
		Map<Fk2, Map<Var, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> xsks = new THashMap<>();

		for (Att2 att2 : b.atts.keySet()) {
			En2 atts_en2 = b.dst.atts.get(att2).first;
			if (!b.atts.get(att2).left) {
				throw new RuntimeException("Elim Redundant");
			}
			if (en2.equals(atts_en2)) {
				xatts.put(att2, Chc.inLeft(b.atts.get(att2).l.replaceHead(Head.GenHead(v), Collections.emptyList(), term.convert())));
			} else {
				xatts.put(att2, Chc.inLeft(b.atts.get(att2).l));
			}
		}

		for (Fk2 fk2 : b.fks.keySet()) {
			En2 src = b.dst.fks.get(fk2).first;
			En2 dst = b.dst.fks.get(fk2).second;
			Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>> g = new THashMap<>(b.fks.get(fk2).first);
			if (en2.equals(dst)) {
				g.remove(v);
			}
			if (en2.equals(src)) {
				g = Util.map(g,
						(k, t) -> new Pair<>(k, t.replaceHead(Head.GenHead(v), (Collections.emptyList()), term)));
			}

			xfks.put(fk2, new Pair<>(g, b.fks.get(fk2).second));
		}

		for (Fk2 fk2 : b.fks.keySet()) {
			En2 src = b.dst.fks.get(fk2).first;
			En2 dst = b.dst.fks.get(fk2).second;
			Map<Var, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> g = new THashMap<>(b.sks.get(fk2));
			if (en2.equals(dst)) {
				// g.remove(v); aleady happens above
			}
			if (en2.equals(src)) {
				g = Util.map(g, (k, t) -> new Pair<>(k,
						t.replaceHead(Head.SkHead(v), (Collections.emptyList()), term.convert())));
			}

			xsks.put(fk2, g);
		}

		for (En2 en : b.ens.keySet()) {
			if (en.equals(en2)) {
				Map<Var, Chc<En1, Ty>> ctx = new THashMap<>(b.ens.get(en).first);
				ctx.remove(v);
				Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>> eqs = (new LinkedList<>());
				for (Eq<Ty, En1, Sym, Fk1, Att1, Var, Var> eq : b.ens.get(en).second) {
					Term<Ty, En1, Sym, Fk1, Att1, Var, Var> l = eq.lhs.replaceHead(Head.GenHead(v),
							(Collections.emptyList()), term.convert()),
							r = eq.rhs.replaceHead(Head.GenHead(v), (Collections.emptyList()), term.convert());
					if (!l.equals(r)) {
						eqs.add(new Eq<>(null, l, r));
					}
				}
				xens.put(en, new Triple<>(ctx, eqs, b.ens.get(en).third));
			} else {
				xens.put(en, b.ens.get(en));
			}
		}

		return new Blob<>(xens, xatts, xfks, xsks, b.src, b.dst);
	}

	private static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Triple<En2, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Head<Ty, En1, Sym, Fk1, Att1, Var, Var>> findNested(
			Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> b) {
		for (En2 en2 : b.ens.keySet()) {
			Set<KBExp<Head<Ty, En1, Sym, Fk1, Att1, Var, Var>, Var>> s = new THashSet<>();

			for (Eq<Ty, En1, Sym, Fk1, Att1, Var, Var> eq : b.ens.get(en2).second) {
				allSubExps(eq.lhs.toKB(), s);
				allSubExps(eq.rhs.toKB(), s);

				for (KBExp<Head<Ty, En1, Sym, Fk1, Att1, Var, Var>, Var> e : s) {
					Triple<En2, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Head<Ty, En1, Sym, Fk1, Att1, Var, Var>> q = findNested(
							en2, e);
					if (q != null) {
						return q;
					}
				}
			}

			for (Att2 att2 : b.dst.attsFrom(en2)) {
				if (!b.atts.get(att2).left) {
					throw new RuntimeException("Unnesting not yet supported with aggregation.");
				}
				Triple<En2, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Head<Ty, En1, Sym, Fk1, Att1, Var, Var>> q = findNested(
						en2, b.atts.get(att2).l.toKB());
				if (q != null) {
					return q;
				}
			}

			for (Fk2 fk2 : b.dst.fksFrom(en2)) {
				for (Term<Void, En1, Void, Fk1, Void, Var, Void> cand : b.fks.get(fk2).first.values()) {
					Term<Ty, En1, Sym, Fk1, Att1, Var, Var> cand2 = cand.convert();
					Triple<En2, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Head<Ty, En1, Sym, Fk1, Att1, Var, Var>> q = findNested(
							en2, cand2.toKB());
					if (q != null) {
						return q;
					}
				}
			}

		}
		return null;
	}

	private static <Ty, En1, Sym, Fk1, Att1> void allSubExps(KBExp<Head<Ty, En1, Sym, Fk1, Att1, Var, Var>, Var> exp,
			Set<KBExp<Head<Ty, En1, Sym, Fk1, Att1, Var, Var>, Var>> set) {
		set.add(exp);
		for (KBExp<Head<Ty, En1, Sym, Fk1, Att1, Var, Var>, Var> x : exp.getArgs()) {
			allSubExps(x, set);
		}
	}

	private static <Fk1, En2, Ty, En1, Att1, Sym> Triple<En2, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Head<Ty, En1, Sym, Fk1, Att1, Var, Var>> findNested(
			/* Iterator<Var> it, */ En2 en2, KBExp<Head<Ty, En1, Sym, Fk1, Att1, Var, Var>, Var> e) {
		KBExp<Head<Ty, En1, Sym, Fk1, Att1, Var, Var>, Var> f = e;
		if (f.getArgs().size() == 1 && f.f().fk() != null || f.f().att() != null) {
			KBExp<Head<Ty, En1, Sym, Fk1, Att1, Var, Var>, Var> g = f.getArgs().get(0);
			if (g.getArgs().size() == 1 && g.f().fk() != null) {
				KBExp<Head<Ty, En1, Sym, Fk1, Att1, Var, Var>, Var> x = g.getArgs().get(0);
				if (x.getArgs().size() == 0 && x.f().gen() != null) {
					return new Triple<>(/* it.next(), */ en2, Term.fromKB(g), f.f());
				}
			}
		}
		return null;
	}

	private static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Triple<Var, En2, Term<Void, En1, Void, Fk1, Void, Var, Void>> findRedundant(
			Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> b) {
		for (En2 en2 : b.ens.keySet()) {
			for (Eq<Ty, En1, Sym, Fk1, Att1, Var, Var> eq : b.ens.get(en2).second) {
				if (eq.lhs.gen() != null && !eq.rhs.gens().contains(eq.lhs.gen())) {
					return new Triple<>(eq.lhs.gen(), en2, eq.rhs.convert());
				} else if (eq.rhs.gen() != null && !eq.lhs.gens().contains(eq.rhs.gen())) {
					return new Triple<>(eq.rhs.gen(), en2, eq.lhs.convert());
				}
			}
		}
		return null;
	}

	private static class Blob<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> {
		public final Map<En2, Triple<Map<Var, Chc<En1, Ty>>, Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>>, AqlOptions>> ens;
		public final Map<Att2, Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>,Agg<Ty, En1, Sym, Fk1, Att1>>> atts;
		public final Map<Fk2, Pair<Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>>, AqlOptions>> fks;
		public final Map<Fk2, Map<Var, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> sks;

		public final Schema<Ty, En1, Sym, Fk1, Att1> src;
		public final Schema<Ty, En2, Sym, Fk2, Att2> dst;

		@Override
		public String toString() {
			return "Blob [ens=" + ens + ", atts=" + atts + ", fks=" + fks + ", sks=" + sks + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((atts == null) ? 0 : atts.hashCode());
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((ens == null) ? 0 : ens.hashCode());
			result = prime * result + ((fks == null) ? 0 : fks.hashCode());
			result = prime * result + ((sks == null) ? 0 : sks.hashCode());
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
			Blob<?, ?, ?, ?, ?, ?, ?, ?> other = (Blob<?, ?, ?, ?, ?, ?, ?, ?>) obj;
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
			if (sks == null) {
				if (other.sks != null)
					return false;
			} else if (!sks.equals(other.sks))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}

		public Blob(
				Map<En2, Triple<Map<Var, Chc<En1, Ty>>, Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>>, AqlOptions>> ens,
				Map<Att2, Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>,Agg<Ty, En1, Sym, Fk1, Att1>>> atts,
				Map<Fk2, Pair<Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>>, AqlOptions>> fks,
				Map<Fk2, Map<Var, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> sks, Schema<Ty, En1, Sym, Fk1, Att1> src,
				Schema<Ty, En2, Sym, Fk2, Att2> dst) {
			this.ens = ens;
			this.atts = atts;
			this.fks = fks;
			this.src = src;
			this.dst = dst;
			this.sks = sks;
			// if (!this.equals(this)) {
			// Util.anomaly();
			// }
		}

	}

	AqlOptions doNotCheckPathEqs;

	// doNotCheckPathEqs will stop construction of dps
		public Query(Map<Var, Ty> params, Map<Var, Term<Ty, Void, Sym, Void, Void, Void, Void>> consts,
				Map<En2, Triple<Map<Var, Chc<En1, Ty>>, Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>>, AqlOptions>> ens,
				Map<Att2, Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Agg<Ty, En1, Sym, Fk1, Att1>>> atts,
				Map<Fk2, Pair<Map<Var, Term<Void, En1, Void, Fk1, Void, Var, Void>>, AqlOptions>> fks,
				Map<Fk2, Map<Var, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> sks, Schema<Ty, En1, Sym, Fk1, Att1> src,
				Schema<Ty, En2, Sym, Fk2, Att2> dst, AqlOptions doNotCheckPathEqs) {
			this.src = src;
			this.dst = dst;
			this.params = params;
			this.consts = consts;
			totalityCheck(ens, atts, fks);
			this.doNotCheckPathEqs = doNotCheckPathEqs;
			Map<Var, Ty> pp = (params);
			for (En2 en2 : ens.keySet()) {
				try {
					this.ens.put(en2, new Frozen<>(pp, (ens.get(en2).first), ens.get(en2).second, src, ens.get(en2).third));
				} catch (Throwable thr) {
					thr.printStackTrace();
					throw new RuntimeException("In block for entity " + en2 + ", " + thr.getMessage());
				}
			}

			for (Ty ty : src.typeSide.tys) {
				this.tys.put(ty, new Frozen<>(pp, Collections.singletonMap(Var.Var("_y_"), Chc.inRight(ty)),
						Collections.emptyList(), src, doNotCheckPathEqs));
			}

			for (Fk2 fk2 : fks.keySet()) {
				Map<Var, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> www = new THashMap<>(sks.get(fk2));
				for (Var v : params.keySet()) {
					www.put(v, Term.Sk(v));
				}
				try {
					AqlOptions b = fks.get(fk2).second;
					doNotValidate.put(fk2, b);
					this.fks.put(fk2,
							new LiteralTransform<>((x, t) -> fks.get(fk2).first.get(x), (x, t) -> www.get(x),
									this.ens.get(dst.fks.get(fk2).second), this.ens.get(dst.fks.get(fk2).first),
									(boolean) b.getOrDefault(AqlOption.dont_validate_unsafe)));
				} catch (Throwable thr) {
					// thr.printStackTrace();
					throw new RuntimeException("In transform for foreign key " + fk2 + ", " + thr.getMessage() + "\n\n");
				}
			}
			this.atts = atts;

			validate((boolean) doNotCheckPathEqs.getOrDefault(AqlOption.dont_validate_unsafe));

		}

	private void totalityCheck(Map<En2, ?> ens2, Map<Att2, ?> atts, Map<Fk2, ?> fks2) {
		for (En2 en2 : dst.ens) {
			if (!ens2.containsKey(en2)) {
				throw new RuntimeException("no query for " + en2);
			}
		}
		for (En2 en2 : ens2.keySet()) {
			if (!dst.ens.contains(en2)) {
				throw new RuntimeException("there is a query for " + en2 + ", which is not an entity in the target");
			}
		}
		for (Att2 att2 : dst.atts.keySet()) {
			if (!atts.containsKey(att2)) {
				throw new RuntimeException("no return clause for attribute " + att2);
			}
		}
		for (Att2 att2 : atts.keySet()) {
			if (!dst.atts.containsKey(att2)) {
				throw new RuntimeException(
						"there is an attributes clause for " + att2 + ", which is not an attribute in the target");
			}
		}
		for (Fk2 fk2 : dst.fks.keySet()) {
			if (!fks2.containsKey(fk2)) {
				throw new RuntimeException("no transform for foreign key " + fk2);
			}
		}
		for (Fk2 fk2 : fks2.keySet()) {
			if (!dst.fks.containsKey(fk2)) {
				throw new RuntimeException(
						"there is a transform for " + fk2 + ", which is not a foreign key in the target");
			}
		}
	}

	public void validate(boolean doNotValidateEqs) {
		if (doNotValidateEqs) {
			return;
		}
		for (Att2 k : atts.keySet()) {
			Frozen<Ty, En1, Sym, Fk1, Att1> fr = ens.get(dst.atts.get(k).first);
			Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Agg<Ty, En1, Sym, Fk1, Att1>> m = atts.get(k);
			if (m.left) {
				fr.type(m.l);
			} else {
				m.r.toFrozen(params, src, fr.options, fr);
			}
		}
		for (Triple<Pair<Var, En2>, Term<Ty, En2, Sym, Fk2, Att2, Void, Void>, Term<Ty, En2, Sym, Fk2, Att2, Void, Void>> eq : dst.eqs) {
			Chc<Ty, En2> ty = dst.type(eq.first, eq.second);
			Frozen<Ty, En1, Sym, Fk1, Att1> I = ens.get(eq.first.second);
			if (ty.left) {
				Term<Ty, En1, Sym, Fk1, Att1, Var, Var> lhs = transT(eq.second.mapGenSk(Util.voidFn(), Util.voidFn()));
				Term<Ty, En1, Sym, Fk1, Att1, Var, Var> rhs = transT(eq.third.mapGenSk(Util.voidFn(), Util.voidFn()));
				if (!I.dp().eq(null, lhs, rhs)) {
					throw new RuntimeException(
							"Target equation " + eq.second + " = " + eq.third + " not respected: transforms to " + lhs
									+ " = " + rhs + ", which is not provable in the sub-query for " + eq.first.second
									+ "\n\n" + I.toString());
				}
			} else { // entity
				for (Var u : ens.get(ty.r).gens.keySet()) {
					Term<Ty, En1, Sym, Fk1, Att1, Var, Var> lhs = transP(eq.second.convert(), Term.Gen(u), ty.r);
					Term<Ty, En1, Sym, Fk1, Att1, Var, Var> rhs = transP(eq.third.convert(), Term.Gen(u), ty.r);
					if (!I.dp().eq(null, lhs, rhs)) {
						throw new RuntimeException("Target equation " + eq.second + " = " + eq.third
								+ " not respected: transforms to " + lhs + " = " + rhs
								+ ", which is not provable in the sub-query for " + eq.first.second);
					}
				}
			}
		}
		for (Fk2 xxx : fks.keySet()) {
			fks.get(xxx).validate(false);
		}
	}

	@Override
	public final String toString() {
		String ret = "";

		if (!params.isEmpty()) {
			ret += "params " + Util.sep(params, ":", " ") + "\n\n";
		}
		if (!consts.isEmpty()) {
			ret += "bindings " + Util.sep(consts, ":", " ") + "\n\n";
		}

		Map<String, String> m1 = new THashMap<>();

		for (En2 en2 : ens.keySet()) {
			Map<String, String> m3 = new THashMap<>();
			Map<Fk2, String> m2 = new THashMap<>();

			for (Att2 att : dst.attsFrom(en2)) {
				m3.put(att.toString(), atts.get(att).toStringMash());
			}
			// Map<String, String> m4 = new HashMap<>();
			for (Fk2 fk : dst.fksFrom(en2)) {
				m2.put(fk, "{" + fks.get(fk).toString("", "") + "}");
			}
			String x = m3.isEmpty() ? "" : " \nattributes\n\t";
			String y = m2.isEmpty() ? "" : " \nforeign_keys\n\t";
			m1.put("entity " + en2, "{" + ens.get(en2).toString("\nfrom", " where `").trim() + x
					+ Util.sep(m3, " -> ", "\n\t") + "\n" + y + Util.sep(m2, " -> ", "\n\t") + "\n}");

			// ret += "\n\nforeign_keys\n\n";
			// ret += Util.sep(m2, " -> ", "\n\n");

		}

		ret += Util.sep(m1, " -> ", "\n\n");

		return ret;
	}

	private Term<Ty, En1, Sym, Fk1, Att1, Var, Var> transT(Term<Ty, En2, Sym, Fk2, Att2, Var, Var> term) {
		if (term.obj() != null) {
			return term.asObj();
		} else if (term.sym() != null) {
			return Term.Sym(term.sym(), (term.args.stream().map(this::transT).collect(Collectors.toList())));
		} else if (term.att() != null) {
			if (!atts.get(term.att()).left) {
				Util.anomaly();
			}
			return transP(term.arg.asArgForAtt(), atts.get(term.att()).l.convert(), dst.atts.get(term.att()).first);
		} else if (term.sk() != null) {
			return Term.Sk(term.sk());
		}
		throw new RuntimeException("Anomaly: please report");
	}

	public List<Fk2> transP(Term<Void, En2, Void, Fk2, Void, Var, Void> term) {
		if (term.var != null) {
			return Collections.emptyList();
		} else if (term.fk() != null) {
			return Util.append(Collections.singletonList(term.fk()), transP(term.arg));
		} else if (term.gen() != null) {
			return Collections.emptyList();
		}
		throw new RuntimeException("Anomaly: please report " + term);
	}

	public Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> compose(
			List<Fk2> l, En2 en2) {
		if (l.isEmpty()) {
			return new IdentityTransform<>(ens.get(en2), Optional.empty());
		}
		Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> t = fks
				.get(l.get(0));
		Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> u = compose(
				l.subList(1, l.size()), dst.fks.get(l.get(0)).first);
		return new ComposeTransform<>(t, u);

	}

	public Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> att(
			Att2 att2) {
		En2 en2 = dst.atts.get(att2).first;
		Ty ty = dst.atts.get(att2).second;
		Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Agg<Ty, En1, Sym, Fk1, Att1>> b0 = atts.get(att2);
		if (!b0.left) {
			throw new RuntimeException("Cannot create transforms with aggregation.");
		}

		Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> a = new LiteralTransform<>(
				(x, t) -> Util.anomaly(), (x, t) -> b0.l, tys.get(ty), ens.get(en2), true);
		return a;
	}


	public Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> composeT(
			Term<Ty, En2, Sym, Fk2, Att2, Var, Var> l, En2 en2) {
		
		if (l.att() != null) {
				Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> a = compose(
						transP(l.arg.asArgForAtt()), dst.atts.get(l.att()).first);
				Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> b = att(
						l.att());
				return new ComposeTransform<>(b, a);
			} else if (l.obj() != null) {
				Term<Ty, En1, Sym, Fk1, Att1, Var, Var> b = Term.Obj(l.obj(), l.ty());
				Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> a = new LiteralTransform<>(
						(x, t) -> Util.anomaly(), (x, t) -> b, tys.get(l.ty()), ens.get(en2), true);
				return a;
			} else if (l.sym() != null) {
				Ty ty = dst.typeSide.syms.get(l.sym()).second;
				List<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> z = new ArrayList<>(l.args.size());
				for (Term<Ty, En2, Sym, Fk2, Att2, Var, Var> x : l.args) {
					z.add(composeT(x, en2).sks().apply(Var.Var("_y_"), ty));
				}
				Term<Ty, En1, Sym, Fk1, Att1, Var, Var> b = Term.Sym(l.sym(), z);
				Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> a = new LiteralTransform<>(
						(x, t) -> Util.anomaly(), (x, t) -> b, tys.get(ty), ens.get(en2), true);
				return a;
			} else if (l.var != null) {
				return new IdentityTransform<>(tys.get(ens.get(en2).sks.get(l.var)), Optional.empty());
			} else if (l.sk() != null) {
				return new IdentityTransform<>(tys.get(ens.get(en2).sks.get(l.sk())), Optional.empty());
			}
			return Util.anomaly();

		}

	

	private Term<Ty, En1, Sym, Fk1, Att1, Var, Var> transP(Term<Void, En2, Void, Fk2, Void, Var, Void> term,
			Term<Void, En1, Void, Fk1, Void, Var, Void> u, En2 en2) {
		List<Fk2> l = transP(term);
		Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> t = compose(
				l, en2);
		return t.trans(u.convert());
	}

	////////////////

	public Pair<Collection<Fk1>, Collection<Att1>> fksAndAttsOfWhere() {
		Set<Fk1> fks = new THashSet<>();
		Set<Att1> atts = new THashSet<>();
		for (Frozen<Ty, En1, Sym, Fk1, Att1> I : ens.values()) {
			for (Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> eq : I.eqs) {
				eq.first.fks(fks);
				eq.second.fks(fks);
				eq.second.atts(atts);
				eq.first.atts(atts);
			}
		}
		return new Pair<>(fks, atts);
	}

	private Map<En2, String> ret;

	// this is used internally
	public static final String internal_id_col_name = "rowid";

	public synchronized Map<En2, String> toSQL(String tick) {
		if (ret != null) {
			return ret;
		}
		String idCol = internal_id_col_name; // used internally, so don't honor options
		ret = new THashMap<>();
		for (En2 en2 : ens.keySet()) {
			Frozen<Ty, En1, Sym, Fk1, Att1> b = ens.get(en2);
			Map<Var, En1> gens = b.gens;
			Collection<Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> eqs = b.eqs;

			if (ens.get(en2).gens.isEmpty()) {
				ret.put(en2, "Empty from clause doesn't work with sql");
				return ret;
			}
			// TODO aql check name collision
			String toString2 = " from ";

			List<String> temp = new ArrayList<>(gens.size());
			List<String> tempL = new ArrayList<>(gens.size());

			for (Var v : gens.keySet()) {
				temp.add(tick + gens.get(v) + tick + " as " + v);
				tempL.add(v.toString() + "." + tick + idCol + tick + " as " + v);
			}

			toString2 += Util.sep(temp, ", ");

			if (!eqs.isEmpty()) {
				toString2 += whereToString(eqs, idCol, tick);
			}

			String toString3 = " select ";
			toString3 += Util.sep(tempL, ", ");
			String y = toString3 + toString2;
			ret.put(en2, y);
		}

		return ret;
	}

	/**
	 * Converts an CQL query, as best as it can, into SQL.
	 * 
	 * @param pre   to be placed a the front of any column name
	 * @param post  to be prepended to the view name for drop and create.
	 * @param idCol the name of the id column which provides a unique identifier for
	 *              the tuple
	 * @param ty    the name of the typeside to be used in resolving properties
	 *              "char" is common usage
	 * @return a pair first : some sql for recreating the view. second : the sql
	 *         describing the view.
	 */
	public Pair<List<String>, Map<En2, String>> toSQLViews(String pre, String post, String idCol, String ty,
			String tick) {
		if (!(src.typeSide.tys.containsAll(SqlTypeSide.tys()))) {
			throw new RuntimeException("Not on SQL typeside");
		}

		List<String> ret1 = new LinkedList<>();
		Map<En2, String> ret2 = new THashMap<>();
		idCol = tick + idCol + tick;
		for (En2 en2 : ens.keySet()) {
			Frozen<Ty, En1, Sym, Fk1, Att1> b = ens.get(en2);
			Map<Var, En1> gens = b.gens;
			Collection<Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> eqs = b.eqs;

			if (ens.get(en2).gens.isEmpty()) {
				throw new RuntimeException("Empty from clause doesn't work with sql");
			}

			List<String> from = new LinkedList<>();
			List<String> select = new LinkedList<>();

			select.add(sk(gens.keySet(), idCol, ty) + " as " + idCol); // add id column
			for (Var v : gens.keySet()) {
				from.add(tick + pre + gens.get(v) + tick + " as " + v);
			}
			for (Att2 att2 : dst.attsFrom(en2)) {
				if (!atts.get(att2).left) {
					throw new RuntimeException("SQL translation for aggregation not yet supported.");
				}
				select.add(atts.get(att2).l.toStringSql(tick) + " as " + tick + att2 + tick);
			}
			for (Fk2 fk2 : dst.fksFrom(en2)) {
				select.add(sk(fks.get(fk2), idCol, ty, tick) + " as " + tick + fk2 + tick);
			}
			// TODO ADD FOREIGN KEYS aql

			String xxx = "  select " + Util.sep(select, ", ") + "\nfrom " + Util.sep(from, ", ") + "\n "
					+ whereToString(eqs, idCol, tick);

			ret1.add("drop view if exists " + tick + post + en2 + tick);

			ret1.add("create view " + tick + post + en2 + tick + " as " + xxx);

			ret2.put(en2, xxx);
		}

		return new Pair<>(ret1, ret2);
	}

	private String convert(String x, String ty) {
		return "convert(" + x + ", " + ty + ")";
	}

	private String qdirty(Term<?, ?, ?, ?, ?, ?, ?> t, String idCol, String tick) {
		if (t.gen() != null) {
			return t.gen() + "." + idCol;
		} else if (t.fk() != null) {
			return t.arg + "." + tick + t.fk() + tick;
		}
		return Util.anomaly();
	}

	private String sk(
			Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> h,
			String idCol, String ty, String tick) {
		
		List<Pair<String, String>> l = new ArrayList<>(h.src().gens().size());
				h.src().gens().entrySet((v,t) -> {
				l.add(new Pair<>(v.var, convert(qdirty(h.gens().apply(v,t), idCol, tick), ty)));
				});

		return sk(l, ty);
	}

	private String sk(Collection<Pair<String, String>> vs, String ty) {
		if (vs.isEmpty()) {
			Util.anomaly();
		}
		List<String> l = (vs.stream()
				.map(x -> "concat('(" + x.first + "=', concat(" + convert(x.second, ty) + ", ')'))")
				.collect(Collectors.toList()));

		String s = l.get(0);
		for (int i = 1; i < l.size(); i++) {
			s = "concat(" + s + ", " + l.get(i) + ")";
		}
		return s;
	}

	private String sk(Set<Var> vs, String idCol, String ty) {
		return sk(vs.stream().map(x -> new Pair<>(x.var, x.var + "." + idCol)).collect(Collectors.toList()), ty);

	}

	private String whereToString(
			Collection<Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> eqs,
			String idCol, String tick) {
		if (eqs.isEmpty()) {
			return "";
		}
		List<String> temp;
		String toString2 = " where ";
		temp = new LinkedList<>();
		for (Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> eq : eqs) {
			String newLhs;
			if (eq.first.gen() != null) {
				newLhs = eq.first.gen() + "." + idCol;
			} else if (eq.first.sk() != null) {
				newLhs = "?";
				if (consts.containsKey(eq.first.sk())) {
					newLhs = quotePrim(consts.get(eq.first.sk()).convert()).toStringSql(tick);
				}
			} else {
				newLhs = quotePrim(eq.first).toStringSql(tick);
			}
			String newRhs;
			if (eq.second.gen() != null) {
				newRhs = eq.second.gen() + "." + idCol;
			} else if (eq.second.sk() != null) {
				newRhs = "?";
				if (consts.containsKey(eq.second.sk())) {
					newRhs = quotePrim(consts.get(eq.second.sk()).convert()).toStringSql(tick);
				}
			} else {
				newRhs = quotePrim(eq.second).toStringSql(tick);
			}
			temp.add(newLhs + " = " + newRhs);
		}
		toString2 += Util.sep(temp, " and ");
		return toString2;
	}

	private Term<Ty, En1, Sym, Fk1, Att1, Var, Var> quotePrim(Term<Ty, En1, Sym, Fk1, Att1, Var, Var> t) {
		if (t.var != null || t.gen() != null || t.sk() != null) {
			return t;
		} else if (t.sym() != null && t.args.size() == 0) {
			return t;
		} else if (t.fk() != null) {
			return Term.Fk(t.fk(), quotePrim(t.arg));
		} else if (t.att() != null) {
			return Term.Att(t.att(), quotePrim(t.arg));
		} else if (t.obj() != null) {
			return Term.Obj("'" + t.obj() + "'", t.ty());
		} else if (t.sym() != null) {
			List<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> l = new ArrayList<>();
			for (Term<Ty, En1, Sym, Fk1, Att1, Var, Var> x : t.args) {
				l.add(quotePrim(x));
			}
			return Term.Sym(t.sym(), l);
		}
		return Util.anomaly();
	}

	public static <Ty, En, Sym, Fk, Att> Query<Ty, En, Sym, Fk, Att, En, Fk, Att> id(AqlOptions options,
			Schema<Ty, En, Sym, Fk, Att> S, Schema<Ty, En, Sym, Fk, Att> T) {
		Var v = Var.Var("v");

		Map<En, Triple<Map<Var, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att, Var, Var>>, AqlOptions>> ens0 = new THashMap<>();
		Map<Att,Chc<Term<Ty, En, Sym, Fk, Att, Var, Var>,Agg<Ty, En, Sym, Fk, Att>>> atts0 = new THashMap<>();
		Map<Fk, Pair<Map<Var, Term<Void, En, Void, Fk, Void, Var, Void>>, AqlOptions>> fks0 = new THashMap<>();
		Map<Fk, Map<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>>> sks0 = new THashMap<>();

		for (En en : S.ens) {
			Map<Var, Chc<En, Ty>> from = new THashMap<>();
			from.put(v, Chc.inLeft(en));
			ens0.put(en, new Triple<>(from, (Collections.emptyList()), options));
			for (Att att : S.attsFrom(en)) {
				atts0.put(att, Chc.inLeft(Term.Att(att, Term.Gen(v))));
			}
			for (Fk fk : S.fksFrom(en)) {
				Map<Var, Term<Void, En, Void, Fk, Void, Var, Void>> h = new THashMap<>();
				h.put(v, Term.Fk(fk, Term.Gen(v)));
				fks0.put(fk, new Pair<>(h, options));
				sks0.put(fk, new THashMap<>());
			}
		}

		return new Query<>(new THashMap<>(), new THashMap<>(), ens0, atts0, fks0, sks0, S, T, options);
	}

	public Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> deParam() {

		Map<Att2, Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>,Agg<Ty, En1, Sym, Fk1, Att1>>> atts2 = new THashMap<>();
		Function<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> f = x -> {
			for (Var var : consts.keySet()) {
				x = x.replace(Term.Sk(var), consts.get(var).convert());
			}
			return x;
		};
		Function<Set<Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>>, Set<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>>> g = x -> {
			Set<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>> ret = (new THashSet<>());
			for (Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> y : x) {
				ret.add(new Eq<>(null, f.apply(y.first), f.apply(y.second)));
			}
			return ret;
		};

		for (Att2 att2 : atts.keySet()) {
			if (!atts.get(att2).left) {
				Util.anomaly();
			}
			atts2.put(att2, Chc.inLeft(f.apply(atts.get(att2).l)));
		}

		Map<En2, Triple<Map<Var, Chc<En1, Ty>>, Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>>, AqlOptions>> ens2 = new THashMap<>();
		for (En2 en2 : ens.keySet()) {
			Map<Var, Chc<En1, Ty>> ctx = new THashMap<>();
			ens.get(en2).sks().entrySet((k,v)->{
				ctx.put(k,Chc.inRight(v));
			});
			ens.get(en2).gens().entrySet((k,v)->{
				ctx.put(k,Chc.inLeft(v));
			});
			ens2.put(en2, new Triple<>(ctx, g.apply(ens.get(en2).eqs), ens.get(en2).options));
		}
		return makeQuery(ens2, atts2, conv2(this), conv3(this), src, dst, this.doNotCheckPathEqs);
	}

	@SuppressWarnings("hiding")
	public static <Ty, En, Sym, Fk, Att> Term<Ty, En, Sym, Fk, Att, Var, Var> freeze(
			Term<Ty, En, Sym, Fk, Att, Var, Var> term, Map<String, String> params, Set<Var> tyvars) {

		Map<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>> m = new THashMap<>();
		for (Var v : term.vars()) {
			if (params.keySet().contains(v.var) || tyvars.contains(v)) {
				m.put(v, Term.Sk(v));
			} else {
				m.put(v, Term.Gen(v));
			}
		}
		Term<Ty, En, Sym, Fk, Att, Var, Var> ret = term.subst(m);
		return ret;

	}
	
	public static <Ty, En, Sym, Fk, Att> Term<Ty, En, Sym, Fk, Att, Var, Var> freezeAgg(
			Term<Ty, En, Sym, Fk, Att, Var, Var> term, Map<String, String> params, Set<Var> tyvars, String a, String b) {

		Map<Var, Term<Ty, En, Sym, Fk, Att, Var, Var>> m = new THashMap<>();
		for (Var v : term.vars()) {
			if (v.var.equals(a) || v.var.equals(b)) {
				return term;
			} else if (params.keySet().contains(v.var) || tyvars.contains(v)) {
				m.put(v, Term.Sk(v));
			} else {
				m.put(v, Term.Gen(v));
			}
		}
		Term<Ty, En, Sym, Fk, Att, Var, Var> ret = term.subst(m);
		return ret;

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((atts == null) ? 0 : atts.hashCode());
		result = prime * result + ((consts == null) ? 0 : consts.hashCode());
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((ens == null) ? 0 : ens.hashCode());
		result = prime * result + ((fks == null) ? 0 : fks.hashCode());
		result = prime * result + ((params == null) ? 0 : params.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		result = prime * result + ((tys == null) ? 0 : tys.hashCode());
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
		Query<?, ?, ?, ?, ?, ?, ?, ?> other = (Query<?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (atts == null) {
			if (other.atts != null)
				return false;
		} else if (!atts.equals(other.atts))
			return false;
		if (consts == null) {
			if (other.consts != null)
				return false;
		} else if (!consts.equals(other.consts))
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
		if (params == null) {
			if (other.params != null)
				return false;
		} else if (!params.equals(other.params))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		if (tys == null) {
			if (other.tys != null)
				return false;
		} else if (!tys.equals(other.tys))
			return false;
		return true;
	}

	public boolean hasAgg() {
		for (Chc<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Agg<Ty, En1, Sym, Fk1, Att1>> x : atts.values()) {
			if (!x.left) {
				return false;
			}
		}
		return true;
	}

}
