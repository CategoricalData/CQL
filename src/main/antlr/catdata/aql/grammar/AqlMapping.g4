parser grammar AqlMapping;
options { tokenVocab=AqlLexerRules; }

mappingId : symbol ;
mappingRef : symbol ;

mappingAssignment : MAPPING mappingId EQUAL mappingExp ;

mappingExp
  : IDENTITY schemaRef                       
  # MappingExp_Identity
  
  | LBRACK mappingRef (SEMI mappingRef)+ RBRACK   
  # MappingExp_Compose
  
  | GET_MAPPING schemaColimitRef schemaRef 
  # MappingExp_Get
  
  | LITERAL COLON schemaRef RARROW schemaRef
            LBRACE mappingLiteralSection RBRACE      
  # MappingExp_Literal
  ;

mappingKind
  : mappingRef                # MappingKind_Ref 
  | mappingExp                # MappingKind_Exp 
  | LPAREN mappingExp RPAREN  # MappingKind_Exp 
  ;

mappingLiteralSection
  : (IMPORTS mappingRef*)?
    mappingLiteralSubsection*
    allOptions
  ;
mappingLiteralSubsection
  : ENTITY mappingEntitySig         
    (FOREIGN_KEYS mappingForeignSig*)? 
    (ATTRIBUTES mappingAttributeSig*)? 
  ;

mappingEntitySig : schemaEntityId RARROW schemaEntityId ;

mappingForeignSig
  : schemaForeignId RARROW schemaPath ;

mappingAttributeSig
  : schemaAttributeId RARROW mappingAttributeTerm ;

mappingAttributeTerm
  : LAMBDA mappingGen (COMMA mappingGen)* DOT evalMappingFn
  # MappingAttrTerm_Lambda
  
  | schemaPath
  # MappingAttrTerm_Path
  ;

mappingGen : symbol (COLON mappingGenType)? ;
mappingGenType : symbol ;

evalMappingFn
  : mappingGen   
  # EvalMappingFn_Gen
  
  | mappingFn LPAREN evalMappingFn (COMMA evalMappingFn)* RPAREN   
  # EvalMappingFn_Mapping
  
  | LPAREN evalMappingFn (typesideFnName evalMappingFn)* RPAREN   
  # EvalMappingFn_Typeside
  ;

mappingFn : typesideFnName | schemaAttributeId | schemaForeignId ;
