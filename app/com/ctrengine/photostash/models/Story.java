package com.ctrengine.photostash.models;

import java.io.File;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Story extends AbstractFileDocument {
	public static final String COLLECTION = "storys";
	public static final String PATH = "path";
	
	private String description;
	private int size;
	private int stashSize;

	public Story(File storyFile, int size, int stashSize) {
		super(storyFile);
		this.description = "";
		this.size = size;
		this.stashSize = stashSize;
	}

	public String getDescription() {
		return description;
	}

	public long getSize() {
		return size;
	}

	public long getStashSize() {
		return stashSize;
	}	
	
	@Override
	protected String getCollection() {
		return COLLECTION;
	}
	
	@Override
	public ObjectNode toJson() {
		ObjectNode storyNode = Json.newObject();
		storyNode.put("storyId", getKey());
		storyNode.put("name", getName());
		return storyNode;
	}

	@Override
	public ObjectNode toJsonExtended() {
		ObjectNode storyNodeExtended = toJson();
		storyNodeExtended.put("description", getDescription());
		storyNodeExtended.put("size", getSize());
		storyNodeExtended.put("stashSize", getStashSize());
		return storyNodeExtended;
	}
}
