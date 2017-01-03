package sushi.execution;

import java.util.Collections;
import java.util.List;

public abstract class Tool<T> {
	public final String getName() {
		return this.getClass().getSimpleName();
	}
	
	public List<Integer> tasks() { return Collections.singletonList(0); }
	
	public abstract T getInvocationParameters(int taskNumber);

	public abstract int getTimeBudget();
	
	public abstract Worker getWorker(int taskNumber);
	
	public Coordinator getCoordinator() { return new DefaultCoordinator(this); } 

	public int degreeOfParallelism() { return 1; }
	
	public int redundance() { return 1; }
}
