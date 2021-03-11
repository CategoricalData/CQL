package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Mor;

public abstract class MorExp extends Exp<Mor<String, Sym, String, Sym>> {

  
  @Override
  public Kind kind() {
    return Kind.THEORY_MORPHISM;
  }

  public abstract Pair<TyExp, TyExp> type(AqlTyping G);

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public Exp<Mor<String, Sym, String, Sym>> Var(String v) {
    Exp ret = new MorExpVar(v);
    return ret;
  }

  public static interface MorExpCoVisitor<R, P, E extends Exception> {
    //public MorExpComp visitMorExpComp(P params, R exp) throws E;

    //public MorExpId visitMorExpId(P params, R exp) throws E;

    //public MorExpLit visitMorExpLit(P params, R exp) throws E;

    public MorExpVar visitMorExpVar(P params, R exp) throws E;

    public MorExpRaw visitMorExpRaw(P params, R exp) throws E;

    //  public MorExpColim visitMorExpColim(P params, R exp) throws E;
  }

  public abstract <R, P, E extends Exception> MorExp coaccept(P params, MorExpCoVisitor<R, P, E> v, R r) throws E;

  public static interface MorExpVisitor<R, P, E extends Exception> {
    //public <Gen, Sk, X, Y> R visit(P params, MorExpPivot<Gen, Sk, X, Y> MorExpPivot) throws E;

    //public R visit(P params, MorExpId exp) throws E;

    //public R visit(P params, MorExpLit exp) throws E;

    //public R visit(P params, MorExpComp exp) throws E;

    public R visit(P params, MorExpVar exp) throws E;

    public R visit(P params, MorExpRaw exp) throws E;

    //public R visit(P params, MorExpColim exp) throws E;
  }

  public abstract <R, P, E extends Exception> R accept(P params, MorExpVisitor<R, P, E> v) throws E;

  public static final class MorExpVar extends MorExp {
    public final String var;

    @Override
    public boolean isVar() {
      return true;
    }

    public <R, P, E extends Exception> R accept(P params, MorExpVisitor<R, P, E> v) throws E {
      return v.visit(params, this);
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {

    }

    @Override
    public <R, P, E extends Exception> MorExp coaccept(P params, MorExpCoVisitor<R, P, E> v, R r) throws E {
      return v.visitMorExpVar(params, r);
    }

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.singleton(new Pair<>(var, Kind.THEORY_MORPHISM));
    }

    public MorExpVar(String var) {
      this.var = var;
    }

    @Override
    public Mor<String, Sym, String, Sym> eval0(AqlEnv env,
        boolean isC) {
      return (Mor<String, Sym, String, Sym>) env.defs.tms.get(var);
    }

    @Override
    public int hashCode() {
      return var.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      MorExpVar other = (MorExpVar) obj;
      return var.equals(other.var);
    }

    @Override
    public String toString() {
      return var;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Pair<TyExp, TyExp> type(AqlTyping G) {
      if (!G.defs.tms.containsKey(var)) {
        throw new RuntimeException("Not a theory_morphism: " + var);
      }
      return (Pair<TyExp, TyExp>) (G.defs.tms.get(var));
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {

    }

  }

/////////////////////////////////////////////////////////////////////

  
}