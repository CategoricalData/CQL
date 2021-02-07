package catdata.aql;

import java.util.Map;
import java.util.function.Function;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Gen;
import catdata.aql.exp.Sk;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;
import gnu.trove.map.hash.THashMap;

public class RdfExporter {

	public static String typeConvert(Ty ty, Map<Ty, String> m) {
		if (m.containsKey(ty)) {
			return m.get(ty);
		}
		return "http://www.w3.org/2001/XMLSchema#String";
	}

	public static <Y> String valueConvert(Map<Ty, Function<Object, String>> printer,
			Term<Ty, Void, Sym, Void, Void, Void, Y> term) {
		try {
			if (term.obj() != null) {
				if (printer.containsKey(term.ty())) {
					return printer.get(term.ty()).apply(term.obj());
				} else {
					return term.obj().toString();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	private static <Y> Resource skToNode(Term<Ty, Void, Sym, Void, Void, Void, Y> t, Map<Y, Resource> blanks) {
		if (t.sk() != null) {
			return blanks.get(t.sk());
		}
		return ResourceFactory.createResource((String)t.obj());
	}
	
	private static <Y> RDFNode skToNode1(Term<Ty, Void, Sym, Void, Void, Void, Y> t, Map<Y, Resource> blanks) {
		if (t.sk() != null) {
			return blanks.get(t.sk());
		}
		return ResourceFactory.createPlainLiteral((String)t.obj());
	}
	
	private static <Y> Property skToNode0(Term<Ty, Void, Sym, Void, Void, Void, Y> t, Map<Y, Resource> blanks) {
		if (t.sk() != null) {
			throw new RuntimeException("Anonymouse predicates not supported in Jena");
		}
		return ResourceFactory.createProperty((String)t.obj());
	}

	public static <X, Y> Model xmlExportRdf(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i) {
		Model ret = ModelFactory.createDefaultModel();
		Att s = Att.Att(En.En("R"), "subject");
		Att p = Att.Att(En.En("R"), "predicate");
		Att o = Att.Att(En.En("R"), "object");

		if (!i.schema().ens.contains(En.En("R")) || !i.schema().atts.containsKey(s) || !i.schema().atts.containsKey(p) || !i.schema().atts.containsKey(o)) {
			throw new RuntimeException("Not on triple schema");
		}
		
		Map<Y, Resource> blanks = new THashMap<>(i.algebra().talg().sks.size() * 2);
		for (Y y : i.algebra().talg().sks.keySet()) {
			blanks.put(y, ResourceFactory.createResource());
		}
		for (X x : i.algebra().en(En.En("R"))) {
			Resource sX = skToNode(i.algebra().att(s, x), blanks);
			Property pX = skToNode0(i.algebra().att(p, x), blanks);
			RDFNode oX = skToNode1(i.algebra().att(o, x), blanks);
			System.out.println(sX + " " + pX + " " + oX);
			ret.add(sX, pX, oX);	
		}
		return ret;
	}
	
	public static <X, Y> Model xmlExport1(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i, int start,
			Map<Ty, Function<Object, String>> printer, Map<Ty, String> tym) {
		Model ret = xmlExport1(i.schema(), tym);

		Map<En, Map<X, Resource>> map = new THashMap<>();
		for (En en : i.schema().ens) {
			Map<X, Resource> m = new THashMap<>(i.algebra().size(en) * 2);
			for (X x : i.algebra().en(en)) {
				var r = ResourceFactory.createResource();
				var p = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				var o = ResourceFactory.createResource("cql://entity/" + en.str);
				ret.add(r, p, o);
				m.put(x, r);
			}
			map.put(en, m);
		}
		for (En en : i.schema().ens) {
			Map<X, Resource> m = map.get(en);
			for (Att att : i.schema().attsFrom(en)) {
				for (X x : i.algebra().en(en)) {
					var r = m.get(x);
					String str = valueConvert(printer, i.algebra().att(att, x));
					if (str == null) {
						continue;
					}
					var p = ResourceFactory.createProperty("cql://attribute/" + att);
					var o = ResourceFactory.createStringLiteral(str);
					ret.add(r, p, o);
					m.put(x, r);
				}
			}
			for (Fk fk : i.schema().fksFrom(en)) {
				Map<X, Resource> n = map.get(i.schema().fks.get(fk).second);
				for (X x : i.algebra().en(en)) {
					var r = m.get(x);
					var p = ResourceFactory.createProperty("cql://foreign_key/" + fk);
					var o = n.get(i.algebra().fk(fk, x));
					ret.add(r, p, o);
					m.put(x, r);
				}
			}
		}

		return ret;
	}

	public static Model xmlExport1(Schema<Ty, En, Sym, Fk, Att> s, Map<Ty, String> m) {
		Model ret = ModelFactory.createDefaultModel();

		for (En en : s.ens) {
			var r = ResourceFactory.createResource("cql://entity/" + en.str);
			var p = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
			var o = ResourceFactory.createResource("http://www.w3.org/2000/01/rdf-schema#Class");

			ret.add(r, p, o);

			for (Att a : s.attsFrom(en)) {
				r = ResourceFactory.createResource("cql://attribute/" + a.str);
				p = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				o = ResourceFactory.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property");
				ret.add(r, p, o);
				r = ResourceFactory.createResource("cql://attribute/" + a.str);
				p = ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#domain");
				o = ResourceFactory.createResource("cql://entity/" + en.str);
				ret.add(r, p, o);
				r = ResourceFactory.createResource("cql://attribute/" + a.str);
				p = ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#range");
				o = ResourceFactory.createResource(typeConvert(s.atts.get(a).second, m));
				ret.add(r, p, o);
			}
			for (Fk a : s.fksFrom(en)) {
				r = ResourceFactory.createResource("cql://foreign_key/" + a.str);
				p = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				o = ResourceFactory.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property");
				ret.add(r, p, o);
				r = ResourceFactory.createResource("cql://foreign_key/" + a.str);
				p = ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#domain");
				o = ResourceFactory.createResource("cql://entity/" + en.str);
				ret.add(r, p, o);
				r = ResourceFactory.createResource("cql://foreign_key/" + a.str);
				p = ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#range");
				o = ResourceFactory.createResource("cql://entity/" + s.fks.get(a).second);
				ret.add(r, p, o);
			}
		}
		return ret;
	}

	public final static String postfix = "<!-- Note: generated OWL schemas do not include path equations, or enforce that foreign keys must be total. -->\n\n</rdf:RDF>";

	public final static String pfix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "\n<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
			+ "\n    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\""
			+ "\n    xmlns:owl=\"http://www.w3.org/2002/07/owl#\"" + "\n    xmlns:entity=\"cql://entity/\""
			+ "\n    xmlns:foreign_key=\"cql://foreign_key/\"" + "\n    xmlns:attribute=\"cql://attribute/\">\n\n";
}
