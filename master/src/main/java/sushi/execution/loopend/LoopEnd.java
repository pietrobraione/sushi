package sushi.execution.loopend;

import sushi.execution.Tool;

public final class LoopEnd extends Tool<Object> {
	public LoopEnd() { }

	@Override
	public Object getInvocationParameters(int i) {
		return null; //no parameters
	}
	
	@Override
	public int getTimeBudget() {
		return 180; //TODO
	}
	
	@Override
	public LoopEndWorker getWorker(int taskNumber) {
		return new LoopEndWorker();
	}
}
