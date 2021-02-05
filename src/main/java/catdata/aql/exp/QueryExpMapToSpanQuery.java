package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Eq;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.Query;
import catdata.aql.Query.Agg;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import catdata.aql.exp.SchExp.SchExpDst;
import catdata.aql.exp.SchExp.SchExpSrc;
import gnu.trove.map.hash.THashMap;

public class QueryExpMapToSpanQuery extends QueryExp {

	private final MapExp map;

	public QueryExpMapToSpanQuery(MapExp r) {
		this.map = r;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		Pair<SchExp, SchExp> u = map.type(G);
		return new Pair<>(new SchExpSpan(u.second), new SchExpSpan(u.first));
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		map.mapSubExps(f);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	protected Map<String, String> options() {
		return Collections.emptyMap();
	}

	@Override
	protected Query<Ty, En, Sym, Fk, Att, En, Fk, Att> eval0(AqlEnv env, boolean isCompileTime) {
		if (isCompileTime) {
			throw new IgnoreException();
		}
		AqlOptions ops = env.defaults;
		Mapping<Ty, En, Sym, Fk, Att, En, Fk, Att> relMap = map.eval(env, isCompileTime);
		Schema<Ty, En, Sym, Fk, Att> srcR = relMap.src;
		Schema<Ty, En, Sym, Fk, Att> dstR = relMap.dst;

		Schema<Ty, En, Sym, Fk, Att> src = new SchExpSpan(new SchExpSrc(map)).eval(env, isCompileTime);
		Schema<Ty, En, Sym, Fk, Att> dst = new SchExpSpan(new SchExpDst(map)).eval(env, isCompileTime);

		Map<En, Triple<Map<catdata.aql.Var, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>>, AqlOptions>> ens = new THashMap<>();
		Map<Att, Chc<Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>, Agg<Ty, En, Sym, Fk, Att>>> atts = new THashMap<>();
		Map<Fk, Pair<Map<catdata.aql.Var, Term<Void, En, Void, Fk, Void, catdata.aql.Var, Void>>, AqlOptions>> fks = new THashMap<>();
		Map<Fk, Map<catdata.aql.Var, Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>>> fks2 = new THashMap<>();

		for (En en : srcR.ens) {
			Att subatt = Att.Att(en, "subject");

			List<Eq<Ty, En, Sym, Fk, Att, Var, Var>> eqs = new LinkedList<>();

			ens.put(en, new Triple<>(Collections.singletonMap(Var.Var("c"), Chc.inLeft(relMap.ens.get(en))), eqs, ops));
			atts.put(subatt, Chc.inLeft(Term.Att(Att.Att(relMap.ens.get(en), "subject"), Term.Gen(Var.Var("c")))));
		}
		for (Entry<Fk, Pair<En, En>> fk : srcR.fks.entrySet()) {
			En enX = En.En(fk.getKey().str + "_" + fk.getValue().first.str + "_" + fk.getValue().second.str);
			Map<Var, Chc<En, Ty>> ctx = new THashMap<>();

			ctx.put(Var.Var("rs"), Chc.inLeft(relMap.ens.get(fk.getValue().first)));
			ctx.put(Var.Var("rt"), Chc.inLeft(relMap.ens.get(fk.getValue().second)));
			List<Eq<Ty, En, Sym, Fk, Att, Var, Var>> eqs = new LinkedList<>();

			Pair<En, List<Fk>> p = relMap.fks.get(fk.getKey());

			int i = 0;
			Term<Ty, En, Sym, Fk, Att, Var, Var> term = null;
			Term<Ty, En, Sym, Fk, Att, Var, Var> term2 = null;
			En first = null;
			for (Fk fk2 : p.second) {
				En en2 = En.En(fk2.str + "_" + dstR.fks.get(fk2).first + "_" + dstR.fks.get(fk2).second);
				if (first == null) {
					first = en2;
				}
				ctx.put(Var.Var("r" + i), Chc.inLeft(en2));
				Fk subfk1 = Fk.Fk(en2, "subject");
				Fk subfk2 = Fk.Fk(en2, "object");

				Term<Ty, En, Sym, Fk, Att, Var, Var> newterm = Term.Fk(subfk1, Term.Gen(Var.Var("r" + i)));
				Term<Ty, En, Sym, Fk, Att, Var, Var> newterm2 = Term.Fk(subfk2, Term.Gen(Var.Var("r" + i)));

				if (term != null) {
					eqs.add(new Eq<>(null, term2, newterm));
				}

				i++;
				term = newterm;
				term2 = newterm2;
			}

			if (p.second.size() > 0) {
				eqs.add(new Eq<>(null, Term.Fk(Fk.Fk(first, "subject"), Term.Gen(Var.Var("r0"))),
						Term.Gen(Var.Var("rs"))));

				eqs.add(new Eq<>(null, term2, Term.Gen(Var.Var("rt"))));
			}

			ens.put(enX, new Triple<>(ctx, eqs, ops));

			fks.put(Fk.Fk(enX, "subject"),
					new Pair<>(Collections.singletonMap(Var.Var("c"), Term.Gen(Var.Var("rs"))), ops));

			fks.put(Fk.Fk(enX, "object"),
					new Pair<>(Collections.singletonMap(Var.Var("c"), Term.Gen(Var.Var("rt"))), ops));

			fks2.put(Fk.Fk(enX, "subject"), Collections.emptyMap());
			fks2.put(Fk.Fk(enX, "object"), Collections.emptyMap());
		}
		for (Entry<Att, Pair<En, Ty>> att : srcR.atts.entrySet()) {
			En enX = En.En(att.getKey().str + "_" + att.getValue().first.str);
			Map<Var, Chc<En, Ty>> ctx = new THashMap<>();

			ctx.put(Var.Var("rs"), Chc.inLeft(relMap.ens.get(att.getValue().first)));
			List<Eq<Ty, En, Sym, Fk, Att, Var, Var>> eqs = new LinkedList<>();

			Triple<catdata.aql.Var, En, Term<Ty, En, Sym, Fk, Att, Void, Void>> p = relMap.atts.get(att.getKey());

			int i = 0;

			if (p.third.att() == null) {
				throw new RuntimeException("Require attribute in rel olog mapping");
			}
			List<Fk> xxx = p.third.arg.toFkList();

			Term<Ty, En, Sym, Fk, Att, Var, Var> term = null;
			Term<Ty, En, Sym, Fk, Att, Var, Var> term2 = null;
			En first = null;
			for (Fk fk2 : xxx) {
				En en2 = En.En(fk2.str + "_" + dstR.fks.get(fk2).first + "_" + dstR.fks.get(fk2).second);
				if (first == null) {
					first = en2;
				}
				ctx.put(Var.Var("r" + i), Chc.inLeft(en2));
				Fk subfk1 = Fk.Fk(en2, "subject");
				Fk subfk2 = Fk.Fk(en2, "object");

				Term<Ty, En, Sym, Fk, Att, Var, Var> newterm = Term.Fk(subfk1, Term.Gen(Var.Var("r" + i)));
				Term<Ty, En, Sym, Fk, Att, Var, Var> newterm2 = Term.Fk(subfk2, Term.Gen(Var.Var("r" + i)));

				if (term != null) {
					eqs.add(new Eq<>(null, term2, newterm));
				}

				i++;
				term = newterm;
				term2 = newterm2;
			}

			if (xxx.size() > 0) {
				eqs.add(new Eq<>(null, Term.Fk(Fk.Fk(first, "subject"), Term.Gen(Var.Var("r0"))),
						Term.Gen(Var.Var("rs"))));

				eqs.add(new Eq<>(null, term2, Term.Gen(Var.Var("rt"))));
			}

			Att a = Util.get0(p.third.atts());
			En en2 = En.En(a.str + "_" + dstR.atts.get(a).first);
			ctx.put(Var.Var("rt"), Chc.inLeft(en2));

			Att att2 = Att.Att(en2, "object");

			ens.put(enX, new Triple<>(ctx, eqs, ops));

			fks.put(Fk.Fk(enX, "subject"),
					new Pair<>(Collections.singletonMap(Var.Var("c"), Term.Gen(Var.Var("rs"))), ops));

			fks2.put(Fk.Fk(enX, "subject"), Collections.emptyMap());
			atts.put(Att.Att(enX, "object"), Chc.inLeft(Term.Att(att2, Term.Gen(Var.Var("rt")))));
		}

		return new Query<Ty, En, Sym, Fk, Att, En, Fk, Att>(Collections.emptyMap(), Collections.emptyMap(), ens, atts,
				fks, fks2, dst, src, ops);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
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
		QueryExpMapToSpanQuery other = (QueryExpMapToSpanQuery) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return map.deps();
	}

	@Override
	public String toString() {
		return "spanify_mapping " + map;
	}

}
