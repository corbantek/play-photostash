package com.ctrengine.photostash.models;

public class Story extends AbstractDocument {
	public static final String COLLECTION = "story";
	public static final String PATH = "path";

	private String path;
	private String name;
	private String description;
	private int size;
	private int stashSize;

	public Story(String path, String name, String description, int size, int stashSize) {
		super();
		this.path = path;
		this.name = name;
		this.description = description;
		this.size = size;
		this.stashSize = stashSize;
	}

	@Override
	protected String getCollection() {
		return COLLECTION;
	}

	public String getPath() {
		return path;
	}

	public String getName() {
		return name;
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
}
