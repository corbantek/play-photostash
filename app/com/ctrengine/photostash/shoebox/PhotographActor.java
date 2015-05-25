package com.ctrengine.photostash.shoebox;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;

import akka.actor.UntypedActor;

import com.ctrengine.photostash.database.DatabaseException;
import com.ctrengine.photostash.database.DatabaseFilter;
import com.ctrengine.photostash.database.DatabaseFilter.DatabaseFilterType;
import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.models.PhotographCacheDocument;
import com.ctrengine.photostash.models.PhotographDocument;
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
		if (message instanceof PhotographRequestMessage) {
			readPhotographFromDisk((PhotographRequestMessage) message);
		} else if (message instanceof PhotographResizeRequestMessage) {
			readPhotographFromDatabase((PhotographResizeRequestMessage) message);
		} else {
			unhandled(message);
		}
	}

	private void readPhotographFromDisk(PhotographRequestMessage photographRequestMessage) {
		Path photographPath = Paths.get(photographRequestMessage.getPhotograph().getPath());
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
				getSender().tell(new PhotographResponseMessage(photographCacheDocument.getPhotograph(), photographDocument.getMimeType()), getSelf());
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
				getSender().tell(photographDocument.getName() + " could not be read or is not a resizable image format.", getSelf());
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
				if(newSquareSize < 400){
					method = Method.SPEED;
				}
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

	private void writePhotographCacheToDatabase(PhotographDocument photographDocument, PhotographCacheDocument photographCacheDocument) {
		try {
			photographDocument.setStashSize(photographDocument.getStashSize()+photographCacheDocument.getSize());
			database.updateDocument(photographDocument);
		} catch (DatabaseException e) {
			final String message = "Unable to update PhotographDocument stash size for '" + photographDocument.getKey() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
		}
	}
	
	private void updatePhotographStashSize(PhotographDocument photographDocument, PhotographCacheDocument photographCacheDocument){
		try {
			database.createDocument(photographCacheDocument);
			database.relateDocumentToDocument(photographDocument, photographCacheDocument);
		} catch (DatabaseException e) {
			final String message = "Unable to create/link PhotographCacheDocument for '" + photographDocument.getKey() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
		}
	}

}
