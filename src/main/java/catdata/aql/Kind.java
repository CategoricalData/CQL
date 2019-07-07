package catdata.aql;

public enum Kind {

	CONSTRAINTS, TYPESIDE, // (TypeSide.class, TyExp.class),
	SCHEMA, // (Schema.class, SchExp.class),
	INSTANCE, // (Instance.class, InstExp.class),
	MAPPING, // (Mapping.class, MapExp.class),
	TRANSFORM, // (Transform.class, TransExp.class),
	QUERY, // (Query.class, QueryExp.class),
	PRAGMA, // (Pragma.class, PragmaExp.class),
	GRAPH, // (DMG.class, GraphExp.class),
	COMMENT, SCHEMA_COLIMIT, THEORY_MORPHISM;
	//APG_typeside, APG_schema, APG_instance; // (Comment.class, CommentExp.class);

	@Override
	public String toString() {
		switch (this) {
		case INSTANCE:
			return "instance";
		case MAPPING:
			return "mapping";
		case PRAGMA:
			return "command";
		case QUERY:
			return "query";
		case SCHEMA:
			return "schema";
		case TRANSFORM:
			return "transform";
		case TYPESIDE:
			return "typeside";
		case GRAPH:
			return "graph";
		case COMMENT:
			return "comment";
		case SCHEMA_COLIMIT:
			return "schema_colimit";
		case CONSTRAINTS:
			return "constraints";
		case THEORY_MORPHISM:
			return "theory_morphism";
		/* case APG_instance:
			return "apg_instance";
		case APG_schema:
			return "apg_schema";
		case APG_typeside:
			return "apg_typeside"; */
		default:
			throw new RuntimeException();
		}

	}
	/*
	 * private final Class<?> literal; private final Class<?> exp;
	 * 
	 * Kind(Class<?> literal, Class<?> exp) { this.literal = literal; this.exp =
	 * exp; }
	 * 
	 * public void checkExp(String k, Object o) { if (!exp.isInstance(o)) { throw
	 * new RuntimeException(k + " is not a " + this + " expression , is a " +
	 * o.getClass()); } }
	 * 
	 * public <X> void checkLiteral(X k, Object o) { if (!literal.isInstance(o)) {
	 * throw new RuntimeException(k + " is not a " + this + " literal, is a " +
	 * o.getClass()); } }
	 */
}
