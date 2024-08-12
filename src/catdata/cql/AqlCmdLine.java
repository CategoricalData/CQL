package catdata.cql;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

import catdata.Program;
import catdata.Util;
import catdata.cql.exp.AqlEnv;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.AqlTyping;
import catdata.cql.exp.Exp;
import catdata.cql.fdm.ToJdbcPragmaQuery;

class AqlCmdLine {

  public static void main(String... args) {
    try {
      System.out.println(openCan(args[0]));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  
	public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> String queryToSql(
			Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q) {
		// use "char" for mysql, "varchar" for H2
		Map<En2, String> Q = ToJdbcPragmaQuery.toSQLViews("", "", "ID", "varchar", "", q.unnest(), 255).second; // must
																												// unnest
		String ret = "";
		for (En2 en2 : q.dst.ens) {
			ret += "INSERT INTO " + en2 + " " + Q.get(en2);
			ret += ";\n\n";
		}
		return ret.trim();
	}
	
  private static String openCan(String can) {
    try {
      String s = Util.readFile(new FileReader(new File(can)));
      Program<Exp<?>> program = AqlParserFactory.getParser().parseProgram(s);
      AqlEnv env = new AqlEnv(program);
      env.typing = new AqlTyping(program, false);
      AqlMultiDriver d = new AqlMultiDriver(program, env);
      
      
      d.start();
      
      if (env.exn != null)	{
    	  env.exn.printStackTrace();
    	  return env.exn.getMessage();
      }
      
      env = d.env;

      
      String html = "";
      for (String n : program.order) {
        Exp<?> exp = program.exps.get(n);
        Object val = env.get(exp.kind(), n);
        if (val == null) {
          html += exp.kind() + " " + n + " = no result for " + n + "\n\n";
        } else if (exp.kind().equals(Kind.QUERY)) {
          html += exp.kind() + " " + n + " = " + queryToSql((Query)val) + "\n\n";
        } else {
           // html += exp.kind() + " " + n + " ok\n";
          }
      }
      return html.trim();
    } catch (Throwable ex) {
      ex.printStackTrace();
      return "ERROR " + ex.getMessage();
    }
  }

}
