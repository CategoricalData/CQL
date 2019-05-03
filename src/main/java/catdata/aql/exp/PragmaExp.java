package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Constraints;
import catdata.aql.ED;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Pragma;
import catdata.aql.Term;
import catdata.aql.fdm.JdbcPragma;
import catdata.aql.fdm.JsPragma;
import catdata.aql.fdm.ProcPragma;
import catdata.aql.fdm.Row;
import catdata.aql.fdm.ToCsvPragmaInstance;
import catdata.aql.fdm.ToCsvPragmaTransform;
import catdata.aql.fdm.ToJdbcPragmaInstance;
import catdata.aql.fdm.ToJdbcPragmaQuery;
import catdata.aql.fdm.ToJdbcPragmaTransform;
import catdata.graph.DMG;
import catdata.graph.Matcher;
import catdata.graph.NaiveMatcher;
import catdata.graph.SimilarityFloodingMatcher;
import gnu.trove.map.hash.THashMap;

public abstract class PragmaExp extends Exp<Pragma> {

	@Override
	public Kind kind() {
		return Kind.PRAGMA;
	}

	@Override
	public Exp<Pragma> Var(String v) {
		Exp<Pragma> ret = new PragmaExpVar(v);
		return ret;
	}

	@Override
	public Optional<Chc<String, Object>> type(AqlTyping G) {
		return Optional.empty();
	}

	public static interface PragmaExpCoVisitor<R, P, E extends Exception> {
		public <Gen, Sk, X, Y> PragmaExpConsistent<Gen, Sk, X, Y> visitPragmaExpConsistent(P params, R exp) throws E;

		public <Gen, Sk, X, Y> PragmaExpCheck<Gen, Sk, X, Y> visitPragmaExpCheck(P params, R exp) throws E;

		public PragmaExpMatch visitPragmaExpMatch(P params, R exp) throws E;

		public PragmaExpSql visitPragmaExpSql(P params, R exp) throws E;

		public <Gen, Sk, X, Y> PragmaExpToCsvInst<Gen, Sk, X, Y> visitPragmaExpToCsvInst(P params, R exp) throws E;

		public PragmaExpVar visitPragmaExpVar(P params, R exp) throws E;

		public PragmaExpJs visitPragmaExpJs(P params, R exp) throws E;

		public PragmaExpProc visitPragmaExpProc(P params, R exp) throws E;

		public <Gen, Sk, X, Y> PragmaExpToJdbcInst<Gen, Sk, X, Y> visitPragmaExpToJdbcInst(P params, R exp) throws E;

		public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> PragmaExpToJdbcTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> visitPragmaExpToJdbcTrans(
				P params, R exp) throws E;

		public PragmaExpToJdbcQuery visitPragmaExpToJdbcQuery(P params, R exp) throws E;

		public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> PragmaExpToCsvTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> visitPragmaExpToCsvTrans(
				P params, R exp) throws E;

		public PragmaExpCheck2 visitPragmaExpCheck2(P params, R exp) throws E;
	}

	public static interface PragmaExpVisitor<R, P, E extends Exception> {
		public <Gen, Sk, X, Y> R visit(P params, PragmaExpConsistent<Gen, Sk, X, Y> exp) throws E;

		public <Gen, Sk, X, Y> R visit(P params, PragmaExpCheck<Gen, Sk, X, Y> exp) throws E;

		public R visit(P params, PragmaExpMatch exp) throws E;

		public R visit(P params, PragmaExpSql exp) throws E;

		public <Gen, Sk, X, Y> R visit(P params, PragmaExpToCsvInst<Gen, Sk, X, Y> exp) throws E;

		public R visit(P params, PragmaExpVar exp) throws E;

		public R visit(P params, PragmaExpJs exp) throws E;

		public R visit(P params, PragmaExpProc exp) throws E;

		public <Gen, Sk, X, Y> R visit(P params, PragmaExpToJdbcInst<Gen, Sk, X, Y> exp) throws E;

		public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> R visit(P params,
				PragmaExpToJdbcTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> exp) throws E;

		public R visit(P params, PragmaExpToJdbcQuery exp) throws E;

		public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> R visit(P params,
				PragmaExpToCsvTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> exp) throws E;

		public R visit(P params, PragmaExpCheck2 exp) throws E;
	}

	public abstract <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E;

	/////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("hiding")
	public static final class PragmaExpConsistent<Gen, Sk, X, Y> extends PragmaExp {
		public final InstExp<Gen, Sk, X, Y> I;

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public boolean isVar() {
			return true;
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			I.map(f);
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		public PragmaExpConsistent(InstExp<Gen, Sk, X, Y> i) {
			I = i;
		}

		@Override
		public int hashCode() {
			return I.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PragmaExpConsistent<?, ?, ?, ?> other = (PragmaExpConsistent<?, ?, ?, ?>) obj;
			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "assert_consistent " + I;
		}

		@Override
		public Pragma eval0(AqlEnv env, boolean isC) {
			return new Pragma() {

				@Override
				public void execute() {
					Instance<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, Gen, Sk, X, Y> J = I
							.eval(env, isC);
					if (!J.algebra().hasFreeTypeAlgebra()) {
						throw new RuntimeException(
								"Not necessarily consistent: type algebra is\n\n" + J.algebra().talg());
					}
				}

				@Override
				public String toString() {
					return "Consistent";
				}

			};
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return I.deps();
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

	}

	/////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("hiding")
	public static final class PragmaExpCheck<Gen, Sk, X, Y> extends PragmaExp {
		public InstExp<Gen, Sk, X, Y> I;
		public EdsExp C;

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			C.map(f);
			I.map(f);
		}

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		public PragmaExpCheck(InstExp<Gen, Sk, X, Y> i, EdsExp c) {
			I = i;
			C = c;
		}

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((C == null) ? 0 : C.hashCode());
			result = prime * result + ((I == null) ? 0 : I.hashCode());
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
			PragmaExpCheck<?, ?, ?, ?> other = (PragmaExpCheck<?, ?, ?, ?>) obj;
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

				@Override
				public synchronized void execute() {
					Instance J = I.eval(env, isC);
					Constraints q = C.eval(env, isC);
					Collection t = q.triggers(J, env.defaults);
					if (!t.isEmpty()) {
						throw new RuntimeException("Not satisfied.\n\n" + printTriggers(q, t, J));
					}
				}

				private String printTriggers(Constraints q,
						Collection<Pair<Integer, Row<String, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>> t,
						@SuppressWarnings("unused") Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> J) {
					Map<Integer, List<Row<String, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>>> m = new THashMap<>(
							t.size());
					for (Pair<Integer, Row<String, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>> p : t) {
						if (!m.containsKey(p.first)) {
							m.put(p.first, new LinkedList<>());
						}
						List<Row<String, Chc<X, Term<Ty, En, Sym, Fk, Att, Gen, Sk>>>> l = m.get(p.first);
						l.add(p.second);

					}
					String ret = "";
					for (Integer ed : m.keySet()) {
						ret += "======================\n";
						ret += "On constraint\n\n" + q.eds.get(ed).toString() + "\n\nthe failing triggers are:\n\n";
						ret += Util.sep(m.get(ed).iterator(), "\n",
								r -> Util.sep(r.map((z, e) -> z.toStringMash()).asMap(), "->", ", "));
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

	}

	///////////////////////////////////////////////////////////////////////////////////////////////

	public static final class PragmaExpMatch extends PragmaExp {
		public final String String;
		public final Map<String, String> options;

		public final GraphExp src;
		public final GraphExp dst;

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			src.map(f);
			dst.map(f);
		}

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public Map<String, String> options() {
			return options;
		}

		public PragmaExpMatch(String String, GraphExp src, GraphExp dst, List<Pair<String, String>> options) {
			this.String = String;
			this.options = Util.toMapSafely(options);
			this.src = src;
			this.dst = dst;
		}

		@Override
		public Pragma eval0(AqlEnv env, boolean isC) {
			if (isC) {
				throw new IgnoreException();
			}
			DMG<String, String> src0 = src.eval(env, false).dmg;
			DMG<String, String> dst0 = dst.eval(env, false).dmg;

			return new Pragma() {

				private String s;

				@Override
				public void execute() {
					toString();
				}

				@Override
				public String toString() {
					if (s != null) {
						return s;
					}
					// TODO aql eventually, this should not catch the exception
					Matcher<String, String, String, String, ?> ret0;
					try {
						switch (String) {
						case "naive":
							ret0 = new NaiveMatcher<>(src0, dst0, options);
							break;
						case "sf":
							ret0 = new SimilarityFloodingMatcher<>(src0, dst0, options);
							break;
						default:
							throw new RuntimeException(
									"Please use naive or sf for String match desired, not " + String);
						}
						s = ret0.bestMatch.toString();
					} catch (Exception e) {
						// e.printStackTrace();
						s = e.getMessage();
						throw e;
					}
					return s;
				}
			};
		}

		@Override
		public String toString() {
			return "match " + Util.quote(String) + " : " + src + " -> " + dst;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Util.union(src.deps(), dst.deps());
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + ((options == null) ? 0 : options.hashCode());
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			result = prime * result + ((String == null) ? 0 : String.hashCode());
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
			PragmaExpMatch other = (PragmaExpMatch) obj;
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			if (String == null) {
				if (other.String != null)
					return false;
			} else if (!String.equals(other.String))
				return false;
			return true;
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

	}

	/////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class PragmaExpSql extends PragmaExp {
		private final List<String> sqls;

		private final String jdbcString;

	//	private final String clazz;

		private final Map<String, String> options;

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
		}

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public Map<String, String> options() {
			return options;
		}

		public PragmaExpSql( String jdbcString, List<String> sqls, List<Pair<String, String>> options) {
		//	this.clazz = clazz;
			this.jdbcString = jdbcString;
			this.options = Util.toMapSafely(options);
			this.sqls = sqls;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			//result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
			result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
			result = prime * result + ((options == null) ? 0 : options.hashCode());
			result = prime * result + ((sqls == null) ? 0 : sqls.hashCode());
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
			PragmaExpSql other = (PragmaExpSql) obj;
			if (jdbcString == null) {
				if (other.jdbcString != null)
					return false;
			} else if (!jdbcString.equals(other.jdbcString))
				return false;
			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			if (sqls == null) {
				if (other.sqls != null)
					return false;
			} else if (!sqls.equals(other.sqls))
				return false;
			return true;
		}

		@Override
		public Pragma eval0(AqlEnv env, boolean isC) {
			if (isC) {
				throw new IgnoreException();
			}
			String toGet = jdbcString;
			AqlOptions op = new AqlOptions(options, null, env.defaults);
			if (jdbcString.trim().isEmpty()) {
				toGet = (String) op.getOrDefault(AqlOption.jdbc_default_string);
			}
			return new JdbcPragma(toGet, sqls);
		}

		@Override
		public String makeString() {
			final StringBuilder sb = new StringBuilder().append("exec_jdbc ").append(" ")
					.append(Util.quote(jdbcString)).append(" {")
					.append(Util.sep(sqls.stream().map(Util::quote).collect(Collectors.toList()), "\n"));

			if (!options.isEmpty()) {
				sb.append("\n\toptions").append(Util.sep(options, "\n\t\t", " = "));
			}
			return sb.append("}").toString();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			set.add(AqlOption.jdbc_default_class);
			set.add(AqlOption.jdbc_default_string);
		}

	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("hiding")
	public static final class PragmaExpToCsvInst<Gen, Sk, X, Y> extends PragmaExp {

		public final String file;

		public final Map<String, String> options;

		public final InstExp<Gen, Sk, X, Y> inst;

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			inst.map(f);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			set.add(AqlOption.csv_field_delim_char);
			set.add(AqlOption.csv_escape_char);
			set.add(AqlOption.csv_quote_char);
			set.add(AqlOption.csv_file_extension);
			set.add(AqlOption.csv_generate_ids);
			set.add(AqlOption.csv_emit_ids);
			set.add(AqlOption.csv_prepend_entity);
			set.add(AqlOption.prepend_entity_on_ids);
			set.add(AqlOption.id_column_name);

		}

		@Override
		public Map<String, String> options() {
			return options;
		}

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		public PragmaExpToCsvInst(InstExp<Gen, Sk, X, Y> inst, String file, List<Pair<String, String>> options) {
			Util.assertNotNull(file, options, inst);
			this.file = file;
			this.options = Util.toMapSafely(options);
			this.inst = inst;
		}

		@Override
		public Pragma eval0(AqlEnv env, boolean isC) {
			if (isC) {
				throw new IgnoreException();
			}
			AqlOptions op = new AqlOptions(options, null, env.defaults);
			return new ToCsvPragmaInstance<>(inst.eval(env, false), file, op);
		}

		@Override
		public String makeString() {
			final StringBuilder sb = new StringBuilder().append("export_csv_instance ").append(inst).append(" ")
					.append(Util.quote(file));

			if (!options.isEmpty()) {
				sb.append(" {").append("\n\toptions").append(Util.sep(options, "\n\t\t", " = ")).append("}");
			}
			return sb.toString();
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			result = prime * result + ((inst == null) ? 0 : inst.hashCode());
			result = prime * result + ((options == null) ? 0 : options.hashCode());
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
			PragmaExpToCsvInst<?, ?, ?, ?> other = (PragmaExpToCsvInst<?, ?, ?, ?>) obj;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.equals(other.file))
				return false;
			if (inst == null) {
				if (other.inst != null)
					return false;
			} else if (!inst.equals(other.inst))
				return false;
			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			return true;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return inst.deps();
		}

	}

	///////////////////////////////////////////////////////////////////

	@SuppressWarnings("hiding")
	public static final class PragmaExpToCsvTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> extends PragmaExp {

		public final String file;

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			set.add(AqlOption.csv_field_delim_char);
			set.add(AqlOption.csv_escape_char);
			set.add(AqlOption.csv_quote_char);
			set.add(AqlOption.csv_file_extension);
			set.add(AqlOption.csv_generate_ids);
			set.add(AqlOption.csv_emit_ids);
			set.add(AqlOption.csv_prepend_entity);
			set.add(AqlOption.prepend_entity_on_ids);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			trans.map(f);
		}

		public final Map<String, String> options1;
		public final Map<String, String> options2;

		public final TransExp<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> trans;

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public Map<String, String> options() {
			return options1;
		}

		public PragmaExpToCsvTrans(TransExp<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> trans, String file,
				List<Pair<String, String>> options1, List<Pair<String, String>> options2) {
			this.file = file;
			this.options1 = Util.toMapSafely(options1);
			this.options2 = Util.toMapSafely(options2);
			this.trans = trans;
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			result = prime * result + ((options1 == null) ? 0 : options1.hashCode());
			result = prime * result + ((options2 == null) ? 0 : options2.hashCode());
			result = prime * result + ((trans == null) ? 0 : trans.hashCode());
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
			PragmaExpToCsvTrans<?, ?, ?, ?, ?, ?, ?, ?> other = (PragmaExpToCsvTrans<?, ?, ?, ?, ?, ?, ?, ?>) obj;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.equals(other.file))
				return false;
			if (options1 == null) {
				if (other.options1 != null)
					return false;
			} else if (!options1.equals(other.options1))
				return false;
			if (options2 == null) {
				if (other.options2 != null)
					return false;
			} else if (!options2.equals(other.options2))
				return false;
			if (trans == null) {
				if (other.trans != null)
					return false;
			} else if (!trans.equals(other.trans))
				return false;
			return true;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return trans.deps();
		}

		@Override
		public String makeString() {
			final StringBuilder sb = new StringBuilder().append("export_csv_transform ").append(trans).append(" ")
					.append(Util.quote(file));

			if (!options1.isEmpty()) {
				sb.append("{\n\toptions").append(Util.sep(options1, "\n\t\t", " = ")).append("\n}");
			}
			if (!options2.isEmpty()) {
				sb.append("\n {\n\toptions").append(Util.sep(options2, "\n\t\t", " = ")).append("\n}");
			}
			return sb.toString();
		}

		@Override
		public Pragma eval0(AqlEnv env, boolean isC) {
			if (isC) {
				throw new IgnoreException();
			}
			AqlOptions op1 = new AqlOptions(options1, null, env.defaults);
			AqlOptions op2 = new AqlOptions(options2, null, env.defaults);
			return new ToCsvPragmaTransform<>(trans.eval(env, false), file, op1, op2);
		}
	}

	//////////////////////////////////////////////////

	public static final class PragmaExpVar extends PragmaExp {
		public final String var;

		@Override
		public Map<String, String> options() {
			return Collections.emptyMap();
		}

		@Override
		public boolean isVar() {
			return true;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.singleton(new Pair<>(var, Kind.PRAGMA));
		}

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		public PragmaExpVar(String var) {
			this.var = var;
		}

		@Override
		public Pragma eval0(AqlEnv env, boolean isC) {
			return env.defs.ps.get(var);
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
			PragmaExpVar other = (PragmaExpVar) obj;
			return var.equals(other.var);
		}

		@Override
		public String toString() {
			return var;
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {

		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
		}

	}

	/////////////////////////////////////////////////////////////////////////////////////////

	public static final class PragmaExpJs extends PragmaExp {
		private final List<String> jss;

		private final Map<String, String> options;

		@Override
		public Map<String, String> options() {
			return options;
		}

		public PragmaExpJs(List<String> jss, List<Pair<String, String>> options) {
			this.options = Util.toMapSafely(options);
			this.jss = jss;
		}

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((options == null) ? 0 : options.hashCode());
			result = prime * result + ((jss == null) ? 0 : jss.hashCode());
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
			PragmaExpJs other = (PragmaExpJs) obj;

			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			if (jss == null) {
				if (other.jss != null)
					return false;
			} else if (!jss.equals(other.jss))
				return false;
			return true;
		}

		@Override
		public Pragma eval0(AqlEnv env, boolean isC) {
			if (isC) {
				throw new IgnoreException();
			}
			return new JsPragma(jss, options, env);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			set.add(AqlOption.js_env_name);
		}

		@Override
		public String makeString() {
			final StringBuilder sb = new StringBuilder().append("exec_js {")
					.append(Util.sep(jss.stream().map(Util::quote).collect(Collectors.toList()), ""));

			if (!options.isEmpty()) {
				sb.append("\n\toptions").append(Util.sep(options, "\n\t\t", " = "));
			}
			return sb.append("}").toString();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

	}

	/////////////////////////////////////////////////////////////////////////////////////////

	public static final class PragmaExpProc extends PragmaExp {
		private final List<String> cmds;

		@Override
		public Map<String, String> options() {
			return options;
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {

		}

		private final Map<String, String> options;

		public PragmaExpProc(List<String> cmds, List<Pair<String, String>> options) {
			this.options = Util.toMapSafely(options);
			this.cmds = cmds;
		}

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((options == null) ? 0 : options.hashCode());
			result = prime * result + ((cmds == null) ? 0 : cmds.hashCode());
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
			PragmaExpProc other = (PragmaExpProc) obj;

			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			if (cmds == null) {
				if (other.cmds != null)
					return false;
			} else if (!cmds.equals(other.cmds))
				return false;
			return true;
		}

		@Override
		public Pragma eval0(AqlEnv env, boolean isC) {
			if (isC) {
				throw new IgnoreException();
			}
			return new ProcPragma(cmds);
		}

		@Override
		public String makeString() {
			final StringBuilder sb = new StringBuilder().append("exec_cmdline {")
					.append(Util.sep(cmds.stream().map(Util::quote).collect(Collectors.toList()), "\n"));

			if (!options.isEmpty()) {
				sb.append("\n\toptions").append(Util.sep(options, "\n\t\t", " = "));
			}
			return sb.append("}").toString();
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Collections.emptyList();
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
		}

	}

	/////////////////////////////////////////////////

	@SuppressWarnings("hiding")
	public static class PragmaExpToJdbcInst<Gen, Sk, X, Y> extends PragmaExp {

		public final String jdbcString;
		public final String prefix;
		//public final String clazz;

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			set.add(AqlOption.jdbc_default_class);
			set.add(AqlOption.jdbc_default_string);
			set.add(AqlOption.start_ids_at);
			set.add(AqlOption.id_column_name);
			set.add(AqlOption.varchar_length);
			set.add(AqlOption.jdbc_export_truncate_after);
			set.add(AqlOption.jdbc_quote_char);
		}

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			I.map(f);
		}

		public final Map<String, String> options;

		public final InstExp<Gen, Sk, X, Y> I;

		@Override
		public Map<String, String> options() {
			return options;
		}

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		public PragmaExpToJdbcInst(InstExp<Gen, Sk, X, Y> i, String jdbcString, String prefix,
				List<Pair<String, String>> options) {
			this.jdbcString = jdbcString;
			this.prefix = prefix;
			//this.clazz = clazz;
			this.options = Util.toMapSafely(options);
			I = i;
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return I.deps();
		}

		@Override
		public synchronized Pragma eval0(AqlEnv env, boolean isC) {
			String toGet = jdbcString;
			//String driver = clazz;
			AqlOptions op = new AqlOptions(options, null, env.defaults);
			//if (clazz.trim().isEmpty()) {
			//	driver = (String) op.getOrDefault(AqlOption.jdbc_default_class);
			//}
			if (jdbcString.trim().isEmpty()) {
				toGet = (String) op.getOrDefault(AqlOption.jdbc_default_string);
			}
			if (isC) {
				throw new IgnoreException();
			}
			return new ToJdbcPragmaInstance<>(prefix, I.eval(env, false), toGet, op);
		}

		@Override
		public String makeString() {
			final StringBuilder sb = new StringBuilder().append("export_jdbc_instance ").append(I).append(" ")
					.append(" ").append(Util.quote(jdbcString)).append(" ")
					.append(Util.quote(prefix));
			if (!options.isEmpty()) {
				sb.append(" {").append("\n\toptions").append(Util.sep(options, "\n\t\t", " = ")).append("}");
			}
			return sb.toString();
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((I == null) ? 0 : I.hashCode());
			//result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
			result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
			result = prime * result + ((options == null) ? 0 : options.hashCode());
			result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
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
			PragmaExpToJdbcInst<?, ?, ?, ?> other = (PragmaExpToJdbcInst<?, ?, ?, ?>) obj;

			if (I == null) {
				if (other.I != null)
					return false;
			} else if (!I.equals(other.I))
				return false;
			if (jdbcString == null) {
				if (other.jdbcString != null)
					return false;
			} else if (!jdbcString.equals(other.jdbcString))
				return false;
			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			if (prefix == null) {
				if (other.prefix != null)
					return false;
			} else if (!prefix.equals(other.prefix))
				return false;
			return true;
		}

	}

	////////////////////////////////////////////////////////////////////////////////////////////

	public static class PragmaExpToJdbcTrans<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> extends PragmaExp {

		public final String jdbcString, prefix;

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			h.map(f);
		}

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			set.add(AqlOption.jdbc_default_class);
			set.add(AqlOption.jdbc_default_string);
			set.add(AqlOption.start_ids_at);
			set.add(AqlOption.id_column_name);
			set.add(AqlOption.varchar_length);
			set.add(AqlOption.jdbc_quote_char);
		}

		public final Map<String, String> options1, options2;

		public final TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h;

		@Override
		public Map<String, String> options() {
			return options1;
		}

		public PragmaExpToJdbcTrans(TransExp<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> h,String jdbcString,
				String prefix, List<Pair<String, String>> options1, List<Pair<String, String>> options2) {
			this.jdbcString = jdbcString;
			this.prefix = prefix;
			//this.clazz = clazz;
			this.options1 = Util.toMapSafely(options1);
			this.options2 = Util.toMapSafely(options2);
			this.h = h;
		}

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return h.deps();
		}

		@Override
		public Pragma eval0(AqlEnv env, boolean isC) {
			if (isC) {
				throw new IgnoreException();
			}
			String toGet = jdbcString;
		//	String driver = clazz;
			AqlOptions op1 = new AqlOptions(options1, null, env.defaults);
			AqlOptions op2 = new AqlOptions(options2, null, env.defaults);

			//if (clazz.trim().isEmpty()) {
			//	driver = (String) op1.getOrDefault(AqlOption.jdbc_default_class);
			//}
			if (jdbcString.trim().isEmpty()) {
				toGet = (String) op1.getOrDefault(AqlOption.jdbc_default_string);
			}
			return new ToJdbcPragmaTransform<>(prefix, h.eval(env, false), toGet, op1, op2);
		}

		@Override
		public String makeString() {
			final StringBuilder sb = new StringBuilder().append("export_jdbc_transform ").append(h).append(" ")
					.append(" ").append(Util.quote(jdbcString)).append(" ")
					.append(Util.quote(prefix)).append(" ");
			if (!options1.isEmpty()) {
				sb.append("{");
				sb.append("\n\toptions").append(Util.sep(options1, "\n\t\t", " = ")).append("\n");
				sb.append("}");
			}
			if (!options2.isEmpty()) {
				sb.append("{");
				sb.append("\n\toptions").append(Util.sep(options2, "\n\t\t", " = ")).append("\n");
				sb.append("}");
			}
			return sb.toString().trim();
		}

		@Override
		public int hashCode() {
			int prime = 31;
			int result = 1;
			result = prime * result + ((h == null) ? 0 : h.hashCode());
			//result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
			result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
			result = prime * result + ((options1 == null) ? 0 : options1.hashCode());
			result = prime * result + ((options2 == null) ? 0 : options2.hashCode());
			result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
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
			PragmaExpToJdbcTrans<?, ?, ?, ?, ?, ?, ?, ?> other = (PragmaExpToJdbcTrans<?, ?, ?, ?, ?, ?, ?, ?>) obj;

			if (h == null) {
				if (other.h != null)
					return false;
			} else if (!h.equals(other.h))
				return false;
		if (jdbcString == null) {
				if (other.jdbcString != null)
					return false;
			} else if (!jdbcString.equals(other.jdbcString))
				return false;
			if (options1 == null) {
				if (other.options1 != null)
					return false;
			} else if (!options1.equals(other.options1))
				return false;
			if (options2 == null) {
				if (other.options2 != null)
					return false;
			} else if (!options2.equals(other.options2))
				return false;
			if (prefix == null) {
				if (other.prefix != null)
					return false;
			} else if (!prefix.equals(other.prefix))
				return false;
			return true;
		}

	}

	/////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("hiding")
	public static class PragmaExpToJdbcQuery extends PragmaExp {

		public final String jdbcString, prefixSrc, prefixDst;

		@Override
		protected void allowedOptions(Set<AqlOption> set) {
			set.add(AqlOption.jdbc_default_class);
			set.add(AqlOption.jdbc_default_string);
			set.add(AqlOption.jdbc_query_export_convert_type);
			set.add(AqlOption.id_column_name);
			set.add(AqlOption.varchar_length);
			set.add(AqlOption.jdbc_quote_char);
		}

		public final Map<String, String> options;

		public final QueryExp Q;

		@Override
		public void mapSubExps(Consumer<Exp<?>> f) {
			Q.map(f);
		}

		@Override
		public Map<String, String> options() {
			return options;
		}

		public PragmaExpToJdbcQuery(QueryExp Q, String jdbcString, String prefixSrc, String prefixDst,
				List<Pair<String, String>> options) {
			this.jdbcString = jdbcString;
			this.prefixSrc = prefixSrc;
			this.prefixDst = prefixDst;
			//this.clazz = clazz;
			this.options = Util.toMapSafely(options);
			this.Q = Q;
		}

		public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
			return v.visit(params, this);
		}

		@Override
		public Collection<Pair<String, Kind>> deps() {
			return Q.deps();
		}

		@Override
		public Pragma eval0(AqlEnv env, boolean isC) {
			if (isC) {
				throw new IgnoreException();
			}
			String toGet = jdbcString;
			//String driver = clazz;
			AqlOptions op = new AqlOptions(options, null, env.defaults);
			////if (clazz.trim().isEmpty()) {
			//	driver = (String) op.getOrDefault(AqlOption.jdbc_default_class);
			//}
			if (jdbcString.trim().isEmpty()) {
				toGet = (String) op.getOrDefault(AqlOption.jdbc_default_string);
			}
			return new ToJdbcPragmaQuery<>(prefixSrc, prefixDst, Q.eval(env, false), toGet, op);
		}

		@Override
		public String makeString() {
			final StringBuilder sb = new StringBuilder().append("export_jdbc_query ").append(Q).append(" ")
					.append(" ").append(Util.quote(jdbcString)).append(" ")
					.append(Util.quote(prefixSrc)).append(" ").append(Util.quote(prefixDst));
			if (!options.isEmpty()) {
				sb.append(" {").append("\n\toptions").append(Util.sep(options, "\n\t\t", " = ")).append("}");
			}
			return sb.toString().trim();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((Q == null) ? 0 : Q.hashCode());
			//result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
			result = prime * result + ((jdbcString == null) ? 0 : jdbcString.hashCode());
			result = prime * result + ((options == null) ? 0 : options.hashCode());
			result = prime * result + ((prefixDst == null) ? 0 : prefixDst.hashCode());
			result = prime * result + ((prefixSrc == null) ? 0 : prefixSrc.hashCode());
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
			PragmaExpToJdbcQuery other = (PragmaExpToJdbcQuery) obj;
			if (Q == null) {
				if (other.Q != null)
					return false;
			} else if (!Q.equals(other.Q))
				return false;
		if (jdbcString == null) {
				if (other.jdbcString != null)
					return false;
			} else if (!jdbcString.equals(other.jdbcString))
				return false;
			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			if (prefixDst == null) {
				if (other.prefixDst != null)
					return false;
			} else if (!prefixDst.equals(other.prefixDst))
				return false;
			if (prefixSrc == null) {
				if (other.prefixSrc != null)
					return false;
			} else if (!prefixSrc.equals(other.prefixSrc))
				return false;
			return true;
		}

	}
}
