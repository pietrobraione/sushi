package sushi.execution.javac;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sushi.Options;
import sushi.exceptions.WorkerFailureException;
import sushi.execution.ExitStatus;
import sushi.execution.Worker;
import sushi.util.DirectoryUtils;

public final class JavacWorker extends Worker {
	private static final Logger LOGGER = LogManager.getFormatterLogger(JavacWorker.class);
	
	private final Options options;
	private final Javac javac;

	public JavacWorker(Options options, Javac javac, int taskNumber) {
		super(taskNumber);
		this.options = options;
		this.javac = javac;
	}

	@Override
	public ExitStatus call() throws IOException, WorkerFailureException {		
		final String[] p = this.javac.getInvocationParameters(this.taskNumber);
		LOGGER.debug("Task %s: invoking %s.", Integer.toString(this.taskNumber), this.javac.getCommandLine());

		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new WorkerFailureException(this.taskNumber, "Unable to find javac.");
		}

		final Path logFilePath = DirectoryUtils.getTmpDirPath(this.options).resolve("javac-task-" + this.taskNumber + "-" + Thread.currentThread().getName() + ".log");		
		try (final OutputStream w = new BufferedOutputStream(Files.newOutputStream(logFilePath))) {
			final int exitStatus = compiler.run(null, w, w, p);
			final ExitStatus result = new ExitStatus(exitStatus);
			return result;
		}
	}
}
