package sushi.execution.minimizer;

import sushi.Options;
import sushi.execution.Tool;
import sushi.execution.Worker;
import sushi.util.DirectoryUtils;

public class Minimizer extends Tool<MinimizerParameters> {
	private final Options options;
	
	public Minimizer(Options options) { 
		this.options = options;
	}

	@Override
	public MinimizerParameters getInvocationParameters(int i) {
		final MinimizerParameters p = new MinimizerParameters();
		p.setBranchesFilePath(DirectoryUtils.getBranchesFilePath(this.options));
		p.setCoverageFilePath(DirectoryUtils.getCoverageFilePath(this.options));
		p.setOutputFilePath(DirectoryUtils.getMinimizerOutFilePath(this.options));
		p.setBranchesToIgnoreFilePath(DirectoryUtils.getBranchesToIgnoreFilePath(this.options));
		p.setTracesToIgnoreFilePath(DirectoryUtils.getTracesToIgnoreFilePath(this.options));
		p.setNumberOfTasks(this.options.getParallelismEvosuite() / this.options.getRedundanceEvosuite());
		p.setTimeout(this.options.getMinimizerBudget());
		
		return p;
	}
	
	@Override
	public int getTimeBudget() {
		return this.options.getMinimizerBudget();
	}

	@Override
	public Worker getWorker(int i) {
		return new MinimizerWorker(this);
	}
}
