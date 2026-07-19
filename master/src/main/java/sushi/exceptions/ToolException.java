package sushi.exceptions;

public class ToolException extends Exception {

	private static final long serialVersionUID = 1613249986163088382L;

	public ToolException(Throwable cause) {
		super(cause);
	}

	public ToolException(String message) {
		super(message);
	}
}
