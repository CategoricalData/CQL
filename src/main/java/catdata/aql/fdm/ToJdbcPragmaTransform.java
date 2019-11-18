package catdata.aql.fdm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Pragma;
import catdata.aql.Transform;
import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;

public class ToJdbcPragmaTransform<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> extends Pragma {

	private final String jdbcString;
	private final String prefix;
//	private final String clazz;
	private final String idCol;

	private final Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h;

	private final String colTy;
	private final int colTy0;

	private final AqlOptions options1;
	private final AqlOptions options2;

	// TODO aql column type mapping for jdbc instance export
	public ToJdbcPragmaTransform(String prefix, Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h,
			 String jdbcString, AqlOptions options1, AqlOptions options2) {
		
		this.jdbcString = jdbcString;
		this.prefix = prefix;
		this.h = h;
		// s this.clazz = clazz;
		idCol = (String) options1.getOrDefault(AqlOption.id_column_name);
		colTy = "VARCHAR(" + options1.getOrDefault(AqlOption.varchar_length) + ")";
		colTy0 = Types.VARCHAR;
		assertDisjoint();
		this.options1 = options1;
		this.options2 = options2;
	}

	private void deleteThenCreate(Connection conn) throws SQLException {
		for (En en : h.src().schema().ens) {
			Statement stmt = conn.createStatement();
			stmt.execute("DROP TABLE IF EXISTS " + prefix + enToString(en));
			String str = "src" + idCol + " " + colTy + " PRIMARY KEY NOT NULL";
			str += ", dst" + idCol + " " + colTy + " NOT NULL";
			stmt.execute("CREATE TABLE " + prefix + enToString(en) + "(" + str + ")");
			stmt.close();
		}
	}

	private void storeMyRecord(Pair<TObjectIntMap<X1>, TIntObjectMap<X1>> i,
			Pair<TObjectIntMap<X2>, TIntObjectMap<X2>> j, Connection conn, X1 x, String table, En en) throws Exception {
		List<String> hdrQ = new LinkedList<>();
		List<String> hdr = new LinkedList<>();
		hdr.add("src" + idCol);
		hdr.add("dst" + idCol);
		hdrQ.add("?");
		hdrQ.add("?");

		String insertSQL = "INSERT INTO " + table + "(" + Util.sep(hdr, ",") + ") values (" + Util.sep(hdrQ, ",") + ")";
		PreparedStatement ps = conn.prepareStatement(insertSQL);

		ps.setObject(1, i.first.get(x), colTy0);
		ps.setObject(2, j.first.get(h.repr(en, x)), colTy0);

		ps.executeUpdate();
		ps.close();
	}

	@Override
	public void execute() {
		try {
			Connection conn = DriverManager.getConnection(jdbcString);
			deleteThenCreate(conn);
			int s1 = (int) options1.getOrDefault(AqlOption.start_ids_at);
			int s2 = (int) options2.getOrDefault(AqlOption.start_ids_at);
			Pair<TObjectIntMap<X1>, TIntObjectMap<X1>> I = h.src().algebra().intifyX(s1);
			Pair<TObjectIntMap<X2>, TIntObjectMap<X2>> J = h.dst().algebra().intifyX(s2);
			for (En en : h.src().schema().ens) {
				for (X1 x : h.src().algebra().en(en)) {
					storeMyRecord(I, J, conn, x, prefix + enToString(en), en);
				}
			}
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void assertDisjoint() {
		Collection<Object> entys = Util.isect(h.src().schema().ens, h.src().schema().typeSide.tys);
		if (!entys.isEmpty()) {
			throw new RuntimeException(
					"Cannot JDBC export: entities and types and idcol share names: " + Util.sep(entys, ","));
		}

	}

	private String enToString(En en) {
		return en.convert();
	}

	@Override
	public String toString() {
		return "Exported " + h.size() + " rows.";
	}

}
