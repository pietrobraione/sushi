package sushi.execution.merger;

import sushi.execution.Tool;
import sushi.util.DirectoryUtils;

public final class Merger extends Tool<MergerParameters> {
	public Merger() { }

	@Override
	public MergerParameters getInvocationParameters(int i) {
		final DirectoryUtils dirUtils = DirectoryUtils.I();
		final MergerParameters p = new MergerParameters();
		p.setMethodsFilePath(dirUtils.getMethodsFilePath());
		p.setBranchesFilePathGlobal(dirUtils.getBranchesFilePath());
		p.setCoverageFilePathGlobal(dirUtils.getCoverageFilePath());
		p.setTracesFilePathGlobal(dirUtils.getTracesFilePath());
		p.setBranchesFilePathLocal(DirectoryUtils.I()::getBranchesFilePath);
		p.setCoverageFilePathLocal(DirectoryUtils.I()::getCoverageFilePath);
		p.setTracesFilePathLocal(DirectoryUtils.I()::getTracesFilePath);

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
