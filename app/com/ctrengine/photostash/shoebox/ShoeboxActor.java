package com.ctrengine.photostash.shoebox;

import java.io.File;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.routing.RoundRobinPool;

import com.ctrengine.photostash.conf.ShoeboxConfiguration;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeAlbumMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeShoeboxMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResizeRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseType;

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

	private ShoeboxActor() {
		photographRouter = getContext().actorOf(new RoundRobinPool(10).props(Props.create(PhotographActor.class)), "photograph-router");
		albumRouter = getContext().actorOf(new RoundRobinPool(2).props(Props.create(AlbumActor.class)), "album-router");
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof PhotographRequestMessage || message instanceof PhotographResizeRequestMessage) {
			photographRouter.tell(message, getSender());
		}else if (message instanceof OrganizeShoeboxMessage) {
			organize((OrganizeShoeboxMessage) message);
		} else {
			unhandled(message);
		}
	}

	private void organize(OrganizeShoeboxMessage organizeMessage) {
		/**
		 * TODO First detect if another organize is running
		 */

		File shoeboxDirectory = new File(organizeMessage.getShoeboxPath());
		if (shoeboxDirectory.exists() && shoeboxDirectory.isDirectory()) {
			/**
			 * Send messages to the Album Router to perform organizations for each album found
			 */
			for (File albumDirectory : shoeboxDirectory.listFiles()) {
				if (albumDirectory.isDirectory()) {
					Shoebox.LOGGER.info("Found Album: " + albumDirectory.getAbsolutePath());
					albumRouter.tell(new OrganizeAlbumMessage(albumDirectory), getSelf());
				}
			}
			/**
			 * TODO Need to look for any deleted directories and remove them
			 * from the database...
			 */

			getSender().tell(new ResponseMessage(ResponseType.INFO, "Shoebox organize started."), getSelf());
		} else {
			String message = "Shoebox Directory does not exist: " + ShoeboxConfiguration.INSTANCE.getShoeboxPath();
			Shoebox.LOGGER.error(message);
			getSender().tell(new ResponseMessage(ResponseType.ERROR, message), getSelf());
		}
	}

}
