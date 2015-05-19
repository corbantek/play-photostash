package com.ctrengine.photostash.models;

import java.io.File;
import java.util.Date;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Photograph extends AbstractFileDocument {
	public static final String COLLECTION = "photographs";

	private String description;
	private long size;
	private long dateTaken;

	public Photograph(File photographFile, long size, long dateTaken) {
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
	public String getCollection() {
		return COLLECTION;
	}
	
	@Override
	public ObjectNode toJson() {
		ObjectNode storyNode = Json.newObject();
		storyNode.put("photographId", getKey());
		storyNode.put("name", getName());
		return storyNode;
	}

	@Override
	public ObjectNode toJsonExtended() {
		ObjectNode storyNodeExtended = toJson();
		storyNodeExtended.put("description", getDescription());
		storyNodeExtended.put("size", getSize());
		storyNodeExtended.put("dateTaken", getDateTaken());
		return storyNodeExtended;
	}
}
