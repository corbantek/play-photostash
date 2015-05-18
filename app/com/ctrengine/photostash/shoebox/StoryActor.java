package com.ctrengine.photostash.shoebox;

import java.io.File;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.PhotostashDatabaseException;
import com.ctrengine.photostash.models.Album;
import com.ctrengine.photostash.models.Story;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeMessage;

public class StoryActor extends UntypedActor {
	static Props props(final File storyDirectory, final Album album) {
		return Props.create(new Creator<StoryActor>() {
			private static final long serialVersionUID = 7739531677211647759L;

			@Override
			public StoryActor create() throws Exception {
				return new StoryActor(storyDirectory, album);
			}
		});
	}
	
	private final PhotostashDatabase database;
	private final File storyDirectory;
	private final Album album;
	private Story story;

	private StoryActor(final File storyDirectory, final Album album) throws ShoeboxException {
		database = PhotostashDatabase.INSTANCE;
		this.storyDirectory = storyDirectory;
		this.album = album;
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
		 * Verify story has a database entry
		 */
		try {
			story = database.findStory(storyDirectory.getAbsolutePath());
			if (story == null) {
				/**
				 * Create new Album Record
				 */
				story = new Story(storyDirectory, 0, 0);
				story = database.createStory(story);
				Shoebox.LOGGER.debug("Album: "+album+" Story:"+story);
				database.relateAlbumToStory(album, story);
			}
		} catch (PhotostashDatabaseException e) {
			final String message = "Unable to find/create/link story '" + storyDirectory.getAbsolutePath() + "': "+e.getMessage();
			Shoebox.LOGGER.error(message);
			throw new ShoeboxException(message);
		}
	}

	private void organize(OrganizeMessage organizeMessage) {
		/**
		 * First detect if another organize is running
		 */

//			for (File story : albumDirectory.listFiles()) {
//				if (story.isDirectory()) {
//					Shoebox.LOGGER.info("Found Photograph: " + story.getAbsolutePath());
//					String actorName = Shoebox.generateActorName(story);
//					ActorRef storyActor = storyActors.get(actorName);
//					if (storyActor == null) {
//						/**
//						 * Create the Album actor if he doesn't exist anymore
//						 */
//						storyActor = getContext().actorOf(StoryActor.props(story), actorName);
//						storyActors.put(actorName, storyActor);
//					}
//					storyActor.tell(organizeMessage, getSelf());
//				}
//			}
	}

}
