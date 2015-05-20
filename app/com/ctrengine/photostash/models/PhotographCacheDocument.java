package com.ctrengine.photostash.models;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class PhotographCacheDocument extends AbstractDocument {
	public static final String SQUARE_SIZE = "squareSize";
	public static final String SIZE = "size";
	public static final String COLLECTION = "photographcache";
	
	private byte[] photograph;
	private int squareSize;
	private long size;
	
	public PhotographCacheDocument(PhotographDocument photographDocument, byte[] photograph, int squareSize) {
		super(photographDocument.getKey()+"-"+squareSize+"px");
		this.photograph = photograph;
		this.squareSize = squareSize;
		this.size = photograph.length;
	}

	public byte[] getPhotograph() {
		return photograph;
	}

	public int getSquareSize() {
		return squareSize;
	}

	public long getSize() {
		return size;
	}

	@Override
	public ObjectNode toJson() {
		ObjectNode photographCacheNode = Json.newObject();
		photographCacheNode.put("photographCacheId", getKey());
		photographCacheNode.put("squareSize",getSquareSize());
		return photographCacheNode;
	}

	@Override
	public ObjectNode toJsonExtended() {
		ObjectNode photographCacheNodeExtended = toJson();
		photographCacheNodeExtended.put("size", getSize());
		return photographCacheNodeExtended;
	}

	@Override
	public String getCollection() {
		return COLLECTION;
	}

}
