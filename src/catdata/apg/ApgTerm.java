package catdata.apg;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import catdata.Pair;
import catdata.Util;

import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.strategy.HashingStrategy;

public class ApgTerm<L, E> {

  public final E e;
  public final Object value;
  public final String prim;

  public final Map<String, ApgTerm<L, E>> fields;

  public final ApgTerm<L, E> a;
  public final String inj;

  public final String var;
  public final String proj;
  public final Map<String, Pair<String, ApgTerm<L, E>>> cases;

  public final L deref;

  public final ApgTy<L> cases_t;

  public final List<ApgTerm<L, E>> args;
  public final String head;

  private ApgTerm(E e, Object v, Map<String, ApgTerm<L, E>> m, String f, ApgTerm<L, E> a, String var, String proj,
      Map<String, Pair<String, ApgTerm<L, E>>> c, L deref, String prim, ApgTy<L> cases_t, List<ApgTerm<L, E>> args,
      String head) {
    this.e = e;
    this.value = v;
    this.fields = m;
    this.inj = f;
    this.a = a;
    this.var = var;
    this.proj = proj;
    this.cases = c;
    this.deref = deref;
    this.prim = prim;
    this.cases_t = cases_t;
    this.args = args;
    this.head = head;
  }

  private static synchronized <L, E> ApgTerm<L, E> mkApgTerm(E e, Object v, Map<String, ApgTerm<L, E>> m, String f,
      ApgTerm<L, E> a, String var, String proj, Map<String, Pair<String, ApgTerm<L, E>>> c, L d, String prim,
      ApgTy<L> cases_t, List<ApgTerm<L, E>> args, String head) {
    ApgTerm<L, E> ret = new ApgTerm<>(e, v, m, f, a, var, proj, c, d, prim, cases_t, args, head);

    ApgTerm<L, E> ret2 = cache.get(ret);
    if (ret2 != null) {
      return ret2;
    }
    cache.put(ret, ret);
    return ret;
  }

  @SuppressWarnings("rawtypes")
  private static HashingStrategy<ApgTerm> strategy = new HashingStrategy<>() {
    private static final long serialVersionUID = 1L;

    @Override
    public int computeHashCode(ApgTerm t) {
      return t.hashCode2();
    }

    @Override
    public boolean equals(ApgTerm s, ApgTerm t) {
      return s.equals2(t);
    }
  };

  @SuppressWarnings("rawtypes")
  private static Map<ApgTerm, ApgTerm> cache = new TCustomHashMap<>(strategy);

  public static synchronized <L, E> ApgTerm<L, E> ApgTermE(E str) {
    return mkApgTerm(str, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  public static synchronized <L, E> ApgTerm<L, E> ApgTermV(Object str, String p) {
    return mkApgTerm(null, str, null, null, null, null, null, null, null, p, null, null, null);
  }

  public static synchronized <L, E> ApgTerm<L, E> ApgTermTuple(Map<String, ApgTerm<L, E>> str) {
    return mkApgTerm(null, null, str, null, null, null, null, null, null, null, null, null, null);
  }

  public static synchronized <L, E> ApgTerm<L, E> ApgTermInj(String f, ApgTerm<L, E> str, ApgTy<L> cases_t) {
    return mkApgTerm(null, null, null, f, str, null, null, null, null, null, cases_t, null, null);
  }

  public static synchronized <L, E> ApgTerm<L, E> ApgTermProj(String f, ApgTerm<L, E> str) {
    return mkApgTerm(null, null, null, null, str, null, f, null, null, null, null, null, null);
  }

  public static synchronized <L, E> ApgTerm<L, E> ApgTermVar(String v) {
    return mkApgTerm(null, null, null, null, null, v, null, null, null, null, null, null, null);
  }

  public static synchronized <L, E> ApgTerm<L, E> ApgTermCase(ApgTerm<L, E> arg,
      Map<String, Pair<String, ApgTerm<L, E>>> str, ApgTy<L> cases_t) {
    return mkApgTerm(null, null, null, null, arg, null, null, str, null, null, cases_t, null, null);
  }

  public static synchronized <L, E> ApgTerm<L, E> ApgTermDeref(L f, ApgTerm<L, E> str) {
    return mkApgTerm(null, null, null, null, str, null, null, null, f, null, null, null, null);
  }

  public static synchronized <L, E> ApgTerm<L, E> ApgTermApp(String head, List<ApgTerm<L, E>> a) {
    return mkApgTerm(null, null, null, null, null, null, null, null, null, null, null, a, head);
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(Object x) {
    return this == x;
  }

  public int hashCode2() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((a == null) ? 0 : a.hashCode2());
    result = prime * result + ((e == null) ? 0 : e.hashCode());
    result = prime * result + ((inj == null) ? 0 : inj.hashCode());
    result = prime * result + ((proj == null) ? 0 : proj.hashCode());
    result = prime * result + ((var == null) ? 0 : var.hashCode());
    result = prime * result + ((deref == null) ? 0 : deref.hashCode());
    result = prime * result + ((prim == null) ? 0 : prim.hashCode());
    result = prime * result + ((head == null) ? 0 : head.hashCode());
    result = prime * result + ((args == null) ? 0 : args.hashCode());
    if (fields != null) {
      for (Entry<String, ApgTerm<L, E>> z : fields.entrySet()) {
        result = prime * result + (z.getValue().hashCode2());
        result = prime * result + (z.getKey().hashCode());
      }
    }
    if (cases != null) {
      for (Entry<String, Pair<String, ApgTerm<L, E>>> z : cases.entrySet()) {
        result = prime * result + (z.getKey().hashCode());
        result = prime * result + (z.getValue().second.hashCode2());
        result = prime * result + (z.getValue().first.hashCode());
      }
    }
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  public boolean equals2(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    @SuppressWarnings("unchecked")
    ApgTerm<L, E> other = (ApgTerm<L, E>) obj;
    if (e == null) {
      if (other.e != null)
        return false;
    } else if (!e.equals(other.e))
      return false;
    if (inj == null) {
      if (other.inj != null)
        return false;
    } else if (!inj.equals(other.inj))
      return false;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
      return false;
    if (var == null) {
      if (other.var != null)
        return false;
    } else if (!var.equals(other.var))
      return false;
    if (prim == null) {
      if (other.prim != null)
        return false;
    } else if (!prim.equals(other.prim))
      return false;
    if (proj == null) {
      if (other.proj != null)
        return false;
    } else if (!proj.equals(other.proj))
      return false;
    if (a == null) {
      if (other.a != null)
        return false;
    } else if (!a.equals2(other.a))
      return false;
    if (deref == null) {
      if (other.deref != null)
        return false;
    } else if (!deref.equals(other.deref))
      return false;

    if (head == null) {
      if (other.head != null)
        return false;
    } else if (!head.equals(other.head))
      return false;
    if (args == null) {
      if (other.args != null)
        return false;
    } else if (!args.equals(other.args))
      return false;
    if (fields == null) {
      if (other.fields != null)
        return false;
    } else {
      if (!fields.keySet().equals(other.fields.keySet())) {
        return false;
      }
      for (Entry<String, ApgTerm<L, E>> f : fields.entrySet()) {
        if (!other.fields.get(f.getKey()).equals2(f.getValue())) {
          return false;
        }
      }
      return true;
    }
    if (cases == null) {
      if (other.cases != null)
        return false;
    } else {
      if (!cases.keySet().equals(other.cases.keySet())) {
        return false;
      }
      for (Entry<String, Pair<String, ApgTerm<L, E>>> f : cases.entrySet()) {
        if (!other.cases.get(f.getKey()).first.equals(f.getValue().first)) {
          return false;
        }
        if (!other.cases.get(f.getKey()).second.equals2(f.getValue().second)) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public String toString() {
    if (e != null) {
      return e.toString();
    }
    if (value != null) {
      return value.toString();
    }
    if (fields != null) {
      return "(" + Util.sep(fields, ":", ", ") + ")";
    }
    if (inj != null) {
      return "<" + inj + ":" + a + ">"; // + Util.sep(m, ": ", " ");
    }
    if (proj != null) {
      return "." + proj + "(" + a + ")";
    }
    if (cases != null) {
      return "case " + a + " where \n"
          + Util.sep(cases, " -> ", " \n ", x -> "lambda " + x.first + ". " + x.second);
    }
    if (var != null) {
      return var.toString();
    }
    if (deref != null) {
      return "!" + deref + "(" + a + ")";
    }
    if (head != null) {
      return head + "(" + Util.sep(args, ",") + ")";
    }
    return Util.anomaly();
  }

  @SuppressWarnings("unchecked")
  public <I, X> ApgTerm<I, X> convert() {
    return (ApgTerm<I, X>) this;
  }

  public <X> ApgTerm<L, X> map(Function<E, X> f) {
    if (e != null) {
      return ApgTermE(f.apply(e));
    }
    if (value != null || var != null) {
      return this.convert();
    }
    if (fields != null) {
      return ApgTermTuple(Util.map(fields, (k, v) -> new Pair<>(k, v.map(f))));
    }
    if (inj != null) {
      return ApgTermInj(inj, a.map(f), cases_t);
    }
    if (proj != null) {
      return ApgTermProj(proj, a.map(f));
    }
    if (cases != null) {
      return ApgTermCase(a.map(f), Util.map(cases, (k, v) -> new Pair<>(k, new Pair<>(v.first, v.second.map(f)))),
          cases_t);
    }
    if (deref != null) {
      return ApgTermDeref(deref, a.map(f));
    }
    if (head != null) {
      return ApgTermApp(head, Util.map(args, x -> x.map(f)));
    }
    return Util.anomaly();
  }

  public ApgTerm<L, E> subst(String from, ApgTerm<L, E> to) {
    if (e != null || value != null) {
      return this.convert();
    }
    if (var != null) {
      if (from.equals(var)) {
        return to;
      }
      return this.convert();
    }
    if (head != null) {
      return ApgTermApp(this.head, Util.map(this.args, x -> x.subst(from, to)));
    }
    if (fields != null) {
      return ApgTermTuple(Util.map(fields, (k, v) -> new Pair<>(k, v.subst(from, to))));
    }
    if (inj != null) {
      return ApgTermInj(inj, a.subst(from, to), cases_t); // + Util.sep(m, ": ", " ");
    }
    if (proj != null) {
      return ApgTermProj(proj, a.subst(from, to));
    }
    if (cases != null) {
      Map<String, Pair<String, ApgTerm<L, E>>> match = new THashMap<>();
      for (Entry<String, Pair<String, ApgTerm<L, E>>> x : cases.entrySet()) {
        String k = x.getKey();
        Pair<String, ApgTerm<L, E>> p = x.getValue();

        if (p.first.equals(from)) {

        } else {
          if (p.second.isFree(from)) {
            String z = findNext(from);
            p = new Pair<>(z, p.second.rename(from, z).subst(from, to));
          } else {
            p = new Pair<>(p.first, p.second.subst(from, to));
          }
        }
        // Pair<Var, ApgTerm<L, E>> = new Pair<>()

        match.put(k, p);
      }

      return ApgTermCase(a.subst(from, to), match, cases_t);
    }
    if (deref != null) {
      return ApgTerm.ApgTermDeref(deref, a.subst(from, to));
    }
    return Util.anomaly();
  }

  private boolean isFree(String v) {
    if (e != null || value != null) {
      return false;
    }
    if (inj != null || proj != null || deref != null) {
      return a.isFree(v);
    }
    if (fields != null) {
      for (Entry<String, ApgTerm<L, E>> x : fields.entrySet()) {
        if (x.getValue().isFree(v)) {
          return true;
        }
      }
      return false;
    }
    if (args != null) {
      for (ApgTerm<L, E> x : args) {
        if (x.isFree(v)) {
          return true;
        }
      }
      return false;
    }

    if (var != null) {
      return var.equals(v);
    }
    if (cases != null) {
      for (Entry<String, Pair<String, ApgTerm<L, E>>> x : cases.entrySet()) {
        if (x.getValue().first.equals(v)) {
          continue;
        }
        if (x.getValue().second.isFree(v)) {
          return true;
        }
      }
      return a.isFree(v);
    }
    return Util.anomaly();
  }

  private static int i = 0;

  private static synchronized String findNext(String from) {
    return ("_gensym_" + (i++));
  }

  public ApgTerm<L, E> rename(String from, String to) {
    if (e != null || value != null) {
      return this;
    }
    if (fields != null) {
      return ApgTerm.ApgTermTuple(Util.map(fields, (k, v) -> new Pair<>(k, v.rename(from, to))));
    }
    if (inj != null) {
      return ApgTerm.ApgTermInj(this.inj, a.rename(from, to), cases_t);
    }
    if (head != null) {
      return ApgTerm.ApgTermApp(this.head, Util.map(this.args, x -> x.rename(from, to)));
    }
    if (proj != null) {
      return ApgTerm.ApgTermProj(this.proj, a.rename(from, to));
    }
    if (var != null) {
      if (var.equals(from)) {
        return ApgTerm.ApgTermVar(to);
      }
      return this;
    }
    if (cases != null) {
      return ApgTerm.ApgTermCase(a.rename(from, to),
          Util.map(cases,
              (k, v) -> new Pair<>(k,
                  new Pair<>(v.first.equals(from) ? to : v.first, v.second.rename(from, to)))),
          cases_t);
    }
    if (deref != null) {
      return ApgTerm.ApgTermDeref(deref, a.rename(from, to));
    }
    return Util.anomaly();
  }
}
