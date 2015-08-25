package com.ctrengine.photostash.shoebox;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.arangodb.ErrorNums;
import com.ctrengine.photostash.database.DatabaseException;
import com.ctrengine.photostash.database.DatabaseFilter;
import com.ctrengine.photostash.database.DatabaseFilter.DatabaseFilterType;
import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.models.DocumentException;
import com.ctrengine.photostash.models.PhotographCacheDocument;
import com.ctrengine.photostash.models.PhotographDocument;
import com.ctrengine.photostash.models.StoryDocument;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeCompleteMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResizeRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResponseMessage;

public class PhotographActor extends UntypedActor {

	private final PhotostashDatabase database;

	public PhotographActor() {
		database = PhotostashDatabase.INSTANCE;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof OrganizeCompleteMessage) {
			OrganizeCompleteMessage organizeCompleteMessage = (OrganizeCompleteMessage)message;
			Shoebox.LOGGER.warn("PhotographActor received OrganizeCompleteMessage "+organizeCompleteMessage.getAbstractFileDocument().getKey()+":"+organizeCompleteMessage.getFile().getName()+" from " + getSender());
		} else if (message instanceof OrganizeMessage) {
			organize((OrganizeMessage) message);
		} else if (message instanceof PhotographRequestMessage) {
			readPhotographFromDisk((PhotographRequestMessage) message);
		} else if (message instanceof PhotographResizeRequestMessage) {
			readPhotographFromDatabase((PhotographResizeRequestMessage) message);
		} else {
			unhandled(message);
		}
	}
	
	private void conditionalReplyToSender(Object message){
		ActorRef sender = getSender(); 
		if(sender != ActorRef.noSender()){
			sender.tell(message, getSelf());
		}
	}

	private void organize(OrganizeMessage organizeMessage) {
		if (organizeMessage.getAbstractFileDocument() instanceof StoryDocument) {
			StoryDocument storyDocument = (StoryDocument) organizeMessage.getAbstractFileDocument();
			File photographFile = organizeMessage.getFile();

			PhotographDocument photographDocument = getPhotographDocument(storyDocument, photographFile);
			if (photographDocument == null) {
				Shoebox.LOGGER.warn("Organize Photograph Failed for: " + organizeMessage.getFile());
			}
			getSender().tell(new OrganizeCompleteMessage(storyDocument, photographFile), getSelf());
		} else {
			Shoebox.LOGGER.warn("Organize Parent Document is not a StoryDocument: " + organizeMessage.getAbstractFileDocument().getName() + " " + getSender());
		}
	}

	private PhotographDocument getPhotographDocument(final StoryDocument storyDocument, final File photographFile) {
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
					if (e.getErrorNumber() == ErrorNums.ERROR_ARANGO_UNIQUE_CONSTRAINT_VIOLATED) {
						Shoebox.LOGGER.warn("Duplicate Story Key Detected: Adding StoryKey to PhotographyKey");
						photographDocument.setKey(storyDocument.getKey() + "-" + photographDocument.getKey());
						try {
							createAndRelatePhotographDocument(storyDocument, photographDocument);
						} catch (DatabaseException e1) {
							final String message = "Unable to create/link photographDocument '" + photographFile.getAbsolutePath() + "': " + e.getMessage();
							Shoebox.LOGGER.error(message);
						}
					} else {
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

	private void readPhotographFromDisk(PhotographRequestMessage photographRequestMessage) {
		Path photographPath = Paths.get(photographRequestMessage.getPhotographDocument().getPath());
		try {
			byte[] photograph = Files.readAllBytes(photographPath);
			String mimeType = Files.probeContentType(photographPath);
			Shoebox.LOGGER.debug("Actor: " + getSelf().toString() + " Location: " + photographPath.toString() + " Mime: " + mimeType);
			getSender().tell(new PhotographResponseMessage(photograph, mimeType), getSelf());
		} catch (IOException e) {
			/**
			 * TODO Need to remove photograph from database...
			 */
			Shoebox.LOGGER.error("Unable to read photograph: " + e.getMessage());
		}
	}

	private void readPhotographFromDatabase(PhotographResizeRequestMessage photographResizeRequestMessage) {
		PhotographDocument photographDocument = photographResizeRequestMessage.getPhotographDocument();
		int squareSize = photographResizeRequestMessage.getSquareSize();

		if (photographDocument.getSquareSize() <= squareSize) {
			readPhotographFromDisk(new PhotographRequestMessage(photographDocument));
		} else {

			/**
			 * Look in cache for photograph
			 */
			DatabaseFilter filter = new DatabaseFilter(PhotographCacheDocument.SQUARE_SIZE, squareSize, DatabaseFilterType.GREATER_EQUAL_THAN);
			PhotographCacheDocument photographCacheDocument = null;
			try {
				List<PhotographCacheDocument> photographCacheDocuments = database.getRelatedDocuments(photographDocument, PhotographCacheDocument.class, Arrays.asList(filter), PhotographCacheDocument.SQUARE_SIZE);
				if (!photographCacheDocuments.isEmpty()) {
					photographCacheDocument = photographCacheDocuments.get(0);
				}
			} catch (DatabaseException e) {
				final String message = "Unable to find PhotographCacheDocument for '" + photographDocument.getKey() + "': " + e.getMessage();
				Shoebox.LOGGER.error(message);
			}
			boolean writeCacheDocument = false;
			if (photographCacheDocument != null) {
				if (photographCacheDocument.getSquareSize() != squareSize) {
					/**
					 * We need to generate a resized photograph from our
					 * previously resized photograph (Performance Improvement)
					 */
					photographCacheDocument = generatePhotograph(photographDocument, photographCacheDocument, squareSize);
					writeCacheDocument = true;
				}
			} else {
				/**
				 * Generate Cached Photograph from Original Photograph
				 */
				photographCacheDocument = generatePhotograph(photographDocument, null, squareSize);
				writeCacheDocument = true;

			}
			if (photographCacheDocument != null) {
				conditionalReplyToSender(new PhotographResponseMessage(photographCacheDocument.getPhotograph(), photographDocument.getMimeType()));
				if (writeCacheDocument) {
					/**
					 * Save to Database
					 */
					writePhotographCacheToDatabase(photographDocument, photographCacheDocument);
					/**
					 * Update Photograph Record with new stash size
					 */
					updatePhotographStashSize(photographDocument, photographCacheDocument);
				}
			} else {
				/**
				 * TODO Need to remove photograph from database...
				 */
				conditionalReplyToSender(photographDocument.getName() + " could not be read or is not a resizable image format.");
			}
		}
	}

	private PhotographCacheDocument generatePhotograph(PhotographDocument photographDocument, PhotographCacheDocument photographCacheDocument, int newSquareSize) {
		/**
		 * Create resized image - needs to be moved into an actor
		 */
		Path photographPath = Paths.get(photographDocument.getPath());
		try {
			String mimeType = photographDocument.getMimeType();
			if (mimeType.startsWith("image/")) {
				String imageFormat = mimeType.substring(6, mimeType.length());

				BufferedImage resizedImage = null;
				Method method = Method.AUTOMATIC;
				/**
				 * This was too Ugly. Switching back to Automatic -KJ
				 */
				/*
				 * if(newSquareSize < 400){ method = Method.SPEED; }
				 */
				if (photographCacheDocument == null) {
					/**
					 * Resize from Disk
					 */
					Shoebox.LOGGER.debug("Resizing " + photographDocument.getKey() + " from disk square size: " + photographDocument.getSquareSize() + " -> " + newSquareSize);
					resizedImage = Scalr.resize(ImageIO.read(photographPath.toFile()), method, newSquareSize);
				} else {
					/**
					 * Resize from previous cached image (Performance)
					 */
					Shoebox.LOGGER.debug("Resizing " + photographDocument.getKey() + " from cache square size: " + photographCacheDocument.getSquareSize() + " -> " + newSquareSize);
					resizedImage = Scalr.resize(ImageIO.read(new ByteArrayInputStream(photographCacheDocument.getPhotograph())), method, newSquareSize);
				}
				Shoebox.LOGGER.debug("Actor: " + getSelf().toString() + " Location: " + photographPath.toString() + " Mime: " + mimeType);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(resizedImage, imageFormat, baos);
				baos.flush();
				byte[] photograph = baos.toByteArray();
				baos.close();

				return new PhotographCacheDocument(photographDocument, photograph, newSquareSize);
			} else {
				Shoebox.LOGGER.error("Could not resize photograph: " + photographDocument.getName());
				return null;
			}
		} catch (IOException e) {
			/**
			 * TODO Need to remove photograph from database...
			 */
			Shoebox.LOGGER.error("Unable to read photograph: " + photographDocument.getName() + " : " + e.getMessage());
			return null;
		}
	}

	private void updatePhotographStashSize(PhotographDocument photographDocument, PhotographCacheDocument photographCacheDocument) {
		try {
			photographDocument.setStashSize(photographDocument.getStashSize() + photographCacheDocument.getSize());
			database.updateDocument(photographDocument);
		} catch (DatabaseException e) {
			final String message = "Unable to update PhotographDocument stash size for '" + photographDocument.getKey() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
		}
	}

	private void writePhotographCacheToDatabase(PhotographDocument photographDocument, PhotographCacheDocument photographCacheDocument) {
		try {
			database.createDocument(photographCacheDocument);
			database.relateDocumentToDocument(photographDocument, photographCacheDocument);
		} catch (DatabaseException e) {
			final String message = "Unable to create/link PhotographCacheDocument for '" + photographDocument.getKey() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
		}
	}

}
