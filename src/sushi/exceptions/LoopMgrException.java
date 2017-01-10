package sushi.exceptions;

public class LoopMgrException extends RuntimeException {

	private static final long serialVersionUID = -3219563995304871060L;

	public LoopMgrException(final String arg0) {
		super(arg0);
	}

	public LoopMgrException(final Throwable arg0) {
		super(arg0);
	}

	public LoopMgrException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
