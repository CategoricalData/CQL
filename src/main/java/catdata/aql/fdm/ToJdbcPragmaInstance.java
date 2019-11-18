package catdata.aql.fdm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Instance;
import catdata.aql.Pragma;
import catdata.aql.Schema;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;

public class ToJdbcPragmaInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> extends Pragma {

	private final String jdbcString;
	private final String prefix;
	private final String idCol;
	private final int truncate;
	private final String tick;

	private final Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I;

	private final int len;
	private AqlOptions options;

	public ToJdbcPragmaInstance(String prefix, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I, 
			String jdbcString, AqlOptions options) {
		// try {
		this.jdbcString = jdbcString;
		this.prefix = prefix;
		this.I = I;
		idCol = (String) options.getOrDefault(AqlOption.id_column_name);
		len = (Integer) options.getOrDefault(AqlOption.varchar_length);
		truncate = (Integer) options.getOrDefault(AqlOption.jdbc_export_truncate_after);
		tick = (String) options.getOrDefault(AqlOption.jdbc_quote_char);
		this.options = options;

		assertDisjoint(idCol);
	}

	private void deleteThenCreate(Connection conn) throws SQLException {
		Map<En, Triple<List<Chc<Fk, Att>>, List<String>, List<String>>> m = I.schema().toSQL(prefix, "integer", idCol,
				truncate, Object::toString, len, tick);
		Statement stmt = conn.createStatement();
		for (En en : I.schema().ens) {
			for (String x : m.get(en).second) {
				// TODO aql drop foreign keys here first
				//System.out.println(x);
				stmt.execute(x); 
				
			}
		}
		stmt.close();
	}

	@Override
	public void execute() {
		try {
			Map<En, Triple<List<Chc<Fk, Att>>, List<String>, List<String>>> zzz = I.schema().toSQL(prefix, "integer",
					idCol, truncate, Object::toString, len, tick);
			// System.out.println(zzz);
			Connection conn = DriverManager.getConnection(jdbcString);
			deleteThenCreate(conn);
			Pair<TObjectIntMap<X>, TIntObjectMap<X>> II = I.algebra()
					.intifyX((int) options.getOrDefault(AqlOption.start_ids_at));
		
			for (En en : I.schema().ens) {
				List<Chc<Fk, Att>> header = headerFor(en);
				List<String> hdrQ = new ArrayList<>(header.size() + 1);
				List<String> hdr = new ArrayList<>(header.size() + 1);
				
				hdr.add(tick + idCol + tick);
				hdrQ.add("?");
				for (Chc<Fk, Att> aHeader : header) {
					hdrQ.add("?");
					Chc<Fk, Att> chc = aHeader;
					if (chc.left) {
						hdr.add(tick + Schema.truncate(chc.l.toString(), truncate) + tick); // TODO aql unsafe
					} else {
						hdr.add(tick + Schema.truncate(chc.r.toString(), truncate) + tick); // TODO aql unsafe
					}
				}
				for (X x : I.algebra().en(en)) {
					I.algebra().storeMyRecord(hdrQ, hdr, II, conn, x, header, enToString(en), prefix, tick);
				}
			}
			Statement stmt = conn.createStatement();
			for (En en : I.schema().ens) {
				for (String x : zzz.get(en).third) {
					stmt.execute(x);
				}
			}
			stmt.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private List<Chc<Fk, Att>> headerFor(En en) {
		List<Chc<Fk, Att>> ret = new LinkedList<>();
		for (Fk fk : I.schema().fksFrom(en)) {
			ret.add(Chc.inLeft(fk));
		}
		for (Att att : I.schema().attsFrom(en)) {
			ret.add(Chc.inRight(att));
		}
		return ret;
	}

	private void assertDisjoint(String idCol) {
		Collection<Object> entys = Util.isect(I.schema().ens, I.schema().typeSide.tys);
		if (!entys.isEmpty()) {
			throw new RuntimeException("Cannot JDBC export: entities and types share names: " + Util.sep(entys, ","));
		}
		Collection<Object> attfks = Util.isect(I.schema().atts.keySet(), I.schema().fks.keySet());
		if (!attfks.isEmpty()) {
			throw new RuntimeException(
					"Cannot JDBC export: attributes and foreign keys share names: " + Util.sep(attfks, ","));
		}
		if (I.schema().atts.keySet().contains(idCol)) {
			throw new RuntimeException("Cannot JDBC export: id column (" + idCol + ") is also an attribute");
		}
		if (I.schema().fks.keySet().contains(idCol)) {
			throw new RuntimeException("Cannot JDBC export: id column (" + idCol + ") is also a foreign key");
		}
	}

	private String enToString(En en) {
		return en.toString();
	}

	/*
	 * private String tyToString(Ty ty) { return (String) ty; }
	 */

	@Override
	public String toString() {
		return "Exported " + I.algebra().size() + " rows.";
	}

}
