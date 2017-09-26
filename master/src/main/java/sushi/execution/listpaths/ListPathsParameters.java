package sushi.execution.listpaths;

import java.nio.file.Path;

public class ListPathsParameters {
	private Path fCoverage;
	private Path fOutput;
	
	public void setCoverageFilePath(Path f) {
		this.fCoverage = f;
	}
	
	public void setOutputFilePath(Path f) {
		this.fOutput = f;
	}
	
	public Path getCoverageFilePath() {
		return this.fCoverage;
	}
	
	public Path getOutputFilePath() {
		return this.fOutput;
	}
}
