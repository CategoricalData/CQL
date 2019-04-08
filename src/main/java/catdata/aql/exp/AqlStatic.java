package catdata.aql.exp;

import java.awt.Color;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;

import catdata.Chc;
import catdata.Pair;
import catdata.Program;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Pragma;
import catdata.aql.gui.AqlCodeEditor.StaticParserNotice;

public class AqlStatic extends AbstractParser {

	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void validate(RSyntaxTextArea area) {
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
								boolean ok = true;
								for (Object o : exp.deps()) {
									Pair<String, Kind> z = (Pair<String, Kind>) o;
									if (null == env.get(z.second, z.first)) {
										ok = false;
										break;
									}
								}
								if (ok) {
									exp.eval0(env, true);
								}
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

	public void typeCheck(RSyntaxTextArea area) {
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

	public final DefaultParseResult result;
	
	@Override
	public synchronized DefaultParseResult parse(RSyntaxDocument doc1, String style) {
		return result;
		
	}

	public final AqlEnv env;
	public final Map<String, Optional<String>> exns = Util.mk();

	public AqlStatic(Program<Exp<?>> p) {
		this.result = new DefaultParseResult(this);
		this.env = new AqlEnv(p);
	}

	@Override
	public String toString() {
		return "AqlStatic [result=" + result + ", env=" + env + ", exns=" + exns + "]";
	}
	
	
	
}
