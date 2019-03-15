package catdata.aql.fdm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import catdata.Chc;
import catdata.Pair;
import catdata.Util;
import catdata.aql.Algebra;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Frozen;
import catdata.aql.Instance;
import catdata.aql.It.ID;
import catdata.aql.Query;
import catdata.aql.Schema;
import catdata.aql.SqlTypeSide;
import catdata.aql.Term;
import catdata.aql.Transform;
import catdata.aql.Var;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class EvalAlgebra<Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, Fk2, Att2, X, Y> extends
		Algebra<Ty, En2, Sym, Fk2, Att2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, Y, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, Y> {

	private final Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> Q;

	private final Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I;

	private final Map<En2, Pair<List<Var>, Collection<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>>>> ens = new THashMap<>();

	@Override
	public Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> fk(Fk2 fk,
			Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> row) {
		Transform<Ty, En1, Sym, Fk1, Att1, Var, Var, Var, Var, ID, Chc<Var, Pair<ID, Att1>>, ID, Chc<Var, Pair<ID, Att1>>> t = Q.fks
				.get(fk);

		List<Var> l = ens.get(Q.dst.fks.get(fk).second).first;
		En2 en2 = Q.dst.fks.get(fk).second;
		Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> r = new Row<>(en2);

//		Map<Var, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> ret = new THashMap<>(l.size());
		for (Var v : l) {
			if (t.gens().containsKey(v)) {
				r = new Row<>(r, v, trans2(row, t.gens().get(v).convert()), en2, t.src().gens().get(v));
			} else {
				r = new Row<>(r, v, trans2(row, t.sks().get(v).convert()), en2, t.src().sks().get(v));
			}

		}

		return r; // Row.mkRow(l, ret, Q.dst.fks.get(fk).second,
					// Q.ens.get(Q.dst.fks.get(fk).second).gens,
					// Q.ens.get(Q.dst.fks.get(fk).second).sks);
	}

	private Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> trans2(
			Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> row,
			Term<Ty, En1, Sym, Fk1, Att1, Var, Var> term) {
		if (term.gen() != null) {
			return row.get(term.gen());
		} else if (term.fk() != null) {
			return Chc.inLeft(I.algebra().fk(term.fk(), trans2(row, term.arg).l));
		} else if (term.sk() != null) {
			return row.get(term.sk());
		} else if (term.att() != null) {
			return Chc.inRight(I.reprT(I.algebra().att(term.att(), trans2(row, term.arg).l)));
		} else if (term.sym() != null) {
			List<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> l = new ArrayList<>(term.args.size());
			for (Term<Ty, En1, Sym, Fk1, Att1, Var, Var> x : term.args) {
				l.add(trans2(row, x).r);
			}
			Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk> done = I.schema().typeSide.js.reduce(Term.Sym(term.sym(), l));
			if (I.model != null) {
				return Chc.inRight(I.talgNF(done));
			}
			return Chc.inRight(done);
		} else if (term.obj() != null) {
			return Chc.inRight(term.convert());
		}
		throw new RuntimeException("Anomaly: please report");
	}

	@Override
	public Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> gen(
			Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> gen) {
		return gen;
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Y> att(Att2 att,
			Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> x) {
		Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk> l = trans1x(x, Q.atts.get(att), I,
				Q.ens.get(Q.dst.atts.get(att).first));
		return I.algebra().intoY(l);
	}

	@Override
	public Collection<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>> en(En2 en) {
		return ens.get(en).second;
	}

	@Override
	public Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Y sk) {
		return Term.Sk(sk);
	}

	@Override
	public Term<Void, En2, Void, Fk2, Void, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, Void> repr(En2 en,
			Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> x) {
		return Term.Gen(x);
	}

	@Override
	public Collage<Ty, Void, Sym, Void, Void, Void, Y> talg0() {
		return I.algebra().talg();
	}

	@Override
	public Object printX(En2 en2, Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> x) {
		return x.toString(z -> {
			if (!z.left) {
				return z.r.toString();
			}
			En1 en1 = (En1) x.t;

			return I.algebra().printX(en1, z.l).toString();
		});
	}

	@Override
	public Object printY(Ty ty, Y y) {
		return I.algebra().printY(ty, y);
	}

	@Override
	public String toStringProver() {
		return I.algebra().toStringProver();
	}

	@Override
	public Schema<Ty, En2, Sym, Fk2, Att2> schema() {
		return Q.dst;
	}

	public final AqlOptions options;

	public EvalAlgebra(Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q,
			Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I, AqlOptions options) {
		this.I = I;
		this.options = options;
		if (!I.schema().equals(q.src)) {
			throw new RuntimeException("Anomaly: please report");
		} else if (!q.consts.keySet().containsAll(q.params.keySet())) {
			throw new RuntimeException(
					"Missing bindings: " + Util.sep(Util.diff(q.params.keySet(), q.consts.keySet()), ",")); // TODO aql
		} else if (!q.consts.keySet().isEmpty()) {
			q = q.deParam();
		}
		Connection conn = null;
		boolean safe = (I.algebra().talg().sks.isEmpty() && I.algebra().talg().eqs.isEmpty()) || allowUnsafeSql();
		boolean useSql = I.size() >= minSizeForSql() && safe && (SqlTypeSide.tys().containsAll(I.schema().typeSide.tys))
				&& SqlTypeSide.syms().keySet().containsAll(I.schema().typeSide.syms.keySet());
		int vlen = (int) options.getOrDefault(AqlOption.varchar_length);
		if (useSql) {
			this.Q = q.unnest();
			Map<En1, List<String>> xx = Collections.emptyMap();
			if (useIndices()) {
				Pair<Collection<Fk1>, Collection<Att1>> l = this.Q.fksAndAttsOfWhere();
				xx = I.schema().toSQL_srcIdxs(l);
			}
			int startId = (Integer) options.getOrDefault(AqlOption.start_ids_at);
			Pair<TObjectIntMap<X>, TIntObjectMap<X>> J = I.algebra().intifyX(startId);
			if (persistentIndices()) {
				conn = I.algebra().addIndices(J, xx, vlen);
			} else {
				conn = I.algebra().createAndLoad(xx, J, vlen);
			}
		} else {
			this.Q = q;
		}
		for (En2 en2 : Q.ens.keySet()) {
			Pair<List<Var>, Collection<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>>> x = eval(en2,
					this.Q.ens.get(en2), conn, useSql);
			ens.put(en2, x);
		}
		if (conn != null) {
			if (!persistentIndices()) {
				try {
					conn.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

	}

	private boolean allowUnsafeSql() {
		return (boolean) options.getOrDefault(AqlOption.eval_approx_sql_unsafe);
	}

	private boolean useIndices() {
		return (boolean) options.getOrDefault(AqlOption.eval_use_indices);
	}

	private boolean persistentIndices() {
		return (boolean) options.getOrDefault(AqlOption.eval_sql_persistent_indices);
	}

	private int minSizeForSql() {
		return (int) options.getOrDefault(AqlOption.eval_use_sql_above);
	}

	private Pair<List<Var>, Collection<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>>> eval(En2 en2,
			Frozen<Ty, En1, Sym, Fk1, Att1> q, Connection conn, boolean useSql) {
		Collection<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>> ret = null;
		// Integer k = maxTempSize();
		if (useSql) {
			for (Frozen<Ty, En1, Sym, Fk1, Att1> z : Q.ens.values()) {
				if (!z.sks().isEmpty()) {
					throw new RuntimeException("FROM clauses can't bind types in SQL.");
				}
			}
			Pair<List<Var>, Collection<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>>> ret2 = evalSql(en2, q,
					I.algebra().intifyX((int) options.getOrDefault(AqlOption.start_ids_at)), conn);
			return ret2;
		}
		ret = new LinkedList<>();
		List<Var> plan = q.order(options, I);
		boolean useIndices = useIndices() && q.gens.size() > 1 && I.algebra().hasFreeTypeAlgebra();
		ret.add(new Row<>(en2));
		Collection<Object> done = new THashSet<>(plan.size());
		for (Var v : plan) {
			Chc<En1, Ty> x;
			if (q.gens().containsKey(v)) {
				x = Chc.inLeft(q.gens.get(v));
			} else {
				x = Chc.inRight(q.sks.get(v));
			}
			ret = EvalAlgebra.extend(ret, v, q, I, useIndices, x, done);
			done.add(v);
		}
		return new Pair<>(plan, ret);

	}

	private Pair<List<Var>, Collection<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>>> evalSql(En2 en2,
			Frozen<Ty, En1, Sym, Fk1, Att1> q, Pair<TObjectIntMap<X>, TIntObjectMap<X>> pair, Connection conn) {

		if (q.gens.isEmpty()) {
			Collection<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>> ret = new LinkedList<>();
			ret.add(new Row<>(en2));
			return new Pair<>((Collections.emptyList()), ret);
		}
		List<Var> order = (new LinkedList<>(q.gens().keySet()));
		try (Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery(Q.toSQL("").get(en2));
			Collection<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>> ret = (new LinkedList<>());
			// ResultSetMetaData rsmd = rs.getMetaData();
			while (rs.next()) {
				Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> r = new Row<>(en2);
				for (Var v : order) {
					X x = pair.second.get(rs.getInt(v.var));
					if (x == null) {
						stmt.close();
						rs.close();
						throw new RuntimeException("Encountered a NULL generator");
					}
					r = new Row<>(r, v, Chc.inLeft(x), r.en2(), q.gens.get(v));
				}
				ret.add(r);
			}
			rs.close();
			stmt.close();
			return new Pair<>(order, ret);
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}

	}

	static <Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y, En2> List<Pair<Fk1, X>> getAccessPath(Var v,
			Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> tuple, Frozen<Ty, En1, Sym, Fk1, Att1> q2,
			Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i2, Collection<Object> done) {
		List<Pair<Fk1, X>> ret = (new LinkedList<>());
		for (Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> eq : q2.eqs()) {
			if (eq.first.fk() != null && eq.first.arg.equals(Term.Gen(v))) {
				Optional<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> rhs = trans1(tuple, eq.first, i2, q2, done);
				if (!rhs.isPresent()) {
					continue;
				}
				X x = i2.algebra().nf(rhs.get().convert());
				ret.add(new Pair<>(eq.first.fk(), x));
			} else if (eq.second.fk() != null && eq.second.arg.equals(Term.Gen(v))) {
				Optional<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> lhs = trans1(tuple, eq.second, i2, q2, done);
				if (!lhs.isPresent()) {
					continue;
				}
				X x = i2.algebra().nf(lhs.get().convert());
				ret.add(new Pair<>(eq.second.fk(), x));
			}
		}
		return ret;
	}

	static <Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y, En2> List<Pair<Att1, Object>> getAccessPath2(Var v,
			Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> tuple, Frozen<Ty, En1, Sym, Fk1, Att1> q2,
			Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i2, Collection<Object> done) {
		List<Pair<Att1, Object>> ret = (new LinkedList<>());
		for (Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> eq : q2.eqs()) {
			if (eq.first.att() != null && eq.first.arg.equals(Term.Gen(v))) {
				Optional<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> rhs = trans1(tuple, eq.second, i2, q2, done);
				if (!rhs.isPresent()) {
					continue;
				}
				Term<Ty, Void, Sym, Void, Void, Void, Y> x = i2.algebra().intoY(rhs.get().convert());
				if (x.obj() != null) {
					ret.add(new Pair<>(eq.first.att(), x.obj()));
				}
			} else if (eq.second.att() != null && eq.second.arg.equals(Term.Gen(v))) {
				Optional<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> lhs = trans1(tuple, eq.first, i2, q2, done);
				if (!lhs.isPresent()) {
					continue;
				}
				Term<Ty, Void, Sym, Void, Void, Void, Y> x = i2.algebra().intoY(lhs.get().convert());
				if (x.obj() != null) {
					ret.add(new Pair<>(eq.second.att(), x.obj()));
				}
			}
		}
		return ret;
	}
	
	static <Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, X, Y> Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk> trans1x(
			Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> tuple,
			Term<Ty, En1, Sym, Fk1, Att1, Var, Var> first, Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i2,
			Frozen<Ty, En1, Sym, Fk1, Att1> Q) {
		if (first.gen() != null) {
			En1 en1 = Q.gens.get(first.gen());
			return i2.algebra().repr(en1, tuple.get(first.gen()).l).convert();
		} else if (first.sk() != null) {
			Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> z = tuple.get(first.sk());
			if (!z.left) {
				return z.r;
			}
			return Util.anomaly(); // Optional.of(i2.algebra().repr(z.l).convert());
		}

		else if (first.obj() != null) {
			return first.asObj();
		} else if (first.fk() != null) {
			Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk> arg = trans1x(tuple, first.arg, i2, Q);
			return(Term.Fk(first.fk(), arg));
		} else if (first.att() != null) {
			Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk> arg = trans1x(tuple, first.arg, i2, Q);
			return Term.Att(first.att(), arg);
		} else if (first.sym() != null) {
			List<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> args = (new ArrayList<>(first.args.size()));
			for (Term<Ty, En1, Sym, Fk1, Att1, Var, Var> arg : first.args) {
				Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk> arg2 = trans1x(tuple, arg, i2, Q);
				args.add(arg2);
			}
			return (Term.Sym(first.sym(), args));
		}
		throw new RuntimeException("Anomaly: please report");
	}

	static <Ty, En1, Sym, Fk1, Att1, Gen, Sk, En2, X, Y> Optional<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> trans1(
			Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> tuple,
			Term<Ty, En1, Sym, Fk1, Att1, Var, Var> first, Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> i2,
			Frozen<Ty, En1, Sym, Fk1, Att1> Q, Collection<Object> done) {
		//if (!tuple.asMap().keySet().equals(done)) {
		//	System.out.println(tuple + " and " + done);
		//	Util.anomaly();
		//}
		
		if (first.gen() != null) {
			//if (!tuple.containsKey(first.gen())) {
			//	return Optional.empty();
			//}
			if (!done.contains(first.gen())) {
				return Optional.empty();
			}
			En1 en1 = Q.gens.get(first.gen());
			return Optional.of(i2.algebra().repr(en1, tuple.get(first.gen()).l).convert());
		} else if (first.sk() != null) {
			//if (!tuple.containsKey(first.sk())) {
			//	return Optional.empty();
			//}
			if (!done.contains(first.sk())) {
				return Optional.empty();
			}
			Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> z = tuple.get(first.sk());
			if (!z.left) {
				return Optional.of(z.r);
			}
			return Util.anomaly(); // Optional.of(i2.algebra().repr(z.l).convert());
		}

		else if (first.obj() != null) {
			return Optional.of(first.asObj());
		} else if (first.fk() != null) {
			Optional<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> arg = trans1(tuple, first.arg, i2, Q, done);
			if (!arg.isPresent()) {
				return Optional.empty();
			}
			return Optional.of(Term.Fk(first.fk(), arg.get()));
		} else if (first.att() != null) {
			Optional<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> arg = trans1(tuple, first.arg, i2, Q, done);
			if (!arg.isPresent()) {
				return Optional.empty();
			}
			return Optional.of(Term.Att(first.att(), arg.get()));
		} else if (first.sym() != null) {
			List<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> args = (new ArrayList<>(first.args.size()));
			for (Term<Ty, En1, Sym, Fk1, Att1, Var, Var> arg : first.args) {
				Optional<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> arg2 = trans1(tuple, arg, i2, Q, done);
				if (!arg2.isPresent()) {
					return Optional.empty();
				}
				args.add(arg2.get());
			}
			return Optional.of(Term.Sym(first.sym(), args));
		}
		throw new RuntimeException("Anomaly: please report");
	}

	@Override
	public boolean hasFreeTypeAlgebra() {
		return I.algebra().hasFreeTypeAlgebra();
	}

	@Override
	public boolean hasFreeTypeAlgebraOnJava() {
		return I.algebra().hasFreeTypeAlgebraOnJava();
	}

	// TODO CQL slowness hurts chase
	public static <En2, X, Y, Ty, En1, Sym, Fk1, Att1, Gen, Sk> Collection<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>> extend(
			Collection<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>> tuples, Var v,
			Frozen<Ty, En1, Sym, Fk1, Att1> q, Instance<Ty, En1, Sym, Fk1, Att1, Gen, Sk, X, Y> I, boolean useIndices,
			Chc<En1, Ty> enOrTy, Collection<Object> done) {
		List<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>> ret = new LinkedList<>();

		Iterator<Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> dom;
		for (Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> tuple : tuples) {
			if (useIndices && enOrTy.left) {
				List<Pair<Fk1, X>> l1 = EvalAlgebra.getAccessPath(v, tuple, q, I, done);
				List<Pair<Att1, Object>> l2 = EvalAlgebra.getAccessPath2(v, tuple, q, I, done);
				dom = Chc.leftIterator(I.algebra().en_indexed(q.gens.get(v), l1, l2).iterator());
			} else {
				dom = I.enOrTy(enOrTy);
			}
			done.add(v);

			outer: while (dom.hasNext()) {
				Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> x = dom.next();

				Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>> row = new Row<>(tuple, v, x, tuple.en2(),
						enOrTy);
				for (Pair<Term<Ty, En1, Sym, Fk1, Att1, Var, Var>, Term<Ty, En1, Sym, Fk1, Att1, Var, Var>> eq : q
						.eqs()) {
					
					Optional<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> lhs = EvalAlgebra.trans1(row, eq.first, I, q,
							done);
					Optional<Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>> rhs = EvalAlgebra.trans1(row, eq.second, I, q, done);
					
					if (!lhs.isPresent() || !rhs.isPresent()) {
						ret.add(row);
						continue outer;
					}
					if (!I.dp().eq(null, lhs.get(), rhs.get())) {
						continue outer;
					}
				}
				ret.add(row);
			}
			done.remove(v);
		}
		
		return ret;
	}

	public String talgToString() {
		return I.algebra().talgToString();
	}

	@Override
	public int size(En2 en) {
		return ens.get(en).second.size();
	}

	@Override
	public Chc<Y, Pair<Row<En2, Chc<X, Term<Ty, En1, Sym, Fk1, Att1, Gen, Sk>>>, Att2>> reprT_prot(Y y) {
		return Chc.inLeft(y);
	}

}