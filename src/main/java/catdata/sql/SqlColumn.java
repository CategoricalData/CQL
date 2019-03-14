package catdata.sql;

public class SqlColumn {

	public SqlTable table;
	public String name;
	public SqlType type;
	public boolean autoInc = false;

	public SqlColumn(SqlTable table, String name, SqlType type) {
		if (table == null || name == null || type == null) {
			throw new RuntimeException();
		}

		this.table = table;
		this.name = name;
		this.type = type;
	}

	@SuppressWarnings("unused")
	private SqlColumn() {
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((table == null) ? 0 : table.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		SqlColumn other = (SqlColumn) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (table == null) {
			if (other.table != null)
				return false;
		} else if (!table.equals(other.table))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}

	// public String toString(String x) {
	// return table + x + name;
//	}

}