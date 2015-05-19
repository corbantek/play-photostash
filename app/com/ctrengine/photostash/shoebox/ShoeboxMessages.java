package com.ctrengine.photostash.shoebox;

import com.ctrengine.photostash.models.Photograph;

public class ShoeboxMessages {
	public static class OrganizeMessage {
	}

	public static class PhotographRequestMessage {
		private final Photograph photograph;

		public PhotographRequestMessage(Photograph photograph) {
			this.photograph = photograph;
		}

		public Photograph getPhotograph() {
			return photograph;
		}
	}

	public static class PhotographResizeRequestMessage {
		private final Photograph photograph;
		private final int squareLength;

		public PhotographResizeRequestMessage(Photograph photograph, int squareLength) {
			this.photograph = photograph;
			this.squareLength = squareLength;
		}

		public Photograph getPhotograph() {
			return photograph;
		}

		public int getSquareLength() {
			return squareLength;
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

	public static class InitializeMessage {
	}

	public static enum ResponseType {
		ERROR, WARNING, INFO
	}

	public static class ResponseMessage {
		private final ResponseType responseType;
		private final String message;

		public ResponseMessage(ResponseType responseType, String message) {
			this.responseType = responseType;
			this.message = message;
		}

		public ResponseType getResponseType() {
			return responseType;
		}

		public String getMessage() {
			return message;
		}
	}
}
