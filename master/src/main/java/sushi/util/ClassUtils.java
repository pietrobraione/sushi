package sushi.util;

/**
 * Utility class for formatting classes' and method's names
 * 
 * @author andrea
 * 
 */
public final class ClassUtils {
	public static String getCanonicalClassname(final String signature) {
		return signature.substring(0, signature.lastIndexOf('.'));
	}
	
	public static String getSimpleClassname(final String signature) {
		String canonical = getCanonicalClassname(signature); 
		return canonical.substring(canonical.lastIndexOf('.') + 1);
	}
	
	public static String getSimpleClassnameFromCanonical(final String canonical) {
		String toReturn = canonical.substring(canonical.lastIndexOf('.') + 1); 
		if (toReturn.contains("<")) {
			toReturn = toReturn.substring(0, toReturn.indexOf('<'));
		}
		return toReturn;
	}
	
	public static String getPackage(final String signature) {
		String canonical = getCanonicalClassname(signature); 
		return canonical.substring(0, canonical.lastIndexOf('.'));
	}
	
	public static String getMethodname(final String signature) {
		return signature.substring(signature.lastIndexOf('.') + 1);
	}
	
	/**
	 * Do not instantiate!
	 */
	private ClassUtils() {
		//nothing to do
	}
}
