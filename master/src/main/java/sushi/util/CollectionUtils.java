package sushi.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public final class CollectionUtils {	
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    
	public static String[] toStringArray(Collection<?> collection) {
		return collection
				.stream()
				.map(x -> x.toString())
				.collect(Collectors.toList())
				.toArray(EMPTY_STRING_ARRAY);
	}
	
	public static String[] toStringArray(Object[] objects) {
		return Arrays
				.stream(objects)
				.map(x -> x.toString())
				.collect(Collectors.toList())
				.toArray(EMPTY_STRING_ARRAY);
	}

	/**
	 * Do not instantiate!
	 */
	private CollectionUtils() {
		//nothing to do
	}
}
