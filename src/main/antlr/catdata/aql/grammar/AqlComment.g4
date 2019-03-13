parser grammar AqlComment;
options { tokenVocab=AqlLexerRules; }

htmlCommentDeclaration: HTML HTML_MULTI_STRING HTML_END;
mdCommentDeclaration: MARKDOWN MD_MULTI_STRING MD_END;
