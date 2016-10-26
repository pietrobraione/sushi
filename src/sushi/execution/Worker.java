package sushi.execution;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public abstract class Worker implements Callable<ExecutionResult> {
	protected final int taskNumber;
	private Future<ExecutionResult> futureTimeout;
	
	public Worker(int taskNumber) {
		this.taskNumber = taskNumber;
		this.futureTimeout = null;
	}
	
	public Worker() { this(0); }
	
	public synchronized void setFutureTimeout(Future<ExecutionResult> futureTimeout) {
		this.futureTimeout = futureTimeout;
	}
	
	/**
	 * Subclass {@link #call()} method should invoke 
	 * this method to spawn a timeout thread.
	 * 
	 * @param timeout timeout in seconds.
	 */
	protected void startTimeout(long timeout) {
		if (timeout > 0) {
			//waits for futureTimeout
			boolean noFuture;
			synchronized (this) {
				noFuture = (this.futureTimeout == null);
			}
			while (noFuture) {
				try {
					Thread.sleep(1000);
					synchronized (this) {
						noFuture = (this.futureTimeout == null);
					}
				} catch (InterruptedException e) {
					//should never happen, but if it happens 
					//we fall through
				}
			}

			//spawns the timeout thread
			final Thread countdown = new Thread(() -> {
				try {
					Thread.sleep(timeout * 1000);
				} catch (InterruptedException e) {
					//should never happen, but if it happens 
					//we fall through to shutdown
				}
				this.futureTimeout.cancel(true);
			});
			countdown.start();
		}
	}

}
