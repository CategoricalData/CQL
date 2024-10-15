package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.InteriorLabel;
import catdata.LocException;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Collage;
import catdata.cql.Eq;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import catdata.cql.Query.Agg;
import catdata.cql.exp.QueryExpRaw.Block;
import catdata.cql.exp.QueryExpRaw.PreAgg;
import catdata.cql.exp.QueryExpRaw.PreBlock;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class QueryExpRawSimple extends QueryExp implements Raw {

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		src.map(f);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((block == null) ? 0 : block.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		return result;
	}

	public <R, P, E extends Exception> R accept(P params, QueryExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QueryExpRawSimple))
			return false;
		QueryExpRawSimple other = (QueryExpRawSimple) obj;
		if (block == null) {
			if (other.block != null)
				return false;
		} else if (!block.equals(other.block))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		return true;
	}

	private final SchExp src;

	private final Optional<Block> block;

	public QueryExpRawSimple(SchExp src, Integer i, PreBlock block) {
		this.src = src;
		this.block = Optional.of(new Block(block, new LocStr(i, ""), block.star));
	}

	public QueryExpRawSimple(SchExp src) {
		this.src = src;
		this.block = Optional.empty();
	}

	public QueryExpRawSimple(SchExp src, Block block) {
		this.src = src;
		this.block = Optional.of(block);
	}

	@Override
	public Map<String, String> options() {
		if (block.isEmpty()) {
			return Collections.emptyMap();
		}
		return block.get().options;
	}

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		if (block.isEmpty()) {
			return Collections.emptyMap();
		}
		return block.get().raw;
	}

	@Override
	public Pair<SchExp, SchExp> type(AqlTyping G) {
		return new Pair<>(src, new SchExpCod(this));
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return src.deps();
	}

	// TODO aql merge with queryexpraw
	@Override
	public Query<String, String, Sym, Fk, Att, String, Fk, Att> eval0(AqlEnv env, boolean isC) {
		Schema<String, String, Sym, Fk, Att> src0 = src.eval(env, isC);
		Collage<String, String, Sym, Fk, Att, Void, Void> srcCol = src0.collage();
		if (block.isEmpty()) {
			Util.anomaly();
		}
		AqlOptions ops = new AqlOptions(block.get().options, env.defaults);

		String q = (String) ops.getOrDefault(AqlOption.simple_query_entity);

		String EEn = (q);
		block.get().en = EEn;

		Map<String, Triple<LinkedHashMap<String, Chc<String, String>>, Collection<Eq<String, String, Sym, Fk, Att, String, String>>, AqlOptions>> ens0 = new THashMap<>();
		Map<Att, Chc<Term<String, String, Sym, Fk, Att, String, String>, Agg<String, String, Sym, Fk, Att>>> atts0 = new THashMap<>();

		Map<String, Collage<String, String, Sym, Fk, Att, String, String>> cols = new THashMap<>();

		QueryExpRaw.processBlock(block.get().options, env, src0, ens0, cols, block.get(), Collections.emptyMap());

		Collage<String, String, Sym, Fk, Att, Void, Void> colForDst = new CCollage<>(src0.typeSide.collage());
		colForDst.getEns().add(EEn);
		for (Pair<Att, Chc<RawTerm, PreAgg>> p : block.get().atts) {

			if (p.second.left) {
				Map<String, Chc<String, String>> s = Util.inRight((cols.get(EEn).gens()));
				Term<String, String, Sym, Fk, Att, String, String> term = RawTerm.infer1x(s, p.second.l, p.second.l,
						null, srcCol.convert(), "", src0.typeSide.js).second;
				Chc<String, String> ty = srcCol.type(Util.map(s, (k, v) -> new Pair<>((k), v)), term.convert());
				if (!ty.left) {
					throw new LocException(find("attributes", p),
							"In return clause for " + p.first + ", the type is " + ty.r + ", which is an entity.");
				}
				colForDst.atts().put(p.first, new Pair<>(EEn, ty.l));
			} else {
				Util.anomaly();
			}
		}
		if (block.get().star) {
			for (Pair<String, String> x : block.get().gens) {
				if (src0.ens.contains((x.second))) {
					for (Att att : src0.attsFrom((x.second))) {
						String ty = src0.atts.get(att).second;
						colForDst.atts().put(Att.Att(EEn, x.first + "_" + att), new Pair<>(EEn, ty));
						atts0.put(Att.Att(EEn, x.first + "_" + att), Chc.inLeft(Term.Att(att, Term.Gen(x.first))));
					}
				}
			}
		}

		Schema<String, String, Sym, Fk, Att> dst0 = new Schema<>(src0.typeSide, colForDst, ops);

		for (Pair<Att, Chc<RawTerm, PreAgg>> p : block.get().atts) {
			Set<String> set = new THashSet<>(block.get().gens.size());
			for (Pair<String, String> z : block.get().gens) {
				if (src0.typeSide.tys.contains((z.second))) {
					set.add(z.first);
				}
			}
			try {
				QueryExpRaw.processAtt(src0, dst0, ens0, atts0, cols, p, Collections.emptyMap(), set);
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				throw new LocException(find("attributes", p),
						"In return clause for " + p.first + ", " + ex.getMessage());
			}
		}

		return Query.makeQuery(ens0, atts0, Collections.emptyMap(), Collections.emptyMap(), src0, dst0, ops);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("simple : ").append(src).append(" {\n");
		List<String> temp = new LinkedList<>();

		if (!block.isEmpty()) {
			temp.add(block.get().toString2());
			sb.append("\t\t").append(Util.sep(temp, "\n\n\t\t")).append("\n");

		}

		return sb.toString().trim() + "}";
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.dont_validate_unsafe);
		set.add(AqlOption.query_remove_redundancy);
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.add(AqlOption.simple_query_entity);
		set.addAll(AqlOptions.proverOptionNames());

	}

}
