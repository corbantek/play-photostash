package com.ctrengine.photostash.shoebox;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import play.libs.Json;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.routing.Broadcast;
import akka.routing.RoundRobinPool;

import com.ctrengine.photostash.conf.ShoeboxConfiguration;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeAlbumMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeShoeboxMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeStopMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResizeRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseType;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ShoeboxActor extends UntypedActor {
	static Props props() {
		return Props.create(new Creator<ShoeboxActor>() {
			private static final long serialVersionUID = 6309187420249216567L;

			@Override
			public ShoeboxActor create() throws Exception {
				return new ShoeboxActor();
			}
		});
	}

	private ActorRef photographRouter;
	private ActorRef albumRouter;

	private Set<File> albumsOrganizing;

	private ShoeboxActor() {
		photographRouter = getContext().actorOf(new RoundRobinPool(20).props(Props.create(PhotographActor.class)), "photograph-router");
		albumRouter = getContext().actorOf(new RoundRobinPool(2).props(Props.create(AlbumActor.class)), "album-router");
		albumsOrganizing = new HashSet<File>();
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof PhotographRequestMessage || message instanceof PhotographResizeRequestMessage) {
			photographRouter.tell(message, getSender());
		} else if (message instanceof OrganizeShoeboxMessage) {
			organize((OrganizeShoeboxMessage) message);
		} else if (message instanceof OrganizeStopMessage) {
			albumRouter.tell(new Broadcast(message), getSender());
		} else {
			unhandled(message);
		}
	}

	private void organize(OrganizeShoeboxMessage organizeMessage) {
		/**
		 * TODO First detect if another organize is running
		 */
		if (albumsOrganizing.size() > 0) {
			getSender().tell(new ResponseMessage(ResponseType.WARNING, Json.newObject().put("message", "Shoebox organize already running.")), getSelf());
		} else {
			albumsOrganizing.clear();
			File shoeboxDirectory = new File(organizeMessage.getShoeboxPath());
			if (shoeboxDirectory.exists() && shoeboxDirectory.isDirectory()) {
				/**
				 * Send messages to the Album Router to perform organizations
				 * for each album found
				 */
				for (File albumDirectory : shoeboxDirectory.listFiles()) {			
					if (albumDirectory.isDirectory()) {
						Shoebox.LOGGER.info("Found Album: " + albumDirectory.getAbsolutePath());
						albumRouter.tell(new OrganizeAlbumMessage(albumDirectory), getSelf());
						albumsOrganizing.add(albumDirectory);
					}
				}
				/**
				 * TODO Need to look for any deleted directories and remove them
				 * from the database...
				 */

				ObjectNode message = Json.newObject();
				message.put("message", "Shoebox organize started.");
				message.put("organizing", Json.newObject().put("albums", albumsOrganizing.size()));
				getSender().tell(new ResponseMessage(ResponseType.INFO, message), getSelf());
			} else {
				String message = "Shoebox Directory does not exist: " + ShoeboxConfiguration.INSTANCE.getShoeboxPath();
				Shoebox.LOGGER.error(message);
				getSender().tell(new ResponseMessage(ResponseType.ERROR, Json.newObject().put("message", message)), getSelf());
			}
		}
	}

}
