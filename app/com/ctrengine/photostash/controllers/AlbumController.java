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
				ObjectNode albumNode = Json.newObject();
				albumNode.put("albumId", album.getKey());
				albumNode.put("name", album.getName());
				if (extended) {
					albumNode.put("description", album.getDescription());
				}
				albums.add(albumNode);
			}
			return ok(albums);
		} catch (PhotostashDatabaseException e) {
			return internalServerError(Json.newObject().put("message", e.getMessage()));
		}
	}
}
