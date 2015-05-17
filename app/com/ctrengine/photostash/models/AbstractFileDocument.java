package com.ctrengine.photostash.models;

import java.io.File;

import com.ctrengine.photostash.util.PhotostashUtil;

public abstract class AbstractFileDocument extends AbstractDocument {
	public static final String PATH = "path";
	private String path;
	private String name;
	
	protected AbstractFileDocument(File file){
		super(PhotostashUtil.generateKeyFromFile(file));
		this.path = file.getAbsolutePath();
		this.name = file.getName();
	}

	public String getPath() {
		return path;
	}

	public String getName() {
		return name;
	}
	
	
}
