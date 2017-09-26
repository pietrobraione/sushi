package sushi.execution;

import java.util.concurrent.Callable;

public abstract class Worker implements Callable<ExecutionResult> {
	protected final int taskNumber;
	
	public Worker(int taskNumber) {
		this.taskNumber = taskNumber;
	}
	
	public Worker() { this(0); }
}
