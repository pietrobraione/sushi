package sushi.execution.jbse;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sushi.ParseException;
import sushi.execution.ExitStatus;
import sushi.execution.Worker;

public class JBSEWorker extends Worker {
	private static final Logger LOGGER = LogManager.getFormatterLogger(JBSEWorker.class);

	private final JBSEAbstract jbse;

	public JBSEWorker(JBSEAbstract jbse, int taskNumber) {
		super(taskNumber);
		this.jbse = jbse;
	}

	@Override
	public ExitStatus call() throws FileNotFoundException, ParseException, IOException {
		//TODO run in spawned process or make RunJBSE_Sushi friendlier with the rest of SUSHI
		final JBSEParameters p = this.jbse.getInvocationParameters(this.taskNumber);
		final RunJBSE_Sushi r = new RunJBSE_Sushi(p);
		final long start = System.currentTimeMillis();
		final int exitStatus = r.run();
		final long elapsed = System.currentTimeMillis() - start;
		LOGGER.debug("Task %s: task ended, elapsed %s seconds.", Integer.toString(this.taskNumber), Long.toString(elapsed/1000));
		final ExitStatus result = new ExitStatus(exitStatus);
		return result;
	}
}
