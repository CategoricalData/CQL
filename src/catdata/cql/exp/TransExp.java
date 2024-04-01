package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.cql.Kind;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.InstExp.InstExpLit;

public abstract class TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>
    extends Exp<Transform<String, String, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>> {

  @Override
  public Kind kind() {
    return Kind.TRANSFORM;
  }

  public abstract Pair<InstExp<Gen1, Sk1, X1, Y1>, InstExp<Gen2, Sk2, X2, Y2>> type(AqlTyping G);

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public Exp<Transform<String, String, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>> Var(String v) {
    Exp ret = new TransExpVar(v);
    return ret;
  }

  public abstract <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E;

  ///////////////////////////////////////////////////////////////////////////////////////

  public static final class TransExpVar
      extends TransExp<Object, Object, Object, Object, Object, Object, Object, Object> {
    public final String var;

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    @Override
    public boolean isVar() {
      return true;
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.singleton(new Pair<>(var, Kind.TRANSFORM));
    }

    public TransExpVar(String var) {
      this.var = var;
    }

    @Override
    public Transform<String, String, Sym, Fk, Att, Object, Object, Object, Object, Object, Object, Object, Object> eval0(
        AqlEnv env, boolean isC) {
      return env.defs.trans.get(var);
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {

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
      TransExpVar other = (TransExpVar) obj;
      if (var == null) {
        if (other.var != null)
          return false;
      } else if (!var.equals(other.var))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return var;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Pair<InstExp<Object, Object, Object, Object>, InstExp<Object, Object, Object, Object>> type(
        AqlTyping G) {
      if (!G.defs.trans.containsKey(var)) {
        throw new RuntimeException("Not a transform: " + var);
      }
      return (Pair<InstExp<Object, Object, Object, Object>, InstExp<Object, Object, Object, Object>>) ((Object) G.defs.trans
          .get(var));
    }

    public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
      return v.visit(params, this);
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {

    }

  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  public static final class TransExpLit<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2>
      extends TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> {

    public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) throws E {
      return v.visit(params, this);
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.emptyList();
    }

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    public final Transform<String, String, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> trans;

    public TransExpLit(Transform<String, String, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> trans) {
      this.trans = trans;
    }

    @Override
    public Transform<String, String, Sym, Fk, Att, Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> eval0(AqlEnv env,
        boolean isC) {
      return trans;
    }

    @Override
    public int hashCode() {
      return trans.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      TransExpLit<?, ?, ?, ?, ?, ?, ?, ?> other = (TransExpLit<?, ?, ?, ?, ?, ?, ?, ?>) obj;
      return trans.equals(other.trans);
    }

    @Override
    public String toString() {
      return trans.toString();
    }

    @Override
    public Pair<InstExp<Gen1, Sk1, X1, Y1>, InstExp<Gen2, Sk2, X2, Y2>> type(AqlTyping G) {
      return new Pair<>(new InstExpLit<>(trans.src()), new InstExpLit<>(trans.dst()));
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {

    }

  }

}