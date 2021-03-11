package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.LocStr;
import catdata.Pair;
import catdata.Quad;
import catdata.Triple;
import catdata.Util;
import catdata.aql.AqlJs;
import catdata.aql.Collage;
import catdata.aql.Head;
import catdata.aql.Term;

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

	private static Set<Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>>> infer_good(
			RawTerm e, Chc<String, String> expected, Collage<String, String, Sym, Fk, Att, String, String> col,
			String pre, AqlJs<String, Sym> js, Map<String, Chc<String, String>> vars) {
		if (e.annotation != null && !col.tys().contains((e.annotation))) {
			throw new RuntimeException(pre + "Annotation " + e.annotation + " is not a type (" + col.tys() + ").");
		}

		String vv = e.head;
		Set<Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>>> ret = new THashSet<>();
		if (vars.keySet().contains(vv) && e.annotation == null && e.args.size() == 0) {
			Term<String, String, Sym, Fk, Att, String, String> ret1 = Term.Var(vv);
			if (expected != null) {
				Map<String, Chc<String, String>> ret2 = new THashMap<>();
				ret2.put(vv, expected);
				if (Util.agreeOnOverlap(ret2, Util.fromNullable(vars))) {
					ret.add(new Triple<>(ret1, ret2, expected));
				}
			} else {
				for (String en : col.getEns()) {
					Map<String, Chc<String, String>> ret2 = new THashMap<>();
					ret2.put(vv, Chc.inRight(en));
					if (Util.agreeOnOverlap(ret2, Util.fromNullable(vars))) {
						ret.add(new Triple<>(ret1, ret2, Chc.inRight(en)));
					}
				}
				for (String ty : col.tys()) {
					Map<String, Chc<String, String>> ret2 = new THashMap<>();
					if (Util.agreeOnOverlap(ret2, Util.fromNullable(vars))) {
						ret2.put(vv, Chc.inLeft(ty));
					}
					ret.add(new Triple<>(ret1, ret2, Chc.inLeft(ty)));
				}
			}
		} else if (e.annotation == null && !(vars.keySet().contains(vv) && e.args.size() == 0)) {
			for (Entry<Sym, Pair<List<String>, String>> www : col.syms().entrySet()) {
				Sym ss = www.getKey();
				if (!ss.str.equals(e.head)) {
					continue;
				}
				List<List<Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>>>> l = new LinkedList<>();
				l.add(new LinkedList<>());
				for (int i = 0; i < e.args.size(); i++) {
					RawTerm arg = e.args.get(i);
					// if (col.syms.get(ss).first.size() > e.args.size()) {
					// throw new RuntimeException("Arity mismatch on " + e + " and " + ss);
					// }
					if (i >= col.syms().get(ss).first.size()) {
						throw new RuntimeException("Wrong number of arguments to top-level application in " + e
								+ ".  Possible cause: name clash with typeside constants.");
					}
					String ty = col.syms().get(ss).first.get(i);
					Set<Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>>> z = infer_good(
							arg, Chc.inLeft(ty), col, pre, js, vars);

					List<List<Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>>>> l2 = new LinkedList<>();
					for (List<Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>>> old : l) {
						for (Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>> y : z) {
							if (y.third.equals(Chc.inLeft(ty))) {
								l2.add(Util.append(old, Collections.singletonList(y)));
							}
						}
					}
					l = l2;
				}

				outer: for (List<Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>>> outcome : l) {

					List<Term<String, String, Sym, Fk, Att, String, String>> w = outcome.stream().map(x -> x.first)
							.collect(Collectors.toList());
					Term<String, String, Sym, Fk, Att, String, String> ret1 = Term.Sym(ss, w);
					Map<String, Chc<String, String>> ret2 = new THashMap<>();
					for (Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>> map0 : outcome) {

						if (!Util.agreeOnOverlap(map0.second, ret2)
								|| !Util.agreeOnOverlap(map0.second, Util.fromNullable(vars))) {
							continue outer;
						}
						ret2.putAll(map0.second);
					}
					for (int i = 0; i < e.args.size(); i++) {
						RawTerm arg = e.args.get(i);
						Chc<String, String> ty = Chc.inLeft(col.syms().get(ss).first.get(i));
						String v = arg.head;
						if (vars.keySet().contains(v)) {
							if (ret2.containsKey(v) && !ret2.get(v).equals(ty)) {
								continue;
							} else if (!ret2.containsKey(v)) {
								ret2.put((e.args.get(i).head), ty);
							}
						}
					}

					Chc<String, String> ret3 = Chc.inLeft(col.syms().get(ss).second);
					if (expected != null && !expected.equals(ret3)) {

					} else {
						if (Util.agreeOnOverlap(ret2, Util.fromNullable(vars))) {
							ret.add(new Triple<>(ret1, ret2, ret3));
						}
					}
				}
			}
		}

//   

		for (String en : col.getEns()) {
			if (col.fks().containsKey(Fk.Fk(en, e.head)) && e.args.size() == 1 && e.annotation == null) {
				for (Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>> outcome : infer_good(
						e.args.get(0), Chc.inRight(col.fks().get(Fk.Fk(en, e.head)).first), col, pre, js, vars)) {
					Term<String, String, Sym, Fk, Att, String, String> ret1 = Term.Fk(Fk.Fk(en, e.head), outcome.first);
					Map<String, Chc<String, String>> ret2 = new THashMap<>(outcome.second);
					String v = (e.args.get(0).head);
					Chc<String, String> ty = Chc.inRight(col.fks().get(Fk.Fk(en, e.head)).first);
					if (vars.keySet().contains(v)) {
						if (ret2.containsKey(v) && !ret2.get(v).equals(ty)) {
							continue;
						} else if (!ret2.containsKey(v)) {
							ret2.put(v, ty);
						}
					}
					Chc<String, String> ret3 = Chc.inRight(col.fks().get(Fk.Fk(en, e.head)).second);
					Chc<String, String> argt = Chc.inRight(col.fks().get(Fk.Fk(en, e.head)).first);

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

			if (col.atts().containsKey(Att.Att(en, e.head)) && e.args.size() == 1 && e.annotation == null) {
				for (Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>> outcome : infer_good(
						e.args.get(0), Chc.inRight(col.atts().get(Att.Att(en, e.head)).first), col, pre, js, vars)) {

					Term<String, String, Sym, Fk, Att, String, String> ret1 = Term.Att(Att.Att(en, e.head),
							outcome.first);
					Map<String, Chc<String, String>> ret2 = new THashMap<>(outcome.second);
					String v = (e.args.get(0).head);
					Chc<String, String> ty = Chc.inRight(col.atts().get(Att.Att(en, e.head)).first);
					if (vars.keySet().contains(v)) {
						if (ret2.containsKey(v) && !ret2.get(v).equals(ty)) {
							continue;
						} else if (!ret2.containsKey(v)) {
							ret2.put(v, ty);
						}
					}

					Chc<String, String> ret3 = Chc.inLeft(col.atts().get(Att.Att(en, e.head)).second);
					Chc<String, String> argt = Chc.inRight(col.atts().get(Att.Att(en, e.head)).first);

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

		if (col.gens().containsKey(e.head) && e.args.isEmpty() && e.annotation == null) {
			Term<String, String, Sym, Fk, Att, String, String> ret1 = Term.Gen(e.head);
			Chc<String, String> ret3 = Chc.inRight(col.gens().get(e.head));
			if (expected != null && !expected.equals(ret3)) {
			} else {
				ret.add(new Triple<>(ret1, new THashMap<>(), ret3));
			}
		}
		if (col.sks().containsKey(e.head) && e.args.isEmpty() && e.annotation == null) {
			Term<String, String, Sym, Fk, Att, String, String> ret1 = Term.Sk(e.head);
			Chc<String, String> ret3 = Chc.inLeft(col.sks().get(e.head));
			if (expected != null && !expected.equals(ret3)) {
			} else {
				ret.add(new Triple<>(ret1, new THashMap<>(), ret3));
			}
		} else if (e.args.isEmpty() && e.annotation != null) {
			String ty = (e.annotation);
			Term<String, String, Sym, Fk, Att, String, String> ret1 = Term.Obj(js.parse(ty, e.head), ty);
			Chc<String, String> ret3 = Chc.inLeft(ty);
			if (expected != null && !expected.equals(ret3)) {
			} else {
				ret.add(new Triple<>(ret1, new THashMap<>(), ret3));
			}
		} else
		// as primitive - if none of the above, but not a var
		if (e.args.isEmpty() && e.annotation == null && ret.isEmpty() && !vars.containsKey(vv) ) {
			// System.out.println("examining " + e + " expected " + expected);
			for (String ty : col.tys()) {
				Chc<String, String> ret3 = Chc.inLeft(ty);
				if (expected == null || !expected.equals(ret3)) {
					continue;
				}
				try {

					// if (!expected.equals(ret3)) {
					// } else {
					Term<String, String, Sym, Fk, Att, String, String> ret1 = Term.Obj(js.parse(ty, e.head), ty);
					ret.add(new Triple<>(ret1, new THashMap<>(), ret3));
					// }
				} catch (Exception ex) {
					// ex.printStackTrace();
					// throw new RuntimeException("On " + e + ", expected sort is " + expected + "
					// but an error is thrown when parsing it: " + ex.getLocalizedMessage() );

				}
			}
		}

		return ret;
	}

	private static boolean isSymbolAll(Collage<String, String, Sym, Fk, Att, String, String> col, String s) {
		return col.syms().keySet().stream().map(x -> x.str).collect(Collectors.toSet()).contains(s)
				|| col.fks().keySet().stream().map(x -> x.str).collect(Collectors.toSet()).contains(s)
				|| col.atts().keySet().stream().map(x -> x.str).collect(Collectors.toSet()).contains(s)
				|| col.gens().containsKey(s) || col.sks().containsKey(s);
	}

	private static String truncateHard(String s) {
		if (s.length() > 16*1024) {
			return s.substring(0, 16*1024) + " ... ";
		}
		return s;
	}

	public synchronized static Quad<Map<String, Chc<String, String>>, Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>, Chc<String, String>> infer1x(
			Map<String, Chc<String, String>> map0, RawTerm e0, RawTerm f, Chc<String, String> expected,
			Collage<String, String, Sym, Fk, Att, String, String> col, String pre, AqlJs<String, Sym> js) {
		Set<Quad<Map<String, Chc<String, String>>, Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>, Chc<String, String>>> ret = new THashSet<>();

		Map<String, Chc<String, String>> vars0 = new THashMap<>(map0.size());
		for (String s : map0.keySet()) {
			vars0.put(s, map0.get(s));

		}
		Set<String> vars = new THashSet<>(map0.keySet());

		Map<String, Chc<String, String>> initial = new THashMap<>();
		for (String v : vars) {
			if (map0.get(v) != null) {
				initial.put(v, map0.get(v));
			}
		}
		Set<Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>>> lhs = infer_good(
				e0, expected, col, pre, js, vars0);

		if (lhs.isEmpty()) {
			String msg = "Cannot infer a well-sorted term for " + e0 + ".\n";
			if (!vars.contains(e0.head) && !isSymbolAll(col, e0.head) && e0.annotation == null) {
				msg += "Undefined (or not java-parseable) symbol: " + e0.head + ".\n";
				msg += "\nAvailable symbols:\n\t"
						+ truncateHard(Util.sep(Util.closest(e0.head, col.allSymbolsAsStrings()), "\n\t"));

			}
			if (expected != null) {
				String msg2 = expected.left ? "type" : "entity";
				msg += "\nExpected " + msg2 + ": " + expected.toStringMash();
			}

			throw new RuntimeException(pre + msg);
		} else if (lhs.size() == 1 && expected == null) {
			expected = Util.get0(lhs).third; // TODO: also reverse
		}

		Set<Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>>> rhs;
		if (f == null) {
			rhs = lhs;
		} else {
			rhs = infer_good(f, expected, col, pre, js, vars0);
		}

		if (rhs.isEmpty()) {
			if (f == null) {
				return Util.anomaly();
			}
			String msg = "Cannot infer a well-sorted term for " + f + ".\n";
			if (!vars.contains((f.head)) && !isSymbolAll(col, f.head) && f.annotation == null) {
				msg += "Undefined (or not java-parseable) symbol: " + f.head + "\n";
				msg += "\nAvailable symbols:\n\t"
						+ truncateHard(Util.sep(Util.closest(f.head, col.allSymbolsAsStrings()), "\n\t"));
				// TODO aql merge this error message with the one above it
			}
			if (expected != null) {
				String msg2 = expected.left ? "type" : "entity";
				msg += "Expected " + msg2 + ": " + expected.toStringMash();
			}
			throw new RuntimeException(pre + msg);
		}

		for (Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>> outcome : lhs) {
			if (!vars.containsAll(outcome.second.keySet())) {
				Util.anomaly();
			}
		}
		for (Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>> outcome : rhs) {
			if (!vars.containsAll(outcome.second.keySet())) {
				Util.anomaly();
			}
		}

		List<String> misses = new LinkedList<>();
		for (Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>> p : lhs) {
			for (Triple<Term<String, String, Sym, Fk, Att, String, String>, Map<String, Chc<String, String>>, Chc<String, String>> q : rhs) {
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
				Map<String, Chc<String, String>> u = new THashMap<>(p.second);
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

			String msg = "Cannot infer a unique well-sorted term for " + e + "\nCandidates:\n\n"
					+ Util.sep(ret.stream()
							.map(x -> f == null ? x.second.toStringUnambig()
									: x.second.toStringUnambig() + " = " + x.third.toStringUnambig())
							.collect(Collectors.toSet()), "\n");
			if (expected != null) {
				msg += "\n\nExpected sort: " + expected.toStringMash() + " (which should be a type:" + expected.left
						+ ")";
			}
			throw new RuntimeException(pre + msg);
		}

		return Util.get0(ret);

	}

	public static void assertUnambig(String head, Collage<String, String, Sym, Fk, Att, String, String> col) {
		if (col == null) {
			throw new RuntimeException("No collage within which to interpret a precedence.");
		}
		int n = boolToInt(col.syms().keySet().stream().map(x -> x.str).collect(Collectors.toSet()).contains(head))
				+ boolToInt(col.atts().keySet().stream().map(x -> x.str).collect(Collectors.toSet()).contains(head))
				+ boolToInt(col.fks().keySet().stream().map(x -> x.str).collect(Collectors.toSet()).contains(head))
				+ boolToInt(col.gens().containsKey(head)) + boolToInt(col.sks().containsKey(head));
		if (n == 0) {
			Util.anomaly();
			throw new RuntimeException(head + " is not a symbol (in the simplified theory) " + col);
		} else if (n > 1) {
			throw new RuntimeException(head + " is ambiguous in " + col);
		}

	}

	// only used for precedences with aql options
	public static Head<String, String, Sym, Fk, Att, String, String> toHeadNoPrim(String head,
			Collage<String, String, Sym, Fk, Att, String, String> col) {
		assertUnambig(head, col);

		for (Entry<Sym, Pair<List<String>, String>> k : col.syms().entrySet()) {
			if (k.getKey().str.equals(head)) {
				return Head.SymHead(k.getKey());
			}
		}
		if (col.gens().containsKey(head)) {
			return Head.GenHead(head);
		} else if (col.sks().containsKey(head)) {
			return Head.SkHead(head);
		}
		for (String en : col.getEns()) { // TODO aql won't work with ambig
			if (col.fks().containsKey(Fk.Fk(en, head))) {
				return Head.FkHead(Fk.Fk(en, head));
			}
			if (col.atts().containsKey(Att.Att(en, head))) {
				return Head.AttHead(Att.Att(en, head));
			}
		}
		return Util.anomaly();
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

	/*
	 * public RawTerm clone() { final List<RawTerm> args = new LinkedList<>(); for
	 * (final RawTerm term : this.args) { args.add(term.clone()); } return new
	 * RawTerm(this.head, args); }
	 */

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
	public static RawTerm fold(Set<String> entities, List<String> l, String v) {
		String head = l.get(0);
		if (!entities.contains((head))) {
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

	public static Triple<Map<String, Chc<String, String>>, Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>> infer2(
			List<Pair<String, String>> l, RawTerm a, RawTerm b,
			Collage<String, String, Sym, Fk, Att, String, String> col, AqlJs<String, Sym> js) {
		Map<String, Chc<String, String>> Map = new THashMap<>();
		for (Pair<String, String> p : l) {
			if (Map.containsKey(p.first)) {
				throw new RuntimeException("Duplicate variable " + p.first + " in context " + Util.sep(l, ", "));
			}
			if (p.second != null) {
				String tt = (p.second);
				if (col.tys().contains(tt) && col.getEns().contains((p.second))) {
					throw new RuntimeException("Ambiguous: " + p.second + " is an entity and a type");
				} else if (col.tys().contains(tt)) {
					// Ty tt = new Ty(p.second);
					Map.put(p.first, Chc.inLeft(tt));
				} else if (col.getEns().contains((p.second))) {
					String tt0 = (p.second);
					Map.put(p.first, Chc.inRight(tt0));
				} else {
					throw new RuntimeException(p.second + " is neither a type nor entity");
				}
			} else {
				Map.put(p.first, null);
			}
		}
		Triple<Map<String, Chc<String, String>>, Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>> eq0 = infer1x(
				Map, a, b, null, col, "", js).first3();

		Map<String, Chc<String, String>> map = new THashMap<>(Map.size());
		for (String k : Map.keySet()) {
			String vv = (k);
			Chc<String, String> v = eq0.first.get(vv);
			map.put(vv, v);
		}

		Map<String, Chc<String, String>> Map2 = new THashMap<>(map);

		Triple<Map<String, Chc<String, String>>, Term<String, String, Sym, Fk, Att, String, String>, Term<String, String, Sym, Fk, Att, String, String>> tr = new Triple<>(
				Map2, eq0.second, eq0.third);
		return tr;
	}

	public static RawTerm foldl1(List<RawTerm> b, String s) {
		RawTerm x = b.get(0);
		return foldl(x, b.subList(1, b.size()), s);
	}

	public static RawTerm foldl(RawTerm x, List<RawTerm> l, String s) {
		for (RawTerm y : l) {
			x = new RawTerm(s, Util.list(x, y));
		}
		return x;
	}

}
