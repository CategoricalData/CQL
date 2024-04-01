package catdata.cql.exp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Quad;
import catdata.Util;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;

public final class EdsExpFromMySql extends EdsExp {

	String jdbcUrl;

	SchExp S;

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return S.deps();
	}

	public <R, P, E extends Exception> R accept(P param, EdsExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Map<String, String> options() {
		return Collections.emptyMap();
	}

	public EdsExpFromMySql(String url, SchExp S) {
		this.jdbcUrl = url;
		this.S = S;
	}

	@Override
	public Constraints eval0(AqlEnv env, boolean isC) {
		String s = """
								select fks.constraint_schema,
				fks.table_name as foreign_table,
				fks.unique_constraint_schema as referenced_schema,
				fks.referenced_table_name  as primary_table,
				kcu.ordinal_position as no,
				kcu.column_name as fk_column_name,
				kcu.referenced_column_name as pk_column_name,
				fks.constraint_name as fk_constraint_name
				from information_schema.referential_constraints fks
				join information_schema.key_column_usage kcu
				on fks.constraint_schema = kcu.table_schema
				and fks.table_name = kcu.table_name
				and fks.constraint_name = kcu.constraint_name
				where kcu.table_schema not in('information_schema','sys',
				              'mysql', 'performance_schema')
				and fks.constraint_schema = database()
				order by fks.constraint_schema,
				fks.table_name,
				kcu.ordinal_position;		 """;

		String p = """
										select stat.table_schema as database_name,
				       stat.table_name,
				       stat.index_name,
				       group_concat(stat.column_name
				            order by stat.seq_in_index separator ', ') as columns,
				       tco.constraint_type
				from information_schema.statistics stat
				join information_schema.table_constraints tco
				     on stat.table_schema = tco.table_schema
				     and stat.table_name = tco.table_name
				     and stat.index_name = tco.constraint_name
				where stat.non_unique = 0
					  and	stat.table_schema = database()
				      and stat.table_schema not in ('information_schema', 'sys',
				                                    'performance_schema', 'mysql')
				group by stat.table_schema,
				         stat.table_name,
				         stat.index_name,
				         tco.constraint_type
				order by stat.table_schema,
				         stat.table_name;			 """;

		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("through");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		String toGet = jdbcUrl;
		Schema<String, String, Sym, Fk, Att> sch = S.eval(env, isC);
		List<ED> eds = new LinkedList<>();
		try {
			Connection conn = null;
			synchronized (DriverManager.class) {
				conn = DriverManager.getConnection(toGet);
			}

			Statement stmt = conn.createStatement();
			stmt.execute(s);
			ResultSet rs = stmt.getResultSet();

			Map<String, List<Pair<String, String>>> map = new HashMap<>();
			Map<String, Pair<String, String>> map2 = new HashMap<>();
			while (rs.next()) {

				String tcol = rs.getString("fk_column_name");
				String scol = rs.getString("pk_column_name");
				String stbl = rs.getString("primary_table");
				String name = rs.getString("fk_constraint_name");
				// int no = rs.getInt("no");

				String sowner = rs.getString("constraint_schema");
				String ttbl = rs.getString("foreign_table");
				String towner = rs.getString("referenced_schema");

				if (!map2.containsKey(name)) {
					map2.put(name, new Pair<>(ttbl, stbl));
					map.put(name, new LinkedList<>());
				}

				map.get(name).add(new Pair<>(tcol, scol));

				if (!sowner.toLowerCase().equals(towner.toLowerCase())) {
					throw new RuntimeException("Different owner in mysql import- please report");
				}
				if (!sch.ens.contains(stbl)) {
					throw new RuntimeException("Not found: " + stbl + " in " + Util.sep(sch.ens, ", "));
				}
				if (!sch.ens.contains(ttbl)) {
					throw new RuntimeException("Not found: " + ttbl + " in " + Util.sep(sch.ens, ", "));
				}

			}

			stmt.close();
			rs.close();

			for (var name : map.keySet()) {
				var stbl = map2.get(name).first;
				var ttbl = map2.get(name).second;

				Map<String, Chc<String, String>> as = new HashMap<>();
				Map<String, Chc<String, String>> es = new HashMap<>();
				Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> ewh = new HashSet<>();

				as.put("s", Chc.inRight(stbl));
				es.put("t", Chc.inRight(ttbl));

				List<Pair<String, String>> list = map.get(name);

				for (Pair<String, String> ll : list) {
//				for (int i = 0; i < slist.length; i++) {
					Term<String, String, Sym, Fk, Att, Void, Void> satt = Term.Att(Att.Att(stbl, ll.first.trim()),
							Term.Var("s"));
					Term<String, String, Sym, Fk, Att, Void, Void> tatt = Term.Att(Att.Att(ttbl, ll.second.trim()),
							Term.Var("t"));
					ewh.add(new Pair<>(satt, tatt));
				}

				ED ed = new ED(as, es, new HashSet<>(), ewh, true, env.defaults);
				eds.add(ed);
			}

			stmt = conn.createStatement();
			stmt.execute(p);
			rs = stmt.getResultSet();

			while (rs.next()) {
				String stbl = rs.getString("table_name");
				// String sowner = rs.getString("database_name");
				String[] slist = rs.getString("columns").split("\\,");

				Map<String, Chc<String, String>> as = new HashMap<>();
				Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> awh = new HashSet<>();

				as.put("x", Chc.inRight(stbl));
				as.put("y", Chc.inRight(stbl));

				for (int i = 0; i < slist.length; i++) {
					Term<String, String, Sym, Fk, Att, Void, Void> xatt = Term.Att(Att.Att(stbl, slist[i].trim()),
							Term.Var("x"));
					Term<String, String, Sym, Fk, Att, Void, Void> yatt = Term.Att(Att.Att(stbl, slist[i].trim()),
							Term.Var("y"));

					awh.add(new Pair<>(xatt, yatt));
				}
				Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> ewh = new HashSet<>();
				ewh.add(new Pair<>(Term.Var("x"), Term.Var("y")));
				ED ed = new ED(as, new HashMap<>(), awh, ewh, false, env.defaults);
				eds.add(ed);
			}

			stmt.close();
			rs.close();

			conn.close();

			Constraints ctr = new Constraints(sch, eds, env.defaults);
			// ctr.asTransforms(ctr.schema);
			return ctr;

		} catch (SQLException ex) {
//      System.out.println("\n\nQuery " + s);
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}

	}

	@Override
	public String toString() {
		return "from_mysql " + jdbcUrl + " : " + S;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		S.mapSubExps(f);
	}

	@Override
	public SchExp type(AqlTyping G) {
		S.type(G);
		return S;
	}

	@Override
	public int hashCode() {
		return Objects.hash(S, jdbcUrl);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EdsExpFromMySql other = (EdsExpFromMySql) obj;
		return Objects.equals(S, other.S) && Objects.equals(jdbcUrl, other.jdbcUrl);
	}

}
