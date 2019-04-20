package catdata.aql;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.LocStr;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.Util;
import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Gen;
import catdata.aql.exp.Sk;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class RawTerm {

	public final String head;
	public final List<RawTerm> args;

	public final String annotation;

	@Override
	public String toString() {
		final String str = (annotation == null ? "" : "@" + annotation);
		if (args.isEmpty()) {
			return Util.maybeQuote(head) + str;
		}
		// if (args.size() == 1) {
		// return args.get(0) + "." + head;
		// }
		return Util.maybeQuote(head) + "(" + Util.sep(args, ", ") + ")";
	}
	
	public static Set<Triple<List<Pair<String, String>>, RawTerm, RawTerm>> eqs1(
			Collection<Pair<Integer, Triple<List<Pair<String, String>>, RawTerm, RawTerm>>> eqs) {
		return eqs.stream().map(x -> x.second).collect(Collectors.toSet());
	}

	public static Set<Quad<String, String, RawTerm, RawTerm>> eqs2(
			List<Pair<LocStr, Quad<String, String, RawTerm, RawTerm>>> t_eqs) {
		return t_eqs.stream().map(x -> x.second).collect(Collectors.toUnmodifiableSet());
	}


	/**
	 * Decend the RawTerms tree so long as args are singular.
	 * 
	 * @return
	 */
	public List<String> forwardPack() {
		final LinkedList<String> ls = new LinkedList<>();
		for (RawTerm ix = this;; ix = ix.args.get(0)) {
			ls.addFirst(ix.head);
			if (ix.args.size() < 1)
				break;
			if (ix.args.size() > 1) {
				// log.warn("forward packing should only be on singular args");
			}
		}
		return ls;
	}

	public List<String> backwardPack() {
		final LinkedList<String> ls = new LinkedList<>();
		for (RawTerm ix = this;; ix = ix.args.get(0)) {
			ls.addLast(ix.head);
			if (ix.args.size() < 1)
				break;
			if (ix.args.size() > 1) {
				// log.warn("backward packing should only be on singular args");
			}
		}
		return ls;
	}

	/**
	 * Grab the element from the RawTerm.
	 * 
	 * @param primary   the depth into the term
	 * @param secondary the width to the node
	 * @return a String representing the value of the node. null if not present.
	 */
	public String byIndex(final int... ixs) {
		RawTerm term = this;
		for (int ix : ixs) {
			if (ix >= term.args.size())
				return null;
			term = term.args.get(ix);
		}
		return term.head;
	}

	private static Set<Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>>> infer_good(
			RawTerm e, Chc<Ty, En> expected, Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col, String pre, AqlJs<Ty, Sym> js,
			Map<Var, Chc<Ty, En>> vars) {
		if (e.annotation != null && !col.tys.contains(Ty.Ty(e.annotation))) {
			throw new RuntimeException(pre + "Annotation " + e.annotation + " is not a type (" + col.tys + ").");
		}

		Var vv = Var.Var(e.head);
		Set<Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>>> ret = new THashSet<>();
		if (vars.keySet().contains(vv) && e.annotation == null) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret1 = Term.Var(vv);
			if (expected != null) {
				Map<Var, Chc<Ty, En>> ret2 = new THashMap<>();
				ret2.put(vv, expected);
				if (Util.agreeOnOverlap(ret2, Util.fromNullable(vars))) {
					ret.add(new Triple<>(ret1, ret2, expected));
				}
			} else {
				for (En en : col.ens) {
					Map<Var, Chc<Ty, En>> ret2 = new THashMap<>();
					ret2.put(vv, Chc.inRight(en));
					if (Util.agreeOnOverlap(ret2, Util.fromNullable(vars))) {
						ret.add(new Triple<>(ret1, ret2, Chc.inRight(en)));
					}
				}
				for (Ty ty : col.tys) {
					Map<Var, Chc<Ty, En>> ret2 = new THashMap<>();
					if (Util.agreeOnOverlap(ret2, Util.fromNullable(vars))) {
						ret2.put(vv, Chc.inLeft(ty));
					}
					ret.add(new Triple<>(ret1, ret2, Chc.inLeft(ty)));
				}
			}
		}
		Sym ss = Sym.Sym(e.head);
		if (col.syms.containsKey(ss) && e.annotation == null) {

			List<List<Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>>>> l = new LinkedList<>();
			l.add(new LinkedList<>());
			for (int i = 0; i < e.args.size(); i++) {
				RawTerm arg = e.args.get(i);
				if (col.syms.get(ss).first.size() > e.args.size()) {
					throw new RuntimeException("Arity mismatch on " + e);
				}
				Ty ty = col.syms.get(ss).first.get(i);
				Set<Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>>> z = infer_good(arg,
						Chc.inLeft(ty), col, pre, js, vars);

				List<List<Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>>>> l2 = new LinkedList<>();
				for (List<Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>>> old : l) {
					for (Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>> y : z) {
						if (y.third.equals(Chc.inLeft(ty))) {
							l2.add(Util.append(old, Collections.singletonList(y)));
						}
					}
				}
				l = l2;
			}

			outer: for (List<Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>>> outcome : l) {

				List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> w = outcome.stream().map(x -> x.first)
						.collect(Collectors.toList());
				Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret1 = Term.Sym(ss, w);
				Map<Var, Chc<Ty, En>> ret2 = new THashMap<>();
				for (Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>> Map0 : outcome) {

					if (!Util.agreeOnOverlap(Map0.second, ret2)
							|| !Util.agreeOnOverlap(Map0.second, Util.fromNullable(vars))) {
						continue outer;
					}
					ret2.putAll(Map0.second);
				}
				for (int i = 0; i < e.args.size(); i++) {
					RawTerm arg = e.args.get(i);
					Chc<Ty, En> ty = Chc.inLeft(col.syms.get(ss).first.get(i));
					Var v = Var.Var(arg.head);
					if (vars.keySet().contains(v)) {
						if (ret2.containsKey(v) && !ret2.get(v).equals(ty)) {
							continue;
						} else if (!ret2.containsKey(v)) {
							ret2.put(Var.Var(e.args.get(i).head), ty);
						}
					}
				}

				Chc<Ty, En> ret3 = Chc.inLeft(col.syms.get(ss).second);
				if (expected != null && !expected.equals(ret3)) {

				} else {
					if (Util.agreeOnOverlap(ret2, Util.fromNullable(vars))) {
						ret.add(new Triple<>(ret1, ret2, ret3));
					}
				}
			}
		}

		for (En en : col.ens) {
			if (col.fks.containsKey(Fk.Fk(en, e.head)) && e.args.size() == 1 && e.annotation == null) {
				for (Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>> outcome : infer_good(
						e.args.get(0), Chc.inRight(col.fks.get(Fk.Fk(en, e.head)).first), col, pre, js, vars)) {
					Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret1 = Term.Fk(Fk.Fk(en, e.head), outcome.first);
					Map<Var, Chc<Ty, En>> ret2 = new THashMap<>(outcome.second);
					Var v = Var.Var(e.args.get(0).head);
					Chc<Ty, En> ty = Chc.inRight(col.fks.get(Fk.Fk(en, e.head)).first);
					if (vars.keySet().contains(v)) {
						if (ret2.containsKey(v) && !ret2.get(v).equals(ty)) {
							continue;
						} else if (!ret2.containsKey(v)) {
							ret2.put(v, ty);
						}
					}
					Chc<Ty, En> ret3 = Chc.inRight(col.fks.get(Fk.Fk(en, e.head)).second);
					Chc<Ty, En> argt = Chc.inRight(col.fks.get(Fk.Fk(en, e.head)).first);

					if (expected != null && !expected.equals(ret3)) {
					} else {
						if (argt.equals(outcome.third)) {
							if (Util.agreeOnOverlap(ret2, Util.fromNullable(vars))) {
								ret.add(new Triple<>(ret1, ret2, ret3));
							} else {

							}
						} else {

						}
					}
				}
			}

			if (col.atts.containsKey(Att.Att(en, e.head)) && e.args.size() == 1 && e.annotation == null) {
				for (Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>> outcome : infer_good(
						e.args.get(0), Chc.inRight(col.atts.get(Att.Att(en, e.head)).first), col, pre, js, vars)) {

					Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret1 = Term.Att(Att.Att(en, e.head), outcome.first);
					Map<Var, Chc<Ty, En>> ret2 = new THashMap<>(outcome.second);
					Var v = Var.Var(e.args.get(0).head);
					Chc<Ty, En> ty = Chc.inRight(col.atts.get(Att.Att(en, e.head)).first);
					if (vars.keySet().contains(v)) {
						if (ret2.containsKey(v) && !ret2.get(v).equals(ty)) {
							continue;
						} else if (!ret2.containsKey(v)) {
							ret2.put(v, ty);
						}
					}

					Chc<Ty, En> ret3 = Chc.inLeft(col.atts.get(Att.Att(en, e.head)).second);
					Chc<Ty, En> argt = Chc.inRight(col.atts.get(Att.Att(en, e.head)).first);

					if (expected != null && !expected.equals(ret3)) {
					} else {
						if (argt.equals(outcome.third)) {
							if (Util.agreeOnOverlap(ret2, Util.fromNullable(vars))) {
								ret.add(new Triple<>(ret1, ret2, ret3));
							}
						}
					}
				}
			}
		}

		if (col.gens.containsKey(Gen.Gen(e.head)) && e.args.isEmpty() && e.annotation == null) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret1 = Term.Gen(Gen.Gen(e.head));
			Chc<Ty, En> ret3 = Chc.inRight(col.gens.get(Gen.Gen(e.head)));
			if (expected != null && !expected.equals(ret3)) {
			} else {
				ret.add(new Triple<>(ret1, new THashMap<>(), ret3));
			}
		}
		if (col.sks.containsKey(Sk.Sk(e.head)) && e.args.isEmpty() && e.annotation == null) {
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret1 = Term.Sk(Sk.Sk(e.head));
			Chc<Ty, En> ret3 = Chc.inLeft(col.sks.get(Sk.Sk(e.head)));
			if (expected != null && !expected.equals(ret3)) {
			} else {
				ret.add(new Triple<>(ret1, new THashMap<>(), ret3));
			}
		}
		if (e.args.isEmpty() && e.annotation != null) {
			Ty ty = Ty.Ty(e.annotation);
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret1 = Term.Obj(js.parse(ty, e.head), ty);
			Chc<Ty, En> ret3 = Chc.inLeft(ty);
			if (expected != null && !expected.equals(ret3)) {
			} else {
				ret.add(new Triple<>(ret1, new THashMap<>(), ret3));
			}
		}
		// as primitive - only if not a variable/generator/etc in scope i.e. none above
		// fired
		if (e.args.isEmpty() && e.annotation == null && ret.isEmpty()) {
			for (Ty ty : col.tys) {
				if (expected != null && !expected.equals(Chc.inLeft(ty))) {
					continue;
				}
				try {
					Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret1 = Term.Obj(js.parse(ty, e.head), ty);
					Chc<Ty, En> ret3 = Chc.inLeft(ty);
					if (expected != null && !expected.equals(ret3)) {
					} else {
						ret.add(new Triple<>(ret1, new THashMap<>(), ret3));
					}
				} catch (Exception ex) {
					if (expected != null) {
						ex.printStackTrace();
						// throw ex;
					}

				}
			}
		}

		return ret;
	}

	private static boolean isSymbolAll(Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col, String s) {
		return col.syms.containsKey(Sym.Sym(s))
				|| col.fks.keySet().stream().map(x -> x.str).collect(Collectors.toSet()).contains(s)
				|| col.atts.keySet().stream().map(x -> x.str).collect(Collectors.toSet()).contains(s)
				|| col.gens.containsKey(Gen.Gen(s)) || col.sks.containsKey(Sk.Sk(s));
	}
	
	private static String truncateHard(String s) {
		if (s.length() > 128) {
			return s.substring(0, 128) + " ... ";
		}
		return s;
	}

	public synchronized static Quad<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Chc<Ty, En>> infer1x(
			Map<String, Chc<Ty, En>> Map0, RawTerm e0, RawTerm f, Chc<Ty, En> expected,
			Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col, String pre, AqlJs<Ty, Sym> js) {
		Set<Quad<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Chc<Ty, En>>> ret = new THashSet<>();

		Map<Var, Chc<Ty, En>> vars0 = new THashMap<>(Map0.size());
		for (String s : Map0.keySet()) {
			vars0.put(Var.Var(s), Map0.get(s));

		}
		Set<Var> vars = Map0.keySet().stream().map(x -> Var.Var(x)).collect(Collectors.toSet());

		Map<Var, Chc<Ty, En>> initial = new THashMap<>();
		for (Var v : vars) {
			if (Map0.get(v.var) != null) {
				initial.put(v, Map0.get(v.var));
			}
		}
		Set<Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>>> lhs = infer_good(e0,
				expected, col, pre, js, vars0);

		if (lhs.isEmpty()) {
			String msg = "Cannot infer a well-sorted term for " + e0 + ".\n";
			if (!vars.contains(Var.Var(e0.head)) && !isSymbolAll(col, e0.head) && e0.annotation == null) {
				msg += "Undefined (or not java-parseable) symbol: " + e0.head + ".\n";
				msg += "\nAvailable symbols:\n\t" + truncateHard(Util.sep(Util.closest(e0.head, col.allSymbolsAsStrings()), "\n\t"));

			} 
			if (expected != null) {
				String msg2 = expected.left ? "type" : "entity";
				msg += "Expected " + msg2 + ": " + expected.toStringMash();
			}

			throw new RuntimeException(pre + msg);
		}

		Set<Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>>> rhs;
		if (f == null) {
			rhs = lhs;
		} else {
			rhs = infer_good(f, expected, col, pre, js, vars0);
		}

		if (rhs.isEmpty()) {
			String msg = "Cannot infer a well-sorted term for " + f + ".\n"; // if f were null, above exn would have
																				// fired
			if (!vars.contains(Var.Var(f.head)) && !isSymbolAll(col, f.head) && f.annotation == null) {
				msg += "Undefined (or not java-parseable) symbol: " + f.head + "\n";
				msg += "\nAvailable symbols:\n\t" + truncateHard(Util.sep(Util.closest(f.head, col.allSymbolsAsStrings()), "\n\t"));
				// TODO aql merge this error message with the one above it
			}
			if (expected != null) {
				String msg2 = expected.left ? "type" : "entity";
				msg += "Expected " + msg2 + ": " + expected.toStringMash();
			}
			throw new RuntimeException(pre + msg);
		}

		for (Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>> outcome : lhs) {
			if (!vars.containsAll(outcome.second.keySet())) {
				Util.anomaly();
			}
		}
		for (Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>> outcome : rhs) {
			if (!vars.containsAll(outcome.second.keySet())) {
				Util.anomaly();
			}
		}

		List<String> misses = new LinkedList<>();
		for (Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>> p : lhs) {
			for (Triple<Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Map<Var, Chc<Ty, En>>, Chc<Ty, En>> q : rhs) {
				if (!Util.agreeOnOverlap(p.second, (q.second))) {
					continue;
				}
				if (!Util.agreeOnOverlap(p.second, (initial))) {
					continue;
				}
				if (!Util.agreeOnOverlap(q.second, (initial))) {
					continue;
				}
				if (!p.third.equals(q.third)) {
					misses.add(p.third + " and " + q.third);
					continue;
				}
				if (expected != null && !p.third.equals(expected)) {
					continue;
				}
				if (expected != null && !q.third.equals(expected)) {
					continue;
				}
				Map<Var, Chc<Ty, En>> u = new THashMap<>(p.second);
				u.putAll(q.second);
				u.putAll(initial);

				if (!u.keySet().equals(vars)) {
					continue;
				}
				ret.add(new Quad<>(u, p.first, q.first, p.third));
			}
		}

		if (ret.size() == 0) {
			String e = (f == null) ? e0.toString() : (e0 + " = " + f);
			String msg = "Cannot infer a well-sorted term for " + e + ".\n";
			if (expected != null) {
				msg += "Expected sort: " + expected.toStringMash() + " (isType=" + expected.left + ")";
			}
			if (!misses.isEmpty()) {
				msg += "\nAttempted LHS and RHS types: " + Util.sep(misses, "\n");
			}
			if (pre == null) {
				pre = "";
			}
			throw new RuntimeException((pre + msg).trim());
		}
		if (pre == null) {
			Util.anomaly();
		}
		if (ret.size() > 1) {
			String e = (f == null) ? (e = e0.toString()) : (e0 + " = " + f);

			String msg = "Cannot infer a unique well-sorted term for " + e + ".\nCandidates: "
					+ Util.sep(ret.stream()
							.map(x -> f == null ? x.second.toStringUnambig()
									: x.second.toStringUnambig() + " = " + x.third.toStringUnambig())
							.collect(Collectors.toList()), "\n");
			if (expected != null) {
				msg += "Expected sort: " + expected.toStringMash() + " (isType=" + expected.left + ")";
			}
			throw new RuntimeException(pre + msg);
		}

		return Util.get0(ret);

	}

	public static void assertUnambig(String head, Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col) {
		int n = boolToInt(col.syms.containsKey(Sym.Sym(head)))
				+ boolToInt(col.atts.keySet().stream().map(x -> x.str).collect(Collectors.toSet()).contains(head))
				+ boolToInt(col.fks.keySet().stream().map(x -> x.str).collect(Collectors.toSet()).contains(head))
				+ boolToInt(col.gens.containsKey(Gen.Gen(head))) + boolToInt(col.sks.containsKey(Sk.Sk(head)));
		if (n == 0) {
			throw new RuntimeException(head + " is not a symbol");
		} else if (n > 1) {
			throw new RuntimeException(head + " is ambiguous");
		}

	}

	// only used for precedences with aql options
	public static Head<Ty, En, Sym, Fk, Att, Gen, Sk> toHeadNoPrim(String head,
			Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col) {
		assertUnambig(head, col);

		if (col.syms.containsKey(Sym.Sym(head))) {
			return Head.SymHead(Sym.Sym(head));
		} else if (col.gens.containsKey(Gen.Gen(head))) {
			return Head.GenHead(Gen.Gen(head));
		} else if (col.sks.containsKey(Sk.Sk(head))) {
			return Head.SkHead(Sk.Sk(head));
		}
		for (En en : col.ens) { // TODO aql won't work with ambig
			if (col.fks.containsKey(Fk.Fk(en, head))) {
				return Head.FkHead(Fk.Fk(en, head));
			}
			if (col.atts.containsKey(Att.Att(en, head))) {
				return Head.AttHead(Att.Att(en, head));
			}
		}
		throw new RuntimeException("Anomaly: please report");
	}

	private static int boolToInt(boolean b) {
		return b ? 1 : 0;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
		result = prime * result + ((args == null) ? 0 : args.hashCode());
		result = prime * result + ((head == null) ? 0 : head.hashCode());
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
		RawTerm other = (RawTerm) obj;
		if (annotation == null) {
			if (other.annotation != null)
				return false;
		} else if (!annotation.equals(other.annotation))
			return false;
		if (args == null) {
			if (other.args != null)
				return false;
		} else if (!args.equals(other.args))
			return false;
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head))
			return false;
		return true;
	}

	private RawTerm(String head, List<RawTerm> args, String annotation) {
		if (head == null) {
			throw new RuntimeException("Attempt to create raw term with null head");
		} else if (args == null) {
			throw new RuntimeException("Attempt to create raw term with null args");
		} else if (annotation != null && !args.isEmpty()) {
			throw new RuntimeException("Attempt to annotate raw term with arguments");
		}
		this.head = head;
		this.args = args;
		this.annotation = annotation;
	}

	public RawTerm(String head, String annotation) {
		this(head, new LinkedList<>(), annotation);
	}

	public RawTerm(String head, List<RawTerm> args) {
		this(head, args, null);
	}

	public RawTerm(String head) {
		this(head, Collections.emptyList(), null);
	}

	/*public RawTerm clone() {
		final List<RawTerm> args = new LinkedList<>();
		for (final RawTerm term : this.args) {
			args.add(term.clone());
		}
		return new RawTerm(this.head, args);
	}*/

	/**
	 * Make a clone (deep-copy) first.
	 * 
	 * @param tail
	 * @return
	 */
	public RawTerm append(String tail) {
		final List<RawTerm> last = new LinkedList<>();
		last.add(new RawTerm(tail));

		List<RawTerm> c = this.args;
		while (!c.isEmpty()) {
			c = c.get(0).args;
		}
		c.addAll(last);
		return this;
	}

	// TODO: aql use of toString here is ugly
	public static RawTerm fold(Set<En> entities, List<String> l, String v) {
		String head = l.get(0);
		if (!entities.contains(En.En(head))) {
			throw new RuntimeException("Not an entity: " + head + ".  Paths must start with entities.");
		}
		l = l.subList(1, l.size());

		RawTerm ret = new RawTerm(v, (String) null);
		for (String o : l) {
			ret = new RawTerm(o, Collections.singletonList(ret));
		}
		return ret;
	}

	public static RawTerm fold(List<String> l, String v) {
		RawTerm ret = new RawTerm(v, (String) null);
		for (Object o : l) {
			ret = new RawTerm(o.toString(), Collections.singletonList(ret));
		}
		return ret;
	}

	public static Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> infer2(
			List<Pair<String, String>> l, RawTerm a, RawTerm b, Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col,
			AqlJs<Ty, Sym> js) {
		Map<String, Chc<Ty, En>> Map = new THashMap<>();
		for (Pair<String, String> p : l) {
			if (Map.containsKey(p.first)) {
				throw new RuntimeException("Duplicate variable " + p.first + " in context " + Util.sep(l, ", "));
			}
			if (p.second != null) {
				Ty tt = Ty.Ty(p.second);
				if (col.tys.contains(tt) && col.ens.contains(En.En(p.second))) {
					throw new RuntimeException("Ambiguous: " + p.second + " is an entity and a type");
				} else if (col.tys.contains(tt)) {
					// Ty tt = new Ty(p.second);
					Map.put(p.first, Chc.inLeft(tt));
				} else if (col.ens.contains(En.En(p.second))) {
					En tt0 = En.En(p.second);
					Map.put(p.first, Chc.inRight(tt0));
				} else {
					throw new RuntimeException(p.second + " is neither a type nor entity");
				}
			} else {
				Map.put(p.first, null);
			}
		}
		Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> eq0 = infer1x(
				Map, a, b, null, col, "", js).first3();

		Map<Var, Chc<Ty, En>> map = new THashMap<>(Map.size());
		for (String k : Map.keySet()) {
			Var vv = Var.Var(k);
			Chc<Ty, En> v = eq0.first.get(vv);
			map.put(vv, v);
		}

		Map<Var, Chc<Ty, En>> Map2 = new THashMap<>(map);

		Triple<Map<Var, Chc<Ty, En>>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>, Term<Ty, En, Sym, Fk, Att, Gen, Sk>> tr = new Triple<>(
				Map2, eq0.second, eq0.third);
		return tr;
	}

}
