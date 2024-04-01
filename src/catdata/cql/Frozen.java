package catdata.cql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.It.ID;
import catdata.cql.fdm.SlowInitialAlgebra;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class Frozen<Ty, En1, Sym, Fk1, Att1>
    extends Instance<Ty, En1, Sym, Fk1, Att1, String, String, ID, Chc<String, Pair<ID, Att1>>> {

  // private final Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col = new Collage<>();

  public final Map<String, En1> gens;
  public final Map<String, Ty> sks;

  public final Set<Pair<Term<Ty, En1, Sym, Fk1, Att1, String, String>, Term<Ty, En1, Sym, Fk1, Att1, String, String>>> eqs;
  public final Schema<Ty, En1, Sym, Fk1, Att1> schema;

  private DP<Ty, En1, Sym, Fk1, Att1, String, String> dp;

  public final AqlOptions options;
  public final List<String> order;

  public Frozen(Map<String, En1> gens, Map<String, Ty> sks, List<String> order,
      Set<Pair<Term<Ty, En1, Sym, Fk1, Att1, String, String>, Term<Ty, En1, Sym, Fk1, Att1, String, String>>> eqs,
      Schema<Ty, En1, Sym, Fk1, Att1> schema, AqlOptions options) {
    Util.assertNotNull(options);
    this.order = order;
    this.gens = gens;
    this.sks = sks;
    this.eqs = eqs;
    this.schema = schema;
    this.options = options;
    validateNoTalg();
  }

  public Frozen(Map<String, Ty> params, Map<String, Chc<En1, Ty>> gens,
      Collection<Eq<Ty, En1, Sym, Fk1, Att1, String, String>> eqs, Schema<Ty, En1, Sym, Fk1, Att1> schema,
      AqlOptions options, List<String> order) {
    Util.assertNotNull(options);
    this.gens = new LinkedHashMap<>();
    this.sks = new LinkedHashMap<>(params);
    this.sks.putAll(params);
    this.order = order; //new ArrayList<>(order);
    //Collections.reverse(this.order); //TODO sigh  
    for (String v : gens.keySet()) {
      Chc<En1, Ty> t = gens.get(v);
      if (t.left) {
        this.gens.put(v, t.l);
      } else {
        this.sks.put(v, t.r);
      }
    }
    //System.out.println("BBB " + order);
    this.eqs = new THashSet<>(eqs.size());
    for (Eq<Ty, En1, Sym, Fk1, Att1, String, String> x : eqs) {
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
  public IMap<String, En1> gens() {
    return Instance.mapToIMap(gens);
  }

  @Override
  public IMap<String, Ty> sks() {
    return Instance.mapToIMap(sks);
  }

  @Override
  public synchronized void eqs(
      BiConsumer<Term<Ty, En1, Sym, Fk1, Att1, String, String>, Term<Ty, En1, Sym, Fk1, Att1, String, String>> f) {
    eqs.forEach(x -> f.accept(x.first, x.second));
  }

  @Override
  public synchronized DP<Ty, En1, Sym, Fk1, Att1, String, String> dp() {
    if (dp == null) {
      dp = AqlProver.createInstance(options, collage(), schema);
      return dp;
    }
    return dp;
  }

  private SlowInitialAlgebra<Ty, En1, Sym, Fk1, Att1, String, String, ID> hidden;

  @Override
  public synchronized Algebra<Ty, En1, Sym, Fk1, Att1, String, String, ID, Chc<String, Pair<ID, Att1>>> algebra() {
    if (hidden != null) {
      return hidden;
    }
    hidden = new SlowInitialAlgebra<>(x -> dp(), schema, gens, sks, eqs, new It(), x -> x.toString(), x -> x.toString(),
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

  private <Gen, Sk, X, Y> float estimateCost(List<String> plan, Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I,
      Map<Pair<String, String>, Float> selectivities) {
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
    List<String> vl = new LinkedList<>();
    vl.add(plan.get(0));
    for (int i = 1; i < plan.size() - 1; i++) {
      String vr = plan.get(i);
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

  private Iterable<List<String>> generatePlans() {
    return Util.permutationsOf(new ArrayList<>(Util.union(sks.keySet(), gens.keySet())));
  }

  private Map<Pair<String, String>, Float> estimateSelectivities() {
    Map<Pair<String, String>, Float> ret = new THashMap<>();
    gens().keySet((v1) -> {
      gens().keySet((v2) -> {
        ret.put(new Pair<>(v1, v2), 1f);
      });
    });
    for (Pair<Term<Ty, En1, Sym, Fk1, Att1, String, String>, Term<Ty, En1, Sym, Fk1, Att1, String, String>> eq : eqs) {
      Set<String> l = new THashSet<>();
      Set<String> r = new THashSet<>();
      eq.first.gens(l);
      eq.second.gens(r);
      for (String v : l) {
        for (String u : r) {
          ret.put(new Pair<>(v, u), ret.get(new Pair<>(v, u)) * .5f);
          ret.put(new Pair<>(u, v), ret.get(new Pair<>(u, v)) * .5f);
        }
      }
    }
    return ret;
  }

  
  
  private static float estimateSelectivity(List<String> l, String v, Map<Pair<String, String>, Float> sel) {
    if (!sel.containsKey(new Pair<>(v, v))) {
      return 1;
    }
    if (l.isEmpty()) {
      return sel.get(new Pair<>(v, v));
    }
    float ret = sel.get(new Pair<>(v, v));
    for (String u : l) {
      if (sel.containsKey(new Pair<>(u, v))) {
        ret *= sel.get(new Pair<>(u, v));
      }
    }
    return ret;
  }

  public synchronized <Gen, Sk, X, Y> List<String> order(AqlOptions options,
      Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I) {

    if (!(Boolean) options.getOrDefault(AqlOption.eval_reorder_joins)
        || gens().size() > (Integer) options.getOrDefault(AqlOption.eval_max_plan_depth)) {
  return order;  	
//      return new ArrayList<>(Util.union(gens.keySet(), sks.keySet()));
    }
    Map<Pair<String, String>, Float> selectivities = estimateSelectivities();
    if (gens().isEmpty() && sks().isEmpty()) {
      return Collections.emptyList();
    }
    List<String> lowest_plan = null;
    float lowest_cost = -1;
    for (List<String> plan : generatePlans()) {
      float cost = estimateCost(plan, I, selectivities);
      if (lowest_plan == null || cost < lowest_cost) {
        lowest_plan = plan;
        lowest_cost = cost;
      }
    }
    return lowest_plan;
  }

  public Collection<Pair<Term<Ty, En1, Sym, Fk1, Att1, String, String>, Term<Ty, En1, Sym, Fk1, Att1, String, String>>> eqsAsIterable() {
    return eqs;
  }

}