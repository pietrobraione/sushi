package sushi.util;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;

import sushi.logging.Logger;

public class IOUtils {

	private static final String END_MESSAGE = "   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>";
	private static final String START_MESSAGE = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<   ";

	public IOUtils() { } // Do not instantiate me
	
	
	/*
	 * IOUTILS for printing
	 */
	public static void formatInitMessage(final Logger logger, final String method) {
		logger.info(START_MESSAGE + method + END_MESSAGE);
		logger.info("Generating equivalences for method " + method);
	}
	
	public static void formatEndMessage(final Logger logger, final String method) {
		logger.info(START_MESSAGE.substring(0, START_MESSAGE.length() - 3) + endString(method.length() + 6) + END_MESSAGE.substring(3));
	}
	
	private static String endString(int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length/2; i++) {
			sb.append('<');
		}
		for (int i = (length/2); i < length; i++) {
			sb.append('>');
		}
		return sb.toString();
	}

	/*
	 * IOUTILS for file system
	 */
	public static String concatFilePath(final String... args) {
		StringBuilder builder = new StringBuilder();
		boolean firstDone = false;
		for (String arg : args) {
			if (firstDone) {
				builder.append(File.separator);
			} else {
				firstDone = true;
			}
			builder.append(arg);
		}
		return builder.toString();
	}
	
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
	
	public static String fromCanonicalToPath(final String packageName) {
		return packageName.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
	}

	public static String lastDir(final String path) {
		String[] dirs = path.split(File.separatorChar=='\\' ? "\\\\" : File.separator);
		return dirs[dirs.length - 1];
	}
	
}
