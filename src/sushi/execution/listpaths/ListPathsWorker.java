package sushi.execution.listpaths;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;

import sushi.exceptions.ListPathsException;
import sushi.execution.ExecutionResult;
import sushi.execution.Worker;
import sushi.logging.Logger;

public class ListPathsWorker extends Worker {
	private static final Logger logger = new Logger(ListPathsWorker.class);
	
	private final ListPaths listPaths;

	public ListPathsWorker(ListPaths listPaths) {
		this.listPaths = listPaths;
	}

	@Override
	public ExecutionResult call() throws ListPathsException {
		final ListPathsParameters p = this.listPaths.getInvocationParameters(this.taskNumber);

		int traceGlobal = 0;
		try (final BufferedReader r = Files.newBufferedReader(p.getCoverageFilePath());
			 final BufferedWriter w = Files.newBufferedWriter(p.getOutputFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				final String[] fields = line.split(",");
				final int method = Integer.parseInt(fields[0].trim());
				final int traceLocal = Integer.parseInt(fields[1].trim());
				w.write(traceGlobal + ", " + method + ", " + traceLocal + "\n");
				++traceGlobal;
			}
		} catch (IOException e) {
			logger.error("I/O error while reading file " + p.getCoverageFilePath().toString() + " or writing file " + p.getOutputFilePath().toString());
			throw new ListPathsException(e);
		} catch (NumberFormatException e) {
			logger.error("File " + p.getCoverageFilePath().toString() + " has wrong format, expected number is missing");
			throw new ListPathsException(e);
		}
		
		final ExecutionResult result = new ExecutionResult();
		result.setExitStatus(0);

		return result;
	}
}
