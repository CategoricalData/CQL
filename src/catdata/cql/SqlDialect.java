package catdata.cql;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Optional;

/**
 * The databases CQL creates tables in, and the column type each gives an attribute of the {@code
 * sql} typeside.
 *
 * <p>A CQL type names a JDBC type ({@link SqlTypeSide#getSqlType}), but the column type a create
 * table statement must spell depends on the database: {@code Bit} is {@code boolean} in PostgreSQL
 * and {@code bit} in SQL Server, and PostgreSQL has no {@code tinyint}, {@code double} or {@code
 * blob} at all. JDBC does not provide the reverse map either. {@code
 * DatabaseMetaData.getTypeInfo()} on PostgreSQL lists no type for {@code BOOLEAN}, {@code TINYINT}
 * or {@code BLOB}, and its first type for {@code VARCHAR} is {@code name}, the 63-byte identifier
 * type. So each dialect below spells the column types itself, starting from the SQL standard ones
 * in {@link #STANDARD}.
 */
public enum SqlDialect {
  /** The SQL standard column types, for a database CQL has no dialect for. */
  STANDARD,
  /** H2, the database CQL ships with and evaluates queries in. */
  H2,
  POSTGRESQL,
  /** MySQL and MariaDB. */
  MYSQL,
  SQLITE,
  SQL_SERVER,
  ORACLE;

  /** A column type: how a create table statement spells it, and the JDBC type to bind with. */
  public record ColumnType(String ddl, int jdbcType) {}

  /**
   * The precision and scale a column needs, in the sense of {@code ResultSetMetaData.getPrecision}:
   * the number of digits for a numeric column, and the length for a character or binary one, whose
   * scale is 0.
   */
  public record Size(int precision, int scale) {}

  /**
   * The dialect of a database, from its {@code DatabaseMetaData.getDatabaseProductName()}, or
   * {@link #STANDARD} for a database CQL has no dialect for.
   */
  public static SqlDialect of(String databaseProductName) {
    String name = databaseProductName.toLowerCase();
    if (name.equals("h2")) {
      return H2;
    } else if (name.equals("postgresql")) {
      return POSTGRESQL;
    } else if (name.equals("mysql") || name.equals("mariadb")) {
      return MYSQL;
    } else if (name.equals("sqlite")) {
      return SQLITE;
    } else if (name.equals("microsoft sql server")) {
      return SQL_SERVER;
    } else if (name.startsWith("oracle")) {
      return ORACLE;
    }
    return STANDARD;
  }

  /**
   * H2's column type for any serializable Java object, in which SQL evaluation stores the
   * attributes that have no column type, such as those of type {@code Other}. The values are bound
   * as {@code JAVA_OBJECT}: H2 2 rejects any value bound as {@code OTHER}, a string or an integer
   * included. An export does not use this type, since a value from another database need not be
   * serializable.
   */
  public static final ColumnType H2_JAVA_OBJECT = new ColumnType("java_object", Types.JAVA_OBJECT);

  /**
   * The column type of an attribute of type {@code cqlType}, or empty when the database has no
   * column type for it, as for {@code Other} and the JDBC types without a portable column type
   * ({@code Array}, {@code Struct}, {@code Ref}, ...). {@code varcharLength} sizes the variable
   * length types, as the {@code varchar_length} option does, and {@code size}, from {@link
   * #sizeOf}, sizes the numeric and fixed length ones.
   */
  public Optional<ColumnType> columnType(String cqlType, int varcharLength, Optional<Size> size) {
    int jdbcType;
    try {
      jdbcType = SqlTypeSide.getSqlType(cqlType);
    } catch (RuntimeException e) {
      return Optional.empty(); // Custom, or a type of another typeside
    }
    // Text is bound as a VARCHAR, but it is a column without a length
    int columnJdbcType = cqlType.equalsIgnoreCase("text") ? Types.LONGVARCHAR : jdbcType;
    String ddl = ddl(columnJdbcType, varcharLength, size);
    return ddl == null ? Optional.empty() : Optional.of(new ColumnType(ddl, bindType(jdbcType)));
  }

  /**
   * The size of a column of type {@code cqlType} that holds {@code values}, or empty for a type
   * whose column has no size, or when there are no values to size it from. A {@code Numeric} or
   * {@code Decimal} column gets the most digits before and after the point of any value, so that no
   * value is rounded, and a {@code Char}, {@code Nchar} or {@code Binary} column gets the longest
   * value.
   */
  public static Optional<Size> sizeOf(String cqlType, Iterable<?> values) {
    int jdbcType;
    try {
      jdbcType = SqlTypeSide.getSqlType(cqlType);
    } catch (RuntimeException e) {
      return Optional.empty();
    }
    switch (jdbcType) {
      case Types.NUMERIC:
      case Types.DECIMAL:
        return decimalSize(values);
      case Types.CHAR:
      case Types.NCHAR:
        return lengthOf(values, false);
      case Types.BINARY:
        return lengthOf(values, true);
      default:
        return Optional.empty();
    }
  }

  /**
   * The JDBC type to bind a value of JDBC type {@code jdbcType} with. CQL holds the values of large
   * objects in memory, as strings and byte arrays, so they are bound as the long types: the SQL
   * Server and PostgreSQL drivers do not convert a string to a {@code CLOB}. A {@code TINYINT} in a
   * {@code smallint} column is bound as a {@code SMALLINT}, since the SQL Server driver converts a
   * {@code TINYINT} to its unsigned tinyint and rejects a negative one. The PostgreSQL driver does
   * not accept the national character types as bind types at all.
   */
  private int bindType(int jdbcType) {
    boolean smallTinyint = this != H2 && this != MYSQL && this != SQLITE;
    int t =
        switch (jdbcType) {
          case Types.CLOB -> Types.LONGVARCHAR;
          case Types.NCLOB -> Types.LONGNVARCHAR;
          case Types.BLOB -> Types.LONGVARBINARY;
          case Types.TINYINT -> smallTinyint ? Types.SMALLINT : Types.TINYINT;
          default -> jdbcType;
        };
    if (this != POSTGRESQL) {
      return t;
    }
    return switch (t) {
      case Types.NCHAR -> Types.CHAR;
      case Types.NVARCHAR -> Types.VARCHAR;
      case Types.LONGNVARCHAR -> Types.LONGVARCHAR;
      default -> t;
    };
  }

  private static Optional<Size> decimalSize(Iterable<?> values) {
    int integerDigits = -1;
    int scale = 0;
    for (Object value : values) {
      BigDecimal d =
          value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
      if (d.scale() < 0) {
        d = d.setScale(0); // 1E+3 has scale -3, but needs four digits before the point
      }
      integerDigits = Math.max(integerDigits, Math.max(d.precision() - d.scale(), 0));
      scale = Math.max(scale, d.scale());
    }
    if (integerDigits < 0) {
      return Optional.empty();
    }
    return Optional.of(new Size(Math.max(integerDigits + scale, 1), scale));
  }

  /**
   * The longest value, in characters, or in bytes for a binary column. A binary column is sized
   * only from byte arrays, which is what an import reads. A {@code Binary} constant is parsed to a
   * string, and drivers store a string in a binary column differently: MySQL and H2 store its UTF-8
   * bytes, SQL Server reads it as hexadecimal, and PostgreSQL and MariaDB reject it.
   */
  private static Optional<Size> lengthOf(Iterable<?> values, boolean binary) {
    int length = 0;
    boolean any = false;
    for (Object value : values) {
      if (binary && !(value instanceof byte[])) {
        continue;
      }
      int n;
      if (binary) {
        n = ((byte[]) value).length;
      } else {
        String s = value.toString();
        n = s.codePointCount(0, s.length());
      }
      length = Math.max(length, n);
      any = true;
    }
    return any ? Optional.of(new Size(Math.max(length, 1), 0)) : Optional.empty();
  }

  private static String sized(String name, Optional<Size> size) {
    return size.map(s -> name + "(" + s.precision() + ")").orElse(name);
  }

  private static String decimal(String name, Optional<Size> size) {
    return size.map(s -> name + "(" + s.precision() + "," + s.scale() + ")").orElse(name);
  }

  /** The column type of {@code jdbcType} in this dialect, or null when it has none. */
  private String ddl(int jdbcType, int varcharLength, Optional<Size> size) {
    return switch (this) {
      case STANDARD -> standard(jdbcType, varcharLength, size);
      case H2 -> h2(jdbcType, varcharLength, size);
      case POSTGRESQL -> postgresql(jdbcType, varcharLength, size);
      case MYSQL -> mysql(jdbcType, varcharLength, size);
      case SQLITE -> sqlite(jdbcType, varcharLength, size);
      case SQL_SERVER -> sqlServer(jdbcType, varcharLength, size);
      case ORACLE -> oracle(jdbcType, varcharLength, size);
    };
  }

  private static String standard(int jdbcType, int varcharLength, Optional<Size> size) {
    return switch (jdbcType) {
      case Types.BIT, Types.BOOLEAN -> "boolean";
      case Types.TINYINT, Types.SMALLINT -> "smallint";
      case Types.INTEGER -> "integer";
      case Types.BIGINT -> "bigint";
      case Types.REAL -> "real";
      case Types.FLOAT, Types.DOUBLE -> "double precision"; // JDBC's FLOAT has double precision
      case Types.NUMERIC -> decimal("numeric", size);
      case Types.DECIMAL -> decimal("decimal", size);
      case Types.CHAR -> sized("char", size);
      case Types.VARCHAR -> "varchar(" + varcharLength + ")";
      case Types.LONGVARCHAR, Types.CLOB -> "clob";
      case Types.NCHAR -> sized("nchar", size);
      case Types.NVARCHAR -> "nvarchar(" + varcharLength + ")";
      case Types.LONGNVARCHAR, Types.NCLOB -> "nclob";
      case Types.BINARY -> sized("binary", size);
      case Types.VARBINARY -> "varbinary(" + varcharLength + ")";
      case Types.LONGVARBINARY, Types.BLOB -> "blob";
      case Types.DATE -> "date";
      case Types.TIME -> "time";
      case Types.TIMESTAMP -> "timestamp";
      case Types.TIME_WITH_TIMEZONE -> "time with time zone";
      case Types.TIMESTAMP_WITH_TIMEZONE -> "timestamp with time zone";
      case Types.SQLXML -> "xml";
      default -> null;
    };
  }

  /**
   * H2 keeps the column types the CQL type names gave it before there were dialects, so that SQL
   * evaluation stores values as it did, except that sized types now have their size.
   */
  private static String h2(int jdbcType, int varcharLength, Optional<Size> size) {
    return switch (jdbcType) {
      case Types.TINYINT -> "tinyint";
      case Types.LONGVARCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> "character varying";
      case Types.VARBINARY, Types.LONGVARBINARY -> "binary varying";
      case Types.SQLXML -> null; // H2 has no xml type
      default -> standard(jdbcType, varcharLength, size);
    };
  }

  private static String postgresql(int jdbcType, int varcharLength, Optional<Size> size) {
    return switch (jdbcType) {
      case Types.LONGVARCHAR, Types.CLOB, Types.LONGNVARCHAR, Types.NCLOB -> "text";
      case Types.NCHAR -> sized("char", size);
      case Types.NVARCHAR -> "varchar(" + varcharLength + ")";
      case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> "bytea";
      default -> standard(jdbcType, varcharLength, size);
    };
  }

  private static String mysql(int jdbcType, int varcharLength, Optional<Size> size) {
    return switch (jdbcType) {
      case Types.TINYINT -> "tinyint";
      case Types.REAL -> "float"; // MySQL's real is a double
      case Types.FLOAT, Types.DOUBLE -> "double";
      case Types.LONGVARCHAR, Types.CLOB, Types.LONGNVARCHAR, Types.NCLOB -> "longtext";
      case Types.LONGVARBINARY, Types.BLOB -> "longblob";
      case Types.TIME -> "time(6)"; // without a precision, fractional seconds are dropped
      case Types.TIMESTAMP -> "datetime(6)";
      case Types.TIME_WITH_TIMEZONE, Types.TIMESTAMP_WITH_TIMEZONE, Types.SQLXML -> null;
      default -> standard(jdbcType, varcharLength, size);
    };
  }

  /** SQLite accepts any name, but its driver reports a JDBC type only for the names it knows. */
  private static String sqlite(int jdbcType, int varcharLength, Optional<Size> size) {
    return switch (jdbcType) {
      case Types.TINYINT -> "tinyint";
      case Types.FLOAT, Types.DOUBLE -> "double";
      case Types.LONGVARCHAR, Types.CLOB, Types.LONGNVARCHAR, Types.NCLOB -> "text";
      case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> "blob";
      default -> standard(jdbcType, varcharLength, size);
    };
  }

  private static String sqlServer(int jdbcType, int varcharLength, Optional<Size> size) {
    return switch (jdbcType) {
      case Types.BIT, Types.BOOLEAN -> "bit";
      // TINYINT stays the standard smallint: SQL Server's tinyint holds 0 to 255, so the negative
      // values of a MySQL or H2 tinyint would fail to insert after the table is created
      case Types.FLOAT, Types.DOUBLE -> "float";
      case Types.LONGVARCHAR, Types.CLOB -> "varchar(max)";
      case Types.LONGNVARCHAR, Types.NCLOB -> "nvarchar(max)";
      case Types.LONGVARBINARY, Types.BLOB -> "varbinary(max)";
      case Types.TIMESTAMP -> "datetime2";
      case Types.TIMESTAMP_WITH_TIMEZONE -> "datetimeoffset";
      case Types.TIME_WITH_TIMEZONE -> null;
      default -> standard(jdbcType, varcharLength, size);
    };
  }

  /** Oracle is not tested; it keeps the one rename CQL made for it before there were dialects. */
  private static String oracle(int jdbcType, int varcharLength, Optional<Size> size) {
    return switch (jdbcType) {
      case Types.NUMERIC -> decimal("number", size);
      default -> standard(jdbcType, varcharLength, size);
    };
  }
}
