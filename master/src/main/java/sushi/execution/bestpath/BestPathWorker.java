package sushi.execution.bestpath;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;

import sushi.execution.ExitStatus;
import sushi.execution.Worker;

public final class BestPathWorker extends Worker {
	private final BestPath listPaths;

	public BestPathWorker(BestPath listPaths) {
		this.listPaths = listPaths;
	}

	@Override
	public ExitStatus call() throws IOException, NumberFormatException {
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
		}

		try (final BufferedWriter w = Files.newBufferedWriter(p.getOutputFilePath())) {
			w.write(bestTraceGlobal + ", " + bestMethod + ", " + bestTraceLocal + "\n");
		}
		
		final ExitStatus result = new ExitStatus(0);
		return result;
	}
}
