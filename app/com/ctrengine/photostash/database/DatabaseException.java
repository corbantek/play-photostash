package com.ctrengine.photostash.database;

import com.arangodb.ArangoException;
import com.arangodb.entity.BaseEntity;

public class DatabaseException extends ArangoException {
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


}
