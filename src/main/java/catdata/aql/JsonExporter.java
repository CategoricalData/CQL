package catdata.aql;

import java.util.Map;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import catdata.Pair;
import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Gen;
import catdata.aql.exp.Sk;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;

public class JsonExporter {

	public static String typeConvert(Ty ty, Map<Ty, String> m) {
		if (m.containsKey(ty)) {
			return m.get(ty);
		}
		return "string";
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

	public static <X, Y> String jsonExport(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> i, int start,
			Map<Ty, Function<Object, String>> printer, Map<Ty, String> tym) {

		Map<En, JsonObjectBuilder> map = new THashMap<>();
		JsonObjectBuilder ret = jsonExport0(i.schema(), tym, map);

		Pair<TObjectIntMap<X>, TIntObjectMap<X>> m = i.algebra().intifyX(start);

		for (En en : i.schema().ens) {
			JsonObjectBuilder www = map.get(en);
			for (X id : i.algebra().en(en)) {
				JsonObjectBuilder xxx = Json.createObjectBuilder();
				int e = m.first.get(id);
				for (Att a : i.schema().attsFrom(en)) {
					String r = valueConvert(printer, i.algebra().att(a, id));
					if (r == null) {
						r = "null";
					}
					xxx.add("http://cql/" + a.str, r);
				}
				for (Fk a : i.schema().fksFrom(en)) {
					xxx.add("http://cql/" + a.str, m.first.get(i.algebra().fk(a, id)));
				}
				www.add("http://cql/" + Integer.toString(e), xxx);
			}
			ret.add(en.str, www);
		}

		return ret.build().toString();
	}

	public static JsonObjectBuilder jsonExport0(Schema<Ty, En, Sym, Fk, Att> s, Map<Ty, String> m,
			Map<En, JsonObjectBuilder> map) {
		JsonObjectBuilder b = Json.createObjectBuilder();
		for (En en : s.ens) {
			JsonObjectBuilder o = Json.createObjectBuilder();
			for (Att w : s.attsFrom(en)) {
				JsonObjectBuilder a = Json.createObjectBuilder();
				a.add("rdfs:domain", "cql://entity/"+ s.atts.get(w).first.str);
					
				Ty t = s.atts.get(w).second;
				String tt = "";
				if (m.containsKey(t)) {
					tt = m.get(t);
				} else {
					tt = "string";
				}
				a.add("rdfs:range", tt);				
				o.add("cql://atribute" + en + "/" + w.str, a);
			}
			for (Fk w : s.fksFrom(en)) {
				JsonObjectBuilder a = Json.createObjectBuilder();
				a.add("rdfs:domain", "cql://entity/" + s.fks.get(w).first.str);
				a.add("rdfs:range", "cql://entity/" + s.fks.get(w).second.str);
				o.add("cql://foreign_key/" + w.str, a);
			}

			b.add("cql://entity" + en.str, o);
			map.put(en, o);
		}

		return b;
	}

}
