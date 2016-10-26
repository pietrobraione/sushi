package sushi.execution.bestpath;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;

import sushi.exceptions.BestPathException;
import sushi.execution.ExecutionResult;
import sushi.execution.Worker;
import sushi.logging.Logger;

public class BestPathWorker extends Worker {
	private static final Logger logger = new Logger(BestPathWorker.class);
	
	private final BestPath listPaths;

	public BestPathWorker(BestPath listPaths) {
		this.listPaths = listPaths;
	}

	@Override
	public ExecutionResult call() throws BestPathException {
		final BestPathParameters p = this.listPaths.getInvocationParameters(this.taskNumber);

		int bestTraceGlobal = 0; //to keep the compiler happy
		int bestMethod = 0;  //to keep the compiler happy
		int bestTraceLocal = 0;  //to keep the compiler happy
		int bestWeight = 0; //to keep the compiler happy
		
		int traceGlobal = 0;
		try (final BufferedReader r = Files.newBufferedReader(p.getCoverageFilePath())) {
			String line;
			boolean first = true;
			while ((line = r.readLine()) != null) {
				final String[] fields = line.split(",");
				final int method = Integer.parseInt(fields[0].trim());
				final int traceLocal = Integer.parseInt(fields[1].trim());
				final int weight = Integer.parseInt(fields[2].trim());
				if (first || weight < bestWeight) {
					bestTraceGlobal = traceGlobal;
					bestMethod = method;
					bestTraceLocal = traceLocal;
					bestWeight = weight;
					first = false;
				}
				++traceGlobal;
			}
		} catch (IOException e) {
			logger.error("I/O error while reading file " + p.getCoverageFilePath().toString());
			throw new BestPathException(e);
		} catch (NumberFormatException e) {
			logger.error("File " + p.getCoverageFilePath().toString() + " has wrong format, expected number is missing");
			throw new BestPathException(e);
		}

		try (final BufferedWriter w = Files.newBufferedWriter(p.getOutputFilePath())) {
			w.write(bestTraceGlobal + ", " + bestMethod + ", " + bestTraceLocal + "\n");
		} catch (IOException e) {
			logger.error("I/O error while writing file " + p.getOutputFilePath().toString());
			throw new BestPathException(e);
		}
		
		final ExecutionResult result = new ExecutionResult();
		result.setExitStatus(0);

		return result;
	}
}
