package sushi.execution.javac;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import sushi.Options;
import sushi.exceptions.JavacException;
import sushi.execution.ExecutionResult;
import sushi.execution.Worker;
import sushi.logging.Logger;
import sushi.util.DirectoryUtils;

public class JavacWorker extends Worker {
	private static final Logger logger = new Logger(JavacWorker.class);
	
	private final Options options;
	private final Javac javac;

	public JavacWorker(Options options, Javac javac, int taskNumber) {
		super(taskNumber);
		this.options = options;
		this.javac = javac;
	}

	@Override
	public ExecutionResult call() throws JavacException {		
		final String[] p = this.javac.getInvocationParameters(this.taskNumber);
		logger.debug("Invoking " + this.javac.getCommandLine());

		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			logger.error("Unable to find javac");
			throw new JavacException(new NullPointerException());
		}

		final Path logFilePath = DirectoryUtils.getTmpDirPath(this.options).resolve("javac-task-" + this.taskNumber + "-" + Thread.currentThread().getName() + ".log");		
		try (final OutputStream w = new BufferedOutputStream(Files.newOutputStream(logFilePath))) {
			final int exitStatus = compiler.run(null, w, w, p);

			final ExecutionResult result = new ExecutionResult();
			result.setExitStatus(exitStatus);
			return result;
		} catch (IOException e) {
			logger.error("I/O error while creating javac log file");
			throw new JavacException(e);
		}
	}
}
