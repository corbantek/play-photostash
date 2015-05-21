package com.ctrengine.photostash.models;

import java.io.File;

import com.ctrengine.photostash.util.PhotostashUtil;

public abstract class AbstractFileDocument extends AbstractDocument implements FileDocument {
	private String path;
	private String name;
	
	protected AbstractFileDocument(File file){
		super();
		this.path = file.getAbsolutePath();
		this.name = file.getName().replaceAll("\\.\\w+$", "");;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int compareTo(Document o) {
		if(o instanceof AbstractFileDocument){
			return name.compareTo(((AbstractFileDocument)o).name);	
		}else{
			return super.compareTo(o);
		}
	}
}
