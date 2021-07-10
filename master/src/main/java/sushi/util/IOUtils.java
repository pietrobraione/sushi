package sushi.util;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public final class IOUtils {
	public static String concatClassPath(List<Path> classpath) {
		return concatClassPath(CollectionUtils.toStringArray(classpath));
	}
	
	public static String concatClassPath(Path... classpath) {
		return concatClassPath(CollectionUtils.toStringArray(classpath));
	}

	public static String concatClassPath(String... args) {
		final StringBuilder builder = new StringBuilder();
		boolean firstDone = false;
		for (String arg : args) {
			if (firstDone) {
				builder.append(File.pathSeparator);
			} else {
				firstDone = true;
			}
			builder.append(arg);
		}
		return builder.toString();
	}
	
	/**
	 * Do not instantiate!
	 */
	private IOUtils() {
		//nothing to do
	}
}
