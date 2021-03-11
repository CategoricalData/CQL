package catdata.aql.fdm;

import java.util.function.BiFunction;

import catdata.aql.Instance;
import catdata.aql.Term;
import catdata.aql.Transform;

public class LiteralTransform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>
    extends Transform<Ty, En, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> {

  private final BiFunction<Gen1, En, Term<Void, En, Void, Fk, Void, Gen2, Void>> gens;
  private final BiFunction<Sk1, Ty, Term<Ty, En, Sym, Fk, Att, Gen2, Sk2>> sks;

  private final Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src;
  private final Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> dst;

  public LiteralTransform(BiFunction<Gen1, En, Term<Void, En, Void, Fk, Void, Gen2, Void>> gens,
      BiFunction<Sk1, Ty, Term<Ty, En, Sym, Fk, Att, Gen2, Sk2>> sks, Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src,
      Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> dst, boolean dontValidateEqs) {
    this.gens = gens;
    this.sks = sks;
    this.src = src;
    this.dst = dst;
    validate(dontValidateEqs);
  }

  @Override
  public BiFunction<Gen1, En, Term<Void, En, Void, Fk, Void, Gen2, Void>> gens() {
    return gens;
  }

  @Override
  public BiFunction<Sk1, Ty, Term<Ty, En, Sym, Fk, Att, Gen2, Sk2>> sks() {
    return sks;
  }

  @Override
  public Instance<Ty, En, Sym, Fk, Att, Gen1, Sk1, X1, Y1> src() {
    return src;
  }

  @Override
  public Instance<Ty, En, Sym, Fk, Att, Gen2, Sk2, X2, Y2> dst() {
    return dst;
  }
}
