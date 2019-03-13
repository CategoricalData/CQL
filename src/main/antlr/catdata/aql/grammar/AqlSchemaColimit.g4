parser grammar AqlSchemaColimit;
options { tokenVocab=AqlLexerRules; }

schemaColimitId: symbol ;
schemaColimitRef: symbol ;

schemaColimitAssignment: SCHEMA_COLIMIT schemaColimitId EQUAL schemaColimitExp ;

schemaColimitExp
  : QUOTIENT schemaRef (PLUS schemaRef)* COLON typesideRef
      (LBRACE schemaColimitQuotientSection allOptions RBRACE)?  
  # SchemaColimitExp_Quotient
    
  | COPRODUCT schemaRef (PLUS schemaRef)* COLON typesideRef
  # SchemaColimitExp_CoProduct
    
  | MODIFY schemaColimitRef
      (LBRACE schemaColimitModifySection allOptions RBRACE)?
  # SchemaColimitExp_Modify
    
  | WRAP schemaColimitRef mappingRef mappingRef
  # SchemaColimitExp_Wrap
  ;

schemaColimitKind
: schemaColimitRef   # SchemaColimitKind_Ref 
| LPAREN schemaColimitExp RPAREN  # SchemaColimitKind_Exp 
;

schemaColimitQuotientSection
: (ENTITY_EQUATIONS scQuotientEqu*)?
  (PATH_EQUATIONS scQuotientFkEqu*)?
    (OBSERVATION_EQUATIONS scObsEquation* )?
  ;

scQuotientEqu : scTermPath EQUAL scTermPath ;
scQuotientFkEqu : scSymPath EQUAL scSymPath ;

scObsEquation
: FORALL scGen DOT scTermPath EQUAL scTermPath
  ;

scGen : symbol (COLON scGenType)? ;
scGenType : symbol ;

scTermPath
: schemaRef DOT schemaTermId  # ScTermPath_Dotted
| schemaTermId                # ScTermPath_Singular
;

scSymPath : scAlias (DOT scAlias)* ;
scAlias : symbol ;

scEntityId : symbol ;
scEntityAlias : symbol ;
scFkId : symbol ;
scFkAlias : symbol ;
scAttrId : symbol ;
scAttrAlias : symbol ;

scArrowRenameEnt : scEntityId RARROW scEntityAlias ;
scArrowRenameFk : scEntityAlias DOT scFkId RARROW scFkAlias ;
scArrowRenameAttr : scEntityAlias DOT scAttrId RARROW scAttrAlias ;
scArrowDeleteFk : scEntityAlias DOT scFkId RARROW scFkAlias (DOT scFkAlias)* ;
scArrowDeleteAttr : scEntityAlias DOT scAttrId RARROW scAttrAlias (DOT scAttrAlias)* ;

schemaColimitModifySection
: ( RENAME ENTITIES scArrowRenameEnt*    
  | RENAME FOREIGN_KEYS scArrowRenameFk* 
  | RENAME ATTRIBUTES scArrowRenameAttr*   
  | REMOVE FOREIGN_KEYS scArrowDeleteFk* 
  | REMOVE ATTRIBUTES scArrowDeleteAttr* )*
  ;
