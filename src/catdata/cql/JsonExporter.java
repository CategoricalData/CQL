package catdata.cql;

import java.util.Map;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import catdata.Pair;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;

public class JsonExporter {

  public static String typeConvert(String ty, Map<String, String> m) {
    if (m.containsKey(ty)) {
      return m.get(ty);
    }
    return "string";
  }

  public static <Y> String valueConvert(Map<String, Function<Object, String>> printer,
      Term<String, Void, Sym, Void, Void, Void, Y> term) {
    try {
      if (term.obj() != null) {
        if (printer.containsKey(term.ty())) {
          return printer.get(term.ty()).apply(term.obj());
        }
          return term.obj().toString();
        
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  public static <X, Y> String jsonExport(Instance<String, String, Sym, Fk, Att, String, String, X, Y> i, int start,
      Map<String, Function<Object, String>> printer, Map<String, String> tym) {

    Map<String, JsonObjectBuilder> map = new THashMap<>();
    JsonObjectBuilder ret = jsonExport0(i.schema(), tym, map);

    Pair<TObjectIntMap<X>, TIntObjectMap<X>> m = i.algebra().intifyX(start);

    for (String en : i.schema().ens) {
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
      ret.add(en, www);
    }

    return ret.build().toString();
  }

  public static JsonObjectBuilder jsonExport0(Schema<String, String, Sym, Fk, Att> s, Map<String, String> m,
      Map<String, JsonObjectBuilder> map) {
    JsonObjectBuilder b = Json.createObjectBuilder();
    for (String en : s.ens) {
      JsonObjectBuilder o = Json.createObjectBuilder();
      for (Att w : s.attsFrom(en)) {
        JsonObjectBuilder a = Json.createObjectBuilder();
        a.add("rdfs:domain", "cql://entity/"+ s.atts.get(w).first);
          
        String t = s.atts.get(w).second;
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
        a.add("rdfs:domain", "cql://entity/" + s.fks.get(w).first);
        a.add("rdfs:range", "cql://entity/" + s.fks.get(w).second);
        o.add("cql://foreign_key/" + w.str, a);
      }

      b.add("cql://entity" + en, o);
      map.put(en, o);
    }

    return b;
  }

}
