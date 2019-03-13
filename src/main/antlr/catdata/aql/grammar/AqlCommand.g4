parser grammar AqlCommand;
options { tokenVocab=AqlLexerRules; }

commandId : symbol ;
commandRef : symbol ;

commandAssignment : COMMAND commandId EQUAL commandExp ;

commandExp
  : EXEC_CMDLINE
      (LBRACE commandCmdLineSection RBRACE)?
    # CommandExp_CmdLine

  | EXEC_JS
      (LBRACE commandExecJsSection RBRACE)?
    # CommandExp_ExecJs

  | EXEC_JDBC commandJdbcClass commandJdbcUri
      (LBRACE commandExecJdbcSection RBRACE)?
    # CommandExp_ExecJdbc

  | CHECK constraintRef instanceRef
    # CommandExp_Check

  | LOAD_JARS 
      (LBRACE commandLoadJarsSection RBRACE)?
    # CommandExp_LoadJars

  | MATCH commandMatchWhich graphRef graphRef
      (LBRACE commandMatchSection RBRACE)?
    # CommandExp_Match

  | ASSERT_CONSISTENT instanceRef
    # CommandExp_AssertConsistent

  | EXPORT_CSV_INSTANCE instanceRef commandFile
      (LBRACE commandExportCsvSection RBRACE)?
    # CommandExp_ExportCsvInstance

  | EXPORT_CSV_TRANSFORM transformRef commandFile
      (LBRACE commandExportCsvSection RBRACE)?
    # CommandExp_ExportCsvTransform

  | EXPORT_JDBC_INSTANCE transformRef
      (commandJdbcClass (commandJdbcUri commandPrefixDst?)?)?
      (LBRACE commandExportJdbcSection RBRACE)?
    # CommandExp_ExportJdbcInstance

  | EXPORT_JDBC_QUERY queryRef
      (commandJdbcClass (commandJdbcUri (commandPrefixSrc commandPrefixDst?)?)?)?
      (LBRACE commandExportJdbcSection RBRACE)?
    # CommandExp_ExportJdbcQuery

  | EXPORT_JDBC_TRANSFORM transformRef
      (commandJdbcClass (commandJdbcUri commandPrefix?)?)?
      (LBRACE commandExportJdbcSection RBRACE)?
      (LBRACE commandExportJdbcSection RBRACE)?
    # CommandExp_ExportJdbcTransform

  | ADD_TO_CLASSPATH
      (LBRACE commandAddClasspathSection RBRACE)?
    # CommandExp_AddToClasspath
  ;

commandKind 
: commandRef               # CommandKind_Ref 
| LPAREN commandExp RPAREN # CommandKind_Exp
;

commandAddClasspathSection : quotedString+ ;

commandCmdLineSection : quotedString* allOptions ;

commandExecJsSection : quotedString* allOptions ;

commandExecJdbcSection : quotedMultiString+ allOptions ;

commandLoadJarsSection : quotedString* ;

commandMatchSection : allOptions ;

commandExportCsvSection : quotedString* allOptions ;

commandExportJdbcSection : quotedString* allOptions ;

commandFile : quotedString ;
commandJdbcClass : quotedString ;
commandJdbcUri : quotedString ;
commandPrefix : quotedString ;
commandPrefixSrc : quotedString ;
commandPrefixDst : quotedString ;
commandMatchWhich : quotedString ;
