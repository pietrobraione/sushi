package sushi.execution;

import java.util.ArrayList;
import java.util.concurrent.Future;

public abstract class Coordinator {
	protected final Tool<?> tool;
	
	public Coordinator(Tool<?> tool) { this.tool = tool; }

	public abstract ExecutionResult[] start(ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures);
}
