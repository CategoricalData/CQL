package catdata.aql.exp;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.text.BadLocationException;

import org.apache.commons.lang3.text.WordUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.Parser;

import catdata.Chc;
import catdata.ParseException;
import catdata.Program;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Pragma;

public class AqlStatic extends AbstractParser {

	private static String truncate(String w) {
		w = w.substring(0, Integer.min(80 * 80, w.length()));
		w = WordUtils.wrap(w, 80);
		return w;
//		
//		List<String> s = w.lines().map(x -> x.substring(0, Integer.min(80, x.length()))).collect(Collectors.toList());
//		return Util.sep(s.subList(0, Integer.min(s.size(), 80)), "\n");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized void validate() {
		Set<String> done = new TreeSet<>(exns.keySet());
		for (String n : env.prog.order) {
			if (exns.containsKey(n)) {
				continue;
			}
			Exp exp = env.prog.exps.get(n);
			long l = (long) exp.getOrDefault(env, AqlOption.static_timeout);
			try {
				Util.timeout(() -> {
					Optional<Chc<String, ?>> oo = exp.eval_static(env, exns);
					if (oo.isEmpty()) {
						if (exp instanceof InstExpQueryQuotient) {
							try {
								exp.eval0(env, true);
							} catch (IgnoreException ex) {
							}
						}
						exns.put(n, Optional.empty());
					} else {
						Chc<String, ?> o = oo.get();
						if (o.left) {
							exns.put(n, Optional.of(o.l));
						} else {
							if (exp instanceof PragmaExpCheck2) {
								((Pragma) o.r).asPragma().execute();
							}
							env.defs.put(n, exp.kind(), o.r);
						}
					}

					return Unit.unit;
				}, l * 1000);
			} catch (Exception ex) {
				if (ex.getMessage() == null) {
					ex.printStackTrace();
				}
				ex.printStackTrace();
				exns.put(n, Optional.of(ex.getMessage() == null ? "Null pointer exception." : ex.getMessage()));
			}

			for (String k : exns.keySet()) {
				Optional<String> text = exns.get(k);
				if (text.isEmpty() || done.contains(k)) {
					continue;
				}
				try {
					int z = area.getLineOfOffset(env.prog.lines.get(k));
					z = Integer.min(z, area.getLineCount());
					result.addNotice(new StaticParserNotice(this, text.get(), z, Color.magenta));
				} catch (BadLocationException ex) {
					ex.printStackTrace();
				}

			}
		}
	}

	public synchronized void typeCheck() {
		env.typing = new AqlTyping(env.prog);
		for (String n : env.prog.order) {
			try {
				Exp<?> exp = env.prog.exps.get(n);
				Chc<String, Object> o = exp.type0(env.typing, exns);
				if (o.left) {
					exns.put(n, Optional.of(o.l));
					continue;
				}
				env.typing.defs.put(n, exp.kind(), o.r);
			} catch (Throwable ex) {
				if (ex.getMessage() == null) {
					ex.printStackTrace();
				}
				exns.put(n, Optional.of(ex.getMessage() == null ? "Null pointer exception" : ex.getMessage()));
			}
		}
		for (String k : exns.keySet()) {
			try {
				int z = area.getLineOfOffset(env.prog.lines.get(k));
				z = Integer.min(z, area.getLineCount());

				result.addNotice(new StaticParserNotice(this, exns.get(k).get(), z, Color.red));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private boolean spPrev = true;

	public final DefaultParseResult result;
	private final RSyntaxTextArea area;
	public final JCheckBox box;

	@Override
	public synchronized DefaultParseResult parse(RSyntaxDocument doc1, String style) {
		boolean spNow = box.isSelected();
		if (!spNow) {
			env = null;
			exns = Util.mk();
			result.clearNotices();
			return result;
		}
		if (spNow != spPrev) {
			spPrev = spNow;
			return parse(doc1, style);
		}

		spPrev = spNow;
		return result;
	}

	public synchronized void doIt(String program) {
		env = null;
		exns = Util.mk();
		try {
			Program<Exp<?>> p = AqlParserFactory.getParser().parseProgram(program);
			env = new AqlEnv(p);
		} catch (ParseException exn) {
			DefaultParserNotice notice = new StaticParserNotice(this, exn.getMessage(), exn.line, Color.red);
			result.addNotice(notice);
			return;
		}
		result.clearNotices();
		area.forceReparsing(this);
		typeCheck();
		area.forceReparsing(this);
		validate();
		area.forceReparsing(this);
	}

	public AqlEnv env;
	Map<String, Optional<String>> exns = Util.mk();

	public AqlStatic(RSyntaxTextArea area, JCheckBox box) {
		this.box = box;
		this.area = area;
		this.result = new DefaultParseResult(this);
		result.clearNotices();
		area.forceReparsing(this);
	}

	private static class StaticParserNotice extends DefaultParserNotice {
		Color c;

		public StaticParserNotice(Parser parser, String msg, int line, Color c) {
			super(parser, truncate(msg), line);
			this.c = c;
		}

		@Override
		public Color getColor() {
			return c;
		}
	}
}
