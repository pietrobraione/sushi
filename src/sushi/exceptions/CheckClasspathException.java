package sushi.exceptions;

public class CheckClasspathException extends RuntimeException {

	private static final long serialVersionUID = 6581122057199703982L;

	public CheckClasspathException(String arg0) {
		super(arg0);
	}

	public CheckClasspathException(Throwable arg0) {
		super(arg0);
	}

	public CheckClasspathException(String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
