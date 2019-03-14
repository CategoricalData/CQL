package catdata.aql.fdm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Pragma;
import catdata.aql.Query;

public class ToJdbcPragmaQuery<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> extends Pragma {

	private final String jdbcString;
	private final String prefixSrc;
	private final String prefixDst;
	private final String ty;
	// private final String clazz;
	private final String idCol;

	private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;
	private final String tick;

	public ToJdbcPragmaQuery(String prefixSrc, String prefixDst, Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q,
			String clazz, String jdbcString, AqlOptions options) {
		try {
			Class.forName(clazz);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		this.jdbcString = jdbcString;
		this.prefixSrc = prefixSrc;
		this.prefixDst = prefixDst;
		this.ty = (String) options.getOrDefault(AqlOption.jdbc_query_export_convert_type);
		this.Q = Q;
		idCol = (String) options.getOrDefault(AqlOption.id_column_name);
		this.tick = (String) options.getOrDefault(AqlOption.jdbc_quote_char);

		assertDisjoint(idCol);
	}

	@Override
	public void execute() {
		try {
			Connection conn = DriverManager.getConnection(jdbcString);
			Statement stmt = conn.createStatement();
			for (String s : Q.unnest().toSQLViews(prefixSrc, prefixDst, idCol, ty, tick).first) {
				stmt.execute(s);
			}
			stmt.close();
			conn.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void assertDisjoint(String idCol) {
		if (Q.dst.atts.keySet().contains(idCol)) {
			throw new RuntimeException("Cannot JDBC export: id column (" + idCol + ") is also an attribute");
		}
		if (Q.dst.fks.keySet().contains(idCol)) {
			throw new RuntimeException("Cannot JDBC export: id column (" + idCol + ") is also a foreign key");
		}
	}

	@Override
	public String toString() {
		return "export_jdbc_query "
				+ Util.sep(Q.unnest().toSQLViews(prefixSrc, prefixDst, idCol, ty, tick).first, "\n\n");
	}

}
