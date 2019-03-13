package catdata.aql;

public enum Kind {

	CONSTRAINTS,
	TYPESIDE, //(TypeSide.class, TyExp.class),
	SCHEMA, //(Schema.class, SchExp.class),
	INSTANCE, //(Instance.class, InstExp.class),
	MAPPING, //(Mapping.class, MapExp.class),
	TRANSFORM, //(Transform.class, TransExp.class),
	QUERY, //(Query.class, QueryExp.class), 
	PRAGMA, //(Pragma.class, PragmaExp.class), 
	GRAPH, //(DMG.class, GraphExp.class),
	COMMENT,
	SCHEMA_COLIMIT; //(Comment.class, CommentExp.class); 
	
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
		default:
			throw new RuntimeException();
		}
		
		
	}
	/*
	private final Class<?> literal;
	private final Class<?> exp;
	
	Kind(Class<?> literal, Class<?> exp) {
		this.literal = literal;
		this.exp = exp;
	} 

	public void checkExp(String k, Object o) {
		if (!exp.isInstance(o)) {
			throw new RuntimeException(k + " is not a " + this + " expression , is a " + o.getClass());
		}
	}
	
	public <X> void checkLiteral(X k, Object o) {
		if (!literal.isInstance(o)) {
			throw new RuntimeException(k + " is not a " + this + " literal, is a " + o.getClass());
		}
	}
	*/
}
