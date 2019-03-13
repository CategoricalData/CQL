parser grammar AqlTransform;
options { tokenVocab=AqlLexerRules; }

transformId : symbol ;
transformRef : symbol ;

transformAssignment : TRANSFORM transformId EQUAL transformExp ;

transformExp
  : IDENTITY instanceRef
  # TransformExp_Identity

  | LBRACK transformRef SEMI transformRef RBRACK
  # TransformExp_Compose

  | DISTINCT transformRef
  # TransformExp_Distinct

  | EVAL queryKind transformRef
  # TransformExp_Eval

  | COEVAL queryKind transformRef
    (LBRACE transformCoevalSection RBRACE)?
    (LBRACE transformCoevalSection RBRACE)?
  # TransformExp_Coeval

  | SIGMA mappingKind transformRef
    (LBRACE transformSigmaSection RBRACE)?
    (LBRACE transformSigmaSection RBRACE)?
  # TransformExp_Sigma

  | DELTA mappingKind transformRef
  # TransformExp_Delta

  | UNIT mappingKind instanceRef
    (LBRACE transformUnitSection RBRACE)?
  # TransformExp_Unit

  | COUNIT mappingKind instanceRef
    (LBRACE transformUnitSection RBRACE)?
  # TransformExp_Counit

  | UNIT_QUERY queryKind instanceRef
    (LBRACE transformUnitQuerySection RBRACE)?
  # TransformExp_UnitQuery

  | COUNIT_QUERY queryKind instanceRef
    (LBRACE transformCounitQuerySection RBRACE)?
  # TransformExp_CounitQuery

  | IMPORT_JDBC transformJdbcClass transformJdbcUri COLON
      instanceRef RARROW instanceRef
    (LBRACE transformImportJdbcSection RBRACE)?
  # TransformExp_ImportJdbc

  | IMPORT_CSV transformFile COLON instanceRef RARROW instanceRef
    (LBRACE transformImportCsvSection  RBRACE)?
  # TransformExp_ImportCsv

  | LITERAL COLON instanceKind RARROW instanceRef
    (LBRACE transformLiteralSection RBRACE)?
  # TransformExp_Literal
  ;

transformKind 
: transformRef                   # TransformKind_Ref 
| (LPAREN transformExp RPAREN)   # TransformKind_Exp 
;

transformJdbcClass : STRING ;
transformJdbcUri : STRING ;
transformFile : STRING ;
transformSqlExpr : STRING ;

transformSigmaSection : allOptions  ;
transformCoevalSection : allOptions ;
transformUnitSection : allOptions ;
transformUnitQuerySection : allOptions ;
transformCounitQuerySection : allOptions ;

transformImportJdbcSection : transformSqlEntityExpr* allOptions ;

transformImportCsvSection : transformFileExpr* allOptions ;

transformSqlEntityExpr : schemaEntityId RARROW transformSqlExpr ;
transformFileExpr : schemaEntityId RARROW transformFile ;

transformLiteralSection
  : (IMPORTS schemaRef*)?
    (GENERATORS transformGen*)?
    allOptions
  ;

transformGen : symbol RARROW schemaPath ;
