package sushi.exceptions;

public class ReflectionUtilsException extends RuntimeException {

	private static final long serialVersionUID = 6581122057199703982L;

	public ReflectionUtilsException(final String arg0) {
		super(arg0);
	}

	public ReflectionUtilsException(final Throwable arg0) {
		super(arg0);
	}

	public ReflectionUtilsException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
