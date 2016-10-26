package sushi.execution.merger;

import java.nio.file.Path;
import java.util.function.Function;

public class MergerParameters {
	private Path fMethods;
	private Path fBranches;
	private Path fCoverage;
	private Path fTraces;
	private Function<Integer, Path> branchesFilePath;
	private Function<Integer, Path> coverageFilePath;
	private Function<Integer, Path> tracesFilePath;
	
	public void setMethodsFilePath(Path f) {
		this.fMethods = f;
	}
	
	public Path getMethodsFilePath() {
		return this.fMethods;
	}
	
	public void setBranchesFilePathGlobal(Path f) {
		this.fBranches = f;
	}
	
	public Path getBranchesFilePathGlobal() {
		return this.fBranches;
	}
	
	public void setCoverageFilePathGlobal(Path f) {
		this.fCoverage = f;
	}
	
	public Path getCoverageFilePathGlobal() {
		return this.fCoverage;
	}
	
	public void setTracesFilePathGlobal(Path f) {
		this.fTraces = f;
	}
	
	public Path getTracesFilePathGlobal() {
		return this.fTraces;
	}

	public void setBranchesFilePathLocal(Function<Integer, Path> branchesFilePath) {
		this.branchesFilePath = branchesFilePath;
	}
	
	public Path getBranchesFilePathLocal(int i) {
		return this.branchesFilePath.apply(i);
	}
	
	public void setCoverageFilePathLocal(Function<Integer, Path> coverageFilePath) {
		this.coverageFilePath = coverageFilePath;
	}
	
	public Path getCoverageFilePathLocal(int i) {
		return this.coverageFilePath.apply(i);
	}

	public void setTracesFilePathLocal(Function<Integer, Path> tracesFilePath) {
		this.tracesFilePath = tracesFilePath;
	}
	
	public Path getTracesFilePathLocal(int i) {
		return this.tracesFilePath.apply(i);
	}
}
