package sushi.execution.loopend;

import sushi.exceptions.TerminationException;
import sushi.execution.ExecutionResult;
import sushi.execution.Worker;

public class LoopEndWorker extends Worker {
	public LoopEndWorker() {
		//nothing
	}

	@Override
	public ExecutionResult call() throws TerminationException {
		throw new TerminationException();
	}
}
