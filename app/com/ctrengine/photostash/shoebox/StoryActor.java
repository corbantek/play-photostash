package com.ctrengine.photostash.shoebox;

import java.io.File;

import akka.actor.UntypedActor;

import com.arangodb.ErrorNums;
import com.ctrengine.photostash.database.DatabaseException;
import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.models.AlbumDocument;
import com.ctrengine.photostash.models.DocumentException;
import com.ctrengine.photostash.models.PhotographDocument;
import com.ctrengine.photostash.models.StoryDocument;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeStoryMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResizeRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResponseMessage;

public class StoryActor extends UntypedActor {
	private final PhotostashDatabase database;

	private StoryActor() throws ShoeboxException {
		database = PhotostashDatabase.INSTANCE;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof OrganizeStoryMessage) {
			organize((OrganizeStoryMessage) message);
		}else if (message instanceof PhotographResponseMessage) {
			/*
			 * Do nothing
			 */
		} else {
			unhandled(message);
		}
	}

	private StoryDocument getStoryDocument(final AlbumDocument albumDocument, final File storyDirectory) throws ShoeboxException {
		/**
		 * Verify storyDocument has a database entry
		 */
		StoryDocument storyDocument = null;
			try {
				storyDocument = database.findStory(storyDirectory.getAbsolutePath());
			} catch (DatabaseException e) {
				final String message = "Unable to find storyDocument '" + storyDirectory.getAbsolutePath() + "': " + e.getMessage();
				Shoebox.LOGGER.warn(message);
			}
			if (storyDocument == null) {
				storyDocument = new StoryDocument(storyDirectory);
				try {
					createAndRelateStoryDocument(albumDocument, storyDocument);
				} catch (DatabaseException e) {
					if(e.getErrorNumber() == ErrorNums.ERROR_ARANGO_UNIQUE_CONSTRAINT_VIOLATED){
						Shoebox.LOGGER.warn("Duplicate Story Key Detected: Adding AlbumKey to StoryKey");
						storyDocument.setKey(albumDocument.getKey()+"-"+storyDocument.getKey());
						try {
							createAndRelateStoryDocument(albumDocument, storyDocument);
						} catch (DatabaseException e1) {
							final String message = "Unable to create/link storyDocument '" + storyDirectory.getAbsolutePath() + "': " + e.getMessage();
							Shoebox.LOGGER.error(message);
							throw new ShoeboxException(message);
						}
					}else{
						final String message = "Unable to create/link storyDocument '" + storyDirectory.getAbsolutePath() + "': " + e.getMessage();
						Shoebox.LOGGER.error(message);
						throw new ShoeboxException(message);
					}
				}
			}
			return storyDocument;
	}
	
	private StoryDocument createAndRelateStoryDocument(AlbumDocument albumDocument, StoryDocument storyDocument) throws DatabaseException {
		/**
		 * Create new StoryDocument Record
		 */
		storyDocument = database.createDocument(storyDocument);
		Shoebox.LOGGER.debug("AlbumDocument: " + albumDocument + " StoryDocument:" + storyDocument);
		database.relateDocumentToDocument(albumDocument, storyDocument);
		return storyDocument;
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
						/**
						 * Create a default resize image of 1024 to speed up initial content delivery
						 */
						//Shoebox.INSTANCE.getShoeboxActor().tell(new PhotographResizeRequestMessage(photographDocument, 1024), getSelf());
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
		 * Verify Photograph has a database entry
		 */
		PhotographDocument photographDocument = null;
			try {
				photographDocument = database.findPhotograph(photographFile.getAbsolutePath());
			} catch (DatabaseException e) {
				final String message = "Unable to find storyDocument '" + photographFile.getAbsolutePath() + "': " + e.getMessage();
				Shoebox.LOGGER.warn(message);
			}
			try {
				if (photographDocument == null) {
					photographDocument = new PhotographDocument(photographFile);
					try {
						createAndRelatePhotographDocument(storyDocument, photographDocument);
					} catch (DatabaseException e) {
						if(e.getErrorNumber() == ErrorNums.ERROR_ARANGO_UNIQUE_CONSTRAINT_VIOLATED){
							Shoebox.LOGGER.warn("Duplicate Story Key Detected: Adding AlbumKey to StoryKey");
							photographDocument.setKey(storyDocument.getKey()+"-"+photographDocument.getKey());
							try {
								createAndRelatePhotographDocument(storyDocument, photographDocument);
							} catch (DatabaseException e1) {
								final String message = "Unable to create/link photographDocument '" + photographFile.getAbsolutePath() + "': " + e.getMessage();
								Shoebox.LOGGER.error(message);
							}
						}else{
							final String message = "Unable to create/link photographDocument '" + photographFile.getAbsolutePath() + "': " + e.getMessage();
							Shoebox.LOGGER.error(message);
						}
					}
				}
			} catch (DocumentException e) {
				final String message = "Unable to create photographDocument '" + photographFile.getAbsolutePath() + "': " + e.getMessage();
				Shoebox.LOGGER.error(message);
			}
			return photographDocument;
	}
	
	private PhotographDocument createAndRelatePhotographDocument(StoryDocument storyDocument, PhotographDocument photographDocument) throws DatabaseException {
		/**
		 * Create new Photograph Record
		 */
		photographDocument = database.createDocument(photographDocument);
		Shoebox.LOGGER.debug("StoryDocument: " + storyDocument + " PhotographDocument:" + photographDocument);
		database.relateDocumentToDocument(storyDocument, photographDocument);
		return photographDocument;
	}

}
