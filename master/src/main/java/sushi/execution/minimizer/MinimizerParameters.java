package sushi.execution.minimizer;

import java.nio.file.Path;

class MinimizerParameters {
	private Path fBranches;
	private Path fCoverage;
	private Path fOutput;
	private Path fBranchesToIgnore;
	private Path fTracesToIgnore;
	private int numberOfTasks;
	private int timeout;
	
	public Path getBranchesFilePath() {
		return this.fBranches;
	}
	
	public void setBranchesFilePath(Path f) {
		this.fBranches = f;
	}
	
	public Path getCoverageFilePath() {
		return this.fCoverage;
	}
	
	public void setCoverageFilePath(Path f) {
		this.fCoverage = f;
	}
	
	public Path getOutputFilePath() {
		return this.fOutput;
	}
	
	public void setOutputFilePath(Path f) {
		this.fOutput = f;
	}

	public Path getBranchesToIgnoreFilePath() {
		return this.fBranchesToIgnore;
	}
	
	public void setBranchesToIgnoreFilePath(Path f) {
		this.fBranchesToIgnore = f;
	}
	
	public Path getTracesToIgnoreFilePath() {
		return this.fTracesToIgnore;
	}
	
	public void setTracesToIgnoreFilePath(Path f) {
		this.fTracesToIgnore = f;
	}
	
	public int getNumberOfTasks() {
		return this.numberOfTasks;
	}
	
	public void setNumberOfTasks(int numberOfTasks) {
		this.numberOfTasks = numberOfTasks;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public int getTimeout() {
		return this.timeout;
	}	
}
