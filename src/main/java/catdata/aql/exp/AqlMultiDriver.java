package catdata.aql.exp;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.collections4.list.TreeList;

import catdata.IntRef;
import catdata.LineException;
import catdata.LocException;
import catdata.Pair;
import catdata.Program;
import catdata.RuntimeInterruptedException;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Pragma;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

//TODO aql does assume unique names
//TODO aql make sure transforms validate
public final class AqlMultiDriver implements Callable<Unit> {

	public void abort() {
		interruptAll();
		exn.add(new RuntimeException(
				"Execution interrupted while waiting.  If execution was not aborted manually, please report."));
	}

	private void interruptAll() {
		for (Thread t : threads) {
			t.interrupt();
		}
	}

	private static <X> Collection<Pair<String, Kind>> wrapDeps(String s, Exp<X> exp, Program<Exp<?>> prog) {
		Collection<Pair<String, Kind>> ret = (new THashSet<>(exp.deps()));
		for (String s0 : prog.order) {
			if (s.equals(s0)) {
				break;
			}
			// each expression depends on all the pragmas before it
			if (prog.exps.get(s0).kind().equals(Kind.PRAGMA)) {
				ret.add(new Pair<>(s0, Kind.PRAGMA));
			}
			// each pragma depends on all expressions before it
			if (exp.kind().equals(Kind.PRAGMA)) {
				ret.add(new Pair<>(s0, prog.exps.get(s0).kind()));
			}
		}
		return ret;
	}

	// n is unchanged if it is equal to old(n) and for every dependency d,
	// unchanged(d)
	// this means that expressions such as 'load from disk' will need to be
	// careful about equality

	@Override
	public String toString() {
		long f = Runtime.getRuntime().freeMemory();
		long t = Runtime.getRuntime().totalMemory();
		long u = t - f;
		if (env.fl > f) {
			env.fl = f;
		}
		if (env.mh < t) {
			env.mh = t;
		}
		if (env.uh < u) {
			env.uh = u;
		}
	
		StringBuffer sb = new StringBuffer();
		sb.append("JVM Memory: ");
		sb.append(u / (1024*1024));
		sb.append(" MB Used, ");
		sb.append(f / (1024*1024));
		sb.append(" MB Free, ");
		sb.append(t / (1024*1024));
		sb.append(" MB Total\nProcessing: ");
		for (String x : Util.alphabetical(processing)) {
			if (env.prog.exps.get(x).kind().equals(Kind.COMMENT)) {
				continue;
			}
			sb.append(x);
			sb.append(" ");
		}
		sb.append("\nTodo: ");
		for (String x : Util.alphabetical(todo)) {
			if (env.prog.exps.get(x).kind().equals(Kind.COMMENT)) {
				continue;
			}
			sb.append(x);
			sb.append(" ");
		}
		

		return sb.toString();

	}

	public final AqlEnv env;

	public final List<String> todo = Collections.synchronizedList(new TreeList<>());
	public final List<String> processing = Collections.synchronizedList(new TreeList<>());
	public final List<String> completed = Collections.synchronizedList(new TreeList<>());

	public final String[] toUpdate;
	public final AqlEnv last_env;

	public final List<RuntimeException> exn = Collections.synchronizedList(new TreeList<>());


	public AqlMultiDriver(Program<Exp<?>> prog, String[] toUpdate, AqlEnv last_env) {
		this.env = new AqlEnv(prog);
		this.toUpdate = toUpdate;
		this.last_env = last_env;

		this.numProcs = (int) this.env.defaults.getOrDefault(AqlOption.num_threads);
		
	}

	public void start() {
		System.gc();
		long f = Runtime.getRuntime().freeMemory();
		long m = Runtime.getRuntime().totalMemory();		
		long u = m - f;
		//checkAcyclic();
		// set the defaults here
		env.typing = new AqlTyping(env.prog, false); 
		init();
		update();
		process();
		System.gc();
		long f2 = Runtime.getRuntime().freeMemory();
		long m2 = Runtime.getRuntime().totalMemory();
		long u2 = m2 - f2;
		env.fd = (f2 - f);
		env.md = (m2 - m);
		env.ud = (u2 - u);
	}
/*
	private void checkAcyclic() {
		DAG<String> dag = new DAG<>();
		for (String n : env.prog.order) {
			Exp<?> e = env.prog.exps.get(n);
			if (e.isVar()) {
				continue;
			}
			for (Pair<String, Kind> d : /* wrapDeps(n, / /* env.prog.exps.get(n).deps() /* , env.prog) ) { // crushes
				// performance
				if (!env.prog.order.contains(d.first)) {
					throw new LineException("Does not exist: " + d.second + " " + d.first, n,
							env.prog.exps.get(n).kind().toString());
				}
				boolean ok = dag.addEdge(n, d.first);
				if (!ok) {
					throw new LineException("Adding " + n + " dependency on " + d + " causes circularity", n,
							env.prog.exps.get(n).kind().toString());
				}
			}
		}
	} */

	private boolean isEnded() {
		synchronized (ended) {
			return ended.i == numProcs || (todo.isEmpty() && processing.isEmpty());
		}
	}

	private void barrier() {
		synchronized (ended) {
			while (!isEnded()) {
				try {
					ended.wait();
				} catch (InterruptedException e) {
					abort();
				}
			}
		}
	}

	private void update() {
		String s = toString();
		//System.gc();
		synchronized (toUpdate) {
			toUpdate[0] = s;
		}
	}

	private final IntRef ended = new IntRef(0);
	private final List<Thread> threads = Collections.synchronizedList(new LinkedList<>());
	private final int numProcs;

	private void process() {
		for (int i = 0; i < numProcs; i++) {
			Thread thr = new Thread(this::call);
			thr.setPriority(Thread.MIN_PRIORITY);
			threads.add(thr);
		}
		for (Thread thr : threads) {
			thr.start();			
		}
		barrier();

		if (!exn.isEmpty()) {
			for (RuntimeException t : exn) {
				if (!(t instanceof RuntimeInterruptedException)) {
					env.exn = t;
				}
			}
			if (env.exn == null) {
				env.exn = new RuntimeException("Anomaly: please report");
			}
			// when uncommented, partial results will not appear
			// throw env.exn;
		}
	}

	private final Map<String, Boolean> changed = Collections.synchronizedMap(new THashMap<>());

	private void init() {
		if (last_env == null || !last_env.prog.options.equals(env.prog.options)) {
			todo.addAll(env.prog.order);
			return;
		}

		for (String n : env.prog.order) {
			if (/* (!last_env.defs.keySet().contains(n)) || */ changed(n)) {
				todo.add(n);
			} else {
				Kind k = env.prog.exps.get(n).kind();
				env.defs.put(n, k, last_env.defs.get(n, k));
				env.performance.put(n, last_env.performance.get(n));
				completed.add(n);
			}
		}
	}

	private boolean changed(String n) {
		if (n == null) {
			Util.anomaly();
		}
		if (env.prog.exps.get(n) == null) {
			return true;
		}
		if (changed.containsKey(n)) {
			return changed.get(n);
		}
		Exp<?> prev = last_env.prog.exps.get(n);
		// System.out.println(xprog.exps.get(n));
		if (prev == null || last_env == null || !last_env.defs.keySet().contains(n)
				|| (Boolean) env.prog.exps.get(n).getOrDefault(env, AqlOption.always_reload)) {
			changed.put(n, true);
			return true;
		}
		for (Pair<String, Kind> d : wrapDeps(n, prev, env.prog)) {
			if (changed(d.first)) {
				changed.put(n, true);
				return true;
			}
		}
		boolean b = !prev.equals(env.prog.exps.get(n));
		changed.put(n, b);
		return b;
	}

	private Unit notifyOfDeath() {
		synchronized (this) {
			notifyAll();
		}
		synchronized (ended) {
			ended.i++;
			ended.notifyAll();
		}
		return Unit.unit;
	}

	@Override
	public Unit call() {
		String k2 = "";
		String n = "";

		try {
			while (true) {
				n = null;

				synchronized (this) {
					if (todo.isEmpty() || Thread.currentThread().isInterrupted()) {
						break;
					}
					n = nextAvailable();
					if (n == null) {
						update();
						wait(5000); // just in case
						continue;
					}
					processing.add(n);
					todo.remove(n);
					update();
				}
				Exp<?> exp = env.prog.exps.get(n);
				Kind k = exp.kind();
				long time1 = System.currentTimeMillis();
				Object val = Util.timeout(() -> exp.eval(env, false),
						(Long) exp.getOrDefault(env, AqlOption.timeout) * 1000);
				if (val == null) {
					throw new RuntimeException("anomaly, please report: null result on " + exp);
				} else if (k.equals(Kind.PRAGMA)) {
					((Pragma) val).execute();
				}
				long time2 = System.currentTimeMillis();
			//	System.gc();
				
				synchronized (this) {
					env.defs.put(n, k, val);
					env.performance.put(n, (time2 - time1) / (1000f));
					processing.remove(n);
					completed.add(n);
					update();
					notifyAll();
				}
			}
		} catch (InterruptedException exp) {
			exn.add(new RuntimeInterruptedException(exp));
		} catch (RuntimeInterruptedException exp) {
			exn.add(exp);
		} catch (Exception e) {
			e.printStackTrace();
			synchronized (this) {
				if (e instanceof LocException) {
					exn.add((LocException) e);
				} else {
					exn.add(new LineException(e.getMessage(), n, k2));
				}
				notifyAll();
				interruptAll();
			}
		}

		update();
		return notifyOfDeath();
	}

	private String nextAvailable() {
		outer: for (String s : todo) {
			for (Pair<String, Kind> d : wrapDeps(s, env.prog.exps.get(s), env.prog)) {
				if (!completed.contains(d.first)) {
					continue outer;
				}
			}
			return s;
		}
		return null;
	}

}
