package catdata.aql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.It.ID;
import catdata.aql.fdm.SlowInitialAlgebra;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class Frozen<Ty, En1, Sym, Fk1, Att1>
		extends Instance<Ty, En1, Sym, Fk1, Att1, Var, Var, ID, Chc<Var, Pair<ID, Att1>>> {

	public final Map<Var, En1> gens;
	public final Map<Var, Ty> sks;

	public final Set<Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> eqs;
	public final Schema<Ty, En1, Sym, Fk1, Att1> schema;

	private DP<Ty, En1, Sym, Fk1, Att1, Var, Var> dp;

	public final AqlOptions options;
	public final List<Var> order;
	
	public Frozen(Map<Var, En1> gens,  Map<Var, Ty> sks, List<Var> order,
			Set<Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> eqs, Schema<Ty, En1, Sym, Fk1, Att1> schema,
			AqlOptions options) {
		Util.assertNotNull(options);
		this.order = order;
		this.gens = gens;
		this.sks = sks;
		this.eqs = eqs;
		this.schema = schema;
		this.options = options;

		validateNoTalg();
	}

	public Frozen(Map<Var, Ty> params, Map<Var, Chc<En1, Ty>> gens,
			Collection<Eq<Ty, En1, Sym, Fk1, Att1, Var, Var>> eqs, Schema<Ty, En1, Sym, Fk1, Att1> schema,
			AqlOptions options) {
		Util.assertNotNull(options);
		this.gens = new THashMap<>();
		this.sks = new THashMap<>(params);
		this.sks.putAll(params);
		this.order = (new LinkedList<>());
		for (Var v : gens.keySet()) {
			Chc<En1, Ty> t = gens.get(v);
			order.add(v);
			if (t.left) {
				this.gens.put(v, t.l);
			} else {
				this.sks.put(v, t.r);
			}
		}
		this.eqs = (new THashSet<>());
		for (Eq<Ty, En1, Sym, Fk1, Att1, Var, Var> x : eqs) {
			this.eqs.add(new Pair<>(x.lhs, x.rhs));
		}
		this.schema = schema;
		this.options = options;

		validateNoTalg();
	}

	@Override
	public Schema<Ty, En1, Sym, Fk1, Att1> schema() {
		return schema;
	}

	@Override
	public Map<Var, En1> gens() {
		return gens;
	}

	@Override
	public Map<Var, Ty> sks() {
		return sks;
	}

	@Override
	public Set<Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>>> eqs() {
		return eqs;
	}

	@Override
	public synchronized DP<Ty, En1, Sym, Fk1, Att1, Var, Var> dp() {
		if (dp == null) {
			dp = AqlProver.createInstance(options, collage(), schema);
			return dp;
		}
		return dp;
	}

	private SlowInitialAlgebra<Ty, En1, Sym, Fk1, Att1, Var, Var, ID> hidden;

	@Override
	public synchronized Algebra<Ty, En1, Sym, Fk1, Att1, Var, Var, ID, Chc<Var, Pair<ID, Att1>>> algebra() {
		if (hidden != null) {
			return hidden;
		}
		hidden = new SlowInitialAlgebra<>(dp(), schema, collage(), new It(), x -> x.toString(), x -> x.toString(),
				options);
		return hidden;
	}

	@Override
	public boolean requireConsistency() {
		return false;
	}

	@Override
	public boolean allowUnsafeJava() {
		return true;
	}

	private <Gen, Sk, X, Y> float estimateCost(List<Var> plan, Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I,
			Map<Pair<Var, Var>, Float> selectivities) {
		if (plan.isEmpty()) {
			return 0;
		} else if (plan.size() == 1) {
			if (sks.containsKey(plan.get(0))) {
				return Integer.MAX_VALUE / 4;
			}
			return I.algebra().size(gens.get(plan.get(0)));
		}
		float cost;
		if (gens.containsKey(plan.get(0))) {
			cost = I.algebra().size(gens.get(plan.get(0)));
		} else {
			cost = Integer.MAX_VALUE / 4;
		}
		List<Var> vl = new LinkedList<>();
		vl.add(plan.get(0));
		for (int i = 1; i < plan.size() - 1; i++) {
			Var vr = plan.get(i);
			float cost2, sel;
			if (sks.containsKey(plan.get(i))) {
				cost2 = Integer.MAX_VALUE / 4;
				sel = 1;
			} else {
				cost2 = I.algebra().size(gens.get(vr));
				sel = estimateSelectivity(vl, vr, selectivities);
			}
			cost *= (sel * cost2);
			vl.add(vr);
		}
		return cost;
	}

	private Iterable<List<Var>> generatePlans() {
		return Util.permutationsOf(new ArrayList<>(Util.union(sks.keySet(), gens.keySet())));
	}

	private Map<Pair<Var, Var>, Float> estimateSelectivities() {
		Map<Pair<Var, Var>, Float> ret = new THashMap<>();
		for (Var v1 : gens().keySet()) {
			for (Var v2 : gens().keySet()) {
				ret.put(new Pair<>(v1, v2), 1f);
			}
		}
		for (Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> eq : eqs) {
			Set<Var> l = new THashSet<>();
			Set<Var> r = new THashSet<>();
			eq.first.gens(l);
			eq.second.gens(r);
			for (Var v : l) {
				for (Var u : r) {
					ret.put(new Pair<>(v, u), ret.get(new Pair<>(v, u)) * .5f);
					ret.put(new Pair<>(u, v), ret.get(new Pair<>(u, v)) * .5f);
				}
			}
		}
		return ret;
	}

	private float estimateSelectivity(List<Var> l, Var v, Map<Pair<Var, Var>, Float> sel) {
		if (!sel.containsKey(new Pair<>(v, v))) {
			return 1;
		}
		if (l.isEmpty()) {
			return sel.get(new Pair<>(v, v));
		}
		float ret = sel.get(new Pair<>(v, v));
		for (Var u : l) {
			if (sel.containsKey(new Pair<>(u, v))) {
				ret *= sel.get(new Pair<>(u, v));
			}
		}
		return ret;
	}

	public <Gen, Sk, X, Y> List<Var> order(AqlOptions options, Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I) {
		if (!(Boolean) options.getOrDefault(AqlOption.eval_reorder_joins)
				|| gens().size() > (Integer) options.getOrDefault(AqlOption.eval_max_plan_depth)) {
			return new ArrayList<>(Util.union(gens.keySet(), sks.keySet()));
		}
		Map<Pair<Var, Var>, Float> selectivities = estimateSelectivities();
		if (gens().isEmpty() && sks().isEmpty()) {
			return Collections.emptyList();
		}
		List<Var> lowest_plan = null;
		float lowest_cost = -1;
		for (List<Var> plan : generatePlans()) {
			float cost = estimateCost(plan, I, selectivities);
			if (lowest_plan == null || cost < lowest_cost) {
				lowest_plan = plan;
				lowest_cost = cost;
			}
		}
		return lowest_plan;
	}

}