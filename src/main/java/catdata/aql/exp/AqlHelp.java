package catdata.aql.exp;

import java.io.File;
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
import catdata.apg.exp.ApgInstExp.ApgInstExpCoEqualize;
import catdata.apg.exp.ApgInstExp.ApgInstExpDelta;
import catdata.apg.exp.ApgInstExp.ApgInstExpEqualize;
import catdata.apg.exp.ApgInstExp.ApgInstExpInitial;
import catdata.apg.exp.ApgInstExp.ApgInstExpPlus;
import catdata.apg.exp.ApgInstExp.ApgInstExpRaw;
import catdata.apg.exp.ApgInstExp.ApgInstExpTerminal;
import catdata.apg.exp.ApgInstExp.ApgInstExpTimes;
import catdata.apg.exp.ApgInstExp.ApgInstExpVar;
import catdata.apg.exp.ApgMapExp.ApgMapExpCompose;
import catdata.apg.exp.ApgMapExp.ApgMapExpRaw;
import catdata.apg.exp.ApgMapExp.ApgMapExpVar;
import catdata.apg.exp.ApgSchExp.ApgSchExpInitial;
import catdata.apg.exp.ApgSchExp.ApgSchExpPlus;
import catdata.apg.exp.ApgSchExp.ApgSchExpRaw;
import catdata.apg.exp.ApgSchExp.ApgSchExpTerminal;
import catdata.apg.exp.ApgSchExp.ApgSchExpTimes;
import catdata.apg.exp.ApgSchExp.ApgSchExpVar;
import catdata.apg.exp.ApgTransExp.ApgTransExpCase;
import catdata.apg.exp.ApgTransExp.ApgTransExpCoEqualize;
import catdata.apg.exp.ApgTransExp.ApgTransExpCoEqualizeU;
import catdata.apg.exp.ApgTransExp.ApgTransExpCompose;
import catdata.apg.exp.ApgTransExp.ApgTransExpDelta;
import catdata.apg.exp.ApgTransExp.ApgTransExpEqualize;
import catdata.apg.exp.ApgTransExp.ApgTransExpEqualizeU;
import catdata.apg.exp.ApgTransExp.ApgTransExpFst;
import catdata.apg.exp.ApgTransExp.ApgTransExpId;
import catdata.apg.exp.ApgTransExp.ApgTransExpInitial;
import catdata.apg.exp.ApgTransExp.ApgTransExpInl;
import catdata.apg.exp.ApgTransExp.ApgTransExpInr;
import catdata.apg.exp.ApgTransExp.ApgTransExpPair;
import catdata.apg.exp.ApgTransExp.ApgTransExpRaw;
import catdata.apg.exp.ApgTransExp.ApgTransExpSnd;
import catdata.apg.exp.ApgTransExp.ApgTransExpTerminal;
import catdata.apg.exp.ApgTransExp.ApgTransExpVar;
import catdata.apg.exp.ApgTyExp.ApgTyExpRaw;
import catdata.apg.exp.ApgTyExp.ApgTyExpVar;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.AqlProver.ProverName;
import catdata.aql.AqlSyntax;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.Pragma;
import catdata.aql.exp.ColimSchExp.ColimSchExpQuotient;
import catdata.aql.exp.ColimSchExp.ColimSchExpRaw;
import catdata.aql.exp.ColimSchExp.ColimSchExpVar;
import catdata.aql.exp.ColimSchExp.ColimSchExpWrap;
import catdata.aql.exp.EdsExp.EdsExpSch;
import catdata.aql.exp.EdsExp.EdsExpVar;
import catdata.aql.exp.Exp.ExpVisitor;
import catdata.aql.exp.GraphExp.GraphExpLiteral;
import catdata.aql.exp.GraphExp.GraphExpRaw;
import catdata.aql.exp.GraphExp.GraphExpVar;
import catdata.aql.exp.InstExp.InstExpLit;
import catdata.aql.exp.InstExp.InstExpVar;
import catdata.aql.exp.MapExp.MapExpLit;
import catdata.aql.exp.MapExp.MapExpVar;
import catdata.aql.exp.MorExp.MorExpVar;
import catdata.aql.exp.PragmaExp.PragmaExpCheck;
import catdata.aql.exp.PragmaExp.PragmaExpConsistent;
import catdata.aql.exp.PragmaExp.PragmaExpJs;
import catdata.aql.exp.PragmaExp.PragmaExpMatch;
import catdata.aql.exp.PragmaExp.PragmaExpProc;
import catdata.aql.exp.PragmaExp.PragmaExpSql;
import catdata.aql.exp.PragmaExp.PragmaExpToCsvInst;
import catdata.aql.exp.PragmaExp.PragmaExpToCsvTrans;
import catdata.aql.exp.PragmaExp.PragmaExpToJdbcInst;
import catdata.aql.exp.PragmaExp.PragmaExpToJdbcQuery;
import catdata.aql.exp.PragmaExp.PragmaExpToJdbcTrans;
import catdata.aql.exp.PragmaExp.PragmaExpVar;
import catdata.aql.exp.QueryExp.QueryExpId;
import catdata.aql.exp.QueryExp.QueryExpLit;
import catdata.aql.exp.QueryExp.QueryExpVar;
import catdata.aql.exp.SchExp.SchExpCod;
import catdata.aql.exp.SchExp.SchExpDom;
import catdata.aql.exp.SchExp.SchExpDst;
import catdata.aql.exp.SchExp.SchExpEmpty;
import catdata.aql.exp.SchExp.SchExpInst;
import catdata.aql.exp.SchExp.SchExpLit;
import catdata.aql.exp.SchExp.SchExpPivot;
import catdata.aql.exp.SchExp.SchExpSrc;
import catdata.aql.exp.SchExp.SchExpVar;
import catdata.aql.exp.TransExp.TransExpId;
import catdata.aql.exp.TransExp.TransExpLit;
import catdata.aql.exp.TransExp.TransExpVar;
import catdata.aql.exp.TyExp.TyExpEmpty;
import catdata.aql.exp.TyExp.TyExpLit;
import catdata.aql.exp.TyExp.TyExpSch;
import catdata.aql.exp.TyExp.TyExpVar;
import catdata.ide.Example;
import catdata.ide.Examples;
import catdata.ide.Language;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class AqlHelp implements
		ExpVisitor<Unit, String, String, String, String, String, String, String, String, String, String, String, String, String, RuntimeException, String,String,String,String,String> {

	static String xxx = "Associated with each object is a size, according to its kind:"
			+ "\nitem command, constraints: length of the text string" + "\n instance: rows in the algebra"
			+ "\n colimit schema: size of the schema" + "\n graph: nodes + edges"
			+ "\n typeside: types + functions + equations" + "\n schema: entities + attributes + fks + equations"
			+ "\n mapping, transform, query: size of source";

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
			return "Applies only to ground (variable-free) theories.  Uses the classical Nelson-Oppen congruence-closure with union-find algorithm.";
		case e:
			return "Uses the E prover.  Must be installed.";
		case fail:
			return "Applies to all theories.  Always fails with an exception.";
		case free:
			return "Applies only to theories without equations.  Provable equivalence is defined as the syntactic equality of two terms.";
		case maedmax:
			return "Uses the maedmax prover.  Must be installed.";
		case monoidal:
			return "Applies to theories where all equations are monadic (unary) or ground.  Applies Knuth-Bendix completion specialized to semi-Thue systems.";
		case program:
			return "Applies only to weakly orthogonal theories.  Interprets all equations p = q as rewrite rules p -> q if p is syntactically smaller than q and q -> p if q is syntactically smaller than p and symbolically evaluates terms to normal forms.  Fails if an equation cannot be oriented.";

		}
		return Util.anomaly();
	}

	static String getOptionText2(AqlOption op) {

		switch (op) {
		case allow_aggregation_unsafe:
			return "Enables aggregation, which is not functorial.  Associativity, commutativity, and unitality of the given aggegrates are not checked for those properties by the prover, at least for the time being.";
		case left_bias:
			return "Use the left-biased schema colimit renaming algorithm.";
//		case lax_literals:
		// return "Allows literals to depend on non-literals.";
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
		case csv_emit_ids:
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
		case import_as_theory:
			return "When enabled, imported data will be interpreted as an equational theory, rather than an entire model.  This allows the importation of partial data sets, at the expense of additional theorem proving.";
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
			return "The path to the maedmax executable";
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
		default:
			break;

		}

		return Util.anomaly();

	}

	@Override
	public String visit(Unit params, SchExpCod exp) {
		return "Returns the range / codomain of a query.";
	}

	@Override
	public String visit(Unit params, SchExpDom exp) {
		return "Returns the domain of a query.";
	}

	@Override
	public String visit(Unit params, SchExpDst exp) {
		return "Returns the range / codomain of a mapping.";
	}

	@Override
	public String visit(Unit params, SchExpSrc exp) {
		return "Returns the domain of a mapping.";
	}

	@Override
	public String visit(Unit params, SchExpEmpty exp) {
		return "Returns the empty schema on a type side.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, SchExpInst<Gen, Sk, X, Y> exp) {
		return "Gets the schema of an instance.";
	}

	@Override
	public String visit(Unit params, SchExpLit exp) {
		return null;
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, SchExpPivot<Gen, Sk, X, Y> exp) {
		return "Converts an instance into a schema.  The schema will be a finite directed graph isomorphic to the term / initial model of the input instance.  Operationally, it is like converting to 'triple' or 'key value' form.";
	}

	@Override
	public String visit(Unit params, SchExpVar exp) {
		return "A schema variable.";
	}

	@Override
	public String visit(Unit params, SchExpRaw exp) {
		return "A literal schema, or an extension of a type side by attributes (entity to type, unary) and foreign keys (entity to entity, unary) and equations in a single variable with entity sort. See All_Syntax for an example.";
	}

	@Override
	public String visit(Unit params, SchExpColim exp) {
		return "The schema part of a colimit of schemas.";
	}

	@Override
	public String visit(Unit params, TyExpSch exp) {
		return "The typeside of a schema.";
	}

	@Override
	public String visit(Unit params, TyExpEmpty exp) {
		return "The empty typeside with no types.";
	}

	@Override
	public String visit(Unit params, TyExpLit exp) {
		return null;
	}

	@Override
	public String visit(Unit params, TyExpVar exp) {
		return "A typeside variable.";
	}

	@Override
	public String visit(Unit params, TyExpRaw exp) {
		return "A typeside literal: a multi-sorted equational theory.  See All_Syntax for an example.  Note that class names in javascript should be fully qualified.";
	}

	@Override
	public String visit(Unit params, TyExpSql exp) {
		return "The SQL typeside.  Contains only java types and no function symbols.";
	}

	@Override
	public String visit(Unit param, InstExpJdbcAll exp) {
		return "Imports a SQL database onto an autogenerated CQL schema.  The CQL schema will have one attribute per column in the input DB, and one foreign key per foreign key in the input DB, and equations capturing the input DB's foreign key constraints.  The type side will have a single type, ``dom''.  When the [jdbcclass] and [jdbcuri] are the empty string, their values will be determined by the  jdbc_default_class and  jdbc_default_string options.  See also option  import_col_seperator and schema_only (imports the empty instance).  See also  prepend_entity_on_ids and jdbc_no_distinct_unsafe.";

	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpSigma<Gen, Sk, X, Y> exp) {
		return "Performs a left kan extension.  Operationally, each generator g of entity e becomes translated to the target schema as a generator of entity f(e), where f is the mapping.  Hence sigma can be thought of as substitution along a theory morphism.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpSigmaChase<Gen, Sk, X, Y> exp) {
		return "Performs a sigma, but using an alternative algorithm that is faster but only on free type sides.";
	}

	@Override
	public String visit(Unit param, InstExpVar exp) {
		return "An instance variable.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpAnonymize<Gen, Sk, X, Y> exp) {
		return "Anonymize an instance by replacing all attributes with fresh strings.  Note: this operation can fail at runtime if there are observation equations or other uses of typeside function symbols.\n"
				+ "";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpChase<Gen, Sk, X, Y> exp) {
		return "Repairs a database that may not conform to a set of constraints.  It can also be used for data integration in the traditional, relational, regular logic / ED style.  If the chase succeeds, the result instance will satisfy the constraints.  The options will be used for the construction of the intermediate instances during the chase process.";
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(Unit param,
			InstExpCod<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) {
		return "The codomain (range) of a transform / database homomorphism.";
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(Unit param,
			InstExpCoEq<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) {
		return "Co-equalizes two parallel transforms / database homomorphisms.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpCoEval<Gen, Sk, X, Y> exp) {
		return "Co-evaluates a query.  Note: co-evaluation is not inversion, but a kind of one-sided inverse called an adjunction.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpColim<Gen, Sk, X, Y> exp) {
		return "Computes a colimit of instances and transforms.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpCoProdFull<Gen, Sk, X, Y> exp) {
		return "Does a co-product of instances, with no requirement that input generators be disjoint.";
	}

	@Override
	public <Gen, Sk, X, Y, Gen1, Sk1, X1> String visit(Unit param, InstExpDiff<Gen, Sk, X, Y, Gen1, Sk1, X1> exp) {
		return "Perform a difference operation on two instances; this operation is adjoint to co-product.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpDistinct<Gen, Sk, X, Y> exp) {
		return "Relationalizes an instance; that is, equates all rows that are equivalent up to observation of attributes.  Converts from bags to sets.";
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(Unit param,
			InstExpDom<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) {
		return "The domain of a transform / database homormorphism.";
	}

	@Override
	public String visit(Unit param, InstExpEmpty exp) {
		return "The instance with no generators or nulls.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpEval<Gen, Sk, X, Y> exp) {
		return "Evaluates a query.  Generalizes relational conjunctive queries to the case of multiple tables connected by constraints.";
	}

	@Override
	public String visit(Unit param, InstExpFrozen exp) {
		return "If  Q : S to T  is a query and t  is a  T -entity, then  frozen  Q  t  is the ``frozen'' or ``canonical''  S -instance for  t  in  Q .";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpLit<Gen, Sk, X, Y> exp) {
		return null;
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpPivot<Gen, Sk, X, Y> exp) {
		return "Computes the instance pivot i instance j such that sigma f j = i.  Has one row per entity, i.e., is initial on the entity side.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpPi<Gen, Sk, X, Y> exp) {
		return "Performs a pi or join-all operation along a mapping.  It is recommended to use eval of queries instead, because pi has non-intuitive semantics and is computationally intractable.";
	}

	@Override
	public String visit(Unit param, InstExpCsv exp) {
		return "For each entity en, expects a file pre + en.csv in the directory, where pre is controlled by option csv_import_file_prefix.  The file extension can be changed with the csv_file_extension option  The file for en should be a CSV file with a header; the fields of the header should be an ID column name (specified using options), as well as any attributes and foreign keys whose domain is en.  The mapping between the schemas entities (ID columns), attributes, and foreign keys and the headers of the CSV file are controlled by the options; see the CSV built-in example for details.  The import_missing_is_empty option will interpret missing files as empty files. See also csv_prepend_entity.  \n"
				+ "Records can contain nulls (using the string specified in options).  Note that these tables must be complete: this keyword imports a model (set of tables) of a schema, not a presentation of a model of a schema (i.e., as  literal does).  To import an instance as a theory, use the  import_as_theory option.  To auto generate IDs, see  csv_generate_ids option. See also csv related options,  id_column_name, and  require_consistency. \n"
				+ " See also  prepend_entity_on_ids.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpDelta<Gen, Sk, X, Y> exp) {
		return "Performs a projection along a mapping.";
	}

	@Override
	public String visit(Unit param, InstExpJdbc exp) {
		return "An instance obtained from a JDBC connection, one SQL query per entity.  The SQL code for each entity should return a table with an ID column and one column per attribute/foreign key from that table.  Note that these tables must be complete: this keyword imports a model (set of tables) of a schema, not a presentation of a model of a schema (i.e., as  literal does).  To import an instance as a theory, use the  import_as_theory option.  When the [jdbcclass] and [jdbcuri] are the empty string, their values will be determined by the  jdbc_default_class and  jdbc_default_string options.  See also import_missing_is_empty.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpQueryQuotient<Gen, Sk, X, Y> exp) {
		return "Expects a list of queries, on per entity, e.g.,\n" + "<pre>\n"
				+ "entity E -> from x:E y:E where x.a = y.b\n" + "entity F -> from x:F y:F\n" + "</pre>\n"
				+ "each query should have no attributes and no foreign keys.  The resulting matched pairs  (x,y)  are treated\n"
				+ "as equations for performing the quotient.  \n"
				+ "By default uses a chase-based algorithm; to disable, set quotient_use_chase = false";
	}

	@Override
	public String visit(Unit param, InstExpRandom exp) {
		return "Not recommended; returns a random instance that may not satify the given path equations.  Constructs a random instance with the specified number of generators per entity or type. Then for each generator  e:s  and fk/att  f : s -> t , it adds equation  f(e) = y , where  y:t  is a randomly selected generator (or no equation if t has no generators).\n"
				+ "See option random_seed.";
	}

	@Override
	public String visit(Unit param, InstExpRaw exp) {
		return "A literal instance, given by generating rows and labelled nulls, and ground equations.  As always, quotes can be used; for example, to write negative numbers.  Convient additional syntax:<pre>\n"
				+ "multi_equations\n" + "	name -> person1 bill, person2 alice\n" + "</pre>\n" + "is equivalent to\n"
				+ "<pre>\n" + "equations\n" + "	person1.name = bill\n" + "	person2.name = alice\n" + "</pre>\n"
				+ "The key-value pairs in multi-equations must be comma separated (necessary for readability and error correction).\n"
				+ "\n"
				+ "See require_consistency, and interpret_as_algebra, which interprets the instance as a model, similar to JDBC / CSV import.  This behavior can be useful when writing down an instance that is already saturated and for which one wants to check the schema constraints, rather than impose them. See All_Syntax for an example.";
	}

	@Override
	public String visit(Unit params, MapExpComp exp) {
		return "Composes two mappings left to right.";
	}

	@Override
	public String visit(Unit params, MapExpId exp) {
		return "The identity mapping.  In fact, it can be written as include instead of identity and given a second argument.";
	}

	@Override
	public String visit(Unit params, MapExpLit exp) {
		return null;
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, MapExpPivot<Gen, Sk, X, Y> exp) {
		return "Computes the mapping from pivot(I) to I.";
	}

	@Override
	public String visit(Unit params, MapExpVar exp) {
		return "A mapping variable.";
	}

	@Override
	public String visit(Unit params, MapExpRaw exp) {
		return "A mapping literal, or derived theory morphism, constant on type sides.  Each source entity maps to a target entity, each foreign key to a path, and each attribute to a lambda term with one variable.  See All_Syntax for an example.";
	}
	
	@Override
	public String visit(Unit params, MorExpRaw exp) {
		return "A theory morphism literal, or derived theory morphism.  Each source type maps to a target type, each function to a lambda term.  See Theory_Mor for an example.";
	}

	@Override
	public String visit(Unit params, MapExpColim exp) {
		return "Get the mapping to an input schema in a schema colimit.";
	}

	@Override
	public String visit(Unit params, QueryExpCompose exp) {
		return "Compose two queries, left to right.";
	}

	@Override
	public String visit(Unit params, QueryExpId exp) {
		return "The identity query.  One can also writte include a b when a appears in b.";
	}

	@Override
	public String visit(Unit params, QueryExpLit exp) {
		return null;
	}

	@Override
	public String visit(Unit params, QueryExpVar exp) {
		return "A query variable.";
	}

	@Override
	public String visit(Unit params, QueryExpRaw exp) {
		return "A query literal, known informally as an uber-flower.  Equivalent to a co-span of mapping, and generalizes relational conjunctive queries to multiple target tables with foreign keys. If Q is from S to T, then for each entity t in T we have a from where query or so-called frozen S instance Q(t).  Furthermore, for each foreign key fk taking e1 to e2 Q(fk) is a tranform from Q(e2) to Q(e1), i.e., contravariantly.  Similarly, each attribute att from e to t has a corresponding transform Q(att) from the representable instance y_t to Q(e).  See All_Syntax for an example.";
	}

	@Override
	public String visit(Unit params, QueryExpDeltaCoEval exp) {
		return "Returns the query that, when co-evaluated, has the same effect as a delta.  And when evaluated, as sigma.";
	}

	@Override
	public String visit(Unit params, QueryExpDeltaEval exp) {
		return "Returns the query that, when evaluated, has the same effect as a delta.  And we co-evaluated, as sigma.";
	}

	@Override
	public String visit(Unit params, QueryExpRawSimple exp) {
		return "A simplified query syntax for targeting a single output table and inferring the return table columns.  Use * after the word attributes to return all possible attributes.";
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(Unit params,
			TransExpCoEval<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) {
		return "Computes the transform from co-eval(Q,I) to co-eval(Q,J), given a transform from I to J.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, TransExpCoEvalEvalCoUnit<Gen, Sk, X, Y> exp) {
		return "Computes the round trip transform from co-eval(Q,eval(Q,I)) to I.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, TransExpCoEvalEvalUnit<Gen, Sk, X, Y> exp) {
		return "Computes the round trip transform from I to eval(Q,co-eval(Q,I)).";
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(Unit params,
			TransExpDelta<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) {
		return "Computes the transform delta(F,I) to delta(F,J) from a transform I to J";
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(Unit params,
			TransExpDistinct<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) {
		return "Computes the transform distinct i1 to distinct i2 from a transform from i1 to i2.";
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(Unit params,
			TransExpEval<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) {
		return "Computes the transform eval q i to eval q j from the transform h from i to j";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, TransExpId<Gen, Sk, X, Y> exp) {
		return "The identity transform on an instance.  One can also write include i1 i2 when i1 appears in i2.";
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(Unit params,
			TransExpLit<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) {
		return null;
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(Unit params,
			TransExpSigma<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> exp) {
		return "Computes the transform sigma f i to sigma f j from a transform from i to j";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, TransExpSigmaDeltaCounit<Gen, Sk, X, Y> exp) {
		return "Computes the round trip transform from sigma m delta m i to j";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, TransExpSigmaDeltaUnit<Gen, Sk, X, Y> exp) {
		return "Computes the injection-like transform from i to delta m sigma m i";
	}

	@Override
	public String visit(Unit params, TransExpRaw exp) {
		return "A literal transform, given as a morphism of instances constant on the schema.  Hence, a target term for each source generator or labelled null.  See All_Syntax for an example.";
	}

	@Override
	public String visit(Unit params, TransExpVar exp) {
		return "A transform / database homomorphism variable.";
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2, Gen3, Sk3, X3, Y3> String visit(Unit params,
			TransExpCompose<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2, Gen3, Sk3, X3, Y3> exp) {
		return "Compose two morphism from left to right.";
	}

	@Override
	public <X1, Y1, X2, Y2> String visit(Unit params, TransExpCsv<X1, Y1, X2, Y2> exp) {
		return "Imports a transform from CSV data, one file per entity.  The first column is interpreted as the source and the second column as the target.";
	}

	@Override
	public <X1, Y1, X2, Y2> String visit(Unit params, TransExpJdbc<X1, Y1, X2, Y2> exp) {
		return "Imports a transform from a JDBC database, one two-column SQL query per entity. "
				+ "The first column is interpreted as the source and the second column as the target."
				+ "When the [class] and [jdbc string] are the empty string, their values will be determined by the  jdbc_default_class and  jdbc_default_string options.  The  map_nulls_arbitrarily_unsafe option can be used to automatically map labelled nulls during import.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, TransExpDistinct2<Gen, Sk, X, Y> exp) {
		return "Computes the transform distinct(I) to I, taking each row to its equivalence class representative";
	}

	@Override
	public String visit(Unit params, ColimSchExpQuotient exp) {
		return "quotient <schema> + ... + <schema> : <typeside> "
				+ "constructs a colimit of schemas as a quotient of a co-product.  Each schema must be a variable, whose name will serve as a prefix for attributes and foreign keys in the resulting colimit schema.  For this reason, the variable names must be distinct.  The entities of the resulting colimit schema are canonicalized strings describing equivalence classes of source entities.\n"
				+ "Each entity equation has the form  v_1.e_1 = v_2.e_2 , where  v_1, v_2  are schema names and  e_1, e_2  are entities in  v_1, v_2 , respectively.\n"
				+ "Each path equation has the form  <path>=<path>, where the foreign keys in each path have the form  v_fk , where  v  is a schema name and  fk  is a foreign key in  v .  Note that the underscore used here is different than the period used in the preceding section; this discrepancy is required for CQL to have an unambiguous grammar.\n"
				+ "Each observation equation has the form  forall x. <term> = <term>, where the symbols in each term have the form  v_f(\\ldots) , where  v  is a schema name and  f  is a symbol in  v .   Alternatively, equations of the form  \\forall x. \\ p(x) = q(x) , where  p,q  are paths, can simply be written as  p = q .  When there are no equations this keyword is printed as coproduct rather than quotient.";
	}

	@Override
	public String visit(Unit params, ColimSchExpRaw exp) {
		return "Computes a colimit of schemas and mappings given by a graph.  The option static_typing when disabled causes CQL to type check the colimit at runtime rather than compile time.  This will reduce the number of transforms required to compute any particular colimit, at the cost of potential runtime failure.  See All_Syntax for an example.";
	}

	@Override
	public String visit(Unit params, ColimSchExpVar exp) {
		return "A colimit of schemas variable.";
	}

	@Override
	public String visit(Unit params, ColimSchExpWrap exp) {
		return "Wraps a colimit of schemas as another schema by providing two mappings witnessing the isomorphism.";
	}

	@Override
	public String visit(Unit params, ColimSchExpModify exp) {
		return "Modifies a schema colimit by a sequence of entity and column renamings and removals.  A mapping name to name to  specifying a renaming of entities, foreign keys, and attributes.  Then, mapping name to path specifying that the foreign key named by name can be deleted by replacing all occurrences of it by the given path.  CQL checks if the removal is semantics-preserving. \n"
				+ "\n"
				+ "Finally a mapping name to lambda x. term specifying that the attribute name can be deleted by replacing all occurrences of it by the given term.  CQL checks if the removal is semantics-preserving. \n"
				+ "";
	}

	@Override
	public String visit(Unit params, CommentExp exp) {
		return null;
	}

	@Override
	public String visit(Unit params, EdsExpVar exp) {
		return "A constraints variable.";
	}

	@Override
	public String visit(Unit params, EdsExpRaw exp) {
		return "A set of data integrity constraints, i.e., a theory in regular logic or a set of embedded dependencies (EDs), or finite limit logic plus epis.  Each constraint is also a transform; satisfying it is tantamount to solving a lifting problem.  See All_Syntax for an example.  Note that you can write 'exists unique'.";
	}

	@Override
	public String visit(Unit params, GraphExpRaw exp) {
		return "A literal directed multi graph, given as nodes and edges.  See All_Syntax for an example.";
	}

	@Override
	public String visit(Unit params, GraphExpVar exp) {
		return "A graph variable.";
	}

	@Override
	public String visit(Unit params, GraphExpLiteral exp) {
		return null;
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, PragmaExpConsistent<Gen, Sk, X, Y> exp) {
		return "Checks if an instance has a free type algebra by repeated definitional simplification.  This is an over zealous check for the undecidable condition of being conservative over the type side.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, PragmaExpCheck<Gen, Sk, X, Y> exp) {
		return "Checks if an instance satifies a constraint.";
	}

	@Override
	public String visit(Unit params, PragmaExpMatch exp) {
		return "Computes a match between graphs based on similar measures.";
	}

	@Override
	public String visit(Unit params, PragmaExpSql exp) {
		return "Executes a list of SQL/PSM expressions over JDBC.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, PragmaExpToCsvInst<Gen, Sk, X, Y> exp) {
		return "Emites an instance as a set of CSV files, one file per entity and one column per attribute and foreign key.  \n"
				+ "The file for en will be a CSV file with a header; the fields of the header will be an ID column name (specified using options), as well as any attributes and foreign keys whose domain is en .   CQL values that are not constants will be exported as nulls.  \n"
				+ "See id_column_name, and start_ids_at and csv_emit_ids";
	}

	@Override
	public String visit(Unit params, PragmaExpVar exp) {
		return "A command variable.";
	}

	@Override
	public String visit(Unit params, PragmaExpJs exp) {
		return "Executes java script code in the java virtual machine a la type sides.  Used to intialize type side state, usually.  The CQL environment being constructed can be accessed by the variable named by the option  js_env_name.";
	}

	@Override
	public String visit(Unit params, PragmaExpProc exp) {
		return "Execute a command line process.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, PragmaExpToJdbcInst<Gen, Sk, X, Y> exp) {
		return "Emit an instance (on SQL type side) over a JDBC connection, one table per entity with one column per foreign key and attribute.  There will be a table prefixed by each entity en.  The columns will be the attributes and foreign keys whose domain is en , and an ID column whose name is set in options.  CQL rows that are not syntactically constants will be exported as NULL.  When the [jdbcclass] and [jdbcuri] are the empty string, their values will be determined by the  jdbc_default_class and jdbc_default_string options.  See start_ids_at and id_column_name and varchar_length.";
	}

	@Override
	public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> String visit(Unit params,
			PragmaExpToJdbcTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> exp) {
		return "Exports a transform using JDBC.  There will be a two-column table prefixed for each entity en.  The two columns will be srcidcol  and dstidcol , where idcol  is the ID column name set in the options.   When the [jdbcclass] and [jdbcuri] are the empty string, their values will be determined by the  jdbc_default_class and  jdbc_default_string options.  See varchar_length, and  start_ids_at.  Note that  start_ids_at is not inherited from the source instance.  Can take two option blocks.";
	}

	@Override
	public String visit(Unit params, PragmaExpToJdbcQuery exp) {
		return "Emits a query over JDBC as a set of views.";
	}

	@Override
	public <Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> String visit(Unit params,
			PragmaExpToCsvTrans<Gen1, Sk1, X1, Y1, Gen2, Sk2, X2, Y2> exp) {
		return "Emit a transform to a set of CSV files, one two-column table per entity with one column per foreign key and attribute.  There will be no headers, column 1 is the source.";
	}

	@Override
	public String visit(Unit params, PragmaExpCheck2 exp) {
		return "Checks if a query from s1 to s2 with constraints c1 on s1 to c2 on s2 is valid.";
	}

	public static String getOptionText(AqlOption op) {
		Object o = AqlOptions.initialOptions.getOrDefault(op);
		String s = getOptionText2(op);
		return "<pre>Default: " + o + "</pre>" + s;
	}

	public static void main(String[] args) {
		help(new File("help"), true);
	}

	@SuppressWarnings("unchecked")
	public static void help(File dir, boolean run) {
		try {
			if (!dir.exists()) {
				dir.mkdir();
			}

			String css = "\n<link rel=\"stylesheet\" href=\"http://categoricaldata.net/css/nstyle.css\"><script src=\"http://categoricaldata.net/js/simple.js\"></script>";

			String search = "<html><head>\n" + "\n"
					+ css 
					+ "\n" + "<div>\n" + "  <form action=\"search.php\" method=\"get\">\n"
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
			for (Example ex : Util.alphabetical(
			
				Examples.getExamples(Language.CQL))) {
				examples.append(
						"\n<a href=\"" + ex.getName().trim() + ".html\" target=\"primary\">" + ex.getName().trim() + "</a><br />");

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
									(Instance<catdata.aql.exp.Ty, catdata.aql.exp.En, catdata.aql.exp.Sym, catdata.aql.exp.Fk, catdata.aql.exp.Att, catdata.aql.exp.Gen, catdata.aql.exp.Sk, Object, Object>) i));
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
			logo.append("\n<a href=\"http://categoricaldata.github.io/CQL/\" target=\"primary\">Help</a><br />");
			logo.append("\n<br />");
			logo.append("\n<a href=\"syntax.html\" target=\"tree\">Syntax</a><br />");
			logo.append("\n<a href=\"options.html\" target=\"tree\">Options</a><br />");
			logo.append("\n<a href=\"examples.html\" target=\"tree\">Examples</a><br />");
			logo.append("\n<a href=\"search.php\" target=\"primary\">Search</a><br />");
		
			logo.append("\n<br />");
		//	logo.append("\n<a href=\"http://categorical.info\" target=\"_blank\">CI Website</a><br />");
			logo.append("\n<a href=\"http://categoricaldata.net\" target=\"_blank\">CQL Website</a><br />");
			logo.append("\n<a href=\"http://github.com/CategoricalData/CQL/wiki\" target=\"_blank\">CQL Wiki</a><br />");
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
			main.append("\n  <FRAME src=\"http://categoricaldata.github.io/CQL\" name=\"primary\">");
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
				isFirstK=false;
				syntax.append("<h3>" + k + "</h3>");
				List<Pair<AqlTyping, Exp<?>>> ee = new ArrayList<>(z.get(k));
				Collections.sort(ee, (x, y) -> x.second.getKeyword().compareTo(y.second.getKeyword()));

				for (Pair<AqlTyping, Exp<?>> pair : ee) {
					if (pair.second.isVar()) {
						continue;
					}
				
					AqlTyping g = pair.first;
					Exp<?> e = pair.second;
					String desc = e.accept0(Unit.unit, new AqlHelp());
					
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

					for (Example ex : Util.alphabetical(
							Examples.getExamples(Language.CQL))) {
						if (index.get(ex).contains(e.getSyntax())) {
							str2 += "\n<a href=\"" + ex.getName().trim() + ".html\" target=\"primary\">" + ex.getName().trim()
									+ "</a><br />";
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
//				syntax.append("\t<br");				
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
				for (Example example : Util.alphabetical(
						Examples.getExamples(Language.CQL))) {
					
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

	@Override
	public <Gen, Sk, X, Y> String visit(Unit params, TransExpDiffReturn<Gen, Sk, X, Y> exp) {
		return "Computes the transform (except i1 i2) -> i1.";
	}

	@Override
	public <Gen, Sk, X, Y, Gen1, Gen2, Sk1, Sk2, X1, X2> String visit(Unit params,
			TransExpDiff<Gen, Sk, X, Y, Gen1, Gen2, Sk1, Sk2, X1, X2> exp) {
		return "Computes the transform (except i1 i) -> (except i2 i) from a transform i1 -> i2.";
	}

	@Override
	public <Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> String visit(Unit params,
			TransExpPi<Gen1, Sk1, Gen2, Sk2, X1, Y1, X2, Y2> transExpPi) {
		return "Computes the transform (pi m i1) -> (pi m i2) from a transform i1 -> i2.";
	}

	@Override
	public String visit(Unit params, QueryExpFromCoSpan exp) {
		return "Computes the query that when evaluated will pi along the first mapping then delta along the second.";
	}

	@Override
	public String visit(Unit params, TransExpFrozen transExpFrozen) {
		return "Given a term in context x:E |- e : t in the target of a query, returns a transform frozen t -> frozen E.";
	}

	@Override
	public <Gen, Sk, X, Y> String visit(Unit param, InstExpCascadeDelete<Gen, Sk, X, Y> exp) {
		return "Populate a schema S by doing a cascade delete of the input instance, removing any tuples violating a path or observation equation.";
	}

	@Override
	public String visit(Unit param, SchExpJdbcAll schExpJdbcAll) {
		return "Returns the schema associated with a SQL DB.";
	}

	@Override
	public String visit(Unit params, QueryExpFromEds exp) {
		return "Turns an ED into a query, such that the ED holds when the query's one foreign key is surjective.";
	}

	@Override
	public String visit(Unit params, EdsExpSch exp) throws RuntimeException {
		return "Turns path / observation equations intro constraints.";
	}

	@Override
	public String visit(Unit params, MorExpVar exp) throws RuntimeException {
		return "A theory morphism variable.";
	}

	@Override
	public String visit(Unit params, TyExpAdt exp) throws RuntimeException {
		return "A symbolic typeside with given products and co-products.";
	}

	@Override
	public String visit(Unit params, ApgTyExpVar exp) throws RuntimeException {
		return "An APG typeside variable.";
	}

	@Override
	public String visit(Unit param, ApgInstExpVar exp) throws RuntimeException {
		return "An APG instance variable.";
	}

	@Override
	public String visit(Unit params, ApgTransExpVar exp) throws RuntimeException {
		return "An APG morphism variable.";
	}

	@Override
	public String visit(Unit params, ApgTyExpRaw exp) throws RuntimeException {
		return "An APG typeside literal, consisting of a set of types and a set of values and a typing function taking each value to a type.";
	}

	@Override
	public String visit(Unit param, ApgInstExpRaw exp) throws RuntimeException {
		return "An APG instance literal on a schema, consisting of a set of elements and a function taking each element to a label and to an APG term in such a way that the term has the type given by the schema.";
	}

	@Override
	public String visit(Unit params, ApgTransExpRaw exp) throws RuntimeException {
		return "An APG morphism, consisting of a function taking labels to labels and a function taking elements to elements.";
	}

	@Override
	public String visit(Unit params, ApgInstExpInitial exp) throws RuntimeException {
		return "Initial object in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgInstExpTerminal exp) throws RuntimeException {
		return "Terminal object in the category of APGs";
	}

	@Override
	public String visit(Unit params, ApgInstExpTimes exp) throws RuntimeException {
		return "Product object in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgInstExpPlus exp) throws RuntimeException {
		return "Co-product object in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpTerminal exp) {
		return "Terminal morphism in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpInitial exp) {
		return "Initial morphism in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpFst exp) {
		return "First projection morphism in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpSnd exp) {
		return "Second projection morphism in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpPair exp) {
		return "Pairing morphism in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpInl exp) {
		return "First injection morphism in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpInr exp) {
		return "Second injection morphism in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpCase exp) {
		return "Case analysis morphism in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpId exp) {
		return "Identity morphism in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpCompose exp) {
		return "Composition of morphisms in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgInstExpEqualize apgInstExpEqualize) {
		return "Equalizer object of two parallel morphisms in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpEqualize apgTransExpEqualize) {
		return "Equalizer morphism of two parallel morphisms in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpEqualizeU apgTransExpEqualizeU) {
		return "Equalizer universal property mediating morphism of two parallel morphisms in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgInstExpCoEqualize exp) throws RuntimeException {
		return "CoEqualizer object of two parallel morphisms in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpCoEqualize exp) {
		return "CoEqualizer morphism of two parallel morphisms in the category of APGs.";
	}

	@Override
	public String visit(Unit params, ApgTransExpCoEqualizeU exp) {
		return "CoEqualizer universal property mediating morphism of two parallel morphisms in the category of APGs.";
	}

	@Override
	public String visit(Unit param, ApgSchExpVar exp) {
		return "An APG schema variable.";
	}

	@Override
	public String visit(Unit param, ApgSchExpRaw exp) {
		return "An APG schema literal, consisting of a set of labels and a function taking each label to a type.";
	}

	@Override
	public String visit(Unit param, ApgMapExpVar exp) {
		return "An APG schema mapping variable.";
	}

	@Override
	public String visit(Unit param, ApgMapExpRaw exp) {
		return "An APG mapping literal, consisting of an assignment of labels to types and labels to open terms.";
	}

	@Override
	public String visit(Unit params, ApgSchExpInitial exp) {
		return "Initial object in the category of APG schemas";
	}

	@Override
	public String visit(Unit params, ApgSchExpTerminal exp) {
		return "Terminal object in the category of APG schemas";
	}

	@Override
	public String visit(Unit params, ApgSchExpTimes exp) {
		return "Product object in the category of APG schemas";
	}

	@Override
	public String visit(Unit params, ApgSchExpPlus exp) {
		return "Coproduct object in the category of APG schemas";
	}

	//@Override
	//public String visit(Unit param, ApgMapExpCompose exp) {
	//	return "Composition of APG schema mappings.";
	//}

	@Override
	public String visit(Unit params, ApgInstExpDelta exp) throws RuntimeException {
		return "Delta (model reduct) functor along a schema mapping, applied to an instance.";
	}

	@Override
	public String visit(Unit params, ApgTransExpDelta exp) {
		return "Delta (model reduct) functor along a schema mapping, applied to an APG morphism that is schema preserving and data natural.";
	}

	@Override
	public String visit(Unit param, SchExpCsv schExpCsv) {
		return "Computes the schema associated with a directory of CSV files";
	}

}
