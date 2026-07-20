package sushi.execution;

public final class ExitStatus {
	private final int exitStatus;

	public ExitStatus(int exitStatus) { 
		this.exitStatus = exitStatus;
	}

	public int getExitStatus() {
		return exitStatus;
	}
}
