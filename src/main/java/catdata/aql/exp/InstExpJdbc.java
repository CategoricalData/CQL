package catdata.aql.exp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Schema;
import gnu.trove.map.hash.THashMap;

//TODO this type is actually a lie bc of import_as_theory option
public class InstExpJdbc extends InstExpImport<Connection, String> {

	//public final String clazz;
	public final String jdbcString;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		this.schema.map(f);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.emptySet();
	}

	public InstExpJdbc(SchExp schema, List<Pair<String, String>> options, String jdbcString,
			List<Pair<LocStr, String>> map) {
		super(schema, map, options);
	//	this.clazz = clazz;
		this.jdbcString = jdbcString;
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	protected synchronized Connection start(Schema<Ty, En, Sym, Fk, Att> sch) throws SQLException {

		if (isJoined) {
			for (String s : map.keySet()) {
				if (!sch.ens.contains(En.En(s))) {
					throw new RuntimeException(s + " is not an entity in " + sch);
				}
			}
		}

		String toGet = jdbcString;
		//String driver = clazz;
		//if (clazz.trim().isEmpty()) {
		//	driver = (String) op.getOrDefault(AqlOption.jdbc_default_class);
			//Util.checkClass(driver);
	//	}
		if (jdbcString.trim().isEmpty()) {
			toGet = (String) op.getOrDefault(AqlOption.jdbc_default_string);
		}
		synchronized (DriverManager.class) {
			return DriverManager.getConnection(toGet);
		}
	}

	@Override
	protected void end(Connection conn) throws SQLException {

		conn.close();

	}

	@Override
	protected String getHelpStr() {
		return "";
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("import_jdbc ").append(schema).append(" ")
				.append(" ").append(Util.quote(jdbcString));

		if (!options.isEmpty() || !map.isEmpty()) {
			sb.append(" {\n\t").append(Util.sep(map, " -> ", "\n\t", Util::quote));
			sb.append("\n\toptions\n\t\t").append(Util.sep(options, " = ", "\n\t\t"));
			sb.append("\n}");
		}
		return sb.toString();
	}

	@Override
	protected void shreddedAtt(Connection conn, Att att, String s, Schema<Ty, En, Sym, Fk, Att> sch) throws Exception {
		Statement stmt = conn.createStatement();
		stmt.execute(s);
		ResultSet rs = stmt.getResultSet();
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnsNumber = rsmd.getColumnCount();
		if (columnsNumber != 2) {
			stmt.close();
			rs.close();
			conn.close();
			throw new RuntimeException("Error in " + att + ": Expected 2 columns but received " + columnsNumber);
		}
		while (rs.next()) {
			Object lhs = rs.getObject(1);
			if (lhs == null) {
				stmt.close();
				rs.close();
				conn.close();
				throw new RuntimeException("Error in " + att + ": Encountered a NULL column 1");
			}
			Object rhs = rs.getObject(2);
			En en = sch.atts.get(att).first;
			if (!atts0.containsKey(toGen(en, lhs.toString()))) {
				atts0.put(toGen(en, lhs.toString()), new THashMap<>());
			}
			atts0.get(toGen(en, lhs.toString())).put(att,
					objectToSk(sch, rhs, toGen(en, lhs.toString()), att, tys0, extraRepr, false, nullOnErr));
		}
		stmt.close();
		rs.close();
	}

	@Override
	protected void shreddedFk(Connection conn, Fk fk, String s, Schema<Ty, En, Sym, Fk, Att> sch) throws Exception {
		Statement stmt = conn.createStatement();
		stmt.execute(s);
		ResultSet rs = stmt.getResultSet();
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnsNumber = rsmd.getColumnCount();
		if (columnsNumber != 2) {
			stmt.close();
			rs.close();
			conn.close();
			throw new RuntimeException("Error in " + fk + ": Expected 2 columns but received " + columnsNumber);
		}
		while (rs.next()) {
			Object lhs = rs.getObject(1);
			if (lhs == null) {
				stmt.close();
				rs.close();
				conn.close();
				throw new RuntimeException("Error in " + fk + ": Encountered a NULL in column 1");
			}
			Object rhs = rs.getObject(2);
			if (rhs == null) {
				stmt.close();
				rs.close();
				conn.close();
				throw new RuntimeException("Error in " + fk + ": Encountered a NULL in column 2");
			}
			Gen g1 = toGen(sch.fks.get(fk).first, lhs.toString());
			Gen g2 = toGen(sch.fks.get(fk).second, rhs.toString()); // store strings
			if (!fks0.containsKey(g1)) {
				fks0.put(g1, new THashMap<>());
			}
			fks0.get(g1).put(fk, g2);
		}
		stmt.close();
		rs.close();
	}

	@Override
	protected void shreddedEn(Connection conn, En en, String s, Schema<Ty, En, Sym, Fk, Att> sch) throws Exception {
		Statement stmt = conn.createStatement();
		stmt.execute(s);
		ResultSet rs = stmt.getResultSet();
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnsNumber = rsmd.getColumnCount();
		if (columnsNumber != 1) {
			rs.close();
			stmt.close();
			conn.close();
			throw new RuntimeException("Expected 1 column but received " + columnsNumber);
		}
		while (rs.next()) {
			Object gen = rs.getObject(1);
			if (gen == null) {
				stmt.close();
				rs.close();
				conn.close();
				throw new RuntimeException("Encountered a NULL generator");
			}
			ens0.get(en).add(toGen(en, gen.toString())); // store strings
		}
		rs.close();
		stmt.close();
	}

	@Override
	protected synchronized void joinedEn(Connection conn, En en, String s, Schema<Ty, En, Sym, Fk, Att> sch)
			throws Exception {

		if (s == null) {
			if (!(boolean) op.getOrDefault(AqlOption.import_missing_is_empty)) {
				throw new RuntimeException("Missing query for entity: " + en + ". \n\nPossible options to consider: "
						+ AqlOption.import_missing_is_empty);
			}
			return;

		}
		Statement stmt = conn.createStatement();
		stmt.execute(s);
		ResultSet rs = stmt.getResultSet();
		ResultSetMetaData rsmd = rs.getMetaData();
		checkColumns(en, s, sch, rsmd);

		while (rs.next()) {
			Object gen = rs.getObject(idCol);
			if (gen == null) {
				stmt.close();
				rs.close();
				conn.close();
				throw new RuntimeException("Encountered a NULL generator in ID column " + idCol);
			}
			Gen g1 = toGen(en, gen.toString());
			ens0.get(en).add(g1); // store strings

			for (Fk fk : sch.fksFrom(en)) {
				Object rhs = rs.getObject(fk.convert());

				if (rhs == null && !import_as_theory) {
					stmt.close();
					rs.close();
					conn.close();
					throw new RuntimeException("ID " + gen + " has a NULL foreign key value on " + fk);
				}
				En en2 = sch.fks.get(fk).second;
				if (rhs != null) {
					Gen g2 = toGen(en2, rhs.toString()); // store strings
					ens0.get(en2).add(g2);

					if (!fks0.containsKey(g1)) {
						fks0.put(g1, new THashMap<>());
					}
					fks0.get(g1).put(fk, g2);
				}
			}
			for (Att att : sch.attsFrom(en)) {
				Object rhs = rs.getObject(att.convert());
				if (!atts0.containsKey(g1)) {
					atts0.put(g1, new THashMap<>());
				}

				atts0.get(g1).put(att, objectToSk(sch, rhs, g1, att, tys0, extraRepr, false, nullOnErr));
			}

		}

	}

	private void checkColumns(En en, String s, Schema<Ty, En, Sym, Fk, Att> sch, ResultSetMetaData rsmd)
			throws SQLException {
		Set<String> colNames = new TreeSet<>();
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			String colName = rsmd.getColumnLabel(i);
			if (!(colName.equalsIgnoreCase(idCol)
					|| Util.containsUpToCase(sch.attsFrom(en).stream().map(x -> x.str).collect(Collectors.toList()),
							colName)
					|| Util.containsUpToCase(sch.fksFrom(en).stream().map(x -> x.str).collect(Collectors.toList()),
							colName))) {
				throw new RuntimeException(
						"Column name " + colName + " does not refer to a foreign key or attribute in \n\n" + s);
			}
			colNames.add(colName);
		}
		for (Att att : sch.attsFrom(en)) {
			if (!Util.containsUpToCase(colNames, att.str)) {
				throw new RuntimeException("Attribute " + att + " has no column in \n\n" + s);
			}
		}
		for (Fk fk : sch.fksFrom(en)) {
			if (!Util.containsUpToCase(colNames, fk.str)) {
				throw new RuntimeException("Foreign key " + fk + " has no column in \n\n" + s);
			}
		}
		if (!Util.containsUpToCase(colNames, idCol)) {
			throw new RuntimeException("No ID column " + idCol + " in \n\n" + s);
		}
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(jdbcString).append(map).append(options).append(schema)
				.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		InstExpJdbc rhs = (InstExpJdbc) obj;
		return new EqualsBuilder().append(jdbcString, rhs.jdbcString).append(map, rhs.map)
				.append(options, rhs.options).append(schema, rhs.schema).isEquals();
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.addAll(AqlOptions.proverOptionNames());
		set.add(AqlOption.jdbc_default_class);
		set.add(AqlOption.jdbc_default_string);
		set.add(AqlOption.import_as_theory);
		set.add(AqlOption.id_column_name);
		set.add(AqlOption.import_missing_is_empty);
		set.add(AqlOption.import_null_on_err_unsafe);
		set.add(AqlOption.import_col_seperator);
		set.add(AqlOption.csv_import_prefix);
		set.add(AqlOption.import_dont_check_closure_unsafe);
		set.add(AqlOption.prepend_entity_on_ids);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.jdbc_quote_char);
	}

}
