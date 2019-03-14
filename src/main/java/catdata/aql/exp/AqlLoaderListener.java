//
package catdata.aql.exp;

public class AqlLoaderListener {

	public Object decls;
	public Object kind;
}
//
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.function.Function;
//import java.util.logging.Logger;
//import java.util.stream.Collectors;
//
//import org.antlr.v4.runtime.ParserRuleContext;
//import org.antlr.v4.runtime.RuleContext;
//import org.antlr.v4.runtime.tree.ParseTree;
//import org.antlr.v4.runtime.tree.ParseTreeProperty;
//import org.antlr.v4.runtime.tree.TerminalNode;
//import org.apache.commons.lang3.StringUtils;
//
//import catdata.Chc;
//import catdata.Pair;
//import catdata.Quad;
//import catdata.Triple;
//import catdata.aql.RawTerm;
//import catdata.aql.Var;
//import catdata.aql.exp.ColimSchExp.ColimSchExpVar;
//import catdata.aql.exp.EdsExpRaw.EdExpRaw;
//import catdata.aql.exp.GraphExp.GraphExpRaw;
//import catdata.aql.exp.InstExpRaw.Gen;
//import catdata.aql.exp.InstExpRaw.Sk;
//import catdata.aql.exp.PragmaExp.PragmaExpCheck;
//import catdata.aql.exp.PragmaExp.PragmaExpConsistent;
//import catdata.aql.exp.PragmaExp.PragmaExpProc;
//import catdata.aql.exp.PragmaExp.PragmaExpSql;
//import catdata.aql.exp.PragmaExp.PragmaExpToJdbcTrans;
//import catdata.aql.exp.QueryExpRaw.Block;
//import catdata.aql.exp.QueryExpRaw.PreBlock;
//import catdata.aql.exp.QueryExpRaw.Trans;
//import catdata.aql.exp.SchExp.SchExpEmpty;
//import catdata.aql.exp.SchExpRaw.Att;
//import catdata.aql.exp.SchExpRaw.En;
//import catdata.aql.exp.SchExpRaw.Fk;
//import catdata.aql.exp.TransExp.TransExpId;
//import catdata.aql.exp.TyExpRaw.Sym;
//import catdata.aql.exp.TyExpRaw.Ty;
//import catdata.aql.grammar.AqlParser;
//import catdata.aql.grammar.AqlParser.CommandCmdLineSectionContext;
//import catdata.aql.grammar.AqlParser.CommandExecJdbcSectionContext;
//import catdata.aql.grammar.AqlParser.CommandExecJsSectionContext;
//import catdata.aql.grammar.AqlParser.CommandExportCsvSectionContext;
//import catdata.aql.grammar.AqlParser.CommandExportJdbcSectionContext;
//import catdata.aql.grammar.AqlParser.CommandFileContext;
//import catdata.aql.grammar.AqlParser.CommandJdbcClassContext;
//import catdata.aql.grammar.AqlParser.CommandJdbcUriContext;
//import catdata.aql.grammar.AqlParser.CommandPrefixContext;
//import catdata.aql.grammar.AqlParser.CommandPrefixDstContext;
//import catdata.aql.grammar.AqlParser.CommandPrefixSrcContext;
//import catdata.aql.grammar.AqlParser.ConstraintExistentialContext;
//import catdata.aql.grammar.AqlParser.ConstraintExistentialEquationContext;
//import catdata.aql.grammar.AqlParser.ConstraintKindContext;
//import catdata.aql.grammar.AqlParser.ConstraintLiteralSectionContext;
//import catdata.aql.grammar.AqlParser.ConstraintRefContext;
//import catdata.aql.grammar.AqlParser.ConstraintUniversalContext;
//import catdata.aql.grammar.AqlParser.ConstraintUniversalEquationContext;
//import catdata.aql.grammar.AqlParser.GraphKindContext;
//import catdata.aql.grammar.AqlParser.GraphLiteralSectionContext;
//import catdata.aql.grammar.AqlParser.InstanceChaseSectionContext;
//import catdata.aql.grammar.AqlParser.InstanceCoProdSectionContext;
//import catdata.aql.grammar.AqlParser.InstanceCoProdUnrestrictSectionContext;
//import catdata.aql.grammar.AqlParser.InstanceCoequalizeSectionContext;
//import catdata.aql.grammar.AqlParser.InstanceCoevalSectionContext;
//import catdata.aql.grammar.AqlParser.InstanceColimitSectionContext;
//import catdata.aql.grammar.AqlParser.InstanceEvalSectionContext;
//import catdata.aql.grammar.AqlParser.InstanceKindContext;
//import catdata.aql.grammar.AqlParser.InstanceLiteralSectionContext;
//import catdata.aql.grammar.AqlParser.InstancePiSectionContext;
//import catdata.aql.grammar.AqlParser.InstanceRandomSectionContext;
//import catdata.aql.grammar.AqlParser.InstanceRefContext;
//import catdata.aql.grammar.AqlParser.InstanceSigmaSectionContext;
//import catdata.aql.grammar.AqlParser.InstanceSymbolContext;
//import catdata.aql.grammar.AqlParser.MappingGenContext;
//import catdata.aql.grammar.AqlParser.MappingKindContext;
//import catdata.aql.grammar.AqlParser.MappingLiteralSectionContext;
//import catdata.aql.grammar.AqlParser.QueryDeltaCoEvalSectionContext;
//import catdata.aql.grammar.AqlParser.QueryDeltaEvalSectionContext;
//import catdata.aql.grammar.AqlParser.QueryGenContext;
//import catdata.aql.grammar.AqlParser.QueryKindContext;
//import catdata.aql.grammar.AqlParser.QueryLiteralSectionContext;
//import catdata.aql.grammar.AqlParser.QueryPathContext;
//import catdata.aql.grammar.AqlParser.QueryRefContext;
//import catdata.aql.grammar.AqlParser.QuerySimpleSectionContext;
//import catdata.aql.grammar.AqlParser.SchemaArrowIdContext;
//import catdata.aql.grammar.AqlParser.SchemaColimitKindContext;
//import catdata.aql.grammar.AqlParser.SchemaColimitModifySectionContext;
//import catdata.aql.grammar.AqlParser.SchemaColimitQuotientSectionContext;
//import catdata.aql.grammar.AqlParser.SchemaColimitRefContext;
//import catdata.aql.grammar.AqlParser.SchemaEntityIdContext;
//import catdata.aql.grammar.AqlParser.SchemaEquationSigContext;
//import catdata.aql.grammar.AqlParser.SchemaGenTypeContext;
//import catdata.aql.grammar.AqlParser.SchemaKindContext;
//import catdata.aql.grammar.AqlParser.SchemaLiteralSectionContext;
//import catdata.aql.grammar.AqlParser.SchemaPathContext;
//import catdata.aql.grammar.AqlParser.SchemaRefContext;
//import catdata.aql.grammar.AqlParser.TransformCoevalSectionContext;
//import catdata.aql.grammar.AqlParser.TransformCounitQuerySectionContext;
//import catdata.aql.grammar.AqlParser.TransformFileContext;
//import catdata.aql.grammar.AqlParser.TransformImportCsvSectionContext;
//import catdata.aql.grammar.AqlParser.TransformImportJdbcSectionContext;
//import catdata.aql.grammar.AqlParser.TransformJdbcClassContext;
//import catdata.aql.grammar.AqlParser.TransformJdbcUriContext;
//import catdata.aql.grammar.AqlParser.TransformKindContext;
//import catdata.aql.grammar.AqlParser.TransformLiteralSectionContext;
//import catdata.aql.grammar.AqlParser.TransformRefContext;
//import catdata.aql.grammar.AqlParser.TransformSigmaSectionContext;
//import catdata.aql.grammar.AqlParser.TransformUnitQuerySectionContext;
//import catdata.aql.grammar.AqlParser.TransformUnitSectionContext;
//import catdata.aql.grammar.AqlParser.TypesideLiteralContext;
//import catdata.aql.grammar.AqlParser.TypesideLiteralSectionContext;
//import catdata.aql.grammar.AqlParser.TypesideRefContext;
//import catdata.aql.grammar.AqlParserBaseListener;
//
///**
// * This is a parser based on the antlr4 grammar.
// * We mimic the behavior of the original 
// * jparsec based AqlParser.
// *
// * see CombinatorParser.java for reference.
// * 
// */
//public class AqlLoaderListener { extends AqlParserBaseListener {
//	private static Logger log = Logger.getLogger(AqlLoaderListener.class.getName());
//	
//	public static class X extends Object {
//		
//	}
//	public static class Y extends Object {
//		
//	}
//	
//	/**
//	 * The location of tokens is used by the editor.
//	 * It may be better to look up the constant id property.
//	 * 
//	 * @param ctx
//	 * @return
//	 */
//	private LocStr makeLocStr(ParserRuleContext ctx) {
//		return new LocStr(ctx.getStart().getStartIndex(), unquote(ctx.getText()));
//	}	
//	private Integer getLoc(ParserRuleContext ctx) {
//		return ctx.getStart().getStartIndex();
//	}
//	
//	public AqlLoaderListener() {}
//
//	public final List<Triple<String, Integer, Exp<?>>> decls = new LinkedList<>();
//	public final List<Pair<String, String>> global_options = new LinkedList<>();
//	public Function<Exp<?>, String> kind = q -> q.kind().toString();
//		
//	private final ParseTreeProperty<List<String>> path = new ParseTreeProperty<>();
//	private final ParseTreeProperty<String> str = new ParseTreeProperty<>();
//	private final ParseTreeProperty<Exp<?>> exps = new ParseTreeProperty<>();
//	private final ParseTreeProperty<RawTerm> terms = new ParseTreeProperty<>();
//	
//	public final Map<String, Exp<?>> ns = new HashMap<>();
//	
//	/******
//	 * Utility functions
//	 * 
//	 */
//
//	private String nullable(final RuleContext ruleCtx) {
//	  return (ruleCtx == null) ? null : ruleCtx.getText();
//	}
//	
//	/**
//	 * The parser could be improved to remove the quotes.
//	 * The tricky bit is that these are island grammars.
//	 * 
//	 * @param quoted
//	 * @return
//	 */
//	private String unquote(final String quoted) {
//		return StringUtils.removeEnd(StringUtils.removeStart(quoted, "\""), "\"");
//	}
//	
//	@Override public void exitQuotedString(AqlParser.QuotedStringContext ctx) {
//		this.str.put(ctx, unquote(ctx.getText()));
//	}	
//	@Override public void exitQuotedMultiString(AqlParser.QuotedMultiStringContext ctx) {
//		this.str.put(ctx, unquote(ctx.getText()));
//	}
//	
///**
// * Process all options rule
// * @param options TODO
// */
//
//	private Map<String, String>
//	toMap(final List<Pair<String,String>> options) 	{ 
//		return options.stream()
//			.collect(Collectors.toMap(p -> p.first, p -> p.second)); 
//	}
//	
//	@SuppressWarnings("unused")
//	private List<Pair<String,String>> 
//	toList(Map<String, String> opts) {
//		return opts.entrySet().stream() 
//			.map(x -> new Pair<>(x.getKey(), x.getValue()))
//			.collect(Collectors.toList());
//	}
//	
//	private final ParseTreeProperty<List<Pair<String,String>>>
//	aopts = new ParseTreeProperty<>();
//	
//	@Override public void exitAllOptions(AqlParser.AllOptionsContext ctx) {
//		final List<Pair<String,String>>	
//		options = ctx.optionsDeclaration().stream() 
//					.map(elt -> 
//						new Pair<>(
//							elt.getStart().getText(),
//							unquote(elt.getStop().getText()))) 
//					.collect(Collectors.toList());
//		
//		this.aopts.put(ctx, options);
//	}
//	
//	
//	/***************************************************
//	 * Program section
//	 * see AqlParser.g4
//	 */
//	
//	@Override 
//	public void exitProgram(AqlParser.ProgramContext ctx) { 
//		if (true) ;
//	}
//	
//	/***************************************************
//	 * Options section
//	 * see AqlOptions.g4
//	 */
//
//	private final ParseTreeProperty<Pair<String,String>> value_option = new ParseTreeProperty<>();
//	
//	@Override public void exitOptionsDeclarationSection(AqlParser.OptionsDeclarationSectionContext ctx) {
//		for(final ParseTree child : ctx.optionsDeclaration() ) {
//			this.global_options.add(this.value_option.get(child));
//		}
//	}
//	
//	@Override 
//	public void exitOptionsDeclaration(AqlParser.OptionsDeclarationContext ctx) { 
//		value_option.put(ctx, 
//				new Pair<>(ctx.start.getText(), ctx.stop.getText()));
//	}
//	
//	/***************************************************
//	 * Comment section
//	 * see AqlComment.g4
//	 */
//	
//	@Override
//	public void exitComment_HTML(AqlParser.Comment_HTMLContext ctx) {
//		final String unquoted = unquote(ctx.htmlCommentDeclaration().HTML_MULTI_STRING().getText());
//		final Exp<?> exp = new CommentExp(unquoted, true);
//		final Integer id = getLoc(ctx);
//		String name = "html" + id;
//		this.decls.add(new Triple<>(name,id,exp));
//	}
//	
//	@Override
//	public void exitComment_MD(AqlParser.Comment_MDContext ctx) {
//		final String unquoted = unquote(ctx.mdCommentDeclaration().MD_MULTI_STRING().getText());
//		final Exp<?> exp = new CommentExp(unquoted, true);
//		final Integer id = getLoc(ctx);
//		String name = "md" + id;
//		this.decls.add(new Triple<>(name,id,exp));
//	}
//	
//	/***************************************************
//	 * Graph section
//	 * see AqlGraph.g4
//	 */
//	
//	@Override public void exitGraphAssignment(AqlParser.GraphAssignmentContext ctx) {
//		final String name = ctx.graphId().getText();
//		final Integer id = getLoc(ctx);
//		final Exp<?> exp = this.exps.get(ctx.graphExp());
//		if (exp == null) {
//			log.warning("null graph exp " + name);
//			return;
//		}
//		ns.put(name, exp);
//		this.decls.add(new Triple<>(name,id,exp));
//	}
//	
//	@Override 
//	public void exitGraphKind_Exp(AqlParser.GraphKind_ExpContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.graphExp()));
//	}
//	
//	@Override 
//	public void exitGraphKind_Ref(AqlParser.GraphKind_RefContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.graphRef()));
//	}
//	
//	@Override 
//	public void exitGraphRef(AqlParser.GraphRefContext ctx) {
//		this.exps.put(ctx, new GraphExp.GraphExpVar(ctx.getText()));
//	}
//	
//
//	/** see tyExpRaw */
//	@Override 
//	public void exitGraphExp_Literal(AqlParser.GraphExp_LiteralContext ctx) {
//		final GraphLiteralSectionContext 
//		sect = ctx.graphLiteralSection();
//		
//		final List<LocStr>
//		imports = sect.graphRef().stream() 
//				.map(elt -> makeLocStr(elt))
//				.collect(Collectors.toList());
//		
//		final List<LocStr> 
//		nodes = sect.graphNodeId().stream() 
//				.map(elt -> makeLocStr(elt)) 
//				.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, Pair<String, String>>> 
//		edges = sect.graphEdgeSig().stream() 
//				.map(sig -> {
//					final Pair<String,String> 
//						arrow = new Pair<>(
//							sig.graphSourceNodeId().getText(),
//							sig.graphTargetNodeId().getText());
//							
//					return sig.graphEdgeId().stream() 
//							.map(elt -> new Pair<>(makeLocStr(elt), arrow))
//							.collect(Collectors.toList());
//				})
//				.flatMap(x -> x.stream())
//				.collect(Collectors.toList());
//		
//		final GraphExpRaw graph = new GraphExpRaw(nodes, edges, null);	
//		
//		this.exps.put(ctx,graph);
//	};
//	
//	/***************************************************
//	 * TypeSide section
//	 * see AqlTypeside.g4
//	 */
//	@Override public void exitTypesideAssignment(AqlParser.TypesideAssignmentContext ctx) {
//		final String name = ctx.typesideId().getText();
//		final Integer id = getLoc(ctx);
//		final Exp<?> exp = this.exps.get(ctx.typesideExp());
//		if (exp == null) {
//			log.warning("null typeside exp " + name);
//			return;
//		}
//		ns.put(name, exp);
//		this.decls.add(new Triple<>(name,id,exp));
//	}
//	
//	@Override 
//	public void exitTypesideKind_Exp(AqlParser.TypesideKind_ExpContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.typesideExp()));
//	}
//
//	@Override 
//	public void exitTypesideKind_Ref(AqlParser.TypesideKind_RefContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.typesideRef()));
//	}
//
//	@Override 
//	public void exitTypesideRef(AqlParser.TypesideRefContext ctx) {
//		this.exps.put(ctx, new TyExp.TyExpVar(ctx.getText()));
//	}
//	
//	@Override 
//	public void exitTypesideExp_Empty(AqlParser.TypesideExp_EmptyContext ctx) {
//		final Exp<?> exp = new TyExp.TyExpEmpty();
//		this.exps.put(ctx,exp);
//	};
//	
//	@Override 
//	public void exitTypesideExp_Sql(AqlParser.TypesideExp_SqlContext ctx) {
//		final Exp<?> exp = new TyExpSql();
//		this.exps.put(ctx,exp);
//	};
//	
//	@Override 
//	public void exitTypesideExp_Of(AqlParser.TypesideExp_OfContext ctx) {
//		@SuppressWarnings("unchecked")
//		final Exp<?> exp = new TyExp.TyExpSch(
//				(SchExp) 
//				this.exps.get(ctx.schemaKind()));
//		this.exps.put(ctx,exp);
//	};
//	
//	
//	/** see tyExpRaw */
//	@Override 
//	public void exitTypesideExp_Literal(AqlParser.TypesideExp_LiteralContext ctx) {
//		final TypesideLiteralSectionContext 
//		sect = ctx.typesideLiteralSection();
//		
//		@SuppressWarnings({ "rawtypes", "unchecked", "cast" })
//		final List<Pair<Integer, TyExp>>
//		imports = sect.typesideImport().stream() 
//				.map(elt -> 
//					new Pair(
//						getLoc(elt),
//						(TyExp) this.exps.get(elt))) 
//				.collect(Collectors.toList());
//		
//		final List<LocStr> 
//		types = sect.typesideTypeSig().stream() 
//				.map(typ -> makeLocStr(typ.typesideTypeId()))
//				.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, Pair<List<String>, String>>>
//		consts = sect.typesideConstantSig().stream() 
//				.map(elt -> {
//					final Pair<List<String>,String> 
//					type = new Pair<>(
//							new LinkedList<>(),
//							elt.typesideTypeId().getText());
//
//					return elt.typesideConstantId().stream()
//							.map(id -> new Pair<>( 
//									makeLocStr(id),	type))
//							.collect(Collectors.toList());
//				})
//				.flatMap(s -> s.stream())
//				.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, Pair<List<String>, String>>>
//		functions = sect.typesideFunctionSig().stream() 
//				.map(elt -> 
//					new Pair<>(
//						makeLocStr(elt.typesideFnName()),
//						new Pair<>(
//								elt.typesideFnLocal()
//									.stream()
//									.map(loc -> loc.getText())
//									.collect(Collectors.toList()),
//								elt.typesideFnTarget().getText()))) 
//				.collect(Collectors.toList());
//		
//		final List<Pair<Integer, Triple<List<Pair<String, String>>, RawTerm, RawTerm>>>
//		eqns = sect.typesideEquationSig().stream() 
//				.map(elt -> 
//					new Pair<>(
//						getLoc(elt),
//						new Triple<>(
//								elt.typesideLocal()
//									.stream() 
//									.map(lvar -> 
//										new Pair<>(
//												lvar.symbol().getText(), 
//												nullable(lvar.typesideLocalType())))
//									.collect(Collectors.toList()),
//								this.terms.get(elt.typesideEval(0)), 
//								this.terms.get(elt.typesideEval(1)))))
//				.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, String>>
//		java_tys = sect.typesideJavaTypeSig().stream() 
//				.map(elt -> new Pair<>(
//						makeLocStr(elt.typesideTypeId()),
//						unquote(elt.typesideJavaType().getText()))) 
//				.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, String>>
//		java_constant = sect.typesideJavaConstantSig().stream() 
//				.map(elt -> 
//					new Pair<>( 
//						makeLocStr(elt.typesideConstantId()), 
//						unquote(elt.typesideJavaConstantValue().getText()))) 
//				.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, Triple<List<String>, String, String>>>
//		java_fns = sect.typesideJavaFunctionSig().stream() 
//				.map(elt -> 
//					new Pair<>( 
//						makeLocStr(elt.typesideFnName()),
//						new Triple<>( 
//							elt.typesideFnLocal()
//								.stream() 
//								.map(lvar -> lvar.getText())
//								.collect(Collectors.toList()),
//							elt.typesideFnTarget().getText(), 
//							unquote(elt.typesideJavaStatement().getText()))))
//				.collect(Collectors.toList());
//		
//		functions.addAll(consts);
//		
//		final TyExpRaw typeside = 
//			new TyExpRaw(null, types, functions, eqns,
//				java_tys, java_constant, java_fns,
//				Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new));
//				
//		this.exps.put(ctx,typeside);
//	};
//
//	@Override public void exitTypesideImport_Ref(AqlParser.TypesideImport_RefContext ctx) {
//		this.exps.put(ctx, new TyExp.TyExpVar(ctx.typesideRef().getText()));
//	}
//
//	@Override public void exitTypesideImport_Sql(AqlParser.TypesideImport_SqlContext ctx) {
//		this.exps.put(ctx, new TyExpSql());
//	}
//	
//	@Override public void exitTypesideEval_Number(AqlParser.TypesideEval_NumberContext ctx) { 
//		final RawTerm term = new RawTerm(ctx.NUMBER().getText());
//		this.terms.put(ctx,term);
//	}
//	@Override public void exitTypesideEval_Gen(AqlParser.TypesideEval_GenContext ctx) { 
//		final TypesideLiteralContext genNode = ctx.typesideLiteral();
//		final String name = genNode.getText();
//		final RawTerm term = new RawTerm(name);
//		this.terms.put(ctx,term);
//	}
//	@Override public void exitTypesideEval_Infix(AqlParser.TypesideEval_InfixContext ctx) {
//		final List<RawTerm> evals = ctx.typesideEval().stream() 
//				.map(x -> this.terms.get(x)) 
//				.collect(Collectors.toList());
//		
//		final RawTerm term = new RawTerm(ctx.typesideFnName().getText(), evals);
//		this.terms.put(ctx,term);	
//	}
//	@Override public void exitTypesideEval_Paren(AqlParser.TypesideEval_ParenContext ctx) { 
//
//		final List<RawTerm> evals = ctx.typesideEval().stream() 
//				.map(x -> this.terms.get(x)) 
//				.collect(Collectors.toList());
//		
//		final RawTerm term = new RawTerm(ctx.typesideFnName().getText(), evals);
//		this.terms.put(ctx,term);	
//	}
//
//	
//	/***************************************************
//	 * Schema section
//	 * see AqlSchema.g4
//	 */
//	
//	@Override public void exitSchemaAssignment(AqlParser.SchemaAssignmentContext ctx) {
//		final String name = ctx.schemaId().getText();
//		final Integer id = getLoc(ctx);
//		final Exp<?> exp = this.exps.get(ctx.schemaExp());
//		if (exp == null) {
//			log.warning("null schema exp " + name);
//			return;
//		}
//		ns.put(name, exp);
//		this.decls.add(new Triple<>(name,id,exp));
//	}
//	
//	@Override 
//	public void exitSchemaKind_Exp(AqlParser.SchemaKind_ExpContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.schemaExp()));
//	}
//	
//	@Override 
//	public void exitSchemaKind_Ref(AqlParser.SchemaKind_RefContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.schemaRef()));
//	}
//	
//	@Override 
//	public void exitSchemaRef(AqlParser.SchemaRefContext ctx) {
//		final Exp<?> schRef = new SchExp.SchExpVar(ctx.getText());
//		this.exps.put(ctx, schRef);
//	}
//	
//	@Override 
//	public void exitSchemaExp_Identity(AqlParser.SchemaExp_IdentityContext ctx) {
//		this.exps.put(ctx, new SchExp.SchExpVar(ctx.schemaRef().getText()));
//	};
//	
//	@Override 
//	public void exitSchemaExp_Empty(AqlParser.SchemaExp_EmptyContext ctx) {
//		@SuppressWarnings("unchecked")
//		final TyExp ty = (TyExp) this.exps.get(ctx.typesideRef());
//		this.exps.put(ctx, new SchExpEmpty(ty));
//	};
//	
//	@Override 
//	public void exitSchemaExp_OfImportAll(AqlParser.SchemaExp_OfImportAllContext ctx) {
//		// TODO Ryan What is intended here?
//		
//		//@SuppressWarnings("unchecked")
//		//final Exp<?> exp = new SchExp.SchExpInst<Ty,Sym,En,Fk,Att>(
//		//		(InstExp<Ty,Sym,En,Fk,Att,?,?,?,?>) 
//		//		this.exps.get(ctx.IMPORT_ALL()));
//		//this.exps.put(ctx,exp);
//	}
//	
//	@Override 
//	public void exitSchemaExp_OfInstance(AqlParser.SchemaExp_OfInstanceContext ctx) {
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,Sym,En,Fk,Att,?,?,?,?> 
//		instExp = (InstExp<Ty,Sym,En,Fk,Att,?,?,?,?>) 
//			this.exps.get(ctx.instanceKind());
//		
//		this.exps.put(ctx, new SchExp.SchExpInst(instExp));
//	}
//
//	@Override public void exitSchemaExp_GetSchemaColimit(AqlParser.SchemaExp_GetSchemaColimitContext ctx) {
//		@SuppressWarnings("unchecked")
//		final ColimSchExp<String>
//		schColimRef = (ColimSchExp<String>) this.exps.get(ctx.schemaColimitRef());
//		this.exps.put(ctx, new SchExpColim<>(schColimRef));
//	}
//
//
//	private final ParseTreeProperty<Quad<String,String,RawTerm,RawTerm>> 
//	mapped_terms_2 = new ParseTreeProperty<>();
//	
//	@Override public void exitSchemaExp_Literal(AqlParser.SchemaExp_LiteralContext ctx) {
//		final SchemaLiteralSectionContext 
//		sect = ctx.schemaLiteralSection();
//		
//		@SuppressWarnings("unchecked")
//		final TyExp
//		typeside = (TyExp) this.exps.get(ctx.typesideKind());
//		
//		final List<LocStr>
//		imports = sect.typesideImport().stream() 
//				.map(ty -> makeLocStr(ty))
//				.collect(Collectors.toList());
//		
//		final List<LocStr> 
//		entities = sect.schemaEntityId().stream() 
//				.map(elt -> makeLocStr(elt)) 
//				.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, Pair<String, String>>>
//		arrows = sect.schemaForeignSig().stream() 
//				.map(fk -> {
//					final Pair<String,String> 
//					arrow = new Pair<>(
//							fk.schemaEntityId(0).getText(),
//							fk.schemaEntityId(1).getText());
//					
//					return fk.schemaForeignId().stream()
//								.map(fkid -> new Pair<>(makeLocStr(fkid), arrow))
//								.collect(Collectors.toList());
//				})
//				.flatMap(x -> x.stream())
//				.collect(Collectors.toList());
//
//		final List<Pair<Integer, Pair<List<String>, List<String>>>>
//		commutes = sect.schemaPathEqnSig().stream() 
//				.map(eq -> 
//					new Pair<>(
//						getLoc(eq), 
//						new Pair<>( 
//								this.terms.get(eq.schemaPath(0)).forwardPack(),  
//								this.terms.get(eq.schemaPath(1)).forwardPack()))) 
//				.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, Pair<String, String>>>
//		attrs = sect.schemaAttributeSig().stream() 
//				.map(att -> {
//					final Pair<String,String> 
//					arrow = new Pair<>(
//							att.schemaEntityId().getText(),
//							att.typesideTypeId().getText());
//					
//					return att.schemaAttributeId().stream()
//								.map(attid -> new Pair<>(makeLocStr(attid), arrow))
//								.collect(Collectors.toList());
//				})
//				.flatMap(x -> x.stream())
//				.collect(Collectors.toList());
//		
//		final List<Pair<Integer, Quad<String, String, RawTerm, RawTerm>>>
//		observes = sect.schemaObservationEquationSig().stream() 
//				.map(obs -> 
//					new Pair<>(getLoc(obs), 
//								this.mapped_terms_2.get(obs)))
//				.collect(Collectors.toList());
//		
//		final SchExpRaw 
//		schema = new SchExpRaw(typeside, null, 
//					entities, arrows, commutes, attrs, observes,
//					Optional.ofNullable(sect)
//						.map(s -> this.aopts.get(s.allOptions()))
//						.orElseGet(LinkedList::new));
//				
//		this.exps.put(ctx,schema);
//	}
//	
//	private Pair<String,String> loadSchemaGen(AqlParser.SchemaGenContext ctx) {
//		final String name = ctx.symbol().getText();
//		final SchemaGenTypeContext type = ctx.schemaGenType();
//		return new Pair<>(name, (type == null) ? null : type.getText());
//	}
//	
//	@Override public void exitSchemaObserve_Forall(AqlParser.SchemaObserve_ForallContext ctx) {
//		final SchemaEquationSigContext 
//		ctx_sig = ctx.schemaEquationSig();
//		
//		final Pair<String,String> gen = loadSchemaGen(ctx_sig.schemaGen(0));
//		if (ctx_sig.schemaGen().size() > 1) {
//			log.warning("excess generators");
//		}
//		final Quad<String, String, RawTerm, RawTerm>
//		obs = new Quad<>(
//				gen.first, gen.second,
//				this.terms.get(ctx_sig.evalSchemaFn(0)),
//				this.terms.get(ctx_sig.evalSchemaFn(1)));
//		this.mapped_terms_2.put(ctx,obs);
//	}
//	
//	@Override public void exitSchemaObserve_Equation(AqlParser.SchemaObserve_EquationContext ctx) { 
//		final String lvar = "_x";
//		
//		final SchemaPathContext 
//		pathLeft = ctx.schemaPath(0),
//		pathRight = ctx.schemaPath(1);
//		
//		final Quad<String, String, RawTerm, RawTerm>
//		obs = new Quad<>(
//				lvar, null,
//				this.terms.get(pathLeft).clone().append(lvar),
//				this.terms.get(pathRight).clone().append(lvar));
//		this.mapped_terms_2.put(ctx,obs);
//	}
//
//	@Override public void exitEvalSchemaFn_Literal(AqlParser.EvalSchemaFn_LiteralContext ctx) {
//		final RawTerm term = new RawTerm(ctx.schemaLiteralValue().getText());
//		this.terms.put(ctx,term);
//	}
//	
//	@Override public void exitEvalSchemaFn_Gen(AqlParser.EvalSchemaFn_GenContext ctx) {
//		final Pair<String,String> gen = loadSchemaGen(ctx.schemaGen());
//		final RawTerm term = (gen.second == null) 
//			? new RawTerm(gen.first)
//			: new RawTerm(gen.first, gen.second);
//		this.terms.put(ctx,term);
//	}
//	
//	@Override public void exitEvalSchemaFn_Paren(AqlParser.EvalSchemaFn_ParenContext ctx) {
//		final List<RawTerm> eval = new LinkedList<>();
//		ctx.evalSchemaFn().stream()
//			.map(t -> eval.add(this.terms.get(t)))
//			.collect(Collectors.toList());
//		
//		final RawTerm term = new RawTerm(ctx.schemaFn().getText(), eval);
//		this.terms.put(ctx,term);
//	}
//	
//	@Override public void exitEvalSchemaFn_Dotted(AqlParser.EvalSchemaFn_DottedContext ctx) {
//		final List<RawTerm> eval = new LinkedList<>();
//		eval.add(this.terms.get(ctx.evalSchemaFn()));
//		
//		final RawTerm term = new RawTerm(ctx.schemaFn().getText(), eval);
//		this.terms.put(ctx,term);
//	}
//	
//	@Override public void exitSchemaPath_ArrowId(AqlParser.SchemaPath_ArrowIdContext ctx) {
//		final RawTerm term = new RawTerm(ctx.schemaArrowId().getText());
//		this.terms.put(ctx,term);
//	}
//	
//	@Override public void exitSchemaPath_Dotted(AqlParser.SchemaPath_DottedContext ctx) {
//		final List<RawTerm> args = new LinkedList<>();
//		args.add(this.terms.get(ctx.schemaPath()));
//		
//		final RawTerm term = new RawTerm(ctx.schemaArrowId().getText(), args);
//		this.terms.put(ctx,term);
//	}
//	
//	@Override public void exitSchemaPath_Paren(AqlParser.SchemaPath_ParenContext ctx) {
//		final List<RawTerm> args = new LinkedList<>();
//		args.add(this.terms.get(ctx.schemaPath()));
//		
//		final RawTerm term = new RawTerm(ctx.schemaArrowId().getText(), args);
//		this.terms.put(ctx,term);
//	}
//	
//
//	/***************************************************
//	 * Mapping section
//	 * see AqlMapping.g4
//	 */
//	
//	@Override public void exitMappingAssignment(AqlParser.MappingAssignmentContext ctx) {
//		final String name = ctx.mappingId().getText();
//		final Integer id = getLoc(ctx);
//		final Exp<?> exp = this.exps.get(ctx.mappingExp());
//		if (exp == null) {
//			log.warning("null mapping exp " + name);
//			return;
//		}
//		ns.put(name, exp);
//		this.decls.add(new Triple<>(name,id,exp));
//	}
//	
//	@Override 
//	public void exitMappingKind_Exp(AqlParser.MappingKind_ExpContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.mappingExp()));
//	}
//	
//	@Override 
//	public void exitMappingKind_Ref(AqlParser.MappingKind_RefContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.mappingRef()));
//	}
//	
//	@Override 
//	public void exitMappingRef(AqlParser.MappingRefContext ctx) {
//		// this.exps.put(ctx, new MapExp.MapExpVar<Ty, En, Sym, Fk, Att>(ctx.getText()));
//		this.exps.put(ctx, new MapExp.MapExpVar(ctx.getText()));
//	}
//
//	@Override 
//	public void exitMappingExp_Identity(AqlParser.MappingExp_IdentityContext ctx) {
//		@SuppressWarnings("unchecked")
//		final SchExp
//		schema = (SchExp) this.exps.get(ctx.schemaRef());
//		
//		final MapExpId
//		exp = new MapExpId(schema);
//		
//		this.exps.put(ctx,exp);
//	}
//	
//	public static class MapComposer<lTy, lEn, lSym, lFk, lAtt> {
//		private MapExp comp;
//		
//		public MapExp result() { return this.comp; }
//		
//		public MapComposer() {
//			this.comp = null;
//		}
//		
//		public void compose(final MapExp next) {
//			if (this.comp == null) { this.comp = next; return; }
//			this.comp = new MapExpComp(next, this.comp);
//		}
//		
//		public void combine(final MapComposer<lTy, lEn, lSym, lFk, lAtt> other) {
//			this.comp = new MapExpComp(other.comp, this.comp);
//		}
//	}
//	
//	@Override 
//	public void exitMappingExp_Compose(AqlParser.MappingExp_ComposeContext ctx) {
//		@SuppressWarnings({ "unchecked", "rawtypes" })
//		final MapExp
//		comp = ctx.mappingRef().stream()
//				.map(ref -> (MapExp.MapExpVar) this.exps.get(ref))
//				.collect(() -> new MapComposer(),
//						(acc, nxt) -> acc.compose(nxt),
//						(lhs, rhs) -> lhs.combine(rhs))
//				.result();
//		
//		this.exps.put(ctx,comp);
//	}
//	
//	@Override
//	public void exitMappingExp_Get(AqlParser.MappingExp_GetContext ctx) {
//		final RuleContext scRef = ctx.schemaColimitRef();
//		final RuleContext schRef = ctx.schemaRef();
//		
//		final String node = schRef.getText();
//		
//		@SuppressWarnings("unchecked")
//		final ColimSchExp<String> colimExp = (ColimSchExp<String>) this.exps.get(scRef);
//		
//		final MapExpColim<String> mapExp = new MapExpColim<>(node, colimExp);
//		this.exps.put(ctx,mapExp);
//	}
//	
//	private final ParseTreeProperty<
//	Pair<LocStr, // old entity name/loc
//         Triple<String, // new entity name
//                List<Pair<LocStr, // old fk name/loc
//                          List<String>>>, // new fk path (names)
//                List<Pair<LocStr, // old attr name/loc
//                          Triple<String, // universal variable
//                                 String, // entity for universal variable
//                                 RawTerm>>>>>> // new path to attribute
//		mapping = new ParseTreeProperty<>();
//	
//	@Override public void exitMappingExp_Literal(AqlParser.MappingExp_LiteralContext ctx) {
//		@SuppressWarnings("unchecked")
//		final SchExp
//		schemaSrc = (SchExp) this.exps.get(ctx.schemaRef(0)),
//		schemaTgt = (SchExp) this.exps.get(ctx.schemaRef(1));
//		
//		final MappingLiteralSectionContext 
//		sect = ctx.mappingLiteralSection();	
//		
//		final List<LocStr>
//		imports = sect.mappingRef().stream() 
//				.map(ref -> makeLocStr(ref))
//				.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, 
//		                Triple<String, 
//		                       List<Pair<LocStr, List<String>>>, 
//		                       List<Pair<LocStr, Triple<String, String, RawTerm>>>>>>  
//		entities = sect.mappingLiteralSubsection().stream() 
//				.map(elt -> this.mapping.get(elt)) 
//				.collect(Collectors.toList());
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//				
//		final MapExpRaw 
//		mapping = new MapExpRaw(schemaSrc, schemaTgt, null, entities, options);
//				
//		this.exps.put(ctx,mapping);
//	}
//	
//	private final ParseTreeProperty<Triple<String,String,RawTerm>> 
//	mapped_terms_1 = new ParseTreeProperty<>();
//
//	@Override public void exitMappingLiteralSubsection(AqlParser.MappingLiteralSubsectionContext ctx) { 
//		final List<SchemaEntityIdContext>
//		entity = ctx.mappingEntitySig().schemaEntityId();
//		
//		final List<Pair<LocStr, // old fk name/loc
//                        List<String>>> // new fk path (names)
//		arrows = ctx.mappingForeignSig().stream() 
//				.map(fk -> 
//					new Pair<>(
//							makeLocStr(fk.schemaForeignId()),
//							this.terms.get(fk.schemaPath()).forwardPack()))
//				.collect(Collectors.toList());
//
//		final List<Pair<LocStr, // old attr name/loc
//                        Triple<String, // universal variable
//                               String, // entity for universal variable
//                               RawTerm>>> // new path to attribute
//		attrs = ctx.mappingAttributeSig().stream() 
//				.map(att -> 
//					new Pair<>(
//							makeLocStr(att.schemaAttributeId()),
//							this.mapped_terms_1.get(att.mappingAttributeTerm())))
//				.collect(Collectors.toList());
//		
//		this.mapping.put(ctx,
//				new Pair<>(
//					makeLocStr(entity.get(0)), // old entity name/loc
//					new Triple<>(
//							entity.get(1).getText(), // new entity name
//							arrows, // foreign_keys
//							attrs)));  // attributes	
//	}
//	
//	@Override public void exitMappingAttrTerm_Lambda(AqlParser.MappingAttrTerm_LambdaContext ctx) {
//		// universal variable
//		final MappingGenContext ctx_map = ctx.mappingGen(0);
//		final String lvar = ctx_map.symbol().getText();
//		// entity for universal variable
//		final String lvar_type = 
//				(ctx_map.mappingGenType() == null) ? null : ctx_map.mappingGenType().getText();
//		// new path to attribute
//		final RawTerm rt = this.terms.get(ctx.evalMappingFn());
//		
//		this.mapped_terms_1.put(ctx, new Triple<>(lvar, lvar_type, rt));
//	}
//	
//	@Override public void exitMappingAttrTerm_Path(AqlParser.MappingAttrTerm_PathContext ctx) {
//		final String lvar = "_x";
//		
//		final RawTerm rt = this.terms.get(ctx.schemaPath());
//		final Triple<String, // universal variable
//		             String, // entity for universal variable
//		             RawTerm> // new path to attribute
//		term = new Triple<>("_x", null, rt.clone().append(lvar));
//		this.mapped_terms_1.put(ctx, term);
//	}
//
//	@Override public void exitEvalMappingFn_Gen(AqlParser.EvalMappingFn_GenContext ctx) {
//		this.terms.put(ctx, new RawTerm(ctx.mappingGen().symbol().getText()));
//	}
//	
//	@Override public void exitEvalMappingFn_Mapping(AqlParser.EvalMappingFn_MappingContext ctx) {
//		final String mappingFn = ctx.mappingFn().getText();
//		final List<RawTerm> term_list = ctx.evalMappingFn().stream()
//				.map(x -> this.terms.get(x))
//				.collect(Collectors.toList());
//		this.terms.put(ctx, new RawTerm(mappingFn, term_list));
//	}
//	
//	@Override public void exitEvalMappingFn_Typeside(AqlParser.EvalMappingFn_TypesideContext ctx) {
//		final List<RawTerm> 
//		children = ctx.evalMappingFn().stream()
//				.map(x -> this.terms.get(x)).collect(Collectors.toList());
//		
//		final List<String> 
//		actions = ctx.typesideFnName().stream()
//				.map(x -> x.getText()).collect(Collectors.toList());
//		
//		RawTerm acc = children.get(0);
//		for (int ix=0; ix < actions.size(); ix++) {
//			final LinkedList<RawTerm> rl = new LinkedList<>();
//			rl.add(acc);
//			rl.add(children.get(ix+1));
//			acc = new RawTerm(actions.get(ix), rl);
//		}
//		this.terms.put(ctx,acc);
//	}
//	
//	/***************************************************
//	 * Query section
//	 * see AqlQuery.g4
//	 */
//	
//	@Override public void exitQueryAssignment(AqlParser.QueryAssignmentContext ctx) {
//		final String name = ctx.queryId().getText();
//		final Integer id = getLoc(ctx);
//		final Exp<?> exp = this.exps.get(ctx.queryExp());
//		if (exp == null) {
//			log.warning("null query exp " + name);
//			return;
//		}
//		ns.put(name, exp);
//		this.decls.add(new Triple<>(name,id,exp));
//	}
//
//	@Override 
//	public void exitQueryKind_Exp(AqlParser.QueryKind_ExpContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.queryExp()));
//	}
//	
//	@Override 
//	public void exitQueryKind_Ref(AqlParser.QueryKind_RefContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.queryRef()));
//	}
//	
//	@Override 
//	public void exitQueryRef(AqlParser.QueryRefContext ctx) {
//		this.exps.put(ctx, new QueryExp.QueryExpVar(ctx.getText()));
//	}
//	
//	@Override 
//	public void exitQueryExp_Identity(AqlParser.QueryExp_IdentityContext ctx) { 
//		@SuppressWarnings("unchecked")
//		final SchExp
//		schema = (SchExp) 
//			this.exps.get(ctx.schemaRef());
//		
//		final QueryExp.QueryExpId 
//		exp = new QueryExp.QueryExpId(schema);
//		
//		this.exps.put(ctx,exp);
//	}
//	
//	@Override public void exitQueryExp_Get(AqlParser.QueryExp_GetContext ctx) {
//		final SchemaRefContext schemaRef = ctx.schemaRef();
//		final SchemaColimitKindContext schemaColimitKind = ctx.schemaColimitKind();
//		
//		@SuppressWarnings("unchecked")
//		final ColimSchExp<String>
//		colim_exp = (ColimSchExp<String>) this.exps.get(schemaColimitKind);
//		
//		final MapExpColim<String> 
//		getmap = new MapExpColim<>(schemaRef.getText(), colim_exp);
//		
//		this.exps.put(ctx, getmap);
//	}
//	
//	@Override public void exitQueryExp_ToQuery(AqlParser.QueryExp_ToQueryContext ctx) {
//		final QueryDeltaEvalSectionContext sect = ctx.queryDeltaEvalSection();
//		
//		@SuppressWarnings("unchecked")
//		final MapExp
//		mapExp = (MapExp) this.exps.get(ctx.mappingKind());
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//		
//		final QueryExp
//		toQuery = new QueryExpDeltaEval(mapExp, options);
//		
//		this.exps.put(ctx, toQuery);
//	}
//	
//	@Override public void exitQueryExp_ToCoquery(AqlParser.QueryExp_ToCoqueryContext ctx) {
//		final QueryDeltaCoEvalSectionContext sect = ctx.queryDeltaCoEvalSection();
//		
//		@SuppressWarnings("unchecked")
//		final MapExp<Ty,En,Sym,Fk,Att,En,Fk,Att>
//		mapExp = (MapExp<Ty,En,Sym,Fk,Att,En,Fk,Att>) this.exps.get(ctx.mappingKind());
//		
//		final List<Pair<String,String>> options = Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//		
//		final QueryExp toCoQuery = new QueryExpDeltaCoEval(mapExp, options);
//		
//		this.exps.put(ctx, toCoQuery);
//	}
//	
//	@Override public void exitQueryExp_Compose(AqlParser.QueryExp_ComposeContext ctx) {
//		final List<QueryKindContext> queryKind = ctx.queryKind();
//		
//		@SuppressWarnings("unchecked")
//		final List<QueryExp> 
//		queries = queryKind.stream()
//		     .map(x -> (QueryExp) this.exps.get(x))
//		     .collect(Collectors.toList());
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(ctx)
//				.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//				
//		final QueryExpCompose
//		comp = new QueryExpCompose(queries.get(0), queries.get(1), options);
//				
//		this.exps.put(ctx, comp);
//	}
//
//
//	private final ParseTreeProperty<PreBlock> 
//	prexps = new ParseTreeProperty<>();
//	
//	@Override public void exitQueryExp_Simple(AqlParser.QueryExp_SimpleContext ctx) {
//		final SchemaKindContext schemaKind = ctx.schemaKind();
//		final QuerySimpleSectionContext simpleSec = ctx.querySimpleSection();
//		
//		@SuppressWarnings("unchecked")
//		final SchExp
//		src = (SchExp) this.exps.get(schemaKind);
//		
//		final QueryExpRaw.PreBlock 
//		preblock = this.prexps.get(simpleSec.queryClauseExpr());
//		
//		final QueryExpRaw.Block 
//		block = new Block(preblock, new LocStr(getLoc(ctx), "Q"), false); //TODO fred FIXME
//		
//		final QueryExp
//		simple = new QueryExpRawSimple(src, block); 
//		
//		this.exps.put(ctx, simple);
//	}
//
//	@Override public void exitQueryExp_Literal(AqlParser.QueryExp_LiteralContext ctx) {
//		final SchemaKindContext schemaKind = ctx.schemaKind();
//		final SchemaRefContext sref = ctx.schemaRef();
//		final QueryLiteralSectionContext sect = ctx.queryLiteralSection();
//		
//		final List<LocStr>
//		imports = sect.queryRef().stream() 
//				.map(elt -> makeLocStr(elt))
//				.collect(Collectors.toList());
//		
//		@SuppressWarnings("unchecked")
//		final SchExp
//		src = (SchExp) this.exps.get(schemaKind);
//		
//		@SuppressWarnings("unchecked")
//		final SchExp
//		tgt = (SchExp) this.exps.get(sref);
//		
//		final List<Pair<LocStr, PreBlock>>
//		preblocks = sect.queryEntityExpr().stream() 
//			.map(x -> 
//					new Pair<>(makeLocStr(x.schemaEntityId()), 
//							this.prexps.get(x.queryClauseExpr())))
//			.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, String>> params = new LinkedList<>();
//		final List<Pair<LocStr, RawTerm>> consts = new LinkedList<>();
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//			.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//		
//		final QueryExp
//		simple = new QueryExpRaw(
//				params, consts, src, tgt, null,
//				preblocks, options);
//		
//		this.exps.put(ctx, simple);
//	}
//	
//	//--- helpers ------------------
//	
//	@Override public void exitQueryClauseExpr(AqlParser.QueryClauseExprContext ctx) { 
//		final List<Pair<LocStr, String>>
//		fromClause = ctx.queryClauseFrom().stream()
//			.map(x -> new Pair<>(makeLocStr(x.queryGen()), x.schemaEntityId().getText())) 
//			.collect(Collectors.toList());
//		
//		final List<Pair<Integer, Pair<RawTerm, RawTerm>>>
//		whereClause = ctx.queryClauseWhere().stream()
//		    .map(x -> new Pair<>(getLoc(x),
//		    		             new Pair<>(this.terms.get(x.queryPath(0)), 
//		    		            		    this.terms.get(x.queryPath(1)))))
//		    .collect(Collectors.toList());
//		    
//		final List<Pair<LocStr, RawTerm>>
//		atts = ctx.queryPathMapping().stream()
//			.map(x -> new Pair<>(makeLocStr(x.queryGen()), this.terms.get(x.queryPath())))
//			.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, Trans>> 
//		fks = ctx.queryForeignSig().stream()
//			.map(x -> 
//			     new Pair<>(
//			    		    makeLocStr(x.schemaForeignId()), 
//					        new Trans(x.queryPathMapping().stream() 
//					    		       .map(y -> 
//					    		            new Pair<>(makeLocStr(y.queryGen()), 
//					    				               this.terms.get(y.queryPath())))
//					    		       .collect(Collectors.toList()), 
//					    		      new LinkedList<>())))
//			.collect(Collectors.toList());
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(ctx)
//				.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//		
//		final PreBlock 
//		preblock = new PreBlock(fromClause, whereClause, atts, fks, options, false);
//		
//		this.prexps.put(ctx, preblock);
//	}
//	
//	@Override public void exitQueryPath_Literal(AqlParser.QueryPath_LiteralContext ctx) {
//		this.terms.put(ctx, new RawTerm(ctx.queryLiteralValue().getText()));
//	}
//	@Override public void exitQueryPath_TypeConst(AqlParser.QueryPath_TypeConstContext ctx) {
//		this.terms.put(ctx, new RawTerm(ctx.typesideConstantId().getText()));
//	}
//	@Override public void exitQueryPath_GenBare(AqlParser.QueryPath_GenBareContext ctx) {
//		this.terms.put(ctx, new RawTerm(ctx.queryGen().getText()));
//	}
//	@Override public void exitQueryPath_GenArrow(AqlParser.QueryPath_GenArrowContext ctx) {
//		final QueryGenContext gen = ctx.queryGen();
//		final List<SchemaArrowIdContext> arrows = ctx.schemaArrowId();
//		
//		RawTerm acc = new RawTerm(gen.getText());
//		for (int ix=0; ix < arrows.size(); ix++) {
//			LinkedList<RawTerm> rts = new LinkedList<>();
//			rts.add(acc);
//			acc = new RawTerm(arrows.get(ix).getText(), rts);
//		}
//		this.terms.put(ctx, acc);
//	}
//	@Override public void exitQueryPath_GenParam(AqlParser.QueryPath_GenParamContext ctx) {
//		final QueryGenContext gens = ctx.queryGen();
//		final List<QueryPathContext> paths = ctx.queryPath();
//		
//		final List<RawTerm> lrt = paths.stream() 
//				.map(x -> this.terms.get(x))
//				.collect(Collectors.toList());
//		this.terms.put(ctx, new RawTerm(gens.getText(), lrt));
//	}
//	
//	
//	/***************************************************
//	 * Instance section
//	 */
//	@Override public void enterInstanceAssignment(AqlParser.InstanceAssignmentContext ctx) {
//		// final String name = ctx.instanceId().getText();
//		// log.info("enter instance: " + name);
//	}
//	@Override public void exitInstanceAssignment(AqlParser.InstanceAssignmentContext ctx) {
//		final String name = ctx.instanceId().getText();
//		final Integer id = getLoc(ctx);
//		final Exp<?> exp = this.exps.get(ctx.instanceExp());
//		if (exp == null) {
//			log.warning("null instance exp " + name);
//			return;
//		}
//		ns.put(name, exp);
//		this.decls.add(new Triple<>(name,id,exp));
//		//log.info("exit instance: " + name);
//	}
//	
//	@Override 
//	public void exitInstanceKind_Exp(AqlParser.InstanceKind_ExpContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.instanceExp()));
//	}
//	
//	@Override 
//	public void exitInstanceKind_Ref(AqlParser.InstanceKind_RefContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.instanceRef()));
//	}
//	
//	@Override 
//	public void exitInstanceRef(AqlParser.InstanceRefContext ctx) {
//		final InstExp.InstExpVar
//		exp = new InstExp.InstExpVar(ctx.getText());
//		
//		this.exps.put(ctx,exp);
//	}
//	
//	@Override 
//	public void exitInstanceExp_Empty(AqlParser.InstanceExp_EmptyContext ctx) {
//		final SchemaKindContext schemaKind = ctx.schemaKind();
//		
//		@SuppressWarnings("unchecked")
//		final SchExp
//		schema = (SchExp) this.exps.get(schemaKind);
//		
//		final InstExp<Ty,En,Sym, Fk, Att, Void, Void, Void, Void> 
//		exp = new InstExpEmpty<>(schema);
//		
//		this.exps.put(ctx,exp);
//	}
//	
//	@Override public void exitInstanceExp_Src(AqlParser.InstanceExp_SrcContext ctx) {
//		@SuppressWarnings("unchecked")
//		final TransExp<Ty,En,Sym,Fk,Att,Gen,Sk,Gen,Sk,X,Y,X,Y> 
//		transVar = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) 
//		     this.exps.get(ctx.transformKind());
//		
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>
//		inst = new InstExpDom<>(transVar);
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_Dst(AqlParser.InstanceExp_DstContext ctx) {
//		@SuppressWarnings("unchecked")
//		final TransExp<Ty,En,Sym,Fk,Att,Gen,Sk,Gen,Sk,X,Y,X,Y> 
//		transVar = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) 
//		    this.exps.get(ctx.transformKind());
//		
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>
//		inst = new InstExpCod<>(transVar);
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_Distinct(AqlParser.InstanceExp_DistinctContext ctx) {
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y> 
//		instVar = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) 
//		    this.exps.get(ctx.instanceKind());
//		
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>
//		inst = new InstExpDistinct<>(instVar);
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_Eval(AqlParser.InstanceExp_EvalContext ctx) { 
//		final QueryKindContext queryKind = ctx.queryKind();
//		final InstanceKindContext instKind = ctx.instanceKind();
//		final InstanceEvalSectionContext sect = ctx.instanceEvalSection();
//		
//		@SuppressWarnings("unchecked")
//		final QueryExp
//		queryKindExp = (QueryExp) 
//		    this.exps.get(queryKind);
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>
//		instKindExp = (InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>) 
//		    this.exps.get(instKind);
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//		
//		final InstExp<Ty,En,Sym, Fk, Att, ?, Y, ?, Y>
//		inst = new InstExpEval<>(queryKindExp, instKindExp, options);
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_Coeval(AqlParser.InstanceExp_CoevalContext ctx) {
//		final QueryKindContext queryKind = ctx.queryKind();
//		final InstanceKindContext instKind = ctx.instanceKind();
//		final InstanceCoevalSectionContext sect = ctx.instanceCoevalSection();
//		
//		@SuppressWarnings("unchecked")
//		final QueryExp
//		queryKindExp = (QueryExp) this.exps.get(queryKind);
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y> 
//		instKindExp = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instKind);
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//		
//		final InstExp<Ty,En,Sym, Fk, Att, Triple<Var, X, En>, Chc<Triple<Var,X,En>,Y>, Integer, Chc<Chc<Triple<Var,X,En>,Y>, Pair<Integer, Att>>>
//		inst = new InstExpCoEval<>(queryKindExp, instKindExp, options);
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_Delta(AqlParser.InstanceExp_DeltaContext ctx) {
//		final MappingKindContext mappingKind = ctx.mappingKind();
//		final InstanceKindContext instKind = ctx.instanceKind();
//		
//		@SuppressWarnings("unchecked")
//		final MapExp<Ty,En,Sym,Fk,Att,En,Fk,Att>
//		mappKindExp = (MapExp<Ty,En,Sym, Fk, Att, En, Fk, Att>) this.exps.get(mappingKind);
//
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y> 
//		instKindExp = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instKind);
//		
//		final InstExpDelta<Gen,Sk,X,Y>
//		inst = new InstExpDelta<>(mappKindExp, instKindExp);
//		
//		this.exps.put(ctx, inst);
//	 }
//	
//	@Override public void exitInstanceExp_Sigma(AqlParser.InstanceExp_SigmaContext ctx) {
//		final MappingKindContext mappingKind = ctx.mappingKind();
//		final InstanceKindContext instKind = ctx.instanceKind();
//		final InstanceSigmaSectionContext sect = ctx.instanceSigmaSection();
//		
//		@SuppressWarnings("unchecked")
//		final MapExp 
//		mapKindExp = (MapExp) this.exps.get(mappingKind);
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>
//		instKindExp = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instKind); 
//		
//		final Map<String, String> 
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//				.map(s -> toMap(s))
//			.orElseGet(HashMap::new);
//		
//		final InstExpSigma<Ty,En,Sym,Fk,Att,Gen,Sk,En,Fk,Att,X,Y>
//		inst = new InstExpSigma<>(mapKindExp, instKindExp, options);
//	
//		this.exps.put(ctx, inst);
//	 }
//	@Override public void enterInstanceExp_CoSigma(AqlParser.InstanceExp_CoSigmaContext ctx) {
//		// TODO write
//		log.info("entering InstanceExp_CoSigma");
//	}
//	
//	
//	@Override public void exitInstanceExp_CoProd(AqlParser.InstanceExp_CoProdContext ctx) {
//		final List<InstanceRefContext> instRef = ctx.instanceRef();
//		final SchemaKindContext schemaKind = ctx.schemaKind();
//		final InstanceCoProdSectionContext sect = ctx.instanceCoProdSection();
//		
//		final List<String> 
//		instList = instRef.stream() 
//			.map(x -> x.getText())
//			.collect(Collectors.toList());
//		
//		@SuppressWarnings("unchecked")
//		final SchExp
//		schema = (SchExp) this.exps.get(schemaKind);
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//		
//		final InstExpCoProdFull<Ty,En,Sym, Fk, Att, ?,?,?,?>
//		inst = new InstExpCoProdFull<>(instList, schema, options);
//		
//		this.exps.put(ctx, inst);
//	 }
//	
//	
//	
//	@Override public void exitInstanceExp_CoProdUn(AqlParser.InstanceExp_CoProdUnContext ctx) {
//		final List<InstanceKindContext> instKind = ctx.instanceKind();
//		final SchemaKindContext schemaKind = ctx.schemaKind();
//		final InstanceCoProdUnrestrictSectionContext sect = ctx.instanceCoProdUnrestrictSection();
//		
//		@SuppressWarnings("unchecked")
//		final SchExp
//		schema = (SchExp) this.exps.get(schemaKind);
//		
//		final List<String>
//		instList = instKind.stream() 
//			.map(x -> x.getText())
//			.collect(Collectors.toList());
//		
//		final List<Pair<String,String>> 
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final InstExpCoProdFull<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>
//		inst = new InstExpCoProdFull<>(instList, schema, options);
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_CoEqual(AqlParser.InstanceExp_CoEqualContext ctx) {
//		final List<TransformKindContext> transKind = ctx.transformKind();
//		final InstanceCoequalizeSectionContext sect = ctx.instanceCoequalizeSection();
//		
//		@SuppressWarnings("unchecked")
//		final TransExp<Ty,En,Sym,Fk,Att,Gen,Sk,Gen,Sk,X,Y,X,Y>
//		transLhs = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) this.exps.get(transKind.get(0)),
//		transRhs = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) this.exps.get(transKind.get(1));
//		
//		final List<Pair<String,String>> 
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final InstExpCoEq<Ty,En,Sym,Fk,Att,Gen,Sk,Gen,Sk,X,Y,X,Y>
//		inst = new InstExpCoEq<>(transLhs, transRhs, options);
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_CoLimit(AqlParser.InstanceExp_CoLimitContext ctx) {
//		final GraphKindContext graphKind = ctx.graphKind();
//		final SchemaKindContext schemaKind = ctx.schemaKind();
//		final InstanceColimitSectionContext sect = ctx.instanceColimitSection();
//		
//		@SuppressWarnings("unchecked")
//		final GraphExp<String,String>
//		graph = (GraphExp<String, String>) this.exps.get(graphKind);
//		
//		@SuppressWarnings("unchecked")
//		final SchExp
//		schema = (SchExp) this.exps.get(schemaKind);
//		
//		@SuppressWarnings("unchecked")
//		final List<Pair<LocStr, InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>>>
//		nodes = sect.instanceColimitNode().stream()
//			.map(x -> 
//				new Pair<>(makeLocStr(x.instanceRef()), 
//						(InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>)this.exps.get(x.instanceKind())))
//			.collect(Collectors.toList());
//		
//		@SuppressWarnings("unchecked")
//		final List<Pair<LocStr, TransExp<Ty,En,Sym,Fk,Att,Gen,Sk,Gen,Sk,X,Y,X,Y>>> 
//		edges = sect.instanceColimitEdge().stream()
//			.map(x -> 
//					new Pair<>(makeLocStr(x.schemaArrowId()),
//							(TransExp<Ty,En,Sym,Fk,Att,Gen,Sk,Gen,Sk,X,Y,X,Y>)this.exps.get(x.transformKind())))
//			.collect(Collectors.toList());
//				
//		final List<Pair<String,String>> 
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final InstExpColim<String,String,Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>
//		inst = new InstExpColim<>(graph, schema, nodes, edges, options);
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_ImportJdbc(AqlParser.InstanceExp_ImportJdbcContext ctx) {
//		// TODO write
//	}
//	
//	@Override public void exitInstanceExp_QuotientJdbc(AqlParser.InstanceExp_QuotientJdbcContext ctx) {
//		// TODO write
//	}
//	
//	@Override public void exitInstanceExp_QuotientCsv(AqlParser.InstanceExp_QuotientCsvContext ctx) {
//		// TODO write
//	}
//	
//	@Override public void exitInstanceExp_ImportJdbcAll(AqlParser.InstanceExp_ImportJdbcAllContext ctx) {
//		// TODO write
//	}
//	
//	@Override public void exitInstanceExp_ImportCsv(AqlParser.InstanceExp_ImportCsvContext ctx) {
//		// TODO write
//	}
//	
//	
//	
//	@Override public void exitInstanceExp_Chase(AqlParser.InstanceExp_ChaseContext ctx) {
//		final ConstraintKindContext constraintKind = ctx.constraintKind();
//		final InstanceKindContext instKind = ctx.instanceKind();
//		final InstanceChaseSectionContext sect = ctx.instanceChaseSection();
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>
//		instExp = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instKind);
//		
//		@SuppressWarnings("unchecked")
//		final EdsExp
//		edsExp = (EdsExp) this.exps.get(constraintKind);
//		
//		final List<Pair<String,String>> 
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final InstExpChase<Gen,Sk,X,Y>
//		inst = new InstExpChase<>(edsExp, instExp, options);
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_Random(AqlParser.InstanceExp_RandomContext ctx) {
//		final SchemaRefContext schemaRef = ctx.schemaRef();
//		final InstanceRandomSectionContext sect = ctx.instanceRandomSection();
//		
//		final SchExp
//		schRef = (SchExp) this.exps.get(schemaRef);
//		
//		final List<Pair<LocStr, String>> 
//		ens = sect.instanceRandomAction().stream()
//			.map(x -> new Pair<>(
//					makeLocStr(x.schemaEntityId()), 
//					x.INTEGER().getText()))
//			.collect(Collectors.toList());
//
//		final List<Pair<String,String>> 
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final InstExp<Ty,En,Sym,Fk,Att,Integer,Integer,Integer,Integer>
//		inst = new InstExpRandom(schRef, ens, options);
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_Anonymize(AqlParser.InstanceExp_AnonymizeContext ctx) {
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y> 
//		instVar = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(ctx.instanceKind());
//		
//		final InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>
//		inst = new InstExpAnonymize<>(instVar);
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_Frozen(AqlParser.InstanceExp_FrozenContext ctx) {
//		@SuppressWarnings("unchecked")
//		final QueryExp
//		queryExp = (QueryExp) this.exps.get(ctx.queryKind());
//		
//		
//		final InstExpFrozen
//		inst = new InstExpFrozen(queryExp, ctx.schemaKind().getText());
//		
//		this.exps.put(ctx, inst);
//	}
//	
//	@Override public void exitInstanceExp_Pi(AqlParser.InstanceExp_PiContext ctx) {
//		final MappingKindContext mapKind = ctx.mappingKind();
//		final InstanceKindContext instKind = ctx.instanceKind();
//		final InstancePiSectionContext sect = ctx.instancePiSection();
//		
//		@SuppressWarnings("unchecked")
//		final MapExp<Ty,En,Sym,Fk,Att,En,Fk,Att>
//		mapExp = (MapExp<Ty,En,Sym, Fk, Att, En, Fk, Att>) this.exps.get(mapKind);
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>
//		instExp = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instKind); 
//		
//		final Map<String, String> 
//		options = Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//					.map(s -> toMap(s))
//			.orElseGet(HashMap::new);
//		
//		final InstExpPi<Ty,En,Sym,Fk,Att,Gen,Sk,En,Fk,Att,X,Y>
//		inst = new InstExpPi<>(mapExp, instExp, options);
//		
//		this.exps.put(ctx, inst);
//	 }
//	
//	@Override public void exitInstanceExp_Literal(AqlParser.InstanceExp_LiteralContext ctx) {
//		final SchemaKindContext schemaKind = ctx.schemaKind();
//		final InstanceLiteralSectionContext sect = ctx.instanceLiteralSection();
//		
//		final List<LocStr>
//		imports = sect.instanceRef().stream() 
//				.map(elt -> makeLocStr(elt))
//				.collect(Collectors.toList());
//		
//		final SchExp 
//		schema = (SchExp) this.exps.get(schemaKind);
//		
//		final List<Pair<LocStr, String>> 
//		gens = sect.instanceLiteralGen().stream() 
//				.map(x -> {
//					final String schemaId = x.schemaEntityId().getText();
//					return x.instanceGenId().stream()
//							.map(y -> new Pair<>(
//									makeLocStr(y), 
//									schemaId))
//							.collect(Collectors.toList());
//				})
//				.flatMap(x -> x.stream())
//				.collect(Collectors.toList());
//		
//		final List<Pair<Integer, Pair<RawTerm, RawTerm>>> 
//		eqs = sect.instanceEquation().stream() 
//			.map(x -> new Pair<>(
//						getLoc(x.instancePath()), 
//						new Pair<>( 
//								this.terms.get(x.instancePath()),
//								this.terms.get(x.instanceEquationValue()))))
//			.collect(Collectors.toList());
//		
//		final List<Pair<Integer, Pair<RawTerm, RawTerm>>> 
//		meqs = sect.instanceMultiEquation().stream() 
//			.map(eq -> 
//			{
//				final String eqid = eq.instanceEquationId().getText();
//				
//				return eq.instanceMultiBind().stream()
//					.map(bind -> 
//					{
//						final List<RawTerm> rt = new LinkedList<>();
//						rt.add(this.terms.get(bind.instancePath()));
//						
//						return new Pair<>(
//							getLoc(bind.instancePath()), 
//							new Pair<>( 
//									new RawTerm(eqid, rt),
//									this.terms.get(bind.instanceEquationValue())));
//					})
//					.collect(Collectors.toList());
//			})
//			.flatMap(s -> s.stream())
//			.collect(Collectors.toList());
//		
//		eqs.addAll(meqs);
//		
//		final List<Pair<String,String>> 
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final InstExpRaw
//		literal = new InstExpRaw(schema, null, gens, eqs, options);
//		
//		this.exps.put(ctx, literal);
//	}
//
//	//**** helpers ****
//
//	@Override public void exitInstancePath_ArrowId(AqlParser.InstancePath_ArrowIdContext ctx) {
//		final RawTerm term = new RawTerm(ctx.instanceArrowId().getText());
//		this.terms.put(ctx,term);
//	}
//	
//	@Override public void exitInstancePath_Dotted(AqlParser.InstancePath_DottedContext ctx) {
//		final List<RawTerm> args = new LinkedList<>();
//		args.add(this.terms.get(ctx.instancePath()));
//		
//		final RawTerm term = new RawTerm(ctx.instanceArrowId().getText(), args);
//		this.terms.put(ctx,term);
//	}
//
//	@Override public void exitInstancePath_Param(AqlParser.InstancePath_ParamContext ctx) {
//		final List<RawTerm> args = new LinkedList<>();
//		args.add(this.terms.get(ctx.instancePath()));
//		
//		final RawTerm term = new RawTerm(ctx.instanceArrowId().getText(), args);
//		this.terms.put(ctx,term);
//	}
//
//	@Override public void exitInstanceLiteral(AqlParser.InstanceLiteralContext ctx) {
//		final String value = this.str.get(ctx.instanceLiteralValue());
//		final InstanceSymbolContext annotate = ctx.instanceSymbol();
//		
//		@SuppressWarnings("unused")
//		final InstanceSymbolContext symbol = ctx.instanceSymbol();
//		
//		final RawTerm term = (annotate == null) ? new RawTerm(value) : new RawTerm(value, annotate.getText());
//		this.terms.put(ctx,  term);
//	}
//	
//	@Override public void exitInstanceEq_Literal(AqlParser.InstanceEq_LiteralContext ctx) { 
//		this.terms.put(ctx,  this.terms.get(ctx.instanceLiteral()));
//	}	
//	
//	@Override public void exitInstanceEq_Path(AqlParser.InstanceEq_PathContext ctx) { 
//		this.terms.put(ctx,  this.terms.get(ctx.instancePath()));
//	}
//		
//	@Override public void exitInstanceMultiBind(AqlParser.InstanceMultiBindContext ctx) { 
//		this.terms.put(ctx, new RawTerm(ctx.getText()));
//	}
//	
//	@Override public void enterInstanceLiteralValue_Straight(AqlParser.InstanceLiteralValue_StraightContext ctx) {
//		this.str.put(ctx, ctx.getText());
//	}
//	
//	@Override public void enterInstanceLiteralValue_Quoted(AqlParser.InstanceLiteralValue_QuotedContext ctx) {
//		this.str.put(ctx, unquote(ctx.getText()));
//	}
//
//	/***************************************************
//	 * Transform section
//	 */
//	
//	@Override public void exitTransformAssignment(AqlParser.TransformAssignmentContext ctx) {
//		final String name = ctx.transformId().getText();
//		final Integer id = getLoc(ctx);
//		final Exp<?> exp = this.exps.get(ctx.transformExp());
//		if (exp == null) {
//			log.warning("null transform exp " + name);
//			return;
//		}
//		ns.put(name, exp);
//		this.decls.add(new Triple<>(name,id,exp));
//	}
//	
//	@Override 
//	public void exitTransformKind_Exp(AqlParser.TransformKind_ExpContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.transformExp()));
//	}
//	
//	@Override 
//	public void exitTransformKind_Ref(AqlParser.TransformKind_RefContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.transformRef()));
//	}
//	
//	@Override 
//	public void exitTransformRef(AqlParser.TransformRefContext ctx) {
//		this.exps.put(ctx, new TransExp.TransExpVar(ctx.getText()));
//	}
//
//	@Override 
//	public void exitTransformExp_Identity(AqlParser.TransformExp_IdentityContext ctx) { 
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, ?, ?>
//		schema = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, ?, ?>) 
//			this.exps.get(ctx.instanceRef());
//		
//		final TransExp<?,?,?,?,?,?,?,?,?,?,?,?,?>
//		exp = new TransExpId<>(schema);
//		
//		this.exps.put(ctx,exp);
//	}
//	
//
//	@Override 
//	public void exitTransformExp_Compose(AqlParser.TransformExp_ComposeContext ctx) {
//		final List<TransformRefContext> refs = ctx.transformRef(); 
//		
//		@SuppressWarnings("unchecked")
//		TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y> 
//		refexpLhs = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) this.exps.get(refs.get(0)),
//		refexpRhs = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) this.exps.get(refs.get(1));
//		
//		final TransExpCompose<Ty,En,Sym,Fk,Att,Gen,Sk,Gen,Sk,X,Y,X,Y,Gen,Sk,X,Y>
//		comp = new TransExpCompose<>(refexpLhs, refexpRhs);
//			
//		this.exps.put(ctx,comp);
//	}
//	
//	@Override public void exitTransformExp_Distinct(AqlParser.TransformExp_DistinctContext ctx) {
//		final TransformRefContext ref = ctx.transformRef(); 
//		
//		@SuppressWarnings("unchecked")
//		final TransExp<Ty,En,Sym,Fk,Att,Gen,Sk,Gen,Sk,X,Y,X,Y>
//		transRaw = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) this.exps.get(ref);
//		
//		final TransExpDistinct<Ty,En,Sym,Fk,Att,Gen,Sk,Gen,Sk,X,Y,X,Y>
//		trans = new TransExpDistinct<>(transRaw);
//			
//		this.exps.put(ctx,trans);		
//	}
//	
//	@Override public void exitTransformExp_Eval(AqlParser.TransformExp_EvalContext ctx) {
//		final QueryKindContext queryKind = ctx.queryKind();
//		final TransformRefContext transRef = ctx.transformRef();
//		
//		@SuppressWarnings("unchecked")
//		final QueryExp 
//		queryExp = (QueryExp) this.exps.get(queryKind);
//		
//		@SuppressWarnings("unchecked")
//		final TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>
//		transExp = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) 
//			this.exps.get(transRef); 
//		
//		final TransExpEval<Ty,En,Sym,Fk, Att,Gen,Sk,En, Fk,Att,Gen,Sk, ?,?,?,?> 
//		trans = new TransExpEval<>(queryExp, transExp, new LinkedList<>());
//			
//		this.exps.put(ctx,trans);
//	}
//
//	@Override public void exitTransformExp_Coeval(AqlParser.TransformExp_CoevalContext ctx) {
//		final QueryKindContext queryKind = ctx.queryKind();
//		final TransformRefContext transRef = ctx.transformRef();
//		final TransformCoevalSectionContext sect0 = ctx.transformCoevalSection(0);
//		final TransformCoevalSectionContext sect1 = ctx.transformCoevalSection(1);
//		
//		@SuppressWarnings("unchecked")
//		final QueryExp
//		queryExp = (QueryExp) this.exps.get(queryKind);
//		
//		@SuppressWarnings("unchecked")
//		final TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>
//		transExp = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) 
//			this.exps.get(transRef); 
//		
//		final List<Pair<String,String>> 
//		options0 = Optional.ofNullable(sect0)
//				.map(s -> this.aopts.get(s.allOptions()))
//				.orElseGet(LinkedList::new), 
//		options1 = Optional.ofNullable(sect1)
//				.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//			
//		final TransExpCoEval<Ty,En,Sym,Fk, Att,Gen,Sk,En, Fk,Att,Gen,Sk, X,Y,X,Y> 
//		trans = new TransExpCoEval<>(queryExp, transExp, options0, options1);
//			
//		this.exps.put(ctx,trans);
//	}
//	
//	@Override public void exitTransformExp_Sigma(AqlParser.TransformExp_SigmaContext ctx) {
//		final MappingKindContext mappingKind = ctx.mappingKind();
//		final TransformRefContext transRef = ctx.transformRef();
//		final TransformSigmaSectionContext sect0 = ctx.transformSigmaSection(0);
//		final TransformSigmaSectionContext sect1 = ctx.transformSigmaSection(1);
//		
//		@SuppressWarnings("unchecked")
//		final MapExp
//		mapExp = (MapExp) this.exps.get(mappingKind);
//		
//		@SuppressWarnings("unchecked")
//		final TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>
//		transExp = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) 
//			this.exps.get(transRef); 
//		
//		final Map<String, String> 
//		options0 = Optional.ofNullable(sect0)
//				.map(s -> this.aopts.get(s.allOptions()))
//				.map(s -> toMap(s))
//				.orElseGet(HashMap::new),  
//		options1 = Optional.ofNullable(sect1)
//				.map(s -> this.aopts.get(s.allOptions()))
//				.map(s -> toMap(s))
//			.orElseGet(HashMap::new);
//			
//		final TransExpSigma
//		trans = new TransExpSigma(mapExp, transExp, options0, options1); 
//			
//		this.exps.put(ctx,trans);
//	}
//
//	@Override public void exitTransformExp_Delta(AqlParser.TransformExp_DeltaContext ctx) {
//		final MappingKindContext mappingKind = ctx.mappingKind();
//		final TransformRefContext transRef = ctx.transformRef();
//		
//		@SuppressWarnings("unchecked")
//		final MapExp
//		mapExp = (MapExp) this.exps.get(mappingKind);
//		
//		@SuppressWarnings("unchecked")
//		final TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>
//		transExp = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) 
//			this.exps.get(transRef); 
//		
//		final TransExpDelta<Ty,En,Sym, Fk, Att, Gen, Sk, En, Fk, Att, Gen, Sk, X, Y, X, Y> 
//		trans = new TransExpDelta<>(mapExp, transExp);
//			
//		this.exps.put(ctx,trans);
//	}
//		
//	@Override public void exitTransformExp_Unit(AqlParser.TransformExp_UnitContext ctx) {
//		final MappingKindContext mappingKind = ctx.mappingKind();
//		final InstanceRefContext instRef = ctx.instanceRef();
//		final TransformUnitSectionContext sect = ctx.transformUnitSection();
//		
//		@SuppressWarnings("unchecked")
//		final MapExp
//		mapExp = (MapExp) this.exps.get(mappingKind);
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>
//		instExp = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instRef);
//		
//		final Map<String, String> 
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//				.map(s -> toMap(s))
//			.orElseGet(HashMap::new);
//			
//		final TransExpSigmaDeltaUnit<?,?,?,?, ?,?,?,?, ?,?,?,?> 
//		trans = new TransExpSigmaDeltaUnit<>(mapExp, instExp, options);
//			
//		this.exps.put(ctx,trans);
//	}
//	
//	@Override public void exitTransformExp_Counit(AqlParser.TransformExp_CounitContext ctx) {
//		final MappingKindContext mappingKind = ctx.mappingKind();
//		final InstanceRefContext instRef = ctx.instanceRef();
//		final TransformUnitSectionContext sect = ctx.transformUnitSection();
//		
//		@SuppressWarnings("unchecked")
//		final MapExp 
//		mapExp = (MapExp) this.exps.get(mappingKind);
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>
//		instExp = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instRef);
//		
//		final Map<String, String> 
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//				.map(s -> toMap(s))
//			.orElseGet(HashMap::new);
//			
//		final TransExpSigmaDeltaCounit<?,?,?,?, ?,?,?,?, ?,?,?,?> 
//		trans = new TransExpSigmaDeltaCounit<>(mapExp, instExp, options);
//			
//		this.exps.put(ctx,trans);
//	}
//	
//	@Override public void exitTransformExp_UnitQuery(AqlParser.TransformExp_UnitQueryContext ctx) {
//		final QueryKindContext queryKind = ctx.queryKind();
//		final InstanceRefContext instRef = ctx.instanceRef();
//		final TransformUnitQuerySectionContext sect = ctx.transformUnitQuerySection();
//		
//		@SuppressWarnings("unchecked")
//		final QueryExp
//		queryExp = (QueryExp) this.exps.get(queryKind);
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>
//		instExp = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instRef);
//		 
//		final Map<String, String> 
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//				.map(s -> toMap(s))
//			.orElseGet(HashMap::new);
//			
//		final TransExpCoEvalEvalUnit<Ty,En,Sym,Fk,Att,Gen,Sk,En,Fk,Att,X,Y>
//		trans = new TransExpCoEvalEvalUnit<>(queryExp, instExp, options);
//			
//		this.exps.put(ctx,trans);
//	}
//	
//	@Override public void exitTransformExp_CounitQuery(AqlParser.TransformExp_CounitQueryContext ctx) {
//		final QueryKindContext queryKind = ctx.queryKind();
//		final InstanceRefContext instRef = ctx.instanceRef();
//		final TransformCounitQuerySectionContext sect = ctx.transformCounitQuerySection();
//		
//		@SuppressWarnings("unchecked")
//		final QueryExp
//		queryExp = (QueryExp) this.exps.get(queryKind);
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y> 
//		instExp = (InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>) this.exps.get(instRef);
//		
//		final Map<String, String> 
//		options = Optional.ofNullable(sect)
//						.map(s -> this.aopts.get(s.allOptions()))
//						.map(s -> toMap(s))
//			.orElseGet(HashMap::new);
//		
//		final TransExpCoEvalEvalCoUnit<Ty,En,Sym,Fk,Att,Gen,Sk,En,Fk,Att,X,Y>
//		trans = new TransExpCoEvalEvalCoUnit<>(queryExp, instExp, options);
//			
//		this.exps.put(ctx,trans);
//	}
//	
//	@Override public void exitTransformExp_ImportJdbc(AqlParser.TransformExp_ImportJdbcContext ctx) {
//		final TransformJdbcClassContext jdbcClass = ctx.transformJdbcClass();
//		final TransformJdbcUriContext jdbcUri = ctx.transformJdbcUri();
//		final InstanceRefContext instSrcRef = ctx.instanceRef(0);
//		final InstanceRefContext instTgtRef = ctx.instanceRef(1);
//		final TransformImportJdbcSectionContext sect = ctx.transformImportJdbcSection();
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>
//		instSrc = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instSrcRef),
//		instTgt = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instTgtRef);
//		
//		final List<Pair<LocStr, String>>
//		sqls = sect.transformSqlEntityExpr().stream()
//					.map(x -> new Pair<>(
//						makeLocStr(x.schemaEntityId()),
//						x.transformSqlExpr().getText()))
//            .collect(Collectors.toList());  
//		 
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//			
//		final TransExpJdbc<X,Y,X,Y> 
//		trans = new TransExpJdbc<>(jdbcClass.getText(), jdbcUri.getText(), instSrc, instTgt, sqls, options);
//
//		this.exps.put(ctx,trans);
//	}
//	
//	@Override public void exitTransformExp_ImportCsv(AqlParser.TransformExp_ImportCsvContext ctx) {
//		@SuppressWarnings("unused")
//		final TransformFileContext csvFile = ctx.transformFile();
//		final InstanceRefContext instSrcRef = ctx.instanceRef(0);
//		final InstanceRefContext instTgtRef = ctx.instanceRef(1);
//		final TransformImportCsvSectionContext sect = ctx.transformImportCsvSection();
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>
//		instSrcExp = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instSrcRef),
//		instTgtExp = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) this.exps.get(instTgtRef);
//		
//		final List<Pair<LocStr, String>>
//		transFiles = sect.transformFileExpr().stream()
//						.map(x -> new Pair<>(
//							makeLocStr(x.schemaEntityId()),
//							x.transformFile().getText()))
//	        .collect(Collectors.toList());
//        
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//						.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//			
//		final TransExpCsv<X,Y,X,Y> 
//		trans = new TransExpCsv<>(instSrcExp, instTgtExp, transFiles, options);
//			
//		this.exps.put(ctx,trans);
//	}
//	
//	@Override public void exitTransformExp_Literal(AqlParser.TransformExp_LiteralContext ctx) {
//		final InstanceKindContext instSrcKind = ctx.instanceKind();
//		final InstanceRefContext instTgtRef = ctx.instanceRef();
//		final TransformLiteralSectionContext sect = ctx.transformLiteralSection();
//		
//		final InstExp<?,?,?,?,?,?,?,?,?> 
//		instSrcExp = (InstExp<?,?,?,?,?,?,?,?,?>) this.exps.get(instSrcKind), 
//		instTgtExp = (InstExp<?,?,?,?,?,?,?,?,?>) this.exps.get(instTgtRef);
//		
//		final List<LocStr>
//		imports = sect.schemaRef().stream() 
//				.map(elt -> makeLocStr(elt))
//				.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, RawTerm>> 
//		gens = sect.transformGen().stream() 
//				.map(x -> new Pair<>(
//									makeLocStr(x.symbol()), 
//									this.terms.get(x.schemaPath())))
//				.collect(Collectors.toList());
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//				.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//			
//		final TransExpRaw 
//		trans = new TransExpRaw(instSrcExp, instTgtExp, null, gens, options); 
//			
//		this.exps.put(ctx,trans);
//	}
//
//
//	/***************************************************
//	 * Constraint section
//	 */
//	
//	@Override public void exitConstraintAssignment(AqlParser.ConstraintAssignmentContext ctx) {
//		final String name = ctx.constraintId().getText();
//		final Integer id = getLoc(ctx);
//		final Exp<?> exp = this.exps.get(ctx.constraintExp());
//		if (exp == null) {
//			log.warning("null constraint exp " + name);
//			return;
//		}
//		ns.put(name, exp);
//		this.decls.add(new Triple<>(name,id,exp));
//	}
//	
//	@Override 
//	public void exitConstraintKind_Exp(AqlParser.ConstraintKind_ExpContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.constraintExp()));
//	}
//	
//	@Override 
//	public void exitConstraintKind_Ref(AqlParser.ConstraintKind_RefContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.constraintRef()));
//	}
//	
//	@Override 
//	public void exitConstraintRef(AqlParser.ConstraintRefContext ctx) {
//		this.exps.put(ctx, new EdsExp.EdsExpVar(ctx.getText()));
//	}
//	
//	@Override public void exitConstraintExp_Literal(AqlParser.ConstraintExp_LiteralContext ctx) { 
//		final SchemaRefContext schemaRef = ctx.schemaRef();
//		final ConstraintLiteralSectionContext sect = ctx.constraintLiteralSection();
//		
//		@SuppressWarnings("unchecked")
//		final SchExp 
//		schemaExp = (SchExp) this.exps.get(schemaRef);
//				
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//			.map(s -> this.aopts.get(s.allOptions()))
//			.orElseGet(LinkedList::new);
//		
//		if (sect == null) {
//			final List<String> imports = new LinkedList<>();
//			final List<EdExpRaw> eds = new LinkedList<>();
//
//			final EdsExpRaw 
//			rule = new EdsExpRaw(schemaExp, null, eds, options);
//				
//			this.exps.put(ctx,rule);
//			return;
//		}
//			
//		final List<LocStr>
//		imports = sect.constraintRef().stream() 
//				.map(elt -> makeLocStr(elt))
//				.collect(Collectors.toList());
//		
//		final List<Pair<Integer, EdExpRaw>>
//		constraints = sect.constraintExpr().stream() 
//				.map(x -> 
//					new Pair<>(
//						getLoc(x), 
//						(EdExpRaw) this.exps.get(x)))
//				.collect(Collectors.toList());
//			
//		final EdsExpRaw 
//		rule = new EdsExpRaw(schemaExp, null, constraints, options);
//			
//		this.exps.put(ctx,rule);
//	}
//	
//	//**** helpers ****
//	
//	@Override public void exitConstraintExpr(AqlParser.ConstraintExprContext ctx) {
//		final List<ConstraintUniversalContext> constUniv = ctx.constraintUniversal();
//		final List<ConstraintUniversalEquationContext> constUnivEq = ctx.constraintUniversalEquation();
//		final List<ConstraintExistentialContext> constExist = ctx.constraintExistential();
//		final TerminalNode constUnique = ctx.UNIQUE() ;
//		final List<ConstraintExistentialEquationContext> constExistEq = ctx.constraintExistentialEquation();
//		
//		final List<Pair<LocStr, String>> 
//		us = constUniv.stream()
//			.map(x -> {
//				final String schemaEntityId = x.schemaEntityId().getText();
//				return x.constraintGen().stream()
//					.map(y ->
//						new Pair<>(makeLocStr(y), schemaEntityId))
//					.collect(Collectors.toList());
//			})
//			.flatMap(s -> s.stream())
//			.collect(Collectors.toList());
//		
//		final List<Pair<LocStr, String>> 
//		es = constExist.stream()
//			.map(x -> {
//				final String schemaEntityId = x.schemaEntityId().getText();
//				return x.constraintGen().stream()
//					.map(y ->
//						new Pair<>(makeLocStr(y), schemaEntityId))
//					.collect(Collectors.toList());
//			})
//			.flatMap(s -> s.stream())
//			.collect(Collectors.toList());
//		
//		final List<Pair<Integer, Pair<RawTerm, RawTerm>>>
//		uEqs = constUnivEq.stream()
//			.map(x -> 
//				new Pair<>( 
//						getLoc(x),
//						new Pair<>(
//							this.terms.get(x.constraintPath(0)),
//							this.terms.get(x.constraintPath(1)))))
//			.collect(Collectors.toList());
//		
//		final List<Pair<Integer, Pair<RawTerm, RawTerm>>>
//		eEqs = constExistEq.stream()
//			.map(x -> 
//				new Pair<>( 
//						getLoc(x),
//						new Pair<>(
//							this.terms.get(x.constraintPath(0)),
//							this.terms.get(x.constraintPath(1)))))
//			.collect(Collectors.toList());
//		
//		final boolean isUnique = constUnique != null;
//		
//		final EdExpRaw rule = new EdExpRaw(us, uEqs, es, eEqs, isUnique);
//		
//		this.exps.put(ctx, rule);
//	}
//
//	@Override public void exitConstraintPath_ArrowId(AqlParser.ConstraintPath_ArrowIdContext ctx) {
//		final RawTerm term = new RawTerm(ctx.schemaArrowId().getText());
//		this.terms.put(ctx,term);
//	}
//	
//	@Override public void exitConstraintPath_Dotted(AqlParser.ConstraintPath_DottedContext ctx) {
//		final List<RawTerm> args = new LinkedList<>();
//		args.add(this.terms.get(ctx.constraintPath()));
//		
//		final RawTerm term = new RawTerm(ctx.schemaArrowId().getText(), args);
//		this.terms.put(ctx,term);
//	}
//
//	@Override public void exitConstraintPath_Param(AqlParser.ConstraintPath_ParamContext ctx) {
//		final List<RawTerm> args = new LinkedList<>();
//		args.add(this.terms.get(ctx.constraintPath()));
//		
//		final RawTerm term = new RawTerm(ctx.schemaArrowId().getText(), args);
//		this.terms.put(ctx,term);
//	}
//			
//
//	/***************************************************
//	 * Command section
//	 */
//	
//	@Override public void exitCommandAssignment(AqlParser.CommandAssignmentContext ctx) {
//		final String name = ctx.commandId().getText();
//		final Integer id = getLoc(ctx);
//		final Exp<?> exp = this.exps.get(ctx.commandExp());
//		if (exp == null) {
//			log.warning("null command exp " + name);
//			return;
//		}
//		ns.put(name, exp);
//		this.decls.add(new Triple<>(name,id,exp));
//	}
//	
//	@Override 
//	public void exitCommandKind_Exp(AqlParser.CommandKind_ExpContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.commandExp()));
//	}
//
//	@Override 
//	public void exitCommandKind_Ref(AqlParser.CommandKind_RefContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.commandRef()));
//	}
//	
//	@Override 
//	public void exitCommandRef(AqlParser.CommandRefContext ctx) {
//		this.exps.put(ctx, new PragmaExp.PragmaExpVar(ctx.getText()));
//	}
//	
//	@Override public void exitCommandExp_CmdLine(AqlParser.CommandExp_CmdLineContext ctx) {
//		final CommandCmdLineSectionContext sect = ctx.commandCmdLineSection();
//		
//		if (sect == null) {
//			this.exps.put(ctx, new PragmaExp.PragmaExpProc(new LinkedList<>(), new LinkedList<>()));
//			return;
//		}
//		final List<String> 
//		cmds = sect.quotedString().stream() 
//			.map(x -> this.str.get(x))
//			.collect(Collectors.toList());
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final PragmaExpProc
//		cmdExp = new PragmaExp.PragmaExpProc(cmds, options);
//		
//		this.exps.put(ctx,cmdExp);
//	}
//	
//	@Override public void exitCommandExp_ExecJs(AqlParser.CommandExp_ExecJsContext ctx) {
//		final CommandExecJsSectionContext sect = ctx.commandExecJsSection();
//		
//		if (sect == null) {
//			this.exps.put(ctx, new PragmaExp.PragmaExpJs(new LinkedList<>(), new LinkedList<>()));
//			return;
//		}
//		final List<String> 
//		cmds = sect.quotedString().stream() 
//			.map(x -> this.str.get(x))
//			.collect(Collectors.toList());
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final PragmaExp.PragmaExpJs
//		cmdExp = new PragmaExp.PragmaExpJs(cmds, options);
//		
//		this.exps.put(ctx,cmdExp);
//	}
//	
//	@Override public void exitCommandExp_ExecJdbc(AqlParser.CommandExp_ExecJdbcContext ctx) {
//		final CommandJdbcClassContext cmdJdbcClass = ctx.commandJdbcClass();
//		final CommandJdbcUriContext cmdJdbcUri = ctx.commandJdbcUri();
//		final CommandExecJdbcSectionContext sect = ctx.commandExecJdbcSection();
//		
//		final String clazz = cmdJdbcClass.getText(); 
//		final String jdbcString = cmdJdbcUri.getText();
//		
//		if (sect == null) {
//			this.exps.put(ctx, new PragmaExp.PragmaExpSql(
//								clazz, jdbcString, new LinkedList<String>(), 
//								new LinkedList<Pair<String,String>>()));
//			return;
//		}
//		
//		final List<String> 
//		sqls = sect.quotedMultiString().stream() 
//			.map(x -> this.str.get(x))
//			.collect(Collectors.toList());
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final PragmaExpSql
//		cmdExp = new PragmaExp.PragmaExpSql(clazz, jdbcString, sqls, options);
//
//		this.exps.put(ctx,cmdExp);
//	}
//	
//	@Override public void exitCommandExp_Check(AqlParser.CommandExp_CheckContext ctx) {
//		final ConstraintRefContext ruleRef = ctx.constraintRef();
//		final InstanceRefContext instRef = ctx.instanceRef();
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y> 
//		instExp = (InstExp<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>) 
//			this.exps.get(instRef); 
//		
//		@SuppressWarnings("unchecked")
//		final EdsExp 
//		ruleExp = (EdsExp) 
//			this.exps.get(ruleRef);
//		
//		final PragmaExpCheck<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>
//		cmd = new PragmaExp.PragmaExpCheck<>(instExp, ruleExp);
//		
//		this.exps.put(ctx,cmd);
//	}
//	
//	@Override public void exitCommandExp_AssertConsistent(AqlParser.CommandExp_AssertConsistentContext ctx) {
//		final InstanceRefContext instRef = ctx.instanceRef();
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y> 
//		instExp = (InstExp<Ty,En,Sym,Fk,Att,Gen,Sk,X,Y>) 
//			this.exps.get(instRef); 
//		
//		final PragmaExpConsistent<Ty,En,Sym, Fk, Att, Gen, Sk, X, Y>
//		cmd = new PragmaExpConsistent<>(instExp);
//		
//		this.exps.put(ctx,cmd);
//	}
//	
//	@Override public void exitCommandExp_ExportCsvInstance(AqlParser.CommandExp_ExportCsvInstanceContext ctx) {
//		final CommandFileContext cmdfileNode = ctx.commandFile();
//		final InstanceRefContext instRef = ctx.instanceRef();
//		final CommandExportCsvSectionContext sect = ctx.commandExportCsvSection();
//		
//		@SuppressWarnings("unchecked")
//		final InstExp<Ty,En,Sym, Att, Fk, Gen, Sk, X, Y> 
//		instExp = (InstExp<Ty,En,Sym, Att, Fk, Gen, Sk, X, Y>) 
//			this.exps.get(instRef); 
//		
//		final String cmdFile = cmdfileNode.getText();
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final PragmaExp.PragmaExpToCsvInst<Ty,En,Sym, Att, Fk, Gen, Sk, X, Y>
//		cmd = new PragmaExp.PragmaExpToCsvInst<>(instExp, cmdFile, options);
//			
//		this.exps.put(ctx,cmd); }
//	
//	@Override public void exitCommandExp_ExportCsvTransform(AqlParser.CommandExp_ExportCsvTransformContext ctx) {
//		final CommandFileContext cmdFileNode = ctx.commandFile();
//		final TransformRefContext transRef = ctx.transformRef();
//		final CommandExportCsvSectionContext sect = ctx.commandExportCsvSection();
//		
//		@SuppressWarnings("unchecked")
//		final TransExp<Ty,En,Sym, Att, Fk, Gen, Sk, X, Y, Gen, Sk, X, Y>
//		transExp = (TransExp<Ty,En,Sym, Att, Fk, Gen, Sk, X, Y, Gen, Sk, X, Y>) 
//			this.exps.get(transRef); 
//		
//		final String cmdFile = cmdFileNode.getText();
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final PragmaExp.PragmaExpToCsvTrans<Ty,En,Sym, Att, Fk, Gen, Sk, X, Y, Gen, Sk, X, Y>
//		cmd = new PragmaExp.PragmaExpToCsvTrans<>(transExp, cmdFile, options, options);
//			
//		this.exps.put(ctx,cmd); 
//	}
//	
//	@Override public void exitCommandExp_ExportJdbcInstance(AqlParser.CommandExp_ExportJdbcInstanceContext ctx) {
//		final TransformRefContext transRef = ctx.transformRef();
//		final CommandJdbcClassContext cmdClassNode = ctx.commandJdbcClass();
//		final CommandJdbcUriContext cmdUriNode = ctx.commandJdbcUri();
//		final CommandPrefixDstContext cmdPreNode = ctx.commandPrefixDst();
//		final CommandExportJdbcSectionContext sect = ctx.commandExportJdbcSection();
//		
//		@SuppressWarnings("unchecked")
//		final TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>
//		transExp = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) 
//			this.exps.get(transRef); 
//
//		final String cmdClass = (cmdClassNode == null) ? null : cmdClassNode.getText();
//		final String cmdUri = (cmdUriNode == null) ? null : cmdUriNode.getText();
//		final String cmdPre = (cmdPreNode == null) ? null : cmdPreNode.getText();
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final PragmaExp.PragmaExpToJdbcTrans<Gen, Sk, Gen, Sk, X, Y, X, Y>
//		cmd = new PragmaExp.PragmaExpToJdbcTrans<>(transExp, cmdClass, cmdUri, cmdPre, 
//				options, options);
//			
//		this.exps.put(ctx,cmd); 
//	}
//	
//	@Override public void exitCommandExp_ExportJdbcQuery(AqlParser.CommandExp_ExportJdbcQueryContext ctx) {
//		final QueryRefContext queryRef = ctx.queryRef();
//		final CommandJdbcClassContext cmdClassNode = ctx.commandJdbcClass();
//		final CommandJdbcUriContext cmdUriNode = ctx.commandJdbcUri();
//		final CommandPrefixSrcContext cmdPreSrcNode = ctx.commandPrefixSrc();
//		final CommandPrefixDstContext cmdPreDstNode = ctx.commandPrefixDst();
//		final CommandExportJdbcSectionContext sect = ctx.commandExportJdbcSection();
//		
//		@SuppressWarnings("unchecked")
//		final QueryExp
//		queryExp = (QueryExp) 
//			this.exps.get(queryRef); 
//
//		final String cmdClass = (cmdClassNode == null) ? null : cmdClassNode.getText();
//		final String cmdUri = (cmdUriNode == null) ? null : cmdUriNode.getText();
//		final String cmdPreSrc = (cmdPreSrcNode == null) ? null : cmdPreSrcNode.getText();
//		final String cmdPreDst = (cmdPreDstNode == null) ? null : cmdPreDstNode.getText();
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(sect)
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final PragmaExp.PragmaExpToJdbcQuery<Ty,En,Sym, Fk, Att, En, Fk, Att>
//		cmd = new PragmaExp.PragmaExpToJdbcQuery<>(
//				queryExp, cmdClass, cmdUri, 
//				cmdPreSrc, cmdPreDst, options);
//		
//		this.exps.put(ctx,cmd); 
//	}
//	
//	@Override public void exitCommandExp_ExportJdbcTransform(AqlParser.CommandExp_ExportJdbcTransformContext ctx) {
//		final TransformRefContext transRef = ctx.transformRef();
//		final CommandJdbcClassContext cmdClassNode = ctx.commandJdbcClass();
//		final CommandJdbcUriContext cmdUriNode = ctx.commandJdbcUri();
//		final CommandPrefixContext cmdPreNode = ctx.commandPrefix();
//		final List<CommandExportJdbcSectionContext> sect = ctx.commandExportJdbcSection();
//		
//		@SuppressWarnings("unchecked")
//		final TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>
//		transExp = (TransExp<Ty,En,Sym, Fk, Att, Gen, Sk, Gen, Sk, X, Y, X, Y>) 
//			this.exps.get(transRef); 
//
//		final String cmdClass = (cmdClassNode == null) ? null : cmdClassNode.getText();
//		final String cmdUri = (cmdUriNode == null) ? null : cmdUriNode.getText();
//		final String cmdPre = (cmdPreNode == null) ? null : cmdPreNode.getText();
//
//		final List<Pair<String,String>>
//		options0 = Optional.ofNullable(sect.get(0))
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//
//		final List<Pair<String,String>>
//		options1 = Optional.ofNullable(sect.get(1))
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//		
//		final PragmaExp.PragmaExpToJdbcTrans<Gen, Sk, Gen, Sk, X, Y, X, Y>
//		cmd = new PragmaExpToJdbcTrans<>(transExp, cmdClass, cmdUri, cmdPre, 
//				options0, options1);
//		
//		this.exps.put(ctx,cmd); 
//        }
//	
//	
//
//	/***************************************************
//	 * SchemaColimit section
//	 */
//	
//	@Override public void exitSchemaColimitAssignment(AqlParser.SchemaColimitAssignmentContext ctx) {
//		final String name = ctx.schemaColimitId().getText();
//		final Integer id = getLoc(ctx);
//		final Exp<?> exp = this.exps.get(ctx.schemaColimitExp());
//		if (exp == null) {
//			log.warning("null schema colimit exp " + name);
//			return;
//		}
//		ns.put(name, exp);
//		this.decls.add(new Triple<>(name,id,exp));
//	}
//	
//	@Override 
//	public void exitSchemaColimitKind_Exp(AqlParser.SchemaColimitKind_ExpContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.schemaColimitExp()));
//	}
//	
//	@Override 
//	public void exitSchemaColimitKind_Ref(AqlParser.SchemaColimitKind_RefContext ctx) {
//		this.exps.put(ctx,this.exps.get(ctx.schemaColimitRef()));
//	}
//	
//	@Override 
//	public void exitSchemaColimitRef(AqlParser.SchemaColimitRefContext ctx) {
//		// TODO complete implementation
//		final ColimSchExpVar<String>
//		exp = new ColimSchExp.ColimSchExpVar<>(ctx.getText());
//		
//		this.exps.put(ctx, exp);
//	}
//	
//	@Override public void exitSchemaColimitExp_Quotient(AqlParser.SchemaColimitExp_QuotientContext ctx) {
//		final List<SchemaRefContext> schRefs = ctx.schemaRef();
//		final TypesideRefContext tyRef = ctx.typesideRef();
//		final SchemaColimitQuotientSectionContext sect = ctx.schemaColimitQuotientSection();
//	
//		@SuppressWarnings("unchecked")
//		final TyExp
//		ty = (TyExp) this.exps.get(tyRef);
//	
//		final List<LocStr> 
//		schs = schRefs.stream() 
//			.map(ref -> makeLocStr(ref))
//			.collect(Collectors.toList());
//	
//		final List<Pair<Integer, Quad<String, String, String, String>>> 
//		eqEn = sect.scQuotientEqu().stream() 
//			.map(eqn -> {
//				final RawTerm lhs = this.terms.get(eqn.scTermPath(0));
//				final RawTerm rhs = this.terms.get(eqn.scTermPath(1));
//				return new Pair<>(
//					getLoc(eqn), 
//					new Quad<>(
//							lhs.byIndex(0), lhs.byIndex(),
//							rhs.byIndex(0), rhs.byIndex())); })
//			.collect(Collectors.toList());
//		
//		final List<Pair<Integer, Quad<String, String, RawTerm, RawTerm>>> 
//		obsEquationTerms = sect.scObsEquation().stream() 
//			.map(eqn -> new Pair<>(
//					getLoc(eqn), 
//					new Quad<>(
//							nullable(eqn.scGen().symbol()),     // free_var
//							nullable(eqn.scGen().scGenType()),  // freevar type
//							this.terms.get(eqn.scTermPath(0)),  // rawterm left
//							this.terms.get(eqn.scTermPath(1))   // rawterm right
//							)))
//			.collect(Collectors.toList());
//		
//		final List<Pair<Integer, Pair<List<String>, List<String>>>> 
//		entEquationTerms = sect.scQuotientFkEqu().stream()
//				.map(eqn -> new Pair<>(
//						getLoc(eqn), 
//						new Pair<>(this.path.get(eqn.scSymPath(0)),
//								this.path.get(eqn.scSymPath(1)))))
//				.collect(Collectors.toList());
//		
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(ctx)
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//			
//		final ColimSchExp.ColimSchExpQuotient<Ty> 
//		term = new ColimSchExp.ColimSchExpQuotient<>(ty, schs, eqEn, obsEquationTerms, entEquationTerms, options); 
//		
//		this.exps.put(ctx,term);
//	}
//	
//	@Override public void exitSchemaColimitExp_CoProduct(AqlParser.SchemaColimitExp_CoProductContext ctx) {
//		final List<SchemaRefContext> schRefs = ctx.schemaRef();
//		final TypesideRefContext tyRef = ctx.typesideRef();
//	
//		@SuppressWarnings("unchecked")
//		final TyExp
//		ty = (TyExp) this.exps.get(tyRef);
//	
//		final List<LocStr> 
//		schs = schRefs.stream() 
//			.map(ref -> makeLocStr(ref))
//			.collect(Collectors.toList());
//					
//		final List<Pair<Integer, Quad<String, String, String, String>>> eqEn = new LinkedList<>(); 
//		final List<Pair<Integer, Quad<String, String, RawTerm, RawTerm>>> entEquationTerms = new LinkedList<>(); 
//		final List<Pair<Integer, Pair<List<String>, List<String>>>> obsEquationTerms = new LinkedList<>(); 
//		final List<Pair<String, String>> options = new LinkedList<>(); 
//		
//		final ColimSchExp.ColimSchExpQuotient<String> 
//		term = new ColimSchExp.ColimSchExpQuotient<>(ty, schs, eqEn , entEquationTerms, obsEquationTerms, options); 
//		
//		this.exps.put(ctx,term);
//	}
//	
//	@Override public void exitSchemaColimitExp_Modify(AqlParser.SchemaColimitExp_ModifyContext ctx) {
//		final SchemaColimitRefContext schColimRef = ctx.schemaColimitRef();
//		final SchemaColimitModifySectionContext sect = ctx.schemaColimitModifySection();
//	
//		@SuppressWarnings("unchecked")
//		final ColimSchExp.ColimSchExpVar<String>
//		schColimRefExp = (ColimSchExpVar<String>) this.exps.get(schColimRef);
//	
//		final List<Pair<LocStr, String>> 
//		ens = sect.scArrowRenameEnt().stream()
//			.map(x -> new Pair<>( 
//					makeLocStr(x.scEntityId()),
//					x.scEntityAlias().getText()))
//			.collect(Collectors.toList());
//		
//		final List<Pair<Pair<String,LocStr>, String>> 
//		fksRename = sect.scArrowRenameFk().stream()
//				.map(x -> new Pair<>( 
//						new Pair<>( 
//								x.scEntityAlias().getText(),
//								makeLocStr(x.scFkId())),
//						x.scFkAlias().getText()))
//				.collect(Collectors.toList());
//		
//		final List<Pair<Pair<String,LocStr>, String>> 
//		attsRename = sect.scArrowRenameAttr().stream()
//				.map(x -> new Pair<>( 
//						new Pair<>( 
//								x.scEntityAlias().getText(),
//								makeLocStr(x.scAttrId())),
//						x.scAttrAlias().getText()))
//				.collect(Collectors.toList());
//		
//		final List<Pair<Pair<String,LocStr>, List<String>>> 
//		fksDelete = sect.scArrowDeleteFk().stream()
//				.map(x -> 
//					new Pair<> ( 
//						new Pair<>( 
//								x.scEntityAlias().getText(),
//								makeLocStr(x.scFkId())),
//						x.scFkAlias().stream() 
//							.map(y -> y.getText())
//							.collect(Collectors.toList())))
//				.collect(Collectors.toList());
//		
//		final List<Pair<Pair<String,LocStr>, Triple<String, String, RawTerm>>> 
//		attsDelete = sect.scArrowDeleteAttr().stream()
//				.map(x -> 
//					new Pair<>( 
//						new Pair<>( 
//								x.scEntityAlias().getText(),
//								makeLocStr(x.scAttrId())),
//						new Triple<>(
//								x.scAttrAlias(0).getText(),
//								x.scAttrAlias(1).getText(),
//								(RawTerm) null)))
//				.collect(Collectors.toList());
//				
//		final List<Pair<String,String>>
//		options = Optional.ofNullable(ctx)
//					.map(s -> this.aopts.get(s.allOptions()))
//					.orElseGet(LinkedList::new);
//			
//		final ColimSchExpModify<String> 
//		term = new ColimSchExpModify<>(schColimRefExp, ens, fksRename, attsRename, fksDelete, attsDelete, options); 
//		
//		this.exps.put(ctx,term);
//	}
//	
//	@Override public void exitSchemaColimitExp_Wrap(AqlParser.SchemaColimitExp_WrapContext ctx) {
//		// TODO write
//	}
//	
//	//**** helpers ****
//	
//	
//	@Override public void exitScTermPath_Dotted(AqlParser.ScTermPath_DottedContext ctx) {
//		final String schRef = ctx.schemaRef().getText();
//		
//		final List<RawTerm> child = new LinkedList<>();
//		child.add(new RawTerm(schRef));
//		
//		final RawTerm term = new RawTerm(ctx.schemaTermId().getText(), child);
//		this.terms.put(ctx,term);
//	}
//	
//	@Override public void exitScTermPath_Singular(AqlParser.ScTermPath_SingularContext ctx) {
//		final RawTerm term = new RawTerm(ctx.schemaTermId().getText());
//		this.terms.put(ctx,term);
//	}
//
//	@Override public void exitScSymPath(AqlParser.ScSymPathContext ctx) {
//		final List<String> path = ctx.scAlias().stream()
//				.map(x -> x.getText())
//				.collect(Collectors.toList());
//		this.path.put(ctx,path);
//	}
//
//}