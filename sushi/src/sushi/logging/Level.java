package sushi.logging;

public enum Level {

	DEBUG, INFO, WARN, ERROR, FATAL;

	public static Level fromString(final String level) {
		if (level.equalsIgnoreCase("debug")) {
			return DEBUG;
		}
		if (level.equalsIgnoreCase("info")) {
			return INFO;
		}
		if (level.equalsIgnoreCase("warn")) {
			return WARN;
		}
		if (level.equalsIgnoreCase("error")) {
			return ERROR;
		}
		if (level.equalsIgnoreCase("fatal")) {
			return FATAL;
		}

		return null;
	}

}
