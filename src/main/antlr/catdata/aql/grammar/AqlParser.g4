/**
    A grammar for the  Algebreic Query Language (AQL)
    See http://categoricaldata.net/

    This grammar follows the grammar as outlined
    in 'All Syntax.aql'.
 */
parser grammar AqlParser;
options { tokenVocab=AqlLexerRules; }

import
  AqlComment,
  AqlOptions,
  AqlTypeside,
  AqlSchema,
  AqlInstance,
  AqlMapping,
  AqlTransform,
  AqlQuery,
  AqlGraph,
  AqlCommand,
  AqlSchemaColimit,
  AqlConstraint;

file : program  EOF ;

symbol
  : LOWER_ID
  | UPPER_ID
  | SPECIAL_ID
  ;

program
  : optionsDeclarationSection?
    (commentDeclarationSection | kindDeclaration)*
  ;

optionsDeclarationSection
  : OPTIONS optionsDeclaration* ;

commentDeclarationSection
  : htmlCommentDeclaration     #Comment_HTML
  | mdCommentDeclaration       #Comment_MD;

kindDeclaration
  : typesideAssignment       # Kind_Typeside
  | schemaAssignment         # Kind_Schema
  | instanceAssignment       # Kind_Instance
  | mappingAssignment        # Kind_Mapping
  | transformAssignment      # Kind_Transform
  | queryAssignment          # Program_QueryKind
  | graphAssignment          # Program_GraphKind
  | commandAssignment        # Program_CommandKind
  | schemaColimitAssignment  # Program_SchemaKind
  | constraintAssignment     # Program_ConstraintKind
  ;


path : pathNodeId (DOT pathNodeId)* ;

pathNodeId : symbol ;

value
  : STRING
  | NUMBER
  | LOWER_ID
  | UPPER_ID
  ;
  
// the intent is to remove the quotes.
quotedString : STRING ;
quotedMultiString : MULTI_STRING | STRING ;

quotedHtmlString : HTML_MULTI_STRING ;

quotedMarkdownString : MD_MULTI_STRING ;
