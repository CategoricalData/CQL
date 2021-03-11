package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Util;
import catdata.aql.ColimitSchema;
import catdata.aql.Kind;
import catdata.aql.AqlOptions.AqlOption;
import gnu.trove.set.hash.THashSet;

public class ColimSchExpWrap extends ColimSchExp {

  public <R, P, E extends Exception> R accept(P param, ColimSchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public Set<Pair<SchExp, SchExp>> gotos(ColimSchExp ths) {
    Set<Pair<SchExp, SchExp>> ret = new THashSet<>();
    SchExp t = new SchExpColim(ths);
    SchExp s = new SchExpColim(colim);
    ret.add(new Pair<>(s, t));
    return ret;
  }

  public final ColimSchExp colim;

  public final MapExp toUser;

  public final MapExp fromUser;

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public SchExp getNode(String n, AqlTyping G) {
    return colim.getNode(n, G);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((colim == null) ? 0 : colim.hashCode());
    result = prime * result + ((fromUser == null) ? 0 : fromUser.hashCode());
    result = prime * result + ((toUser == null) ? 0 : toUser.hashCode());
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
    ColimSchExpWrap other = (ColimSchExpWrap) obj;
    if (colim == null) {
      if (other.colim != null)
        return false;
    } else if (!colim.equals(other.colim))
      return false;
    if (fromUser == null) {
      if (other.fromUser != null)
        return false;
    } else if (!fromUser.equals(other.fromUser))
      return false;
    if (toUser == null) {
      if (other.toUser != null)
        return false;
    } else if (!toUser.equals(other.toUser))
      return false;
    return true;
  }

  public ColimSchExpWrap(ColimSchExp colim, MapExp toUser, MapExp fromUser) {
    this.colim = colim;
    this.toUser = toUser;
    this.fromUser = fromUser;
  }

  @Override
  public Set<String> type(AqlTyping G) {
    return colim.type(G);
  }

  @Override
  public TyExp typeOf(AqlTyping G) {
    return colim.typeOf(G);
  }

  @Override
  public synchronized ColimitSchema<String> eval0(AqlEnv env, boolean isC) {
    return colim.eval(env, isC).wrap(toUser.eval(env, isC), fromUser.eval(env, isC));
  }

  @Override
  public String toString() {
    return "wrap " + colim + " " + toUser + " " + fromUser;
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(colim.deps(), Util.union(toUser.deps(), fromUser.deps()));
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    colim.map(f);
    toUser.map(f);
    fromUser.map(f);
  }

}