package catdata.aql.fdm;

import java.util.function.BiFunction;

import catdata.Chc;
import catdata.Pair;
import catdata.aql.AqlOptions;
import catdata.aql.Instance;
import catdata.aql.Mapping;
import catdata.aql.Term;
import catdata.aql.Transform;

public class SigmaDeltaUnitTransform<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> extends
    Transform<Ty, En1, Sym, Fk1, Att1, Gen, Sk, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>, X, Y, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>> {

  private final Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> F;
  private final Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I;
  private final SigmaInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> J;
  private final DeltaInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, Integer, Chc<Sk, Pair<Integer, Att2>>> K; 
  private final BiFunction<Gen, En1, Term<Void, En1, Void, Fk1, Void, Pair<En1, Integer>, Void>> gens;
  private final BiFunction<Sk, Ty, Term<Ty, En1, Sym, Fk1, Att1, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>>> sks;

  public SigmaDeltaUnitTransform(Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> f,
      Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i, AqlOptions options) {
    F = f;
    I = i;
    J = new SigmaInstance<>(F, I, options);
    K = new DeltaInstance<>(F, J);

    gens = (gen1,t) -> Term.Gen(new Pair<>(t, J.algebra().intoX(F.trans(Term.Gen(gen1)))));
    sks = (sk1,t) -> J.algebra().intoY(F.trans(Term.Sk(sk1))).convert(); 
    
    //validate((Boolean) options.getOrDefault(AqlOption.dont_validate_unsafe));
  }

  @Override
  public BiFunction<Gen, En1, Term<Void, En1, Void, Fk1, Void, Pair<En1, Integer>, Void>> gens() {
    return gens;
  }

  @Override
  public BiFunction<Sk, Ty, Term<Ty, En1, Sym, Fk1, Att1, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>>> sks() {
    return  sks;
  }

  @Override
  public Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> src() {
    return I;
  }

  @Override
  public Instance<Ty, En1, Sym, Fk1, Att1, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>, Pair<En1, Integer>, Chc<Sk, Pair<Integer, Att2>>> dst() {
    return K;
  }

}
