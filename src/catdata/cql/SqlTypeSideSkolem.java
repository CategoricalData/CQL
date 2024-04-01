package catdata.cql;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.exp.Att;
import catdata.cql.exp.Fk;
import catdata.cql.exp.Sym;
import gnu.trove.map.hash.THashMap;

//TODO CQL add dates
public class SqlTypeSideSkolem extends TypeSide<String, Sym> {

	private static Map<AqlOptions, SqlTypeSideSkolem> cache = new THashMap<>();

	public synchronized static SqlTypeSideSkolem SqlTypeSideSkolem(AqlOptions ops) {
		SqlTypeSideSkolem ret = cache.get(ops);
		if (ret != null) {
			return ret;
		}
		ret = new SqlTypeSideSkolem(ops);
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

	private SqlTypeSideSkolem(AqlOptions ops) {
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
		m.put(("Longvarbinary"), "java.lang.Object"); // TODO aql
		m.put(("Varbinary"), "java.lang.Object"); // TODO aql
		m.put(("Binary"), "java.lang.Object"); // TODO aql
		m.put(("Bigint"), "java.lang.Long");
		m.put(("Boolean"), "java.lang.Optional"); 
		m.put(("Bit"), "java.lang.Boolean");
		m.put(("Char"), "java.lang.Object"); // TODO aql
		m.put(("Double"), "java.lang.Double");
		m.put(("Double precision"), "java.lang.Double");
		m.put(("Decimal"), "java.math.BigDecimal");
		m.put(("Numeric"), "java.math.BigDecimal");
		m.put(("Float"), "java.lang.Float");
		m.put(("Real"), "java.lang.Double");
		m.put(("Integer"), "java.lang.Integer");
		m.put(("Smallint"), "java.lang.Integer");
		m.put(("Tinyint"), "java.lang.Integer");
		m.put(("Text"), "java.lang.String");
		m.put(("Nvarchar"), "java.lang.String");
		m.put(("Varchar"), "java.lang.String");
		m.put(("Longvarchar"), "java.lang.String");
		m.put(("String"), "java.lang.String");
		m.put(("Custom"), "java.lang.Object");
		m.put(("Blob"), "java.lang.Object");
		m.put(("Clob"), "java.lang.Object");
		m.put(("Other"), "java.lang.Object");
		m.put(("Date"), "java.lang.Object");
		m.put(("Time"), "java.lang.Object");
		m.put(("Timestamp"), "java.lang.Object");

		return m;
	}

	private static Map<String, String> jps() {
		Map<String, String> m = (new THashMap<>(32));

		final String ID = "x => (x)";

		m.put(("Longvarbinary"), ID); // TODO CQL
		m.put(("Varbinary"), ID); // TODO CQL
		m.put(("Binary"), ID); // TODO CQL
		m.put(("Clob"), ID);
		m.put(("Date"), ID); // java.sql.Date.valueOf(input[0])");
		m.put(("Time"), "x => (java.sql.Time.valueOf(x))");
		m.put(("Timestamp"), "x => (java.sql.Timestamp.valueOf(x))");
		m.put(("Bigint"), "x => (new java.lang.Long(x))");
		m.put(("Boolean"), "x => (new java.lang.Boolean(x))");
		m.put(("Char"), ID); // TODO aql
		m.put(("Bit"), "x => (new java.lang.Boolean(x))");
		m.put(("Double"), "x => (new java.lang.Double(x))");
		m.put(("Double precision"), "x => (new java.lang.Double(x))");
		m.put(("Numeric"), "x => (new java.math.BigDecimal(x))");
		m.put(("Decimal"), "x => (new java.math.BigDecimal(x))");
		m.put(("Real"), "x => (new java.lang.Double(x))");
		m.put(("Float"), "x => (new java.lang.Float(x))");
		m.put(("Integer"), "x => (new java.lang.Integer(x))");
		m.put(("Tinyint"), "x => (new java.lang.Integer(x))");
		m.put(("Smallint"), "x => (new java.lang.Integer(x))");
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

	public static Float plusFloat(Float x, Float y) {
		return x + y;
	}
	public static BigDecimal plusBigDecimal(BigDecimal x, BigDecimal y) {
		return x.add(y);
	}
	public static Integer plusInteger(Integer x, Integer y) {
		return x + y;
	}
	public static Double plusDouble(Double x, Double y) {
		return x + y;
	}
	
	
	
	public static <X> Optional<Boolean> isFalse(Optional<Boolean> x) {
		return Optional.of(!x.isEmpty() && !x.get());
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
	
	public static <X> Optional<Boolean> eq(Optional<X> x, Optional<X> y) {
		if (x.isEmpty() || y.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(x.get().equals(y.get()));
	}
	public static <X> Optional<Boolean> isNull(Optional<X> x) {
		return Optional.of(x.isEmpty());
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
		m.put(Sym.Sym("isFalse", new Pair<>(Collections.singletonList("Boolean"), "Boolean")),
				"Java.type('catdata.aql.SqlTypeSide').isFalse");
		for (String k : jps().keySet()) {
			m.put(Sym.Sym("null", new Pair<>(Collections.emptyList(), k)), "Java.type('java.util.Optional').empty");
			m.put(Sym.Sym("eq", new Pair<>(Util.list(k, k), "Boolean")), "Java.type('catdata.aql.SqlTypeSide').eq");
			m.put(Sym.Sym("isNull", new Pair<>(Collections.singletonList(k), "Boolean")),
					"Java.type('catdata.aql.SqlTypeSide').isNull");
		}
		List<String> l = new LinkedList<>();
		l.add("Float");
		l.add("Float");
		
		m.put(Sym.Sym("+", new Pair<>(l, "Float")),
				"Java.type('catdata.aql.SqlTypeSide').plusFloat");
		
		List<String> l2 = new LinkedList<>();
		l2.add("Real");
		l2.add("Real");
		
		m.put(Sym.Sym("+", new Pair<>(l2, "Real")),
				"Java.type('catdata.aql.SqlTypeSide').plusDouble");
		
		List<String> l3 = new LinkedList<>();
		l3.add("Double");
		l3.add("Double");
		
		m.put(Sym.Sym("+", new Pair<>(l3, "Double")),
				"Java.type('catdata.aql.SqlTypeSide').plusDouble");
		
		
		List<String> l4 = new LinkedList<>();
		l4.add("Double precision");
		l4.add("Double precision");
		m.put(Sym.Sym("+", new Pair<>(l4, "Double precision")),
				"Java.type('catdata.aql.SqlTypeSide').plusDouble");

		List<String> l5 = new LinkedList<>();
		l5.add("Numeric");
		l5.add("Numeric");
		m.put(Sym.Sym("+", new Pair<>(l5, "Numeric")),
				"Java.type('catdata.aql.SqlTypeSide').plusBigDecimal");

		List<String> l6 = new LinkedList<>();
		l6.add("Numeric");
		l6.add("Numeric");
		m.put(Sym.Sym("+", new Pair<>(l6, "Decimal")),
				"Java.type('catdata.aql.SqlTypeSide').plusBigDecimal");
		
		List<String> l7 = new LinkedList<>();
		l7.add("Integer");
		l7.add("Integer");
		m.put(Sym.Sym("+", new Pair<>(l7, "Integer")),
				"Java.type('catdata.aql.SqlTypeSide').plusInteger");
		
		List<String> l8 = new LinkedList<>();
		l8.add("Tinyint");
		l8.add("Tinyint");
		m.put(Sym.Sym("+", new Pair<>(l8, "Tinyint")),
				"Java.type('catdata.aql.SqlTypeSide').plusInteger");
		
		List<String> l9 = new LinkedList<>();
		l9.add("Smallint");
		l9.add("Smallint");
		m.put(Sym.Sym("+", new Pair<>(l9, "Smallint")),
				"Java.type('catdata.aql.SqlTypeSide').plusInteger");

		
//		m.put(("Integer"), "x => java.util.Optional.of(new java.lang.Integer(x))");
//		m.put(("Tinyint"), "x => java.util.Optional.of(new java.lang.Integer(x))");
//		m.put(("Smallint"), "x => java.util.Optional.of(new java.lang.Integer(x))");

		
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
		m.put(Sym.Sym("or", new Pair<>(Util.list("Boolean", "Boolean"), "Boolean")),
				new Pair<>(Util.list("Boolean", "Boolean"), "Boolean"));

		for (String k : jps().keySet()) {
			Pair<List<String>, String> x = new Pair<>(Collections.emptyList(), k);
		//	m.put(Sym.Sym("null", x), x);
		//	m.put(Sym.Sym("eq", new Pair<>(Util.list(k, k), "Boolean")), new Pair<>(Util.list(k, k), "Boolean"));
		//	m.put(Sym.Sym("isNull", new Pair<>(Collections.singletonList(k), "Boolean")),
		//			new Pair<>(Collections.singletonList(k), "Boolean"));
		//	m.put(Sym.Sym("isFalse", new Pair<>(Collections.singletonList(k), "Boolean")),
		//			new Pair<>(Collections.singletonList(k), "Boolean"));
		}
		
		List<String> l = new LinkedList<>();
		l.add("Float");
		l.add("Float");
		
		m.put(Sym.Sym("+", new Pair<>(l, "Float")), new Pair<>(l, "Float"));
		
		List<String> l2 = new LinkedList<>();
		l2.add("Real");
		l2.add("Real");
		
		m.put(Sym.Sym("+", new Pair<>(l2, "Real")),
				new Pair<>(l2, "Real"));
		
		List<String> l3 = new LinkedList<>();
		l3.add("Double");
		l3.add("Double");
		
		m.put(Sym.Sym("+", new Pair<>(l3, "Double")),
				new Pair<>(l3, "Double"));
		
		
		List<String> l4 = new LinkedList<>();
		l4.add("Double precision");
		l4.add("Double precision");
		m.put(Sym.Sym("+", new Pair<>(l4, "Double precision")),
				 new Pair<>(l4, "Double precision"));

		List<String> l5 = new LinkedList<>();
		l5.add("Numeric");
		l5.add("Numeric");
		m.put(Sym.Sym("+", new Pair<>(l5, "Numeric")),
				 new Pair<>(l5, "Numeric"));

		List<String> l6 = new LinkedList<>();
		l6.add("Numeric");
		l6.add("Numeric");
		m.put(Sym.Sym("+", new Pair<>(l6, "Decimal")),
				 new Pair<>(l6, "Decimal"));
		
		List<String> l7 = new LinkedList<>();
		l7.add("Integer");
		l7.add("Integer");
		m.put(Sym.Sym("+", new Pair<>(l7, "Integer")),
				new Pair<>(l7, "Integer"));
		
		List<String> l8 = new LinkedList<>();
		l8.add("Tinyint");
		l8.add("Tinyint");
		m.put(Sym.Sym("+", new Pair<>(l8, "Tinyint")),
				new Pair<>(l8, "Tinyint"));
		
		List<String> l9 = new LinkedList<>();
		l9.add("Smallint");
		l9.add("Smallint");
		m.put(Sym.Sym("+", new Pair<>(l9, "Smallint")),
				new Pair<>(l9, "Smallint"));

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

	public static Constraints makeEds(Schema<String, String, Sym, Fk, Att> schT, AqlOptions op) {
		LinkedList<ED> ret = new LinkedList<>();
		String x = ("x");
		String y = ("y");
		String z = ("z");
		Term<String, String, Sym, Fk, Att, Void, Void> t = Term.Sym(SqlTypeSide.t, Collections.emptyList());
		Term<String, String, Sym, Fk, Att, Void, Void> f = Term.Sym(SqlTypeSide.f, Collections.emptyList());

		List<Term<String, String, Sym, Fk, Att, Void, Void>> lxx = new ArrayList<>(2);
		lxx.add(Term.Var(x));
		lxx.add(Term.Var(x));

		List<Term<String, String, Sym, Fk, Att, Void, Void>> lxy = new ArrayList<>(2);
		lxy.add(Term.Var(x));
		lxy.add(Term.Var(y));

		List<Term<String, String, Sym, Fk, Att, Void, Void>> lyx = new ArrayList<>(2);
		lyx.add(Term.Var(y));
		lyx.add(Term.Var(x));

		List<Term<String, String, Sym, Fk, Att, Void, Void>> lyz = new ArrayList<>(2);
		lyz.add(Term.Var(y));
		lyz.add(Term.Var(z));

		List<Term<String, String, Sym, Fk, Att, Void, Void>> lxz = new ArrayList<>(2);
		lxz.add(Term.Var(x));
		lxz.add(Term.Var(z));

		for (String ty : schT.typeSide.tys) {
			if (ty.equals("Bool")) {
				continue;
			}
			var ll = new ArrayList<String>(2);
			ll.add(ty);
			ll.add(ty);
			Pair<List<String>, String> p = new Pair<>(ll, "Boolean");
			var sss = Sym.Sym("eq", p);
			Term<String, String, Sym, Fk, Att, Void, Void> xx = Term.Sym(sss, lxx);
			Term<String, String, Sym, Fk, Att, Void, Void> xx0 = Term.Sym(Sym.Sym("isFalse", SqlTypeSide.boolSort1),
					Util.list(xx));
			ret.add(new ED(Collections.singletonMap(x, Chc.inLeft(ty)), Collections.emptyMap(), Collections.emptySet(),
					Collections.singleton(new Pair<>(xx0, f)), false, op));

			Term<String, String, Sym, Fk, Att, Void, Void> xy = Term.Sym(sss, lxy);
			Term<String, String, Sym, Fk, Att, Void, Void> yx = Term.Sym(sss, lyx);
			Map<String, Chc<String, String>> m2 = new THashMap<>(2, 2);
			m2.put(x, Chc.inLeft(ty));
			m2.put(y, Chc.inLeft(ty));
			ret.add(new ED(m2, Collections.emptyMap(), Collections.emptySet(), Collections.singleton(new Pair<>(xy, yx)), false,
					op));

			Map<String, Chc<String, String>> m3 = new THashMap<>(2, 2);
			m3.put(x, Chc.inLeft(ty));
			m3.put(y, Chc.inLeft(ty));
			m3.put(z, Chc.inLeft(ty));

			Term<String, String, Sym, Fk, Att, Void, Void> xy0 = Term.Sym(sss, Util.list(Term.Var(x), Term.Var(y)));

			ret.add(new ED(m2, Collections.emptyMap(), Collections.singleton(new Pair<>(xy0, t)),
					Collections.singleton(new Pair<>(Term.Var(x), Term.Var(y))), false, op));
			
			// other congruences
			// todo: eq(a,b)=false -> x<>y?

		}

		return new Constraints(schT, ret, op);
	} 

}
