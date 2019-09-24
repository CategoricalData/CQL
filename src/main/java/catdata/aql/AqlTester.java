package catdata.aql;

import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import catdata.Program;
import catdata.Util;
import catdata.aql.exp.AqlEnv;
import catdata.aql.exp.AqlMultiDriver;
import catdata.aql.exp.AqlParserFactory;
import catdata.aql.exp.Exp;
import catdata.ide.CodeTextPanel;
import catdata.ide.Example;
import catdata.ide.Examples;
import catdata.ide.Language;
import gnu.trove.map.hash.THashMap;

public class AqlTester {

	static String message = "This self-test will run all the built-in CQL examples and check for exceptions.  This test cannot be interupted.  This window will disappear for a while. Continue?";

	public static void doSelfTests() {
		int c = JOptionPane.showConfirmDialog(null, message, "Run Self-Test?", JOptionPane.YES_NO_OPTION);
		if (c != JOptionPane.YES_OPTION) {
			return;
		}
		Map<String, String> exs = new THashMap<>();
		for (Example e : Examples.getExamples(Language.CQL)) {
			if (e.getName().equals("TutorialTSP") || e.getName().equals("QuickSQL")) {
				continue;
			}
			exs.put(e.getName(), e.getText());
		}
		Map<String, Throwable> result = runMany(exs);
		if (result.isEmpty()) {
			JOptionPane.showMessageDialog(null, "OK: Tests Passed");
			return;
		}
		JTabbedPane t = new JTabbedPane();
		for (String k : result.keySet()) {
			t.addTab(k, new CodeTextPanel("Error", result.get(k).getMessage()));
		}
		JOptionPane.showMessageDialog(null, t);
	}

	private static Map<String, Throwable> runMany(Map<String, String> progs) {
		Map<String, Throwable> result = new THashMap<>();
		// int i = 0;
		for (String k : Util.alphabetical(progs.keySet())) {

			try {
				System.out.println(k);
				Program<Exp<?>> prog = AqlParserFactory.getParser().parseProgram(progs.get(k));
				AqlMultiDriver driver = new AqlMultiDriver(prog, null);
				driver.start(); // blocks
				AqlEnv env = driver.env;
				if (env.exn != null) {
					result.put(k, env.exn);
				}
				// Thread.sleep(3000);
			} catch (Throwable ex) {
				ex.printStackTrace();
				result.put(k, ex);
			}
		}
		return result;
	}

}
