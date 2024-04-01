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
import catdata.cql.SqlTypeSide;
import catdata.cql.SqlTypeSide2;
import catdata.cql.Term;
import catdata.cql.TypeSide;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.exp.EdsExp.EdsExpVisitor;
import gnu.trove.map.hash.THashMap;

public final class EdsExpSqlNull extends EdsExp {

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