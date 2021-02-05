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
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.Eq;
import catdata.aql.Kind;
import catdata.aql.Query;
import catdata.aql.Query.Agg;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Var;
import catdata.aql.exp.SchExp.SchExpRdf;
import gnu.trove.map.hash.THashMap;

public class QueryExpSpanify extends QueryExp {

	private final SchExp rel;

	public QueryExpSpanify(SchExp rel) {
		this.rel = rel;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		TyExp u = rel.type(G);
		TyExp t = InstExpRdfAll.makeSch().type(G);
		if (!t.equals(u)) {
			throw new RuntimeException("Typeside mismatch on " + this);
		}
		return new Pair<>(new SchExpRdf(), new SchExpSpan(rel));
	}

	@Override
	public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		rel.map(f);
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
		Schema<Ty, En, Sym, Fk, Att> src = InstExpRdfAll.makeSch().eval(env, isCompileTime);
		Schema<Ty, En, Sym, Fk, Att> relOlog = rel.eval(env, isCompileTime);
		Collage<Ty, En, Sym, Fk, Att, Void, Void> col = new CCollage<>(src.typeSide.collage());
		if (!relOlog.eqs.isEmpty()) {
			throw new RuntimeException("RelOlog equations not supported yet");
		}

		Map<En, Triple<Map<catdata.aql.Var, Chc<En, Ty>>, Collection<Eq<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>>, AqlOptions>> ens = new THashMap<>();
		Map<Att, Chc<Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>, Agg<Ty, En, Sym, Fk, Att>>> atts = new THashMap<>();
		Map<Fk, Pair<Map<catdata.aql.Var, Term<Void, En, Void, Fk, Void, catdata.aql.Var, Void>>, AqlOptions>> fks = new THashMap<>();
		Map<Fk, Map<catdata.aql.Var, Term<Ty, En, Sym, Fk, Att, catdata.aql.Var, catdata.aql.Var>>> fks2 = new THashMap<>();

		Att subatt = Att.Att(En.En("R"), "subject");
		Att predatt = Att.Att(En.En("R"), "predicate");
		Att obatt = Att.Att(En.En("R"), "object");
		for (En en : relOlog.ens) {
			col.getEns().add(en);
			List<Eq<Ty, En, Sym, Fk, Att, Var, Var>> eqs = new LinkedList<>();
			eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(Var.Var("c"))),
					Term.Obj("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", Ty.Ty("Dom"))));
			eqs.add(new Eq<>(null, Term.Att(obatt, Term.Gen(Var.Var("c"))), Term.Obj(en.str, Ty.Ty("Dom"))));

			ens.put(en, new Triple<>(Collections.singletonMap(Var.Var("c"), Chc.inLeft(En.En("R"))), eqs, ops));
			atts.put(Att.Att(en, "subject"), Chc.inLeft(Term.Att(subatt, Term.Gen(Var.Var("c")))));
			col.atts().put(Att.Att(en, "subject"), new Pair<>(en, Ty.Ty("Dom")));
		}
		for (Entry<Fk, Pair<En, En>> fk : relOlog.fks.entrySet()) {
			En en = En.En(fk.getKey().str + "_" + fk.getValue().first.str + "_" + fk.getValue().second.str);
			col.getEns().add(en);
			col.fks().put(Fk.Fk(en, "subject"), new Pair<>(en, fk.getValue().first));
			col.fks().put(Fk.Fk(en, "object"), new Pair<>(en, fk.getValue().second));

			Map<Var, Chc<En, Ty>> ctx = new THashMap<>();
			ctx.put(Var.Var("r"), Chc.inLeft(En.En("R")));
			ctx.put(Var.Var("rs"), Chc.inLeft(En.En("R")));
			ctx.put(Var.Var("rt"), Chc.inLeft(En.En("R")));
			List<Eq<Ty, En, Sym, Fk, Att, Var, Var>> eqs = new LinkedList<>();
			eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(Var.Var("r"))), Term.Obj(fk.getKey().str, Ty.Ty("Dom"))));
			eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(Var.Var("rs"))),
					Term.Obj("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", Ty.Ty("Dom"))));
			eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(Var.Var("rt"))),
					Term.Obj("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", Ty.Ty("Dom"))));
			eqs.add(new Eq<>(null, Term.Att(obatt, Term.Gen(Var.Var("rs"))),
					Term.Obj(fk.getValue().first.str, Ty.Ty("Dom"))));
			eqs.add(new Eq<>(null, Term.Att(obatt, Term.Gen(Var.Var("rt"))),
					Term.Obj(fk.getValue().second.str, Ty.Ty("Dom"))));
			eqs.add(new Eq<>(null, Term.Att(obatt, Term.Gen(Var.Var("r"))), Term.Att(subatt, Term.Gen(Var.Var("rt")))));
			eqs.add(new Eq<>(null, Term.Att(subatt, Term.Gen(Var.Var("r"))),
					Term.Att(subatt, Term.Gen(Var.Var("rs")))));

			ens.put(en, new Triple<>(ctx, eqs, ops));
			fks.put(Fk.Fk(en, "subject"),
					new Pair<>(Collections.singletonMap(Var.Var("c"), Term.Gen(Var.Var("rs"))), ops));
			fks.put(Fk.Fk(en, "object"),
					new Pair<>(Collections.singletonMap(Var.Var("c"), Term.Gen(Var.Var("rt"))), ops));
			fks2.put(Fk.Fk(en, "subject"), Collections.emptyMap());
			fks2.put(Fk.Fk(en, "object"), Collections.emptyMap());

		}
		for (Entry<Att, Pair<En, Ty>> att : relOlog.atts.entrySet()) {
			En en = En.En(att.getKey().str + "_" + att.getValue().first.str);
			col.getEns().add(en);
			col.fks().put(Fk.Fk(en, "subject"), new Pair<>(en, att.getValue().first));
			col.atts().put(Att.Att(en, "object"), new Pair<>(en, att.getValue().second));

			Map<Var, Chc<En, Ty>> ctx = new THashMap<>();
			ctx.put(Var.Var("r"), Chc.inLeft(En.En("R")));
			ctx.put(Var.Var("rs"), Chc.inLeft(En.En("R")));
			List<Eq<Ty, En, Sym, Fk, Att, Var, Var>> eqs = new LinkedList<>();
			eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(Var.Var("r"))),
					Term.Obj(att.getKey().str, Ty.Ty("Dom"))));
			eqs.add(new Eq<>(null, Term.Att(predatt, Term.Gen(Var.Var("rs"))),
					Term.Obj("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", Ty.Ty("Dom"))));
			eqs.add(new Eq<>(null, Term.Att(obatt, Term.Gen(Var.Var("rs"))),
					Term.Obj(att.getValue().first.str, Ty.Ty("Dom"))));
			eqs.add(new Eq<>(null, Term.Att(subatt, Term.Gen(Var.Var("r"))),
					Term.Att(subatt, Term.Gen(Var.Var("rs")))));

			ens.put(en, new Triple<>(ctx, eqs, ops));
			fks.put(Fk.Fk(en, "subject"),
					new Pair<>(Collections.singletonMap(Var.Var("c"), Term.Gen(Var.Var("rs"))), ops));
			atts.put(Att.Att(en, "object"), Chc.inLeft(Term.Att(obatt, Term.Gen(Var.Var("r")))));
			fks2.put(Fk.Fk(en, "subject"), Collections.emptyMap());

		}

		Schema<Ty, En, Sym, Fk, Att> dst = new Schema<Ty, En, Sym, Fk, Att>(src.typeSide, col, ops);

		return new Query<Ty, En, Sym, Fk, Att, En, Fk, Att>(Collections.emptyMap(), Collections.emptyMap(), ens, atts,
				fks, fks2, src, dst, ops);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rel == null) ? 0 : rel.hashCode());
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
		QueryExpSpanify other = (QueryExpSpanify) obj;
		if (rel == null) {
			if (other.rel != null)
				return false;
		} else if (!rel.equals(other.rel))
			return false;
		return true;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return rel.deps();
	}

	@Override
	public String toString() {
		return "spanify " + rel;
	}

}
