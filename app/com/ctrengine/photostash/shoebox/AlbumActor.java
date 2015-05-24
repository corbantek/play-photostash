package com.ctrengine.photostash.shoebox;

import java.io.File;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.RoundRobinPool;

import com.ctrengine.photostash.database.DatabaseException;
import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.models.AlbumDocument;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeAlbumMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeStoryMessage;

public class AlbumActor extends UntypedActor {

	private final PhotostashDatabase database;
	private ActorRef storyRouter;

	private AlbumActor() {
		database = PhotostashDatabase.INSTANCE;
		storyRouter = getContext().actorOf(new RoundRobinPool(5).props(Props.create(StoryActor.class)), "story-router");
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof OrganizeAlbumMessage) {
				organize((OrganizeAlbumMessage) message);
		} else {
			unhandled(message);
		}
	}

	private AlbumDocument getAlbumDocument(File albumDirectory) throws ShoeboxException {
		/**
		 * Verify albumDocument has a database entry
		 */
		try {
			AlbumDocument albumDocument = database.findAlbum(albumDirectory.getAbsolutePath());
			if (albumDocument == null) {
				/**
				 * Create new AlbumDocument Record
				 */
				albumDocument = new AlbumDocument(albumDirectory);
				albumDocument = database.createDocument(albumDocument);
			}
			return albumDocument;
		} catch (DatabaseException e) {
			final String message = "Unable to find/create albumDocument: '" + albumDirectory.getAbsolutePath() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
			throw new ShoeboxException(message);
		}
	}

	private void organize(OrganizeAlbumMessage organizeMessage) {
		/**
		 * First detect if another organize is running
		 */
		File albumDirectory = organizeMessage.getAlbumDirectory();
		for (File storyDirectory : organizeMessage.getAlbumDirectory().listFiles()) {
			if (storyDirectory.isDirectory()) {
				Shoebox.LOGGER.info("Found Story: " + storyDirectory.getAbsolutePath());
				try {
					storyRouter.tell(new OrganizeStoryMessage(getAlbumDocument(albumDirectory), storyDirectory), getSelf());
				} catch (ShoeboxException e) {
					final String message = "Unable to organize album: '" + albumDirectory.getAbsolutePath() + "': " + e.getMessage();
					Shoebox.LOGGER.error(message);
				}
			}
		}
	}

}
