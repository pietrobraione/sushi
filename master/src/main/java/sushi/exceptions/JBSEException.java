package sushi.exceptions;

public class JBSEException extends RuntimeException {

	private static final long serialVersionUID = 1849681908912731819L;

	public JBSEException(final String arg0) {
		super(arg0);
	}

	public JBSEException(final Throwable arg0) {
		super(arg0);
	}

	public JBSEException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
