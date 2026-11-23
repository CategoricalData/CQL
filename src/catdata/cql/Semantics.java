package catdata.cql;

import java.util.List;

public interface Semantics {

  public default String sample(@SuppressWarnings("unused") int size) {
    return null;
  }
  ;

  /**
   * Warnings about this result, one message each. A warning reports something the user may not
   * expect, such as two symbols merged because they were given one name, without stopping the run.
   */
  public default List<String> warnings() {
    return List.of();
  }

  public Kind kind();

  public int size();

  //  public long time();

  //  public void time(long time);

  /* public default TypeSide<?, ?> asTypeSide() {
    return (TypeSide<?, ?>) this;
  }

  public default Graph<?, ?> asGraph() {
    return (Graph<?, ?>) this;
  }

  public default Schema<?, ?, ?, ?, ?> asSchema() {
    return (Schema<?, ?, ?, ?, ?>) this;
  }

  public default Constraints asConstraints() {
    return (Constraints) this;
  }

  public default Instance<?, ?, ?, ?, ?, ?, ?, ?, ?> asInstance() {
    return (Instance<?, ?, ?, ?, ?, ?, ?, ?, ?>) this;
  }

  public default Transform<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> asTransform() {
    return (Transform<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) this;
  }

  public default Mapping<?, ?, ?, ?, ?, ?, ?, ?> asMapping() {
    return (Mapping<?, ?, ?, ?, ?, ?, ?, ?>) this;
  }

  public default Query<?, ?, ?, ?, ?, ?, ?, ?> asQuery() {
    return (Query<?, ?, ?, ?, ?, ?, ?, ?>) this;
  }

  public default Pragma asPragma() {
    return (Pragma) this;
  }

  public default Comment asComment() {
    return (Comment) this;
  }

  public default ColimitSchema<?> asSchemaColimit() {
    return (ColimitSchema<?>) this;
  }

  public default Mor<?,?,?,?> asTheoryMorphism() {
    return (Mor) this;
  } */

}
