package catdata.aql.fdm;

import java.util.List;
import java.util.Map;

import catdata.Chc;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.AqlOptions;
import catdata.aql.DP;
import catdata.aql.Instance;
import catdata.aql.Query;
import catdata.aql.Schema;
import catdata.aql.Term;


public class EvalInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> extends
    Instance<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, Y, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, Y>
    implements DP<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, Y> {

  private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;
  private final Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I;
  private final EvalAlgebra<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> alg;
  private final SaturatedInstance<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, Y, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, Y> J;

  public EvalInstance(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q,
      Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i, AqlOptions options) {
    if (!q.src.equals(i.schema())) {
      throw new RuntimeException(
          "In eval instance, source of query is " + q.src + ", but instance has type " + i.schema());
    }

    Q = q;
    I = i;
    alg = new EvalAlgebra<>(Q, I, options);

    J = new SaturatedInstance<>(alg, dp(), I.requireConsistency(), I.allowUnsafeJava(), false, null);

    if (J.size() < 1024 * 16) {
      validate();
    }
  }

  @Override
  public Schema<Ty, En2, Sym, Fk2, Att2> schema() {
    return Q.dst;
  }

  @Override
  public IMap<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, En2> gens() {
    return J.gens();
  }

  @Override
  public IMap<Y, Ty> sks() {
    return J.sks();
  }

  @Override
  public DP<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, Y> dp() {
    return this;
  }

  @Override
  public Algebra<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, Y, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, Y> algebra() {
    return alg;
  }

  @Override
  public boolean eq(Map<String, Chc<Ty, En2>> ctx,
      Term<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, Y> lhs,
      Term<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, Y> rhs) {
    if (ctx != null && !ctx.isEmpty()) {
      Util.anomaly();
    }
    return atType(lhs) ? I.dp().eq(null, I.reprT(alg.intoY(lhs)), I.reprT(alg.intoY(rhs)))
        : alg.intoX(lhs).equals(alg.intoX(rhs));
  }

  private boolean atType(
      Term<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>, Chc<En1,Ty>>, Y> term) {
    if (term.obj() != null || term.sk() != null) {
      return true;
    } else if (term.gen() != null) {
      return false;
    } else if (term.att() != null) {
      return true;
    } else if (term.fk() != null) {
      return false;
    } else if (term.sym() != null) {
      return true;
    }
    throw new RuntimeException("Anomaly: please report: EvalInstance.atType called with " + term);
  }

  @Override
  public String toStringProver() {
    return alg.toStringProver();
  }

  @Override
  public boolean requireConsistency() {
    return I.requireConsistency();
  }

  @Override
  public boolean allowUnsafeJava() {
    return I.allowUnsafeJava();
  }

  public List<String> order(En2 en2) {
    return alg.order.get(en2);
  }

}
