package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Program;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.DP;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.Term;
import gnu.trove.map.hash.THashMap;

public class SchExpFront extends SchExp {

  public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  EdsExp eds;
  int i;

  public SchExpFront(EdsExp eds, String i) {
    this.eds = eds;
    this.i = Integer.parseInt(i);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((eds == null) ? 0 : eds.hashCode());
    result = prime * result + i;
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
    SchExpFront other = (SchExpFront) obj;
    if (eds == null) {
      if (other.eds != null)
        return false;
    } else if (!eds.equals(other.eds))
      return false;
    if (i != other.i)
      return false;
    return true;
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    eds.map(f);
  }

  @Override
  public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
    return this;
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  @Override
  public String toString() {
    return "front " + eds + " " + i;
  }

  @Override
  public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    var eds0 = eds.eval(env, isC);
    if (!eds0.schema.fks.isEmpty()) {
      throw new RuntimeException("Front/Back cannot be used with foreign keys.");
    }

    if (i < 0 || i >= eds0.eds.size()) {
      throw new RuntimeException("Expected a number between zero and " + (eds0.eds.size() - 1));
    }
    var ed = eds0.eds.get(i);

    Collection<String> ens = Collections.singleton("Front");
    Map<Att, Pair<String, String>> atts = new THashMap<>();

    for (Entry<String, Chc<String, String>> x : ed.As.entrySet()) {
      String v = x.getKey();
      if (x.getValue().left) {
        continue;
      }
      for (Att att : eds0.schema.attsFrom(x.getValue().r)) {
        String s = v + "_" + att.str;
        atts.put(Att.Att("Front", s), new Pair<>("Front", eds0.schema.atts.get(att).second));
      }
    }

    DP<String, String, Sym, Fk, Att, Void, Void> dp = new DP<>() {

      @Override
      public String toStringProver() {
        return "SchExpFront";
      }

      @Override
      public boolean eq(Map<String, Chc<String, String>> ctx, Term<String, String, Sym, Fk, Att, Void, Void> lhs,
          Term<String, String, Sym, Fk, Att, Void, Void> rhs) {
        return lhs.equals(rhs);
      }

    };
    return new Schema<String, String, Sym, Fk, Att>(eds0.schema.typeSide, ens, atts, Collections.emptyMap(),
        Collections.emptyList(), dp, false);
  }

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return eds.deps();
  }

  @Override
  public TyExp type(AqlTyping G) {
    return eds.type(G).type(G);
  }

  @Override
  public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitSchExpFront(params, r);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {

  }

}