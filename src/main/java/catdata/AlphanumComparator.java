package catdata;

import java.util.Comparator;

/**
 * This is an updated version with enhancements made by Daniel Migowski, Andre
 * Bogus, and David Koelle. Updated by David Koelle in 2017.
 *
 * To use this class: Use the static "sort" method from the
 * java.util.Collections class: Collections.sort(your list, new
 * AlphanumComparator());
 */
public class AlphanumComparator implements Comparator<Object> {
	private final boolean isDigit(char ch) {
		return   ((ch >= 48) && (ch <= 57));
	}

	/**
	 * Length of string is passed in for improved efficiency (only need to
	 * calculate it once)
	 **/
	private final String getChunk(String s, int slength, int marker) {
		StringBuilder chunk = new StringBuilder();
		char c = s.charAt(marker);
		chunk.append(c);
		marker++;
		if (isDigit(c)) {
			while (marker < slength) {
				c = s.charAt(marker);
				if (!isDigit(c))
					break;
				chunk.append(c);
				marker++;
			}
		} else {
			while (marker < slength) {
				c = s.charAt(marker);
				if (isDigit(c))
					break;
				chunk.append(c);
				marker++;
			}
		}
		return chunk.toString();
	}

	public int compare(Object s1o, Object s2o) {
		String s1 = s1o.toString();
		String s2 = s2o.toString();

		if ((s1 == null) || (s2 == null)) {
			return 0;
		}

		int thisMarker = 0;
		int thatMarker = 0;
		int s1Length = s1.length();
		int s2Length = s2.length();

		while (thisMarker < s1Length && thatMarker < s2Length) {
			String thisChunk = getChunk(s1, s1Length, thisMarker);
			thisMarker += thisChunk.length();

			String thatChunk = getChunk(s2, s2Length, thatMarker);
			thatMarker += thatChunk.length();

			// If both chunks contain numeric characters, sort them numerically
			int result = 0;
			if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
				// Simple chunk comparison by length.
				int thisChunkLength = thisChunk.length();
				result = thisChunkLength - thatChunk.length();
				// If equal, the first different number counts
				if (result == 0) {
					for (int i = 0; i < thisChunkLength; i++) {
						result = thisChunk.charAt(i) - thatChunk.charAt(i);
						if (result != 0) {
							return result;
						}
					}
				}
			} else {
				result = thisChunk.compareTo(thatChunk);
			}

			if (result != 0)
				return result;
		}

		return s1Length - s2Length;
	}


}