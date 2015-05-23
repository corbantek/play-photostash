package com.ctrengine.photostash.database;

public class DatabaseFilter {
	public static enum DatabaseFilterType {
		EQUAL("=="), LESS_THAN("<"), GREATER_THAN(">"), LESS_EQUAL_THAN("<="), GREATER_EQUAL_THAN(">=");

		private final String operand;

		private DatabaseFilterType(String operand) {
			this.operand = operand;
		}

		public String getOperand() {
			return operand;
		}
	}

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

	public String getOperand() {
		return databaseFilterType.getOperand();
	}
}
