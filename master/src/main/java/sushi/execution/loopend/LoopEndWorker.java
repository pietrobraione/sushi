package sushi.execution.loopend;

import sushi.exceptions.WorkerTerminationException;
import sushi.execution.ExitStatus;
import sushi.execution.Worker;

public class LoopEndWorker extends Worker {
	public LoopEndWorker() {
		//nothing
	}

	@Override
	public ExitStatus call() throws WorkerTerminationException {
		throw new WorkerTerminationException();
	}
}
