package sushi.execution.minimizer;

import sushi.exceptions.MinimizerException;
import sushi.exceptions.WorkerTerminationException;
import sushi.execution.ExitStatus;
import sushi.execution.Worker;

public class MinimizerWorker extends Worker {
	private final Minimizer minimizer;

	public MinimizerWorker(Minimizer minimizer) {
		this.minimizer = minimizer;
	}

	@Override
	public ExitStatus call() throws WorkerTerminationException, MinimizerException {
		final MinimizerParameters p = this.minimizer.getInvocationParameters(this.taskNumber);
		final RunMinimizer r = new RunMinimizer(p);
		final int exitStatus = r.run();
		final ExitStatus result = new ExitStatus(exitStatus);
		return result;
	}
}
