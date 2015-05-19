package com.ctrengine.photostash.models;

public interface FileDocument extends Document {

	public static final String PATH = "path";

	public abstract String getPath();

	public abstract String getName();

}