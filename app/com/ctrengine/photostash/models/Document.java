package com.ctrengine.photostash.models;

import play.Logger;
import play.Logger.ALogger;

public interface Document extends Comparable<Document> {
	
	public static final ALogger LOGGER = Logger.of("document");

	abstract public String getCollection();

	public abstract String getKey();

	public abstract String getDocumentAddress();

}