package catdata.sql;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catdata.Util;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class SqlTable {

	public String db;
	public String name;
	public final List<SqlColumn> columns = new LinkedList<>();
	public Set<SqlColumn> pk = new THashSet<>();

	public SqlColumn getCnfId() {
		return Util.get0(pk);
	}

	public boolean isCnf() {
		return pk.size() == 1 && getCnfId().autoInc;
	}

	public void validate() {
		if (name == null) {
			throw new RuntimeException();
		}
		for (SqlColumn col : columns) {
			if (!col.table.equals(this)) {
				throw new RuntimeException();
			}
		}
		if (!columns.containsAll(pk)) {
			throw new RuntimeException();
		}
		if (columns.size() != new THashSet<>(columns).size()) {
			throw new RuntimeException();
		}
	}

	private final Map<String, SqlColumn> colMap = new THashMap<>();

	public SqlColumn getColumn(String name0) {
		SqlColumn t = colMap.get(name0);
		if (t != null) {
			return t;
		}
		for (SqlColumn col : columns) {
			if (col.name.equals(name0)) {
				colMap.put(name0, col);
				return col;
			}
		}
		throw new RuntimeException("Not a column in " + this + ": " + name0);
	}

	private Map<String, String> typeMap = null;

	public Map<String, String> typeMap() {
		if (typeMap != null) {
			return typeMap;
		}
		typeMap = new THashMap<>();
		for (SqlColumn col : columns) {
			typeMap.put(col.name, col.type.name);
		}
		return typeMap;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		SqlTable other = (SqlTable) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}

}