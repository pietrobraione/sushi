package sushi.execution.evosuite;

import static sushi.util.DirectoryUtils.getTmpDirPath;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sushi.Options;
import sushi.exceptions.ToolException;
import sushi.execution.ExitStatus;
import sushi.execution.Worker;

public final class EvosuiteWorker extends Worker {
	private static final Logger LOGGER = LogManager.getFormatterLogger(EvosuiteWorker.class);

	private final Options options;
	private final Evosuite evosuite;

	public EvosuiteWorker(Options options, Evosuite evosuite, int taskNumber) {
		super(taskNumber);
		this.options = options;
		this.evosuite = evosuite;
	}

	@Override
	public ExitStatus call() throws IOException, InterruptedException, ToolException {
		final String[] p = this.evosuite.getInvocationParameters(this.taskNumber);
		LOGGER.debug("Task %s: invoking %s.", Integer.toString(this.taskNumber), this.evosuite.getCommandLine());
		
		final ProcessBuilder pb = new ProcessBuilder(p).redirectErrorStream(true);
		Process process = null; //to keep the compiler happy
		TestDetector td = null; //to keep the compiler happy
		try {
			final Path logFilePath = getTmpDirPath(this.options).resolve("evosuite-task-" + this.taskNumber + "-" + Thread.currentThread().getName() + ".log");		
			final long start = System.currentTimeMillis();
			process = pb.start();
			td = new TestDetector(this.taskNumber, process.getInputStream(), logFilePath, this.evosuite.getTestGenerationNotifier());
			td.start();
			final int exitStatus = process.waitFor();
			final long elapsed = System.currentTimeMillis() - start;
			LOGGER.debug("Task %s: task ended, elapsed %s seconds.", Integer.toString(this.taskNumber), Long.toString(elapsed/1000));
			td.join();
			final ExitStatus retVal = new ExitStatus(exitStatus);
			return retVal;
		} catch (InterruptedException e) {
			if (td != null) {
				td.interrupt();
			}
			process.destroy();
			throw e;
		}
	}
}
