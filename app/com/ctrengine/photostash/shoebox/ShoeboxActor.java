package com.ctrengine.photostash.shoebox;

import java.io.File;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;

import com.ctrengine.photostash.conf.ShoeboxConfiguration;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.InitializeMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseType;
import com.ctrengine.photostash.util.PhotostashUtil;

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

	private ShoeboxActor() {
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof OrganizeMessage) {
			organize((OrganizeMessage) message);
		} else {
			unhandled(message);
		}
	}

	private void organize(OrganizeMessage organizeMessage) {
		/**
		 * TODO First detect if another organize is running
		 */

		File shoeboxDirectory = new File(ShoeboxConfiguration.INSTANCE.getShoeboxPath());
		if (shoeboxDirectory.exists() && shoeboxDirectory.isDirectory()) {
			/**
			 * Look for all existing and new directories and create actors for
			 * them to monitor
			 */
			for (File album : shoeboxDirectory.listFiles()) {
				if (album.isDirectory()) {
					Shoebox.LOGGER.info("Found Album: " + album.getAbsolutePath());
					String actorName = PhotostashUtil.generateKeyFromFile(album);
					ActorRef albumActor = getContext().getChild(actorName);
					if (albumActor == null) {
						/**
						 * Create the Album actor if he doesn't exist anymore
						 */
						albumActor = getContext().actorOf(AlbumActor.props(album), actorName);
						albumActor.tell(new InitializeMessage(), getSelf());
					}
					albumActor.tell(organizeMessage, getSelf());
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
