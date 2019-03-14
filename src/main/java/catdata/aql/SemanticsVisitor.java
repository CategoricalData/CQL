package catdata.aql;

import catdata.aql.fdm.ColimitSchema;

public interface SemanticsVisitor<R, G, E extends Throwable> {

	public <T, C> R visit(String k, G arg, TypeSide<T, C> T) throws E;

	public <N> R visit(String k, G arg, ColimitSchema<N> S) throws E;

	public <Ty, En, Sym, Fk, Att> R visit(String k, G arg, Schema<Ty, En, Sym, Fk, Att> S) throws E;

	public <Ty, En, Sym, Fk, Att> R visit(String k, G arg, Constraints S) throws E;

	public <Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> R visit(String k, G arg,
			Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I) throws E;

	public <Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> R visit(String k, G arg,
			Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h) throws E;

	public R visit(String k, G arg, Pragma P) throws E;

	public R visit(String k, G arg, Comment C) throws E;

	public <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> R visit(String k, G arg,
			Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q) throws E;

	public <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> R visit(String k, G arg,
			Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> M) throws E;

	public <N, e> R visit(String k, G arg, Graph<N, e> G) throws E;

	public default R visit(String k, G arg, Semantics o) throws E {
		switch (o.kind()) {
		case COMMENT:
			return visit(k, arg, o.asComment());
		case GRAPH:
			return visit(k, arg, o.asGraph());
		case INSTANCE:
			return visit(k, arg, o.asInstance());
		case MAPPING:
			return visit(k, arg, o.asMapping());
		case PRAGMA:
			return visit(k, arg, o.asPragma());
		case QUERY:
			return visit(k, arg, o.asQuery());
		case SCHEMA:
			return visit(k, arg, o.asSchema());
		case TRANSFORM:
			return visit(k, arg, o.asTransform());
		case TYPESIDE:
			return visit(k, arg, o.asTypeSide());
		case SCHEMA_COLIMIT:
			return visit(k, arg, o.asSchemaColimit());
		case CONSTRAINTS:
			return visit(k, arg, o.asConstraints());
		default:
			break;
		}
		throw new RuntimeException("Anomaly: please report");
	}

}
