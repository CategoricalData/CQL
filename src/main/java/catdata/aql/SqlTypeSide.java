package catdata.aql;

import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;
import gnu.trove.map.hash.THashMap;

//TODO CQL add dates
public class SqlTypeSide extends TypeSide<Ty, Sym> {

	private static Map<AqlOptions, SqlTypeSide> cache = new THashMap<>();

	public synchronized static SqlTypeSide SqlTypeSide(AqlOptions ops) {
		SqlTypeSide ret = cache.get(ops);
		if (ret != null) {
			return ret;
		}
		ret = new SqlTypeSide(ops);
		cache.put(ops, ret);
		return ret;
	}

	/**
	 * Force language to JavaScript to match hard-coded parsers and functions.
	 */
	private static AqlOptions modifyOps(AqlOptions ops) {
		Map<String, String> opsMod = Util.mk();
		opsMod.put("graal_language", ExternalCodeUtils.LANG_JS);
		return new AqlOptions(opsMod, ops);
	}

	private SqlTypeSide(AqlOptions ops) {
		super(tys(), syms(), eqs(), jts(), jps(), jfs(), modifyOps(ops));
	}

	public static Set<Ty> tys() {
		return Collections.unmodifiableSet(jts().keySet());
	}

	public static Map<Sym, Pair<List<Ty>, Ty>> syms() {
		Map<Sym, Pair<List<Ty>, Ty>> m = Util.mk();
		m.put(Sym.Sym("true"), new Pair<>(Collections.emptyList(), Ty.Ty("Boolean")));
		m.put(Sym.Sym("false"), new Pair<>(Collections.emptyList(), Ty.Ty("Boolean")));
		return m;
	}

	private static Set<Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>>> eqs() {
		return Collections.emptySet();
	}

	// bit tinyint real numeric date time timestamp
	// TODO aql do this generically
	public static int getSqlType(String s) {
		switch (s.toLowerCase()) {
		case "varbinary":
			return Types.VARBINARY;
		case "longvarbinary":
			return Types.LONGVARBINARY;
		case "binary":
			return Types.BINARY;
		case "date":
			return Types.DATE;
		case "time":
			return Types.TIME;
		case "timestamp":
			return Types.TIMESTAMP;
		case "bigint":
			return Types.BIGINT;
		case "boolean":
			return Types.BOOLEAN;
		case "char":
			return Types.CHAR;
		case "double":
			return Types.DOUBLE;
		case "doubleprecision":
			return Types.DOUBLE;
		case "numeric":
			return Types.NUMERIC;
		case "decimal":
			return Types.DECIMAL;
		case "real":
			return Types.REAL;
		case "float":
			return Types.FLOAT;
		case "integer":
			return Types.INTEGER;
		case "tinyint":
			return Types.TINYINT;
		case "bit":
			return Types.BIT;
		case "smallint":
			return Types.SMALLINT;
		case "nvarchar":
			return Types.NVARCHAR;
		case "longvarchar":
			return Types.LONGVARCHAR;
		case "text":
			return Types.VARCHAR;
		case "varchar":
			return Types.VARCHAR;
		case "string":
			return Types.VARCHAR;
		case "blob":
			return Types.BLOB;
		case "other":
			return Types.OTHER;
		case "clob":
			return Types.CLOB;
		// case "long": return Types.Lo
		}
		// Types.
		throw new RuntimeException("Unknown sql type: " + s);
	}

	public static Map<Ty, String> jts() {
		Map<Ty, String> m = new THashMap<>(32);
		m.put(Ty.Ty("Longvarbinary"), "[B"); // TODO aql
		m.put(Ty.Ty("Varbinary"), "[B"); // TODO aql
		m.put(Ty.Ty("Binary"), "[B"); // TODO aql

		m.put(Ty.Ty("Bigint"), "java.lang.Long");
		m.put(Ty.Ty("Boolean"), "java.lang.Boolean");
		m.put(Ty.Ty("Bit"), "java.lang.Boolean");
		m.put(Ty.Ty("Char"), "java.lang.String"); // TODO aql
		m.put(Ty.Ty("Double"), "java.lang.Double");
		m.put(Ty.Ty("Doubleprecision"), "java.lang.Double");
		m.put(Ty.Ty("Decimal"), "java.math.BigDecimal");
		m.put(Ty.Ty("Numeric"), "java.math.BigDecimal");
		m.put(Ty.Ty("Float"), "java.lang.Float");
		m.put(Ty.Ty("Real"), "java.lang.Float");
		m.put(Ty.Ty("Integer"), "java.lang.Integer");
		m.put(Ty.Ty("Smallint"), "java.lang.Integer");
		m.put(Ty.Ty("Tinyint"), "java.lang.Integer");

		m.put(Ty.Ty("Text"), "java.lang.String");
		m.put(Ty.Ty("Nvarchar"), "java.lang.String");
		m.put(Ty.Ty("Varchar"), "java.lang.String");
		m.put(Ty.Ty("Longvarchar"), "java.lang.String");
		m.put(Ty.Ty("String"), "java.lang.String");
		m.put(Ty.Ty("Custom"), "java.lang.Object");
		m.put(Ty.Ty("Blob"), "java.lang.Object");
		m.put(Ty.Ty("Clob"), "java.lang.Object");
		m.put(Ty.Ty("Other"), "java.lang.Object");

		m.put(Ty.Ty("Date"), "java.lang.Object");
		m.put(Ty.Ty("Time"), "java.sql.Time");
		m.put(Ty.Ty("Timestamp"), "java.sql.Timestamp");

		return m;
	}

	private static Map<Ty, String> jps() {
		Map<Ty, String> m = (new THashMap<>(32));

		final String ID = "x => x";

		m.put(Ty.Ty("Longvarbinary"), ID); // TODO CQL
		m.put(Ty.Ty("Varbinary"), ID); // TODO CQL
		m.put(Ty.Ty("Binary"), ID); // TODO CQL

		m.put(Ty.Ty("Clob"), ID);
		m.put(Ty.Ty("Date"), ID); // java.sql.Date.valueOf(input[0])");
		m.put(Ty.Ty("Time"), "x => java.sql.Time.valueOf(x)");
		m.put(Ty.Ty("Timestamp"), "x => java.sql.Timestamp.valueOf(x)");

		m.put(Ty.Ty("Bigint"), "x => new java.lang.Long(x)");
		m.put(Ty.Ty("Boolean"), "x => new java.lang.Boolean(x)");
		m.put(Ty.Ty("Char"), ID); // TODO aql
		m.put(Ty.Ty("Bit"), "x => new java.lang.Boolean(x)");

		m.put(Ty.Ty("Double"), "x => new java.lang.Double(x)");
		m.put(Ty.Ty("Doubleprecision"), "x => new java.lang.Double(x)");
		m.put(Ty.Ty("Numeric"), "x => new java.math.BigDecimal(x)");

		m.put(Ty.Ty("Decimal"), "x => new java.math.BigDecimal(x)");
		m.put(Ty.Ty("Real"), "x => new java.lang.Float(x)");

		m.put(Ty.Ty("Float"), "x => new java.lang.Float(x)");
		m.put(Ty.Ty("Integer"), "x => new java.lang.Integer(x)");

		m.put(Ty.Ty("Tinyint"), "x => new java.lang.Integer(x)");
		m.put(Ty.Ty("Smallint"), "x => new java.lang.Integer(x)");
		m.put(Ty.Ty("Text"), ID);
		m.put(Ty.Ty("String"), ID);

		m.put(Ty.Ty("Nvarchar"), ID);
		m.put(Ty.Ty("Varchar"), ID);
		m.put(Ty.Ty("Longvarchar"), ID);
		m.put(Ty.Ty("Custom"), ID);
		m.put(Ty.Ty("Other"), ID);
		m.put(Ty.Ty("Blob"), ID);
		return m;
	}

	private static Map<Sym, String> jfs() {
		Map<Sym, String> m = new THashMap<>(2);
		m.put(Sym.Sym("true"), "x => true");
		m.put(Sym.Sym("false"), "x => false");
		return Collections.unmodifiableMap(m);

	}

	public static String mediate(int len, String t) {
		switch (t.toLowerCase()) {
		case "varchar":
			return "varchar(" + len + ")";
		case "string":
			return "varchar(" + len + ")";
		}
		return t;
	}

}
