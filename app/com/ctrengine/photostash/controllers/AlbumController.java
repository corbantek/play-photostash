package com.ctrengine.photostash.controllers;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.ctrengine.photostash.database.PhotostashDatabase;
import com.ctrengine.photostash.database.PhotostashDatabaseException;
import com.ctrengine.photostash.models.Album;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AlbumController extends Controller {

	public static Result getAlbums(Boolean extended) {
		ArrayNode albums = Json.newObject().arrayNode();
		try {
			for (Album album : PhotostashDatabase.INSTANCE.getAlbums()) {
				albums.add(album.toJson(extended));
			}
			return ok(albums);
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
				/**
				 * Get the stories associated with this Album
				 */
				PhotostashDatabase.INSTANCE.getStories(album);
				return ok(album.toJson(extended));
			}
		} catch (PhotostashDatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
}
