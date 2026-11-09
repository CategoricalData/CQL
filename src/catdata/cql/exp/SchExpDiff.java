package catdata.cql.exp;

import catdata.Pair;
import catdata.Program;
import catdata.Util;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.Kind;
import catdata.cql.Schema;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class SchExpDiff extends SchExp {

  private final SchExp S, T;

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return Util.union(S.deps(), T.deps());
  }

  public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r)
      throws E {
    return v.visitSchExpPrefix(params, r);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public SchExpDiff(SchExp s, SchExp t) {
    this.S = s;
    this.T = t;
  }

  @Override
  public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    var s = S.eval(env, isC);
    var t = T.eval(env, isC);
    if (!s.eqs.isEmpty() || !t.eqs.isEmpty()) {
      throw new RuntimeException("Equations not support with schema except");
    }
    Collection<String> ens = new HashSet<>(s.ens);
    ens.removeAll(t.ens);
    Map<Att, Pair<String, String>> atts = new HashMap<>(s.atts);
    for (Att att : t.atts.keySet()) {
      atts.remove(att);
    }
    Map<Fk, Pair<String, String>> fks = new HashMap<>(s.fks);
    for (Fk fk : t.fks.keySet()) {
      fks.remove(fk);
    }
    return new Schema<String, String, Sym, Fk, Att>(
        s.typeSide, ens, atts, fks, Collections.emptySet(), s.dp, true);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((S == null) ? 0 : S.hashCode());
    result = prime * result + ((T == null) ? 0 : T.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SchExpDiff other = (SchExpDiff) obj;
    if (S == null) {
      if (other.S != null) return false;
    } else if (!S.equals(other.S)) return false;
    if (T == null) {
      if (other.T != null) return false;
    } else if (!T.equals(other.T)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "except " + S + " " + T;
  }

  @Override
  public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
    return this;
  }

  @Override
  public TyExp type(AqlTyping G) {
    TyExp x = S.type(G);
    TyExp y = T.type(G);
    if (!x.equals(y)) {
      throw new RuntimeException("Unequal type sides: " + this);
    }
    return x;
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {}

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    S.mapSubExps(f);
    T.mapSubExps(f);
  }
}
