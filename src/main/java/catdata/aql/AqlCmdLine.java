package catdata.aql;

import java.io.File;
import java.io.FileReader;

import catdata.Program;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.exp.AqlEnv;
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
			String html = "";
			for (String n : program.order) {
				Exp<?> exp = program.exps.get(n);
				Object val = Util.timeout(() -> exp.eval(env, false),
						(Long) exp.getOrDefault(env, AqlOption.timeout) * 1000);
				if (val == null) {
					throw new RuntimeException("anomaly, please report: null result on " + exp);
				} else if (exp.kind().equals(Kind.PRAGMA)) {
					((Pragma) val).execute();
				}
				env.defs.put(n, exp.kind(), val);
				html += exp.kind() + " " + n + " = " + val + "\n\n";
			}
			return html.trim();
		} catch (Throwable ex) {
			ex.printStackTrace();
			return "ERROR " + ex.getMessage();
		}
	}

}
