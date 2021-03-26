package catdata.aql;

public enum AqlSyntax {

  ApgSchExpInitial,
  ApgSchExpTerminal,
  ApgSchExpPlus,
  ApgSchExpTimes,
  ApgSchExpVar,
  ApgSchExpRaw,
  //ApgSchExpCsv,

  //ApgMapExpId,
  //ApgMapExpCompose,
  ApgMapExpRaw,
  ApgMapExpVar,

  ApgInstExpDelta,
  ApgInstExpInitial,
  ApgInstExpTerminal,
  ApgInstExpPlus,
  ApgInstExpTimes,
  ApgInstExpEqualize,
  ApgInstExpCoEqualize,

  ApgTransExpId,
  ApgTransExpDelta,
  ApgTransExpCompose,
  ApgTransExpTerminal,
  ApgTransExpInitial,
  ApgTransExpFst,
  ApgTransExpSnd,
  ApgTransExpInl,
  ApgTransExpInr,
  ApgTransExpPair,
  ApgTransExpCase,
  ApgTransExpEqualize,
  ApgTransExpEqualizeU,
  ApgTransExpCoEqualize,
  ApgTransExpCoEqualizeU,

  CommentExp, ColimSchExpQuotient, ColimSchExpRaw, ColimSchExpVar, ColimSchExpWrap, ColimSchExpModify, EdsExpVar, SchExpFront, SchExpTinkerpop, EdsExpTinkerpop,
  EdsExpRaw, EdsExpSch, GraphExpRaw, GraphExpVar, InstExpAnonymize, InstExpCascadeDelete, InstExpChase, InstExpFrozen,
  PragmaExpTinkerpop, PragmaExpBitsy, PragmaExpTinkerpopInstExport, InstExpTinkerpop,
  InstExpCoEq, InstExpCoEval, InstExpCoProdFull, InstExpMarkdown, TyExpRdf, InstExpSpanify, SchExpSpan
  , QueryExpSpanify, QueryExpMapToSpanQuery, InstExpCod, InstExpColim, InstExpDelta, InstExpDiff, InstExpDistinct, InstExpDom, InstExpEmpty, InstExpEval,
  InstExpPi, InstExpPivot, InstExpSigma, InstExpSigmaChase, InstExpVar, InstExpCsv, EdsExpInclude, QueryExpFront
  , InstExpJdbc, InstExpRdfAll, PragmaExpRdfDirectExport, PragmaExpRdfInstExport, InstExpJsonAll, InstExpXmlAll
  , InstExpQueryQuotient, /*PragmaExpCheckSql,*/ PragmaExpJsonInstExport, InstExpJdbcDirect, MapExpFromPrefix, MapExpToPrefix,
  InstExpRandom, InstExpRaw, MapExpComp, MapExpId, MapExpPivot, MapExpVar, MapExpColim, MapExpRaw, PragmaExpCheck,
  PragmaExpConsistent, PragmaExpJs, PragmaExpMatch, PragmaExpProc, PragmaExpSql, PragmaExpToCsvInst,
  PragmaExpToCsvTrans, PragmaExpToJdbcInst, PragmaExpToJdbcQuery, PragmaExpToJdbcTrans, PragmaExpVar, PragmaExpCheck2,
  QueryExpFromEds, QueryExpCompose, QueryExpDeltaCoEval, QueryExpDeltaEval, QueryExpId, QueryExpVar, QueryExpRaw, InstExpMsError,
  QueryExpRawSimple, QueryExpFromCoSpan, SchExpCod, SchExpMsQuery, SchExpMsCatalog, EdsExpFromMsCatalog, SchExpRdf, SchExpDom, SchExpSrc, SchExpDst, SchExpEmpty, SchExpInst, SchExpMsError,
  SchExpPivot, SchExpVar, SchExpColim, SchExpRaw, SchExpJdbcAll, TransExpDiff, TransExpDiffReturn, TransExpCoEval, SchExpFromMsCatalog,
  TransExpCoEvalEvalCoUnit, TransExpCoEvalEvalUnit, TransExpDelta, TransExpDistinct, TransExpEval, TransExpId,
  TransExpSigma, TransExpSigmaDeltaCounit, TransExpSigmaDeltaUnit, TransExpVar, TransExpCompose, TransExpCsv,
  TransExpDistinct2, TransExpJdbc, TransExpPi, TransExpRaw, TransExpFrozen, TyExpEmpty, TyExpSch, TyExpVar, TyExpRaw,
  TyExpSql  ,  ApgTyExpVar, ApgInstExpVar, ApgTransExpVar ,  ApgTyExpRaw, ApgInstExpRaw, ApgTransExpRaw , SchExpPrefix   ;

}
