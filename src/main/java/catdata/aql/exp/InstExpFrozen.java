package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Frozen;
import catdata.aql.It.ID;
import catdata.aql.Kind;


public final class InstExpFrozen extends InstExp<String, String, ID, Chc<String, Pair<ID, Att>>> {

  public final QueryExp Q;
  public final String I;

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    Q.map(f);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public <R, P, E extends Exception> R accept(P param, InstExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  public InstExpFrozen(QueryExp q, String i) {
    Q = q;
    I = i;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((I == null) ? 0 : I.hashCode());
    result = prime * result + ((Q == null) ? 0 : Q.hashCode());
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
    InstExpFrozen other = (InstExpFrozen) obj;
    if (I == null) {
      if (other.I != null)
        return false;
    } else if (!I.equals(other.I))
      return false;
    if (Q == null) {
      if (other.Q != null)
        return false;
    } else if (!Q.equals(other.Q))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "frozen " + Q + " " + I;
  }

  @Override
  public SchExp type(AqlTyping G) {
    return Q.type(G).first;
  }

  @Override
  public synchronized Frozen<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    if (Q.eval(env, isC).ens.containsKey((I))) {
      return Q.eval(env, isC).ens.get((I));
    }
    return Q.eval(env, isC).tys.get((I));
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Q.deps();
  }

  @Override
  public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
    return Collections.emptySet();
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }
}