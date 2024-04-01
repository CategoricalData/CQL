package catdata.cql.fdm;

import java.util.function.BiFunction;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Query;
import catdata.cql.Term;
import catdata.cql.Transform;


public class CoEvalTransform<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1, En2, Fk2, Att2, Gen2, Sk2, X1, Y1, X2, Y2> extends
    Transform<Ty, En1, Sym, Fk1, Att1, Triple<String, X1, En2>, Chc<Triple<String, X1, En2>, Y1>, Triple<String, X2, En2>, Chc<Triple<String, X2, En2>, Y2>, Integer, Chc<Chc<Triple<String, X1, En2>, Y1>, Pair<Integer, Att1>>, Integer, Chc<Chc<Triple<String, X2, En2>, Y2>, Pair<Integer, Att1>>> {

  private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;
  @SuppressWarnings("unused")
  private final Transform<Ty, En2, Sym, Fk2, Att2, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h;

  private final Instance<Ty, En1, Sym, Fk1, Att1, Triple<String, X1, En2>, Chc<Triple<String, X1, En2>, Y1>, Integer, Chc<Chc<Triple<String, X1, En2>, Y1>, Pair<Integer, Att1>>> src;
  private final Instance<Ty, En1, Sym, Fk1, Att1, Triple<String, X2, En2>, Chc<Triple<String, X2, En2>, Y2>, Integer, Chc<Chc<Triple<String, X2, En2>, Y2>, Pair<Integer, Att1>>> dst;

  private final BiFunction<Triple<String, X1, En2>, En1, Term<Void, En1, Void, Fk1, Void, Triple<String, X2, En2>, Void>> gens; // =
                                                            // new
                                                            // THashMap<>();
  private final BiFunction<Chc<Triple<String, X1, En2>, Y1>, Ty, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X2, En2>, Chc<Triple<String, X2, En2>, Y2>>> sks; // =
                                                                          // new
                                                                          // THashMap<>();

  public CoEvalTransform(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q,
      Transform<Ty, En2, Sym, Fk2, Att2, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h, AqlOptions options1,
      AqlOptions options2) {
    if (!h.src().schema().equals(q.dst)) {
      throw new RuntimeException("Target of query is " + q.dst + " but transform is on " + h.src().schema());
    }

    Q = q;
    this.h = h;

    src = new CoEvalInstance<>(Q, h.src(), options1);
    dst = new CoEvalInstance<>(Q, h.dst(), options2);
    
    gens = (gen1,t) -> Term.Gen(new Triple<>(gen1.first, h.repr(gen1.third, gen1.second), gen1.third));

    sks = (sk1,t) -> sk1.left
        ? Term.Sk(Chc.inLeft(new Triple<>(sk1.l.first, h.repr(sk1.l.third, sk1.l.second), sk1.l.third)))
        : h.dst().algebra().intoY(h.reprT(sk1.r)).map(Function.identity(), Function.identity(), Util.voidFn(),
            Util.voidFn(), Util.voidFn(), Chc::inRight);

    validate(false);
  }

  @Override
  public BiFunction<Triple<String, X1, En2>, En1, Term<Void, En1, Void, Fk1, Void, Triple<String, X2, En2>, Void>> gens() {
    return gens;
  }

  @Override
  public BiFunction<Chc<Triple<String, X1, En2>, Y1>, Ty, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X2, En2>, Chc<Triple<String, X2, En2>, Y2>>> sks() {
    return sks;
  }

  @Override
  public Instance<Ty, En1, Sym, Fk1, Att1, Triple<String, X1, En2>, Chc<Triple<String, X1, En2>, Y1>, Integer, Chc<Chc<Triple<String, X1, En2>, Y1>, Pair<Integer, Att1>>> src() {
    return src;
  }

  @Override
  public Instance<Ty, En1, Sym, Fk1, Att1, Triple<String, X2, En2>, Chc<Triple<String, X2, En2>, Y2>, Integer, Chc<Chc<Triple<String, X2, En2>, Y2>, Pair<Integer, Att1>>> dst() {
    return dst;
  }

}
