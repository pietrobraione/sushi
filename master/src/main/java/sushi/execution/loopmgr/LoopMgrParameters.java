package sushi.execution.loopmgr;

import java.nio.file.Path;

public final class LoopMgrParameters {
	private Path fBranches;
	private Path fCoverage;
	private Path fBranchesToIgnore;
	private Path fTracesToIgnore;
	private Path fCoveredByTest;
	private Path fMinimizerOut;
	
	public void setBranchesFilePath(Path f) {
		this.fBranches = f;
	}
	
	public void setCoverageFilePath(Path f) {
		this.fCoverage = f;
	}
	
	public void setBranchesToIgnoreFilePath(Path f) {
		this.fBranchesToIgnore = f;
	}
	
	public void setTracesToIgnoreFilePath(Path f) {
		this.fTracesToIgnore = f;
	}
	
	public void setCoveredByTestFilePath(Path f) {
		this.fCoveredByTest = f;
	}
	
	public void setMinimizerOutFilePath(Path f) {
		this.fMinimizerOut = f;
	}
	
	public Path getBranchesFilePath() {
		return this.fBranches;
	}
	
	public Path getCoverageFilePath() {
		return this.fCoverage;
	}

	public Path getBranchesToIgnoreFilePath() {
		return this.fBranchesToIgnore;
	}
	
	public Path getTracesToIgnoreFilePath() {
		return this.fTracesToIgnore;
	}	
	
	public Path getCoveredByTestFilePath() {
		return this.fCoveredByTest;
	}	
	
	public Path getMinimizerOutFilePath() {
		return this.fMinimizerOut;
	}	
}
