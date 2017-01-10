package sushi.execution.minimizer;

import sushi.configure.MinimizerParameters;
import sushi.configure.Options;
import sushi.execution.Tool;
import sushi.execution.Worker;
import sushi.modifier.Modifier;
import sushi.util.DirectoryUtils;

public class Minimizer extends Tool<MinimizerParameters> {
	public Minimizer() { }

	@Override
	public MinimizerParameters getInvocationParameters(int i) {
		final DirectoryUtils dirs = DirectoryUtils.I();
		final MinimizerParameters p = new MinimizerParameters();
		p.setBranchesFilePath(dirs.getBranchesFilePath());
		p.setCoverageFilePath(dirs.getCoverageFilePath());
		p.setOutputFilePath(dirs.getMinimizerOutFilePath());
		p.setBranchesToIgnoreFilePath(dirs.getBranchesToIgnoreFilePath());
		p.setTracesToIgnoreFilePath(dirs.getTracesToIgnoreFilePath());
		p.setNumberOfTasks(Options.I().getParallelismEvosuite() / Options.I().getRedundanceEvosuite());
		
		setUserDefinedParameters(p);
		
		return p;
	}
	
	private void setUserDefinedParameters(MinimizerParameters p) {
	    Modifier.I().modify(p);
	}
	
	@Override
	public int getTimeBudget() {
		return Options.I().getMinimizerBudget();
	}

	@Override
	public Worker getWorker(int i) {
		return new MinimizerWorker(this);
	}
}
