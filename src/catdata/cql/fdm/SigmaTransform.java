package catdata.cql.fdm;

import java.util.function.BiFunction;

import catdata.Chc;
import catdata.Pair;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Mapping;
import catdata.cql.Term;
import catdata.cql.Transform;

public class SigmaTransform<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1, En2, Fk2, Att2, Gen2, Sk2, X1, Y1, X2, Y2> extends
    Transform<Ty, En2, Sym, Fk2, Att2, Gen1, Sk1, Gen2, Sk2, Integer, Chc<Sk1, Pair<Integer, Att2>>, Integer, Chc<Sk2, Pair<Integer, Att2>>> {


  private final SigmaInstance<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1, En2, Fk2, Att2, X1, Y1> src;
  private final SigmaInstance<Ty, En1, Sym, Fk1, Att1, Gen2, Sk2, En2, Fk2, Att2, X2, Y2> dst;

  private final BiFunction<Gen1, En2, Term<Void, En2, Void, Fk2, Void, Gen2, Void>> gens; 
  private final BiFunction<Sk1, Ty, Term<Ty, En2, Sym, Fk2, Att2, Gen2, Sk2>> sks; 

  // TODO: aql this recomputes the instances
  public SigmaTransform(Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> f,
      Transform<Ty, En1, Sym, Fk1, Att1, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h, AqlOptions options1,
      AqlOptions options2) {
    if (!h.src().schema().equals(f.src)) {
      throw new RuntimeException("Source of mapping is " + f.src + " but instances are on " + h.src().schema());
    }
  //  F = f;
    //this.h = h;
    src = new SigmaInstance<>(f, h.src(), options1);
    dst = new SigmaInstance<>(f, h.dst(), options2);
    gens = (gen1,t) -> f.trans(h.gens().apply(gen1,h.src().gens().get(gen1)).convert()).convert();
    sks = (sk1,t) -> f.trans(h.sks().apply(sk1,t));
    //validate(true);
  }

  @Override
  public BiFunction<Gen1, En2, Term<Void, En2, Void, Fk2, Void, Gen2, Void>> gens() {
    return gens;
  }

  @Override
  public BiFunction<Sk1, Ty, Term<Ty, En2, Sym, Fk2, Att2, Gen2, Sk2>> sks() {
    return sks;
  }

  @Override
  public Instance<Ty, En2, Sym, Fk2, Att2, Gen1, Sk1, Integer, Chc<Sk1, Pair<Integer, Att2>>> src() {
    return src;
  }

  @Override
  public Instance<Ty, En2, Sym, Fk2, Att2, Gen2, Sk2, Integer, Chc<Sk2, Pair<Integer, Att2>>> dst() {
    return dst;
  }

}
