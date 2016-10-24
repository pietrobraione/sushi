package sushi.execution;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import sushi.exceptions.WorkerException;
import sushi.logging.Logger;

public class ExecutionManager {

	private static final Logger logger = new Logger(ExecutionManager.class);

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
		final Thread[] takers = new Thread[toReturn.length];
		for (int i = 0; i < takers.length; ++i) {
			final ArrayList<Future<ExecutionResult>> futures = tasksFutures.get(i / tool.redundance());
			final int taskNumber = i; //to make the compiler happy
			takers[i] = new Thread(() -> {
				//waits for the result of its worker
				try {
					toReturn[taskNumber] = futures.get(taskNumber % tool.redundance()).get();
				} catch (CancellationException e) {
					//the worker was cancelled: nothing left to do
					return;
				} catch (ExecutionException e) {
					logger.fatal("Error occurred during execution of tool " + tool.getName());
					throw new WorkerException(e);
				} catch (InterruptedException e)  {
					//should never happen, but if it happens
					//it's ok to fall through to shutdown
				}
				//cancels redundant workers
				for (Future<ExecutionResult> f : futures) {
					f.cancel(true);
				}
			});
			takers[i].start();
		}
		
		for (int i = 0; i < takers.length; ++i) {
			try {
				takers[i].join();
			} catch (InterruptedException e) {
				//does nothing
			}
		}

		return toReturn;
	}
	
}
