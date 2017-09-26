package sushi.exceptions;

public class InternalUnexpectedException extends RuntimeException {

	private static final long serialVersionUID = -7753989753958447660L;

	public InternalUnexpectedException(final String arg0) {
		super(arg0);
	}

	public InternalUnexpectedException(final Throwable arg0) {
		super(arg0);
	}

	public InternalUnexpectedException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
