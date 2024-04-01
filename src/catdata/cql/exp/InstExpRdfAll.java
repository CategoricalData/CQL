package catdata.cql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.RDFDataMgr;

import catdata.Null;
import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.ImportAlgebra;
import catdata.cql.fdm.SaturatedInstance;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class InstExpRdfAll extends InstExp<String, Null<?>, String, Null<?>> {

	private final Map<String, String> options;

	private final String jdbcString;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.emptySet();
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public InstExpRdfAll(String jdbcString, List<Pair<String, String>> options) {
		this.jdbcString = jdbcString;
		this.options = Util.toMapSafely(options);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.jena_reasoner);
	}

	public Schema<String, String, Sym, Fk, Att> makeSchema(AqlEnv env, AqlOptions ops) {
		return makeSch().eval(env, false);
	}

	@Override
	public synchronized Instance<String, String, Sym, Fk, Att, String, Null<?>, String, Null<?>> eval0(AqlEnv env,
			boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		AqlOptions ops = new AqlOptions(options, env.defaults);

		Map<String, Collection<String>> ens0 = Collections.synchronizedMap(new THashMap<>(2));

		Map<String, Map<String, Map<Fk, String>>> fks0x = Collections.synchronizedMap(new THashMap<>(2, 2));
		Map<String, Map<String, Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>>>> atts0x = Collections
				.synchronizedMap(new THashMap<>(2, 2));

		Map<Null<?>, Term<String, String, Sym, Fk, Att, String, Null<?>>> extraRepr = null;

		Model model = RDFDataMgr.loadModel(jdbcString);

		String rstr = (String) ops.getOrDefault(AqlOption.jena_reasoner);
		Reasoner reasoner;
		if (rstr.equals("getOWLReasoner")) {
			reasoner = ReasonerRegistry.getOWLReasoner();
		} else if (rstr.equals("getTransitiveReasoner")) {
			reasoner = ReasonerRegistry.getTransitiveReasoner();
		} else if (rstr.equals("getRDFSReasoner")) {
			reasoner = ReasonerRegistry.getRDFSReasoner();
		} else if (rstr.equals("getRDFSSimpleReasoner")) {
			reasoner = ReasonerRegistry.getRDFSSimpleReasoner();
		} else if (rstr.equals("getOWLMiniReasoner")) {
			reasoner = ReasonerRegistry.getOWLMiniReasoner();
		} else if (rstr.equals("getOWLMicroReasoner")) {
			reasoner = ReasonerRegistry.getOWLMicroReasoner();
		} else if (rstr.isBlank()) {
			reasoner = null;
		} else {
			throw new RuntimeException("Not a jena reasoner: " + rstr
					+ ".  Expecting one of getTransitiveReasoner, getRDFSReasoner, getRDFSSimpleReasoner, getOWLReasoner, getOWLMiniReasoner, getOWLMicroReasoner, or empty string");
		}
		if (reasoner != null) {
			reasoner = reasoner.bindSchema(model);
			InfModel model2 = ModelFactory.createInfModel(reasoner, model);
			var validity = model2.validate();
			if (!validity.isValid()) {
				StringBuffer sb = new StringBuffer("Conflicts: ");
				for (Iterator<?> i = validity.getReports(); i.hasNext();) {
					sb.append(i.toString());
					sb.append("\n");
				}
				throw new RuntimeException("Not valid.  " + sb);
			}
			model = model2;
		}

		// list the statements in the Model
		StmtIterator iter = model.listStatements();

		Map<String, Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>>> atts0 = Collections
				.synchronizedMap(new THashMap<>((int) model.size(), 2));
		atts0x.put(("R"), atts0);
		Map<String, Collection<Null<?>>> tys0 = Collections.synchronizedMap(new THashMap<>(2, 2));
		tys0.put(("Dom"), Collections.synchronizedSet(new THashSet<>(1024 * 16)));

		int i = 0;
		var ll = new ArrayList<String>((int) model.size());
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement(); // get next statement
			Resource subject = stmt.getSubject(); // get the subject
			Property predicate = stmt.getPredicate(); // get the predicate
			RDFNode object = stmt.getObject(); // get the object
			Map<Att, Term<String, Void, Sym, Void, Void, Void, Null<?>>> map = Collections
					.synchronizedMap(new THashMap<>(3, 2));

			if (subject.isAnon()) {
				var v = new Null<>(subject.toString());
				tys0.get(("Dom")).add(v);
				map.put(Att.Att(("R"), "subject"), Term.Sk(v));
			} else {
				if (subject.isLiteral()) {
					map.put(Att.Att(("R"), "subject"), Term.Obj(subject.asLiteral().getValue(), ("Dom")));
				} else if (subject.isURIResource()) {
					map.put(Att.Att(("R"), "subject"), Term.Obj(subject.asResource().toString(), ("Dom")));
				} else {
					map.put(Att.Att(("R"), "subject"), Term.Obj(subject, ("Dom")));
				}

			}
			if (object.isAnon()) {
				var v = new Null<>(object.toString());
				tys0.get(("Dom")).add(v);
				map.put(Att.Att(("R"), "object"), Term.Sk(v));
			} else {
				if (object.isLiteral()) {
					map.put(Att.Att(("R"), "object"), Term.Obj(object.asLiteral().getValue(), ("Dom")));
				} else if (object.isURIResource()) {
					map.put(Att.Att(("R"), "object"), Term.Obj(object.asResource().toString(), ("Dom")));
				} else {
					map.put(Att.Att(("R"), "object"), Term.Obj(object, ("Dom")));
				}
			}

			map.put(Att.Att(("R"), "predicate"), Term.Obj(predicate.toString(), ("Dom")));
			atts0.put((Integer.toString(i)), map);
			ll.add((Integer.toString(i)));
			i++;
		}

		ens0.put(("R"), ll);

		List<String> ensX = new LinkedList<>();
		List<Pair<String, Pair<String, String>>> fksX = new LinkedList<>();
		List<Pair<String, Pair<String, String>>> l = new LinkedList<>();

		l.add(new Pair<>("subject", new Pair<>("R", ("Dom"))));
		l.add(new Pair<>("predicate", new Pair<>("R", ("Dom"))));
		l.add(new Pair<>("object", new Pair<>("R", ("Dom"))));
		ensX.add("R");

		Schema<String, String, Sym, Fk, Att> sch = new SchExpRaw(new TyExpRdf(), Collections.emptyList(), ensX, fksX,
				Collections.emptyList(), l, Collections.emptyList(), Collections.emptyList(), Unit.unit, null).eval(env,
						isC);

		// System.out.println(atts0);
		ImportAlgebra<String, String, Sym, Fk, Att, String, Null<?>> alg = new ImportAlgebra<>(sch, x -> ens0.get(x),
				tys0, (en, x) -> fks0x.get(en).get(x), (en, x) -> atts0x.get(en).get(x), (x, y) -> y, (x, y) -> y, true,
				Collections.emptySet());
		alg.validateMore();

		return new SaturatedInstance<>(alg, alg, (Boolean) ops.getOrDefault(AqlOption.require_consistency),
				(Boolean) ops.getOrDefault(AqlOption.allow_java_eqs_unsafe), true, extraRepr);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("import_rdf_all ").append(Util.quote(jdbcString));
		if (!options.isEmpty()) {
			sb.append(" {\n\t").append("\n\toptions\n\t\t").append(Util.sep(options, " = ", "\n\t\t")).append("}");
		}
		return sb.toString();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Collections.emptySet();
	}

	@Override
	public SchExp type(AqlTyping G) {
		return makeSch();
	}

	public static SchExpRaw makeSch() {
		List<Pair<String, Pair<String, String>>> l = new LinkedList<>();
		l.add(new Pair<>("subject", new Pair<>("R", ("Dom"))));
		l.add(new Pair<>("predicate", new Pair<>("R", ("Dom"))));
		l.add(new Pair<>("object", new Pair<>("R", ("Dom"))));
		return new SchExpRaw(new TyExpRdf(), Collections.emptyList(), Collections.singletonList("R"),
				Collections.emptyList(), Collections.emptyList(), l, Collections.emptyList(), Collections.emptyList(),
				Unit.unit, null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
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
		InstExpRdfAll other = (InstExpRdfAll) obj;
		if (jdbcString == null) {
			if (other.jdbcString != null)
				return false;
		} else if (!jdbcString.equals(other.jdbcString))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

}
