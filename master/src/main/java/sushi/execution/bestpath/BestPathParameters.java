package sushi.execution.bestpath;

import java.nio.file.Path;

public final class BestPathParameters {
	private final Path fCoverage;
	private final Path fOutput;
	
	public BestPathParameters(Path fCoverage, Path fOutput) {
		this.fCoverage = fCoverage;
		this.fOutput = fOutput;
	}
	
	public Path getCoverageFilePath() {
		return this.fCoverage;
	}
	
	public Path getOutputFilePath() {
		return this.fOutput;
	}
}
