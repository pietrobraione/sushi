package sushi.execution.bestpath;

import static sushi.util.DirectoryUtils.getCoverageFilePath;
import static sushi.util.DirectoryUtils.getMinimizerOutFilePath;

import sushi.Options;
import sushi.execution.Tool;

public final class BestPath extends Tool<BestPathParameters> {
	private final Options options;
	
	public BestPath(Options options) {
		this.options = options;
	}

	@Override
	public BestPathParameters getInvocationParameters(int i) {
		final BestPathParameters p = new BestPathParameters(getCoverageFilePath(this.options),
		getMinimizerOutFilePath(this.options));
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
