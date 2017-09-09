package sushi.exceptions;

public class EvosuiteException extends RuntimeException {

	private static final long serialVersionUID = -4915882424522724921L;

	public EvosuiteException(final String arg0) {
		super(arg0);
	}

	public EvosuiteException(final Throwable arg0) {
		super(arg0);
	}

	public EvosuiteException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
