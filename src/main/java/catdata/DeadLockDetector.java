package catdata;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

public class DeadLockDetector {

	public interface DeadlockHandler {
		void handleDeadlock(final ThreadInfo[] deadlockedThreads);
	}

	public static void makeDeadLockDetector() {
		DeadLockDetector deadlockDetector = new DeadLockDetector(new DeadlockConsoleHandler(), 60, TimeUnit.SECONDS);
		deadlockDetector.start();
	}

	
	public static class DeadlockConsoleHandler implements DeadlockHandler {

		
		@Override
		public void handleDeadlock(final ThreadInfo[] deadlockedThreads) {
			if (deadlockedThreads != null) {
				JOptionPane.showMessageDialog(null, "Deadlock!");
				System.err.println("Deadlock detected!");

//				Map<Thread, StackTraceElement[]> stackTraceMap = Thread.getAllStackTraces();
				for (ThreadInfo threadInfo : deadlockedThreads) {

					if (threadInfo != null) {

						for (Thread thread : Thread.getAllStackTraces().keySet()) {

							if (thread.getId() == threadInfo.getThreadId()) {
								System.err.println(threadInfo.toString().trim());

								for (StackTraceElement ste : thread.getStackTrace()) {
									System.err.println("\t" + ste.toString().trim());
								}
							}
						}
					}
				}
			}
		}
	}

	private final DeadlockHandler deadlockHandler;
	private final long period;
	private final TimeUnit unit;
	private final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	final Runnable deadlockCheck = new Runnable() {
		@Override
		public void run() {
			//System.out.println("+++ " + Term.cache.keySet().size());

			long[] deadlockedThreadIds = DeadLockDetector.this.mbean.findDeadlockedThreads();

			if (deadlockedThreadIds != null) {
				ThreadInfo[] threadInfos = DeadLockDetector.this.mbean.getThreadInfo(deadlockedThreadIds);

				DeadLockDetector.this.deadlockHandler.handleDeadlock(threadInfos);
			}
		}
	};

	public DeadLockDetector(final DeadlockHandler deadlockHandler, final long period, final TimeUnit unit) {
		this.deadlockHandler = deadlockHandler;
		this.period = period;
		this.unit = unit;
	}

	public void start() {
		this.scheduler.scheduleAtFixedRate(this.deadlockCheck, this.period, this.period, this.unit);
	}
}
