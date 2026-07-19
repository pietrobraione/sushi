package sushi.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

import sushi.exceptions.ToolException;
import sushi.exceptions.WorkerFailureException;
import sushi.exceptions.WorkerTerminationException;

public class DefaultCoordinator extends Coordinator {
	private static final Logger LOGGER = LogManager.getFormatterLogger(DefaultCoordinator.class);
	
	public DefaultCoordinator(Tool<?> tool) { super(tool); }
	
	//attribute to report the anomalous termination of a task
	private boolean terminate;
	private boolean fail;
	private String message;
	private Throwable exception;
	private int taskNumber;

	@Override
	public ExitStatus[] start(ArrayList<ArrayList<Future<ExitStatus>>> allTasksFutures) 
	throws ToolException, WorkerTerminationException, WorkerFailureException {
		init();
		final List<Integer> tasks;
		try {
			tasks = this.tool.tasks();
		} catch (Exception e) {
			throw new ToolException(e);
		}
		final ExitStatus[] retVal = new ExitStatus[tasks.size() * this.tool.redundance()];
		final Thread[] takers = new Thread[retVal.length];
		for (int i = 0; i < takers.length; ++i) {
			final int threadNumber = i; //to make the compiler happy
			final int taskNumber = threadNumber / this.tool.redundance();
			final int replicaNumber = threadNumber % this.tool.redundance();
			final ArrayList<Future<ExitStatus>> thisTaskFutures = allTasksFutures.get(taskNumber);
			final Future<ExitStatus> thisThreadFuture = thisTaskFutures.get(replicaNumber);
			takers[i] = new Thread(() -> {
				//waits for the result of its worker
				try {
					if (this.tool.delegateTimeoutToCoordinator()) {
						retVal[threadNumber] = thisThreadFuture.get(this.tool.getTimeBudget(), TimeUnit.SECONDS);
					} else {
						retVal[threadNumber] = thisThreadFuture.get();
					}
					//the worker correctly terminated and produced a result: 
					//cancels all the redundant workers (i.e., its replicas)
					cancelTask(thisTaskFutures);
				} catch (TimeoutException e) {
					//the worker timed out
					LOGGER.info("Task %s replica %s timed out.", taskNumber, replicaNumber);
					retVal[threadNumber] = null;
					thisThreadFuture.cancel(true);
				} catch (CancellationException e) {
					//the worker was cancelled
					retVal[threadNumber] = null;
				} catch (ExecutionException e) { 
					//the worker threw an exception
					if (e.getCause() instanceof WorkerTerminationException) {
						//notifies the coordinator the need to relaunch the WorkerTerminationException
						setTerminate(e.getCause().getMessage());
					} else { //any other exception
						//notifies the coordinator the need to launch a WorkerFailureException
						setFail(taskNumber, e.getCause());
					}
					//cancels all the workers and exits
					cancelAllTasks(allTasksFutures);
				} catch (InterruptedException e)  {
					//should never happen, but if it happens
					//behaves as a cancellation
					retVal[threadNumber] = null;
				}
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
		
		//if termination/failure happened, launches the corresponding exception
		if (this.terminate) {
			throw new WorkerTerminationException(this.taskNumber, this.message);
		} else if (this.fail) {
			throw new WorkerFailureException(this.taskNumber, this.exception);
		}
		
		//otherwise returns retVal
		return retVal;
	}
	
	private void init() {
		this.terminate = false;
		this.fail = false;
		this.message = null;
		this.exception = null;
		this.taskNumber = -1;
	}
	
	private synchronized void setTerminate(String message) {
		this.terminate = true;
		this.fail = false;
		this.message = message;
		this.exception = null;
		this.taskNumber = -1;
	}
	
	private synchronized void setFail(int taskNumber, Throwable cause) {
		this.terminate = false;
		this.fail = true;
		this.message = null;
		this.exception = cause;
		this.taskNumber = taskNumber;
	}
	
	private synchronized void cancelAllTasks(ArrayList<ArrayList<Future<ExitStatus>>> allTasksFutures) {
		for (final ArrayList<Future<ExitStatus>> group : allTasksFutures) {
			for (final Future<ExitStatus> f : group) {
				f.cancel(true);
			}
		}
	}
	
	private synchronized void cancelTask(ArrayList<Future<ExitStatus>> thisTaskFutures) {
		for (final Future<ExitStatus> f : thisTaskFutures) {
			f.cancel(true);
		}
	}
}
