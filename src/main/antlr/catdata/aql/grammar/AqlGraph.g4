parser grammar AqlGraph;
options { tokenVocab=AqlLexerRules; }

graphId : symbol ;
graphRef : symbol ;

graphAssignment : GRAPH graphId EQUAL graphExp ;

graphExp
  : LITERAL
    (LBRACE graphLiteralSection RBRACE)?
  #GraphExp_Literal
  ;

graphKind 
: graphRef               # GraphKind_Ref 
| LPAREN graphExp RPAREN # GraphKind_Exp
;

graphLiteralSection
  : (IMPORTS graphRef*)?
    (NODES graphNodeId*)?
    (EDGES graphEdgeSig*)?
  ;
graphEdgeSig : graphEdgeId+ COLON graphSourceNodeId RARROW graphTargetNodeId ;

graphNodeId : symbol ;
graphSourceNodeId : symbol ;
graphTargetNodeId : symbol ;
graphEdgeId : symbol ;
