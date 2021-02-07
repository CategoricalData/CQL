package catdata.aql;

import java.io.File;
import java.io.FileReader;

import catdata.Program;
import catdata.Util;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlMultiDriver;
import catdata.aql.exp.AqlParserFactory;
import catdata.aql.exp.AqlTyping;
import catdata.aql.exp.Exp;

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
			
			String html = "";
			for (String n : program.order) {
				Exp<?> exp = program.exps.get(n);
				Object val = env.get(exp.kind(), n);
				if (val == null) {
					html += exp.kind() + " " + n + " = no result for " + n;
				} else {
					html += exp.kind() + " " + n + " = " + val + "\n\n";
				}
			}
			return html.trim();
		} catch (Throwable ex) {
			ex.printStackTrace();
			return "ERROR " + ex.getMessage();
		}
	}

}
