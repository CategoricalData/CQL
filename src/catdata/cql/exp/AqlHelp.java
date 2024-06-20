package catdata.cql.exp;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import catdata.Pair;
import catdata.Program;
import catdata.Unit;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.AqlSyntax;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.AqlOptions.AqlOption;
import catdata.cql.AqlProver.ProverName;
import catdata.ide.Example;
import catdata.ide.Examples;
import catdata.ide.Language;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class AqlHelp {

	static String xxx = """
			Associated with each object is a size, according to its kind:
			  - command and constraints: length of the text string
			  - instance: number of rows in the tables
			  - schemas and colimits of schema: number of the entities, foreign keys, attributes, and equations
			  - graph: number of nodes and edges
			  - typeside: number of types, functions, and equations
			  - mapping and transform and query: size of source
			""";

	private static String allProver() {
		StringBuffer sb = new StringBuffer();
		for (ProverName p : Util.alphabetical(Arrays.asList(ProverName.class.getEnumConstants()))) {
			sb.append(p.name() + " : " + getProverText(p) + "\n\n");
		}
		return sb.toString();
	}

	private static String getProverText(ProverName p) {
		switch (p) {
		case auto:
			return "The auto theorem proving method attempts the free, congruence, monoidal, and program methods, in that order.";
		case completion:
			return "Applies unfailing (ordered) Knuth-Bendix completion specialized to lexicographic path ordering.  If no completion precedence is given, attempts to infer a precedence using constraint-satisfaction techniques.";
		case congruence:
			return "Applies only to ground (variable-free) theories.  Uses the classical Nelson-Oppen congruence-closure-with-union-find algorithm.";
		case e:
			return "Uses the E prover.  Must be installed.";
		case fail:
			return "Applies to all theories.  Always fails with an exception.";
		case free:
			return "Applies only to theories without equations.  Equivalence is implemented as the syntactic equality of two terms.";
		/* START CLOSED SOURCE */
		// case jaedmax:
		// return "Uses the jaedmax prover. Does not work with empty sorts.";
		// case maedmax:
		// return "Uses the maedmax prover. Must be installed. Support for maedmax will
		// be ending in early 2021.";
		/* END CLOSED SOURCE */
		case monoidal:
			return "Applies to theories where all equations are monadic (unary) or ground.  Applies Knuth-Bendix completion specialized to semi-Thue systems.";
		case program:
			return "Applies only to weakly orthogonal theories.  Interprets all equations p = q as rewrite rules p -> q if p is syntactically smaller than q and q -> p if q is syntactically smaller than p and symbolically evaluates terms to normal forms.  Fails if an equation cannot be oriented.";
		case vampire:
			return "Experimental.";
		}
		return Util.anomaly();
	}

	static String getOptionText2(AqlOption op) {

		switch (op) {
		case tinkerpop_host:
			return "Sets the Tinkerpop hostname.";
		case tinkerpop_port:
			return "Sets the Tinkerpop port.";
		case tinkerpop_graph_name:
			return "Sets the Tinkerpop graph name.";
		case bitsy_db_path:
			return "Sets the Bitsy database file path.";
		case allow_aggregation_unsafe:
			return "Enables aggregation, which is not functorial.  Associativity, commutativity, and unitality of the given aggegrates are not checked for those properties by the prover.";
		case left_bias:
			return "Use the left-biased schema colimit renaming algorithm.";
		case fast_consistency_check:
			return "Use a faster but less accurate conservativity/consistency check.";
		case static_timeout:
			return "Timeout to use for spellchecker.";
		case simplify_names:
			return "Whether to simplify the names of entities etc in schema colimits.";
		case prover_simplify_max:
			return "Before going to the theorem prover, simplify collages less than this size.";
		case jdbc_quote_char:
			return "The quote symbol to use in SQL statements around identifiers.  E.g., the backtick in MySQL, or double quote in ANSI spec.";
		case allow_empty_sorts_unsafe:
			return "When enabled, allows single-sorted theorem proving methods, which may be unsound.";
		case allow_java_eqs_unsafe:
			return "When enabled, allows arbitrary equations involving java typeside symbols.  May induce undefined behavior.";
		case always_reload:
			return "When enabled, commands (which can have side effects, like loading data from CSV files) are always executed and are not cached between runs of the CQL program.";
		case chase_style:
			return "Either parallel (faster, but uses more space) or leftkan (slower, but uses less space).  Note that parallel will not be compatible with sigma operations such as sigma on transforms, the co-unit, etc.  Parallel is recommended only when data volume is too large for the other algorithms.";
		case completion_compose:
			return "Uses the ``compose'' inference rule in Knuth-Bendix completion.";
		case completion_filter_subsumed:
			return "Filters out equations that are substitution instances of other equations in Knuth-Bendix completion.";
		case completion_precedence:
			return "Defines the precedence to be used for Knuth-Bendix completion.  The list  a b c indicates that a < b < c.  Every symbol in a typeside or schema or instance must appear exactly once in this list.";
		case completion_sort:
			return "Sorts the list of critical pairs in Knuth-Bendix completion by length, processing shorter pairs first (but still fairly).";
		case completion_syntactic_ac:
			return "Enables special support for associative and commutative operators in Knuth-Bendix completion.";
		case coproduct_allow_entity_collisions_unsafe:
			return "The generators and labelled nulls of instances participating in a coproduct are required to be unique.  This option disables the uniqueness check.  It is marked unsafe because it can result in an instance that is not a coproduct, but is rather a coproduct followed by a quotient.";
		case coproduct_allow_type_collisions_unsafe:
			return "The generators and labelled nulls of instances participating in a coproduct are required to be unique.  This option disables the uniqueness check.  It is marked unsafe because it can result in an instance that is not a coproduct, but is rather a coproduct followed by a quotient.";
		case emit_ids:
			return "When true, emits an ID column.";
		case csv_escape_char:
			return "Sets the escape character for use in CSV import/export.";
		case csv_field_delim_char:
			return "Sets the field delimiter for use in CSV import/export.";
		case csv_file_extension:
			return "Sets the file extension to expect during CSV import.";
		case csv_generate_ids:
			return "If no ID column is found, then CQL will create IDs during CSV import.";
		case csv_import_prefix:
			return "Controls pre in the CSV file pre + en + ext for entity en.";
		case csv_prepend_entity:
			return "In the column mapping for CSV import, controls whether or not to prepend entity names when searching for columns in the CSV file.   After the entity the  import_col_seperator is inserted.  In other words, the attribute en + sep + att in the SQL schema will be mapped to column with header att in the CSV file.";
		case csv_quote_char:
			return "Sets the quote character for use in CSV import/export.";
		case dont_validate_unsafe:
			return "When enabled, mappings and transforms and queries are not checked to be equality-preserving.  For a query, when true it also disables decision procedure construction for sub-queries.";
		case dont_verify_is_appropriate_for_prover_unsafe:
			return "Many provers require that their input equational theories have a certain form (e.g., be unary).  When this option is enabled, this (possibly expensive) condition will not be checked.";
		case e_path:
			return "The path the the E executable.";
		case eval_approx_sql_unsafe:
			return "Queries executed against instances containing nulls cannot be implemented by translation to SQL due to differences between null semantics in CQL and SQL. When enabled, CQL will execute the generated SQL anyway, leading to an undefined, but quickly computed, answer.";
		case eval_join_selectivity:
			return "Sets the join selectivity factor used by the cost metric heuristic for the join order chooser.";
		case eval_max_plan_depth:
			return "If enabled, join reordering will be performed on FROM clauses with a number of variables smaller than this number (used to prevent the join ordering chooser from spending forever enumerating huge spaces of plans).";
		case eval_max_temp_size:
			return "Sets the maximum size of intermediate tables when executing sub-queries.";
		case eval_reorder_joins:
			return "Sets whether or not it is permissible for CQL to choose a join order based on exhaustive order enumeration under a heuristic cost metric.";
		case eval_sql_persistent_indices:
			return "Queries executed by translation to SQL can store any computed indicies for future use.";
		case eval_use_indices:
			return "Sets whether CQL should automatically index instances along their foreign keys and attributes, to reduce the number of intermediate tuples generated during query evaluation. Also applies to SQL-based execution method.";
		case eval_use_sql_above:
			return "If possible, translate CQL queries into SQL and use CQL's built-in SQL database (``H2'')) to execute the query.  The translation process incurs overhead, so this option controls how large input instances have to be before translation is used.";
		case gui_max_graph_size:
			return "The maximum size of such an object to be displayed in the viewer is controlled by the three options below.\n"
					+ ".  For type sides, (colimit) schemas, mappings, transforms, queries. \n\n" + xxx;
		case gui_max_string_size:
			return "The maximum size of such an object to be displayed in the viewer is controlled by the three options below.\n"
					+ ".  For commands and constraints." + "\n\n" + xxx;
		case gui_max_table_size:
			return "The maximum size of such an object to be displayed in the viewer is controlled by the three options below.\n"
					+ ".  For instances." + "\n\n" + xxx;
		case gui_rows_to_display:
			return "Sets the maximum number of rows to display per table per instance in the viewer.  (I.e., the table in the viewer is conceptually similar to a SQL LIMIT query for the given number of rows).\n\n"
					+ xxx;
		case gui_sample:
			return "When an object's display is suppressed, when set to to true this will display some sample data in the overflow message.\n\n"
					+ xxx;
		case gui_sample_size:
			return "When an object's display is suppressed, sets how much sample data to display the overflow message.\n\n"
					+ xxx;
		case id_column_name:
			return "Specifies the name of the ID columns for CSV and JDBC import/export.";
		case import_col_seperator:
			return "When importing a SQL schema the column c of table t becomes CQL attribute t + sep + c.  This option defines sep.  Also used during CSV import to indicate a similar situation.";
		case import_dont_check_closure_unsafe:
			return "When set to true, will skip some validation of the imported data.";
		case import_missing_is_empty:
			return "When true missing CSV files are treated as empty, and so are missing SQL queries.";
		case import_null_on_err_unsafe:
			return "When true, imported values that throw exceptions when converted into CQL values will be treated as labelled nulls.";
		case interpret_as_algebra:
			return "Interprets the set of equations in an instance as being the saturation of a model, similar to JDBC / CSV import.  Enabling this options bypasses the construction of a decision procedure, increasing performance, potentially greatly.";
		case jdbc_default_class:
			return "This string will be used in JDBC-related expressions when the JDBC driver class is the empty string.";
		case jdbc_default_string:
			return "This string will be used in JDBC-related expressions when the JDBC string is the empty string.";
		case jdbc_export_truncate_after:
			return "Truncates column names during SQL export to the given length.";
		case jdbc_no_distinct_unsafe:
			return "Used with import_jdbc_all.  Setting to false omits DISTINCTifying the set of rows given to CQL during the import process.  Certain SQL datatypes such as blobs do not support the comparison operation required by DISTINCT.";
		case jdbc_query_export_convert_type:
			return "Defines the SQL quasi-type to be used with CONVERT statements when emitting SQL for CQL queries.  A good initial guess is VARCHAR for H2 and CHAR for MySQL.";
		case js_env_name:
			return "Defines the name of the CQL environment being constructed for use with the exec_js command.";
		case maedmax_path:
			return "do not use";
		case map_nulls_arbitrarily_unsafe:
			return "In transform import, specifies whether labelled nulls can be mapped into the target by choosing arbitrarily.";
		case num_threads:
			return "Sets the number of threads to be used when running an CQL program.";
		case prepend_entity_on_ids:
			return "During import, when enable this will import IDs x as en + sep , where en is the entity for x and sep is import_col_seperator. ";
		case program_allow_nonconfluence_unsafe:
			return "Interprets all equations p = q as rewrite rules p -> q regardless of confluence.  Can diverge.";
		case program_allow_nontermination_unsafe:
			return "Interprets all equations p = q as rewrite rules p -> q regardless of termination behavior.  Can diverge.";
		case prover:
			return "Specifies which theorem prover to use.  The prover string should come from the list below.  Only the completion method has options.  Note that these theorem proving methods are not ``java aware''; to use java typesides, instances ``wrap'' these provers with java simplification.  Provers are:\n\n"
					+ allProver();
		case query_remove_redundancy:
			return "Sets whether redundant joins (i.e., containing a binding v and an equation v = e where v notin e) should be eliminated.  Note that this only has an effect when specified at the top level of a query, not within each block.";
		case quotient_use_chase:
			return "In doing instance quotients, determines whether or not to use chase-based algorithm.";
		case random_seed:
			return "Sets the random number generator seed.";
		case require_consistency:
			return "When enabled, requires CQL instances to be consistent (e.g., to not prove 1=2).  (This is checked at runtime.).  Note: is a conservative approximation to conservativity over the type side, the desired condition.";
		case simple_query_entity:
			return "Gives the name of the entity to use in a simple query.";
		case start_ids_at:
			return "Specifies the integer to use as the initial integer when exporting data.";
		case static_typing:
			return "When disabled, relaxes CQL's nominal typing discipline for colimit instances.";
		case timeout:
			return "Causes execution to halt after number seconds.";
		case toCoQuery_max_term_size:
			return "Controls the maximum size of the terms to be searched.";
		case varchar_length:
			return "Specifies the length of the VARCHAR fields to use for JDBC export.";
		case completion_unfailing:
			return "Specify if completion should not fail when encountering rules that don't orient.";
		case talg_reduction:
			return "Specifies the number of times to simplify a type algebra.  Used for the display and for export.";
		case prover_allow_fresh_constants:
			return "If false, theorem provers will fail when encountering new constants generated from java execution.";
		case second_prover:
			return "Prover for the type algebra part of the instance.";
		case gui_show_atts:
			return "If true, display attributes in the GUI schema and mapping viewers.";
		case interpet_as_frozen:
			return "Interprets an instance as having a lazy decision procedure and algebra.";
		case diverge_limit:
			return "The number of foreign keys in a schema above which not to perform the instance literal divergence warning check.";
		case diverge_warn:
			return "Stops CQL on instance literals below the divergence limit, that have cyclic schemas without equations and an instance with generators but no equations.";
		case csv_entity_name:
			return "The entity name to use for imported CSV files with inferred schemas.";
		case allow_sql_import_all_unsafe:
			return "Allows the sql_import_all primitive to be used (can result in unsoundness).";
		case graal_language:
			return "Specifies which external language to use with graalvm.";
		case import_sql_direct_prefix:
			return "Given prefix P, in import_sql_direct the table P will be selected as prefix + P; e.g the prefix could be INFORMATION_SCHEMA";
		case jena_reasoner:
			return "Specifies which Apache JENA OWL Reasoner to use.";
		case triviality_check_best_effort:
			return "Specifies whether to make a best effort to determine if particular theories are trivial (prove that all things are equal).";
		case vampire_path:
			return "The path to the Vampire executable.";
		case check_command_export_file:
			return "The path to export check command failing rows to (JSON)";
		case active_domain:
			return "When true, evaluation will loop through the active domains of type";
		case csv_row_sort_order:
			return "Comma separated list of columns to use to sort CSV output";
		case csv_utf8_bom:
			return "When true, the UTF watermark will be prepended to output CSV files";
		case e_use_auto:
			return "When true, invocations of the e prover will use -auto";
		case is_oracle:
			return "do not use";
		case jdbc_zero:
			return "Specifies which sql query to use in a select statement to obtain a zero.  Works with direct sql import.";
		case oracle_schema_mode:
			return "do not use";
		case sql_constraints_simple:
			return "do not use";
		default:
			break;

		}

		throw new RuntimeException(op.name());

	}

	public static String getOptionText(AqlOption op) {
		Object o = AqlOptions.initialOptions.getOrDefault(op);
		String s = getOptionText2(op);
		return "<pre>Default: " + o + "</pre>" + s;
	}

	public static void main(String[] args) throws java.io.IOException {
		File tmp = java.nio.file.Files.createTempDirectory("aqlhelp").toFile();
		catdata.cql.AqlTester.deleteFilesCreatedDuring(() -> {
			help(tmp, true);
			return null; // not used
		});
		File help = new File("help");
		if (help.exists()) {
			for (File f : help.listFiles()) {
				f.delete();
			}
			help.delete();
		}
		tmp.renameTo(help);
		System.exit(0); // slay daemons
	}

	@SuppressWarnings("unchecked")
	public static void help(File dir, boolean run) {
		try {
			if (!dir.exists()) {
				dir.mkdir();
			}

			String css = "\n<link rel=\"stylesheet\" href=\"http://categoricaldata.net/css/nstyle.css\"><script src=\"http://categoricaldata.net/js/simple.js\"></script>";

			String search = "<html><head>\n" + "\n" + css + "\n" + "<div>\n"
					+ "  <form action=\"search.php\" method=\"get\">\n"
					+ "       <input type=\"text\" name=\"text\" value=<?php echo \"\\\"\" . $_GET[\"text\"] . \"\\\"\" ; ?> > \n"
					+ "              <input type=\"submit\" name=\"submit\" value=\"Search\">\n" + "              \n"
					+ "              <br>\n" + "    </form>\n" + "\n" + "</div>\n" + "\n" + "\n" + "<?php\n" + "\n"
					+ "$string = $_GET[\"text\"];\n" + "\n" + "\n"
					+ "if (strpos(file_get_contents('../logo.html'), $string) !== false) {\n"
					+ "        echo \"<a href=\\\"../logo.html\\\">CQL Manual</a><br/>\";\n" + "}\n" + "    \n"
					+ "$dir = new DirectoryIterator('.');\n" + "foreach ($dir as $file) {\n"
					+ "    if ($file == 'search.php') {\n" + "        continue;   \n" + "    }\n"
					+ "    if ($file == 'logo.html') {\n" + "        continue;   \n" + "    }\n"
					+ "    if ($file == 'options.html') {\n" + "        continue;   \n" + "    }\n"
					+ "    if ($file == 'examples.html') {\n" + "        continue;   \n" + "    }\n"
					+ "    if ($file == 'syntax.html') {\n" + "        continue;   \n" + "    }\n"
					+ "    $content = file_get_contents($file->getPathname());\n" + "    \n"
					+ "    if (strpos($content, $string) !== false) {\n"
					+ "        echo \"<a href=\\\"\" . $file . \"\\\">\" . $file . \"</a><br/>\";\n" + "    }\n" + "}\n"
					+ "\n" + "?>\n" + "\n" + "\n" + "\n" + "</body></html>";
			Util.writeFile(search, new File(dir, "search.php").getAbsolutePath());
			Map<Example, Set<AqlSyntax>> index = new THashMap<>();

			StringBuffer examples = new StringBuffer("<html><head>" + css + "</head><body>");
			for (Example ex : Util.alphabetical(Examples.getExamples(Language.CQL))) {
				if (ex.getName().equals("Stdlib") || ex.getName().equals("TutorialTSP")) {
					continue;
				}
				examples.append("\n<a href=\"" + ex.getName().trim() + ".html\" target=\"primary\">"
						+ ex.getName().trim() + "</a><br />");

				System.out.println(ex.getName());
				Program<Exp<?>> prog = new CombinatorParser().parseProgram(ex.getText());

				StringBuffer other = new StringBuffer();
				Set<AqlSyntax> set = new THashSet<>();
				Set<String> opSeen = new THashSet<>();
				index.put(ex, set);
				other.append("Keywords:<br/></br>");
				for (Exp<?> e : prog.exps.values()) {
					if (!set.contains(e.getSyntax())) {
						set.add(e.getSyntax());
						other.append("\t\t\t<a href=\"" + e.kind() + e.getKeyword() + ".html" + "\" >" + e.getKeyword()
								+ "</a><br />\n");
					}
					for (String k : e.options().keySet()) {
						if (opSeen.contains(k)) {
							continue;
						}
						opSeen.add(k);
					}
				}
				other.append("<br/>Options:<br/></br>");
				for (String x : opSeen) {
					other.append("<a href=\"" + x + ".html\" >" + x + "</a><br/>\n");
				}

				StringBuffer insts = new StringBuffer();
				if (run) {
					insts.append("\n");
					try {
						AqlMultiDriver dr = new AqlMultiDriver(prog, null);
						dr.start();
						for (String k : dr.env.defs.insts.keySet()) {
							Instance<?, ?, ?, ?, ?, ?, ?, ?, ?> i = dr.env.defs.insts.get(k);
							insts.append("<hr/>");
							insts.append("<h3>instance " + k + "</h3>");
							insts.append(AqlInACan.toHtml(dr.env,
									(Instance<String, String, catdata.cql.exp.Sym, catdata.cql.exp.Fk, catdata.cql.exp.Att, String, String, Object, Object>) i));
						}
						for (String k : dr.env.defs.ps.keySet()) {
							Pragma i = dr.env.defs.ps.get(k);
							insts.append("<hr/>");
							insts.append("<h3>command " + k + "</h3><pre>");
							insts.append(i.toString());
							insts.append("</pre>");
						}
					} catch (Exception exe) {
						exe.printStackTrace();
					}
				}

				String sss = "<html><head>" + css + "</head><body><h1>" + "example " + ex.getName() + "</h1><pre>\n"
						+ AqlInACan.strip(ex.getText().trim()) + "\n</pre> " + other.toString() + "\n<br/></br>"
						+ insts.toString() + "</body></html>";

				Util.writeFile(sss, new File(dir, ex.getName() + ".html").getAbsolutePath());
			}
			examples.append("\n</body></html>");

			StringBuffer logo = new StringBuffer("");
			logo.append("<html><head>" + css + "</head><body>");
			logo.append("\n<a href=\"https://categoricaldata.github.io/CQL/\" target=\"primary\">Help</a><br />");
			logo.append("\n<br />");
			logo.append("\n<a href=\"syntax.html\" target=\"tree\">Syntax</a><br />");
			logo.append("\n<a href=\"options.html\" target=\"tree\">Options</a><br />");
			logo.append("\n<a href=\"examples.html\" target=\"tree\">Examples</a><br />");
			logo.append("\n<a href=\"search.php\" target=\"primary\">Search</a><br />");

			logo.append("\n<br />");
			// logo.append("\n<a href=\"http://categorical.info\" target=\"_blank\">CI
			// Website</a><br />");
			logo.append("\n<a href=\"http://categoricaldata.net\" target=\"_blank\">CQL Website</a><br />");
			logo.append(
					"\n<a href=\"http://github.com/CategoricalData/CQL/wiki\" target=\"_blank\">CQL Wiki</a><br />");
			logo.append("\n</body></html>");

			StringBuffer main = new StringBuffer();
			main.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\"");
			main.append("\n   \"http://www.w3.org/TR/html4/frameset.dtd\">");
			main.append("\n<HTML>");
			main.append("\n<HEAD>");
			main.append("\n<TITLE>CQL</TITLE>");
			main.append(css);
			main.append("\n</HEAD>");
			main.append("\n<FRAMESET cols=\"20%, 80%\">");
			main.append("\n  <FRAMESET rows=\"100, 200\">");
			main.append("\n      <FRAME src=\"logo.html\" name=\"logo\">");
			main.append("\n      <FRAME src=\"syntax.html\" name=\"tree\">");
			main.append("\n  </FRAMESET>");
			main.append("\n  <FRAME src=\"https://categoricaldata.github.io/CQL\" name=\"primary\">");
			main.append("\n</FRAMESET>");
			main.append("\n</HTML>");

			StringBuffer syntax = new StringBuffer();
			syntax.append("<html><head>" + css + "</head>\n\t");
			Map<Kind, Set<Pair<AqlTyping, Exp<?>>>> z = new TypingInversion().covisit(Unit.unit, x -> new AqlTyping());
			Map<String, Set<Exp<?>>> opInv = Util.newSetsFor(AqlOptions.optionNames());

			boolean isFirstK = true;
			for (Kind k : Util.alphabetical(Arrays.asList(Kind.class.getEnumConstants()))) {

				if (k.equals(Kind.COMMENT)) {
					continue;
				}
				if (!isFirstK) {
					syntax.append("<br/>");
					isFirstK = false;
				}
				isFirstK = false;
				syntax.append("<h3>" + k + "</h3>");
				List<Pair<AqlTyping, Exp<?>>> ee = new ArrayList<>(z.get(k));
				Collections.sort(ee, (x, y) -> x.second.getKeyword().compareTo(y.second.getKeyword()));

				for (Pair<AqlTyping, Exp<?>> pair : ee) {
					if (pair.second.isVar()) {
						continue;
					}

					AqlTyping g = pair.first;
					Exp<?> e = pair.second;

					String docFile = "docs/" + pair.second.getSyntax() + ".md";
					String desc;
					try {
						desc = Util.readFile(new FileReader(new File(docFile)));
					} catch (java.io.FileNotFoundException fnfe) {
						System.out.println("Missing doc: " + docFile);
						desc = "TODO";
					}
					// Util.writeFile(desc, "docs/" + pair.second.getSyntax() + ".md");

					// String desc = e.accept0(Unit.unit, new AqlHelp());

					String str = e.printNicely(g).replace("<", "&lt;").replace(">", "&gt;");
					String str2 = "<br/>Appears in:<br/><br/>";

					Set<AqlOption> xx = e.allowedOptions();
					for (AqlOption op : xx) {
						opInv.get(op.name()).add(e);
					}
					List<String> yy = xx.stream()
							.map(x -> "<a href=\"" + x.name() + ".html\" target=\"primary\">" + x.name() + "</a>\n")
							.collect(Collectors.toList());
					String str3 = "Options:<br/><br/>" + Util.sep(Util.alphabetical(yy), "<br/>\n");

					for (Example ex : Util.alphabetical(Examples.getExamples(Language.CQL))) {
						if (!index.containsKey(ex)) {
							continue;
						}
						if (index.get(ex).contains(e.getSyntax())) {
							str2 += "\n<a href=\"" + ex.getName().trim() + ".html\" target=\"primary\">"
									+ ex.getName().trim() + "</a><br />";
						}
					}

					String dstFile = new File(dir, e.kind() + e.getKeyword() + ".html").getAbsolutePath();
					Util.writeFile(
							"<html><head>" + css + "</head><h1>" + e.kind() + " " + e.getKeyword() + "</h1>\n<pre>\n"
									+ str + "\n\t</pre>\n" + desc + "<br/>" + str2 + "<br/>" + str3 + "</html>",
							dstFile);
					syntax.append("<a href=\"" + e.kind() + e.getKeyword() + ".html" + "\" target=\"primary\">"
							+ e.getKeyword() + "</a><br />\n");

				}
//        syntax.append("\t<br");
			}
			syntax.append("</html>\n");

			StringBuffer options = new StringBuffer("<html><head>" + css + "</head><body>");
			for (AqlOption ex : Util.alphabetical(Arrays.asList(AqlOption.class.getEnumConstants()))) {

				options.append("\n<a href=\"" + ex.name() + ".html\" target=\"primary\">" + ex.name() + "</a><br />");
				StringBuffer zzz = new StringBuffer();
				Set<AqlSyntax> seen = new THashSet<>();
				for (Exp<?> e : opInv.get(ex.toString())) {
					AqlSyntax f = e.getSyntax();
					if (seen.contains(f)) {
						continue;
					}
					seen.add(f);
					zzz.append("\t\t\t<a href=\"" + e.kind() + e.getKeyword() + ".html" + "\" target=\"primary\">"
							+ e.getKeyword() + "</a><br />\n");
				}
				StringBuffer yyy = new StringBuffer();
				for (Example example : Util.alphabetical(Examples.getExamples(Language.CQL))) {

					if (example.getText().contains(ex.toString())) {
						yyy.append("\n<a href=\"" + example.getName().trim() + ".html\" target=\"primary\">"
								+ example.getName() + "</a><br />");
					}
				}

				String sss = "<html><head>" + css + "</head><body><h1>" + "option " + ex.name() + "</h1>\n"
						+ AqlHelp.getOptionText(ex) + "<br/><br/>Keyword:</br><br/>" + zzz
						+ "<br/>Appears in:</br><br/>" + yyy + "</body></html>";
				Util.writeFile(sss, new File(dir, ex.name() + ".html").getAbsolutePath());
			}
			options.append("\n</body></html>");

			Util.writeFile(options.toString(), new File(dir, "options.html").getAbsolutePath());
			Util.writeFile(examples.toString(), new File(dir, "examples.html").getAbsolutePath());
			Util.writeFile(main.toString(), new File(dir, "index.html").getAbsolutePath());
			Util.writeFile(syntax.toString(), new File(dir, "syntax.html").getAbsolutePath());
			Util.writeFile(logo.toString(), new File(dir, "logo.html").getAbsolutePath());

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}

	}

	public String report() {

		Map<Kind, Set<Pair<AqlTyping, Exp<?>>>> exps = new TypingInversion().covisit(Unit.unit, x -> new AqlTyping());

		StringBuffer sb = new StringBuffer();

		for (Kind k : Kind.class.getEnumConstants()) {
			for (Pair<AqlTyping, Exp<?>> pair : exps.get(k)) {
				AqlTyping g = pair.first;
				Exp<?> e = pair.second;
				if (e == null) {
					continue;
				}
				sb.append(e.printNicely(g));
				sb.append("\n\n");
			}
		}
		return sb.toString();
	}

}
