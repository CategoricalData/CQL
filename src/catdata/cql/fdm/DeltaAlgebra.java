package catdata.cql.fdm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import catdata.Chc;
import catdata.Pair;
import catdata.cql.Algebra;
import catdata.cql.Instance;
import catdata.cql.Mapping;
import catdata.cql.Schema;
import catdata.cql.Term;
import gnu.trove.map.hash.THashMap;

public class DeltaAlgebra<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y>
    extends Algebra<Ty, En1, Sym, Fk1, Att1, Pair<En1, X>, Y, Pair<En1, X>, Y> {

  private final Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> F;
  private final Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> J;

  @Override
  public String toStringProver() {
    return J.algebra().toStringProver();
  }

  public DeltaAlgebra(Mapping<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> F,
      Instance<Ty, En2, Sym, Fk2, Att2, Gen, Sk, X, Y> alg) {
    this.F = F;
    this.J = alg;
  }

  @Override
  public Schema<Ty, En1, Sym, Fk1, Att1> schema() {
    return F.src;
  }

  public Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk> translate(Term<Ty, En1, Sym, Fk1, Att1, Pair<En1, X>, Y> e) {
    if (e.var != null) {
      return e.convert();
    } else if (e.obj() != null) {
      return e.convert();
    } else if (e.gen() != null) {
      return J.algebra().repr(F.ens.get(e.gen().first), e.gen().second).convert();
    } else if (e.fk() != null) {
      return Schema.fold(F.fks.get(e.fk()).second, translate(e.arg));
    } else if (e.att() != null) {
      Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk> t = F.atts.get(e.att()).third.convert();
      Map mm = Collections.singletonMap(F.atts.get(e.att()).first, translate(e.arg));
      return t.subst(mm);
    } else if (e.sym() != null) {
      List<Term<Ty, En2, Sym, Fk2, Att2, Gen, Sk>> l = new ArrayList<>(e.args.size());
      for (Term<Ty, En1, Sym, Fk1, Att1, Pair<En1, X>, Y> x : e.args) {
        l.add(translate(x));
      }
      return Term.Sym(e.sym(), l);
    } else if (e.sk() != null) {
      return J.reprT(Term.Sk(e.sk()));
    }
    throw new RuntimeException("Anomaly: please report: " + e);
  }

  @Override
  public Pair<En1, X> gen(Pair<En1, X> gen) {
    return gen;
  }

  private final Map<En1, Collection<Pair<En1, X>>> en_cache = new THashMap<>();

  @Override
  public synchronized Collection<Pair<En1, X>> en(En1 en) {
    if (en_cache.containsKey(en)) {
      return en_cache.get(en);
    }
    Iterable<X> in = J.algebra().en(F.ens.get(en));
    Collection<Pair<En1, X>> ret = new ArrayList<>(J.algebra().size(F.ens.get(en)));
    for (X x : in) {
      ret.add(new Pair<>(en, x));
    }
    en_cache.put(en, ret);
    return ret;
  }

  @Override
  public Pair<En1, X> fk(Fk1 fk1, Pair<En1, X> e) {
    X x = e.second;
    for (Fk2 fk2 : F.trans(Collections.singletonList(fk1))) {
      x = J.algebra().fk(fk2, x);
    }
    En1 en1 = F.src.fks.get(fk1).second;
    return new Pair<>(en1, x);
  }

  @Override
  public TAlg<Ty, Sym, Y> talg0() {
    return J.algebra().talg();
  }

  @Override
  public Term<Void, En1, Void, Fk1, Void, Pair<En1, X>, Void> repr(En1 en, Pair<En1, X> x) {
    return Term.Gen(x);
  }

  @Override
  public Term<Ty, Void, Sym, Void, Void, Void, Y> att(Att1 att, Pair<En1, X> e) {
    Term<Ty, Void, Sym, Void, Void, Void, Y> ret = attY(F.atts.get(att).third, e);
    return J.algebra().intoY(J.reprT(ret));
  }

  private Term<Ty, Void, Sym, Void, Void, Void, Y> attY(Term<Ty, En2, Sym, Fk2, Att2, Void, Void> term,
      Pair<En1, X> x) {
    if (term.obj() != null) {
      return term.convert();
    } else if (term.att() != null) {
      return J.algebra().att(term.att(), attX(term.arg.asArgForAtt().convert(), x.second));
    } else if (term.sym() != null) {
      List<Term<Ty, Void, Sym, Void, Void, Void, Y>> l = new ArrayList<>(term.args.size());
      for (Term<Ty, En2, Sym, Fk2, Att2, Void, Void> xx : term.args) {
        l.add(attY(xx, x));
      }
      return Term.Sym(term.sym(), l);
    }
    throw new RuntimeException("Anomaly: please report: " + term + " and " + x);
  }

  private X attX(Term<Void, En2, Void, Fk2, Void, Gen, Void> term, X x) {
    if (term.var != null) {
      return x;
    } else if (term.gen() != null) {
      return J.algebra().nf(term);
    } else if (term.fk() != null) {
      return J.algebra().fk(term.fk(), attX(term.arg, x));
    }
    throw new RuntimeException("Anomaly: please report");
  }

  @Override
  public Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Y sk) {
    return Term.Sk(sk);
  }

  @Override
  public Object printY(Ty ty, Y y) {
    return J.algebra().printY(ty, y);
  }

  @Override
  public Object printX(En1 en, Pair<En1, X> p) {
    return p.first + ":" + J.algebra().printX(F.ens.get(en), p.second);
  }

  @Override
  public boolean hasFreeTypeAlgebra() {
    return J.algebra().hasFreeTypeAlgebra();
  }

  @Override
  public boolean hasFreeTypeAlgebraOnJava() {
    return J.algebra().hasFreeTypeAlgebraOnJava();
  }

  public String talgToString() {
    return J.algebra().talgToString();
  }

  @Override
  public int size(En1 en) {
    return J.algebra().size(F.ens.get(en));
  }

  @Override
  public Chc<Y, Pair<Pair<En1, X>, Att1>> reprT_prot(Y y) {
    return Chc.inLeft(y);
  }

  @Override
  public boolean hasNulls() {
    return J.algebra().hasNulls();
  }

}