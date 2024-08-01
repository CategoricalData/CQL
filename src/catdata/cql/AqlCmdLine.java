package catdata.cql;

import java.io.File;
import java.io.FileReader;

import catdata.Program;
import catdata.Util;
import catdata.cql.exp.AqlEnv;
import catdata.cql.exp.AqlMultiDriver;
import catdata.cql.exp.AqlParserFactory;
import catdata.cql.exp.AqlTyping;
import catdata.cql.exp.Exp;

class AqlCmdLine {

  public static void main(String... args) {
    try {
      System.out.println(openCan(args[0]));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
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
        } else {
          html += exp.kind() + " " + n + " ok\n";
        }
      }
      return html.trim();
    } catch (Throwable ex) {
      ex.printStackTrace();
      return "ERROR " + ex.getMessage();
    }
  }

}
