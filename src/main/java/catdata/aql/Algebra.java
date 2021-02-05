package catdata.aql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.collections4.iterators.IteratorIterable;
import org.apache.commons.collections4.list.TreeList;

import com.google.common.collect.Iterators;

import catdata.Chc;
import catdata.Pair;
import catdata.Triple;
import catdata.Util;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

public abstract class Algebra<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> /* implements DP<Ty,En,Sym,Fk,Att,Gen,Sk> */ {

	public static class TAlg<Ty, Sym, Y> {

		private String toString;

		@Override
		public synchronized String toString() {
			if (toString != null) {
				return toString;
			}
			StringBuffer sb = new StringBuffer("Labelled nulls:\n");
			sb.append(Util.sep(sks, " : ", "\n"));
			sb.append("\nEquations\n");
			sb.append(Util.sep(eqs.iterator(), " \n ", x -> x.first + " = " + x.second));
			// sb.append("\nDefinitions\n");
			// sb.append(Util.sep(subst, " -> ", "\n"));

			toString = sb.toString();
			return toString;
		}

		public TAlg(Map<Y, Ty> sks,
				Collection<Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>>> eqs) {
			this.sks = sks;
			this.eqs = eqs;
//			this.subst = subst;
		}

		public final Map<Y, Ty> sks;

		public Collection<Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>>> eqsNoDefns() {
			return eqs;
		}

		public Iterable<Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>>> allEqs() {
			if (eqs == null) {
				Util.anomaly();
			}
			return new Iterable<>() {

				@Override
				public Iterator<Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>>> iterator() {
					return eqs.iterator();
				}
			};
		}

		private final Collection<Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>>> eqs;

		public Ty type(TypeSide<Ty, Sym> ts, Term<Ty, Void, Sym, Void, Void, Void, Y> e) {
			return toCollage(ts, false).type(Collections.emptyMap(), e).l;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((eqs == null) ? 0 : eqs.hashCode());
			result = prime * result + ((sks == null) ? 0 : sks.hashCode());
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
			TAlg other = (TAlg) obj;
			if (eqs == null) {
				if (other.eqs != null)
					return false;
			} else if (!eqs.equals(other.eqs))
				return false;
			if (sks == null) {
				if (other.sks != null)
					return false;
			} else if (!sks.equals(other.sks))
				return false;
			return true;
		}

		public Collage<Ty, Void, Sym, Void, Void, Void, Y> toCollage(TypeSide<Ty, Sym> ts, boolean addTsEqs) {
			return new Collage<>() {

				@Override
				public Set<Ty> tys() {
					return ts.tys;
				}

				@Override
				public Map<Sym, Pair<List<Ty>, Ty>> syms() {
					return ts.syms;
				}

				@Override
				public Map<Ty, String> java_tys() {
					return ts.js.java_tys;
				}

				@Override
				public Map<Ty, String> java_parsers() {
					return ts.js.java_parsers;
				}

				@Override
				public Map<Sym, String> java_fns() {
					return ts.js.java_fns;
				}

				@Override
				public Set<Void> getEns() {
					return Collections.emptySet();
				}

				@Override
				public Map<Void, Pair<Void, Ty>> atts() {
					return Collections.emptyMap();
				}

				@Override
				public Map<Void, Pair<Void, Void>> fks() {
					return Collections.emptyMap();
				}

				@Override
				public Map<Void, Void> gens() {
					return Collections.emptyMap();
				}

				@Override
				public Map<Y, Ty> sks() {
					return sks;
				}

				@Override
				public synchronized Collection<Eq<Ty, Void, Sym, Void, Void, Void, Y>> eqs() {
					if (!addTsEqs) {

						Collection<Eq<Ty, Void, Sym, Void, Void, Void, Y>> ret = new AbstractCollection<>() {

							@Override
							public synchronized Iterator<Eq<Ty, Void, Sym, Void, Void, Void, Y>> iterator() {
								return Iterators.transform(eqs.iterator(), x -> new Eq<>(null, x.first, x.second));
							}

							@Override
							public int size() {
								return eqs.size(); // +subst.size();
							}
						};

						return ret;
					}
					Collection<Eq<Ty, Void, Sym, Void, Void, Void, Y>> ret = new AbstractCollection<>() {

						@Override
						public synchronized Iterator<Eq<Ty, Void, Sym, Void, Void, Void, Y>> iterator() {
							Collage<Ty, Void, Sym, Void, Void, Void, Y> c = ts.collage();
							return Iterators.concat(c.eqs().iterator(),
									Iterators.transform(eqs.iterator(), x -> new Eq<>(null, x.first, x.second)));
						}

						@Override
						public int size() {
							return eqs.size() + ts.eqs.size(); // + subst.size();
						}
					};

					return ret;
				}
			};
		}
	}

	/*
	 * @SuppressWarnings("unchecked") public <Ty, En, Sym, Fk, Att, Gen, Sk>
	 * Collage<Ty, En, Sym, Fk, Att, Gen, Sk> convert() { return (Collage<Ty, En,
	 * Sym, Fk, Att, Gen, Sk>) this; }
	 */

	public abstract Schema<Ty, En, Sym, Fk, Att> schema();

	// TODO aql cant validate algebras bc are not dps

	public abstract boolean hasNulls();

	public boolean hasFreeTypeAlgebra() {
		for (Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>> eq : talg().eqs) {
			if (!schema().typeSide.eqs.contains(new Triple<>(Collections.EMPTY_MAP, eq.first, eq.second))) {
				return false;
			}
		}
		return true;
	}

	public boolean hasFreeTypeAlgebraOnJava() {
		for (Pair<Term<Ty, Void, Sym, Void, Void, Void, Y>, Term<Ty, Void, Sym, Void, Void, Void, Y>> eq : talg().eqs) {
			if (schema().typeSide.js.java_tys.containsKey(talg().type(schema().typeSide, eq.first))) {
				if (!schema().typeSide.eqs.contains(new Triple<>(Collections.EMPTY_MAP, eq.first, eq.second))) {
					return false;
				}
			}
		}
		return true;
	}

	public abstract Iterable<X> en(En en);

	private final Map<Triple<En, List<Pair<Fk, X>>, List<Pair<Att, Object>>>, Collection<X>> en_index2 = (new THashMap<>());

	public synchronized Iterable<X> en_indexed(En en, List<Pair<Fk, X>> fks, List<Pair<Att, Object>> atts) {
		Triple<En, List<Pair<Fk, X>>, List<Pair<Att, Object>>> t = new Triple<>(en, fks, atts);
		if (en_index2.containsKey(t)) {
			return en_index2.get(t);
		} else if (atts.isEmpty() && fks.size() == 1) {
			return en_indexedFk(en, fks.get(0).first, fks.get(0).second);
		} else if (fks.isEmpty() && atts.size() == 1) {
			return en_indexedAtt(en, atts.get(0).first, atts.get(0).second);
		} else if (atts.isEmpty() && fks.isEmpty()) {
			return en(en);
		}
		List<X> l = new LinkedList<>();
		en(en).forEach(l::add);
		for (Pair<Fk, X> p : fks) {
			l.retainAll(en_indexedFk(en, p.first, p.second));
		}
		for (Pair<Att, Object> p : atts) {
			l.retainAll(en_indexedAtt(en, p.first, p.second));
		}
		en_index2.put(t, l);
		return l;
	}

	private final Map<Triple<En, Fk, X>, Collection<X>> fk_index = (new THashMap<>());

	public synchronized Collection<X> en_indexedFk(En en, Fk fk, X x) {
		Triple<En, Fk, X> t = new Triple<>(en, fk, x);
		if (fk_index.containsKey(t)) {
			return fk_index.get(t);
		}
		List<X> ret = new LinkedList<>();
		for (X y : en(en)) {
			if (fk(fk, y).equals(x)) {
				ret.add(y);
			}
		}
		fk_index.put(t, ret);
		return ret;
	}

	private static <X> Collection<X> iterToCol(Iterable<X> it, int size) {
		return new AbstractCollection<>() {
			@Override
			public Iterator<X> iterator() {
				return it.iterator();
			}

			@Override
			public int size() {
				return size;
			}
		};
	}

	public synchronized Collection<X> en_indexedAtt(En en, Att att, Object y) {
		Triple<En, Att, Object> t = new Triple<>(en, att, y);
		if (att_index.containsKey(t)) {
			return att_index.get(t);
		} else if (!hasFreeTypeAlgebra() || schema().typeSide.tys.size() != schema().typeSide.js.java_tys.size()) {
			return iterToCol(en(en), size(en));
		}
		List<X> ret = new LinkedList<>();
		for (X x : en(en)) {
			if (att(att, x).equals(Term.Obj(y, schema().atts.get(att).second))) { // TODO aql only works bc free
				ret.add(x);
			}
		}
		att_index.put(t, ret);
		return ret;
	}

	private final Map<Triple<En, Att, Object>, Collection<X>> att_index = (new THashMap<>());

	public abstract X gen(Gen gen);

	public abstract X fk(Fk fk, X x);

	public abstract Term<Ty, Void, Sym, Void, Void, Void, Y> att(Att att, X x);

	public abstract Term<Ty, Void, Sym, Void, Void, Void, Y> sk(Sk sk);

	public final X nf(Term<Void, En, Void, Fk, Void, Gen, Void> term) {
		if (term.gen() != null) {
			return gen(term.gen());
		} else if (term.fk() != null) {
			return fk(term.fk(), nf(term.arg));
		}
		throw new RuntimeException("Anomaly: please report");
	}

	public abstract Term<Void, En, Void, Fk, Void, Gen, Void> repr(En en, X x);

	// rows
	public int size() {
		int i = 0;
		for (En en : schema().ens) {
			i += size(en);
		}
		return i;
	}

	public abstract int size(En en);

	public Iterable<X> allXs() {
		IteratorChain<X> it = new IteratorChain<>();
		for (En en : schema().ens) {
			it.addIterator(en(en).iterator());
		}
		return new IteratorIterable<>(it, false);
	}

	/**
	 * @return only equations for instance part (no typeside, no schema)
	 */
	public synchronized final TAlg<Ty, Sym, Y> talg() {
		if (talg0 != null) {
			return talg0;
		}
		talg0 = talg0();
		// talg0.addAll(schema().typeSide.collage()); //j
		// talg0.validate();
		return talg0;
	}

	protected abstract TAlg<Ty, Sym, Y> talg0();

	private TAlg<Ty, Sym, Y> talg0;

	public abstract Chc<Sk, Pair<X, Att>> reprT_prot(Y y);

	public synchronized final Term<Ty, En, Sym, Fk, Att, Gen, Sk> reprT(Term<Ty, Void, Sym, Void, Void, Void, Y> y) {
		if (y.sk() != null) {
			Chc<Sk, Pair<X, Att>> x = reprT_prot(y.sk());
			if (x.left) {
				return Term.Sk(x.l);
			}
			return Term.Att(x.r.second, repr(schema().atts.get(x.r.second).first, x.r.first).convert());
		} else if (y.sym() != null) {
			List<Term<Ty, En, Sym, Fk, Att, Gen, Sk>> arr = new ArrayList<>(y.args.size());
			for (Term<Ty, Void, Sym, Void, Void, Void, Y> x : y.args) {
				arr.add(reprT(x));
			}
			Term<Ty, En, Sym, Fk, Att, Gen, Sk> ret = Term.Sym(y.sym(), arr);
			if (schema().typeSide.js.java_tys.isEmpty()) {
				return ret;
			}
			return schema().typeSide.js.reduce(ret);
		} else if (y.obj() != null) {
			return y.convert();
		}
		throw new RuntimeException("Please report, reprT on " + y);
	}

	/**
	 * @param term of type sort
	 */
	public final Term<Ty, Void, Sym, Void, Void, Void, Y> intoY(Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		if (term.obj() != null) {
			return term.convert();
		} else if (term.sym() != null) {
			List<Term<Ty, Void, Sym, Void, Void, Void, Y>> l = new ArrayList<>(term.args.size());
			for (Term<Ty, En, Sym, Fk, Att, Gen, Sk> x : term.args) {
				l.add(intoY(x));
			}
			return Term.Sym(term.sym(), l); // could js reduce here
		} else if (term.sk() != null) {
			return sk(term.sk());
		} else if (term.att() != null) {
			return att(term.att(), intoX(term.arg));
		}
		if (term.var != null) {
			return term.convert(); // for aggregation only
		}
		throw new RuntimeException("Anomaly: please report: " + term);
	}

	/**
	 * @param term term of type entity
	 */
	public synchronized X intoX(Term<Ty, En, Sym, Fk, Att, Gen, Sk> term) {
		if (term.gen() != null) {
			return nf(term.asGen());
		} else if (term.fk() != null) {
			return fk(term.fk(), nf(term.arg.asArgForFk()));
		}
		throw new RuntimeException("Anomaly: please report");
	}

	public abstract String toStringProver();

	public abstract Object printX(En en, X x);

	public abstract Object printY(Ty ty, Y y);

	private static <X> Collection<X> iterToColLazy(Iterable<X> it) {
		return new AbstractCollection<>() {
			@Override
			public Iterator<X> iterator() {
				return it.iterator();
			}

			@Override
			public int size() {
				return -1;
			}
		};
	}

	@Override
	public String toString() {
		String ret;

		ret = "carriers\n\t";
		ret += Util.sep(
				iterToColLazy(schema().ens).stream()
						.map(x -> x + " -> {"
								+ Util.sep(iterToColLazy(en(x)).stream().map(Object::toString)
										.collect(Collectors.toList()), ", ")
								+ "}")
						.collect(Collectors.toList()),
				"\n\t");

		ret += "\n\nforeign keys";
		for (Fk fk : schema().fks.keySet()) {
			ret += "\n\t" + fk + " -> {" + Util.sep(iterToColLazy(en(schema().fks.get(fk).first)).stream()
					.map(x -> "(" + x + ", " + fk(fk, x) + ")").collect(Collectors.toList()), ", ") + "}";
		}

		ret += "\n\nattributes";
		for (Att att : schema().atts.keySet()) {
			ret += "\n\t" + att + " -> {"
					+ Util.sep(iterToColLazy(en(schema().atts.get(att).first)).stream()
							.map(x -> "(" + x + ", " + att(att, x).toString() + ")").collect(Collectors.toList()), ", ")
					+ "}";
		}

		ret += "\n\n----- type algebra\n\n";
		ret += talg().toString();

		return ret;
	}

	public Pair<TObjectIntMap<X>, TIntObjectMap<X>> intifyX(int i) {
		Pair<TObjectIntMap<X>, TIntObjectMap<X>> intifyX = new Pair<>(new TObjectIntHashMap<>(),
				new TIntObjectHashMap<>());
		for (En en : schema().ens) {
			for (X x : en(en)) {
				intifyX.first.put(x, i);
				intifyX.second.put(i, x);
				i++;
			}
		}
		return intifyX;
	}

	private static int session_id = 0;
	private Connection conn;
	private Collection<String> indicesLoaded = new TreeList<>();

	private final Map<Fk, Set<Pair<X, X>>> fkAsSet0 = new THashMap<>();

	public synchronized Set<Pair<X, X>> fkAsSet(Fk fk) {
		if (fkAsSet0.containsKey(fk)) {
			return fkAsSet0.get(fk);
		}
		Set<Pair<X, X>> set = new THashSet<>();
		for (X x : en(schema().fks.get(fk).first)) {
			set.add(new Pair<>(x, fk(fk, x)));
		}
		fkAsSet0.put(fk, set);
		return set;
	}

	private final Map<Att, Set<Pair<X, Term<Ty, Void, Sym, Void, Void, Void, Y>>>> attsAsSet0 = new THashMap<>();

	public synchronized Set<Pair<X, Term<Ty, Void, Sym, Void, Void, Void, Y>>> attAsSet(Att att) {
		if (attsAsSet0.containsKey(att)) {
			return attsAsSet0.get(att);
		}
		Set<Pair<X, Term<Ty, Void, Sym, Void, Void, Void, Y>>> set = new THashSet<>();
		for (X x : en(schema().atts.get(att).first)) {
			set.add(new Pair<>(x, att(att, x)));
		}
		attsAsSet0.put(att, set);
		return set;
	}

	/**
	 * MUST close this connection
	 */
	public synchronized Connection createAndLoad(Map<En, List<String>> indices,
			Pair<TObjectIntMap<X>, TIntObjectMap<X>> j, int vlen) {
		try {
			Map<En, Triple<List<Chc<Fk, Att>>, List<String>, List<String>>> xxx = schema().toSQL("", "integer",
					Query.internal_id_col_name, true, vlen, "");
			// System.out.println(xxx);
			Connection conn = DriverManager.getConnection("jdbc:h2:mem:db_temp_" + session_id++ + ";DB_CLOSE_DELAY=-1");
			String tick = "";
			String idcol = Query.internal_id_col_name;
			// int truncate = -1;

			try (Statement stmt = conn.createStatement()) {
				for (En en1 : schema().ens) {
					Triple<List<Chc<Fk, Att>>, List<String>, List<String>> qqq = xxx.get(en1);
					for (String s : qqq.second) {
						stmt.execute(s);
					}
					for (String s : qqq.third) {
						// don't need fks for CQL's internal use
						if (!s.startsWith("alter table")) {
							stmt.execute(s);
						}
					}
					for (String s : indices.get(en1)) {
						stmt.execute(s);
					}
					List<Chc<Fk, Att>> header = qqq.first;
					List<String> hdrQ = new ArrayList<>(header.size() + 1);
					List<String> hdr = new ArrayList<>(header.size() + 1);

					hdr.add(tick + idcol + tick);
					hdrQ.add("?");
					for (Chc<Fk, Att> aHeader : header) {
						hdrQ.add("?");
						Chc<Fk, Att> chc = aHeader;
						hdr.add(tick + schema().truncate(chc, true) + tick); // TODO aql unsafe
					}
					for (X x : en(en1)) {
						storeMyRecord(hdrQ, hdr, j, conn, x, qqq.first, en1, "", "", true);
					}
				}

				stmt.close();
				// this.conn = conn;
				return conn;
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	/**
	 * DO NOT close this connection
	 */
	// client should not remove
	public synchronized Connection addIndices(Pair<TObjectIntMap<X>, TIntObjectMap<X>> j, Map<En, List<String>> indices,
			int vlen) {
		if (conn == null) {
			conn = createAndLoad(indices, j, vlen);
			this.indicesLoaded = new LinkedList<>();
			for (List<String> l : indices.values()) {
				this.indicesLoaded.addAll(l);
			}
			return conn;
		}
		try (Statement stmt = conn.createStatement()) {
			for (List<String> ss : indices.values()) {
				for (String s : ss) {
					if (!indicesLoaded.contains(s)) {
						stmt.execute(s);
						indicesLoaded.add(s);
					}
				}
			}
			stmt.close();
			return conn;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

	public synchronized void storeMyRecord(List<String> hdrQ, List<String> hdr,
			Pair<TObjectIntMap<X>, TIntObjectMap<X>> j, Connection conn2, X x, List<Chc<Fk, Att>> header, En en,
			String prefix, String tick, boolean truncate) throws Exception {

		StringBuffer sb = new StringBuffer("INSERT INTO ");
		sb.append(tick);
		sb.append(prefix);
		sb.append(schema().truncate(en, truncate));
		sb.append(tick);
		sb.append("(");
		boolean b = false;
		for (String o : hdr) {
			if (b) {
				sb.append(",");
			}
			b = true;
			sb.append(o);
		}
		sb.append(") values (");
		b = false;
		for (String o : hdrQ) {
			if (b) {
				sb.append(",");
			}
			b = true;
			sb.append(o);
		}
		sb.append(")");
		String insertSQL = sb.toString();
		PreparedStatement ps = conn2.prepareStatement(insertSQL);

		ps.setObject(1, j.first.get(x), Types.INTEGER);

		int i = 0;
		for (Chc<Fk, Att> chc : header) {
			if (chc.left) {
				ps.setObject(i + 1 + 1, j.first.get(fk(chc.l, x)), Types.INTEGER);
			} else {
				Object o = fromTerm(att(chc.r, x));
				ps.setObject(i + 1 + 1, o, SqlTypeSide.getSqlType(schema().atts.get(chc.r).second.toString()));
			}
			i++;
		}

		ps.executeUpdate();
		ps.close();
	}

	private Object fromTerm(Term<Ty, Void, Sym, Void, Void, Void, Y> term) {
		if (term.obj() != null) {
			return term.obj();
		}
		return null;
	}

	@Override
	protected void finalize() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public String talgToString() {
		return talg().toString();
	}

	public int sizeOfBiggest() {
		int ret = 0;
		for (En en : schema().ens) {
			int x = size(en);
			if (x > ret) {
				ret = x;
			}
		}
		return ret;
	}

	public int estimateNullSize(float factor) {
		int i = 0;
		for (En x : schema().ens) {
			int rows = size(x);
			int cols = schema().attsFrom(x).size();
			float f = rows * cols * factor;
			i += f;
		}
		return i;
	}

	public void validateMore() {

		for (En en : schema().ens) {
			int i = 0;
			List<X> l = new LinkedList<>();
			for (X x : en(en)) {
				i++;
				l.add(x);
			}
			int j = size(en);
			if (i != j) {
				throw new RuntimeException(
						"On entity " + en + ", given size is " + j + " but counted " + i + ": " + Util.sep(l, ", "));
			}
		}
	}

}
