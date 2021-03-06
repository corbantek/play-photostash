package com.ctrengine.photostash.controllers.api;

import static akka.pattern.Patterns.ask;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.Routes;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.DatabaseException;
import com.ctrengine.photostash.models.PhotographDocument;
import com.ctrengine.photostash.shoebox.Shoebox;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResizeRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.PhotographResponseMessage;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PhotographController extends Controller {
	private static final int PHOTOGRAPH_REQUEST_TIMEOUT = 30000;

	public static Result getPhotographs(Boolean extended) {
		try {
			ArrayNode photographNode = Json.newObject().arrayNode();
			for (PhotographDocument photographDocument : PhotostashDatabase.INSTANCE.getPhotographs()) {
				photographNode.add(photographDocument.toJson(extended).put("link", routes.PhotographController.getPhotograph(photographDocument.getKey(), extended).absoluteURL(request())));
			}
			return ok(photographNode);
		} catch (DatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}

	public static Result getPhotograph(String photographId, Boolean extended) {
		try {
			PhotographDocument photographDocument = PhotostashDatabase.INSTANCE.getPhotograph(photographId);
			if (photographDocument == null) {
				return badRequest(Json.newObject().put("message", photographId + " not found."));
			} else {
				ObjectNode photogrphNode = photographDocument.toJson(extended);
				
				ObjectNode photographImages = Json.newObject();
				photographImages.put("original", routes.PhotographController.getPhotographImage(photographDocument.getKey()).absoluteURL(request()));
				photographImages.put("thumbnail", routes.PhotographController.getPhotographResizeImage(photographDocument.getKey(), 100).absoluteURL(request()));
				photographImages.put("small", routes.PhotographController.getPhotographResizeImage(photographDocument.getKey(), 640).absoluteURL(request()));
				photographImages.put("standard", routes.PhotographController.getPhotographResizeImage(photographDocument.getKey(), 1024).absoluteURL(request()));
				photographImages.put("large", routes.PhotographController.getPhotographResizeImage(photographDocument.getKey(), 1600).absoluteURL(request()));
				
				photogrphNode.put("images", photographImages);
				return ok(photogrphNode);
			}
		} catch (DatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}

	public static Promise<Result> getPhotographImage(final String photographId) {
		try {
			final PhotographDocument photographDocument = PhotostashDatabase.INSTANCE.getPhotograph(photographId);
			if (photographDocument == null) {
				return Promise.promise(new Function0<Result>() {
					@Override
					public Status apply() throws Throwable {
						return badRequest(Json.newObject().put("message", photographId + " not found."));
					}
				});
			} else {
				return Promise.wrap(ask(Shoebox.INSTANCE.getPhotographRouter(), new PhotographRequestMessage(photographDocument), PHOTOGRAPH_REQUEST_TIMEOUT)).map(new Function<Object, Result>() {
					public Result apply(Object response) {
						if (response instanceof PhotographResponseMessage) {
							PhotographResponseMessage photographResponseMessage = (PhotographResponseMessage) response;
							return ok(photographResponseMessage.getPhotograph()).as(photographResponseMessage.getMimeType());
						} else {
							return internalServerError(Json.newObject().put("message", response.toString()));
						}
					}
				});
			}
		} catch (final DatabaseException e) {
			return Promise.promise(new Function0<Result>() {
				@Override
				public Status apply() throws Throwable {
					return internalServerError(Json.newObject().put("message", e.getMessage()));
				}
			});

		}
	}

	public static Promise<Result> getPhotographResizeImage(final String photographId, final Integer squareSize) {
		try {
			final PhotographDocument photographDocument = PhotostashDatabase.INSTANCE.getPhotograph(photographId);
			if (photographDocument == null) {
				return Promise.promise(new Function0<Result>() {
					@Override
					public Status apply() throws Throwable {
						return badRequest(Json.newObject().put("message", photographId + " not found."));
					}
				});
			} else {
				if (squareSize == null) {
					return getPhotographImage(photographId);
				} else {
					return Promise.wrap(ask(Shoebox.INSTANCE.getPhotographRouter(), new PhotographResizeRequestMessage(photographDocument, squareSize), PHOTOGRAPH_REQUEST_TIMEOUT)).map(new Function<Object, Result>() {
						public Result apply(Object response) {
							if (response instanceof PhotographResponseMessage) {
								PhotographResponseMessage photographResponseMessage = (PhotographResponseMessage) response;
								return ok(photographResponseMessage.getPhotograph()).as(photographResponseMessage.getMimeType());
							} else {
								return internalServerError(Json.newObject().put("message", response.toString()));
							}
						}
					});
				}
			}
		} catch (final DatabaseException e) {
			return Promise.promise(new Function0<Result>() {
				@Override
				public Status apply() throws Throwable {
					return internalServerError(Json.newObject().put("message", e.getMessage()));
				}
			});

		}
	}
	
	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter("jsRoutesPhotographController",
		/**
		 * Routes
		 */
		com.ctrengine.photostash.controllers.api.routes.javascript.PhotographController.getPhotograph(),
		
		com.ctrengine.photostash.controllers.api.routes.javascript.PhotographController.getPhotographs(),

		com.ctrengine.photostash.controllers.api.routes.javascript.PhotographController.getPhotographImage(),
		
		com.ctrengine.photostash.controllers.api.routes.javascript.PhotographController.getPhotographResizeImage()));
	}
}
