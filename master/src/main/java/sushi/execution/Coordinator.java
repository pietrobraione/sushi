package sushi.execution;

import java.util.ArrayList;
import java.util.concurrent.Future;

import sushi.exceptions.WorkerTerminationException;
import sushi.exceptions.CoordinatorException;
import sushi.exceptions.ToolException;
import sushi.exceptions.WorkerFailureException;

public abstract class Coordinator {
	protected final Tool<?> tool;
	
	public Coordinator(Tool<?> tool) { this.tool = tool; }
	
	/**
	 * Put here all the initialization that must be performed
	 * right before the threads for the tasks are created
	 * (after the previous tools have completed their execution).
	 * 
	 * @throws CoordinatorException if initialization fails.
	 */
	public abstract void init() throws CoordinatorException;

	public abstract ExitStatus[] start(ArrayList<ArrayList<Future<ExitStatus>>> allTasksFutures) 
	throws CoordinatorException, ToolException, WorkerTerminationException, WorkerFailureException;
}
