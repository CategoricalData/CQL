package catdata.cql.fdm;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Query;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import gnu.trove.map.hash.THashMap;

public class CoEvalEvalUnitTransform<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> extends
    Transform<Ty, En2, Sym, Fk2, Att2, Gen, Sk, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>>>, Chc<En1,Ty>>, Chc<Chc<Triple<String, X, En2>, Y>, Pair<Integer, Att1>>, X, Y, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>>>, Chc<En1,Ty>>, Chc<Chc<Triple<String, X, En2>, Y>, Pair<Integer, Att1>>> {
  // TODO aql recomputes
  private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;
  private final Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> I;
  private final CoEvalInstance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> J;
  private final EvalInstance<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>, En2, Fk2, Att2, Integer, Chc<Chc<Triple<String, X, En2>, Y>, Pair<Integer, Att1>>> K;
  private final BiFunction<Gen, En2, Term<Void, En2, Void, Fk2, Void, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>>>, Chc<En1,Ty>>, Void>> gens; 
  private final BiFunction<Sk, Ty, Term<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>>>, Chc<En1,Ty>>, Chc<Chc<Triple<String, X, En2>, Y>, Pair<Integer, Att1>>>> sks; 

  public CoEvalEvalUnitTransform(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q,
      Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> i, AqlOptions options) {
    if (!q.dst.equals(i.schema())) {
      throw new RuntimeException("Q has dst schema " + q.src + " but instance has schema " + i.schema());
    }
    Q = q;
    I = i;
    J = new CoEvalInstance<>(Q, I, options);
    K = new EvalInstance<>(Q, J, options);

    gens = (gen, en2) -> {

      X x = I.algebra().gen(gen);
      List<String> l = K.order(en2);
      Map<String, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>>>> tuple = new THashMap<>(
          l.size());
      for (String v : l) {
        if (Q.ens.get(en2).gens.containsKey(v)) {
          tuple.put(v, Chc.inLeft(J.algebra().gen(new Triple<>(v, x, en2))));
        } else {
          tuple.put(v, Chc.inRight(J.reprT(Term.Sk(Chc.inLeft(Chc.inLeft(new Triple<>(v, x, en2)))))));
        }
      }

      return Term.Gen(Row.mkRow(l, tuple, en2, Util.inLeft(Q.ens.get(en2).gens), Util.inRight(Q.ens.get(en2).sks)));
    };
    sks = (sk, t) -> trans0(I.algebra().sk(sk)).convert();

    validate((Boolean) options.getOrDefault(AqlOption.dont_validate_unsafe));
  }

  private Term<Ty, Void, Sym, Void, Void, Void, Chc<Chc<Triple<String, X, En2>, Y>, Pair<Integer, Att1>>> trans0(
      Term<Ty, Void, Sym, Void, Void, Void, Y> term) {
    if (term.sk() != null) {
      return Term.Sk(Chc.inLeft(Chc.inRight(term.sk()))); //TODO ryan
    } else if (term.sym() != null) {
      return Term.Sym(term.sym(), term.args.stream().map(this::trans0).collect(Collectors.toList()));
    } else if (term.obj() != null) {
      return term.asObj();
    }
    throw new RuntimeException("Anomaly: please report");
  }

  @Override
  public BiFunction<Gen, En2, Term<Void, En2, Void, Fk2, Void, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>>>, Chc<En1,Ty>>, Void>> gens() {
    return gens;
  }

  @Override
  public BiFunction<Sk, Ty, Term<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>>>, Chc<En1,Ty>>, Chc<Chc<Triple<String, X, En2>, Y>, Pair<Integer, Att1>>>> sks() {
    return sks;
  }

  @Override
  public Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> src() {
    return I;
  }

  @Override
  public Instance<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>>>, Chc<En1,Ty>>, Chc<Chc<Triple<String, X, En2>, Y>, Pair<Integer, Att1>>, Row<En2, Chc<Integer, Term<Ty, En1, Sym, Fk1, Att1, Triple<String, X, En2>, Chc<Triple<String, X, En2>, Y>>>, Chc<En1,Ty>>, Chc<Chc<Triple<String, X, En2>, Y>, Pair<Integer, Att1>>> dst() {
    return K;
  }

}
