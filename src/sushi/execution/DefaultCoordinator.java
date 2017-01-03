package sushi.execution;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import sushi.exceptions.WorkerException;
import sushi.logging.Logger;

public class DefaultCoordinator extends Coordinator {
	private static final Logger logger = new Logger(DefaultCoordinator.class);
	
	public DefaultCoordinator(Tool<?> tool) { super(tool); }

	@Override
	public ExecutionResult[] start(ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures) {
		final ExecutionResult[] retVal = new ExecutionResult[this.tool.tasks().size() * this.tool.redundance()];
		final Thread[] takers = new Thread[retVal.length];
		for (int i = 0; i < takers.length; ++i) {
			final int threadNumber = i; //to make the compiler happy
			final ArrayList<Future<ExecutionResult>> futures = tasksFutures.get(threadNumber / this.tool.redundance());
			takers[i] = new Thread(() -> {
				//waits for the result of its worker
				try {
					retVal[threadNumber] = futures.get(threadNumber % this.tool.redundance()).get();
				} catch (CancellationException e) {
					//the worker was cancelled: nothing left to do
					return;
				} catch (ExecutionException e) {
					logger.fatal("Error occurred during execution of tool " + this.tool.getName());
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
		
		return retVal;
	}
}
