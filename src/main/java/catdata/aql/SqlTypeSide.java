package catdata.aql;

import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.exp.Sym;
import gnu.trove.map.hash.THashMap;

//TODO CQL add dates
public class SqlTypeSide extends TypeSide<String, Sym> {

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

	public static Set<String> tys() {
		return Collections.unmodifiableSet(jts().keySet());
	}

	private static Set<Triple<Map<String, String>, Term<String, Void, Sym, Void, Void, Void, Void>, Term<String, Void, Sym, Void, Void, Void, Void>>> eqs() {
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
			case "double precision":
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

	public static Map<String, String> jts() {
		Map<String, String> m = new THashMap<>(32);
		m.put(("Longvarbinary"), "java.util.Optional"); // TODO aql
		m.put(("Varbinary"), "java.util.Optional"); // TODO aql
		m.put(("Binary"), "java.util.Optional"); // TODO aql
		m.put(("Bigint"), "java.util.Optional");
		m.put(("Boolean"), "java.util.Optional");
		m.put(("Bit"), "java.util.Optional");
		m.put(("Char"), "java.util.Optional"); // TODO aql
		m.put(("Double"), "java.util.Optional");
		m.put(("Double precision"), "java.util.Optional");
		m.put(("Decimal"), "java.util.Optional");
		m.put(("Numeric"), "java.util.Optional");
		m.put(("Float"), "java.util.Optional");
		m.put(("Real"), "java.util.Optional");
		m.put(("Integer"), "java.util.Optional");
		m.put(("Smallint"), "java.util.Optional");
		m.put(("Tinyint"), "java.util.Optional");
		m.put(("Text"), "java.util.Optional");
		m.put(("Nvarchar"), "java.util.Optional");
		m.put(("Varchar"), "java.util.Optional");
		m.put(("Longvarchar"), "java.util.Optional");
		m.put(("String"), "java.util.Optional");
		m.put(("Custom"), "java.util.Optional");
		m.put(("Blob"), "java.util.Optional");
		m.put(("Clob"), "java.util.Optional");
		m.put(("Other"), "java.util.Optional");
		m.put(("Date"), "java.util.Optional");
		m.put(("Time"), "java.util.Optional");
		m.put(("Timestamp"), "java.util.Optional");

		return m;
	}

	private static Map<String, String> jps() {
		Map<String, String> m = (new THashMap<>(32));

		final String ID = "x => java.util.Optional.of(x)";

		m.put(("Longvarbinary"), ID); // TODO CQL
		m.put(("Varbinary"), ID); // TODO CQL
		m.put(("Binary"), ID); // TODO CQL
		m.put(("Clob"), ID);
		m.put(("Date"), ID); // java.sql.Date.valueOf(input[0])");
		m.put(("Time"), "x => java.util.Optional.of(java.sql.Time.valueOf(x))");
		m.put(("Timestamp"), "x => java.util.Optional.of(java.sql.Timestamp.valueOf(x))");
		m.put(("Bigint"), "x => java.util.Optional.of(new java.lang.Long(x))");
		m.put(("Boolean"), "x => java.util.Optional.of(new java.lang.Boolean(x))");
		m.put(("Char"), ID); // TODO aql
		m.put(("Bit"), "x => java.util.Optional.of(new java.lang.Boolean(x))");
		m.put(("Double"), "x => java.util.Optional.of(new java.lang.Double(x))");
		m.put(("Double precision"), "x => java.util.Optional.of(new java.lang.Double(x))");
		m.put(("Numeric"), "x => java.util.Optional.of(new java.math.BigDecimal(x))");
		m.put(("Decimal"), "x => java.util.Optional.of(new java.math.BigDecimal(x))");
		m.put(("Real"), "x => java.util.Optional.of(new java.lang.Float(x))");
		m.put(("Float"), "x => java.util.Optional.of(new java.lang.Float(x))");
		m.put(("Integer"), "x => java.util.Optional.of(new java.lang.Integer(x))");
		m.put(("Tinyint"), "x => java.util.Optional.of(new java.lang.Integer(x))");
		m.put(("Smallint"), "x => java.util.Optional.of(new java.lang.Integer(x))");
		m.put(("Text"), ID);
		m.put(("String"), ID);
		m.put(("Nvarchar"), ID);
		m.put(("Varchar"), ID);
		m.put(("Longvarchar"), ID);
		m.put(("Custom"), ID);
		m.put(("Other"), ID);
		m.put(("Blob"), ID);

		return m;
	}

	public static <X> Optional<Boolean> isNull(Optional<X> x) {
		return Optional.of(x.isEmpty());
	}

	public static <X> Optional<Boolean> eq(Optional<X> x, Optional<X> y) {
		if (x.isEmpty() || y.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(x.get().equals(y.get()));
	}

	public static Optional<Boolean> and(Optional<Boolean> x, Optional<Boolean> y) {
		// U,F
		if (x.isEmpty() && !y.isEmpty() && !y.get()) {
			return Optional.of(false);
		}
		// F,U
		if (y.isEmpty() && !x.isEmpty() && !x.get()) {
			return Optional.of(false);
		}

		// U,T
		if (x.isEmpty() && !y.isEmpty() && y.get()) {
			return Optional.empty();
		}
		// T,U
		if (y.isEmpty() && !x.isEmpty() && x.get()) {
			return Optional.empty();
		}
		// U,U
		if (x.isEmpty() && y.isEmpty()) {
			return Optional.empty();
		}

		// F,F
		// F,T
		// T,F
		// T,T
		return Optional.of(x.get() && y.get());
	}

	public static Optional<Boolean> or(Optional<Boolean> x, Optional<Boolean> y) {
		// U,F
		if (x.isEmpty() && !y.isEmpty() && !y.get()) {
			return Optional.empty();
		}
		// F,U
		if (y.isEmpty() && !x.isEmpty() && !x.get()) {
			return Optional.empty();
		}

		// U,T
		if (x.isEmpty() && !y.isEmpty() && y.get()) {
			return Optional.of(true);
		}
		// T,U
		if (y.isEmpty() && !x.isEmpty() && x.get()) {
			return Optional.of(true);
		}
		// U,U
		if (x.isEmpty() && y.isEmpty()) {
			return Optional.empty();
		}

		// F,F
		// F,T
		// T,F
		// T,T
		return Optional.of(x.get() || y.get());
	}

	public static Optional<Boolean> not(Optional<Boolean> x) {
		if (x.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(!x.get());
	}

	public static Pair<List<String>, String> boolSort = new Pair<>(Collections.emptyList(), "Boolean");
	public static Sym t = Sym.Sym("true", boolSort);
	public static Sym f = Sym.Sym("false", boolSort);
	public static Sym n = Sym.Sym("null", boolSort);

	public static Pair<List<String>, String> boolSort1 = new Pair<>(Collections.singletonList("Boolean"), "Boolean");
	public static Pair<List<String>, String> boolSort2 = new Pair<>(Util.list("Boolean", "Boolean"), "Boolean");

	private static Map<Sym, String> jfs() {
		Map<Sym, String> m = Util.mk();

		// makes it easier than writing true@bool everywhere
		m.put(t, "x => java.util.Optional.of(true)");
		m.put(f, "x => java.util.Optional.of(false)");
		m.put(Sym.Sym("not", new Pair<>(Collections.singletonList("Boolean"), "Boolean")),
				"Java.type('catdata.aql.SqlTypeSide').not");
		m.put(Sym.Sym("and", new Pair<>(Util.list("Boolean", "Boolean"), "Boolean")),
				"Java.type('catdata.aql.SqlTypeSide').and");
		m.put(Sym.Sym("or", new Pair<>(Util.list("Boolean", "Boolean"), "Boolean")), "Java.type('catdata.aql.SqlTypeSide').or");

		for (String k : jps().keySet()) {
			m.put(Sym.Sym("null", new Pair<>(Collections.emptyList(), k)), "Java.type('java.util.Optional').empty");
			m.put(Sym.Sym("eq", new Pair<>(Util.list(k, k), "Boolean")), "Java.type('catdata.aql.SqlTypeSide').eq");
			m.put(Sym.Sym("isNull", new Pair<>(Collections.singletonList(k), "Boolean")),
					"Java.type('catdata.aql.SqlTypeSide').isNull");
		}
		return m;
	}

	public static Map<Sym, Pair<List<String>, String>> syms() {
		Map<Sym, Pair<List<String>, String>> m = Util.mk();
		m.put(t, boolSort);
		m.put(f, boolSort);
		m.put(Sym.Sym("not", new Pair<>(Collections.singletonList("Boolean"), "Boolean")),
				new Pair<>(Collections.singletonList("Boolean"), "Boolean"));
		m.put(Sym.Sym("and", new Pair<>(Util.list("Boolean", "Boolean"), "Boolean")),
				 new Pair<>(Util.list("Boolean", "Boolean"), "Boolean"));
		m.put(Sym.Sym("or", new Pair<>(Util.list("Boolean", "Boolean"), "Boolean")),  new Pair<>(Util.list("Boolean", "Boolean"), "Boolean"));

		for (String k : jps().keySet()) {
			Pair<List<String>, String> x = new Pair<>(Collections.emptyList(), k);
			m.put(Sym.Sym("null", x), x);
			m.put(Sym.Sym("eq", new Pair<>(Util.list(k,k), "Boolean")), new Pair<>(Util.list(k,k), "Boolean"));
			m.put(Sym.Sym("isNull", new Pair<>(Collections.singletonList(k), "Boolean")),  new Pair<>(Collections.singletonList(k), "Boolean"));
		}

		return m;
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
