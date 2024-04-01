package catdata.cql.exp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Constraints;
import catdata.cql.ED;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import gnu.trove.set.hash.THashSet;

public final class EdsExpInclude extends EdsExp {

    private final SchExp parent;
    private final String old;
    private final String nw;
    private final Map<String, String> ops;

    public EdsExpInclude(SchExp p, String o, String n, List<Pair<String, String>> x) {
      this.parent = p;
      this.old = o;
      this.nw = n;
      this.ops = Util.toMapSafely(x);
    }

    @Override
    public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
      return v.visit(params, this);
    }

    @Override
    public Map<String, String> options() {
      return ops;
    }

    @Override
    public boolean isVar() {
      return false;
    }

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return parent.deps();
    }

    @Override
    public synchronized Constraints eval0(AqlEnv env, boolean isC) {
      Schema<String, String, Sym, Fk, Att> base = parent.eval(env, isC);
      Schema<String, String, Sym, Fk, Att> schT = type(env.typing).eval(env, isC);
      return makeEds(base, schT, new AqlOptions(ops, env.defaults));
    }

    public Constraints makeEds(Schema<String, String, Sym, Fk, Att> base, Schema<String, String, Sym, Fk, Att> schT,
        AqlOptions op) {
      LinkedList<ED> ret = new LinkedList<>();

      String o = ("old");
      String n = ("new");

      for (String en : base.ens) {
        Map<String, Chc<String, String>> as = Collections.singletonMap(o, Chc.inRight((old + en)));
        Map<String, Chc<String, String>> es = Collections.singletonMap(n, Chc.inRight((nw + en)));
        Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> ewh = new THashSet<>(
            2 * (base.attsFrom(en).size() + base.fksFrom(en).size()));
        for (Fk fk : base.fksFrom(en)) {
          ewh.add(new Pair<>(Term.Fk(Fk.Fk((old + en), fk.str), Term.Var(o)),
              Term.Fk(Fk.Fk((nw + en), fk.str), Term.Var(n))));
        }
        for (Att att : base.attsFrom(en)) {
          ewh.add(new Pair<>(Term.Att(Att.Att((old + en), att.str), Term.Var(o)),
              Term.Att(Att.Att((nw + en),  att.str), Term.Var(n))));
        }

        ret.add(new ED(as, es, Collections.emptySet(), ewh, false, op));
      }

      return new Constraints(schT, ret, op);
    }

    @Override
    public String toString() {
      return "include " + parent + " " + old + " " + nw;
    }

    

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((nw == null) ? 0 : nw.hashCode());
      result = prime * result + ((old == null) ? 0 : old.hashCode());
      result = prime * result + ((ops == null) ? 0 : ops.hashCode());
      result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
      EdsExpInclude other = (EdsExpInclude) obj;
      if (nw == null) {
        if (other.nw != null)
          return false;
      } else if (!nw.equals(other.nw))
        return false;
      if (old == null) {
        if (other.old != null)
          return false;
      } else if (!old.equals(other.old))
        return false;
      if (ops == null) {
        if (other.ops != null)
          return false;
      } else if (!ops.equals(other.ops))
        return false;
      if (parent == null) {
        if (other.parent != null)
          return false;
      } else if (!parent.equals(other.parent))
        return false;
      return true;
    }

    @Override
    public SchExp type(AqlTyping G) {
      TyExp t = parent.type(G);
      SchExp o = new SchExpPrefix(parent, old);
      SchExp n = new SchExpPrefix(parent, nw);
      List<SchExp> l = new ArrayList<>(2);
      l.add(o);
      l.add(n);
      return new SchExpRaw(t, l, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
          Collections.emptyList(), Collections.emptyList(), Util.toList(ops));
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {
      set.addAll(AqlOptions.proverOptionNames());
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
      f.accept(parent);
      parent.mapSubExps(f);
    }
  }