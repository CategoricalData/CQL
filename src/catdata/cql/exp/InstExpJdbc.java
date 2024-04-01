package catdata.cql.exp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import catdata.LocStr;
import catdata.Pair;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Schema;
import catdata.cql.SqlTypeSide;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import gnu.trove.map.hash.THashMap;

public class InstExpJdbc extends InstExpImport<Connection, String> {

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
		this.jdbcString = jdbcString;
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	protected synchronized Connection start(Schema<String, String, Sym, Fk, Att> sch) throws SQLException {
		if (!sch.typeSide.syms.keySet().containsAll(SqlTypeSide.syms().keySet())) {
			throw new RuntimeException("CSV import must be onto sql typeside.");
		}
		for (String s : map.keySet()) {
			if (!sch.ens.contains((s))) {
				throw new RuntimeException(s + " is not an entity in " + sch);
			}
		}

		String toGet = jdbcString;

		if (jdbcString.trim().isEmpty()) {
			toGet = (String) op.getOrDefault(AqlOption.jdbc_default_string);
		}
		Connection conn = null;
		synchronized (DriverManager.class) {
			conn = DriverManager.getConnection(toGet);
		}

		tys0 = new THashMap<>(sch.typeSide.tys.size(), 2);

		data = new THashMap<>(sch.ens.size(), 2);

		for (String en : sch.ens) {
			data.put(en, new THashMap<>(8 * 1024, 1));
		}

		for (String ty : sch.typeSide.tys) {
			tys0.put(ty, new LinkedList<>());
		}
		return conn;
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
		final StringBuilder sb = new StringBuilder().append("import_jdbc ").append(schema).append(" ").append(" ")
				.append(Util.quote(jdbcString));

		if (!options.isEmpty() || !map.isEmpty()) {
			sb.append(" {\n\t").append(Util.sep(map, " -> ", "\n\t", Util::quote));
			sb.append("\n\toptions\n\t\t").append(Util.sep(options, " = ", "\n\t\t"));
			sb.append("\n}");
		}
		return sb.toString();
	}

	@Override
	protected synchronized void joinedEn(Connection conn, String en, String s,
			Schema<String, String, Sym, Fk, Att> sch) {
		if (s == null) {
			if (!(boolean) op.getOrDefault(AqlOption.import_missing_is_empty)) {
				throw new RuntimeException("Missing query for entity: " + en + ". \n\nPossible options to consider: "
						+ AqlOption.import_missing_is_empty);
			}
			return;
		}

		try (Statement stmt = conn.createStatement()) {
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
				String g1 = toGen(en, gen.toString()); // store strings
				data.get(en).put(g1, new Pair<>(new THashMap<>(sch.fksFrom(en).size(), 2),
						new THashMap<>(sch.attsFrom(en).size(), 2)));

				for (Fk fk : sch.fksFrom(en)) {
					Object rhs = rs.getObject(fk.convert());

					if (rhs == null) {
						stmt.close();
						rs.close();
						conn.close();
						throw new RuntimeException("ID " + gen + " has a NULL foreign key value on " + fk);
					}
					String en2 = sch.fks.get(fk).second;
					String g2 = toGen(en2, rhs.toString()); // store strings

					data.get(en).get(g1).first.put(fk, g2);
				}
				for (Att att : sch.attsFrom(en)) {
					Object rhs = rs.getObject(att.convert());
					String ty = sch.atts.get(att).second;
					data.get(en).get(g1).second.put(att, objectToSk(sch, rhs, g1, att, ty));
				}

			}
		} catch (SQLException ex) {
			if (!(boolean) op.getOrDefault(AqlOption.import_null_on_err_unsafe)) {
				throw new RuntimeException("SQL error (consider option import_null_on_err_unsafe=true\n\n"
						+ ex.getMessage() + "\n\nQuery " + s);
			}
//      System.out.println("\n\nQuery " + s);
			ex.printStackTrace();
		}

	}

	private void checkColumns(String en, String s, Schema<String, String, Sym, Fk, Att> sch, ResultSetMetaData rsmd)
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
		return new HashCodeBuilder().append(jdbcString).append(map).append(options).append(schema).toHashCode();
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
		return new EqualsBuilder().append(jdbcString, rhs.jdbcString).append(map, rhs.map).append(options, rhs.options)
				.append(schema, rhs.schema).isEquals();
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.addAll(AqlOptions.proverOptionNames());
		set.add(AqlOption.jdbc_default_class);
		set.add(AqlOption.jdbc_default_string);
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

	private static <Z> Term<String, Void, Sym, Void, Void, Void, Z> objectToSk(Schema<String, String, Sym, Fk, Att> sch,
			Object rhs, String x, Att att, String ty) {
		if (rhs == null) {
			return Term.Obj(Optional.empty(), ty);
		}
		if (rhs instanceof Float) {
			double d = (double) (float) ((Float) rhs);
			rhs = (Double) d; //No floats in nashorn for only god knows why
		}
		return Term.Obj(Optional.of(rhs), ty);
	}

	@Override
	public SchExp type(AqlTyping G) {
		schema.type(G);
		return schema;
	}

}
