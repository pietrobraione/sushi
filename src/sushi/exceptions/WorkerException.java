package sushi.exceptions;

public class WorkerException extends RuntimeException {

	private static final long serialVersionUID = -5535521120724072600L;

	public WorkerException(String message) {
		super(message);
	}

	public WorkerException(Throwable cause) {
		super(cause);
	}

	public WorkerException(String message, Throwable cause) {
		super(message, cause);
	}

}
