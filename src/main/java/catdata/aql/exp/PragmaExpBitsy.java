package catdata.aql.exp;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.tinkerpop.gremlin.groovy.engine.GremlinExecutor;
import org.apache.tinkerpop.gremlin.jsr223.ConcurrentBindings;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;

import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Pragma;

public final class PragmaExpBitsy extends PragmaExp {
	private final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	@Override
	public Unit type(AqlTyping G) {
		return Unit.unit;
	}

	public PragmaExpBitsy(List<Pair<String, String>> options) {
		this.options = Util.toMapSafely(options);
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PragmaExpBitsy other = (PragmaExpBitsy) obj;

		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}

	@Override
	public Pragma eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		return new Pragma() {
			String str;
			GremlinServer srv;

			@Override
			public void execute() {
				if (str != null) {
					return;
				}

				AqlOptions ops = new AqlOptions(options, env.defaults);

				try {
					srv = prepareGremlin(ops);
					str = "Bitsy Tinkerpop-Gremlin-Groovy Server spawned: " + srv + "\n\n";
				} catch (Exception ex) {
					ex.printStackTrace();
					str = ex.getMessage();
				}

			}

			@Override
			public String toString() {
				return str;
			}

		};
	}

	private static GremlinServer prepareGremlin(AqlOptions ops) throws Exception {
		String host = (String) ops.getOrDefault(AqlOption.tinkerpop_host);
		Integer port = (Integer) ops.getOrDefault(AqlOption.tinkerpop_port);
		long timeout = 1000 * (Long) ops.getOrDefault(AqlOption.timeout);
		String path = (String) ops.getOrDefault(AqlOption.bitsy_db_path);
		String gname = (String) ops.getOrDefault(AqlOption.tinkerpop_graph_name);

    if (path.isEmpty()) {
      path = Files.createTempDirectory("bitsyDB").toFile().getAbsolutePath();
    }

		File h = File.createTempFile("bitsySrc", ".properties");
		String toH = "gremlin.graph=com.lambdazen.bitsy.BitsyGraph\n" + "dbPath=" + path;
		Util.writeFile(toH, h.getAbsolutePath());

		File ff = new File(path);
		if (!ff.exists()) {
			ff.mkdir();
		} else if (!ff.isDirectory()) {
			throw new RuntimeException("Bitsy path exists but is not a folder: " + path);
		}

		String aa = "host: " + host + "\nport: " + port + " \nscriptEvaluationTimeout: " + timeout + "\ngraphs: {\n  graph: "
				+ h.getAbsolutePath() + " }\n";

		String grvy = "def addItUp(x, y) { x + y }\ndef globals = [:]\nglobals << [";
		String grvy2 = gname + " : traversal().withEmbedded(graph).withStrategies(ReferenceElementStrategy)]";
		File g = File.createTempFile("tempfile3", ".groovy");
		Util.writeFile(grvy + grvy2, g.getAbsolutePath());

		String a2 = """
				scriptEngines: {
				  gremlin-groovy: {
				    plugins: { org.apache.tinkerpop.gremlin.server.jsr223.GremlinServerGremlinPlugin: {},
				               com.lambdazen.bitsy.jsr223.BitsyGremlinPlugin: {},
				               org.apache.tinkerpop.gremlin.jsr223.ScriptFileGremlinPlugin: {files: [""";
		String a3 = """
				]},
				               org.apache.tinkerpop.gremlin.jsr223.ImportGremlinPlugin: {classImports: [java.lang.Math], methodImports: [java.lang.Math#*]}}}}
				serializers:
				  - { className: org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV3d0, config: { ioRegistries: [com.lambdazen.bitsy.BitsyIoRegistryV3d0] }}
				  - { className: org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV3d0, config: { serializeResultToString: true }}
				  - { className: org.apache.tinkerpop.gremlin.driver.ser.GraphSONMessageSerializerV3d0, config: { ioRegistries: [com.lambdazen.bitsy.BitsyIoRegistryV3d0] }}
				processors:
				  - { className: org.apache.tinkerpop.gremlin.server.op.session.SessionOpProcessor, config: { sessionTimeout: 28800000 }}
				  - { className: org.apache.tinkerpop.gremlin.server.op.traversal.TraversalOpProcessor, config: { cacheExpirationTime: 600000, cacheMaxSize: 1000 }}
				strictTransactionManagement: false
				maxInitialLineLength: 4096
				maxHeaderSize: 8192
				maxChunkSize: 8192
				graphManager: org.apache.tinkerpop.gremlin.server.util.CheckedGraphManager
				maxContentLength: 65536
				maxAccumulationBufferComponents: 1024
				resultIterationBatchSize: 64
				""";

		File f = File.createTempFile("tempfile", ".yaml");
		String www = aa + a2 + g.getAbsolutePath() + a3;
		Util.writeFile(www, f.getAbsolutePath());
		GremlinServer server = new GremlinServer(Settings.read(f.getAbsolutePath()));
		server.start();
		return server;
	}

	public static String maybeQuote(String s) {
		String z = StringEscapeUtils.escapeEcmaScript(s);
		return z;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.tinkerpop_host);
		set.add(AqlOption.tinkerpop_port);
		set.add(AqlOption.tinkerpop_graph_name);
		set.add(AqlOption.bitsy_db_path);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("spawn_bitsy {");

		if (!options.isEmpty()) {
			sb.append("\n\toptions").append(Util.sep(options, "\n\t\t", " = "));
		}
		return sb.append("}").toString();
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Collections.emptyList();
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {

	}

}
