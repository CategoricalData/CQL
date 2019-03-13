
parser grammar AqlOptions;
options { tokenVocab=AqlLexerRules; }

allOptions :  (OPTIONS optionsDeclaration*)? ;

optionsDeclaration
  : numThreadsOption
  | randomSeedOption
  | timeoutOption
  | requireConsistencyOption
  | schemaOnlyOption
  | allowJavaEqsUnsafeOption
  | dontValidateUnsafeOption
  | alwaysReloadOption
  | queryComposeUseIncomplete
  | csvOptions
  | idColumnNameOption
  | varcharLengthOption
  | startIdsAtOption
  | importAsTheoryOption
  | jdbcDefaultClassOption
  | jdbDefaultStringOption
  | dVIAFProverUnsafeOption
  | proverOptions
  | guiOptions
  | evalOptions
  | queryRemoveRedundancyOption
  | coproductOptions
  | importJoinedOption
  | completionPresedenceOption
  | prependEntityOnIds
  ;

// options not mentioned in the manual
importJoinedOption : IMPORT_JOINED EQUAL truthy;
completionPresedenceOption : COMPLETION_PRECEDENCE EQUAL STRING;
prependEntityOnIds : PREPEND_ENTITY_ON_IDS  EQUAL truthy ;

mapNullsArbitrarilyUnsafeOption:
  MAP_NULLS_ARBITRARILY_UNSAFE EQUAL truthy;
interpretAsAlgebraOption:  INTERPRET_AS_ALGEGRA EQUAL truthy;

numThreadsOption : NUM_THREADS EQUAL INTEGER;
randomSeedOption :  RANDOM_SEED EQUAL INTEGER;
timeoutOption :  TIMEOUT EQUAL INTEGER;
requireConsistencyOption : REQUIRE_CONSISTENCY EQUAL truthy;
schemaOnlyOption :  SCHEMA_ONLY EQUAL truthy;
allowJavaEqsUnsafeOption : ALLOW_JAVA_EQS_UNSAFE EQUAL truthy;
dontValidateUnsafeOption : DONT_VALIDATE_UNSAFE EQUAL truthy;
alwaysReloadOption :  ALWAYS_RELOAD EQUAL truthy;
queryComposeUseIncomplete : QUERY_COMPOSE_USE_INCOMPLETE EQUAL truthy ;

// docs/aqlmanual/aqlmanual.tex ch 13.10 Import Options
csvOptions:
    CSV_FIELD_DELIM_CHAR EQUAL CHAR
  | CSV_ESCAPE_CHAR EQUAL CHAR
  | CSV_QUOTE_CHAR EQUAL CHAR
  | CSV_FILE_EXTENSION EQUAL STRING
  | CSV_GENERATE_IDS EQUAL truthy
  ;

idColumnNameOption: ID_COLUMN_NAME EQUAL STRING;
varcharLengthOption: VARCHAR_LENGTH EQUAL INTEGER;
startIdsAtOption: START_IDS_AT EQUAL INTEGER;
importAsTheoryOption: IMPORT_AS_THEORY EQUAL truthy;
jdbcDefaultClassOption: JDBC_DEFAULT_CLASS EQUAL STRING;
jdbDefaultStringOption: JDBC_DEFAULT_STRING EQUAL STRING;
dVIAFProverUnsafeOption:
  DONT_VERIFY_FOR_UNSAFE EQUAL truthy;

// provers and their options
proverOptions:
    PROVER EQUAL proverType
  | PROGRAM_ALLOW_NONTERM_UNSAFE EQUAL truthy
  | COMPLETION_PRECEDENCE EQUAL LBRACK STRING+ RBRACK
  | COMPLETION_SORT EQUAL truthy
  | COMPLETION_COMPOSE EQUAL truthy
  | COMPLETION_FILTER_SUBSUMED EQUAL truthy
  | COMPLETION_SYNTACTIC_AC EQUAL truthy
  ;

// docs/aqlmanual/aqlmanual.tex ch 13.19 GUI Options
guiOptions:
    GUI_MAX_TABLE_SIZE EQUAL INTEGER
  | GUI_MAX_GRAPH_SIZE EQUAL INTEGER
  | GUI_MAX_STRING_SIZE EQUAL INTEGER
  | GUI_ROWS_TO_DISPLAY EQUAL INTEGER
  ;

// docs/aqlmanual/aqlmanual.tex ch 13.20 Evaluation Options
evalOptions:
    EVAL_MAX_TEMP_SIZE EQUAL INTEGER
  | EVAL_REORDER_JOINS EQUAL truthy
  | EVAL_MAX_PLAN_DEPTH EQUAL INTEGER
  | EVAL_JOIN_SELECTIVITY EQUAL truthy
  | EVAL_USE_INDICES EQUAL truthy
  | EVAL_USE_SQL_ABOVE EQUAL truthy
  | EVAL_APPROX_SQL_UNSAFE EQUAL truthy
  | EVAL_SQL_PERSISTENT_INDICIES EQUAL truthy
  ;


// docs/aqlmanual/aqlmanual.tex ch 13.22 Coproduct Options
coproductOptions
  : COPRODUCT_ALLOW_ENTITY EQUAL truthy
  | COPRODUCT_ALLOW_TYPE EQUAL truthy
  ;

queryRemoveRedundancyOption: QUERY_REMOVE_REDUNDANCY EQUAL truthy;

truthy : TRUE | FALSE;

proverType
  : AUTO
  | FAIL
  | FREE
  | SATURATED
  | CONGRUENCE
  | MONOIDAL
  | PROGRAM
  | COMPLETION
  ;
