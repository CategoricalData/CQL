package catdata.cql.exp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Program;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Collage;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.SqlTypeSide;
import catdata.cql.TypeSide;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import catdata.sql.SqlColumn;
import catdata.sql.SqlSchema;
import catdata.sql.SqlTable;

public class SchExpJdbcAll extends SchExp {

	private final Map<String, String> options;

//  private final String clazz;
	private final String jdbcString;

	@Override
	public Map<String, String> options() {
		return options;
	}

	public SchExpJdbcAll(/* String clazz, */ String jdbcString, List<Pair<String, String>> options) {
		// this.clazz = clazz;
		this.jdbcString = jdbcString;
		this.options = Util.toMapSafely(options);
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.addAll(AqlOptions.proverOptionNames());
		set.add(AqlOption.jdbc_default_class);
		set.add(AqlOption.jdbc_default_string);
		set.add(AqlOption.oracle_schema_mode);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.add(AqlOption.jdbc_quote_char);
		set.add(AqlOption.allow_sql_import_all_unsafe);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("import_jdbc_all ").append(" ")
				.append(Util.quote(jdbcString));
		if (!options.isEmpty()) {
			sb.append(" {\n\t").append("\n\toptions\n\t\t").append(Util.sep(options, " = ", "\n\t\t")).append("}");
		}
		return sb.toString();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Collections.emptyList();
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		// result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SchExpJdbcAll other = (SchExpJdbcAll) obj;
		if (jdbcString == null) {
			if (other.jdbcString != null)
				return false;
		} else if (!jdbcString.equals(other.jdbcString))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

	@Override
	public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
		return this;
	}

	@Override
	public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
		return v.visitSchExpJdbcAll(params, r);
	}

	@Override
	public TyExp type(AqlTyping G) {
		return new TyExpSql();
	}

	public static String sqlTypeToAqlType(TypeSide<String, Sym> typeSide, String s) {
		String x = s.toLowerCase();
		String y = x.substring(0, 1).toUpperCase() + x.substring(1, x.length());
		if (!typeSide.tys.contains((y))) {
			return "Other";
		}
		return y;
	}

	@Override
	public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
		AqlOptions ops = new AqlOptions(options, env.defaults);
		boolean allow = (boolean) ops.getOrDefault(AqlOption.allow_sql_import_all_unsafe);
		if (!allow) {
			throw new RuntimeException(
					"Please use jdbc_import instead.  jdbc_import_all is best-effort only and unsound (reasoning on the resulting schema and data will not respect e.g. SQL null or key semantics). Set allow_sql_import_all_unsafe=true to continue.  Also, please see services.conexus.com for a more elaborate (and sound) mechanism to translate (some of) SQL to CQL.");
		}
		if (isC) {
			throw new IgnoreException();
		}

		String toGet = jdbcString;
		String tick = (String) ops.getOrDefault(AqlOption.jdbc_quote_char);
		if (jdbcString.isBlank()) {
			toGet = (String) ops.getOrDefault(AqlOption.jdbc_default_string);
		}
		boolean oracleSchMode = (Boolean) ops.getOrDefault(AqlOption.oracle_schema_mode);
		TypeSide<String, Sym> typeSide = SqlTypeSide.SqlTypeSide(ops);

		Collage<String, String, Sym, Fk, Att, Void, Void> col0 = new CCollage<>(typeSide.collage());
		try (Connection conn = DriverManager.getConnection(toGet)) {
			SqlSchema info = new SqlSchema(conn.getMetaData(), tick);
			for (SqlTable table : info.tables) {
				if (table.name.first != null && (table.name.first.toLowerCase().equals("system") || table.name.first.toLowerCase().equals("sys")
						|| table.name.first.toLowerCase().equals("ctxsys")
						|| table.name.first.toLowerCase().equals("xdb"))) {
					continue; // ignore system tables
				}
				String x = (table.name.second);
				if (table.name.first != null) {
					x = (table.name.first + "." + table.name.second);
				}
				if (table.name.first != null && table.name.first.toLowerCase().equals("dbo")) {
					x = table.name.second;
				}
			//	if (oracleSchMode) {
			//		x = x.toLowerCase();
			//	}
				col0.getEns().add(x);
				for (SqlColumn c : table.columns) {
					if (col0.atts().containsKey(Att.Att(x, c.name))) {
						throw new RuntimeException("Name collision: table " + c.table.name + " col " + c.name
								+ " against table "
								+ col0.atts().get(Att.Att((x), c.name)).first
								+ "\n\n.Possible solution: set option jdbc_import_col_seperator so as to avoid name collisions.");
					}

					col0.atts().put(Att.Att(x,  c.name), new Pair<>(x, (sqlTypeToAqlType(typeSide, c.type.name))));
				}
			}
			return new Schema<>(typeSide, col0, new AqlOptions(options, env.defaults));
		} catch (SQLException exn) {
			exn.printStackTrace();
			throw new RuntimeException("JDBC exception: " + exn.getMessage());
		}
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {

	}

}
