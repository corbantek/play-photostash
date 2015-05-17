package com.ctrengine.photostash.shoebox;

import java.io.File;
import java.util.UUID;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Akka;
import akka.actor.ActorRef;

public enum Shoebox {
	INSTANCE;
	
	static final ALogger LOGGER = Logger.of("shoebox");
	
	private ActorRef shoeboxActor;
	
	private Shoebox(){
		shoeboxActor = Akka.system().actorOf(ShoeboxActor.props(), "Shoebox");
	}
	
	public ActorRef getShoeboxActor(){
		return shoeboxActor;
	}
	
	static String generateActorName(File file){
		if(file == null){
			return UUID.randomUUID().toString();
		}else{
			return file.getName().replaceAll("\\s", "");
		}
	}
}
