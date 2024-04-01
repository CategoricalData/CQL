package catdata.cql;

import catdata.graph.DMG;

public class Graph<N, E> implements Semantics {

  /**
   * return size of nodes plus size of edges
   */
  @Override
  public int size() {
    return dmg.nodes.size() + dmg.edges.size();
  }

  public final DMG<N, E> dmg;

  public Graph(DMG<N, E> dmg) {
    this.dmg = dmg;
  }

  @Override
  public Kind kind() {
    return Kind.GRAPH;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dmg == null) ? 0 : dmg.hashCode());
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
    Graph<?, ?> other = (Graph<?, ?>) obj;
    if (dmg == null) {
      if (other.dmg != null)
        return false;
    } else if (!dmg.equals(other.dmg))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return dmg.toString();
  }

}
