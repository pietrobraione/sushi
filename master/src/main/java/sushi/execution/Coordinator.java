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

	public abstract ExitStatus[] start(ArrayList<ArrayList<Future<ExitStatus>>> allTasksFutures) 
	throws CoordinatorException, ToolException, WorkerTerminationException, WorkerFailureException;
}
