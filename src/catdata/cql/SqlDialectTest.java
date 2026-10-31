package catdata.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import catdata.cql.SqlDialect.ColumnType;
import catdata.cql.SqlDialect.Size;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SqlDialectTest {

  private static final int LENGTH = 32;

  private static Optional<ColumnType> columnType(SqlDialect d, String ty) {
    return d.columnType(ty, LENGTH, Optional.empty());
  }

  @Nested
  class Of {

    @ParameterizedTest
    @CsvSource({
      "H2, H2",
      "PostgreSQL, POSTGRESQL",
      "MySQL, MYSQL",
      "MariaDB, MYSQL",
      "SQLite, SQLITE",
      "Microsoft SQL Server, SQL_SERVER",
      "Oracle, ORACLE",
      "Apache Derby, STANDARD"
    })
    void readsTheDatabaseProductName(String product, SqlDialect expected) {
      assertEquals(expected, SqlDialect.of(product));
    }

    @Test
    void ignoresCase() {
      assertEquals(SqlDialect.POSTGRESQL, SqlDialect.of("POSTGRESQL"));
    }
  }

  @Nested
  class ColumnTypes {

    /** The column type and the JDBC type each dialect binds with, for every case of its table. */
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "STANDARD | Bit | boolean | -7",
          "STANDARD | Boolean | boolean | 16",
          "STANDARD | Tinyint | smallint | 5",
          "STANDARD | Smallint | smallint | 5",
          "STANDARD | Integer | integer | 4",
          "STANDARD | Bigint | bigint | -5",
          "STANDARD | Real | real | 7",
          "STANDARD | Float | double precision | 6",
          "STANDARD | Double | double precision | 8",
          "STANDARD | Double precision | double precision | 8",
          "STANDARD | Numeric | numeric | 2",
          "STANDARD | Decimal | decimal | 3",
          "STANDARD | Char | char | 1",
          "STANDARD | Varchar | varchar(32) | 12",
          "STANDARD | String | varchar(32) | 12",
          "STANDARD | Text | clob | 12",
          "STANDARD | Longvarchar | clob | -1",
          "STANDARD | Clob | clob | -1",
          "STANDARD | Nchar | nchar | -15",
          "STANDARD | Nvarchar | nvarchar(32) | -9",
          "STANDARD | Longnvarchar | nclob | -16",
          "STANDARD | Nclob | nclob | -16",
          "STANDARD | Binary | binary | -2",
          "STANDARD | Varbinary | varbinary(32) | -3",
          "STANDARD | Longvarbinary | blob | -4",
          "STANDARD | Blob | blob | -4",
          "STANDARD | Date | date | 91",
          "STANDARD | Time | time | 92",
          "STANDARD | Timestamp | timestamp | 93",
          "STANDARD | Time_with_timezone | time with time zone | 2013",
          "STANDARD | Timestamp_with_timezone | timestamp with time zone | 2014",
          "STANDARD | Sqlxml | xml | 2009",
          "H2 | Tinyint | tinyint | -6",
          "H2 | Text | character varying | 12",
          "H2 | Longvarchar | character varying | -1",
          "H2 | Nvarchar | character varying | -9",
          "H2 | Longnvarchar | character varying | -16",
          "H2 | Varbinary | binary varying | -3",
          "H2 | Longvarbinary | binary varying | -4",
          "H2 | Clob | clob | -1",
          "H2 | Integer | integer | 4",
          "POSTGRESQL | Bit | boolean | -7",
          "POSTGRESQL | Tinyint | smallint | 5",
          "POSTGRESQL | Text | text | 12",
          "POSTGRESQL | Clob | text | -1",
          "POSTGRESQL | Longnvarchar | text | -1",
          "POSTGRESQL | Nclob | text | -1",
          "POSTGRESQL | Nchar | char | 1",
          "POSTGRESQL | Nvarchar | varchar(32) | 12",
          "POSTGRESQL | Binary | bytea | -2",
          "POSTGRESQL | Varbinary | bytea | -3",
          "POSTGRESQL | Blob | bytea | -4",
          "POSTGRESQL | Double | double precision | 8",
          "MYSQL | Tinyint | tinyint | -6",
          "MYSQL | Real | float | 7",
          "MYSQL | Float | double | 6",
          "MYSQL | Double | double | 8",
          "MYSQL | Text | longtext | 12",
          "MYSQL | Nclob | longtext | -16",
          "MYSQL | Longvarbinary | longblob | -4",
          "MYSQL | Blob | longblob | -4",
          "MYSQL | Time | time(6) | 92",
          "MYSQL | Timestamp | datetime(6) | 93",
          "MYSQL | Bit | boolean | -7",
          "SQLITE | Tinyint | tinyint | -6",
          "SQLITE | Double | double | 8",
          "SQLITE | Clob | text | -1",
          "SQLITE | Binary | blob | -2",
          "SQLITE | Blob | blob | -4",
          "SQLITE | Date | date | 91",
          "SQL_SERVER | Bit | bit | -7",
          "SQL_SERVER | Boolean | bit | 16",
          "SQL_SERVER | Tinyint | smallint | 5",
          "SQL_SERVER | Double | float | 8",
          "SQL_SERVER | Clob | varchar(max) | -1",
          "SQL_SERVER | Nclob | nvarchar(max) | -16",
          "SQL_SERVER | Blob | varbinary(max) | -4",
          "SQL_SERVER | Timestamp | datetime2 | 93",
          "SQL_SERVER | Timestamp_with_timezone | datetimeoffset | 2014",
          "SQL_SERVER | Integer | integer | 4",
          "ORACLE | Numeric | number | 2",
          "ORACLE | Decimal | decimal | 3"
        })
    void spellsEachTypeForItsDatabase(SqlDialect d, String ty, String ddl, int jdbcType) {
      assertEquals(Optional.of(new ColumnType(ddl, jdbcType)), columnType(d, ty));
    }

    @ParameterizedTest
    @CsvSource({
      "STANDARD, Other",
      "STANDARD, Array",
      "STANDARD, Struct",
      "STANDARD, Java_object",
      "STANDARD, Custom",
      "STANDARD, Money",
      "H2, Other",
      "H2, Sqlxml",
      "MYSQL, Time_with_timezone",
      "MYSQL, Timestamp_with_timezone",
      "MYSQL, Sqlxml",
      "SQL_SERVER, Time_with_timezone",
      "POSTGRESQL, Other",
      "SQLITE, Ref"
    })
    void hasNoColumnTypeForSomeTypes(SqlDialect d, String ty) {
      assertEquals(Optional.empty(), columnType(d, ty));
    }

    @Test
    void sizesNumericFixedLengthAndVariableLengthTypes() {
      Optional<Size> five2 = Optional.of(new Size(5, 2));
      assertEquals("numeric(5,2)", SqlDialect.POSTGRESQL.columnType("Numeric", 8, five2).get().ddl());
      assertEquals("decimal(5,2)", SqlDialect.MYSQL.columnType("Decimal", 8, five2).get().ddl());
      assertEquals("number(5,2)", SqlDialect.ORACLE.columnType("Numeric", 8, five2).get().ddl());
      Optional<Size> three = Optional.of(new Size(3, 0));
      assertEquals("char(3)", SqlDialect.H2.columnType("Char", 8, three).get().ddl());
      assertEquals("char(3)", SqlDialect.POSTGRESQL.columnType("Nchar", 8, three).get().ddl());
      assertEquals("nchar(3)", SqlDialect.SQL_SERVER.columnType("Nchar", 8, three).get().ddl());
      assertEquals("binary(3)", SqlDialect.MYSQL.columnType("Binary", 8, three).get().ddl());
      assertEquals("varchar(8)", SqlDialect.SQLITE.columnType("Varchar", 8, three).get().ddl());
    }

    @Test
    void bindsATinyintAsATinyintWhereTheColumnIsOne() {
      for (SqlDialect d : List.of(SqlDialect.H2, SqlDialect.MYSQL, SqlDialect.SQLITE)) {
        assertEquals(Types.TINYINT, columnType(d, "Tinyint").get().jdbcType(), d.name());
      }
      for (SqlDialect d : List.of(SqlDialect.STANDARD, SqlDialect.SQL_SERVER, SqlDialect.ORACLE)) {
        assertEquals(Types.SMALLINT, columnType(d, "Tinyint").get().jdbcType(), d.name());
      }
    }

    @Test
    void storesAnyJavaObjectInH2ForSqlEvaluation() {
      assertEquals(new ColumnType("java_object", Types.JAVA_OBJECT), SqlDialect.H2_JAVA_OBJECT);
    }
  }

  @Nested
  class Sizes {

    @Test
    void giveADecimalTheMostDigitsBeforeAndAfterThePoint() {
      List<Object> values =
          List.of(new BigDecimal("9.99"), new BigDecimal("123.4"), new BigDecimal("-0.5"));
      assertEquals(Optional.of(new Size(5, 2)), SqlDialect.sizeOf("Numeric", values));
      assertEquals(Optional.of(new Size(5, 2)), SqlDialect.sizeOf("Decimal", values));
    }

    @Test
    void countTheDigitsANegativeScaleStandsFor() {
      // 1E+3 is 1000, which needs four digits
      assertEquals(
          Optional.of(new Size(4, 0)),
          SqlDialect.sizeOf("Numeric", List.of(new BigDecimal("1E+3"))));
    }

    @Test
    void giveAFractionNoDigitsBeforeThePoint() {
      assertEquals(
          Optional.of(new Size(3, 3)),
          SqlDialect.sizeOf("Numeric", List.of(new BigDecimal("0.001"))));
    }

    @Test
    void giveZeroOneDigit() {
      assertEquals(
          Optional.of(new Size(1, 0)), SqlDialect.sizeOf("Numeric", List.of(BigDecimal.ZERO)));
    }

    @Test
    void readOtherNumbersThroughTheirDecimalString() {
      assertEquals(Optional.of(new Size(2, 1)), SqlDialect.sizeOf("Numeric", List.of(2.5)));
    }

    @Test
    void giveACharacterColumnItsLongestValueInCharacters() {
      // the first value is two characters, one of them outside the basic multilingual plane
      List<Object> values = List.of("\uD835\uDD38b", "abc", "a");
      assertEquals(Optional.of(new Size(3, 0)), SqlDialect.sizeOf("Char", values));
      assertEquals(Optional.of(new Size(3, 0)), SqlDialect.sizeOf("Nchar", values));
    }

    @Test
    void giveAnEmptyCharacterColumnOneCharacter() {
      assertEquals(Optional.of(new Size(1, 0)), SqlDialect.sizeOf("Char", List.of("")));
    }

    @Test
    void giveABinaryColumnItsLongestValueInBytes() {
      assertEquals(
          Optional.of(new Size(3, 0)),
          SqlDialect.sizeOf("Binary", List.of(new byte[] {1, 2, 3}, new byte[] {1})));
    }

    @Test
    void sizeABinaryColumnOnlyFromByteArrays() {
      // a Binary constant is a string, which each driver converts to bytes in its own way
      assertEquals(Optional.empty(), SqlDialect.sizeOf("Binary", List.of("ab")));
      assertEquals(
          Optional.of(new Size(1, 0)), SqlDialect.sizeOf("Binary", List.of("abc", new byte[] {1})));
    }

    @Test
    void areEmptyWithoutValues() {
      for (String ty : List.of("Numeric", "Char", "Binary")) {
        assertEquals(Optional.empty(), SqlDialect.sizeOf(ty, List.of()), ty);
      }
    }

    @Test
    void areEmptyForTypesWhoseColumnsHaveNoSize() {
      assertEquals(Optional.empty(), SqlDialect.sizeOf("Varchar", List.of("abc")));
      assertEquals(Optional.empty(), SqlDialect.sizeOf("Integer", List.of(5)));
      assertEquals(Optional.empty(), SqlDialect.sizeOf("Custom", List.of("abc")));
    }

    @Test
    void rejectANumericValueThatIsNoNumber() {
      assertThrows(
          NumberFormatException.class, () -> SqlDialect.sizeOf("Numeric", List.of("abc")));
    }
  }
}
