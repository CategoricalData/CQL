package catdata.aql.fdm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.Frozen;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.It.ID;
import gnu.trove.map.hash.THashMap;
import catdata.aql.Pragma;
import catdata.aql.Query;
import catdata.aql.SqlTypeSide;
import catdata.aql.Term;
import catdata.aql.Transform;

public class ToJdbcPragmaQuery<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> extends Pragma {

	private final String jdbcString;
	private final String prefixSrc;
	private final String prefixDst;
	private final String ty;
	// private final String clazz;
	private final String idCol;
	private final int vlen;

	private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;
	private final String tick;

	public ToJdbcPragmaQuery(String prefixSrc, String prefixDst, Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q,
			String jdbcString, AqlOptions options) {
		this.jdbcString = jdbcString;
		this.prefixSrc = prefixSrc;
		this.prefixDst = prefixDst;
		this.ty = (String) options.getOrDefault(AqlOption.jdbc_query_export_convert_type);
		this.Q = Q;
		idCol = (String) options.getOrDefault(AqlOption.id_column_name);
		this.tick = (String) options.getOrDefault(AqlOption.jdbc_quote_char);
		this.vlen = (int) options.getOrDefault(AqlOption.varchar_length);

		assertDisjoint(idCol);
	}

	@Override
	public void execute() {
		try {
			Connection conn = DriverManager.getConnection(jdbcString);
			Statement stmt = conn.createStatement();
			for (String s : toSQLViews(prefixSrc, prefixDst, idCol, ty, tick, Q.unnest(), vlen).first) {
				System.err.println("Exec: " + s);
				stmt.execute(s);
			}
			stmt.close();
			conn.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void assertDisjoint(String idCol) {
		if (Q.dst.atts.keySet().contains(idCol)) {
			throw new RuntimeException("Cannot JDBC export: id column (" + idCol + ") is also an attribute");
		}
		if (Q.dst.fks.keySet().contains(idCol)) {
			throw new RuntimeException("Cannot JDBC export: id column (" + idCol + ") is also a foreign key");
		}
	}

	@Override
	public String toString() {
		return "export_jdbc_query "
				+ Util.sep(toSQLViews(prefixSrc, prefixDst, idCol, ty, tick, Q.unnest(), vlen).first, "\n\n");
	}

	/**
	 * Converts an CQL query, as best as it can, into SQL.
	 * 
	 * @param pre   to be placed a the front of any column name
	 * @param post  to be prepended to the view name for drop and create.
	 * @param idCol the name of the id column which provides a unique identifier for
	 *              the tuple
	 * @param ty    the name of the typeside to be used in resolving properties
	 *              "char" is common usage
	 * @return a pair first : some sql for recreating the view. second : the sql
	 *         describing the view.
	 */
	public static <Ty, En1, Fk1, Fk2, Sym, Att1, Att2, En2> Pair<List<String>, Map<En2, String>> toSQLViews(String pre,
			String post, String idCol, String ty, String tick, Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q,
			int vlen) {
		// if (!(src.typeSide.tys.containsAll(SqlTypeSide.tys()))) {
		// throw new RuntimeException("Not on SQL typeside");
		// }

		List<String> ret1 = new LinkedList<>();
		Map<En2, String> ret2 = new THashMap<>();
		idCol = tick + idCol + tick;
		for (En2 en2 : Q.ens.keySet()) {
			Frozen<Ty, En1, Sym, Fk1, Att1> b = Q.ens.get(en2);
			Map<String, En1> gens = b.gens;
			Collection<Pair<Term<Ty, En1, Sym, Fk1, Att1, String, String>, Term<Ty, En1, Sym, Fk1, Att1, String, String>>> eqs = b.eqs;

			if (Q.ens.get(en2).gens.isEmpty()) {
				throw new RuntimeException("Empty from clause doesn't work with sql");
			}

			List<String> from = new LinkedList<>();
			List<String> select = new LinkedList<>();

			select.add(sk(gens.keySet(), idCol, ty, vlen) + " as " + idCol); // add id column
			for (String v : gens.keySet()) {
				from.add(tick + pre + gens.get(v) + tick + " as " + v);
			}
			for (Att2 att2 : Q.dst.attsFrom(en2)) {
				if (!Q.atts.get(att2).left) {
					throw new RuntimeException("SQL translation for aggregation not yet supported.");
				}
				select.add(Q.atts.get(att2).l.toStringSql(Q.src, tick, false) + " as " + tick + att2 + tick);
			}
			for (Fk2 fk2 : Q.dst.fksFrom(en2)) {
				select.add(sk(Q.fks.get(fk2), idCol, ty, tick, vlen) + " as " + tick + fk2 + tick);
			}
			// TODO ADD FOREIGN KEYS aql

			String xxx = "  select " + Util.sep(select, ", ") + "\nfrom " + Util.sep(from, ", ") + "\n "
					+ whereToString(eqs, idCol, tick, false, Q);

			ret1.add("drop view if exists " + tick + post + en2 + tick);

			ret1.add("create view " + tick + post + en2 + tick + " as " + xxx);

			ret2.put(en2, xxx);
		}

		return new Pair<>(ret1, ret2);
	}

	private static <Ty, En1, Fk1, Fk2, Sym, Att1, Att2, En2> String sk(
			Transform<Ty, En1, Sym, Fk1, Att1, String, String, String, String, ID, Chc<String, Pair<ID, Att1>>, ID, Chc<String, Pair<ID, Att1>>> h,
			String idCol, String ty, String tick, int vlen) {

		List<Pair<String, String>> l = new ArrayList<>(h.src().gens().size());
		h.src().gens().entrySet((v, t) -> {
			l.add(new Pair<>(v, convert(qdirty(h.gens().apply(v, t), idCol, tick), ty, vlen)));
		});

		return sk(l, ty, vlen);
	}

	private static <Ty, En1, Fk1, Fk2, Sym, Att1, Att2, En2> String sk(Collection<Pair<String, String>> vs, String ty,
			int vlen) {
		if (vs.isEmpty()) {
			Util.anomaly();
		}
		List<String> l = (vs.stream()
				.map(x -> "concat('(" + x.first + "=', concat(" + convert(x.second, ty, vlen) + ", ')'))")
				.collect(Collectors.toList()));

		String s = l.get(0);
		for (int i = 1; i < l.size(); i++) {
			s = "concat(" + s + ", " + l.get(i) + ")";
		}
		return s;
	}

	private static String convert(String x, String ty, int vlen) {
		return "cast(" + x + " as " + SqlTypeSide.mediate(vlen, ty) + ")";
	}

	private static String qdirty(Term<?, ?, ?, ?, ?, ?, ?> t, String idCol, String tick) {
		if (t.gen() != null) {
			return t.gen() + "." + idCol;
		} else if (t.fk() != null) {
			return t.arg + "." + tick + t.fk() + tick;
		}
		return Util.anomaly();
	}

	private static String sk(Set<String> vs, String idCol, String ty, int vlen) {
		return sk(vs.stream().map(x -> new Pair<>(x, x + "." + idCol)).collect(Collectors.toList()), ty, vlen);
	}

	public static <Ty, En1, Fk1, Fk2, Sym, Att1, Att2, En2> String whereToString(
			Collection<Pair<Term<Ty, En1, Sym, Fk1, Att1, String, String>, Term<Ty, En1, Sym, Fk1, Att1, String, String>>> eqs,
			String idCol, String tick, boolean truncate, Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q) {
		if (eqs.isEmpty()) {
			return "";
		}
		List<String> temp;
		String toString2 = " where ";
		temp = new LinkedList<>();
		for (Pair<Term<Ty, En1, Sym, Fk1, Att1, String, String>, Term<Ty, En1, Sym, Fk1, Att1, String, String>> eq : eqs) {
			String newLhs;
			if (eq.first.gen() != null) {
				newLhs = eq.first.gen() + "." + idCol;
			} else if (eq.first.sk() != null) {
				newLhs = "?";
				if (Q.consts.containsKey(eq.first.sk())) {
					newLhs = Q.consts.get(eq.first.sk()).toStringSql(Q.src, tick, truncate);
				}
			} else {
				newLhs = eq.first.toStringSql(Q.src, tick, truncate);
			}
			String newRhs;
			if (eq.second.gen() != null) {
				newRhs = eq.second.gen() + "." + idCol;
			} else if (eq.second.sk() != null) {
				newRhs = "?";
				if (Q.consts.containsKey(eq.second.sk())) {
					newRhs = Q.consts.get(eq.second.sk()).toStringSql(Q.src, tick, truncate);
				}
			} else {
				newRhs = eq.second.toStringSql(Q.src, tick, truncate);
			}
			temp.add(newLhs + " = " + newRhs);
		}
		toString2 += Util.sep(temp, " and ");
		return toString2;
	}

}
