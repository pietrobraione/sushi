package sushi.execution.loopmgr;

import sushi.Options;
import sushi.execution.Tool;
import sushi.util.DirectoryUtils;

public final class LoopMgr extends Tool<LoopMgrParameters> {
	private final Options options;
	
	public LoopMgr(Options options) {
		this.options = options;
	}

	@Override
	public LoopMgrParameters getInvocationParameters(int i) {
		final LoopMgrParameters p = new LoopMgrParameters();
		p.setBranchesFilePath(DirectoryUtils.getBranchesFilePath(this.options));
		p.setCoverageFilePath(DirectoryUtils.getCoverageFilePath(this.options));
		p.setBranchesToIgnoreFilePath(DirectoryUtils.getBranchesToIgnoreFilePath(this.options));
		p.setTracesToIgnoreFilePath(DirectoryUtils.getTracesToIgnoreFilePath(this.options));
		p.setCoveredByTestFilePath(DirectoryUtils.getCoveredByTestFilePath(this.options));
		p.setMinimizerOutFilePath(DirectoryUtils.getMinimizerOutFilePath(this.options));
		
		//no user defined parameters

		return p;
	}
	
	@Override
	public int getTimeBudget() {
		return 180; //TODO
	}
	
	@Override
	public LoopMgrWorker getWorker(int taskNumber) {
		return new LoopMgrWorker(this);
	}
}
