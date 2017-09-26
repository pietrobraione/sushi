package sushi.exceptions;

public class JavacException extends RuntimeException {

	private static final long serialVersionUID = 586551523206201681L;

	public JavacException(final String arg0) {
		super(arg0);
	}

	public JavacException(final Throwable arg0) {
		super(arg0);
	}

	public JavacException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
