package sushi.execution;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import sushi.exceptions.WorkerException;
import sushi.logging.Logger;

public class DefaultCoordinator implements Coordinator {
	private static final Logger logger = new Logger(DefaultCoordinator.class);

	@Override
	public void start(Tool<?> tool, ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures, ExecutionResult[] toReturn) {
		final Thread[] takers = new Thread[toReturn.length];
		for (int i = 0; i < takers.length; ++i) {
			final int threadNumber = i; //to make the compiler happy
			final ArrayList<Future<ExecutionResult>> futures = tasksFutures.get(threadNumber / tool.redundance());
			takers[i] = new Thread(() -> {
				//waits for the result of its worker
				try {
					toReturn[threadNumber] = futures.get(threadNumber % tool.redundance()).get();
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
				for (final Future<ExecutionResult> f : futures) {
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
	}
}
