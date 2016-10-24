package sushi.exceptions;

public class BestPathException extends RuntimeException {

	private static final long serialVersionUID = -4915882424522724921L;

	public BestPathException(final String arg0) {
		super(arg0);
	}

	public BestPathException(final Throwable arg0) {
		super(arg0);
	}

	public BestPathException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
