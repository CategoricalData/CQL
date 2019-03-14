package catdata;

public final class ClassUtil {
	/**
	 * This method is used when an unchecked cast is appropriate. This should be
	 * almost never.
	 * 
	 * e.g. final E e = ClassUtil.<E>unchecked_cast(x); ( ..., x ->
	 * ClassUtil.<E>unchecked_cast(x), ... )
	 */
	@SuppressWarnings("unchecked")
	public static <T> T unchecked_cast(Object obj) {
		return (T) obj;
	}
}