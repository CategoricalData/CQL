/**
 *	A lexical grammar for Aql
 */
lexer grammar AqlLexerRules;

options
   {
     // superClass = 'LexerAdaptor' ;
   }

// Common set of fragments
import LexBasic;

channels {
  HIDDEN_CHANNEL,
  WHITESPACE_CHANNEL,
  BLOCK_COMMENT_CHANNEL,
  LINE_COMMENT_CHANNEL,
  DOC_COMMENT_CHANNEL
}

// ======================================================
// Lexer specification
//
// -------------------------
// Comments

DOC_COMMENT
   : DocComment  -> channel  (DOC_COMMENT_CHANNEL)
   ;


BLOCK_COMMENT
   : BlockComment -> channel (BLOCK_COMMENT_CHANNEL)
   ;


BLOCK_COMMMENT
  : BlockComment -> channel(HIDDEN_CHANNEL)
  ;

LINE_COMMENT
  : LineComment   -> channel(HIDDEN_CHANNEL)
  ;

// -------------------------
// Integer
//

INTEGER
   : DecimalNumeral
   ;

NUMBER
    // 1.35, 1.35E-9, 0.3, -4.5
    :   '-'? DecimalNumeral Dot DecimalNumeral Exponent?
    // 1e10 -3e4
    |   '-'? DecimalNumeral Exponent
    // -3, 45
    |   '-'? DecimalNumeral
    ;

// -------------------------
// Literal string
//
// Aql makes no distinction between a single character
// literal and a multi-character string.
// All literals are single quote delimited and
// may contain unicode escape sequences of the form \uxxxx, where x
// is a valid hexadecimal number (per Unicode standard).

STRING_LITERAL
   : SQuoteLiteral
   ;

UNTERMINATED_STRING_LITERAL
   : USQuoteLiteral
   ;

CHAR : CharLiteral ;
STRING :  DQuoteLiteral ;
MULTI_STRING : DQuote MultiLine DQuote ;

// -------------------------
// Keywords
//
// Keywords may not be used as labels for rules or in any other context where
// they would be ambiguous with the keyword vs some other identifier.
// Some blocks are handled idiomatically
// as island grammars in dedicated lexical modes.

HTML
   : 'html' Ws* LDocQuote  -> pushMode (Html)
   ;


MARKDOWN
   : 'md' Ws* LDocQuote -> pushMode (MarkDown)
   ;

OPTIONS
   : 'options' // -> pushMode (Options)
   ;


LITERAL : 'literal' ;
IMPORTS : 'imports' ;
FORALL : 'forall' ;
WHERE : 'where' ;
EXISTS : 'exists' ;
UNIQUE : 'unique' ;

GRAPH : 'graph' ;
NODES : 'nodes' ;
EDGES : 'edges' ;

INSTANCE : 'instance' ;
EMPTY : 'empty' ;
SRC : 'src' ;
DST : 'dst' ;
DISTINCT : 'distinct' ;
EVAL : 'eval' ;
COEVAL : 'coeval' ;
DELTA : 'delta' ;
SIGMA : 'sigma' ;
COPRODUCT_SIGMA : 'coproduct_sigma' ;
COPRODUCT : 'coproduct' ;
UNION : 'union' ;
COPRODUCT_UNRESTRICTED: 'coproduct_unrestricted' ;
COEQUALIZE : 'coequalize' ;
COLIMIT : 'colimit' ;
IMPORT_JDBC : 'import_jdbc' ;
QUOTIENT_JDBC : 'quotient_jdbc' ;
QUOTIENT_CSV : 'quotient_csv' ;
IMPORT_JDBC_ALL : 'import_jdbc_all' ;
IMPORT_CSV : 'import_csv' ;
STATIC_TYPING : 'static_typing' ;
QUOTIENT : 'quotient' ;
CHASE : 'chase' ;
RANDOM : 'random' ;
GENERATORS : 'generators' ;
EQUATIONS : 'equations' ;
MULTI_EQUATIONS : 'multi_equations' ;
RANDOM_SEED : 'random_seed' ;
ANONYMIZE : 'anonymize' ;
FROZEN : 'frozen' ;
PI : 'pi' ;

MAPPING : 'mapping' ;
IDENTITY : 'identity' ;
ENTITY : 'entity' ;
ENTITIES : 'entities' ;
FOREIGN_KEYS : 'foreign_keys' ;
ATTRIBUTES : 'attributes' ;
LAMBDA : 'lambda' ;

IMPORT_JOINED : 'import_joined' ;
MAP_NULLS_ARBITRARILY_UNSAFE :  'map_nulls_arbitrarily_unsafe' ;
INTERPRET_AS_ALGEGRA :  'interpret_as_algebra' ;
PREPEND_ENTITY_ON_IDS : 'prepend_entity_on_ids' ;
NUM_THREADS :  'num_threads' ;
TIMEOUT :  'timeout' ;
REQUIRE_CONSISTENCY :  'require_consistency' ;
SCHEMA_ONLY :  'schema_only' ;
ALLOW_JAVA_EQS_UNSAFE :  'allow_java_eqs_unsafe' ;
DONT_VALIDATE_UNSAFE :  'dont_validate_unsafe' ;
ALWAYS_RELOAD :  'always_reload' ;
CSV_FIELD_DELIM_CHAR :  'csv_field_delim_char' ;
CSV_ESCAPE_CHAR :  'csv_escape_char' ;
CSV_QUOTE_CHAR :  'csv_quote_char' ;
CSV_FILE_EXTENSION :  'csv_file_extension' ;
CSV_GENERATE_IDS :  'csv_generate_ids' ;
ID_COLUMN_NAME :  'id_column_name' ;
VARCHAR_LENGTH :  'varchar_length' ;
START_IDS_AT :  'start_ids_at' ;
IMPORT_AS_THEORY :  'import_as_theory' ;
JDBC_DEFAULT_CLASS :  'jdbc_default_class' ;
JDBC_DEFAULT_STRING :  'jdbc_default_string' ;
DONT_VERIFY_FOR_UNSAFE :  'dont_verify_is_appropriate_for_prover_unsafe' ;
PROVER :  'prover' ;
PROGRAM_ALLOW_NONTERM_UNSAFE :  'program_allow_nontermination_unsafe' ;
COMPLETION_PRECEDENCE :  'completion_precedence' ;
COMPLETION_SORT :  'completion_sort' ;
COMPLETION_COMPOSE :  'completion_compose' ;
COMPLETION_FILTER_SUBSUMED :  'completion_filter_subsumed' ;
COMPLETION_SYNTACTIC_AC :  'completion_syntactic_ac' ;
QUERY_COMPOSE_USE_INCOMPLETE : 'query_compose_use_incomplete' ;

GUI_MAX_TABLE_SIZE : 'gui_max_table_size' ;
GUI_MAX_GRAPH_SIZE : 'gui_max_graph_size' ;
GUI_MAX_STRING_SIZE : 'gui_max_string_size' ;
GUI_ROWS_TO_DISPLAY : 'gui_rows_to_display' ;

EVAL_MAX_TEMP_SIZE : 'eval_max_temp_size' ;
EVAL_REORDER_JOINS : 'eval_reorder_joins' ;
EVAL_MAX_PLAN_DEPTH : 'eval_max_plan_depth' ;
EVAL_JOIN_SELECTIVITY : 'eval_join_selectivity' ;
EVAL_USE_INDICES : 'eval_use_indices' ;
EVAL_USE_SQL_ABOVE : 'eval_use_sql_above' ;
EVAL_APPROX_SQL_UNSAFE : 'eval_approx_sql_unsafe' ;
EVAL_SQL_PERSISTENT_INDICIES : 'eval_sql_persistent_indices' ;


COPRODUCT_ALLOW_ENTITY : 'coproduct_allow_entity_collisions_unsafe' ;
COPRODUCT_ALLOW_TYPE : 'coproduct_allow_type_collisions_unsafe' ;
QUERY_REMOVE_REDUNDANCY : 'query_remove_redundancy' ;
TRUE : True ;
FALSE : False ;

AUTO : 'auto' ;
FAIL : 'fail' ;
FREE : 'free' ;
SATURATED : 'saturated' ;
CONGRUENCE : 'congruence' ;
MONOIDAL : 'monoidal' ;
PROGRAM : 'program' ;
COMPLETION : 'completion' ;

COMMAND : 'command' ;
EXEC_CMDLINE : 'exec_cmdline' ;
EXEC_JS : 'exec_js' ;
EXEC_JDBC : 'exec_jdbc' ;
LOAD_JARS : 'load_jars' ;
MATCH : 'match' ;
CHECK : 'check' ;
ASSERT_CONSISTENT : 'assert_consistent' ;
EXPORT_CSV_INSTANCE : 'export_csv_instance' ;
EXPORT_CSV_TRANSFORM : 'export_csv_transform' ;
EXPORT_JDBC_INSTANCE : 'export_jdbc_instance' ;
EXPORT_JDBC_QUERY : 'export_jdbc_query' ;
EXPORT_JDBC_TRANSFORM : 'export_jdbc_transform' ;
ADD_TO_CLASSPATH : 'add_to_classpath' ;

QUERY : 'query' ;
SIMPLE : 'simple' ;
GET_MAPPING : 'getMapping' ;
FROM : 'from' ;
RETURN : 'return' ;
TO_QUERY : 'toQuery' ;
TO_COQUERY : 'toCoQuery' ;

SCHEMA : 'schema' ;
SCHEMA_OF : 'schemaOf' ;
GET_SCHEMA : 'getSchema' ;
IMPORT_ALL : 'import_all' ;

SCHEMA_COLIMIT : 'schema_colimit' ;
MODIFY : 'modify' ;
WRAP : 'wrap' ;
ENTITY_EQUATIONS : 'entity_equations' ;
PATH_EQUATIONS : 'path_equations' ;
OBSERVATION_EQUATIONS : 'observation_equations' ;
RENAME : 'rename' ;
REMOVE : 'remove' ;

TRANSFORM : 'transform' ;
UNIT : 'unit' ;
COUNIT : 'counit' ;
UNIT_QUERY : 'unit_query' ;
COUNIT_QUERY : 'counit_query' ;

TYPESIDE : 'typeside' ;
SQL : 'sql' ;
TYPESIDE_OF : 'typesideOf' ;
TYPES : 'types' ;
CONSTANTS : 'constants' ;
FUNCTIONS : 'functions' ;
JAVA_TYPES : 'java_types' ;
JAVA_CONSTANTS : 'java_constants' ;
JAVA_FUNCTIONS : 'java_functions' ;


CONSTRAINTS : 'constraints' ;

// -------------------------
// Punctuation

COLON : Colon ;
COLON_COLON : DColon ;
COMMA : Comma ;
SEMI : Semi ;
LPAREN : LParen ;
RPAREN : RParen ;
LBRACE : LBrace ;
RBRACE : RBrace ;
LBRACK : LBrack ;
RBRACK : RBrack ;
RARROW : RArrow ;
LT : Lt ;
GT : Gt ;
EQUAL : Equal  ;
QUESTION : Question ;
STAR : Star ;
PLUS_ASSIGN : PlusAssign ;
PLUS : Plus ;
OR : Pipe ;
DOLLAR : Dollar ;
RANGE : Range ;
DOT : Dot ;
AT : At ;
POUND : Pound ;
NOT : Tilde ;
UNDERSCORE : Underscore ;

// -------------------------
// Identifiers - allows unicode rule/token names

UPPER_ID: UpperIdLetter (IdLetter | DecDigit)* ;
LOWER_ID: LowerIdLetter (IdLetter | DecDigit)* ;
SPECIAL_ID: IdLetter (IdLetter | DecDigit)* ;

// -------------------------
// Whitespace

WS
   : Ws + -> channel (HIDDEN_CHANNEL)
   ;

// -------------------------
// Illegal Characters
//
// This is an illegal character trap which is always the last rule in the
// lexer specification. It matches a single character of any value and being
// the last rule in the file will match when no other rule knows what to do
// about the character. It is reported as an error but is not passed on to the
// parser. This means that the parser to deal with the gramamr file anyway
// but we will not try to analyse or code generate from a file with lexical
// errors.
//
// Comment this rule out to allow the error to be propagated to the parser

ERRCHAR
   : . -> channel (HIDDEN_CHANNEL)
   ;


// ======================================================
// Lexer modes
// ------------------------------------------------------

mode Html ;
HTML_END : RDocQuote -> popMode;
HTML_MULTI_STRING : DQuote MultiLine DQuote ;

mode MarkDown ;
MD_END : RDocQuote -> popMode;
MD_MULTI_STRING : DQuote MultiLine DQuote ;

// ======================================================
// Grammar specific fragments
// ------------------------------------------------------

fragment IdLetter : 'a'..'z'|'A'..'Z'|'_'|'$' ;
fragment UpperIdLetter : 'A'..'Z' ;
fragment LowerIdLetter : 'a'..'z' ;

fragment LDocQuote : LBrace Ws* LParen Star Ws+ ;
fragment RDocQuote : Ws+ Star RParen Ws* RBrace ;

fragment Exponent : [Ee] [+\-]? DecimalNumeral ;
fragment MultiLine : (EscSeq | ~ ["\\])* ;
