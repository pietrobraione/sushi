package sushi.execution.bestpath;

import sushi.execution.Tool;
import sushi.util.DirectoryUtils;

public final class BestPath extends Tool<BestPathParameters> {
	public BestPath() { }

	@Override
	public BestPathParameters getInvocationParameters(int i) {
		final DirectoryUtils dirs = DirectoryUtils.I();
		final BestPathParameters p = new BestPathParameters();
		p.setCoverageFilePath(dirs.getCoverageFilePath());
		p.setOutputFilePath(dirs.getMinimizerOutFilePath());

		return p;
	}

	@Override
	public int getTimeBudget() {
		return 180; //TODO
	}
	
	@Override
	public BestPathWorker getWorker(int taskNumber) {
		return new BestPathWorker(this);
	}
}
