package sushi.compile.distance;

/*
 * The difference between this impl. and the standard one is that, rather
 * than creating and retaining a matrix of size threadName.length()+1 by
 * t.length()+1, we maintain two single-dimensional arrays of length
 * threadName.length()+1. The first, d, is the 'current working' distance array
 * that maintains the newest distance cost counts as we iterate through
 * the characters of String threadName. Each time we increment the index of
 * String t we are comparing, d is copied to p, the second int[]. Doing
 * so allows us to retain the previous cost counts as required by the
 * algorithm (taking the minimum of the cost count to the left, up one,
 * and diagonally up and to the left of the current cost count being
 * calculated).
 */
public class PrefixDistance {

	public static int calculateDistance(final String s, final String t) {
		int sLength = (s == null) ? 0 : s.length();
		int tLength = (t == null) ? 0 : t.length();

		if (sLength == 0) {
			return tLength;
		} else if (tLength == 0) {
			return sLength;
		}

		int countMissing = sLength;
		int minLength = Math.min(sLength, tLength);
		for (int i = 0; i < minLength; i++) {
			if (s.charAt(i) == t.charAt(i)) 
				--countMissing;
			else break;		
		}
		
		int result = (countMissing != 0) ? countMissing : tLength - sLength;
	
		return result;
	}

}
