package catdata.cql.exp;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import catdata.Pair;
import catdata.ParseException;
import catdata.Program;
import catdata.Triple;
import catdata.Util;
import catdata.cql.AqlOptions;
import catdata.cql.Kind;

public interface IAqlParser {

	public static final String[] ops = new String[] { ",", ".", ";", ":", "(", ")", "=", "->", "@", "(*", "*)", "+",
			"[", "]", "<-", "*", "<", ">", "|", "!" };

	public static final char[] opsC = new char[] { ',', '.', ';', ':', '(', ')', '=', '-', '@', '(', ')', '+', '[', ']',
			'*', '<', '>', '|', '!' };

	public static final String[] res = new String[] { "skolem", "tinkerpop", "import_tinkerpop_all", "pseudo_quotient",
			"getPseudo", "export_tinkerpop_instance", "exec_tinkerpop", "spawn_bitsy", "front", "back",
			"from_ms_catalog", "ms_error_shallow", "ms_error", "to_prefix", "from_prefix", "prefix", "ms_query",
			"import_jdbc_direct", "ms_catalog", "spanify_mapping", "export_rdf_direct_xml", "import_md",
			"import_xml_all", "import_json_ld_all", "export_json_instance", "export_rdf_instance_xml", "fromInstance",
			"check_sql", "aggregate", "case", "from", "base", "label", "fst", "snd", "inl", "inr", "equalize_u",
			"equalize", "data", "elements", "labels", "void", "coequalize_u", "from_oracle", "from_mysql",
			"cascade_delete", "fromCoSpan", "dom_q", "cod_q", "dom_t", "sqlNull", "check_sql", "rdf", "spanify",
			"cod_t", "except_return", "quotient_query", "pivot", "include", "except", "check_query", "distinct_return",
			"sigma_chase", "entity", "md", "quotient_jdbc", "random", "sql", "chase", "check", "import_csv",
			"quotient_csv", "coproduct", "simple", "rext", "assert_consistent", "coproduct_sigma", "coequalize", "html",
			"quotient", "entity_equations", "exists", "getMapping", "getSchema", "exec_jdbc", "exec_js", "simplify",
			"chase_return", "exec_cmdline", "literal", "identity", "match", "attributes", "empty", "imports", "types",
			"constants", "functions", "equations", "forall", "java_types", "external_types", "multi_equations", "pi",
			"bindings", "fromSchema", "toQuery", "toCoQuery", "anonymize", "excel", "import_excel", "frozen", "params",
			"java_constants", "java_functions", "external_parsers", "export_excel_instance", "external_functions",
			"options", "entities", "dom_m", "unique", "cod_m", "path_equations", "observation_equations", "generators",
			"rename", "remove", "modify", "foreign_keys", "lambda", "sigma", "delta", "pi", "unit", "counit", "eval",
			"coeval", "ed", "chase", "from", "where", "return", "pivot", "copivot", "colimit", "nodes", "edges",
			"typesideOf", "schemaOf", "distinct", "export_csv_instance", "export_csv_transform", "import_jdbc",
			"import_rdf_all", "learn", "import_jdbc_all", "export_jdbc_transform", "export_jdbc_instance",
			"export_jdbc_query", "unit_query", "counit_query", "union", "entity_isomorphisms", "wrap",
			"fromConstraints", "theory_morphism", "reformulate", "subseteq" };

	public static final String[] opts = AqlOptions.optionNames().toArray(new String[0]);

	public Program<Exp<?>> parseProgram(String str) throws ParseException;

	public Program<Exp<?>> parseProgram(Reader rdr) throws ParseException, IOException;

	public Triple<List<Pair<String, String>>, RawTerm, RawTerm> parseEq(String s) throws ParseException;

	public Pair<List<Pair<String, String>>, RawTerm> parseTermInCtx(String s) throws ParseException;

	public RawTerm parseTermNoCtx(String s) throws ParseException;

}
