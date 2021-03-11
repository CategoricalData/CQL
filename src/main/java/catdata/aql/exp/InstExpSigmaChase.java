package catdata.aql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Collage.CCollage;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Mapping;
import catdata.aql.Term;
import catdata.aql.fdm.LiteralInstance;
import catdata.aql.fdm.SigmaChaseAlgebra;
import catdata.aql.fdm.SigmaLeftKanAlgebra;

public final class InstExpSigmaChase<Gen, Sk, X, Y> extends InstExp<Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>> {

  public final InstExp<Gen, Sk, X, Y> I;
  public final MapExp F;
  public final Map<String, String> options;
  // public final Integer max;

  public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    F.map(f);
    I.map(f);
  }

  @Override
  public Map<String, String> options() {
    return options;
  }

  @Override
  public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
    return Collections.singleton(I);
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(I.deps(), F.deps());
  }

  public InstExpSigmaChase(MapExp f, InstExp<Gen, Sk, X, Y> i, Map<String, String> options) {
    I = i;
    F = f;
    this.options = options;
    // this.max = max;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    // result = prime * result + ((max == null) ? 0 : max.hashCode());
    result = prime * result + ((F == null) ? 0 : F.hashCode());
    result = prime * result + ((I == null) ? 0 : I.hashCode());
    result = prime * result + ((options == null) ? 0 : options.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    InstExpSigmaChase<?, ?, ?, ?> other = (InstExpSigmaChase<?, ?, ?, ?>) obj;
    if (F == null) {
      if (other.F != null)
        return false;
    } else if (!F.equals(other.F))
      return false;
    if (I == null) {
      if (other.I != null)
        return false;
    } else if (!I.equals(other.I))
      return false;
    if (options == null) {
      if (other.options != null)
        return false;
    } else if (!options.equals(other.options))
      return false;
    return true;
  }

  @Override
  public SchExp type(AqlTyping G) {
    SchExp t0 = I.type(G);
    Pair<SchExp, SchExp> t1 = F.type(G);

    if (!G.eq(t1.first, t0)) { // TODO aql schema equality
      throw new RuntimeException(
          "Type error: In " + this + " domain of mapping is " + t1.first + " but instance has schema " + t0);
    }

    return t1.second;
  }

  @Override
  public String toString() {
    String l = "";
    if (!options.isEmpty()) {
      l = " {\n" + Util.sep(options, " = ", "\n\t") + "\n}";
    }
    return new StringBuilder().append("sigma_chase " + F).append(" " + I).append(" " + l).toString();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public synchronized Instance<String, String, Sym, Fk, Att, Gen, Sk, Integer, Chc<Sk, Pair<Integer, Att>>> eval0(AqlEnv env,
      boolean isC) {
    if (isC) {
      throw new IgnoreException();
    }
    Mapping<String, String, catdata.aql.exp.Sym, Fk, Att, String, Fk, Att> f = F.eval(env, isC);
    Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> i = I.eval(env, isC);
    

    AqlOptions op = new AqlOptions(options, env.defaults);

    String type = (String) op.getOrDefault(AqlOption.chase_style);
    Integer reduce = (Integer) op.getOrDefault(AqlOption.talg_reduction);

    if (!("sequential".equals(type) || "parallel".equals(type))) {
      throw new RuntimeException("Style must be sequential or parallel");
    }

    if (type.equals("sequential")) {
      Collage<String, String, Sym, Fk, Att, Gen, Sk> col = new CCollage<>(f.dst.collage());

      i.sks().entrySet((a,b)->{
        col.sks().put(a,b);
      });
      
      i.gens().entrySet((a,b) -> {
        col.gens().put(a, f.ens.get(b));
      });

      List<Pair<Term<String, String, Sym, Fk, Att, Gen, Sk>,Term<String, String, Sym, Fk, Att, Gen, Sk>>> 
      l = Collections.synchronizedList(new ArrayList<>(col.eqs().size()));
      
      i.eqs((a,b) -> {
        Term<String,String,Sym,Fk,Att,Gen,Sk> aa = f.trans(a);
        Term<String,String,Sym,Fk,Att,Gen,Sk> bb = f.trans(b);
        Eq<String, String, Sym, Fk, Att, Gen, Sk> w = new Eq<>(null, aa, bb);
        col.eqs().add(w);
        l.add(new Pair<>(aa, bb));
      });
      SigmaLeftKanAlgebra<String, String, Sym, Fk, Att, String, Fk, Att, Gen, Sk, X, Y> alg = new SigmaLeftKanAlgebra<>(f, i,
          col, reduce);

      Instance zz = new LiteralInstance<>(alg.schema(), col.gens(), col.sks(), l, alg, alg,
          (Boolean) op.getOrDefault(AqlOption.require_consistency),
          (Boolean) op.getOrDefault(AqlOption.allow_java_eqs_unsafe));

      zz.validate();
      zz.validateMore();
      
      //System.out.println("valid " + zz);
      return zz;
    } else if (type.equals("parallel")) {

      SigmaChaseAlgebra<String, String, Sym, Fk, Att, String, Fk, Att, Gen, Sk, X, Y> alg = new SigmaChaseAlgebra<>(f, i,
          Collections.emptyMap(), op);
      return alg.theInst;

    }
    throw new RuntimeException("Chase style must be sequential or parallel.");
  }
  
  

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
    set.add(AqlOption.chase_style);
    set.add(AqlOption.require_consistency);
    set.add(AqlOption.allow_java_eqs_unsafe);
  }

}