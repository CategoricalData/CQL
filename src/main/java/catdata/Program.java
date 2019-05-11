package catdata;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jparsec.error.Location;
import org.jparsec.error.ParseErrorDetails;
import org.jparsec.error.ParserException;

import catdata.aql.AqlOptions;
import catdata.aql.AqlOptions.AqlOption;
import gnu.trove.map.hash.THashMap;

public class Program<X> implements Prog {

	public synchronized Map<X, String> invert() {
		Map<X, String> inv = new THashMap<>(exps.size());
		for (String x : exps.keySet()) {
			inv.put(exps.get(x), x);
		}
		return inv;
	}

	public long timeout() {
		if (options.options.containsKey(AqlOption.timeout)) {
			return (Long) options.get(AqlOption.timeout);
		}
		return 30;
	}

	/*
	 * 'order' consists of a list of the names of the CQL expressions in order.
	 * Named objects are as they appear in the CQL program, but anonymous objects
	 * have names generated. Example: "graph g100 = ..." -> "g100" "md { ... }" ->
	 * "md35"
	 * 
	 * These names are the keys into other containers.
	 */
	public final List<String> order = Collections.synchronizedList(new LinkedList<>());
	/*
	 * 'lines' is keyed by the names from order and the value is the offset from the
	 * beginning of the program to the name of the expression. The 'lines' is a bit
	 * of a misnomer as there is an entry for expression and the value is measured
	 * in characters.
	 */
	public final Map<String, Pair<Integer,Integer>> lines = Collections.synchronizedMap(new THashMap<>());
	/*
	 * 'expr' contains the expressions. There are many types of expressions, each
	 * one has its own data structure. see catadata.aql.exp
	 */
	public final Map<String, X> exps = Collections.synchronizedMap(new THashMap<>());
	/*
	 * 'options' is a dictionary of the global options. note: options are not
	 * considered expressions.
	 */
	public final AqlOptions options;
	/*
	 * 'text' is a copy of the original program.
	 */
	private final String text;

	@Override
	public String toString() {
		String ret = "";
		for (String s : order) {
			ret += s + " = " + exps.get(s) + "\n\n";
		}
		return ret;
	}

	public Function<X, String> kindOf;

	@Override
	public String kind(String s) {
		return kindOf.apply(exps.get(s));
	}

	public Program(List<Quad<String, Integer, X, Integer>> decls, String text) {
		this(decls, text, Collections.emptyList(), x -> "");
	}

	/**
	 * The main program constructor.
	 * 
	 * @param decls
	 * @param text
	 * @param options
	 * @param k
	 */
	public Program(List<Quad<String, Integer, X, Integer>> decls, String text, List<Pair<String, String>> options,
			Function<X, String> k) {
		this.text = text;
		List<Quad<String, Integer, X, Integer>> seen = new LinkedList<>();
		for (Quad<String, Integer, X, Integer> decl : decls) {
			if (decl.second == null || decl.third == null) {
				Util.anomaly();
			}
			checkDup(seen, decl);
			X x = decl.third;
			exps.put(decl.first, x);
			lines.put(decl.first, new Pair<>(decl.second, decl.fourth));
			order.add(decl.first);
			if (!decl.third.equals(decl.third)) {
				throw new RuntimeException("Please report: non-reflexive: " + decl.third.toString());
			}
			// log.info(decl.toString());
		}
		this.kindOf = k;
		try {
			this.options = new AqlOptions(Util.toMapSafely(options), null, AqlOptions.initialOptions);
		} catch (Exception ex) {
			throw new ParserException(ex, null, "options", new Location(1, 1));
		}
	}

	private Location conv(int i) {
		int c = 1;
		int line = 1, col = 1;
		while (c++ <= i) {
			if (text.charAt(c) == '\n') {
				++line;
				col = 1;
			} else {
				++col;
			}
		}
		return new Location(line, col);
	}

	@SuppressWarnings("deprecation")
	private void checkDup(List<Quad<String, Integer, X, Integer>> seen, Quad<String, Integer, X, Integer> toAdd) {
		for (Quad<String, Integer, X, Integer> other : seen) {
			if (other.first.equals(toAdd.first)) {
				if (text == null) {
					throw new RuntimeException("Duplicate name: " + toAdd.first); // TODO CQL + " on line " +
																					// other.second + " and " +
																					// toAdd.second);
				}
				throw new ParserException(new ParseErrorDetails() {

					@Override
					public String getEncountered() {
						return other.first;
					}

					@Override
					public List<String> getExpected() {
						return new LinkedList<>();
					}

					@Override
					public String getFailureMessage() {
						return "Other occurance: line " + conv(other.second).line + ", column "
								+ conv(other.second).column;
					}

					@Override
					public int getIndex() {
						return other.second;
					}

					@Override
					public String getUnexpected() {
						return "";
					}
				}, "Duplicate name: " + toAdd.first, conv(toAdd.second)); // TODO CQL );

			}
		}
		seen.add(toAdd);
	}

	@Override
	public int getLine(String s) {
		Integer i = lines.get(s).first;
		if (i == null) {
			return -1;
		}
		return i.intValue();
	}

	@Override
	public Collection<String> keySet() {
		return order;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((exps == null) ? 0 : exps.hashCode());
		result = prime * result + ((lines == null) ? 0 : lines.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((order == null) ? 0 : order.hashCode());
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
		Program other = (Program) obj;
		if (exps == null) {
			if (other.exps != null)
				return false;
		} else if (!exps.equals(other.exps))
			return false;
		if (lines == null) {
			if (other.lines != null)
				return false;
		} else if (!lines.equals(other.lines))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (order == null) {
			if (other.order != null)
				return false;
		} else if (!order.equals(other.order))
			return false;
		return true;
	}
	
	

}
