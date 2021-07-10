package sushi.util;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import sushi.Options;
import sushi.exceptions.ReflectionUtilsException;
import sushi.logging.Logger;

public final class ClassReflectionUtils {
	private static final Logger logger = new Logger(ClassReflectionUtils.class);
	
	private static final Set<String> excluded;
	
	static {
		excluded = new HashSet<String>();
		excluded.add("equals");
		excluded.add("hashCode");
		excluded.add("toString");
		excluded.add("clone");
		excluded.add("immutableEnumSet");
	}
	
	public static ClassLoader getInternalClassloader(Options options) {
		final List<Path> classpath = options.getClassesPath();
		final ClassLoader classLoader;
		try {
			ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

			if (classpath == null || classpath.size() == 0) {
				logger.debug("The inner classpath is empty, relying on SystemClassLoader");
				classLoader = systemClassLoader;
			} else {
				final List<File> paths = new ArrayList<File>();
				for (Path path : classpath) {
					File newPath = path.toFile();
					if (!newPath.exists()) {
						throw new MalformedURLException("The new path " + newPath + " does not exist");
					} else {
						paths.add(newPath);
					}
				}

				final List<URL> urls = new ArrayList<URL>();
				if (systemClassLoader instanceof URLClassLoader) {
					urls.addAll(Arrays.asList(((URLClassLoader) systemClassLoader).getURLs()));
				}

				for (File newPath : paths) {
					urls.add(newPath.toURI().toURL());
				}
				classLoader = new URLClassLoader(urls.toArray(new URL[0]), ClassReflectionUtils.class.getClassLoader());
			}
		} catch (MalformedURLException | SecurityException e) {
			logger.error("Unable to load ClassLoader", e);
			throw new ReflectionUtilsException(e);
		}
		return classLoader;
	}
	
	/**
	 * Returns the externally callable methods of a class.
	 * 
	 * @param options an {@link Options} object.
	 * @param className a {@link String}, the name of the class.
	 * @param onlyPublic {@code true} to restrict the list to the public methods of the class.
	 * @return a {@link List}{@code <}{@link List}{@code <}{@link String}{@code >>} of the methods
	 *         of the class {@code className} that are not private, nor synthetic, nor one of the 
	 *         {@code equals}, {@code hashCode}, {@code toString}, {@code clone}, {@code immutableEnumSet}.
	 *         If {@code onlyPublic == true} only the public methods are returned. Each {@link List}{@code <}{@link String}{@code >}
	 *         has three elements and is a method signature.
	 * @throws ClassNotFoundException if the class is not in the classpath.
	 */
	public static List<List<String>> getVisibleMethods(Options options, String className, boolean onlyPublic) throws ClassNotFoundException {
		final ClassLoader ic = getInternalClassloader(options);
		final Class<?> clazz = ic.loadClass(className.replace('/', '.'));
		final List<List<String>> methods = new ArrayList<>();
		for (Method m : clazz.getDeclaredMethods()) {
			if (!excluded.contains(m.getName()) &&
				((onlyPublic && (m.getModifiers() & Modifier.PUBLIC) != 0) || (m.getModifiers() & Modifier.PRIVATE) == 0) &&
				!m.isSynthetic()) {
				final List<String> methodSignature = new ArrayList<>(3);
				methodSignature.add(className);
				methodSignature.add("(" +
					Arrays.stream(m.getParameterTypes())
					.map(c -> c.getName())
					.map(s -> s.replace('.', '/'))
					.map(ClassReflectionUtils::convertPrimitiveTypes)
					.map(ClassReflectionUtils::addReferenceMark)
					.collect(Collectors.joining()) +
					")" + addReferenceMark(convertPrimitiveTypes(m.getReturnType().getName().replace('.', '/'))));
				methodSignature.add(m.getName());
				methods.add(methodSignature);
			}
		}
		return methods;
	}
	
	private static final String convertPrimitiveTypes(String s) {
		if (s.equals("boolean")) {
			return "Z";
		} else if (s.equals("byte")) {
			return "B";
		} else if (s.equals("short")) {
			return "S";
		} else if (s.equals("int")) {
			return "I";
		} else if (s.equals("long")) {
			return "J";
		} else if (s.equals("char")) {
			return "C";
		} else if (s.equals("float")) {
			return "F";
		} else if (s.equals("double")) {
			return "D";
		} else if (s.equals("void")) {
			return "V";
		} else {
			return s;
		}
	}
	
	private static final String addReferenceMark(String s) {
		if (s.equals("Z") ||
			s.equals("B") ||
			s.equals("S") ||
			s.equals("I") ||
			s.equals("J") ||
			s.equals("C") ||
			s.equals("F") ||
			s.equals("D") ||
			s.equals("V") ||
			s.charAt(0) == '[') {
			return s;
		} else {
			return "L" + s + ";";
		}
	}

	
	/**
	 * Do not instantiate!
	 */
	private ClassReflectionUtils() {
		//nothing to do
	}
}
