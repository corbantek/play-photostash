package com.ctrengine.photostash.models;

import java.io.File;
import java.util.Date;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Photograph extends AbstractFileDocument {
	public static final String COLLECTION = "photographs";

	private String description;
	private int size;
	private Date dateTaken;

	public Photograph(File photographFile, String description, int size, Date dateTaken) {
		super(photographFile);
		this.description = description;
		this.size = size;
		this.dateTaken = dateTaken;
	}
	
	public String getDescription() {
		return description;
	}

	public int getSize() {
		return size;
	}

	public Date getDateTaken() {
		return dateTaken;
	}
	
	@Override
	public String getCollection() {
		return COLLECTION;
	}
	
	@Override
	public ObjectNode toJson() {
		ObjectNode storyNode = Json.newObject();
		storyNode.put("photoId", getKey());
		storyNode.put("name", getName());
		return storyNode;
	}

	@Override
	public ObjectNode toJsonExtended() {
		ObjectNode storyNodeExtended = toJson();
		storyNodeExtended.put("description", getDescription());
		storyNodeExtended.put("size", getSize());
		storyNodeExtended.put("dateTaken", getDateTaken().getTime());
		return storyNodeExtended;
	}
}
