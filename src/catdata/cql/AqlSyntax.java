package catdata.cql;

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

  CommentExp, ColimSchExpPseudo, ColimSchExpQuotient, MapExpPseudo, ColimSchExpRaw, ColimSchExpVar, ColimSchExpWrap, ColimSchExpModify, EdsExpVar, SchExpFront, SchExpTinkerpop, EdsExpTinkerpop,
  EdsExpRaw, EdsExpSch, GraphExpRaw, GraphExpVar, InstExpAnonymize, InstExpCascadeDelete, InstExpChase, InstExpFrozen,
  PragmaExpTinkerpop, PragmaExpBitsy, PragmaExpTinkerpopInstExport, InstExpTinkerpop, // EdsExpOracle, EdsExpMySql, 
  InstExpCoEq, InstExpCoEval, InstExpCoProdFull, InstExpMarkdown, TyExpRdf, InstExpSpanify, SchExpSpan
  , QueryExpSpanify, QueryExpMapToSpanQuery, InstExpCod, InstExpColim, InstExpDelta, InstExpDiff, InstExpDistinct, InstExpDom, InstExpEmpty, InstExpEval,
  InstExpPi, InstExpPivot, InstExpSigma, InstExpSigmaChase, InstExpVar, InstExpCsv, EdsExpInclude, QueryExpFront //, TyExpExcel, InstExpExcel
  , InstExpJdbc, InstExpRdfAll, PragmaExpRdfDirectExport, PragmaExpRdfInstExport, InstExpJsonAll, InstExpXmlAll //, PragmaExpToExcelInst
  , InstExpQueryQuotient, /*PragmaExpCheckSql,*/ PragmaExpJsonInstExport, InstExpJdbcDirect, MapExpFromPrefix, MapExpToPrefix,
  InstExpRandom, InstExpRaw, MapExpComp, MapExpId, MapExpPivot, MapExpVar, MapExpColim, MapExpRaw, PragmaExpCheck,
  PragmaExpConsistent, PragmaExpJs, PragmaExpMatch, PragmaExpProc, PragmaExpSql, PragmaExpToCsvInst, ColimSchExpSimplify, EdsExpSigma,
  PragmaExpToCsvTrans, PragmaExpToJdbcInst, PragmaExpToJdbcQuery, PragmaExpToJdbcTrans, PragmaExpVar, PragmaExpCheck2,
  QueryExpFromEds, QueryExpCompose, QueryExpDeltaCoEval, QueryExpDeltaEval, QueryExpId, QueryExpVar, QueryExpRaw, QueryExpRext,
  QueryExpRawSimple, QueryExpFromCoSpan, SchExpCod, SchExpMsQuery, SchExpMsCatalog, EdsExpFromMsCatalog, SchExpRdf, SchExpDom, SchExpSrc, SchExpDst, SchExpEmpty, SchExpInst,
  SchExpPivot, SchExpVar, SchExpColim, SchExpRaw, SchExpJdbcAll, TransExpDiff, TransExpDiffReturn, TransExpCoEval, SchExpFromMsCatalog,
  TransExpCoEvalEvalCoUnit, TransExpCoEvalEvalUnit, TransExpDelta, TransExpDistinct, TransExpEval, TransExpId,
  TransExpSigma, TransExpSigmaDeltaCounit, TransExpSigmaDeltaUnit, TransExpVar, TransExpCompose, TransExpCsv, SchExpUnit, QueryExpFromInst,
  TransExpDistinct2, TransExpJdbc, TransExpPi, TransExpRaw, TransExpFrozen, TyExpEmpty, TyExpSch, TyExpVar, TyExpRaw,
  TyExpSql  ,  ApgTyExpVar, ApgInstExpVar, ApgTransExpVar ,  ApgTyExpRaw, ApgInstExpRaw, ApgTransExpRaw , SchExpPrefix, EdsExpLearn, QueryExpChase, QueryExpReformulate, TransExpSubseteq   ;

}
