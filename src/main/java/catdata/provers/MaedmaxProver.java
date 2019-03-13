
package catdata.provers;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import catdata.Util;
import gnu.trove.set.hash.THashSet;

public class MaedmaxProver<T, C, V> extends DPKB<T, C, V>  {
	
	
	@Override 
	public void finalize() {
		try {
			writer.println("exit");
			writer.close();
			reader.close();
			proc.destroyForcibly();
		} catch (Exception ex) {
			try {
	
			} catch (Exception ex2) {
				
			}
		}
	}

	private final Process proc;
	private final BufferedReader reader;
	private final PrintWriter writer;
	private final File g;
	
	//done elsewhere for convenience
	//TODO CQL empty sorts check
	public MaedmaxProver(String exePath, KBTheory<T,C,V> th, boolean allowEmptySorts, long seconds) {
		super(th);
		
		File f = new File(exePath);
		if (!f.exists()) {
			throw new RuntimeException("File does not exist: " + exePath);
		}
		
		if (!allowEmptySorts) {
			Set<T> es = new THashSet<>();
			th.inhabGen(es);
			if (!es.equals(th.tys)) {
				throw new RuntimeException("Sorts " + Util.sep(Util.diff(th.tys, es), ", ")
						+ " have no ground terms (consider allow_empty_sorts_unsafe = true).");
			}
		}
		
		try {
			g = File.createTempFile("AqlMaedmax", ".tptp");
			Util.writeFile(th.tptp_cnf(), g.getAbsolutePath());
			//System.out.println(g.getAbsolutePath());
			
			String str = exePath + " -T " + seconds + " --interactive " + g.getAbsolutePath();
			proc = Runtime.getRuntime().exec(str);
			
			
			reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			writer = new PrintWriter(proc.getOutputStream());
			
			String line = reader.readLine();
			//System.out.println(line);
			line = reader.readLine();
			//System.out.println(line);
			
			
			if (line == null) {
				throw new RuntimeException("Call to maedmax yields null, process is alive: " + proc.isAlive() + ".  Command: " + str);
			} // else if (!line.equals("OK")) {
		//		throw new RuntimeException("Maedmax error: " + line);
		//	}
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public synchronized boolean eq(Map<V, T> ctx, KBExp<C, V> lhs, KBExp<C, V> rhs) {
		if (!ctx.keySet().equals(Util.union(lhs.getVars(), rhs.getVars()))) {
			throw new RuntimeException("Maedmax does not currently support contexts.");
		}
		writer.println(kb.convert(lhs) + " = " + kb.convert(rhs));
		writer.flush();
		try {
			reader.readLine(); //enter ... :
			String line = reader.readLine();
			
			if ("YES".equals(line)) {
				return true;
			} else if ("NO".equals(line)) {
				return false;
			}
			throw new RuntimeException("Maedmax error on " + lhs + " = " + rhs + ", " + line + " is not YES or NO.\n\n" + kb.convert(lhs) + " = " + kb.convert(rhs) + "\n\n" + g.getAbsolutePath() );
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	
	@Override
	public String toString() {
		return "Maedmax prover";
	}

	@Override
	public void add(C c, T t) {
		throw new RuntimeException("Maedmax does not support fresh constants.");
	}

}
