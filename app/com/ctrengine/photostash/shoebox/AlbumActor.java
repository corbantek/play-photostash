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

	private final Map<String, ActorRef> storyActors;

	private final PhotostashDatabase database;
	private final File albumDirectory;
	private Album album;

	private AlbumActor(final File albumDirectory) throws ShoeboxException {
		storyActors = new TreeMap<String, ActorRef>();
		database = PhotostashDatabase.INSTANCE;
		this.albumDirectory = albumDirectory;
		verifyDatabaseEntry();
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof OrganizeMessage) {
			organize((OrganizeMessage) message);
		} else {
			unhandled(message);
		}
	}

	private void verifyDatabaseEntry() throws ShoeboxException {
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
			throw new ShoeboxException(message);
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
				ActorRef storyActor = storyActors.get(actorName);
				try {
					if (storyActor == null) {
						/**
						 * Create the Album actor if he doesn't exist anymore
						 */
						storyActor = getContext().actorOf(StoryActor.props(story, album), actorName);
						storyActors.put(actorName, storyActor);
					}
					storyActor.tell(organizeMessage, getSelf());
				} catch (Exception e) {
					String message = "Unable to create actor: " + actorName + " - " + e.getMessage();
					Shoebox.LOGGER.error(message);
				}
			}
		}
	}

}
