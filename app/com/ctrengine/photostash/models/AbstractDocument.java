package com.ctrengine.photostash.models;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractDocument implements Document {
	private String _key;	
	
	abstract public ObjectNode toJson();
	
	abstract public ObjectNode toJsonExtended();

	public AbstractDocument() {
	}
	
	public AbstractDocument(String key) {
		this._key = key;
	}
	
	public ObjectNode toJson(boolean extended){
		if(extended){
			return toJsonExtended();
		}else{
			return toJson();
		}
	}
	
	@Override
	abstract public String getCollection();

	@Override
	public String getKey(){
		return _key;
	}
	
	protected void setKey(String key){
		this._key = key;
	}
	
	@Override
	public String getDocumentAddress(){
		return getCollection()+"/"+_key;
	}

	@Override
	public int compareTo(Document o) {
		return getKey().compareTo(o.getKey());
	}
	
	
}
