package com.ctrengine.photostash.models;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractDocument implements Document {
	private final String _key;	
	
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
	
	@Override
	abstract public String getCollection();

	@Override
	public String getKey(){
		return _key;
	}
	
	@Override
	public String getDocumentAddress(){
		return getCollection()+"/"+_key;
	}
}
