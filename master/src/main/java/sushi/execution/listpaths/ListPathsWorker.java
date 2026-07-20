package sushi.execution.listpaths;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;

import sushi.execution.ExitStatus;
import sushi.execution.Worker;

public final class ListPathsWorker extends Worker {
	private final ListPaths listPaths;

	public ListPathsWorker(ListPaths listPaths) {
		this.listPaths = listPaths;
	}

	@Override
	public ExitStatus call() throws IOException {
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
		}
		
		final ExitStatus result = new ExitStatus(0);
		return result;
	}
}
