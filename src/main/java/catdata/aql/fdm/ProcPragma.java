package catdata.aql.fdm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;

import catdata.Util;
import catdata.aql.Pragma;

public class ProcPragma extends Pragma {

	private final List<String> cmds;

	private final List<String> responses = (new LinkedList<>());

	@SuppressWarnings("unused")
	private final Map<String, String> options;

	public ProcPragma(List<String> cmds) {
		this.cmds = cmds;
		this.options = Collections.emptyMap();
	}

	@Override
	public void execute() {
		try {
			for (String cmd : cmds) {
				CommandLine cmdLine = CommandLine.parse(cmd);
				DefaultExecutor executor = new DefaultExecutor();
				executor.setStreamHandler(new ProcHandler(cmd));
				executor.execute(cmdLine);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return Util.sep(responses, "\n\n--------------\n\n");
	}

	private class ProcHandler implements ExecuteStreamHandler {

		private final String cmd;

		Thread out, err;

		InputStream outs, errs;

		private ProcHandler(String str) {
			cmd = str;
		}

		@Override
		public void setProcessErrorStream(InputStream arg0) {
			errs = arg0;
			err = new Thread(make(arg0, "stderr:"));
		}

		private Runnable make(InputStream is, String pre) {
			return () -> {
				String newLine = System.getProperty("line.separator");
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder result = new StringBuilder();
				String line;
				boolean flag = false;
				try {
					while ((line = reader.readLine()) != null) {
						result.append(flag ? newLine : "").append(line);
						flag = true;
					}
				} catch (IOException e) {
					responses.add("Err: " + cmd + " " + pre + " " + result + " and " + e.getMessage());

					if (!e.getLocalizedMessage().equals("Stream closed")) {
						throw new RuntimeException(e);
					}
				}

				responses.add(cmd + " " + pre + " " + result);
			};
		}

		@Override
		public void setProcessOutputStream(InputStream arg0) {
			outs = arg0;
			out = new Thread(make(arg0, "stdout:"));
		}

		@Override
		public void start() {
			out.start();
			err.start();
		}

		@Override
		public void stop() throws IOException {
			outs.close();
			errs.close();
			// threads should stop automatically
		}

		@Override
		public void setProcessInputStream(OutputStream arg0) {
		}

	}

}