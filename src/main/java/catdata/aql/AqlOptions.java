package catdata.aql;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import catdata.Util;
import catdata.aql.AqlProver.ProverName;
import catdata.aql.exp.Att;
import catdata.aql.exp.En;
import catdata.aql.exp.Fk;
import catdata.aql.exp.Gen;
import catdata.aql.exp.Sk;
import catdata.aql.exp.Sym;
import catdata.aql.exp.Ty;
import gnu.trove.map.hash.THashMap;

public final class AqlOptions {

	private static List<AqlOption> proverOptionNames;

	public static synchronized Collection<AqlOption> proverOptionNames() {
		if (proverOptionNames != null) {
			return proverOptionNames;
		}
		proverOptionNames = new LinkedList<>();
		proverOptionNames.add(AqlOption.prover_simplify_max);
		proverOptionNames.add(AqlOption.allow_empty_sorts_unsafe);
		proverOptionNames.add(AqlOption.maedmax_path);
		proverOptionNames.add(AqlOption.program_allow_nonconfluence_unsafe);
		proverOptionNames.add(AqlOption.program_allow_nontermination_unsafe);
		proverOptionNames.add(AqlOption.completion_precedence);
		proverOptionNames.add(AqlOption.completion_sort);
		proverOptionNames.add(AqlOption.completion_compose);
		proverOptionNames.add(AqlOption.completion_filter_subsumed);
		proverOptionNames.add(AqlOption.completion_syntactic_ac);
		proverOptionNames.add(AqlOption.completion_unfailing);
		proverOptionNames.add(AqlOption.dont_verify_is_appropriate_for_prover_unsafe);
		proverOptionNames.add(AqlOption.prover);
		proverOptionNames.add(AqlOption.second_prover);
		proverOptionNames.add(AqlOption.e_path);

		return proverOptionNames;
	}

	private static List<String> optionNames;

	public static synchronized Collection<String> optionNames() {
		if (optionNames != null) {
			return optionNames;
		}
		List<String> optionNames = new LinkedList<>();
		for (AqlOption x : AqlOption.values()) {
			optionNames.add(x.toString());
		}
		optionNames.sort(Util.AlphabeticalComparator);
		return optionNames;
	}

	public static final AqlOptions initialOptions = new AqlOptions();

	public enum AqlOption { diverge_warn, diverge_limit,
		interpet_as_frozen, static_timeout, prover_simplify_max, talg_reduction, prover_allow_fresh_constants,
		second_prover, simple_query_entity, quotient_use_chase, chase_style, allow_empty_sorts_unsafe, maedmax_path,
		program_allow_nonconfluence_unsafe, gui_sample, gui_sample_size, import_dont_check_closure_unsafe, js_env_name,
		interpret_as_algebra, csv_field_delim_char, csv_escape_char, csv_quote_char, csv_file_extension,
		csv_generate_ids, csv_emit_ids, id_column_name, always_reload, varchar_length, gui_max_table_size,
		gui_max_graph_size, gui_max_string_size, gui_rows_to_display, gui_show_atts, random_seed, num_threads,
		eval_max_temp_size, eval_reorder_joins, eval_max_plan_depth, eval_join_selectivity, eval_use_indices,
		eval_use_sql_above, eval_approx_sql_unsafe, eval_sql_persistent_indices, query_remove_redundancy,
		import_as_theory, import_null_on_err_unsafe, simplify_names, left_bias, jdbc_quote_char,
		map_nulls_arbitrarily_unsafe, jdbc_default_class, jdbc_default_string, jdbc_no_distinct_unsafe,
		toCoQuery_max_term_size, program_allow_nontermination_unsafe, completion_precedence, completion_sort,
		completion_compose, completion_filter_subsumed, completion_syntactic_ac, allow_java_eqs_unsafe,
		require_consistency, timeout, dont_verify_is_appropriate_for_prover_unsafe, dont_validate_unsafe, static_typing,
		prover, start_ids_at, coproduct_allow_entity_collisions_unsafe, coproduct_allow_type_collisions_unsafe,
		import_col_seperator, csv_import_prefix, csv_prepend_entity, prepend_entity_on_ids, jdbc_export_truncate_after,
		import_missing_is_empty, jdbc_query_export_convert_type, e_path, completion_unfailing;

		private String getString(Map<String, String> map) {
			String n = map.get(toString());
			if (n == null) {
				throw new RuntimeException("No option named " + this + " in options");
			}
			return n;
		}

		public Boolean getBoolean(Map<String, String> map) {
			String s = getString(map).toLowerCase();
			if (s.equals("true")) {
				return true;
			} else if (s.equals("false")) {
				return false;
			}
			throw new RuntimeException("In " + map + ", neither true nor false: " + s);
		}

		/*
		 * public String getMaybeString(Map<String, String> map) { if
		 * (map.containsKey(this.toString())) { return getString(map); } return null; }
		 */
		public Character getChar(Map<String, String> map) {
			String s = getString(map);
			if (s.length() != 1) {
				throw new RuntimeException("Expected a character, instead received " + s);
			}
			return s.charAt(0);
		}

		public Float getFloat(Map<String, String> map) {
			return Float.parseFloat(getString(map));
		}

		public Integer getInteger(Map<String, String> map) {
			return Integer.parseInt(getString(map));
		}

		public Long getLong(Map<String, String> map) {
			return Long.parseLong(getString(map));
		}

		public Integer getNat(Map<String, String> map) {
			Integer ret = getInteger(map);
			if (ret < 0) {
				throw new RuntimeException("Expected non-zero integer for " + this);
			}
			return ret;
		}

		public static List<Head<Ty, En, Sym, Fk, Att, Gen, Sk>> getPrec(String str,
				Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col) {
			Util.assertNotNull(str);

			return (Arrays.asList(str.split("\\s+")).stream().map(x -> RawTerm.toHeadNoPrim(x, col))
					.collect(Collectors.toList()));
		}

		public ProverName getDPName(Map<String, String> map) {
			return ProverName.valueOf(getString(map));
		}

	}

	static final int numProc = Runtime.getRuntime().availableProcessors();

	public final Map<AqlOption, Object> options;

	private AqlOptions() {
		options = (new THashMap<>());
	}

	public AqlOptions(AqlOptions ops, AqlOption op, Object o) {
		options = (new THashMap<>(ops.options));
		options.put(op, o);
	}

	public AqlOptions(ProverName name) {
		options = (new THashMap<>());
		options.put(AqlOption.prover, name);
	}

	private String printDefault() {
		List<String> l = new LinkedList<>();
		for (AqlOption option : AqlOption.values()) {
			Object o = getDefault(option);
			if (o == null) {
				l.add(option + " has a null default ");
			} else {
				l.add(option + " = " + o);
			}
		}
		return Util.sep(l, "\n\t");
	}

	// anything 'unsafe' should default to false
	// @SuppressWarnings("static-method")
	private static Object getDefault(AqlOption option) {
		switch (option) {
		case left_bias:
			return false;
		case static_timeout:
			return 5L;
		// case lax_literals:
		// return false;
		case interpet_as_frozen:
			return false;
		case simplify_names:
			return true;
		case gui_show_atts:
			return false;
		case prover_allow_fresh_constants:
			return true;
		case talg_reduction:
			return Integer.MAX_VALUE;
		case prover_simplify_max:
			return 64;
		case jdbc_quote_char:
			return "\"";
		case simple_query_entity:
			return "Q";
		case program_allow_nonconfluence_unsafe:
			return false;
		case quotient_use_chase:
			return false;
		case jdbc_no_distinct_unsafe:
			return false;
		case jdbc_export_truncate_after:
			return -1;
		case prepend_entity_on_ids:
			return true;
		case csv_prepend_entity:
			return false;
		case import_null_on_err_unsafe:
			return false;
		case csv_import_prefix:
			return "";
		case import_missing_is_empty:
			return false;
		case import_col_seperator:
			return "_";
		case toCoQuery_max_term_size:
			return 3;
		case csv_generate_ids:
			return false;
		case csv_file_extension:
			return "csv";
		case start_ids_at:
			return 0;

		case map_nulls_arbitrarily_unsafe:
			return false;
		case coproduct_allow_type_collisions_unsafe:
			return false;
		case coproduct_allow_entity_collisions_unsafe:
			return false;
		case eval_max_temp_size:
			return 1024 * 1024 * 8;
		case import_as_theory:
			return false;
		case eval_reorder_joins:
			return true;
		case allow_java_eqs_unsafe:
			return false;
		case num_threads:
			return numProc;
		case random_seed:
			return 0;
		case completion_precedence:
			return null;
		case prover:
			return ProverName.auto;
		case dont_validate_unsafe:
			return false;
		case require_consistency:
			return true;
		case timeout:
			return 30L;
		case dont_verify_is_appropriate_for_prover_unsafe:
			return false;
		case completion_compose:
			return true;
		case completion_filter_subsumed:
			return true;
		case completion_sort:
			return true;
		case completion_syntactic_ac:
			return false;
		case static_typing:
			return false;
		case always_reload:
			return false;
		case csv_escape_char:
			return '\\';
		case csv_field_delim_char:
			return ',';

		case id_column_name:
			return "id";

		case csv_quote_char:
			return '\"';
		case varchar_length:
			return 256;

		case program_allow_nontermination_unsafe:
			return false;
		case gui_max_table_size:
			return 1024;
		case gui_max_string_size:
			return 8096;
		case gui_max_graph_size:
			return 128;
		case eval_join_selectivity:
			return 0.5f;
		case eval_max_plan_depth:
			return 8;
		case eval_use_indices:
			return true;
		case gui_rows_to_display:
			return 32;
		case query_remove_redundancy:
			return true;
		case eval_sql_persistent_indices:
			return false;
		case jdbc_default_class:
			return "org.h2.Driver";
		case jdbc_query_export_convert_type:
			return "varchar";
		case jdbc_default_string:
			return "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1";
		case interpret_as_algebra:
			return false;
		case js_env_name:
			return "aql_env";
		case import_dont_check_closure_unsafe:
			return false;
		case gui_sample:
			return true;
		case gui_sample_size:
			return 2;

		case eval_approx_sql_unsafe:
			return false;
		case eval_use_sql_above:
			return 16 * 1024;
		case maedmax_path:
			return "/home/ryan/maedmax/maedmax";
		case allow_empty_sorts_unsafe:
			return false;
		case chase_style:
			return "parallel";
		case csv_emit_ids:
			return true;
		case e_path:
			return "/usr/local/bin/eprover";
		case completion_unfailing:
			return true;
		case second_prover:
			return ProverName.auto;
		case diverge_limit:
			return 32;
		case diverge_warn:
			return true;
		default:
			throw new RuntimeException("Anomaly: please report: " + option);
		}

	}

	public Object getOrDefault(Map<String, String> map, AqlOption op) {
		if (map.containsKey(op.toString())) {
			return getFromMap(map, null, op);
		} else if (options.containsKey(op)) {
			return options.get(op);
		}
		return getDefault(op);
	}

	/**
	 * @param map
	 * @param col possibly null
	 */
	public AqlOptions(Map<String, String> map, @SuppressWarnings("rawtypes") Collage col, AqlOptions defaults) {
		options = new THashMap<>(defaults.options);
		for (String key : map.keySet()) {
			AqlOption op = AqlOption.valueOf(key);
			@SuppressWarnings("unchecked")
			Object ob = getFromMap(map, col, op);
			options.put(op, ob);
		}
	}

	private static Object getFromMap(Map<String, String> map, Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col,
			AqlOption op) {
		switch (op) {
		case left_bias:
			return op.getBoolean(map);
		case static_timeout:
			return op.getLong(map);
		// case lax_literals:
		// return op.getBoolean(map);
		case interpet_as_frozen:
			return op.getBoolean(map);
		case simplify_names:
			return op.getBoolean(map);
		case gui_show_atts:
			return op.getBoolean(map);
		case second_prover:
			return op.getDPName(map);
		case talg_reduction:
			return op.getInteger(map);
		case prover_simplify_max:
			return op.getInteger(map);
		case jdbc_quote_char:
			return op.getString(map);
		case simple_query_entity:
			return op.getString(map);
		case program_allow_nonconfluence_unsafe:
			return op.getBoolean(map);
		case quotient_use_chase:
			return op.getBoolean(map);
		case jdbc_query_export_convert_type:
			return op.getString(map);
		case jdbc_no_distinct_unsafe:
			return op.getBoolean(map);
		case jdbc_export_truncate_after:
			return op.getInteger(map);
		case prepend_entity_on_ids:
			return op.getBoolean(map);
		case csv_prepend_entity:
			return op.getBoolean(map);
		case import_null_on_err_unsafe:
			return op.getBoolean(map);
		case csv_import_prefix:
			return op.getString(map);
		case import_missing_is_empty:
			return op.getBoolean(map);
		case import_col_seperator:
			return op.getString(map);
		case toCoQuery_max_term_size:
			return op.getBoolean(map);
		case csv_generate_ids:
			return op.getBoolean(map);
		case csv_file_extension:
			return op.getString(map);
		case map_nulls_arbitrarily_unsafe:
			return op.getBoolean(map);
		case coproduct_allow_type_collisions_unsafe:
			return op.getBoolean(map);
		case coproduct_allow_entity_collisions_unsafe:
			return op.getBoolean(map);
		case import_as_theory:
			return op.getBoolean(map);
		case start_ids_at:
			return op.getInteger(map);
		case eval_max_temp_size:
			return op.getInteger(map);
		case eval_reorder_joins:
			return op.getBoolean(map);
		case num_threads:
			return op.getInteger(map);
		case gui_max_table_size:
			return op.getInteger(map);
		case gui_max_graph_size:
			return op.getInteger(map);
		case gui_max_string_size:
			return op.getInteger(map);
		case random_seed:
			return op.getInteger(map);
		case allow_java_eqs_unsafe:
			return op.getBoolean(map);
		case completion_precedence:
			return AqlOption.getPrec(map.get(op.toString()), col);
		case prover:
			return op.getDPName(map);
		case require_consistency:
			return op.getBoolean(map);
		case timeout:
			return op.getLong(map);
		case dont_verify_is_appropriate_for_prover_unsafe:
			return op.getBoolean(map);
		case completion_compose:
			return op.getBoolean(map);
		case completion_filter_subsumed:
			return op.getBoolean(map);
		case completion_sort:
			return op.getBoolean(map);
		case completion_syntactic_ac:
			return op.getBoolean(map);
		case dont_validate_unsafe:
			return op.getBoolean(map);
		case static_typing:
			return op.getBoolean(map);
		case always_reload:
			return op.getBoolean(map);
		case csv_escape_char:
			return op.getChar(map);
		case csv_field_delim_char:
			return op.getChar(map);
		case id_column_name:
			return op.getString(map);
		case csv_quote_char:
			return op.getChar(map);
		case varchar_length:
			return op.getInteger(map);
		case program_allow_nontermination_unsafe:
			return op.getBoolean(map);
		case eval_join_selectivity:
			return op.getFloat(map);
		case eval_max_plan_depth:
			return op.getInteger(map);
		case eval_use_indices:
			return op.getBoolean(map);
		case gui_rows_to_display:
			return op.getInteger(map);
		case query_remove_redundancy:
			return op.getBoolean(map);
		case eval_approx_sql_unsafe:
			return op.getBoolean(map);
		case eval_use_sql_above:
			return op.getInteger(map);
		case eval_sql_persistent_indices:
			return op.getBoolean(map);
		case jdbc_default_class:
			return op.getString(map);
		case jdbc_default_string:
			return op.getString(map);
		case interpret_as_algebra:
			return op.getBoolean(map);
		case js_env_name:
			return op.getString(map);
		case import_dont_check_closure_unsafe:
			return op.getBoolean(map);
		case gui_sample:
			return op.getBoolean(map);
		case gui_sample_size:
			return op.getInteger(map);
		case maedmax_path:
			return op.getString(map);
		case allow_empty_sorts_unsafe:
			return op.getBoolean(map);
		case chase_style:
			return op.getString(map);
		case csv_emit_ids:
			return op.getBoolean(map);
		case e_path:
			return op.getString(map);
		case completion_unfailing:
			return op.getBoolean(map);
		case prover_allow_fresh_constants:
			return op.getBoolean(map);
		case diverge_limit:
			return op.getInteger(map);
		case diverge_warn:
			return op.getBoolean(map);
		default:
			break;
		}
		throw new RuntimeException("Anomaly: please report");
	}

	public Object getOrDefault(AqlOption op) {
		Object o = options.get(op);
		if (o == null) {
			return getDefault(op);
		}
		return o;
	}

	public Object get(AqlOption op) {
		Object o = options.get(op);
		if (o == null) {
			throw new RuntimeException("Missing required option " + op);
		}
		return o;
	}

	@Override
	public int hashCode() {
		return options.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AqlOptions other = (AqlOptions) obj;
		return options.equals(other.options);
	}

	@Override
	public String toString() {
		return Util.sep(options, " = ", "\n");
	}

	static String msg0 = "completion_precedence = \"a b c\" means a < b < c";
	static String msg1 = msg0 + "\n\nAvailable provers: " + Arrays.toString(ProverName.values());
	static String msg = msg1
			+ "\n\nOption descriptions are available in the CQL manual, see categoricaldata.net/cql.html";

	public static String getMsg() {
		return "Options are specified in each CQL expression.\nHere are the available options and their defaults:\n\n\t"
				+ new AqlOptions().printDefault() + "\n\n" + msg;
	}

}
