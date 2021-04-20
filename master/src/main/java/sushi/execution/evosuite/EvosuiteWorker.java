package sushi.execution.evosuite;

import java.io.IOException;
import java.nio.file.Path;

import sushi.exceptions.EvosuiteException;
import sushi.execution.ExecutionResult;
import sushi.execution.Worker;
import sushi.logging.Logger;
import sushi.util.DirectoryUtils;

public class EvosuiteWorker extends Worker {
	private static final Logger logger = new Logger(EvosuiteWorker.class);

	private final Evosuite evosuite;

	public EvosuiteWorker(Evosuite evosuite, int taskNumber) {
		super(taskNumber);
		this.evosuite = evosuite;
	}

	@Override
	public ExecutionResult call() throws EvosuiteException, InterruptedException {
		final String[] p = this.evosuite.getInvocationParameters(this.taskNumber);
		logger.debug("Task " + this.taskNumber + ": invoking " + this.evosuite.getCommandLine());
		
		final Path logFilePath = DirectoryUtils.I().getTmpDirPath().resolve("evosuite-task-" + this.taskNumber + "-" + Thread.currentThread().getName() + ".log");		
		final ProcessBuilder pb = new ProcessBuilder(p).redirectErrorStream(true);
		Process process = null; //to keep the compiler happy
		TestDetector td = null; //to keep the compiler happy
		try {
			final long start = System.currentTimeMillis();
			process = pb.start();
			td = new TestDetector(this.taskNumber, process.getInputStream(), logFilePath, this.evosuite.getTestGenerationNotifier());
			td.start();
			final int exitStatus = process.waitFor();
			final long elapsed = System.currentTimeMillis() - start;
			logger.debug("Task " + this.taskNumber + " ended, elapsed " + elapsed/1000 + " seconds");
			td.join();
			final ExecutionResult result = new ExecutionResult();
			result.setExitStatus(exitStatus);
			return result;
		} catch (IOException e) {
			logger.error("I/O error while creating evosuite process or log file");
			throw new EvosuiteException(e);
		} catch (InterruptedException e) {
			if (td != null) {
				td.interrupt();
			}
			process.destroy();
			throw e;
		}
	}
}
