package com.ctrengine.photostash.models;

import java.io.File;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class AlbumDocument extends AbstractFileDocument implements RelateDocument {
	public static final String COLLECTION = "albums";
	public static final String RELATE_COLLECTION = "albumrelations";

	private String description;

	public AlbumDocument(File albumFile) {
		super(albumFile);
		generateAlbumKey(albumFile);
		this.description = "";
	}
	
	private void generateAlbumKey(File file){
		setKey(file.getName().trim().replaceAll("[^A-Za-z\\-\\d\\s]+", "").replaceAll("\\s+-\\s+", "-").replaceAll("\\s+", "-").toLowerCase());
	}

	public String getDescription() {
		return description;
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
		ObjectNode albumNode = Json.newObject();
		albumNode.put("albumId", getKey());
		albumNode.put("name", getName());
		return albumNode;
	}

	@Override
	public ObjectNode toJsonExtended() {
		ObjectNode albumNodeExtended = toJson();
		albumNodeExtended.put("description", getDescription());
		return albumNodeExtended;
	}
	
}
