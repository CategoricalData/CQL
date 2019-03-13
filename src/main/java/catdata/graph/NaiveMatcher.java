package catdata.graph;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import catdata.Util;
import gnu.trove.map.hash.THashMap;

/**
 * Naive (edge to edge) graph matching based on string distance .
 * 
 * @author ryan
 */
public class NaiveMatcher<N1,N2,E1,E2> extends Matcher<N1,E1,N2,E2,BiFunction<String,String,Integer>> {
	
	public NaiveMatcher(DMG<N1, E1> src, DMG<N2, E2> dst, Map<String, String> options) {
		super(src, dst, options);
	}
	
	@Override
	public Match<N1, E1, N2, E2> bestMatch() {
		Map<N1, N2> nodes = new THashMap<>();
		Map<E1, List<E2>> edges = new THashMap<>();
		
		for (N1 s : src.nodes) {
			int min_d = Integer.MAX_VALUE;
			N2 min_t = null;
			for (N2 t : dst.nodes) {
				int cur_d = params.apply(s.toString(), t.toString());
				if (cur_d < min_d) {
					min_d = cur_d;
					min_t = t;
				}
			}
			if (min_t == null) {
				throw new RuntimeException("No match from " + s);
			}
			nodes.put(s, min_t);
		}
			
		for (E1 c : src.edges.keySet()) {
			int min_d = Integer.MAX_VALUE;
			E2 min_c = null;
			N2 n2_s = nodes.get(src.edges.get(c).first);
			N2 n2_t = nodes.get(src.edges.get(c).second);
			for (E2 d : dst.edges(n2_s, n2_t)) {
				int cur_d = params.apply(c.toString(), d.toString());
				if (cur_d < min_d) {
					min_d = cur_d;
					min_c = d;
				}
			}
			if (min_c != null) {
				edges.put(c, Collections.singletonList(min_c));
			} else if (n2_s.equals(n2_t)) {
				edges.put(c, new LinkedList<>());
			} else {
				ShortestPath<N2,E2> sp = new ShortestPath<>(dst, n2_s);
				if (sp.hasPathTo(n2_t)) {
					edges.put(c, sp.pathTo(n2_t));
				} else {
					throw new RuntimeException("No match from " + c + " under node mapping\n\n" + Util.sep(nodes, " -> ", "\n") );
				}
			}
		}
		
		return new Match<>(src, dst, nodes, edges);
	}

	

	@Override
	public BiFunction<String, String, Integer> createParams(Map<String, String> options) {
		if (!options.isEmpty()) {
			throw new RuntimeException("No options allowed for naive matching");
		}
		return Util::editDistance;
	}
	
	//TODO: explore all node matchings in order, veto-ing ones that cause edge mappings to fail

}
