package catdata.aql.fdm;

import java.util.Map;

import catdata.Chc;
import catdata.Pair;
import catdata.aql.Algebra;
import catdata.aql.DP;
import catdata.aql.Instance;
import catdata.aql.Mapping;
import catdata.aql.Schema;
import catdata.aql.Term;


public class DeltaInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y>
    extends Instance<Ty, En1, Sym, Fk1, Att1, Pair<En1, X>, Y, Pair<En1, X>, Y>
    implements DP<Ty, En1, Sym, Fk1, Att1, Pair<En1, X>, Y> {
  private final Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> F;
  private final Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> I;
  private final DeltaAlgebra<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> alg;
  private final SaturatedInstance<Ty, En1, Sym, Fk1, Att1, Pair<En1, X>, Y, Pair<En1, X>, Y> J;

  public DeltaInstance(Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> f,
      Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> i) {
    if (!f.dst.equals(i.schema())) {
      throw new RuntimeException(
          "In delta instance, target of mapping is " + f.src + ", but instance has type " + i.schema());
    }

    F = f;
    I = i;
    alg = new DeltaAlgebra<>(F, I);
    J = new SaturatedInstance<>(alg, dp(), I.requireConsistency(), I.allowUnsafeJava(), false, null);
    if (J.size() < 16 * 1024) {
      validate();
    }
  }

  @Override
  public Schema<Ty, En1, Sym, Fk1, Att1> schema() {
    return F.src;
  }

  @Override
  public IMap<Pair<En1, X>, En1> gens() {
    return J.gens();
  }

  @Override
  public IMap<Y, Ty> sks() {
    return J.sks();
  }

  @Override
  public DP<Ty, En1, Sym, Fk1, Att1, Pair<En1, X>, Y> dp() {
    return this;
  }

  @Override
  public Algebra<Ty, En1, Sym, Fk1, Att1, Pair<En1, X>, Y, Pair<En1, X>, Y> algebra() {
    return alg;
  }

  @Override
  public boolean eq(Map<String, Chc<Ty, En1>> ctx, Term<Ty, En1, Sym, Fk1, Att1, Pair<En1, X>, Y> lhs,
      Term<Ty, En1, Sym, Fk1, Att1, Pair<En1, X>, Y> rhs) {
    return I.dp().eq(null, alg.translate(lhs), alg.translate(rhs));
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

}
