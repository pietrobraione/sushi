package sushi.execution.merger;

import sushi.configure.MergerParameters;
import sushi.execution.Tool;
import sushi.modifier.Modifier;
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
		p.setBranchesFilePathLocal(dirUtils::getBranchesFilePath);
		p.setCoverageFilePathLocal(dirUtils::getCoverageFilePath);
		p.setTracesFilePathLocal(dirUtils::getTracesFilePath);
		p.setBranchesToIgnoreFilePath(dirUtils.getBranchesToIgnoreFilePath());
		
		setUserDefinedParameters(p);

		return p;
	}
	
	private void setUserDefinedParameters(MergerParameters p) {
	    Modifier.I().modify(p);
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
