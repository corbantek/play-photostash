package com.ctrengine.photostash.models;

public abstract class AbstractDocument {
	private String _key;
	
	public String getKey(){
		return _key;
	}
	
	public void setKey(String key){
		_key = key;
	}
	
	abstract protected String getCollection();
	
	public String getDocumentAddress(){
		return getCollection()+"/"+_key;
	}
}
