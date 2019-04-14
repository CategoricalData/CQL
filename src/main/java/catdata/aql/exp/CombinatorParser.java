package catdata.aql.exp;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.jparsec.Parser;
import org.jparsec.Parser.Reference;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Terminals.Identifier;
import org.jparsec.Terminals.IntegerLiteral;
import org.jparsec.Terminals.StringLiteral;
import org.jparsec.Token;
import org.jparsec.error.ParserException;
import org.jparsec.functors.Pair;
import org.jparsec.functors.Tuple3;
import org.jparsec.functors.Tuple4;
import org.jparsec.functors.Tuple5;

import catdata.LocStr;
import catdata.ParseException;
import catdata.Program;
import catdata.Quad;
import catdata.Triple;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.Kind;
import catdata.aql.RawTerm;
import catdata.aql.exp.ColimSchExp.ColimSchExpQuotient;
import catdata.aql.exp.ColimSchExp.ColimSchExpRaw;
import catdata.aql.exp.ColimSchExp.ColimSchExpVar;
import catdata.aql.exp.ColimSchExp.ColimSchExpWrap;
import catdata.aql.exp.EdsExp.EdsExpSch;
import catdata.aql.exp.EdsExp.EdsExpVar;
import catdata.aql.exp.EdsExpRaw.EdExpRaw;
import catdata.aql.exp.GraphExp.GraphExpRaw;
import catdata.aql.exp.GraphExp.GraphExpVar;
import catdata.aql.exp.InstExp.InstExpVar;
import catdata.aql.exp.MapExp.MapExpVar;
import catdata.aql.exp.PragmaExp.PragmaExpCheck;
import catdata.aql.exp.PragmaExp.PragmaExpConsistent;
import catdata.aql.exp.PragmaExp.PragmaExpJs;
import catdata.aql.exp.PragmaExp.PragmaExpMatch;
import catdata.aql.exp.PragmaExp.PragmaExpProc;
import catdata.aql.exp.PragmaExp.PragmaExpSql;
import catdata.aql.exp.PragmaExp.PragmaExpToCsvInst;
import catdata.aql.exp.PragmaExp.PragmaExpToCsvTrans;
import catdata.aql.exp.PragmaExp.PragmaExpToJdbcInst;
import catdata.aql.exp.PragmaExp.PragmaExpToJdbcQuery;
import catdata.aql.exp.PragmaExp.PragmaExpToJdbcTrans;
import catdata.aql.exp.PragmaExp.PragmaExpVar;
import catdata.aql.exp.QueryExp.QueryExpId;
import catdata.aql.exp.QueryExp.QueryExpVar;
import catdata.aql.exp.QueryExpRaw.PreBlock;
import catdata.aql.exp.QueryExpRaw.Trans;
import catdata.aql.exp.SchExp.SchExpCod;
import catdata.aql.exp.SchExp.SchExpDom;
import catdata.aql.exp.SchExp.SchExpDst;
import catdata.aql.exp.SchExp.SchExpEmpty;
import catdata.aql.exp.SchExp.SchExpInst;
import catdata.aql.exp.SchExp.SchExpPivot;
import catdata.aql.exp.SchExp.SchExpSrc;
import catdata.aql.exp.SchExp.SchExpVar;
import catdata.aql.exp.TransExp.TransExpId;
import catdata.aql.exp.TransExp.TransExpVar;
import catdata.aql.exp.TyExp.TyExpEmpty;
import catdata.aql.exp.TyExp.TyExpSch;
import catdata.aql.exp.TyExp.TyExpVar;

public class CombinatorParser implements IAqlParser {

	protected CombinatorParser() {
	}

	private static final Terminals RESERVED = Terminals.caseSensitive(ops, Util.union(res, opts));

	private static final Parser<Void> IGNORED = Parsers
			.or(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();

	private static final Parser<Object> TOKENIZER = Parsers.or(StringLiteral.DOUBLE_QUOTE_TOKENIZER,
			RESERVED.tokenizer(), Identifier.TOKENIZER, IntegerLiteral.TOKENIZER);

	private static Parser<Token> token(String... names) {
		return RESERVED.token(names);
	}

	private static final Parser<String> ident = Parsers.or(StringLiteral.PARSER, IntegerLiteral.PARSER,
			Identifier.PARSER);

	private static final Parser<LocStr> locstr = Parsers.tuple(Parsers.INDEX, ident).map(x -> new LocStr(x.a, x.b));

	private static Parser<RawTerm> term() {

		Reference<RawTerm> ref = Parser.newReference();

		Parser<RawTerm> ann = Parsers.tuple(ident, token("@"), ident).map(x -> new RawTerm(x.a, x.c));

		Parser<RawTerm> app = Parsers.tuple(ident, token("("), ref.lazy().sepBy(token(",")), token(")"))
				.map(x -> new RawTerm(x.a, x.c));

		Parser<RawTerm> app2 = Parsers.tuple(token("("), ref.lazy(), ident, ref.lazy(), token(")"))
				.map(x -> new RawTerm(x.c, Util.list(x.b, x.d)));

		Parser<RawTerm> sing = ident.map(x -> new RawTerm(x, Collections.emptyList()));

		// use of ref.lazy for first argument leads to left recursion
		Parser<RawTerm> dot = Parsers.tuple(ident.label(
				"\n\n **** Possible problem: only identifiers allowed in . notation (lest left-recusion ensue)\n\n"),
				(Parsers.tuple(token("."), ident).map(x -> x.b)).many1()).map(x -> {
					RawTerm r = new RawTerm(x.a, Collections.emptyList());
					for (String s : x.b) {
						r = new RawTerm(s, Collections.singletonList(r));
					}
					return r;
				});

		Parser<RawTerm> ret = Parsers.or(ann, app, app2, dot, sing /* ,parens(ref) */); // conflicts with infix

		ref.set(ret);
		return ret;
	}

	private static <X> Parser<X> parens(Reference<X> p) {
		Parser<X> q = Parsers.tuple(token("("), p.lazy(), token(")")).map(x -> x.b);
		return q;
	}

	private static void tyExp() {
		Parser<TyExp> var = ident.map(TyExpVar::new), sql = token("sql").map(x -> new TyExpSql()),
				empty = token("empty").map(x -> new TyExpEmpty()),
				sch = Parsers.tuple(token("typesideOf"), sch_ref.lazy()).map(x -> new TyExpSch(x.b)),
				ret = Parsers.or(sch, empty, sql, tyExpRaw(), var, parens(ty_ref));

		ty_ref.set(ret);
	}

	private static void schExp() {
		@SuppressWarnings("unchecked")
		Parser<SchExp> var = ident.map(SchExpVar::new),
				empty = Parsers.tuple(token("empty"), token(":"), ty_ref.get()).map(x -> new SchExpEmpty(x.c)),
				pivot = Parsers
						.tuple(token("pivot"), inst_ref.lazy(), options.between(token("{"), token("}")).optional())
						.map(x -> new SchExpPivot<>(x.b, x.c == null ? Collections.emptyList() : x.c)),

				inst = Parsers.tuple(token("schemaOf"), inst_ref.lazy()).map(x -> new SchExpInst<>(x.b)),
				colim = Parsers.tuple(token("getSchema"), colim_ref.lazy()).map(x -> new SchExpColim(x.b)),
				cod = Parsers.tuple(token("cod_q"), query_ref.lazy()).map(x -> new SchExpCod(x.b)),
				dom = Parsers.tuple(token("dom_q"), query_ref.lazy()).map(x -> new SchExpDom(x.b)),
				cod2 = Parsers.tuple(token("cod_m"), map_ref.lazy()).map(x -> new SchExpDst(x.b)),
				dom2 = Parsers.tuple(token("dom_m"), map_ref.lazy()).map(x -> new SchExpSrc(x.b)),
				all = Parsers
						.tuple(token("import_jdbc_all"), ident,
								options.between(token("{"), token("}")).optional())
						.map(x -> new SchExpJdbcAll(x.b, Util.newIfNull(x.c))),

				ret = Parsers.or(inst, empty, schExpRaw(), var, all, colim, parens(sch_ref), pivot, cod, dom, cod2,
						dom2);

		sch_ref.set(ret);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void pragmaExp() {
		Parser<Pair<List<String>, List<catdata.Pair<String, String>>>> p = Parsers.tuple(ident.many(), options)
				.between(token("{"), token("}"));

		Parser<PragmaExp> var = ident.map(PragmaExpVar::new),

				csvInst = Parsers
						.tuple(token("export_csv_instance"), inst_ref.lazy(), ident,
								options.between(token("{"), token("}")).optional())
						.map(x -> new PragmaExpToCsvInst(x.b, x.c, x.d == null ? Collections.emptyList() : x.d)),

				csvTrans = Parsers
						.tuple(token("export_csv_transform"), trans_ref.lazy(), ident,
								options.between(token("{"), token("}")).optional(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new PragmaExpToCsvTrans(x.b, x.c, x.d == null ? Collections.emptyList() : x.d,
								x.e == null ? Collections.emptyList() : x.e)),

				sql = Parsers.tuple(token("exec_jdbc"), ident, p)
						.map(x -> new PragmaExpSql(x.b, x.c.a, x.c.b)),

				js = Parsers.tuple(token("exec_js"), p).map(x -> new PragmaExpJs(x.b.a, x.b.b)),
				proc = Parsers.tuple(token("exec_cmdline"), p).map(x -> new PragmaExpProc(x.b.a, x.b.b)),

				jdbcInst = Parsers
						.tuple(Parsers.tuple(token("export_jdbc_instance"), inst_ref.lazy()), ident, ident,
								options.between(token("{"), token("}")).optional())
						.map(x -> new PragmaExpToJdbcInst(x.a.b, x.b, x.c,
								x.d == null ? Collections.emptyList() : x.d)),

				jdbcQuery = Parsers.tuple(Parsers.tuple(token("export_jdbc_query"), query_ref.lazy()),
						Parsers.tuple(ident, ident, ident), options.between(token("{"), token("}")).optional())
						.map(x -> new PragmaExpToJdbcQuery(x.a.b, x.b.a, x.b.b, x.b.c, 
								x.c == null ? Collections.emptyList() : x.c)),

				jdbcTrans = Parsers
						.tuple(Parsers.tuple(token("export_jdbc_transform"), trans_ref.lazy()), ident, ident,
								Parsers.tuple(options.between(token("{"), token("}")).optional(),
										options.between(token("{"), token("}")).optional()))
						.map(x -> new PragmaExpToJdbcTrans(x.a.b, x.b, x.c, 
								x.d.a == null ? Collections.emptyList() : x.d.a,
								x.d.b == null ? Collections.emptyList() : x.d.b)),

				match = Parsers
						.tuple(token("match"), ident.followedBy(token(":")), graph_ref.lazy().followedBy(token("->")),
								graph_ref.lazy(), options.between(token("{"), token("}")).optional())
						.map(x -> new PragmaExpMatch(x.b, x.c, x.d, x.e == null ? Collections.emptyList() : x.e)),

				check = Parsers.tuple(token("check"), edsExp(), inst_ref.lazy()).map(x -> new PragmaExpCheck(x.c, x.b)),

				check2 = Parsers.tuple(token("check_query"), query_ref.lazy(), edsExp(), edsExp())
						.map(x -> new PragmaExpCheck2(x.b, x.c, x.d)),

				cons = Parsers.tuple(token("assert_consistent"), inst_ref.lazy())
						.map(x -> new PragmaExpConsistent(x.b)),

				ret = Parsers.or(jdbcQuery, check, check2, csvInst, cons, csvTrans, var, sql, js, proc, jdbcInst,
						jdbcTrans, match, parens(pragma_ref));

		pragma_ref.set(ret);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void instExp() {

		Parser<InstExpCascadeDelete> cd = Parsers
				.tuple(token("cascade_delete"), inst_ref.lazy(), token(":"), sch_ref.lazy())
				.map(x -> new InstExpCascadeDelete(x.b, x.d));

		Parser<InstExpCoProdFull> l2 = Parsers
				.tuple(token("coproduct"), ident.sepBy(token("+")), token(":"), sch_ref.lazy(),
						options.between(token("{"), token("}")).optional())
				.map(x -> new InstExpCoProdFull(x.b, x.d, Util.newIfNull(x.e)));

		Parser<InstExp<?, ?, ?, ?>> var = ident.map(InstExpVar::new),
				empty = Parsers.tuple(token("empty"), token(":"), sch_ref.get()).map(x -> new InstExpEmpty(x.c)),
				pi = Parsers
						.tuple(token("pi"), map_ref.lazy(), inst_ref.lazy(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new InstExpPi(x.b, x.c, x.d == null ? new HashMap() : Util.toMapSafely(x.d))),

				except = Parsers.tuple(token("except"), inst_ref.lazy(), inst_ref.lazy())
						.map(x -> new InstExpDiff(x.b, x.c)),

				sigma = Parsers
						.tuple(token("sigma"), map_ref.lazy(), inst_ref.lazy(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new InstExpSigma(x.b, x.c, x.d == null ? new HashMap() : Util.toMapSafely(x.d))),
				sigma_chase = Parsers
						.tuple(token("sigma_chase"), map_ref.lazy(), inst_ref.lazy(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new InstExpSigmaChase(x.b, x.c, x.d == null ? new HashMap() : Util.toMapSafely(x.d))),

				frozen = Parsers.tuple(token("frozen"), query_ref.lazy(), ident).map(x -> new InstExpFrozen(x.b, x.c)),
				delta = Parsers.tuple(token("delta"), map_ref.lazy(), inst_ref.lazy())
						.map(x -> new InstExpDelta(x.b, x.c)),
				distinct = Parsers.tuple(token("distinct"), inst_ref.lazy()).map(x -> new InstExpDistinct(x.b)),
				anon = Parsers.tuple(token("anonymize"), inst_ref.lazy()).map(x -> new InstExpAnonymize(x.b)),
				pivot = Parsers
						.tuple(token("pivot"), inst_ref.lazy(), options.between(token("{"), token("}")).optional())
						.map(x -> new InstExpPivot(x.b, x.c == null ? new LinkedList() : x.c)),

				eval = Parsers
						.tuple(token("eval"), query_ref.lazy(), inst_ref.lazy(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new InstExpEval(x.b, x.c, x.d == null ? Collections.emptyList() : x.d)),
				dom = Parsers.tuple(token("dom_t"), trans_ref.lazy()).map(x -> new InstExpDom(x.b)),
				cod = Parsers.tuple(token("cod_t"), trans_ref.lazy()).map(x -> new InstExpCod(x.b)),
				chase = Parsers
						.tuple(token("chase"), edsExp(), inst_ref.lazy(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new InstExpChase(x.b, x.c, x.d == null ? Collections.emptyList() : x.d)),

				coeval = Parsers
						.tuple(token("coeval"), query_ref.lazy(), inst_ref.lazy(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new InstExpCoEval(x.b, x.c, x.d == null ? Collections.emptyList() : x.d));

		Parser ret = Parsers.or(queryQuotientExpRaw(), sigma_chase, l2, pi, frozen, instExpRand(), instExpCoEq(),
				instExpJdbcAll(), chase, instExpJdbc(), empty, instExpRaw(), var, sigma, delta, distinct, eval,
				colimInstExp(), dom, cd, anon, except, pivot, cod, instExpCsv(), coeval, parens(inst_ref));

		inst_ref.set(ret);
	}

	// @SuppressWarnings({"unchecked"})
	private static void graphExp() {
		Parser<GraphExp> var = ident.map(GraphExpVar::new),

				ret = Parsers.or(var, graphExpRaw(), parens(graph_ref));

		graph_ref.set(ret);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void mapExp() {
		Parser<MapExp> var = ident.map(MapExpVar::new),
				id2 = Parsers.tuple(token("include"), sch_ref.lazy(), sch_ref.lazy()).map(x -> new MapExpId(x.b, x.c)),
				id = Parsers.tuple(token("identity"), sch_ref.lazy()).map(x -> new MapExpId(x.b)),
				colim = Parsers.tuple(token("getMapping"), colim_ref.lazy(), ident).map(x -> new MapExpColim(x.c, x.b)),
				comp = Parsers.tuple(token("["), map_ref.lazy(), token(";"), map_ref.lazy(), token("]"))
						.map(x -> new MapExpComp(x.b, x.d)),
				pivot = Parsers
						.tuple(token("pivot"), inst_ref.lazy(), options.between(token("{"), token("}")).optional())
						.map(x -> new MapExpPivot(x.b, x.c == null ? Collections.emptyList() : x.c)),

				ret = Parsers.or(id, id2, mapExpRaw(), var, pivot, colim, comp, parens(map_ref));

		map_ref.set(ret);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void transExp() {
		Parser<TransExp<?, ?, ?, ?, ?, ?, ?, ?>> var = ident.map(TransExpVar::new),
				id = Parsers.tuple(token("identity"), inst_ref.lazy()).map(x -> new TransExpId(x.b)),
				id1 = Parsers.tuple(token("include"), inst_ref.lazy(), inst_ref.lazy())
						.map(x -> new TransExpId(x.b, x.c)),
				distinct_return = Parsers.tuple(token("distinct_return"), inst_ref.lazy())
						.map(x -> new TransExpDistinct2(x.b)),

				frozen = Parsers
						.tuple(Parsers.tuple(token("frozen"), query_ref.lazy().followedBy(token("lambda")))
								.map(x -> x.b), ident.followedBy(token(":")), ident.followedBy(token(".")),
								term().followedBy(token(":")), ident)
						.map(x -> new TransExpFrozen(x.a, x.b, x.c, x.d, x.e)),

				pi = Parsers
						.tuple(token("pi"), map_ref.lazy(), trans_ref.lazy(),
								options.between(token("{"), token("}")).optional(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new TransExpPi(x.b, x.c, x.d == null ? new HashMap() : Util.toMapSafely(x.d),
								x.e == null ? new HashMap() : Util.toMapSafely(x.e))),

				sigma = Parsers
						.tuple(token("sigma"), map_ref.lazy(), trans_ref.lazy(),
								options.between(token("{"), token("}")).optional(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new TransExpSigma(x.b, x.c, x.d == null ? new HashMap() : Util.toMapSafely(x.d),
								x.e == null ? new HashMap() : Util.toMapSafely(x.e))),
				delta = Parsers.tuple(token("delta"), map_ref.lazy(), trans_ref.lazy())
						.map(x -> new TransExpDelta(x.b, x.c)),
				unit = Parsers
						.tuple(token("unit"), map_ref.lazy(), inst_ref.lazy(),
								options.between(token("{"), token("}")).optional(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new TransExpSigmaDeltaUnit(x.b, x.c,
								x.d == null ? new HashMap() : Util.toMapSafely(x.d))),
				counit = Parsers
						.tuple(token("counit"), map_ref.lazy(), inst_ref.lazy(),
								options.between(token("{"), token("}")).optional(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new TransExpSigmaDeltaCounit(x.b, x.c,
								x.d == null ? new HashMap() : Util.toMapSafely(x.d))),
				distinct = Parsers.tuple(token("distinct"), trans_ref.lazy()).map(x -> new TransExpDistinct(x.b)),
				eval = Parsers
						.tuple(token("eval"), query_ref.lazy(), trans_ref.lazy(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new TransExpEval(x.b, x.c, x.d == null ? new LinkedList() : Util.newIfNull(x.d))),
				except = Parsers.tuple(token("except_return"), inst_ref.lazy(), inst_ref.lazy())
						.map(x -> new TransExpDiffReturn(x.b, x.c)),
				except2 = Parsers.tuple(token("except"), trans_ref.lazy(), inst_ref.lazy())
						.map(x -> new TransExpDiff(x.c, x.b)),

				coeval = Parsers
						.tuple(token("coeval"), query_ref.lazy(), trans_ref.lazy(),
								options.between(token("{"), token("}")).optional(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new TransExpCoEval(x.b, x.c, x.d == null ? new LinkedList() : x.d,
								x.e == null ? new LinkedList() : x.e)),

				unitq = Parsers.tuple(token("unit_query"), query_ref.lazy(), inst_ref.lazy(),
						// options.between(token("{"),
						// token("}")).optional(),
						options.between(token("{"), token("}")).optional())
						.map(x -> new TransExpCoEvalEvalUnit(x.b, x.c,
								x.d == null ? new HashMap() : Util.toMapSafely(x.d))),
				counitq = Parsers.tuple(token("counit_query"), query_ref.lazy(), inst_ref.lazy(),
						// options.between(token("{"),
						// token("}")).optional(),
						options.between(token("{"), token("}")).optional())
						.map(x -> new TransExpCoEvalEvalCoUnit(x.b, x.c,
								x.d == null ? new HashMap() : Util.toMapSafely(x.d))),

				comp = Parsers.tuple(token("["), trans_ref.lazy(), token(";"), trans_ref.lazy(), token("]"))
						.map(x -> new TransExpCompose(x.b, x.d));

		Parser ret = Parsers.or(id, id1, transExpRaw(), var, sigma, delta, unit, counit, distinct, eval, coeval,
				transExpCsv(), pi, distinct_return, frozen, except2, unitq, except, counitq, transExpJdbc(), comp,
				parens(trans_ref));

		trans_ref.set(ret);
	}

	private static final Parser<String> option = Parsers.or(AqlOptions.optionNames().stream()
			.map(x -> token(x).map(y -> y.value().toString())).collect(Collectors.toList()));

//	private static final Parser<List<LocStr>> imports = Parsers.tuple(token("imports"), locstr.many()).optional()
//			.map(x -> x == null ? new LinkedList() : x.b);

	private static final <X> Parser<List<X>> imports(Parser<X> p) {
		return Parsers.tuple(token("imports"), p.many()).optional().map(x -> x == null ? Collections.emptyList() : x.b);
	}

	private static final Parser<List<catdata.Pair<String, String>>> options = Parsers
			.tuple(token("options"), Parsers.tuple(option, token("="), ident).many()).optional().map(x -> {
				if (x != null) {
					List<catdata.Pair<String, String>> ret = new ArrayList<>(x.b.size());
					for (Tuple3<String, Token, String> y : x.b) {
						ret.add(new catdata.Pair<>(y.a, y.c));
					}
					return ret;
				}
				return Collections.emptyList();

			});

	private static final Parser<List<catdata.Pair<String, String>>> ctx = Parsers
			.tuple(ident.many1(), Parsers.tuple(token(":"), ident).optional()).sepBy(token(",")).map(x -> {
				if (x.size() == 0) {
					return Collections.emptyList();
				}
				List<catdata.Pair<String, String>> ret = new ArrayList<>(x.size());
				for (Pair<List<String>, Pair<Token, String>> y : x) {
					for (String z : y.a) {
						ret.add(new catdata.Pair<>(z, y.b == null ? null : y.b.b));
					}
				}
				return ret;
			});

	private static <X> Parser<List<catdata.Pair<LocStr, X>>> env(Parser<X> p, String t) {
		return Parsers.tuple(locstr.many1(), Parsers.tuple(token(t), p)).many().map(x -> {
			if (x.isEmpty()) {
				return Collections.emptyList();
			}
			List<catdata.Pair<LocStr, X>> ret = new ArrayList<>(x.size());
			for (Pair<List<LocStr>, Pair<Token, X>> y : x) {
				for (LocStr z : y.a) {
					ret.add(new catdata.Pair<>(z, y.b.b));
				}
			}
			return ret;
		});
	}

	private static <X> Parser<List<catdata.Pair<catdata.Pair<String, LocStr>, X>>> env2(Parser<X> p, String t) {
		return Parsers.tuple(Parsers.tuple(ident.followedBy(token(".")), locstr).many1(), Parsers.tuple(token(t), p))
				.many().map(x -> {
					if (x.isEmpty()) {
						return Collections.emptyList();
					}
					List<catdata.Pair<catdata.Pair<String, LocStr>, X>> ret = new ArrayList<>(x.size());
					for (Pair<List<Pair<String, LocStr>>, Pair<Token, X>> y : x) {
						for (Pair<String, LocStr> z : y.a) {
							ret.add(new catdata.Pair<>(new catdata.Pair<>(z.a, z.b), y.b.b));
						}
					}
					return ret;
				});
	}

	private static Parser<GraphExpRaw> graphExpRaw() {
		Parser<List<LocStr>> nodes = Parsers.tuple(token("nodes"), locstr.many()).map(x -> x.b);

		Parser<Pair<Token, List<Tuple5<List<LocStr>, Token, String, Token, String>>>> edges = Parsers
				.tuple(token("edges"), Parsers.tuple(locstr.many1(), token(":"), ident, token("->"), ident).many());
		Parser<List<catdata.Pair<LocStr, catdata.Pair<String, String>>>> edges0 = edges.map(x -> {
			if (x.b.isEmpty()) {
				return Collections.emptyList();
			}
			List<catdata.Pair<LocStr, catdata.Pair<String, String>>> ret = new ArrayList<>(x.b.size());
			for (Tuple5<List<LocStr>, Token, String, Token, String> a : x.b) {
				for (LocStr b : a.a) {
					ret.add(new catdata.Pair<>(b, new catdata.Pair<>(a.c, a.e)));
				}
			}
			return ret;
		});

		Parser<Tuple3<List<GraphExp>, List<LocStr>, List<catdata.Pair<LocStr, catdata.Pair<String, String>>>>> pa = Parsers
				.tuple(imports(graph_ref.lazy()), nodes.optional(), edges0.optional());

		Parser<GraphExpRaw> ret = pa.map(x -> new GraphExpRaw(Util.newIfNull(x.b), Util.newIfNull(x.c), x.a));
		return ret.between(token("literal").followedBy(token("{")), token("}"));

	}

	private static Parser<InstExpCsv> instExpCsv() {
		Parser<catdata.Pair<List<catdata.Pair<LocStr, String>>, List<catdata.Pair<String, String>>>> b = Parsers
				.tuple(token("{"), env(ident, "->"), options, token("}")).map(x -> new catdata.Pair<>(x.b, x.c));

		Parser<Pair<List<catdata.Pair<LocStr, catdata.Pair<List<catdata.Pair<LocStr, String>>, List<catdata.Pair<String, String>>>>>, List<catdata.Pair<String, String>>>> qs = Parsers
				.tuple(env(b, "->"), options).between(token("{"), token("}"))
				.optional(new Pair<>(Collections.emptyList(), Collections.emptyList()));

		Parser<InstExpCsv> ret = Parsers.tuple(token("import_csv"), ident.followedBy(token(":")), sch_ref.lazy(), qs)
				.map(x -> new InstExpCsv(x.c, x.d.a, x.d.b, x.b));
		return ret;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Parser<TransExpCsv> transExpCsv() {
		Parser<Pair<List<catdata.Pair<LocStr, String>>, List<catdata.Pair<String, String>>>> qs = Parsers
				.tuple(env(ident, "->"), options).between(token("{"), token("}"));

		Parser<TransExpCsv> ret = Parsers.tuple(token("import_csv").followedBy(token(":")),
				inst_ref.lazy().followedBy(token("->")), inst_ref.lazy(), qs)
				.map(x -> new TransExpCsv(x.b, x.c, x.d.a, x.d.b));
		return ret;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Parser<TransExpJdbc> transExpJdbc() {
		Parser<Pair<InstExp, InstExp>> st = Parsers.tuple(inst_ref.lazy(), token("->"), inst_ref.lazy())
				.map(x -> new Pair(x.a, x.c));

		Parser<Pair<List<catdata.Pair<LocStr, String>>, List<catdata.Pair<String, String>>>> qs = Parsers
				.tuple(env(ident, "->"), options).between(token("{"), token("}"));

		Parser<TransExpJdbc> ret = Parsers.tuple(token("import_jdbc"), ident.followedBy(token(":")), st, qs)
				.map(x -> new TransExpJdbc(x.b, x.c.a, x.c.b, x.d.a, x.d.b)); 
		return ret;
	}

	private static Parser<TyExpRaw> tyExpRaw() {
		Parser<List<LocStr>> types = Parsers.tuple(token("types"), locstr.many()).map(x -> x.b);

		Parser<Pair<Token, List<Tuple3<List<LocStr>, Token, String>>>> consts = Parsers.tuple(token("constants"),
				Parsers.tuple(locstr.many1(), token(":"), ident).many());
		Parser<List<catdata.Pair<LocStr, catdata.Pair<List<String>, String>>>> consts0 = consts.map(x -> {
			if (x.b.isEmpty()) {
				return Collections.emptyList();
			}
			List<catdata.Pair<LocStr, catdata.Pair<List<String>, String>>> ret = new ArrayList<>(x.b.size());
			for (Tuple3<List<LocStr>, Token, String> a : x.b) {
				for (LocStr b : a.a) {
					ret.add(new catdata.Pair<>(b, new catdata.Pair<>(Collections.emptyList(), a.c)));
				}
			}
			return ret;
		});

		Parser<Pair<Token, List<Tuple5<List<LocStr>, Token, List<String>, Token, String>>>> fns = Parsers.tuple(
				token("functions"),
				Parsers.tuple(locstr.many1(), token(":"), ident.sepBy(token(",")), token("->"), ident).many());
		Parser<List<catdata.Pair<LocStr, catdata.Pair<List<String>, String>>>> fns0 = fns.map(x -> {
			if (x.b.isEmpty()) {
				return Collections.emptyList();
			}
			List<catdata.Pair<LocStr, catdata.Pair<List<String>, String>>> ret = new ArrayList<>(x.b.size());
			for (Tuple5<List<LocStr>, Token, List<String>, Token, String> a : x.b) {
				for (LocStr b : a.a) {
					ret.add(new catdata.Pair<>(b, new catdata.Pair<>(a.c, a.e)));
				}
			}
			return ret;
		});

		Parser<Pair<Token, List<catdata.Pair<Integer, Triple<List<catdata.Pair<String, String>>, RawTerm, RawTerm>>>>> eqs = Parsers
				.tuple(token("equations"), Parsers.tuple(Parsers.INDEX, Parsers.or(eq1, eq2))
						.map(x -> new catdata.Pair<>(x.a, x.b)).many());
		Parser<List<catdata.Pair<Integer, Triple<List<catdata.Pair<String, String>>, RawTerm, RawTerm>>>> eqs0 = eqs
				.map(x -> x.b);

		Parser<Pair<Token, List<Tuple3<LocStr, Token, String>>>> java_typs = Parsers.tuple(token("java_types"),
				Parsers.tuple(locstr, token("="), ident).many());
		Parser<List<catdata.Pair<LocStr, String>>> java_typs0 = java_typs.map(x -> {
			List<catdata.Pair<LocStr, String>> ret = new ArrayList<>(x.b.size());
			for (Tuple3<LocStr, Token, String> p : x.b) {
				ret.add(new catdata.Pair<>(p.a, p.c));
			}
			return ret;
		});

		Parser<Pair<Token, List<Tuple3<LocStr, Token, String>>>> java_consts = Parsers.tuple(token("java_constants"),
				Parsers.tuple(locstr, token("="), ident).many());
		Parser<List<catdata.Pair<LocStr, String>>> java_consts0 = java_consts.map(x -> {
			List<catdata.Pair<LocStr, String>> ret = new ArrayList<>(x.b.size());
			for (Tuple3<LocStr, Token, String> p : x.b) {
				ret.add(new catdata.Pair<>(p.a, p.c));
			}
			return ret;
		});

		Parser<List<String>> lll = ident.sepBy(token(",")).followedBy(token("->"));
		Parser<List<String>> jjj = Parsers.constant(Collections.emptyList());
		Parser<List<String>> uuu = Parsers.longer(lll, jjj);

		Parser<Pair<Token, List<Tuple5<LocStr, List<String>, String, Token, String>>>> java_fns = Parsers.tuple(
				token("java_functions"),
				Parsers.tuple(locstr.followedBy(token(":")), uuu, ident, token("="), ident).many());
		Parser<List<catdata.Pair<LocStr, Triple<List<String>, String, String>>>> java_fns0 = java_fns.map(x -> {
			List<catdata.Pair<LocStr, Triple<List<String>, String, String>>> ret = new ArrayList<>(x.b.size());
			for (Tuple5<LocStr, List<String>, String, Token, String> p : x.b) {
				ret.add(new catdata.Pair<>(p.a, new Triple<>(p.b, p.c, p.e)));
			}
			return ret;
		});

		Parser<List<TyExp>> ly = Parsers.tuple(token("imports"), ty_ref.lazy().many()).map(x -> x.b).optional();

		Parser<Tuple5<List<TyExp>, List<LocStr>, List<catdata.Pair<LocStr, catdata.Pair<List<String>, String>>>, List<catdata.Pair<LocStr, catdata.Pair<List<String>, String>>>, List<catdata.Pair<LocStr, String>>>> pa = Parsers
				.tuple(ly, types.optional(), consts0.optional(), fns0.optional(), java_typs0.optional());

		Parser<Tuple4<List<catdata.Pair<LocStr, String>>, List<catdata.Pair<LocStr, Triple<List<String>, String, String>>>, List<catdata.Pair<Integer, Triple<List<catdata.Pair<String, String>>, RawTerm, RawTerm>>>, List<catdata.Pair<String, String>>>> pb = Parsers
				.tuple(java_consts0.optional(), java_fns0.optional(), eqs0.optional(), options);

		Parser<TyExpRaw> ret = Parsers.tuple(pa, pb).map(x -> {

			List<catdata.Pair<LocStr, catdata.Pair<List<String>, String>>> l = new LinkedList<>();
			if (x.a.c != null) {
				l.addAll(x.a.c);
			}
			if (x.a.d != null) {
				l.addAll(x.a.d);
			}

			return new TyExpRaw(Util.newIfNull(x.a.a), Util.newIfNull(x.a.b), l, Util.newIfNull(x.b.c),
					Util.newIfNull(x.a.e), Util.newIfNull(x.b.a), Util.newIfNull(x.b.b), Util.newIfNull(x.b.d));
		});
		return ret.between(token("literal").followedBy(token("{")), token("}"));
	}

	private static Parser<SchExpRaw> schExpRaw() {
		Parser<List<LocStr>> entities = Parsers.tuple(token("entities"), locstr.many()).map(x -> x.b);

		Parser<Pair<Token, List<Tuple5<List<LocStr>, Token, String, Token, String>>>> fks = Parsers.tuple(
				token("foreign_keys"), Parsers.tuple(locstr.many1(), token(":"), ident, token("->"), ident).many());
		Parser<List<catdata.Pair<LocStr, catdata.Pair<String, String>>>> fks0 = fks.map(x -> {
			List<catdata.Pair<LocStr, catdata.Pair<String, String>>> ret = new ArrayList<>(x.b.size());
			for (Tuple5<List<LocStr>, Token, String, Token, String> a : x.b) {
				for (LocStr b : a.a) {
					ret.add(new catdata.Pair<>(b, new catdata.Pair<>(a.c, a.e)));
				}
			}
			return ret;
		});

		Parser<Pair<Token, List<Tuple5<List<LocStr>, Token, String, Token, String>>>> atts = Parsers.tuple(
				token("attributes"), Parsers.tuple(locstr.many1(), token(":"), ident, token("->"), ident).many());
		Parser<List<catdata.Pair<LocStr, catdata.Pair<String, String>>>> atts0 = atts.map(x -> {
			List<catdata.Pair<LocStr, catdata.Pair<String, String>>> ret = new ArrayList<>(x.b.size());
			for (Tuple5<List<LocStr>, Token, String, Token, String> a : x.b) {
				for (LocStr b : a.a) {
					ret.add(new catdata.Pair<>(b, new catdata.Pair<>(a.c, a.e)));
				}
			}
			return ret;
		});

		Parser<catdata.Pair<Integer, catdata.Pair<List<String>, List<String>>>> p_eq = Parsers
				.tuple(Parsers.INDEX, Parsers.tuple(ident.sepBy(token(".")), token("="), ident.sepBy(token("."))))
				.map(x -> new catdata.Pair<>(x.a, new catdata.Pair<>(x.b.a, x.b.c)));

		Parser<Pair<Token, List<catdata.Pair<Integer, catdata.Pair<List<String>, List<String>>>>>> p_eqs = Parsers
				.tuple(token("path_equations"), p_eq.many());
		Parser<List<catdata.Pair<Integer, catdata.Pair<List<String>, List<String>>>>> p_eqs0 = p_eqs.map(x -> x.b);

		Parser<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>> o_eq_from_p_eq = p_eq
				.map(x -> new catdata.Pair<>(x.first, new Quad<>("_x", null, RawTerm.fold(x.second.first, "_x"),
						RawTerm.fold(x.second.second, "_x"))));

		Parser<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>> o_eq_old = Parsers
				.tuple(Parsers.INDEX,
						Parsers.tuple(token("forall"), ident,
								Parsers.tuple(token(":"), ident).optional().followedBy(token(".")),
								term().followedBy(token("=")), term()))
				.map(x -> new catdata.Pair<>(x.a, new Quad<>(x.b.b, x.b.c == null ? null : x.b.c.b, x.b.d, x.b.e)));

		Parser<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>> o_eq = Parsers.or(o_eq_old,
				o_eq_from_p_eq);

		Parser<Pair<Token, List<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>>>> o_eqs = Parsers
				.tuple(token("observation_equations"), o_eq.many());
		Parser<List<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>>> o_eqs0 = o_eqs.map(x -> x.b);

		Parser<Tuple4<List<SchExp>, List<LocStr>, List<catdata.Pair<LocStr, catdata.Pair<String, String>>>, List<catdata.Pair<Integer, catdata.Pair<List<String>, List<String>>>>>> pa = Parsers
				.tuple(imports(sch_ref.lazy()), entities.optional(), fks0.optional(), p_eqs0.optional());
		Parser<Tuple3<List<catdata.Pair<LocStr, catdata.Pair<String, String>>>, List<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>>, List<catdata.Pair<String, String>>>> pb = Parsers
				.tuple(atts0.optional(), o_eqs0.optional(), options);

		Parser<Tuple4<Token, Token, TyExp, Token>> l = Parsers.tuple(token("literal"), token(":"), ty_ref.lazy(),
				token("{")); // .map(x -> x.c);

		// needs tyexp
		Parser<SchExpRaw> ret = Parsers.tuple(l, pa, pb, token("}"))
				.map(x -> new SchExpRaw(x.a.c, x.b.a, Util.newIfNull(x.b.b), Util.newIfNull(x.b.c),
						Util.newIfNull(x.b.d), Util.newIfNull(x.c.a), Util.newIfNull(x.c.b), x.c.c));

		return ret;
	}

	private static Parser<ColimSchExpQuotient> colimSchExpQuotient() {
		Parser<catdata.Pair<Integer, Quad<String, String, String, String>>> q = Parsers.tuple(Parsers.INDEX,
				Parsers.tuple(ident.followedBy(token(".")), ident, token("="), ident.followedBy(token(".")), ident)
						.map(x -> new Quad<>(x.a, x.b, x.d, x.e)))
				.map(x -> new catdata.Pair<>(x.a, x.b));

		Parser<List<catdata.Pair<Integer, Quad<String, String, String, String>>>> entities = Parsers
				.tuple(token("entity_equations"), q.many()).map(x -> x.b);

		Parser<catdata.Pair<Integer, catdata.Pair<List<String>, List<String>>>> p_eq = Parsers
				.tuple(Parsers.INDEX, Parsers.tuple(ident.sepBy(token(".")), token("="), ident.sepBy(token(".")))
						.map(x -> new catdata.Pair<>(x.a, x.c)))
				.map(x -> new catdata.Pair<>(x.a, x.b));

		Parser<catdata.Pair<Token, List<catdata.Pair<Integer, catdata.Pair<List<String>, List<String>>>>>> p_eqs = Parsers
				.tuple(token("path_equations"), p_eq.many()).map(x -> new catdata.Pair<>(x.a, x.b));
		Parser<List<catdata.Pair<Integer, catdata.Pair<List<String>, List<String>>>>> p_eqs0 = p_eqs.map(x -> x.second);

		Parser<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>> o_eq_old = Parsers
				.tuple(Parsers.INDEX,
						Parsers.tuple(token("forall"), ident,
								Parsers.tuple(token(":"), ident).optional().followedBy(token(".")),
								term().followedBy(token("=")), term())
								.map(x -> new Quad<>(x.b, x.c == null ? null : x.c.b, x.d, x.e)))
				.map(x -> new catdata.Pair<>(x.a, x.b));

		Parser<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>> o_eq_from_p_eq = p_eq
				.map(x -> new catdata.Pair<>(x.first, new Quad<>("_x", null, RawTerm.fold(x.second.first, "_x"),
						RawTerm.fold(x.second.second, "_x"))));

		Parser<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>> o_eq = Parsers.or(o_eq_old,
				o_eq_from_p_eq);

		Parser<Pair<Token, List<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>>>> o_eqs = Parsers
				.tuple(token("observation_equations"), o_eq.many());
		Parser<List<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>>> o_eqs0 = o_eqs.map(x -> x.b);

		Parser<Tuple4<List<catdata.Pair<Integer, Quad<String, String, String, String>>>, List<catdata.Pair<Integer, catdata.Pair<List<String>, List<String>>>>, List<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>>, List<catdata.Pair<String, String>>>> pa = Parsers
				.tuple(entities.optional(), p_eqs0.optional(), o_eqs0.optional(), options);

		Parser<Tuple4<Token, List<LocStr>, Token, TyExp>> l = Parsers.tuple(
				Parsers.or(token("coproduct"), token("quotient")), locstr.sepBy(token("+")), token(":"), ty_ref.lazy());

		Parser<Tuple4<List<catdata.Pair<Integer, Quad<String, String, String, String>>>, List<catdata.Pair<Integer, catdata.Pair<List<String>, List<String>>>>, List<catdata.Pair<Integer, Quad<String, String, RawTerm, RawTerm>>>, List<catdata.Pair<String, String>>>> ppp = Parsers
				.tuple(token("{"), pa, token("}")).map(x -> x.b).optional();

		Parser<ColimSchExpQuotient> ret = Parsers.tuple(l, ppp).map(x -> {
			TyExp ty = x.a.d;
			List<LocStr> nodes = x.a.b;

			if (x.b == null) {
				return new ColimSchExpQuotient(ty, nodes, Collections.emptyList(), Collections.emptyList(),
						Collections.emptyList(), Collections.emptyList());
			}
			return new ColimSchExpQuotient(ty, nodes, Util.newIfNull(x.b.a), Util.newIfNull(x.b.c),
					Util.newIfNull(x.b.b), x.b.d);

		});

		return ret;
	}

	private static Parser<EdsExp> edsExpRaw() {
		Parser<SchExp> l = Parsers.tuple(token("literal"), token(":"), sch_ref.lazy(), token("{")).map(x -> x.c);

		return Parsers.tuple(l, imports(eds_ref.lazy()),
				Parsers.tuple(Parsers.INDEX, edExpRaw()).map(x -> new catdata.Pair<>(x.a, x.b)).many(), options,
				token("}")).map(x -> new EdsExpRaw(x.a, x.b, x.c, x.d));
	}

	private static Parser<EdsExp> edsExp() {
		Parser<EdsExp> var = ident.map(EdsExpVar::new),
				empty = Parsers.tuple(token("empty"), token(":"), sch_ref.lazy())
						.map(x -> new EdsExpRaw(x.c, Collections.emptyList(), Collections.emptyList(), Unit.unit)),
				fc = Parsers.tuple(token("fromSchema"), sch_ref.lazy()).map(x -> new EdsExpSch(x.b)),

				raw = edsExpRaw();
		Parser<EdsExp> ret = Parsers.or(var, fc, empty, raw);
		eds_ref.set(ret);
		return ret;
	}

	private static Parser<EdExpRaw> edExpRaw() {
		Parser<List<catdata.Pair<LocStr, String>>> as = Parsers.tuple(token("forall"), env(ident, ":")).map(x -> x.b)
				.optional();

		Parser<catdata.Pair<List<catdata.Pair<LocStr, String>>, Boolean>> es = Parsers
				.tuple(token("exists"), token("unique").optional(), env(ident, ":"))
				.map(x -> new catdata.Pair<>(x.c, x.b != null)).optional();

		Parser<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>> eq = Parsers
				.tuple(Parsers.INDEX, Parsers.tuple(term(), token("="), term()).map(x -> new catdata.Pair<>(x.a, x.c)))
				.map(x -> new catdata.Pair<>(x.a, x.b));

		Parser<List<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>>> eqs = Parsers
				.tuple(token("where"), eq.many()).map(x -> x.b).optional();

		Parser<EdExpRaw> ret = Parsers.tuple(as, eqs, token("->"), es, eqs)
				.map(x -> new EdExpRaw(Util.newIfNull(x.a), Util.newIfNull(x.b),
						x.d == null ? Collections.emptyList() : x.d.first, Util.newIfNull(x.e),
						x.d == null ? false : x.d.second));
		return ret;
	}

	private static Parser<InstExpRandom> instExpRand() {
		Parser<List<catdata.Pair<LocStr, String>>> generators = Parsers
				.tuple(token("generators"), env(IntegerLiteral.PARSER, "->")).map(x -> x.b);

		Parser<Tuple4<Token, Token, SchExp, Token>> l = Parsers.tuple(token("random"), token(":"), sch_ref.lazy(),
				token("{"));

		Parser<InstExpRandom> ret = Parsers.tuple(l, generators, options, token("}"))
				.map(x -> new InstExpRandom(x.a.c, x.b, x.c));

		return ret;
	}

	private static Parser<InstExpRaw> instExpRaw() {
		Parser<List<catdata.Pair<LocStr, String>>> generators = Parsers.tuple(token("generators"), env(ident, ":"))
				.map(x -> x.b);

		Parser<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>> eq = Parsers
				.tuple(Parsers.INDEX, Parsers.tuple(term(), token("="), term()))
				.map(x -> new catdata.Pair<>(x.a, new catdata.Pair<>(x.b.a, x.b.c)));

		Parser<List<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>>> eqs = Parsers
				.tuple(token("equations"), eq.many()).map(x -> x.b);

		Parser<List<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>>> table = Parsers.tuple(Parsers.INDEX, Parsers
				.tuple(ident, token("->"), token("{"), Parsers.tuple(term(), term()).sepBy(token(",")), token("}")))
				.map(x -> {
					List<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>> ret = new ArrayList<>(x.b.d.size());
					for (Pair<RawTerm, RawTerm> y : x.b.d) {
						ret.add(new catdata.Pair<>(x.a,
								new catdata.Pair<>(new RawTerm(x.b.a, Collections.singletonList(y.a)), y.b)));
					}
					return ret;
				});
		Parser<List<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>>> tables = Parsers
				.tuple(token("multi_equations"), table.many()).map(x -> Util.concat(x.b));

		Parser<Tuple5<List<InstExp<?, ?, ?, ?>>, List<catdata.Pair<LocStr, String>>, List<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>>, List<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>>, List<catdata.Pair<String, String>>>> pa = Parsers
				.tuple(imports(inst_ref.lazy()), generators.optional(), eqs.optional(), tables.optional(), options);

		Parser<Tuple4<Token, Token, SchExp, Token>> l = Parsers.tuple(token("literal"), token(":"), sch_ref.lazy(),
				token("{"));

		Parser<InstExpRaw> ret = Parsers.tuple(l, pa, token("}")).map(x -> new InstExpRaw(x.a.c, Util.newIfNull(x.b.a),
				Util.newIfNull(x.b.b), Util.append(Util.newIfNull(x.b.c), Util.newIfNull(x.b.d)), x.b.e));

		return ret;
	}

	private static Parser<catdata.Pair<LocStr, PreBlock>> preblock(boolean isSimple) {
		Parser<List<catdata.Pair<LocStr, String>>> fr = Parsers.tuple(token("from"), env(ident, ":")).map(x -> x.b);

		Parser<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>> eq = Parsers
				.tuple(Parsers.INDEX, Parsers.tuple(term(), token("="), term()).map(x -> new catdata.Pair<>(x.a, x.c)))
				.map(x -> new catdata.Pair<>(x.a, x.b));

		Parser<List<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>>> wh = Parsers
				.tuple(token("where"), eq.many()).map(x -> x.b);

		Parser<Pair<Boolean, List<catdata.Pair<LocStr, RawTerm>>>> atts = Parsers
				.tuple(token("attributes"), token("*").optional(),
						Parsers.tuple(locstr, token("->"), term()).map(x -> new catdata.Pair<>(x.a, x.c)).many())
				.map(x -> new Pair<>(x.b == null ? false : true, x.c));

		Parser<List<catdata.Pair<LocStr, Trans>>> fks = Parsers
				.tuple(token("foreign_keys"),
						Parsers.tuple(locstr, token("->"), trans()).map(x -> new catdata.Pair<>(x.a, x.c)).many())
				.map(x -> x.b);

		Parser<LocStr> lp = Parsers.tuple(token("entity"), locstr.followedBy(token("->"))).map(x -> x.b);
		if (isSimple) {
			lp = Parsers.tuple(Parsers.INDEX, Parsers.constant("")).map(x -> new LocStr(x.a, x.b));
		}
		Parser<Tuple3<LocStr, Tuple4<List<catdata.Pair<LocStr, String>>, List<catdata.Pair<Integer, catdata.Pair<RawTerm, RawTerm>>>, Pair<Boolean, List<catdata.Pair<LocStr, RawTerm>>>, List<catdata.Pair<LocStr, Trans>>>, List<catdata.Pair<String, String>>>> ret2 = Parsers
				.tuple(lp.followedBy(token("{")),
						Parsers.tuple(fr.optional(), wh.optional(), atts.optional(), fks.optional()),
						options.followedBy(token("}")));

		Parser<catdata.Pair<LocStr, PreBlock>> ret = ret2.map(x -> new catdata.Pair<>(x.a,
				new PreBlock(Util.newIfNull(x.b.a), Util.newIfNull(x.b.b),
						x.b.c == null ? Collections.emptyList() : Util.newIfNull(x.b.c.b), Util.newIfNull(x.b.d),
						Util.newIfNull(x.c), x.b.c == null ? false : x.b.c.a)));

		return ret;
	}

	private static void queryExp() {
		@SuppressWarnings({ "unchecked" })
		Parser<QueryExp> var = ident.map(QueryExpVar::new),
				deltaQueryEval = Parsers
						.tuple(token("toQuery"), map_ref.lazy(), options.between(token("{"), token("}")).optional())
						.map(x -> new QueryExpDeltaEval(x.b, Util.newIfNull(x.c))),
				deltaQueryCoEval = Parsers
						.tuple(token("toCoQuery"), map_ref.lazy(), options.between(token("{"), token("}")).optional())
						.map(x -> new QueryExpDeltaCoEval(x.b, Util.newIfNull(x.c))),

				comp = Parsers
						.tuple(token("["), query_ref.lazy(), token(";"), query_ref.lazy().followedBy(token("]")),
								options.between(token("{"), token("}")).optional())
						.map(x -> new QueryExpCompose(x.b, x.d, Util.newIfNull(x.e))),

				fromCoSpan = Parsers
						.tuple(token("fromCoSpan"), map_ref.lazy(), map_ref.lazy(),
								options.between(token("{"), token("}")).optional())
						.map(x -> new QueryExpFromCoSpan(x.b, x.c, Util.newIfNull(x.d))),

				id = Parsers.tuple(token("identity"), sch_ref.lazy()).map(x -> new QueryExpId(x.b)),
				id2 = Parsers.tuple(token("include"), sch_ref.lazy(), sch_ref.lazy())
						.map(x -> new QueryExpId(x.b, x.c)),
				fromConstraints = Parsers.tuple(token("fromConstraints"), ident, eds_ref.lazy())
						.map(x -> new QueryExpFromEds(x.c, Integer.parseInt(x.b))),

				ret = Parsers.or(id, id2, fromCoSpan, fromConstraints, queryExpRaw(), queryExpRawSimple(), var,
						deltaQueryEval, deltaQueryCoEval, comp, parens(query_ref));

		query_ref.set(ret);
	}

	private static void colimSchExp() {
		Parser<List<catdata.Pair<LocStr, SchExp>>> nodes = Parsers.tuple(token("nodes"), env(sch_ref.lazy(), "->"))
				.map(x -> x.b);

		Parser<List<catdata.Pair<LocStr, MapExp>>> edges = Parsers.tuple(token("edges"), env(map_ref.lazy(), "->"))
				.map(x -> x.b);

		Parser<Tuple4<Object, List<catdata.Pair<LocStr, SchExp>>, List<catdata.Pair<LocStr, MapExp>>, List<catdata.Pair<String, String>>>> pa = Parsers
				.tuple(Parsers.always(), nodes.optional(), edges.optional(), options);

		Parser<Tuple5<Token, GraphExp, Token, TyExp, Token>> l = Parsers.tuple(token("literal"), graph_ref.lazy(),
				token(":"), ty_ref.lazy(), token("{"));

		Parser<ColimSchExp> ret = Parsers.tuple(l, pa, token("}")).map(x -> {

			// schema graph nodes edges options imports
			return new ColimSchExpRaw(x.a.b, x.a.d, Util.newIfNull(x.b.b), Util.newIfNull(x.b.c), x.b.d);
		});

		Parser<ColimSchExp> ret2 = ident.map(x -> new ColimSchExpVar(x)),
				ret3 = Parsers.tuple(token("wrap"), colim_ref.lazy(), map_ref.lazy(), map_ref.lazy())
						.map(x -> new ColimSchExpWrap(x.b, x.c, x.d));

		Parser<ColimSchExp> retX = Parsers.or(ret, ret2, ret3, colimExpModify(), colimSchExpQuotient(),
				parens(colim_ref));

		colim_ref.set(retX);

	}

	private static Parser<InstExpColim<String, String, String, String>> colimInstExp() {
		Parser<List<catdata.Pair<LocStr, InstExp<?, ?, ?, ?>>>> nodes = Parsers
				.tuple(token("nodes"), env(inst_ref.lazy(), "->")).map(x -> x.b);

		Parser<List<catdata.Pair<LocStr, TransExp<?, ?, ?, ?, ?, ?, ?, ?>>>> edges = Parsers
				.tuple(token("edges"), env(trans_ref.lazy(), "->")).map(x -> x.b);

		Parser<Tuple4<String, List<catdata.Pair<LocStr, InstExp<?, ?, ?, ?>>>, List<catdata.Pair<LocStr, TransExp<?, ?, ?, ?, ?, ?, ?, ?>>>, List<catdata.Pair<String, String>>>> pa = Parsers
				.tuple(Parsers.always(), nodes.optional(), edges.optional(), options);

		Parser<Tuple4<Token, GraphExp, SchExp, Token>> l = Parsers.tuple(token("colimit"), graph_ref.lazy(),
				sch_ref.lazy(), token("{")); // .map(x ->
												// x.c);

		@SuppressWarnings({ "rawtypes", "unchecked" })
		Parser<InstExpColim<String, String, String, String>> ret = Parsers.tuple(l, pa, token("}")).map(x -> {

			// schema graph nodes edges options imports
			return new InstExpColim(x.a.b, x.a.c,

					Util.newIfNull(x.b.b), Util.newIfNull(x.b.c), x.b.d);
		});

		return ret;
	}

	private static Parser<QueryExpRawSimple> queryExpRawSimple() {
		Parser<Tuple5<Token, Token, SchExp, Integer, catdata.Pair<LocStr, PreBlock>>> l = Parsers.tuple(token("simple"),
				token(":"), sch_ref.lazy(), Parsers.INDEX, preblock(true));

		Parser<QueryExpRawSimple> ret = l.map(x -> new QueryExpRawSimple(x.c, x.d, x.e.second));

		return ret;
	}

	@SuppressWarnings("rawtypes")
	private static Parser<InstExpQueryQuotient> queryQuotientExpRaw() {

		Parser<Pair<List<catdata.Pair<LocStr, PreBlock>>, List<catdata.Pair<String, String>>>> pa = Parsers
				.tuple(preblock(false).many(), options);

		Parser<Tuple3<Token, InstExp<?, ?, ?, ?>, Token>> l = Parsers.tuple(token("quotient_query"), inst_ref.lazy(),
				token("{"));

		@SuppressWarnings({ "unchecked", })
		Parser<InstExpQueryQuotient> ret = Parsers.tuple(l, pa, token("}"))
				.map(x -> new InstExpQueryQuotient(x.a.b, x.b.a, x.b.b));

		return ret;
	}

	private static Parser<QueryExpRaw> queryExpRaw() {
		Parser<Pair<List<catdata.Pair<LocStr, String>>, List<catdata.Pair<LocStr, RawTerm>>>> q = Parsers.tuple(
				Parsers.tuple(token("params"), env(ident, ":")).map(x -> x.b).optional(),
				Parsers.tuple(token("bindings"), env(term(), "=")).map(x -> x.b).optional());

		Parser<Tuple4<Pair<List<catdata.Pair<LocStr, String>>, List<catdata.Pair<LocStr, RawTerm>>>, List<QueryExp>, List<catdata.Pair<LocStr, PreBlock>>, List<catdata.Pair<String, String>>>> pa = Parsers
				.tuple(q, imports(query_ref.lazy()), preblock(false).many(), options);

		Parser<Tuple5<Token, Token, SchExp, SchExp, Token>> l = Parsers.tuple(token("literal"), token(":"),
				sch_ref.lazy().followedBy(token("->")), sch_ref.lazy(), token("{"));

		Parser<QueryExpRaw> ret = Parsers.tuple(l, pa, token("}")).map(x -> new QueryExpRaw(Util.newIfNull(x.b.a.a),
				Util.newIfNull(x.b.a.b), x.a.c, x.a.d, x.b.b, Util.newIfNull(x.b.c), Util.newIfNull(x.b.d)));

		return ret;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Parser<InstExpCoEq> instExpCoEq() {
		Parser<InstExpCoEq> ret = Parsers
				.tuple(token("coequalize"), trans_ref.lazy(), trans_ref.lazy(),
						options.between(token("{"), token("}")).optional())
				.map(x -> new InstExpCoEq(x.b, x.c, Util.newIfNull(x.d)));

		return ret;
	}

	private static Parser<InstExpJdbc> instExpJdbc() {
		Parser<Pair<List<catdata.Pair<LocStr, String>>, List<catdata.Pair<String, String>>>> qs = Parsers
				.tuple(env(ident, "->"), options).between(token("{"), token("}"));

		Parser<InstExpJdbc> ret = Parsers
				.tuple(token("import_jdbc"), ident.followedBy(token(":")), sch_ref.lazy(), qs)
				.map(x -> new InstExpJdbc(x.c, x.d.b, x.b, x.d.a));
		return ret;
	}

	private static Parser<InstExpJdbcAll> instExpJdbcAll() {
		Parser<InstExpJdbcAll> ret = Parsers
				.tuple(token("import_jdbc_all"), ident, options.between(token("{"), token("}")).optional())
				.map(x -> new InstExpJdbcAll(x.b, Util.newIfNull(x.c)));

		return ret;
	}

	// TODO: aql reverse order on arguments env
	private static Parser<MapExpRaw> mapExpRaw() {

		Parser<List<String>> p1 = ident.sepBy1(token("."));
		Parser<List<String>> p2 = token("identity").map(x -> Collections.emptyList());

		Parser<List<catdata.Pair<LocStr, List<String>>>> fks = Parsers
				.tuple(token("foreign_keys"), env(Parsers.or(p1, p2), "->")).map(x -> x.b);

		Parser<Tuple5<Token, String, Pair<Token, String>, Token, RawTerm>> lp0 = Parsers.tuple(token("lambda"), ident,
				Parsers.tuple(token(":"), ident).optional(), token("."), term());

		Parser<Tuple5<Token, String, Pair<Token, String>, Token, RawTerm>> lq = ident.sepBy1(token(".")).map(x -> {
			RawTerm term = RawTerm.fold(x, "_x");
			return new Tuple5<>(null, "_x", new Pair<>(null, null), null, term);
		});

		Parser<Tuple5<Token, String, Pair<Token, String>, Token, RawTerm>> lp = Parsers.or(lp0, lq);

		Parser<List<catdata.Pair<LocStr, Triple<String, String, RawTerm>>>> envp = env(
				lp.map(x -> new Triple<>(x.b, x.c == null ? null : x.c.b, x.e)), "->");

		Parser<List<catdata.Pair<LocStr, Triple<String, String, RawTerm>>>> atts = Parsers
				.tuple(token("attributes"), envp).map(x -> x.b);

		Parser<Tuple3<List<MapExp>, List<catdata.Pair<LocStr, Triple<String, List<catdata.Pair<LocStr, List<String>>>, List<catdata.Pair<LocStr, Triple<String, String, RawTerm>>>>>>, List<catdata.Pair<String, String>>>> pa = Parsers
				.tuple(imports(map_ref.lazy()), Parsers
						.tuple(token("entity"), locstr.followedBy(token("->")), ident, fks.optional(), atts.optional())
						.map(x -> new catdata.Pair<>(x.b, new Triple<>(x.c, Util.newIfNull(x.d), Util.newIfNull(x.e))))
						.many(), options);

		Parser<Tuple5<Token, Token, SchExp, SchExp, Token>> l = Parsers.tuple(token("literal"), token(":"),
				sch_ref.lazy().followedBy(token("->")), sch_ref.lazy(), token("{"));

		Parser<MapExpRaw> ret = Parsers.tuple(l, pa, token("}"))
				.map(x -> new MapExpRaw(x.a.c, x.a.d, x.b.a, x.b.b, x.b.c));

		return ret;
	}

	private static Parser<ColimSchExpModify> colimExpModify() {
		Parser<List<catdata.Pair<LocStr, String>>> ens = Parsers
				.tuple(token("rename").followedBy(token("entities")), env(ident, "->")).map(x -> x.b);

		Parser<List<catdata.Pair<catdata.Pair<String, LocStr>, String>>> fks0 = Parsers
				.tuple(token("rename").followedBy(token("foreign_keys")), env2(ident, "->")).map(x -> x.b);

		Parser<List<catdata.Pair<catdata.Pair<String, LocStr>, String>>> atts0 = Parsers
				.tuple(token("rename").followedBy(token("attributes")), env2(ident, "->")).map(x -> x.b);

		Parser<List<catdata.Pair<catdata.Pair<String, LocStr>, List<String>>>> fks = Parsers
				.tuple(token("remove").followedBy(token("foreign_keys")), env2(ident.sepBy1(token(".")), "->"))
				.map(x -> x.b);

		Parser<List<catdata.Pair<catdata.Pair<String, LocStr>, Triple<String, String, RawTerm>>>> envp = env2(
				Parsers.tuple(token("lambda"), ident, Parsers.tuple(token(":"), ident).optional(), token("."), term())
						.map(x -> new Triple<>(x.b, x.c == null ? null : x.c.b, x.e)),
				"->");

		Parser<List<catdata.Pair<catdata.Pair<String, LocStr>, Triple<String, String, RawTerm>>>> atts = Parsers
				.tuple(token("remove").followedBy(token("attributes")), envp).map(x -> x.b);

		Parser<Tuple3<List<catdata.Pair<LocStr, String>>, List<catdata.Pair<catdata.Pair<String, LocStr>, String>>, List<catdata.Pair<catdata.Pair<String, LocStr>, String>>>> pa = Parsers
				.tuple(ens.optional(), fks0.optional(), atts0.optional());

		Parser<Tuple3<List<catdata.Pair<catdata.Pair<String, LocStr>, List<String>>>, List<catdata.Pair<catdata.Pair<String, LocStr>, Triple<String, String, RawTerm>>>, List<catdata.Pair<String, String>>>> pb = Parsers
				.tuple(fks.optional(), atts.optional(), options);

		Parser<Tuple3<Token, ColimSchExp, Token>> l = Parsers.tuple(token("modify"), colim_ref.lazy(), token("{")); // .map(x
																													// ->

		Parser<ColimSchExpModify> ret = Parsers.tuple(l, pa, pb, token("}"))
				.map(x -> new ColimSchExpModify(x.a.b, Util.newIfNull(x.b.a), Util.newIfNull(x.b.b), // x.b.b
						Util.newIfNull(x.b.c), Util.newIfNull(x.c.a), Util.newIfNull(x.c.b), x.c.c));// x.c.a

		return ret;
	}

	private static Parser<TransExpRaw> transExpRaw() {
		Parser<List<catdata.Pair<LocStr, RawTerm>>> gens1 = Parsers.tuple(token("generators"), env(term(), "->"))
				.map(x -> x.b);

		Parser<Tuple3<List<TransExp<?, ?, ?, ?, ?, ?, ?, ?>>, List<catdata.Pair<LocStr, RawTerm>>, List<catdata.Pair<String, String>>>> pa = Parsers
				.tuple(imports(trans_ref.lazy()), gens1.optional(), options);

		Parser<Tuple5<Token, Token, InstExp<?, ?, ?, ?>, InstExp<?, ?, ?, ?>, Token>> l = Parsers.tuple(
				token("literal"), token(":"), inst_ref.lazy().followedBy(token("->")), inst_ref.lazy(), token("{")); // .map(x
																														// ->
																														// x.c);

		Parser<TransExpRaw> ret = Parsers.tuple(l, pa, token("}"))
				.map(x -> new TransExpRaw(x.a.c, x.a.d, x.b.a, Util.newIfNull(x.b.b), x.b.c));

		return ret;
	}

	private static Parser<Trans> trans() {
		Parser<List<catdata.Pair<LocStr, RawTerm>>> gens = env(term(), "->");
		// Parser<List<catdata.Pair<LocStr, RawTerm>>> gens2 =
		// env_backwards(term(), "<-");

		// Parser<List<catdata.Pair<LocStr, RawTerm>>> gens = Parsers.or(gens2,
		// gens1);

		Parser<Pair<List<catdata.Pair<LocStr, RawTerm>>, List<catdata.Pair<String, String>>>> pa = Parsers
				.tuple(gens.optional(), options);

		Parser<Trans> ret = Parsers.tuple(token("{"), pa, token("}")).map(x -> new Trans(Util.newIfNull(x.b.a), x.b.b));

		return ret;
	}

	private static <Y> Parser<Triple<String, Integer, Y>> decl(String s, Parser<Y> p) {
		return Parsers.tuple(token(s), Parsers.INDEX, ident, token("="), p).map(x -> new Triple<>(x.c, x.b, x.e));
	}

	private static final Reference<GraphExp> graph_ref = Parser.newReference();
	private static final Reference<TyExp> ty_ref = Parser.newReference();
	private static final Reference<SchExp> sch_ref = Parser.newReference();
	private static final Reference<ColimSchExp> colim_ref = Parser.newReference();
	private static final Reference<PragmaExp> pragma_ref = Parser.newReference();
	private static final Reference<InstExp<?, ?, ?, ?>> inst_ref = Parser.newReference();
	private static final Reference<MapExp> map_ref = Parser.newReference();
	private static final Reference<TransExp<?, ?, ?, ?, ?, ?, ?, ?>> trans_ref = Parser.newReference();
	private static final Reference<QueryExp> query_ref = Parser.newReference();
	private static final Reference<EdsExp> eds_ref = Parser.newReference();

	private static Parser<Program<Exp<?>>> program(String s) {
		tyExp();
		schExp();
		instExp();
		mapExp();
		transExp();
		graphExp();
		queryExp();
		pragmaExp();
		colimSchExp();
		edsExp();

		@SuppressWarnings("unchecked")
		Parser<Triple<String, Integer, ? extends Exp<?>>> p = Parsers.or(comment(), decl("typeside", ty_ref.get()),
				decl("schema", sch_ref.get()), decl("instance", inst_ref.get()), decl("mapping", map_ref.get()),
				decl("transform", trans_ref.get()), decl("graph", graph_ref.get()), decl("query", query_ref.get()),
				decl("command", pragma_ref.get()), decl("schema_colimit", colim_ref.get()),
				decl("constraints", eds_ref.get()));

		return Parsers.tuple(options, p.many())
				.map(x -> new Program((x.b), s, x.a, q -> ((Exp) q).kind().toString()));
	}

	private static Parser<Triple<String, Integer, ? extends Exp<?>>> comment() {
		Parser<Triple<String, Integer, ? extends Exp<?>>> p1 = Parsers
				.tuple(token("html"), token("{").followedBy(token("(*")), StringLiteral.PARSER, Parsers.INDEX,
						token("*)").followedBy(token("}")))
				.map(x -> new Triple<>("html" + x.d, x.d, new CommentExp(x.c, false)));

		Parser<Triple<String, Integer, ? extends Exp<?>>> p2 = Parsers
				.tuple(token("md"), token("{").followedBy(token("(*")), StringLiteral.PARSER, Parsers.INDEX,
						token("*)").followedBy(token("}")))
				.map(x -> new Triple<>("md" + x.d, x.d, new CommentExp(x.c, true)));

		return p1.or(p2);
	}

	public Program<Exp<?>> parseProgram(Reader r) throws ParseException, IOException {
		return parseProgram(Util.readFile(r));
	}

	public Program<Exp<?>> parseProgram(String s) throws ParseException {
		try {
			return program(s).from(TOKENIZER, IGNORED).parse(s);
		} catch (ParserException e) {
			throw new ParseException(e.getLocation().column, e.getLocation().line, e);
		}
	}

	public Triple<List<catdata.Pair<String, String>>, RawTerm, RawTerm> parseEq(String s) throws ParseException {
		try {
			return Parsers.or(eq1, eq2).from(TOKENIZER, IGNORED).parse(s);
		} catch (ParserException e) {
			throw new ParseException(e.getLocation().column, e.getLocation().line, e);
		}
	}

	public catdata.Pair<List<catdata.Pair<String, String>>, RawTerm> parseTermInCtx(String s) throws ParseException {
		try {
			return Parsers.or(term1, term2).from(TOKENIZER, IGNORED).parse(s);
		} catch (ParserException e) {
			throw new ParseException(e.getLocation().column, e.getLocation().line, e);
		}
	}

	public RawTerm parseTermNoCtx(String s) throws ParseException {
		try {
			return term().from(TOKENIZER, IGNORED).parse(s);
		} catch (ParserException e) {
			throw new ParseException(e.getLocation().column, e.getLocation().line, e);
		}
	}

	static Parser<Kind> kind() {
		Parser<Kind> p = Parsers.fail("Not a kind");
		for (Kind k : Kind.values()) {
			if (!k.equals(Kind.COMMENT)) {
				p = Parsers.or(p, token(k.toString().toLowerCase()).map(x->k));
			}
		}
		return p;
	}
	public static Quad<Kind, String, String, String> parseInfer(String s) {
		Parser<Quad<Kind, String, String, String>> p = Parsers
				.tuple(kind(), ident, token("=").followedBy(token("literal")).followedBy(token(":")), ident.followedBy(token("->")), ident)
				.map(x -> new catdata.Quad<>(x.a, x.b, x.d, x.e));
		return p.from(TOKENIZER, IGNORED).parse(s);
	}

	public static String parseInfer1(String s) {
		Parser<String> p = Parsers.tuple(token("literal"), token(":"), ident).map(x -> x.c);
		return p.from(TOKENIZER, IGNORED).parse(s);
	}

	private static final Parser<Triple<List<catdata.Pair<String, String>>, RawTerm, RawTerm>> eq1 = Parsers
			.tuple(token("forall"), ctx.followedBy(token(".")), term(), token("="), term())
			.map(x -> new Triple<>(x.b, x.c, x.e));

	private static final Parser<Triple<List<catdata.Pair<String, String>>, RawTerm, RawTerm>> eq2 = Parsers
			.tuple(term(), token("="), term())
			.map(x -> new Triple<>(Collections.<catdata.Pair<String, String>>emptyList(), x.a, x.c));

	private static final Parser<catdata.Pair<List<catdata.Pair<String, String>>, RawTerm>> term1 = Parsers
			.tuple(token("lambda"), ctx.followedBy(token(".")), term()).map(x -> new catdata.Pair<>(x.b, x.c));

	private static final Parser<catdata.Pair<List<catdata.Pair<String, String>>, RawTerm>> term2 = term()
			.map(x -> new catdata.Pair<>(Collections.<catdata.Pair<String, String>>emptyList(), x));

	@Override
	public Collection<String> getReservedWords() {
		return Util.union(Util.list(IAqlParser.ops), Util.list(IAqlParser.res));
	}

	@Override
	public Collection<String> getOperations() {
		return Util.list(IAqlParser.ops);
	}

	// TODO: aql visitor for aql exps?

}
