package com.ctrengine.photostash.database;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.arangodb.ArangoException;
import com.arangodb.entity.BaseEntity;

public class DatabaseException extends ArangoException {
	private static final Pattern ERROR_PATTERN = Pattern.compile("\\[\\d+\\]");
	private static final long serialVersionUID = 5538351870152319346L;

	public DatabaseException() {
		super();
	}

	public DatabaseException(BaseEntity entity) {
		super(entity);
	}

	public DatabaseException(String message, Throwable cause) {
		super(message, cause);
	}

	public DatabaseException(String message) {
		super(message);
	}

	public DatabaseException(Throwable cause) {
		super(cause);
	}

	@Override
	public int getErrorNumber() {
		String errorMessage = getErrorMessage();
		Matcher matcher = ERROR_PATTERN.matcher(errorMessage);
		if (matcher.find()) {
			String errorNumber = errorMessage.substring(matcher.start() + 1, matcher.end() - 1);
			try {
				return Integer.parseInt(errorNumber);
			} catch (NumberFormatException e) {
			}
		} 
		return super.getErrorNumber();
	}

}
