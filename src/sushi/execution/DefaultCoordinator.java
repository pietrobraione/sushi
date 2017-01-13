package sushi.execution;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

import sushi.exceptions.TerminationException;
import sushi.exceptions.WorkerException;
import sushi.logging.Logger;

public class DefaultCoordinator extends Coordinator {
	private static final Logger logger = new Logger(DefaultCoordinator.class);
	
	public DefaultCoordinator(Tool<?> tool) { super(tool); }
	
	private boolean terminate;
	private String message;

	@Override
	public ExecutionResult[] start(ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures) {
		setTerminate(false, null);
		final ExecutionResult[] retVal = new ExecutionResult[this.tool.tasks().size() * this.tool.redundance()];
		final Thread[] takers = new Thread[retVal.length];
		for (int i = 0; i < takers.length; ++i) {
			final int threadNumber = i; //to make the compiler happy
			final int taskNumber = threadNumber / this.tool.redundance();
			final int replicaNumber = threadNumber % this.tool.redundance();
			final ArrayList<Future<ExecutionResult>> futures = tasksFutures.get(taskNumber);
			final Future<ExecutionResult> thisThreadFuture = futures.get(replicaNumber);
			takers[i] = new Thread(() -> {
				//waits for the result of its worker
				try {
					if (this.tool.delegateTimeoutToCoordinator()) {
						retVal[threadNumber] = thisThreadFuture.get();
					} else {
						retVal[threadNumber] = thisThreadFuture.get(this.tool.getTimeBudget(), TimeUnit.SECONDS);
					}
				} catch (TimeoutException e) {
					logger.debug("Task " + taskNumber + " replica " + replicaNumber + " timed out");
					thisThreadFuture.cancel(true);
					return;
				} catch (CancellationException e) {
					//the worker was cancelled: nothing left to do
					return;
				} catch (ExecutionException e) {
					if (e.getCause() instanceof TerminationException) {
						//schedules relaunch of exception
						setTerminate(true, e.getCause().getMessage());
						
						//cancels all the workers and exits
						cancelAll(tasksFutures);
						return;
					} else {
						logger.fatal("Error occurred during execution of tool " + this.tool.getName());
						throw new WorkerException(e);
					}
				} catch (InterruptedException e)  {
					//should never happen, but if it happens
					//it's ok to fall through to shutdown
				}
				//cancels redundant workers
				cancelReplicas(futures);
			});
			takers[i].start();
		}
		
		//waits
		for (int i = 0; i < takers.length; ++i) {
			try {
				takers[i].join();
			} catch (InterruptedException e) {
				//does nothing
			}
		}
		
		//if a thread required termination, launches the exception
		if (this.terminate) {
			throw new TerminationException(this.message);
		}
		
		return retVal;
	}
	
	private synchronized void setTerminate(boolean terminate, String message) {
		this.terminate = terminate;
		this.message = message;
	}
	
	private synchronized void cancelAll(ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures) {
		for (final ArrayList<Future<ExecutionResult>> group : tasksFutures) {
			for (final Future<ExecutionResult> f : group) {
				f.cancel(true);
			}
		}
	}
	
	private synchronized void cancelReplicas(ArrayList<Future<ExecutionResult>> futures) {
		for (final Future<ExecutionResult> f : futures) {
			f.cancel(true);
		}
	}
}
