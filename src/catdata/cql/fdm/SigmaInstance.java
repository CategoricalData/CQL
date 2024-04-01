package catdata.cql.fdm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.Algebra;
import catdata.cql.AqlOptions;
import catdata.cql.Collage;
import catdata.cql.DP;
import catdata.cql.Eq;
import catdata.cql.Instance;
import catdata.cql.Mapping;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;

public class SigmaInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y>
    extends Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>> {

  private final Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> F;
  private final Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I;
  private final LiteralInstance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>> J;

  public SigmaInstance(Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> f,
      Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i, AqlOptions strat) {
    F = f;
    I = i;

    Collage<Ty, En2, Sym, Fk2, Att2, Gen, Sk> col = new CCollage<>();

    I.sks().entrySet((k, v) -> {
      col.sks().put(k, v);
    });

    I.gens().keySet(gen -> {
      col.gens().put(gen, F.ens.get(I.gens().get(gen)));
    });

    List<Pair<Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>,Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>>> l = new ArrayList<>(col.eqs().size());
    I.eqs((a, b) -> {
      Term<Ty,En2,Sym,Fk2,Att2,Gen,Sk> aa = F.trans(a);
      Term<Ty,En2,Sym,Fk2,Att2,Gen,Sk> bb = F.trans(b);
      Eq<Ty, En2, Sym, Fk2, Att2, Gen, Sk> w = new Eq<>(null, aa, bb);
      col.eqs().add(w);
      l.add(new Pair<>(aa, bb));
    });

    Function<Gen, Object> printGen = (x) -> I.algebra().printX(I.type(Term.Gen(x)).r, I.algebra().nf(Term.Gen(x)));
    BiFunction<Ty, Sk, Object> printSk = (y, x) -> I.algebra().sk(x)
        .toString(z -> I.algebra().printY(y, z).toString(), Util.voidFn(), false);
    InitialAlgebra<Ty, En2, Sym, Fk2, Att2, Gen, Sk> initial = new InitialAlgebra<>(strat, schema(), col, printGen,
        printSk);
    
    J = new LiteralInstance<>(schema(), col.gens(), col.sks(), l, initial.dp(), initial,
        (Boolean) strat.getOrDefault(AqlOption.require_consistency),
        (Boolean) strat.getOrDefault(AqlOption.allow_java_eqs_unsafe));
    validate();
    validateMore();
  }

  @Override
  public Schema<Ty, En2, Sym, Fk2, Att2> schema() {
    return F.dst;
  }

  @Override
  public IMap<Gen, En2> gens() {
    return J.gens();
  }

  @Override
  public IMap<Sk, Ty> sks() {
    return J.sks();
  }

  @Override
  public synchronized void eqs(
      BiConsumer<Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>, Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>> f) {
    I.eqs((x, y) -> f.accept(F.trans(x), F.trans(y)));
  }

  @Override
  public DP<Ty, En2, Sym, Fk2, Att2, Gen, Sk> dp() {
    return J.dp();
  }

  @Override
  public Algebra<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att2>>> algebra() {
    return J.algebra();
  }

  @Override
  public boolean requireConsistency() {
    return J.requireConsistency();
  }

  @Override
  public boolean allowUnsafeJava() {
    return J.allowUnsafeJava();
  }

}
