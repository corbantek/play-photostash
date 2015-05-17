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

	private AlbumActor(final File albumDirectory) {
		database = PhotostashDatabase.INSTANCE;
		this.albumDirectory = albumDirectory;
		storyActors = new TreeMap<String, ActorRef>();
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
		 * First detect if another organize is running
		 */

		/**
		 * Verify album has a database entry
		 */
		try {
			Album album = database.findAlbum(albumDirectory.getAbsolutePath());
			if (album == null) {
				/**
				 * Create new Album Record
				 */
				album = new Album(albumDirectory.getAbsolutePath(), albumDirectory.getName(), "");
				album = database.createAlbum(album);
			}
			for (File story : albumDirectory.listFiles()) {
				if (story.isDirectory()) {
					Shoebox.LOGGER.info("Found Story: " + story.getAbsolutePath());
					String actorName = Shoebox.generateActorName(story);
					ActorRef storyActor = storyActors.get(actorName);
					if (storyActor == null) {
						/**
						 * Create the Album actor if he doesn't exist anymore
						 */
						storyActor = getContext().actorOf(StoryActor.props(story, album), actorName);
						storyActors.put(actorName, storyActor);
					}
					storyActor.tell(organizeMessage, getSelf());
				}
			}
		} catch (PhotostashDatabaseException e) {
			Shoebox.LOGGER.error("Unable to find/create album: '" + albumDirectory.getAbsolutePath() +"': "+e.getMessage());
		}
	}

}
