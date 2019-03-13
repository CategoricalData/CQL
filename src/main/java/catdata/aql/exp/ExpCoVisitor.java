package catdata.aql.exp;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import catdata.Pair;
import catdata.Unit;
import catdata.aql.AqlSyntax;
import catdata.aql.Kind;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public interface ExpCoVisitor<R,P,E extends Exception> extends
	 TyExp.TyExpCoVisitor<R,P,E>,
	 SchExp.SchExpCoVisitor<R,P,E>,
	 MapExp.MapExpCoVisitor<R,P,E>,
	 EdsExp.EdsExpCoVisitor<R,P,E>,
	 GraphExp.GraphExpCoVisitor<R,P,E>,
	 PragmaExp.PragmaExpCoVisitor<R,P,E>,
	 ColimSchExp.ColimSchExpCoVisitor<R,P,E>,
	 InstExp.InstExpCoVisitor<R,P,E>,
	 TransExpCoVisitor<R,P,E>,
	 CommentExp.CommentExpCoVisitor<R,P,E>,
	 QueryExpCoVisitor<R,P,E> {
	
	public default Map<Kind,Set<Pair<R,Exp<?>>>> covisit(P params, Function<Unit,R> exp) {
		Map<Kind,Set<Pair<R,Exp<?>>>> ret = new THashMap<>();
		for (Kind k : Kind.class.getEnumConstants()) {
			ret.put(k, new THashSet<>());
		}
		for (AqlSyntax x : AqlSyntax.class.getEnumConstants()) {
			R r = exp.apply(Unit.unit);
			Exp<?> e = covisit(params, r, x);
			ret.get(e.kind()).add(new Pair<>(r, e));
		}
		return ret;
	}
	
	public default Exp<?> covisit(P params, R exp, AqlSyntax x) {
		try {
			Method clazz = this.getClass().getMethod("visit" + x, params.getClass(), exp.getClass());
			
			Exp<?> ret = (Exp<?>) clazz.invoke(this, params, exp);
			
			return ret;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
}
