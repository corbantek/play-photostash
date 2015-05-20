package com.ctrengine.photostash.models;

import java.io.File;
import java.util.Date;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class PhotographDocument extends AbstractFileDocument implements RelateDocument {
	public static final String COLLECTION = "photographs";
	public static final String RELATE_COLLECTION = "photographrelations";

	private String description;
	private long size;
	private long dateTaken;

	public PhotographDocument(File photographFile, long size, long dateTaken) {
		super(photographFile);
		this.description = "";
		this.size = size;
		this.dateTaken = dateTaken;
	}
	
	public String getDescription() {
		return description;
	}

	public long getSize() {
		return size;
	}

	public long getDateTaken() {
		return dateTaken;
	}
	
	@Override
	public String getRelateCollection() {
		return RELATE_COLLECTION;
	}
	
	@Override
	public String getCollection() {
		return COLLECTION;
	}
	
	@Override
	public ObjectNode toJson() {
		ObjectNode photographNode = Json.newObject();
		photographNode.put("photographId", getKey());
		photographNode.put("name", getName());
		return photographNode;
	}

	@Override
	public ObjectNode toJsonExtended() {
		ObjectNode photographNodeExtended = toJson();
		photographNodeExtended.put("description", getDescription());
		photographNodeExtended.put("size", getSize());
		photographNodeExtended.put("dateTaken", getDateTaken());
		return photographNodeExtended;
	}
}
