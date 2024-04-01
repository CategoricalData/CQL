package catdata.cql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.list.TreeList;

import catdata.IntRef;
import catdata.InteriorLabel;
import catdata.LocStr;
import catdata.Pair;
import catdata.Raw;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Schema;
import catdata.cql.Term;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.fdm.ImportAlgebra;
import catdata.cql.fdm.SaturatedInstance;
import gnu.trove.map.hash.THashMap;

public abstract class InstExpImport<Handle, Q> extends InstExp<String, String, String, String> implements Raw {

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
	public static <En> String toGen(En en, String o, boolean b, String sep) {
		if (b) {
			return (en + sep + o);
		}
		return o;
	}

	public static String toGen(String en, String o, AqlOptions op) {
		boolean b = (boolean) op.getOrDefault(AqlOption.prepend_entity_on_ids);
		String sep = (String) op.getOrDefault(AqlOption.import_col_seperator);
//    String pre = (String) op.getOrDefault(AqlOption.csv_import_prefix);
		return toGen(en, o, b, sep);
	}

	public String toGen(String en, String o) {
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

	protected AqlOptions op;

	protected String idCol;
	protected boolean nullOnErr;
	protected boolean prepend_entity_on_ids;
	protected String import_col_seperator;
	protected String prefix;
	protected boolean dont_check_closure;

	protected Map<String, Collection<String>> tys0;

//  protected Map<String, Collection<String>> ens0;
//  protected Map<String, Map<String, Map<Fk, String>>> fks0;
//  protected Map<String, Map<String, Map<Att, Term<String, Void, Sym, Void, Void, Void, String>>>> atts0;

	protected Map<String, Map<String, Pair<Map<Fk, String>, Map<Att, Term<String, Void, Sym, Void, Void, Void, String>>>>> data;

	@Override
	public synchronized Instance<String, String, Sym, Fk, Att, String, String, String, String> eval0(AqlEnv env,
			boolean isC) {
		Schema<String, String, Sym, Fk, Att> sch = schema.eval(env, isC);
		for (String ty : sch.typeSide.tys) {
			if (!sch.typeSide.js.java_tys.containsKey(ty)) {
				throw new RuntimeException("Import is only allowed onto java types");
			}
		}
		if (isC) {
			throw new IgnoreException();
		}

		op = new AqlOptions(options, env.defaults);

		idCol = (String) op.getOrDefault(AqlOption.id_column_name);
		nullOnErr = (Boolean) op.getOrDefault(AqlOption.import_null_on_err_unsafe);
		prepend_entity_on_ids = (Boolean) op.getOrDefault(AqlOption.prepend_entity_on_ids);
		import_col_seperator = (String) op.getOrDefault(AqlOption.import_col_seperator);
		prefix = (String) op.getOrDefault(AqlOption.csv_import_prefix);
		dont_check_closure = (boolean) op.getOrDefault(AqlOption.import_dont_check_closure_unsafe);
		String last = null;

		try {
			Handle h = start(sch);
			for (String en : sch.ens) {
			//	System.out.println("doing " + en);
				last = en;
				Q z = map.get(en);
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
		ImportAlgebra<String, String, Sym, Fk, Att, String, String> alg = new ImportAlgebra<String, String, Sym, Fk, Att, String, String>(
				sch, en -> data.get(en).keySet(), tys0, (en, x) -> data.get(en).get(x).first,
				(en, x) -> data.get(en).get(x).second, (x, y) -> y, (x, y) -> y, dont_check_closure,
				Collections.emptySet());

		SaturatedInstance<String, String, Sym, Fk, Att, String, String, String, String> x = new SaturatedInstance<String, String, Sym, Fk, Att, String, String, String, String>(
				alg, alg, (Boolean) op.getOrDefault(AqlOption.require_consistency),
				(Boolean) op.getOrDefault(AqlOption.allow_java_eqs_unsafe), true, null);
		// x.validate(); so don't trigger eqs
		// x.checkSatisfaction(); done in constructor
		return x;
	}

	protected abstract String getHelpStr();

	protected abstract Handle start(Schema<String, String, Sym, Fk, Att> sch) throws Exception;

	protected abstract void end(Handle h) throws Exception;

	protected abstract void joinedEn(Handle h, String en, Q s, Schema<String, String, Sym, Fk, Att> sch)
			throws Exception;

	@SuppressWarnings("unused")
	private void totalityCheck(Schema<String, String, Sym, Fk, Att> sch, Map<String, Q> ens, Map<String, Q> tys,
			Map<Att, Q> atts, Map<Fk, Q> fks) {
		for (String En : ens.keySet()) {
			if (!sch.ens.contains(En)) {
				throw new RuntimeException("there is an import for " + En + ", which is not an entity in the schema");
			}
		}
		for (String ty : tys.keySet()) {
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
