package sushi.execution.bestpath;

import sushi.Options;
import sushi.execution.Tool;
import sushi.util.DirectoryUtils;

public final class BestPath extends Tool<BestPathParameters> {
	private final Options options;
	
	public BestPath(Options options) {
		this.options = options;
	}

	@Override
	public BestPathParameters getInvocationParameters(int i) {
		final BestPathParameters p = new BestPathParameters();
		p.setCoverageFilePath(DirectoryUtils.getCoverageFilePath(this.options));
		p.setOutputFilePath(DirectoryUtils.getMinimizerOutFilePath(this.options));

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
