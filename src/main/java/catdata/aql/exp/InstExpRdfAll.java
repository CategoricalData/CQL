package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.RDFDataMgr;

import catdata.Null;
import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.fdm.ImportAlgebra;
import catdata.aql.fdm.SaturatedInstance;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class InstExpRdfAll extends InstExp<Gen, Null<?>, Gen, Null<?>> {

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

	public Schema<Ty, En, Sym, Fk, Att> makeSchema(AqlEnv env, AqlOptions ops) {
		return makeSch().eval(env, false);
	}

	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Gen, Null<?>, Gen, Null<?>> eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		AqlOptions ops = new AqlOptions(options, env.defaults);

		Map<En, Collection<Gen>> ens0 = new THashMap<>(1);
		Map<Ty, Collection<Null<?>>> tys0 = new THashMap<>();
		Map<Gen, Map<Fk, Gen>> fks0 = new THashMap<>();
		Map<Gen, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Null<?>>>> atts0 = new THashMap<>();

		tys0.put(Ty.Ty("Dom"), new THashSet<>());

		Map<Null<?>, Term<Ty, En, Sym, Fk, Att, Gen, Null<?>>> extraRepr = null; // new THashMap<>();

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

		int i = 0;
		var ll = new LinkedList<Gen>();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement(); // get next statement
			Resource subject = stmt.getSubject(); // get the subject
			Property predicate = stmt.getPredicate(); // get the predicate
			RDFNode object = stmt.getObject(); // get the object
			Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Null<?>>> map = new THashMap<>(3);

			if (subject.isAnon()) {
				var v = new Null<>(subject.toString());
				tys0.get(Ty.Ty("Dom")).add(v);
				map.put(Att.Att(En.En("R"), "subject"), Term.Sk(v));
			} else {
				map.put(Att.Att(En.En("R"), "subject"), Term.Obj(subject.toString(), Ty.Ty("Dom")));
			}
			if (object.isAnon()) {
				var v = new Null<>(object.toString());
				tys0.get(Ty.Ty("Dom")).add(v);
				map.put(Att.Att(En.En("R"), "object"), Term.Sk(v));
			} else {
				map.put(Att.Att(En.En("R"), "object"), Term.Obj(object.toString(), Ty.Ty("Dom")));
			}

			map.put(Att.Att(En.En("R"), "predicate"), Term.Obj(predicate.toString(), Ty.Ty("Dom")));
			atts0.put(Gen.Gen(Integer.toString(i)), map);
			ll.add(Gen.Gen(Integer.toString(i)));
			i++;
		}

		ens0.put(En.En("R"), ll);

		List<String> ensX = new LinkedList<>();
		List<Pair<String, Pair<String, String>>> fksX = new LinkedList<>();
		List<Pair<String, Pair<String, Ty>>> l = new LinkedList<>();

	

		l.add(new Pair<>("subject", new Pair<>("R", Ty.Ty("Dom"))));
		l.add(new Pair<>("predicate", new Pair<>("R", Ty.Ty("Dom"))));
		l.add(new Pair<>("object", new Pair<>("R", Ty.Ty("Dom"))));
		ensX.add("R");

		Schema<Ty, En, Sym, Fk, Att> sch = new SchExpRaw(new TyExpRdf(), Collections.emptyList(), ensX, fksX,
				Collections.emptyList(), l, Collections.emptyList(), Collections.emptyList(), Unit.unit).eval(env, isC);

		// System.out.println(atts0);
		ImportAlgebra<Ty, En, Sym, Fk, Att, Gen, Null<?>> alg = new ImportAlgebra<>(sch, ens0, tys0, fks0, atts0,
				(x, y) -> y, (x, y) -> y, true, Collections.emptySet());
		alg.validateMore();

		return new SaturatedInstance<>(alg, alg, (Boolean) ops.getOrDefault(AqlOption.require_consistency),
				(Boolean) ops.getOrDefault(AqlOption.allow_java_eqs_unsafe), true, extraRepr);
	}

	private boolean skip(String s) {
		return s.startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns")
				|| s.startsWith("http://www.w3.org/2000/01/rdf-schema")
				|| s.startsWith("http://www.w3.org/2002/07/owl");
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
		List<Pair<String, Pair<String, Ty>>> l = new LinkedList<>();
		l.add(new Pair<>("subject", new Pair<>("R", Ty.Ty("Dom"))));
		l.add(new Pair<>("predicate", new Pair<>("R", Ty.Ty("Dom"))));
		l.add(new Pair<>("object", new Pair<>("R", Ty.Ty("Dom"))));
		return new SchExpRaw(new TyExpRdf(), Collections.emptyList(), Collections.singletonList("R"),
				Collections.emptyList(), Collections.emptyList(), l, Collections.emptyList(), Collections.emptyList(),
				Unit.unit);
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
