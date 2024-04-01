package catdata.cql.fdm;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.BiFunction;

import com.google.common.collect.Iterators;

import catdata.Util;
import catdata.cql.Algebra;
import catdata.cql.DP;
import catdata.cql.Eq;
import catdata.cql.Instance;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.Transform;
import gnu.trove.map.hash.THashMap;

public class DiffInstance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> extends Instance<Ty, En, Sym, Fk, Att, X, Y, X, Y> {

  private final Instance<Ty, En, Sym, Fk, Att, X, Y, X, Y> K;

  public final Transform<Ty, En, Sym, Fk, Att, X, Y, Gen, Sk, X, Y, X, Y> h;

  public <Z> DiffInstance(Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I,
      Instance<Ty, En, Sym, Fk, Att, ?, ?, Z, Y> J, boolean uj, boolean rc) {
    if (!I.schema().fks.isEmpty()) {
      throw new RuntimeException("Can't diff with fks.");
    }
    if (!I.schema().equals(J.schema())) {
      throw new RuntimeException("Schemas differ.");
    }
    if (!I.algebra().talg().equals(J.algebra().talg())) {
      throw new RuntimeException("Type algebras not the same.");
    }

    Map<En, Collection<X>> ens = new THashMap<>(J.schema().ens.size(), 2);
    Map<Ty, Collection<Y>> tys = new THashMap<>(J.schema().typeSide.tys.size(), 2);

    BiFunction<X, En, Term<Void, En, Void, Fk, Void, Gen, Void>> m = (x,en) -> I.algebra().repr(en, x); 
    BiFunction<Y, Ty, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> n = (k,t) -> I.reprT(Term.Sk(k));

    for (Ty ty : I.schema().typeSide.tys) {
      tys.put(ty, new LinkedList<>());
    }
    for (Y k : I.algebra().talg().sks.keySet()) {
      tys.get(I.algebra().talg().sks.get(k)).add(k);
    }

    Map<En, Map<X, Map<Fk, X>>> fks0 = new THashMap<>(J.schema().ens.size(), 2);
    Map<En, Map<X, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Y>>>> atts0 = new THashMap<>(J.schema().ens.size(), 2);

    BiFunction<En, X, Boolean> inOther = (en, x) -> {
      outer: for (Z y : J.algebra().en(en)) {
        for (Att att : I.schema().attsFrom(en)) {
          Term<Ty, Void, Sym, Void, Void, Void, Y> l = J.algebra().att(att, y);
          Term<Ty, Void, Sym, Void, Void, Void, Y> r = I.algebra().att(att, x);
          Term<Ty, En, Sym, Fk, Att, Gen, Sk> l2 = I.reprT(l);
          Term<Ty, En, Sym, Fk, Att, Gen, Sk> r2 = I.reprT(r);
          if (!I.dp().eq(null, l2, r2)) {
            continue outer;
          }
        }
        return true;
      }
      return false;
    };

    for (En en : I.schema().ens) {
      Collection<X> c = new ArrayList<>(I.algebra().size(en));
      ens.put(en, c);
      Map<X, Map<Fk, X>> fks = new THashMap<>(I.algebra().size(en) , 2);
      Map<X, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Y>>> atts = new THashMap<>(I.algebra().size(en), 2);
      atts0.put(en, atts);
      for (X x : I.algebra().en(en)) {
        if (inOther.apply(en, x)) {
          continue;
        }
        fks.put(x, Collections.emptyMap()); // no fks
        atts.put(x, new THashMap<>( I.schema().attsFrom(en).size() , 2));
        
        c.add(x);

        for (Att att : I.schema().attsFrom(en)) {
          atts.get(x).put(att, I.algebra().att(att, x));
        }
      }
    }

    boolean dontCheckClosure = false;

    Collection<Eq<Ty, Void, Sym, Void, Void, Void, Y>> ooo = new AbstractCollection<>() {
      @Override
      public synchronized Iterator<Eq<Ty, Void, Sym, Void, Void, Void, Y>> iterator() {
        return Iterators.transform(I.algebra().talg().eqsNoDefns().iterator(), x->new Eq<>(null,x.first,x.second));
      }

      @Override
      public int size() {
        return I.algebra().talg().eqsNoDefns().size();
      }
    };


    int j = 0;
    for (@SuppressWarnings("unused") Eq<Ty, Void, Sym, Void, Void, Void, Y> i : ooo) {
      j++;
    }
    if (j != ooo.size()) {
      Util.anomaly();
    }
    
    ImportAlgebra<Ty, En, Sym, Fk, Att, X, Y> alg 
    = new ImportAlgebra<>(I.schema(), en -> ens.get(en), tys, (en, x) -> fks0.get(en).get(x), (en, x) -> atts0.get(en).get(x),
        I.algebra()::printX, I.algebra()::printY, dontCheckClosure,
        ooo);

    K = new SaturatedInstance<>(alg, alg, rc, uj, false, Collections.EMPTY_MAP);
    validate();

    h = new LiteralTransform<>(m, n, this, I, true);
  }

  @Override
  public Schema<Ty, En, Sym, Fk, Att> schema() {
    return K.schema();
  }

  @Override
  public IMap<X, En> gens() {
    return K.gens();
  }

  @Override
  public IMap<Y, Ty> sks() {
    return K.sks();
  }

  @Override
  public boolean requireConsistency() {
    return K.requireConsistency();
  }

  @Override
  public boolean allowUnsafeJava() {
    return K.requireConsistency();
  }

  @Override
  public DP<Ty, En, Sym, Fk, Att, X, Y> dp() {
    return K.dp();
  }

  @Override
  public Algebra<Ty, En, Sym, Fk, Att, X, Y, X, Y> algebra() {
    return K.algebra();
  }

}
