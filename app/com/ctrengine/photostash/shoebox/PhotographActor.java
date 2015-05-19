package com.ctrengine.photostash.shoebox;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import akka.actor.UntypedActor;

import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResizeRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResponseMessage;

public class PhotographActor extends UntypedActor {

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
		/**
		 * TODO Get from database
		 */

		/**
		 * Create resized image - needs to be moved into an actor
		 */
		Path photographPath = Paths.get(photographResizeRequestMessage.getPhotograph().getPath());
		try {
			String mimeType = Files.probeContentType(photographPath);
			if (mimeType.startsWith("image/")) {
				String imageFormat = mimeType.substring(6, mimeType.length());
				BufferedImage resizedImage = Scalr.resize(ImageIO.read(photographPath.toFile()), photographResizeRequestMessage.getSquareLength());
				Shoebox.LOGGER.debug("Actor: " + getSelf().toString() + " Location: " + photographPath.toString() + " Mime: " + mimeType);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(resizedImage, imageFormat, baos);
				baos.flush();
				byte[] photograph = baos.toByteArray();
				baos.close();

				getSender().tell(new PhotographResponseMessage(photograph, mimeType), getSelf());
			}else{
				/**
				 * TODO Need to remove photograph from database...
				 */
				getSender().tell(photographResizeRequestMessage.getPhotograph().getName()+" is not a resizable image format.", getSelf());
			}
		} catch (IOException e) {
			/**
			 * TODO Need to remove photograph from database...
			 */
			Shoebox.LOGGER.error("Unable to read photograph: " + e.getMessage());
		}
	}

}
