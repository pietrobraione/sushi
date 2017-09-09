package sushi.exceptions;

public class MinimizerException extends RuntimeException {

	private static final long serialVersionUID = -4915882424522724921L;

	public MinimizerException(final String arg0) {
		super(arg0);
	}

	public MinimizerException(final Throwable arg0) {
		super(arg0);
	}

	public MinimizerException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
