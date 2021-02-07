package catdata.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import catdata.Util;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

//TODO: aql can store these not as sets, but as maps from primary key
public class SqlInstance {

	private final Map<SqlTable, Set<Map<SqlColumn, Optional<Object>>>> db = new THashMap<>();
	private final SqlSchema schema;
	private final String tick;
	// private final Connection conn;

	public Map<SqlColumn, Optional<Object>> follow(Map<SqlColumn, Optional<Object>> row, SqlForeignKey fk) {
		Map<SqlColumn, Optional<Object>> cand = new THashMap<>();

		for (SqlColumn tcol : fk.target.pk) {
			SqlColumn scol = fk.map.get(tcol);
			cand.put(tcol, row.get(scol));
		}
		Map<SqlColumn, Optional<Object>> ret = null;

		outer: for (Map<SqlColumn, Optional<Object>> tuple : get(fk.target)) {
			for (SqlColumn col : fk.target.pk) {
				if (!cand.get(col).equals(tuple.get(col))) {
					continue outer;
				}
			}
			if (ret != null) {
				throw new RuntimeException("Target is not actually a key: " + fk + " in " + row);
			}
			ret = tuple;
		}

		if (ret == null) {
			throw new RuntimeException("Could not resolve: " + fk + " in " + row);
		}

		return ret;
	}

	public Set<Map<SqlColumn, Optional<Object>>> get(SqlTable t) {
		if (!db.containsKey(t)) {
			throw new RuntimeException("Not a table: " + t.name);
		}
		return db.get(t);
	}

	public SqlInstance(SqlSchema schema, Connection conn, boolean errMeansNull, boolean useDistinct, String tick)
			throws SQLException {
		if (schema == null) {
			throw new RuntimeException("Null schema given to SqlInstance");
		}
		if (conn == null) {
			throw new RuntimeException("Null JDBC connection given to SqlInstance");
		}
		this.schema = schema;
		this.tick = tick;
		// this.conn = conn;
		String d = useDistinct ? "DISTINCT" : "";
		for (SqlTable table : schema.tables) {
			try (Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
				// System.out.println("SELECT " + d + " * FROM " + tick + table.name + tick);
				stmt.execute("SELECT " + d + " * FROM " + tick + table.name + tick);
				try (ResultSet resultSet = stmt.getResultSet()) {
					int sz = resultSet.getFetchSize();
					// int rowCount = resultSet.last() ? resultSet.getRow() : 0;
					// resultSet.first();
					Set<Map<SqlColumn, Optional<Object>>> rows = new THashSet<>(sz);
					while (resultSet.next()) {
						Map<SqlColumn, Optional<Object>> row = new THashMap<>(table.columns.size());
						for (SqlColumn col : table.columns) {
							try {
								Object o = resultSet.getObject(col.name);
								if (o != null) {
									row.put(col, Optional.of(o));
								} else {
									row.put(col, Optional.empty());
								}
							} catch (Exception ex) {
								ex.printStackTrace();
								if (errMeansNull) {
									System.err.println("Errors mean null - continuing import");
									System.err.flush();
									row.put(col, Optional.empty());
								} else {
									throw ex;
								}
							}
						}
						rows.add(row);
					}
					db.put(table, rows);
				}
			}
		}
	}

	@Override
	public String toString() {
		List<String> all = new LinkedList<>();

		for (SqlTable table : schema.tables) {
			if (db.get(table).isEmpty()) {
				continue;
			}
			List<String> x = new LinkedList<>();
			for (Map<SqlColumn, Optional<Object>> row : db.get(table)) {
				String y = "(";
				List<String> q = new LinkedList<>();
				for (SqlColumn col : table.columns) {
					Optional<Object> o = row.get(col);
					String z = o.isPresent() ? "'" + o.get() + "'" : "NULL";
					q.add(z);
				}
				y += Util.sep(q, ", ") + ")";
				x.add(y);
			}
			all.add("INSERT INTO " + tick + table.name + tick + " VALUES\n  " + Util.sep(x, ",\n  ") + ";");
		}

		return Util.sep(all, "\n\n");
	}

}
