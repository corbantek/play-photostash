package com.ctrengine.photostash.database;

public enum DatabaseFilterType {
	EQUAL ("=="),
	LESS_THAN ("<"),
	GREATER_THAN (">"),
	LESS_EQUAL_THAN ("<="),
	GREATER_EQUAL_THAN (">=");
	
	private final String operand;
	
	private DatabaseFilterType(String operand){
		this.operand = operand;
	}

	public String getOperand() {
		return operand;
	}	
}
