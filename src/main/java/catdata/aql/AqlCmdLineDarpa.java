package catdata.aql;

import java.io.FileReader;
import java.util.List;
import java.util.Map;

import catdata.Chc;
import catdata.Program;
import catdata.Triple;
import catdata.Util;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlMultiDriver;
import catdata.aql.exp.AqlParserFactory;
import catdata.aql.exp.Exp;

/**
 * Program entry point for Fred.
 */
public class AqlCmdLineDarpa {

	public static <Ty, En, Sym, Fk, Att> String schemaToSql(Schema<Ty, En, Sym, Fk, Att> S) {
		// prefix, type of ID, ID col name , truncater, printer, varchar length
		Map<En, Triple<List<Chc<Fk, Att>>, List<String>, List<String>>> sch_sql = S.toSQL("", "Integer", "ID",
				false,255, "\"");

		// (k,q,f) where q is a bunch of drops and then adds and f is the adding of
		// constraints and
		String sch = "";
		// drop if exists, then create
		for (En en : S.ens) {
			Triple<List<Chc<Fk, Att>>, List<String>, List<String>> t = sch_sql.get(en);
			sch += Util.sep(t.second, "\n");
			sch += "\n";
		}
		// add constraints
		for (En en1 : S.ens) {
			Triple<List<Chc<Fk, Att>>, List<String>, List<String>> t = sch_sql.get(en1);
			sch += Util.sep(t.third, "\n");
			sch += "\n";
		}

		return sch.trim();
	}

	public static <Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> String queryToSql(
			Query<Ty, En1, Sym, Fk1, Att1, En2, Fk2, Att2> q) {
		// use "char" for mysql, "varchar" for H2
		Map<En2, String> Q = q.unnest().toSQLViews("", "", "ID", "char", "\"").second; // must unnest
		String ret = "";
		for (En2 en2 : q.dst.ens) {
			ret += "INSERT INTO " + en2 + " " + Q.get(en2);
			ret += ";\n\n";
		}
		return ret.trim();
	}

	public static void main(String[] args) {
		try (FileReader r = new FileReader(args[0])) {
			Program<Exp<?>> prog = AqlParserFactory.getParser().parseProgram(r);

			AqlMultiDriver driver = new AqlMultiDriver(prog, null);
			driver.start();
			AqlEnv last_env = driver.env;
			if (last_env.exn != null) {
				throw last_env.exn;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
