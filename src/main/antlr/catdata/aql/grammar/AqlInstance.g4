parser grammar AqlInstance;
options { tokenVocab=AqlLexerRules; }

instanceId : symbol ;
instanceRef : symbol ;

instanceAssignment : INSTANCE instanceId EQUAL instanceExp ;

instanceExp
  : EMPTY COLON schemaKind
  # InstanceExp_Empty

  | SRC transformKind
  # InstanceExp_Src

  | DST transformKind
  # InstanceExp_Dst

  | DISTINCT instanceKind
  # InstanceExp_Distinct

  | EVAL queryKind instanceKind
    (LBRACE instanceEvalSection RBRACE)?
  # InstanceExp_Eval

  | COEVAL queryKind instanceKind
    (LBRACE instanceCoevalSection RBRACE)?
  # InstanceExp_Coeval

  | DELTA mappingKind instanceKind
  # InstanceExp_Delta

  | SIGMA mappingKind instanceKind
    (LBRACE instanceSigmaSection RBRACE)?
  # InstanceExp_Sigma

  | COPRODUCT_SIGMA instanceCoProdPair+ COLON schemaKind
    (LBRACE instanceCoProdSigmaSection RBRACE)?
  # InstanceExp_CoSigma

  | COPRODUCT instanceRef (PLUS instanceRef)* COLON schemaKind
    (LBRACE instanceCoProdSection RBRACE)?
  # InstanceExp_CoProd

  | UNION instanceKind (PLUS instanceKind)* COLON schemaKind
    (LBRACE instanceCoProdSection RBRACE)?
  # InstanceExp_Union

  | COPRODUCT_UNRESTRICTED instanceKind (PLUS instanceKind)* COLON schemaKind
    (LBRACE instanceCoProdUnrestrictSection RBRACE)?
  # InstanceExp_CoProdUn

  | COEQUALIZE transformKind transformKind
    (LBRACE instanceCoequalizeSection RBRACE)?
  # InstanceExp_CoEqual

  | COLIMIT graphKind schemaKind
    (LBRACE instanceColimitSection RBRACE)?
  # InstanceExp_CoLimit

  | IMPORT_JDBC jdbcClass jdbcUri COLON schemaKind
    (LBRACE instanceImportJdbcSection RBRACE)?
  # InstanceExp_ImportJdbc
  
  | QUOTIENT instanceKind
    (LBRACE instanceQuotientSection RBRACE)?
  # InstanceExp_Quotient

  | QUOTIENT_JDBC (jdbcClass (jdbcUri)?)? instanceKind
    (LBRACE instanceQuotientJdbcSection RBRACE)?
  # InstanceExp_QuotientJdbc

  | QUOTIENT_CSV schemaExp
    (LBRACE instanceQuotientCsvSection RBRACE)?
  # InstanceExp_QuotientCsv

  | IMPORT_JDBC_ALL (jdbcClass (jdbcUri)?)?
    (LBRACE instanceImportJdbcAllSection RBRACE)?
  # InstanceExp_ImportJdbcAll

  | IMPORT_CSV instanceFile COLON schemaRef
    (LBRACE instanceImportCsvSection RBRACE)?
  # InstanceExp_ImportCsv

  | CHASE constraintKind instanceKind INTEGER?
    (LBRACE instanceChaseSection RBRACE)?
  # InstanceExp_Chase

  | RANDOM COLON schemaRef
    (LBRACE instanceRandomSection RBRACE)?
  # InstanceExp_Random

  | ANONYMIZE instanceKind
  # InstanceExp_Anonymize

  | FROZEN queryKind schemaKind
  # InstanceExp_Frozen

  | PI mappingKind instanceKind
    (LBRACE instancePiSection RBRACE)?
  # InstanceExp_Pi

  | LITERAL COLON schemaKind
    (LBRACE instanceLiteralSection RBRACE)?
  # InstanceExp_Literal
  ;

instanceKind 
  : instanceRef                 # InstanceKind_Ref 
  | instanceExp                 # InstanceKind_Exp 
  | (LPAREN instanceExp RPAREN) # InstanceKind_Exp
  ;

instanceImportJdbcAllSection : allOptions ;
instancePiSection : allOptions ;

instanceColimitSection
  : NODES instanceColimitNode+ 
    EDGES instanceColimitEdge+
    allOptions
  ;
  
instanceCoProdPair : mappingKind instanceKind ;
instanceColimitNode : instanceRef RARROW instanceKind ;
instanceColimitEdge : schemaArrowId RARROW transformKind ;

instanceLiteralSection
  : (IMPORTS instanceRef*)?
    (GENERATORS instanceLiteralGen+)?
    (EQUATIONS instanceEquation*)?
    (MULTI_EQUATIONS instanceMultiEquation*)?
    allOptions
  ;
  
instanceLiteralGen : instanceGenId+ COLON schemaEntityId ;

instanceImportJdbcSection
  : ((schemaEntityId | schemaAttributeId | schemaForeignId | typesideTypeId)
      RARROW
      instanceSql)+
    allOptions
  ;

jdbcClass : STRING ;
jdbcUri : STRING ;
instanceSql : STRING | MULTI_STRING ;

instanceQuotientCsvSection : instanceFile+ ;
instanceFile : STRING ;

instanceGenId
: symbol
| instanceLiteralValue
;

instanceEquation : instancePath EQUAL instanceEquationValue ;

instanceEquationValue 
: instanceLiteral  # InstanceEq_Literal
| instancePath     # InstanceEq_Path
; 

instanceMultiEquation
  : instanceEquationId RARROW
    LBRACE instanceMultiBind (COMMA instanceMultiBind)* RBRACE
  ;

instanceEquationId : symbol ;

instanceMultiBind : instancePath instanceEquationValue ;

instanceLiteral :  instanceLiteralValue (AT instanceSymbol)? ;

instanceSymbol : symbol ;

instanceLiteralValue
  : truthy    # InstanceLiteralValue_Straight
  | INTEGER   # InstanceLiteralValue_Straight
  | NUMBER    # InstanceLiteralValue_Straight
  | STRING    # InstanceLiteralValue_Quoted
  ;

instancePath
  : instanceArrowId                     # InstancePath_ArrowId
  | instanceLiteralValue                # InstancePath_Literal
  | instancePath DOT instanceArrowId    # InstancePath_Dotted
  | instanceArrowId LPAREN instancePath RPAREN
                                        # InstancePath_Param
  ;

// identity arrows are indicated with entity-names.
instanceArrowId 
: schemaEntityId 
| schemaForeignId
| instanceGenId
;

instanceQuotientJdbcSection
  : (instanceQuotientJdbcName RARROW instanceSql)+
    allOptions
  ;
  
instanceQuotientJdbcName 
: schemaEntityId 
| schemaAttributeId 
| schemaForeignId 
| typesideTypeId
;

instanceQuotientSection
  : EQUATIONS instanceQuotientEqn*
    allOptions
  ;
  
instanceQuotientEqn : instancePath EQUAL instancePath ;
  
instanceChaseSection : allOptions  ;

instanceRandomSection
  : GENERATORS instanceRandomAction*
  allOptions
  ;
  
instanceRandomAction : schemaEntityId RARROW INTEGER ;

instanceEvalSection : allOptions ;
instanceCoevalSection : allOptions  ;
instanceSigmaSection : allOptions ;
instanceCoProdSection : allOptions ;
instanceCoProdSigmaSection : allOptions ;
instanceCoProdUnrestrictSection : allOptions ;
instanceCoequalizeSection : allOptions ;

instanceImportCsvSection
  : (schemaEntityId RARROW
       (LBRACE instanceCsvAssign+ RBRACE))*
    allOptions
  ;

instanceCsvAssign : instanceCsvId RARROW instanceCsvId ;

instanceCsvId : symbol ;
