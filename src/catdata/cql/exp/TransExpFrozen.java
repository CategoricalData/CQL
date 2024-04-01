package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Quad;
import catdata.cql.AqlJs;
import catdata.cql.Collage;
import catdata.cql.Kind;
import catdata.cql.Query;
import catdata.cql.Term;
import catdata.cql.Transform;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Collage.CCollage;
import catdata.cql.It.ID;
import gnu.trove.map.hash.THashMap;

public class TransExpFrozen
    extends TransExp<String, String, String, String, ID, Chc<String, Pair<ID, Att>>, ID, Chc<String, Pair<ID, Att>>> {

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((Q == null) ? 0 : Q.hashCode());
    result = prime * result + ((dst == null) ? 0 : dst.hashCode());
    result = prime * result + ((src == null) ? 0 : src.hashCode());
    result = prime * result + ((term == null) ? 0 : term.hashCode());
    result = prime * result + ((var == null) ? 0 : var.hashCode());
    return result;
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    Q.map(f);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TransExpFrozen other = (TransExpFrozen) obj;
    if (Q == null) {
      if (other.Q != null)
        return false;
    } else if (!Q.equals(other.Q))
      return false;
    if (dst == null) {
      if (other.dst != null)
        return false;
    } else if (!dst.equals(other.dst))
      return false;
    if (src == null) {
      if (other.src != null)
        return false;
    } else if (!src.equals(other.src))
      return false;
    if (term == null) {
      if (other.term != null)
        return false;
    } else if (!term.equals(other.term))
      return false;
    if (var == null) {
      if (other.var != null)
        return false;
    } else if (!var.equals(other.var))
      return false;
    return true;
  }

  public final QueryExp Q;
  public final String src, dst, var;
  public final RawTerm term;

  @Override
  public Pair<InstExp<String, String, ID, Chc<String, Pair<ID, Att>>>, InstExp<String, String, ID, Chc<String, Pair<ID, Att>>>> type(
      AqlTyping G) {
    return new Pair<>(new InstExpFrozen(Q, dst), new InstExpFrozen(Q, src));
  }

  @Override
  public <R, P, E extends Exception> R accept(P params, TransExpVisitor<R, P, E> v) {
    return v.visit(params, this);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }

  @Override
  protected Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public Transform<String, String, Sym, Fk, Att, String, String, String, String, ID, Chc<String, Pair<ID, Att>>, ID, Chc<String, Pair<ID, Att>>> eval0(
      AqlEnv env, boolean isC) {
    Query<String, String, Sym, Fk, Att, String, Fk, Att> q = Q.eval(env, isC);
    if (isC) {
      throw new IgnoreException();
    }

    Map<String, Chc<String, String>> Map = new THashMap<>();
    Map.put(var, Chc.inRight((src)));
    Chc<String, String> expected;
    if (!q.tys.keySet().contains((dst))) {
      expected = Chc.inRight((dst));
    } else {
      expected = Chc.inLeft((dst));
    }

    Collage<String, String, Sym, Fk, Att, String, String> col = new CCollage<>(q.dst.collage());
    AqlJs<String, Sym> js = q.src.typeSide.js;

    Quad<Map<String, Chc<String, String>>, Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>, Chc<String, String>> result = RawTerm
        .infer1x(Map, term, term, expected, col, "", js);

    if (expected.left) {
      return q.composeT(result.second.convert(), (src));
    }
    return q.compose(q.transP(result.second.convert()), (src));

  }

  public TransExpFrozen(QueryExp q, String var, String src, RawTerm term, String dst) {
    Q = q;
    this.src = src;
    this.dst = dst;
    this.var = var;
    this.term = term;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Q.deps();
  }

  // frozen qTS lambda x:s0. x.ss.att : Integer
  @Override
  public String toString() {
    return "frozen " + Q + "lambda " + var + ":" + src + ". " + term + " : " + dst;
  }

}
