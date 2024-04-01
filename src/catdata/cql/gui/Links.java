package catdata.cql.gui;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import catdata.Pair;
import catdata.Quad;
import catdata.Util;
import catdata.cql.exp.RawTerm;
import catdata.cql.exp.EdsExpRaw.EdExpRaw;

public class Links<N, T> {
	public final Map<String, Quad<N, String, N, String>> enLinks;
	public final Map<String, Quad<String, Pair<N, String>, RawTerm, RawTerm>> colLinks;
	public final Map<String, EdExpRaw> rowLinks;

	public Links(Map<String, Quad<N, String, N, String>> enLinks,
			Map<String, Quad<String, Pair<N, String>, RawTerm, RawTerm>> colLinks, Map<String, EdExpRaw> rowLinks) {
		this.enLinks = enLinks;
		this.colLinks = colLinks;
		this.rowLinks = rowLinks;
		if (!Collections.disjoint(enLinks.keySet(), colLinks.keySet())
				|| !Collections.disjoint(enLinks.keySet(), rowLinks.keySet())) {
			Util.anomaly();
		}
	}
	
	public void clear() {
		enLinks.clear();
		colLinks.clear();
		rowLinks.clear();
	}

	@Override
	public String toString() {
		return "entity_isomorphisms\n\n"
				+ Util.sep(enLinks, ": ", "\n\n", x -> x.first + "." + x.second + " -> " + x.third + "." + x.fourth)
				+ "\n\nequations\n\n"
				+ Util.sep(colLinks, ": ", "\n\n", x -> "forall " + x.first + ":" + x.second.first + "."
						+ x.second.second + ", " + x.third + " = " + x.fourth)
				+ "\n\nconstraints\n\n" + Util.sep(rowLinks, ": ", "\n\n");
	}

	public void add(Links<N, T> newLinks) {
		for (var x : newLinks.enLinks.entrySet()) {
			enLinks.put(x.getKey(), x.getValue());
		}
		for (var x : newLinks.colLinks.entrySet()) {
			colLinks.put(x.getKey(), x.getValue());
		}
		for (var x : newLinks.rowLinks.entrySet()) {
			rowLinks.put(x.getKey(), x.getValue());
		}
	}

/*	public void remove(Links<N, T> newLinks) {
		for (var x : newLinks.enLinks.entrySet()) {
			enLinks.remove(x.getKey());
		}
		for (var x : newLinks.colLinks.entrySet()) {
			colLinks.remove(x.getKey());
		}
		for (var x : newLinks.rowLinks.entrySet()) {
			rowLinks.remove(x.getKey());
		}
	}*/

	public void remove(Collection<N> toRemove) {
		for (var x : toRemove) {
			if (enLinks.containsKey(x))
				enLinks.remove(x);
			if (colLinks.containsKey(x))
				colLinks.remove(x);
			if (rowLinks.containsKey(x))
				rowLinks.remove(x);
		}
	}

	public Iterable<String> keySet() {
		return Util.union(enLinks.keySet(), Util.union(rowLinks.keySet(), colLinks.keySet()));
	}

}