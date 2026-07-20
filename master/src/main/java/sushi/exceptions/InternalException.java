package sushi.exceptions;

public class InternalException extends Exception {
	private static final long serialVersionUID = -6879145894616444284L;
	
	private final String tool;
	private final int task;
	private final int toolExitStatus;
	
	public InternalException(String tool, Throwable cause) {
		super(cause);
		this.tool = tool; 
		this.task = -1; 
		this.toolExitStatus = -1; 
	}
	
	public InternalException(String tool, String message) {
		super(message);
		this.tool = tool; 
		this.task = -1; 
		this.toolExitStatus = -1; 
	}
	
	public InternalException(String tool, int task, Throwable cause) {
		super(cause);
		this.tool = tool; 
		this.task = task; 
		this.toolExitStatus = -1; 
	}
	
	public InternalException(String tool, int task, String message) {
		super(message);
		this.tool = tool; 
		this.task = task; 
		this.toolExitStatus = -1; 
	}

	public InternalException(String tool, int task, int toolExitStatus) {
		super();
		this.tool = tool; 
		this.task = task; 
		this.toolExitStatus = toolExitStatus; 
	}
	
	public String getTool() {
		return this.tool;
	}
	
	public int getTask() {
		return this.task;
	}
	
	public int getToolExitStatus() {
		return this.toolExitStatus;
	}	
}
