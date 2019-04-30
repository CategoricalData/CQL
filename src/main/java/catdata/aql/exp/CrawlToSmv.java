package catdata.aql.exp;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CrawlToSmv {
	
	public static void main(String[] args) {
		Crawl counter = makeIncDecCounter(3);
		System.out.println("Crawl is:\n" + counter);
		String nusmv = counter.toSmv();
		System.out.println("\n\nNuSMV is: " + nusmv);
	}
	
	public static enum SmvType { STR, INT, BOOL }
	
	public static String printSmvType(SmvType t) {
		switch (t) {
		case BOOL:
			return "boolean";
		case INT:
			return "integer";
		case STR:
			return "string";	
		};
		throw new RuntimeException();
	}
	
	public static Object defaultSmvValue(SmvType t) {
		switch (t) {
		case BOOL:
			return false;
		case INT:
			return 0;
		case STR:
			return "";	
		};
		throw new RuntimeException();
	}
	
	public static Object printSmvValue(SmvType t, Object o) {
		switch (t) {
		case BOOL:
			return o.toString();
		case INT:
			return o.toString();
		case STR:
			return o;	
		};
		throw new RuntimeException();
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static class Crawl {
		public final String name;
		public final int numStates;
		public final Map<Integer, Set<Integer>> next; //0..numStates
		public final int init;
		public final Map<String, SmvType> vars;
		public final Map<Integer, Map<String, Object>> current;
		
		
		
		/**
		 * Data for a web crawl.
		 * Uses -1 for finished state and -2 for error state.
		 * 
		 * @param name String name of crawl
		 * @param numStates int number of nodes in graph
		 * @param next Map<Integer, Set<Integer>> next relation 0..numStates
		 * @param init int initial node in graph
		 * @param vars Map<String, SmvType> the schema of the data to attach to each node
		 * @param current Map<Integer, Map<String, Object>> the data for each node 0..numStates
		 */
		public Crawl(String name, Map<String, SmvType> vars, int numStates, int init, 
				Map<Integer, Map<String, Object>> current, Map<Integer, Set<Integer>> next) {
			if (init < 0 || init >= numStates) {
				throw new RuntimeException();
			}
			this.name = name;
			this.numStates = numStates;
			this.init = init;
			this.next = next;
			this.vars = vars;
			this.current = current;
			this.next.put(-1, Collections.singleton(-1)); // finished -> finished
			this.next.put(-2, Collections.singleton(-2)); // error -> error
			this.current.put(-1, new HashMap<>());
			this.current.put(-2, new HashMap<>());
			for (String var : this.vars.keySet()) {
				SmvType type = this.vars.get(var);
				Object o = defaultSmvValue(type);
				this.current.get(-1).put(var, o); // terminal state gets default data
				this.current.get(-2).put(var, o); // error state gets default data
			}
			this.next.putAll(next);
			this.current.putAll(current);
		}
		
		public String toSmv() {
			StringBuffer sb = new StringBuffer("MODULE ");
			sb.append(name);
			sb.append("\nVAR");
			sb.append("\n\tstate: -2 .. "); // -1 = finished, -2 = error
			sb.append(numStates);
			sb.append(";");
			
			for (String var : vars.keySet()) {
				SmvType type = vars.get(var);
				sb.append("\n\t");
				sb.append(var);
				sb.append(" : ");
				sb.append(printSmvType(type));
				sb.append(";");
			}
		    sb.append("\nINIT state = ");
		    sb.append(init);  
		    sb.append(";\nASSIGN\n");

		    for (String var : vars.keySet()) {
		    	sb.append(var);
		    	SmvType type = vars.get(var);
		    	sb.append(" := case");
		    	for (int state = -2; state < numStates; state++) {
			    	sb.append("\n\tstate = ");
			    	sb.append(state);
			    	sb.append(" : ");
			    	sb.append(printSmvValue(type, current.get(state).get(var)));
			    	sb.append(";");
		    	}
		    	sb.append("\nesac;");
		    }
		    sb.append("\nnext(state) := case ");
		    for (int state = -2; state < numStates; state++) {
		    	sb.append("\n\tstate = ");
		    	sb.append(state);
		    	sb.append(" : {");
		    	Set<Integer> nextStates = next.get(state);
		    	if (nextStates.isEmpty()) {
		    		throw new RuntimeException("No transitions out of state " + state);
		    	}
		    	boolean first = true;
		    	for (Integer nextState : nextStates) {
		    		if (!first) {
		    			sb.append(", ");
		    		}
		    		sb.append(nextState);
		    		first = false;
		    	}
		    	sb.append("};");
	    	}
		    sb.append("\nesac;");
		    return sb.toString();
		}

		@Override
		public String toString() {
			return "Crawl [\nname=" + name + "\nnumStates=" + numStates + "\ninit=" + init + "\nvars="
					+ vars + "\ncurrent=" + current + "\nnext=" + next + "\n]";
		}	
		
	}
		
	///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Idealized output of +1 -1 counter web crawl 
	 * @param depth
	 * @return perfect binary tree of given depth as Crawl
	 */
	public static Crawl makeIncDecCounter(int depth) {
		if (depth < 0 || depth > 32) {
			throw new RuntimeException();
		}
		int numStates = (int) Math.pow(2, depth) - 1;
		Map<Integer, Set<Integer>> next = new HashMap<>(); //0..numStates
		Map<Integer, Map<String, Object>> current = new HashMap<>();
		
		for (int i = 0; i < numStates; i++) {
			Set<Integer> lr = new HashSet<>();
			if (2*i+1 < numStates) {
				lr.add(2*i + 1);
				lr.add(2*i + 2);
			} else {
				lr.add(-1); //success
			}
			next.put(i, lr);
			if (i == 0) {
				current.put(i, Collections.singletonMap("N", 0));
			} 
			int n = (int) current.get(i).get("N");
			if (2*i+1 < numStates) {
				current.put(2*i+1, Collections.singletonMap("N", n-1));
				current.put(2*i+2, Collections.singletonMap("N", n+1));
			}
		}
		System.out.println("[current] needed to be generated by crawl: " + current);
		System.out.println("[next] needed to be generated by crawl: " + next);
		Crawl pbt = new Crawl("Counter", Collections.singletonMap("N", SmvType.INT), numStates, 0, current, next);
		
		return pbt;
	}
}
