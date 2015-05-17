package com.ctrengine.photostash.database;

public class PhotostashDatabaseException extends Exception {

	public PhotostashDatabaseException() {
		super();
	}

	public PhotostashDatabaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PhotostashDatabaseException(String message, Throwable cause) {
		super(message, cause);
	}

	public PhotostashDatabaseException(String message) {
		super(message);
	}

	public PhotostashDatabaseException(Throwable cause) {
		super(cause);
	}

}
