package sushi.exceptions;

public class CoordinatorException extends Exception {

	private static final long serialVersionUID = 1613249986163088382L;

	public CoordinatorException(String message) {
		super(message);
	}

	public CoordinatorException(Throwable cause) {
		super(cause);
	}
}
