package sushi.execution.loopmgr;

import sushi.execution.Tool;
import sushi.util.DirectoryUtils;

public final class LoopMgr extends Tool<LoopMgrParameters> {
	public LoopMgr() { }

	@Override
	public LoopMgrParameters getInvocationParameters(int i) {
		final LoopMgrParameters p = new LoopMgrParameters();
		final DirectoryUtils dirs = DirectoryUtils.I();
		p.setBranchesFilePath(dirs.getBranchesFilePath());
		p.setCoverageFilePath(dirs.getCoverageFilePath());
		p.setBranchesToIgnoreFilePath(dirs.getBranchesToIgnoreFilePath());
		p.setTracesToIgnoreFilePath(dirs.getTracesToIgnoreFilePath());
		p.setCoveredByTestFilePath(dirs.getCoveredByTestFilePath());
		p.setMinimizerOutFilePath(dirs.getMinimizerOutFilePath());
		
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
