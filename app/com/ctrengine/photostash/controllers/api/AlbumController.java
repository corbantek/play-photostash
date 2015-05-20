package com.ctrengine.photostash.controllers.api;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.PhotostashDatabaseException;
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
		} catch (PhotostashDatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
	
	public static Result getAlbum(String albumId, Boolean extended) {
		try {
			AlbumDocument albumDocument = PhotostashDatabase.INSTANCE.getAlbum(albumId);
			if(albumDocument == null){
				return badRequest(Json.newObject().put("message", albumId+" not found."));
			}else{
				ObjectNode albumNode = albumDocument.toJson(extended);
				ArrayNode storysNode = albumNode.arrayNode();
				/**
				 * Get the stories associated with this AlbumDocument
				 */
				for(StoryDocument storyDocument: PhotostashDatabase.INSTANCE.getRelatedDocuments(albumDocument, StoryDocument.class)){
					storysNode.add(storyDocument.toJson(extended).put("link", routes.StoryController.getStory(storyDocument.getKey(), extended).absoluteURL(request())));
				}
				albumNode.put("stories", storysNode);
				return ok(albumNode);
			}
		} catch (PhotostashDatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
}