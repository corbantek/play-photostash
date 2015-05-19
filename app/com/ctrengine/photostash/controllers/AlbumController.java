package com.ctrengine.photostash.controllers;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.PhotostashDatabaseException;
import com.ctrengine.photostash.models.Album;
import com.ctrengine.photostash.models.Story;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AlbumController extends Controller {

	public static Result getAlbums(Boolean extended) {
		ArrayNode albumsNode = Json.newObject().arrayNode();
		try {
			for (Album album : PhotostashDatabase.INSTANCE.getAlbums()) {
				albumsNode.add(album.toJson(extended).put("link", routes.AlbumController.getAlbum(album.getKey(), extended).absoluteURL(request())));
			}
			return ok(albumsNode);
		} catch (PhotostashDatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
	
	public static Result getAlbum(String albumId, Boolean extended) {
		try {
			Album album = PhotostashDatabase.INSTANCE.getAlbum(albumId);
			if(album == null){
				return badRequest(Json.newObject().put("message", albumId+" not found."));
			}else{
				ObjectNode albumNode = album.toJson(extended);
				ArrayNode storysNode = albumNode.arrayNode();
				/**
				 * Get the stories associated with this Album
				 */
				for(Story story: PhotostashDatabase.INSTANCE.getRelatedDocuments(album, Story.class)){
					storysNode.add(story.toJson(extended).put("link", routes.StoryController.getStory(story.getKey(), extended).absoluteURL(request())));
				}
				albumNode.put("stories", storysNode);
				return ok(albumNode);
			}
		} catch (PhotostashDatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
}
