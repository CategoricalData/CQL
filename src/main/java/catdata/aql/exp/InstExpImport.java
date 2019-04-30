package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.list.TreeList;

import catdata.Chc;
import catdata.IntRef;
import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Null;
import catdata.Pair;
import catdata.Raw;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Collage;
import catdata.aql.Eq;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Schema;
import catdata.aql.Term;
import catdata.aql.fdm.ImportAlgebra;
import catdata.aql.fdm.InitialAlgebra;
import catdata.aql.fdm.LiteralInstance;
import catdata.aql.fdm.SaturatedInstance;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public abstract class InstExpImport<Handle, Q> extends InstExp<Gen, Null<?>, Gen, Null<?>> implements Raw {

	public abstract boolean equals(Object o);

	public abstract int hashCode();

	@Override
	public Collection<InstExp<?, ?, ?, ?>> direct(AqlTyping G) {
		return Collections.emptySet();
	}

	private Map<String, List<InteriorLabel<Object>>> raw = new THashMap<>();

	@Override
	public Map<String, List<InteriorLabel<Object>>> raw() {
		return raw;
	}

	@SuppressWarnings("hiding")
	public static <En> Gen toGen(En en, String o, boolean b, String sep) {
		if (b) {
			return Gen.Gen(en + sep + o);
		}
		return Gen.Gen(o);
	}

	public static Gen toGen(En en, String o, AqlOptions op) {
		boolean b = (boolean) op.getOrDefault(AqlOption.prepend_entity_on_ids);
		String sep = (String) op.getOrDefault(AqlOption.import_col_seperator);
//		String pre = (String) op.getOrDefault(AqlOption.csv_import_prefix);
		return toGen(en, o, b, sep);
	}

	public Gen toGen(En en, String o) {
		return toGen(en, o, prepend_entity_on_ids, import_col_seperator);
	}

	public final SchExp schema;

	public final Map<String, String> options;

	public final Map<String, Q> map;

	@Override
	public Map<String, String> options() {
		return options;
	}

	public static IntRef counter = new IntRef(0);

	public InstExpImport(SchExp schema, List<Pair<LocStr, Q>> map, List<Pair<String, String>> options) {
		this.schema = schema;

		this.options = Util.toMapSafely(options);
		this.map = Util.toMapSafely(LocStr.set2(map));

		List<InteriorLabel<Object>> f = new TreeList<>();
		for (Pair<LocStr, Q> p : map) {
			f.add(new InteriorLabel<>("imports", new Pair<>(p.first.str, p.second), p.first.loc,
					x -> x.first + " -> " + x.second).conv());
		}
		raw.put("imports", f);
	}

	@Override
	public SchExp type(AqlTyping G) {
		return schema;
	}

	public static Term<Ty, Void, Sym, Void, Void, Void, Null<?>> objectToSk(Schema<Ty, En, Sym, Fk, Att> sch,
			Object rhs, Gen x, Att att, Map<Ty, Collection<Null<?>>> sks,
			Map<Null<?>, Term<Ty, En, Sym, Fk, Att, Gen, Null<?>>> extraRepr, boolean shouldJS, boolean errMeansNull) {
		Ty ty = sch.atts.get(att).second;
		if (rhs == null) {
			Null<?> n = new Null<>(Term.Att(att, Term.Gen(x)));
			// extraRepr.put(n, Term.Att(att, Term.Gen(x)));
			sks.get(ty).add(n);
			return Term.Sk(n);
		} else if (sch.typeSide.js.java_tys.containsKey(ty)) {
			if (shouldJS) {
				try {
					return Term.Obj(sch.typeSide.js.parse(ty, (String) rhs), ty);
				} catch (Exception ex) {
					if (errMeansNull) {
						return objectToSk(sch, null, x, att, sks, extraRepr, shouldJS, errMeansNull);
					}
					ex.printStackTrace();
					throw new RuntimeException("Error while importing " + rhs + " of class " + rhs.getClass()
							+ ".  Consider option import_null_on_err_unsafe.  Error was " + ex.getMessage());
				}
			}
			try {
				if (!Class.forName(sch.typeSide.js.java_tys.get(ty)).isInstance(rhs)) {
					if (errMeansNull) {
						return objectToSk(sch, null, x, att, sks, extraRepr, shouldJS, errMeansNull);
					}
					throw new RuntimeException("On " + x + "." + att + ", error while importing " + rhs + " of "
							+ rhs.getClass() + " was expecting " + sch.typeSide.js.java_tys.get(ty)
							+ ".\n\nConsider option " + AqlOption.import_null_on_err_unsafe);
				}

			} catch (ClassNotFoundException ex) {
				Util.anomaly();
			}
			return Term.Obj(rhs, ty);
		}
		return Util.anomaly();
	}

	protected AqlOptions op;

	protected String idCol;
	protected boolean import_as_theory;
	protected boolean isJoined;
	protected boolean nullOnErr;
	protected boolean prepend_entity_on_ids;
	protected String import_col_seperator;
	protected String prefix;
	protected boolean dont_check_closure;

	protected Map<En, Collection<Gen>> ens0;
	protected Map<Ty, Collection<Null<?>>> tys0;
	protected THashMap<Gen, Map<Fk, Gen>> fks0;
	protected THashMap<Gen, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Null<?>>>> atts0;
	protected Map<Null<?>, Term<Ty, En, Sym, Fk, Att, Gen, Null<?>>> extraRepr;

	@Override
	public synchronized Instance<Ty, En, Sym, Fk, Att, Gen, Null<?>, Gen, Null<?>> eval0(AqlEnv env, boolean isC) {
		Schema<Ty, En, Sym, Fk, Att> sch = schema.eval(env, isC);
		for (Ty ty : sch.typeSide.tys) {
			if (!sch.typeSide.js.java_tys.containsKey(ty)) {
				throw new RuntimeException("Import is only allowed onto java types");
			}
		}
		if (isC) {
			throw new IgnoreException();
		}

		op = new AqlOptions(options, null, env.defaults);

		import_as_theory = (boolean) op.getOrDefault(AqlOption.import_as_theory);
		isJoined = true; // (boolean) op.getOrDefault(AqlOption.import_joined);
		idCol = (String) op.getOrDefault(AqlOption.id_column_name);
		nullOnErr = (Boolean) op.getOrDefault(AqlOption.import_null_on_err_unsafe);
		prepend_entity_on_ids = (Boolean) op.getOrDefault(AqlOption.prepend_entity_on_ids);
		import_col_seperator = (String) op.getOrDefault(AqlOption.import_col_seperator);
		prefix = (String) op.getOrDefault(AqlOption.csv_import_prefix);
		dont_check_closure = (boolean) op.getOrDefault(AqlOption.import_dont_check_closure_unsafe);
		ens0 = Util.newSetsFor0(sch.ens);
		tys0 = Util.newSetsFor0(sch.typeSide.tys);
		fks0 = new THashMap<>();
		atts0 = new THashMap<>();
		extraRepr = null; // new THashMap<>();
		En last = null;

		try {
			Handle h = start(sch);

			if (!isJoined) {
				throw new RuntimeException("Unjoined form no longer supported.");
			}
			for (En en : sch.ens) {
				last = en;
				Q z = map.get(en.str);
				joinedEn(h, en, z, sch);

			}

			end(h);

		} catch (Exception exn) {
			exn.printStackTrace();
			String pre = "";
			if (last != null) {
				pre = "On entity " + last + ", ";
			}
			throw new RuntimeException(pre + exn.getMessage() + "\n\n" + getHelpStr());
		}

		if (import_as_theory) {
			return forTheory(sch, ens0, tys0, fks0, atts0, op);
		}

		ImportAlgebra<Ty, En, Sym, Fk, Att, Gen, Null<?>> alg = new ImportAlgebra<>(sch, ens0, tys0, fks0, atts0,
				(x, y) -> y, (x, y) -> y, dont_check_closure, Collections.emptySet());

		SaturatedInstance<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, catdata.aql.exp.Gen, Null<?>, catdata.aql.exp.Gen, Null<?>> x = new SaturatedInstance<>(
				alg, alg, (Boolean) op.getOrDefault(AqlOption.require_consistency),
				(Boolean) op.getOrDefault(AqlOption.allow_java_eqs_unsafe), true, extraRepr);
		// x.validate(); so don't trigger eqs
		// x.checkSatisfaction(); done in constructor
		return x;
	}

	protected abstract String getHelpStr();

	protected abstract Handle start(Schema<Ty, En, Sym, Fk, Att> sch) throws Exception;

	protected abstract void end(Handle h) throws Exception;

	protected abstract void shreddedAtt(Handle h, Att att, Q s, Schema<Ty, En, Sym, Fk, Att> sch) throws Exception;

	protected abstract void shreddedFk(Handle h, Fk fk, Q s, Schema<Ty, En, Sym, Fk, Att> sch) throws Exception;

	protected abstract void shreddedEn(Handle h, En en, Q s, Schema<Ty, En, Sym, Fk, Att> sch) throws Exception;

	protected abstract void joinedEn(Handle h, En en, Q s, Schema<Ty, En, Sym, Fk, Att> sch) throws Exception;

	@SuppressWarnings("hiding")
	public static <Ty, En, Sym, Fk, Att, Gen> Instance<Ty, En, Sym, Fk, Att, Gen, Null<?>, Gen, Null<?>> forTheory(
			Schema<Ty, En, Sym, Fk, Att> sch, Map<En, Collection<Gen>> ens0, Map<Ty, Collection<Null<?>>> tys0,
			Map<Gen, Map<Fk, Gen>> fks0, Map<Gen, Map<Att, Term<Ty, Void, Sym, Void, Void, Void, Null<?>>>> atts0,
			AqlOptions op) {

		Set<Pair<Term<Ty, En, Sym, Fk, Att, Gen, Null<?>>, Term<Ty, En, Sym, Fk, Att, Gen, Null<?>>>> eqs0 = new THashSet<>();
		Collage<Ty, En, Sym, Fk, Att, Gen, Null<?>> col = new Collage<>(sch.collage());
		for (Gen gen : fks0.keySet()) {
			for (Fk fk : fks0.get(gen).keySet()) {
				eqs0.add(new Pair<>(Term.Fk(fk, Term.Gen(gen)), Term.Gen(fks0.get(gen).get(fk))));
				col.eqs.add(new Eq<>(null, Term.Fk(fk, Term.Gen(gen)), Term.Gen(fks0.get(gen).get(fk))));
			}
		}
		for (Gen gen : atts0.keySet()) {
			for (Att att : atts0.get(gen).keySet()) {
				eqs0.add(new Pair<>(Term.Att(att, Term.Gen(gen)), atts0.get(gen).get(att).convert()));
				col.eqs.add(new Eq<>(null, Term.Att(att, Term.Gen(gen)), atts0.get(gen).get(att).convert()));
			}
		}
		for (En en : ens0.keySet()) {
			for (Gen gen : ens0.get(en)) {
				col.gens.put(gen, en);
			}
		}
		for (Ty ty : tys0.keySet()) {
			for (Null<?> sk : tys0.get(ty)) {
				col.sks.put(sk, ty);
			}
		}

		InitialAlgebra<Ty, En, Sym, Fk, Att, Gen, Null<?>> initial = new InitialAlgebra<>(op, sch, col,
				(x) -> x.toString(), (ty, x) -> x.toString());

		Instance<Ty, En, Sym, Fk, Att, Gen, Null<?>, Integer, Chc<Null<?>, Pair<Integer, Att>>> I = new LiteralInstance<>(
				sch, col.gens, col.sks, eqs0, initial.dp(), initial,
				(Boolean) op.getOrDefault(AqlOption.require_consistency),
				(Boolean) op.getOrDefault(AqlOption.allow_java_eqs_unsafe));

		@SuppressWarnings("unchecked")
		Instance<Ty, En, Sym, Fk, Att, Gen, Null<?>, Gen, Null<?>> J = (Instance<Ty, En, Sym, Fk, Att, Gen, Null<?>, Gen, Null<?>>) ((Object) I);

		return J;
	}

	@SuppressWarnings("unused")
	private void totalityCheck(Schema<Ty, En, Sym, Fk, Att> sch, Map<En, Q> ens, Map<Ty, Q> tys, Map<Att, Q> atts,
			Map<Fk, Q> fks) {
		for (En En : ens.keySet()) {
			if (!sch.ens.contains(En)) {
				throw new RuntimeException("there is an import for " + En + ", which is not an entity in the schema");
			}
		}
		for (Ty ty : tys.keySet()) {
			if (!sch.typeSide.tys.contains(ty)) {
				throw new RuntimeException("there is an import for " + ty + ", which is not a type in the schema");
			}
		}
		for (Att Att : atts.keySet()) {
			if (!sch.atts.containsKey(Att)) {
				throw new RuntimeException(
						"there is an import for " + Att + ", which is not an attribute in the schema");
			}
		}
		for (Fk Fk : fks.keySet()) {
			if (!sch.fks.containsKey(Fk)) {
				throw new RuntimeException(
						"there is an import for " + Fk + ", which is not a foreign key in the schema");
			}
		}
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return schema.deps();
	}

}
