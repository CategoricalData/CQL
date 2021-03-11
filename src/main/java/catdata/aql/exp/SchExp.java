package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Program;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.exp.TyExp.TyExpLit;

public abstract class SchExp extends Exp<Schema<String, String, Sym, Fk, Att>> {

  public static interface SchExpCoVisitor<R, P, E extends Exception> {
    public abstract SchExpEmpty visitSchExpEmpty(P params, R exp) throws E;

    public abstract <Gen, Sk, X, Y> SchExpInst<Gen, Sk, X, Y> visitSchExpInst(P params, R exp) throws E;

    public abstract SchExpLit visitSchExpLit(P params, R exp) throws E;

    public abstract <Gen, Sk, X, Y> SchExpPivot<Gen, Sk, X, Y> visitSchExpPivot(P params, R exp) throws E;

    public abstract SchExpVar visitSchExpVar(P params, R exp) throws E;

    public abstract SchExpRaw visitSchExpRaw(P params, R exp) throws E;

    public abstract SchExpRdf visitSchExpRdf(P params, R exp) throws E;

    public abstract SchExpTinkerpop visitSchExpTinkerpop(P params, R exp) throws E;
    
    public abstract SchExpColim visitSchExpColim(P params, R exp) throws E;
    
    public abstract SchExpFront visitSchExpFront(P params, R exp) throws E;

    public abstract SchExpDom visitSchExpDom(P params, R exp) throws E;

    public abstract SchExpPrefix visitSchExpPrefix(P params, R exp) throws E;

    public abstract SchExpCod visitSchExpCod(P params, R exp) throws E;

    public abstract SchExpSrc visitSchExpSrc(P params, R exp) throws E;

    public abstract SchExpSpan visitSchExpSpan(P params, R exp) throws E;

    public abstract SchExpDst visitSchExpDst(P params, R exp) throws E;

    public abstract SchExpJdbcAll visitSchExpJdbcAll(P params, R r) throws E;

    public abstract SchExpMsCatalog visitSchExpMsCatalog(P params, R r) throws E;

    public abstract SchExpMsQuery visitSchExpMsQuery(P params, R r) throws E;

    public abstract SchExpMsError visitSchExpMsError(P params, R r) throws E;

    public abstract SchExpMsErrorShallow visitSchExpMsErrorShallow(P params, R r) throws E;

    public abstract SchExpCsv visitSchExpCsv(P params, R r) throws E;

    public abstract SchExpFromMsCatalog visitSchExpFromMsCatalog(P params, R r) throws E;
  }

  public abstract <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E;

  public static interface SchExpVisitor<R, P, E extends Exception> {
    public abstract R visit(P params, SchExpEmpty exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P params, SchExpInst<Gen, Sk, X, Y> exp) throws E;

    public abstract R visit(P params, SchExpLit exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P params, SchExpPivot<Gen, Sk, X, Y> exp) throws E;

    public abstract R visit(P params, SchExpVar exp) throws E;

    public abstract R visit(P params, SchExpRaw exp) throws E;

    public abstract <N> R visit(P params, SchExpColim exp) throws E;

    public abstract R visit(P param, SchExpMsCatalog exp) throws E;

    public abstract R visit(P param, SchExpMsQuery exp) throws E;

    public abstract R visit(P param, SchExpMsError exp) throws E;

    public abstract R visit(P param, SchExpMsErrorShallow exp) throws E;

    public abstract R visit(P param, SchExpPrefix exp) throws E;

    public abstract R visit(P param, SchExpDom schExpDom) throws E;

    public abstract R visit(P params, SchExpCod exp) throws E;

    public abstract R visit(P param, SchExpRdf exp) throws E;
    
    public abstract R visit(P param, SchExpTinkerpop exp) throws E;

    public abstract R visit(P param, SchExpSrc schExpDom) throws E;

    public abstract R visit(P param, SchExpSpan exp) throws E;

    public abstract R visit(P params, SchExpDst exp) throws E;

    public abstract R visit(P param, SchExpJdbcAll exp) throws E;

    public abstract R visit(P param, SchExpCsv exp) throws E;

    public abstract R visit(P param, SchExpFromMsCatalog exp) throws E;
    
    public abstract R visit(P param, SchExpFront exp) throws E;
    
  }

  public abstract <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E;

  public abstract SchExp resolve(AqlTyping G, Program<Exp<?>> prog);

  public abstract TyExp type(AqlTyping G);

  @Override
  public Kind kind() {
    return Kind.SCHEMA;
  }

  @Override
  public Exp<Schema<String, String, Sym, Fk, Att>> Var(String v) {
    Exp<Schema<String, String, Sym, Fk, Att>> ret = new SchExpVar(v);
    return ret;
  }

    

////////////////////////////////////////////////////////////////////////////////////////////////////

  public static final class SchExpVar extends SchExp {
    @Override
    protected void allowedOptions(Set<AqlOption> set) {
      set.add(AqlOption.import_col_seperator);
    }

    public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
      return v.visit(param, this);
    }

    @Override
    public boolean isVar() {
      return true;
    }

    @Override
    public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
      return v.visitSchExpVar(params, r);
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
    }

    @Override
    public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
      if (!prog.exps.containsKey(var)) {
        throw new RuntimeException("Unbound typeside variable: " + var);
      }
      Exp<?> x = prog.exps.get(var);
      if (!(x instanceof SchExp)) {
        throw new RuntimeException(
            "Variable " + var + " is bound to something that is not a schema, namely\n\n" + x);
      }
      SchExp texp = (SchExp) x;
      return texp.resolve(G, prog);
    }

    public final String var;

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.singleton(new Pair<>(var, Kind.SCHEMA));
    }

    public SchExpVar(String var) {
      this.var = var;
    }

    @Override
    public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
      if (!env.defs.schs.containsKey(var)) {
        throw new RuntimeException("Missing schema: " + var);
      }
      return env.defs.schs.get(var);
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
      SchExpVar other = (SchExpVar) obj;
      return var.equals(other.var);
    }

    @Override
    public String toString() {
      return var;
    }

    @Override
    public TyExp type(AqlTyping G) {
      TyExp e = G.defs.schs.get(var);
      if (e == null) {
        throw new RuntimeException("Not a schema: " + var);
      }
      return e;
    }
  }

////////////////////////////////////////////////////////////////////////////////////////////////////

  public static final class SchExpLit extends SchExp {

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.emptyList();
    }

    public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
      return v.visit(param, this);
    }

    @Override
    public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
      return v.visitSchExpLit(params, r);
    }

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    public final Schema<String, String, Sym, Fk, Att> schema;

    public SchExpLit(Schema<String, String, Sym, Fk, Att> schema) {
      this.schema = schema;
    }

    @Override
    public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
      return schema;
    }

    @Override
    public int hashCode() {
      return schema.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      SchExpLit other = (SchExpLit) obj;
      return schema.equals(other.schema);
    }

    @Override
    public String toString() {
      return ("constant " + schema).trim();
    }

    @Override
    public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
      return this;
    }

    @Override
    public TyExp type(AqlTyping G) {
      return new TyExpLit(schema.typeSide);
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {

    }
  }  


}
