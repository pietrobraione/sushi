package sushi.execution;

import java.util.ArrayList;
import java.util.concurrent.Future;

public interface Coordinator {
	void start(Tool<?> tool, ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures, ExecutionResult[] toReturn);
}
