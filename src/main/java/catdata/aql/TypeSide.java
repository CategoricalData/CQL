package catdata.aql;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Iterators;

import catdata.Chc;
//import catdata.Ctx;
import catdata.Pair;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import gnu.trove.set.hash.THashSet;

public class TypeSide<Ty, Sym> implements Semantics {

	public synchronized Set<Term<Ty, Void, Sym, Void, Void, Void, Void>> hom(List<Ty> ctx, Ty ty) {
		Map<Var, Chc<Ty, Void>> ret = Util.mk();
		int i = 0;
		for (Ty t : ctx) {
			ret.put(Var.Var(t.toString() + (i++)), Chc.inLeft(t));
		}
		return makeModel(ret, -1).get(ty);
	}

	public static Collection<Object> enumerate(String clazz) {
		try {
			Class<?> c = Class.forName(clazz);
			if (c.isEnum()) {
				return Arrays.asList(c.getEnumConstants());
			} else if (c.equals(Boolean.class)) {
				return Util.union(Collections.singletonList(true), Collections.singletonList(false));
			} else if (c.equals(Unit.class)) {
				return Collections.singletonList(Unit.unit);
			} else if (c.equals(Void.class)) {
				return Collections.emptySet();
			}
			throw new RuntimeException("Cannot enumerate: " + clazz + ".\n\nLikely cause: evaluation of var:type binding in a query, or pi with a mapping that is not surjective on attributes.  This often indicates a modeling error.  To proceed, use a finite, non-java typeside.");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized Map<Ty, Set<Term<Ty, Void, Sym, Void, Void, Void, Void>>> makeModel(
			Map<Var, Chc<Ty, Void>> ctx, int inbound) {
		Map<Ty, Set<Term<Ty, Void, Sym, Void, Void, Void, Void>>> model = Util.mk();
		for (Ty ty : collage().tys()) {
			model.put(ty, (new THashSet<>()));
			if (this.model != null) {
				model.get(ty).addAll(this.model.get(ty));
			}
		}
		for (Var var : ctx.keySet()) {
			model.get(ctx.get(var).l).add(Term.Var(var));
		}
		for (Ty ty : js.java_tys.keySet()) {
			for (Object o : enumerate(js.java_tys.get(ty))) {
				model.get(ty).add(Term.Obj(o, ty));
			}
		}

		for (int bound = 0; bound < inbound; bound++) {
			boolean changed = false;
			for (Sym f : syms.keySet()) {
				List<Ty> s = syms.get(f).first;
				Ty t = syms.get(f).second;
				List<Set<Term<Ty, Void, Sym, Void, Void, Void, Void>>> l = new ArrayList<>(s.size());
				for (Ty ty : s) {
					l.add(model.get(ty));
				}
				List<List<Term<Ty, Void, Sym, Void, Void, Void, Void>>> r = Util.prod(l);
				outer: for (List<Term<Ty, Void, Sym, Void, Void, Void, Void>> args : r) {
					Term<Ty, Void, Sym, Void, Void, Void, Void> cand = Term.Sym(f, args);
					for (Term<Ty, Void, Sym, Void, Void, Void, Void> other : model.get(t)) {
						if (semantics().eq(ctx, cand, other)) {
							continue outer;
						}
					}
					model.get(t).add(cand);
					changed = true;
				}
			}
			if (!changed) {
				break;
			}
		}

		return model;
	}

	private Map<Ty, Set<Term<Ty, Void, Sym, Void, Void, Void, Void>>> model;

	public synchronized Map<Ty, Set<Term<Ty, Void, Sym, Void, Void, Void, Void>>> getModel() {
		if (model != null) {
			return model;
		}
		this.model = makeModel(Util.mk(), -1);

		return model;
	}

	@Override
	public int size() {
		return tys.size() + syms.size() + eqs.size();
	}

	@Override
	public Kind kind() {
		return Kind.TYPESIDE;
	}

	public final Set<Ty> tys;
	public final Map<Sym, Pair<List<Ty>, Ty>> syms;
	public final Set<Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>>> eqs;

	public final AqlJs<Ty, Sym> js;

  public synchronized void addTy(Ty ty) {
    tys.add(ty);
  }

  public synchronized void addSym(Sym sym, Pair<List<Ty>, Ty> sort) {
    syms.put(sym, sort);
  }

  public synchronized void addEq(Map<Var, Ty> ctx, Term<Ty, Void, Sym, Void, Void, Void, Void> lhs, Term<Ty, Void, Sym, Void, Void, Void, Void> rhs) {
    eqs.add(new Triple<>(ctx, lhs, rhs));
  }

	public <En, Fk, Att, Gen, Sk> Ty type(Map<Var, Ty> ctx, Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		if (!term.isTypeSide()) {
			throw new RuntimeException(term + " is not a typeside term");
		}
		Chc<Ty, En> t = term.type(ctx, Collections.emptyMap(), tys, syms, js.java_tys, Collections.emptySet(),
				Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
		if (!t.left) {
			throw new RuntimeException(term + " has type " + t.l
					+ " which is not in the typeside. Anomaly: please report");
		}
		return t.l;
	}
/*
	private static <Ty, Sym> Collage<Ty, Void, Sym, Void, Void, Void, Void> col(Set<Ty> tys,
			Map<Sym, Pair<List<Ty>, Ty>> syms,
			Set<Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>>> eqs,
			Map<Ty, String> java_tys, Map<Ty, String> java_parsers, Map<Sym, String> java_fns) {
		Collage<Ty, Void, Sym, Void, Void, Void, Void> col = new Collage<>();
		col.tys().addAll(tys);
		col.syms().putAll(syms);
		col.java_tys().putAll(java_tys);
		col.java_parsers().putAll(java_parsers);
		col.java_fns().putAll(java_fns);
		for (Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>> eq : eqs) {
			col.eqs().add(new Eq<>(Util.inLeft(eq.first), eq.second, eq.third));
		}
		return col;
	}
*/
	final AqlOptions strat;

	public TypeSide(Set<Ty> tys, Map<Sym, Pair<List<Ty>, Ty>> syms,
			Set<Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>>> eqs,
			Map<Ty, String> java_tys_string, Map<Ty, String> java_parser_string, Map<Sym, String> java_fns_string,
			AqlOptions strategy) {
		Util.assertNotNull(tys, syms, eqs, java_tys_string, java_parser_string, java_fns_string);
		this.tys = tys;
		this.syms = syms;
		this.eqs = eqs;
		boolean checkJava = !((Boolean) strategy.getOrDefault(AqlOption.allow_java_eqs_unsafe));
		this.strat = strategy;

		this.js = new AqlJs<>(strategy, syms, java_tys_string, java_parser_string, java_fns_string);

		// if (checkJava) {
		validate(checkJava);
		// }

		boolean makeDp = !((Boolean) strategy.getOrDefault(AqlOption.dont_validate_unsafe));
		if (makeDp) {
			semantics();
		}
	}

	public TypeSide(Set<Ty> tys, Map<Sym, Pair<List<Ty>, Ty>> syms,
			Set<Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>>> eqs,
			AqlJs<Ty, Sym> js, DP<Ty, Void, Sym, Void, Void, Void, Void> semantics, AqlOptions strategy) {
		Util.assertNotNull(tys, syms, eqs, js, semantics);
		this.tys = tys;
		this.syms = syms;
		this.eqs = eqs;
		this.semantics = semantics;
		this.js = js;
		// this.strat =
		this.strat = strategy;
		boolean checkJava = !((Boolean) strategy.getOrDefault(AqlOption.allow_java_eqs_unsafe));

		validate(checkJava);
	}

	private synchronized void validate(boolean checkJava) {
		// check that each sym is in tys
		for (Sym sym : syms.keySet()) {
			Pair<List<Ty>, Ty> ty = syms.get(sym);
			if (!tys.contains(ty.second)) {
				throw new RuntimeException(
						"On typeside symbol " + sym + ", the return type " + ty.second + " is not declared.");
			}
			for (Ty t : ty.first) {
				if (!tys.contains(t)) {
					throw new RuntimeException(
							"On typeside symbol " + sym + ", the argument type " + t + " is not declared.");
				}
			}
			// System.out.println(sym);
		}
		for (Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>> eq : eqs) {
			// check that the context is valid for each eq
			Set<Ty> used_tys = (new THashSet<>(eq.first.values()));
			used_tys.removeAll(tys);
			if (!used_tys.isEmpty()) {
				throw new RuntimeException("In typeside equation " + toString(eq) + ", context uses types " + used_tys
						+ " that are not declared.");
			}
			// check lhs and rhs types match in all eqs
			Ty lhs = type(eq.first, eq.second);
			Ty rhs = type(eq.first, eq.third);
			if (!lhs.equals(rhs)) {
				throw new RuntimeException(
						"In typeside equation " + toString(eq) + ", lhs type is " + lhs + " but rhs type is " + rhs);
			}

		}
		for (Ty k : js.java_parsers.keySet()) {
			if (!js.java_tys.containsKey(k)) {
				throw new RuntimeException(
						"There is a java parser for " + k + " but it is not declared as a java type");
			}
		}
		for (Sym sym : js.java_fns.keySet()) {
			if (!syms.containsKey(sym)) {
				throw new RuntimeException("The java function " + sym + " is not a declared function");
			}
		}
		for (Ty ty : js.java_tys.keySet()) {
			String parser = js.java_parsers.get(ty);
			if (parser == null) {
				throw new RuntimeException("No constant parser for " + ty);
			}
			String clazz = js.java_tys.get(ty);
			Util.load(clazz);
		}
		if (checkJava) {
			validateJava();
		}
	}

	private void validateJava() {

		for (Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>> eq : eqs) {
			Ty lhs = type(eq.first, eq.second);

			if (js.java_tys.containsKey(lhs)) {
				throw new RuntimeException("In typeside equation " + toString(eq) + ", the return type is " + lhs
						+ " which is a java type " + "\n\nPossible solution: add options allow_java_eqs_unsafe=true");
			}
			if (!Collections.disjoint(js.java_tys.keySet(), eq.first.values())) {
				throw new RuntimeException(
						"In typeside equation " + toString(eq) + ", the context variable(s) bind java type(s)"
								+ "\n\nPossible solution: add options allow_java_eqs_unsafe=true");
			}
			assertNoJava(eq.second);
			assertNoJava(eq.third);
		}

		for (Sym sym : syms.keySet()) {
			Pair<List<Ty>, Ty> t = syms.get(sym);
			if (allJava(t) || noJava(t)) {

			} else {
				throw new RuntimeException("In symbol " + sym + ", functions must not mix java and non-java types"
						+ "\n\nPossible solution: add options allow_java_eqs_unsafe=true");
			}
		}
	}

	private boolean noJava(Pair<List<Ty>, Ty> t) {
		List<Ty> l = (new LinkedList<>(t.first));
		l.add(t.second);

		for (Ty ty : l) {
			if (js.java_tys.containsKey(ty)) {
				return false;
			}
		}

		return true;
	}

	// TODO aql move to collage?
	private boolean allJava(Pair<List<Ty>, Ty> t) {
		List<Ty> l = (new LinkedList<>(t.first));
		l.add(t.second);

		for (Ty ty : l) {
			if (!js.java_tys.containsKey(ty)) {
				return false;
			}
		}

		return true;
	}

	public <En, Fk, Att, Gen, Sk> void assertNoJava(Term<Ty, En, Sym, Fk, Att, Gen, Sk> t) {
		if (t.var != null) {
			return;
		} else if (t.fk() != null || t.att() != null) {
			assertNoJava(t.arg);
			return;
		} else if (t.sym() != null) {
			Pair<List<Ty>, Ty> x = syms.get(t.sym());
			if (!Collections.disjoint(x.first, js.java_tys.keySet())) {
				throw new RuntimeException("In " + t + ", functions with java types are not allowed ");
			} else if (js.java_tys.keySet().contains(x.second)) {
				throw new RuntimeException("In " + t + ", functions with java types are not allowed");
			}
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> arg : t.args) {
				assertNoJava(arg);
			}
			return;
		} else if (t.obj() != null) {
			throw new RuntimeException("In " + t + ", java constants are not allowed ");
		}

		throw new RuntimeException("Anomaly: please report."); // else if (t.g)
	}

	private String toString(
			Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>> eq) {
		String pre = eq.first.isEmpty() ? "" : "forall ";
		return pre + eq.first + ". " + eq.second + " = " + eq.third;
	}

	public static <X, Y> TypeSide<X, Y> initial(AqlOptions ops) {
		return new TypeSide<>(new LinkedHashSet<>(0), new LinkedHashMap<>(0), Collections.emptySet(),
				new AqlJs<>(ops, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
						Collections.emptyMap()),
				DP.initial(), ops);
	}

	private DP<Ty, Void, Sym, Void, Void, Void, Void> semantics;

	public synchronized DP<Ty, Void, Sym, Void, Void, Void, Void> semantics() {
		if (semantics != null) {
			return semantics;
		}
		semantics = AqlProver.createTypeSide(strat, collage(), js);

		return semantics;
	}

	//private Collage<Ty, Void, Sym, Void, Void, Void, Void> collage;

	public synchronized <En, Fk, Att, Gen, Sk> Collage<Ty, En, Sym, Fk, Att, Gen, Sk> collage() {
		return new Collage<>() {

			@Override
			public Set<Ty> tys() {
				return tys;
			}

			@Override
			public Map<Sym, Pair<List<Ty>, Ty>> syms() {
				return syms;
			}

			@Override
			public Map<Ty, String> java_tys() {
				return js.java_tys;
			}

			@Override
			public Map<Ty, String> java_parsers() {
				return js.java_parsers;
			}

			@Override
			public Map<Sym, String> java_fns() {
				return js.java_fns;
			}

			@Override
			public Set<En> getEns() {
				return Collections.emptySet();
			}

			@Override
			public Map<Att, Pair<En, Ty>> atts() {
				return Collections.emptyMap();
			}

			@Override
			public Map<Fk, Pair<En, En>> fks() {
				return Collections.emptyMap();
			}

			@Override
			public Map<Gen, En> gens() {
				return Collections.emptyMap();
			}

			@Override
			public Map<Sk, Ty> sks() {
				return Collections.emptyMap();
			}

			@Override
			public Collection<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> eqs() {
				Collection<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> ret = new AbstractCollection<>() {

					@Override
					public Iterator<Eq<Ty, En, Sym, Fk, Att, Gen, Sk>> iterator() {
						return Iterators.transform(eqs.iterator(), (t)->new Eq<>(Util.inLeft(t.first), t.second.convert(), t.third.convert()));
					}

					@Override
					public int size() {
						return eqs.size();
					}

				};

				int j = 0;
				for (Eq<Ty, En, Sym, Fk, Att, Gen, Sk> i : ret) {
					j++;
				}
				if (j != ret.size()) {
					Util.anomaly();
				}

				return ret;
			}

			@Override
			public String toString() {
				return this.toString(new CCollage<>());
			}

		};
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
		// result = prime * result + ((java_fns == null) ? 0 : java_fns.hashCode());
		// result = prime * result + ((java_parsers == null) ? 0 :
		// java_parsers.hashCode());
		result = prime * result + ((js == null) ? 0 : js.hashCode());
		result = prime * result + ((syms == null) ? 0 : syms.hashCode());
		result = prime * result + ((tys == null) ? 0 : tys.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		TypeSide<?, ?> other = (TypeSide<?, ?>) obj;
		if (eqs == null) {
			if (other.eqs != null)
				return false;
		} else if (!eqs.equals(other.eqs))
			return false;
		/*
		 * if (java_fns == null) { if (other.java_fns != null) return false; } else if
		 * (!java_fns.equals(other.java_fns)) return false; if (java_parsers == null) {
		 * if (other.java_parsers != null) return false; } else if
		 * (!java_parsers.equals(other.java_parsers)) return false;
		 */
		if (js == null) {
			if (other.js != null)
				return false;
		} else if (!js.equals(other.js))
			return false;
		if (syms == null) {
			if (other.syms != null)
				return false;
		} else if (!syms.equals(other.syms))
			return false;
		if (tys == null) {
			if (other.tys != null)
				return false;
		} else if (!tys.equals(other.tys))
			return false;
		return true;
	}

	private String toString;

	@Override
	public synchronized String toString() {
		if (toString != null) {
			return toString;
		}
		List<Ty> tys0 = Util.alphabetical(tys);
		List<Sym> syms0 = Util.alphabetical(syms.keySet());

		List<String> eqs1 = new LinkedList<>();
		for (Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>> x : eqs) {
			if (x.first.isEmpty()) {
				eqs1.add(x.second + " = " + x.third);
			} else {
				eqs1.add("forall " + Util.sep(x.first, ":", ",") + ". " + x.second + " = " + x.third);
			}
		}
		eqs1 = Util.alphabetical(eqs1);
		List<String> eqs0 = Util.alphabetical(eqs1);

		toString = "";
		if (!tys0.isEmpty()) {
			toString += "types";
			toString += "\n\t" + Util.sep(tys0, " ");
		}
		if (!syms0.isEmpty()) {
			toString += "\nfunctions";
			List<String> temp = new LinkedList<>();
			for (Sym sym : syms0) {
				Pair<List<Ty>, Ty> t = syms.get(sym);
				temp.add(sym + " : " + Util.sep(t.first, ", ") + " -> " + t.second);
			}
			toString += "\n\t" + Util.sep(temp, "\n\t");
		}

		if (!eqs0.isEmpty()) {
			toString += "\nequations";
			toString += "\n\t" + Util.sep(eqs0, "\n\t");
		}

		if (!js.java_tys.isEmpty()) {
			toString += "\njava_types";
			toString += "\n\t" + Util.sep(tys0, js.java_tys, " = ", "\n\t", true, Object::toString, Object::toString);
		}

		if (!js.java_parsers.isEmpty()) {
			toString += "\njava_constants";
			toString += "\n\t" + Util.sep(tys0, js.java_parsers, " = ", "\n\t", true, Object::toString, Object::toString);
		}
		if (!js.java_fns.isEmpty()) {
			toString += "\njava_functions";
			toString += "\n\t" + Util.sep(syms0, js.java_fns, " = ", "\n\t", true, Object::toString, Object::toString);
		}

		return toString;
	}

	public boolean hasImplicitJavaEqs() {
		for (Sym x : js.java_fns.keySet()) {
			if (!syms.get(x).first.isEmpty()) {
				return true;
			}
		}
		return false;
	}


	public String toCheckerTpTp() {
		StringBuffer sb = new StringBuffer();
		if (!js.java_tys.isEmpty()) {
			Util.anomaly();
		}
		for (Ty ty : tys) {
			sb.append("tff(" + ty + "_type, type, " + ty + ": $tType).\n");
		}
		for (Entry<Sym, Pair<List<Ty>, Ty>> o : syms.entrySet()) {
			String b = o.getValue().second.toString();
			String a = Util.sep(o.getValue().first, "*");
			if (o.getValue().first.isEmpty()) {
				sb.append("tff(" + o.getKey() + "_type, type, " + o.getKey() + ": " + b + ").\n");
			} else if (o.getValue().first.size() == 1) {
				sb.append("tff(" + o.getKey() + "_type, type, " + o.getKey() + ": " + a + " > " + b + ").\n");
			} else {
				sb.append("tff(" + o.getKey() + "_type, type, " + o.getKey() + ": (" + a + ") > " + b + ").\n");
			}
		}
		int i = 0;
		for (Triple<Map<Var, Ty>, Term<Ty, Void, Sym, Void, Void, Void, Void>, Term<Ty, Void, Sym, Void, Void, Void, Void>> eq : eqs) {
			if (!eq.first.isEmpty()) {
				Util.anomaly();
			}
			sb.append("tff(ts" + (i++) + ", axiom, (" + eq.second.toTpTpForChecker() + " = " + eq.third.toTpTpForChecker() + ")).\n");
		}
		return sb.toString();
	}

}
