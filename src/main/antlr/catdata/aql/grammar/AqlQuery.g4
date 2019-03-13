parser grammar AqlQuery;
options { tokenVocab=AqlLexerRules; }

queryId : symbol ;
queryRef : symbol ;

queryFromSchema : LPAREN IDENTITY schemaRef RPAREN ;

queryAssignment : QUERY queryId EQUAL queryExp ;

queryExp
  : IDENTITY schemaRef
  #QueryExp_Identity

  | GET_MAPPING schemaColimitKind schemaRef
  #QueryExp_Get

  | TO_QUERY mappingKind
      (LBRACE queryDeltaEvalSection RBRACE)?
  #QueryExp_ToQuery

  | TO_COQUERY mappingKind
      (LBRACE queryDeltaCoEvalSection RBRACE)?
  #QueryExp_ToCoquery

  | LBRACK queryKind SEMI queryKind RBRACK allOptions
  #QueryExp_Compose

  | SIMPLE COLON schemaKind
      (LBRACE querySimpleSection RBRACE)?
  #QueryExp_Simple

  | LITERAL COLON schemaKind RARROW schemaRef
      (LBRACE queryLiteralSection RBRACE)?
  #QueryExp_Literal
  ;

queryKind
  : queryRef                # QueryKind_Ref 
  | queryExp                # QueryKind_Exp 
  | LPAREN queryExp RPAREN  # QueryKind_Exp 
  ;

queryDeltaEvalSection : allOptions ;
queryDeltaCoEvalSection : allOptions ;

querySimpleSection : queryClauseExpr ;
queryLiteralSection
  : (IMPORTS queryRef*)?
    (ENTITY queryEntityExpr*)+
    allOptions
  ;
  
queryEntityExpr : schemaEntityId RARROW LBRACE queryClauseExpr RBRACE ;

queryClauseExpr
  : FROM queryClauseFrom+
    (WHERE queryClauseWhere+)?
    (ATTRIBUTES queryPathMapping+)?
    (FOREIGN_KEYS queryForeignSig+)?
    allOptions
  ;
  
queryClauseFrom : queryGen COLON schemaEntityId ;
queryClauseWhere : queryPath EQUAL queryPath ;
queryPathMapping : queryGen RARROW queryPath ;
queryForeignSig
  : schemaForeignId RARROW LBRACE queryPathMapping+ RBRACE ;

queryGen : symbol ;

queryPath
   : queryLiteralValue                # QueryPath_Literal
   | typesideConstantId               # QueryPath_TypeConst
   | queryGen                         # QueryPath_GenBare
   | queryGen (DOT schemaArrowId)+    # QueryPath_GenArrow
   | queryGen LPAREN queryPath (COMMA queryPath)* RPAREN
                                      # QueryPath_GenParam
   ;

queryLiteralValue
  : STRING
  | NUMBER
  | INTEGER
  | TRUE
  | FALSE ;

