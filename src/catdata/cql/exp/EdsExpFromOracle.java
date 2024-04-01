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
import catdata.Util;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;

public final class EdsExpFromOracle extends EdsExp {

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

	public EdsExpFromOracle(String url, SchExp S) {
		this.jdbcUrl = url;
		this.S = S;
	}

	@Override
	public Constraints eval0(AqlEnv env, boolean isC) {
		String s = """
								with constraint_colum_list as (select owner, table_name, constraint_name, listagg(column_name,',') WITHIN GROUP(order by position ) as column_list
								                 FROM USER_CONS_COLUMNS GROUP BY owner, table_name, constraint_name )
								select distinct c1.owner as sowner, c1.table_name as stbl, c1.constraint_name, c2.column_list as slist, c3.owner as towner, c3.table_name as ttbl, c3.constraint_name, c3.column_list as tlist
								from USER_CONSTRAINTS c1
								JOIN constraint_colum_list c2 ON c1.CONSTRAINT_NAME=C2.CONSTRAINT_NAME and c1.owner=c2.owner
								JOIN constraint_colum_list c3 ON C1.R_CONSTRAINT_NAME=C3.CONSTRAINT_NAME AND C1.R_OWNER= C3.owner
				where C1.constraint_type = 'R'				 """;
		
		String p = """
											with constraint_colum_list as (select owner, table_name, constraint_name, listagg(column_name,',') WITHIN GROUP(order by position ) as column_list
								                 FROM USER_CONS_COLUMNS GROUP BY owner, table_name, constraint_name )
								select distinct c1.owner as sowner, c1.table_name as stbl, c1.constraint_name, c2.column_list as slist
								from USER_CONSTRAINTS c1
								JOIN constraint_colum_list c2 ON c1.CONSTRAINT_NAME=C2.CONSTRAINT_NAME and c1.owner=c2.owner
				where C1.constraint_type = 'P' or C1.constraint_type = 'U'				 """;
		
		String toGet = jdbcUrl;
		Schema<String,String,Sym,Fk,Att> sch = S.eval(env, isC);
		List<ED> eds = new LinkedList<>(); 
		try {
			Connection conn = null;
			synchronized (DriverManager.class) {
				conn = DriverManager.getConnection(toGet);
			}

			Statement stmt = conn.createStatement();
			stmt.execute(s);
			ResultSet rs = stmt.getResultSet();

			while (rs.next()) {
				String stbl = rs.getString("stbl");
				String sowner = rs.getString("sowner");
				String[] slist = rs.getString("slist").split("\\,");
				String ttbl = rs.getString("ttbl");
				String towner = rs.getString("towner");
				String[] tlist = rs.getString("tlist").split("\\,");
				if (!sowner.equals(towner)) {
					throw new RuntimeException("Different owner in oracle import- please report");
				}
				if (!sch.ens.contains(sowner + "." + stbl)) {
					throw new RuntimeException("Not found: " + sowner + "." + stbl + " in " + Util.sep(sch.ens, ", "));
				}
				if (!sch.ens.contains(towner + "." + ttbl)) {
					throw new RuntimeException("Not found: " + towner + "." + ttbl + " in " + Util.sep(sch.ens, ", "));
				}
				if (slist.length != tlist.length) {
					Util.anomaly();
				}
				
				Map<String, Chc<String, String>> as = new HashMap<>();
				Map<String, Chc<String, String>> es = new HashMap<>();
				Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> ewh = new HashSet<>();
				
				as.put("s", Chc.inRight(sowner + "." + stbl));
				es.put("t", Chc.inRight(towner + "." + ttbl));
				
				for (int i = 0; i < slist.length; i++) {
					Term<String, String, Sym, Fk, Att, Void, Void> satt = Term.Att(Att.Att(sowner + "." + stbl, slist[i]), Term.Var("s"));
					Term<String, String, Sym, Fk, Att, Void, Void> tatt = Term.Att(Att.Att(towner + "." + ttbl, tlist[i]), Term.Var("t"));
					ewh.add(new Pair<>(satt, tatt));
				}
				
				ED ed = new ED(as, es, new HashSet<>(), ewh, true, env.defaults);
				eds.add(ed);
			}
			
			stmt.close();
			rs.close();
			
			stmt = conn.createStatement();
			stmt.execute(p);
			rs = stmt.getResultSet();

			while (rs.next()) {
				String stbl = rs.getString("stbl");
				String sowner = rs.getString("sowner");
				String[] slist = rs.getString("slist").split("\\,");

				
				Map<String, Chc<String, String>> as = new HashMap<>();
				Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> awh = new HashSet<>();
				
				as.put("x", Chc.inRight(sowner + "." + stbl));
				as.put("y", Chc.inRight(sowner + "." + stbl));

				
				for (int i = 0; i < slist.length; i++) {
					Term<String, String, Sym, Fk, Att, Void, Void> xatt = Term.Att(Att.Att(sowner + "." + stbl, slist[i]), Term.Var("x"));
					Term<String, String, Sym, Fk, Att, Void, Void> yatt = Term.Att(Att.Att(sowner + "." + stbl, slist[i]), Term.Var("y"));

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
		return "from_oracle " + jdbcUrl + " : " + S;
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
		EdsExpFromOracle other = (EdsExpFromOracle) obj;
		return Objects.equals(S, other.S) && Objects.equals(jdbcUrl, other.jdbcUrl);
	}

}
