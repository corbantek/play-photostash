package com.ctrengine.photostash.shoebox;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Akka;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.routing.RoundRobinPool;

public enum Shoebox {
	INSTANCE;
	
	static final ALogger LOGGER = Logger.of("shoebox");
	
	private ActorRef shoeboxActor;
	private ActorRef photographRouter;
	
	private Shoebox(){
		shoeboxActor = Akka.system().actorOf(ShoeboxActor.props(), "shoebox");
		photographRouter = Akka.system().actorOf(new RoundRobinPool(20).props(Props.create(PhotographActor.class)), "photograph-router");
	}
	
	public ActorRef getShoeboxActor(){
		return shoeboxActor;
	}
	
	public ActorRef getPhotographRouter() {
		return photographRouter;
	}
}
