package com.ctrengine.photostash.shoebox;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Akka;
import akka.actor.ActorRef;

public enum Shoebox {
	INSTANCE;
	
	static final ALogger LOGGER = Logger.of("shoebox");
	
	private ActorRef shoeboxActor;
	
	private Shoebox(){
		shoeboxActor = Akka.system().actorOf(ShoeboxActor.props(), "shoebox");
	}
	
	public ActorRef getShoeboxActor(){
		return shoeboxActor;
	}
}
