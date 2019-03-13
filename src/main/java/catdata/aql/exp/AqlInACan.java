package catdata.aql.exp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catdata.Pair;
import catdata.ParseException;
import catdata.Program;
import catdata.Util;
import catdata.aql.Instance;
import catdata.aql.Kind;
import catdata.aql.gui.AqlViewer;
import catdata.ide.Example;
import catdata.ide.Examples;
import catdata.ide.Language;

//TODO aql merge aqldoc with aqlinacan
//TODO: aql have this execute pragmas?
class AqlInACan {

	private static String quote(String s) {
		s = s.replace("\\", "\\" + "\\"); // \ --> \\
		s = s.replace("\"", "\\\""); // " --> \"
		s = s.replace("\n", "\\n"); // <LF> --> \n
		return s;
	}

	// TODO: CQL skipping feature will have to change
	private static boolean skip(Example e) {
		try {
			Program<Exp<?>> program = AqlParserFactory.getParser().parseProgram(e.getText());
			for (String n : program.order) {
				Exp<?> exp = program.exps.get(n);
				if (exp.kind() == Kind.PRAGMA || exp.kind() == Kind.COMMENT) {
					return true;
				}
			}
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		return false;
	}


	@SuppressWarnings("unused")
	private static String makeHtml() {
		String s = "";
		String t = "";
		for (Example ex : Util.alphabetical(Examples.getExamples(Language.CQL))) {
			if (skip(ex)) {
				continue;
			}
			s += "\nif (v == \"" + ex.getName() + "\") { document.getElementById('code').innerHTML = \""
					+ quote(ex.getText()) + "\" }";
			t += "\n<option value = \"" + ex.getName() + "\">" + ex.getName() + "</option>";
		}
		String html = "<html>" + "\n<head>" + "\n<title>Try CQL</title>" + "\n</head>" + "\n<script>"
				+ "\nfunction setExample(t) {" + "\n    var v = t.value;" + "\n" + s + "\n}; " + "\n</script>"
				+ "\n<body>" + "Choose example:" + "\n<select name=\"example\" onChange = \"setExample(this);\">"
				+ "\n<option disabled selected value> -- select an option -- </option>" + "\n" + t + "\n</select>"
				+ "\n<br>" + "\nEnter CQL code here:" + "\n<form action=\"cgi-bin/try.cgi\""
				+ "\n      method=\"POST\">" + "\n<textarea name=\"code\" id=\"code\" cols=80 rows=40>"
				+ "\n</textarea>" + "\n<br>" + "\n<input type=\"submit\" value=\"Run\">" + "\n</form>" + "\n</body>"
				+ "\n</html>" + "\n";
		return html;
	}

	public static void main(String... args) {
		try {
		//Util.writeFile(makeHtml(), "tryAql.html");
		System.out.println(openCan(args[0]));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private static String openCan(String can) {
		try {
			Program<Exp<?>> program = AqlParserFactory.getParser().parseProgram(can);
			String html = "<html><head><script src=\"http://categoricaldata.net/js/simple.js\"></script><title>Result</title></head><body>\n\n";
			AqlEnv env = new AqlEnv(program);
			env.typing = new AqlTyping(program, false);
			for (String n : program.order) {
				Exp<?> exp = program.exps.get(n);
				if (exp.kind() == Kind.PRAGMA) {
					throw new RuntimeException("Commands disabled in web-CQL");
				}
				Object o = Util.timeout(() -> exp.eval(env, false), 10 * 1000); // hardcode timeout, do not exec pragmas
				env.defs.put(n, exp.kind(), o);
				if (exp.kind().equals(Kind.INSTANCE)) {
					html += "<p><h2>" + n + " =\n</h2>" + toHtml(env, (Instance<Ty, En, Sym, Fk, Att, Gen, Sk, ?, ?>) o)
							+ "\n</p><br><hr>\n";
				}
				
			}
			return html + "\n\n</body></html>";
		} catch (Throwable ex) {
			ex.printStackTrace();
			return "ERROR " + ex.getMessage();
		}
	}

	private static int i = 0;

	public static <X, Y> String toHtml(@SuppressWarnings("unused") AqlEnv env, Instance<Ty, En, Sym, Fk, Att, Gen, Sk, X, Y> I) {
		String ret = "<div>";

		Map<En, Pair<List<String>, Object[][]>> tables = AqlViewer.makeEnTables(I.algebra(), false, 256, new HashMap<>()); 

		for (En t : Util.alphabetical(tables.keySet())) {
			ret += "<table id=\"table" + i
					+ "\" style=\"float: left; border: 1px solid black; padding: 5px; border-collapse: collapse; margin-right:10px\" border=\"1\"  cellpadding=\"3\">";
			ret += "<caption><b>" + t.toString() + "</b></caption>";
			List<String> cols = tables.get(t).first;
			cols.remove(0);
			cols.add(0, "ID");
			Object[][] rows = tables.get(t).second;
			ret += "<tr>";
			int j = 0;
			for (String col : cols) {
				ret += "<th onclick=\"sortTable('table" + i + "', " + j + ")\">" + col + "</th>";
				j++;
			}
			ret += "</tr>";
			for (Object[] row : rows) {
				ret += "<tr>";
				for (Object col : row) {
					// System.ou
					ret += "<td>" + strip(col.toString()) + "</td>";
				}
				ret += "</tr>";
			}
			ret += "</table>";
			i++;
		}
		return ret + "</div><br style=\"clear:both;\"/>";
	}

	/*
	 * 
	 * & becomes &amp; < becomes &lt; > becomes &gt;
	 */
	public static String strip(String s) {
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		// s = s.replace("[", "");
		// s = s.replace("]", "");
		// s = s.replace("|", "");
		return s;
	}

}
