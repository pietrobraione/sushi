package sushi.execution.listpaths;

import sushi.Options;
import sushi.execution.Tool;
import sushi.util.DirectoryUtils;

public final class ListPaths extends Tool<ListPathsParameters> {
	private final Options options;
	
	public ListPaths(Options options) {
		this.options = options;
	}

	@Override
	public ListPathsParameters getInvocationParameters(int i) {
		final ListPathsParameters p = new ListPathsParameters();
		p.setCoverageFilePath(DirectoryUtils.getCoverageFilePath(this.options));
		p.setOutputFilePath(DirectoryUtils.getMinimizerOutFilePath(this.options));

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
