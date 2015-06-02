package com.ctrengine.photostash.shoebox;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.arangodb.ErrorNums;
import com.ctrengine.photostash.database.DatabaseException;
import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.models.AlbumDocument;
import com.ctrengine.photostash.models.DocumentException;
import com.ctrengine.photostash.models.PhotographDocument;
import com.ctrengine.photostash.models.StoryDocument;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeCompleteMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResponseMessage;

public class StoryActor extends UntypedActor {
	private static class OrganizeStoryRequester {
		private final ActorRef requester;
		private final AlbumDocument albumDocument;
		private final File storyFile;

		public OrganizeStoryRequester(ActorRef requester, AlbumDocument albumDocument, File storyFile) {
			super();
			this.requester = requester;
			this.albumDocument = albumDocument;
			this.storyFile = storyFile;
		}

		public ActorRef getRequester() {
			return requester;
		}

		public AlbumDocument getAlbumDocument() {
			return albumDocument;
		}

		public File getStoryFile() {
			return storyFile;
		}
	}

	/**
	 * Basic Needs
	 */
	private final PhotostashDatabase database;

	/**
	 * Organizing Tracking
	 */
	private Map<StoryDocument, Set<File>> photographsOrganizing;
	private Map<StoryDocument, OrganizeStoryRequester> organizingRequestMap;

	private StoryActor() throws ShoeboxException {
		database = PhotostashDatabase.INSTANCE;
		photographsOrganizing = new HashMap<StoryDocument, Set<File>>();
		organizingRequestMap = new HashMap<StoryDocument, OrganizeStoryRequester>();
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof OrganizeCompleteMessage) {
			organizeComplete((OrganizeCompleteMessage) message);
		} else if (message instanceof OrganizeMessage) {
			organize((OrganizeMessage) message);
		} else if (message instanceof PhotographResponseMessage) {
			/*
			 * Do nothing
			 */
		} else {
			unhandled(message);
		}
	}

	private void organizeComplete(OrganizeCompleteMessage organizeCompleteMessage) {
		if (organizeCompleteMessage.getAbstractFileDocument() instanceof StoryDocument) {
			StoryDocument storyDocument = (StoryDocument) organizeCompleteMessage.getAbstractFileDocument();
			Set<File> photographs = photographsOrganizing.get(storyDocument);
			if (photographs == null) {
				Shoebox.LOGGER.warn("Story not registered to organize:" + storyDocument.getKey());
			} else {
				if (!photographs.remove(organizeCompleteMessage.getFile())) {
					Shoebox.LOGGER.warn("Completed organize photograph not found: " + organizeCompleteMessage.getFile().getName());
				}
				if (photographs.isEmpty()) {
					Shoebox.LOGGER.info("Organize Story Complete for " + organizeCompleteMessage.getAbstractFileDocument().getKey());
					/**
					 * No more photographs in this story are organizing, tell
					 * the original sender and clean up memory
					 */
					photographsOrganizing.remove(storyDocument);
					OrganizeStoryRequester organizeStoryRequester = organizingRequestMap.remove(storyDocument);
					if (organizeStoryRequester != null) {
						organizeStoryRequester.getRequester().tell(new ShoeboxMessages.OrganizeCompleteMessage(organizeStoryRequester.getAlbumDocument(), organizeStoryRequester.getStoryFile()), getSelf());
					} else {
						Shoebox.LOGGER.warn("Organize complete message was not an instance of StoryDocument: " + storyDocument.getName());
					}

					if (storyDocument.getCoverPhotographKey() == null) {
						try {
							List<PhotographDocument> photographDocuments = database.getRelatedDocuments(storyDocument, PhotographDocument.class);
							if (!photographDocuments.isEmpty()) {
								updateStoryCoverPhoto(storyDocument, photographDocuments.get(0));
							}
						} catch (DatabaseException e) {
							Shoebox.LOGGER.error("Unable to update storyDocument's Cover Photo '" + storyDocument.getName() + "': " + e.getMessage());
						}
					}
				}
			}
		} else {
			Shoebox.LOGGER.warn("Organize Complete Document is not a StoryDocument: " + organizeCompleteMessage.getAbstractFileDocument().getName());
		}
	}

	private void updateStoryCoverPhoto(StoryDocument storyDocument, PhotographDocument photographDocument) {
		if (storyDocument != null) {
			try {
				storyDocument.setCoverPhotographKey(photographDocument.getKey());
				database.updateDocument(storyDocument);
			} catch (DatabaseException e) {
				Shoebox.LOGGER.error("Unable to update storyDocument '" + storyDocument.getName() + "': " + e.getMessage());
			}
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
				if (e.getErrorNumber() == ErrorNums.ERROR_ARANGO_UNIQUE_CONSTRAINT_VIOLATED) {
					Shoebox.LOGGER.warn("Duplicate Story Key Detected: Adding AlbumKey to StoryKey");
					storyDocument.setKey(albumDocument.getKey() + "-" + storyDocument.getKey());
					try {
						createAndRelateStoryDocument(albumDocument, storyDocument);
					} catch (DatabaseException e1) {
						final String message = "Unable to create/link storyDocument '" + storyDirectory.getAbsolutePath() + "': " + e.getMessage();
						Shoebox.LOGGER.error(message);
						throw new ShoeboxException(message);
					}
				} else {
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

	private void organize(OrganizeMessage organizeMessage) {
		if (organizeMessage.getAbstractFileDocument() instanceof AlbumDocument) {
			AlbumDocument albumDocument = (AlbumDocument) organizeMessage.getAbstractFileDocument();
			File storyDirectory = organizeMessage.getFile();
			Set<File> photographFiles = new HashSet<File>();
			try {
				StoryDocument storyDocument = getStoryDocument(albumDocument, storyDirectory);

				/**
				 * Store this organize requester so we can respond later when
				 * organizing is finished
				 */
				organizingRequestMap.put(storyDocument, new OrganizeStoryRequester(getSender(), albumDocument, storyDirectory));
				photographsOrganizing.put(storyDocument, photographFiles);

				for (File photographFile : storyDirectory.listFiles()) {
					if (photographFile.isFile()) {
						Shoebox.LOGGER.debug("Found PhotographDocument: " + photographFile.getAbsolutePath());
						Shoebox.INSTANCE.getShoeboxActor().tell(new OrganizeMessage(storyDocument, photographFile), getSelf());
						photographFiles.add(photographFile);
						/*
						 * TODO Need to move to Photograph Actor
						 */
						// /PhotographDocument photographDocument =
						// verifyPhotograph(storyDocument, photographFile);
					}
				}
			} catch (ShoeboxException e) {
				final String message = "Unable to organize story: '" + storyDirectory.getAbsolutePath() + "': " + e.getMessage();
				Shoebox.LOGGER.error(message);
			}
			if (photographFiles.isEmpty()) {
				/**
				 * Something wrong happened and there are no photograph files to
				 * organize, so tell the sender we are done
				 */
				Shoebox.LOGGER.warn("Unable to organize story: "+storyDirectory.getName()+".  No photographs.");
				getSender().tell(new ShoeboxMessages.OrganizeCompleteMessage(albumDocument, storyDirectory), getSender());
			}
		} else {
			Shoebox.LOGGER.warn("Organize Parent Document is not a AlbumDocument: " + organizeMessage.getAbstractFileDocument().getName());
		}
	}
}
