package catdata.cql.fdm;

import java.util.function.BiFunction;

import catdata.cql.Instance;
import catdata.cql.Term;
import catdata.cql.Transform;

public class ComposeTransform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2, Gen3, Sk3, X3, Y3>
    extends Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen3, Sk3, X1, Y1, X3, Y3> {

  private final Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t1;
  private final Transform<Ty, En, Sym, Fk, Att, Gen2, Sk2, Gen3, Sk3, X2, Y2, X3, Y3> t2;

  private final BiFunction<Gen1, En, Term<Void, En, Void, Fk, Void, Gen3, Void>> gens;
  private final BiFunction<Sk1, Ty, Term<Ty, En, Sym, Fk, Att, Gen3, Sk3>> sks;

  public ComposeTransform(Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> t1,
      Transform<Ty, En, Sym, Fk, Att, Gen2, Sk2, Gen3, Sk3, X2, Y2, X3, Y3> t2) {
    this.t1 = t1;
    this.t2 = t2;
    if (t1.dst().collage() != null && t2.src().collage() != null && !t1.dst().equals(t2.src())) {
      throw new RuntimeException("Anomaly: in compose transform, dst of t1 is \n\n" + t1.dst()
          + " \n\n but src of t2 is \n\n" + t2.src());
    }
    gens = (gen1,t) -> t2.trans(t1.gens().apply(gen1,t).convert()).convert();
    sks = (sk1,t) -> t2.trans(t1.sks().apply(sk1,t));
  }

  @Override
  public BiFunction<Gen1, En, Term<Void, En, Void, Fk, Void, Gen3, Void>> gens() {
    return gens;
  }

  @Override
  public BiFunction<Sk1, Ty, Term<Ty, En, Sym, Fk, Att, Gen3, Sk3>> sks() {
    return sks;
  }

  @Override
  public Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src() {
    return t1.src();
  }

  @Override
  public Instance<Ty, En, Sym, Fk, Att, Gen3, Sk3, X3, Y3> dst() {
    return t2.dst();
  }

}
