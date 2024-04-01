package catdata.cql.exp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import catdata.Pair;
import catdata.Unit;
import catdata.Util;
import catdata.cql.Kind;
import catdata.cql.Pragma;
import catdata.cql.AqlOptions.AqlOption;
import catdata.graph.DMG;
import catdata.graph.Matcher;
import catdata.graph.NaiveMatcher;
import catdata.graph.SimilarityFloodingMatcher;

public final class PragmaExpMatch extends PragmaExp {
	public final String String;
	public final Map<String, String> options;

	public final GraphExp src;
	public final GraphExp dst;

	@Override
	public Unit type(AqlTyping G) {
		src.type(G);
		dst.type(G);
		return Unit.unit;
	}

	@Override
	public void mapSubExps(Consumer<Exp<?>> f) {
		src.map(f);
		dst.map(f);
	}

	public <R, P, E extends Exception> R accept(P params, PragmaExpVisitor<R, P, E> v) throws E {
		return v.visit(params, this);
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	public PragmaExpMatch(String String, GraphExp src, GraphExp dst, List<Pair<String, String>> options) {
		this.String = String;
		this.options = Util.toMapSafely(options);
		this.src = src;
		this.dst = dst;
	}

	@Override
	public Pragma eval0(AqlEnv env, boolean isC) {
		if (isC) {
			throw new IgnoreException();
		}
		DMG<String, String> src0 = src.eval(env, false).dmg;
		DMG<String, String> dst0 = dst.eval(env, false).dmg;

		return new Pragma() {

			private String s;

			@Override
			public void execute() {
				toString();
			}

			@Override
			public String toString() {
				if (s != null) {
					return s;
				}
				// TODO aql eventually, this should not catch the exception
				Matcher<String, String, String, String, ?> ret0;
				try {
					switch (String) {
						case "naive":
							ret0 = new NaiveMatcher<>(src0, dst0, options);
							break;
						case "sf":
							ret0 = new SimilarityFloodingMatcher<>(src0, dst0, options);
							break;
						default:
							throw new RuntimeException("Please use naive or sf for String match desired, not " + String);
					}
					s = ret0.bestMatch.toString();
				} catch (Exception e) {
					// e.printStackTrace();
					s = e.getMessage();
					throw e;
				}
				return s;
			}
		};
	}

	@Override
	public String toString() {
		return "match " + Util.quote(String) + " : " + src + " -> " + dst;
	}

	@Override
	public Collection<Pair<String, Kind>> deps() {
		return Util.union(src.deps(), dst.deps());
	}

	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		result = prime * result + ((String == null) ? 0 : String.hashCode());
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
		PragmaExpMatch other = (PragmaExpMatch) obj;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		if (String == null) {
			if (other.String != null)
				return false;
		} else if (!String.equals(other.String))
			return false;
		return true;
	}

	@Override
	protected void allowedOptions(Set<AqlOption> set) {

	}

}