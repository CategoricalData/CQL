package catdata.provers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import gnu.trove.set.hash.THashSet;

public class KBTheory<T, C, V> {

	public final KBExpFactory<T, C, V> factory;

	@Override
	public String toString() {
		return "KBTheory [tys=" + tys + ", syms=" + syms + ", eqs=" + eqs + "]";
	}

	public KBTheory(KBTheory<T, C, V> kb2) {
		this(kb2.factory);
		this.tys.addAll(kb2.tys);
		this.syms.putAll(kb2.syms);
		this.eqs.addAll(kb2.eqs);
		// validate(); // TODO aql disable for production
	}

	public void add(KBTheory<T, C, V> kb2) {
		this.tys.addAll(kb2.tys);
		this.syms.putAll(kb2.syms);
		this.eqs.addAll(kb2.eqs);
		// validate(); // TODO aql disable for production
	}

	public synchronized void validate() {
		for (C sym : syms.keySet()) {
			Pair<List<T>, T> T = syms.get(sym);
			if (!tys.contains(T.second)) {
				throw new RuntimeException("On symbol " + sym + ", the return Type " + T.second + " is not declared.");
			}
			for (T t : T.first) {
				if (!tys.contains(t)) {
					throw new RuntimeException("On symbol " + sym + ", the argument Type " + t + " is not declared.");
				}
			}

		}
		for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : eqs) {
			// check that the context is valid for each eq
			Set<T> used_Ts = (new THashSet<>(eq.first.values()));
			used_Ts.removeAll(tys);
			if (!used_Ts.isEmpty()) {
				throw new RuntimeException(
						"In equation " + eq + ", context uses types " + used_Ts + " that are not declared.");
			}
			// check lhs and rhs Types match in all eqs
			T lhs = eq.second.type(syms, eq.first);
			T rhs = eq.third.type(syms, eq.first);
			if (!lhs.equals(rhs)) {
				throw new RuntimeException("In equation " + eq + ", lhs type is " + lhs + " but rhs type is " + rhs);
			}
		}

	}

	public final Collection<T> tys;
	public Map<C, Pair<List<T>, T>> syms;

	public final Collection<Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>>> eqs;

	public KBTheory(KBExpFactory<T, C, V> factory) {
		this.tys = (new THashSet<>());
		this.syms = Util.mk();
		this.eqs = (new THashSet<>());
		this.factory = factory;
	}
	/*
	 * public KBTheory(Collection<T> tys, Map<C, Pair<List<T>, T>> syms,
	 * Collection<Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>>> eqs,
	 * KBExpFactoryImpl<T,C,V> factory) { this.tys = tys; this.syms = syms; this.eqs
	 * = eqs; this.factory = factory; validate(); // TODO aql disable for production
	 * }
	 */

	public void inhabGen(Set<T> inhabited) {
		while (inhabGen1(inhabited))
			;
	}

	private boolean inhabGen1(Set<T> ret) {
		boolean changed = false;
		outer: for (C c : syms.keySet()) {
			for (T t : syms.get(c).first) {
				if (!ret.contains(t)) {
					continue outer;
				}
			}
			changed = changed | ret.add(syms.get(c).second);
		}
		return changed;
	}

	private final Map<Object, String> isoC1 = Util.mk();
	private final Map<String, Object> isoC2 = Util.mk();

	private final Map<Object, String> isoV1 = Util.mk();
	private final Map<String, Object> isoV2 = Util.mk();

	private final Map<Object, String> isoT1 = Util.mk();
	private final Map<String, Object> isoT2 = Util.mk();

	private int i = 0;

	public final synchronized String convert(KBExp<C, V> e) {
		if (e.isVar()) {
			return convertV(e.getVar());
		}
		List<String> l = new ArrayList<>(e.getArgs().size());
		for (KBExp<C, V> arg : e.getArgs()) {
			l.add(convert(arg));
		}
		if (l.isEmpty()) {
			return convertC(e.f());
		}
		return convertC(e.f()) + "(" + Util.sep(l, ",") + ")";
	}

	public final synchronized String convertV(V e) {
		if (isoV1.containsKey(e)) {
			return isoV1.get(e);
		}
		isoV1.put(e, "V" + i);
		isoV2.put("V" + i, e);
		i++;

		return isoV1.get(e);
	}

	public final synchronized String convertC(C e) {
		if (isoC1.containsKey(e)) {
			return isoC1.get(e);
		}
		isoC1.put(e, "s" + i);
		isoC2.put("s" + i, e);
		i++;

		return isoC1.get(e);
	}

	public final synchronized String convertT(T e) {
		if (isoT1.containsKey(e)) {
			return isoT1.get(e);
		}
		isoT1.put(e, "p" + i);
		isoT2.put("p" + i, e);
		i++;

		return isoT1.get(e);
	}

	// private String tptp = null;

	public synchronized String tptp(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
		StringBuffer sb = new StringBuffer(tptp());
		sb.append("fof(eq" + 0 + ",conjecture,(");
		if (!ctx.isEmpty()) {
			sb.append("! [");
			sb.append(Util.sep(ctx.keySet().stream().map(this::convertV).collect(Collectors.toList()), ","));
			sb.append("] : (");
		}
		sb.append("($true");
		for (V v : ctx.keySet()) {
			sb.append(" & " + convertT(ctx.get(v)) + "(" + convertV(v) + ")");
		}
		sb.append(") => ");

		sb.append(convert(lhs) + " = " + convert(rhs) + "))");
		if (!ctx.isEmpty()) {
			sb.append(")");
		}
		sb.append(".");
		sb.append(System.lineSeparator());
		return sb.toString();
	}

	public synchronized String tptp() {
		// if (tptp != null) {
		// return tptp;
		// }

		int j = 1; // 0 reserved for other tptp fn
		StringBuilder sb = new StringBuilder();
		for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : eqs) {
			Map<V, T> ctx = eq.first;
			sb.append("fof(eq" + j + ",axiom,(");
			if (!ctx.isEmpty()) {
				sb.append("! [");
				sb.append(Util.sep(ctx.keySet().stream().map(this::convertV).collect(Collectors.toList()), ","));
				sb.append("] : (");
			}
			sb.append("($true");
			for (V v : ctx.keySet()) {
				sb.append(" & " + convertT(ctx.get(v)) + "(" + convertV(v) + ")");
			}
			sb.append(") => ");
			sb.append(convert(eq.second) + " = " + convert(eq.third) + "))");
			if (!ctx.isEmpty()) {
				sb.append(")");
			}
			sb.append(".");
			sb.append(System.lineSeparator());
			j++;
		}

		String tptp = sb.toString();
		return tptp;
	}

	// todo: S(x) -> x=c1 or ... or Eyz. x = f(y,z)?
	// String preamble;
	public synchronized String tptp_preamble() {
		// if (preamble != null) {
		// return preamble;
		// }
		int j = 0; // 0 reserved for other tptp fn
		StringBuilder sb = new StringBuilder();

		for (T t : tys) {
			List<String> y = new LinkedList<>();
			for (T t2 : tys) {
				if (t.equals(t2)) {
					continue;
				}
				y.add("(~" + convertT(t2) + "(X))");
			}

			sb.append("fof(sort" + (j++) + ",axiom,(");
			sb.append("! [ X ] ");
			sb.append(" : (");
			sb.append(convertT(t) + "(X) => (");
			if (y.isEmpty()) {
				sb.append("$true");
			} else {
				sb.append(Util.sep(y, " & "));
			}

			sb.append(")))).\n");
		}

		for (C c : syms.keySet()) {
			sb.append("fof(sym" + j + ",axiom,(");
			List<String> l = new LinkedList<>();
			int i = 0;
			for (@SuppressWarnings("unused")
			T t : syms.get(c).first) {
				String x = "X" + (i++);
				l.add(x);
			}

			if (!syms.get(c).first.isEmpty()) {
				sb.append("! [ ");
				sb.append(Util.sep(l, ","));
				sb.append(" ] : (");
			}

			i = 0;
			sb.append("($true");
			for (T t : syms.get(c).first) {
				sb.append(" & ");
				String x = "X" + (i++);
				sb.append(convertT(t) + "(" + x + ")");
			}
			sb.append(") => ");
			sb.append(convertT(syms.get(c).second) + "(" + convertC(c) + "(" + Util.sep(l, ",") + "))");
			if (!syms.get(c).first.isEmpty()) {
				sb.append(")");
			}
			sb.append(")).");

			sb.append(System.lineSeparator());
			j++;
		}

		String preamble = sb.toString();
		return preamble;
	}

	////////////////////////////////////////////////////////////////////////////////////

	private String tptp_cnf = null;

	public synchronized String tptp_cnf() {
		// if (tptp_cnf != null) {
		// return tptp;
		// }

		int j = 1; // 0 reserved for other tptp fn
		StringBuilder sb = new StringBuilder();
		for (Triple<Map<V, T>, KBExp<C, V>, KBExp<C, V>> eq : eqs) {
			// Map<V, T> ctx = eq.first;
			sb.append("cnf(eq" + j + ",axiom,(");
			sb.append(convert(eq.second) + " = " + convert(eq.third) + "))");
			sb.append(".");
			sb.append(System.lineSeparator());
			j++;
		}

		tptp_cnf = sb.toString();
		return tptp_cnf;
	}

}
