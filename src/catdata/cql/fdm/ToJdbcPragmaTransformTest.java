package catdata.cql.fdm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import catdata.cql.AqlOptions;
import catdata.cql.Transform;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Test;

class ToJdbcPragmaTransformTest {

  @Test
  void classCanBeLoaded() throws ClassNotFoundException {
    Class<?> clazz = Class.forName("catdata.cql.fdm.ToJdbcPragmaTransform");
    assertEquals("ToJdbcPragmaTransform", clazz.getSimpleName());
  }

  @Test
  void classExtendsPragma() {
    assertTrue(catdata.cql.Pragma.class.isAssignableFrom(ToJdbcPragmaTransform.class));
  }

  /** The identity transform of an instance whose schema has the given entities. */
  @SuppressWarnings("unchecked")
  private static Transform<
          String,
          String,
          Sym,
          Fk,
          Att,
          String,
          String,
          String,
          String,
          Object,
          Object,
          Object,
          Object>
      identity(String entities) throws Exception {
    String program =
        "typeside Ty = literal { types String }\n"
            + "schema S = literal : Ty { entities "
            + entities
            + " }\n"
            + "instance I = literal : S { generators a : "
            + entities.split(" ")[0]
            + " }\n"
            + "transform t = identity I\n";
    AqlMultiDriver d = new AqlMultiDriver(AqlParserFactory.getParser().parseProgram(program), null);
    d.start();
    return (Transform<
            String,
            String,
            Sym,
            Fk,
            Att,
            String,
            String,
            String,
            String,
            Object,
            Object,
            Object,
            Object>)
        d.env.defs.trans.get("t");
  }

  private static ToJdbcPragmaTransform<
          String, String, String, String, Object, Object, Object, Object>
      pragma(String entities) throws Exception {
    return new ToJdbcPragmaTransform<>(
        "p_",
        identity(entities),
        "jdbc:h2:mem:unused",
        AqlOptions.initialOptions,
        AqlOptions.initialOptions);
  }

  /** The count is the rows of the source plus those of the target, one each here. */
  @Test
  void reportsTheRowsOfTheTransformAndRefusesToRun() throws Exception {
    var p = pragma("A B");
    assertEquals("Exported 2 rows.", p.toString());
    RuntimeException e = assertThrows(RuntimeException.class, p::execute);
    assertEquals("JDBC export for transforms deprecated", e.getMessage());
  }

  @Test
  void rejectsAnEntityNamedAsAType() {
    RuntimeException e = assertThrows(RuntimeException.class, () -> pragma("String"));
    assertEquals(
        "Cannot JDBC export: entities and types and idcol share names: String", e.getMessage());
  }
}
