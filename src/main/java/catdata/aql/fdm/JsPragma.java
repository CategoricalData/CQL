package catdata.aql.fdm;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import catdata.Util;
import catdata.aql.AqlJs;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Pragma;
import catdata.aql.exp.AqlEnv;
import gnu.trove.map.hash.THashMap;

public class JsPragma extends Pragma {

	private final List<String> jss;
	
	private final List<String> responses = new LinkedList<>();
	
	private final AqlEnv env;
		
	private final AqlOptions options; 
	
	public JsPragma(List<String> jss, Map<String, String> o, AqlEnv env) {
		this.jss = jss;
		this.env = env;
		this.options = new AqlOptions(o, null, env.defaults);
	}

	@Override
	public void execute() {
		List<String> ret = new LinkedList<>();
		String e = (String) options.getOrDefault(AqlOption.js_env_name);
		Map<String, Object> m = new THashMap<>();
		m.put(e, env);
		for (String js : jss) {
			try {
				@SuppressWarnings("deprecation")
				Object o = AqlJs.exec(js, m);
				ret.add(js + (o == null ? "" : " : " + o));
			} catch (Exception ex) {
				ex.printStackTrace();
				ret.add(js + " : " + ex.getMessage());
			}
		}
		responses.add(Util.sep(ret, "\n"));
	}
	
	@Override
	public String toString() {
		return Util.sep(responses, "\n\n--------------\n\n");
	}
		
}

