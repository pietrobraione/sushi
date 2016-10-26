package sushi.logging;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

	private final SimpleDateFormat dateFmt = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS");
	public static Level level = Level.fromString("info");

	public static void setLevel(final Level level) {
		Logger.level = level;
	}

	public Logger(final Class<?> clazz) {
	}

	public void debug(final String message) {
		print(Level.DEBUG, message);
	}

	public void info(final String message) {
		print(Level.INFO, message);
	}

	public void warn(final String message) {
		print(Level.WARN, message);
	}

	public void error(final String message) {
		print(Level.ERROR, message);
	}

	public void error(final String message, final Throwable t) {
		print(Level.ERROR, message, t);
	}

	public void fatal(final String message) {
		print(Level.FATAL, message);
	}

	private void print(final Level level, final String message) {
		print(level, message, null);
	}

	private synchronized void print(final Level level, final String message, final Throwable t) {
		if(level.ordinal() >= Logger.level.ordinal()) {
			final PrintStream stream = System.out;
			final Date now = new Date();
			stream.format("%s %-5s - %s", this.dateFmt.format(now), level.name(), message);
			stream.println();
			if (t != null) {
				t.printStackTrace(stream);
			}
			stream.flush();
		}
	}
}

