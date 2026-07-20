package sushi.execution.listpaths;

import static sushi.util.DirectoryUtils.getCoverageFilePath;
import static sushi.util.DirectoryUtils.getMinimizerOutFilePath;

import sushi.Options;
import sushi.execution.Tool;

public final class ListPaths extends Tool<ListPathsParameters> {
	private final Options options;
	
	public ListPaths(Options options) {
		this.options = options;
	}

	@Override
	public ListPathsParameters getInvocationParameters(int i) {
		final ListPathsParameters p = new ListPathsParameters();
		p.setCoverageFilePath(getCoverageFilePath(this.options));
		p.setOutputFilePath(getMinimizerOutFilePath(this.options));

		return p;
	}

	@Override
	public int getTimeBudget() {
		return 180; //TODO
	}
	
	@Override
	public ListPathsWorker getWorker(int taskNumber) {
		return new ListPathsWorker(this);
	}
}
