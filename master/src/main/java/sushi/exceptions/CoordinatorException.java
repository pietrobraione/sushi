package sushi.exceptions;

public class CoordinatorException extends RuntimeException {

	private static final long serialVersionUID = -553552172484072600L;

	public CoordinatorException(String message) {
		super(message);
	}

	public CoordinatorException(Throwable cause) {
		super(cause);
	}

	public CoordinatorException(String message, Throwable cause) {
		super(message, cause);
	}

}
