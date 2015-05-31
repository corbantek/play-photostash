package com.ctrengine.photostash.shoebox;

import java.io.File;

import com.ctrengine.photostash.models.AbstractFileDocument;
import com.ctrengine.photostash.models.AlbumDocument;
import com.ctrengine.photostash.models.PhotographDocument;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ShoeboxMessages {
	public static class OrganizeStatusRequestMessage {
	}

	public static class OrganizeStopMessage {
	}

	public static class OrganizeShoeboxMessage {
		private final String shoeboxPath;

		public OrganizeShoeboxMessage(String shoeboxPath) {
			this.shoeboxPath = shoeboxPath;
		}

		public String getShoeboxPath() {
			return shoeboxPath;
		}
	}

	public static class OrganizeCompleteMessage {
		private final AbstractFileDocument abstractFileDocument;
		private final File file;

		public OrganizeCompleteMessage(AbstractFileDocument abstractFileDocument, File file) {
			this.abstractFileDocument = abstractFileDocument;
			this.file = file;
		}

		public OrganizeCompleteMessage(File file) {
			this.abstractFileDocument = null;
			this.file = file;
		}

		public AbstractFileDocument getAbstractFileDocument() {
			return abstractFileDocument;
		}

		public File getFile() {
			return file;
		}
	}

	public static class OrganizeAlbumMessage {
		private final File albumDirectory;

		public OrganizeAlbumMessage(File albumDirectory) {
			this.albumDirectory = albumDirectory;
		}

		public File getAlbumDirectory() {
			return albumDirectory;
		}
	}

	public static class OrganizeStoryMessage {
		private final AlbumDocument albumDocument;
		private final File storyDirectory;

		public OrganizeStoryMessage(AlbumDocument albumDocument, File storyDirectory) {
			this.albumDocument = albumDocument;
			this.storyDirectory = storyDirectory;
		}

		public AlbumDocument getAlbumDocument() {
			return albumDocument;
		}

		public File getStoryDirectory() {
			return storyDirectory;
		}
	}

	public static class PhotographRequestMessage {
		private final PhotographDocument photographDocument;

		public PhotographRequestMessage(PhotographDocument photographDocument) {
			this.photographDocument = photographDocument;
		}

		public PhotographDocument getPhotograph() {
			return photographDocument;
		}
	}

	public static class PhotographResizeRequestMessage {
		private final PhotographDocument photographDocument;
		private final int squareSize;

		public PhotographResizeRequestMessage(PhotographDocument photographDocument, int squareSize) {
			this.photographDocument = photographDocument;
			this.squareSize = squareSize;
		}

		public PhotographDocument getPhotographDocument() {
			return photographDocument;
		}

		public int getSquareSize() {
			return squareSize;
		}
	}

	public static class PhotographResponseMessage {
		private final byte[] photograph;
		private final String mimeType;

		public PhotographResponseMessage(byte[] photograph, String mimeType) {
			this.photograph = photograph;
			this.mimeType = mimeType;
		}

		public byte[] getPhotograph() {
			return photograph;
		}

		public String getMimeType() {
			return mimeType;
		}
	}

	public static enum ResponseType {
		ERROR, WARNING, INFO
	}

	public static class ResponseMessage {
		private final ResponseType responseType;
		private final ObjectNode message;

		public ResponseMessage(ResponseType responseType, ObjectNode message) {
			this.responseType = responseType;
			this.message = message;
		}

		public ResponseType getResponseType() {
			return responseType;
		}

		public ObjectNode getMessage() {
			return message;
		}
	}
}
