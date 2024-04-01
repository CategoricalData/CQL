package catdata.cql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Program;
import catdata.Triple;
import catdata.Util;
import catdata.cql.DP;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class SchExpPrefix extends SchExp {

  private final String dom;
  private final SchExp sch;

  @Override
  public Collection<Pair<String, Kind>> deps() {
    return sch.deps();
  }

  public <R, P, E extends Exception> R accept(P param, SchExpVisitor<R, P, E> v) throws E {
    return v.visit(param, this);
  }

  @Override
  public <R, P, E extends Exception> SchExp coaccept(P params, SchExpCoVisitor<R, P, E> v, R r) throws E {
    return v.visitSchExpPrefix(params, r);
  }

  @Override
  public Map<String, String> options() {
    return Collections.emptyMap();
  }

  public SchExpPrefix(SchExp t, String d) {
    this.dom = d;
    this.sch = t;
  }

  @Override
  public Schema<String, String, Sym, Fk, Att> eval0(AqlEnv env, boolean isC) {
    Schema<String, String, Sym, Fk, Att> old = sch.eval(env, isC);

    Set<String> ens = new THashSet<>(old.ens.size() * 2);
    Map<Att, Pair<String, String>> atts = new THashMap<>(old.atts.size(), 2);
    Map<Fk, Pair<String, String>> fks = new THashMap<>(old.fks.size(), 2);
    for (String en : old.ens) {
      String en2 = (dom + en);
      ens.add(en2);
      for (Att att : old.attsFrom(en)) {
        atts.put(Att.Att(en2, att.str), new Pair<>(en2, old.atts.get(att).second));
      }
      for (Fk fk : old.fksFrom(en)) {
        fks.put(Fk.Fk(en2, fk.str), new Pair<>(en2, (dom + old.fks.get(fk).second)));
      }
    }

    Collection<Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> eqs = new ArrayList<>(
        old.eqs.size());
    for (Triple<Pair<String, String>, Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>> eq : old.eqs) {
      Term<String, String, Sym, Fk, Att, Void, Void> a = eq.second.map(w -> w, w -> w,
          fk -> Fk.Fk((dom + old.fks.get(fk).first), fk.str),
          att -> Att.Att((dom + old.atts.get(att).first), att.str), x -> Util.abort(x),
          x -> Util.abort(x));
      Term<String, String, Sym, Fk, Att, Void, Void> b = eq.third.map(w -> w, w -> w,
          fk -> Fk.Fk((dom + old.fks.get(fk).first), fk.str),
          att -> Att.Att((dom + old.atts.get(att).first), att.str), x -> Util.abort(x),
          x -> Util.abort(x));
      eqs.add(new Triple<>(new Pair<>(eq.first.first, (dom + eq.first.second)), a, b));
    }

    DP<String, String, Sym, Fk, Att, Void, Void> dp = new DP<String, String, Sym, Fk, Att, Void, Void>() {

      @Override
      public String toStringProver() {
        return "Prefix renaming";
      }

      @Override
      public boolean eq(Map<String, Chc<String, String>> ctx,
          Term<String, String, Sym, Fk, Att, Void, Void> lhs,
          Term<String, String, Sym, Fk, Att, Void, Void> rhs) {
        Map<String, Chc<String, String>> ctx2 = null;

        if (ctx != null) {
          ctx2 = new THashMap<>(ctx.size(), 2);
          for (Entry<String, Chc<String, String>> k : ctx.entrySet()) {
            ctx.put(k.getKey(), k.getValue().left ? Chc.inLeft(k.getValue().l)
                : Chc.inRight((dom + k.getValue().r)));

          }
        }

        Term<String, String, Sym, Fk, Att, Object, Object> a = lhs.map(w -> w, w -> w,
            fk -> Fk.Fk((dom + old.fks.get(fk).first), fk.str),
            att -> Att.Att((dom + old.atts.get(att).first), att.str), x -> Util.abort(x),
            x -> Util.abort(x));
        Term<String, String, Sym, Fk, Att, Object, Object> b = rhs.map(w -> w, w -> w,
            fk -> Fk.Fk((dom + old.fks.get(fk).first), fk.str),
            att -> Att.Att((dom + old.atts.get(att).first), att.str), x -> Util.abort(x),
            x -> Util.abort(x));

        return old.dp().eq(ctx2, a, b);
      }

    };
    return new Schema<>(old.typeSide, ens, atts, fks, eqs, dp, false);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dom == null) ? 0 : dom.hashCode());
    result = prime * result + ((sch == null) ? 0 : sch.hashCode());
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
    SchExpPrefix other = (SchExpPrefix) obj;
    if (dom == null) {
      if (other.dom != null)
        return false;
    } else if (!dom.equals(other.dom))
      return false;
    if (sch == null) {
      if (other.sch != null)
        return false;
    } else if (!dom.equals(other.dom))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "prefix " + sch + " " + dom;
  }

  @Override
  public SchExp resolve(AqlTyping G, Program<Exp<?>> prog) {
    return this;
  }

  @Override
  public TyExp type(AqlTyping G) {
    return sch.type(G);
  }

  @Override
  protected void allowedOptions(Set<AqlOption> set) {
  }

  @Override
  public void mapSubExps(Consumer<Exp<?>> f) {
    sch.mapSubExps(f);
  }
}