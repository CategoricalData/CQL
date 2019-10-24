package catdata.aql.exp;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.InteriorLabel;
import catdata.LocException;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.RawTerm;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.fdm.LiteralTransform;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class TransExpRaw extends TransExp<Gen, Sk, Gen, Sk, String, String, String, String> implements Raw {

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Exp<?>> imports() {
		return (Collection<Exp<?>>) (Object) imports;
	}

	Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return raw;
	}

	public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		Set<Pair<String, Kind>> ret = new THashSet<>(src.deps());
		ret.addAll(dst.deps());
		for (TransExp<?, ?, ?, ?, ?, ?, ?, ?> x : imports) {
			ret.addAll(x.deps());
		}
		return ret;
	}

	public final InstExp<Gen, Sk, String, String> src;
	public final InstExp<Gen, Sk, String, String> dst;

	public final Set<TransExp<?, ?, ?, ?, ?, ?, ?, ?>> imports;

	public final Set<Pair<String, RawTerm>> gens;

	public final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	@Override
	public String makeString() {
		final StringBuilder sb = new StringBuilder().append("literal : ").append(src).append(" -> ").append(dst)
				.append(" {");

		if (!imports.isEmpty()) {
			sb.append("\n\timports");
			sb.append("\n\t\t").append(Util.sep(imports, " ")).append("\n");
		}

		List<String> temp = new LinkedList<>();

		if (!gens.isEmpty()) {
			sb.append("\n\tgenerators");

			for (Pair<String, RawTerm> x : Util.alphabetical(gens)) {
				temp.add(x.first + " -> " + x.second);
			}

			sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
		}

		if (!options.isEmpty()) {
			sb.append("\n\toptions");
			temp = new LinkedList<>();
			for (Entry<String, String> sym : options.entrySet()) {
				temp.add(sym.getKey() + " = " + sym.getValue());
			}

			sb.append("\n\t\t").append(Util.sep(temp, "\n\t\t")).append("\n");
		}

		return sb.append("}").toString();
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((gens == null) ? 0 : gens.hashCode());
		result = prime * result + ((imports == null) ? 0 : imports.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
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
		TransExpRaw other = (TransExpRaw) obj;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		if (gens == null) {
			if (other.gens != null)
				return false;
		} else if (!gens.equals(other.gens))
			return false;
		if (imports == null) {
			if (other.imports != null)
				return false;
		} else if (!imports.equals(other.imports))
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
		return true;
	}

	@SuppressWarnings("unchecked")
	public TransExpRaw(InstExp<?, ?, ?, ?> src, InstExp<?, ?, ?, ?> dst, List<TransExp<?, ?, ?, ?, ?, ?, ?, ?>> imports,
			List<Pair<LocStr, RawTerm>> gens, List<Pair<String, String>> options) {
		this.src = (InstExp<Gen, Sk, String, String>) src;
		this.dst = (InstExp<Gen, Sk, String, String>) dst;
		this.imports = new THashSet<>(imports);
		this.gens = LocStr.set2(gens);
		Util.toMapSafely(this.gens); // do here rather than wait
		this.options = Util.toMapSafely(options);

		// List<InteriorLabel<Object>> t = InteriorLabel.imports("imports", imports);
		// raw.put("imports", t);

		List<InteriorLabel<Object>> f = new LinkedList<>();
		for (Pair<LocStr, RawTerm> p : gens) {
			f.add(new InteriorLabel<>("generators", new Pair<>(p.first.str, p.second), p.first.loc,
					x -> x.first + " -> " + x.second).conv());
		}
		raw.put("generators", f);
	}

	@Override
	public synchronized Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, String, String, String, String> eval0(
			AqlEnv env, boolean isC) {
		Instance<Ty, En, Sym, Fk, Att, Gen, Sk, String, String> src0 = src.eval(env, isC);
		Instance<Ty, En, Sym, Fk, Att, Gen, Sk, String, String> dst0 = dst.eval(env, isC);
		// Collage<String, String, String, String, String, Void, Void> scol = new
		// Collage<>(src0);
		Collage<Ty, En, Sym, Fk, Att, Gen, Sk> dcol = new CCollage<>(); 

		Map<Gen, Term<Void, En, Void, Fk, Void, Gen, Void>> gens0 = new THashMap<>();
		Map<Sk, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> sks0 = new THashMap<>();
		for (TransExp<?, ?, ?, ?, ?, ?, ?, ?> k : imports) {
			@SuppressWarnings("unchecked")
			Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, String, String, String, String> v = (Transform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, String, String, String, String>) k
					.eval(env, isC);
			//Util.putAllSafely(gens0, v.gens());
			//Util.putAllSafely(sks0, v.sks());
			Util.anomaly();
		}

		for (Pair<String, RawTerm> gen : gens) {
			try {
				RawTerm term = gen.second;
				Map<String, Chc<Ty, En>> ctx = new THashMap<>();

				Chc<Ty, En> required;
				if (src0.gens().containsKey(Gen.Gen(gen.first)) && src0.sks().containsKey(Sk.Sk(gen.first))) {
					throw new RuntimeException(gen.first + " is ambiguous");
				} else if (src0.gens().containsKey(Gen.Gen(gen.first))) {
					required = Chc.inRight(src0.gens().get(Gen.Gen(gen.first)));
				} else if (src0.sks().containsKey(Sk.Sk(gen.first))) {
					required = Chc.inLeft(src0.sks().get(Sk.Sk(gen.first)));
				} else {
					throw new RuntimeException(gen.first + " is not a source generator/labelled null");
				}

				Term<Ty, En, Sym, Fk, Att, Gen, Sk> term0 = RawTerm.infer1x(ctx, term, null, required, dcol, "",
						src0.schema().typeSide.js).second;

				if (required.left) {
					Util.putSafely(sks0, Sk.Sk(gen.first), term0.convert());
				} else {
					Util.putSafely(gens0, Gen.Gen(gen.first), term0.convert());
				}
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				throw new LocException(find("generators", gen),
						"In transform for " + gen.first + ", " + ex.getMessage());
			}
		}

		AqlOptions ops = new AqlOptions(options, env.defaults);

		LiteralTransform<Ty, En, Sym, Fk, Att, Gen, Sk, Gen, Sk, String, String, String, String> ret = new LiteralTransform<>(
				(k,t)->gens0.get(k), (k,t)->sks0.get(k), src0, dst0, (Boolean) ops.getOrDefault(AqlOption.dont_validate_unsafe));
		return ret;
	}

	@Override
	public Pair<InstExp<Gen, Sk, String, String>, InstExp<Gen, Sk, String, String>> type(AqlTyping G) {
		SchExp a = src.type(G);
		SchExp b = dst.type(G);
		if (!G.eq(a, b)) {
			throw new RuntimeException("Mismatched schemas:\n\n" + a + "\n\nand\n\n" + b);
		}
		return new Pair<>(src, dst);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.dont_validate_unsafe);
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		src.map(f);
		dst.map(f);
	}

}
