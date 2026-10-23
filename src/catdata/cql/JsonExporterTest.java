package catdata.cql;

import static catdata.cql.CqlTestHelpers.VARCHAR;
import static catdata.cql.CqlTestHelpers.literal;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import catdata.cql.fdm.LiteralInstance;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** JsonExporter has no callers in src. */
class JsonExporterTest {

  private static final Att N = Att.Att("E", "n");
  private static final Fk F = Fk.Fk("E", "f");

  /** f : E -> D and n : E -> Varchar. */
  private static final Schema<String, String, Sym, Fk, Att> SCHEMA =
      sqlSchema(List.of("D", "E"), Map.of(N, "E"), Map.of(F, new Pair<>("E", "D")));

  /** Prints a Varchar without its Optional, and an empty one as NULL. */
  private static final Map<String, Function<Object, String>> PRINTER =
      Map.of(VARCHAR, o -> ((Optional<?>) o).map(Object::toString).orElse("NULL"));

  private static Term<String, String, Sym, Fk, Att, String, String> gen(String g) {
    return Term.Gen(g);
  }

  /** d : D and e1 e2 : E, with e1.f = e2.f = d and e1.n = ann; e2.n is unknown. */
  private static LiteralInstance<
          String, String, Sym, Fk, Att, String, String, Integer, Chc<String, Pair<Integer, Att>>>
      instance() {
    Map<String, String> gens = new LinkedHashMap<>();
    gens.put("d", "D");
    gens.put("e1", "E");
    gens.put("e2", "E");
    return literal(
        SCHEMA,
        gens,
        List.of(
            new Pair<>(Term.Fk(F, gen("e1")), gen("d")),
            new Pair<>(Term.Fk(F, gen("e2")), gen("d")),
            new Pair<>(Term.Att(N, gen("e1")), Term.Obj(Optional.of("ann"), VARCHAR))));
  }

  private static JsonObject parse(String s) {
    return Json.createReader(new StringReader(s)).readObject();
  }

  @Nested
  class Values {

    @Test
    void isAClassOfStaticMethods() {
      assertEquals(JsonExporter.class, new JsonExporter().getClass());
    }

    @Test
    void typeConvertFallsBackToString() {
      assertEquals("xsd:int", JsonExporter.typeConvert("Integer", Map.of("Integer", "xsd:int")));
      assertEquals("string", JsonExporter.typeConvert("Integer", Map.of()));
    }

    @Test
    void valueConvertUsesThePrinterOfTheType() {
      Term<String, Void, Sym, Void, Void, Void, String> ann =
          Term.Obj(Optional.of("ann"), VARCHAR);
      assertEquals("ann", JsonExporter.valueConvert(PRINTER, ann));
      assertEquals("Optional[ann]", JsonExporter.valueConvert(Map.of(), ann));
    }

    @Test
    void valueConvertGivesNullForALabelledNullOrAFailingPrinter() {
      assertNull(JsonExporter.valueConvert(PRINTER, Term.Sk("y")));
      Map<String, Function<Object, String>> failing =
          Map.of(
              VARCHAR,
              o -> {
                throw new IllegalStateException("cannot print");
              });
      assertNull(JsonExporter.valueConvert(failing, Term.Obj(Optional.of("ann"), VARCHAR)));
    }
  }

  @Nested
  class Schemas {

    @Test
    void describesEveryEntityAttributeAndForeignKey() {
      Map<String, JsonObjectBuilder> builders = new HashMap<>();
      JsonObject o =
          JsonExporter.jsonExport0(SCHEMA, Map.of(VARCHAR, "xsd:string"), builders).build();
      assertEquals(Set.of("cql://entityD", "cql://entityE"), o.keySet());
      assertEquals(Set.of("D", "E"), builders.keySet());
      JsonObject e = o.getJsonObject("cql://entityE");
      assertEquals(Set.of("cql://atributeE/n", "cql://foreign_key/f"), e.keySet());
      JsonObject n = e.getJsonObject("cql://atributeE/n");
      assertEquals("cql://entity/E", n.getString("rdfs:domain"));
      assertEquals("xsd:string", n.getString("rdfs:range"));
      JsonObject f = e.getJsonObject("cql://foreign_key/f");
      assertEquals("cql://entity/E", f.getString("rdfs:domain"));
      assertEquals("cql://entity/D", f.getString("rdfs:range"));
      assertEquals(0, o.getJsonObject("cql://entityD").size());
    }

    @Test
    void givesAnUnmappedTypeTheRangeString() {
      JsonObject o = JsonExporter.jsonExport0(SCHEMA, Map.of(), new HashMap<>()).build();
      JsonObject n = o.getJsonObject("cql://entityE").getJsonObject("cql://atributeE/n");
      assertEquals("string", n.getString("rdfs:range"));
    }
  }

  @Nested
  class Instances {

    @Test
    void writesEveryRowUnderItsNumber() {
      var i = instance();
      JsonObject o = parse(JsonExporter.jsonExport(i, 10, PRINTER, Map.of()));
      assertEquals(Set.of("cql://entityD", "cql://entityE", "D", "E"), o.keySet());
      Pair<TObjectIntMap<Integer>, TIntObjectMap<Integer>> ids = i.algebra().intifyX(10);
      int d = ids.first.get(i.algebra().gen("d"));
      int e1 = ids.first.get(i.algebra().gen("e1"));
      int e2 = ids.first.get(i.algebra().gen("e2"));
      assertEquals(Set.of(10, 11, 12), Set.of(d, e1, e2));
      assertEquals(Set.of("http://cql/" + d), o.getJsonObject("D").keySet());
      assertEquals(0, o.getJsonObject("D").getJsonObject("http://cql/" + d).size());
      JsonObject rows = o.getJsonObject("E");
      assertEquals(Set.of("http://cql/" + e1, "http://cql/" + e2), rows.keySet());
      JsonObject r1 = rows.getJsonObject("http://cql/" + e1);
      assertEquals("ann", r1.getString("http://cql/n"));
      assertEquals(d, r1.getInt("http://cql/f"));
      assertEquals(d, rows.getJsonObject("http://cql/" + e2).getInt("http://cql/f"));
    }

    /** A labelled null and the string null are written the same way. */
    @Test
    void writesALabelledNullAsTheStringNull() {
      var i = instance();
      JsonObject o = parse(JsonExporter.jsonExport(i, 0, PRINTER, Map.of()));
      int e2 = i.algebra().intifyX(0).first.get(i.algebra().gen("e2"));
      JsonObject row = o.getJsonObject("E").getJsonObject("http://cql/" + e2);
      assertEquals("null", row.getString("http://cql/n"));
    }

    @Test
    void writesTheSchemaPartWithTheGivenRanges() {
      JsonObject o =
          parse(JsonExporter.jsonExport(instance(), 0, PRINTER, Map.of(VARCHAR, "xsd:string")));
      JsonObject n = o.getJsonObject("cql://entityE").getJsonObject("cql://atributeE/n");
      assertEquals("xsd:string", n.getString("rdfs:range"));
    }

    /**
     * jsonExport0 adds each entity's builder to the schema part, which builds and empties it, so
     * jsonExport fills it again with the rows only.
     */
    @Test
    void handsBackTheEntityBuildersEmpty() {
      Map<String, JsonObjectBuilder> builders = new HashMap<>();
      JsonExporter.jsonExport0(SCHEMA, Map.of(), builders);
      assertEquals(0, builders.get("E").build().size());
    }
  }
}
