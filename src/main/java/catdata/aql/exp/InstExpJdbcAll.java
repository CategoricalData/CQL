package catdata.aql.exp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Null;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.SqlTypeSide;
import catdata.aql.Term;
import catdata.aql.TypeSide;
import catdata.aql.Var;
import catdata.aql.fdm.ImportAlgebra;
import catdata.aql.fdm.SaturatedInstance;
import catdata.sql.SqlColumn;
import catdata.sql.SqlForeignKey;
import catdata.sql.SqlInstance;
import catdata.sql.SqlSchema;
import catdata.sql.SqlTable;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class InstExpJdbcAll extends InstExp<Gen, Null<?>, Gen, Null<?>> {

	private final Map<String, String> options;

	private final String clazz;
	private final String jdbcString;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
	}

	public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.emptySet();
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public InstExpJdbcAll(String clazz, String jdbcString, List<Pair<String, String>> options) {
		this.clazz = clazz;
		this.jdbcString = jdbcString;
		this.options = Util.toMapSafely(options);
	}

	public static String sqlTypeToAqlType(String s) {
		String x = s.toLowerCase();
		return x.substring(0, 1).toUpperCase() + x.substring(1, x.length());
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.addAll(AqlOptions.proverOptionNames());
		set.add(AqlOption.jdbc_default_class);
		set.add(AqlOption.jdbc_default_string);
		set.add(AqlOption.import_null_on_err_unsafe);
		set.add(AqlOption.import_dont_check_closure_unsafe);
		set.add(AqlOption.allow_java_eqs_unsafe);
		set.add(AqlOption.require_consistency);
		set.add(AqlOption.jdbc_quote_char);
	}

	private Instance<Ty, En, Sym, Fk, Att, Gen, Null<?>, Gen, Null<?>> toInstance(AqlEnv env, SqlInstance inst,
			SqlSchema info) {
		AqlOptions ops = new AqlOptions(options, null, env.defaults);
		Schema<Ty, En, Sym, Fk, Att> sch = makeSchema(env, info, ops);

		Map<En, Collection<Gen>> ens0 = Util.newSetsFor0(sch.ens);
		Map<Ty, Collection<Null<?>>> tys0 = new THashMap<>();
		Map<Gen, Map<Fk, Gen>> fks0 = new THashMap<>();
		Map<Gen, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Null<?>>>> atts0 = new THashMap<>();
		Map<Null<?>, Term<Ty, En, Sym, Fk, Att, Gen, Null<?>>> extraRepr = null; // new THashMap<>();

		for (Ty ty : sch.typeSide.tys) {
			tys0.put(ty, new THashSet<>());
		}

		boolean nullOnErr = (Boolean) ops.getOrDefault(AqlOption.import_null_on_err_unsafe);
		boolean dontCheckClosure = (Boolean) ops.getOrDefault(AqlOption.import_dont_check_closure_unsafe);
		// String tick = (String) ops.getOrDefault(AqlOption.jdbc_quote_char);
		// String sep = (String) ops.getOrDefault(AqlOption.import_col_seperator);

		int fr = 0;
		Map<SqlTable, Map<Map<SqlColumn, Optional<Object>>, Gen>> iso1 = new THashMap<>();

		for (SqlTable table : info.tables) {
			Set<Map<SqlColumn, Optional<Object>>> tuples = inst.get(table);
			En x = En.En(table.name);
			Map<Map<SqlColumn, Optional<Object>>, Gen> i1 = new THashMap<>();
			SqlColumn thePk = null;
			if (table.pk.size() == 1) {
				thePk = Util.get0(table.pk);
//					 TINYINT SMALLINT  INTEGER  BIGINT VARCHAR
			}
			for (Map<SqlColumn, Optional<Object>> tuple : tuples) {
				Gen i;
				if (thePk == null) {
					i = Gen.Gen(Integer.toString(fr++));
				} else {
					Optional<Object> xx = tuple.get(thePk);
					if (!xx.isPresent()) {
						Util.anomaly();
					}
					Object oo = xx.get();
					if (!(oo instanceof Integer) || !(oo instanceof String)) {
						i = Gen.Gen(Integer.toString(fr++));
					} else {
						i = Gen.Gen(xx.get().toString());
					}
				}
				i1.put(tuple, i);

				// i2.put(i, tuple);
				ens0.get(x).add(i);
				for (SqlColumn c : table.columns) {
					if (!atts0.containsKey(i)) {
						atts0.put(i, new THashMap<>());
					}
					Optional<Object> val = tuple.get(c);
					Att a = Att.Att(x, c.name);
					Term<Ty, Void, Sym, Void, Void, Void, Null<?>> xxx = InstExpJdbc.objectToSk(sch, val.orElse(null),
							i, a, tys0, extraRepr, false, nullOnErr);
					atts0.get(i).put(a, xxx);
				}
			}
			iso1.put(table, i1);
			// iso2.put(table, i2);
		}

		for (SqlForeignKey fk : info.fks) {
			for (Map<SqlColumn, Optional<Object>> in : inst.get(fk.source)) {
				Map<SqlColumn, Optional<Object>> out = inst.follow(in, fk);
				Gen tgen = iso1.get(fk.target).get(out);
				Gen sgen = iso1.get(fk.source).get(in);
				if (!fks0.containsKey(sgen)) {
					fks0.put(sgen, new THashMap<>());
				}
				fks0.get(sgen).put(Fk.Fk(En.En(fk.source.name), fk.toString()), tgen);
			}
		}

		ImportAlgebra<Ty, En, Sym, Fk, Att, Gen, Null<?>> alg = new ImportAlgebra<>(sch, ens0, tys0, fks0, atts0,
				(x, y) -> y, (x, y) -> y, dontCheckClosure, Collections.emptySet());

		return new SaturatedInstance<>(alg, alg, (Boolean) ops.getOrDefault(AqlOption.require_consistency),
				(Boolean) ops.getOrDefault(AqlOption.allow_java_eqs_unsafe), true, extraRepr);
	}

	public Schema<Ty, En, Sym, Fk, Att> makeSchema(AqlEnv env, SqlSchema info, AqlOptions ops) {
		TypeSide<Ty, Sym> typeSide = SqlTypeSide.SqlTypeSide(ops);

		Collage<Ty, En, Sym, Fk, Att, Void, Void> col0 = new Collage<>(typeSide.collage());

		for (SqlTable table : info.tables) {
			En x = En.En(table.name);
			col0.ens.add(x);
			for (SqlColumn c : table.columns) {
				if (col0.atts.containsKey(Att.Att(x, c.name))) {
					throw new RuntimeException("Name collision: table " + c.table.name + " col " + c.name
							+ " against table " + col0.atts.get(Att.Att(En.En(table.name), c.name)).first
							+ "\n\n.Possible solution: set option jdbc_import_col_seperator so as to avoid name collisions.");
				}
				col0.atts.put(Att.Att(x, c.name), new Pair<>(x, Ty.Ty(sqlTypeToAqlType(c.type.name))));
			}
		}
		Var v = Var.Var("x");

		for (SqlForeignKey fk : info.fks) {
			En x = En.En(fk.source.name);
			col0.fks.put(Fk.Fk(x, fk.toString()), new Pair<>(x, En.En(fk.target.name)));

			for (SqlColumn tcol : fk.map.keySet()) {
				SqlColumn scol = fk.map.get(tcol);
				Att l = Att.Att(En.En(scol.table.name), scol.name);
				Att r = Att.Att(En.En(tcol.table.name), tcol.name);
				Term<Ty, En, Sym, Fk, Att, Void, Void> lhs = Term.Att(l, Term.Var(v));
				Term<Ty, En, Sym, Fk, Att, Void, Void> rhs = Term.Att(r, Term.Fk(Fk.Fk(x, fk.toString()), Term.Var(v)));
				col0.eqs.add(new Eq<>(Collections.singletonMap(v, Chc.inRight(x)), lhs, rhs));
			}
		}

		Schema<Ty, En, Sym, Fk, Att> sch = new Schema<>(typeSide, col0, new AqlOptions(options, col0, env.defaults));
		return sch;
	}

	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Gen, Null<?>, Gen, Null<?>> eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		String toGet = jdbcString;
		AqlOptions op = new AqlOptions(options, null, env.defaults);
		String tick = (String) op.getOrDefault(AqlOption.jdbc_quote_char);
		if (jdbcString.trim().isEmpty()) {
			toGet = (String) op.getOrDefault(AqlOption.jdbc_default_string);
		}
		try (Connection conn = DriverManager.getConnection(toGet)) {
			SqlSchema sch = new SqlSchema(conn.getMetaData(), tick);
			boolean noDistinct = (Boolean) op.getOrDefault(AqlOption.jdbc_no_distinct_unsafe);
			boolean nullOnErr = (Boolean) op.getOrDefault(AqlOption.import_null_on_err_unsafe);
			SqlInstance inst = new SqlInstance(sch, conn, nullOnErr, noDistinct, tick);
			return toInstance(env, inst, sch);
		} catch (SQLException exn) {
			exn.printStackTrace();
			throw new RuntimeException("JDBC exception: " + exn.getMessage());
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("import_jdbc_all ").append(Util.quote(clazz)).append(" ")
				.append(Util.quote(jdbcString));
		if (!options.isEmpty()) {
			sb.append(" {\n\t").append("\n\toptions\n\t\t").append(Util.sep(options, " = ", "\n\t\t")).append("}");
		}
		return sb.toString();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Collections.emptySet();
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
		InstExpJdbcAll other = (InstExpJdbcAll) obj;
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
	public SchExp type(AqlTyping G) {
		return new SchExpJdbcAll(clazz, jdbcString, Util.toList(options));
	}

}
