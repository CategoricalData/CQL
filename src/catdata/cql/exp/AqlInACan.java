package catdata.cql.exp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catdata.Pair;
import catdata.ParseException;
import catdata.Program;
import catdata.Util;
import catdata.cql.Instance;
import catdata.cql.Kind;
import catdata.cql.gui.CqlViewer;
import catdata.ide.Example;
import catdata.ide.Examples;
import catdata.ide.Language;

//TODO aql merge aqldoc with aqlinacan
//TODO: aql have this execute pragmas?
public class AqlInACan {


  public static void main(String... args) {
    try {
      // Util.writeFile(makeHtml(), "tryAql.html");
      System.out.println(openCan(args[0]));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  public static String openCan(String can) {
    try {
      Program<Exp<?>> program = AqlParserFactory.getParser().parseProgram(can);
      String html = "<!DOCTYPE html><html><head><link rel=\"StyleSheet\" href=\"https://dua.azureedge.net/nstyle.css\" type=\"text/css\" media=\"all\" >\r\n"
      		+ "			<link rel=\"icon\" type=\"image/png\" href=\"https://categoricaldata.net/favicon.ico\" ><script src=\"http://categoricaldata.net/js/simple.js\"></script><title>Result</title></head><body><h1>Result</h1>";
      AqlEnv env = new AqlEnv(program);
      env.typing = new AqlTyping(program, false);
      for (String n : program.order) {
        Exp<?> exp = program.exps.get(n);
        if (exp.kind() == Kind.PRAGMA) {
          throw new RuntimeException("Commands disabled in web-CQL");
        }
        Object o = Util.timeout(() -> exp.eval(env, false), 10 * 1000, "Error in " + n + ": "); // hardcode timeout, do not exec pragmas
        env.defs.put(n, exp.kind(), o);
        if (exp.kind().equals(Kind.INSTANCE)) {
          html += "<p><h2>" + n + " = </h2>" + toHtml(env, (Instance<String, String, Sym, Fk, Att, String, String, ?, ?>) o)
              + " </p><br><hr>";
        } else {
        	   html += "<p><h2>" + n + " = </h2>" + toHtmlPre(o.toString())
               + " </p><br><hr>";
        }
      }
      return html + "</body></html>";
    } catch (Throwable ex) {
      ex.printStackTrace();
      return "ERROR " + ex.getMessage();
    }
  }

  
  private static String toHtmlPre(String string) {
	return "<pre>" + string + "</pre>";
}


private static int i = 0;

  public static <X, Y> String toHtml(@SuppressWarnings("unused") AqlEnv env,
      Instance<String, String, Sym, Fk, Att, String, String, X, Y> I) {
    String ret = "<div>";

    Map<String, Pair<List<String>, Object[][]>> tables = CqlViewer.makeEnTables(I.algebra(), false, 256,
        new HashMap<>());

    for (String t : Util.alphabetical(tables.keySet())) {
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
