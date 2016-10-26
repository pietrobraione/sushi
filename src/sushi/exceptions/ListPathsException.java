package sushi.exceptions;

public class ListPathsException extends RuntimeException {

	private static final long serialVersionUID = -2849474990193397014L;

	public ListPathsException(final String arg0) {
		super(arg0);
	}

	public ListPathsException(final Throwable arg0) {
		super(arg0);
	}

	public ListPathsException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
