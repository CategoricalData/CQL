package catdata.cql.fdm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.Algebra;
import catdata.cql.DP;
import catdata.cql.Eq;
import catdata.cql.Schema;
import catdata.cql.Term;
import gnu.trove.map.hash.THashMap;

public class ImportAlgebra<Ty, En, Sym, Fk, Att, X, Y> extends Algebra<Ty, En, Sym, Fk, Att, X, Y, X, Y>
    implements DP<Ty, En, Sym, Fk, Att, X, Y> {

  @Override
  public boolean hasNulls() {
    return talg.sks.isEmpty();
  }

  private final Schema<Ty, En, Sym, Fk, Att> schema;
  private final Function<En, Collection<X>> ens;
  private final Map<Ty, Collection<Y>> tys;
  private final BiFunction<En, X, Map<Fk, X>> fks;
  private final BiFunction<En, X, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Y>>> atts;

  private final BiFunction<En, X, Object> printX;
  private final BiFunction<Ty, Y, Object> printY;

  private final TAlg<Ty, Sym, Y> talg;

  //private Map<X, En> gens = new THashMap<>();

  public ImportAlgebra(Schema<Ty, En, Sym, Fk, Att> schema, Function<En, Collection<X>> ens,
      Map<Ty, Collection<Y>> tys, BiFunction<En, X, Map<Fk, X>> fks,
      BiFunction<En, X, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Y>>> atts,
      BiFunction<En, X, Object> printX, BiFunction<Ty, Y, Object> printY, boolean dontCheckClosure,
      Collection<Eq<Ty, Void, Sym, Void, Void, Void, Y>> eqs) {
	  if (schema == null) {
		  Util.anomaly();
	  }
    this.schema = schema;
    this.ens = ens;
    this.tys = tys;
    this.fks = fks;
    this.atts = atts;
    this.printX = printX;
    this.printY = printY;

    if (!dontCheckClosure) {
      checkClosure();
    }

    int ts = 0;
    for (Collection<Y> x : tys.values()) {
      ts += x.size();
    }

    // TODO: land onto definitions rather than equations
    talg = new TAlg<>(new THashMap<>(ts), new ArrayList<>(eqs.size()));
    for (Entry<Ty, Collection<Y>> ty : tys.entrySet()) {
      for (Y y : ty.getValue()) {
        talg.sks.put(y, ty.getKey());
      }
    }
    for (Eq<Ty, Void, Sym, Void, Void, Void, Y> x : eqs) {
      talg.eqsNoDefns().add(new Pair<>(x.lhs, x.rhs));
    }


  }

  private void checkClosure() {
    for (En en : schema.ens) {
      for (X x : ens.apply(en)) {
        for (Fk fk : schema.fksFrom(en)) {
          if (fks.apply(en, x).get(fk) == null) {
            throw new RuntimeException("Incomplete import: no value for foreign key " + fk
                + " specified for  ID " + x + " in entity " + en);
          }
          X y = fk(fk, x);
          if (!ens.apply(schema.fks.get(fk).second).contains(y)) {
            throw new RuntimeException("Incomplete import: value for " + x + "'s foreign key " + fk + " is "
                + y + " which is not an ID imported into " + schema.fks.get(fk).second);
          }
        }
        for (Att att : schema.attsFrom(en)) {
          if (atts.apply(en, x).get(att) == null) {
            throw new RuntimeException("Incomplete import: no value for attribute " + att
                + " specified for  ID " + x + " in entity " + en);
          }
          Term<Ty, Void, Sym, Void, Void, Void, Y> y = att(att, x);
          if (y.sk() != null && !tys.get(schema.atts.get(att).second).contains(y.sk())) {
            throw new RuntimeException("Incomplete import: value for " + x + "'s attribute " + att + " is "
                + y.sk() + " which is not an ID imported into " + schema.atts.get(att).second);
          }
        }
      }
    }
    for (Ty ty : schema.typeSide.tys) {
      if (!tys.containsKey(ty)) {
        throw new RuntimeException("Incomplete import: no skolem values for " + ty);
      }
    }

  }

  @Override
  public synchronized boolean eq(Map<String, Chc<Ty, En>> ctx, Term<Ty, En, Sym, Fk, Att, X, Y> lhs,
      Term<Ty, En, Sym, Fk, Att, X, Y> rhs) {
    if (ctx != null && !ctx.isEmpty()) {
      Util.anomaly();
    } else if (lhs.hasTypeType()) {
      if (schema.typeSide.js.java_tys.isEmpty()) {
        return intoY(lhs).equals(intoY(rhs)); // free talg
      }
      return schema.typeSide.js.reduce(intoY(lhs)).equals(schema.typeSide.js.reduce(intoY(rhs))); // free talg

//      schema.typeSide.js.reduce(lhs)
    }
    return intoX(lhs).equals(intoX(rhs));
  }

  @Override
  public Schema<Ty, En, Sym, Fk, Att> schema() {
    return schema;
  }

  @Override
  public Collection<X> en(En en) {
    return ens.apply(en);
  }

  @Override
  public X gen(X gen) {
    return gen;
  }

  @Override
  public X fk(Fk fk, X x) {
    return fks.apply(schema.fks.get(fk).first, x).get(fk);
  }

  @Override
  public Term<Ty, Void, Sym, Void, Void, Void, Y> att(Att att, X x) {
		// System.out.println("Att " + att + " on " + x  + " and " +  schema.atts.keySet() );

	  if (schema.atts.get(att) == null) {
	//	 System.out.println("Att " + att + " on " + x  + " and " +  schema.atts.get(att) );
		// System.out.println("Att " + att + " on " + x  + " and " +  schema.atts.keySet() );
	//	 System.out.println(schema.atts.get(att) == null );
	//	 System.out.println("Att " + att + " on " + x  + " and " +  schema.atts.keySet() );
	//	 System.out.println(schema.atts.get(att) == null );
				try {
					Thread.currentThread().sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	//			System.out.println(schema.atts.get(att) == null );
		  Util.anomaly();
		  
	  }
	//  System.out.println("Att " + att + " on " + x  + " and " +  schema.atts.get(att) );
    return atts.apply(schema.atts.get(att).first, x).get(att);
  }

  @Override
  public Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Y sk) {
    return Term.Sk(sk);
  }

  @Override
  public Term<Void, En, Void, Fk, Void, X, Void> repr(En en, X x) {
    return Term.Gen(x);
  }

  @Override
  public String toStringProver() {
    return "Import algebra prover";
  }

  @Override
  public Object printX(En en, X x) {
    return printX.apply(en, x);
  }

  @Override
  public Object printY(Ty ty, Y y) {
    return printY.apply(ty, y);
  }

  @Override
  public TAlg<Ty, Sym, Y> talg0() {
    return talg;
  }

  @Override
  public boolean hasFreeTypeAlgebra() {
    return talg.eqsNoDefns().isEmpty();
  }

  @Override
  public int size(En en) {
    var x = ens.apply(en);
    return x.size();
  }

  @Override
  public Chc<Y, Pair<X, Att>> reprT_prot(Y y) {
    return Chc.inLeft(y);
  }

}
