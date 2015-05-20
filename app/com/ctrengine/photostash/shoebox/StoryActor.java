package com.ctrengine.photostash.shoebox;

import java.io.File;
import java.nio.file.Files;
import java.util.Date;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.PhotostashDatabaseException;
import com.ctrengine.photostash.models.AlbumDocument;
import com.ctrengine.photostash.models.DocumentException;
import com.ctrengine.photostash.models.PhotographDocument;
import com.ctrengine.photostash.models.StoryDocument;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.InitializeMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeMessage;

public class StoryActor extends UntypedActor {
	static Props props(final File storyDirectory, final AlbumDocument albumDocument) {
		return Props.create(new Creator<StoryActor>() {
			private static final long serialVersionUID = 7739531677211647759L;

			@Override
			public StoryActor create() throws Exception {
				return new StoryActor(storyDirectory, albumDocument);
			}
		});
	}

	private final PhotostashDatabase database;
	private final File storyDirectory;
	private final AlbumDocument albumDocument;
	private StoryDocument storyDocument;

	private StoryActor(final File storyDirectory, final AlbumDocument albumDocument) throws ShoeboxException {
		database = PhotostashDatabase.INSTANCE;
		this.storyDirectory = storyDirectory;
		this.albumDocument = albumDocument;
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
		 * Verify storyDocument has a database entry
		 */
		try {
			storyDocument = database.findStory(storyDirectory.getAbsolutePath());
			if (storyDocument == null) {
				/**
				 * Create new AlbumDocument Record
				 */
				storyDocument = new StoryDocument(storyDirectory, 0, 0);
				storyDocument = database.createDocument(storyDocument);
				Shoebox.LOGGER.debug("AlbumDocument: " + albumDocument + " StoryDocument:" + storyDocument);
				database.relateDocumentToDocument(albumDocument, storyDocument);
			}
		} catch (PhotostashDatabaseException e) {
			final String message = "Unable to find/create/link storyDocument '" + storyDirectory.getAbsolutePath() + "': " + e.getMessage();
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
				Shoebox.LOGGER.info("Found PhotographDocument: " + photographFile.getAbsolutePath());
				verifyPhotograph(photographFile);
			}
		}
	}

	private void verifyPhotograph(File photographFile) {
		/**
		 * Verify storyDocument has a database entry
		 */
		try {
			PhotographDocument photographDocument = database.findPhotograph(photographFile.getAbsolutePath());
			if (photographDocument == null) {
				/**
				 * Create new PhotographDocument Record
				 */
				photographDocument = new PhotographDocument(photographFile);
				photographDocument = database.createDocument(photographDocument);
				Shoebox.LOGGER.debug("StoryDocument: " + storyDocument + " PhotographDocument:" + photographDocument);
				database.relateDocumentToDocument(storyDocument, photographDocument);
			}
		} catch (PhotostashDatabaseException | DocumentException e) {
			final String message = "Unable to find/create/link storyDocument '" + photographFile.getAbsolutePath() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
		}
	}

}
