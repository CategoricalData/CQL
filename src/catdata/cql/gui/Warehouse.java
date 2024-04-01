package catdata.cql.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import catdata.Chc;
import catdata.Triple;
import catdata.cql.AqlOptions;
import catdata.cql.ColimitSchema;
import catdata.cql.Constraints;
import catdata.cql.Instance;
import catdata.cql.Mapping;
import catdata.cql.Query;
import catdata.cql.Schema;
import catdata.cql.Transform;
import catdata.cql.TypeSide;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;

public abstract class Warehouse<N, T> {

	public Warehouse(AqlOptions options) {
		this.options = options;
	}

	/////////////////////////////////////////////////////////////////////////////////

	protected AqlOptions options;

	public AqlOptions getOptions() {
		return options;
	}

	public void setOptions(AqlOptions options) {
		this.options = options;
	}

	/////////////////////////////////////////////////////////////////////////////////

	protected TypeSide<String, Sym> typeside;

	public TypeSide<String, Sym> getTypeside() {
		return typeside;
	}

	public void setTypeside(TypeSide<String, Sym> t) {
		typeside = t;
	}

	/////////////////////////////////////////////////////////////////////////////////

	protected final Map<N, Triple<Schema<String, String, Sym, Fk, Att>, Constraints, Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>>> sources = new LinkedHashMap<>();

	public void addSources(
			Map<N, Triple<Schema<String, String, Sym, Fk, Att>, Constraints, Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>>> sourcesToAdd) {
		TypeSide ty = null;
		for (var x : sourcesToAdd.entrySet()) {
			if (x.getKey().toString().startsWith("Pre")) continue;
			
			sources.put(x.getKey(), x.getValue());
			TypeSide ty2 = x.getValue().first.typeSide;
			if (ty != null && ty != ty2) {
				throw new RuntimeException("Typeside mismatch");
			} else {
				ty = ty2;
			}
		}
		this.typeside = ty;
	}
	
	public void setSources(
			Map<N, Triple<Schema<String, String, Sym, Fk, Att>, Constraints, Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>>> sourcesToAdd) {
		sources.clear();
		addSources(sourcesToAdd);
	}

	public Map<N, Triple<Schema<String, String, Sym, Fk, Att>, Constraints, Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object>>> getSources() {
		return sources;
	}

	public void removeSources(Collection<N> sourcesToRemove) {
		for (var x : sourcesToRemove) {
			sources.remove(x);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////

	protected final Links<N, T> links = new Links<N, T>(new HashMap<>(), new HashMap<>(), new HashMap<>());

	public void addLinks(Links<N, T> newLinks) {
		links.add(newLinks);
	}
	
	public void setLinks(Links<N, T> newLinks) {
		links.clear();
		addLinks(newLinks);
	}
	

	public void removeLinks(Collection<N> toRemove) {
		links.remove(toRemove);
	}

	public Links<N, T> getLinks() {
		return links;
	}

	/////////////////////////////////////////////////////////////////////////////////

	public abstract void run();

	/////////////////////////////////////////////////////////////////////////////////

	public abstract Query<String, String, Sym, Fk, Att, String, Fk, Att> getRoundTripQuery(N n);

	public abstract Constraints getFwdConstraints(N n);

	public abstract Constraints getUniversalConstraints();

	public abstract ColimitSchema<String> getColimit();

	public Mapping<String, String, Sym, Fk, Att, String, Fk, Att> getInclusionMapping(N n) {
		return getColimit().mappingsStr.get(n);
	}

	public Schema<String, String, Sym, Fk, Att> getUniversalSchema() {
		return getColimit().schemaStr;
	}

	public Mapping<String, String, Sym, Fk, Att, String, Fk, Att> getMasterSchema() {
		return getColimit().fromPsuedo;
	}

	/////////////////////////////////////////////////////////////////////////////////

	public abstract Transform<String, String, Sym, Fk, Att, Object, Object, Object, Object, Object, Object, Object, Object> getRoundTripUnit(
			N n);

	public Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> getUnionInstance() {
		return getTransformUnionToUniversal().src();
	}

	public Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> getUniversalInstance() {
		return getTransformUnionToUniversal().dst();
	}

//	public abstract Transform<String, String, Sym, Fk, Att, Object, Object, Object, Object, Object, Object, Object, Object> getTransformIntoUniversal(
//			N n);

	public abstract Transform<String, String, Sym, Fk, Att, Object, Object, Object, Object, Object, Object, Object, Object> getTransformUnionToUniversal();

	
	public abstract Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> getMasterInstance();

	public abstract Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> getFwdInstance(N n);

	public abstract Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> pointToPoint(N src,
			String tgt);

	public abstract Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> getEnriched(N name);

	/////////////////////////////////////////////////////////

	protected final Map<T, Triple<Schema<String, String, Sym, Fk, Att>, Constraints, Query<String, String, Sym, Fk, Att, String, Fk, Att>>> targets = new HashMap<>();

	public void addTargets(
			Map<T, Triple<Schema<String, String, Sym, Fk, Att>, Constraints, Query<String, String, Sym, Fk, Att, String, Fk, Att>>> w) {
		targets.putAll(w);
	}
	
	public void setTargets(
			Map<T, Triple<Schema<String, String, Sym, Fk, Att>, Constraints, Query<String, String, Sym, Fk, Att, String, Fk, Att>>> w) {
		targets.clear();
		addTargets(w);
	}

	public Map<T, Triple<Schema<String, String, Sym, Fk, Att>, Constraints, Query<String, String, Sym, Fk, Att, String, Fk, Att>>> getTargets() {
		return targets;
	}

	public void removeTargets(Collection<T> qs) {
		for (var t : qs) {
			targets.remove(t);
		}
	}

	/////////////////////////////////////////////////////////

	public abstract Chc<Boolean,String> isTargetConstraintsOk(T q);

	public abstract Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> getResult(T q, boolean distinct);

	public abstract Transform<String, String, Sym, Fk, Att, Object, Object, Object, Object, Object, Object, Object, Object> getRoundTripCoUnit(
			T q);

	public abstract boolean ready();

}