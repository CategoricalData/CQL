package catdata.sql;

import java.util.LinkedList;
import java.util.List;

class SqlPath {

	public SqlTable source, target;
	
	public List<SqlForeignKey> edges;
	
	public SqlPath(SqlTable s) {
		source = s;
		edges = new LinkedList<>();
		validate();
	}
	
	public SqlPath(List<SqlForeignKey> edges) {
		if (edges.isEmpty()) {
			throw new RuntimeException();
		}
		source = edges.get(0).source;
		this.edges = edges;
		validate();
	}
	
	public SqlPath(SqlTable s, List<SqlForeignKey> edges) {
		source = s;
		this.edges = edges;
		validate();
	}
	
	private void validate() {
		target = source;
		for (SqlForeignKey edge : edges) {
			if (!edge.source.equals(target)) {
				throw new RuntimeException("On edge " + edge + ", src is " + edge.source + " but last dst was " + target);
			}
			target = edge.target;
		}
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((edges == null) ? 0 : edges.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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
		SqlPath other = (SqlPath) obj;
		if (edges == null) {
			if (other.edges != null)
				return false;
		} else if (!edges.equals(other.edges))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}
	
	
}
