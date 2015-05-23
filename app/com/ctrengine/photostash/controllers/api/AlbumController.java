package com.ctrengine.photostash.controllers.api;

import play.Routes;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.DatabaseException;
import com.ctrengine.photostash.models.AlbumDocument;
import com.ctrengine.photostash.models.StoryDocument;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AlbumController extends Controller {

	public static Result getAlbums(Boolean extended) {
		ArrayNode albumsNode = Json.newObject().arrayNode();
		try {
			for (AlbumDocument albumDocument : PhotostashDatabase.INSTANCE.getAlbums()) {
				albumsNode.add(albumDocument.toJson(extended).put("link", routes.AlbumController.getAlbum(albumDocument.getKey(), extended).absoluteURL(request())));
			}
			return ok(albumsNode);
		} catch (DatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}

	public static Result getAlbum(String albumId, Boolean extended) {
		try {
			AlbumDocument albumDocument = PhotostashDatabase.INSTANCE.getAlbum(albumId);
			if (albumDocument == null) {
				return badRequest(Json.newObject().put("message", albumId + " not found."));
			} else {
				ObjectNode albumNode = albumDocument.toJson(extended);
				albumNode.put("link", routes.AlbumController.getAlbum(albumDocument.getKey(), extended).absoluteURL(request()));
				ArrayNode storysNode = albumNode.arrayNode();
				/**
				 * Get the stories associated with this AlbumDocument
				 */
				for (StoryDocument storyDocument : PhotostashDatabase.INSTANCE.getRelatedDocuments(albumDocument, StoryDocument.class)) {
					ObjectNode storyNode = storyDocument.toJson(extended);
					System.out.println("story: "+storyDocument.getName()+" key"+storyDocument.getCoverPhotographKey());
					if(storyDocument.getCoverPhotographKey() != null){
						storyNode.put("coverLink", routes.PhotographController.getPhotographImage(storyDocument.getCoverPhotographKey()).absoluteURL(request()));
					}
					storyNode.put("link", routes.StoryController.getStory(storyDocument.getKey(), extended).absoluteURL(request()));
					storysNode.add(storyNode);
				}
				albumNode.put("stories", storysNode);
				return ok(albumNode);
			}
		} catch (DatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}

	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter("jsRoutesAlbumController",
		/**
		 * Routes
		 */
		com.ctrengine.photostash.controllers.api.routes.javascript.AlbumController.getAlbums(),

		com.ctrengine.photostash.controllers.api.routes.javascript.AlbumController.getAlbum()));
	}
}
