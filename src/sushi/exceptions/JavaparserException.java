package sushi.exceptions;

public class JavaparserException extends RuntimeException {

	private static final long serialVersionUID = -554736718631410640L;

	public JavaparserException(final String arg0) {
		super(arg0);
	}

	public JavaparserException(final Throwable arg0) {
		super(arg0);
	}

	public JavaparserException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
