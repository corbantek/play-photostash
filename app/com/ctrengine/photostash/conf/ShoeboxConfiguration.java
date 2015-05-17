package com.ctrengine.photostash.conf;

import play.Configuration;

public enum ShoeboxConfiguration {
	INSTANCE;
	private final String shoeboxPath;
	
	private ShoeboxConfiguration(){
		String shoeboxPath = null;
		Configuration configuration = play.Play.application().configuration().getConfig("photostash.shoebox");
		if(configuration != null){
			shoeboxPath = configuration.getString("path");
		}
		if(shoeboxPath == null){
			shoeboxPath = "";
		}
		this.shoeboxPath = shoeboxPath;
	}
	
	public String getShoeboxPath(){
		return shoeboxPath;
	}
}
