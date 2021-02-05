package catdata.aql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import catdata.aql.exp.Sym;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;


public class SqlTypeSide2<Ty> {

  public final Function<Ty, String> toCql;
  public final Ty boolTy;

  public SqlTypeSide2(Function<Ty, String> toCql, Ty boolTy) {
    this.toCql = toCql;
    this.boolTy = boolTy;
  }

  public static final SqlTypeSide2<catdata.aql.exp.Ty> FOR_TY =
      new SqlTypeSide2<>(ty -> ty.str, catdata.aql.exp.Ty.Ty("boolean"));

  public synchronized TypeSide<Ty, Sym> make(TypeSide<Ty, Sym> parent, AqlOptions ops) {
    if (!parent.js.java_tys.isEmpty()) {
			throw new RuntimeException("Cannot nullify java typesides");
		}
		return new TypeSide<Ty, Sym>(tys(parent), syms(parent), eqs(parent), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), ops);
  }

  public static final Sym TRUE_SYM = Sym.Sym("true"),
                          FALSE_SYM = Sym.Sym("false"),
                          NOT_SYM = Sym.Sym("not"),
                          IS_TRUE_SYM = Sym.Sym("isTrue"),
                          IS_FALSE_SYM = Sym.Sym("isFalse"),
                          AND_SYM = Sym.Sym("and"),
                          OR_SYM = Sym.Sym("or");


	public Set<Ty> tys(TypeSide<Ty, Sym> parent) {
		return Util.union(parent.tys, Collections.singleton(boolTy));
	}

  public Sym typedNull(Ty ty) {
    return Sym.Sym("null_" + toCql.apply(ty));
  }

  public Sym typedEq(Ty ty) {
    return Sym.Sym("eq_" + toCql.apply(ty));
  }

  public Sym typedIsNull(Ty ty) {
    return Sym.Sym("isNull_" + toCql.apply(ty));
  }

  private List<Pair<Sym, Pair<List<Ty>, Ty>>> genSymsForTy(Ty ty) {
    List<Ty> l =  Collections.synchronizedList(new ArrayList<>(2));
    l.add(ty);
    l.add(ty);
    List<Ty> p =  Collections.synchronizedList(new ArrayList<>(1));
    p.add(ty);
    return Util.list(
      new Pair<>(typedNull(ty), new Pair<>(Collections.emptyList(), ty)),
      new Pair<>(typedEq(ty), new Pair<>(l, boolTy)),
      new Pair<>(typedIsNull(ty), new Pair<>(p, boolTy))
    );
  }

  public boolean addTy(TypeSide<Ty, Sym> ts, Ty ty) {
    synchronized (ts) {
      if (!ts.tys.contains(ty)) {
        ts.addTy(ty);
        for (Pair<Sym, Pair<List<Ty>, Ty>> genSym : genSymsForTy(ty)) {
          ts.addSym(genSym.first, genSym.second);
        }
        return true;
      } else return false;
    }
  }

  public boolean addSym(TypeSide<Ty, Sym> ts, Sym sym, List<Ty> dom, Ty cod) {
    synchronized (ts) {
      if (!ts.syms.containsKey(sym)) {
        for (Ty ty : dom) addTy(ts, ty);
        addTy(ts, cod);
        ts.addSym(sym, new Pair<>(dom, cod));
        return true;
      } else return false;
    }
  }

	private synchronized Map<Sym, Pair<List<Ty>, Ty>> syms(TypeSide<Ty, Sym> parent) {
		Map<Sym, Pair<List<Ty>, Ty>> m = Util.mk();
		m.putAll(parent.syms);
		m.put(TRUE_SYM, new Pair<>(Collections.emptyList(), boolTy));
		m.put(FALSE_SYM, new Pair<>(Collections.emptyList(), boolTy));

		List<Ty> x = Collections.synchronizedList(new ArrayList<>(1));
		x.add(boolTy);
		m.put(NOT_SYM, new Pair<>(x, boolTy));
		m.put(IS_TRUE_SYM, new Pair<>(x, boolTy));
		m.put(IS_FALSE_SYM, new Pair<>(x, boolTy));
	//	m.put(Sym.Sym("isNotTrue"), new Pair<>(x, boolTy));
	//	m.put(Sym.Sym("isNotFalse"), new Pair<>(x, boolTy));

		List<Ty> y =  Collections.synchronizedList(new ArrayList<>(2));
		y.add(boolTy);
		y.add(boolTy);
		m.put(AND_SYM, new Pair<>(y, boolTy));
		m.put(OR_SYM, new Pair<>(y, boolTy));

		for (Ty ty : tys(parent)) {
			for (Pair<Sym, Pair<List<Ty>, Ty>> genSym : genSymsForTy(ty)) {
        m.put(genSym.first, genSym.second);
      }
	//		m.put(Sym.Sym("isNotNull_" + ty.str), new Pair<>(p, boolTy));
		}

		return m;
	}



	private Set<Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>>> eqs(TypeSide<Ty,Sym> parent) {
		Set<Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>>> ret = new THashSet<>();
		ret.addAll(parent.eqs);

		//null propagation
		//congruence for each symbol (EDs)

		Term<Ty, Void, Sym, Void, Void, Void, Void> t = Term.Sym(TRUE_SYM, Collections.emptyList());
		Term<Ty, Void, Sym, Void, Void, Void, Void> f = Term.Sym(FALSE_SYM, Collections.emptyList());
		Term<Ty, Void, Sym, Void, Void, Void, Void> u = Term.Sym(typedNull(boolTy), Collections.emptyList());

		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(NOT_SYM, Collections.singletonList(t)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(NOT_SYM, Collections.singletonList(f)), t));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(NOT_SYM, Collections.singletonList(u)), u));

		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(IS_TRUE_SYM, Collections.singletonList(t)), t));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(IS_TRUE_SYM, Collections.singletonList(f)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(IS_TRUE_SYM, Collections.singletonList(u)), f));

		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(IS_FALSE_SYM, Collections.singletonList(t)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(IS_FALSE_SYM, Collections.singletonList(f)), t));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(IS_FALSE_SYM, Collections.singletonList(u)), f));

		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(AND_SYM, Util.list(t,t)), t));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(AND_SYM, Util.list(t,f)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(AND_SYM, Util.list(f,t)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(AND_SYM, Util.list(f,f)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(AND_SYM, Util.list(u,f)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(AND_SYM, Util.list(f,u)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(AND_SYM, Util.list(u,t)), u));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(AND_SYM, Util.list(t,u)), u));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(AND_SYM, Util.list(u,u)), u));

		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(OR_SYM, Util.list(t,t)), t));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(OR_SYM, Util.list(t,f)), t));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(OR_SYM, Util.list(f,t)), t));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(OR_SYM, Util.list(f,f)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(OR_SYM, Util.list(u,f)), u));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(OR_SYM, Util.list(f,u)), u));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(OR_SYM, Util.list(u,t)), t));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(OR_SYM, Util.list(t,u)), t));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(OR_SYM, Util.list(u,u)), u));

		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedEq(boolTy), Util.list(t,t)), t));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedEq(boolTy), Util.list(t,f)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedEq(boolTy), Util.list(f,t)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedEq(boolTy), Util.list(f,f)), t));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedEq(boolTy), Util.list(u,f)), u));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedEq(boolTy), Util.list(f,u)), u));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedEq(boolTy), Util.list(u,t)), u));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedEq(boolTy), Util.list(t,u)), u));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedEq(boolTy), Util.list(u,u)), u));

		for (Entry<Sym, Pair<List<Ty>, Ty>> sym : parent.syms.entrySet()) {
			if (sym.getValue().first.isEmpty()) {
				continue;
			}
			for (int j = 0; j < sym.getValue().first.size(); j++) {
				int i = 0;
				Map<Var, Ty> m = new THashMap<>();
				List<Term<Ty, Void, Sym, Void, Void, Void, Void>> l = new LinkedList<>();
				for (Ty arg : sym.getValue().first) {
					if (i == j) {
						l.add(Term.Sym(typedNull(arg), Collections.emptyList()));
						continue;
					}
					Var v = Var.Var("x" + i++);
					l.add(Term.Var(v));
					m.put(v, arg);
				}
				ret.add(new Triple<>(m, Term.Sym(sym.getKey(), l),Term.Sym(typedNull(sym.getValue().second), Collections.emptyList())));
			}
		}


		for (Ty ty : tys(parent)) {
			ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedIsNull(ty), Collections.singletonList(Term.Sym(typedNull(ty), Collections.emptyList()))), t));
		}

		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedIsNull(boolTy), Collections.singletonList(t)), f));
		ret.add(new Triple<>(Collections.emptyMap(), Term.Sym(typedIsNull(boolTy), Collections.singletonList(f)), f));
		return ret;
	}
	//options	prover_simplify_max = 0

/*
 * //should be theorems:
	//eq is an equiv relation [can't be - no closed world]
	//eq is a congruence [can't be - no closed world]
	//eqP is an equiv relation
	//eqp is a congruence
	//forall x y : Prop where (x eqP y) = true -> where x = y
	//forall x y : Prop where (x eqP y) = false -> where x <> y
    //forall x y : Dom where (x eq y) = true -> where x = y
	//forall x y : Dom where (x eq y) = false -> where x <> y
 */



}
