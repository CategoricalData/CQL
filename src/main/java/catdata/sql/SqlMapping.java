package catdata.sql;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import catdata.Pair;
import catdata.Util;
import gnu.trove.map.hash.THashMap;

public class SqlMapping {

	//TODO aql give fks deterministic names based on table names, col names, etc 
	
	private final SqlSchema source;
    private final SqlSchema target;
	
	private final Map<SqlTable, SqlTable> tm;
	private final Map<SqlColumn, Pair<SqlPath, SqlColumn>> am;
	private final Map<SqlForeignKey, SqlPath> em;
	
	private static void fromPath(SqlPath path, String... arr) {
		arr[1] = path.source.name;
		int i = 0;
		for (SqlForeignKey edge : path.edges) {
			arr[i++] = edge.name;
		}
	}
	
	public String[][] toStrings() {
		String[][] ret = new String[tm.keySet().size() + am.keySet().size() + em.keySet().size()][];
		
		int i = 0;
		for (SqlTable s : tm.keySet()) {
			ret[i] = new String[2];
			ret[i][0] = s.name;
			ret[i][1] = tm.get(s).name;			
			i++;
		}
		for (SqlForeignKey fk : em.keySet()) {
			SqlPath p = em.get(fk);
			ret[i] = new String[2 + p.edges.size()];
			ret[i][0] = fk.name;
			fromPath(p, ret[i]);
			i++;
		}
		for (SqlColumn fk : am.keySet()) {
			Pair<SqlPath, SqlColumn> p = am.get(fk);
			ret[i] = new String[2 + 1 + p.first.edges.size()];
			ret[i][0] = fk.table.name + "." + fk.name;
			fromPath(p.first, ret[i]);
			ret[i][ret[i].length-1] = p.second.table.name + "." + p.second.name;
			i++;
		}
		
		return ret;
	}
	
	public static SqlMapping guess(SqlSchema source, SqlSchema target) {
		if (!source.fks.isEmpty()) {
			throw new RuntimeException("Cannot guess with source foreign keys");
		}
		
		Map<SqlTable, SqlTable> tm = new THashMap<>();
		Map<SqlColumn, Pair<SqlPath, SqlColumn>> am = new THashMap<>();

		for (SqlTable s : source.tables) {
			double max_d = -1;
			SqlTable max_t = null;
			for (SqlTable t : target.tables) {
				double cur_d = Util.similarity(s.name, t.name); //TODO aql similarity is broken
				if (cur_d > max_d) {
					max_d = cur_d;
					max_t = t;
				}
			}
			if (max_t == null) {
				throw new RuntimeException();
			}
			tm.put(s, max_t);
			
			for (SqlColumn c : s.columns) {
				max_d = -1;
				SqlColumn max_c = null;
				for (SqlColumn d : max_t.columns) {
					if (!c.type.equals(d.type)) {
						continue;
					}
					double cur_d = Util.similarity(c.name, d.name); //TODO aql similarity is broken
					if (cur_d > max_d) {
						max_d = cur_d;
						max_c = d;
					}
				}
				if (max_c == null) {
					throw new RuntimeException("Cannot find partner for " + c + " in " + max_t);
				}
				am.put(c, new Pair<>(new SqlPath(max_t), max_c));
			}
		}
		
		
		
		return new SqlMapping(source, target, tm, am, new THashMap<>());
	}
	
	public SqlMapping(SqlSchema source, SqlSchema target, String[][] ms) {
		this.source = source;
		this.target = target;
        tm = new THashMap<>();
        am = new THashMap<>();
        em = new THashMap<>();
		
		for (String[] m : ms) {
			if (m.length == 0) {
				throw new RuntimeException("Empty row in " + Arrays.deepToString(ms) );
			}
			if (m.length == 1) {
				throw new RuntimeException("Row of length 1 in " + Arrays.deepToString(ms));
			}
			String x = m[0];
			if (source.isTable(x)) {
				addTM(m);
			} else if (source.isColumn(x)) {
				addAM(m);
			} else if (source.isForeignKey(x)) {
				addEM(m);
			} else {
				throw new RuntimeException(x + " is not a table, column, or foreign key");
			}	
		}
		
		validate();
	}
			
	private SqlPath toPath(String[] m, int endIndex) {
		List<SqlForeignKey> edges = new LinkedList<>();
		for (int i = 2; i < endIndex; i++) {
			edges.add(target.getForeignKey(m[i]));
		}
		if (!edges.isEmpty()) {
			return new SqlPath(edges);
		}
		return new SqlPath(target.getTable(m[1]));
	}
	
	private void addEM(String... m) {
		em.put(source.getForeignKey(m[0]), toPath(m, m.length));
	}


	private void addAM(String... m) {
		SqlPath path = toPath(m, m.length - 1);
		SqlColumn col = target.getColumn(m[m.length-1]);
		am.put(source.getColumn(m[0]), new Pair<>(path, col));
	}


	private void addTM(String... m) {
		String s = m[0];
		String t = m[1];
		if (m.length > 2) {
			throw new RuntimeException("Table mapping not length two: " + Arrays.deepToString(m));
		}
                if (tm.containsKey(source.getTable(s))) {
			throw new RuntimeException("Duplicate table mapping for " + s);
		}
		tm.put(source.getTable(s), target.getTable(t));
	}


	private SqlMapping(SqlSchema source, SqlSchema target, Map<SqlTable, SqlTable> tm, Map<SqlColumn, Pair<SqlPath, SqlColumn>> am, Map<SqlForeignKey, SqlPath> em) {
		this.source = source;
		this.target = target;
		this.tm = tm;
		this.am = am;
		this.em = em;
		validate();
	}
	
	private SqlPath apply(SqlForeignKey in) {
		SqlPath out = em.get(in);
		if (out == null) {
			throw new RuntimeException(in + " is not a foreign key in " + source);
		}
		return out;
	}
	
	private SqlTable apply(SqlTable in) {
		SqlTable out = tm.get(in);
		if (out == null) {
			throw new RuntimeException(in + " is not a table in " + source);
		}
		return out;
	}
	
	private Pair<SqlPath, SqlColumn> apply(SqlColumn in) {
		Pair<SqlPath, SqlColumn> out = am.get(in);
		if (out == null) {
			throw new RuntimeException(in + " is not a column in " + source);
		}
		return out;
	}
	
	private void validate() {
		for (SqlTable t : tm.keySet()) {
			if (!source.tables.contains(t)) {
				throw new RuntimeException(t + " is not a table in " + source);
			}
		}
		for (SqlColumn t : am.keySet()) {
			if (!source.tables.contains(t.table)) {
				throw new RuntimeException(t + " is not a column in " + source);
			}
		}
		for (SqlForeignKey t : em.keySet()) {
			if (!source.fks.contains(t)) {
				throw new RuntimeException(t + " is not a foreign key in " + source);
			}
		}
		
		for (SqlTable in : source.tables) {
			if (!target.tables.contains(apply(in))) {
				throw new RuntimeException(apply(in) + " is not a table in " + target);
			}
		}
		for (SqlColumn in : source.cols()) {
			Pair<SqlPath, SqlColumn> p = apply(in);
			if (!target.cols().contains(p.second)) {
				throw new RuntimeException(p.second + " is not a column in " + target);
			}
			if (!target.tables.contains(p.first.target)) {
				throw new RuntimeException(p.first.target + " is not a table in " + target);
			}
			if (!target.tables.contains(p.first.target)) {
				throw new RuntimeException(p.first.target + " is not a table in " + target);
			}
			if (!p.first.source.equals(apply(in.table))) {
				throw new RuntimeException("Column " + in + " has table " + in.table + " which becomes " + apply(in.table) + " but path starts at " + p.first.source);
			}
			if (!p.first.target.equals(p.second.table)) {
				throw new RuntimeException("Path ends at " + p.first.target + " but column starts at " + p.second.table);
			}
			if (!in.type.equals(p.second.type)) {
				throw new RuntimeException("Column " + in + " of type " + in.type + " sent to " + p.second + " which has type " + p.second.type);
			}
		}
		for (SqlForeignKey in : source.fks) {
			SqlPath p = apply(in);
			if (!target.tables.contains(p.source)) {
				throw new RuntimeException(p.source + " is not a table in " + target);
			}
			if (!target.tables.contains(p.target)) {
				throw new RuntimeException(p.target + " is not a table in " + target);
			}
                       for (SqlForeignKey fk : p.edges) {
				if (!target.fks.contains(fk)) {
					throw new RuntimeException(fk + " is not a foreign key in " + target);
				}
			}
			if (!p.source.equals(apply(in.source))) {
				throw new RuntimeException("Foreign key " + in + " has source " + in.source + " which becomes " + apply(in.source) + " but path starts at " + p.source);
			}
			if (!p.target.equals(apply(in.target))) {
				throw new RuntimeException("Foreign key " + in + " has target " + in.target + " which becomes " + apply(in.target) + " but path ends at " + p.target);
			}
		}
	}
}
