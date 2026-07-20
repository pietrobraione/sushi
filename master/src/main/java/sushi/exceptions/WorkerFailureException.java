package sushi.exceptions;

public class WorkerFailureException extends Exception {

	private static final long serialVersionUID = -5535521120724072600L;
	
	private final int task;

	public WorkerFailureException(int task, Throwable cause) {
		super(cause);
		this.task = task;
	}
	
	public WorkerFailureException(int task, String message) {
		super(message);
		this.task = task;
	}
	
	public int getTask() {
		return this.task;
	}
}
