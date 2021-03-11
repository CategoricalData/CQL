package catdata.aql.exp;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Constraints;
import catdata.aql.ED;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Pragma;
import catdata.aql.Term;
import catdata.aql.fdm.Row;
import gnu.trove.map.hash.THashMap;

@SuppressWarnings("hiding")
public final class PragmaExpCheck<X, Y> extends PragmaExp {
	public InstExp<String, String, X, Y> I;
	public EdsExp C;
	public Map<String, String> ops;

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		C.map(f);
		I.map(f);
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	public PragmaExpCheck(InstExp<String, String, X, Y> i, EdsExp c, List<Pair<String, String>> o) {
		I = i;
		C = c;
		ops = Util.toMapSafely(o);
	}

	@Override
	public Map<String, String> options() {
		return ops;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.check_command_export_file);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((C == null) ? 0 : C.hashCode());
		result = prime * result + ((I == null) ? 0 : I.hashCode());
		result = prime * result + ((I == null) ? 0 : ops.hashCode());
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
		PragmaExpCheck<?, ?> other = (PragmaExpCheck<?, ?>) obj;
		if (C == null) {
			if (other.C != null)
				return false;
		} else if (!C.equals(other.C))
			return false;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		if (ops == null) {
			if (other.ops != null)
				return false;
		} else if (!ops.equals(other.ops))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "check " + C + " " + I;
	}

	@Override
	public synchronized Pragma eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		return new Pragma() {
			Instance<String, String, Sym, Fk, Att, String, String, X, Y> J;

			@Override
			public synchronized void execute() {
				J = I.eval(env, isC);
				Constraints q = C.eval(env, isC);
				Collection<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>, Chc<String, String>>>> t = q
						.triggers(J, env.defaults);
				String outPath = (String) new AqlOptions(ops, env.defaults).getOrDefault(AqlOption.check_command_export_file);
				if (!outPath.isBlank()) {
					try {
						Util.writeFile(printCsv(q, t, J), outPath);
					} catch (Exception ex) {
						ex.printStackTrace();
						throw new RuntimeException(ex);
					}

				} else {
					if (!t.isEmpty()) {
						throw new RuntimeException("Not satisfied.\n\n" + printTriggers(q, t, J));
					}
				}
			}

			private String printCsv(Constraints q,
					Collection<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>, Chc<String, String>>>> t,
					@SuppressWarnings("unused") Instance<String, String, Sym, Fk, Att, String, String, X, Y> J) {
				Map<Integer, List<Row<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>, Chc<String, String>>>> m = new THashMap<>(
						t.size());

				for (Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>, Chc<String, String>>> p : t) {
					if (!m.containsKey(p.first)) {
						m.put(p.first, new LinkedList<>());
					}
					List<Row<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>, Chc<String, String>>> l = m
							.get(p.first);
					l.add(p.second);
				}

				JsonObjectBuilder xxx = Json.createObjectBuilder();
				for (Integer ed : m.keySet()) {
					JsonArrayBuilder yyy = Json.createArrayBuilder();
					for (Row<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>, Chc<String, String>> x : m
							.get(ed)) {
						JsonObjectBuilder zzz = Json.createObjectBuilder();
						for (Entry<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>> entry : x.asMap()
								.entrySet()) {
							var x2 = entry.getValue();
							if (x2.left) {
								Chc<String, String> z = q.eds.get(ed).As.get(entry.getKey());
								if (z.left) {
									zzz.add(entry.getKey(), x2.r.toString());
								} else {
									String en = z.r;
									X xx = x2.l;
									JsonObjectBuilder jjj = Json.createObjectBuilder();
									for (Att att : J.schema().attsFrom(en)) {
										jjj.add(att.str, J.algebra().reprT(J.algebra().att(att, xx)).toString());
									}
									zzz.add(entry.getKey(), jjj);
								}
							} else {
								zzz.add(entry.getKey(), x2.r.toString());
							}
						}
						yyy.add(zzz);
					}
					xxx.add(q.eds.get(ed).toString(), yyy);
				}

				return xxx.build().toString();
			}

			private String thePrintFn(
					Row<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>, Chc<String, String>> list, ED ed) {

				return "\t" + Util.sep(list.asMap().entrySet(), ",  ",
						x -> x.getKey() + " -> " + helper(x.getKey(), x.getValue(), ed));
			}

			private String helper(String v, Chc<X, Term<String, String, Sym, Fk, Att, String, String>> x, ED ed) {
				if (x.left) {
					Chc<String, String> z = ed.As.get(v);
					if (z.left) {
						return x.r.toString();
					}
					String en = z.r;
					List<String> w = new LinkedList<>();
					X xx = x.l;
					for (Att att : J.schema().attsFrom(en)) {
						w.add(att + ":" + J.algebra().reprT(J.algebra().att(att, xx)).toString());
					}
					return (String) J.algebra().printX(en, xx) + "  [" + Util.sep(w, ", ") + "]";

				}
				return x.r.toString();
			}

			private String printTriggers(Constraints q,
					Collection<Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>, Chc<String, String>>>> t,
					@SuppressWarnings("unused") Instance<String, String, Sym, Fk, Att, String, String, X, Y> J) {
				Map<Integer, List<Row<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>, Chc<String, String>>>> m = new THashMap<>(
						t.size());

				for (Pair<Integer, Row<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>, Chc<String, String>>> p : t) {
					if (!m.containsKey(p.first)) {
						m.put(p.first, new LinkedList<>());
					}
					List<Row<String, Chc<X, Term<String, String, Sym, Fk, Att, String, String>>, Chc<String, String>>> l = m
							.get(p.first);
					l.add(p.second);
				}
				String ret = "";
				for (Integer ed : m.keySet()) {
					ret += "======================\n";
					ret += "On constraint\n\n" + q.eds.get(ed).toString() + "\n\nthe failing triggers are:\n\n";
					ret += Util.sep(Util.map(m.get(ed), x -> thePrintFn(x, q.eds.get(ed))), "\n");
					ret += "\n";
				}
				if (ret.equals("")) {
					return "Passed";
				}
				return ret;
			}

			@Override
			public String toString() {
				return "Satisfies";
			}

		};
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(C.deps(), I.deps());
	}

	@Override
	public Unit type(AqlTyping G) {
		SchExp x = I.type(G);
		SchExp y = C.type(G);
		if (!x.equals(y)) {
			throw new RuntimeException("Schema of instance: " + x + " does not match constraint schema " + y);
		}
		return Unit.unit;
	}

}