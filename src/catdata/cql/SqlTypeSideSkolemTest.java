package catdata.cql;

import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import catdata.Chc;
import catdata.Pair;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SqlTypeSideSkolemTest {

  private static final Optional<Boolean> T = Optional.of(true);
  private static final Optional<Boolean> F = Optional.of(false);
  private static final Optional<Boolean> U = Optional.empty();

  @Nested
  class GetSqlType {

    @ParameterizedTest
    @CsvSource({
      "varbinary, " + Types.VARBINARY,
      "longvarbinary, " + Types.LONGVARBINARY,
      "binary, " + Types.BINARY,
      "date, " + Types.DATE,
      "time, " + Types.TIME,
      "timestamp, " + Types.TIMESTAMP,
      "bigint, " + Types.BIGINT,
      "boolean, " + Types.BOOLEAN,
      "char, " + Types.CHAR,
      "double, " + Types.DOUBLE,
      "double precision, " + Types.DOUBLE,
      "numeric, " + Types.NUMERIC,
      "decimal, " + Types.DECIMAL,
      "real, " + Types.REAL,
      "float, " + Types.FLOAT,
      "integer, " + Types.INTEGER,
      "tinyint, " + Types.TINYINT,
      "bit, " + Types.BIT,
      "smallint, " + Types.SMALLINT,
      "nvarchar, " + Types.NVARCHAR,
      "longvarchar, " + Types.LONGVARCHAR,
      "text, " + Types.VARCHAR,
      "varchar, " + Types.VARCHAR,
      "string, " + Types.VARCHAR,
      "blob, " + Types.BLOB,
      "other, " + Types.OTHER,
      "clob, " + Types.CLOB
    })
    void mapsEveryKnownNameToItsJdbcType(String name, int expected) {
      assertEquals(expected, SqlTypeSideSkolem.getSqlType(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"VARCHAR", "VarChar", "Double Precision", "BIGINT"})
    void isCaseInsensitive(String name) {
      assertEquals(
          SqlTypeSideSkolem.getSqlType(name.toLowerCase()), SqlTypeSideSkolem.getSqlType(name));
    }

    @Test
    void collapsesTextAndStringOntoVarchar() {
      assertEquals(Types.VARCHAR, SqlTypeSideSkolem.getSqlType("text"));
      assertEquals(Types.VARCHAR, SqlTypeSideSkolem.getSqlType("string"));
      assertEquals(Types.VARCHAR, SqlTypeSideSkolem.getSqlType("varchar"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "", "long", "int"})
    void rejectsUnknownNames(String name) {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> SqlTypeSideSkolem.getSqlType(name));
      assertTrue(e.getMessage().contains("Unknown sql type"));
    }

    @Test
    void rejectsCustomEvenThoughItIsADeclaredType() {
      // "Custom" is a key of jts(), so it is a legal type of this typeside, but getSqlType has
      // no case for it. Documents the gap rather than endorsing it.
      assertTrue(SqlTypeSideSkolem.jts().containsKey("Custom"));
      assertThrows(RuntimeException.class, () -> SqlTypeSideSkolem.getSqlType("Custom"));
    }
  }

  @Nested
  class Mediate {

    @ParameterizedTest
    @ValueSource(strings = {"varchar", "string", "VARCHAR", "String"})
    void lengthQualifiesTheStringTypes(String ty) {
      assertEquals("varchar(42)", SqlTypeSideSkolem.mediate(42, ty));
    }

    @ParameterizedTest
    @ValueSource(strings = {"integer", "Bigint", "date", "blob"})
    void passesOtherTypesThroughUnchanged(String ty) {
      assertEquals(ty, SqlTypeSideSkolem.mediate(42, ty));
    }

    @Test
    void preservesTheCaseOfPassedThroughTypes() {
      assertEquals("Timestamp", SqlTypeSideSkolem.mediate(1, "Timestamp"));
    }
  }

  @Nested
  class JavaTypes {

    @Test
    void tysIsExactlyTheKeySetOfJts() {
      assertEquals(SqlTypeSideSkolem.jts().keySet(), SqlTypeSideSkolem.tys());
    }

    @Test
    void tysIsUnmodifiable() {
      Set<String> tys = SqlTypeSideSkolem.tys();
      assertThrows(UnsupportedOperationException.class, () -> tys.add("Nonesuch"));
    }

    @Test
    void everyTypeHasAJavaBacking() {
      Map<String, String> jts = SqlTypeSideSkolem.jts();
      assertEquals(28, jts.size());
      for (Map.Entry<String, String> e : jts.entrySet()) {
        assertNotNull(e.getValue(), e.getKey() + " has no java type");
        assertTrue(e.getValue().startsWith("java."), e.getKey() + " -> " + e.getValue());
      }
    }

    @ParameterizedTest
    @CsvSource({
      "Bigint, java.lang.Long",
      "Bit, java.lang.Boolean",
      "Double, java.lang.Double",
      "Double precision, java.lang.Double",
      "Real, java.lang.Double",
      "Float, java.lang.Float",
      "Integer, java.lang.Integer",
      "Smallint, java.lang.Integer",
      "Tinyint, java.lang.Integer",
      "Decimal, java.math.BigDecimal",
      "Numeric, java.math.BigDecimal",
      "Varchar, java.lang.String",
      "Text, java.lang.String",
      "String, java.lang.String"
    })
    void bindsScalarTypesToTheirJavaClasses(String ty, String java) {
      assertEquals(java, SqlTypeSideSkolem.jts().get(ty));
    }

    /** Boolean is bound to java.lang.Optional, which does not exist. */
    @Test
    void everyBindingButBooleanNamesALoadableClass() {
      for (Map.Entry<String, String> e : SqlTypeSideSkolem.jts().entrySet()) {
        if (e.getKey().equals("Boolean")) {
          assertThrows(ClassNotFoundException.class, () -> Class.forName(e.getValue()));
        } else {
          assertDoesNotThrow(() -> Class.forName(e.getValue()), e.getKey());
        }
      }
    }

    @Test
    void jtsIsAFreshMapEachCall() {
      Map<String, String> first = SqlTypeSideSkolem.jts();
      first.put("Nonesuch", "java.lang.Void");
      assertTrue(!SqlTypeSideSkolem.jts().containsKey("Nonesuch"));
    }
  }

  @Nested
  class Conjunction {

    @Test
    void agreesWithTwoValuedLogicOnKnownOperands() {
      assertEquals(T, SqlTypeSideSkolem.and(T, T));
      assertEquals(F, SqlTypeSideSkolem.and(T, F));
      assertEquals(F, SqlTypeSideSkolem.and(F, T));
      assertEquals(F, SqlTypeSideSkolem.and(F, F));
    }

    @Test
    void falseAnnihilatesUnknown() {
      assertEquals(F, SqlTypeSideSkolem.and(U, F));
      assertEquals(F, SqlTypeSideSkolem.and(F, U));
    }

    @Test
    void trueAndUnknownIsUnknown() {
      assertEquals(U, SqlTypeSideSkolem.and(U, T));
      assertEquals(U, SqlTypeSideSkolem.and(T, U));
    }

    @Test
    void unknownAndUnknownIsUnknown() {
      assertEquals(U, SqlTypeSideSkolem.and(U, U));
    }

    @Test
    void isCommutative() {
      List<Optional<Boolean>> vals = List.of(T, F, U);
      for (Optional<Boolean> x : vals) {
        for (Optional<Boolean> y : vals) {
          assertEquals(
              SqlTypeSideSkolem.and(x, y), SqlTypeSideSkolem.and(y, x), x + " and " + y);
        }
      }
    }
  }

  @Nested
  class Disjunction {

    @Test
    void agreesWithTwoValuedLogicOnKnownOperands() {
      assertEquals(T, SqlTypeSideSkolem.or(T, T));
      assertEquals(T, SqlTypeSideSkolem.or(T, F));
      assertEquals(T, SqlTypeSideSkolem.or(F, T));
      assertEquals(F, SqlTypeSideSkolem.or(F, F));
    }

    @Test
    void trueAnnihilatesUnknown() {
      assertEquals(T, SqlTypeSideSkolem.or(U, T));
      assertEquals(T, SqlTypeSideSkolem.or(T, U));
    }

    @Test
    void falseOrUnknownIsUnknown() {
      assertEquals(U, SqlTypeSideSkolem.or(U, F));
      assertEquals(U, SqlTypeSideSkolem.or(F, U));
    }

    @Test
    void unknownOrUnknownIsUnknown() {
      assertEquals(U, SqlTypeSideSkolem.or(U, U));
    }

    @Test
    void isCommutative() {
      List<Optional<Boolean>> vals = List.of(T, F, U);
      for (Optional<Boolean> x : vals) {
        for (Optional<Boolean> y : vals) {
          assertEquals(SqlTypeSideSkolem.or(x, y), SqlTypeSideSkolem.or(y, x), x + " or " + y);
        }
      }
    }
  }

  @Nested
  class Negation {

    @Test
    void invertsKnownValuesAndFixesUnknown() {
      assertEquals(F, SqlTypeSideSkolem.not(T));
      assertEquals(T, SqlTypeSideSkolem.not(F));
      assertEquals(U, SqlTypeSideSkolem.not(U));
    }

    @Test
    void isAnInvolution() {
      for (Optional<Boolean> x : List.of(T, F, U)) {
        assertEquals(x, SqlTypeSideSkolem.not(SqlTypeSideSkolem.not(x)), "not not " + x);
      }
    }

    @Test
    void satisfiesDeMorgan() {
      List<Optional<Boolean>> vals = List.of(T, F, U);
      for (Optional<Boolean> x : vals) {
        for (Optional<Boolean> y : vals) {
          assertEquals(
              SqlTypeSideSkolem.not(SqlTypeSideSkolem.and(x, y)),
              SqlTypeSideSkolem.or(SqlTypeSideSkolem.not(x), SqlTypeSideSkolem.not(y)),
              "not(" + x + " and " + y + ")");
          assertEquals(
              SqlTypeSideSkolem.not(SqlTypeSideSkolem.or(x, y)),
              SqlTypeSideSkolem.and(SqlTypeSideSkolem.not(x), SqlTypeSideSkolem.not(y)),
              "not(" + x + " or " + y + ")");
        }
      }
    }
  }

  @Nested
  class IsFalse {

    @Test
    void holdsOnlyOfFalse() {
      assertEquals(T, SqlTypeSideSkolem.isFalse(F));
      assertEquals(F, SqlTypeSideSkolem.isFalse(T));
    }

    @Test
    void isTotalOnUnknown() {
      // SQL's IS FALSE predicate is a total function: it never yields UNKNOWN, so an unknown
      // operand answers FALSE rather than propagating. This is what separates it from not().
      assertEquals(F, SqlTypeSideSkolem.isFalse(U));
      assertEquals(U, SqlTypeSideSkolem.not(U));
    }
  }

  @Nested
  class Equality {

    @Test
    void comparesPresentValues() {
      assertEquals(T, SqlTypeSideSkolem.eq(Optional.of(1), Optional.of(1)));
      assertEquals(F, SqlTypeSideSkolem.eq(Optional.of(1), Optional.of(2)));
      assertEquals(T, SqlTypeSideSkolem.eq(Optional.of("a"), Optional.of("a")));
    }

    @Test
    void isUnknownWheneverEitherSideIsUnknown() {
      assertEquals(U, SqlTypeSideSkolem.eq(Optional.empty(), Optional.of(1)));
      assertEquals(U, SqlTypeSideSkolem.eq(Optional.of(1), Optional.empty()));
      assertEquals(U, SqlTypeSideSkolem.eq(Optional.empty(), Optional.empty()));
    }

    @Test
    void isNullDetectsAbsenceAndNeverReturnsUnknown() {
      assertEquals(T, SqlTypeSideSkolem.isNull(Optional.empty()));
      assertEquals(F, SqlTypeSideSkolem.isNull(Optional.of(1)));
    }
  }

  @Nested
  class Arithmetic {

    @Test
    void addsEachNumericType() {
      assertEquals(Integer.valueOf(5), SqlTypeSideSkolem.plusInteger(2, 3));
      assertEquals(Float.valueOf(5.5f), SqlTypeSideSkolem.plusFloat(2.5f, 3.0f));
      assertEquals(Double.valueOf(5.5), SqlTypeSideSkolem.plusDouble(2.5, 3.0));
      assertEquals(
          new BigDecimal("5.5"),
          SqlTypeSideSkolem.plusBigDecimal(new BigDecimal("2.5"), new BigDecimal("3.0")));
    }

    @Test
    void bigDecimalAdditionPreservesScale() {
      assertEquals(
          new BigDecimal("1.50"),
          SqlTypeSideSkolem.plusBigDecimal(new BigDecimal("1.00"), new BigDecimal("0.50")));
    }
  }

  @Nested
  class Symbols {

    @Test
    void boolConstantsShareTheNullarySort() {
      assertEquals(List.of(), SqlTypeSideSkolem.boolSort.first);
      assertEquals("Boolean", SqlTypeSideSkolem.boolSort.second);
      assertEquals(SqlTypeSideSkolem.boolSort, SqlTypeSideSkolem.t.ty);
      assertEquals(SqlTypeSideSkolem.boolSort, SqlTypeSideSkolem.f.ty);
      assertEquals(SqlTypeSideSkolem.boolSort, SqlTypeSideSkolem.n.ty);
    }

    @Test
    void boolConstantsAreDistinctAndNamed() {
      assertEquals("true", SqlTypeSideSkolem.t.str);
      assertEquals("false", SqlTypeSideSkolem.f.str);
      assertEquals("null", SqlTypeSideSkolem.n.str);
      assertTrue(!SqlTypeSideSkolem.t.equals(SqlTypeSideSkolem.f));
    }

    @Test
    void symsAreInterned() {
      assertSame(
          SqlTypeSideSkolem.t, Sym.Sym("true", SqlTypeSideSkolem.boolSort), "Sym.Sym caches");
    }

    @Test
    void boolSortArities() {
      assertEquals(1, SqlTypeSideSkolem.boolSort1.first.size());
      assertEquals(2, SqlTypeSideSkolem.boolSort2.first.size());
      assertEquals("Boolean", SqlTypeSideSkolem.boolSort1.second);
      assertEquals("Boolean", SqlTypeSideSkolem.boolSort2.second);
    }

    @Test
    void declaresBothBoolConstants() {
      Map<Sym, Pair<List<String>, String>> syms = SqlTypeSideSkolem.syms();
      assertEquals(SqlTypeSideSkolem.boolSort, syms.get(SqlTypeSideSkolem.t));
      assertEquals(SqlTypeSideSkolem.boolSort, syms.get(SqlTypeSideSkolem.f));
    }

    @Test
    void everySymbolReturnsADeclaredType() {
      Set<String> tys = SqlTypeSideSkolem.tys();
      for (Map.Entry<Sym, Pair<List<String>, String>> e : SqlTypeSideSkolem.syms().entrySet()) {
        assertTrue(
            tys.contains(e.getValue().second),
            e.getKey() + " returns undeclared type " + e.getValue().second);
        for (String arg : e.getValue().first) {
          assertTrue(tys.contains(arg), e.getKey() + " takes undeclared type " + arg);
        }
      }
    }

    /** The eq, isNull, isFalse and null declarations are commented out. */
    @Test
    void declaresOnlyTheConstantsTheConnectivesAndPlus() {
      Map<Sym, Pair<List<String>, String>> syms = SqlTypeSideSkolem.syms();
      assertEquals(
          Set.of("true", "false", "not", "and", "or", "+"),
          syms.keySet().stream().map(x -> x.str).collect(Collectors.toSet()));
      assertEquals(5 + 9, syms.size());
    }

    /** Including the Decimal overload declared on Numeric arguments. */
    @Test
    void declaresThePlusSortsOfSqlTypeSide() {
      assertEquals(plusSorts(SqlTypeSide.syms()), plusSorts(SqlTypeSideSkolem.syms()));
    }

    private Set<Pair<List<String>, String>> plusSorts(Map<Sym, Pair<List<String>, String>> syms) {
      return syms.keySet().stream()
          .filter(x -> x.str.equals("+"))
          .map(x -> x.ty)
          .collect(Collectors.toSet());
    }

    @Test
    void symKeysAgreeWithTheirDeclaredSignature() {
      for (Map.Entry<Sym, Pair<List<String>, String>> e : SqlTypeSideSkolem.syms().entrySet()) {
        assertEquals(e.getKey().ty, e.getValue(), e.getKey() + " signature disagrees with key");
      }
    }
  }

  @Nested
  class Construction {

    /**
     * Every argument of the super call, jfs() included, is evaluated before TypeSide rejects the
     * first java function without a symbol, so the factory throws and caches nothing.
     */
    @Test
    void failsOnIsFalseEveryTime() {
      for (int i = 0; i < 2; i++) {
        RuntimeException e =
            assertThrows(
                RuntimeException.class, () -> SqlTypeSideSkolem.SqlTypeSideSkolem(ops()));
        assertEquals("The java function isFalse is not a declared function", e.getMessage());
      }
    }
  }

  /** makeEds has no callers; it is a copy of SqlTypeSide.makeEds. */
  @Nested
  class Eds {

    @Test
    void areTheEdsOfSqlTypeSide() {
      Schema<String, String, Sym, Fk, Att> schema = sqlSchema(List.of("E"), Map.of(), Map.of());
      Constraints c = SqlTypeSideSkolem.makeEds(schema, ops());
      assertSame(schema, c.schema);
      assertEquals(3 * SqlTypeSide.tys().size(), c.eds.size());
      assertEquals(
          new HashSet<>(SqlTypeSide.makeEds(schema, ops()).eds), new HashSet<>(c.eds));
    }

    @Test
    void skipATypeNamedBool() {
      TypeSide<String, Sym> ts =
          new TypeSide<>(
              new HashSet<>(Set.of("Bool", "Int")),
              new HashMap<>(),
              new HashSet<>(),
              Map.of(),
              Map.of(),
              Map.of(),
              ops());
      Constraints c = SqlTypeSideSkolem.makeEds(Schema.terminal(ts), ops());
      assertEquals(3, c.eds.size());
      for (ED ed : c.eds) {
        assertTrue(ed.As.values().stream().allMatch(Chc.inLeft("Int")::equals));
      }
    }
  }
}
