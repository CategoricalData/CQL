package catdata.cql;

import static catdata.cql.CqlTestHelpers.ops;
import static catdata.cql.CqlTestHelpers.sqlSchema;
import static catdata.cql.CqlTestHelpers.sqlTypeSide;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
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
import catdata.Util;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SqlTypeSideTest {

  private static final String BOOLEAN = "Boolean";

  /** Every type of the sql typeside. */
  private static final Set<String> TYPES =
      Set.of(
          "Longvarbinary", "Varbinary", "Binary", "Bigint", "Boolean", "Bit", "Char", "Double",
          "Double precision", "Decimal", "Numeric", "Float", "Real", "Integer", "Smallint",
          "Tinyint", "Text", "Nvarchar", "Varchar", "Longvarchar", "String", "Custom", "Blob",
          "Clob", "Other", "Date", "Time", "Timestamp");

  /** The types whose parser keeps its input as a string. */
  private static final Set<String> STRING_TYPES =
      Set.of(
          "Longvarbinary", "Varbinary", "Binary", "Char", "Text", "Nvarchar", "Varchar",
          "Longvarchar", "String", "Custom", "Blob", "Clob", "Other", "Date");

  private static final Optional<Boolean> T = Optional.of(true);
  private static final Optional<Boolean> F = Optional.of(false);
  private static final Optional<Boolean> U = Optional.empty();

  /** The arguments of the truth tables below, in order. */
  private static final List<Optional<Boolean>> TFU = List.of(T, F, U);

  private static Object parse(String ty, String s) {
    return sqlTypeSide().js.parse(ty, s);
  }

  private static String parseError(String ty, String s) {
    return assertThrows(RuntimeException.class, () -> parse(ty, s)).getMessage();
  }

  private static Sym sym(String name, List<String> dom, String cod) {
    return Sym.Sym(name, new Pair<>(dom, cod));
  }

  private static Sym plus(String arg, String cod) {
    return sym("+", List.of(arg, arg), cod);
  }

  private static Term<String, Void, Sym, Void, Void, Void, Void> obj(Object o, String ty) {
    return Term.Obj(o, ty);
  }

  @SafeVarargs
  private static Term<String, Void, Sym, Void, Void, Void, Void> app(
      Sym f, Term<String, Void, Sym, Void, Void, Void, Void>... args) {
    return Term.Sym(f, List.of(args));
  }

  /** Evaluates a closed term with the typeside's javascript bindings. */
  private static Term<String, Void, Sym, Void, Void, Void, Void> reduce(
      Term<String, Void, Sym, Void, Void, Void, Void> t) {
    return sqlTypeSide().js.reduce(t);
  }

  @Nested
  class Signature {

    @Test
    void bindsEveryTypeToOptional() {
      assertEquals(TYPES, SqlTypeSide.tys());
      assertEquals(TYPES, sqlTypeSide().tys);
      assertTrue(SqlTypeSide.jts().values().stream().allMatch("java.util.Optional"::equals));
      assertEquals(SqlTypeSide.jts(), sqlTypeSide().js.java_tys);
    }

    @Test
    void tysCannotBeModified() {
      assertThrows(UnsupportedOperationException.class, () -> SqlTypeSide.tys().add("Money"));
    }

    @Test
    void hasNoEquations() {
      assertTrue(sqlTypeSide().eqs.isEmpty());
    }

    @Test
    void hasAParserForEveryTypeAndAFunctionForEverySymbol() {
      assertEquals(TYPES, sqlTypeSide().js.java_parsers.keySet());
      assertEquals(sqlTypeSide().syms.keySet(), sqlTypeSide().js.java_fns.keySet());
    }

    @Test
    void declaresTheConnectivesAndEqAndIsNullForEveryType() {
      Map<Sym, Pair<List<String>, String>> syms = SqlTypeSide.syms();
      assertEquals(SqlTypeSide.boolSort1, syms.get(sym("not", List.of(BOOLEAN), BOOLEAN)));
      List<String> two = List.of(BOOLEAN, BOOLEAN);
      assertEquals(SqlTypeSide.boolSort2, syms.get(sym("and", two, BOOLEAN)));
      assertEquals(SqlTypeSide.boolSort2, syms.get(sym("or", two, BOOLEAN)));
      assertEquals(SqlTypeSide.boolSort1, syms.get(sym("isFalse", List.of(BOOLEAN), BOOLEAN)));
      for (String ty : TYPES) {
        Pair<List<String>, String> eq = new Pair<>(List.of(ty, ty), BOOLEAN);
        Pair<List<String>, String> isNull = new Pair<>(List.of(ty), BOOLEAN);
        assertEquals(eq, syms.get(Sym.Sym("eq", eq)), ty);
        assertEquals(isNull, syms.get(Sym.Sym("isNull", isNull)), ty);
      }
      assertEquals(4 + 2 * TYPES.size() + 9, syms.size());
      assertEquals(syms, sqlTypeSide().syms);
    }

    /** The Decimal overload is declared on Numeric arguments, so nothing adds two Decimals. */
    @Test
    void declaresPlusForNineSortsAndNoneOnDecimalArguments() {
      Set<Pair<List<String>, String>> sorts =
          SqlTypeSide.syms().keySet().stream()
              .filter(s -> s.str.equals("+"))
              .map(s -> s.ty)
              .collect(Collectors.toSet());
      Set<Pair<List<String>, String>> expected = new HashSet<>();
      for (String ty :
          List.of("Float", "Real", "Double", "Double precision", "Numeric", "Integer", "Tinyint",
              "Smallint")) {
        expected.add(new Pair<>(List.of(ty, ty), ty));
      }
      expected.add(new Pair<>(List.of("Numeric", "Numeric"), "Decimal"));
      assertEquals(expected, sorts);
    }

    @Test
    void trueFalseAndNullAreBooleanConstants() {
      assertSame(obj(T, BOOLEAN), SqlTypeSide.t);
      assertSame(obj(F, BOOLEAN), SqlTypeSide.f);
      assertSame(sym("null", List.of(), BOOLEAN), SqlTypeSide.n);
      assertEquals(new Pair<>(List.of(), BOOLEAN), SqlTypeSide.boolSort);
      assertFalse(sqlTypeSide().syms.containsKey(SqlTypeSide.n));
    }
  }

  @Nested
  class Construction {

    @Test
    void isBuiltOncePerOptions() {
      assertSame(sqlTypeSide(), SqlTypeSide.SqlTypeSide(ops()));
      assertSame(sqlTypeSide(), SqlTypeSide.SqlTypeSide(new AqlOptions(Map.of(), ops())));
    }

    @Test
    void alwaysUsesJavascript() {
      AqlOptions python = new AqlOptions(ops(), AqlOption.graal_language, "python");
      SqlTypeSide ts = SqlTypeSide.SqlTypeSide(python);
      assertNotSame(sqlTypeSide(), ts);
      assertEquals(ExternalCodeUtils.LANG_JS, ts.strat.getOrDefault(AqlOption.graal_language));
      assertSame(ts, SqlTypeSide.SqlTypeSide(python));
    }
  }

  @Nested
  class Parsers {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "Longvarbinary", "Varbinary", "Binary", "Bigint", "Boolean", "Bit", "Char", "Double",
          "Float", "Real", "Integer", "Tinyint", "Text", "Nvarchar", "Varchar", "Longvarchar",
          "String", "Custom", "Blob", "Clob", "Other", "Date", "Time", "Timestamp"
        })
    void readNullInAnyCaseAsEmpty(String ty) {
      assertEquals(Optional.empty(), parse(ty, "null"));
      assertEquals(Optional.empty(), parse(ty, "NULL"));
    }

    @Test
    void keepStringTypesAsWritten() {
      for (String ty : STRING_TYPES) {
        assertEquals(Optional.of("a b"), parse(ty, "a b"), ty);
      }
    }

    @Test
    void doNotValidateDates() {
      assertEquals(Optional.of("not a date"), parse("Date", "not a date"));
    }

    @Test
    void readNumbersAsTheirJavaTypes() {
      for (String ty : List.of("Integer", "Tinyint", "Smallint")) {
        assertEquals(Optional.of(5), parse(ty, "5"), ty);
      }
      assertEquals(Optional.of(5L), parse("Bigint", "5"));
      for (String ty : List.of("Double", "Float", "Real")) {
        assertEquals(Optional.of(1.5), parse(ty, "1.5"), ty);
        assertEquals(Optional.of(5.0), parse(ty, "5"), ty);
      }
      for (String ty : List.of("Numeric", "Decimal")) {
        assertEquals(Optional.of(new BigDecimal("1.5")), parse(ty, "1.5"), ty);
      }
    }

    @Test
    void doNotCheckTheRangeOfSmallTypes() {
      assertEquals(Optional.of(100000), parse("Tinyint", "100000"));
      assertEquals(Optional.of(100000), parse("Smallint", "100000"));
    }

    @Test
    void readTimesAndTimestamps() {
      assertEquals(Optional.of(Time.valueOf("12:00:00")), parse("Time", "12:00:00"));
      Object ts = parse("Timestamp", "2020-01-01 12:00:00");
      assertEquals(Optional.of(Timestamp.valueOf("2020-01-01 12:00:00")), ts);
      assertEquals("Optional[2020-01-01 12:00:00.0]", ts.toString());
    }

    @Test
    void readBooleansInAnyCase() {
      for (String ty : List.of("Boolean", "Bit")) {
        assertEquals(T, parse(ty, "TRUE"), ty);
        assertEquals(F, parse(ty, "False"), ty);
      }
    }

    /** java.lang.Boolean's constructor maps every string other than "true" to false. */
    @Test
    void readEveryOtherWordAsFalse() {
      for (String ty : List.of("Boolean", "Bit")) {
        for (String s : List.of("1", "yes", "abc")) {
          assertEquals(F, parse(ty, s), ty + " " + s);
        }
      }
    }

    @Test
    void keepTheScaleOfADecimal() {
      assertEquals(Optional.of(new BigDecimal("1.50")), parse("Numeric", "1.50"));
      assertNotEquals(parse("Numeric", "1.5"), parse("Numeric", "1.50"));
    }

    @Test
    void rejectAValueOfTheWrongForm() {
      String m = parseError("Integer", "1.5");
      assertTrue(m.contains("execution of 1.5 cannot convert to Integer"), m);
      assertTrue(m.contains("For input string: \"1.5\""), m);
    }

    /** `if ( (!(...)) {` has an unbalanced parenthesis. */
    @Test
    void doublePrecisionParsesNothing() {
      for (String s : List.of("1.5", "null")) {
        String m = parseError("Double precision", s);
        assertTrue(m.contains("SyntaxError"), m);
      }
    }

    /** The null branch returns `ava.util.Optional.empty()`. */
    @Test
    void smallintCannotReadNull() {
      String m = parseError("Smallint", "null");
      assertTrue(m.contains("ReferenceError: ava is not defined"), m);
    }

    /** The null branch has no `return`. */
    @Test
    void numericAndDecimalReadNullAsJavaNull() {
      assertNull(parse("Numeric", "null"));
      assertNull(parse("Decimal", "null"));
    }
  }

  @Nested
  class Functions {

    @Test
    void andOrAndNotFollowKleeneLogic() {
      List<List<Optional<Boolean>>> and =
          List.of(List.of(T, F, U), List.of(F, F, F), List.of(U, F, U));
      List<List<Optional<Boolean>>> or =
          List.of(List.of(T, T, T), List.of(T, F, U), List.of(T, U, U));
      for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
          String at = TFU.get(i) + ", " + TFU.get(j);
          assertEquals(and.get(i).get(j), SqlTypeSide.and(TFU.get(i), TFU.get(j)), "and " + at);
          assertEquals(or.get(i).get(j), SqlTypeSide.or(TFU.get(i), TFU.get(j)), "or " + at);
        }
      }
      assertEquals(F, SqlTypeSide.not(T));
      assertEquals(T, SqlTypeSide.not(F));
      assertEquals(U, SqlTypeSide.not(U));
    }

    @Test
    void isFalseAndIsNullNeverReturnNull() {
      assertEquals(F, SqlTypeSide.isFalse(T));
      assertEquals(T, SqlTypeSide.isFalse(F));
      assertEquals(F, SqlTypeSide.isFalse(U));
      assertEquals(T, SqlTypeSide.isNull(Optional.empty()));
      assertEquals(F, SqlTypeSide.isNull(Optional.of(5)));
    }

    @Test
    void eqIsNullWhenEitherSideIs() {
      assertEquals(T, SqlTypeSide.eq(Optional.of(1), Optional.of(1)));
      assertEquals(F, SqlTypeSide.eq(Optional.of(1), Optional.of(2)));
      assertEquals(U, SqlTypeSide.eq(Optional.empty(), Optional.of(1)));
      assertEquals(U, SqlTypeSide.eq(Optional.of(1), Optional.empty()));
    }

    @Test
    void eqComparesDecimalsByScale() {
      assertEquals(
          F,
          SqlTypeSide.eq(Optional.of(new BigDecimal("1.5")), Optional.of(new BigDecimal("1.50"))));
    }

    @Test
    void plusIsNullWhenEitherSideIs() {
      assertEquals(Optional.of(5), SqlTypeSide.plusInteger(Optional.of(2), Optional.of(3)));
      assertEquals(Optional.empty(), SqlTypeSide.plusInteger(Optional.empty(), Optional.of(3)));
      assertEquals(Optional.empty(), SqlTypeSide.plusInteger(Optional.of(2), Optional.empty()));
      assertEquals(Optional.of(3.75), SqlTypeSide.plusDouble(Optional.of(1.5), Optional.of(2.25)));
      assertEquals(Optional.empty(), SqlTypeSide.plusDouble(Optional.empty(), Optional.of(1.5)));
      assertEquals(Optional.empty(), SqlTypeSide.plusDouble(Optional.of(1.5), Optional.empty()));
      BigDecimal x = new BigDecimal("1.5");
      assertEquals(
          Optional.of(new BigDecimal("3.75")),
          SqlTypeSide.plusBigDecimal(Optional.of(x), Optional.of(new BigDecimal("2.25"))));
      assertEquals(Optional.empty(), SqlTypeSide.plusBigDecimal(Optional.empty(), Optional.of(x)));
      assertEquals(Optional.empty(), SqlTypeSide.plusBigDecimal(Optional.of(x), Optional.empty()));
    }

    @Test
    void plusIntegerWrapsAroundOnOverflow() {
      assertEquals(
          Optional.of(Integer.MIN_VALUE),
          SqlTypeSide.plusInteger(Optional.of(Integer.MAX_VALUE), Optional.of(1)));
    }
  }

  /** The javascript bindings of the symbols, run through {@link AqlJs#reduce}. */
  @Nested
  class Bindings {

    @Test
    void plusOnIntegerTypesAddsIntegers() {
      for (String ty : List.of("Integer", "Tinyint", "Smallint")) {
        assertSame(
            obj(Optional.of(5), ty),
            reduce(app(plus(ty, ty), obj(Optional.of(2), ty), obj(Optional.of(3), ty))),
            ty);
      }
    }

    @Test
    void plusOnFloatingTypesAddsDoubles() {
      for (String ty : List.of("Float", "Real", "Double", "Double precision")) {
        assertSame(
            obj(Optional.of(3.75), ty),
            reduce(app(plus(ty, ty), obj(Optional.of(1.5), ty), obj(Optional.of(2.25), ty))),
            ty);
      }
    }

    @Test
    void plusOnNumericsAddsDecimalsUnderBothCodomains() {
      Term<String, Void, Sym, Void, Void, Void, Void> a =
          obj(Optional.of(new BigDecimal("1.5")), "Numeric");
      Term<String, Void, Sym, Void, Void, Void, Void> b =
          obj(Optional.of(new BigDecimal("2.25")), "Numeric");
      Optional<BigDecimal> sum = Optional.of(new BigDecimal("3.75"));
      assertSame(obj(sum, "Numeric"), reduce(app(plus("Numeric", "Numeric"), a, b)));
      assertSame(obj(sum, "Decimal"), reduce(app(plus("Numeric", "Decimal"), a, b)));
    }

    @Test
    void connectivesAndPredicatesCallTheirJavaMethods() {
      Term<String, Void, Sym, Void, Void, Void, Void> t = obj(T, BOOLEAN);
      Term<String, Void, Sym, Void, Void, Void, Void> u = obj(U, BOOLEAN);
      assertSame(obj(U, BOOLEAN), reduce(app(sym("not", List.of(BOOLEAN), BOOLEAN), u)));
      Sym and = sym("and", List.of(BOOLEAN, BOOLEAN), BOOLEAN);
      Sym or = sym("or", List.of(BOOLEAN, BOOLEAN), BOOLEAN);
      assertSame(obj(U, BOOLEAN), reduce(app(and, t, u)));
      assertSame(obj(T, BOOLEAN), reduce(app(or, t, u)));
      assertSame(obj(F, BOOLEAN), reduce(app(sym("isFalse", List.of(BOOLEAN), BOOLEAN), u)));
      assertSame(
          obj(T, BOOLEAN),
          reduce(app(sym("isNull", List.of("Varchar"), BOOLEAN), obj(U, "Varchar"))));
      Term<String, Void, Sym, Void, Void, Void, Void> a = obj(Optional.of("a"), "Varchar");
      assertSame(
          obj(T, BOOLEAN), reduce(app(sym("eq", List.of("Varchar", "Varchar"), BOOLEAN), a, a)));
    }
  }

  @Nested
  class SqlTypes {

    @Test
    void mapsEveryTypeButCustomToAJdbcType() {
      Map<String, Integer> expected = new HashMap<>();
      expected.put("Varbinary", Types.VARBINARY);
      expected.put("Longvarbinary", Types.LONGVARBINARY);
      expected.put("Binary", Types.BINARY);
      expected.put("Date", Types.DATE);
      expected.put("Time", Types.TIME);
      expected.put("Timestamp", Types.TIMESTAMP);
      expected.put("Bigint", Types.BIGINT);
      expected.put("Boolean", Types.BOOLEAN);
      expected.put("Char", Types.CHAR);
      expected.put("Double", Types.DOUBLE);
      expected.put("Double precision", Types.DOUBLE);
      expected.put("Numeric", Types.NUMERIC);
      expected.put("Decimal", Types.DECIMAL);
      expected.put("Real", Types.REAL);
      expected.put("Float", Types.FLOAT);
      expected.put("Integer", Types.INTEGER);
      expected.put("Tinyint", Types.TINYINT);
      expected.put("Bit", Types.BIT);
      expected.put("Smallint", Types.SMALLINT);
      expected.put("Nvarchar", Types.NVARCHAR);
      expected.put("Longvarchar", Types.LONGVARCHAR);
      expected.put("Text", Types.VARCHAR);
      expected.put("Varchar", Types.VARCHAR);
      expected.put("String", Types.VARCHAR);
      expected.put("Blob", Types.BLOB);
      expected.put("Other", Types.OTHER);
      expected.put("Clob", Types.CLOB);
      Map<String, Integer> actual = new HashMap<>();
      for (String ty : TYPES) {
        if (!ty.equals("Custom")) {
          actual.put(ty, SqlTypeSide.getSqlType(ty));
        }
      }
      assertEquals(expected, actual);
    }

    @Test
    void ignoresCase() {
      assertEquals(Types.VARCHAR, SqlTypeSide.getSqlType("VARCHAR"));
    }

    @Test
    void rejectsCustomAndUnknownNames() {
      for (String ty : List.of("Custom", "Money")) {
        assertEquals(
            "Unknown sql type: " + ty,
            assertThrows(RuntimeException.class, () -> SqlTypeSide.getSqlType(ty)).getMessage());
      }
    }

    @Test
    void mediateGivesStringsALengthAndLeavesOtherTypesAsNamed() {
      assertEquals("varchar(256)", SqlTypeSide.mediate(false, 256, "Varchar"));
      assertEquals("varchar(16)", SqlTypeSide.mediate(true, 16, "String"));
      assertEquals("Number", SqlTypeSide.mediate(true, 256, "Numeric"));
      for (String ty : List.of("Numeric", "Decimal", "Nvarchar", "Text", "Binary", "Custom")) {
        assertEquals(ty, SqlTypeSide.mediate(false, 256, ty), ty);
      }
      assertEquals("Decimal", SqlTypeSide.mediate(true, 256, "Decimal"));
    }
  }

  /** The constraints of {@code constraints C = sql S}. */
  @Nested
  class Eds {

    private final Schema<String, String, Sym, Fk, Att> schema =
        sqlSchema(List.of("E"), Map.of(), Map.of());

    private final Constraints eds = SqlTypeSide.makeEds(schema, ops());

    private final Term<String, String, Sym, Fk, Att, Void, Void> x = Term.Var("x");
    private final Term<String, String, Sym, Fk, Att, Void, Void> y = Term.Var("y");

    private Term<String, String, Sym, Fk, Att, Void, Void> eq(
        String ty,
        Term<String, String, Sym, Fk, Att, Void, Void> a,
        Term<String, String, Sym, Fk, Att, Void, Void> b) {
      return Term.Sym(sym("eq", List.of(ty, ty), BOOLEAN), List.of(a, b));
    }

    private List<ED> over(String ty) {
      return eds.eds.stream()
          .filter(ed -> ed.As.values().stream().allMatch(Chc.inLeft(ty)::equals))
          .collect(Collectors.toList());
    }

    @Test
    void stateThreePropertiesOfEqPerType() {
      assertSame(schema, eds.schema);
      assertEquals(3 * TYPES.size(), eds.eds.size());
      for (String ty : TYPES) {
        assertEquals(3, over(ty).size(), ty);
      }
    }

    @Test
    void stateReflexivitySymmetryAndThatEqImpliesEquality() {
      Chc<String, String> i = Chc.inLeft("Integer");
      Set<String> found = new HashSet<>();
      for (ED ed : over("Integer")) {
        assertFalse(ed.isUnique);
        assertTrue(ed.Es.isEmpty());
        if (ed.As.equals(Map.of("x", i))) {
          assertTrue(ed.Awh.isEmpty());
          Term<String, String, Sym, Fk, Att, Void, Void> isFalse =
              Term.Sym(sym("isFalse", List.of(BOOLEAN), BOOLEAN), List.of(eq("Integer", x, x)));
          assertEquals(Set.of(new Pair<>(isFalse, SqlTypeSide.f.convert())), ed.Ewh);
          found.add("reflexive");
        } else if (ed.Awh.isEmpty()) {
          assertEquals(Map.of("x", i, "y", i), ed.As);
          assertEquals(Set.of(new Pair<>(eq("Integer", x, y), eq("Integer", y, x))), ed.Ewh);
          found.add("symmetric");
        } else {
          assertEquals(Map.of("x", i, "y", i), ed.As);
          assertEquals(Set.of(new Pair<>(eq("Integer", x, y), SqlTypeSide.t.convert())), ed.Awh);
          assertEquals(Set.of(new Pair<>(x, y)), ed.Ewh);
          found.add("eq implies equality");
        }
      }
      assertEquals(Set.of("reflexive", "symmetric", "eq implies equality"), found);
    }

    private Chc<String, String> type(
        TypeSide<String, Sym> ts,
        Map<String, String> ctx,
        Term<String, String, Sym, Fk, Att, Void, Void> t) {
      return t.type(
          ctx, Map.of(), ts.tys, ts.syms, ts.js.java_tys, Set.of(), Map.of(), Map.of(), null, null);
    }

    @Test
    void typeCheckOverTheSqlTypeSide() {
      TypeSide<String, Sym> ts = sqlTypeSide();
      for (ED ed : eds.eds) {
        Map<String, String> ctx = new HashMap<>();
        ed.As.forEach((v, s) -> ctx.put(v, s.l));
        for (var e : Util.union(ed.Awh, ed.Ewh)) {
          assertEquals(type(ts, ctx, e.first), type(ts, ctx, e.second));
        }
      }
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
      Constraints c = SqlTypeSide.makeEds(Schema.terminal(ts), ops());
      assertEquals(3, c.eds.size());
      for (ED ed : c.eds) {
        assertTrue(ed.As.values().stream().allMatch(Chc.inLeft("Int")::equals));
      }
    }
  }
}
