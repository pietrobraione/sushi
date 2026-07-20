package sushi.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import sushi.exceptions.CoordinatorException;
import sushi.exceptions.ToolException;
import sushi.exceptions.WorkerFailureException;
import sushi.exceptions.WorkerTerminationException;

public final class ExecutionManager {
	public static ExitStatus[] execute(Tool<?> tool) 
	throws CoordinatorException, ToolException, WorkerTerminationException, WorkerFailureException {
		//creates a thread pool for the tool
		final int nthreads;
		try {
			nthreads = tool.degreeOfParallelism();
		} catch (Exception e) {
			throw new ToolException(e);
		}
		final ExecutorService executor = Executors.newFixedThreadPool(nthreads);
		final ExecutorCompletionService<ExitStatus> pool = new ExecutorCompletionService<>(executor);

		//gets all the tasks of the tool
		final List<Integer> tasks;
		try {
			tasks = tool.tasks();
		} catch (Exception e) {
			throw new ToolException(e);
		}
		
		//for all the tasks of the tool creates and launches all the workers 
		//for them by using the thread pool
		final ArrayList<ArrayList<Future<ExitStatus>>> allTasksFutures = new ArrayList<>();
		for (int task : tasks) {
			final ArrayList<Future<ExitStatus>> thisTaskFutures = new ArrayList<>();
			for (int i = 1; i <= tool.redundance(); ++i) {
				final Worker worker = tool.getWorker(task);
				final Future<ExitStatus> thisWorkerFuture = pool.submit(worker);
				thisTaskFutures.add(thisWorkerFuture);
			}
			allTasksFutures.add(thisTaskFutures);
		}
		executor.shutdown();

		//uses the tool's coordinator to synchronize with the workers 
		//and build the result
		final Coordinator coordinator = tool.getCoordinator();
		final ExitStatus[] retVal = coordinator.start(allTasksFutures);
		return retVal;
	}
}
