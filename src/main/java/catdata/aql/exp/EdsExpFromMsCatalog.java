package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Chc;
import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Constraints;
import catdata.aql.DP;
import catdata.aql.ED;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.SqlTypeSide;
import catdata.aql.Term;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public final class EdsExpFromMsCatalog extends EdsExp {

	InstExp<String, Void, String, Void> I;
	Map<String, String> options;

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return I.deps();
	}

	public <R, P, E extends Exception> R accept(P param, EdsExpVisitor<R, P, E> v) throws E {
		return v.visit(param, this);
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public EdsExpFromMsCatalog(InstExp I, List<Pair<String, String>> ops) {
		this.I = I;
		this.options = Util.toMapSafely(ops);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((I == null) ? 0 : I.hashCode());
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
		EdsExpFromMsCatalog other = (EdsExpFromMsCatalog) obj;
		if (I == null) {
			if (other.I != null)
				return false;
		} else if (!I.equals(other.I))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

	@Override
	public Constraints eval0(AqlEnv env, boolean isC) {
		var J = I.eval(env, isC);
		Att table_name = Att.Att("TABLES", "TABLE_NAME");
		Att table_schema = Att.Att("TABLES", "TABLE_SCHEMA");

		Att column_name = Att.Att("COLUMNS", "COLUMN_NAME");
		Att column_table_name = Att.Att("COLUMNS", "TABLE_NAME");
		Att column_table_schema = Att.Att("COLUMNS", "TABLE_SCHEMA");
		Att column_data_type = Att.Att("COLUMNS", "DATA_TYPE");
		Att column_is_nullable = Att.Att("COLUMNS", "IS_NULLABLE");

		List<ED> eds = new LinkedList<>();

		Collection<String> ens = new LinkedList<>();
		Map<Att, Pair<String, String>> atts = new THashMap<>();
		Set<Att> notNull = new THashSet<>(J.algebra().size("COLUMNS"));

		String sep = (String) new AqlOptions(options, env.defaults).getOrDefault(AqlOption.import_col_seperator);
		// Map<Stromg, List<String>> keys = new THashMap<>(J.algebra().size("TABLES"));

		for (String table0 : J.algebra().en("TABLES")) {
			Term<String, Void, Sym, Void, Void, Void, Void> t = J.algebra().att(table_name, table0);
			if (t.obj() == null) {
				throw new RuntimeException("Encountered labelled null table name");
			}
			Optional<String> table_name0 = ((Optional<String>) t.obj());
			if (table_name0.isEmpty()) {
				throw new RuntimeException("Encountered null table name");
			}

			Term<String, Void, Sym, Void, Void, Void, Void> s = J.algebra().att(table_schema, table0);
			if (s.obj() == null) {
				throw new RuntimeException("Encountered labelled null schema name");
			}
			Optional<String> schema_name0 = ((Optional<String>) s.obj());
			String toAdd = schema_name0.isPresent() ? schema_name0.get() + sep + table_name0.get() : table_name0.get();

			ens.add(toAdd);

		}
		for (String column0 : J.algebra().en("COLUMNS")) {
			Term<String, Void, Sym, Void, Void, Void, Void> w = J.algebra().att(column_name, column0);
			if (w.obj() == null) {
				throw new RuntimeException("Encountered labelled null column name");
			}
			Optional<String> column_name0 = ((Optional<String>) w.obj());
			if (column_name0.isEmpty()) {
				throw new RuntimeException("Encountered null column name");
			}

			Term<String, Void, Sym, Void, Void, Void, Void> t = J.algebra().att(column_table_name, column0);
			Optional<String> table_name0 = ((Optional<String>) t.obj());
			if (table_name0.isEmpty()) {
				throw new RuntimeException("Encountered null column table name");
			}

			Term<String, Void, Sym, Void, Void, Void, Void> s = J.algebra().att(column_table_schema, column0);
			if (s.obj() == null) {
				throw new RuntimeException("Encountered labelled null column schema name");
			}
			Optional<String> schema_name0 = ((Optional<String>) s.obj());
			String toAdd = schema_name0.isPresent() ? schema_name0.get() + sep + table_name0.get() : table_name0.get();

			Term<String, Void, Sym, Void, Void, Void, Void> r = J.algebra().att(column_data_type, column0);
			if (r.obj() == null) {
				throw new RuntimeException("Encountered labelled null type name");
			}
			Optional<String> type_name0 = ((Optional<String>) r.obj());
			if (type_name0.isEmpty()) {
				throw new RuntimeException("Encountered null type name");
			}
			String ty = SchExpJdbcAll.sqlTypeToAqlType(J.schema().typeSide, type_name0.get());

			var theAtt = Att.Att(toAdd, column_name0.get());
			atts.put(theAtt, new Pair<>(toAdd, ty));

			Term<String, Void, Sym, Void, Void, Void, Void> n = J.algebra().att(column_is_nullable, column0);
			if (n.obj() == null) {
				throw new RuntimeException("Encountered labelled null nullability bit");
			}
			Optional<String> is_nullable0 = ((Optional<String>) n.obj());
			if (is_nullable0.isEmpty()) {
				throw new RuntimeException("Encountered null nullability name");
			}

			if (!Boolean.parseBoolean(is_nullable0.get())) {
				processNullable(env, eds, ty, theAtt);
				notNull.add(theAtt);
			}
		}

		Att table_constraints_table_name = Att.Att("TABLE_CONSTRAINTS", "TABLE_NAME");
		Att table_constraints_table_schema = Att.Att("TABLE_CONSTRAINTS", "TABLE_SCHEMA");
		Att table_constraints_type = Att.Att("TABLE_CONSTRAINTS", "CONSTRAINT_TYPE");
		Att table_constraints_name = Att.Att("TABLE_CONSTRAINTS", "CONSTRAINT_NAME");

		// (schema, constraint name)
		Map<Pair<String, String>, Unit> checks = new THashMap<>(J.algebra().size("TABLE_CONSTRAINTS"));
		Map<Pair<String, String>, List<Att>> uniques = new THashMap<>(J.algebra().size("TABLE_CONSTRAINTS"));
		Map<Pair<String, String>, List<Att>> pks = new THashMap<>(J.algebra().size("TABLE_CONSTRAINTS"));
		Map<Pair<String, String>, List<Pair<Att, Att>>> fks = new THashMap<>(J.algebra().size("TABLE_CONSTRAINTS"));
		Map<Pair<String, String>, List<String>> kcu_idx = new THashMap<>(J.algebra().size("TABLE_CONSTRAINTS"));
		for (String constraint0 : J.algebra().en("TABLE_CONSTRAINTS")) {
			Term<String, Void, Sym, Void, Void, Void, Void> t = J.algebra().att(table_constraints_table_name, constraint0);
			Optional<String> table_name0 = ((Optional<String>) t.obj());

			Term<String, Void, Sym, Void, Void, Void, Void> s = J.algebra().att(table_constraints_table_schema, constraint0);
			Optional<String> schema_name0 = ((Optional<String>) s.obj());
			String toAdd = schema_name0.isPresent() ? schema_name0.get() + sep + table_name0.get() : table_name0.get();

			Term<String, Void, Sym, Void, Void, Void, Void> w = J.algebra().att(table_constraints_name, constraint0);
			Optional<String> constraint_name0 = ((Optional<String>) w.obj());

			Term<String, Void, Sym, Void, Void, Void, Void> c = J.algebra().att(table_constraints_type, constraint0);
			Optional<String> type0 = ((Optional<String>) c.obj());

			switch (type0.get().toLowerCase()) {
				case "check":
					checks.put(new Pair<>(toAdd, constraint_name0.get()), Unit.unit);
					break;
				case "unique":
					uniques.put(new Pair<>(toAdd, constraint_name0.get()), new LinkedList<>());
					break;
				case "primary key":
					pks.put(new Pair<>(toAdd, constraint_name0.get()), new LinkedList<>());
					break;
				case "foreign key":
					fks.put(new Pair<>(schema_name0.get(), constraint_name0.get()), new LinkedList<>());
					break;
				default:
					return Util.anomaly();
			}
			kcu_idx.put(new Pair<>(schema_name0.get(), constraint_name0.get()), new LinkedList<>());

		}

		Att key_table_name = Att.Att("KEY_COLUMN_USAGE", "TABLE_NAME");
		Att key_table_schema = Att.Att("KEY_COLUMN_USAGE", "TABLE_SCHEMA");
		Att key_column_name = Att.Att("KEY_COLUMN_USAGE", "COLUMN_NAME");
		Att key_constraint_name = Att.Att("KEY_COLUMN_USAGE", "CONSTRAINT_NAME");
		Att key_column_pos = Att.Att("KEY_COLUMN_USAGE", "ORDINAL_POSITION");

		for (String key_usage0 : J.algebra().en("KEY_COLUMN_USAGE")) {
			Term<String, Void, Sym, Void, Void, Void, Void> c = J.algebra().att(key_constraint_name, key_usage0);
			Optional<String> constraint_name0 = ((Optional<String>) c.obj());

			Term<String, Void, Sym, Void, Void, Void, Void> t = J.algebra().att(key_table_name, key_usage0);
			Optional<String> table_name0 = ((Optional<String>) t.obj());

			Term<String, Void, Sym, Void, Void, Void, Void> s = J.algebra().att(key_table_schema, key_usage0);
			Optional<String> schema_name0 = ((Optional<String>) s.obj());
			String toAdd = schema_name0.isPresent() ? schema_name0.get() + sep + table_name0.get() : table_name0.get();

			Term<String, Void, Sym, Void, Void, Void, Void> w = J.algebra().att(key_column_name, key_usage0);
			Optional<String> column_name0 = ((Optional<String>) w.obj());

			var theAtt = Att.Att(toAdd, column_name0.get());
			var p = new Pair<>(toAdd, constraint_name0.get());

			if (checks.containsKey(p)) {
				// TODO ignore for now
			} else if (fks.containsKey(p)) {
			} else if (pks.containsKey(p)) {
				pks.get(p).add(theAtt);
			} else if (uniques.containsKey(p)) {
				uniques.get(p).add(theAtt);
			}
			kcu_idx.get(new Pair<>(schema_name0.get(), constraint_name0.get())).add(key_usage0);
		}

		for (Entry<Pair<String, String>, List<Att>> entry : pks.entrySet()) {
			processUniq(env, eds, atts, entry);
			for (Att theAtt : entry.getValue()) {
				if (!notNull.contains(theAtt)) {
					processNullable(env, eds, atts.get(theAtt).second, theAtt);
				}
			}
		}
		for (Entry<Pair<String, String>, List<Att>> entry : uniques.entrySet()) {
			processUniq(env, eds, atts, entry);
		}

		Att ref_constraint_schema = Att.Att("REFERENTIAL_CONSTRAINTS", "CONSTRAINT_SCHEMA");
		Att ref_constraint_name = Att.Att("REFERENTIAL_CONSTRAINTS", "CONSTRAINT_NAME");
		Att ref_target_constraint_schema = Att.Att("REFERENTIAL_CONSTRAINTS", "UNIQUE_CONSTRAINT_SCHEMA");
		Att ref_target_constraint_name = Att.Att("REFERENTIAL_CONSTRAINTS", "UNIQUE_CONSTRAINT_NAME");

		for (String ref : J.algebra().en("REFERENTIAL_CONSTRAINTS")) {
			Term<String, Void, Sym, Void, Void, Void, Void> r = J.algebra().att(ref_constraint_schema, ref);
			Optional<String> ref_constraint_schema0 = ((Optional<String>) r.obj());

			Term<String, Void, Sym, Void, Void, Void, Void> n = J.algebra().att(ref_constraint_name, ref);
			Optional<String> ref_constraint_name0 = ((Optional<String>) n.obj());

			Term<String, Void, Sym, Void, Void, Void, Void> ts = J.algebra().att(ref_target_constraint_schema, ref);
			Optional<String> ref_target_constraint_schema0 = ((Optional<String>) ts.obj());

			Term<String, Void, Sym, Void, Void, Void, Void> tn = J.algebra().att(ref_target_constraint_name, ref);
			Optional<String> ref_target_constraint_name0 = ((Optional<String>) tn.obj());


			var pp = new Pair<>(ref_constraint_schema0.get(), ref_constraint_name0.get());
			var src = kcu_idx.get(pp);
			if (src == null) {
				System.out.println(pp);
				System.out.println(kcu_idx);
				Util.anomaly();
			}
			var dst = kcu_idx.get(new Pair<>(ref_target_constraint_schema0.get(), ref_target_constraint_name0.get()));
			if (dst == null) {
				System.out.println(new Pair<>(ref_target_constraint_schema0.get(), ref_target_constraint_name0.get()));
				System.out.println(kcu_idx);
				Util.anomaly();
			}

			for (String kcu1 : src) {
				Term<String, Void, Sym, Void, Void, Void, Void> w = J.algebra().att(key_column_name, kcu1);
				Optional<String> column_name0 = ((Optional<String>) w.obj());

				Term<String, Void, Sym, Void, Void, Void, Void> t = J.algebra().att(key_table_name, kcu1);
				Optional<String> table_name0 = ((Optional<String>) t.obj());

				Term<String, Void, Sym, Void, Void, Void, Void> s = J.algebra().att(key_table_schema, kcu1);
				Optional<String> schema_name0 = ((Optional<String>) s.obj());
				String toAdd = schema_name0.isPresent() ? schema_name0.get() + sep + table_name0.get() : table_name0.get();

				var theAtt = Att.Att(toAdd, column_name0.get());

				Term<String, Void, Sym, Void, Void, Void, Void> pos = J.algebra().att(key_column_pos, kcu1);
				Optional<Integer> pos0 = ((Optional<Integer>) pos.obj());

				for (String kcu2 : dst) {
					Term<String, Void, Sym, Void, Void, Void, Void> d = J.algebra().att(key_column_name, kcu2);
					Optional<String> column_name0X = ((Optional<String>) d.obj());

					Term<String, Void, Sym, Void, Void, Void, Void> posX = J.algebra().att(key_column_pos, kcu2);
					Optional<Integer> pos0X = ((Optional<Integer>) posX.obj());

					if (pos0.get().equals(pos0X.get())) {
						Term<String, Void, Sym, Void, Void, Void, Void> tX = J.algebra().att(key_table_name, kcu2);
						Optional<String> table_name0X = ((Optional<String>) tX.obj());

						Term<String, Void, Sym, Void, Void, Void, Void> sX = J.algebra().att(key_table_schema, kcu2);
						Optional<String> schema_name0X = ((Optional<String>) sX.obj());
						String toAddX = schema_name0X.isPresent() ? schema_name0X.get() + sep + table_name0X.get() : table_name0X.get();

						var theAttX = Att.Att(toAddX, column_name0X.get());

						fks.get(pp).add(new Pair<>(theAtt, theAttX));
					}
				}
			}
		}

		for (List<Pair<Att, Att>> list : fks.values()) {
			if (list.isEmpty()) {
				continue;
			}
			String s = list.get(0).first.en;
			String t = list.get(0).second.en;

			String x = "s";
			String y = "t";

			Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> ewh = new THashSet<>();

			for (Pair<Att, Att> p : list) {
				Term<String, String, Sym, Fk, Att, Void, Void> l = Term.Att(p.first, Term.Var(x));
				Term<String, String, Sym, Fk, Att, Void, Void> r = Term.Att(p.second, Term.Var(y));
				Term<String, String, Sym, Fk, Att, Void, Void> j = Term.Sym(
						Sym.Sym("eq",
								new Pair<>(Util.list(atts.get(p.first).second, atts.get(p.first).second), SqlTypeSide.boolSort.second)),
						Util.list(l, r));
				ewh.add(new Pair<>(j, Term.Sym(SqlTypeSide.t, Collections.emptyList())));
			}

			var ed = new ED(Collections.singletonMap("s",Chc.inRight(s)), Collections.singletonMap("t",Chc.inRight(t)), Collections.emptySet(), ewh, false, env.defaults);

			eds.add(ed);
		}

		var ret = new Schema<String, String, Sym, Fk, Att>(J.schema().typeSide, ens, atts, Collections.emptyMap(),
				Collections.emptyList(), dp, false);

		Constraints ctr = new Constraints(ret, eds, env.defaults);
		// ctr.asTransforms(ctr.schema);
		return ctr;
	}

	private void processNullable(AqlEnv env, List<ED> eds, String ty, Att theAtt) {
		Term<String, String, Sym, Fk, Att, Void, Void> lhs = Term.Sym(
				Sym.Sym("isNull", new Pair<>(Collections.singletonList(ty), "Boolean")),
				Collections.singletonList(Term.Att(theAtt, Term.Var("v"))));
		Term<String, String, Sym, Fk, Att, Void, Void> rhs = Term.Sym(SqlTypeSide.f, Collections.emptyList());

		var ed = new ED(Collections.singletonMap("v", Chc.inRight(theAtt.en)), Collections.emptyMap(), Collections.emptySet(),
				Collections.singleton(new Pair<>(lhs, rhs)), false, env.defaults);
		eds.add(ed);
	}

	private void processUniq(AqlEnv env, List<ED> eds, Map<Att, Pair<String, String>> atts,
			Entry<Pair<String, String>, List<Att>> entry) {
		String en = entry.getKey().first;
		String x = "x";
		String y = "y";

		Map<String, Chc<String, String>> m = new THashMap<>();
		Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> awh = new THashSet<>();
		Set<Pair<Term<String, String, Sym, Fk, Att, Void, Void>, Term<String, String, Sym, Fk, Att, Void, Void>>> ewh = new THashSet<>();

		m.put(x, Chc.inRight(en));
		m.put(y, Chc.inRight(en));

		for (Att att : entry.getValue()) {
			Term<String, String, Sym, Fk, Att, Void, Void> l = Term.Att(att, Term.Var(x));
			Term<String, String, Sym, Fk, Att, Void, Void> r = Term.Att(att, Term.Var(y));
			Term<String, String, Sym, Fk, Att, Void, Void> j = Term.Sym(
					Sym.Sym("eq",
							new Pair<>(Util.list(atts.get(att).second, atts.get(att).second), SqlTypeSide.boolSort.second)),
					Util.list(l, r));
			awh.add(new Pair<>(j, Term.Sym(SqlTypeSide.t, Collections.emptyList())));
		}
		ewh.add(new Pair<>(Term.Var(x), Term.Var(y)));

		var ed = new ED(m, Collections.emptyMap(), awh, ewh, false, env.defaults);

		eds.add(ed);
	}

	private DP<String, String, Sym, Fk, Att, Void, Void> dp = new DP<>() {
		@Override
		public String toStringProver() {
			return "SchExpFromMsCatalog";
		}

		@Override
		public boolean eq(Map<String, Chc<String, String>> ctx, Term<String, String, Sym, Fk, Att, Void, Void> lhs,
				Term<String, String, Sym, Fk, Att, Void, Void> rhs) {
			return lhs.equals(rhs);
		}
	};

	@Override
	public String toString() {
		return "from_ms_catalog " + I;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		I.mapSubExps(f);
	}

	@Override
	public SchExp type(AqlTyping G) {
		return new SchExpFromMsCatalog(I, Collections.emptyList());
	}

}
