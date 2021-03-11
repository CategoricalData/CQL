package catdata.aql;

public interface Semantics {

  public default String sample(@SuppressWarnings("unused") int size) {
    return null;
  };

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
