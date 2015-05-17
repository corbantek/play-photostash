package com.ctrengine.photostash.conf;

import play.Configuration;

public enum DatabaseConfiguration {
	INSTANCE;
	private final String database;
	private final String host;
	private final int port;
	private final String user;
	private final String password;
	
	private DatabaseConfiguration(){
		String database = null;
		String host = null;
		int port = 8529;
		String portString = null;
		String user = null;
		String password = null;
		Configuration configuration = play.Play.application().configuration().getConfig("photostash.db");
		if(configuration != null){
			database = configuration.getString("database");
			host = configuration.getString("host");
			portString = configuration.getString("port");
			user = configuration.getString("user");
			password = configuration.getString("password");
		}
		if(database == null){
			database = "photostash";
		}
		if(host == null){
			host = "127.0.0.1";
		}
		if(portString != null && !portString.isEmpty()){
			port = Integer.parseInt(portString);
		}
		if(user == null){
			user = "";
		}
		if(password == null){
			password = "";
		}
		this.database = database;
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
	}

	public String getDatabase() {
		return database;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}
}
