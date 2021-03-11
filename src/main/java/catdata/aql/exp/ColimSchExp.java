package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.ColimitSchema;
import catdata.aql.Kind;

public abstract class ColimSchExp extends Exp<ColimitSchema<String>> {

  @Override
  public Kind kind() {
    return Kind.SCHEMA_COLIMIT;
  }

  public abstract SchExp getNode(String n, AqlTyping G);

  public abstract TyExp typeOf(AqlTyping G);

  public abstract Set<String> type(AqlTyping G);

  public abstract Set<Pair<SchExp, SchExp>> gotos(ColimSchExp ths);

  @Override
  public Exp<ColimitSchema<String>> Var(String v) {
    Exp<ColimitSchema<String>> ret = new ColimSchExpVar(v);
    return ret;
  }

  public static interface ColimSchExpCoVisitor<R, P, E extends Exception> {
    public abstract ColimSchExpQuotient visitColimSchExpQuotient(P params, R exp) throws E;

    public abstract ColimSchExpRaw visitColimSchExpRaw(P params, R exp) throws E;

    public abstract ColimSchExpVar visitColimSchExpVar(P params, R exp) throws E;

    public abstract ColimSchExpWrap visitColimSchExpWrap(P params, R exp) throws E;

    public abstract ColimSchExpModify visitColimSchExpModify(P params, R exp) throws E;
  }

  public static interface ColimSchExpVisitor<R, P, E extends Exception> {
    public abstract R visit(P params, ColimSchExpQuotient exp) throws E;

    public abstract R visit(P params, ColimSchExpRaw exp) throws E;

    public abstract R visit(P params, ColimSchExpVar exp) throws E;

    public abstract R visit(P params, ColimSchExpWrap exp) throws E;

    public abstract R visit(P params, ColimSchExpModify exp) throws E;
  }

  public abstract <R, P, E extends Exception> R accept(P params, ColimSchExpVisitor<R, P, E> v) throws E;

  /////////////////////////////////////////////////////////////////

  public static final class ColimSchExpVar extends ColimSchExp {
    public final String var;

    public <R, P, E extends Exception> R accept(P param, ColimSchExpVisitor<R, P, E> v) throws E {
      return v.visit(param, this);
    }

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    @Override
    public boolean isVar() {
      return true;
    }

    @Override
    public SchExp getNode(String n, AqlTyping G) {
      if (!G.prog.exps.containsKey(var)) {
        throw new RuntimeException("Not a named top-level declaration: " + var);
      }
      return ((ColimSchExp) G.prog.exps.get(var)).getNode(n, G);
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.singleton(new Pair<>(var, Kind.SCHEMA_COLIMIT));
    }

    public ColimSchExpVar(String var) {
      this.var = var;
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
      ColimSchExpVar other = (ColimSchExpVar) obj;
      return var.equals(other.var);
    }

    @Override
    public String toString() {
      return var;
    }

    @Override
    public synchronized ColimitSchema<String> eval0(AqlEnv env, boolean isC) {
      return env.defs.scs.get(var);
    }

    @Override
    public Set<String> type(AqlTyping G) {
      Set<String> v = G.defs.scs.get(var);
      Util.assertNotNull(v);
      return v;
    }

    @Override
    public TyExp typeOf(AqlTyping G) {
      Exp<?> l = G.prog.exps.get(var);

      if (l == null) {
        throw new RuntimeException("Not a colimit schema variable: " + var);
      }
      return ((ColimSchExp) l).typeOf(G);

    }

    @Override
    public Set<Pair<SchExp, SchExp>> gotos(ColimSchExp ths) {
      return Collections.emptySet();
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {

    }

  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
