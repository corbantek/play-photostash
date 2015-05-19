package com.ctrengine.photostash.shoebox;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.PhotostashDatabaseException;
import com.ctrengine.photostash.models.Album;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.InitializeMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseType;
import com.ctrengine.photostash.util.PhotostashUtil;

public class AlbumActor extends UntypedActor {
	static Props props(final File albumDirectory) {
		return Props.create(new Creator<AlbumActor>() {
			private static final long serialVersionUID = 65066616020832801L;

			@Override
			public AlbumActor create() throws Exception {
				return new AlbumActor(albumDirectory);
			}
		});
	}

	private final PhotostashDatabase database;
	private final File albumDirectory;
	private Album album;

	private AlbumActor(final File albumDirectory) {
		database = PhotostashDatabase.INSTANCE;
		this.albumDirectory = albumDirectory;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof InitializeMessage) {
			initialize();
		}else if (message instanceof OrganizeMessage) {
				organize((OrganizeMessage) message);
		} else {
			unhandled(message);
		}
	}

	private void initialize() {
		/**
		 * Verify album has a database entry
		 */
		try {
			album = database.findAlbum(albumDirectory.getAbsolutePath());
			if (album == null) {
				/**
				 * Create new Album Record
				 */
				album = new Album(albumDirectory);
				album = database.createDocument(album);
			}
		} catch (PhotostashDatabaseException e) {
			final String message = "Unable to find/create album: '" + albumDirectory.getAbsolutePath() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
			getContext().stop(getSelf());
		}
	}

	private void organize(OrganizeMessage organizeMessage) {
		/**
		 * First detect if another organize is running
		 */

		for (File story : albumDirectory.listFiles()) {
			if (story.isDirectory()) {
				Shoebox.LOGGER.info("Found Story: " + story.getAbsolutePath());
				String actorName = PhotostashUtil.generateKeyFromFile(story);
				ActorRef storyActor = getContext().getChild(actorName);
					if (storyActor == null) {
						/**
						 * Create the Album actor if he doesn't exist anymore
						 */
						storyActor = getContext().actorOf(StoryActor.props(story, album), actorName);
						storyActor.tell(new InitializeMessage(), getSelf());
					}
					storyActor.tell(organizeMessage, getSelf());
			}
		}
	}

}
