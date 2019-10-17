package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import catdata.Chc;
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
import catdata.aql.SkeletonExtensionalInstance;
import catdata.aql.SkeletonInstanceWrapperInv;
import catdata.aql.Term;
import gnu.trove.map.hash.THashMap;

public final class InstExpRandom extends InstExp<Integer, Integer, Integer, Integer> implements Raw {

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		sch.map(f);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.emptySet();
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return raw;
	}

	public final Map<String, Integer> ens;

	public final Map<String, String> options;

	public final SchExp sch;

	@Override
	public Map<String, String> options() {
		return options;
	}

	public InstExpRandom(SchExp sch, List<Pair<LocStr, String>> ens, List<Pair<String, String>> options) {
		this.ens = Util.toMapSafely(LocStr.set2y(ens, x -> Integer.parseInt(x)));
		this.options = Util.toMapSafely(options);
		this.sch = sch;
		List<InteriorLabel<Object>> f = new LinkedList<>();
		for (Pair<LocStr, String> p : ens) {
			f.add(new InteriorLabel<>("generators", new Pair<>(p.first.str, p.second), p.first.loc,
					x -> x.first + " -> " + x.second).conv());
		}
		raw.put("generators", f);
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return sch.deps();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ens == null) ? 0 : ens.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((sch == null) ? 0 : sch.hashCode());
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
		InstExpRandom other = (InstExpRandom) obj;
		if (ens == null) {
			if (other.ens != null)
				return false;
		} else if (!ens.equals(other.ens))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (sch == null) {
			if (other.sch != null)
				return false;
		} else if (!sch.equals(other.sch))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("random : ").append(sch).append(" {");
		if (ens.size() > 0) {
			sb.append("\ngenerators\n").append(Util.sep(ens, " -> ", "\n"));
		}
		return sb.append("}").toString();
	}

	@Override
	public SchExp type(AqlTyping G) {
		return sch;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.random_seed);
	}

	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Integer, Integer, Integer, Integer> eval0(AqlEnv env,
			boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		AqlOptions ops = new AqlOptions(options, env.defaults);
		int seed = (Integer) ops.getOrDefault(AqlOption.random_seed);
		Random rand = new Random(seed);

		Schema<Ty, En, Sym, Fk, Att> schema = sch.eval(env, false);

		for (En e : schema.ens) {
			if (!ens.containsKey(e.str)) {
				ens.put(e.str, 1);
			}
		}
		for (Ty e : schema.typeSide.tys) {
			if (!ens.containsKey(e.str)) {
				ens.put(e.str, 1);
			}
		}

		Map<En, Integer> en = new THashMap<>();
		for (En x : schema.ens) {
			int y = this.ens.get(x.str);
			en.put(x, y);
		}
		Map<Ty, Integer> ty = new THashMap<>();
		for (Ty x : schema.typeSide.tys) {
			int y = this.ens.get(x.str);
			ty.put(x, y);
		}

		int t = 0;
		Map<En, Integer> m = new THashMap<>();
		Map<Ty, Integer> n = new THashMap<>();
		for (En x : schema.ens) {
			m.put(x, t);
			t += this.ens.get(x.str);
		}
		for (Ty x : schema.typeSide.tys) {
			n.put(x, t);
			t += this.ens.get(x.str);
		}
		Map<Fk, int[]> fks = new THashMap<>();
		Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Integer>[]> atts = new THashMap<>();

		for (En e : schema.ens) {
			int e_size = this.ens.get(e.str);
			for (Fk f : schema.fksFrom(e)) {
				int[] xx = new int[e_size];
				fks.put(f, xx);
				En y = schema.fks.get(f).second;
				int size0 = this.ens.get(y.str);
				for (int a = 0; a < e_size; a++) {
					xx[a] = rand.nextInt(size0) + m.get(y);
				}
			}
			for (Att f : schema.attsFrom(e)) {
				@SuppressWarnings("unchecked")
				Term<Ty, Void, Sym, Void, Void, Void, Integer>[] xx = new Term[e_size];
				atts.put(f, xx);
				Ty y = schema.atts.get(f).second;
				int size0 = this.ens.get(y.str);
				for (int a = 0; a < e_size; a++) {
					xx[a] = Term.Sk(rand.nextInt(size0) + n.get(y));
				}
			}
		}

		@SuppressWarnings("unchecked")
		Term<Ty, Void, Sym, Void, Void, Void, Integer>[][] talg_eqs = new Term[0][];
		BiPredicate<Term<Ty, Void, Sym, Void, Void, Void, Integer>, Term<Ty, Void, Sym, Void, Void, Void, Integer>> talg_dp = (
				x, y) -> Util.anomaly();

		SkeletonExtensionalInstance<Ty, En, Sym, Fk, Att> J = new SkeletonExtensionalInstance<>(schema, en, ty, m, n,
				fks, atts, talg_eqs, talg_dp, x -> Chc.inLeft(x), ops);
		return new SkeletonInstanceWrapperInv<>(J);

	}

}
