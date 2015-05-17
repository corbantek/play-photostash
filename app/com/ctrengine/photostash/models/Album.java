package com.ctrengine.photostash.models;

public class Album extends AbstractDocument {
	public static final String COLLECTION = "album";
	public static final String PATH = "path";

	private String path;
	private String name;
	private String description;

	public Album(String path, String name, String description) {
		super();
		this.path = path;
		this.name = name;
		this.description = description;
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
}
