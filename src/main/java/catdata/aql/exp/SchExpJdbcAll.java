package catdata.aql.exp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Program;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Eq;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.SqlTypeSide;
import catdata.aql.Term;
import catdata.aql.TypeSide;
import catdata.aql.Var;
import catdata.sql.SqlColumn;
import catdata.sql.SqlForeignKey;
import catdata.sql.SqlSchema;
import catdata.sql.SqlTable;

public class SchExpJdbcAll extends SchExp {

	private final Map<String, String> options;

	private final String clazz;
	private final String jdbcString;
	
	@Override
	public Map<String, String> options() {
		return options;
	}

	public SchExpJdbcAll(String clazz, String jdbcString, List<Pair<String, String>> options) {
		this.clazz = clazz;
		this.jdbcString = jdbcString;
		this.options = Util.toMapSafely(options);
	}

	
	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.addAll(AqlOptions.proverOptionNames());
		set.add(AqlOption.jdbc_default_class);
		set.add(AqlOption.jdbc_default_string);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.add(AqlOption.jdbc_quote_char);
	}
	

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder()
				.append("import_jdbc_all ")
				.append(Util.quote(clazz)).append(" ").append(Util.quote(jdbcString))
				;
		if (!options.isEmpty()) {
			sb.append(" {\n\t").append("\n\toptions\n\t\t")
				.append(Util.sep(options, " = ", "\n\t\t")).append("}");
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
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
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
		if (clazz == null) {
			if (other.clazz != null)
				return false;
		} else if (!clazz.equals(other.clazz))
			return false;
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
	public <R,P,E extends Exception> R accept(P param, SchExpVisitor<R,P,E> v) throws E {
		return v.visit(param, this);
	}
	
	@Override
	public <R, P, E extends Exception> SchExp coaccept(P params,
			SchExpCoVisitor<R, P, E> v, R r) throws E {
		return v.visitSchExpJdbcAll(params, r);
	}

	@Override
	public TyExp type(AqlTyping G) {
		return new TyExpSql();
	}
	
	public static String sqlTypeToAqlType(String s) {
		String x = s.toLowerCase();
		return x.substring(0, 1).toUpperCase() + x.substring(1, x.length());
	}
	
	

	@Override
	public Schema<Ty, En, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
		AqlOptions ops = new AqlOptions(options, null, env.defaults);
		
		String toGet = jdbcString;
		String tick = (String) ops.getOrDefault(AqlOption.jdbc_quote_char);
		if (jdbcString.trim().isEmpty()) {
			toGet = (String) ops.getOrDefault(AqlOption.jdbc_default_string);
		}
		TypeSide<Ty, Sym> typeSide = SqlTypeSide.SqlTypeSide(ops);
		
		Collage<Ty, En, Sym, Fk, Att, Void, Void> col0 = new Collage<>(typeSide.collage());
		Var v = Var.Var("x");
		
		try (Connection conn = DriverManager.getConnection(toGet)) {
			SqlSchema info = new SqlSchema(conn.getMetaData(), tick);

			for (SqlTable table : info.tables) {
				En x = En.En(table.name);
				col0.ens.add(x);
				for (SqlColumn c : table.columns) {
					col0.atts.put(Att.Att(x, c.name), new Pair<>(x, Ty.Ty(sqlTypeToAqlType(c.type.name))));
				}
			}
			for (SqlForeignKey fk : info.fks) {
				En x = En.En(fk.source.name);
				col0.fks.put(Fk.Fk(x,fk.toString()), new Pair<>(x, En.En(fk.target.name)));
				for (SqlColumn tcol : fk.map.keySet()) {
					SqlColumn scol = fk.map.get(tcol);
					Att l = Att.Att(En.En(scol.table.name), scol.name);
					Att r = Att.Att(En.En(tcol.table.name), tcol.name);
					Term<Ty, En, Sym, Fk, Att, Void, Void> lhs = Term.Att(l, Term.Var(v));
					Term<Ty, En, Sym, Fk, Att, Void, Void> rhs = Term.Att(r, Term.Fk(Fk.Fk(x, fk.toString()), Term.Var(v)));
					col0.eqs.add(new Eq<>(Collections.singletonMap(v, Chc.inRight(x)), lhs, rhs));
				}
			}

			return new Schema<>(typeSide, col0, new AqlOptions(options, col0, env.defaults));
			
		} catch (SQLException exn) {
			exn.printStackTrace();
			throw new RuntimeException("JDBC exception: " + exn.getMessage());
		} 
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		
	}

}
