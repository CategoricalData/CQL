package catdata.graph;

import java.util.Map;

import catdata.Util;

/**
 * Class for matching graphs.
 * 
 * @author ryan
 *
 * @param <P> type of parameters
 */
public abstract class Matcher<N1, E1, N2, E2, P> {

  final DMG<N1, E1> src;
  final DMG<N2, E2> dst;
  final P params;
  public final Match<N1, E1, N2, E2> bestMatch;

  Matcher(DMG<N1, E1> src, DMG<N2, E2> dst, Map<String, String> options) {
    Util.assertNotNull(src, dst, options);
    this.src = src;
    this.dst = dst;
    params = createParams(options);
    bestMatch = bestMatch();
    if (bestMatch == null) {
      throw new RuntimeException("Anomaly: please report");
    }
  }

  /**
   * @return default parameters. must not be null
   */
  protected abstract P createParams(Map<String, String> options);

  /**
   * @return the best match. must not be null - throw exception if no best match
   *         exists
   */
  protected abstract Match<N1, E1, N2, E2> bestMatch();

}
