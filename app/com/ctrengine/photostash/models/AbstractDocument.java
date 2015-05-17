package com.ctrengine.photostash.models;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractDocument {
	private final String _key;	
	
	abstract protected String getCollection();
	
	abstract public ObjectNode toJson();
	
	abstract public ObjectNode toJsonExtended();
	
	public ObjectNode toJson(boolean extended){
		if(extended){
			return toJsonExtended();
		}else{
			return toJson();
		}
	}

	public AbstractDocument(String key) {
		this._key = key;
	}

	public String getKey(){
		return _key;
	}
	
	public String getDocumentAddress(){
		return getCollection()+"/"+_key;
	}
}
