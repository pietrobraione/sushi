package sushi.exceptions;

import sushi.execution.Worker;

/**
 * Exception thrown by a {@link Worker} when it wants to preempt
 * the termination of the main SUSHI loop.
 */
public class WorkerTerminationException extends Exception {

	private static final long serialVersionUID = -6545725567832871060L;
	
	private final int task;

	public WorkerTerminationException() {
		super();
		this.task = -1;
	}
	
	public WorkerTerminationException(int task, String message) {
		super(message);
		this.task = task;
	}

	public int getTask() {
		return this.task;
	}
}

