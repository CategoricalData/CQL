package catdata.aql.exp;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.List;

import catdata.Pair;
import catdata.ParseException;
import catdata.Program;
import catdata.Triple;
import catdata.aql.RawTerm;

//import catdata.aql.grammar.AqlLexerRules;
//import catdata.aql.grammar.AqlParser;

/**
 * The real parsers are called with a CharStreams. Wrappers are provided for the
 * interface methods: CharStreams.fromReader(input)
 * CharStreams.fromString(input) CharStreams.fromStream(input)
 * 
 * @author fredrick.eisele
 *
 */
public class Antlr4Parser   {

	
	/*
	 * protected Antlr4Parser() { }
	 * 
	 * private AqlParser getParser(CharStream cs) { final AqlLexerRules lexer = new
	 * AqlLexerRules(cs); final CommonTokenStream tokens = new
	 * CommonTokenStream(lexer); return new AqlParser(tokens); }
	 * 
	 * private Program<Exp<?>> parseProgram(CharStream cs) { final AqlParser parser
	 * = getParser(cs); final ParseTree tree = parser.program();
	 * 
	 * final AqlLoaderListener loader = new AqlLoaderListener(); final
	 * ParseTreeWalker walker = new ParseTreeWalker();
	 * walker.walk((ParseTreeListener) loader, tree);
	 * 
	 * throw new RuntimeException("Antlr support disabled."); //new
	 * Program(loader.decls, cs.toString(), null, loader.kind); }
	 * 
	 * public Program<Exp<?>> parseProgram(Reader rdr) throws ParseException,
	 * IOException { return parseProgram(CharStreams.fromReader(rdr)); } public
	 * Program<Exp<?>> parseProgram(String str) throws ParseException { return
	 * parseProgram(CharStreams.fromString(str)); }
	 * 
	 * public Triple<List<catdata.Pair<String, String>>, RawTerm, RawTerm>
	 * parseEq(String s) throws ParseException { // try { // return
	 * Parsers.or(term1, term2).from(TOKENIZER, IGNORED).parse(s); // } catch
	 * (ParserException e) { // throw new ParseException(e.getLocation().column,
	 * e.getLocation().line, e); // } return null; }
	 * 
	 * public catdata.Pair<List<catdata.Pair<String, String>>, RawTerm>
	 * parseTermInCtx(String s) throws ParseException { // try { // return
	 * Parsers.or(term1, term2).from(TOKENIZER, IGNORED).parse(s); //} catch
	 * (ParserException e) { // throw new ParseException(e.getLocation().column,
	 * e.getLocation().line, e); //} return null; }
	 * 
	 * public RawTerm parseTermNoCtx(String s) throws ParseException { // try { //
	 * return term().from(TOKENIZER, IGNORED).parse(s); // } catch (ParserException
	 * e) { // throw new ParseException(e.getLocation().column,
	 * e.getLocation().line, e); // } return null; }
	 * 
	 * //public static catdata.Pair<String, String> //parseInfer(String s) { //
	 * Parser<catdata.Pair<String, String>> p = Parsers //
	 * .tuple(token("literal").followedBy(token(":")),
	 * ident.followedBy(token("->")), ident) // .map(x -> new catdata.Pair<>(x.b,
	 * x.c)); // return p.from(TOKENIZER, IGNORED).parse(s); // return null; //}
	 * 
	 * //public static String //parseInfer1(String s) { // Parser<String> p =
	 * Parsers.tuple(token("literal"), token(":"), ident).map(x -> x.c); // return
	 * p.from(TOKENIZER, IGNORED).parse(s); // return null; //}
	 * 
	 * public Collection<String> getReservedWords() { final Vocabulary vocab =
	 * AqlLexerRules.VOCABULARY; final String[] tokenNames = new
	 * String[vocab.getMaxTokenType()]; for (int ix = 0; ix < tokenNames.length;
	 * ix++) { tokenNames[ix] = vocab.getLiteralName(ix); if (tokenNames[ix] ==
	 * null) { tokenNames[ix] = vocab.getSymbolicName(ix); }
	 * 
	 * if (tokenNames[ix] == null) { tokenNames[ix] = "<INVALID>"; } } return
	 * Util.list(tokenNames); }
	 * 
	 * @Override public Collection<String> getOperations() { // TODO Auto-generated
	 * method stub return null; }
	 */
}