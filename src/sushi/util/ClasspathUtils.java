package sushi.util;

import java.nio.file.Files;

import sushi.configure.Options;
import sushi.exceptions.CheckClasspathException;
import sushi.logging.Logger;

public class ClasspathUtils {

	private static final Logger logger = new Logger(ClasspathUtils.class);
	
	public static void checkClasspath() {
		//check classpath: if the class is not found it raises an exception
		logger.debug("Checking classpath");
		final String className = (Options.I().getTargetMethod() == null ?
				Options.I().getTargetClass() :
				Options.I().getTargetMethod().get(0));
		try {
			final ClassLoader ic = ReflectionUtils.getInternalClassloader();
			ic.loadClass(className.replace('/', '.'));
		} catch (ClassNotFoundException e) {
			logger.error("Could not find class under test: " + className);
			throw new CheckClasspathException(e);
		}
		logger.debug("Classpath OK");
		logger.debug("Checking EvoSuite");
		if (!Files.exists(Options.I().getEvosuitePath()) || !Files.isReadable(Options.I().getEvosuitePath())) {
			logger.error("Could not find or execute EvoSuite");
			throw new CheckClasspathException("Could not find or execute EvoSuite");
		}
		logger.debug("EvoSuite OK");
	}
}
