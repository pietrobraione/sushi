package sushi.execution.merger;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.regex.Pattern;

class MergerParameters {
	private Path fMethods;
	private Path fBranches;
	private Path fCoverage;
	private Path fTraces;
	private Function<Integer, Path> branchesFilePath;
	private Function<Integer, Path> coverageFilePath;
	private Function<Integer, Path> tracesFilePath;
	private Pattern toIgnore;
	private Pattern toCover;
	private Path fBranchesToIgnore;
	private Path fTracesToIgnore;
	
	public Path getMethodsFilePath() {
		return this.fMethods;
	}
	
	public void setMethodsFilePath(Path f) {
		this.fMethods = f;
	}
	
	public Path getBranchesFilePathGlobal() {
		return this.fBranches;
	}
	
	public void setBranchesFilePathGlobal(Path f) {
		this.fBranches = f;
	}
	
	public Path getCoverageFilePathGlobal() {
		return this.fCoverage;
	}
	
	public void setCoverageFilePathGlobal(Path f) {
		this.fCoverage = f;
	}
	
	public Path getTracesFilePathGlobal() {
		return this.fTraces;
	}

	public void setTracesFilePathGlobal(Path f) {
		this.fTraces = f;
	}
	
	public Path getBranchesFilePathLocal(int i) {
		return this.branchesFilePath.apply(i);
	}
	
	public void setBranchesFilePathLocal(Function<Integer, Path> branchesFilePath) {
		this.branchesFilePath = branchesFilePath;
	}
	
	public Path getCoverageFilePathLocal(int i) {
		return this.coverageFilePath.apply(i);
	}

	public void setCoverageFilePathLocal(Function<Integer, Path> coverageFilePath) {
		this.coverageFilePath = coverageFilePath;
	}
	
	public Path getTracesFilePathLocal(int i) {
		return this.tracesFilePath.apply(i);
	}
		
	public void setTracesFilePathLocal(Function<Integer, Path> tracesFilePath) {
		this.tracesFilePath = tracesFilePath;
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
	
	public Pattern getBranchesToIgnore() {
		return this.toIgnore;
	}
	
	public void setBranchesToIgnore(Pattern pattern) {
		this.toIgnore = pattern;
		this.toCover = null;
	}
	
	public Pattern getBranchesToCover() {
		return this.toCover;
	}

	public void setBranchesToCover(Pattern pattern) {
		this.toIgnore = null;
		this.toCover = pattern;
	}
}
