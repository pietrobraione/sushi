package sushi.execution.minimizer;

import sushi.configure.MinimizerParameters;
import sushi.execution.ExecutionResult;
import sushi.execution.Worker;

public class MinimizerWorker extends Worker {
	private final Minimizer minimizer;

	public MinimizerWorker(Minimizer minimizer) {
		this.minimizer = minimizer;
	}

	@Override
	public ExecutionResult call() {
		final MinimizerParameters p = this.minimizer.getInvocationParameters(this.taskNumber);
		final RunMinimizer r = new RunMinimizer(p);
		final int exitStatus = r.run();
		final ExecutionResult result = new ExecutionResult();
		result.setExitStatus(exitStatus);
		return result;
	}
}
