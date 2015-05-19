package com.ctrengine.photostash.shoebox;

import java.io.File;
import java.util.Date;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.PhotostashDatabaseException;
import com.ctrengine.photostash.models.Album;
import com.ctrengine.photostash.models.Photograph;
import com.ctrengine.photostash.models.Story;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.InitializeMessage;
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
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof InitializeMessage) {
			initialize();
		} else if (message instanceof OrganizeMessage) {
			organize((OrganizeMessage) message);
		} else {
			unhandled(message);
		}
	}

	private void initialize() {
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
				story = database.createDocument(story);
				Shoebox.LOGGER.debug("Album: " + album + " Story:" + story);
				database.relateDocumentToDocument(album, story);
			}
		} catch (PhotostashDatabaseException e) {
			final String message = "Unable to find/create/link story '" + storyDirectory.getAbsolutePath() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
			getContext().stop(getSelf());
		}
	}

	private void organize(OrganizeMessage organizeMessage) {
		/**
		 * First detect if another organize is running
		 */

		for (File photographFile : storyDirectory.listFiles()) {
			if (photographFile.isFile()) {
				Shoebox.LOGGER.info("Found Photograph: " + photographFile.getAbsolutePath());
				verifyPhotograph(photographFile);
			}
		}
	}
	
	private void verifyPhotograph(File photographFile){
		/**
		 * Verify story has a database entry
		 */
		try {
			Photograph photograph = database.findPhotograph(photographFile.getAbsolutePath());
			if (photograph == null) {
				/**
				 * Create new Album Record
				 */
				photograph = new Photograph(photographFile, photographFile.length(), new Date().getTime());
				photograph = database.createDocument(photograph);
				Shoebox.LOGGER.debug("Story: " + story + " Photograph:" + photograph);
				database.relateDocumentToDocument(story, photograph);
			}
		} catch (PhotostashDatabaseException e) {
			final String message = "Unable to find/create/link story '" + photographFile.getAbsolutePath() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
			getContext().stop(getSelf());
		}
	}

}
