package com.ctrengine.photostash.shoebox;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import akka.actor.UntypedActor;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.PhotostashDatabaseException;
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

		/**
		 * Look in cache for photograph
		 */
		Map<String, Object> filter = new TreeMap<String, Object>();
		filter.put(PhotographCacheDocument.SQUARE_SIZE, squareSize);
		PhotographCacheDocument photographCacheDocument = null;
		try {
			List<PhotographCacheDocument> photographCacheDocuments = database.getRelatedDocuments(photographDocument, PhotographCacheDocument.class, filter);
			if (!photographCacheDocuments.isEmpty()) {
				if (photographCacheDocuments.size() == 1) {
					photographCacheDocument = photographCacheDocuments.get(0);
				} else {
					/**
					 * TODO: Why is there more than one???
					 */
				}
			}
		} catch (PhotostashDatabaseException e) {
			final String message = "Unable to find PhotographCacheDocument for '" + photographDocument.getKey() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
		}
		if (photographCacheDocument != null) {
			/**
			 * Send Cached Photograph
			 */
			getSender().tell(new PhotographResponseMessage(photographCacheDocument.getPhotograph(), photographDocument.getMimeType()), getSelf());
		} else {
			/**
			 * Generate Cached Photograph 
			 */
			photographCacheDocument = generatePhotograph(photographDocument, squareSize);
			if (photographCacheDocument != null) {
				getSender().tell(new PhotographResponseMessage(photographCacheDocument.getPhotograph(), photographDocument.getMimeType()), getSelf());
				/**
				 * Save to Database
				 */
				writePhotographCacheToDatabase(photographDocument, photographCacheDocument);
			} else {
				/**
				 * TODO Need to remove photograph from database...
				 */
				getSender().tell(photographDocument.getName() + " could not be read or is not a resizable image format.", getSelf());
			}
		}
	}

	private PhotographCacheDocument generatePhotograph(PhotographDocument photographDocument, int squareSize) {
		/**
		 * Create resized image - needs to be moved into an actor
		 */
		Path photographPath = Paths.get(photographDocument.getPath());
		try {
			String mimeType = photographDocument.getMimeType();
			if (mimeType.startsWith("image/")) {
				String imageFormat = mimeType.substring(6, mimeType.length());

				BufferedImage resizedImage = Scalr.resize(ImageIO.read(photographPath.toFile()), squareSize);
				Shoebox.LOGGER.debug("Actor: " + getSelf().toString() + " Location: " + photographPath.toString() + " Mime: " + mimeType);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(resizedImage, imageFormat, baos);
				baos.flush();
				byte[] photograph = baos.toByteArray();
				baos.close();

				return new PhotographCacheDocument(photographDocument, photograph, squareSize);
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
			database.createDocument(photographCacheDocument);
			database.relateDocumentToDocument(photographDocument, photographCacheDocument);
		} catch (PhotostashDatabaseException e) {
			final String message = "Unable to create/link PhotographCacheDocument for '" + photographDocument.getKey() + "': " + e.getMessage();
			Shoebox.LOGGER.error(message);
		}
	}

}
