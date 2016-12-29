package sushi.execution;

import java.util.ArrayList;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutionManager {
	public static ExecutionResult[] execute(Tool<?> tool) {
		final ExecutorService executor = Executors.newFixedThreadPool(tool.degreeOfParallelism());
		final ExecutorCompletionService<ExecutionResult> pool = new ExecutorCompletionService<>(executor);
		final ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures = new ArrayList<>();
		for (int task : tool.tasks()) {
			final ArrayList<Future<ExecutionResult>> futures = new ArrayList<>();
			for (int i = 1; i <= tool.redundance(); ++i) {
				final Worker worker = tool.getWorker(task);
				final Future<ExecutionResult> f = pool.submit(worker);
				worker.setFutureTimeout(f);
				futures.add(f);
			}
			tasksFutures.add(futures);
		}
		executor.shutdown();

		final ExecutionResult[] toReturn = new ExecutionResult[tool.tasks().size() * tool.redundance()];
		final Coordinator coordinator = tool.getCoordinator();
		coordinator.start(tool, tasksFutures, toReturn);
		return toReturn;
	}
}
