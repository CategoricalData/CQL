package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.SchExp.SchExpLit;

public abstract class InstExp<Gen, Sk, X, Y> extends Exp<Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y>> {

  public abstract <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E;

  public static interface InstExpCoVisitor<R, P, E extends Exception> {
    public abstract InstExpXmlAll visitInstExpXmlAll(P param, R exp) throws E;

    public abstract InstExpXmlAll visitInstExpMarkdown(P param, R exp) throws E;

    public abstract InstExpSpanify visitInstExpSpanify(P param, R exp) throws E;

    public abstract InstExpMsError visitInstExpMsError(P param, R exp) throws E;

    public abstract InstExpRdfAll visitInstExpRdfAll(P param, R exp) throws E;

    public abstract InstExpJsonAll visitInstExpJsonAll(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpSigma<Gen, Sk, X, Y> visitInstExpSigma(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpSigmaChase<Gen, Sk, X, Y> visitInstExpSigmaChase(P param, R exp)
        throws E;

    public abstract InstExpVar visitInstExpVar(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpAnonymize<Gen, Sk, X, Y> visitInstExpAnonymize(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpChase<Gen, Sk, X, Y> visitInstExpChase(P param, R exp) throws E;

    public abstract <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> InstExpCod<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitInstExpCod(
        P param, R exp) throws E;

    public abstract <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> InstExpCoEq<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitInstExpCoEq(
        P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpCoEval<Gen, Sk, X, Y> visitInstExpCoEval(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpColim<Gen, Sk, X, Y> visitInstExpColim(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpCoProdFull<Gen, Sk, X, Y> visitInstExpCoProdFull(P param, R exp)
        throws E;

    public abstract <Gen, Sk, X, Y, Gen1, Sk1, X1> InstExpDiff<Gen, Sk, X, Y, Gen1, Sk1, X1> visitInstExpDiff(
        P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpDistinct<Gen, Sk, X, Y> visitInstExpDistinct(P param, R exp) throws E;

    public abstract <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> InstExpDom<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> visitInstExpDom(
        P param, R exp) throws E;

    public abstract InstExpEmpty visitInstExpEmpty(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpEval<Gen, Sk, X, Y> visitInstExpEval(P param, R exp) throws E;

    public abstract InstExpFrozen visitInstExpFrozen(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpLit<Gen, Sk, X, Y> visitInstExpLit(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpPivot<Gen, Sk, X, Y> visitInstExpPivot(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpPi<Gen, Sk, X, Y> visitInstExpPi(P param, R exp) throws E;

    public abstract InstExpCsv visitInstExpCsv(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpCascadeDelete<Gen, Sk, X, Y> visitInstExpCascadeDelete(P param, R exp)
        throws E;

    public abstract <Gen, Sk, X, Y> InstExpDelta<Gen, Sk, X, Y> visitInstExpDelta(P param, R exp) throws E;

    public abstract InstExpJdbc visitInstExpJdbc(P param, R exp) throws E;

    public abstract <Gen, Sk, X, Y> InstExpQueryQuotient<Gen, Sk, X, Y> visitInstExpQueryQuotient(P param, R exp)
        throws E;

    public abstract InstExpRandom visitInstExpRandom(P param, R exp) throws E;

    public abstract InstExpRaw visitInstExpRaw(P param, R exp) throws E;

    public abstract InstExpJdbcDirect visitInstExpJdbcDirect(P param, R exp) throws E;
    
    public abstract InstExpTinkerpop visitInstExpTinkerpop(P param, R exp) throws E;
  }

  public static interface InstExpVisitor<R, P, E extends Exception> {

    public abstract R visit(P param, InstExpJdbcDirect exp) throws E;

   // public abstract R visit(P param, InstExpMarkdown exp) throws E;

    public abstract R visit(P param, InstExpRdfAll exp) throws E;

    public abstract R visit(P param, InstExpSpanify exp) throws E;

    public abstract R visit(P param, InstExpMsError exp) throws E;

    public abstract R visit(P param, InstExpJsonAll exp) throws E;

    public abstract R visit(P param, InstExpXmlAll exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpSigma<Gen, Sk, X, Y> exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpSigmaChase<Gen, Sk, X, Y> exp) throws E;

    public abstract R visit(P param, InstExpVar exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpAnonymize<Gen, Sk, X, Y> exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpChase<Gen, Sk, X, Y> exp) throws E;

    public abstract <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> R visit(P param,
        InstExpCod<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) throws E;

    public abstract <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> R visit(P param,
        InstExpCoEq<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpCoEval<Gen, Sk, X, Y> exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpColim<Gen, Sk, X, Y> exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpCoProdFull<Gen, Sk, X, Y> exp) throws E;

    public abstract <Gen, Sk, X, Y, Gen1, Sk1, X1> R visit(P param, InstExpDiff<Gen, Sk, X, Y, Gen1, Sk1, X1> exp)
        throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpDistinct<Gen, Sk, X, Y> exp) throws E;

    public abstract <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> R visit(P param,
        InstExpDom<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) throws E;

    public abstract R visit(P param, InstExpEmpty exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpEval<Gen, Sk, X, Y> exp) throws E;

    public abstract R visit(P param, InstExpFrozen exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpLit<Gen, Sk, X, Y> exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpPivot<Gen, Sk, X, Y> exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpPi<Gen, Sk, X, Y> exp) throws E;

    public abstract R visit(P param, InstExpCsv exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpDelta<Gen, Sk, X, Y> exp) throws E;

    public abstract R visit(P param, InstExpJdbc exp) throws E;
    
    public abstract R visit(P param, InstExpTinkerpop exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpQueryQuotient<Gen, Sk, X, Y> exp) throws E;

    public abstract R visit(P param, InstExpRandom exp) throws E;

    public abstract R visit(P param, InstExpRaw exp) throws E;

    public abstract <Gen, Sk, X, Y> R visit(P param, InstExpCascadeDelete<Gen, Sk, X, Y> exp) throws E;

	public abstract <Gen, Sk, X, Y> R visit(P param, InstExpSkolem<Gen, Sk, X, Y> exp) throws E;

	//public abstract R visit(P param, InstExpExcel instExpExcel);
  }

  @Override
  public Kind kind() {
    return Kind.INSTANCE;
  }

  public abstract SchExp type(AqlTyping G);

  public abstract Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G);

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public Exp<Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y>> Var(String v) {
    Exp ret = new InstExpVar(v);
    return ret;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  public static final class InstExpVar extends InstExp<Object, Object, Object, Object> {
    public final String var;

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    @Override
    public boolean isVar() {
      return true;
    }

    public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
      return v.visit(param, this);
    }

    @Override
    public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
      return Collections.emptySet();
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.singleton(new Pair<>(var, Kind.INSTANCE));
    }

    public InstExpVar(String var) {
      this.var = var;
    }

    @Override
    public synchronized Instance<String, String, Sym, Fk, Att, Object, Object, Object, Object> eval0(AqlEnv env,
        boolean isC) {
      return env.defs.insts.get(var);
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
      InstExpVar other = (InstExpVar) obj;
      return var.equals(other.var);
    }

    @Override
    public String toString() {
      return var;
    }

    @Override
    public synchronized SchExp type(AqlTyping G) {
      if (!G.defs.insts.containsKey(var)) {
        throw new RuntimeException("Not an instance: " + var);
      }
      return G.defs.insts.get(var);
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {

    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {

    }

  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  public static final class InstExpLit<Gen, Sk, X, Y> extends InstExp<Gen, Sk, X, Y> {

    public final Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> inst;

    @Override
    public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
      return Collections.emptySet();
    }

    public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
      return v.visit(param, this);
    }

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.emptyList();
    }

    public InstExpLit(Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> inst) {
      this.inst = inst;
    }

    @Override
    public synchronized Instance<String, String, Sym, Fk, Att, Gen, Sk, X, Y> eval0(AqlEnv env, boolean isC) {
      return inst;
    }

    @Override
    public int hashCode() {
      return inst.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      InstExpLit<?, ?, ?, ?> other = (InstExpLit<?, ?, ?, ?>) obj;
      return inst.equals(other.inst);
    }

    @Override
    public String toString() {
      return "InstExpLit " + inst;
    }

    @Override
    public SchExp type(AqlTyping G) {
      return new SchExpLit(inst.schema());
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {

    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {

    }

  }

}
