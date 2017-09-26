package sushi.execution.listpaths;

import sushi.execution.Tool;
import sushi.util.DirectoryUtils;

public final class ListPaths extends Tool<ListPathsParameters> {
	public ListPaths() { }

	@Override
	public ListPathsParameters getInvocationParameters(int i) {
		final DirectoryUtils dirs = DirectoryUtils.I();
		final ListPathsParameters p = new ListPathsParameters();
		p.setCoverageFilePath(dirs.getCoverageFilePath());
		p.setOutputFilePath(dirs.getMinimizerOutFilePath());

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
