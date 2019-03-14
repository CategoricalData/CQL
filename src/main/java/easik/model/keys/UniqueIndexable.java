package easik.model.keys;

/**
 * Interface used to collectively identify items that can be added to a unique
 * index. This currently includes EntityAttribute, NormalEdge, and PartialEdge
 * objects.
 */
public interface UniqueIndexable {
	/**
	 * Returns a human-readable name of this indexable item. Typically the attribute
	 * name or edge name.
	 *
	 * @return
	 */
	public String getName();
}
