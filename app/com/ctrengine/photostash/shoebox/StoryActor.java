package com.ctrengine.photostash.shoebox;

import java.io.File;

import akka.actor.UntypedActor;

import com.ctrengine.photostash.database.DatabaseException;
import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.models.AlbumDocument;
import com.ctrengine.photostash.models.DocumentException;
import com.ctrengine.photostash.models.PhotographDocument;
import com.ctrengine.photostash.models.StoryDocument;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeStoryMessage;

public class StoryActor extends UntypedActor {
	private final PhotostashDatabase database;

	private StoryActor() throws ShoeboxException {
		database = PhotostashDatabase.INSTANCE;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof OrganizeStoryMessage) {
			organize((OrganizeStoryMessage) message);
		} else {
			unhandled(message);
		}
	}

	private StoryDocument getStoryDocument(final AlbumDocument albumDocument, final File storyDirectory) throws ShoeboxException {
		/**
		 * Verify storyDocument has a database entry
		 */
		try {
			StoryDocument storyDocument = database.findStory(storyDirectory.getAbsolutePath());
			if (storyDocument == null) {
				/**
				 * Create new AlbumDocument Record
				 */
				storyDocument = new StoryDocument(storyDirectory);
				storyDocument = database.createDocument(storyDocument);
				Shoebox.LOGGER.debug("AlbumDocument: " + albumDocument + " StoryDocument:" + storyDocument);
				database.relateDocumentToDocument(albumDocument, storyDocument);
			}
			return storyDocument;
		} catch (DatabaseException e) {
			final String message = "Unable to find/create/link storyDocument '" + storyDirectory.getAbsolutePath() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
			throw new ShoeboxException(message);
		}
	}

	private void organize(OrganizeStoryMessage organizeMessage) {
		/**
		 * First detect if another organize is running
		 */
		File storyDirectory = organizeMessage.getStoryDirectory();
		try {
			StoryDocument storyDocument = getStoryDocument(organizeMessage.getAlbumDocument(), storyDirectory);
			String coverPhotographKey = null;
			long size = 0;
			for (File photographFile : storyDirectory.listFiles()) {
				if (photographFile.isFile()) {
					Shoebox.LOGGER.info("Found PhotographDocument: " + photographFile.getAbsolutePath());
					PhotographDocument photographDocument = verifyPhotograph(storyDocument, photographFile);
					if (photographDocument != null) {
						size += photographDocument.getSize();
						/**
						 * Set first photograph to cover
						 */
						if (coverPhotographKey == null) {
							coverPhotographKey = photographDocument.getKey();
						}
					}
				}
			}
			/**
			 * Set Story Disk Size
			 */
			storyDocument.setSize(size);
			/**
			 * Setup a cover photo if missing
			 */
			if (storyDocument.getCoverPhotographKey() == null) {
				storyDocument.setCoverPhotographKey(coverPhotographKey);
			}	
			try {
				database.updateDocument(storyDocument);
			} catch (DatabaseException e) {
				final String message = "Unable to update storyDocument '" + storyDocument.getName() + "': " + e.getMessage();
				Shoebox.LOGGER.error(message);
			}
		} catch (ShoeboxException e) {
			final String message = "Unable to organize story: '" + storyDirectory.getAbsolutePath() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
		}
	}

	private PhotographDocument verifyPhotograph(final StoryDocument storyDocument, final File photographFile) {
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
			return photographDocument;
		} catch (DatabaseException | DocumentException e) {
			final String message = "Unable to find/create/link photographDocument '" + photographFile.getAbsolutePath() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
			return null;
		}
	}

}
