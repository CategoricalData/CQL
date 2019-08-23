package catdata.aql;

import catdata.apg.ApgInstance;
import catdata.apg.ApgTransform;
import catdata.apg.ApgTypeside;

public interface SemanticsVisitor<R, G, E extends Throwable> {
	
	public R visit(String k, G arg, ApgTypeside t) throws E;

	public <L,e> R visit(String k, G arg, ApgInstance<L,e> t) throws E;

	public <l1,e1,l2,e2> R visit(String k, G arg, ApgTransform<l1,e1,l2,e2> t) throws E;

	public <T, C, T0, C0> R visit(String k, G arg, Mor<T,C,T0,C0> M) throws E;
	
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
			return visit(k, arg, (Comment) o);
		case GRAPH:
			return visit(k, arg, (Graph<?,?>) o);
		case INSTANCE:
			return visit(k, arg, (Instance<?,?,?,?,?,?,?,?,?>) o);
		case MAPPING:
			return visit(k, arg, (Mapping<?,?,?,?,?,?,?,?>) o);
		case PRAGMA:
			return visit(k, arg, (Pragma) o);
		case QUERY:
			return visit(k, arg, (Query<?,?,?,?,?,?,?,?>) o);
		case SCHEMA:
			return visit(k, arg, (Schema<?,?,?,?,?>) o);
		case TRANSFORM:
			return visit(k, arg, (Transform<?,?,?,?,?,?,?,?,?,?,?,?,?>) o);
		case TYPESIDE:
			return visit(k, arg, (TypeSide<?,?>) o);
		case SCHEMA_COLIMIT:
			return visit(k, arg, (ColimitSchema<?>) o);
		case CONSTRAINTS:
			return visit(k, arg, (Constraints) o);
		case THEORY_MORPHISM:	
			return visit(k, arg, o);
		case APG_instance:
			return visit(k, arg, (ApgInstance<?,?>) o);
		case APG_morphism:
			return visit(k, arg, (ApgTransform<?,?,?,?>) o);
		case APG_typeside:
			return visit(k, arg, (ApgTypeside) o);
		default:
			break;
		}
		throw new RuntimeException("Anomaly: please report");
	}

}
