package sushi.execution.merger;

import sushi.Options;
import sushi.execution.Tool;
import sushi.util.DirectoryUtils;

public final class Merger extends Tool<MergerParameters> {
	private final Options options;
	
	public Merger(Options options) { 
		this.options = options;
	}

	@Override
	public MergerParameters getInvocationParameters(int i) {
		final MergerParameters p = new MergerParameters();
		p.setMethodsFilePath(DirectoryUtils.getMethodsFilePath(this.options));
		p.setBranchesFilePathGlobal(DirectoryUtils.getBranchesFilePath(this.options));
		p.setCoverageFilePathGlobal(DirectoryUtils.getCoverageFilePath(this.options));
		p.setTracesFilePathGlobal(DirectoryUtils.getTracesFilePath(this.options));
		p.setBranchesFilePathLocal((n) -> DirectoryUtils.getBranchesFilePath(this.options, n));
		p.setCoverageFilePathLocal((n) -> DirectoryUtils.getCoverageFilePath(this.options, n));
		p.setTracesFilePathLocal((n) -> DirectoryUtils.getTracesFilePath(this.options, n));
		p.setBranchesToIgnoreFilePath(DirectoryUtils.getBranchesToIgnoreFilePath(this.options));
		p.setTracesToIgnoreFilePath(DirectoryUtils.getTracesToIgnoreFilePath(this.options));
		p.setBranchesToIgnore(this.options.getBranchesToIgnore());
		p.setBranchesToCover(this.options.getBranchesToCover());

		return p;
	}
	
	@Override
	public int getTimeBudget() {
		return 180; //TODO
	}
	
	@Override
	public MergerWorker getWorker(int taskNumber) {
		return new MergerWorker(this);
	}
}
