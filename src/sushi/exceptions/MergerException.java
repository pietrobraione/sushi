package sushi.exceptions;

public class MergerException extends RuntimeException {

	private static final long serialVersionUID = -3290763187363713060L;

	public MergerException(final String arg0) {
		super(arg0);
	}

	public MergerException(final Throwable arg0) {
		super(arg0);
	}

	public MergerException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

}
