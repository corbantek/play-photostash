package com.ctrengine.photostash.shoebox;

import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResizeRequestMessage;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Akka;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.routing.ConsistentHashingPool;
import akka.routing.ConsistentHashingRouter.ConsistentHashMapper;

public enum Shoebox {
	INSTANCE;

	static final ALogger LOGGER = Logger.of("shoebox");

	private ActorRef shoeboxActor;
	private ActorRef photographRouter;

	private Shoebox() {
		shoeboxActor = Akka.system().actorOf(ShoeboxActor.props(), "shoebox");
		
		final ConsistentHashMapper consistentHashMapper = new ConsistentHashMapper() {
			@Override
			public Object hashKey(Object message) {
				if (message instanceof PhotographRequestMessage) {
					return ((PhotographRequestMessage) message).getPhotographDocument().getKey();
				} else if (message instanceof PhotographResizeRequestMessage) {
					return ((PhotographResizeRequestMessage) message).getPhotographDocument().getKey();
				} else
					return null;
			}
		};
		photographRouter = Akka.system().actorOf(new ConsistentHashingPool(25).withHashMapper(consistentHashMapper).props(Props.create(PhotographActor.class)), "photograph-router");
	}

	public ActorRef getShoeboxActor() {
		return shoeboxActor;
	}

	public ActorRef getPhotographRouter() {
		return photographRouter;
	}
}
