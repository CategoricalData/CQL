package catdata.ide;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class ProgressMonitorWrapper implements ActionListener {

	private final ProgressMonitor pbar;
	private int counter = 0;

	private final long start_time;
	private final Thread thread;
	private final Timer timer;

	public ProgressMonitorWrapper(String msg, Runnable task) {
		pbar = new ProgressMonitor(null, msg, "Elapsed: 0 secs", 0, 4);
		start_time = System.currentTimeMillis();
		timer = new Timer(500, this);
		timer.start();

		thread = new Thread(() -> {
			task.run();
			timer.stop();
			pbar.close();
		});
		thread.start();
	}

	/*
	 * public static void main(String args[]) { Runnable r = () -> { for (;;) { } };
	 * new ProgressMonitorWrapper("test", r); }
	 */

	@Override
	public void actionPerformed(ActionEvent e) {
		SwingUtilities.invokeLater(new Update());
	}

	class Update implements Runnable {
		@Override
		@SuppressWarnings("deprecation")
		public void run() {
			if (pbar.isCanceled()) {
				try {
					thread.stop();
				} catch (RuntimeException ex) {
					ex.printStackTrace();
				}
				timer.stop();
				return;
			}
			pbar.setProgress(counter % 4);
			pbar.setNote(
					"Elapsed: " + ((double) (System.currentTimeMillis() - start_time) / ((double) 1000) + " secs"));
			counter++;
		}
	}
}
