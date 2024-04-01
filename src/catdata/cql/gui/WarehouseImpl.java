package catdata.cql.gui;



import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import catdata.Chc;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.ColimitSchema;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Instance;
import catdata.cql.Query;
import catdata.cql.SqlTypeSide;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.Att;
import catdata.cql.exp.CombinatorParser;
import catdata.cql.exp.Fk;
import catdata.cql.exp.PragmaExpCheck2;
import catdata.cql.exp.QueryExpDeltaEval;
import catdata.cql.exp.Sym;
import catdata.cql.exp.EdsExpRaw.EdExpRaw;
import catdata.cql.fdm.CoprodInstance;
import catdata.cql.fdm.DeltaInstance;
import catdata.cql.fdm.DistinctInstance;
import catdata.cql.fdm.EvalInstance;
import catdata.cql.fdm.SigmaDeltaUnitTransform;
import catdata.cql.fdm.SigmaInstance;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class WarehouseImpl extends Warehouse<String, String> {

	private Map<String, Constraints> edsFwd = null;
	private ColimitSchema<String> univ = null;
	public Transform<String, String, Sym, Fk, Att, Object, Object, Object, Object, Object, Object, Object, Object> chase_return = null;
	private Map<String, Instance> fwds = null;
	private Constraints rowLinks = null;
	private boolean push = false;

	private static AqlOptions stringToOptions(String s) {
		var x = CombinatorParser.parseOptions(s);
		return new AqlOptions(Util.toMapSafely(x),AqlOptions.initialOptions);
	}
	public WarehouseImpl(String prover, String options, boolean p) {
		super(new AqlOptions(stringToOptions(options), AqlOption.e_path, prover));
		push = p;
	}

	public void run() {

		var vv = Triple.proj(sources);

		var schs = vv.first;
		var eds = vv.second;
		var insts = vv.third;

		univ = new ColimitSchema<String>(typeside, schs, links.enLinks, links.colLinks.values(), options);

		var l = new LinkedList<ED>();
		for (EdExpRaw ed : links.rowLinks.values()) {
			l.add(ed.eval(univ.schemaStr, options));
		}

		edsFwd = new HashMap<>();
		 
		 for (var v : getSources().keySet()) {
		//	System.out.println("trying " + v + " in " + getSources().keySet() + " and " + univ.mappingsStr.keySet());
			if (!eds.containsKey(v) || !univ.mappingsStr.containsKey(v)) {
				continue;
			}
			if (null == eds.get(v) || null == univ.mappingsStr.get(v)) {
				continue;
			}
			var xxx = eds.get(v).sigma(univ.mappingsStr.get(v), options);
			edsFwd.put(v, xxx);
			if (push) l.addAll(xxx.eds);
		 
		}
		rowLinks = new Constraints(univ.schemaStr, l, options);

		fwds = new HashMap<>();
		for (var v : getSources().keySet()) {
			fwds.put(v, new SigmaInstance<>(univ.mappingsStr.get(v), insts.get(v), options));
		}

		var ci = new CoprodInstance(fwds, univ.schemaStr, false, false);

		chase_return = (Transform) rowLinks.chase(ci, options).second;

	}

	@Override
	public String toString() {
		return "WarehouseImpl [edsFwd=" + edsFwd + ", univ=" + univ + ", chase_return=" + chase_return + ", fwds="
				+ fwds + ", rowLinks=" + rowLinks + ", push=" + push + "]";
	}
	@Override
	public ColimitSchema getColimit() {
		return univ;
	}

	@Override
	public Constraints getFwdConstraints(String name) {
		if (univ == null) {
			Util.anomaly();
		}
		return edsFwd.get(name);
	}

	@Override
	public Query getRoundTripQuery(String n) {
		if (univ == null) {
			Util.anomaly();
		}

		return QueryExpDeltaEval.extracted(options, getInclusionMapping(n));
	}

	@Override
	public Transform getRoundTripUnit(String n) {
		if (univ == null) {
			Util.anomaly();
		}

		return new SigmaDeltaUnitTransform<>(univ.mappingsStr.get(n), this.getSources().get(n).third, options);
	}


	@Override
	public Instance pointToPoint(String s, String t) {
		if (univ == null) {
			Util.anomaly();
		}
		if (getSources().get(t).second == null) {
			throw new RuntimeException("Anomaly " + t);
		}

		return (Instance) getSources().get(t).second
				.chase(new DeltaInstance(this.getInclusionMapping(t), getFwdInstance(s)), options).first;
	}

	@Override
	public Instance getFwdInstance(String n) {
		if (univ == null) {
			Util.anomaly();
		}

		return fwds.get(n);
	}

	@Override
	public Instance getEnriched(String t) {
		if (univ == null) {
			Util.anomaly();
		}

		return (Instance) getSources().get(t).second
				.chase(new DeltaInstance<>(getInclusionMapping(t), getUniversalInstance()), options).first;
	}

	@Override
	public Instance getResult(String name, boolean distinct) {
		if (univ == null) {
			Util.anomaly();
		}
		var ret = new EvalInstance<>(getTargets().get(name).third, getUniversalInstance(), options);
		if (!distinct) {
			return ret;
		}
		return new DistinctInstance<>(ret, options);
	}

	@Override
	public Instance getMasterInstance() {
		if (univ == null) {
			Util.anomaly();
		}

		return new SigmaInstance<>(getMasterSchema(), getUniversalInstance(), options);
	}

	@Override
	public Constraints getUniversalConstraints() {
		if (univ == null) {
			Util.anomaly();
		}
		return rowLinks;
	}

	@Override
	public Transform<String, String, Sym, Fk, Att, Object, Object, Object, Object, Object, Object, Object, Object> getTransformUnionToUniversal() {
		return chase_return;
	}

	@Override
	public Chc<Boolean,String> isTargetConstraintsOk(String q) {
		if (univ == null) {
			Util.anomaly();
		}

		try {
			PragmaExpCheck2.extracted(getTargets().get(q).second, getUniversalConstraints(), getTargets().get(q).third, options);
		} catch (Exception e) {
			return Chc.inRight(e.getMessage());
		}
		return Chc.inLeft(true);
	}
	
	@Override
	public Transform getRoundTripCoUnit(String q) {
		if (univ == null) {
			Util.anomaly();
		}

		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean ready() {
		return univ != null && chase_return != null;
	}

	

}
