package com.ctrengine.photostash.shoebox;

public class ShoeboxException extends Exception {

	public ShoeboxException() {
		super();
	}

	public ShoeboxException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ShoeboxException(String message, Throwable cause) {
		super(message, cause);
	}

	public ShoeboxException(String message) {
		super(message);
	}

	public ShoeboxException(Throwable cause) {
		super(cause);
	}

}
