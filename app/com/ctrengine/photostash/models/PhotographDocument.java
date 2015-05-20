package com.ctrengine.photostash.models;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class PhotographDocument extends AbstractFileDocument implements RelateDocument {
	public static final String COLLECTION = "photographs";
	public static final String RELATE_COLLECTION = "photographrelations";

	private String description;
	private String mimeType;
	private long size;
	private long dateTaken;

	public PhotographDocument(File photographFile) throws DocumentException {
		super(photographFile);
		Path photographPath = Paths.get(photographFile.getAbsolutePath());
		try {
			mimeType = Files.probeContentType(photographPath);
			if(!mimeType.startsWith("image")){
				throw new DocumentException(photographFile.getName() + "is not an image.");
			}			
			size = Files.size(photographPath);
		} catch (IOException e) {
			throw new DocumentException(e);
		}
		this.description = "";
		this.dateTaken = new Date().getTime();
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getMimeType() {
		return mimeType;
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
		photographNodeExtended.put("mimeType", getMimeType());
		photographNodeExtended.put("size", getSize());
		photographNodeExtended.put("dateTaken", getDateTaken());
		return photographNodeExtended;
	}
}
