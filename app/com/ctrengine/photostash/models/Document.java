package com.ctrengine.photostash.models;

public interface Document {

	abstract public String getCollection();

	public abstract String getKey();

	public abstract String getDocumentAddress();

}