package com.ctrengine.photostash.database;

public class DatabaseFilter {
	private final String key;
	private final Object value;
	private final DatabaseFilterType databaseFilterType;
	
	public DatabaseFilter(String key, Object value, DatabaseFilterType databaseFilterType) {
		super();
		this.key = key;
		this.value = value;
		this.databaseFilterType = databaseFilterType;
	}

	public String getKey() {
		return key;
	}

	public Object getValue() {
		return value;
	}

	public DatabaseFilterType getDatabaseFilterType() {
		return databaseFilterType;
	}
}
