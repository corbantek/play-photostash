package com.ctrengine.photostash.models;

public interface Document extends Comparable<Document> {

	abstract public String getCollection();

	public abstract String getKey();

	public abstract String getDocumentAddress();

}