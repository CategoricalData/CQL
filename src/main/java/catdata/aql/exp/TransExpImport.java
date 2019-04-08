package catdata.aql.exp;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.fdm.LiteralTransform;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public abstract class TransExpImport<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2, Handle>
		extends TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> implements Raw {

	private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return raw;
	}

	public final InstExp<Gen1, Sk1, X1, Y1> src;
	public final InstExp<Gen2, Sk2, X2, Y2> dst;

	public final Map<String, String> options;

	public final Map<String, String> map;

	@Override
	public Map<String, String> options() {
		return options;
	}

	public TransExpImport(InstExp<Gen1, Sk1, X1, Y1> src, InstExp<Gen2, Sk2, X2, Y2> dst,
			List<Pair<LocStr, String>> map, List<Pair<String, String>> options) {
		this.src = src;
		this.dst = dst;
		this.options = Util.toMapSafely(options);
		this.map = Util.toMapSafely(LocStr.set2(map));

		List<InteriorLabel<Object>> f = new LinkedList<>();
		for (Pair<LocStr, String> p : map) {
			f.add(new InteriorLabel<>("imports", new Pair<>(p.first.str, p.second), p.first.loc,
					x -> x.first + " -> " + x.second).conv());
		}
		raw.put("imports", f);
	}

	protected Map<Gen1, Term<Void, En, Void, Fk, Void, Gen2, Void>> gens;
	protected Map<Sk1, Term<Ty, En, Sym, Fk, Att, Gen2, Sk2>> sks;

	protected AqlOptions op;
	protected Boolean dontValidateEqs;
	protected boolean labelledNulls;

	@Override
	public Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> eval0(AqlEnv env, boolean isC) {
		Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src0 = src.eval(env, isC);
		Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> dst0 = dst.eval(env, isC);
		if (!src0.schema().equals(dst0.schema())) {
			throw new RuntimeException(
					"Schema of instance source is " + src0 + " but schema of target instance is " + dst0);
		}
		Schema<Ty, En, Sym, Fk, Att> sch = src0.schema();

		for (String o : map.keySet()) {
			if (!sch.ens.contains(En.En(o))) {
				throw new RuntimeException("there is an import for " + o + ", which is not an entity in the schema ");
			}
		}
		if (isC) {
			throw new IgnoreException();
		}
		gens = new THashMap<>();
		sks = new THashMap<>();

		op = new AqlOptions(options, null, env.defaults);
		dontValidateEqs = (Boolean) op.getOrDefault(AqlOption.dont_validate_unsafe);
		boolean autoMapNulls = (Boolean) op.getOrDefault(AqlOption.map_nulls_arbitrarily_unsafe);

		for (Sk1 sk : src0.sks().keySet()) {
			Ty ty = src0.sks().get(sk);
			Set<Sk2> xxx = Util.revS(dst0.sks()).get(ty);
			if (xxx.isEmpty()) {
				throw new RuntimeException("Cannot map null " + sk
						+ " to target instance because target instance has no nulls at type " + ty);
			}
			if (xxx.size() > 1) {
				if (autoMapNulls) {
					Sk2 sk2 = Util.get0X(xxx);
					sks.put(sk, Term.Sk(sk2));
				} else {
					throw new RuntimeException("Cannot automatically map null " + sk
							+ " to target instance because target instance has " + xxx.size() + " nulls at type " + ty
							+ ". Possible solution: add options map_nulls_arbitrarily_unsafe = true");
				}
			} else {
				Sk2 sk2 = Util.get0(xxx);
				sks.put(sk, Term.Sk(sk2));
			}

		}

		try {
			Handle h = start(sch);

			for (En en : sch.ens) {
				if (map.containsKey(en.str)) {
					processEn(en, sch, h, map.get(en.str));
				}
			}

			stop(h);
		} catch (Exception exn) {
			// exn.printStackTrace();
			throw new RuntimeException(exn); // .getMessage() + "\n\n" + getHelpStr());
		}

		return new LiteralTransform<>(gens, sks, src0, dst0, dontValidateEqs);
	}

	protected abstract String getHelpStr();

	protected abstract void stop(Handle h) throws Exception;

	protected abstract void processEn(En en, Schema<Ty, En, Sym, Fk, Att> sch, Handle h, String q) throws Exception;

	protected abstract Handle start(Schema<Ty, En, Sym, Fk, Att> sch) throws Exception;

	@Override
	public Collection<Pair<String, Kind>> deps() {
		Set<Pair<String, Kind>> ret = new THashSet<>();
		ret.addAll(src.deps());
		ret.addAll(dst.deps());
		return ret;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TransExpImport)) // TODO aql note!!!!
			return false;
		TransExpImport<?, ?, ?, ?, ?, ?, ?, ?, ?> other = (TransExpImport<?, ?, ?, ?, ?, ?, ?, ?, ?>) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		return true;
	}

	@Override
	public Pair<InstExp<Gen1, Sk1, X1, Y1>, InstExp<Gen2, Sk2, X2, Y2>> type(AqlTyping G) {
		SchExp s = src.type(G);
		SchExp t = dst.type(G);
		if (!G.eq(s, t)) {
			throw new RuntimeException(
					"Source instance of transform has schema\n" + s + " \n\n but target instance has schema\n" + t);
		}
		return new Pair<>(src, dst);
	}

}
