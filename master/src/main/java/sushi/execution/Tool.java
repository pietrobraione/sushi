package sushi.execution;

import java.util.Collections;
import java.util.List;

public abstract class Tool<T> {
	/**
	 * The name of this tool.
	 * 
	 * @return a {@link String}.
	 */
	public final String getName() {
		return this.getClass().getSimpleName();
	}
	
	/**
	 * Gets a list of task identifiers.
	 * 
	 * @return a {@link List}{@code <}{@link Integer}{@code >}.
	 *         Each element in the list identifies a task.
	 * @throws Exception if the method fails in determining 
	 *         the tasks.
	 */
	public List<Integer> tasks() throws Exception { return Collections.singletonList(0); }
	
	/**
	 * Invoked by a {@link Worker} created by this tool so
	 * it can obtain its parameters.
	 * 
	 * @param taskNumber an {@code int}, the task number of the worker.
	 * @return the invocation parameters for the worker.
	 * @throws Exception if the method fails in determining the
	 *         invocation parameters for the worker.
	 */
	public abstract T getInvocationParameters(int taskNumber) throws Exception;
	
	/**
	 * Invoked after this tool is used, in case it could be used again
	 * so it is brought back in a pristine state.
	 */
	public void reset() { }
	
	/**
	 * Shall we delegate the management of timeout
	 * (exhaustion of time budget) to the coordinator
	 * (i.e., through the futures) or it is somehow 
	 * managed directly by the workers implementation?
	 * This parameter is used by some (not all) the
	 * coordinators.
	 * 
	 * @return a {@code boolean}.
	 */
	public boolean delegateTimeoutToCoordinator() { return false; } 

	/**
	 * The total time budget available to the workers
	 * to complete their work.
	 * 
	 * @return an {@code int}, the time budget in seconds.
	 */
	public abstract int getTimeBudget();

	/**
	 * Factory method. Creates a {@link Worker} for
	 * a task this tool manages.
	 * 
	 * @param taskNumber the number of the task.
	 * @return a {@link Worker}.
	 */
	public abstract Worker getWorker(int taskNumber);
	
	/**
	 * Factory method. Creates a {@link Coordinator} for
	 * managing synchronization with the workers created
	 * by this tool and build the final result.
	 * 
	 * @return a {@link Coordinator}.
	 */
	public Coordinator getCoordinator() { return new DefaultCoordinator(this); } 

	/**
	 * The number of threads in the thread pool 
	 * that is used to actually execute the workers
	 * created by this tool.
	 * 
	 * @return a positive {@code int}.
	 * @throws Exception if the method fails in determining 
	 *         the degree of parallelism.
	 */
	public int degreeOfParallelism() throws Exception { return 1; }
	
	/**
	 * The number of replicas of a same task.
	 * For each task it will be possible 
	 * to create a number of workers equal to
	 * {@link #redundance()}.
	 * 
	 * @return a positive {@code int}.
	 */
	public int redundance() { return 1; }
}
