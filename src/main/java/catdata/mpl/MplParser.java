package catdata.mpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jparsec.Parser;
import org.jparsec.Parser.Reference;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Terminals.Identifier;
import org.jparsec.Terminals.IntegerLiteral;
import org.jparsec.Terminals.StringLiteral;
import org.jparsec.Token;
import org.jparsec.functors.Tuple3;
import org.jparsec.functors.Tuple4;
import org.jparsec.functors.Tuple5;

import catdata.Pair;
import catdata.Program;
import catdata.Quad;
import catdata.Triple;
import catdata.mpl.Mpl.MplExp;
import catdata.mpl.Mpl.MplExp.MplEval;
import catdata.mpl.Mpl.MplExp.MplSch;
import catdata.mpl.Mpl.MplExp.MplVar;
import catdata.mpl.Mpl.MplTerm;
import catdata.mpl.Mpl.MplTerm.MplAlpha;
import catdata.mpl.Mpl.MplTerm.MplComp;
import catdata.mpl.Mpl.MplTerm.MplConst;
import catdata.mpl.Mpl.MplTerm.MplId;
import catdata.mpl.Mpl.MplTerm.MplLambda;
import catdata.mpl.Mpl.MplTerm.MplPair;
import catdata.mpl.Mpl.MplTerm.MplRho;
import catdata.mpl.Mpl.MplTerm.MplSym;
import catdata.mpl.Mpl.MplTerm.MplTr;
import catdata.mpl.Mpl.MplType;
import catdata.mpl.Mpl.MplType.MplBase;
import catdata.mpl.Mpl.MplType.MplProd;
import catdata.mpl.Mpl.MplType.MplUnit;

@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
class MplParser {

	static final Parser<Integer> NUMBER = IntegerLiteral.PARSER.map(Integer::valueOf);

	private static final String[] ops = new String[] { ",", ".", ";", ":", "{", "}", "(", ")", "=", "->", "+", "*", "^",
			"|", "?", "@" };

	private static final String[] res = new String[] { "sym", "tr", "id", "I", "theory", "eval", "sorts", "symbols",
			"equations", "lambda1", "lambda2", "rho1", "rho2", "alpha1", "alpha2" };

	private static final Terminals RESERVED = Terminals.caseSensitive(ops, res);

	private static final Parser<Void> IGNORED = Parsers
			.or(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();

	private static final Parser<?> TOKENIZER = Parsers.or((Parser<?>) StringLiteral.DOUBLE_QUOTE_TOKENIZER,
			RESERVED.tokenizer(), (Parser<?>) Identifier.TOKENIZER, (Parser<?>) IntegerLiteral.TOKENIZER);

	private static Parser<?> term(String... names) {
		return RESERVED.token(names);
	}

	private static Parser<?> ident() {
		return Parsers.or(StringLiteral.PARSER, Identifier.PARSER);
	}

	private static final Parser<?> program = program().from(TOKENIZER, IGNORED);

	private static Parser<?> program() {
		return Parsers.tuple(decl().source().peek(), decl()).many();
	}

	private static Parser<?> exp() {
		Reference ref = Parser.newReference();

		Parser<?> eval = Parsers.tuple(term("eval"), ident(), term());

		Parser<?> a = Parsers.or(new Parser[] { theory(), eval });

		ref.set(a);

		return a;
	}

	private static Parser<?> eq() {
		return Parsers.tuple(term(), term("="), term());
	}

	private static Parser<?> term() {
		Reference ref = Parser.newReference();

		Parser<?> prod = Parsers.tuple(term("("), ref.lazy(), term("*"), ref.lazy(), term(")"));
		Parser<?> comp = Parsers.tuple(term("("), ref.lazy(), term(";"), ref.lazy(), term(")"));
		Parser<?> alpha1 = Parsers.tuple(term("alpha1"), type(), type(), type());
		Parser<?> alpha2 = Parsers.tuple(term("alpha2"), type(), type(), type());
		Parser<?> lambda1 = Parsers.tuple(term("lambda1"), type());
		Parser<?> lambda2 = Parsers.tuple(term("lambda2"), type());
		Parser<?> rho1 = Parsers.tuple(term("rho1"), type());
		Parser<?> rho2 = Parsers.tuple(term("rho2"), type());
		Parser<?> id = Parsers.tuple(term("id"), type());
		Parser<?> tr = Parsers.tuple(term("tr"), ref.lazy());
		Parser<?> sym = Parsers.tuple(term("sym"), type(), type());

//		Parser<?> rho = Parsers.tuple(term("("), ref.lazy(), term("*"), ref.lazy(), term(")"));

		Parser<?> a = Parsers.or(sym, tr, id, ident(), prod, comp, alpha1, alpha2, lambda1, lambda2, rho1, rho2);

		ref.set(a);

		return a;

	}

	private static Parser<?> type() {
		Reference ref = Parser.newReference();

		Parser<?> prod = Parsers.tuple(term("("), ref.lazy(), term("*"), ref.lazy(), term(")"));

		Parser<?> a = Parsers.or(new Parser[] { term("I"), ident(), prod });

		ref.set(a);

		return a;
	}

	private static Parser<?> theory() {
		Parser<?> z1 = Parsers.tuple(type(), term("->"), type());

		Parser<?> p = Parsers.tuple(ident().sepBy1(term(",")), term(":"), z1);
		Parser<?> foo = Parsers.tuple(section("sorts", ident()), section("symbols", p), section("equations", eq()));
		return Parsers.tuple(Parsers.constant("theory"),
				Parsers.between(term("theory").followedBy(term("{")), foo, term("}")));
	}

	private static Parser<?> decl() {
		Parser p1 = Parsers.tuple(ident(), term("="), exp());

		return Parsers.or(p1);
	}

	public static Parser<?> section2(String s, Parser<?> p) {
		return Parsers.tuple(term(s), p, term(";"));
	}

	public static Program<MplExp<String, String>> program(String s) {
		List<Quad<String, Integer, MplExp<String, String>, Integer>> ret = new LinkedList<>();
		List decls = (List) program.parse(s);

		for (Object d : decls) {
			org.jparsec.functors.Pair pr = (org.jparsec.functors.Pair) d;
			Tuple3 decl = (Tuple3) pr.b;

			toProgHelper(pr.a.toString(), s, ret, decl);
		}

		return new Program<>(ret, null);
	}

	private static MplExp<String, String> toExp(Object o) {
		if (o instanceof String) {
			return new MplVar<>((String) o);
		}
		if (o instanceof Tuple3) {
			Tuple3 t = (Tuple3) o;
			return new MplEval<>((String) t.b, toTerm(t.c));
		}
		if (o instanceof org.jparsec.functors.Pair) {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) o;
			if (p.a.toString().equals("theory")) {
				return toTheory(p.b);
			}
			MplEval<String, String> ret = new MplEval<>((String) p.a, toTerm(p.b));
			return ret;
		}
		return toTheory(o);
	}

	private static void toProgHelper(String txt, String s, List<Quad<String, Integer, MplExp<String, String>, Integer>> ret,
			Tuple3 decl) {
		int idx = s.indexOf(txt);
		if (idx < 0) {
			throw new RuntimeException();
		}

		String name = decl.a.toString();
		ret.add(new Quad<>(name, idx, toExp(decl.c), idx));
	}

	private static MplTerm<String, String> toTerm(Object o) {
		if (o instanceof Tuple5) {
			Tuple5 t = (Tuple5) o;
			if (t.c.toString().equals(";")) {
				return new MplComp<>(toTerm(t.b), toTerm(t.d));
			}
			if (t.c.toString().equals("*")) {
				return new MplPair<>(toTerm(t.b), toTerm(t.d));
			}
		}
		if (o instanceof Tuple4) {
			Tuple4 t = (Tuple4) o;
			if (t.a.toString().equals("alpha1")) {
				return new MplAlpha<>(toType(t.b), toType(t.c), toType(t.d), true);
			}
			if (t.a.toString().equals("alpha2")) {
				return new MplAlpha<>(toType(t.b), toType(t.c), toType(t.d), false);
			}
		}
		if (o instanceof Tuple3) {
			Tuple3 t = (Tuple3) o;
			if (t.a.toString().equals("sym")) {
				return new MplSym<>(toType(t.b), toType(t.c));
			}
		}
		if (o instanceof org.jparsec.functors.Pair) {
			org.jparsec.functors.Pair p = (org.jparsec.functors.Pair) o;
			if (p.a.toString().equals("id")) {
				return new MplId(toType(p.b));
			}
			if (p.a.toString().equals("lambda1")) {
				return new MplLambda<>(toType(p.b), true);
			}
			if (p.a.toString().equals("lambda2")) {
				return new MplLambda<>(toType(p.b), false);
			}
			if (p.a.toString().equals("rho1")) {
				return new MplRho<>(toType(p.b), true);
			}
			if (p.a.toString().equals("rho2")) {
				return new MplRho<>(toType(p.b), false);
			}
			if (p.a.toString().equals("tr")) {
				return new MplTr<>(toTerm(p.b));
			}
		}
		if (o instanceof String) {
			return new MplConst<>((String) o);
		}

		throw new RuntimeException(o.toString());

	}

	private static MplType<String> toType(Object o) {
		if (o instanceof String) {
			return new MplBase<>((String) o);
		}
		if (o instanceof Token) {
			return new MplUnit<>();
		}
		Tuple5 t = (Tuple5) o;
		return new MplProd<>(toType(t.b), toType(t.d));
	}

	private static MplExp toTheory(Object o) {
		Tuple3 t = (Tuple3) o;

		Tuple3 a = (Tuple3) t.a;
		Tuple3 b = (Tuple3) t.b;
		Tuple3 c = (Tuple3) t.c;

		Set<String> sorts = new HashSet<>((Collection<String>) a.b);

		List<Tuple3> symbols0 = (List<Tuple3>) b.b;
		List<Tuple3> equations0 = (List<Tuple3>) c.b;

		Map<String, Pair<MplType<String>, MplType<String>>> symbols = new HashMap<>();
		for (Tuple3 x : symbols0) {
			MplType<String> dom;
			MplType<String> args;
			Tuple3 zzz = (Tuple3) x.c;
			args = toType(zzz.a);
			dom = toType(zzz.c);

			List<String> name0s = (List<String>) x.a;
			for (String name : name0s) {

				if (symbols.containsKey(name)) {
					throw new DoNotIgnore("Duplicate symbol " + name);
				}
				symbols.put(name, new Pair<>(args, dom));
			}
		}

		Set<Pair<MplTerm<String, String>, MplTerm<String, String>>> equations = new HashSet<>();
		for (Tuple3 eq : equations0) {
			MplTerm<String, String> lhs = toTerm(eq.a);
			MplTerm<String, String> rhs = toTerm(eq.c);
			equations.add(new Pair<>(lhs, rhs));
		}

		return new MplSch<>(sorts, symbols, equations);
	}

	public static class DoNotIgnore extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public DoNotIgnore(String string) {
			super(string);
		}

	}

	private static Parser<?> section(String s, Parser<?> p) {
		return Parsers.tuple(term(s), p.sepBy(term(",")), term(";"));
	}

	/*
	 * private static Parser<?> string() { return
	 * Parsers.or(Terminals.StringLiteral.PARSER, Terminals.IntegerLiteral.PARSER,
	 * Terminals.Identifier.PARSER); }
	 */

}
