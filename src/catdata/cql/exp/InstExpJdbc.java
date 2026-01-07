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
import java.util.function.Consumer;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import catdata.LocStr;
import catdata.Pair;
import catdata.Util;
import catdata.cql.AqlJs;
import catdata.cql.AqlOptions;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage;
import catdata.cql.Term;
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

	public InstExpJdbc(TyExp schema, List<Pair<String, String>> options, String jdbcString,
			List<Pair<LocStr, String>> map) {
		super(schema, map, options);
		this.jdbcString = jdbcString;
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	protected synchronized Connection start(AqlJs<String, Sym> js, Collage<String, String, Sym, Fk, Att, Void, Void> sch) throws SQLException {
		for (String s : map.keySet()) {
			sch.getEns().add(s);
		}

		String toGet = jdbcString;

		if (jdbcString.trim().isEmpty()) {
			toGet = (String) op.getOrDefault(AqlOption.jdbc_default_string);
		}
		Connection conn = null;
		synchronized (DriverManager.class) {
			conn = DriverManager.getConnection(toGet);
		}

		tys0 = new THashMap<>(sch.tys().size(), 2);

		data = new THashMap<>(sch.getEns().size(), 2);

		for (String en : sch.getEns()) {
			data.put(en, new THashMap<>(8 * 1024, 1));
		}

		for (String ty : sch.tys()) {
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

	public static String sqlTypeToAqlType(Collage<String, String, Sym, Fk, Att, Void, Void>  typeSide, String s) {
		String x = s.toLowerCase();
		String y = x.substring(0, 1).toUpperCase() + x.substring(1, x.length());
	//	System.out.println(x + " and " + y);
		if (x.equals("character varying")) {
			return "String";
		}
		if (!typeSide.tys().contains((y))) {
			return "Other";
		}
		return y;
	}
	
	static int fresh=0;
	@Override
	protected synchronized void joinedEn(AqlJs<String, Sym> js, Connection conn, String en, String s,
			Collage<String, String, Sym, Fk, Att, Void, Void> sch) {
		

		try (Statement stmt = conn.createStatement()) {
		//	System.out.println(s);
			stmt.execute(s);
			ResultSet rs = stmt.getResultSet();
			ResultSetMetaData rsmd = rs.getMetaData();
			List<Att> atts = new LinkedList<>();

			int cols = rsmd.getColumnCount();
			for (int i = 0; i < cols; i++) {
				String att = rsmd.getColumnName(i+1);
				String ty = sqlTypeToAqlType(sch, rsmd.getColumnTypeName(i+1));
				var a = Att.Att(en, att);
				sch.atts().put(a, new Pair<>(en, ty));
				atts.add(a);
			}
			
			while (rs.next()) {
				Object gen = fresh++;
				String g1 = toGen(en, gen.toString()); // store strings
				
				data.get(en).put(g1, new Pair<>(new THashMap<>(0, 2),
						new THashMap<>(cols, 2)));
				
				for (Att att : atts) {
					Object rhs = rs.getObject(att.str);
					String ty = sch.atts().get(att).second;
					data.get(en).get(g1).second.put(att, objectToSk(rhs, g1, att, ty));
				}

			}
		} catch (SQLException ex) {
			System.out.println("-----");
			ex.printStackTrace();
			if (!(boolean) op.getOrDefault(AqlOption.import_null_on_err_unsafe)) {
				throw new RuntimeException("SQL error (consider option import_null_on_err_unsafe=true\n\n"
						+ ex.getMessage() + "\n\nQuery " + s);
			}
//      System.out.println("\n\nQuery " + s);
			
		}

	}

	/* private void checkColumns(String en, String s, Schema<String, String, Sym, Fk, Att> sch, ResultSetMetaData rsmd, boolean ignore)
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
	/*	for (Fk fk : sch.fksFrom(en)) {
			if (!Util.containsUpToCase(colNames, fk.str)) {
				throw new RuntimeException("Foreign key " + fk + " has no column in \n\n" + s);
			}
		} 
		if (!Util.containsUpToCase(colNames, idCol)) {
			throw new RuntimeException("No ID column " + idCol + " in \n\n" + s);
		} 
	} */

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

	private static <Z> Term<String, Void, Sym, Void, Void, Void, Z> objectToSk(
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
		return new SchExpInst<>(this);
	}

}
