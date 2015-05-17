package com.ctrengine.photostash.shoebox;

public class ShoeboxMessages {
	public static class OrganizeMessage {
		private final String shoeboxPath;

		public OrganizeMessage(String shoeboxPath) {
			this.shoeboxPath = shoeboxPath;
		}

		public String getShoeboxPath() {
			return shoeboxPath;
		}
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
