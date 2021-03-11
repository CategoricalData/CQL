package catdata.aql.exp;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

import java.io.File;
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

import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import catdata.aql.Kind;
import catdata.aql.Pragma;

public final class PragmaExpTinkerpop extends PragmaExp {
	private final List<String> jss;

	private final Map<String, String> options;

	@Override
	public Map<String, String> options() {
		return options;
	}

	@Override
	public Unit type(AqlTyping G) {
		return Unit.unit;
	}

	public PragmaExpTinkerpop(List<String> jss, List<Pair<String, String>> options) {
		this.options = Util.toMapSafely(options);
		this.jss = jss;
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((jss == null) ? 0 : jss.hashCode());
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
		PragmaExpTinkerpop other = (PragmaExpTinkerpop) obj;

		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (jss == null) {
			if (other.jss != null)
				return false;
		} else if (!jss.equals(other.jss))
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

			@Override
			public void execute() {
				if (str != null) {
					return;
				}
				str = "";
				AqlOptions ops = new AqlOptions(options, env.defaults);

				try {
					List<Object> str0 = execGremlin(ops, jss);
					str += format(str0, jss);
					str += "\n";

				} catch (Exception ex) {
					ex.printStackTrace();
					str += "\n\n" + ex.getMessage();
				}

			}

			@Override
			public String toString() {
				return str;
			}

		};
	}

	public static List<Object> execGremlin(AqlOptions ops, List<String> jss) throws Exception {
		String host = (String) ops.getOrDefault(AqlOption.tinkerpop_host);
		Integer port = (Integer) ops.getOrDefault(AqlOption.tinkerpop_port);
		long timeout = 1000 * (Long) ops.getOrDefault(AqlOption.timeout);
		String gname = (String) ops.getOrDefault(AqlOption.tinkerpop_graph_name);

		String file0 = "hosts: [" + host + "]\n" + "port: " + port + "\n"
				+ "serializer: { className: org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV3d0, config: { ioRegistries: [com.lambdazen.bitsy.BitsyIoRegistryV3d0] }}";
		File f0 = File.createTempFile("tempfile0", ".yaml");
		Util.writeFile(file0, f0.getAbsolutePath());

		String file1 = "gremlin.remote.remoteConnectionClass=org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection\ngremlin.remote.driver.clusterFile="
				+ maybeQuote(f0.getAbsolutePath()) + "\ngremlin.remote.driver.sourceName=" + gname;
		File f1 = File.createTempFile("tempfile1", ".properties");
		Util.writeFile(file1, f1.getAbsolutePath());

		ConcurrentBindings b = new ConcurrentBindings();
		var zzz = traversal().withRemote(f1.getAbsolutePath());
		b.putIfAbsent(gname, zzz);

		GremlinExecutor conn = GremlinExecutor.build().scriptEvaluationTimeout(timeout).globalBindings(b).create();
		List<Object> ret = new ArrayList<>(jss.size());
		for (String s : jss) {
			CompletableFuture<Object> evalResult = conn.eval(s);
			Object actualResult = evalResult.get();
			ret.add(actualResult);
		}
		conn.close();
		return ret;
	}

	public static String maybeQuote(String s) {
		return StringEscapeUtils.escapeEcmaScript(s);
	}

	public static String format(List<Object> str0, List<String> jss) throws Exception {
		String str = "";
		for (int i = 0; i < str0.size(); i++) {
			String s = jss.get(i);
			Object o = str0.get(i);
			str += "Exec " + s + ":\n";

			if (o instanceof Iterable) {
				str += "Java class: Iterable\n";
				Iterable<Object> it = (Iterable<Object>) o;
				str += Util.sep(it, "\n");
			} else if (o.getClass().isArray()) {
				str += "Java class: Array\n";
				Object[] oo = (Object[]) o;
				str += Arrays.deepToString(oo);
			} else if (o instanceof GraphTraversal<?, ?>) {
				str += "Java class: GraphTraversal\n";
				GraphTraversal<?, ?> t = (GraphTraversal<?, ?>) o;
				for (Object x : t.toList()) {
					str += (x + "  [ " + x.getClass() + "]\n");
				}
				t.close();
			} else {
				str += "Java class: " + o.getClass() + "\n";
				str += o;
			}
		}
		return str;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {
		set.add(AqlOption.tinkerpop_host);
		set.add(AqlOption.tinkerpop_port);
		set.add(AqlOption.tinkerpop_graph_name);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder().append("exec_tinkerpop {")
				.append(Util.sep(jss.stream().map(Util::quote).collect(Collectors.toList()), ""));

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