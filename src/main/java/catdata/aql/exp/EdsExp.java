package catdata.aql.exp;

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
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Constraints;
import catdata.aql.ED;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.SqlTypeSide;
import catdata.aql.SqlTypeSide2;
import catdata.aql.Term;
import catdata.aql.TypeSide;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public abstract class EdsExp extends Exp<Constraints> {

  @Override
  public Kind kind() {
    return Kind.CONSTRAINTS;
  }

  public abstract SchExp type(AqlTyping G);

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public Exp<Constraints> Var(String v) {
    Exp ret = new EdsExpVar(v);
    return ret;
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  public static interface EdsExpCoVisitor<R, P, E extends Exception> {
    public EdsExpVar visitEdsExpVar(P params, R exp) throws E;

    public EdsExpRaw visitEdsExpRaw(P params, R exp) throws E;

    public EdsExpSch visitEdsExpSch(P params, R exp) throws E;

    public EdsExpSqlNull visitEdsExpSqlNull(P params, R exp) throws E;

    public EdsExpInclude visitEdsExpInclude(P params, R exp) throws E;
    
    public EdsExpTinkerpop visitEdsExpTinkerpop(P params, R exp) throws E;
    
    public EdsExpFromMsCatalog visitEdsExpFromMsCatalog(P params, R exp) throws E;
  }

  public static interface EdsExpVisitor<R, P, E extends Exception> {
    public R visit(P params, EdsExpVar exp) throws E;

    public R visit(P params, EdsExpRaw exp) throws E;

    public R visit(P params, EdsExpSch exp) throws E;

    public R visit(P params, EdsExpSqlNull exp) throws E;

    public R visit(P params, EdsExpInclude exp) throws E;
    
    public R visit(P params, EdsExpTinkerpop exp) throws E;
    
    public R visit(P params, EdsExpFromMsCatalog exp) throws E;
  }

  public abstract <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E;

  /////////////////////////////////////////////////////////////////////////////////////////

  public static final class EdsExpVar extends EdsExp {

    @Override
    public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
      return v.visit(params, this);
    }

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    @Override
    public boolean isVar() {
      return true;
    }

    public final String var;

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return Collections.singleton(new Pair<>(var, Kind.CONSTRAINTS));
    }

    public EdsExpVar(String var) {
      this.var = var;
    }

    @Override
    public synchronized Constraints eval0(AqlEnv env, boolean isC) {
      return env.defs.eds.get(var);
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
      EdsExpVar other = (EdsExpVar) obj;
      return var.equals(other.var);
    }

    @Override
    public String toString() {
      return var;
    }

    @Override
    public SchExp type(AqlTyping G) {
      if (!G.defs.eds.containsKey(var)) {
        throw new RuntimeException("Not constraints: " + var);
      }
      return G.defs.eds.get(var);
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
    }
  }

  public static final class EdsExpSch extends EdsExp {

    @Override
    public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
      return v.visit(params, this);
    }

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
    }

    @Override
    public boolean isVar() {
      return false;
    }

    public final SchExp sch;

    @Override
    public Collection<Pair<String, Kind>> deps() {
      return sch.deps();
    }

    public EdsExpSch(SchExp sch) {
      this.sch = sch;
    }

    @Override
    public synchronized Constraints eval0(AqlEnv env, boolean isC) {
      Schema<String, String, Sym, Fk, Att> ret = sch.eval(env, isC);
      return new Constraints(env.defaults, ret);
    }

    @Override
    public int hashCode() {
      return sch.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      EdsExpSch other = (EdsExpSch) obj;
      return sch.equals(other.sch);
    }

    @Override
    public String toString() {
      return "fromSchema " + sch;
    }

    @Override
    public SchExp type(AqlTyping G) {
      sch.type(G);
      return sch;
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
      sch.map(f);
    }
  }

  ///////

  public static final class EdsExpSqlNull extends EdsExp {

    private final TyExp parent;

    public EdsExpSqlNull(TyExp parent) {
      this.parent = parent;
    }

    @Override
    public <R, P, E extends Exception> R accept(P params, EdsExpVisitor<R, P, E> v) throws E {
      return v.visit(params, this);
    }

    @Override
    public Map<String, String> options() {
      return Collections.emptyMap();
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
      TypeSide<String, Sym> ts = parent.eval(env, isC);
      AqlOptions op = new AqlOptions(options(), env.defaults);

      return makeEds(Schema.terminal(SqlTypeSide2.FOR_TY.make(ts, op)), op);
    }

    public static Constraints makeEds(Schema<String, String, Sym, Fk, Att> schT, AqlOptions op) {
      LinkedList<ED> ret = new LinkedList<>();
      String x = ("x");
      String y = ("y");
      String z = ("z");
      Term<String, String, Sym, Fk, Att, Void, Void> t = Term.Sym(SqlTypeSide.t, Collections.emptyList());
      Term<String, String, Sym, Fk, Att, Void, Void> f = Term.Sym(SqlTypeSide.f, Collections.emptyList());

      // Term<Ty, En, Sym, Fk, Att, Void, Void> u = Term.Sym(Sym.Sym("null_Boolean"),
      // Collections.emptyList());

      List<Term<String, String, Sym, Fk, Att, Void, Void>> lxx = new ArrayList<>(2);
      lxx.add(Term.Var(x));
      lxx.add(Term.Var(x));

      List<Term<String, String, Sym, Fk, Att, Void, Void>> lxy = new ArrayList<>(2);
      lxy.add(Term.Var(x));
      lxy.add(Term.Var(y));

      List<Term<String, String, Sym, Fk, Att, Void, Void>> lyx = new ArrayList<>(2);
      lyx.add(Term.Var(y));
      lyx.add(Term.Var(x));

      List<Term<String, String, Sym, Fk, Att, Void, Void>> lyz = new ArrayList<>(2);
      lyz.add(Term.Var(y));
      lyz.add(Term.Var(z));

      List<Term<String, String, Sym, Fk, Att, Void, Void>> lxz = new ArrayList<>(2);
      lxz.add(Term.Var(x));
      lxz.add(Term.Var(z));

      for (String ty : schT.typeSide.tys) {
        if (ty.equals("Bool")) {
          continue;
        }
        var ll = new ArrayList<String>(2);
        ll.add(ty);
        ll.add(ty);
        Pair<List<String>, String> p = new Pair<>(ll, "Boolean");
        var sss = Sym.Sym("eq_" + ty, p);
        Term<String, String, Sym, Fk, Att, Void, Void> xx = Term.Sym(sss, lxx);
        Term<String, String, Sym, Fk, Att, Void, Void> xx0 = Term.Sym(Sym.Sym("isFalse", SqlTypeSide.boolSort1), Util.list(xx));
        // Term<Ty, En, Sym, Fk, Att, Void, Void> xx1 = Term.Sym(Sym.Sym("not"),
        // Util.list(xx0));
        ret.add(new ED(Collections.singletonMap(x, Chc.inLeft(ty)), Collections.emptyMap(),
            Collections.emptySet(), Collections.singleton(new Pair<>(xx0, f)), false, op));

        Term<String, String, Sym, Fk, Att, Void, Void> xy = Term.Sym(sss, lxy);
        Term<String, String, Sym, Fk, Att, Void, Void> yx = Term.Sym(sss, lyx);
        Map<String, Chc<String, String>> m2 = new THashMap<>(2, 2);
        m2.put(x, Chc.inLeft(ty));
        m2.put(y, Chc.inLeft(ty));
        ret.add(new ED(m2, Collections.emptyMap(), Collections.emptySet(),
            Collections.singleton(new Pair<>(xy, yx)), false, op));

        Map<String, Chc<String, String>> m3 = new THashMap<>(2, 2);
        m3.put(x, Chc.inLeft(ty));
        m3.put(y, Chc.inLeft(ty));
        m3.put(z, Chc.inLeft(ty));

        Term<String, String, Sym, Fk, Att, Void, Void> xy0 = Term.Sym(sss,
            Util.list(Term.Var(x), Term.Var(y)));

        ret.add(new ED(m2, Collections.emptyMap(), Collections.singleton(new Pair<>(xy0, t)),
            Collections.singleton(new Pair<>(Term.Var(x), Term.Var(y))), false, op));
        // other congruences
        // todo: eq(a,b)=false -> x<>y?

      }

      return new Constraints(schT, ret, op);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
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
      EdsExpSqlNull other = (EdsExpSqlNull) obj;
      if (parent == null) {
        if (other.parent != null)
          return false;
      } else if (!parent.equals(other.parent))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "sqlNull " + parent;
    }

    @Override
    public SchExp type(AqlTyping G) {
      return new SchExpEmpty(new TyExpSqlNull(parent));
    }

    @Override
    protected void allowedOptions(Set<AqlOption> set) {
    }

    @Override
    public void mapSubExps(Consumer<Exp<?>> f) {
      f.accept(parent);
      parent.mapSubExps(f);
    }
  }

  ///////////////////////////////////

  public static final class EdsExpInclude extends EdsExp {

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

}
