package catdata.sql;

import java.util.Map;

import gnu.trove.map.hash.THashMap;

public class SqlForeignKey {
	public SqlTable source, target;
	public final Map<SqlColumn, SqlColumn> map = new THashMap<>(); //target->source
	public String name;
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
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
		SqlForeignKey other = (SqlForeignKey) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

	public void validate() {
		if (source == null) {
			throw new RuntimeException();
		}
		if (target == null) {
			throw new RuntimeException();
		}
		if (name == null) {
			throw new RuntimeException();
		}
		if (!name.equals(name)) {
			throw new RuntimeException();
		}
		if (!map.keySet().equals(target.pk)) {
			throw new RuntimeException(map.keySet() + " is not the primary key of " + target);			
		}
		for (SqlColumn tcol : map.keySet()) {
			SqlColumn scol = map.get(tcol);
			if (!source.columns.contains(scol)) {
				throw new RuntimeException(scol + " is not a column in " + source);
			}
			if (!scol.type.equals(tcol.type)) {
				throw new RuntimeException("Types do not match for " + scol + " and " + tcol);
			}
		}
	}
	
	
	
}