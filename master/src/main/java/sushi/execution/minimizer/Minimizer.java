package sushi.execution.minimizer;

import static sushi.util.DirectoryUtils.getBranchesFilePath;
import static sushi.util.DirectoryUtils.getBranchesToIgnoreFilePath;
import static sushi.util.DirectoryUtils.getCoverageFilePath;
import static sushi.util.DirectoryUtils.getMinimizerOutFilePath;
import static sushi.util.DirectoryUtils.getTracesToIgnoreFilePath;


import sushi.Options;
import sushi.execution.Tool;
import sushi.execution.Worker;

public class Minimizer extends Tool<MinimizerParameters> {
	private final Options options;
	
	public Minimizer(Options options) { 
		this.options = options;
	}

	@Override
	public MinimizerParameters getInvocationParameters(int taskNumber) {
		final MinimizerParameters p = new MinimizerParameters();
		p.setTaskNumber(taskNumber);
		p.setBranchesFilePath(getBranchesFilePath(this.options));
		p.setCoverageFilePath(getCoverageFilePath(this.options));
		p.setOutputFilePath(getMinimizerOutFilePath(this.options));
		p.setBranchesToIgnoreFilePath(getBranchesToIgnoreFilePath(this.options));
		p.setTracesToIgnoreFilePath(getTracesToIgnoreFilePath(this.options));
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
