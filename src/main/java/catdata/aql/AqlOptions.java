package catdata.aql;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import catdata.Util;
import catdata.aql.AqlProver.ProverName;
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
		proverOptionNames.add(AqlOption.vampire_path);
		proverOptionNames.add(AqlOption.triviality_check_best_effort);

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

	public static AqlOptions initialOptions = new AqlOptions(); // removed final for sql checker

	public enum AqlOption {
		allow_sql_import_all_unsafe, jena_reasoner, allow_aggregation_unsafe, import_sql_direct_prefix,
		fast_consistency_check, diverge_warn, diverge_limit, csv_entity_name, interpet_as_frozen, static_timeout,
		prover_simplify_max, talg_reduction, prover_allow_fresh_constants, second_prover, simple_query_entity,
		quotient_use_chase, chase_style, allow_empty_sorts_unsafe, maedmax_path, program_allow_nonconfluence_unsafe,
		gui_sample, gui_sample_size, import_dont_check_closure_unsafe, js_env_name, interpret_as_algebra,
		csv_field_delim_char, csv_escape_char, csv_quote_char, csv_file_extension, csv_generate_ids, csv_emit_ids,
		id_column_name, always_reload, varchar_length, gui_max_table_size, gui_max_graph_size, gui_max_string_size,
		gui_rows_to_display, gui_show_atts, random_seed, num_threads, eval_max_temp_size, eval_reorder_joins,
		eval_max_plan_depth, eval_join_selectivity, eval_use_indices, eval_use_sql_above, eval_approx_sql_unsafe,
		eval_sql_persistent_indices, query_remove_redundancy, import_as_theory, import_null_on_err_unsafe,
		simplify_names, left_bias, jdbc_quote_char, map_nulls_arbitrarily_unsafe, jdbc_default_class,
		jdbc_default_string, jdbc_no_distinct_unsafe, toCoQuery_max_term_size, program_allow_nontermination_unsafe,
		completion_precedence, completion_sort, completion_compose, completion_filter_subsumed, completion_syntactic_ac,
		allow_java_eqs_unsafe, require_consistency, timeout, dont_verify_is_appropriate_for_prover_unsafe,
		dont_validate_unsafe, static_typing, prover, start_ids_at, coproduct_allow_entity_collisions_unsafe,
		coproduct_allow_type_collisions_unsafe, import_col_seperator, csv_import_prefix, csv_prepend_entity,
		prepend_entity_on_ids, jdbc_export_truncate_after, import_missing_is_empty, jdbc_query_export_convert_type,
		e_path, vampire_path, completion_unfailing, graal_language, triviality_check_best_effort, check_command_export_file;

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

		public ProverName getDPName(Map<String, String> map) {
			return ProverName.valueOf(getString(map));
		}

	}

	static final int numProc = Runtime.getRuntime().availableProcessors();

	public final Map<AqlOption, Object> options;

	private AqlOptions() {
		options = new THashMap<>();
	}

	public AqlOptions(AqlOptions ops, AqlOption op, Object o) {
		options = new THashMap<>(ops.options);
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
		return switch (option) {
		case allow_sql_import_all_unsafe -> false;
		case allow_aggregation_unsafe -> false;
		case csv_entity_name -> "E";
		case import_sql_direct_prefix -> "";
		case left_bias -> false;
		case static_timeout -> 5L;
		case fast_consistency_check -> true;
		case jena_reasoner -> "";
		case check_command_export_file -> "";
		// case lax_literals -> false;
		case interpet_as_frozen -> false;
		case simplify_names -> true;
		case gui_show_atts -> false;
		case prover_allow_fresh_constants -> true;
		case talg_reduction -> Integer.MAX_VALUE;
		case prover_simplify_max -> 64;
		case jdbc_quote_char -> "\"";
		case simple_query_entity -> "Q";
		case program_allow_nonconfluence_unsafe -> false;
		case quotient_use_chase -> false;
		case jdbc_no_distinct_unsafe -> false;
		case jdbc_export_truncate_after -> -1;
		case prepend_entity_on_ids -> true;
		case csv_prepend_entity -> false;
		case import_null_on_err_unsafe -> false;
		case csv_import_prefix -> "";
		case import_missing_is_empty -> true;
		case import_col_seperator -> "_";
		case toCoQuery_max_term_size -> 3;
		case csv_generate_ids -> false;
		case csv_file_extension -> "csv";
		case start_ids_at -> 0;

		case map_nulls_arbitrarily_unsafe -> false;
		case coproduct_allow_type_collisions_unsafe -> false;
		case coproduct_allow_entity_collisions_unsafe -> false;
		case eval_max_temp_size -> 1024 * 1024 * 8;
		case import_as_theory -> false;
		case eval_reorder_joins -> true;
		case allow_java_eqs_unsafe -> false;
		case num_threads -> numProc;
		case random_seed -> 0;
		case completion_precedence -> null;
		case prover -> ProverName.auto;
		case dont_validate_unsafe -> false;
		case require_consistency -> true;
		case timeout -> 30L;
		case dont_verify_is_appropriate_for_prover_unsafe -> false;
		case completion_compose -> true;
		case completion_filter_subsumed -> true;
		case completion_sort -> true;
		case completion_syntactic_ac -> false;
		case static_typing -> false;
		case always_reload -> false;
		case csv_escape_char -> '\\';
		case csv_field_delim_char -> ',';

		case id_column_name -> "id";

		case csv_quote_char -> '\"';
		case varchar_length -> 256;

		case program_allow_nontermination_unsafe -> false;
		case gui_max_table_size -> 16384;
		case gui_max_string_size -> 8096 * 8;
		case gui_max_graph_size -> 512 * 10;
		case eval_join_selectivity -> 0.5f;
		case eval_max_plan_depth -> 8;
		case eval_use_indices -> true;
		case gui_rows_to_display -> 128;
		case query_remove_redundancy -> true;
		case eval_sql_persistent_indices -> false;
		case jdbc_default_class -> "org.h2.Driver";
		case jdbc_query_export_convert_type -> "varchar";
		case jdbc_default_string -> "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1";
		case interpret_as_algebra -> false;
		case js_env_name -> "aql_env";
		case import_dont_check_closure_unsafe -> false;
		case gui_sample -> true;
		case gui_sample_size -> 2;
		case graal_language -> ExternalCodeUtils.LANG_JS;
		case triviality_check_best_effort -> true;

		case eval_approx_sql_unsafe -> false;
		case eval_use_sql_above -> 16 * 1024;
		case maedmax_path -> "/home/ryan/maedmax/maedmax";
		case allow_empty_sorts_unsafe -> false;
		case chase_style -> "parallel";
		case csv_emit_ids -> true;
		case e_path -> "/usr/local/bin/eprover";
		case vampire_path -> "/usr/local/bin/vampire";
		case completion_unfailing -> true;
		case second_prover -> ProverName.auto;
		case diverge_limit -> 32;
		case diverge_warn -> true;
		};
	}

	public Object getOrDefault(Map<String, String> map, AqlOption op) {
		if (map.containsKey(op.toString())) {
			return getFromMap(map, op);
		} else if (options.containsKey(op)) {
			return options.get(op);
		}
		return getDefault(op);
	}

	/**
	 * @param map
	 * @param col possibly null
	 */
	public AqlOptions(Map<String, String> map, AqlOptions defaults) {
		options = new THashMap<>(defaults.options);
		for (String key : map.keySet()) {
			AqlOption op = AqlOption.valueOf(key);
			@SuppressWarnings("unchecked")
			Object ob = getFromMap(map, op);
			options.put(op, ob);
		}
	}

	private static Object getFromMap(Map<String, String> map, /* Collage<Ty, En, Sym, Fk, Att, Gen, Sk> col, */
			AqlOption op) {
		return switch (op) {
		case allow_aggregation_unsafe -> op.getBoolean(map);
		case fast_consistency_check -> op.getBoolean(map);
		case csv_entity_name -> op.getString(map);
		case left_bias -> op.getBoolean(map);
		case static_timeout -> op.getLong(map);
		case jena_reasoner -> op.getString(map);
		case allow_sql_import_all_unsafe -> op.getBoolean(map);
		case import_sql_direct_prefix -> op.getString(map);
		// case lax_literals:
		// return op.getBoolean(map);
		case interpet_as_frozen -> op.getBoolean(map);
		case simplify_names -> op.getBoolean(map);
		case gui_show_atts -> op.getBoolean(map);
		case second_prover -> op.getDPName(map);
		case talg_reduction -> op.getInteger(map);
		case prover_simplify_max -> op.getInteger(map);
		case jdbc_quote_char -> op.getString(map);
		case simple_query_entity -> op.getString(map);
		case check_command_export_file -> op.getString(map);
		case program_allow_nonconfluence_unsafe -> op.getBoolean(map);
		case quotient_use_chase -> op.getBoolean(map);
		case jdbc_query_export_convert_type -> op.getString(map);
		case jdbc_no_distinct_unsafe -> op.getBoolean(map);
		case jdbc_export_truncate_after -> op.getInteger(map);
		case prepend_entity_on_ids -> op.getBoolean(map);
		case csv_prepend_entity -> op.getBoolean(map);
		case import_null_on_err_unsafe -> op.getBoolean(map);
		case csv_import_prefix -> op.getString(map);
		case import_missing_is_empty -> op.getBoolean(map);
		case import_col_seperator -> op.getString(map);
		case toCoQuery_max_term_size -> op.getBoolean(map);
		case csv_generate_ids -> op.getBoolean(map);
		case csv_file_extension -> op.getString(map);
		case map_nulls_arbitrarily_unsafe -> op.getBoolean(map);
		case coproduct_allow_type_collisions_unsafe -> op.getBoolean(map);
		case coproduct_allow_entity_collisions_unsafe -> op.getBoolean(map);
		case import_as_theory -> op.getBoolean(map);
		case start_ids_at -> op.getInteger(map);
		case eval_max_temp_size -> op.getInteger(map);
		case eval_reorder_joins -> op.getBoolean(map);
		case num_threads -> op.getInteger(map);
		case gui_max_table_size -> op.getInteger(map);
		case gui_max_graph_size -> op.getInteger(map);
		case gui_max_string_size -> op.getInteger(map);
		case random_seed -> op.getInteger(map);
		case allow_java_eqs_unsafe -> op.getBoolean(map);
		case completion_precedence -> map.get(op.toString());
		case prover -> op.getDPName(map);
		case require_consistency -> op.getBoolean(map);
		case timeout -> op.getLong(map);
		case dont_verify_is_appropriate_for_prover_unsafe -> op.getBoolean(map);
		case completion_compose -> op.getBoolean(map);
		case completion_filter_subsumed -> op.getBoolean(map);
		case completion_sort -> op.getBoolean(map);
		case completion_syntactic_ac -> op.getBoolean(map);
		case dont_validate_unsafe -> op.getBoolean(map);
		case static_typing -> op.getBoolean(map);
		case always_reload -> op.getBoolean(map);
		case csv_escape_char -> op.getChar(map);
		case csv_field_delim_char -> op.getChar(map);
		case id_column_name -> op.getString(map);
		case csv_quote_char -> op.getChar(map);
		case varchar_length -> op.getInteger(map);
		case program_allow_nontermination_unsafe -> op.getBoolean(map);
		case eval_join_selectivity -> op.getFloat(map);
		case eval_max_plan_depth -> op.getInteger(map);
		case eval_use_indices -> op.getBoolean(map);
		case gui_rows_to_display -> op.getInteger(map);
		case query_remove_redundancy -> op.getBoolean(map);
		case eval_approx_sql_unsafe -> op.getBoolean(map);
		case eval_use_sql_above -> op.getInteger(map);
		case eval_sql_persistent_indices -> op.getBoolean(map);
		case jdbc_default_class -> op.getString(map);
		case jdbc_default_string -> op.getString(map);
		case interpret_as_algebra -> op.getBoolean(map);
		case js_env_name -> op.getString(map);
		case import_dont_check_closure_unsafe -> op.getBoolean(map);
		case gui_sample -> op.getBoolean(map);
		case gui_sample_size -> op.getInteger(map);
		case maedmax_path -> op.getString(map);
		case allow_empty_sorts_unsafe -> op.getBoolean(map);
		case chase_style -> op.getString(map);
		case csv_emit_ids -> op.getBoolean(map);
		case e_path -> op.getString(map);
		case vampire_path -> op.getString(map);
		case completion_unfailing -> op.getBoolean(map);
		case prover_allow_fresh_constants -> op.getBoolean(map);
		case diverge_limit -> op.getInteger(map);
		case diverge_warn -> op.getBoolean(map);
		case graal_language -> op.getString(map);
		case triviality_check_best_effort -> op.getBoolean(map);
		};
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
	static String msg2 = msg1 + "\n\nAvailable Graal languages: "
			+ org.graalvm.polyglot.Engine.create().getLanguages().keySet();
	static String msg = msg2
			+ "\n\nOption descriptions are available in the CQL manual, see http://categoricaldata.net";

	public static String getMsg() {
		return "Options are specified in each CQL expression.\nHere are the available options and their defaults:\n\n\t"
				+ new AqlOptions().printDefault() + "\n\n" + msg;
	}

}
