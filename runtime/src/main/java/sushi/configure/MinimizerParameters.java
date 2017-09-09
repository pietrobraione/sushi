package sushi.configure;

import java.nio.file.Path;

public class MinimizerParameters {
	private Path fBranches;
	private Path fCoverage;
	private Path fOutput;
	private Path fBranchesToIgnore;
	private Path fTracesToIgnore;
	private int numberOfTasks;
	private int timeout;
	
	public void setBranchesFilePath(Path f) {
		this.fBranches = f;
	}
	
	public void setCoverageFilePath(Path f) {
		this.fCoverage = f;
	}
	
	public void setOutputFilePath(Path f) {
		this.fOutput = f;
	}

	public void setBranchesToIgnoreFilePath(Path f) {
		this.fBranchesToIgnore = f;
	}
	
	public void setTracesToIgnoreFilePath(Path f) {
		this.fTracesToIgnore = f;
	}
	
	public void setNumberOfTasks(int numberOfTasks) {
		this.numberOfTasks = numberOfTasks;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public Path getBranchesFilePath() {
		return this.fBranches;
	}
	
	public Path getCoverageFilePath() {
		return this.fCoverage;
	}
	
	public Path getOutputFilePath() {
		return this.fOutput;
	}
	
	public Path getBranchesToIgnoreFilePath() {
		return this.fBranchesToIgnore;
	}
	
	public Path getTracesToIgnoreFilePath() {
		return this.fTracesToIgnore;
	}
	
	public int getNumberOfTasks() {
		return this.numberOfTasks;
	}
	
	public int getTimeout() {
		return this.timeout;
	}	
}
