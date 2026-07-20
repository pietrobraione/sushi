package sushi.exceptions;

public class NoTmpDirException extends Exception {

	private static final long serialVersionUID = -4530237090359832473L;

	public NoTmpDirException(Throwable e) {
		super(e);
	}
}
