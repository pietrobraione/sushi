package sushi.execution.jbse;

import sushi.configure.JBSEParameters;
import sushi.execution.ExecutionResult;
import sushi.execution.Worker;
import sushi.logging.Logger;

public class JBSEWorker extends Worker {
	private static final Logger logger = new Logger(JBSEWorker.class);

	private final JBSEAbstract jbse;

	public JBSEWorker(JBSEAbstract jbse, int taskNumber) {
		super(taskNumber);
		this.jbse = jbse;
	}

	@Override
	public ExecutionResult call() {
		//TODO run in spawned process or make RunJBSE_Sushi friendlier with the rest of sushi
		final JBSEParameters p = this.jbse.getInvocationParameters(this.taskNumber);
		final RunJBSE_Sushi r = new RunJBSE_Sushi(p);
		final long start = System.currentTimeMillis();
		final int exitStatus = r.run();
		final long elapsed = System.currentTimeMillis() - start;
		logger.debug("Task " + this.taskNumber + " ended, elapsed " + elapsed/1000 + " seconds");
		final ExecutionResult result = new ExecutionResult();
		result.setExitStatus(exitStatus);
		return result;
	}
}
